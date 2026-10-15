# Glossary

These are domain terms as the legacy code names them. Each is defined by what the code
does with it.

- The code identifier is given where it differs from the plain term.
- Terms coined by this analysis rather than the code are marked *(analysis term)*.
- All citations use paths relative to the repository root.

## Budgets and periods

| Term | Meaning in code | Source |
|---|---|---|
| Budget | A dated period (`startsOn`…`lastDayAt`) in one currency. It has total `plan` and `fact` and holds Expenditures and Spendings | `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:8-18` |
| Active budget | The budget with `id > 0` whose date range contains today. If none exists, a new one is created | `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:70-71`, `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:34-35` |
| Completed budget | A budget with `id > 0` and `lastDayAt` before today. Used by statistics | `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:16-17` |
| Last budget (`getLast`, `isPeriodChangeable`) | The budget with `id > 0` and the latest `lastDayAt`. Only it can have its end date changed, or be removed from the list. The phrase is used in a UI message | `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:31-32`, `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:40`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:73-74`, `android/legacy/src/main/res/values/strings.xml:109` |
| Scheduled budget (`SCHEDULED_BUDGET_ID = -2`) | Hidden budget with id −2, holding the Spendings that act as recurring templates | `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:24`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:94`, `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:30-33` |
| Period | Length of an automatically created budget: `MONTH`, `WEEK`, `TWO_WEEKS`, `QUARTER`, `YEAR` | `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:29-31` |
| Start day (`startDayOfMonth`, `startDayOfWeek`) | The day a MONTH, WEEK or TWO_WEEKS period begins | `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:11-12`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:208-237` |
| Truncation comment (`constant_budget_truncated`) | Budget comment set when a budget starts later than its period's computed start | `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:260-265`, `android/legacy/src/main/res/values/strings.xml:178` |

## Expenditures and spendings

| Term | Meaning in code | Source |
|---|---|---|
| Expenditure | A named budget line within one Budget, with `plan`, computed `fact` and `displayOrder` | `android/legacy/src/main/java/com/mas/mobile/domain/budget/Expenditure.kt:5-21` |
| Spending | One expense amount on a date, assigned to an Expenditure | `android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:10-18` |
| Plan / Fact | Planned and actual amount. An Expenditure's fact is the sum of its spendings. A Budget's plan and fact are the sums over its expenditures | `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:74-83` |
| Progress | `toInt(fact × 100 / plan)`. Expenditures use 100 when the plan is ≈ 0. Budgets have no zero guard | `android/legacy/src/main/java/com/mas/mobile/domain/budget/Expenditure.kt:15-20`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:22` |
| Unplanned (`formatter_unplanned`) | Label shown instead of progress when `|plan| < 0.001` | `android/legacy/src/main/java/com/mas/mobile/presentation/activity/binding/Adapter.kt:110-118`, `android/legacy/src/main/res/values/strings.xml:127` |
| Display order (`displayOrder`, `display_order`) | Sort key for categories and expenditures, applied before `name` | `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/CategoryDAO.kt:13-17`, `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/ExpenditureDAO.kt:10-16` |
| Scheduled spending (`createScheduledSpendings`, `ScheduledSpendingWorker`) | A Spending in the scheduled budget with a Recurrence. Each due occurrence is copied into the active budget | `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:131-152`, `android/legacy/src/main/java/com/mas/mobile/service/ScheduledSpendingWorker.kt:10-17` |
| Recurrence | Repeat rule of a scheduled Spending: `Never`, `Daily`, `Weekly`, `Monthly`, `Quarterly` | `android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:61-63` |
| Scheduled date (`scheduledDate`) | The next due occurrence of a scheduled Spending | `android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:19-20`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:28-40` |
| ExchangeInfo | Original foreign amount (`rawAmount`), `rate` and `currency` of a Spending entered in another currency | `android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:52-56` |
| Rate | Units of the **active** budget's currency per 1 foreign unit, fetched from FreeCurrencyAPI | `android/legacy/src/main/java/com/mas/mobile/repository/budget/FreeCurrencyAPIRepositoryImpl.kt:24-34`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:124` |

## Categories

| Term | Meaning in code | Source |
|---|---|---|
| Category | Template for Expenditures: name, plan, icon, active flag, merchant list and display order | `android/legacy/src/main/java/com/mas/mobile/domain/budget/Category.kt:3-12` |
| Active category (`isActive`, column `active`) | A category copied into each newly created budget | `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:170` |
| Merchant | A merchant-name string stored in a Category's merchant list. It is used to pick the Category for a captured message | `android/legacy/src/main/java/com/mas/mobile/domain/budget/Category.kt:17-18`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:12-15` |
| Icon (`IconId`) | Icon-pack id stored on categories and expenditures | `android/legacy/src/main/java/com/mas/mobile/domain/budget/Category.kt:20-21`, `android/legacy/src/main/java/com/mas/mobile/presentation/activity/IconPickerActivity.kt:27-33` |

