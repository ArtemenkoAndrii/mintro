# Area 1: Budgets and periods

Scope:
- How the active budget is resolved and how new budgets are created.
- Period types, start day and the truncation comment.
- Gaps between budgets.
- Browsing, editing and deleting budgets.
- The scheduled budget (id −2) and its scheduled spendings.

Out of scope here:
- Expenditure details (area 2).
- The spending form and currency conversion (area 3).
- Worker scheduling mechanics (area 7).

All citations use paths relative to the repository root.

## UC-L1.1: Resolve the active budget

- **Actor:** system
- **Trigger:** Any call to `BudgetService.getActiveBudget()`. Callers:
  - `spend` (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:97`)
  - `getRate` (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:124`)
  - `reloadActiveBudget` (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:44`)
  - `loadBudgetOrGetActive` with id −1 (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:55-61`)
  - `SpendingViewModel.getCurrencyFromBudget` (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:96-101`)
  - `ExpenditureViewModel` (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:46`)
  - `ChartViewModel` (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:69`)
- **Preconditions:** None.
- **Main flow:**
  1. Query `budgets` for a row with `id > 0` and `today BETWEEN startsOn AND lastDayAt`
     (`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:34-35`,
     `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:38`,
     `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:70-71`).
  2. If a row is found, return it.
  3. Otherwise run UC-L1.2 and return the new budget
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:71`).
- **Rules:**
  - "Today" is `LocalDate.now()`, the device date
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:71`).
  - Both ends are inclusive (`BETWEEN`).
  - The scheduled budget (id −2) is never the active budget, because of `id > 0`
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:34`).
- **Edge cases:**
  - The query has no `ORDER BY` and returns a single row
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:34-35`).
    The code does not decide which row wins if two budgets cover today.
  - Resolving the active budget can create and save a budget as a side effect. This
    applies to every caller listed above, including the exchange-rate lookup and the
    chart screen.
- **Data touched:** reads `budgets`. May write `budgets`, `expenditures` and `generator`
  through UC-L1.2.
- **Source:**
  - `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:55-71`
  - `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:34-35`

## UC-L1.2: Create the next budget automatically

- **Actor:** system
- **Trigger:**
  - UC-L1.1 finds no active budget.
  - The user adds a budget from the list (UC-L1.4).
  - `BudgetViewModel` is opened with action `ADD`
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetViewModel.kt:47`).
- **Preconditions:** Settings `period`, `startDayOfMonth`, `startDayOfWeek` and `currency`
  are readable. Defaults apply when keys are missing
  (`android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:8-17`).
- **Main flow:**
  1. Find the last budget: the row with `id > 0` and the highest `lastDayAt` below
     `LocalDate.MAX`
     (`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:31-32`,
     `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:40`).
  2. `startDate = max(last.lastDayAt + 1 day, today)`. If there is no budget, `startDate = today`
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:77-79`).
  3. Take a new id from the `generator` table
     (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:27-30`).
     Compute `name`, `startsOn`, `lastDayAt` and `comment` for the configured period (UC-L1.3)
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:194-206`).
  4. Set `currency` to the Settings currency
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:82`).
  5. Make the name unique (see Rules)
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:83`).
  6. For every active category, add an expenditure with the category's name, plan and icon
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:84`,
     `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:169-180`).
     Each add recalculates the totals
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:32-45`).
  7. Save the budget row and its expenditures in one transaction. This runs inside
     `runBlocking` on the calling thread
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:76`,
     `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:87`,
     `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:50-58`).
- **Rules:**
  - `budget.plan = Σ expenditure.plan` and `budget.fact = Σ expenditure.fact`, where
    `expenditure.fact = Σ amount` of its spendings
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:74-83`).
  - Unique name: while a budget with the same name exists, the code tries
    `name + "(" + i + ")"` for `i = 2, 3, …`, with no space before the parenthesis
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:182-191`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:268`).
    The database also has a unique index on `budgets.name`
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Budget.kt:12`).
  - Categories are read in `display_order, name` order
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/CategoryDAO.kt:16-17`).
    Each new expenditure first gets the category's `displayOrder`
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:176`).
    It is then added to the list, and its `displayOrder` is set to `max + 1`, where the
    max includes its own copied value
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:36-39`).
    The result is `order_i = max(order_{i−1}, category.displayOrder_i) + 1`. For example,
    the seeded categories with orders 1–19 become 2–20.
  - `budgets.isActive` is always written as `false`, because the mapper does not set it
    (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetMapper.kt:23-33`).
