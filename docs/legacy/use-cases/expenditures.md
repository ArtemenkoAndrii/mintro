# Area 2: Expenditures

Scope:
- Expenditures created from categories, created manually, and created implicitly from a
  spending.
- Name uniqueness and display order.
- Viewing, editing, cloning and deleting expenditures.
- The plan / fact / progress calculation.

Out of scope here:
- The spending form (area 3).
- Category management (area 4).
- How budgets themselves are created (area 1).

All citations use paths relative to the repository root.

## UC-L2.1: View expenditures of the active budget (Expenditures tab)

- **Actor:** user
- **Trigger:** Opening the app or the Expenditures tab. It is the start destination, with
  `budget_id` default −1
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:55-59`,
  `android/legacy/src/main/res/navigation/nav_graph.xml:54-63`).
- **Preconditions:** None. The active budget is resolved, and created if needed, by UC-L1.1
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:63-68`).
- **Main flow:**
  1. Observe the active budget. Each time it emits, reload its expenditures from the
     database, ordered by `display_order, name`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureListViewModel.kt:22-23`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/ExpenditureDAO.kt:10-12`).
  2. Set the screen title to the budget name
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ExpenditureListFragment.kt:41-43`).
  3. Show a header with a progress bar for the budget's progress and the status text
     `fact / plan`
     (`android/legacy/src/main/res/layout/expenditure_list_fragment.xml:47`,
     `android/legacy/src/main/res/layout/expenditure_list_fragment.xml:52`,
     `android/legacy/src/main/res/layout/expenditure_list_fragment.xml:61`,
     `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:22-23`).
  4. Each row shows an icon, the name, a progress text and the fact amount
     (`android/legacy/src/main/res/layout/expenditure_list_row.xml:43`,
     `android/legacy/src/main/res/layout/expenditure_list_row.xml:51`,
     `android/legacy/src/main/res/layout/expenditure_list_row.xml:59`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/adapter/ExpenditureAdapter.kt:22`).
     The row menu is hidden
     (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/ExpenditureAdapter.kt:18-19`).
  5. Tapping a row opens a new spending pre-filled with this expenditure. Long-tapping
     opens this expenditure's spending list
     (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/ExpenditureAdapter.kt:24-41`).
     The add button opens a new spending
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ExpenditureListFragment.kt:56-58`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ExpenditureListFragment.kt:74-75`).
- **Rules:**
  - The header colour depends on budget progress: red if > 150, yellow if > 100,
    otherwise the accent colour
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureListViewModel.kt:40-45`).
  - Budget progress is `toInt(fact × 100 / plan)`, with no zero guard
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:22`).
  - Row progress uses the expenditure formula (UC-L2.9). When `|plan| < 0.001`, the row
    text is "unplanned"
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/binding/Adapter.kt:110-118`,
    `android/legacy/src/main/res/values/strings.xml:126-127`).
  - A missing icon falls back to a generic circle
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ExpenditureListFragment.kt:79-82`).
- **Edge cases:** On this tab there is no direct way to add, edit or delete an expenditure.
  A new spending saved with an unknown name still creates an expenditure implicitly
  (UC-L2.5).
- **Data touched:** Reads `budgets`, `expenditures`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ExpenditureListFragment.kt:25-83`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureListViewModel.kt:15-51`

## UC-L2.2: View and manage expenditures of a chosen budget

