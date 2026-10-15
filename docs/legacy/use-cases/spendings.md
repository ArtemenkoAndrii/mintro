# Area 3: Spendings and currency exchange

Scope:
- Browsing, adding, editing, cloning and deleting spendings.
- Spending dates.
- Foreign-currency entry, conversion and exchange-rate lookup.
- The system entry point `BudgetService.spend`.
- Side effects of saving: linking a message, linking a merchant, analytics.

Covered elsewhere:
- Scheduled spendings and recurrence (area 1, UC-L1.9 and UC-L1.10).
- Expenditure creation (area 2).
- Message classification and template generation (area 5).

All citations use paths relative to the repository root.

## UC-L3.1: Browse recent spendings (Spending tab)

- **Actor:** user
- **Trigger:** The Spending tab, `nav_spending_list`
  (`android/legacy/src/main/res/menu/bottom_menu.xml:10-11`).
- **Preconditions:** None.
- **Main flow:**
  1. Load spendings with `date ≥ start of (today − 30 days)`, newest first
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingListViewModel.kt:21-26`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/SpendingDAO.kt:11-13`).
  2. Drop spendings whose expenditure has `budgetId ≤ 0`, which hides scheduled templates
     (`android/legacy/src/main/java/com/mas/mobile/repository/budget/SpendingRepositoryImpl.kt:37-40`).
  3. Each row shows:
     - an icon, the expenditure name and the time (`HH:mm`);
     - the amount, negated;
     - the comment, hidden if empty;
     - a date header, shown only when the date differs from the previous row's.
     The header reads "Today", "Yesterday" or `dd MMM, yyyy`.

     Sources: `android/legacy/src/main/res/layout/spending_list_row.xml:75`,
     `android/legacy/src/main/res/layout/spending_list_row.xml:83`,
     `android/legacy/src/main/res/layout/spending_list_row.xml:92`,
     `android/legacy/src/main/res/layout/spending_list_row.xml:112`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/adapter/SpendingAdapter.kt:26-38`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/adapter/SpendingAdapter.kt:86-93`,
     `android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:11`,
     `android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:14`.
  4. Tapping a row opens it in VIEW mode. Long-tapping opens EDIT. The row menu offers View,
     Edit, Clone and Remove
     (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/SpendingAdapter.kt:39-83`).
  5. The add button opens a new spending (UC-L3.3)
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingListFragment.kt:49-52`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingListFragment.kt:62-63`).
- **Rules:**
  - The window is a rolling 30 days, filtered by spending date, not by budget.
  - The amount is displayed as `amount × −1` with two decimals
    (`android/legacy/src/main/res/layout/spending_list_row.xml:92`,
    `android/legacy/src/main/res/values/strings.xml:125`).
- **Edge cases:**
  - Spendings from several budgets can appear together.
  - Future-dated spendings appear, because there is no upper bound
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/SpendingDAO.kt:12`).
- **Data touched:** Reads `spendings` and `expenditures`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingListFragment.kt:24-64`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/adapter/SpendingAdapter.kt:16-96`

## UC-L3.2: Browse spendings of a budget or an expenditure

