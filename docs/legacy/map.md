# Legacy code map

Stage 1 output. This file maps the structure of the legacy app in `android/legacy/`.
Behaviour details belong in the area files (`use-cases/<area>.md`). Here they appear
only when needed to classify a component.

- App version described: `versionName` 1.2.5, `versionCode` 39 (`android/legacy/build.gradle:17-18`).
- `minSdkVersion` 26, `targetSdkVersion` 34 (`android/legacy/build.gradle:15-16`).
- Tests under `android/legacy/src/test/` and `android/legacy/src/androidTest/` were not
  used as a source of behaviour.
- The unit tests for `domain/message` (6 files, 31 tests) were read, but not run. Their
  coverage is recorded in `use-cases/capture.md` ("Test coverage of `domain/message`").

## 1. Module and package overview

There is one Gradle module, configured in `android/legacy/build.gradle`, with root
package `com.mas.mobile` (`android/legacy/build.gradle:48`). It applies the Android,
Kotlin, kapt, Safe Args, Google Services and Crashlytics plugins
(`android/legacy/build.gradle:3-8`), and data binding is enabled
(`android/legacy/build.gradle:45-47`).

Main libraries:

| Library | Use | Source |
|---|---|---|
| Room 2.5.0 | Local SQLite database | `android/legacy/build.gradle:131-135` |
| Retrofit 2.9.0 + Gson | HTTP clients for exchange rates and GPT | `android/legacy/build.gradle:137-140` |
| Dagger 2.40.1 | Dependency injection | `android/legacy/build.gradle:142-145` |
| Firebase BoM, Analytics, Crashlytics | Event logging and crash reporting | `android/legacy/build.gradle:147-150` |
| WorkManager | Periodic background jobs | `android/legacy/build.gradle:125` |
| Play app-update | In-app update check | `android/legacy/build.gradle:121-123` |
| Navigation (fragment, ui, dynamic features) | Screen navigation | `android/legacy/build.gradle:114-119` |
| MPAndroidChart, icondialog + iconpack-default, currency-picker | Charts, icon picker, currency picker | `android/legacy/build.gradle:129`, `android/legacy/build.gradle:152-155` |

Package layout under `android/legacy/src/main/java/com/mas/mobile/`:

| Package | Contents |
|---|---|
| `domain/budget` | Budget, Expenditure, Spending and Category models, `BudgetService`, `CategoryService`, and repository interfaces |
| `domain/message` | Message, MessageTemplate, Pattern and Qualifier models, `MessageService`, `MessageTemplateService`, `QualifierService`, `CurrencyExpert` |
| `domain/settings` | Settings model, `SettingsService`, DeferrableAction model, `DeferrableActionService` |
| `domain/analytics` | Statistics (`AnalyticsService`) **and** Firebase event definitions (`Event`, `EventLogger`) |
| `domain/backup` | JSON export/import of budgets (`BackupService`, `BackupModels`, `BackupJson`) |
| `repository/db` | Room `AppDatabase`, seed SQL (`DML`), DAOs, entities, type converter |
| `repository/budget`, `repository/message`, `repository/settings` | Repository implementations and DTO↔model mappers, plus `FreeCurrencyAPIRepositoryImpl` |
| `presentation/activity` | 3 Activities. `fragment/` holds the Fragments, `converter/` and `binding/` hold data-binding helpers |
| `presentation/adapter` | RecyclerView adapters, including row menus and row navigation |
| `presentation/viewmodel` | ViewModels, `validator/` (field validation, `Action` enum) |
| `service` | Android components (receivers, listener service, workers) and infrastructure services |
| `service/ai` | OpenAI chat-completions client used to generate message patterns |
| `util` | `CurrencyTools`, `DateTool`, `FirebaseEventLogger` |
| (root) | `MasApplication` (Dagger component and modules), `Extensions.kt` |

Wiring: `AppModule` binds every domain repository interface to its Room-backed
implementation (`android/legacy/src/main/java/com/mas/mobile/MasApplication.kt:135-211`).
`ExternalServicesModule` binds the exchange-rate client, the GPT client, the message
analyzer and the task service (`android/legacy/src/main/java/com/mas/mobile/MasApplication.kt:225-247`).

Mismatch: `BudgetRepositoryImpl.kt` lives in `repository/budget/` but declares
package `com.mas.mobile.repository`
(`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:1`).

## 2. Screens and navigation

### Activities

| Activity | Role | Source |
|---|---|---|
| `MainActivity` | Launcher activity (`MAIN` + `LAUNCHER`). Hosts the navigation graph, with start destination `nav_expenditure_list`. | `android/legacy/src/main/AndroidManifest.xml:26-35`, `android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:55-59` |
| `PolicyActivity` | Shows `res/raw/<POLICY_VERSION>.html` in a WebView. **Agree** stores the policy version, **Decline** calls `finishAffinity()`. | `android/legacy/src/main/java/com/mas/mobile/presentation/activity/PolicyActivity.kt:23-36` |
| `IconPickerActivity` | Icon dialog. Returns the chosen icon id as extra `selected_icon_id`. | `android/legacy/src/main/java/com/mas/mobile/presentation/activity/IconPickerActivity.kt:24-43` |

`MainActivity.onCreate` also does the following:
- Opens `PolicyActivity` when the stored policy version differs from `privacy_policy_v3`
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:82-86`,
  `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:44-45`,
  `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:57`).
- Registers `DateListener` and enqueues the workers (see §4).
- Shows a badge with the number of unread messages on the Messages tab
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:121-129`).

`IconPickerActivity` is launched for a result from `CategoryFragment`
(`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryFragment.kt:37-38`,
`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryFragment.kt:95-96`).

### Navigation shell

- Two `main_activity` layouts exist:
  - `res/layout-h470dp/` has a `BottomNavigationView`
    (`android/legacy/src/main/res/layout-h470dp/main_activity.xml:25-29`).
  - The default `res/layout/` has a `NavigationView`
    (`android/legacy/src/main/res/layout/main_activity.xml:31-36`).
  - Both use `@menu/bottom_menu`.