- **Actor:** user
- **Trigger:** "Expenditures" from a budget's row menu or the budget form
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:59`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetFragment.kt:90`).
- **Preconditions:** The budget exists.
- **Main flow:**
  1. The list is the same as in UC-L2.1, but for the given `budget_id`
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:63-68`).
     Two observers set the title:
     - The subclass sets "budget name + current title"
       (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ExpenditureListFragment.kt:93-96`).
     - The base class, registered afterwards, sets the budget name alone
       (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ExpenditureListFragment.kt:41-43`,
       `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ListFragment.kt:53-57`).
  2. Tapping a row opens the expenditure in VIEW mode. Long-tapping opens its spending list
     (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/BudgetExpenditureAdapter.kt:56-71`).
  3. The row menu offers View, Edit, Clone and Remove
     (`android/legacy/src/main/res/menu/standard_row_menu.xml:3-22`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/adapter/BudgetExpenditureAdapter.kt:19-54`).
  4. The add button opens a new expenditure for this budget (UC-L2.4)
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ExpenditureListFragment.kt:99-102`).
- **Rules:** As in UC-L2.1.
- **Edge cases:** None beyond UC-L2.4 to UC-L2.8.
- **Data touched:** Reads `budgets`, `expenditures`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/adapter/BudgetExpenditureAdapter.kt:13-73`

## UC-L2.3: Create expenditures from categories when a budget is created

- **Actor:** system
- **Trigger:** UC-L1.2 step 6
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:84`).
- **Preconditions:** None. Inactive categories are skipped.
- **Main flow:**
  1. Read all categories in `display_order, name` order and keep the active ones
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:170`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/CategoryDAO.kt:16-17`).
  2. For each one, create an expenditure with a new id from `generator`. Copy the
     category's `name`, `plan`, icon and `displayOrder`
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:171-177`,
     `android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureRepositoryImpl.kt:13-23`).
  3. Add the expenditure to the budget (UC-L2.10 for order, UC-L2.9 for totals)
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:178`).
- **Rules:**
  - Names must be unique within the budget, ignoring case
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:85-91`).
  - `fact` starts at 0 and `comment` at `""`
    (`android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureRepositoryImpl.kt:13-23`).
  - Merchants and description are not copied
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:171-177`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/Category.kt:9-10`).
- **Edge cases:** Duplicate category names make `require` throw (see UC-L1.2).
- **Data touched:** Writes `expenditures`: `id`, `name`, `plan`, `fact`, `icon`,
  `budget_id`, `display_order`, `comment`. Reads `categories`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:169-180`

## UC-L2.4: Add an expenditure manually

- **Actor:** user
- **Trigger:** The add button on a chosen budget's expenditure list (UC-L2.2 step 4).
- **Preconditions:** The target budget exists. Budget id −1 means the active budget
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:42-48`,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:55-61`).
- **Main flow:**
  1. Create an empty expenditure with a new id, in edit mode
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:52`).
  2. The user enters the name, plan and comment. The fact field is always disabled
     (`android/legacy/src/main/res/layout/expenditure_fragment.xml:27`,
     `android/legacy/src/main/res/layout/expenditure_fragment.xml:46`,
     `android/legacy/src/main/res/layout/expenditure_fragment.xml:64`,
     `android/legacy/src/main/res/layout/expenditure_fragment.xml:81`).
     The name field suggests every distinct expenditure name in the database
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:32-36`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ExpenditureFragment.kt:45-49`).
  3. Save runs the validators (Rules). If they pass, the expenditure is added to the budget
     and the budget is saved in the background
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ItemViewModel.kt:19-29`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:119-125`,
     `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:50-58`).
- **Rules:**
  - The name is trimmed when set, and its trimmed length must be ≥ 3
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:72-76`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:115`).
  - "Already exists" if the budget has an expenditure whose lower-cased name, not trimmed,
    equals the entered name trimmed and lower-cased
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:94-105`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:29-30`,
    `android/legacy/src/main/res/values/strings.xml:136`).
  - `Budget.addExpenditure` checks the same thing again with `require`
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:85-91`).
  - The icon is not set on this path. A manual expenditure has no icon
    (`android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureRepositoryImpl.kt:17`).
- **Edge cases:**
  - Plan defaults to 0.00
    (`android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureRepositoryImpl.kt:18`).
    Such an expenditure shows as "unplanned" (UC-L2.1).
  - The name suggestions include names from all budgets, including the scheduled budget.
    `getExpenditureNames` ignores its `sortByRating` and `limit` arguments
    (`android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureRepositoryImpl.kt:25-26`,
    `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/ExpenditureDAO.kt:18-19`).
