# Legacy data model

This file describes every entity the legacy app persists, as implemented in Room schema
version 10.

For each table it lists:
- the column, its SQLite type and nullability (from the v10 schema export);
- the Kotlin entity field;
- the domain field it maps to;
- the constraints and notes from code.

Migration history is in [`map.md`](map.md) §5. Behaviour that writes these fields is in
the area files.

All citations use paths relative to the repository root. DDL lines refer to the v10
schema export.

## 1. Storage overview

| Store | What | Source |
|---|---|---|
| Room database `mas_home.db`, version 10 | 10 tables (below) | `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:34-49`, `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:58` |
| SharedPreferences file `<applicationId>` | `firstRun` (Boolean). Written and read by `SettingsService` only, and never called (UC-L6.8) | `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:26-37` |
| User-chosen JSON file | Backup export (UC-L9.1) | `android/legacy/src/main/java/com/mas/mobile/domain/backup/BackupModels.kt:6-54` |
| Remote | None. Firebase receives analytics events only (UC-L6.10) | `android/legacy/src/main/java/com/mas/mobile/util/FirebaseEventLogger.kt:13-26` |

## 2. Value encodings

| Kotlin type | Stored as | Source |
|---|---|---|
| `LocalDate` | INTEGER, epoch day | `android/legacy/src/main/java/com/mas/mobile/repository/db/config/converter/SQLiteTypeConverter.kt:27-35`, `android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:49-50` |
| `LocalDateTime` | INTEGER, epoch milliseconds in the device's default time zone | `android/legacy/src/main/java/com/mas/mobile/repository/db/config/converter/SQLiteTypeConverter.kt:17-25`, `android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:51-52` |
| `Boolean` | INTEGER 1 or 0 | `android/legacy/src/main/java/com/mas/mobile/repository/db/config/converter/SQLiteTypeConverter.kt:37-45` |
| `List<String>` | TEXT, a Gson JSON array. `""` is read as an empty list | `android/legacy/src/main/java/com/mas/mobile/repository/db/config/converter/SQLiteTypeConverter.kt:57-66` |
| `Currency` (domain) | TEXT, ISO 4217 code | `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetMapper.kt:19`, `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetMapper.kt:32` |
| Money amounts | REAL (double). Spending `amount` is in the budget currency. Foreign spendings are converted with `amount = rawAmount × rate` | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Spending.kt:61-62`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:22-26` |

- The device time zone is used for `LocalDateTime`, so moving the device between time
  zones shifts the displayed wall-clock time of stored timestamps.
- An invalid stored currency code falls back to the system currency for budgets and
  templates, because `toCurrency()` catches the error
  (`android/legacy/src/main/java/com/mas/mobile/Extensions.kt:8-12`). Spendings call
  `Currency.getInstance` directly
  (`android/legacy/src/main/java/com/mas/mobile/repository/budget/SpendingMapper.kt:24`).

## 3. Identifiers

- **Shared sequence.** Every entity created by the app takes its id from one shared
  sequence: an `INSERT … REPLACE` into the `generator` table, which returns a new rowid
  (`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/IdGeneratorDAO.kt:10-11`).
  That covers budgets, expenditures, spendings, categories, messages, templates and
  qualifiers (map §3). Backup import also goes through `create()`.
  Generator-assigned ids are unique across these tables. Seed rows reuse the same low ids
  in different tables, for example category 1, template 1 and qualifier 1.
- **Starting value.** A fresh install seeds the sequence at 100
  (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:25-28`).
  Migration 1→2 seeded it at 1000
  (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:96-101`).
