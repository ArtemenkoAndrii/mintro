# ADR-002: Tech stack and hosting

- **Status:** Accepted
- **Date:** 2026-10-02
- **Author:** Andrii Artemenko
- **Related ADRs:** ADR-001 (monorepo and naming), ADR-003 (Kotlin Multiplatform for `shared`), ADR-004 (code-first contracts), ADR-005 (authentication), ADR-006 (testing), ADR-007 (sync)

## Context

Mintro has three runtime components (ADR-001):

- **Mintro Core** — domain, identity, authorization; REST for the Android App, MCP for the Telegram Agent, internal endpoints for scheduled jobs;
- **Telegram Agent** — receives Telegram webhooks, runs an LLM agent loop, the only MCP client of Core;
- **Android App** — offline-first client with a local database.

Constraints:

- a single developer; most code is written by AI agents;
- traffic of a few families; GCP cost must stay at or near zero;
- no servers to maintain;
- infrastructure reproducible from the repository.

This ADR decides what each component is built with and where it runs. Authentication, contract formats, and the structure of shared modules are decided in their own ADRs.

## Decision drivers

1. One language across the product.
2. GCP free tier at family-scale traffic.
3. Scale to zero; no always-on resources.
4. All infrastructure as code.
5. Mainstream technologies that both the developer and AI agents know well.

## Decision

### Languages and build

- **Kotlin** for all components; **Gradle Kotlin DSL**, version catalog, convention plugins (ADR-001).
- One JVM version for Core and the Telegram Agent, defined in a convention plugin.

### Mintro Core

| Concern | Choice |
|---|---|
| HTTP server | Ktor |
| Serialization | kotlinx-serialization |
| Concurrency | Kotlin coroutines |
| DI | Koin |
| MCP | MCP Kotlin SDK, Streamable HTTP transport, stateless mode |
| Firebase | Firebase Admin SDK (ID token verification, FCM sending) |
| Service identity | google-auth-library (OIDC verification) |
| Storage | Firestore client behind repository interfaces |
| Container image | Jib (no Dockerfile, reproducible builds) |

### Telegram Agent

| Concern | Choice |
|---|---|
| HTTP server and client | Ktor |
| DI | Koin |
| Telegram | Bot API via webhook (a library or a thin own client) |
| MCP | MCP Kotlin SDK, client side |
| LLM | OpenAI GPT via REST, behind an `LlmClient` interface |
| Service identity | google-auth-library (OIDC tokens from the metadata server) |
| Container image | Jib |

### Android App

| Concern | Choice |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose with type-safe routes |
| DI | Koin |
| Local database | Room |
| Background work | WorkManager |
| Sign-in | Credential Manager + Firebase Authentication |
| Push | Firebase Cloud Messaging |
| Networking | Ktor client |

### AI / LLM

- **Provider:** OpenAI GPT, used by the Telegram Agent (agent loop) and by Core (fallback parsing of bank notifications).
- **Abstraction:** an `LlmClient` interface in each consumer; switching providers changes one implementation.
- **Keys:** stored in Secret Manager; never shipped to the Android App. The app sends masked notification text to Core, which calls the LLM.

### Hosting topology

All resources live in **`us-east1`**: the closest free-tier region to the developer in Spain and to Telegram's European servers.

| Purpose | GCP service | Notes |
|---|---|---|
| Mintro Core | Cloud Run service `mintro-core` | Public; authentication in the application |
| Telegram Agent | Cloud Run service `mintro-telegram-agent` | Public webhook; verifies Telegram's secret header |
| Application data | Firestore, Standard edition, regional | Single `(default)` database per project |
| Sign-in | Identity Platform (Firebase Authentication) | Google provider |
| Push to Android | Firebase Cloud Messaging | Sync signal only |
| Secrets | Secret Manager | Telegram bot token, webhook secret, LLM API key |
| Container images | Artifact Registry | Transit storage only (see Deployment) |
| Scheduled jobs | Cloud Scheduler | Calls Core's `/internal/*` with OIDC |
| Terraform state, backups | Cloud Storage | Not used for application data |

### Data storage

- **Firestore** in regional mode, co-located with Cloud Run. Chosen because the legacy app already uses it and the developer has experience with it.
- Firestore is accessed only through repository interfaces in Core; the domain does not depend on it.
- Data layout, the family change counter, and the deletion log are defined in ADR-007; system-wide identity collections in ADR-005.
- **Backups:** a nightly Cloud Scheduler job exports family data as JSON to Cloud Storage. Built-in Firestore backups and point-in-time recovery are paid and not used.
- **Cleanup** of expired invites and link codes runs as a scheduled job; Firestore TTL is paid and not used.
- Raw captured notifications never reach Core storage; they stay on the device (ADR-007).

### Notifications

- After accepting a change, Core sends an FCM **data message** ("family version N available") to the family's other registered devices (ADR-005). The device then runs its sync job (ADR-007).
- WebSocket/SSE are not used: a persistent connection keeps a Cloud Run instance active, defeating scale-to-zero and consuming the free quota.
- Delivery is not guaranteed; the app also syncs on start and periodically via WorkManager.

### Cloud Run configuration

- `min_instances = 0`; `max_instances = 2` per service to cap the cost of any bug.
- Request-based billing (CPU allocated only during requests).
- The Telegram Agent uses webhooks, never long polling.
- JVM cold starts (seconds) are accepted. If they become a problem: smaller images (distroless, `jlink`), then GraalVM native image.

### Infrastructure as code

```
infra/
├── bootstrap/     applied once with local state: state bucket, Workload Identity Federation
├── modules/       cloud-run-service, firestore, secrets, scheduler, ...
└── envs/
    ├── dev/
    └── prod/
```

