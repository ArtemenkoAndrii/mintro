# ADR-006: Testing strategy and quality gates

- **Status:** Accepted
- **Date:** 2026-10-02
- **Author:** Andrii Artemenko
- **Related ADRs:** ADR-001 (monorepo and naming), ADR-002 (tech stack and hosting), ADR-003 (Kotlin Multiplatform for `shared`), ADR-004 (code-first contracts), ADR-005 (authentication), ADR-007 (sync)

## Context

Most Mintro code is written by AI agents: first through Claude Code, later through a custom farm. Neither a human nor a farm can review every line, so tests, evals, and static analysis act as the **oracle**: the automatic answer to "is this change correct?".

Specifics of the system:

- domain and parsers in KMP modules (`commonTest` runs on JVM and Android);
- an offline-first sync protocol between phones and Core;
- Firestore as storage, accessed through repository interfaces;
- LLM-dependent behaviour in Core (fallback parsing) and the Telegram Agent (agent loop);
- infrastructure in Terraform.

## Decision drivers

1. Fast, reliable feedback that agents can act on inside their working loop.
2. Tests check behaviour, not implementation details, so refactoring does not break them.
3. Agents cannot make a change "pass" by weakening tests.
4. Non-deterministic LLM behaviour is measured without making CI flaky or expensive.
5. Minimal tooling for a single developer.

## Decision

### 1. Test levels

| Level | Verifies | Location | How |
|---|---|---|---|
| Unit | Domain invariants, budget rules, parsers, conflict resolution, mappers | `commonTest` in `shared/*`; unit tests in Core and the Agent | No I/O; fakes for ports |
| Integration | Firestore repositories, authentication plugins, HTTP routes | Core | Firestore emulator via Testcontainers; Ktor `testApplication` |
| Contract | JSON fixtures for current and previous protocol versions; Problem Details format | `:shared:contract`, Core | Fixtures (ADR-004) |
| End-to-end | Two phones converge through a real Core (scenarios from ADR-007) | `e2e/` (ADR-001) | Two `sync-client` instances with in-memory ports + Core + Firestore emulator |
| MCP | Tools through a real MCP client, including identity headers | Core | MCP SDK client in tests |
| Telegram Agent | Webhook handling, agent loop, tool-call handling, redemptions | Agent | Fake Telegram API, fake LLM, fake Core |
| UI | Key Android screens and flows | Android | Compose UI tests |
| Smoke | Deployed `dev` serves sync, MCP, and health endpoints | After deploy | A few HTTP calls with test credentials |

**Weight of the pyramid:** most behaviour is covered by unit tests and the end-to-end sync test. UI tests cover only critical flows (sign-in, add spending, invite).

### 2. Frameworks and test doubles

- **Kotest** for all test code, including `commonTest` (multiplatform), with **property-based testing** for money, dates, time zones, and month boundaries.
- **Fakes by default.** Ports (`LocalStore`, `SyncApi`, `Clock`, repositories, `LlmClient`) have in-memory fakes in `:shared:test-fixtures` (ADR-003) or in the test fixtures of the owning component.
- **MockK only for external SDKs** that cannot be faked reasonably (Firebase Admin, Telegram client). Mocks of own interfaces are not allowed.

### 3. Test conventions

- One behaviour per test; names describe behaviour (`rejects spending for a missing expenditure`).
- Structure: Given / When / Then.
- **Test data builders** live in `:shared:test-fixtures`; entities are never constructed ad hoc in different ways.
- **Time is controlled:** every time-dependent test uses a fake `Clock`; direct `now()` calls are forbidden outside the `Clock` implementation.
- **Data is synthetic or masked.** Real family data never enters the repository.

### 4. LLM-dependent behaviour: tests and evals

**Deterministic tests** use a fake `LlmClient` returning scripted responses. They verify code around the model: tool-call handling, error handling, retries, confirmation flows. They run in CI like any other test.

**Evals** measure quality with the real model on datasets in `evals/`:

| Eval | Input → expected | Metric |
|---|---|---|
| Fallback parsing | Masked notification → expected spending fields | Per-field accuracy |
| Telegram Agent | User message → expected tool calls and arguments | Exact/partial match rate |

- Run **manually, on demand**, with one command: `./gradlew evals` (optionally `-Peval=parsing|agent`).
- Results are written to `evals/results/` and compared with the committed baseline in `evals/baseline/`.
- Evals are **not** part of CI: they cost money and are non-deterministic.
- **Definition of Done** for changes to prompts, tool descriptions, or the model: evals were run and the result is not below the baseline; the summary is pasted into the PR.
- **Deterministic parsers** are not evals: they must pass their golden dataset at **100%**, as regular tests in CI.

### 5. Coverage

