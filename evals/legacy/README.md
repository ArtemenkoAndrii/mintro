# Legacy behaviour dataset

Concrete examples of legacy app behaviour, extracted from the legacy JUnit tests in
`android/legacy/src/test/` and `android/legacy/src/androidTest/`. Each example records
what a test asserts, **as written**: nothing is corrected or judged. The use cases,
entities and terms come from `docs/legacy/use-cases.md`, `docs/legacy/data-model.md`
and `docs/legacy/glossary.md`.

The tests were read, not run. Running them would write build output under
`android/legacy/`, which is read-only.

## Layout

```
evals/legacy/
  README.md            this file
  coverage.md          examples per use case, gaps, records needing review
  <area>/<use-case-id>.jsonl
```

`<area>` is the area file name from `docs/legacy/use-cases/`: `budgets`, `expenditures`,
`spendings`, `categories`, `capture`, `settings`, `background`, `statistics`, `backup`.

## Record format

One JSON object per line:

| Field | Meaning |
|---|---|
| `id` | `<use-case-id>-<nnn>`, numbered within the file |
| `useCase` | The primary use case, e.g. `UC-L1.3`. A test with several steps is split into one record per step, each under its own use case |
| `entities` | Entity names from `docs/legacy/data-model.md` (`Budget`, `Expenditure`, `Spending`, `Message`, `Category`, `MessageTemplate`, `Qualifier`, `Settings`, `DeferrableAction`) |
| `given` | The initial state, with concrete values |
| `when` | The action or input. `action` names the code entry point or the UI steps |
| `then` | The asserted result. Only what the test asserts is included |
| `origin` | `path:line` of the main assertion, relative to the repo root |
| `surprise` | Id of a related entry in `docs/legacy/surprises.md`, or `null` |
| `needsReview` | `true` when the example may not show what it seems to (see `notes`) |
| `notes` | Extra origins, assertion ranges, mocked collaborators, test-name mismatches |

Rules:
- When several tests assert the same example, it is one record. The other origins are
  listed in `notes`.
- Fields the test does not assert are left out of `then`. Values that come from test
  mocks rather than the app are left out of `then` and explained in `notes`. For
  example, truncation comments built by a mocked `ResourceService`.
- UI tests record display strings exactly as asserted, e.g. `"25.00 / 50.00"` or `"-25.00"`.

## Value conventions

| Kind | Format | Example |
|---|---|---|
| Money | `{"minor": <int>, "currency": "<ISO 4217>" \| null}`. `currency` is `null` when the test sets no currency for that value, e.g. Category plans or `Pattern.parse` results | `{"minor": 410, "currency": "EUR"}` |
| Rates, percentages | Plain decimals | `2.0`, `125.56` |
| Dates, date-times | ISO-8601 | `2024-02-25`, `2024-02-25T12:00` |
| Durations | ISO-8601 durations | `PT1H`, `P1D` |
| Run-time-relative dates | `today` / `now` plus or minus an ISO-8601 duration, relative to the moment the test runs | `today+P1D`, `now-P1M`, `today+P29D` |
| Computed expectations | A short rule in words, when the test computes the expected value at run time (only `BudgetCreationTest`, marked `needsReview`) | `estimatedStart+P6D` |

## Masking

The tests were written with real data. Personal data is masked before writing.
**Values under test are never masked**: amounts, currencies, merchants, senders, dates.

| Data | Masked as | Example |
|---|---|---|
| Card and account numbers | Same format, digits replaced by `0` | `ending in 0000`, `****0000`, `*000` |
| Balances | `0.00`, keeping the currency symbol | `$0.00` |
| Names | `[NAME]` | |
| Phone numbers | `[PHONE]` | |
| E-mail addresses | `[EMAIL]` | |
| Postal addresses | `[ADDRESS]` | |
| OTP and verification codes | `[CODE]` | |

When a masked value also appears in a pattern, it is masked the same way in both, so the
example still matches. For example, the ING account suffix appears in both the pattern
and the text. Masking was checked not to change any recorded outcome: the 250-character
limit still holds, and no text starts to end in digits.

Never add secrets, API keys, tokens, signing data or Firebase configuration values to
this dataset.