- `bottom_menu` has four root items: Expenditures (`nav_expenditure_list`), Spending
  (`nav_spending_list`), Messages (`nav_message_list_fragment`) and Menu (`nav_menu`)
  (`android/legacy/src/main/res/menu/bottom_menu.xml:4-23`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:61-66`).
- The bottom bar is hidden on non-root destinations
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:113-119`).
  Reselecting a tab pops its back stack
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:105-108`).
- `res/menu/right_side_menu.xml` defines items `nav_home`, `nav_categories` and
  `nav_scheduled` (`android/legacy/src/main/res/menu/right_side_menu.xml:4-48`). No layout
  or Kotlin file references this menu, and those ids are not destinations in the
  navigation graph.

### Destinations (`android/legacy/src/main/res/navigation/nav_graph.xml`)

All Fragment classes are in `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/`.

| Destination id | Class | Purpose | Graph lines | Class line |
|---|---|---|---|---|
| `nav_expenditure_list` | `ExpenditureListFragment` | Start tab: expenditures of a budget (`budget_id`). FAB adds a spending. | 54-89 | `ExpenditureListFragment.kt:25` |
| `nav_expenditure_spending` | `SpendingFragment` | Spending form opened from the expenditure tab | 91-117 | `SpendingFragment.kt:19` |
| `nav_expenditure_spending_list` | `ExpenditureSpendingListFragment` | Spendings of one expenditure | 118-149 | `SpendingListFragment.kt:89` |
| `nav_spending_list` | `SpendingListFragment` | Spending tab | 153-188 | `SpendingListFragment.kt:24` |
| `nav_spending` | `SpendingFragment` | Spending form opened from spending lists | 190-216 | `SpendingFragment.kt:19` |
| `nav_budget_list` | `BudgetListFragment` | List of budgets | 219-266 | `BudgetListFragment.kt:14` |
| `nav_budget` | `BudgetFragment` | Budget form | 268-298 | `BudgetFragment.kt:15` |
| `nav_budget_expenditure_list` | `BudgetExpenditureListFragment` | Expenditures of a chosen budget | 300-335 | `ExpenditureListFragment.kt:85` |
| `nav_budget_expenditure` | `ExpenditureFragment` | Expenditure form | 337-355 | `ExpenditureFragment.kt:15` |
| `nav_budget_spending_list` | `BudgetSpendingListFragment` | Spendings of a chosen budget | 357-388 | `SpendingListFragment.kt:66` |
| `nav_scheduled_spending_list` | `ScheduledSpendingListFragment` | Spendings of the scheduled budget (id −2) | 390-421 | `SpendingListFragment.kt:111` |
| `nav_budget_spending` | `SpendingFragment` | Spending form opened from budget/scheduled lists | 423-449 | `SpendingFragment.kt:19` |
| `nav_message_list_fragment` | `MessageListFragment` | Messages tab: captured messages | 8-51 | `MessageListFragment.kt:29` |
| `nav_message_template_list_fragment` | `MessageTemplateListFragment` | Message templates. Links to black list and filters. | 452-478 | `MessageTemplateListFragment.kt:18` |
| `nav_message_template_fragment` | `MessageTemplateFragment` | Message template form | 480-494 | `MessageTemplateFragment.kt:18` |
| `nav_qualifier_list` | `QualifierListFragment` | Qualifier tabs (CATCH / SKIP) | 525-530 | `QualifierListFragment.kt:19` |
| `nav_black_list` | `BlackListFragment` | BLACKLIST qualifiers | 531-536 | `QualifierListFragment.kt:93` |
| `nav_settings` | `SettingsFragment` | Settings. Argument `guided` (default `false`). | 497-522 | `SettingsFragment.kt:36` |
| `nav_analytics` | `AnalyticsFragment` | Statistics menu with three chart types | 539-553 | `AnalyticsFragment.kt:12` |
| `nav_chart` | `ChartFragment` | Chart for an `analytics.Type` argument | 556-565 | `ChartFragment.kt:44` |
| `nav_menu` | `MenuFragment` | Menu tab: links to budgets, statistics, settings, message templates, categories, scheduled spendings | 568-603 | `MenuFragment.kt:13` |
| `nav_category_list` | `CategoryListFragment` | Categories, with drag-to-reorder | 606-624 | `CategoryListFragment.kt:24` |
| `nav_category` | `CategoryFragment` | Category form | 625-639 | `CategoryFragment.kt:24` |

Graph-wide actions: `action_to_settings` (with `guided`) and `action_to_analytics`
(`android/legacy/src/main/res/navigation/nav_graph.xml:642-653`).

Navigation conventions:
- Form screens take an `action` argument holding an `Action` name: `VIEW`, `ADD`, `EDIT`,
  `REMOVE` or `CLONE` (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/validator/Action.kt:3-5`).
- New items use id `NEW_ITEM = -1`
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BaseViewModel.kt:9`).
- Budget id `-1` means "the active budget"
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:55-68`).
- `MenuFragment` opens the scheduled list with `Budget.SCHEDULED_BUDGET_ID`
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MenuFragment.kt:29-31`).
- A message row opens the existing spending if the message has one, otherwise a new
  spending form with `message_id`
  (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/SpendingMessageAdapter.kt:95-99`).
- When message capturing is off, the Messages tab offers once to open Settings with
  `guided = true`
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MessageListFragment.kt:92-98`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageListViewModel.kt:23`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageListViewModel.kt:37-38`).
  With `guided = true`, Settings blinks the two capture switches
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:157-161`).
- `action_to_budget_template` (default `budget_id` = 1) is declared
  (`android/legacy/src/main/res/navigation/nav_graph.xml:238-245`), but no Kotlin call site
  uses it (`actionToBudgetTemplate` has no matches).

## 3. Domain classes, services, repositories

### Domain models

All paths are under `android/legacy/src/main/java/com/mas/mobile/domain/`.

