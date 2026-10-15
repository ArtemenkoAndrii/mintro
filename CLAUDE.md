# CLAUDE.md

## Project
Mintro rewrites a single-user Android family budget app as a multi-device product:
an Android app, a central backend service and a Telegram agent, with offline-first
sync between family members' phones.

## Current phase and scope
- Phase: **legacy analysis only**. No product code exists or may be written.
- Goal: document what the legacy app does today, as input for the rewrite.
- In scope: reading `android/legacy/`, writing analysis files in `docs/legacy/`.
- In scope: the legacy behaviour dataset in `evals/legacy/` (examples extracted from the legacy tests).
- Out of scope: design, implementation, build setup, ADR edits, refactoring suggestions.

## Repository map
- `android/legacy/` — legacy app source (`build.gradle`, `src/`, `schemas/`). Read-only.
- `docs/legacy/` — analysis outputs. The only place you write.
- `docs/adr/`, `docs/review.md` — drafts for a later phase. Do not read them
  during legacy analysis: describe the legacy app on its own terms.
- `core/`, `shared/`, `telegram-agent/`, `mcp/`, `infra/`, `build-logic/`, `gradle/`,
  `evals/` — placeholders for later phases. Ignore them.

## Rules
1. Never modify, move, delete or format anything under `android/legacy/`, by any
   means (editor, shell, scripts, git).
2. Write only under `docs/legacy/` unless I explicitly ask for another location.
3. Cite every factual claim about legacy behaviour as `path:line` or
   `path:start-end`, with paths relative to the repo root. If you can't cite a
   claim, don't state it — add it to the "Open questions" section of that file.
4. Describe behaviour as implemented, bugs included. No fixes, improvements or
   redesign. Put odd or suspicious behaviour in `docs/legacy/surprises.md`,
   stated as fact.
5. Never copy secrets, API keys, tokens, signing data, Firebase config values
   (`google-services.json`, project IDs, etc.) or real personal data. Refer to
   them by location only, e.g. "Firebase API key at `path:line`".
6. Don't create build files, Gradle modules, source code or scaffolding in the
   placeholder folders (or anywhere else) during this phase.
7. Ask before doing anything outside the current phase, including editing ADRs,
   touching `.claude/`, `.gitignore` or root files, and committing.
8. Update existing files instead of creating new ones; create only the files
   listed in `docs/legacy/README.md` unless I approve another.
9. Ignore generated and build output (`build/`, `.gradle/`, generated sources).
   `android/legacy/schemas/` (Room schema exports) is a valid source.

## Output conventions
- English, Markdown. One topic per file, using the planned file set.
- Templates, citation format and the planned file list are in
  `docs/legacy/README.md`. Follow it and don't duplicate it elsewhere.
- Use domain terms exactly as named in the legacy code (Spending, Expenditure,
  Budget, Category). Add any new term to `docs/legacy/glossary.md` before using it.
- If the code is ambiguous, write down both readings with citations instead of
  picking one.

## Pointers
- Analysis guide and templates: `docs/legacy/README.md`
- Permission guardrails: `.claude/settings.json`
