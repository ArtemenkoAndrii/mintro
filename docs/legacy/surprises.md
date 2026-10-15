# Surprises

This file collects all 75 "Surprises" entries from the area files in one place. A surprise
is behaviour that looks unintended or inconsistent. It is stated as fact and cited, with
no fix proposed.

How the list was built:
- The entries are copied verbatim from the area files' "Surprises" sections, in their
  original order.
- Each entry has a stable id `S-L<area>.<n>`.
- The area file is the source of truth. Its use cases give the full context.

## Thematic index

Some entries appear under more than one theme.

| Theme | Entries |
|---|---|
| Records end up in the wrong budget or period | S-L1.1, S-L1.2, S-L1.4, S-L1.5, S-L3.6, S-L3.7, S-L5.8 |
| Currency handling | S-L1.3, S-L1.11, S-L3.3, S-L3.4, S-L3.8, S-L5.5, S-L5.15, S-L8.3 |
| Broken links between records, and partial writes | S-L1.9, S-L2.3, S-L3.1, S-L3.2, S-L9.4, S-L9.5 |
| Matching by name instead of by id | S-L2.4, S-L2.6, S-L3.9, S-L4.4, S-L4.5 |
| Validation and form behaviour | S-L1.6, S-L1.8, S-L1.10, S-L1.12, S-L2.1, S-L2.2, S-L2.5, S-L3.5, S-L4.3 |
| Message intake, parsing and classification | S-L5.1, S-L5.2, S-L5.3, S-L5.4, S-L5.6, S-L5.7, S-L5.9, S-L5.12, S-L5.16 |
| Seed data and migrations | S-L1.13, S-L4.1, S-L4.2, S-L5.14, S-L5.17 |
| Consent, privacy and settings | S-L5.10, S-L6.1, S-L6.2, S-L6.3, S-L6.4 |
| Background work reliability | S-L1.5, S-L7.1, S-L7.2, S-L7.3, S-L7.4 |
| Analytics events | S-L3.10, S-L4.6, S-L5.13, S-L8.5 |
| Statistics | S-L8.1, S-L8.2, S-L8.3, S-L8.4 |
| Backup | S-L9.1, S-L9.2, S-L9.3, S-L9.4, S-L9.5, S-L9.6 |
| Dead or unreachable code | S-L1.7, S-L2.7, S-L5.11, S-L6.5, S-L6.6, S-L7.5 |

## Area 1: Budgets and periods ([`use-cases/budgets.md`](use-cases/budgets.md))

