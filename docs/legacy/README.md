# Legacy analysis

This folder documents what the legacy Android app in `android/legacy/` does
**as implemented**, bugs included. It is the input for the Mintro rewrite.
It describes behaviour only. It does not propose fixes or designs.

## Citation rules

- Every factual claim about legacy behaviour has a citation.
- Format: `path:line` or `path:start-end`, with paths relative to the repo root,
  e.g. `android/legacy/src/main/java/.../SpendingDao.kt:42-58`.
- Use one citation per claim. If the behaviour spans several files, cite each one.
- Cite source files, not generated or build output.
- If you can't cite a statement, prefix it with **Assumption:** or leave it out.
- Never quote secrets, API keys, Firebase config values or personal data.
  Cite their location only.

## Use-case template

Each area file `use-cases/<area>.md` has one block per use case. `<area>` is the
area number from `map.md`.

```markdown
## UC-L<area>.<n>: <short name>
Actor:          user | system
Trigger:        what starts it (screen action, event, schedule) — `path:line`
Preconditions:  state that must hold before it runs — `path:line`
Main flow:      1. Step — `path:line`
                2. Step — `path:start-end`
Rules:          invariants, validations, calculations (with formulas) — `path:line`
Edge cases:     what happens at boundaries and on errors — `path:line`
Data touched:   entities and fields read/written (see `data-model.md`)
Source:         main `path:start-end` references
```

Every area file ends with two sections:

- `## Open questions`: things the code does not answer, and claims that could
  not be cited.
- `## Surprises`: behaviour that looks unintended or inconsistent, stated as fact
  with a citation. These are later collected in `surprises.md`.

If code and comments or names disagree, describe what the code does and note the
mismatch.

## Planned output files

Create only these files unless a new one is approved.

| File | Contents |
|---|---|
| `map.md` | Structure of the legacy code: modules, packages, screens, entry points, and what each one is responsible for. |
| `use-cases/<area>.md` | Use cases for one functional area, using the template above. |
| `use-cases.md` | Index of all use cases: ID, name, area file, one-line summary. |
| `data-model.md` | Entities, fields, types, relations, constraints and migrations, from code and `android/legacy/schemas/`. |
| `glossary.md` | Domain terms exactly as named in the legacy code, each with a definition and citation. |
| `surprises.md` | Odd, inconsistent or suspicious behaviour, stated as fact and cited, with no fixes. |
