# Review of ADR-001 … ADR-007

- **Date:** 2026-10-07
- **Scope:** `docs/adr/ADR-001` to `ADR-007` (all marked *Accepted*, dated 2026-10-01 to 2026-10-03)
- **Method:** Read each ADR in full and cross-checked claims between them. External facts (GCP quotas, library maturity, Play policy) were **not** re-verified. Items that depend on them are marked *verify*.

## 1. Overall assessment

The set is **strong and mostly coherent**. Each ADR has drivers, rejected alternatives, consequences and a compliance section, and the cross-references are consistent. The main strengths:

- The core idea for AI-written code is sound. Compile-time boundaries (KMP, Gradle module deps), a drift-checked contract, tests as the oracle, and an anti-test-gaming section.
- The sync design (ADR-007) is simple and keeps Core as the single entry point. Returning server state on an ignored push closes a classic dead-end.
- The security model (ADR-005) is careful. Identity never comes from LLM-controlled arguments, credentials are separated per route group, and the Telegram trust chain is explicit.
- The cost guardrails are concrete: `max_instances`, no WebSocket, and delete-after-deploy.

The weaknesses are not in the big choices. They fall into three groups:

1. A few **real design holes in sync and contract evolution**. Two ADRs make promises that the rules in other ADRs break (§3, items C1–C4).
2. **Telegram Agent behaviour is underspecified**, and what is said about it is partly contradictory (C5, C6).
3. **Whole topics have no ADR** and carry the highest external risk: Play Store SMS policy, EU data protection, legacy data migration, and the guardrails around the AI agents themselves (§5).

No decision needs to be reversed. Roughly ten need amendment or an explicit follow-up before implementation starts.

## 2. Soundness of each decision

| ADR | Verdict | Comment |
|---|---|---|
| 001 Monorepo + naming | **Sound** | Product monorepo plus separate farm repo is well argued. Role-based naming is fine. The gaps are protection of CI/build files against agents (R3) and small naming inconsistencies (M1). |
| 002 Stack + hosting | **Sound, with questionable details** | Cloud Run + Firestore + FCM suits the constraints. Questionable: deleting images after deploy (R5), region choice (R2), and "build per environment" instead of promoting one artifact (R6). |
| 003 KMP for `shared` | **Defensible, not obviously necessary** | Compile-time protection from JDK APIs is real. But the one place that uses JVM APIs (Unicode normalization) is exactly the risky one (R1). A plain JVM module plus Android API-level checking would also solve the stated problem at lower build cost. KMP is justified mainly by optional future iOS/web, which is speculative. |
| 004 Code-first contracts | **Sound; two compatibility rules are wrong as written** | See C1 and C2. |
| 005 Auth + membership | **Sound** | Best ADR of the set. Gaps: email-invite lookup index, the owner/Google invariant versus unlink, rate-limit storage (C6, M3). |
| 006 Testing | **Sound; one blind spot** | The headless e2e uses in-memory ports, so the most fragile client code (Room transactions, `localRev` guard) is untested by it (R4). The 10-minute budget for stage 2 is optimistic. |
| 007 Sync | **Sound for the stated needs; several edge cases open** | See C3, C4 and R7–R9. |

## 3. Contradictions and inconsistencies between ADRs

### Substantive (need a decision)

**C1 — "New optional field is compatible" (ADR-004 §6) vs. whole-record push (ADR-007)**
ADR-004 says a new optional field needs no version bump, and `ignoreUnknownKeys = true`. ADR-007 pushes the *full record state* and the later edit replaces the record whole.
- *Failure:* An old app pulls a record that has the new field. The old app drops the field on deserialization. The user edits the record and pushes it. The server stores it whole, so the new field is **silently erased for everyone**.
- *Fix:* Either treat any new field on a synced entity as a protocol-version bump, or make clients preserve unknown fields (keep the raw JSON extras and write them back), or have Core merge unknown fields from the stored record.

**C2 — "New enum value is compatible" (ADR-004 §6) vs. `MintroJson` configuration**
`ignoreUnknownKeys` covers unknown *keys*, not unknown enum *values*. With the listed `Json { … }`, an old client decoding a new `SCREAMING_SNAKE_CASE` value throws. ADR-004 gives the `Unknown` fallback only for sealed hierarchies.
- *Fix:* Set `coerceInputValues = true` with an explicit default or `UNKNOWN` member on every wire enum, or declare enum additions breaking. Add a contract test for it.