- **Edge cases:**
  - **Gap.** If the last budget ended before yesterday, `startDate` is today. The days in
    between belong to no budget
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:77-79`).
  - **Future last budget.** If the last budget ends after today, the new budget starts the
    day after it, in the future
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:77-79`).
    If, in addition, no budget covers today, the new budget does not cover today either.
    Every later UC-L1.1 call then creates yet another future budget
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:70-71`).
    One way to reach this state: add a future budget from the list (UC-L1.4), then delete
    the active budget from the form (UC-L1.7).
  - **No active categories.** The budget has no expenditures, and plan = fact = 0.
  - **Duplicate category names.** If two active categories differ only in letter case,
    `addExpenditure` fails its `require` with `IllegalArgumentException`
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:85-91`).
    `createNext` does not catch it
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:76-90`).
- **Data touched:**
  - Writes `budgets`: `id`, `name`, `plan`, `fact`, `startsOn`, `lastDayAt`, `comment`, `currency`, `isActive` = false.
  - Writes `expenditures`: `id`, `name`, `plan`, `fact` = 0, `icon`, `budget_id`, `display_order`.
  - Writes `generator`.
  - Reads `categories` and `settings`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:76-90`,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:169-191`

## UC-L1.3: Compute period boundaries, name and truncation comment

- **Actor:** system
- **Trigger:** Step 3 of UC-L1.2 (`BudgetService.createBudget(startDate)`)
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:194-206`).
- **Preconditions:** `startDate` is known.
- **Main flow:**
  1. Compute `estimatedStart` and `lastDayAt` from `startDate` and the period (table below).
  2. Set `startsOn = startDate`. `startsOn` is always the actual start, not the estimated one.
  3. Set `name` from `estimatedStart`.
  4. If `estimatedStart < startDate`, set `comment` to the truncation text. Otherwise set
     it to `""`
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:260-265`).
- **Rules:**

  | Period | `estimatedStart` | `lastDayAt` | Name pattern | Source |
  |---|---|---|---|---|
  | `MONTH` | If `startDate.day ≥ S`, day `S` of `startDate`'s month. Otherwise day `S` of the previous month. `S` = `startDayOfMonth`. | `estimatedStart + 1 month − 1 day` | `MMMM yyyy` | `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:224-237` |
  | `WEEK` | `startDate − diff`. If `dow(startDate) ≥ W`, `diff = dow(startDate) − W`. Otherwise `diff = 7 − W + dow(startDate)`. `W` = `startDayOfWeek`, `dow` = ISO 1–7. | `estimatedStart + 6 days` | `EEEE dd/MM/yyyy` | `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:208-222` |
  | `TWO_WEEKS` | Same as `WEEK` | WEEK result `+ 7 days` (`estimatedStart + 13`) | `EEEE dd/MM/yyyy` | `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:199-201` |
  | `QUARTER` | Day 1 of the first month among 10, 7, 4, 1 that is ≤ `startDate`'s month | Last day of `estimatedStart + 2 months` | `QQQ yyyy` | `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:239-248` |
  | `YEAR` | 1 January of `startDate`'s year | 31 December of that year | `yyyy` | `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:250-258` |

  - Name patterns are defined at
    `android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:54-57`.
  - `QUARTER` and `YEAR` ignore both start-day settings.
  - The truncation text is "The start date was automatically truncated. Original value is %s"
    (`android/legacy/src/main/res/values/strings.xml:178`). `%s` is `estimatedStart` in the
    format `dd/MM/yyyy`
    (`android/legacy/src/main/java/com/mas/mobile/service/ResourceService.kt:15-18`,
    `android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:13`,
    `android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:34-37`).
  - The Settings screen offers start days 1–28
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:68`).
    The default is today's day of month, capped at 28
    (`android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:11`,
    `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:20-27`).
  - The default start day of week is today's day of week
    (`android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:12`,
    `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:19`).