| Model | Fields / notes | Source |
|---|---|---|
| `Budget` | `id`, `name`, `plan`, `fact`, `startsOn`, `lastDayAt`, `comment`, `currency`, and lazily loaded `BudgetDetails` (expenditures + spendings). `SCHEDULED_BUDGET_ID = -2`. | `budget/Budget.kt:8-104` |
| `Expenditure` | `id`, `name`, `iconId`, `plan`, `fact`, `comment`, `budgetId`, `displayOrder`, and derived `progress` | `budget/Expenditure.kt:5-27` |
| `Spending` | `id`, `comment`, `date`, `amount`, `expenditure`, `exchangeInfo`, `recurrence`, and derived `scheduledDate` | `budget/Spending.kt:10-50` |
| `ExchangeInfo` | `rawAmount`, `rate`, `currency` | `budget/Spending.kt:52-56` |
| `Recurrence` | `Never`, `Daily`, `Weekly`, `Monthly`, `Quarterly` | `budget/Spending.kt:61-63` |
| `Category` | `id`, `iconId`, `name`, `plan`, `isActive`, `description`, `merchants: List<Merchant>`, `displayOrder` | `budget/Category.kt:3-21` |
| `Message` | `id`, `sender`, `text`, `receivedAt`, `spendingId`, `status`, `isNew`. Status is `Recommended`, `Rejected` or `Matched(messageTemplateId, amount, merchant)`. | `message/Message.kt:7-31` |
| `MessageTemplate` | `id`, `sender`, `pattern`, `example`, `currency`, `isEnabled` | `message/MessageTemplate.kt:6-30` |
| `Pattern` | Text with `{amount}` (required) and `{merchant}` placeholders, at most 100 characters | `message/Pattern.kt:5-10`, `message/Pattern.kt:93-104` |
| `Qualifier` | `id`, `type` (`CATCH`, `SKIP`, `BLACKLIST`), `value` | `message/Qualifier.kt:3-14` |
| `Settings` | `period`, `currency`, `startDayOfMonth`, `startDayOfWeek`, `captureSms`, `captureNotifications`, `policyVersion`, `discoveryMode` | `settings/Settings.kt:8-38` |
| `Period` | `MONTH`, `WEEK`, `TWO_WEEKS`, `QUARTER`, `YEAR` | `settings/Settings.kt:29-31` |
| `DeferrableAction` | `key`, `activeAfter`, `increment`. Keys are `ProgressiveDaily` and `UniformDaily`. | `settings/DeferrableAction.kt:5-47` |
| Statistics types | `OverspendingAlert`, `TrendEntry`, `Share`, `Percentage`, `Type` | `analytics/Analytics.kt:5-35` |
| Backup models | `BackupFile` → `BudgetBackup` → `ExpenditureBackup`, `SpendingBackup` → `MessageBackup` | `backup/BackupModels.kt:6-56` |

**IDs.** Every repository's `create()` takes its id from the shared `generator` table,
using an `INSERT … REPLACE` that returns a new rowid
(`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/IdGeneratorDAO.kt:10-11`).
As a result, budgets, expenditures, spendings, categories, messages, templates and
qualifiers all draw from one id sequence. Sources:
- `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:27-30`
- `android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureRepositoryImpl.kt:13-15`
- `android/legacy/src/main/java/com/mas/mobile/repository/budget/SpendingRepositoryImpl.kt:13-15`
- `android/legacy/src/main/java/com/mas/mobile/repository/budget/CategoryRepositoryImpl.kt:25-27`
- `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageRepositoryImpl.kt:25-27`
- `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageTemplateRepositoryImpl.kt:28-30`
- `android/legacy/src/main/java/com/mas/mobile/repository/message/QualifierRepositoryImpl.kt:26-28`

### Domain services

All paths are under `android/legacy/src/main/java/com/mas/mobile/domain/`.

| Service | Responsibilities | Source |
|---|---|---|
| `BudgetService` | Resolve the active budget. If none covers today, create the next one. Compute period boundaries per `Period`. Record a spending (`spend`), creating the expenditure implicitly if needed. Fetch exchange rates. Materialise scheduled spendings. Seed a new budget's expenditures from active categories. Make budget names unique. | `budget/BudgetService.kt:23-270` |
| `CategoryService` | Find a category by merchant or name (case-insensitive). Persist a new display order after drag-reorder. | `budget/CategoryService.kt:7-39` |
| `MessageService` | Classify an incoming message (Matched / Recommended / Rejected). Store it. Create a spending for a matched message whose merchant belongs to a category. Promote a recommended message by generating a template, then re-match messages from the same sender. | `message/MessageService.kt:17-161` |
| `MessageTemplateService` | Build a template from a message via `MessageAnalyzer` and detect its currency | `message/MessageTemplateService.kt:8-35` |
| `QualifierService` | Recommendation heuristic: length < 250, sender not blacklisted, contains an amount and a currency, has a CATCH word and no SKIP word. Add a sender to the blacklist. | `message/QualifierService.kt:9-75` |
| `CurrencyExpert` | Detect currency codes and symbols in text (UC-L5.14) | `message/CurrencyExpert.kt:5-17` |
| `SettingsService` | Read/write discovery mode (`autodetect`), first-run flag, policy version, and "is capturing enabled" | `settings/SettingsService.kt:10-59` |
| `DeferrableActionService` | Check whether an action is deferred, defer it (progressive back-off), and purge old rows | `settings/DeferrableActionService.kt:8-36` |
| `AnalyticsService` | Statistics: trends (last 12 completed budgets), overspending alerts (last 3 completed budgets), expenditure distribution of one budget | `analytics/AnalyticsService.kt:10-68` |
| `BackupService` | Build the export model. Import budgets, skipping any whose name already exists. | `backup/BackupService.kt:20-160` |

### Repositories

All paths are under `android/legacy/src/main/java/com/mas/mobile/`.