- **Data touched:** Writes `expenditures` (new row) and `generator`. Updates `budgets.plan`
  and `fact`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:14-131`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ExpenditureFragment.kt:15-62`

## UC-L2.5: Create an expenditure implicitly when recording a spending

- **Actor:** user (spending form) or system (captured message, scheduled spending)
- **Trigger:** Any of:
  - Saving a spending whose expenditure name is not in the target budget
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:266-277`).
  - `BudgetService.spend`, called for matched messages and scheduled spendings
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:97-99`).
- **Preconditions:** The name is not found in the budget. The stored name, lower-cased, is
  compared with the given name, trimmed and lower-cased
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:29-30`).
- **Main flow:**
  1. Create an expenditure with a new id and the given name
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:154-163`).
  2. If a category has the same name (case-insensitive, trimmed), copy its `plan` and
     icon. Otherwise `plan` = 0.00 and there is no icon
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:155-162`).
  3. Add the expenditure to the target budget when the spending is added
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:55-58`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:331-336`).
- **Rules:**
  - On the spending form, the name must be ≥ 3 characters after trimming
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:162-167`).
  - `spend` has no length check
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:92-99`).
  - For matched messages, the name is the category name
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:119-127`).
  - For scheduled spendings, the name is the template's expenditure name
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:143`).
  - The category's `displayOrder` is not copied here. Order comes from UC-L2.10.
- **Edge cases:**
  - The category lookup uses the trimmed name, but the expenditure stores the name as
    passed in, untrimmed
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:155-159`).
    The spending form passes a trimmed name
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:163`).
  - Renaming an expenditure (UC-L2.6) does not affect scheduled templates or category
    names. The next implicit creation with the old name adds a new expenditure. This
    follows from the name-based lookup above. No code propagates renames.
- **Data touched:** Writes `expenditures` and `generator`. Reads `categories`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:154-163`

## UC-L2.6: View and edit an expenditure

- **Actor:** user
- **Trigger:** Any of:
  - A row tap (VIEW), or the row menu's View or Edit (UC-L2.2).
  - The form's Edit menu item
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ItemFragment.kt:66-89`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ExpenditureFragment.kt:59-61`).
- **Preconditions:** The expenditure belongs to the given budget. Otherwise
  `ItemNotFoundException` is thrown
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:53-54`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:67`).
- **Main flow:**
  1. Load the budget, then the expenditure from it
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:42-54`).
  2. In edit mode, `name`, `plan` and `comment` are editable. `fact` is never editable
     (`android/legacy/src/main/res/layout/expenditure_fragment.xml:27`,
     `android/legacy/src/main/res/layout/expenditure_fragment.xml:46`,
     `android/legacy/src/main/res/layout/expenditure_fragment.xml:64`,
     `android/legacy/src/main/res/layout/expenditure_fragment.xml:81`).
  3. Save validates, then replaces the item with the same id in the budget, recalculates
     the totals and saves
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:32-45`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:122-125`).
- **Rules:**
  - The name rules are the same as in UC-L2.4.
  - The "already exists" check runs only when the entered name differs from the original
    name, compared case-sensitively
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:99-105`).
  - Editing an existing item keeps its `displayOrder`
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:39-41`).
  - A changed `plan` changes `budget.plan` through recalculation (UC-L2.9).
- **Edge cases:**
  - **Changing only the letter case of the name** (for example `Food` → `food`) differs
    from the original, so the existence check runs. `findExpenditure` matches the
    expenditure itself, ignoring case, and validation fails with "Already exists"
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:100-101`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:29-30`).
  - **Changing only surrounding whitespace** fails the same way. The comparison uses the
    raw `name.value`, not the trimmed name
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:95`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:100`).
  - **Renaming** changes only this budget's row. Categories, other budgets and scheduled
    templates are unchanged.
- **Data touched:** Updates `expenditures.name`, `plan`, `comment`. Updates `budgets.plan`
  and `fact`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:50-105`