- **Edge cases:**
  - A budget created after a gap, or after the previous budget ended early, starts on
    `startDate`. It keeps the full-period `lastDayAt` and gets the truncation comment.
  - `MONTH` naming uses the month of `estimatedStart`. For example, with `S` = 25, a budget
    starting 25 September is named after September and ends 24 October.
  - Changing the period or start day in Settings does not change existing budgets. The
    settings screen only saves settings
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:75-124`).
    The new values apply to the next budget created.
- **Data touched:** In memory only. The result is saved by UC-L1.2.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:194-265`

## UC-L1.4: Add a budget from the budget list

- **Actor:** user
- **Trigger:** Tap the add button on the Budgets screen
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:37-40`).
- **Preconditions:** None.
- **Main flow:**
  1. `BudgetListViewModel.createBudget()` calls `BudgetService.createNext()`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetListViewModel.kt:17`).
  2. UC-L1.2 runs and saves the budget.
  3. The list refreshes from its live query
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetListViewModel.kt:15`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:10-11`).
- **Rules:**
  - There is no form and no confirmation.
  - The new budget always follows the last budget (UC-L1.2 step 2).
- **Edge cases:**
  - Each tap creates another budget after the last one, so repeated taps create
    consecutive future budgets.
  - The call runs `runBlocking` on the UI thread, and the database allows main-thread
    queries
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:76`,
    `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:59`).
  - A second "add" path exists: `BudgetFragment` with action `ADD`, whose ViewModel calls
    `createNext()` while loading, so the budget is saved as soon as the form opens
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetViewModel.kt:45-51`).
    The only routes to it are the options items `budget_list_menu_add` and
    `standartd_list_menu_add`. The second is handled by the parent class
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:47`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:50`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:54`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ListFragment.kt:21-24`).
    `BudgetListFragment` inherits `onCreateOptionsMenu` from `ListFragment`, which only
    clears the menu
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ListFragment.kt:15-17`).
    No code inflates `budget_list_menu` or `standard_list_menu`, so both routes are
    unreachable.
- **Data touched:** Same as UC-L1.2.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:20-54`

## UC-L1.5: Browse budgets