| Interface | Implementation | Notes | Source |
|---|---|---|---|
| `BudgetRepository` | `BudgetRepositoryImpl` | Saves the budget row plus changed/deleted expenditures and spendings in one transaction. Changes are detected by an MD5 hash of each item's `toString()`. Deleting a spending clears the `spending_id` of its message. | `domain/budget/BudgetRepository.kt:7-28`, `repository/budget/BudgetRepositoryImpl.kt:50-91`, `repository/budget/BudgetRepositoryImpl.kt:137-155` |
| `ExpenditureRepository` | `ExpenditureRepositoryImpl` | `create()` and reads only. `getExpenditureNames` ignores its `sortByRating` and `limit` parameters. | `repository/budget/ExpenditureRepositoryImpl.kt:9-38` |
| `SpendingRepository` | `SpendingRepositoryImpl` | `create()` and live reads. The "from date" list drops spendings whose expenditure has `budgetId <= 0`. | `repository/budget/SpendingRepositoryImpl.kt:11-51` |
| `CategoryRepository` | `CategoryRepositoryImpl` | CRUD via upsert/delete | `repository/budget/CategoryRepositoryImpl.kt:13-56` |
| `MessageRepository` | `MessageRepositoryImpl` | Upsert/delete. `countUnreadLive(from)` ignores `from`. | `repository/message/MessageRepositoryImpl.kt:14-51` |
| `MessageTemplateRepository` | `MessageTemplateRepositoryImpl` | CRUD and `clone` (new id) | `repository/message/MessageTemplateRepositoryImpl.kt:15-48` |
| `QualifierRepository` | `QualifierRepositoryImpl` | CRUD. `create()` defaults to type SKIP. | `repository/message/QualifierRepositoryImpl.kt:12-40` |
| `SettingsRepository` | `SettingsRepositoryImpl` | Key/value rows. `remove` is `TODO("Not supported!")`. | `repository/settings/SettingsRepositoryImpl.kt:9-26` |
| `DeferrableActionRepository` | `DeferrableActionRepositoryImpl` | Get by key, upsert, remove, purge | `repository/settings/DeferrableActionRepositoryImpl.kt:10-30` |
| `ExchangeRepository` | `FreeCurrencyAPIRepositoryImpl` | HTTP (see §6) | `domain/budget/ExchangeRepository.kt:5-13`, `repository/budget/FreeCurrencyAPIRepositoryImpl.kt:14-53` |
| `MessageAnalyzer` | `GPTMessageAnalyzer` | HTTP (see §6) | `domain/message/MessageAnalyzer.kt:3-5`, `service/ai/GPTMessageAnalyzer.kt:9-43` |

### ViewModels

All paths are under `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/`.
Each ViewModel is created by an assisted-inject factory exposed on `AppComponent`
(`android/legacy/src/main/java/com/mas/mobile/MasApplication.kt:97-112`).

| ViewModel | Main collaborators | Source |
|---|---|---|
| `BudgetListViewModel` / `BudgetViewModel` | `BudgetRepository`, `BudgetService` | `BudgetListViewModel.kt:10-14`, `BudgetViewModel.kt:17-24` |
| `ExpenditureListViewModel` / `ExpenditureViewModel` | `BudgetService` | `ExpenditureListViewModel.kt:15-19`, `ExpenditureViewModel.kt:14-21` |
| `SpendingListViewModel` / `SpendingViewModel` | `BudgetService`, `SpendingRepository`, `MessageService`, `CategoryService` | `SpendingListViewModel.kt:11-18`, `SpendingViewModel.kt:26-41` |
| `CategoryListViewModel` / `CategoryViewModel` | `CategoryService` / `CategoryRepository` | `CategoryListViewModel.kt:10-14`, `CategoryViewModel.kt:19-26` |
| `MessageListViewModel` | `MessageRepository`, `SpendingRepository`, `QualifierService`, `SettingsService` | `MessageListViewModel.kt:15-22` |
| `MessageTemplateListViewModel` / `MessageTemplateViewModel` | `MessageTemplateRepository`, `SettingsService` | `MessageTemplateListViewModel.kt:11-15`, `MessageTemplateViewModel.kt:22-29` |
| `QualifierListViewModel` | `QualifierRepository` | `QualifierListViewModel.kt:15-20` |
| `SettingsViewModel` | `SettingsRepository`, `PermissionService`, `BackupService`, `BackupFileService` | `SettingsViewModel.kt:24-31` |
| `ChartViewModel` | `AnalyticsService`, `BudgetService` | `ChartViewModel.kt:20-26` |

## 4. Background work, receivers, listeners

All paths are under `android/legacy/src/main/java/com/mas/mobile/` unless shown in full.

| Component | Trigger | What it does | Source |
|---|---|---|---|
| `SmsListener` (BroadcastReceiver) | `SMS_RECEIVED`. Declared in the manifest with priority 999 and protected by `BROADCAST_SMS`. | Calls `MessageService.handleRawMessage(sender, body, now)` once per PDU in the intent | `android/legacy/src/main/AndroidManifest.xml:50-58`, `service/SmsListener.kt:20-38` |
| `NotificationListener` (NotificationListenerService) | Any posted notification. Declared in the manifest with `BIND_NOTIFICATION_LISTENER_SERVICE`. | Skips packages starting with `com.android.`. Uses the posting app's label as sender. Uses `title\ntext` as the text, or, when both are null, text read by reflection from the custom content view. Calls `handleRawMessage`. | `android/legacy/src/main/AndroidManifest.xml:59-66`, `service/NotificationListener.kt:21-79` |
| `DateListener` (BroadcastReceiver) | `ACTION_TIME_TICK`, registered at runtime in `MainActivity.onCreate` and unregistered in `onDestroy` | Reloads the active budget when the calendar date has changed since its last tick | `presentation/activity/MainActivity.kt:72`, `presentation/activity/MainActivity.kt:77-80`, `service/DateListener.kt:17-23` |
| `AppUpdateCheckWorker` (Worker) | Periodic every 2 h. Unique name `periodicUpdateCheck`, policy `REPLACE`. Enqueued in `MainActivity.onCreate`. | Asks Play for an update. If one is available and the action is not deferred, defers it, posts a notification and logs `app_update_suggested`. | `presentation/activity/MainActivity.kt:156-162`, `service/AppUpdateCheckWorker.kt:26-44` |
| `ScheduledSpendingWorker` (Worker) | Periodic every 2 h. Unique name `scheduledSpending`, policy `REPLACE`. Enqueued in `MainActivity.onCreate`. | Calls `BudgetService.createScheduledSpendings()` | `presentation/activity/MainActivity.kt:164-169`, `service/ScheduledSpendingWorker.kt:14-17`, `domain/budget/BudgetService.kt:131-152` |
| `NotificationService` | Called by `AppUpdateCheckWorker` | Creates channel `MasMoney`. Posts the only notification in the app ("new version", id 1) if `POST_NOTIFICATIONS` is granted. | `service/NotificationService.kt:25-81` |
| `CoroutineService` (`TaskService`) | Called by services | `backgroundTask` runs on `Dispatchers.IO` and shows a toast on error. `blockingTask` runs `runBlocking`. | `service/CoroutineService.kt:11-25`, `service/ErrorHandler.kt:20-29` |
| `BudgetService` init | First injection of the singleton | Calls `reloadActiveBudget()` | `domain/budget/BudgetService.kt:165-167` |

