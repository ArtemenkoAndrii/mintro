# Area 4: Categories and merchants

Scope:
- Category CRUD: name, icon, plan, active flag, description.
- Display order and drag-to-reorder.
- Merchant lists.
- How categories are used to build expenditures and to match captured messages.
- Seed categories.

Covered elsewhere:
- How expenditures copy from categories (UC-L1.2, UC-L2.3, UC-L2.5).
- The merchant prompt on the spending form (UC-L3.10).
- Message matching (area 5).

All citations use paths relative to the repository root.

## UC-L4.1: Browse categories

- **Actor:** user
- **Trigger:** Menu tab → Categories
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MenuFragment.kt:28`).
- **Preconditions:** None.
- **Main flow:**
  1. Observe all categories, ordered by `display_order`, then `name`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryListViewModel.kt:15`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/CategoryDAO.kt:13-14`).
  2. Each row shows the icon (a generic circle if none), the name and the plan.
     Inactive rows are drawn at 50 % opacity
     (`android/legacy/src/main/res/layout/category_list_row.xml:17`,
     `android/legacy/src/main/res/layout/category_list_row.xml:43`,
     `android/legacy/src/main/res/layout/category_list_row.xml:51`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryListFragment.kt:62-65`).
  3. Tapping a row opens it in VIEW mode. The row menu offers View, Edit, Clone and Remove
     (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/CategoryAdapter.kt:21-58`).
     The add button opens a new category
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryListFragment.kt:54-57`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryListFragment.kt:67-68`).
- **Rules:** The plan has no currency attached. Category has no currency field
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Category.kt:3-12`).
- **Edge cases:** Long-press starts a drag (UC-L4.6), not edit mode
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryListFragment.kt:105-108`).
- **Data touched:** Reads `categories`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryListFragment.kt:24-69`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/adapter/CategoryAdapter.kt:13-62`

## UC-L4.2: Add a category

- **Actor:** user
- **Trigger:** The add button on the category list (UC-L4.1).
- **Preconditions:** None.
- **Main flow:**
  1. Create a category with a new id from `generator`, `isActive = true`, empty name and
     description, no merchants, plan 0, and no icon. The form opens in edit mode
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:106`,
     `android/legacy/src/main/java/com/mas/mobile/repository/budget/CategoryRepositoryImpl.kt:25-35`).
  2. The user sets:
     - a name (maximum 30 characters);
     - an icon, through the icon picker;
     - a description (maximum 200 characters);
     - a plan;
     - the active switch;
     - merchants (UC-L4.7).

     Sources: `android/legacy/src/main/res/layout/category_fragment.xml:40`,
     `android/legacy/src/main/res/layout/category_fragment.xml:69`,
     `android/legacy/src/main/res/layout/category_fragment.xml:90`,
     `android/legacy/src/main/res/layout/category_fragment.xml:114`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryFragment.kt:92-104`.
  3. Save validates the name. If valid, set `displayOrder = max(all categories) + 1`,
     upsert the row, and log `category_modified`/`Save`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:124-130`,
     `android/legacy/src/main/java/com/mas/mobile/repository/budget/CategoryRepositoryImpl.kt:37-39`).
- **Rules:**
  - The name is trimmed when set, and its trimmed length must be ≥ 3
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:66-74`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:144`).
  - The name must be unique, ignoring case, among categories with a different id. Stored
    names are compared as stored, without trimming
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:99-102`).
  - Only the first failing rule's message is shown
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ItemViewModel.kt:59-66`).
- **Edge cases:**
  - The name validator is registered inside the name observer
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:66-75`).
    Every name change registers another validator instance under a new key
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ItemViewModel.kt:56-57`).
    All of them check the current name.
  - The icon picker returns the chosen icon id. If the picker is cancelled, the icon is
    unchanged
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryFragment.kt:37-44`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/IconPickerActivity.kt:27-39`).
- **Data touched:** Writes `categories`: `id`, `name`, `plan`, `active`, `description`,
  `merchants`, `icon`, `display_order`. Writes `generator`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:19-146`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryFragment.kt:24-114`

## UC-L4.3: View and edit a category

- **Actor:** user
- **Trigger:** A row tap (VIEW), the row menu's View or Edit, or the form's Edit menu item
  (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/CategoryAdapter.kt:21-40`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ItemFragment.kt:71-89`).
- **Preconditions:** The category exists. Otherwise `ItemNotFoundException` is thrown
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:107-108`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:122`).
- **Main flow:**
  1. Load the category. Fields, merchant chips and the icon are enabled only in edit mode
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryFragment.kt:106-111`,
     `android/legacy/src/main/res/layout/category_fragment.xml:31`,
     `android/legacy/src/main/res/layout/category_fragment.xml:63`,
     `android/legacy/src/main/res/layout/category_fragment.xml:80`,
     `android/legacy/src/main/res/layout/category_fragment.xml:115`).
  2. Save works as in UC-L4.2 step 3, except that `displayOrder` is kept
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:125-127`).
- **Rules:** Edits change only the `categories` row. Nothing updates existing
  expenditures, so earlier budgets are unaffected. The sections below show where a
  category is read.