- **Actor:** user
- **Trigger:** Menu tab → Budgets
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MenuFragment.kt:24`).
- **Preconditions:** None.
- **Main flow:**
  1. List every budget with `id > 0`, newest `startsOn` first
     (`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:10-11`,
     `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:104-107`).
  2. Each row shows the name, a progress text and `fact`
     (`android/legacy/src/main/res/layout/budget_list_row.xml:35`,
     `android/legacy/src/main/res/layout/budget_list_row.xml:43`,
     `android/legacy/src/main/res/layout/budget_list_row.xml:52`).
  3. Tap opens the budget in `VIEW` mode. Long-tap opens it in `EDIT` mode
     (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/BudgetAdapter.kt:35-44`).
  4. The row menu offers Edit, Expenditures, Spendings and Remove
     (`android/legacy/src/main/res/menu/budget_row_menu.xml:3-22`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:56-64`).
- **Rules:**
  - The progress text is "unplanned" when `|plan| < 0.001`. Otherwise it is
    `progress` "% of" `plan`, with `progress = toInt(fact × 100 / plan)` (truncated)
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/binding/Adapter.kt:120-128`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:22`).
- **Edge cases:** The scheduled budget is not listed (`id > 0`).
- **Data touched:** Reads `budgets`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:14-75`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/adapter/BudgetAdapter.kt:17-45`

## UC-L1.6: View and edit a budget

- **Actor:** user
- **Trigger:** Any of:
  - A row tap or long-tap, or the row menu's Edit (UC-L1.5).
  - The form's Edit menu item
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetFragment.kt:81-85`).
- **Preconditions:** The budget exists. Otherwise `ItemNotFoundException` is thrown
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetViewModel.kt:45-51`).
- **Main flow:**
  1. Load the budget by id
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetViewModel.kt:48-49`).
  2. Show the fields.
     - Always disabled: `name`, `startsOn` and `fact`.
     - Enabled only in edit mode: `lastDayAt`, `plan`, `currency` and `comment`.

     Sources: `android/legacy/src/main/res/layout/budget_fragment.xml:25`,
     `android/legacy/src/main/res/layout/budget_fragment.xml:43`,
     `android/legacy/src/main/res/layout/budget_fragment.xml:64`,
     `android/legacy/src/main/res/layout/budget_fragment.xml:85`,
     `android/legacy/src/main/res/layout/budget_fragment.xml:103`,
     `android/legacy/src/main/res/layout/budget_fragment.xml:122`,
     `android/legacy/src/main/res/layout/budget_fragment.xml:140`.
  3. Tapping `lastDayAt` depends on whether this is the last budget.
     - If it is the last budget, or no last budget exists, a date picker opens with
       minimum date `startsOn`.
     - Otherwise an info dialog says "You can change end date only for the last budget."

     Sources: `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetFragment.kt:58-69`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetViewModel.kt:95-96`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/DatePickerFragment.kt:19-21`,
     `android/legacy/src/main/res/values/strings.xml:109`.
  4. Tapping `currency` opens a currency picker
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetFragment.kt:38-52`).
  5. Save validates `name` (trimmed length ≥ 3)
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetViewModel.kt:55-59`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetViewModel.kt:104`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/validator/FieldValidator.kt:11-16`).
     If valid, it saves in the background and closes the form
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ItemFragment.kt:60-64`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ItemViewModel.kt:19-29`).
     The save writes the budget row and any changed children
     (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:50-58`).
- **Rules:**
  - "Last budget" is the budget with the latest `lastDayAt` (UC-L1.2 step 1). Being the
    active budget does not matter.
  - The date picker sets only a minimum. There is no maximum, and no check against today.
  - Changing `currency` changes only the budget field. Amounts are not converted
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetViewModel.kt:84-87`).
  - An edited `plan` is saved as entered. Adding or removing any expenditure or spending
    in that budget later recalculates it as the sum of expenditure plans
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:74-83`).
  - Validators registered with `validateOnChange` do not run while typing, because
    `validationAllowed` is never set to `true`. They run only on save through `isValid()`
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ItemViewModel.kt:13`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ItemViewModel.kt:68-78`).
- **Edge cases:**
  - **End date moved before today on the current last budget.** The next UC-L1.1 call
    finds no active budget, and UC-L1.2 creates one starting today.
  - **End date moved later.** The next automatic budget starts later.
  - **Shortening the end date** changes only `lastDayAt`
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetViewModel.kt:78-82`).
    Spendings dated after the new end stay in this budget.
  - **Invalid save.** The form stays open
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ItemFragment.kt:60-64`).
- **Data touched:** `budgets.lastDayAt`, `plan`, `currency`, `comment` (and `isActive` = false).
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetFragment.kt:15-95`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetViewModel.kt:17-106`

## UC-L1.7: Delete a budget

- **Actor:** user
- **Trigger:** One of two entry points:
  - The row menu's Remove on the list
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:61`).
  - The Remove menu item on the budget form
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetFragment.kt:86-89`).
- **Preconditions:** The budget exists.
- **Main flow:**
  1. **From the list:** if the budget is the last budget
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:73-74`),
     ask for confirmation, then delete. Otherwise show "Budgets can only be removed from
     the end." and stop
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:66-74`,
     `android/legacy/src/main/res/values/strings.xml:108`).
  2. **From the form:** ask for confirmation, then delete and close. There is no
     last-budget check
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetFragment.kt:86-89`).
  3. The delete removes the `budgets` row
     (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:60`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:46-47`).
     Expenditures, and through them spendings, are declared `ON DELETE CASCADE`
     (`android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Expenditure.kt:40-45`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Spending.kt:41-47`).
- **Rules:** List deletion is allowed only "from the end", meaning for the budget with the
  latest `lastDayAt`.
- **Edge cases:**
  - Messages linked to the deleted spendings keep their `spending_id`. Only the budget-save
    path clears it
    (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:80-83`,
    `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/SpendingMessageDAO.kt:38-39`).
  - Deleting the active budget means the next UC-L1.1 call creates a new budget. It starts
    at `max(lastDayAt of the remaining last budget + 1, today)`. If that remaining budget
    lies in the future, see the "future last budget" edge case in UC-L1.2.
  - Deleting from the form can remove a budget in the middle of the sequence, which leaves
    a gap.