- **Fixed ids.** Seed rows use fixed ids:
  - scheduled budget −2;
  - categories 1–19;
  - templates 1–4;
  - qualifiers 1–38 always, plus at most one locale block chosen by the device language
    (UA 39–47, RU 48–56, ES 57–79), on a fresh install only.

  Source: `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:30-175`.
  Installs upgraded through migration 8→9 instead have qualifier ids renumbered 1..N by
  `ROW_NUMBER() OVER (ORDER BY name)`
  (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:166-176`).
- **Not an identity.** Although several tables declare `AUTOINCREMENT`, the app supplies
  every id explicitly, from the generator or from the seed DML. The exception is the
  `generator` table itself: `IdGenerator(id = 0)` is inserted, so SQLite assigns the rowid
  (`android/legacy/src/main/java/com/mas/mobile/repository/db/entity/IdGenerator.kt:11`).

## 4. Tables

### 4.1 `budgets`

Entity `Budget`: `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Budget.kt:12-41`.
DDL: `android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/10.json:9`.
Mapper: `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetMapper.kt:10-33`.

| Column | Type | Null | Entity field | Domain field | Notes | Source |
|---|---|---|---|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | no | `id` | `Budget.id: BudgetId` | −2 is the scheduled budget. Real budgets are > 0 | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Budget.kt:14-16` |
| `name` | TEXT | no | `name` | `name` | Unique index `index_budgets_name` | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Budget.kt:12`, `android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/10.json:80` |
| `plan` | REAL | no | `plan` | `plan` | Stored sum of expenditure plans (UC-L2.9) | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Budget.kt:21-22`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:81` |
| `fact` | REAL | no | `fact` | `fact` | Stored sum of expenditure facts | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Budget.kt:24-25`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:82` |
| `startsOn` | INTEGER (epoch day) | no | `startsOn` | `startsOn` | Actual first day | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Budget.kt:27-28` |
| `lastDayAt` | INTEGER (epoch day) | no | `lastDayAt` | `lastDayAt` | Inclusive last day | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Budget.kt:30-31` |
| `isActive` | INTEGER | no | `isActive` | not mapped | Always written `false` | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Budget.kt:33-34`, `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetMapper.kt:23-33` |
| `comment` | TEXT | yes | `comment` | `comment` | Holds the truncation comment (UC-L1.3) | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Budget.kt:36-37` |
| `currency` | TEXT | no | `currency` | `currency: Currency` | ISO code. Added in migration 6→7 | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Budget.kt:39-40`, `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:136` |

Invariants maintained in code, but not by the schema:
- Budgets with `id > 0` do not overlap. This holds only for automatic creation
  (UC-L1.2). Import can break it (UC-L9.2).
- Exactly one budget, the scheduled one, has `id = −2`.

### 4.2 `expenditures`

Entity `ExpenditureData`: `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Expenditure.kt:40-77`.
DDL: `android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/10.json:177`.
Mapper: `android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureMapper.kt:9-33`.

| Column | Type | Null | Entity field | Domain field | Notes | Source |
|---|---|---|---|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | no | `id` | `Expenditure.id: ExpenditureId` | From the generator | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Expenditure.kt:53-55` |
| `name` | TEXT | no | `name` | `name` | Unique per budget ignoring case. This is enforced in code only (UC-L2.4) | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Expenditure.kt:57-58`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:85-91` |
| `plan` | REAL | no | `plan` | `plan` | | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Expenditure.kt:60-61` |
| `fact` | REAL | no | `fact` | `fact` | Stored sum of spending amounts | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Expenditure.kt:63-64` |
| `comment` | TEXT | yes | `comment` | `comment` (`""` when null) | | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Expenditure.kt:66-67`, `android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureMapper.kt:17` |
| `budget_id` | INTEGER | no | `budget_id` | `budgetId` | FK → `budgets.id`, ON DELETE CASCADE | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Expenditure.kt:40-45`, `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Expenditure.kt:69-70` |
| `icon` | INTEGER | yes | `icon` | `iconId: IconId?` | Icon-pack id. Added in migration 8→9 | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Expenditure.kt:72-73`, `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:194` |
| `display_order` | INTEGER | no | `display_order` | `displayOrder` | Added in migration 9→10. No default in the v10 export. `DEFAULT 0` appears only in the migration DDL | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Expenditure.kt:75-76`, `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:209` |

Indices are `(budget_id)`, `(budget_id, display_order, name)` and `(name)`
(`android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Expenditure.kt:46-50`,
`android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/10.json:242`,
`android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/10.json:253`,
`android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/10.json:262`).

### 4.3 `spendings`

Entity `SpendingData`: `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Spending.kt:41-78`.
DDL: `android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/10.json:87`.
Mapper: `android/legacy/src/main/java/com/mas/mobile/repository/budget/SpendingMapper.kt:12-46`.

| Column | Type | Null | Entity field | Domain field | Notes | Source |
|---|---|---|---|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | no | `id` | `Spending.id: SpendingId` | From the generator | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Spending.kt:51-53` |
| `comment` | TEXT | yes | `comment` | `comment` (`""` when null) | | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Spending.kt:55-56` |
| `date` | INTEGER (epoch ms) | no | `date` | `date: LocalDateTime` | Not checked against the budget range. `addSpending` does not look at the date | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Spending.kt:58-59`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:55-67` |
| `amount` | REAL | no | `amount` | `amount` | In the budget currency. Converted when foreign | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Spending.kt:61-62`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:22-26` |
| `expenditure_id` | INTEGER | no | `expenditureId` | `expenditure` | FK → `expenditures.id`, ON DELETE CASCADE. Index on the column | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Spending.kt:41-48`, `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Spending.kt:64-65` |
| `currency` | TEXT | yes | `currency` | `exchangeInfo.currency` | Foreign ISO code | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Spending.kt:67-68` |
| `rate` | REAL | yes | `rate` | `exchangeInfo.rate` | Budget-currency units per 1 foreign unit | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Spending.kt:70-71`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:23` |
| `foreignAmount` | REAL | yes | `foreignAmount` | `exchangeInfo.rawAmount` | Not null means the spending is foreign | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Spending.kt:73-74`, `android/legacy/src/main/java/com/mas/mobile/repository/budget/SpendingMapper.kt:20-26` |
| `recurrence` | TEXT | no | `recurrence` | `recurrence: Recurrence` | `DEFAULT 'Never'` appears only in the 8→9 migration DDL (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:199`). An unknown value reads as `Never` | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Spending.kt:76-77`, `android/legacy/src/main/java/com/mas/mobile/repository/budget/SpendingMapper.kt:27-31` |

`recurrence` values: `Never`, `Daily`, `Weekly`, `Monthly`, `Quarterly`
(`android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:61-63`).
Values other than `Never` are meaningful only in the scheduled budget (UC-L1.9).

### 4.4 `spending_messages`

Entity `SpendingMessage`: `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/SpendingMessage.kt:9-49`.
DDL: `android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/10.json:281`.
Mapper: `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageMapper.kt:10-45`.

| Column | Type | Null | Entity field | Domain field | Notes | Source |
|---|---|---|---|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | no | `id` | `Message.id` | From the generator | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/SpendingMessage.kt:11-13` |
| `name` | TEXT | no | `name` | `sender` | SMS address or app label | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/SpendingMessage.kt:15-16` |
| `message` | TEXT | no | `message` | `text` | Full captured text | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/SpendingMessage.kt:18-19` |
| `receivedAt` | INTEGER (epoch ms) | no | `receivedAt` | `receivedAt` | SMS: processing time. Notification: post time | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/SpendingMessage.kt:21-22`, `android/legacy/src/main/java/com/mas/mobile/service/SmsListener.kt:34`, `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:25` |
| `rule_id` | INTEGER | no | `ruleId` | `Matched.messageTemplateId` | −1 when not Matched. No FK | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/SpendingMessage.kt:24-25`, `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageMapper.kt:38` |
| `spending_id` | INTEGER | yes | `spendingId` | `spendingId` | Link to `spendings.id`. No FK | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/SpendingMessage.kt:27-28` |
| `suggested_expenditure_name` | TEXT | yes | `suggestedExpenditureName` | not mapped | Never written by the mapper | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/SpendingMessage.kt:30-31` |
| `suggested_merchant` | TEXT | yes | `suggestedMerchant` | `Matched.merchant` | Added in migration 8→9 | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/SpendingMessage.kt:33-34`, `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:197` |
| `suggested_amount` | REAL | no | `suggestedAmount` | `Matched.amount` | 0.0 when not Matched | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/SpendingMessage.kt:36-37` |
| `is_new` | INTEGER | no | `isNew` | `isNew` | Unread flag | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/SpendingMessage.kt:39-40` |
| `status` | TEXT | no | `status` | `status` | `MATCHED` or `RECOMMENDED`. Added in migration 2→3, default `'MATCHED'` | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/SpendingMessage.kt:42-48`, `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:103-107` |

Status mapping:
- Reading: `MATCHED` becomes `Matched` built from `rule_id`, `suggested_amount` and
  `suggested_merchant`. Any other value becomes `Recommended`
  (`android/legacy/src/main/java/com/mas/mobile/repository/message/MessageMapper.kt:18-26`).
- Writing: `Rejected` is never stored (UC-L5.3,
  `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:32`).

### 4.5 `categories`

Entity `Category`: `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Category.kt:8-40`.
DDL: `android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/10.json:538`.
Mapper: `android/legacy/src/main/java/com/mas/mobile/repository/budget/CategoryMapper.kt:9-33`.

| Column | Type | Null | Entity field | Domain field | Notes | Source |
|---|---|---|---|---|---|---|
| `id` | INTEGER PK | no | `id` | `Category.id: CategoryId` | Seed rows 1–19. Others from the generator | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Category.kt:16-18`, `android/legacy/src/main/java/com/mas/mobile/repository/budget/CategoryRepositoryImpl.kt:27` |
| `name` | TEXT | no | `name` | `name` | Unique ignoring case. Enforced in code only (UC-L4.2) | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Category.kt:20-21` |
| `plan` | REAL | no | `plan` | `plan` | No currency | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Category.kt:23-24` |
| `active` | INTEGER | no | `isActive` | `isActive` | Affects only new budgets | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Category.kt:26-27`, `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:170` |
| `description` | TEXT | no | `description` | `description` | | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Category.kt:29-30` |
| `merchants` | TEXT (JSON array) | no | `merchants: List<String>` | `merchants: MutableList<Merchant>` | Seed rows store `''` | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Category.kt:32-33`, `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:138` |
| `icon` | INTEGER | yes | `icon` | `iconId: IconId?` | | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Category.kt:35-36` |
| `display_order` | INTEGER | no | `displayOrder` | `displayOrder` | Added in migration 9→10. No default in the v10 export. `DEFAULT 0` appears only in the migration DDL | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Category.kt:38-39`, `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:206` |

Indices are `index_categories_on_name (name)` and
`index_categories_on_order_and_name (display_order, name)`
(`android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Category.kt:10-13`).

### 4.6 `message_template`

Entity `MessageTemplate`: `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/MessageTemplate.kt:9-29`.
DDL: `android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/10.json:488`.
Mapper: `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageTemplateMapper.kt:9-29`.

| Column | Type | Null | Entity field | Domain field | Notes | Source |
|---|---|---|---|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | no | `id` | `MessageTemplate.id` | Seed rows 1–4 | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/MessageTemplate.kt:11-13` |
| `sender` | TEXT | no | `sender` | `sender` | Matched ignoring case against the message sender | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/MessageTemplate.kt:15-16`, `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:99` |
| `pattern` | TEXT | no | `pattern` | `pattern: Pattern` | Must contain `{amount}` and be ≤ 100 characters. Checked when the value is read | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/MessageTemplate.kt:18-19`, `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageTemplateMapper.kt:14` |
| `example` | TEXT | no | `example` | `example` | Sample text | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/MessageTemplate.kt:21-22` |
| `currency` | TEXT | no | `currency` | `currency: Currency` | Currency of matched amounts | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/MessageTemplate.kt:24-25` |
| `enabled` | INTEGER | no | `isEnabled` | `isEnabled` | Default 1 in the 8→9 DDL | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/MessageTemplate.kt:27-28`, `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:159` |

A stored pattern that violates the `Pattern` rules throws when it is mapped, because the
`require` checks run in the constructor
(`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:6-10`).

### 4.7 `qualifiers`

Entity `Qualifier`: `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Qualifier.kt:10-30`.
DDL: `android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/10.json:407`.
Mapper: `android/legacy/src/main/java/com/mas/mobile/repository/message/QualifierMapper.kt:10-37`.

| Column | Type | Null | Entity field | Domain field | Notes | Source |
|---|---|---|---|---|---|---|
| `id` | INTEGER PK | no | `id` | `Qualifier.id` | Rebuilt in migration 8→9 using `ROW_NUMBER() OVER (ORDER BY name)` | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Qualifier.kt:15-17`, `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:171-173` |
| `name` | TEXT | no | `name` | `value` | Keyword or sender | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Qualifier.kt:19-20` |
| `type` | INTEGER | no | `type` | `type: Qualifier.Type` | See the enum below. Index `(type, name)` | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Qualifier.kt:12`, `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Qualifier.kt:22-23` |

| `type` value | Domain | Meaning | Source |
|---|---|---|---|
| 1 | `CATCH` | Keyword that makes a message look like a payment | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Qualifier.kt:26` |
| 2 | `SKIP` | Keyword that excludes a message. Also the fallback for unknown codes | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Qualifier.kt:27`, `android/legacy/src/main/java/com/mas/mobile/repository/message/QualifierMapper.kt:22` |
| 3 | `BLACKLIST` | Sender that is never recommended | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Qualifier.kt:28` |

There is no uniqueness constraint. Duplicate blacklist rows are possible (UC-L5.12).

### 4.8 `settings`

Entity `Settings`: `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Settings.kt:7-15`.
DDL: `android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/10.json:361`.

| Column | Type | Null | Notes | Source |
|---|---|---|---|---|
| `key` | TEXT PK | no | One of the 8 keys below | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Settings.kt:9-11` |
| `value` | TEXT | yes | String form of the value | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Settings.kt:13-14` |

| Key | Value format | Default when missing |
|---|---|---|
| `budget.period` | `MONTH`, `WEEK`, `TWO_WEEKS`, `QUARTER` or `YEAR` | `MONTH` |
| `budget.currency` | ISO code | System currency |
| `budget.startDayOfMonth` | Integer 1–31. The UI offers 1–28 (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:68`) | Today's day of month, capped at 28 |
| `budget.startDayOfWeek` | Integer 1–7 (ISO, Monday = 1) | Today's day of week |
| `budget.captureSMS` | `true` / `false` | `false` |
| `budget.captureNotifications` | `true` / `false` | `false` |
| `policy.version` | e.g. `privacy_policy_v3` | `""` |
| `message.discoveryMode` | `true` / `false` | `true` |

Sources: `android/legacy/src/main/java/com/mas/mobile/repository/settings/SettingsMapper.kt:10-51`,
`android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:8-38`. Usage is
in UC-L6.7.

The defaults apply only when a key is missing. A malformed stored value throws, because
the mapping has no fallback
(`android/legacy/src/main/java/com/mas/mobile/repository/settings/SettingsMapper.kt:13-16`).

### 4.9 `deferrable_actions`

Entity `DeferrableAction`: `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/DeferrableAction.kt:8-27`.
DDL: `android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/10.json:450`.
Mapper: `android/legacy/src/main/java/com/mas/mobile/repository/settings/DeferrableActionMapper.kt:8-29`.

| Column | Type | Null | Entity field | Domain field | Notes | Source |
|---|---|---|---|---|---|---|
| `key` | TEXT PK | no | `key` | `key.value` | e.g. `Version available: <code>` | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/DeferrableAction.kt:10-12`, `android/legacy/src/main/java/com/mas/mobile/service/AppUpdateCheckWorker.kt:32` |
| `type` | INTEGER | no | `type` | key class | 1 = `UniformDaily`, 2 = `ProgressiveDaily`. Any value other than 2 reads as `UniformDaily` (`android/legacy/src/main/java/com/mas/mobile/repository/settings/DeferrableActionMapper.kt:11-14`) | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/DeferrableAction.kt:14-15`, `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/DeferrableAction.kt:24-25` |
| `increment` | INTEGER | no | `increment` | `increment: Hours` | Next wait, in hours | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/DeferrableAction.kt:17-18` |
| `active_after` | INTEGER (epoch ms) | no | `activeAfter` | `activeAfter` | Rows with `active_after` more than 30 days before now are deleted on each defer | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/DeferrableAction.kt:20-21`, `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/DeferredActionDAO.kt:15-16`, `android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableActionService.kt:21` |

### 4.10 `generator`

Entity `IdGenerator`: `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/IdGenerator.kt:7-12`.
DDL: `android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/10.json:387`.

| Column | Type | Null | Notes | Source |
|---|---|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | no | Each `INSERT … REPLACE` returns the next id (section 3) | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/IdGenerator.kt:9-11` |

Migration 1→2 created `generator` without `AUTOINCREMENT`, so installs upgraded from v1
differ from the v10 export
(`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:98`).

Two seed inserts name columns that their migration's table does not have yet: categories
in 8→9 (UC-L4.10) and qualifiers in 4→5 (capture open question 2)
(`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:120-121`,
`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:192`).

## 5. Relations

```
budgets 1 ──< expenditures 1 ──< spendings          (FK, ON DELETE CASCADE)
spendings 1 ── 0..1 spending_messages.spending_id   (no FK)
message_template 1 ── 0..* spending_messages.rule_id (no FK)
categories ·· expenditures                           (by name, case-insensitive; no column)
categories.merchants ·· spending_messages.suggested_merchant (by value, case-insensitive)
qualifiers.name (BLACKLIST) ·· spending_messages.name (by value, case-insensitive)
```

| Relation | Kind | Enforced by | Source |
|---|---|---|---|
| Expenditure → budget | FK `budget_id`, cascade | SQLite | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Expenditure.kt:40-45` |
| Spending → expenditure | FK `expenditure_id`, cascade | SQLite | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Spending.kt:41-47` |
| Message → spending | `spending_id` value | Code only. It is cleared only when a spending is removed through a budget save | `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:80-83` |
| Message → template | `rule_id` value | Not enforced | `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageMapper.kt:20` |
| Category ↔ expenditure | Same name, ignoring case | Code lookups only | `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:154-180` |
| Merchant → category | Merchant string inside the category's list | Code lookups only | `android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:12-15` |
| Blacklisted sender | Qualifier value equal to the message sender, ignoring case | Code lookups only | `android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:31-33` |

- A spending's budget is reached only through its expenditure. Spendings have no
  `budget_id` (section 4.3).
- Cascade deletes do not clear `spending_messages.spending_id`, which leaves dangling
  links (S-L1.9, S-L2.3).

## 6. Special and seed rows

| Row(s) | Table | Content | Source |
|---|---|---|---|
| Scheduled budget | `budgets` | `id = −2`, name `Scheduled`, `startsOn = lastDayAt = LocalDate.MIN`. Hidden by `id > 0` filters | `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:30-33`, `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:10-35` |
| Generator start | `generator` | `id = 100` | `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:25-28` |
| Bank templates | `message_template` | 4 rows, ids 1–4 | `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:35-44` |
| Qualifiers | `qualifiers` | English 1–38, plus language-specific rows | `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:47-134` |
| Categories | `categories` | 19 rows, ids 1–19, 4 of them inactive | `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:136-175` |

## 7. Domain types that are not tables

| Domain type | Persisted as | Source |
|---|---|---|
| `Budget.budgetDetails` (expenditures and spendings) | Loaded lazily by `budget_id` and saved with the budget. Changed or deleted children are detected by an MD5 hash of each child's `toString()` | `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:117-155` |
| `ExchangeInfo` | `spendings.currency`, `rate`, `foreignAmount` | `android/legacy/src/main/java/com/mas/mobile/repository/budget/SpendingMapper.kt:20-26`, `android/legacy/src/main/java/com/mas/mobile/repository/budget/SpendingMapper.kt:41-43` |
| `Message.Status` | `spending_messages.status`, `rule_id`, `suggested_amount`, `suggested_merchant` | `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageMapper.kt:18-43` |
| `Merchant` | Strings in `categories.merchants` | `android/legacy/src/main/java/com/mas/mobile/repository/budget/CategoryMapper.kt:18`, `android/legacy/src/main/java/com/mas/mobile/repository/budget/CategoryMapper.kt:30` |
| `Pattern` | `message_template.pattern` text | `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageTemplateMapper.kt:14`, `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageTemplateMapper.kt:24` |
| `Settings` | 8 rows in `settings` | `android/legacy/src/main/java/com/mas/mobile/repository/settings/SettingsMapper.kt:24-34` |

## 8. Declared but unused

| Item | State | Source |
|---|---|---|
| Entity `ExpenseReport` (table `expenses`) | Not registered in `@Database`. No references | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/ExpenseReport.kt:6-12` |
| Entity `Notification` (table `notifications`) | Not registered in `@Database`. No references | `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Notification.kt:8-16` |
| `budgets.isActive` | Always `false` | `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetMapper.kt:23-33` |
| `spending_messages.suggested_expenditure_name` | Never written | `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageMapper.kt:30-44` |
| Table `message_rules` (v5–v8) | Dropped in migration 8→9 | `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:198` |
