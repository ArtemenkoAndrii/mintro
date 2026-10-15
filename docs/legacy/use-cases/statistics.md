# Area 8: Statistics

Scope:
- The statistics ("analytics") menu.
- Its three charts: trends, overspending alerts, expenditure distribution.

Firebase event logging is covered in area 6. Only the `chart_shown` event is mentioned
here.

All citations use paths relative to the repository root.

"Completed budget" means a budget with `id > 0` and `lastDayAt < today`, newest
`startsOn` first
(`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:16-17`,
`android/legacy/src/main/java/com/mas/mobile/repository/budget/BudgetRepositoryImpl.kt:42`).
The active budget covers today, so it is never completed
(`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/BudgetDAO.kt:34-35`,
`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:70-71`).

## UC-L8.1: Open the statistics menu

- **Actor:** user
- **Trigger:** Menu tab → Analytics
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MenuFragment.kt:25`).
- **Preconditions:** None.
- **Main flow:**
  1. Show three entries: trends, overspending alerts, expenditure distribution
     (`android/legacy/src/main/res/layout/analytics_fragment.xml:37-148`).
  2. Each entry opens the chart screen with the matching `Type`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/AnalyticsFragment.kt:24-34`,
     `android/legacy/src/main/java/com/mas/mobile/domain/analytics/Analytics.kt:33-35`).
- **Rules:** None beyond navigation.
- **Edge cases:** The budget list also handles a `budget_analytics` menu item, but that
  menu is never inflated (UC-L1.4)
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/BudgetListFragment.kt:49`).
- **Data touched:** None.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/AnalyticsFragment.kt:12-39`

## UC-L8.2: View plan/fact trends

- **Actor:** user
- **Trigger:** The "trends" entry (UC-L8.1).
- **Preconditions:** None.
- **Main flow:**
  1. On open, compute trends for all expenditures (filter = none)
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:41-50`).
  2. Take the completed budgets, sort them by `startsOn` ascending, and keep the last 12
     (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:15-17`,
     `android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:65`).
  3. **No filter:** one point per budget, with its name, `plan` and `fact`
     (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:29-31`).
  4. **Expenditure-name filter:** one point per expenditure in those budgets whose name
     equals the filter, ignoring case. Each point is labelled with its budget name
     (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:18-28`).
  5. Draw a line chart with a blue "Plan" line and a red "Fact" line. The x-axis labels
     are budget names. Names longer than 14 characters are cropped to their first 11
     characters plus `..`. With no points, show "You do not have any completed budgets
     yet"
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:112-143`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:276-289`,
     `android/legacy/src/main/res/values/strings.xml:117`).
  6. The filter field offers every distinct expenditure name, with the hint "all
     categories". Picking a name recomputes the chart. Clearing the text resets the
     filter to none
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:32-35`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:43-45`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:68-83`,
     `android/legacy/src/main/res/values/strings.xml:24`).
  7. Log `chart_shown` (`Trends`) on every computation
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:57`).
- **Rules:**
  - Values are the stored `plan` and `fact` of each budget or expenditure, in that
    budget's own currency. There is no currency conversion.
  - Label cropping: names longer than 14 characters become their first 11 characters
    plus `..`
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:269-274`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:327-328`).
- **Edge cases:**
  - **Mixed currencies.** Budgets in different currencies are plotted on one axis
    (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:30`).
  - **Filtered gaps.** With a filter, a budget that lacks that expenditure has no point.
    Points are indexed in sequence, so such gaps are not shown on the axis
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:282`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:286`).
  - **Unrelated names.** The filter list includes names from any budget, including the
    scheduled budget and budgets outside the last 12
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/ExpenditureDAO.kt:18-19`).
  - **Active budget excluded.** The current active budget is never part of the trend.