`ProgressiveDaily` deferral, used for update notifications: the first interval is 24 h
and doubles on each deferral until the increment reaches 16 days
(`android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableAction.kt:12-20`,
`android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableAction.kt:34-42`).
Rows whose `active_after` is older than 30 days are purged on each defer
(`android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableActionService.kt:16-23`,
`android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableActionService.kt:33-35`).

## 5. Persistence

### Room database

- File `mas_home.db`, schema version 10, 10 registered entities
  (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:34-49`,
  `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:58`).
- Main-thread queries are allowed
  (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:59`).
- Value encoding: `LocalDate` is stored as an epoch day, `LocalDateTime` as epoch
  milliseconds in the device time zone, `Boolean` as 0/1, and `List<String>` as a Gson
  JSON array (empty string = empty list).
  Sources: `android/legacy/src/main/java/com/mas/mobile/repository/db/config/converter/SQLiteTypeConverter.kt:14-66`,
  `android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:49-52`.

Tables, as in the v10 schema export. `S10` = `android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/10.json`.
Entity files are under `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/`.

| Table | Columns | Keys and indices | Entity | DDL |
|---|---|---|---|---|
| `budgets` | `id` INTEGER PK autoincrement, `name` TEXT, `plan` REAL, `fact` REAL, `startsOn` INTEGER, `lastDayAt` INTEGER, `isActive` INTEGER, `comment` TEXT NULL, `currency` TEXT | unique `index_budgets_name(name)` | `Budget.kt:12-41` | `S10:9`, `S10:80` |
| `expenditures` | `id` PK autoincrement, `name`, `plan`, `fact`, `comment` NULL, `budget_id`, `icon` NULL, `display_order` | FK `budget_id → budgets.id` ON DELETE CASCADE. Indices: `(budget_id)`, `(budget_id, display_order, name)`, `(name)` | `Expenditure.kt:40-77` | `S10:177`, `S10:242`, `S10:253`, `S10:262` |
| `spendings` | `id` PK autoincrement, `comment` NULL, `date` INTEGER, `amount` REAL, `expenditure_id`, `currency` NULL, `rate` NULL, `foreignAmount` NULL, `recurrence` TEXT | FK `expenditure_id → expenditures.id` ON DELETE CASCADE. Index `(expenditure_id)` | `Spending.kt:41-78` | `S10:87`, `S10:158` |
| `spending_messages` | `id` PK autoincrement, `name` (sender), `message`, `receivedAt`, `rule_id`, `spending_id` NULL, `suggested_expenditure_name` NULL, `suggested_merchant` NULL, `suggested_amount`, `is_new`, `status` | No FK on `spending_id` or `rule_id` | `SpendingMessage.kt:9-49` | `S10:281` |
| `categories` | `id` PK (not autoincrement), `name`, `plan`, `active`, `description`, `merchants` (JSON list), `icon` NULL, `display_order` | Indices `(name)`, `(display_order, name)` | `Category.kt:8-40` | `S10:538`, `S10:603`, `S10:613` |
| `message_template` | `id` PK autoincrement, `sender`, `pattern`, `example`, `currency`, `enabled` | — | `MessageTemplate.kt:9-29` | `S10:488` |
| `qualifiers` | `id` PK, `name`, `type` (1 = CATCH, 2 = SKIP, 3 = BLACKLIST) | Index `(type, name)` | `Qualifier.kt:10-30` | `S10:407`, `S10:443` |
| `settings` | `key` TEXT PK, `value` TEXT NULL | — | `Settings.kt:7-15` | `S10:361` |
| `deferrable_actions` | `key` TEXT PK, `type` (1 = UNIFORM, 2 = PROGRESSIVE), `increment` (hours), `active_after` | — | `DeferrableAction.kt:8-27` | `S10:450` |
| `generator` | `id` PK autoincrement | Id sequence (see §3) | `IdGenerator.kt:7-12` | `S10:387` |

Relations and links not enforced by the schema:
- `spending_messages.spending_id` → `spendings.id` and `spending_messages.rule_id` →
  `message_template.id` are plain integer columns
  (`android/legacy/src/main/java/com/mas/mobile/repository/db/entity/SpendingMessage.kt:24-28`,
  `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageMapper.kt:17-22`).
- Categories and expenditures are linked only by matching `name` case-insensitively
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:154-163`,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:169-180`).
- Merchants are not a table. They are a JSON string list inside `categories.merchants`
  (`android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Category.kt:32-33`).
- `spending_messages.status` stores only `MATCHED` or `RECOMMENDED`. `Rejected` messages
  are not stored
  (`android/legacy/src/main/java/com/mas/mobile/repository/message/MessageMapper.kt:18-26`,
  `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageMapper.kt:37`,
  `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:32-48`).

