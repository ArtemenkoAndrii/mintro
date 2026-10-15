# ADR-005: Authentication, authorization, and family membership

- **Status:** Accepted
- **Date:** 2026-10-02
- **Author:** Andrii Artemenko
- **Related ADRs:** ADR-001 (monorepo and naming), ADR-002 (tech stack and hosting), ADR-004 (code-first contracts), ADR-006 (testing), ADR-007 (sync)

## Context

Mintro is multi-tenant: any number of families, each fully isolated. Early on, family creation is capped to stay within the GCP free tier.

- **Mintro Core** (Cloud Run, public) owns identity, membership, and authorization. Endpoints: `/sync/*` and `/api/v1/*` for the Android App, `/mcp` and `/api/v1/telegram/*` for the Telegram Agent, `/internal/*` for scheduled jobs.
- **Android App** signs in with Google and calls Core over HTTP.
- **Telegram Agent** (separate Cloud Run service) receives Telegram webhooks, runs the LLM loop, and is the only MCP client. Stateless: knows nothing about families or members.
- **Cloud Scheduler** calls `/internal/*`.

A person may use both the app and Telegram, under different names in each ("Alice" vs "Alice M. Smith"). Some members may use Telegram only.

## Decision drivers

1. One person = one record, regardless of how many clients they use; one family's data is never visible to another.
2. Every caller resolves to one identity model and one set of authorization rules.
3. No passwords or user secrets stored by Mintro; no secrets for service-to-service calls.
4. Login methods are linked only by proof of control over both accounts.
5. A family never becomes unmanageable because one person lost access.

## Decision

### 1. Identity model

One person = one `Member`. Ways to sign in are separate `LoginMethod`s.

```kotlin
data class Member(
    val id: MemberId,          // UUID v7, Mintro's own id
    val familyId: FamilyId,
    val displayName: String,   // set in the family, not taken from providers
    val role: Role,            // OWNER | MEMBER
    val status: Status,        // INVITED | ACTIVE | REMOVED (domain status, not deletion)
)

data class LoginMethod(
    val memberId: MemberId,
    val provider: Provider,    // GOOGLE | TELEGRAM
    val externalId: String,    // Firebase uid | Telegram from.id
    val label: String?,        // display only: email | @username
)

data class Device(
    val id: DeviceId,
    val memberId: MemberId,
    val fcmToken: String,      // refreshed by the app whenever FCM issues a new one
    val lastSeenAt: Instant,
)
```

- `Member` is server-owned and read-only for clients in sync (ADR-007); sync metadata is not part of the domain model.
- `LoginMethod`, `Device`, invites, and link codes are **server-only**: never synced, managed through REST.
- Devices register and refresh their FCM token through `PUT /api/v1/devices/{id}`; Core sends sync signals only to registered devices of active members.

**Invariants**

- `(provider, externalId)` is unique system-wide.
- An `ACTIVE` member has at least one `LoginMethod`.
- A family has at least one active `OWNER`.
- An `OWNER` has a `GOOGLE` login method (family management is available only in the Android App).
- A member belongs to exactly one family.
- Login methods are never linked by comparing names, emails, or phone numbers.

### 2. Authentication

Every authenticated request resolves to one object, which is all use cases see:

```kotlin
data class Actor(val memberId: MemberId, val familyId: FamilyId, val role: Role)
```

| Caller → endpoints | Credential | Verified by Core | Resolves to |
|---|---|---|---|
| Android App → `/sync/*`, `/api/v1/*` | `Authorization: Bearer <Firebase ID token>` | Signature (cached Google keys), `iss`, `aud`, `exp` | `uid` → `LoginMethod(GOOGLE)` → `Member` |
| Telegram Agent → `/mcp`, `/api/v1/telegram/*` | `Authorization: Bearer <OIDC token of the agent's service account>` + `X-Mintro-Telegram-User: <from.id>` | OIDC signature, `aud` = Core URL, `email` = agent service account; **only then** the header is read | `from.id` → `LoginMethod(TELEGRAM)` → `Member` |
| Cloud Scheduler → `/internal/*` | OIDC token of the scheduler service account | Signature, `aud`, `email` = scheduler service account | System principal, no `Member` |

**Rules**

- **Local verification.** Tokens are JWTs verified locally; no introspection calls.
- **Credentials are not interchangeable.** Firebase tokens are accepted only on `/sync/*` and `/api/v1/*` (except `/api/v1/telegram/*`); the agent's token only on `/mcp` and `/api/v1/telegram/*`; the scheduler's token only on `/internal/*`.
- **The acting user is never an MCP tool argument.** Arguments are produced by the LLM and can be manipulated by prompt injection; the user comes only from the header set by agent code.
- **Member lookup is cached for 60 seconds.** A removed member loses access within one minute, regardless of token lifetime.

