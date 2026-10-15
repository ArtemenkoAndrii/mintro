# ADR-001: Monorepo and naming

- **Status:** Accepted
- **Date:** 2026-10-01
- **Author:** Andrii Artemenko
- **Related ADRs:** ADR-002 (tech stack and hosting), ADR-003 (Kotlin Multiplatform for `shared`), ADR-004 (code-first contracts), ADR-005 (authentication), ADR-006 (testing), ADR-007 (sync)

## Context

Mintro is a family expense tracker. Its runtime components share one domain model and one sync protocol:

- **Mintro Core** — the central service: domain, identity, authorization, REST for the Android app, MCP for the Telegram agent, internal endpoints for scheduled jobs;
- **Android App** — offline-first REST client;
- **Telegram Agent** — a separate service that receives Telegram webhooks, runs an LLM agent loop, and is the only MCP client of Core.

Supporting parts: shared Kotlin Multiplatform modules, Terraform for GCP, a golden dataset for parsers, and documentation.

Most code is written by AI agents: first via Claude Code, later via a custom farm built on the Claude Agent SDK. The farm is a separate system with its own language (Python or TypeScript), lifecycle, and potential reuse across projects.

There is a single developer. Names must stay valid if the technology behind a component changes.

## Decision drivers

1. **Atomic contract changes.** A change in `shared` updates every consumer in one PR, verified by one CI run.
2. **Context for agents.** Agents see where an interface is used and can run end-to-end tests without the contract being restated in prompts.
3. **Isolation of parallel work.** Several agents work at once without interfering.
4. **Uniform rules.** One `CLAUDE.md`, one set of standards, one version catalog, one CI pipeline.
5. **Product and tooling separated.** The farm is not part of the product build and does not depend on its stack.
6. **Role-based names.** Components are named by what they do, not by the framework they use; technology choices live only in the ADRs that make them.

## Considered options

| Option | Verdict |
|---|---|
| **1. Product monorepo + separate farm repository** | **Chosen.** Atomic contract changes; agents see the whole product; `shared` is consumed as a project dependency with no publishing; worktrees isolate agents in one history; the farm keeps its own stack. Cost: two repositories, and the farm needs a token to create branches and PRs. |
| 2. One monorepo including the farm | Rejected. Python/TypeScript inside a Gradle build; farm agents could modify the code and checks that control them; harder to reuse. |
| 3. Polyrepo, one repository per module | Rejected. `shared` becomes a versioned library; contract changes span several coordinated PRs with incompatible states in between; agents see only one repository. Independent releases are not needed. |

## Decision

### Repositories

| Repository | Contents |
|---|---|
| `mintro` | Gradle monorepo for the product |
| `mintro-farm` | AI farm orchestrator; created when the farm is built |
| Legacy app repository | Read-only reference; optionally attached to `mintro` as a git submodule under `legacy/` |

### Monorepo layout

```
mintro/
├── shared/
│   ├── domain/          entities, invariants, budget rules
│   ├── parsing/         SMS/push parsers → domain objects
│   ├── contract/        sync DTOs, entity records, protocol version
│   ├── sync-client/     dirty tracking, push/pull engine (ports only)
│   └── test-fixtures/   test data builders and fakes (test-only)
├── core/                Mintro Core: REST, MCP, internal endpoints, storage adapters
├── telegram-agent/      Telegram Agent: webhook, LLM loop, MCP client
├── android/             Android App
├── e2e/                 end-to-end tests: sync-client instances against a real Core
├── evals/               golden datasets and runners
├── infra/               Terraform
├── docs/
│   ├── adr/
│   ├── api/             generated contract documents (ADR-004)
│   ├── spec/            domain, sync protocol, scenarios
│   └── legacy/          specifications extracted from the legacy app
├── legacy/              optional read-only git submodule with the legacy app
├── build-logic/         Gradle convention plugins
├── gradle/libs.versions.toml
└── CLAUDE.md
```

Top-level folders map one-to-one to deployable components (`core`, `telegram-agent`, `android`) or to supporting concerns (`shared`, `e2e`, `evals`, `infra`, `docs`, `legacy`, `build-logic`). Internal sub-modules of `core` and `telegram-agent` are decided in their own ADRs.

