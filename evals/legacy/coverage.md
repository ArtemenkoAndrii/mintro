# Coverage

132 examples cover 36 of the 78 legacy use cases (`docs/legacy/use-cases.md`). 13 records
have `needsReview: true`.

## Examples per use case

| Area | Use case | File | Examples | needsReview |
|---|---|---|---|---|
| 1 Budgets | UC-L1.2 Create the next budget automatically | `budgets/UC-L1.2.jsonl` | 1 | 0 |
| | UC-L1.3 Compute period boundaries, name and truncation comment | `budgets/UC-L1.3.jsonl` | 16 | 0 |
| | UC-L1.4 Add a budget from the budget list | `budgets/UC-L1.4.jsonl` | 10 | 10 |
| | UC-L1.10 Materialise scheduled spendings (`scheduledDate` rule only) | `budgets/UC-L1.10.jsonl` | 9 | 0 |
| 2 Expenditures | UC-L2.1 View expenditures of the active budget | `expenditures/UC-L2.1.jsonl` | 1 | 0 |
| | UC-L2.3 Create expenditures from categories | `expenditures/UC-L2.3.jsonl` | 1 | 0 |
| | UC-L2.4 Add an expenditure manually (name lookup ignoring case) | `expenditures/UC-L2.4.jsonl` | 2 | 0 |
| | UC-L2.5 Create an expenditure implicitly | `expenditures/UC-L2.5.jsonl` | 1 | 0 |
| | UC-L2.6 View and edit an expenditure (domain update) | `expenditures/UC-L2.6.jsonl` | 1 | 0 |
| | UC-L2.8 Delete an expenditure (domain removal) | `expenditures/UC-L2.8.jsonl` | 1 | 0 |
| | UC-L2.9 Calculate plan, fact and progress | `expenditures/UC-L2.9.jsonl` | 3 | 0 |
| | UC-L2.10 Order expenditures | `expenditures/UC-L2.10.jsonl` | 2 | 0 |
| 3 Spendings | UC-L3.3 Add a spending manually | `spendings/UC-L3.3.jsonl` | 1 | 0 |
| | UC-L3.4 View and edit a spending | `spendings/UC-L3.4.jsonl` | 2 | 0 |
| | UC-L3.6 Delete a spending | `spendings/UC-L3.6.jsonl` | 2 | 0 |
| | UC-L3.7 Enter a spending in a foreign currency | `spendings/UC-L3.7.jsonl` | 1 | 0 |
| | UC-L3.9 Record a spending programmatically | `spendings/UC-L3.9.jsonl` | 2 | 0 |
| 4 Categories | UC-L4.2 Add a category | `categories/UC-L4.2.jsonl` | 1 | 0 |
| | UC-L4.3 View and edit a category | `categories/UC-L4.3.jsonl` | 2 | 0 |
| | UC-L4.5 Delete a category | `categories/UC-L4.5.jsonl` | 1 | 0 |
| | UC-L4.6 Reorder categories by drag (`CategoryService.reorder`) | `categories/UC-L4.6.jsonl` | 2 | 0 |
| | UC-L4.7 Manage a category's merchants | `categories/UC-L4.7.jsonl` | 1 | 0 |
| | UC-L4.9 Look up a category by merchant or by name | `categories/UC-L4.9.jsonl` | 4 | 0 |
| 5 Capture | UC-L5.3 Classify and store an incoming message | `capture/UC-L5.3.jsonl` | 4 | 0 |
| | UC-L5.4 Parse a message with a template pattern | `capture/UC-L5.4.jsonl` | 12 | 0 |
| | UC-L5.5 Create a spending automatically for a matched message | `capture/UC-L5.5.jsonl` | 4 | 0 |
| | UC-L5.6 Decide whether an unmatched message is "Recommended" | `capture/UC-L5.6.jsonl` | 9 | 2 |
| | UC-L5.9 Generate a template from a Recommended message (GPT) | `capture/UC-L5.9.jsonl` | 5 | 0 |
| | UC-L5.14 Detect currencies in a message text | `capture/UC-L5.14.jsonl` | 4 | 0 |
| 6 Settings | UC-L6.7 Store and read settings (system-currency default) | `settings/UC-L6.7.jsonl` | 3 | 0 |
| 7 Background | UC-L7.4 Defer an action with progressive back-off | `background/UC-L7.4.jsonl` | 13 | 1 |
| 8 Statistics | UC-L8.2 View plan/fact trends | `statistics/UC-L8.2.jsonl` | 2 | 0 |
| | UC-L8.3 View overspending alerts | `statistics/UC-L8.3.jsonl` | 1 | 0 |
| | UC-L8.4 View expenditure distribution of a budget | `statistics/UC-L8.4.jsonl` | 1 | 0 |
| 9 Backup | UC-L9.1 Export budgets to a JSON file | `backup/UC-L9.1.jsonl` | 3 | 0 |
| | UC-L9.2 Import budgets from a JSON file | `backup/UC-L9.2.jsonl` | 4 | 0 |