- **Actor:** user
- **Trigger:** One of:
  - A budget's "Spendings" menu item
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:60`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetFragment.kt:91`).
  - A long-tap on an expenditure row
    (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/ExpenditureAdapter.kt:34-41`).
- **Preconditions:** None.
- **Main flow:**
  1. If an `expenditure_id` is given, list that expenditure's spendings. Otherwise, if a
     `budget_id` is given, list the budget's spendings. Both are newest first
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingListViewModel.kt:21-26`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/SpendingDAO.kt:27-37`).
  2. Rows and menus are as in UC-L3.1.
  3. The add button opens a new spending with that budget, and that expenditure if one is
     given
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingListFragment.kt:66-109`).
- **Rules:** There is no 30-day window and no scheduled-budget filter.
- **Edge cases:** For the expenditure list, the title is the expenditure name, or empty if
  the expenditure is not found
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingListFragment.kt:101`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingListViewModel.kt:20`).
- **Data touched:** Reads `spendings`, `expenditures`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingListFragment.kt:66-109`

## UC-L3.3: Add a spending manually

- **Actor:** user
- **Trigger:** Any of:
  - The add button on a spending list (UC-L3.1, UC-L3.2).
  - The add button on the Expenditures tab
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ExpenditureListFragment.kt:74-75`).
  - Tapping an expenditure row, which pre-fills that expenditure
    (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/ExpenditureAdapter.kt:24-32`).
- **Preconditions:** The target budget can be resolved.
  - The default `budget_id` is −1, meaning the active budget
    (`android/legacy/src/main/res/navigation/nav_graph.xml:200-211`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:110-116`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:55-61`).
- **Main flow:**
  1. Create a spending with a new id, `date = now`, `amount = 0`, `recurrence = Never`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:120`,
     `android/legacy/src/main/java/com/mas/mobile/repository/budget/SpendingRepositoryImpl.kt:13-30`).
     The form opens in edit mode
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:104`).
  2. If an `expenditure_id` > 0 is given, pre-fill that expenditure's name. This happens
     only if the expenditure belongs to the resolved budget. Otherwise the name is left
     empty
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:269-273`).
  3. The user enters:
     - the amount (at most 2 decimals while typing);
     - the date and time, through date and time pickers;
     - a comment (maximum 100 characters);
     - the expenditure name (maximum 30 characters, with suggestions of every distinct
       expenditure name);
     - optionally a currency (UC-L3.7).

     Sources: `android/legacy/src/main/res/layout/spending_fragment.xml:119-190`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/binding/Adapter.kt:25-52`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ItemFragment.kt:40-54`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingFragment.kt:77-85`.
  4. Save validates the expenditure name
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:162-167`).
     If valid:
     - Resolve the expenditure by name in the budget, or create it (UC-L2.5)
       (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:275-277`).
     - Add the spending to the budget and save the budget
       (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:331-338`).
     - Run the side effects (UC-L3.10)
       (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:254-259`).
- **Rules:**
  - The only validator is the expenditure name: trimmed length ≥ 3
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:162-167`).
  - `amountError`, `dateError` and `commentError` are declared but never assigned
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:51-55`).
  - An empty amount field binds as 0.0. A comma is accepted as the decimal separator
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/binding/Adapter.kt:46-52`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/converter/MoneyConverter.kt:13-16`).
  - The spending goes into the budget resolved in the preconditions, whatever its date.
    `addSpending` does not look at the date
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:55-67`).
  - The date is shown and parsed as `d.MM HH:mm`, with no year. Parsing fills in the
    current year
    (`android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:12`,
    `android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:26-32`,
    `android/legacy/src/main/res/layout/spending_fragment.xml:149`).
- **Edge cases:**
  - An amount of 0 or a negative amount is accepted, since amount is not validated.
  - A date outside the budget's `startsOn`…`lastDayAt` range is accepted, and the spending
    still counts in that budget.
  - The date/time picker starts from the parsed text, or from now if parsing fails
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ItemFragment.kt:40-46`).
- **Data touched:**
  - Writes `spendings`: `id`, `comment`, `date`, `amount`, `expenditure_id`, `currency`,
    `rate`, `foreignAmount`, `recurrence`.
  - May write `expenditures`.
  - Updates `expenditures.fact` and `budgets.plan`/`fact`.
  - Writes `generator`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:26-338`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingFragment.kt:19-145`

## UC-L3.4: View and edit a spending

- **Actor:** user
- **Trigger:** A row tap (VIEW) or long-tap (EDIT), the row menu, or the form's Edit menu
  item (UC-L3.1).
- **Preconditions:** The spending exists. Otherwise `ItemNotFoundException` is thrown
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:121`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:135`).
- **Main flow:**
  1. Resolve the budget that contains the spending
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:110-113`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:28-29`).
     Load the spending and any message linked to it
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:121`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:137-142`).
  2. Fields are editable only in edit mode
     (`android/legacy/src/main/res/layout/spending_fragment.xml:113`,
     `android/legacy/src/main/res/layout/spending_fragment.xml:136`,
     `android/legacy/src/main/res/layout/spending_fragment.xml:158`,
     `android/legacy/src/main/res/layout/spending_fragment.xml:179`).
  3. Save works as in UC-L3.3 step 4. `addSpending` replaces the item with the same id
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:60-65`).
- **Rules:**
  - Changing the expenditure name moves the spending to another expenditure of the
    **same** budget. If that expenditure does not exist, it is created (UC-L2.5).
  - A spending cannot move to another budget.
