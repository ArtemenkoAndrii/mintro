# Area 6: Settings, onboarding, privacy policy, analytics

Scope:
- The privacy-policy gate.
- The Settings screen and how settings are stored.
- The capture permissions flow.
- Onboarding: the first-run flag and the guided settings view.
- The "about" section.
- The Firebase analytics event catalogue.

Covered elsewhere:
- Backup rows on the Settings screen (area 9).
- The discovery-mode switch (UC-L5.11).

All citations use paths relative to the repository root.

## UC-L6.1: Accept the privacy policy

- **Actor:** user
- **Trigger:** `MainActivity.onCreate`, when the stored `policy.version` differs from
  `privacy_policy_v3`
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:50`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:82-86`,
  `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:44-45`,
  `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:57`).
- **Preconditions:** None. On a fresh install, `policy.version` defaults to `""`
  (`android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:15`).
- **Main flow:**
  1. Start `PolicyActivity`. `MainActivity` continues its own setup underneath
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:50-74`).
  2. Show `res/raw/privacy_policy_v3.html` in a WebView, with content and file access
     enabled
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/PolicyActivity.kt:23-27`).
  3. **Agree:** store `policy.version = privacy_policy_v3` in the background, then close
     the policy screen
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/PolicyActivity.kt:29-32`,
     `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:47-53`).
  4. **Decline:** `finishAffinity()` closes the app's task
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/PolicyActivity.kt:34-36`).
- **Rules:**
  - A new policy version (a new `POLICY_VERSION` constant) shows the gate again to users
    who accepted an older version.
  - The policy is checked only when `MainActivity` is created.
- **Edge cases:**
  - **Nothing waits for consent.** `MainActivity` registers the date listener and
    enqueues the workers before the user has answered
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:72-74`).
    Capture components and analytics do not check consent either (UC-L6.10, area 5).
  - **Back button.** `PolicyActivity` does not override back handling
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/PolicyActivity.kt:12-38`).
    See open question 1.
  - **Old policy file.** `res/raw/privacy_policy_v2.html` exists but nothing references it.
- **Data touched:** `settings` (`policy.version`).
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/PolicyActivity.kt:12-38`

## UC-L6.2: Open the Settings screen

- **Actor:** user
- **Trigger:** Any of:
  - Menu → Settings
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MenuFragment.kt:26`).
  - The message-list prompt, in guided mode (UC-L6.6).
  - The `action_to_settings` graph action.
- **Preconditions:** None.
- **Main flow:**
  1. Load the settings model once, when the ViewModel is created
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:32`).
  2. Show the following
     (`android/legacy/src/main/res/layout/settings_fragment.xml:45-395`):
     - **Budget section:** period; start day of month or of week, depending on the
       period; currency.
     - **Messages section:** capture-notifications switch; capture-SMS switch; link to
       message templates.
     - **Backup section** (area 9).
     - **About section:** "What is the MasHome?" link; app version.
  3. The start-day-of-month row is visible only for `MONTH`. The start-day-of-week row is
     visible only for `WEEK` and `TWO_WEEKS`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:176-185`,
     `android/legacy/src/main/res/layout/settings_fragment.xml:75`,
     `android/legacy/src/main/res/layout/settings_fragment.xml:100`).
     These visibility flags are set only by the period observer
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:79`),
     so their state when the screen opens depends on open question 2.
  4. The options menu is cleared
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:164-166`).
- **Rules:**
  - The screen has no discovery-mode switch. `SettingsViewModel.discoveryMode` exists, but
    the layout does not bind it
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:46`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:119-123`).
  - The version shown is `BuildConfig.VERSION_NAME`
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:47`).
- **Edge cases:** Every field observer calls `save()`
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:75-124`).
  Whether they also fire once on open is covered by open question 2.
- **Data touched:** Reads `settings`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:36-227`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:24-191`

## UC-L6.3: Change budget settings (period, start day, currency)