Columns or entities present in code but not used:
- `budgets.isActive` is not mapped by `BudgetMapper`, so saves always write the entity
  default `false`
  (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetMapper.kt:23-33`,
  `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Budget.kt:33-34`).
- `spending_messages.suggested_expenditure_name` is not mapped by `MessageMapper`
  (`android/legacy/src/main/java/com/mas/mobile/repository/message/MessageMapper.kt:30-44`).
- Entities `ExpenseReport` (table `expenses`) and `Notification` (table `notifications`)
  are declared but not registered in `@Database`, and nothing references them
  (`android/legacy/src/main/java/com/mas/mobile/repository/db/entity/ExpenseReport.kt:6-12`,
  `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Notification.kt:8-16`,
  `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:37-48`).

### Special rows and seed data

The `onCreate` callback, which runs on a fresh install, executes the seed SQL from `DML`
(`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:60-69`):

| Seed | Content | Source (`…/repository/db/config/DML.kt`) |
|---|---|---|
| Scheduled budget | `budgets` row with id −2, name `Scheduled`, `startsOn` and `lastDayAt` = `LocalDate.MIN`, currency = system currency | `DML.kt:30-33` |
| Id generator | `generator` row with id 100 | `DML.kt:25-28` |
| Qualifiers | English CATCH / SKIP / BLACKLIST words (ids 1–38), plus extra words when the device language is `UA`, `RU` or `ES` | `DML.kt:46-134` |
| Message templates | 4 bank templates (ids 1–4) | `DML.kt:35-44` |
| Categories | 19 categories (ids 1–19) with plan, icon and display order. `Auto`, `Childcare`, `Insurance` and `Pet care` are inactive. | `DML.kt:136-175` |

Full path of `DML.kt`: `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt`.

All budget queries except by-id, by-name and by-spending-id filter on `id > 0`, which hides the
scheduled budget
(`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:10-35`).

### Migrations

All migrations are in `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt`.
Schema exports exist only for versions 5–10
(`android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/`).

| Step | Change | Lines |
|---|---|---|
| 1→2 | Create `generator` and insert id 1000 | 96-101 |
| 2→3 | Add `spending_messages.status`, default `'MATCHED'` | 103-107 |
| 3→4 | Set `startsOn` of budget id 1 to `LocalDate.MAX` | 109-115 |
| 4→5 | Create `qualifiers(name PK, type)` and seed it | 117-124 |
| 5→6 | Create indices `index_expenditures_on_budget_id` and `index_spendings_on_expenditure_id` | 126-131 |
| 6→7 | Add `currency` to `budgets` and `message_rules` (default = system currency at migration time). Add `currency`, `rate`, `foreignAmount` to `spendings`. | 133-142 |
| 7→8 | Create `deferrable_actions` | 144-148 |
| 8→9 | Create and seed `message_template`. Rebuild `qualifiers` with integer ids (`ROW_NUMBER() OVER (ORDER BY name)`). Create and seed `categories`. Add `expenditures.icon`. Delete budget id 1 and its expenditures. Add `spending_messages.suggested_merchant`. Drop `message_rules`. Add `spendings.recurrence` (default `'Never'`). Insert the scheduled budget. | 150-202 |
| 9→10 | Add `display_order` to `categories` and `expenditures`, with new indices | 204-212 |

Removed table `message_rules` (v5–v8): `id`, `name`, `amount_matcher`,
`expenditure_matcher`, `expenditure_name`, `currency`
(`android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/8.json:316-317`).

### SharedPreferences

| File | Key | Use | Source |
|---|---|---|---|
| `BuildConfig.APPLICATION_ID` | `firstRun` (Boolean, default `true`) | `isFirstLaunch` / `completeFirstLaunch` | `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:26-37`, `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:56` |

No code outside `SettingsService` calls `isFirstLaunch` or `completeFirstLaunch`.

### Settings table keys

Keys from `android/legacy/src/main/java/com/mas/mobile/repository/settings/SettingsMapper.kt:43-50`.
Defaults from `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:8-27`.

| Key | Model field | Default when the key is missing |
|---|---|---|
| `budget.period` | `period` | `MONTH` |
| `budget.currency` | `currency` | System currency (`android/legacy/src/main/java/com/mas/mobile/util/CurrencyTools.kt:7-19`) |
| `budget.startDayOfMonth` | `startDayOfMonth` | Today's day of month, capped at 28 |
| `budget.startDayOfWeek` | `startDayOfWeek` | Today's day of week |
| `budget.captureSMS` | `captureSms` | `false` |
| `budget.captureNotifications` | `captureNotifications` | `false` |
| `policy.version` | `policyVersion` | `""` |
| `message.discoveryMode` | `discoveryMode` | `true` |

### Files and platform backup

- Backup export/import writes and reads a JSON file chosen through the system document
  picker. The suggested name is `mashome-backup-yyyyMMdd.json`.
  Sources: `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:42-47`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:131-137`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:151-152`,
  `android/legacy/src/main/java/com/mas/mobile/service/BackupFileService.kt:12-21`.
- The backup format has `schemaVersion = 1`. It contains budgets with their
  expenditures, spendings and linked messages, but not categories, templates, qualifiers
  or settings
  (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupModels.kt:6-54`,
  `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:27-31`).
- The manifest sets `android:allowBackup="true"`
  (`android/legacy/src/main/AndroidManifest.xml:18`).

### Remote storage

There is no Firestore or other remote data store. Searching the sources for `Firestore`
finds nothing, and Firebase is used only for Analytics (§6).

## 6. Remote usage

API keys and model ids are not reproduced here. Only their locations are given.