- **Edge cases:**
  - **Year from parsing.** Because the date format has no year, the date parsed from the
    text field gets the current year (UC-L3.3). The format also has no seconds, while
    storage keeps milliseconds
    (`android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:51-52`). Open
    question 1 asks whether merely opening and saving a spending changes its year or
    drops its seconds.
  - **Foreign-currency spending.** Opening it reaches the exchange-currency logic in
    UC-L3.7, which can fetch a fresh rate. See open question 2.
- **Data touched:** Same as UC-L3.3.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:110-224`

## UC-L3.5: Clone a spending

- **Actor:** user
- **Trigger:** Row menu → Clone
  (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/SpendingAdapter.kt:56-60`).
- **Preconditions:** The source spending exists. If it is missing, the form opens with an
  empty new spending
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:122-132`).
- **Main flow:**
  1. Create a new spending (new id). Copy from the source:
     - `amount` and `comment`;
     - the expenditure, found or created by name in the source's budget;
     - the `exchangeInfo` object.

     Set `date = now`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:122-132`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:266-267`).
  2. Save works as in UC-L3.3.
- **Rules:**
  - The clone goes into the source spending's budget
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:110-113`).
  - `recurrence` is not copied, so it is `Never`.
- **Edge cases:**
  - Cloning a spending from a past budget adds a spending dated now to that past budget.
  - `exchangeInfo` is the same object as the source's
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:130`).
  - **Message link moves to the clone.** In CLONE mode `message_id` defaults to −1, so the
    form loads the message linked to the **source** spending. Saving the clone sets that
    message's `spending_id` to the clone, which removes the link from the source. The save
    is then logged with source `message`
    (`android/legacy/src/main/res/navigation/nav_graph.xml:212-215`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:137-142`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:279-283`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:296`).
- **Data touched:** Same as UC-L3.3.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:118-135`

## UC-L3.6: Delete a spending

- **Actor:** user
- **Trigger:** One of:
  - Row menu → Remove, with confirmation
    (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/SpendingAdapter.kt:61-68`).
  - The form's Remove menu item, with confirmation
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ItemFragment.kt:78-96`).
- **Preconditions:** None.
- **Main flow:**
  1. **List path:** load the budget by the spending's `expenditure.budgetId`, remove the
     spending, then save the budget
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingListViewModel.kt:37-54`).
  2. **Form path:** remove the spending from the budget loaded when the form opened, then
     save it
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:340-343`).
     Log `spending_modified` (or `scheduled_modified`) with status `Remove`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:261-264`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:304-312`).
  3. On save, each removed id is deleted. Any message linked to it gets
     `spending_id = NULL`
     (`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:77-83`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/SpendingMessageDAO.kt:38-39`).
- **Rules:**
  - `removeSpending` uses `MutableList.remove(spending)`, which relies on data-class
    equality of every field, not on the id
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:69-72`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:10-18`).
  - Totals are recalculated (UC-L2.9).