- **Actor:** user
- **Trigger:** Tapping the period, start-day or currency row
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:65-126`).
- **Preconditions:** None.
- **Main flow:**
  1. **Period:** a radio list of Week, Two weeks, Month, Quarter, Year
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:60-66`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CommonFragment.kt:84-113`).
  2. **Start day of month:** a number picker offering 1–28
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:68`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:205-225`).
  3. **Start day of week:** a picker of localised weekday names
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:51-59`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:69`).
  4. **Currency:** the currency picker
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:119-126`).
  5. Each change updates the model and saves **all** settings (UC-L6.7)
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:76-99`).
- **Rules:** These values are read only when a new budget is created (UC-L1.2, UC-L1.3).
  The currency is also the fallback currency for templates and GPT detection (UC-L5.9).
  Existing budgets are not changed.
- **Edge cases:**
  - **Stored start day can exceed 28.** `DayOfMonth` accepts 1–31, so a stored value
    above 28 from elsewhere would be accepted
    (`android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:33-38`).
  - **Weekday names are matched by display text** to map back to `DayOfWeek`
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:96-98`).
- **Data touched:** `settings`: `budget.period`, `budget.startDayOfMonth`,
  `budget.startDayOfWeek`, `budget.currency`, plus a rewrite of all other keys.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:75-99`

## UC-L6.4: Turn SMS capture on or off

- **Actor:** user
- **Trigger:** Tapping the "Capture SMS" row or switch
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:105-107`,
  `android/legacy/src/main/res/layout/settings_fragment.xml:221-225`).
- **Preconditions:** None.
- **Main flow:**
  1. On any change, set `model.captureSms = isSMSAllowed()`, the current `RECEIVE_SMS`
     grant
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:102-103`,
     `android/legacy/src/main/java/com/mas/mobile/service/PermissionService.kt:16-20`).
  2. If the switch was turned on and the permission is not granted, request `RECEIVE_SMS`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:104-106`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:128`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:187-189`).
     The result is written back to the switch, which re-runs step 1
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:39-41`).
  3. Save all settings
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:107`).
- **Rules:** The stored `captureSms` always reflects the permission state at the moment of
  the change, not the switch position.
- **Edge cases:**
  - **Turning the switch off while the permission is granted stores `true`.** Nothing
    revokes the permission or disables the receiver. No code calls
    `setComponentEnabledSetting`, and `SmsListener` does not read the setting
    (`android/legacy/src/main/java/com/mas/mobile/service/SmsListener.kt:20-38`). SMS
    capture therefore continues.
  - **Denied permission** stores `false`. The switch is set back from the result
    callback.
- **Data touched:** `settings` (`budget.captureSMS`, plus all other keys).
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:101-108`

## UC-L6.5: Turn notification capture on or off

- **Actor:** user
- **Trigger:** Tapping the "Capture Notifications" row or switch
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:101-103`,
  `android/legacy/src/main/res/layout/settings_fragment.xml:190-194`).
- **Preconditions:** None.
- **Main flow:**
  1. On any change, set `model.captureNotifications = isNotificationsAllowed()`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:111-112`).
     The check uses `NotificationManager.isNotificationListenerAccessGranted` when
     `SDK_INT > O`. Otherwise it searches the `enabled_notification_listeners` secure
     setting
     (`android/legacy/src/main/java/com/mas/mobile/service/PermissionService.kt:28-37`).
  2. If the switch was turned on and access is not granted, open the system screen for
     this app's notification-listener access. If that screen does not exist, show "Please
     allow notification access for this application."
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:113-115`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:129`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:191-203`,
     `android/legacy/src/main/res/values/strings.xml:110`).
  3. Save all settings
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:116`).
- **Rules:** As in UC-L6.4, the stored value reflects the grant, not the switch.
- **Edge cases:**
  - **No callback on return.** When the user returns from system settings, nothing
    re-checks the grant. The stored value keeps the state from before the user granted
    access, until the next change.
  - **Turning the switch off** does not revoke listener access, and `NotificationListener`
    does not read the setting
    (`android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:21-38`).
    Capture continues.
- **Data touched:** `settings` (`budget.captureNotifications`, plus all other keys).
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:110-117`