### Naming rules

**1. Components are named by role.** Framework names (Ktor, Compose, Firestore) never appear in component, folder, module, service, or package names.

| Component | Name in docs | Folder / Gradle module |
|---|---|---|
| Central service | Mintro Core | `core` → `:core` |
| Telegram service | Telegram Agent | `telegram-agent` → `:telegram-agent` |
| Mobile client | Android App | `android` → `:android` |
| Shared modules | — | `shared/<name>` → `:shared:<name>` |

**2. The `mintro` prefix is used only in global namespaces.** Inside the repository the context is already clear.

| Namespace | Convention | Example |
|---|---|---|
| Folders, Gradle modules | No prefix | `core`, `:shared:domain` |
| Gradle `group` | Base namespace | `app.mintro` |
| Kotlin packages | `<base>.<component>[.<area>]` | `app.mintro.core.sync`, `app.mintro.shared.domain` |
| Android `applicationId` | `<base>` | `app.mintro` |
| Cloud Run services | `mintro-<component>` | `mintro-core`, `mintro-telegram-agent` |
| Service accounts | `mintro-<component>` (30-char limit) | `mintro-core@…`, `mintro-telegram@…`, `mintro-scheduler@…`, `mintro-deploy@…` |
| Container images | `<registry>/mintro/<component>` | `…/mintro/core` |
| Terraform resources | Role-based, no prefix inside modules | `google_cloud_run_v2_service.core` |
| GCP projects | `mintro-<env>` | `mintro-dev`, `mintro-prod` |

The base namespace `app.mintro` must be confirmed before the first Play Store release: `applicationId` cannot change after publishing. If the `mintro.app` domain is not owned, use a namespace under a domain you control.

**3. Other conventions.**

| Item | Convention | Example |
|---|---|---|
| Kebab-case | Folders, services, images, Terraform files | `telegram-agent` |
| ADR files | `ADR-NNN-<kebab-title>.md` in `docs/adr/` | `ADR-001-monorepo-and-naming.md` |
| Specs | `docs/spec/<area>/<kebab-title>.md` | `docs/spec/scenarios/add-spending.md` |
| Branches | `<type>/<kebab-summary>` | `feat/category-sync`, `fix/push-retry` |
| Domain terms | Glossary in `docs/spec/domain.md`; the legacy terms are kept | `Spending` → `Expenditure` → `Budget`, `Category` |
| Environments | `dev`, `prod` | — |

## Consequences

### Positive

- A contract change and all its consumers go through one PR and one CI run.
- Parallel agent work uses `git worktree`, split by top-level module.
- Versions and module configuration are centralized in `libs.versions.toml` and convention plugins.
- Farm agents cannot modify the farm's own code and rules.
- Replacing a framework does not invalidate names in code, infrastructure, or documentation.
- One name per component across docs, folders, services, and IAM makes logs and diagrams easy to correlate.

### Negative and risks

- **Build time grows.** Mitigated by modular builds, configuration cache, build cache in CI, and checks for affected modules only.
- **Conflicts in shared build files** (`libs.versions.toml`, `settings.gradle.kts`) during parallel work. Shared files change only in the contract step, never by workers.
- **Agents stepping outside their module.** Mitigated by path permissions and a diff check in CI / the verifier.
- **Farm access to `mintro`.** The farm token can create branches and PRs but cannot merge; `main` is protected.
- **Generic module names** (`core`) may produce generic artifact names. Mitigated by setting `group = "app.mintro"` in convention plugins.

## Compliance

- Branch protection on `main`: merge only via PR with green CI.
- `CLAUDE.md` describes the layout and naming rules and forbids changes to `legacy/` and shared build files without an explicit task.
- CI checks:
  - no hardcoded dependency coordinates in `*/build.gradle.kts`;
  - Kotlin packages start with the base namespace;
  - no framework names in top-level folders, Gradle module names, or Cloud Run service names.
- Revisit if a second team with its own release cycle appears, or if a component on a different stack with no shared code is added.