- **Data touched:** Deletes from `budgets`, `expenditures` and `spendings` (cascade).
  `spending_messages` is left unchanged.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:56-74`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetFragment.kt:79-94`

## UC-L1.8: Refresh the active budget when the date changes

- **Actor:** system
- **Trigger:**
  - `ACTION_TIME_TICK` broadcasts while `MainActivity` exists
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:72`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:77-80`).
  - Creation of the `BudgetService` singleton
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:165-167`).
- **Preconditions:** None.
- **Main flow:**
  1. `DateListener` holds `lastReloadDate`, set when the listener is created. On each
     tick, if `lastReloadDate < today`, it calls `reloadActiveBudget()`. It then stores
     today
     (`android/legacy/src/main/java/com/mas/mobile/service/DateListener.kt:12`,
     `android/legacy/src/main/java/com/mas/mobile/service/DateListener.kt:17-23`).
  2. `reloadActiveBudget()` resolves the active budget (UC-L1.1) and switches the shared
     live source to that budget's id
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:42-53`).
  3. Screens that show "the active budget" (budget id −1) observe this live source
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:63-68`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureListViewModel.kt:22`).
- **Rules:** If the observed budget emits `null` (deleted), the service reloads again
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:46-51`).
- **Edge cases:** The listener is created together with `MainActivity`
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:38`).
  A newly created listener starts with today's date, so it does not reload on its first
  tick.
- **Data touched:** Reads `budgets`. May create a budget through UC-L1.2.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/service/DateListener.kt:11-24`,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:39-68`

## UC-L1.9: Manage scheduled spendings

- **Actor:** user
- **Trigger:** Menu tab → Scheduled. This opens `ScheduledSpendingListFragment` with
  budget id −2
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MenuFragment.kt:29-31`).
- **Preconditions:** The scheduled budget row (id −2, name `Scheduled`) exists. It is seeded
  on a fresh install and by migration 8→9
  (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:30-33`,
  `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:63`,
  `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:200`).
- **Main flow:**
  1. List the spendings of budget −2, newest date first
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingListViewModel.kt:21-26`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/SpendingDAO.kt:31-33`).
     Each row shows its recurrence and the next scheduled date
     (`android/legacy/src/main/res/layout/scheduled_spending_list_row.xml:70`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/binding/Adapter.kt:130-147`).
  2. Add opens the spending form for budget −2
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingListFragment.kt:111-114`).
  3. The form shows the recurrence field only when the budget is the scheduled one
     (`android/legacy/src/main/res/layout/spending_fragment.xml:194-201`). Options are
     Never, Daily, Weekly, Monthly and Quarterly
     (`android/legacy/src/main/java/com/mas/mobile/service/ResourceService.kt:51-58`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingFragment.kt:88-93`).
     Changing the date or the recurrence recomputes the shown next date
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:150-154`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:216-223`).
  4. The default currency is the active budget's currency, not the scheduled budget's
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:96-101`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:180`).
  5. Save finds the expenditure by name in budget −2. If none exists, it creates one with
     the entered name. A category whose name matches (case-insensitively) supplies only
     `plan` and icon. Without a matching category, the plan is 0.00 and there is no icon
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:266-277`,
     `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:154-163`).
     It then adds the spending to the scheduled budget and saves that budget
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:328-338`).
     It logs `scheduled_modified`/`Save` when recurrence ≠ Never
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:294-301`).
  6. The row menu offers view, edit, clone and remove
     (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/ScheduledSpendingAdapter.kt:27-61`).
- **Rules:**
  - Template spendings and their expenditures live in budget −2. They never appear in a
    real budget's totals, because totals are summed per budget
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:74-83`).
  - Budget −2 has its own plan and fact, calculated like any budget
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:74-83`).
- **Edge cases:**
  - **Clone** sets `date = now` and does not copy `recurrence`. A new spending defaults to
    `Never`
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:122-132`,
    `android/legacy/src/main/java/com/mas/mobile/repository/budget/SpendingRepositoryImpl.kt:29`).
  - **The recurrence picker** always pre-selects the first option (Never), whatever the
    current value
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingFragment.kt:90`).
- **Data touched:** Writes `spendings` (incl. `recurrence`, `date`) and `expenditures`
  (`budget_id` = −2). Recalculates `budgets` plan and fact for id −2.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingListFragment.kt:111-135`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/adapter/ScheduledSpendingAdapter.kt:13-75`

## UC-L1.10: Materialise scheduled spendings into the active budget

- **Actor:** system
- **Trigger:** `ScheduledSpendingWorker`, periodic every 2 hours
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:164-169`,
  `android/legacy/src/main/java/com/mas/mobile/service/ScheduledSpendingWorker.kt:14-17`).