**C3 — Unknown-record handling (ADR-004) vs. pull algorithm (ADR-007)**
ADR-004 says the client must not advance `lastPulledVersion` past an unknown record. ADR-007 pulls all pages, applies them in one transaction and then advances.
- *Failure:* One unknown record **blocks all later changes**, including known ones, until the user updates the app. The same page is re-fetched on every sync.
- *Fix:* Define the behaviour explicitly: apply known records, remember skipped ones separately, advance to the last contiguous known version, or accept a hard stop and say so (and show the user a blocking "update required" state).

**C4 — Natural keys (ADR-007) vs. renaming and the identity rule**
Core "recomputes the id and rejects mismatches". If the key contains a user-editable name (an expenditure or budget label), then a rename either changes the id (breaking references and history) or is rejected by Core.
- *Fix:* State in `sync-protocol.md` that natural-key components are **immutable** and that "rename" is a separate attribute. Also decide what happens when two devices set the same name on different records. Decide whether *Category* (client-created, UUID v7) should also be naturally keyed, because two phones creating "Food" offline will produce duplicates today.

**C5 — "Telegram Agent is stateless" (ADR-005 context) vs. "its own conversation store" (ADR-005 §8)**
The Agent needs state for per-user rate limiting and conversation context, and also for webhook de-duplication (see R10). No ADR places that store: it is not in the ADR-002 hosting table, not in Terraform scope, and not in the cost table. ADR-005 refers to a "Telegram Agent ADR" that does not exist.
- *Fix:* Write that ADR. Decide the store (Firestore collection in the same database, or nothing), retention and privacy.

**C6 — "Agent talks to Core only through MCP" (ADR-003 context, ADR-001/002) vs. REST endpoints used by the Agent (ADR-005)**
ADR-005 has the Agent call `POST /api/v1/telegram/redemptions` over REST. Consequences:
- The statement in ADR-003 is false.
- The Agent does not consume `:shared:contract`, so this REST hop has **no compile-time contract**, which defeats ADR-004 driver 1 for this path.
- ADR-004's list of resource endpoints does not mention `/api/v1/telegram/*`.
- *Fix:* Either put the redemption call behind an MCP tool that is not LLM-visible, or document the REST endpoint in ADR-004 and add a consumer-side contract test in the Agent against `docs/api/openapi.yaml` and `mcp-tools.json`.

### Minor / wording