Several examples test domain methods (`Budget.addExpenditure`, `Budget.addSpending`,
`Budget.findExpenditure`) rather than the screen flow named by the use case. Their
`notes` say so.

## Use cases without examples (42)

| Area | Use cases |
|---|---|
| 1 Budgets | UC-L1.1, UC-L1.5, UC-L1.6, UC-L1.7, UC-L1.8, UC-L1.9 |
| 2 Expenditures | UC-L2.2, UC-L2.7 |
| 3 Spendings | UC-L3.1, UC-L3.2, UC-L3.5, UC-L3.8, UC-L3.10 |
| 4 Categories | UC-L4.1, UC-L4.4, UC-L4.8, UC-L4.10 |
| 5 Capture | UC-L5.1, UC-L5.2, UC-L5.7, UC-L5.8, UC-L5.10, UC-L5.11, UC-L5.12, UC-L5.13 |
| 6 Settings | UC-L6.1, UC-L6.2, UC-L6.3, UC-L6.4, UC-L6.5, UC-L6.6, UC-L6.8, UC-L6.9, UC-L6.10 |
| 7 Background | UC-L7.1, UC-L7.2, UC-L7.3, UC-L7.5, UC-L7.6, UC-L7.7 |
| 8 Statistics | UC-L8.1 |
| 9 Backup | UC-L9.3 |

Some of these are touched by tests but not asserted on their own:
- **UC-L5.1, UC-L5.2:** the capture tests call `MessageService.handleRawMessage`
  directly. They never go through `SmsListener` or `NotificationListener`. The
  notification test only copies the `"title\ntext"` text shape.
- **UC-L5.7, UC-L3.1:** message-list icons and Spending-tab rows appear inside the
  `then` of UC-L5.3, UC-L5.5 and UC-L3.3 to UC-L3.6 records.
- **UC-L6.1:** every instrumented test taps the policy agree button (`agreeToPolicy()`),
  but asserts nothing about the gate.
- **UC-L6.3:** `BudgetCreationTest` sets the period and start day through Settings, but
  only as setup for UC-L1.4.
- **UC-L1.1:** the instrumented fixtures seed a budget covering today so that the active
  budget resolves to it. This is not asserted.

## Effectively untested behaviour

- **Weekly recurrence.** `should calc weekly recurrence` uses `Recurrence.Daily`. Its two
  examples are identical to daily examples and are merged into `UC-L1.10-001` and
  `UC-L1.10-002`.
- **Quarterly recurrence.** `should calc quarterly recurrence` uses `Recurrence.Monthly`
  (`UC-L1.10-008`).
- **Scheduled-spending materialisation (UC-L1.10).** Only the `scheduledDate` rule is
  tested. The worker run that copies occurrences into the active budget is not.
- **Truncation comment text in the period unit test.** The text comes from a mocked
  `ResourceService`, so it is left out of the 16 `UC-L1.3` records. The app's real string
  is checked only in `BudgetCreationTest` (`UC-L1.4`, relative values).
- **Edge paths of `domain/message`:** see "Test coverage of `domain/message`" in
  `docs/legacy/use-cases/capture.md` and S-L5.16.

## Records with `needsReview`

| Record | Reason |
|---|---|
| `UC-L1.4-001` … `UC-L1.4-010` | `BudgetCreationTest` computes its expected names, dates and comments at run time. The records state the rule with relative dates, not literal values |
| `UC-L5.6-008`, `UC-L5.6-009` | The BLACKLIST fixtures are built with type `CATCH` (`android/legacy/src/test/java/com/mas/mobile/domain/message/QualifierServiceTest.kt:33-34`), so these examples may not exercise the blacklist rule |
| `UC-L7.4-008` | The "new action" key is not equal to the stubbed key, so `getByKey` hits an unstubbed relaxed mock. Without running the test, it is unclear which branch produced `false` |

## Test-name mismatches (recorded with actual inputs)

| Record | Test name | Actual input |
|---|---|---|
| `UC-L1.10-001`, `UC-L1.10-002` (merged) | `should calc weekly recurrence` | `Recurrence.Daily` |
| `UC-L1.10-008` | `should calc quarterly recurrence` | `Recurrence.Monthly` |
| `UC-L7.4-009` | `should not` | `DeferrableActionService.remove(key)` |
| `UC-L7.4-001` … `UC-L7.4-006` | constants `ONE_DAY`, `TWO_DAY` | `Hours(1)`, `Hours(2)` |