### System-wide lookups

Identity data that must be found before the family is known lives in top-level collections, outside `families/{familyId}` (ADR-007):

```
loginMethods/{provider}:{externalId}   memberId, familyId   (document id enforces uniqueness)
invites/{codeHash}                     memberId, familyId, channel, expiresAt
linkCodes/{codeHash}                   memberId, target, expiresAt
system/families                        family count for the family cap
```

**How each caller obtains its credential**

- **Android App:** Firebase Auth SDK with Credential Manager (Google sign-in). The SDK keeps the refresh token and returns a valid ID token on demand. Offline writes need no token; pending changes are pushed once a token is available.
- **Telegram Agent:** OIDC token for its Cloud Run service account from the metadata server (google-auth-library, `audience` = Core URL). No secrets for this.
- **Cloud Scheduler:** OIDC token configured on the job in Terraform.

### 3. Telegram trust chain

| Hop | Proof | Therefore |
|---|---|---|
| Telegram → Agent webhook | `X-Telegram-Bot-Api-Secret-Token` matches the secret set at webhook registration | `from.id` is genuine |
| Agent → Core | OIDC token of the agent service account | `X-Mintro-Telegram-User` is trusted |
| Core | `from.id` → `LoginMethod` → `Member` | `Actor` |

- `from.id` is used, never `username` (mutable); stored as `Long`.
- Only **private chats** are processed; messages from groups and channels are ignored.
- Free text goes through the LLM, which calls MCP tools.
- `/start inv_<code>` and `/start link_<code>` are handled **deterministically by agent code**, not by the LLM, and forwarded to `POST /api/v1/telegram/redemptions`. This endpoint is not exposed as an MCP tool.
- Unlinked `from.id`: tools return an error; the agent replies with instructions to link Telegram or ask the owner for an invite.

### 4. Authorization

- **`familyId` always comes from `Actor`, never from the request.**
- Every repository query is scoped by `familyId` at the storage layer.
- Authorship (`createdBy`) is set by Core from `Actor`.
- Resources of another family return `404` (ADR-004), not revealing their existence.

| Capability | OWNER | MEMBER | Telegram-only MEMBER |
|---|---|---|---|
| Spendings, expenditures, budgets, categories | ✓ | ✓ | ✓ (via the agent) |
| Own login methods (link/unlink) | ✓ | ✓ | ✓ (link via `/link_app`) |
| Leave the family | ✓ (if another owner exists) | ✓ | ✓ |
| Invite, remove, change roles, merge members | ✓ (Android App) | — | — |
| Transfer ownership, delete the family | ✓ (Android App) | — | — |

### 5. Membership flows

**Create a family.** Only in the Android App: sign in with Google → create family → become `OWNER`. Subject to the family cap.

**Family cap.** Maximum number of families and an optional allowlist of Google emails, set as Cloud Run environment variables through Terraform. When reached, the app shows that registration is closed. Joining by invite is never blocked.

**Add a member (owner).** Create `Member(displayName, role, status = INVITED)`, then invite through one channel:

| | Android invite | Telegram invite |
|---|---|---|
| Owner provides | The member's Google email | Nothing extra |
| Delivery | Owner tells the person to install Mintro; **no email is sent** | Android share sheet with `https://t.me/<bot>?start=inv_<code>` |
| Redeemed when | Google sign-in with `email_verified = true` and a case-insensitive email match | Person presses Start and confirms "Join family Artemenko as Alice?" |
| Result | `LoginMethod(GOOGLE, uid)`, status `ACTIVE` | `LoginMethod(TELEGRAM, from.id)`, status `ACTIVE` |

A Google sign-in that matches no invite shows "No invitation for this account" with options to switch account or enter a code — never a silent "create family".

**Link a second login method (self-service).** Proof of control over both accounts within a short window:

- App → Telegram: "Link Telegram" → `t.me/<bot>?start=link_<code>` → agent asks for confirmation.
- Telegram → App: `/link_app` → code → entered in the app after Google sign-in.

| | Invite (`inv_`) | Link code (`link_`) |
|---|---|---|
| Effect | Activates an `INVITED` member | Adds a login method to an `ACTIVE` member |
| Lifetime | A few days | About 10 minutes |
| Use | Single | Single |
| Storage | Hash only | Hash only |