- **Edge cases:**
  - **Form path, item changed in memory.** If the form's spending object differs from the
    budget's copy in any field, `remove` finds no equal element. Examples are an edited
    field, or a rate refreshed by UC-L3.7. Nothing is deleted, and no error is shown. The
    `Remove` event is still logged
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:261-264`).
    If the date field's inverse conversion runs on open (open question 1), the in-memory
    date loses its seconds, and this mismatch would affect almost every spending.
  - **List path** does not log an analytics event.
  - **Linked message.** On the list path, the `spending_messages` row is kept, with
    `spending_id = NULL`.
- **Data touched:** Deletes from `spendings`. Updates `spending_messages.spending_id`,
  `expenditures.fact` and `budgets.plan`/`fact`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:69-72`,
  `android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:77-91`

## UC-L3.7: Enter a spending in a foreign currency

- **Actor:** user
- **Trigger:** Tapping the currency icon on the amount field, or on the foreign-amount
  field, then picking a currency
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingFragment.kt:107-129`).
- **Preconditions:** The form is in edit mode.
- **Main flow:**
  1. The "budget currency" is the budget's currency. For the scheduled budget it is the
     active budget's currency
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:96-101`).
  2. If the picked currency differs from the budget currency:
     - If there is no `exchangeInfo` yet, create one with `rawAmount = current amount`,
       `rate = getRate(picked)` and `currency = picked`.
     - Otherwise refresh its `rate` and `currency`.
     - Then recompute `amount` and show the foreign-amount and rate fields

     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:181-195`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:64-70`,
     `android/legacy/src/main/res/layout/spending_fragment.xml:70`,
     `android/legacy/src/main/res/layout/spending_fragment.xml:92`).
  3. If the picked currency equals the budget currency, set
     `amount = rawAmount` (or keep the amount) and drop `exchangeInfo`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:196-199`).
  4. Editing the foreign amount recomputes `amount`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:203-207`).
     Editing the rate recomputes `amount` through the foreign-amount field
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:210-214`).
- **Rules:**
  - `amount = round(rawAmount × rate, 2, HALF_EVEN)`, computed through `BigDecimal(double)`
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Spending.kt:22-26`).
  - `rate` is the number of budget-currency units per 1 foreign unit (UC-L3.8).
  - Stored as `spendings.currency`, `rate` and `foreignAmount`. A spending is "foreign"
    when `foreignAmount` is not null
    (`android/legacy/src/main/java/com/mas/mobile/repository/budget/SpendingMapper.kt:20-26`,
    `android/legacy/src/main/java/com/mas/mobile/repository/budget/SpendingMapper.kt:41-43`).
- **Edge cases:**
  - **Rate source.** `getRate` always uses the **active** budget's currency as the base,
    not the spending's budget currency
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:123-124`).
    For a spending in a budget with another currency, the rate is relative to the wrong
    currency.
  - **Rate lookup fails.** A toast is shown and `rate = 0.00`, so `amount = 0.00`
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:124-128`).
  - **Editing the main amount** while a foreign currency is set does not update
    `rawAmount` or `rate`. The stored triple can then disagree with `amount`.
  - **Rate lookup on open.** The exchange-currency observer is registered with an initial
    value
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:180-181`).
    If the observer receives that value immediately (standard LiveData behaviour, not
    shown in app code), then opening an existing foreign spending fetches a fresh rate and
    recomputes `amount` in memory. Saving would store it.
- **Data touched:** `spendings.amount`, `currency`, `rate`, `foreignAmount`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:180-214`

## UC-L3.8: Fetch an exchange rate

- **Actor:** system
- **Trigger:** `BudgetService.getRate(currency)`, called from UC-L3.7 and UC-L3.9
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:111`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:186`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:190`).
- **Preconditions:** Network access. The API key is in `BuildConfig.FREE_CURRENCY_API_KEY`
  (`android/legacy/src/main/java/com/mas/mobile/repository/budget/FreeCurrencyAPIRepositoryImpl.kt:26`).
- **Main flow:**
  1. Resolve the active budget (UC-L1.1). Its currency is `base`
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:124`).
  2. Send `GET /v1/latest?apikey=…&base_currency=<foreign>&currencies=<base>`
     (`android/legacy/src/main/java/com/mas/mobile/repository/budget/FreeCurrencyAPIRepositoryImpl.kt:26`,
     `android/legacy/src/main/java/com/mas/mobile/repository/budget/FreeCurrencyAPIRepositoryImpl.kt:36-46`).
  3. Return `data[base]`
     (`android/legacy/src/main/java/com/mas/mobile/repository/budget/FreeCurrencyAPIRepositoryImpl.kt:27-28`).