- **Terraform**, state in a versioned GCS bucket. State is never committed to git (it can contain sensitive values and needs locking).
- **Managed by Terraform:** APIs, service accounts and IAM roles, Cloud Run services, Firestore database and indexes, secret resources (not their values), Artifact Registry with cleanup policy, Cloud Scheduler jobs, budgets.
- **Not managed by Terraform:** secret values (set out of band so they never enter state); Android OAuth clients with SHA-1 fingerprints, if the provider does not cover them.
- Cloud Run services are created with a placeholder image. Afterwards CI deploys images; the image field is in `lifecycle { ignore_changes }` so Terraform never rolls back a deployment.

### Environments

- Two GCP projects: **`mintro-dev`** and **`mintro-prod`**, each with its own Firestore database, secrets, and service accounts.
- Free-tier quotas of Cloud Run and Artifact Registry are shared per billing account; at family scale both environments fit.

### Deployment

1. **PR:** the checks defined in ADR-006 (build, tests, static analysis, `terraform plan`). Evals are not part of CI.
2. **Merge to `main`:** Jib builds images → pushed to Artifact Registry → `gcloud run deploy` to `dev` → smoke test → **image deleted** from Artifact Registry.
3. **Release to `prod`:** on a git tag, with manual approval in GitHub Environments; same build-push-deploy-delete sequence.
4. GitHub Actions authenticates to GCP via **Workload Identity Federation**; no service account keys.

**Artifact Registry is transit storage only.** Cloud Run imports the image at deploy time and keeps its own copy while a revision serves traffic, so the registry copy is not needed afterwards. A cleanup policy deleting anything older than one day is a safety net.

**Rollback = rebuild from the previous git tag and redeploy.** Older non-serving revisions may not start once their image is deleted, so revision-based rollback is not relied upon.

### Cost guardrails

| Service | Free tier (verify on the Free Tier page) | Main risk | Guardrail |
|---|---|---|---|
| Cloud Run | 180,000 vCPU-s, 360,000 GiB-s, 2M requests per month per billing account | Instances kept alive by open connections | No WebSocket/long polling; `max_instances` |
| Firestore | 1 GiB, 50k reads, 20k writes, 20k deletes per day | Read-heavy queries | Version-based pull; per-request member lookup cached briefly |
| Artifact Registry | 0.5 GB, then $0.10/GB-month | Accumulating images | Delete after deploy; cleanup policy |
| Cloud Storage | 5 GB-month in US regions | Unbounded exports | Lifecycle rule deleting old exports |
| Secret Manager | Small number of versions and accesses | Reading secrets per request | Secrets injected as env vars at instance start |
| LLM API | Not GCP; paid per token | Agent loops, abuse | Spending limit on the API key; rate limits per family in Core and per Telegram user in the Agent (ADR-005) |

- A **budget with alerts at $1 and $5** on the billing account. Budgets only alert; they do not stop spending.
- Container vulnerability scanning stays disabled (billed separately).

### Observability

- Cloud Logging with structured logs; never log tokens, request bodies, or notification texts.
- Cloud Run built-in metrics; an alert on sustained 5xx rate.

## Considered alternatives

| Area | Alternative | Rejected because |
|---|---|---|
| Compute | Compute Engine e2-micro (always free) | Always-on server to patch, secure, and back up |
| Compute | App Engine Standard | Older platform; Cloud Run is Google's primary serverless path |
| Compute | GKE Autopilot | Excessive for the scale; pods are not free |
| Storage | Cloud SQL | No free tier |
| Storage | PostgreSQL on e2-micro | Self-managed database |
| Storage | Neon (serverless PostgreSQL) | External provider; no existing experience; Firestore is already used by the legacy app |
| Realtime | WebSocket / SSE | Prevents scale-to-zero, consumes free quota |
| Realtime | Polling only | Delayed updates, battery cost |
| Region | `us-west1`, `us-central1` | Higher latency from Spain; same free tier |
| DI | Hilt (Dagger) | Android only; Core and the Telegram Agent would need a different approach |
| Local DB | SQLDelight | Less familiar; Room is the Android standard and supports KMP if ever needed |
| IaC | gcloud scripts | Not declarative; no plan/diff; drift is invisible |
| Images | Keep N versions for revision rollback | Exceeds the free storage tier with JVM images; rebuild from git is sufficient |
| Images | Public registry (Docker Hub) | Images and code become public |

## Consequences

### Positive

- One language and one DI approach across all components.
- Expected GCP bill at family scale: zero; the only real cost is LLM usage.
- No servers to maintain; infrastructure is reviewable as code in PRs.
- Storage is behind interfaces; replacing Firestore later means replacing one Core module.

### Negative and risks

- **Cold starts:** the first Telegram message after idle may wait for two cold starts (Agent, then Core). Accepted; mitigations listed above.
- **Firestore model constraints:** no SQL joins or ad-hoc aggregation; queries and indexes must be designed up front.
- **Slower rollback:** rebuild and redeploy instead of switching revisions.
- **Google dependency:** FCM requires Google Play Services; devices without them rely on periodic sync.
- **Free-tier terms can change.** Quotas are reviewed when the budget alert fires or once a year.

## Compliance

- `terraform plan` runs on every PR touching `infra/`; `apply` runs only from CI after merge.
- CI fails if any `*.tfstate` file or secret value is committed.
- Cloud Run services declare `min_instances = 0` and a `max_instances` limit; enforced by a check on the Terraform module inputs.
- The deploy pipeline deletes the pushed image after a successful deploy; the Artifact Registry cleanup policy is present in Terraform.
- No component outside Core and the Telegram Agent holds an LLM API key; the Android App build is checked for embedded secrets.
- Revisit when free-tier usage exceeds 50% of any quota, when cold starts affect usability, or when relational queries become a recurring need.