- **Edge cases:**
  - **Rename.** Expenditures are linked to categories only by name
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:154-163`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:17-20`).
    After a rename, implicitly created expenditures with the old name no longer get the
    category's plan and icon. The merchant prompt (UC-L3.10) no longer finds the category
    for those expenditures.
  - **Deactivate.** It affects only budgets created afterwards
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:170`).
    An inactive category is still used for merchant matching and implicit creation, which
    do not check `isActive`
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:12-20`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:156`).
- **Data touched:** Updates `categories`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:58-130`

## UC-L4.4: Clone a category

- **Actor:** user
- **Trigger:** Row menu → Clone
  (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/CategoryAdapter.kt:41-45`).
- **Preconditions:** None. If the source is missing, an empty new category opens.
- **Main flow:**
  1. Create a new category (new id). Copy `name`, `plan`, `isActive` and `description` from
     the source
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:109-120`).
  2. Save works as in UC-L4.2, including `displayOrder = max + 1`.
- **Rules:** The icon and merchants are not copied.
- **Edge cases:** The copied name collides with the source, so saving without renaming
  fails with "Already exists"
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:99-102`).
- **Data touched:** Same as UC-L4.2.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:109-120`

## UC-L4.5: Delete a category

- **Actor:** user
- **Trigger:** One of:
  - Row menu → Remove, with confirmation
    (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/CategoryAdapter.kt:46-53`).
  - The form's Remove menu item
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ItemFragment.kt:78-96`).
- **Preconditions:** None.
- **Main flow:**
  1. Delete the `categories` row
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ListViewModel.kt:12-16`,
     `android/legacy/src/main/java/com/mas/mobile/repository/budget/CategoryRepositoryImpl.kt:41-43`).
  2. The form path also logs `category_modified`/`Remove`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:132-135`).
- **Rules:**
  - Nothing references a category by id, so there is no cascade and no check. Existing
    expenditures with the same name remain.
  - Merchants stored on the category are deleted with it.
- **Edge cases:**
  - The list path does not log an analytics event.
  - After deletion, messages from that category's merchants are no longer turned into
    spendings automatically, unless another category lists the same merchant (UC-L4.9,
    `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:119`).
- **Data touched:** Deletes from `categories`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/repository/budget/CategoryRepositoryImpl.kt:41-43`

## UC-L4.6: Reorder categories by drag

- **Actor:** user
- **Trigger:** Long-press a row, drag it up or down, release
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryListFragment.kt:45-48`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryListFragment.kt:75-108`).
- **Preconditions:** None.
- **Main flow:**
  1. While dragging, `onMove` swaps the items at the dragged and target positions in the
     adapter's list (`Collections.swap`)
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryListFragment.kt:84-91`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/adapter/BaseAdapter.kt:61-64`).
  2. On release (idle state), call `reorder(oldItems, items)`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryListFragment.kt:93-99`).
     `oldItems` is a copy of the list taken at the last `setItems`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/BaseAdapter.kt:50-59`).
  3. `reorder` stops if the lists differ in size. For every position `i` where the
     category id changed, set `new[i].displayOrder = old[i].displayOrder`. Then save each
     changed category in the background
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:22-35`).
- **Rules:** The moved categories take over the existing `displayOrder` values by position.
  No new values are created.
- **Edge cases:**
  - **Equal display orders.** If several categories share a `displayOrder`, swapping them
    reassigns the same value, so the stored `display_order` values do not change
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:25-27`).
    The next load is ordered by `display_order, name`
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/CategoryDAO.kt:13-14`).
    It returns those categories in name order again, so the drag does not persist. Migration 9→10 gives every existing category `display_order = 0`
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:206`).
  - **No-op drag.** If `reorder` receives lists with the same ids in the same order, no
    position differs and nothing is saved
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:25-27`).
    Whether a long-press without a move triggers this call is ItemTouchHelper behaviour.
- **Data touched:** Updates `categories.display_order`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:22-35`

## UC-L4.7: Manage a category's merchants

- **Actor:** user
- **Trigger:** The merchant input and chips on the category form, in edit mode
  (`android/legacy/src/main/res/layout/category_fragment.xml:130-161`).
- **Preconditions:** The form is in edit mode.
- **Main flow:**
  1. The user types a merchant (maximum 30 characters) and taps add. The value is trimmed.
     If its length is ≥ 3, it is added to the set and the input is cleared. Otherwise an
     error is shown
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:39-48`,
     `android/legacy/src/main/res/layout/category_fragment.xml:140`).
  2. Tapping a chip's close icon removes the merchant
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryFragment.kt:71-85`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:50-52`).
  3. The merchant list is saved with the category (UC-L4.3). It is stored as a JSON array
     of strings in `categories.merchants`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:93-96`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/config/converter/SQLiteTypeConverter.kt:57-66`).
     Seeded rows store an empty string, which is read as an empty list
     (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:138`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/config/converter/SQLiteTypeConverter.kt:63`).
- **Rules:**
  - Within one category, merchants are a set, so exact duplicates are dropped. Values that
    differ only in case are kept as separate entries
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:35`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:93`).
  - There is no uniqueness check across categories. The same merchant can be listed under
    several categories.