- **Data touched:** Reads `budgets` and `expenditures`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:14-32`

## UC-L8.3: View overspending alerts

- **Actor:** user
- **Trigger:** The "alerts" entry (UC-L8.1).
- **Preconditions:** None.
- **Main flow:**
  1. Take the completed budgets, sort them by `startsOn`, and keep the last 3
     (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:35-37`,
     `android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:66`).
  2. Group all their expenditures by **exact** name. Keep only groups with at least 3
     items
     (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:39-41`).
  3. For each group, compute `percentage`
     (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:42-51`,
     `android/legacy/src/main/java/com/mas/mobile/Extensions.kt:14-15`):
     - If `Σplan > 0`: `percentage = round_half_even(Σfact × 100 / Σplan, 2)`.
     - Otherwise: `percentage = threshold`, which is 120.
  4. Keep groups with `percentage ≥ 120`, sorted ascending
     (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:53-54`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:61-64`,
     `android/legacy/src/main/java/com/mas/mobile/domain/analytics/Analytics.kt:28-30`).
  5. Draw a horizontal red bar chart with one bar per expenditure. There is no filter.
     With no bars, show "Yahoo! You have no alerts!"
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:85-88`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:202-243`,
     `android/legacy/src/main/res/values/strings.xml:116`).
  6. Log `chart_shown` (`Overspending`)
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:62`).
- **Rules:**
  - The threshold is fixed at 120 %, and the window at 3 completed budgets.
  - Names are unique within a budget (UC-L2.4), so a group can reach 3 items only when the
    exact name appears in all 3 budgets.
- **Edge cases:**
  - **Fewer than 3 completed budgets:** no alerts
    (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:37`,
    `android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:41`).
  - **Case-sensitive grouping:** names that differ only in case form separate groups.
  - **Zero total plan:** an expenditure that appears 3 times with a total plan of 0 is
    always reported, at exactly 120 %.
  - **Mixed currencies:** plan and fact from budgets in different currencies are summed
    without conversion.
- **Data touched:** Reads `budgets` and `expenditures`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:34-55`

## UC-L8.4: View expenditure distribution of a budget

- **Actor:** user
- **Trigger:** The "distribution" entry (UC-L8.1).
- **Preconditions:** None.
- **Main flow:**
  1. On open, with no filter, use the **active** budget's name. Load the budget with that
     name
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:66-73`).
  2. Take that budget's expenditures, sorted by `fact` descending. Each becomes a share
     with its name, its `fact` and the budget currency
     (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:57-62`).
  3. Draw a pie chart
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:145-200`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:245-267`).
     - Slice labels are shown only for shares of 3 % or more.
     - The legend shows `name - x.x%`.
     - Tapping a slice shows its name, its amount and the currency symbol in the centre.
     - Slice labels use the cropped names. The legend and the centre text use full names
       (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:148`).
     - Colours come from a fixed list of 21 colours. The legend cycles through them by
       index
       (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:264`).
  4. The filter offers the names of completed budgets, newest first. Picking one shows
     that budget
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:36-37`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:68-71`).
  5. Log `chart_shown` (`Distribution`)
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:67`).
- **Rules:**
  - Shares are computed against the sum of all `fact` values shown.
  - Percentages are rounded `HALF_EVEN` to 2 decimals before display
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:245-252`).
- **Edge cases:**
  - **Filter text vs chart.** On open, the filter text is set to the first completed
    budget's name, without triggering the filter, while the chart shows the **active**
    budget. The text and the chart therefore disagree
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:89-92`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:42`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:69`).
  - **Active budget not selectable.** The active budget is not in the filter list, which
    holds completed budgets only. Once another budget is picked, the active one cannot be
    selected again on that screen.
  - **Lookup by name.** The budget is looked up by name, so it depends on budget names
    being unique (UC-L1.2).
  - **Side effect.** Resolving the active budget can create a budget (UC-L1.1).
  - **Zero total.** If all facts are 0, every percentage is 0
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:246-251`).
- **Data touched:** Reads `budgets` and `expenditures`. May create a budget through
  UC-L1.2.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:66-74`,
  `android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:57-62`

## Open questions

1. **Bar labels.** The bar chart uses MPAndroidChart's `PercentFormatter` on values that
   are already percentages
   (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:207`).
   The exact label text is library behaviour.
2. **Reuse of the constant.** `OVERSPENDING_LIMIT` (3) is used both as the budget window
   and as the minimum group size
   (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:37`,
   `android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:41`).
   The code does not say whether that reuse is intended.

## Surprises

- **Filter text mismatch.** The distribution chart opens on the active budget, while the
  filter field shows a different, completed budget's name
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ChartFragment.kt:89-92`).
- **Unplanned always alerted.** Unplanned expenditures (plan 0) that appear in 3 completed
  budgets are always reported as 120 % overspent
  (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:45-50`).
- **No currency conversion.** All statistics mix currencies without conversion
  (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:14-62`).
- **Case-sensitive grouping.** Overspending groups by exact name, while trends filter
  ignoring case
  (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:21`,
  `android/legacy/src/main/java/com/mas/mobile/domain/analytics/AnalyticsService.kt:40`).
- **Repeated events.** `chart_shown` is logged on open and again on every filter change,
  because each `run()` logs it
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:41-67`).
