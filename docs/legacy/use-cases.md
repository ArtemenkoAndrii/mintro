# Use-case index

There are 78 use cases across 9 areas. Each entry links to its area file, where the full
use case and its citations are. The summaries describe behaviour as implemented.

| Area | File | Use cases |
|---|---|---|
| 1 | Budgets and periods: [`use-cases/budgets.md`](use-cases/budgets.md) | 10 |
| 2 | Expenditures: [`use-cases/expenditures.md`](use-cases/expenditures.md) | 10 |
| 3 | Spendings and currency exchange: [`use-cases/spendings.md`](use-cases/spendings.md) | 10 |
| 4 | Categories and merchants: [`use-cases/categories.md`](use-cases/categories.md) | 10 |
| 5 | Automatic capture: [`use-cases/capture.md`](use-cases/capture.md) | 14 |
| 6 | Settings, onboarding, privacy policy, analytics: [`use-cases/settings.md`](use-cases/settings.md) | 10 |
| 7 | Background work and notifications: [`use-cases/background.md`](use-cases/background.md) | 7 |
| 8 | Statistics: [`use-cases/statistics.md`](use-cases/statistics.md) | 4 |
| 9 | Backup export and import: [`use-cases/backup.md`](use-cases/backup.md) | 3 |

## Area 1: Budgets and periods

| ID | Name | Actor | Summary |
|---|---|---|---|
| UC-L1.1 | Resolve the active budget | system | Finds the budget whose date range contains today. Creates the next budget if none does. |
| UC-L1.2 | Create the next budget automatically | system | Starts the day after the last budget, or today if later. Copies active categories as expenditures and saves. |
| UC-L1.3 | Compute period boundaries, name and truncation comment | system | Derives the start, end and name for MONTH, WEEK, TWO_WEEKS, QUARTER or YEAR. Adds a comment when the start was truncated. |
| UC-L1.4 | Add a budget from the budget list | user | The add button creates the next budget straight away, with no form. |
| UC-L1.5 | Browse budgets | user | Lists every budget except the scheduled one, newest first, with progress against plan. |
| UC-L1.6 | View and edit a budget | user | Edits end date (last budget only), plan, currency and comment. Name and start date are read-only. |
| UC-L1.7 | Delete a budget | user | The list allows deleting only the last budget. The form allows any. Children are removed by cascade. |
| UC-L1.8 | Refresh the active budget when the date changes | system | A time-tick listener reloads the active budget once the date has changed. |
| UC-L1.9 | Manage scheduled spendings | user | Edits recurring spending templates stored in the hidden scheduled budget (id −2). |
| UC-L1.10 | Materialise scheduled spendings into the active budget | system | The worker copies due occurrences into the active budget, at most one per template per run. |

## Area 2: Expenditures

| ID | Name | Actor | Summary |
|---|---|---|---|
| UC-L2.1 | View expenditures of the active budget (Expenditures tab) | user | The start tab lists the active budget's expenditures with progress. A tap adds a spending. |
| UC-L2.2 | View and manage expenditures of a chosen budget | user | Same list for any budget, with view, edit, clone and remove. |
| UC-L2.3 | Create expenditures from categories when a budget is created | system | One expenditure per active category, copying name, plan and icon. |
| UC-L2.4 | Add an expenditure manually | user | Name of at least 3 characters, unique ignoring case. No icon is set. |
| UC-L2.5 | Create an expenditure implicitly when recording a spending | user, system | An unknown expenditure name creates a new expenditure. A same-named category supplies plan and icon. |
| UC-L2.6 | View and edit an expenditure | user | Edits name, plan and comment. A rename that changes only letter case or surrounding whitespace fails. |
| UC-L2.7 | Clone an expenditure | user | Copies name, plan and comment into the same budget. It must be renamed to save. |
| UC-L2.8 | Delete an expenditure | user | Removes the expenditure. Its spendings go by cascade, and linked messages keep their link. |
| UC-L2.9 | Calculate plan, fact and progress | system | Fact is the sum of spendings. Budget totals are sums over expenditures. Totals are stored on save. |
| UC-L2.10 | Order expenditures | system | A new expenditure gets `max + 1` as display order. There is no reorder action. |