- **Rules:**
  - The call is wrapped in `runBlocking`, so it blocks the calling thread
    (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:123`).
  - There is no caching. Every call is a network request.
- **Edge cases:**
  - An unsuccessful HTTP response or an exception leads to a toast ("Unable to retrieve
    the current exchange rate…") and `0.00`
    (`android/legacy/src/main/java/com/mas/mobile/repository/budget/FreeCurrencyAPIRepositoryImpl.kt:29-34`,
    `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:124-128`).
  - A missing key in `data` triggers `!!`, which is caught as an exception, giving 0.00
    (`android/legacy/src/main/java/com/mas/mobile/repository/budget/FreeCurrencyAPIRepositoryImpl.kt:28`,
    `android/legacy/src/main/java/com/mas/mobile/repository/budget/FreeCurrencyAPIRepositoryImpl.kt:32-34`).
  - Resolving the active budget can create a budget (UC-L1.1).
- **Data touched:** None stored. It may create a budget through UC-L1.2.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:123-129`,
  `android/legacy/src/main/java/com/mas/mobile/repository/budget/FreeCurrencyAPIRepositoryImpl.kt:14-53`

## UC-L3.9: Record a spending programmatically (`BudgetService.spend`)

- **Actor:** system
- **Trigger:** One of:
  - A captured message is matched to a category
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:116-129`).
  - A scheduled occurrence is due (UC-L1.10).
- **Preconditions:** None.
- **Main flow:**
  1. Resolve the active budget (UC-L1.1)
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:97`).
  2. Find the expenditure by name, or create it (UC-L2.5)
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:98-99`).
  3. Create a spending with the given date, amount, trimmed comment and expenditure
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:101-106`).
  4. If a currency is given and differs from the budget's currency, attach `exchangeInfo`
     with `rawAmount = amount` and `rate = getRate(currency)`, then convert
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:108-115`).
  5. Add the spending to the budget, save it, and return the spending id
     (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:117-120`).
- **Rules:**
  - The spending always goes to the budget active **now**, whatever its date.
  - `recurrence` is `Never`
    (`android/legacy/src/main/java/com/mas/mobile/repository/budget/SpendingRepositoryImpl.kt:29`).
- **Edge cases:**
  - A message received on the last day of a period but processed after midnight goes to
    the new budget. This follows from rule 1. The SMS path stamps the processing time;
    the notification path uses the post time
    (`android/legacy/src/main/java/com/mas/mobile/service/SmsListener.kt:34`,
    `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:25`).
  - Rate failure gives amount 0 (UC-L3.8).
