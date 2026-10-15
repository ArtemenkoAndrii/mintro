# ADR-007: Offline-first sync and data model

- **Status:** Accepted
- **Date:** 2026-10-03
- **Author:** Andrii Artemenko
- **Related ADRs:** ADR-002 (tech stack and hosting), ADR-003 (Kotlin Multiplatform for `shared`), ADR-004 (code-first contracts), ADR-005 (authentication), ADR-006 (testing)

## Context

Several family members edit shared data from different phones, often offline. Mintro Core is the single entry point to data; the Android App keeps a local copy.

Requirements:

- edits work offline and are synced later;
- on concurrent edits, **the later edit by device time wins**; an old offline edit must not override a newer one;
- the algorithm is as simple as possible;
- adding or changing entities must not require changing this ADR.

This ADR defines sync classes and rules. Concrete entities, attributes, and domain rules live in living documents:

- `docs/spec/domain.md` — entities, attributes, domain invariants;
- `docs/spec/sync-protocol.md` — for every entity: sync class, natural key (if any), parents.

## Decision

### 1. Sync classes

Every entity belongs to exactly one class.

| Class | Rule | Examples (illustrative) |
|---|---|---|
| **Client-created** | Id is UUID v7, generated on the device. Synced by the algorithm below. | Spending, category |
| **Naturally keyed** | May be created independently on several devices. Id is UUID v5 of the normalized natural key, so independent creations merge. Core recomputes the id and rejects mismatches. | Budget for a period, expenditure in a budget |
| **Server-owned** | Clients only read it via pull; writes go through REST with authorization (ADR-005). | Family, member |
| **Server-only** | Never synced; exists only in Core and is managed through REST. | Login methods, devices, invites |
| **Device-local** | Never leaves the device. | Raw captured messages |
| **Derived** | Cached value computed from other data; recomputed as a full aggregation, never incremented; never synced. Core computes its own when needed. | Budget totals |

Rules for keys:

- The UUID v5 namespace is a constant in `:shared:domain`.
- Natural key components are normalized before hashing: Unicode NFC, trimmed, case-folded. Uniqueness of natural keys is a domain invariant.
- References between entities use ids, never names.

### 2. Sync fields

| Field | Set by | Purpose |
|---|---|---|
| `updatedAt` | Device, on every local edit | Decides the winner |
| `version` | Core | Position in the family's change sequence; used by pull |
| `isDirty` | Device, local only | Changed locally, not yet pushed |
| `localRev` | Device, local only | Increments on every local edit; guards against races |

Sync fields are not part of the domain model (ADR-003): `updatedAt` and `version` exist in contract DTOs and Core storage; `isDirty` and `localRev` exist only in the device's local database. Server-originated writes (for example, member merge in ADR-005) use Core's time as `updatedAt`.

Core keeps per family:

- a **counter**, incremented for every accepted change;
- a **deletion log** (entity type, id, deletion time, version), kept indefinitely. Deleted records are removed from domain data.

### 3. Algorithm

**Local edit.** One local transaction: write the record, `updatedAt = now`, `isDirty = true`, `localRev++`.

**Push.**

1. Collect all dirty records with their `localRev`, ordered **parents before children** by the reference graph declared in `sync-protocol.md`.
2. Send them in one request as full record state; deletions as delete markers.
3. Core processes them in order. For each record:
   - `updatedAt` later than Core's clock → replaced with Core's time;
   - a referenced record does not exist → rejected with a `code` (ADR-004);
   - `updatedAt` later than the stored record or its deletion log entry, or no record exists → stored whole, counter incremented, `version` assigned;
   - otherwise → ignored as stale;
   - equal `updatedAt` → the later arrival wins.
4. The response contains a result per record: accepted with its new `version`; or ignored/rejected **with the current server state** (the record or a deletion marker).
5. For each record whose `localRev` still equals the sent value, the device clears `isDirty` and, for ignored or rejected records, applies the returned server state. Records edited during the push stay dirty and go in the next push.