- **M1 — Naming rule drift (ADR-001).** The service account examples (`mintro-telegram`, `mintro-scheduler`, `mintro-deploy`) do not follow the stated `mintro-<component>` rule or the Cloud Run name `mintro-telegram-agent`. The package `app.mintro.shared.sync` does not match the module `sync-client`.
- **M2 — Normalization (ADR-007 vs. ADR-003).** ADR-007 says "case-folded". ADR-003 implements `lowercase()`. These differ (for example `ß`, final sigma). Pick one and pin it with test vectors.
- **M3 — Owner invariants (ADR-005).** "An OWNER has a GOOGLE login method" conflicts with owners being able to unlink or remove login methods (own, or an owner removing another's). The enforcement point is not specified.
- **M4 — Where conflict resolution lives.** ADR-003 puts it in Core. ADR-006 lists "conflict resolution" under unit tests in `shared/*`. Fine only if the shared code contains no resolver; clarify.
- **M5 — `:shared:test-fixtures` (ADR-006) vs. Agent not consuming `shared` (ADR-003).** ADR-006 hedges ("or in the owning component") but `LlmClient` fakes and builders will be duplicated.
- **M6 — `expect/actual` (ADR-003).** "Both targets share one implementation" requires an intermediate `jvmAndroid` source set that the default KMP hierarchy does not create. Otherwise the code is duplicated in two `actual`s. *Verify with the Kotlin version in use.*

## 4. Risks (ordered by severity)

### High

**R1 — UUID v5 ids from platform Unicode normalization.** ADR-003 uses `java.text.Normalizer` on both targets. Android's implementation is OS-version dependent (the Unicode version changes with the OS). The same name could normalize differently on different phones, so the "same id, records merge" guarantee silently fails and duplicates appear. Mitigation: golden test vectors for tricky strings, run on several API levels. Or restrict/define normalization in pure Kotlin with a pinned Unicode version. Core rejecting mismatched ids will also turn this into user-visible rejections.

**R2 — Data residency and privacy (EU users).** Everything is in `us-east1` and the LLM is OpenAI. The stated reason is the developer's latency and the free tier, but the cold start (seconds) dwarfs transatlantic latency. Family financial data and masked bank notifications of presumably EU users go to the US and to a third-party LLM provider. This needs a decision (GDPR basis, privacy policy, masking guarantees, possibly an EU region or a paid-region trade-off). No ADR covers it, and it affects the Play listing too.

**R3 — Agents can weaken the gates.** The ADRs protect `shared/*/build.gradle.kts`, `docs/api/*` and tests, but not the files that define the checks: `.github/workflows/`, `build-logic/`, detekt/ktlint configuration, `CODEOWNERS`, `CLAUDE.md`. A PR that edits the workflow or convention plugin can change what "green" means. Mitigation: CODEOWNERS plus required human review on those paths; workflows for PRs loaded from the base branch where possible. Also restrict the GCP Workload Identity Federation trust: PR workflows get a read-only plan identity; only `main`/tags can deploy or apply. ADR-002 does not say how WIF is scoped.

**R4 — The riskiest client code is outside the headless e2e.** Room transactions, the `isDirty`/`localRev` race guard, deferred foreign keys on pull, WorkManager uniqueness, and the Ktor `SyncApi` are Android-only and use in-memory ports in e2e. Mitigation: a shared abstract `LocalStore` contract test suite run against the in-memory fake and against Room (instrumented or Robolectric), plus at least one e2e through the real HTTP client.

**R5 — Image deletion after deploy.** The assumption that Cloud Run no longer needs the registry copy should be checked against current docs (*verify*). Even if true, the saving is pennies (0.5 GB free, then $0.10/GB-month, and Jib layers deduplicate), while the cost is slow, rebuild-based production rollback during an incident. Keeping the last 2–3 images is cheap insurance and fits "near zero".

**R6 — Play Store policy for SMS / notification capture.** The product captures bank SMS and push notifications. Google Play restricts SMS permissions heavily (default-handler or narrow exceptions) and treats notification access as sensitive. This is a possible product blocker for the Play release, and ADR-001 already plans a Play release. It deserves its own ADR/spike before the parsing and capture work is built.

### Medium

**R7 — Equal `updatedAt` ("later arrival wins") breaks convergence under retries.** A lost response makes device A retry. If B's equal-timestamp push arrived in between, A's retry now overwrites B, and the state flip-flops. Make ties deterministic (compare content hash or device id) and treat an identical re-push as a no-op without a new `version`.

**R8 — Clock handling.** Future timestamps are clamped to Core's time, but the device keeps its own `updatedAt`, so the device and server disagree on the stored value. A device that is behind loses to older real edits. Return the stored `updatedAt` on accept, and consider a per-device skew estimate. Accepted for family use, but write the limits down.

**R9 — Parent deletion semantics.** A child created offline under a parent that another device deleted is rejected, and the device then applies the deletion. The user's spending is silently lost. The ADR does not say whether deleting a parent cascades to existing children, nor that rejected user-entered data is surfaced to the user (a "conflict inbox" or re-parenting) rather than discarded. Spendings are the product's core asset; decide this explicitly.

**R10 — Telegram webhook retries and cold starts.** Two JVM cold starts plus an LLM loop can exceed Telegram's patience; Telegram redelivers; the same message is processed twice. ADR-004 gives write tools an `idempotencyKey`, but nobody says who generates it. If the LLM does, a redelivery gets a new key and a duplicate spending. Derive keys in agent code from `update_id` and call index, and de-duplicate by `update_id`. Also note that with request-based billing, work after the HTTP response is CPU-throttled, so the loop must finish inside the request.

**R11 — Firestore free-tier consumption is dominated by reads you have not sized.**
- Derived values are "full aggregation, never incremented", and Core computes its own. A "how much did I spend this month" tool call can read hundreds of documents, and an LLM loop can repeat it.
- The nightly JSON export reads every family document each night.
- Member merge touches every affected record, and Firestore limits a batch to 500 writes, so it cannot be atomic as described and could burn the 20k writes/day.
- Core's per-family LLM rate limits need storage (per-instance memory is useless with scale-to-zero and 2 instances).
- The capacity estimate lives in `docs/architecture.md`, which does not exist yet. Write it before the design is frozen.

**R12 — Family counter as a hot document and transaction scope.** Every accepted change increments one document. That is fine at this scale, but the ADR does not state that record write + counter increment happen in one transaction, and it is required for gap-free versions that pull relies on. State it, and state the batch size and contention behaviour for large pushes (first sync from legacy, long offline period).

**R13 — Invite by Google email.** Email invites have no code, but `invites/` is keyed by `codeHash` and there is no system-wide index by normalized email for sign-in. Gmail dot and plus aliases and Workspace aliases may not match what the owner typed. Specify the lookup index and the normalization.

**R14 — Backups.** A JSON export without a consistent snapshot, RPO 24 h, and no documented or tested restore procedure. A backup that has never been restored is a hope. Add a restore tool or script to scope, and a restore drill.

**R15 — Prod artifact is not the tested artifact.** Images are rebuilt at the tag and the dev image is deleted, so there is no promotion of a verified digest. Jib reproducibility helps but does not remove the gap.

**R16 — Library maturity and version churn.** *Verify:* MCP Kotlin SDK stateless Streamable-HTTP server support, KMP `androidTarget()` versus the newer Android KMP library plugin in current AGP/KGP, and Kotest on the Android KMP target. A single developer plus agents has little spare capacity for tooling migrations. Pin versions and spike these early.

### Low

- **R17 — "Four production modules may be premature"** (ADR-003, acknowledged). Merging is easy later; this is the correct trade-off.
- **R18 — Adding an entity is not as cheap as ADR-007 implies.** Domain, DTO, two mappers (client and Core), Room entity, Firestore mapping, `sync-protocol.md` row, index, OpenAPI regeneration. It is mechanical, so it suits agents, but it is a lot of files. Consider generating or testing the mappers with a shared round-trip property test.
- **R19 — Defaults are part of the wire contract.** `encodeDefaults = false` means a changed default silently changes the meaning of old data. `explicitNulls = false` makes it impossible to distinguish "clear this field" from "absent" in REST `PATCH`.
- **R20 — 10-minute budget for stages 1–2** includes KMP builds for two targets, Testcontainers Firestore and e2e. Measure early; the "exceeding is a defect" rule may fire from the start.
- **R21 — FCM sending from request handlers.** Sending after the response is throttled; sending before adds latency and a Firestore read of the device list on every change. Coalesce to one signal per push request.

## 5. Missing decisions (no ADR yet)

Ordered by how much they can change earlier decisions:

1. **Legacy migration.** The brief is a *rewrite* of an existing product, but no ADR covers moving users and data, the legacy Firestore schema versus the new layout, ID mapping, or cut-over. ADR-001 mentions only a read-only legacy reference.
2. **Privacy, data residency and Play compliance** (R2, R6): SMS/notification capture, masking implementation and guarantees, retention, privacy policy.
3. **AI-development workflow.** The premise is extensive use of AI agents, but the ADRs cover this only indirectly: who approves merges, what agents can touch (R3), what credentials agents and the farm hold, how prompt injection through repository content or notification text is contained, and when a human must review. At minimum, one ADR on agent boundaries and review policy.
4. **Telegram Agent design** (C5, C6, R10): conversation store, retention, rate limits, idempotency, confirmation flows.
5. **Core internals and Android app architecture:** sub-modules are deferred to "their own ADRs"; none exist.
6. **Operations:** restore drills, incident rollback, alerting beyond sustained 5xx (for example cost/LLM spend), on-call reality for a single developer.
7. **Living docs referenced but missing:** `docs/spec/domain.md`, `docs/spec/sync-protocol.md`, `docs/architecture.md`. The compliance rules of ADR-004 and ADR-007 depend on them. Write them before implementation.

## 6. Process observations

- All seven ADRs are *Accepted* within three days, before the domain and sync specs exist. Consider marking the parts that depend on missing documents as *Proposed*, or run a short spike (sync + one KMP module + one deploy) to validate the riskiest assumptions: R1, R4, R5, R16.
- Several ADRs mix architecture with implementation detail (Jib, Koin, Dependabot grouping, exact quota numbers). These will change often and cause ADR churn. Move the volatile detail to a "tech radar" or README and keep ADRs for decisions and their reasons.
- ADRs state free-tier numbers with "verify". Good. Put a dated check in the repository (budget alert is not enough, because it fires after the money is spent).

## 7. Recommended actions (priority order)

1. Amend ADR-004 and ADR-007 for C1–C3 (unknown fields, enum fallback, pull behaviour with unknown records). These affect the wire format and are cheapest to fix now.
2. Decide R1/M2 (normalization) and C4 (immutable natural keys) in `sync-protocol.md`, with test vectors.
3. Write the Telegram Agent ADR (C5, C6, R10) and fix the ADR-003 "only MCP" statement.
4. Add CODEOWNERS / protected paths for CI, build-logic and lint configuration; scope WIF per branch/job (R3).
5. Spike the Play Store SMS/notification question and the EU residency question before building parsing and capture (R2, R6).
6. Add a legacy-migration ADR.
7. Add a `LocalStore` contract test suite against Room (R4); define tie-breaking and idempotent re-push (R7); state parent-delete behaviour and surface lost user input (R9).
8. Reconsider image retention (R5) and write `docs/architecture.md` with read/write sizing (R11).
9. Fix the minor naming/wording items (M1–M6).