## UC-L6.6: Guided capture setup from the Messages tab

- **Actor:** user
- **Trigger:** Opening the Messages tab while `captureSms` and `captureNotifications` are
  both `false`
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageListViewModel.kt:23`,
  `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:39-42`).
- **Preconditions:** The prompt has not been shown yet by this ViewModel instance
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageListViewModel.kt:37-38`).
- **Main flow:**
  1. Ask "Message capturing is not enabled. Would you like to enable it?"
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MessageListFragment.kt:92-98`,
     `android/legacy/src/main/res/values/strings.xml:115`).
  2. On yes, open Settings with `guided = true`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MessageListFragment.kt:95`).
  3. In guided mode, the notifications switch flashes red for 850 ms, then the SMS switch
     does the same
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:154-185`).
- **Rules:** The check uses the stored flags, which mirror the permission state (UC-L6.4,
  UC-L6.5).
- **Edge cases:** The prompt appears again whenever a new `MessageListViewModel` is
  created and both flags are still false.
- **Data touched:** Reads `settings`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MessageListFragment.kt:71-98`

## UC-L6.7: Store and read settings

- **Actor:** system
- **Trigger:** Any settings read, or any save from UC-L6.1 to UC-L6.5, or the
  discovery-mode switch (UC-L5.11).
- **Preconditions:** None.
- **Main flow:**
  1. **Read:** load every `settings` row and build `Settings`. A missing key keeps its
     default (table below)
     (`android/legacy/src/main/java/com/mas/mobile/repository/settings/SettingsRepositoryImpl.kt:11-12`,
     `android/legacy/src/main/java/com/mas/mobile/repository/settings/SettingsMapper.kt:11-22`).
  2. **Save:** upsert all 8 keys, one row each, with the values converted to strings
     (`android/legacy/src/main/java/com/mas/mobile/repository/settings/SettingsRepositoryImpl.kt:14-18`,
     `android/legacy/src/main/java/com/mas/mobile/repository/settings/SettingsMapper.kt:24-34`).
- **Rules:**

  | Key | Default | Source |
  |---|---|---|
  | `budget.period` | `MONTH` | `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:9` |
  | `budget.currency` | System currency: the locale currency, else USD for `en`, else EUR | `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:10`, `android/legacy/src/main/java/com/mas/mobile/util/CurrencyTools.kt:7-19` |
  | `budget.startDayOfMonth` | Today's day of month, capped at 28 | `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:11`, `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:20-27` |
  | `budget.startDayOfWeek` | Today's day of week | `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:12`, `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:19` |
  | `budget.captureSMS` | `false` | `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:13` |
  | `budget.captureNotifications` | `false` | `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:14` |
  | `policy.version` | `""` | `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:15` |
  | `message.discoveryMode` | `true` | `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:16` |

  - Keys come from
    `android/legacy/src/main/java/com/mas/mobile/repository/settings/SettingsMapper.kt:43-50`.
  - Removing settings is not supported (`TODO`)
    (`android/legacy/src/main/java/com/mas/mobile/repository/settings/SettingsRepositoryImpl.kt:20-22`).