- **Data touched:** Same as UC-L3.3.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:92-121`

## UC-L3.10: Side effects of saving a spending from the form

- **Actor:** user
- **Trigger:** A successful save in UC-L3.3, UC-L3.4 or UC-L3.5
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:254-259`).
- **Preconditions:** None.
- **Main flow:**
  1. **Merchant prompt.** In ADD mode, if the linked message has a merchant that no
     category lists, remember it
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:244-252`).
     Before saving, ask "Would you like to link <merchant> to the '<expenditure>' category?"
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingFragment.kt:46-61`,
     `android/legacy/src/main/res/values/strings.xml:111`).
     **No** discards the merchant. Both answers save.
  2. **Link the message.** After the budget is saved, if a message is attached, set its
     `spendingId` to this spending and save it
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:279-283`).
  3. **Link the merchant.** If a merchant is still remembered and a category has the same
     name as the spending's expenditure (ignoring case), add the merchant to that category
     and save it
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:285-291`,
     `android/legacy/src/main/java/com/mas/mobile/domain/budget/CategoryService.kt:17-20`).
  4. **Log the event.** Log `spending_modified`/`Save`, with source `message` when a
     message is attached and `form` otherwise. Use `scheduled_modified` when
     recurrence ≠ Never
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:294-302`,
     `android/legacy/src/main/java/com/mas/mobile/domain/analytics/Event.kt:25-49`).
- **Rules:**
  - The attached message is the one passed as `message_id`. Otherwise it is the message
    linked to `spending_id`. In CLONE mode that is the **source** spending, so saving a
    clone moves the source's message link to the clone (UC-L3.5)
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:137-142`).
  - In ADD mode from a Matched message, the amount and comment are pre-filled from the
    message
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:169-178`).
- **Edge cases:**
  - **No matching category.** If no category has the expenditure's name (ignoring case),
    the merchant is silently not saved, even after the user answered yes.
  - **Prompt with an invalid form.** The prompt appears only when the form is valid,
    because `hasUnsavedMerchant` calls `isValid()`
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:90`).
  - **Message flow.** Discovery-mode messages (Recommended) show the message text and a
    "parse" button instead of the normal fields. That flow is in area 5
    (`android/legacy/src/main/res/layout/spending_fragment.xml:31-60`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingFragment.kt:63-75`).
- **Data touched:** `spending_messages.spending_id`, `categories.merchants`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:244-302`

## Open questions

1. **Year on re-save.** The date field shows `d.MM HH:mm`, and the parser fills in the
   current year
   (`android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:12`,
   `android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:26-32`). Does data
   binding call the inverse converter when it first sets the text? If so, opening a
   spending from a previous year and saving it changes the year. It also drops seconds
   and milliseconds, because the format has no seconds while storage keeps milliseconds
   (`android/legacy/src/main/java/com/mas/mobile/util/DateTool.kt:51-52`). That depends
   on data-binding runtime behaviour.
2. **Rate fetched on open.** Does opening a foreign spending fetch a new rate (UC-L3.7)?
   That depends on LiveData delivering the initial value to `observeForever`.
3. **Comment truncation.** The comment field has `maxLength="100"`
   (`android/legacy/src/main/res/layout/spending_fragment.xml:166`). The comment is
   pre-filled with the full message text
   (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:173`).
   Is the stored comment truncated? That depends on how `InputFilter` and the inverse
   binding interact. On the `spend` path the full text is stored
   (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:126`).
4. **Thread blocking.** `getRate` uses `runBlocking` and is reached from LiveData observers
   on the UI thread. How much the network call blocks the UI is not visible from the code.

## Surprises

- **Clone steals the message link.** Cloning a spending moves the source's captured
  message link to the clone
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:137-142`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:279-283`).

- **Silent no-op delete.** Deleting from the form uses value equality, so it can silently
  do nothing if the in-memory spending differs from the stored one
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:69-72`).
- **Wrong base currency.** Exchange rates are always relative to the active budget's
  currency, even for spendings in other budgets
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:124`).
- **Silent zero amounts.** A failed rate lookup turns the amount into 0.00, with only a
  toast
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:124-128`).
- **Amount not validated.** Amount is never validated, so 0 and negative values are
  accepted. The `amountError`, `dateError` and `commentError` fields are never set
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:51-55`).
- **Budget ignores date.** A spending's budget is chosen by context (active budget, or
  the source's budget), never by its date. Dates outside the budget range are allowed
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/Budget.kt:55-67`,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:97`).
- **Clone into past budget.** Cloning a spending from a past budget creates a spending
  dated now inside that past budget
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:127`).
- **Inconsistent triple.** Editing the amount of a foreign spending leaves `rawAmount` and
  `rate` unchanged, so the stored triple becomes inconsistent
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:156-159`).
- **Lost merchant.** A merchant the user agreed to link is dropped if no category has the
  expenditure's name
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:285-291`).
- **Missing analytics.** Deleting from the list does not log an analytics event, but
  deleting from the form does
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingListViewModel.kt:37-54`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:261-264`).