- **S-L1.1** **Scheduled spendings: occurrences lost.** The occurrence on a template's own date is never materialised, and `Never` templates never are. Any occurrence with no worker run between its time and midnight is lost (UC-L1.10, `android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:28-49`).
- **S-L1.2** **Endless future budgets.** If no budget covers today and the last budget lies in the future, every active-budget lookup creates another future budget (UC-L1.2, `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:70-79`).
- **S-L1.3** **Scheduled spendings: double conversion.** Foreign-currency templates are converted twice when materialised (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:139-145`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:108-115`).
- **S-L1.4** **Scheduled spendings: wrong budget for past dates.** A materialised occurrence dated in a past period is added to the current active budget (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:97`).
- **S-L1.5** **Side-effect creation.** Looking up an exchange rate or opening the chart screen can create and save a new budget (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:124`, `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:69`).
- **S-L1.6** **Add form saves on open.** Opening the budget form in ADD mode saves a new budget before the user presses Save (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetViewModel.kt:47`).
- **S-L1.7** **Dead options menu.** The budget list handles `budget_list_menu` items, but the menu is never inflated (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:45-52`, `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ListFragment.kt:15-17`).
- **S-L1.8** **Inconsistent delete rule.** The list enforces "remove only from the end", but the form does not (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:66-74`, `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetFragment.kt:86-89`).
- **S-L1.9** **Dangling links after delete.** Deleting a budget leaves `spending_messages.spending_id` pointing at deleted spendings (UC-L1.7). The link is cleared only for spendings removed through a budget save (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:77-83`, `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/SpendingMessageDAO.kt:38-39`).
- **S-L1.10** **Plan overwritten.** A manually edited budget `plan` is overwritten by the next recalculation (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:74-83`).
- **S-L1.11** **No currency conversion.** Changing a budget's currency does not convert any amount (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetViewModel.kt:84-87`).
- **S-L1.12** **Clone loses recurrence.** Cloning a scheduled spending drops its recurrence (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:122-132`).
- **S-L1.13** **Category order shifted.** A category's `displayOrder` is copied to the new expenditure and then replaced by `max + 1`, where the max includes the copied value. Seeded orders 1–19 become 2–20 (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:176`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:37-38`).

## Area 2: Expenditures ([`use-cases/expenditures.md`](use-cases/expenditures.md))

- **S-L2.1** **Case-only rename blocked.** Renaming an expenditure only by letter case or surrounding whitespace fails with "Already exists" (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:99-105`).
- **S-L2.2** **Clone can't be saved as is.** A cloned expenditure keeps the source name, so saving it unchanged always fails (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:55-65`).
- **S-L2.3** **Dangling message links.** Deleting an expenditure cascades to its spendings, but their messages keep `spending_id` (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:77-83`).
- **S-L2.4** **No icons for manual expenditures.** Manually added expenditures never get an icon. Implicit ones get the icon only if a category has the same name (`android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureRepositoryImpl.kt:17`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:160`).
- **S-L2.5** **Inconsistent zero-plan progress.** Expenditure progress treats `plan = 0` as 100 %, but budget progress does not guard against zero (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Expenditure.kt:15-20`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:22`).
- **S-L2.6** **Untrimmed implicit names.** Implicit creation stores the untrimmed name, while the category lookup uses the trimmed one (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:155-159`).
- **S-L2.7** **Ignored arguments.** Name suggestions ignore their `sortByRating` and `limit` arguments (`android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureRepositoryImpl.kt:25-26`).

## Area 3: Spendings and currency exchange ([`use-cases/spendings.md`](use-cases/spendings.md))

- **S-L3.1** **Clone steals the message link.** Cloning a spending moves the source's captured message link to the clone (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:137-142`, `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:279-283`).
- **S-L3.2** **Silent no-op delete.** Deleting from the form uses value equality, so it can silently do nothing if the in-memory spending differs from the stored one (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:69-72`).
- **S-L3.3** **Wrong base currency.** Exchange rates are always relative to the active budget's currency, even for spendings in other budgets (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:124`).
- **S-L3.4** **Silent zero amounts.** A failed rate lookup turns the amount into 0.00, with only a toast (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:124-128`).
- **S-L3.5** **Amount not validated.** Amount is never validated, so 0 and negative values are accepted. The `amountError`, `dateError` and `commentError` fields are never set (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:51-55`).
- **S-L3.6** **Budget ignores date.** A spending's budget is chosen by context (active budget, or the source's budget), never by its date. Dates outside the budget range are allowed (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:55-67`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:97`).
- **S-L3.7** **Clone into past budget.** Cloning a spending from a past budget creates a spending dated now inside that past budget (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:127`).
- **S-L3.8** **Inconsistent triple.** Editing the amount of a foreign spending leaves `rawAmount` and `rate` unchanged, so the stored triple becomes inconsistent (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:156-159`).
- **S-L3.9** **Lost merchant.** A merchant the user agreed to link is dropped if no category has the expenditure's name (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:285-291`).
- **S-L3.10** **Missing analytics.** Deleting from the list does not log an analytics event, but deleting from the form does (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingListViewModel.kt:37-54`, `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:261-264`).

## Area 4: Categories and merchants ([`use-cases/categories.md`](use-cases/categories.md))

- **S-L4.1** **Broken migration.** Migration 8→9 seeds categories with a `display_order` value before that column exists (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:181-192`, `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:137`).
- **S-L4.2** **Reorder doesn't stick.** Drag-to-reorder does not persist when display orders are equal, for example all 0 after migration 9→10 (`android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:22-35`, `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:206`).
- **S-L4.3** **Clone can't be saved as is.** A cloned category keeps the source name, so saving it unchanged fails. The icon and merchants are not copied (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:109-120`).
- **S-L4.4** **Inactive still matches.** Inactive categories still match merchants, and still supply plan and icon to implicit expenditures. Only new-budget seeding filters on `isActive` (`android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:12-20`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:154-162`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:170`).
- **S-L4.5** **Shared merchants.** A merchant can be listed under several categories, and only the first in display order is used (`android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:12-15`).
- **S-L4.6** **Missing analytics.** Deleting from the list does not log `category_modified`, but deleting from the form does (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ListViewModel.kt:12-16`, `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:132-135`).

## Area 5: Automatic capture ([`use-cases/capture.md`](use-cases/capture.md))

- **S-L5.1** **Bare `{amount}` crashes the fallback.** A pattern of exactly `{amount}`, the default `Pattern.SIMPLE`, throws `StringIndexOutOfBoundsException` whenever its sender's text does not start with a number. This aborts classification of that message. Other patterns without `{merchant}` lose their first 9 characters in the fallback (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:54-63`).
- **S-L5.2** **Thousands separators misparsed.** `1,234.56` is either read as 1.234 or not parsed at all, depending on the pattern (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:91`, `android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:99`).
- **S-L5.3** **Multi-part SMS split.** A multi-part SMS is processed as separate messages, one per PDU (`android/legacy/src/main/java/com/mas/mobile/service/SmsListener.kt:27-35`).
- **S-L5.4** **Literal "null" text.** Notifications with only a title or only a text get the literal `null` in the stored text. An unknown app gives the sender `"null"` (`android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:26`, `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:33`).
- **S-L5.5** **"Fr" passes the currency check.** CHF's symbol `Fr` makes any text containing "fr" pass the currency check (`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:66`).
- **S-L5.6** **Whole amounts not recommended.** Amounts without decimals fail the recommendation heuristic, unless some other `digit[.,]digit` appears in the text (`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:55`).
- **S-L5.7** **Disabled templates suppress.** A disabled template that parses a message makes it Rejected, so the message is not recommended either (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:105-113`).
- **S-L5.8** **Manual matching drops time and currency.** Manual matching of a Matched message uses the current time and the budget currency, not the message time and template currency (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:169-180`).
- **S-L5.9** **Siblings get no spending.** Promoted sibling messages become Matched, but no spending is created for them (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:85-96`).
- **S-L5.10** **Message text in logs.** The full message text is written to the debug log, and sent to OpenAI on promotion (`android/legacy/src/main/java/com/mas/mobile/service/ai/GPTMessageAnalyzer.kt:28`, `android/legacy/src/main/java/com/mas/mobile/service/ai/GPTMessageAnalyzer.kt:33`).
- **S-L5.11** **Templates can't be added manually.** There is no screen action to add a template. Template clone exists in code but opens read-only and is unreachable (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageTemplateViewModel.kt:134-138`).
- **S-L5.12** **Badge counts beyond the list.** The unread badge counts messages older than the 30-day list window (`android/legacy/src/main/java/com/mas/mobile/repository/message/MessageRepositoryImpl.kt:38-39`).
- **S-L5.13** **Wrong delete event.** Qualifier deletion logs a `Save` event (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/QualifierListViewModel.kt:57`).
- **S-L5.14** **ING seed template broken.** Seed template 4 cannot parse its own example, because a literal account suffix is part of the pattern (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:42-43`).
- **S-L5.15** **Ends-in-digits means a `$` currency.** Currency detection treats any text ending in digits as containing the currency whose locale symbol is `$` (USD under en-US), because one regex part is not escaped (`android/legacy/src/main/java/com/mas/mobile/domain/message/CurrencyExpert.kt:15`).
- **S-L5.16** **Edge paths untested.** The unit tests assert none of the edge paths listed in "Test coverage of `domain/message`" (`android/legacy/src/test/java/com/mas/mobile/domain/message/PatternTest.kt:10-120`, `android/legacy/src/test/java/com/mas/mobile/domain/message/MessageServiceTest.kt:101-162`).
- **S-L5.17** **Ukrainian words never seeded.** The Ukrainian seed words are guarded by `"UA"`, which never matches Java's `uk` (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:87`).