Returning the server state is required: pull skips dirty records, so without it a device whose edit was ignored would never receive the winning version, which is already behind its `lastPulledVersion`.

**Pull.**

1. Fetch all changes and deletion log entries with `version > lastPulledVersion`, all pages first (paging per ADR-004).
2. Apply them in one local transaction with deferred foreign keys.
3. Skip incoming records that are dirty locally; Core will decide when they are pushed.
4. Recompute affected derived values; update `lastPulledVersion`.

**Access revoked.** If the member was removed or the family deleted, Core answers sync requests with `403` and a code (ADR-005); the device stops syncing and informs the user. Local data is not wiped remotely.

**Scheduling.** Push then pull, as a single unique background job: on app start, after local edits, and periodically. An FCM data message (ADR-002) only triggers the job earlier; correctness does not depend on it. A new device pulls from version 0.

### 4. Conflict outcomes

| Situation | Result |
|---|---|
| Two devices edit the same record | Later `updatedAt` wins, for the whole record |
| Old offline edit arrives after a newer one | Ignored |
| Edit vs delete | Later by time wins; a later edit restores a deleted record |
| Two devices create the same naturally keyed record | Same id; records merge |
| Write references a deleted record | Rejected; the response carries the current server state |
| Edit ignored as stale | The response carries the winning server version; the device applies it |

### 5. Storage layout

```
families/{familyId}                     counter, settings
families/{familyId}/{collection}/{id}   current state, updatedAt, version
families/{familyId}/deletions/{id}      type, deletedAt, version
```

Pull queries each synced collection on `version > N` (composite indexes in Terraform). System-wide identity collections live outside families (ADR-005).

Each accepted change costs two writes (the record and the family counter), three for deletions. The capacity estimate against the Firestore free tier lives in `docs/architecture.md`.

### 6. Adding or changing an entity

1. Choose its sync class.
2. Declare its natural key (if naturally keyed) and its parents in `sync-protocol.md`.
3. Describe attributes and invariants in `domain.md`.
4. Follow ADR-004 for contract changes.

A new ADR is required only if an entity fits no class (for example, if field-level merge becomes necessary).

## Considered alternatives

| Alternative | Rejected because |
|---|---|
| Last write by server arrival | An old offline edit would override a newer one |
| Client-incremented revision numbers | Compare the number of edits, not time; the first writer wins |
| Broadcast-only distribution | Offline devices miss broadcasts; catch-up is needed anyway |
| Event sourcing with a stored event log | Only the latest state per record matters; storage and projections add cost |
| Outbox queue with operation ids | Dirty flags give the same result with automatic coalescing |
| Field-level merge, CRDTs | Complexity beyond the needs of a family budget |
| Firestore SDK on the device | Authorization moves into Security Rules; Core is no longer the single entry point |
| Online-only writes | No manual entry without network |
| Soft-deleted records kept in domain data | The separate deletion log is smaller and keeps collections clean |

## Consequences

### Positive

- Works offline with four mechanisms: dirty flags, time comparison, versioned pull, deletion log.
- Independent creation of naturally keyed records is safe.
- New entities are added by classification, without new architecture.
- Correctness does not depend on push notifications.

### Negative and risks

- **Whole-record conflicts:** concurrent edits of different fields of one record lose one edit. Accepted.
- **Device clocks:** a device clock running behind can lose to an earlier real edit. Future timestamps are clamped; accepted for family use.
- **Growing deletion log:** negligible at family scale.

## Compliance

- Every synced entity has a row in `sync-protocol.md`; a test fails if an entity in `:shared:contract` is not classified.
- Tests: two `sync-client` instances against a real Core converge in every scenario of section 4, including offline edits, edits during push, ignored edits, and revoked access.
- Tests: v5 id recomputation, future-timestamp clamping, rejection of writes referencing missing records, derived values never present in payloads.
- Property tests: random sequences of offline edits on two clients converge to the same state.
- `CLAUDE.md`: never sync derived values; clear `isDirty` only conditionally on `localRev`.