## UC-L2.7: Clone an expenditure

- **Actor:** user
- **Trigger:** Row menu → Clone on a chosen budget's expenditure list
  (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/BudgetExpenditureAdapter.kt:31-36`).
- **Preconditions:** The source expenditure exists in the budget.
- **Main flow:**
  1. Create a new expenditure with a new id. Copy `name`, `plan` and `comment` from the
     source, and open it in edit mode
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:55-65`).
  2. Save works as in UC-L2.4.
- **Rules:**
  - The icon and `fact` are not copied
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:58-63`).
  - The clone goes into the same budget as the source
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:120`).
- **Edge cases:** The copied name already exists in the same budget, so saving without
  renaming fails with "Already exists"
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:94-105`).
- **Data touched:** Same as UC-L2.4.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:55-65`

## UC-L2.8: Delete an expenditure

- **Actor:** user
- **Trigger:** One of:
  - Row menu → Remove on a chosen budget's list, with confirmation
    (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/BudgetExpenditureAdapter.kt:37-43`).
  - The form's Remove menu item, with confirmation
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ItemFragment.kt:78-96`).
- **Preconditions:** None.
- **Main flow:**
  1. Remove the expenditure from the budget's in-memory list and recalculate the totals
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:47-53`).
     - The list path uses
       `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureListViewModel.kt:33-38`.
     - The form path uses
       `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:127-130`.
  2. Save the budget. The removed id is deleted with `DELETE FROM expenditures WHERE id = ?`
     (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:62-71`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/ExpenditureDAO.kt:53-54`).
  3. The expenditure's spendings are removed by the declared `ON DELETE CASCADE`
     (`android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Spending.kt:41-47`).
- **Rules:**
  - Budget totals after removal: `fact = Σ fact` of the remaining expenditures (UC-L2.9).
  - There is no check for existing spendings, and no warning about them beyond the generic
    "are you sure" dialog
    (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/BudgetExpenditureAdapter.kt:39`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ItemFragment.kt:91-96`).
- **Edge cases:**
  - The cascaded spendings are not in the budget's "deleted" set. Their linked messages
    therefore keep `spending_id`. Only spendings removed explicitly have it cleared
    (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:77-83`).
  - Deleting is not possible from the Expenditures tab (UC-L2.1).
- **Data touched:** Deletes from `expenditures` and `spendings` (cascade). Updates
  `budgets.plan` and `fact`. `spending_messages` is left unchanged.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:47-53`,
  `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:62-91`

## UC-L2.9: Calculate plan, fact and progress

- **Actor:** system
- **Trigger:** Every `addExpenditure`, `removeExpenditure`, `addSpending` or
  `removeSpending` on a budget
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:32-72`).
- **Preconditions:** The budget details (expenditures and spendings) are loaded
  (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:117-135`).
- **Main flow:**
  1. For each expenditure `e`: `e.fact = Σ amount` of the spendings whose expenditure id
     is `e.id`.
  2. `budget.plan = Σ e.plan`.
  3. `budget.fact = Σ e.fact`.

  Source: `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:74-83`.
- **Rules:**
  - `amount` is the budget-currency amount. For foreign spendings it is the converted
    amount (area 3)
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:22-26`).
  - `Expenditure.progress = 100` if `|plan| < 0.001`. Otherwise it is
    `toInt(fact × 100 / plan)`
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Expenditure.kt:15-20`).
  - `Budget.getProgress() = toInt(fact × 100 / plan)`, with no zero guard
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:22`).
  - Values are recalculated in memory, then stored in `expenditures.fact`,
    `budgets.plan` and `budgets.fact` when the budget is saved
    (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:50-58`).
- **Edge cases:**
  - Stored `plan` and `fact` are recalculated values. They are not recomputed on read: the
    mappers copy the stored columns as they are
    (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetMapper.kt:10-21`,
    `android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureMapper.kt:10-20`).
    Values edited directly (budget plan in UC-L1.6) stay until the next recalculation.
  - Budget progress with `plan = 0` divides by zero in floating point. Only the budget-list
    row text guards against it ("unplanned")
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/binding/Adapter.kt:120-128`,
    `android/legacy/src/main/res/layout/budget_list_row.xml:43`).
    The Expenditures-tab progress bar and colour use the unguarded value
    (`android/legacy/src/main/res/layout/expenditure_list_fragment.xml:47`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureListViewModel.kt:40-45`).