## Area 6: Settings, onboarding, privacy policy, analytics ([`use-cases/settings.md`](use-cases/settings.md))

- **S-L6.1** **Capture switches don't stop capture.** Turning a capture switch off does not stop capture. The stored flag keeps the permission state, and the listeners never read it (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:102-117`, `android/legacy/src/main/java/com/mas/mobile/service/SmsListener.kt:20-38`, `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:21-38`).
- **S-L6.2** **Discovery switch not on Settings.** The Settings screen has no discovery-mode switch, although the ViewModel has the field (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:119-123`).
- **S-L6.3** **Nothing waits for consent.** Workers, the date listener, capture and analytics all run without waiting for policy consent (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:50-74`, `android/legacy/src/main/java/com/mas/mobile/service/SmsListener.kt:20-38`, `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:21-38`, `android/legacy/src/main/java/com/mas/mobile/util/FirebaseEventLogger.kt:16-26`).
- **S-L6.4** **Stale settings.** Every save writes all keys, so a stale `SettingsViewModel` copy can overwrite a newer discovery-mode value (`android/legacy/src/main/java/com/mas/mobile/repository/settings/SettingsRepositoryImpl.kt:14-18`).
- **S-L6.5** **Dead code.** The first-run flag, `CommonFragment.logEvent` and `privacy_policy_v2.html` are unused. A search finds no reference to the v2 file (`android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:26-37`, `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CommonFragment.kt:51-53`).
- **S-L6.6** **Missing "What is" guard.** The "What is" link has no guard for a missing browser, unlike the notification-settings intent (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:109-113`).

## Area 7: Background work and notifications ([`use-cases/background.md`](use-cases/background.md))

- **S-L7.1** **Success too early.** The scheduled-spending worker reports success without waiting for its coroutine to complete, so failures are never retried (`android/legacy/src/main/java/com/mas/mobile/service/ScheduledSpendingWorker.kt:14-17`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:131-132`).
- **S-L7.2** **Permission never requested.** The only notification depends on a permission the app never requests (`android/legacy/src/main/java/com/mas/mobile/service/NotificationService.kt:55-59`).
- **S-L7.3** **Workers need a launch.** Workers are first enqueued only when the main screen opens. No other app code, such as a boot receiver, enqueues them (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:74`, `android/legacy/src/main/AndroidManifest.xml:26-66`).
- **S-L7.4** **Save errors after closing.** Saves run as untracked background coroutines. A save error appears as a generic toast after the form has closed (`android/legacy/src/main/java/com/mas/mobile/service/CoroutineService.kt:11-25`).
- **S-L7.5** **Unused code.** `UniformDaily`, `DeferrableActionService.remove` and `blockingTask` are not used by any feature (`android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableAction.kt:44-47`, `android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableActionService.kt:25-31`, `android/legacy/src/main/java/com/mas/mobile/service/CoroutineService.kt:17-21`).

## Area 8: Statistics ([`use-cases/statistics.md`](use-cases/statistics.md))

- **S-L8.1** **Filter text mismatch.** The distribution chart opens on the active budget, while the filter field shows a different, completed budget's name (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:89-92`).
- **S-L8.2** **Unplanned always alerted.** Unplanned expenditures (plan 0) that appear in 3 completed budgets are always reported as 120 % overspent (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:45-50`).
- **S-L8.3** **No currency conversion.** All statistics mix currencies without conversion (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:14-62`).
- **S-L8.4** **Case-sensitive grouping.** Overspending groups by exact name, while trends filter ignoring case (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:21`, `android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:40`).
- **S-L8.5** **Repeated events.** `chart_shown` is logged on open and again on every filter change, because each `run()` logs it (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:41-67`).

## Area 9: Backup export and import ([`use-cases/backup.md`](use-cases/backup.md))

- **S-L9.1** **Status and read flag lost.** Imported messages lose their status, which becomes `Recommended`, and their unread flag, which becomes read (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:105-113`).
- **S-L9.2** **No configuration in the backup.** Categories, merchants, message templates, qualifiers, settings and scheduled spendings are not part of the backup (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:27-31`).
- **S-L9.3** **Name-only duplicate check.** Duplicates are detected by budget name only, not by dates. Locale-dependent names can produce two budgets for one period (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:38`).
- **S-L9.4** **Partial import, no detail.** A failure part-way leaves a partial import, and the user sees only a generic failure message (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:159-165`).
- **S-L9.5** **Orphan messages.** Messages are written one by one before their budget, so any failure later in that budget leaves orphan messages (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:97-114`).
- **S-L9.6** **Unchecked version.** `schemaVersion` is exported but never checked on import (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupModels.kt:7`).