Codes use at most 64 characters from `A–Z a–z 0–9 _ -` (Telegram `start` limit). Owners see how each member joined (`@alice_k`), can revoke invites, remove login methods, and merge duplicate members (spendings and login methods move to the survivor).

**Transfer ownership.** An owner promotes another member with a `GOOGLE` login method to `OWNER`; afterwards the original owner may demote themselves or leave. Role changes run in a transaction that enforces "at least one active owner", so two owners cannot demote each other at the same time.

**Merge members.** Reassigning authorship of the duplicate's records is a server-side write: each affected record gets Core's current time as `updatedAt` and a new `version`, so the change reaches every device and wins over older edits (ADR-007).

### 6. Leaving, sign-out, and deletion

**Sign-out on a device.** The app first tries to push pending changes. If that fails, it warns that unsent changes will be lost. Then it clears the local database.

**Leave the family or removal by an owner.** The member's login methods and devices are deleted, status becomes `REMOVED`; their spendings remain, shown with their display name. The last owner must transfer ownership first.

**Devices of a removed member or a deleted family.** Core rejects their requests with `403` (`MEMBER_REMOVED`, `FAMILY_DELETED`) and sends them no sync signals. Data already on the device is not wiped remotely: the device may never come online again, and that is acceptable.

**Delete the family (owner).** Deletes all family data in Core: members, login methods, devices, invites, all synced entities, the deletion log, and exports in Cloud Storage. Irreversible, with explicit confirmation. Raw captured notifications exist only on devices (ADR-007).

**Deletion without the app.** Required by Google Play for apps with account creation: a web page linked from the privacy policy explains how to request deletion (email or simple form); requests are fulfilled with the same operations as above.

### 7. Security

- Stored codes and tokens are hashed; comparisons are constant-time.
- Removing a member revokes all their login methods and pending codes.
- Owners are encouraged to enable Telegram two-step verification.
- Logs never contain tokens, codes, or message texts.
- Least-privilege IAM: the agent's service account can only invoke Core; the scheduler's only `/internal/*` jobs.

### 8. LLM usage limits

- **Core** limits its own LLM calls (fallback parsing) per family.
- **The Telegram Agent** limits LLM usage per Telegram user, using its own conversation store (details in the Telegram Agent ADR). It does not need family knowledge for this.

### 9. Out of scope

- External MCP clients (Claude Code, Claude Desktop, the AI farm) — would require personal access tokens or OAuth; separate ADR.
- Firebase App Check.
- Telegram Mini App, family creation from the bot, group chats, membership in multiple families.

## Considered alternatives

| Alternative | Rejected because |
|---|---|
| Own email/password accounts | Password storage and resets for no benefit |
| Verify Google ID tokens directly, without Firebase | Manual token refresh on the client |
| Firebase uid as the universal identity (custom tokens for Telegram) | Extra machinery; couples the domain to Firebase |
| Agent signs its own per-user delegation JWT | Agent would need a signing key and member knowledge; OIDC + header keeps it stateless |
| Opaque tokens with introspection | A network call per request; local JWT verification suffices |
| Matching Telegram and Google accounts by name or email | Names differ between providers; risk of linking the wrong people |
| Sending invite emails | Requires an email provider; owner can tell the person directly |

## Consequences

### Positive

- One record per person across clients; Telegram-only members are possible.
- No passwords, no email service, no secrets for service-to-service calls.
- One authorization model (`Actor`) for REST and MCP.
- Play Store account-deletion requirements are met.

### Negative and risks

- Invites and link codes are bearer secrets → single use, expiry, confirmation, owner visibility.
- A compromised agent service account could act as any linked Telegram user → least-privilege IAM, deploys only through CI.
- Revocation is delayed by the 60-second member cache → acceptable for a family app.
- Telegram-only security equals Telegram account security → encourage two-step verification.
- Google sign-in requires SHA-1 fingerprints of debug, release, and Play App Signing keys → documented in the setup guide.
- A removed member keeps the data already on their device → accepted; the data was visible to them before removal.

## Compliance

- Tests on every entry point (REST, sync, MCP, Telegram redemptions): a member of family A cannot read or modify family B's data.
- Tests for credential separation: each credential type is rejected outside its endpoints; `X-Mintro-Telegram-User` is ignored without a valid agent token.
- Tests for invariants and flows: unique `(provider, externalId)`, owner protection including concurrent demotion, invite and link code expiry and single use, wrong Google account, merge, leave, family deletion, requests from removed members.
- Ktor authentication plugins per route group; no route reaches a use case without an `Actor` or a verified system principal.
- `CLAUDE.md`: never read `familyId` or the acting user from request bodies or tool arguments.