- **Edge cases:**
  - **Date-dependent defaults.** Until settings are saved, the start-day defaults depend on
    the day they are read.
  - **Stale overwrite.** Every save writes all keys from the caller's copy of `Settings`.
    `SettingsViewModel` keeps the copy loaded when it was created
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:32`).
    Suppose discovery mode is changed elsewhere (UC-L5.11) while that ViewModel exists. A
    later change on the Settings screen then writes back the old `discoveryMode` value.
  - **Read-modify-write.** `SettingsService` changes discovery mode and the policy version
    with a separate read-modify-write in a background task
    (`android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:16-24`,
    `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:47-53`).
  - **Invalid stored values.** Any of the following throws while mapping: an invalid
    stored period name or currency code, a start day of month that is not an integer or
    is outside 1–31, or a start day of week outside 1–7
    (`android/legacy/src/main/java/com/mas/mobile/repository/settings/SettingsMapper.kt:13-16`).
- **Data touched:** `settings`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/repository/settings/SettingsMapper.kt:10-51`

## UC-L6.8: First run and onboarding

- **Actor:** system
- **Trigger:** First app start.
- **Preconditions:** None.
- **Main flow:**
  1. Create the database and seed it (UC-L4.10, UC-L5.13, and the scheduled budget in
     UC-L1.9).
  2. Show the privacy-policy gate (UC-L6.1).
  3. Open the Expenditures tab, which creates the first budget through UC-L1.1 and
     UC-L1.2.
- **Rules:**
  - A first-run flag exists in SharedPreferences: `firstRun` in the file named after the
    application id. `isFirstLaunch` and `completeFirstLaunch` read and write it, but no
    other code calls them
    (`android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:26-37`,
    `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:56`).
  - There is no onboarding flow beyond the policy gate and the guided capture prompt
    (UC-L6.6).
- **Edge cases:** The first budget uses the default settings, so its period is `MONTH` and
  its start day is today's day of month, capped at 28. It therefore starts today with no
  truncation comment, unless the day is after the 28th (UC-L1.3).
- **Data touched:** See the referenced use cases.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:26-37`

## UC-L6.9: About section

- **Actor:** user
- **Trigger:** Tapping "What is the MasHome?"
  (`android/legacy/src/main/res/layout/settings_fragment.xml:351-371`).
- **Preconditions:** A browser is available.
- **Main flow:** Open `MasApplication.HOME_PAGE`, a project wiki URL, with `ACTION_VIEW`
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:109-113`,
  `android/legacy/src/main/java/com/mas/mobile/MasApplication.kt:81`).
- **Rules:** The URL is hard-coded.
- **Edge cases:** No handling exists for a missing browser activity, unlike the
  notification-settings intent (UC-L6.5).
- **Data touched:** None.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:109-113`

## UC-L6.10: Log analytics events

- **Actor:** system
- **Trigger:** The call sites listed in the table below.
- **Preconditions:** None. There is no consent check and no opt-out setting.
  `FirebaseEventLogger` logs unconditionally
  (`android/legacy/src/main/java/com/mas/mobile/util/FirebaseEventLogger.kt:16-26`).
- **Main flow:**
  1. Convert the event's parameters to strings and call `FirebaseAnalytics.logEvent(name,
     params)`. Exceptions are caught and logged
     (`android/legacy/src/main/java/com/mas/mobile/util/FirebaseEventLogger.kt:16-26`).
- **Rules:** Event catalogue (definitions in
  `android/legacy/src/main/java/com/mas/mobile/domain/analytics/Event.kt:5-114`):

  | Event | Params | Emitted from |
  |---|---|---|
  | `message_evaluated` | `Status`: Spent, Pending, Recommended, Rejected | `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:148-160` |
  | `spending_modified` | `Status`: Save, Remove. `Source`: form, message | `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:294-312` |
  | `scheduled_modified` | `Status`: Save, Remove | `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:294-312` |
  | `message_template_modified` | `Status`: Save, Remove | `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageTemplateViewModel.kt:143-151` |
  | `qualifier_modified` | `Status` (always Save). `Type`: CATCH, SKIP, BLACKLIST | `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/QualifierListViewModel.kt:52`, `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/QualifierListViewModel.kt:57` |
  | `category_modified` | `Status`: Save, Remove | `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:129`, `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/CategoryViewModel.kt:134` |
  | `message_template_failed` | `Status`: Generation, Evaluation | `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:63`, `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:69` |
  | `app_update_suggested` | none | `android/legacy/src/main/java/com/mas/mobile/service/AppUpdateCheckWorker.kt:36` |
  | `scheduled_budget_not_found` | none | `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:150` |
  | `chart_shown` | `Type`: Trends, Overspending, Distribution | `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:57`, `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:62`, `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ChartViewModel.kt:67` |

  - No event carries amounts, message text, names or ids. Parameter values are enum
    names, plus the fixed strings `form` and `message` for `Source`
    (`android/legacy/src/main/java/com/mas/mobile/domain/analytics/Event.kt:14-113`).
- **Edge cases:**
  - **Unused helper.** `CommonFragment.logEvent` exists, but no code calls it
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CommonFragment.kt:51-53`).
  - **No events for budgets and expenditures.** Budget and expenditure changes do not log
    anything.
  - **Missing deletes.** List-row deletions of spendings and categories log nothing
    (UC-L3.6, UC-L4.5). Qualifier deletion logs `Save` (UC-L5.11).
  - **Crashlytics** is configured only in Gradle
    (`android/legacy/build.gradle:8`, `android/legacy/build.gradle:150`).
