# Area 9: Backup export and import

Scope:
- The manual JSON export and import of budgets from the Settings screen.
- The backup file format.
- Platform backup settings.

All citations use paths relative to the repository root.

## UC-L9.1: Export budgets to a JSON file

- **Actor:** user
- **Trigger:** Settings → "Export budgets"
  (`android/legacy/src/main/res/layout/settings_fragment.xml:291-307`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:131-133`).
- **Preconditions:** None.
- **Main flow:**
  1. Open the system "create document" picker, for type `application/json`, with the
     suggested name `mashome-backup-yyyyMMdd.json` (today's date)
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:42-44`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:151-152`).
  2. Once the user picks a location, show the progress bar and disable both backup rows
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:142-143`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:139-143`).
  3. In the background, build the export model from every budget with `id > 0`, including
     each budget's expenditures and spendings
     (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:27-31`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:13-14`).
     Serialise it with Gson, writing dates as ISO strings
     (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupJson.kt:11-18`).
  4. Write the UTF-8 bytes to the chosen document
     (`android/legacy/src/main/java/com/mas/mobile/service/BackupFileService.kt:12-16`).
  5. Show "Budgets exported successfully.", or "Something went wrong while processing the
     backup file." on any failure. Hide the progress bar
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:144-153`,
     `android/legacy/src/main/res/values/strings.xml:120`,
     `android/legacy/src/main/res/values/strings.xml:122`).
- **Rules:** The file format (`schemaVersion` 1) is shown in the table below.

  | Object | Fields | Source |
  |---|---|---|
  | `BackupFile` | `schemaVersion` (1), `exportedAt`, `budgets[]` | `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupModels.kt:6-10` |
  | `BudgetBackup` | `id`, `name`, `plan`, `fact`, `startsOn`, `lastDayAt`, `comment`, `currency` (code), `expenditures[]`, `spendings[]` | `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupModels.kt:12-23`, `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:117-128` |
  | `ExpenditureBackup` | `id`, `name`, `iconId`, `plan`, `fact`, `comment`, `displayOrder` | `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupModels.kt:25-33` |
  | `SpendingBackup` | `id`, `comment`, `date`, `amount`, `expenditureId`, `exchangeCurrency`, `exchangeRate`, `exchangeRawAmount`, `recurrence`, `messages[]` | `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupModels.kt:35-46` |
  | `MessageBackup` | `id`, `sender`, `text`, `receivedAt`, `isNew` | `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupModels.kt:48-54` |

  - Each spending carries at most one message: the one linked to it by `spending_id`
    (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:150`,
    `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/SpendingMessageDAO.kt:17-18`).
  - The file is plain JSON, with no encryption
    (`android/legacy/src/main/java/com/mas/mobile/service/BackupFileService.kt:12-16`).
- **Edge cases:**
  - **Excluded data.** The following are not exported, because only budgets with `id > 0`
    are read and the backup model holds nothing else
    (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:27-31`):
    - the scheduled budget (−2) and its templates;
    - categories and merchants;
    - message templates and qualifiers;
    - settings;
    - messages without a spending;
    - message status, parsed amount, merchant, template id and
      `suggested_expenditure_name`
      (`android/legacy/src/main/java/com/mas/mobile/repository/db/entity/SpendingMessage.kt:30-37`).
  - **Picker cancelled.** If the user cancels the picker (`uri == null`), nothing happens
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:43`).
  - **Silent failure.** Failures are not logged. Only the generic dialog is shown
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:149-151`).
- **Data touched:** Reads `budgets`, `expenditures`, `spendings` and `spending_messages`.
  Writes an external file.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:27-31`,
  `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:117-159`

## UC-L9.2: Import budgets from a JSON file

- **Actor:** user
- **Trigger:** Settings → "Import budgets"
  (`android/legacy/src/main/res/layout/settings_fragment.xml:309-327`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:135-137`).
- **Preconditions:** The user picks a document. The system open-document picker is
  launched with the MIME filter `application/json`
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:45-47`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:136`).
- **Main flow:**
  1. Show the progress bar. In the background, read the file as text and parse it with
     Gson
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:156-160`,
     `android/legacy/src/main/java/com/mas/mobile/service/BackupFileService.kt:18-21`,
     `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupJson.kt:20`).
  2. For each budget in the file, in file order
     (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:33-47`):
     - If a budget with the **same name** already exists, count it as skipped.
     - Otherwise, import it (steps 3–6) and count it as imported.
  3. **Budget.** Create a budget with a new id. Copy `name`, `startsOn`, `lastDayAt`,
     `comment` and `currency`
     (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:50-56`).
  4. **Expenditures.** For each one, create an expenditure with a new id. Copy `name`,
     `iconId`, `plan`, `fact`, `comment` and `displayOrder` (`addExpenditure` then
     overwrites `displayOrder`,
     `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:38`). Add it to
     the budget, and
     record the mapping from old id to new id
     (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:58-70`).
  5. **Spendings.** For each one, look up the new expenditure through the mapping. If it
     is missing, skip the spending
     (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:72-75`).
     Otherwise create a spending with a new id and copy these fields
     (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:77-95`):
     - `comment`, `date` and `amount`;
     - `recurrence`, or `Never` if the value is unknown;
     - `exchangeInfo`, if `exchangeCurrency` is set. A missing rate or raw amount becomes
       0.

     Then add the spending to the budget.
  6. **Messages.** For each message of that spending, create **and save immediately** a
     message with a new id. Copy `sender`, `text` and `receivedAt`, and set
     `status = Recommended`, `isNew = false` and `spendingId = new spending id`
     (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:97-115`).
  7. Save the budget, with its expenditures and spendings, in one transaction
     (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:102`,
     `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:50-58`).
  8. Show "Imported N budget(s). Skipped M already present.", or the generic failure
     message
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:161-166`,
     `android/legacy/src/main/res/values/strings.xml:121-122`).