## Area 3: Spendings and currency exchange

| ID | Name | Actor | Summary |
|---|---|---|---|
| UC-L3.1 | Browse recent spendings (Spending tab) | user | The Spending tab shows the last 30 days across budgets, excluding scheduled templates. |
| UC-L3.2 | Browse spendings of a budget or an expenditure | user | Lists the spendings of one budget or one expenditure. |
| UC-L3.3 | Add a spending manually | user | Goes to the resolved budget whatever its date. Only the expenditure name is validated. |
| UC-L3.4 | View and edit a spending | user | Edits within the same budget. A spending can move only between that budget's expenditures. |
| UC-L3.5 | Clone a spending | user | Copies into the source's budget, dated now. The source's message link moves to the clone. |
| UC-L3.6 | Delete a spending | user | Removes by value equality, unlinks the message and recalculates totals. Deleting from the form can silently fail. |
| UC-L3.7 | Enter a spending in a foreign currency | user | Converts with a fetched rate: `amount = round(raw × rate, 2)`. |
| UC-L3.8 | Fetch an exchange rate | system | Calls FreeCurrencyAPI against the active budget's currency. A failure gives 0.00. |
| UC-L3.9 | Record a spending programmatically (`BudgetService.spend`) | system | `BudgetService.spend` adds to the active budget and converts when a currency is given. |
| UC-L3.10 | Side effects of saving a spending from the form | user | Links the message, optionally links the merchant to a category, and logs analytics. |

## Area 4: Categories and merchants

| ID | Name | Actor | Summary |
|---|---|---|---|
| UC-L4.1 | Browse categories | user | Lists categories by display order. Inactive ones are dimmed. |
| UC-L4.2 | Add a category | user | Name of at least 3 characters, unique ignoring case. Display order becomes `max + 1`. |
| UC-L4.3 | View and edit a category | user | Edits affect only future budgets and lookups. Existing expenditures are untouched. |
| UC-L4.4 | Clone a category | user | Copies name, plan, active flag and description. It must be renamed to save. |
| UC-L4.5 | Delete a category | user | Deletes the row. Nothing references a category by id. |
| UC-L4.6 | Reorder categories by drag | user | Swaps existing display-order values by position. Drags do not persist when values are equal. |
| UC-L4.7 | Manage a category's merchants | user | Adds or removes merchant strings of at least 3 characters, stored as a JSON list on the category. |
| UC-L4.8 | Link a merchant from a spending | user | After a spending from a message, and if the user agrees, the merchant is appended to the category with the expenditure's name. |
| UC-L4.9 | Look up a category by merchant or by name | system | Exact match ignoring case. The first match in display order wins. Inactive categories are included. |
| UC-L4.10 | Seed default categories | system | Inserts 19 English categories. Migration 8→9 inserts into a column that does not exist yet. |

## Area 5: Automatic capture

| ID | Name | Actor | Summary |
|---|---|---|---|
| UC-L5.1 | Capture an incoming SMS | system | Each PDU becomes a raw message, stamped with the processing time. |
| UC-L5.2 | Capture an app notification | system | The sender is the app label. The text is "title\ntext", or is read from the custom view. |
| UC-L5.3 | Classify and store an incoming message | system | Matched by the first parsing template, else Recommended or Rejected. Rejected messages are not stored. |
| UC-L5.4 | Parse a message with a template pattern | system | Builds a regex from `{amount}` and `{merchant}` and falls back to the amount only. Has known crash and misparse paths. |
| UC-L5.5 | Create a spending automatically for a matched message | system | Needs a merchant listed in a category. Spends into the active budget in the template's currency. |
| UC-L5.6 | Decide whether an unmatched message is "Recommended" | system | Length, blacklist, amount, currency and keyword checks, all substring-based. |
| UC-L5.7 | Browse captured messages | user | Last 30 days, with status-specific actions. The unread badge has no date limit. |
| UC-L5.8 | Match a message to a spending manually | user | Opens the spending form pre-filled with amount and text. Date is now and currency is the budget's. |
| UC-L5.9 | Generate a template from a Recommended message (GPT) | user | Sends the text to OpenAI, builds and validates a template, and re-matches messages from the same sender. |
| UC-L5.10 | Manage message templates | user | View, edit, enable and delete. There is no way to add one manually. |
| UC-L5.11 | Manage qualifiers and discovery mode | user | CATCH and SKIP tabs, the blacklist screen, and the discovery switch on the template list. |
| UC-L5.12 | Blacklist a sender or delete a message | user | Blacklisting adds a qualifier and deletes the message. A swipe deletes the message only. |
| UC-L5.13 | Seed templates and qualifiers | system | 4 bank templates and language-specific keywords. The Ukrainian branch never fires. |
| UC-L5.14 | Detect currencies in a message text | system | Matches currency symbols and codes by regex. Texts ending in digits detect the `$` currency. |