- **Kover** produces a coverage report on every CI run; the summary is shown in the PR.
- **No coverage threshold.** Coverage is a signal for review, not a gate, because thresholds invite meaningless tests.

### 6. Static analysis

| Tool | Scope |
|---|---|
| Kotlin compiler with **`allWarningsAsErrors = true`** | All modules, from day one |
| ktlint | Formatting, all Kotlin code |
| detekt | Code smells, complexity, forbidden calls (for example, direct `now()`) |
| Android Lint | Android App; warnings as errors |
| `terraform fmt`, `terraform validate`, tflint | `infra/` |
| gitleaks | Whole repository, in CI and pre-commit |

All Kotlin checks run through convention plugins and one command: `./gradlew check`.

### 7. Protection against test gaming

- Acceptance tests and fixtures written for a task are **read-only** for the agent implementing it.
- PRs that modify or delete existing tests are flagged; such changes are reviewed separately from the implementation.
- Golden datasets and contract fixtures change only with explicit approval.
- **Mutation testing** (PIT for `:shared:domain` and `:shared:parsing`) is planned as a periodic check once the AI farm is built; not used now.

### 8. CI pipeline

| Stage | Runs on | Contents |
|---|---|---|
| 1. Fast checks | Every PR | `check` for affected modules: compile, ktlint, detekt, unit tests, gitleaks |
| 2. Integration | Every PR touching `shared/`, `core/`, `telegram-agent/` | Integration, contract, MCP, end-to-end tests; drift check for `docs/api/` |
| 3. Android UI | Only PRs touching `android/` | Compose UI tests on an emulator |
| 4. Infrastructure | PRs touching `infra/` | fmt, validate, tflint, `terraform plan` as a PR comment |
| 5. Smoke | After deploy to `dev` | Smoke tests against the deployed services |

- **Affected modules only**, using Gradle's project dependency graph.
- **Time budget:** stages 1–2 finish within 10 minutes; exceeding it is treated as a defect.
- **Flaky tests** are quarantined with an issue to fix them; automatic retries are not used to hide flakiness.

### 9. Definition of Done

A change is done when:

1. `./gradlew check` passes for affected modules, with no new warnings.
2. New behaviour has tests at the appropriate level.
3. `docs/api/` is regenerated if a contract changed (ADR-004).
4. Evals were run if prompts, tool descriptions, or the model changed.
5. An ADR is added or updated if an architectural decision was made.
6. No real personal data, secrets, or tokens are committed.

`CLAUDE.md` references this list verbatim.

### 10. Dependency updates

- **Dependabot** for Gradle (`libs.versions.toml`), GitHub Actions, and Terraform providers.
- Weekly schedule; related updates grouped (for example, all Ktor artifacts, all Firebase artifacts) to limit PR noise.
- Dependabot PRs go through the same CI as any other PR.

## Considered alternatives

| Alternative | Rejected because |
|---|---|
| `kotlin.test` + JUnit 5 only | No built-in property-based testing; Kotest covers both and works in `commonTest` |
| Mocks of own interfaces | Verify calls instead of behaviour; break on refactoring; agents overuse them |
| Evals in CI on every PR | Cost and non-determinism; would make CI flaky |
| Coverage threshold | Invites meaningless tests written to reach a number |
| Mutation testing now | Slow; more valuable once the domain stabilizes |
| UI tests on every PR | Slow and flaky emulators; most PRs do not touch the UI |
| Renovate | More powerful but more configuration; Dependabot is built into GitHub |

## Consequences

### Positive

- Agents get fast, actionable feedback inside their loop.
- Behaviour-focused tests with fakes survive refactoring.
- LLM quality is measured explicitly without destabilizing CI.
- Warnings cannot accumulate.

### Negative and risks

- **Evals depend on discipline:** nothing in CI forces them. Mitigated by the Definition of Done and PR review.
- **No coverage gate:** gaps are visible but not blocked. Mitigated by review of the coverage summary.
- **UI regressions** from non-Android PRs (for example, a contract change) are not caught by UI tests until an Android PR. Mitigated by contract and end-to-end tests.
- **`allWarningsAsErrors`** can block builds after dependency updates introduce deprecations. Accepted: deprecations are fixed in the Dependabot PR.

## Compliance

- Branch protection requires CI stages 1–4 (when applicable) to pass.
- detekt rules forbid direct `now()` and mocks of `app.mintro.*` interfaces.
- CI flags PRs that modify existing test files or files in `evals/` and `shared/contract/src/*Test*/resources`.
- `CLAUDE.md` contains the Definition of Done and the commands `./gradlew check` and `./gradlew evals`.
- Revisit when the AI farm is built: mutation testing, eval automation, and stricter test-change rules.