- **Preconditions:** Budget −2 exists. Otherwise the event `scheduled_budget_not_found` is
  logged and nothing else happens
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:133-134`,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:149-151`).
- **Main flow:**
  1. Load budget −2 with its spendings
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:133-135`).
  2. Select the spendings where `scheduledDate != null` and `scheduledDate < now`
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:136`).
  3. For each one, call `spend(date = scheduledDate, amount, comment, expenditure name,
     currency = exchangeInfo?.currency)`
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:137-145`).
     This adds a new spending to the **active** budget, creating the expenditure there if
     needed
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:97-120`).
  4. Set the template's `date` to that `scheduledDate`
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:146`).
  5. Save budget −2
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:148`).
- **Rules:** `scheduledDate` is computed as follows
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:28-49`).
  - `limit = end of today` if the template `date` falls on today. Otherwise
    `limit = start of today`.
  - `scheduledDate` is the first element of `date, date+step, date+2·step, …` that is
    `≥ limit`.
  - The step is Daily +1 day, Weekly +1 week, Monthly +1 month, Quarterly +3 months.
  - For `Never` the sequence is only `date`.

  These follow from the formula:
  - **A `Never` template is never selected.** Its `scheduledDate` is non-null only before
    its own date, and then it is not `< now`.
  - **The occurrence on the template's own date is never materialised.** On that day the
    `limit` is end of day, so `scheduledDate` jumps to `date + step`.
  - **The candidate is fixed to the run day.** It is the earliest `date + k·step` (k ≥ 1)
    on or after the start of the run day. It is materialised only if a run happens after
    its time on that same day. If no run falls between the occurrence time and midnight,
    the next day's run moves on to the following candidate, and the occurrence is lost.
    Example: Daily at 23:00, with runs at 21:30 and 00:30, loses one occurrence.
  - **A template dated in the past** starts from the first candidate on or after the
    start of the day it is saved. Earlier occurrences are not materialised.
  - **At most one occurrence is materialised per template per run.** After that, `date`
    is today, so the next candidate is at least one step later
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:136-147`).
  - **Each occurrence goes to the budget active at run time**, whatever budget covers its
    date (UC-L1.1).
- **Edge cases:**
  - **Foreign-currency template.** `spend` receives the template's `amount`, which is
    already in the budget currency, together with the foreign currency. It treats that
    amount as the foreign raw amount and multiplies it by the current rate
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:108-115`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:22-26`).
  - **Rate lookup fails.** A toast is shown and the rate is `0.00`, so the materialised
    amount is 0
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:123-128`).
  - **Comment** is trimmed
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:102`).
  - **Exceptions** in the background task are shown as a toast
    (`android/legacy/src/main/java/com/mas/mobile/service/CoroutineService.kt:11-25`).
- **Data touched:**
  - Writes `spendings` (new rows in the active budget).
  - Writes `expenditures` (if missing).
  - Recalculates `budgets` plan and fact for the active budget.
  - Updates `spendings.date` of the template (budget −2).
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:92-152`,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:19-50`