## Area 6: Settings, onboarding, privacy policy, analytics

| ID | Name | Actor | Summary |
|---|---|---|---|
| UC-L6.1 | Accept the privacy policy | user | Gate shown while the stored version ≠ v3. Decline closes the app. Nothing else waits for consent. |
| UC-L6.2 | Open the Settings screen | user | Budget, Messages, Backup and About sections. There is no discovery switch. |
| UC-L6.3 | Change budget settings (period, start day, currency) | user | Period, start day and currency. Used only when the next budget is created. |
| UC-L6.4 | Turn SMS capture on or off | user | Stores the permission state, not the switch position. Capture continues while the permission is granted. |
| UC-L6.5 | Turn notification capture on or off | user | Opens system listener settings. Stores the grant state, which is not re-checked on return. |
| UC-L6.6 | Guided capture setup from the Messages tab | user | Offers to open Settings, which flashes the two capture switches. |
| UC-L6.7 | Store and read settings | system | Eight key/value rows with defaults. Every save rewrites all keys. |
| UC-L6.8 | First run and onboarding | system | Seeds, policy gate, first monthly budget. The first-run flag is unused. |
| UC-L6.9 | About section | user | Opens a hard-coded wiki URL. Shows the app version. |
| UC-L6.10 | Log analytics events | system | Ten Firebase events with enum parameters. No consent check and no opt-out. |

## Area 7: Background work and notifications

| ID | Name | Actor | Summary |
|---|---|---|---|
| UC-L7.1 | Schedule the periodic workers | system | Two 2-hour unique works, re-enqueued with REPLACE on every main-screen start. |
| UC-L7.2 | Run the scheduled-spending worker | system | Starts materialisation and returns success without waiting. |
| UC-L7.3 | Check for an app update and notify | system | Asks Play for an update. When one is available and not already deferred, defers, notifies and logs. |
| UC-L7.4 | Defer an action with progressive back-off | system | Minimum waits of 24, 48, 96, 192 h, then 384 h. Rows whose `active_after` is older than 30 days are purged. |
| UC-L7.5 | Post the "new version" notification | system | The only notification. Posted only if `POST_NOTIFICATIONS` is granted, which the app never requests. |
| UC-L7.6 | Reload the active budget on date change | system | Same mechanism as UC-L1.8, tied to the main screen's lifetime. |
| UC-L7.7 | Run background tasks and report errors | system | Fire-and-forget IO coroutines. Errors are shown as a generic toast. |

## Area 8: Statistics

| ID | Name | Actor | Summary |
|---|---|---|---|
| UC-L8.1 | Open the statistics menu | user | Three chart entries. |
| UC-L8.2 | View plan/fact trends | user | Last 12 completed budgets, optionally filtered by expenditure name. |
| UC-L8.3 | View overspending alerts | user | Expenditures whose exact name appears in all of the last 3 completed budgets, with total fact ≥ 120 % of total plan. A zero total plan counts as 120 %. |
| UC-L8.4 | View expenditure distribution of a budget | user | Pie of expenditure facts. It opens on the active budget, while the filter shows another budget. |

## Area 9: Backup export and import

| ID | Name | Actor | Summary |
|---|---|---|---|
| UC-L9.1 | Export budgets to a JSON file | user | Budgets with expenditures, spendings and linked messages. No configuration. |
| UC-L9.2 | Import budgets from a JSON file | user | Skips by name, regenerates ids, resets messages. Not atomic. |
| UC-L9.3 | Platform backup | system | `allowBackup="true"`, with no rules or agent. |