- **Data touched:** `expenditures.fact`, `budgets.plan`, `budgets.fact`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:22-83`,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/Expenditure.kt:15-20`

## UC-L2.10: Order expenditures

- **Actor:** system
- **Trigger:** Adding a new expenditure to a budget
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:32-45`).
- **Preconditions:** None.
- **Main flow:**
  1. Append the expenditure to the budget's list.
  2. Set its `displayOrder = max(displayOrder of all expenditures in the budget,
     including itself) + 1`
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:36-38`).
  3. Lists sort by `display_order`, then `name`
     (`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/ExpenditureDAO.kt:10-16`).
- **Rules:**
  - Expenditures created from categories keep the relative category order (`display_order`,
    then `name`). Each one gets `max(current max, its category displayOrder) + 1`. For
    example, category orders 1, 5, 6 become 2, 6, 7
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:176`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:36-38`).
  - Expenditures created manually or implicitly start with `displayOrder` 0, so they go to
    the end
    (`android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureRepositoryImpl.kt:22`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:38`).
  - Replacing an existing item (edit) does not change its order
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:39-41`).
- **Edge cases:** There is no user action to reorder expenditures. Drag-to-reorder exists
  only on the category list
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryListFragment.kt:45-46`).
- **Data touched:** `expenditures.display_order`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:32-45`

## Open questions

1. `Budget.getProgress()` with `plan = 0` produces an infinite or NaN double, which is
   then converted with `toInt()`
   (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:22`). The
   resulting progress-bar value and colour on the Expenditures tab depend on Kotlin
   conversion rules and widget clamping, not on app code.
2. `ExpenditureRepositoryAdapter` loads its own copy of the budget when the form opens
   (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:120`).
   If the same budget changes elsewhere while the form is open, the outcome of the save
   is not determined by the code shown.
3. Expenditure name suggestions accept `sortByRating` and `limit` (20), which are ignored
   (`android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureRepositoryImpl.kt:25-26`).
   The intended ranking is unknown.

4. Which title does a chosen budget's expenditure list end up showing (UC-L2.2)? Two
   observers set it, and the result depends on the order in which LiveData notifies
   them. That is framework behaviour, not app code.

## Surprises

- **Case-only rename blocked.** Renaming an expenditure only by letter case or surrounding
  whitespace fails with "Already exists"
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:99-105`).
- **Clone can't be saved as is.** A cloned expenditure keeps the source name, so saving it
  unchanged always fails
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ExpenditureViewModel.kt:55-65`).
- **Dangling message links.** Deleting an expenditure cascades to its spendings, but
  their messages keep `spending_id`
  (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:77-83`).
- **No icons for manual expenditures.** Manually added expenditures never get an icon.
  Implicit ones get the icon only if a category has the same name
  (`android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureRepositoryImpl.kt:17`,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:160`).
- **Inconsistent zero-plan progress.** Expenditure progress treats `plan = 0` as 100 %,
  but budget progress does not guard against zero
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Expenditure.kt:15-20`,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:22`).
- **Untrimmed implicit names.** Implicit creation stores the untrimmed name, while the
  category lookup uses the trimmed one
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:155-159`).
- **Ignored arguments.** Name suggestions ignore their `sortByRating` and `limit`
  arguments
  (`android/legacy/src/main/java/com/mas/mobile/repository/budget/ExpenditureRepositoryImpl.kt:25-26`).