| Integration | Endpoint / API | Credential | Where used | Source |
|---|---|---|---|---|
| FreeCurrencyAPI | `GET https://api.freecurrencyapi.com/v1/latest` with query `apikey`, `base_currency`, `currencies` | `BuildConfig.FREE_CURRENCY_API_KEY`, read from `local.properties` key `FREE_CURRENCY_API_KEY` | `BudgetService.getRate`. Called from `spend` and `SpendingViewModel`. On failure: toast and rate `0.00`. | `android/legacy/src/main/java/com/mas/mobile/repository/budget/FreeCurrencyAPIRepositoryImpl.kt:16-46`, `android/legacy/build.gradle:23-26`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:108-129`, `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:186-190` |
| OpenAI chat completions | `POST https://api.openai.com/v1/chat/completions`, temperature 0.2, fine-tuned model id hard-coded | Bearer token from `BuildConfig.GPT_API_KEY` (`local.properties` key `GPT_API_KEY`) | `GPTMessageAnalyzer.buildPattern`. Sends the full message text with a fixed system prompt. Triggered only by `MessageService.promoteRecommendedMessage`, which is called from `SpendingViewModel`. | `android/legacy/src/main/java/com/mas/mobile/service/ai/GptChatApiClient.kt:9-28`, `android/legacy/src/main/java/com/mas/mobile/service/ai/GptChatConnector.kt:10-17`, `android/legacy/src/main/java/com/mas/mobile/service/ai/GPTMessageAnalyzer.kt:22-41`, `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:56-61`, `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:227` |
| Firebase Analytics | `logEvent(name, params)` | Google Services plugin config | `FirebaseEventLogger`. Events: `message_evaluated`, `spending_modified`, `scheduled_modified`, `message_template_modified`, `qualifier_modified`, `category_modified`, `message_template_failed`, `app_update_suggested`, `scheduled_budget_not_found`, `chart_shown` | `android/legacy/src/main/java/com/mas/mobile/util/FirebaseEventLogger.kt:13-26`, `android/legacy/src/main/java/com/mas/mobile/domain/analytics/Event.kt:5-114` |
| Firebase Crashlytics | — | Google Services plugin config | Gradle plugin and dependency only. No Kotlin references. | `android/legacy/build.gradle:8`, `android/legacy/build.gradle:150` |
| Google Play In-App Update | `AppUpdateManager.appUpdateInfo` | — | `AppUpdateCheckWorker`. The notification opens `market://details?id=<package>` in the Play Store. | `android/legacy/src/main/java/com/mas/mobile/service/AppUpdateCheckWorker.kt:27-31`, `android/legacy/src/main/java/com/mas/mobile/service/NotificationService.kt:25-37` |
| Project wiki link | `MasApplication.HOME_PAGE` (a GitHub wiki URL) | — | Settings "What is" row opens it in a browser | `android/legacy/src/main/java/com/mas/mobile/MasApplication.kt:81`, `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:109-113` |

Name/argument mismatch: `FreeCurrencyAPIRepositoryImpl.getRate(base, foreign)` sends
`foreign` as `base_currency` and `base` as `currencies`, then reads `data[base]`
(`android/legacy/src/main/java/com/mas/mobile/repository/budget/FreeCurrencyAPIRepositoryImpl.kt:24-28`,
`android/legacy/src/main/java/com/mas/mobile/repository/budget/FreeCurrencyAPIRepositoryImpl.kt:38-44`).

## 7. Manifest permissions

Manifest: `android/legacy/src/main/AndroidManifest.xml`.

| Permission / declaration | Manifest line | Code that uses it |
|---|---|---|
| `INTERNET` | 8 | FreeCurrencyAPI and OpenAI clients (§6), Firebase. `PermissionService.isInternetAllowed()` exists (`android/legacy/src/main/java/com/mas/mobile/service/PermissionService.kt:22-26`) but has no callers. |
| `RECEIVE_SMS` | 9 | `SmsListener`. Requested at runtime when the user turns on "capture SMS" in Settings (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:39-41`, `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:187-189`, `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:101-108`). Checked by `PermissionService.isSMSAllowed()` (`android/legacy/src/main/java/com/mas/mobile/service/PermissionService.kt:16-20`). |
| `POST_NOTIFICATIONS` | 10 | Checked before posting the update notification (`android/legacy/src/main/java/com/mas/mobile/service/NotificationService.kt:55-59`). No code requests it at runtime. |
| `BROADCAST_SMS` (receiver protection) | 54 | Only system SMS broadcasts reach `SmsListener` |
| `BIND_NOTIFICATION_LISTENER_SERVICE` (service protection) | 61 | `NotificationListener`. The user grants access in system settings, which Settings opens (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:191-203`). Checked by `PermissionService.isNotificationsAllowed()` (`android/legacy/src/main/java/com/mas/mobile/service/PermissionService.kt:28-37`). |
| `uses-feature telephony`, `required="false"` | 4-6 | — |
| `<queries>` for `MAIN`/`LAUNCHER` intents | 11-16 | The only code that lists installed apps is `NotificationListener.extractSender` (`android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:70-74`) |

## 8. Proposed functional areas for Stage 2

The numbering follows your expected list. Area 9 is new.

All paths are under `android/legacy/src/main/java/com/mas/mobile/` unless shown in full.