- **Edge cases:** Merchant changes are lost unless the category is saved. They change only
  the in-memory model, which is written only by save
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:94-96`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ItemViewModel.kt:45-47`).
- **Data touched:** `categories.merchants`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:35-52`

## UC-L4.8: Link a merchant from a spending

- **Actor:** user
- **Trigger:** Saving a spending created from a message whose merchant no category lists,
  and answering yes (UC-L3.10).
- **Preconditions:** A category exists whose name equals the spending's expenditure name,
  ignoring case
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:17-20`).
- **Main flow:**
  1. Append the merchant to that category's list and save the category
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:285-291`).
- **Rules:** The merchant text is the value extracted from the message, unmodified (area 5).
- **Edge cases:** Without a matching category, nothing is saved (UC-L3.10).
- **Data touched:** `categories.merchants`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:244-252`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:285-291`

## UC-L4.9: Look up a category by merchant or by name

- **Actor:** system
- **Trigger:** Any of:
  - **By merchant:** when a captured message is matched
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:118-119`).
  - **By merchant:** when the spending form decides whether to prompt
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:247`).
  - **By name:** when an expenditure is created implicitly
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:156`).
  - **By name:** when a merchant is linked
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:286`).
- **Preconditions:** None.
- **Main flow:**
  1. Load all categories in `display_order, name` order
     (`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/CategoryDAO.kt:16-17`).
  2. **By merchant:** return the first category with a merchant equal to the value,
     ignoring case
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:12-15`,
     `android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:37-38`).
  3. **By name:** return the first category with an equal name, ignoring case
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:17-20`).
- **Rules:**
  - Matching is on the whole string. There is no substring or fuzzy matching.
  - Inactive categories are included.
  - The first match in display order wins.
- **Edge cases:**
  - **Same merchant in several categories:** the first category in display order wins.
  - **Name lookup in `BudgetService.createExpenditure`** trims the input. `findCategoryByName`
    does not trim
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:155-156`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:17-20`).
- **Data touched:** Reads `categories`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:12-20`

## UC-L4.10: Seed default categories

- **Actor:** system
- **Trigger:** One of:
  - Creation of a new database
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:61-67`).
  - Migration 8→9
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:180-192`).
- **Preconditions:** None.
- **Main flow:**
  1. Insert 19 categories with fixed ids 1–19, English names and descriptions, plans,
     icons, `display_order` 1–19 and no merchants
     (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:136-175`).
- **Rules:**
  - `Auto`, `Childcare`, `Insurance` and `Pet care` are inactive. The rest are active
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:142`,
    `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:154`,
    `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:164`,
    `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:174`).
  - Plans are plain numbers, with no currency.
  - Names are English whatever the device language. Only qualifiers vary by language
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:87-134`).
- **Edge cases:**
  - **Migration 8→9 fails.** It creates `categories` without a `display_order` column
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:181-189`).
    It then runs the seed `INSERT`, which names the `display_order` column
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:192`,
    `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:137`). The
    column is added only in 9→10
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:206`).
  - **Ids 1–19 are fixed.** New categories take ids from `generator`, which starts at 100
    on a fresh install
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:25-28`).
- **Data touched:** Writes `categories`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:136-175`

## Open questions

1. **Upgrades from v8.** Migration 8→9 inserts into a column that does not exist yet
   (UC-L4.10). Did any installation upgrade from schema 8 with this code? If so, what
   happened? Room's handling of a failing migration is framework behaviour. The same
   migration also runs its own `BEGIN TRANSACTION` … `COMMIT` before the categories step
   (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:164-178`).
   Whether that works inside Room's migration transaction is also framework behaviour.
2. **Empty-name save.** The name validator is registered inside `name.observeForever`
   (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:66-75`).
   If LiveData does not deliver the initial value to that observer, a category saved
   without touching the name field has no validator, and an empty name would be accepted.
   That depends on framework behaviour.
3. **Picker without extra.** If the icon picker returns `RESULT_OK` without the extra, the
   icon becomes `IconId(-1)`
   (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CategoryFragment.kt:41-42`).
   `IconPickerActivity` always sets the extra, so the code does not show when this could
   happen.

## Surprises

- **Broken migration.** Migration 8→9 seeds categories with a `display_order` value before
  that column exists
  (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:181-192`,
  `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:137`).
- **Reorder doesn't stick.** Drag-to-reorder does not persist when display orders are
  equal, for example all 0 after migration 9→10
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:22-35`,
  `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:206`).
- **Clone can't be saved as is.** A cloned category keeps the source name, so saving it
  unchanged fails. The icon and merchants are not copied
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:109-120`).
- **Inactive still matches.** Inactive categories still match merchants, and still supply
  plan and icon to implicit expenditures. Only new-budget seeding filters on `isActive`
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:12-20`,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:154-162`,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:170`).
- **Shared merchants.** A merchant can be listed under several categories, and only the
  first in display order is used
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:12-15`).
- **Missing analytics.** Deleting from the list does not log `category_modified`, but
  deleting from the form does
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ListViewModel.kt:12-16`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:132-135`).