- **Data touched:** None stored locally. Events are sent to Firebase.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/util/FirebaseEventLogger.kt:12-27`,
  `android/legacy/src/main/java/com/mas/mobile/domain/analytics/Event.kt:5-114`

## Open questions

1. **Back on the policy screen.** `PolicyActivity` extends `Activity` and does not
   override back handling. With the platform default, pressing back would close it
   without recording consent and reveal `MainActivity`. The code cannot confirm what
   happens on a device.
2. **Saves on open.** `SettingsViewModel` registers `observeForever` on LiveData that
   already has values
   (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:76-123`).
   If LiveData delivers those values immediately, opening Settings saves all keys and
   re-syncs both capture flags with the permission state. When settings are first
   persisted depends on this. `SettingsFragment` also attaches the period and start-day
   click listeners only inside `observeForever` callbacks
   (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:65-99`),
   so whether those rows respond to taps depends on the same behaviour.
3. **What the policy says.** The privacy policy's content (`privacy_policy_v3.html`)
   describes data handling, and it was not compared with the actual behaviour, such as
   sending message text to OpenAI (UC-L5.9). The comparison is outside "behaviour as
   implemented".

## Surprises

- **Capture switches don't stop capture.** Turning a capture switch off does not stop
  capture. The stored flag keeps the permission state, and the listeners never read it
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:102-117`,
  `android/legacy/src/main/java/com/mas/mobile/service/SmsListener.kt:20-38`,
  `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:21-38`).
- **Discovery switch not on Settings.** The Settings screen has no discovery-mode switch,
  although the ViewModel has the field
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:119-123`).
- **Nothing waits for consent.** Workers, the date listener, capture and analytics all run
  without waiting for policy consent
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:50-74`,
  `android/legacy/src/main/java/com/mas/mobile/service/SmsListener.kt:20-38`,
  `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:21-38`,
  `android/legacy/src/main/java/com/mas/mobile/util/FirebaseEventLogger.kt:16-26`).
- **Stale settings.** Every save writes all keys, so a stale `SettingsViewModel` copy can
  overwrite a newer discovery-mode value
  (`android/legacy/src/main/java/com/mas/mobile/repository/settings/SettingsRepositoryImpl.kt:14-18`).
- **Dead code.** The first-run flag, `CommonFragment.logEvent` and
  `privacy_policy_v2.html` are unused. A search finds no reference to the v2 file
  (`android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:26-37`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/CommonFragment.kt:51-53`).
- **Missing "What is" guard.** The "What is" link has no guard for a missing browser,
  unlike the notification-settings intent
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:109-113`).