- **Rules:**
  - **Name is the identity.** The duplicate check is by exact budget name, including the
    scheduled budget's name "Scheduled"
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:25-26`,
    `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:32`).
  - **Ids are not preserved.** All ids are regenerated from `generator`
    (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:27-30`,
    `android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureRepositoryImpl.kt:15`,
    `android/legacy/src/main/java/com/mas/mobile/repository/budget/SpendingRepositoryImpl.kt:15`,
    `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageRepositoryImpl.kt:27`).
  - **Totals are recalculated.** Budget `plan` and `fact` from the file are ignored and
    recalculated when expenditures and spendings are added. Expenditure `fact` is also
    recalculated from the imported spendings
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:74-83`).
  - **Order values are renumbered.** Expenditure `displayOrder` is replaced by
    `max(current max, own value) + 1`, so the values rise strictly in file order.
    - For a file exported by the app, which is sorted by `display_order, name`, the
      visible order stays the same.
    - For a hand-edited file, file order wins.

    Sources: `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:32-45`,
    `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/ExpenditureDAO.kt:10-12`,
    UC-L2.10.
  - **Amounts are stored as given.** Spending `amount` is not re-converted, and
    `exchange()` is never called
    (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:80`).
  - **Messages are reset.** Every imported message's original status and its `isNew` flag
    are discarded
    (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:110-111`).
  - **Dates are not checked.** Only the name is checked, so imported budgets can overlap
    existing budgets
    (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:37-44`).
  - **`schemaVersion` is never read**
    (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupModels.kt:7`).
- **Edge cases:**
  - **Failure mid-import.** Each budget is saved separately, so a failure leaves earlier
    budgets imported, and only the failure dialog is shown, with no count. Malformed JSON
    fails earlier, before anything is saved. Causes that fail after earlier budgets are
    saved include:
    - An unknown currency code makes `Currency.getInstance` throw
      (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:55`,
      `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:91`).
    - Two expenditures in one budget whose names are equal ignoring case (identical names
      included) fail `addExpenditure`'s `require`
      (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:85-91`).
  - **Orphan messages.** Each message is saved immediately. If anything fails after the
    first message is saved, those messages stay, pointing at spending ids that were never
    stored. Examples are an unknown exchange currency on a later spending, or a failed
    budget save
    (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:91`,
    `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:97-114`).
  - **Re-importing an export from the same device** skips every budget that still exists
    under the same name
    (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:38`).
  - **Names are locale-dependent.** Weekly (`EEEE`), monthly (`MMMM`) and quarterly
    (`QQQ`) budget names are formatted with patterns that pass no locale, so they follow
    the device's default locale. That is JDK behaviour
    (`android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:54-56`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:218`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:233`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:244`).
    Yearly names (`yyyy`) are not locale-dependent
    (`android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:57`). An export from a
    device with another language can therefore add a second budget for the same period.
  - **Overlap.** An imported budget whose dates cover today overlaps the active budget,
    which raises the ambiguity in area 1 (UC-L1.1, open question 1).
  - **Re-matching.** Imported messages are `Recommended`, so a later GPT promotion for the
    same sender can re-match them (UC-L5.9)
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:85-96`).
- **Data touched:**
  - Writes `budgets`, `expenditures`, `spendings`, `spending_messages` and `generator`.
  - Reads `budgets` (for the name check).
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:33-115`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:156-174`

## UC-L9.3: Platform backup

- **Actor:** system
- **Trigger:** Android's own backup mechanism.
- **Preconditions:** None.
- **Main flow:**
  1. The manifest declares `android:allowBackup="true"`
     (`android/legacy/src/main/AndroidManifest.xml:18`).
- **Rules:** The app has no backup rules file and no backup agent. The manifest names
  neither
  (`android/legacy/src/main/AndroidManifest.xml:17-24`).
- **Edge cases:** What the platform includes and when it runs is OS behaviour, not app
  code.
- **Data touched:** Not determined by app code.
- **Source:** `android/legacy/src/main/AndroidManifest.xml:17-24`

## Open questions

1. **Malformed files.** Gson constructs objects without Kotlin constructors, so it may
   leave non-null properties `null` when a field is missing from the JSON. A missing
   `schemaVersion` might then read as 0 rather than the Kotlin default 1
   (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupJson.kt:20`). Where
   such a file fails, or what it imports, depends on Gson's runtime behaviour.
2. **Platform restore.** Does Android's platform backup restore the Room database between
   devices (UC-L9.3)? That depends on OS settings and versions.
3. **Purpose of `schemaVersion`.** It is written but never read on import, so its intended
   use is unknown. For context, `exportedAt` and the `id` fields of budgets, spendings and
   messages are also not read. `ExpenditureBackup.id` is used only to map spendings to
   expenditures
   (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:69`,
   `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:73`).

## Surprises

- **Status and read flag lost.** Imported messages lose their status, which becomes
  `Recommended`, and their unread flag, which becomes read
  (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:105-113`).
- **No configuration in the backup.** Categories, merchants, message templates,
  qualifiers, settings and scheduled spendings are not part of the backup
  (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:27-31`).
- **Name-only duplicate check.** Duplicates are detected by budget name only, not by
  dates. Locale-dependent names can produce two budgets for one period
  (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:38`).
- **Partial import, no detail.** A failure part-way leaves a partial import, and the user
  sees only a generic failure message
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:159-165`).
- **Orphan messages.** Messages are written one by one before their budget, so any
  failure later in that budget leaves orphan messages
  (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupService.kt:97-114`).
- **Unchecked version.** `schemaVersion` is exported but never checked on import
  (`android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupModels.kt:7`).
