# ADR-004: Code-first contracts

- **Status:** Accepted
- **Date:** 2026-10-02
- **Author:** Andrii Artemenko
- **Related ADRs:** ADR-002 (tech stack and hosting), ADR-003 (Kotlin Multiplatform for `shared`), ADR-005 (authentication), ADR-006 (testing), ADR-007 (offline-first sync and data model)

## Context

Mintro Core exposes two contracts:

- **HTTP API** for the Android App: the sync protocol (`/sync/*`) and resource endpoints for everything that is not synchronized (members, invites, login links, devices).
- **MCP tools** for the Telegram Agent: tool names, descriptions, and argument/result schemas, published at runtime via `tools/list`.

All HTTP consumers are Kotlin (Android App, Core, end-to-end tests). Sync payloads mix records of different entity types, expressed as sealed hierarchies. The Telegram Agent and Core are deployed independently. Most code is written by AI agents, so an accidental contract change must be impossible to miss in review.

This ADR defines how contracts are expressed, encoded, versioned, documented, and verified. **What** is synchronized and the sync algorithm are decided in ADR-007.

## Decision drivers

1. Client/server incompatibility is caught at compile time.
2. Polymorphic messages are expressed naturally.
3. Every contract change is visible as an explicit diff in the PR.
4. Old app versions keep working after Core is updated.
5. Readable documentation without manual upkeep.

## Considered options

| Option | Verdict |
|---|---|
| **Code-first; generated documents committed and drift-checked** | **Chosen.** Kotlin types are the source of truth; generated OpenAPI and MCP tool snapshots make every change visible. |
| Spec-first: OpenAPI as source, Kotlin generated | Rejected. Extra generation step in an all-Kotlin system; `oneOf` + `discriminator` generates poor Kotlin for sealed types; agents edit YAML and regenerate. |
| Code-first without generated documents | Rejected. Contract changes hide in ordinary code diffs; no readable documentation. |
| Protobuf / gRPC | Rejected. Binary payloads are harder to debug; more tooling on Android and Cloud Run; no need for streaming or cross-language clients. |

## Decision

### 1. Source of truth

| Contract | Source of truth | Generated document |
|---|---|---|
| Sync protocol | Kotlin classes in `:shared:contract` | `docs/api/openapi.yaml` |
| Resource endpoints | Kotlin DTOs in `:shared:contract`, routes in Core | `docs/api/openapi.yaml` |
| MCP tools | Tool definitions in Core (MCP Kotlin SDK) | `docs/api/mcp-tools.json` (snapshot of `tools/list`) |

### 2. Encoding

- **JSON** via kotlinx-serialization, on both sides.
- **Field names:** `camelCase`, matching Kotlin property names; no naming strategy configured.
- **Polymorphism:** class discriminator `type`. In sync payloads it names the entity type of a record (`spending`, `expenditure`, `budget`, `category`, …); deletion markers use `type: "deletion"` with `entityType` and `id`.
- **Optional fields** have defaults and are omitted when equal to the default.
- **Shared `Json` configuration** lives in `:shared:contract` and is used by Core and the Android App:

```kotlin
val MintroJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    explicitNulls = false
    classDiscriminator = "type"
}
```

### 3. Data representation

| Type | Wire format | Example |
|---|---|---|
| Money | `amountMinor: Long` + `currency` (ISO 4217); never floating point | `{"amountMinor": 35000, "currency": "EUR"}` |
| Instant | ISO-8601 string in UTC with `Z` | `"2026-10-02T14:30:00Z"` |
| Date | ISO-8601 date | `"2026-10-02"` |
| Time zone | IANA name | `"Europe/Madrid"` |
| Identifier | UUID string | `"0192f0c4-…"` |
| Enum | `SCREAMING_SNAKE_CASE` string | `"PARENT_NOT_FOUND"` |

Domain semantics (family time zone, budget period boundaries, currency conversion and rounding) are specified in `docs/spec/domain.md`.

### 4. HTTP API style

**Sync protocol** — RPC-style, its own versioning:

- `POST /sync/push`: request contains `protocolVersion` and the list of records and deletion markers. Response contains one result per record: `status` (`ACCEPTED`, `IGNORED`, `REJECTED`), the assigned `version` when accepted, the **current server state** (record or deletion marker) when ignored or rejected, and a `code` when rejected (ADR-007).
- `GET /sync/pull?since=<version>&limit=<n>&protocolVersion=<n>`: response contains `records`, `deletions`, `nextSince`, `hasMore`.
- `protocolVersion` is sent with every sync request; no version in the path.

**Resource endpoints** — REST, versioned in the path:

- `/api/v1/<plural-noun>`, kebab-case paths: `/api/v1/members`, `/api/v1/invites`, `/api/v1/login-links`, `/api/v1/devices`;
- standard methods: `GET`, `POST`, `PATCH`, `DELETE`; query parameters in `camelCase`;
- list endpoints use cursor pagination: `?cursor=…&limit=…`, response contains `items` and `nextCursor`;
- `POST` endpoints that create resources accept an `Idempotency-Key` header. Keys are stored per family with the original response for 24 hours.

### 5. Errors

**HTTP errors** use RFC 9457 Problem Details, content type `application/problem+json`:

```json
{
  "type": "<problemBaseUrl>/parent-not-found",
  "title": "Referenced record not found",
  "status": 409,
  "detail": "Expenditure 'Cafe' no longer exists.",
  "code": "PARENT_NOT_FOUND"
}
```

`problemBaseUrl` is configuration, not a hardcoded domain, until the product domain is confirmed (ADR-001).

- `code` is a machine-readable enum and part of the contract; clients branch on `code`, never on `title` or `detail`.
- Status codes: `400` validation, `401` missing/invalid credentials, `403` not allowed, `404` not found (also for resources of other families), `409` conflict with current state, `426` unsupported protocol version, `429` rate limit, `5xx` server faults.

**Per-record results in sync** are not HTTP errors: `POST /sync/push` returns `200` with a result per record. A rejected record carries a `code` from the same enum as Problem Details, not a full Problem Details body.

### 6. Versioning and compatibility

| Change | Compatible? | Action |
|---|---|---|
| New optional field, new endpoint, new enum value in a response the client ignores | Yes | No version change |
| New entity type in sync payloads | Partially | Clients must handle unknown types (below) |
| Removed/renamed field, changed meaning, new required field | No | Increment `protocolVersion` (sync) or path version (REST) |

- Core supports the **current and previous** sync protocol versions. Older clients receive `426` with code `UNSUPPORTED_PROTOCOL`; the app asks the user to update.
- **Unknown types:** every sealed hierarchy received by clients has a fallback `Unknown` subtype (default polymorphic deserializer). The client does not crash; it does not advance `lastPulledVersion` past the unknown record (so nothing is skipped permanently) and asks the user to update.
- REST `v1` stays available until all supported app versions use `v2`.

### 7. MCP tools

| Aspect | Rule |
|---|---|
| Naming | `snake_case`, verb + object, using glossary terms: `add_spending`, `get_budget_status`, `list_expenditures` |
| Granularity | Task-oriented tools matching user intents, not table-level CRUD |
| Descriptions | Part of the contract: the LLM selects tools by description; wording changes are reviewed like schema changes |
| Arguments | Same representation rules as section 3; no identity or family fields |
| Results | Structured result matching a declared output schema, plus a short human-readable text |
| Errors | Tool result with `isError = true` and an actionable message (for example, listing valid categories). JSON-RPC errors only for transport and authentication failures |
| Writes | Write tools accept an `idempotencyKey` argument so the agent can retry safely |
| Identity | Never a tool argument; comes from authenticated headers (ADR-005) |
| Compatibility | Core and the Telegram Agent deploy independently: additive changes only. Removal: mark as deprecated in the description, remove in a later release |

### 8. Documentation and verification

- One Gradle task regenerates `docs/api/openapi.yaml` and `docs/api/mcp-tools.json`; CI fails if the committed files differ.
- Protocol semantics that schemas cannot express (conflict rules, ordering, version rules) are specified in `docs/spec/sync-protocol.md` (ADR-007).
- **Contract tests:** JSON fixtures recorded for the current and previous protocol versions live in `:shared:contract` test resources; they must deserialize and be handled by Core.
- The OpenAPI generator library for Ktor is chosen at implementation time; the document serves review and drift detection, not code generation.

## Consequences

### Positive

- Incompatible changes between the Android App and Core fail compilation.
- Every contract change appears as a dedicated diff in `docs/api/`.
- One error format and one data representation across all endpoints and tools.
- Old app versions degrade gracefully instead of crashing.

### Negative and risks

- **OpenAPI generation quality** may require manual schema hints for sealed types. Acceptable: the document is for reading and drift detection.
- **Version discipline** depends on recognizing breaking changes. Mitigated by fixture-based contract tests for the previous version.
- **Supporting two protocol versions** adds code paths in Core. Accepted for a limited transition window.

## Compliance

- CI drift check for `docs/api/openapi.yaml` and `docs/api/mcp-tools.json`.
- Changes to `docs/api/*` and `:shared:contract` require explicit approval in review; in the farm, only the contract step may change them.
- Contract tests for current and previous protocol versions pass.
- All HTTP errors are Problem Details with a `code`; enforced by a Ktor status-pages handler and tests.
- `CLAUDE.md` states: change contracts only in `:shared:contract` or Core's MCP tool definitions, then regenerate `docs/api/`.
- Revisit and switch to spec-first if a non-Kotlin client appears or the API becomes public.