## Open questions

1. If two budgets cover the same day, which one is active? The query has no `ORDER BY`
   (`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:34-35`).
   Normal creation cannot produce an overlap. Import (area 9) has not been checked yet.
2. What happens when `createNext` throws, for example on duplicate category names
   (UC-L1.2)? None of these call sites catch the exception:
   - `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:44`
   - `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:57`
   - `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:97`
   - `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:124`
   - `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:69`
   - `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:46`
   - `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetListViewModel.kt:17`
   - `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetViewModel.kt:47`
3. Until the Settings screen saves settings, `startDayOfMonth` and `startDayOfWeek`
   default to values computed from the read date
   (`android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:11-12`). When
   settings are first persisted depends on the Settings screen's save-on-observe
   behaviour, which is covered in area 6.
4. If the process survives a date change while `MainActivity` is recreated, the new
   `DateListener` starts with today's date (UC-L1.8). It is not clear whether the shared
   active-budget source is then refreshed.
5. In VIEW mode, the `lastDayAt` click listener is set regardless of `isEditable`
   (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetFragment.kt:58-69`).
   Its parent layout is disabled
   (`android/legacy/src/main/res/layout/budget_fragment.xml:64`). Whether the picker opens
   in VIEW mode depends on Android widget behaviour.
6. Cascade deletes depend on SQLite foreign-key enforcement. Room enables it in generated
   code, which is not in the sources.
7. What did `budgets.isActive` mean? It is stored but never set
   (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetMapper.kt:23-33`).
8. `action_to_budget_template` (default `budget_id` 1) and
   `action_to_template_expenditures` (default −1) are declared, but nothing uses them
   (`android/legacy/src/main/res/navigation/nav_graph.xml:238-245`,
   `android/legacy/src/main/res/navigation/nav_graph.xml:508-517`). Migration 3→4 set budget
   1's `startsOn` to `LocalDate.MAX`, and 8→9 deleted it
   (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:109-115`,
   `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:195-196`).
   This suggests an earlier "template budget" concept, but the code does not say so.

## Surprises

- **Scheduled spendings: occurrences lost.** The occurrence on a template's own date is
  never materialised, and `Never` templates never are. Any occurrence with no worker run
  between its time and midnight is lost (UC-L1.10,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:28-49`).
- **Endless future budgets.** If no budget covers today and the last budget lies in the
  future, every active-budget lookup creates another future budget (UC-L1.2,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:70-79`).
- **Scheduled spendings: double conversion.** Foreign-currency templates are converted
  twice when materialised
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:139-145`,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:108-115`).
- **Scheduled spendings: wrong budget for past dates.** A materialised occurrence dated in
  a past period is added to the current active budget
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:97`).
- **Side-effect creation.** Looking up an exchange rate or opening the chart screen can
  create and save a new budget
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:124`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:69`).
- **Add form saves on open.** Opening the budget form in ADD mode saves a new budget before
  the user presses Save
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetViewModel.kt:47`).
- **Dead options menu.** The budget list handles `budget_list_menu` items, but the menu is
  never inflated
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:45-52`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ListFragment.kt:15-17`).
- **Inconsistent delete rule.** The list enforces "remove only from the end", but the form
  does not
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:66-74`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetFragment.kt:86-89`).
- **Dangling links after delete.** Deleting a budget leaves `spending_messages.spending_id`
  pointing at deleted spendings (UC-L1.7). The link is cleared only for spendings removed
  through a budget save
  (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:77-83`,
  `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/SpendingMessageDAO.kt:38-39`).
- **Plan overwritten.** A manually edited budget `plan` is overwritten by the next
  recalculation
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:74-83`).
- **No currency conversion.** Changing a budget's currency does not convert any amount
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/BudgetViewModel.kt:84-87`).
- **Clone loses recurrence.** Cloning a scheduled spending drops its recurrence
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:122-132`).
- **Category order shifted.** A category's `displayOrder` is copied to the new expenditure
  and then replaced by `max + 1`, where the max includes the copied value. Seeded orders
  1–19 become 2–20
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:176`,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:37-38`).