## Messages and capture

| Term | Meaning in code | Source |
|---|---|---|
| Message (table `spending_messages`) | A captured SMS or notification with a status and an optional linked Spending | `android/legacy/src/main/java/com/mas/mobile/domain/message/Message.kt:7-28` |
| Sender (column `name`) | SMS originating address, or the label of the app that posted a notification | `android/legacy/src/main/java/com/mas/mobile/service/SmsListener.kt:32`, `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:70-74` |
| Message status | `Matched` (parsed by a template), `Recommended` (looks like a payment) or `Rejected` (not stored) | `android/legacy/src/main/java/com/mas/mobile/domain/message/Message.kt:20-27` |
| MessageTemplate (table `message_template`) | Per-sender parsing rule: `pattern`, `example`, `currency`, `enabled` | `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageTemplate.kt:6-13` |
| Rule id (`rule_id`) | Id of the template that matched a message. It is mapped to `messageTemplateId`. A table `message_rules` existed until migration 8→9 | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/SpendingMessage.kt:24-25`, `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageMapper.kt:20`, `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:198` |
| Pattern | Template text containing an `{amount}` placeholder and optionally a `{merchant}` placeholder | `android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:5-10` |
| Example | Sample text a template's pattern must parse before the template is saved | `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageTemplateViewModel.kt:74-87` |
| Qualifier | Keyword or sender used by the recommendation heuristic. Types: `CATCH` (payment word), `SKIP` (exclusion word), `BLACKLIST` (excluded sender) | `android/legacy/src/main/java/com/mas/mobile/domain/message/Qualifier.kt:3-11` |
| Discovery mode (`discoveryMode`, `autodetect`) | Setting that enables the Recommended status for unmatched messages | `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:16-24`, `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:141-146` |
| Promote (`promoteRecommendedMessage`) | Turn a Recommended message into Matched by generating a template with GPT | `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:56-83` |
| Siblings (`promoteSiblings`) | Other Recommended messages from the same sender, re-matched after a promotion | `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:85-96` |
| Message analyzer (`MessageAnalyzer`) | Component that builds a Pattern from message text. Implemented with OpenAI | `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageAnalyzer.kt:3-5`, `android/legacy/src/main/java/com/mas/mobile/service/ai/GPTMessageAnalyzer.kt:9-43` |
| Capture switches (`captureSms`, `captureNotifications`) | Settings flags that mirror the OS permission state. Intake does not read them | `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:13-14`, `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:101-117` |

## Statistics

| Term | Meaning in code | Source |
|---|---|---|
| Trend entry (`TrendEntry`) | Plan and fact of one completed budget, or of one same-named expenditure in it | `android/legacy/src/main/java/com/mas/mobile/domain/analytics/Analytics.kt:10-14` |
| Overspending alert (`OverspendingAlert`) | An expenditure whose exact name is present in the last 3 completed budgets, with total fact ≥ 120 % of total plan. A zero total plan counts as 120 % | `android/legacy/src/main/java/com/mas/mobile/domain/analytics/Analytics.kt:5-8`, `android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:34-55` |
| Share (`Share`) | One expenditure's fact within a budget, for the distribution chart | `android/legacy/src/main/java/com/mas/mobile/domain/analytics/Analytics.kt:16-20` |

## Settings, background, analytics, backup

| Term | Meaning in code | Source |
|---|---|---|
| Settings | Eight key/value rows (period, currency, start days, capture flags, policy version, discovery mode) | `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:8-17` |
| Policy version (`policy.version`, `POLICY_VERSION`) | The accepted privacy-policy file name. A mismatch shows the policy gate | `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:44-57` |
| DeferrableAction | A stored back-off timer that suppresses a repeated action until `activeAfter`. Keys are `ProgressiveDaily` (doubling) and `UniformDaily` | `android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableAction.kt:5-47` |
| Event | A Firebase analytics event, with a name and enum parameters | `android/legacy/src/main/java/com/mas/mobile/domain/analytics/Event.kt:5-114` |
| Backup file (`BackupFile`) | JSON export of budgets with expenditures, spendings and linked messages, `schemaVersion` 1 | `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupModels.kt:6-54` |
| Generator (table `generator`) | The shared id sequence for all app-created entities | `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/IdGeneratorDAO.kt:10-11` |
| Action | Screen mode passed as a navigation argument: `VIEW`, `ADD`, `EDIT`, `REMOVE`, `CLONE` | `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/validator/Action.kt:3-5` |
| `NEW_ITEM` | Id value −1 meaning "new item". Budget id −1 also means "the active budget" | `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BaseViewModel.kt:9`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:55-61` |