| # | Area (file) | Scope | Main files |
|---|---|---|---|
| 1 | Budgets and periods (`use-cases/budgets.md`) | Active budget resolution, automatic next budget, period types and start day, truncation comment, gaps, name uniqueness, scheduled budget (id −2) and scheduled spendings, budget list/edit/delete | `domain/budget/Budget.kt`, `domain/budget/BudgetService.kt`, `repository/budget/BudgetRepositoryImpl.kt`, `repository/db/dao/BudgetDAO.kt`, `presentation/viewmodel/BudgetViewModel.kt`, `presentation/viewmodel/BudgetListViewModel.kt`, `presentation/activity/fragment/BudgetFragment.kt`, `presentation/activity/fragment/BudgetListFragment.kt`, `domain/budget/Spending.kt` (`scheduledDate`) |
| 2 | Expenditures (`use-cases/expenditures.md`) | Creation from categories, manual and implicit creation, name uniqueness, display order, plan/fact/progress | `domain/budget/Expenditure.kt`, `domain/budget/Budget.kt`, `domain/budget/BudgetService.kt`, `presentation/viewmodel/ExpenditureViewModel.kt`, `presentation/viewmodel/ExpenditureListViewModel.kt`, `presentation/activity/fragment/ExpenditureFragment.kt`, `presentation/activity/fragment/ExpenditureListFragment.kt`, `presentation/adapter/ExpenditureAdapter.kt`, `presentation/adapter/BudgetExpenditureAdapter.kt` |
| 3 | Spendings and currency exchange (`use-cases/spendings.md`) | Add/edit/clone/delete, dates, recurrence, foreign currency and rates, link to messages | `domain/budget/Spending.kt`, `presentation/viewmodel/SpendingViewModel.kt`, `presentation/viewmodel/SpendingListViewModel.kt`, `presentation/activity/fragment/SpendingFragment.kt`, `presentation/activity/fragment/SpendingListFragment.kt`, `repository/budget/SpendingRepositoryImpl.kt`, `repository/budget/FreeCurrencyAPIRepositoryImpl.kt`, `util/CurrencyTools.kt` |
| 4 | Categories and merchants (`use-cases/categories.md`) | Category CRUD, activation, icons, reorder, merchant lists, effect on new budgets | `domain/budget/Category.kt`, `domain/budget/CategoryService.kt`, `repository/budget/CategoryRepositoryImpl.kt`, `presentation/viewmodel/CategoryViewModel.kt`, `presentation/viewmodel/CategoryListViewModel.kt`, `presentation/activity/fragment/CategoryFragment.kt`, `presentation/activity/fragment/CategoryListFragment.kt`, `presentation/activity/IconPickerActivity.kt` |
| 5 | Automatic capture (`use-cases/capture.md`) | SMS and notification intake, template matching, `Pattern` parsing, qualifiers and discovery mode, message statuses, GPT template generation, currency detection, manual matching from the message list, blacklist, message deletion, unit-test coverage of `domain/message` | `service/SmsListener.kt`, `service/NotificationListener.kt`, `domain/message/*.kt`, `android/legacy/src/test/java/com/mas/mobile/domain/message/*Test.kt`, `service/ai/*.kt`, `repository/message/*.kt`, `presentation/viewmodel/MessageListViewModel.kt`, `presentation/viewmodel/MessageTemplateViewModel.kt`, `presentation/viewmodel/MessageTemplateListViewModel.kt`, `presentation/viewmodel/QualifierListViewModel.kt`, `presentation/activity/fragment/Message*Fragment.kt`, `presentation/activity/fragment/QualifierListFragment.kt`, `presentation/adapter/SpendingMessageAdapter.kt` |
| 6 | Settings, onboarding, privacy policy, analytics events (`use-cases/settings.md`) | Settings screen and storage, permissions flow, policy gate, first-run flag, Firebase events | `domain/settings/Settings.kt`, `domain/settings/SettingsService.kt`, `repository/settings/*.kt`, `presentation/viewmodel/SettingsViewModel.kt`, `presentation/activity/fragment/SettingsFragment.kt`, `presentation/activity/PolicyActivity.kt`, `service/PermissionService.kt`, `domain/analytics/Event.kt`, `util/FirebaseEventLogger.kt`, `android/legacy/src/main/res/raw/` |
| 7 | Background work and notifications (`use-cases/background.md`) | Worker scheduling, date-change reload, app-update check, deferrable actions, notification channel | `presentation/activity/MainActivity.kt`, `service/AppUpdateCheckWorker.kt`, `service/ScheduledSpendingWorker.kt`, `service/DateListener.kt`, `service/NotificationService.kt`, `domain/settings/DeferrableAction*.kt` |
| 8 | Statistics (`use-cases/statistics.md`) | Trends, overspending alerts, expenditure distribution charts | `domain/analytics/AnalyticsService.kt`, `domain/analytics/Analytics.kt`, `presentation/viewmodel/ChartViewModel.kt`, `presentation/activity/fragment/ChartFragment.kt`, `presentation/activity/fragment/AnalyticsFragment.kt` |
| 9 | Backup export/import (`use-cases/backup.md`), **new** | JSON export of budgets, import with name-based skip, id remapping, message re-creation | `domain/backup/*.kt`, `service/BackupFileService.kt`, `presentation/viewmodel/SettingsViewModel.kt`, `presentation/activity/fragment/SettingsFragment.kt` |

Overlaps:
- **Scheduled spendings.** The rules are covered in area 1. Area 7 covers only the
  worker trigger.
- **Firebase events.** Each area lists the events it emits. Area 6 holds the catalogue.

Suggested order: 1 → 2 → 3 → 4 → 5 → 9 → 8 → 6 → 7. The core budget data model comes
first, then capture, which builds on it. Backup touches every core entity, so it follows
them.

## 9. Open questions

1. `res/menu/right_side_menu.xml` has no references, and its ids are not graph
   destinations (§2). Is it dead code? The code doesn't say.
2. `action_to_budget_template` (`android/legacy/src/main/res/navigation/nav_graph.xml:238-245`)
   defaults to `budget_id` 1 and has no call site. Budget id 1 is deleted by migration
   8→9 (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:195-196`).
   Its intended use is unknown.
3. `google-services.json` is not in `android/legacy/`, but the Google Services plugin is
   applied (`android/legacy/build.gradle:7`). How Firebase is configured in real builds
   cannot be seen.
4. There are no schema exports for versions 1–4, so the original tables (`budgets`,
   `expenditures`, `spendings`, `spending_messages`, `settings`) at v1 are known only
   from later exports.
5. Migration 8→9 runs `BEGIN TRANSACTION` / `COMMIT` inside a Room migration
   (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:164-178`).
   The runtime effect cannot be confirmed from the code alone.
6. Migration 5→6 creates indices named `index_expenditures_on_budget_id` and
   `index_spendings_on_expenditure_id`
   (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:128-129`).
   The v6 export already contains `index_spendings_expenditure_id` and
   `index_expenditures_budget_id` on the same columns
   (`android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/6.json:128`,
   `android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/6.json:200`).
   Whether upgraded databases end up with duplicate indices is not visible from code.
7. The `captureSms` and `captureNotifications` settings are not read by `SmsListener`,
   `NotificationListener` or `MessageService.handleRawMessage`. Outside the Settings screen
   (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:101`,
   `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:110`),
   they are read only by `SettingsService.isMessageCapturingEnabled`
   (`android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:39-42`).
   **Answered in Stage 2:** no app code revokes the permissions or disables the
   listeners, so capture continues while the OS permission is granted, whatever the
   switch shows (UC-L5.1, UC-L5.2, UC-L6.4, UC-L6.5).
8. `POST_NOTIFICATIONS` is declared but never requested at runtime (§7). On API 33+ the
   update notification may never appear. This cannot be confirmed from code.
9. `SettingsService.isFirstLaunch` / `completeFirstLaunch` have no callers (§5).
   **Answered in Stage 2:** there is no onboarding beyond the policy gate and the guided
   capture prompt (UC-L6.8, UC-L6.6).
10. Both workers are enqueued with `ExistingPeriodicWorkPolicy.REPLACE` on every
    `MainActivity.onCreate`
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:156-170`).
    The effect on their schedule across app restarts depends on WorkManager behaviour,
    not on app code.
