# ADR-003: Kotlin Multiplatform for `shared`

- **Status:** Accepted
- **Date:** 2026-10-02
- **Author:** Andrii Artemenko
- **Related ADRs:** ADR-001 (monorepo and naming), ADR-002 (tech stack and hosting), ADR-004 (code-first contracts), ADR-005 (authentication), ADR-006 (testing), ADR-007 (sync)

## Context

The Android App and Mintro Core share code:

- the domain model, invariants, and budget rules;
- deterministic bank SMS/push parsers, validated against the golden dataset in `evals`;
- the sync contract between the Android App and Core;
- client-side sync logic (dirty tracking, push/pull, applying server state; ADR-007), also used by headless end-to-end tests that simulate two phones against a real Core.

The Telegram Agent does not consume `shared`: it talks to Core only through MCP, whose tool schemas Core publishes at runtime.

Both consumers run on the JVM, so a plain Kotlin/JVM module (`.jar`) would technically work. Two factors make the choice non-trivial:

1. **Most code is written by AI agents.** A JVM module compiles against the full JDK, but Android supports only part of it (for example, `java.net.http.HttpClient`, many `javax.*` packages). Such code compiles, passes Core's tests, and crashes on the device.
2. **The architecture follows Onion principles.** `shared` is a physical grouping, not a layer. It contains several logical layers whose dependency direction must be enforced, not just documented.

## Decision drivers

1. The domain is free of platform and infrastructure APIs, enforced by the compiler.
2. Dependency direction between layers inside `shared` is enforced.
3. The same tests run on every target.
4. Future clients (iOS, web) are possible without rewriting the domain.
5. Build complexity stays acceptable for a single developer.

## Considered options

| Option | Verdict |
|---|---|
| **1. KMP with `androidTarget()` and `jvm()`** | **Chosen.** `commonMain` cannot import `java.*`, so platform leaks are compile errors; `commonTest` runs on both targets; new clients are new targets. Cost: only multiplatform libraries in common code; a more complex, slower build. |
| 2. Plain Kotlin/JVM module | Rejected. No compile-time protection against JDK APIs missing on Android; protection would rely on custom lint rules that are weaker and easy to bypass; no path to non-JVM clients. |
| 3. No shared code; duplicate logic in the Android App and Core | Rejected. Two domain and parser implementations drift apart; the contract is defined twice. |

## Decision

`shared` is a group of KMP modules with targets `androidTarget()` and `jvm()`, split by Onion layer.

### Modules

| Module | Gradle path | Package | Layer | Contents |
|---|---|---|---|---|
| `domain` | `:shared:domain` | `app.mintro.shared.domain` | Domain | Entities, value objects, invariants, budget rules |
| `parsing` | `:shared:parsing` | `app.mintro.shared.parsing` | Domain services | Deterministic bank SMS/push parsers → domain objects |
| `contract` | `:shared:contract` | `app.mintro.shared.contract` | Application boundary | Sync DTOs (entity records with sync metadata), protocol version, error codes |
| `sync-client` | `:shared:sync-client` | `app.mintro.shared.sync` | Application | Dirty tracking, push/pull engine, applying server state; ports `LocalStore`, `SyncApi`, `Clock` |
| `test-fixtures` | `:shared:test-fixtures` | `app.mintro.shared.testing` | Test-only | Test data builders and in-memory fakes of ports (ADR-006); never a production dependency |

### Dependency rules

| Module | May depend on | Must not contain |
|---|---|---|
| `domain` | Kotlin stdlib, `kotlinx-datetime`, Okio (hashing for UUID v5) | Serialization annotations, DTOs, I/O, DI, sync metadata (`updatedAt`, `version`, dirty flags) |
| `parsing` | `domain` | Network calls, LLM clients |
| `contract` | `kotlinx-serialization` | Domain types in its public API |
| `sync-client` | `domain`, `contract`, `kotlinx-coroutines` | HTTP clients, databases, push messaging, DI framework code |

- **Mapping** between DTOs and domain objects happens at the edges: in `sync-client` and in Core. The internal model and the wire format evolve independently.
- **Infrastructure is never part of `shared`.** Port implementations live in the Android App (Room for `LocalStore`, Ktor client for `SyncApi`, FCM handling) and in tests (in-memory).
- **DI** (Koin, ADR-002) is wired in the consuming applications. `shared` modules expose plain constructors and interfaces and do not depend on Koin.
- **Sync metadata placement:** domain entities carry no sync fields. `updatedAt` and `version` exist in `:shared:contract` DTOs and in Core storage; `isDirty` and `localRev` exist only in the Android App's Room entities (ADR-007).
- **Server-side sync logic** (version assignment, conflict resolution by `updatedAt`, deletion log) lives in Core.
- **LLM fallback parsing** lives in Core: the Android App sends masked text to Core, which calls the LLM. API keys never reach the device.

### Consumers

| Module | Android App | Mintro Core | Telegram Agent | Tests |
|---|---|---|---|---|
| `domain` | ✓ | ✓ | — | ✓ |
| `parsing` | ✓ | — | — | ✓ |
| `contract` | ✓ | ✓ | — | ✓ |
| `sync-client` | ✓ | — | — | ✓ |

### Contract format

How contracts in `:shared:contract` are defined, versioned, and documented is decided in ADR-004.

### Libraries for common code

- `kotlinx-datetime` — dates and time zones;
- `kotlinx-serialization` — the contract;
- `kotlinx-coroutines` — asynchronous sync logic;
- `okio` — SHA-1 for UUID v5 identifiers (ADR-007);
- Kotest for `commonTest`, including property-based tests (ADR-006).

### Platform-specific code

The Kotlin common standard library lacks Unicode normalization. Natural-key normalization (ADR-007) uses `expect`/`actual`: both targets run on the JVM and share one implementation based on `java.text.Normalizer` (NFC), followed by locale-invariant `lowercase()` and trimming. No other `expect`/`actual` declarations are planned.

### Build

- One convention plugin, **`mintro.kmp.library`**, configures targets, compiler options, ktlint, detekt, and tests for every `shared` module.
- No `shared` module configures targets or quality tools on its own.

## Consequences

### Positive

- Platform APIs in the domain are compile errors, not runtime crashes.
- Layer boundaries are enforced by Gradle dependencies: `domain` physically cannot import `contract` or `sync-client`.
- One parser implementation, validated by one golden dataset.
- The two-phone end-to-end test runs headless: two `sync-client` instances with in-memory ports against a real Core.
- iOS or web clients can be added as new targets.

### Negative and risks

- **Build complexity and time.** Mitigated by the single convention plugin, configuration cache, and build cache in CI.
- **Library availability.** Some JVM libraries are unusable in common code. Acceptable: the domain needs few libraries; JVM-only code belongs in Core or the Android App.
- **Four production modules may be premature.** Merging modules later is easy; splitting them is hard.
- **Agents adding dependencies to `shared`.** Mitigated by the checks below.

## Compliance

- Every `shared` module applies `mintro.kmp.library`.
- Changes to `shared/*/build.gradle.kts` require explicit approval in review; in the farm, only the contract step may change them.
- `commonTest` passes on both `jvm` and `android` targets in CI.
- CI fails if any `shared` module depends on Koin, Room, Ktor, Firebase, or other infrastructure libraries.
- `CLAUDE.md` states the dependency rules and links to this ADR.
- Revisit if KMP tooling becomes a recurring obstacle, or if the project narrows to a single JVM target with no plans for other clients.
