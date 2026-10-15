# Area 7: Background work and notifications

Scope:
- Scheduling of the periodic workers.
- Date-change handling.
- The app-update check and its notification.
- Deferrable actions (progressive back-off).
- Background task execution and error reporting.

Covered elsewhere:
- What the scheduled-spending worker does (UC-L1.10).
- Message intake (area 5).

All citations use paths relative to the repository root.

## UC-L7.1: Schedule the periodic workers

- **Actor:** system
- **Trigger:** Every `MainActivity.onCreate`
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:74`).
- **Preconditions:** None.
- **Main flow:**
  1. Enqueue `AppUpdateCheckWorker` as unique periodic work `periodicUpdateCheck`, every
     2 hours, with policy `REPLACE`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:157-162`).
  2. Enqueue `ScheduledSpendingWorker` as unique periodic work `scheduledSpending`, every
     2 hours, with policy `REPLACE`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:164-169`).
- **Rules:**
  - Neither request sets constraints. No code calls `setConstraints`.
  - The app has no boot receiver. Workers are enqueued only from `MainActivity`
    (`android/legacy/src/main/AndroidManifest.xml:26-66`).
- **Edge cases:**
  - **Before the first open.** Until the app's main screen has been opened once, neither
    worker is scheduled.
  - **`REPLACE` on every launch.** See open question 1.
- **Data touched:** WorkManager's own storage only.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:156-170`

## UC-L7.2: Run the scheduled-spending worker

- **Actor:** system
- **Trigger:** The `scheduledSpending` periodic work (UC-L7.1).
- **Preconditions:** None.
- **Main flow:**
  1. Inject dependencies through the application component
     (`android/legacy/src/main/java/com/mas/mobile/service/ScheduledSpendingWorker.kt:19-21`).
  2. Call `BudgetService.createScheduledSpendings()` (UC-L1.10), then return `success()`
     (`android/legacy/src/main/java/com/mas/mobile/service/ScheduledSpendingWorker.kt:14-17`).
- **Rules:** `createScheduledSpendings` starts a fire-and-forget background coroutine and
  returns immediately
  (`android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:131-132`,
  `android/legacy/src/main/java/com/mas/mobile/service/CoroutineService.kt:11-15`).
  The worker therefore reports success before the work is done.
- **Edge cases:** The worker always returns `success()`, so WorkManager never retries it
  after a failure.
- **Data touched:** See UC-L1.10.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/service/ScheduledSpendingWorker.kt:10-22`

## UC-L7.3: Check for an app update and notify

- **Actor:** system
- **Trigger:** The `periodicUpdateCheck` periodic work (UC-L7.1).
- **Preconditions:** The Google Play app-update API is available.
- **Main flow:**
  1. Request `appUpdateInfo` from Play. `doWork` returns `success()` immediately, and the
     result is handled in asynchronous listeners
     (`android/legacy/src/main/java/com/mas/mobile/service/AppUpdateCheckWorker.kt:26-44`).
  2. If an update is available, build a deferrable action with key
     `ProgressiveDaily("Version available: <availableVersionCode>")`
     (`android/legacy/src/main/java/com/mas/mobile/service/AppUpdateCheckWorker.kt:31-32`).
  3. If that action is not deferred
     (`android/legacy/src/main/java/com/mas/mobile/service/AppUpdateCheckWorker.kt:33-37`):
     - defer it (UC-L7.4);
     - post the "New Version Available" notification (UC-L7.5);
     - log `app_update_suggested`.
  4. On failure, write an error log entry only
     (`android/legacy/src/main/java/com/mas/mobile/service/AppUpdateCheckWorker.kt:39-41`).
- **Rules:** Each new available version code is a new key, so its back-off starts from
  the beginning.
- **Edge cases:** Results that arrive after `doWork` has returned depend on the process
  staying alive (open question 2).
- **Data touched:** `deferrable_actions`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/service/AppUpdateCheckWorker.kt:18-49`

## UC-L7.4: Defer an action with progressive back-off

- **Actor:** system
- **Trigger:** `DeferrableActionService.isDeferred(key)` and `defer(key)` (UC-L7.3)
  (`android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableActionService.kt:13-23`).
- **Preconditions:** None.
- **Main flow:**
  1. `isDeferred` loads the action by key. It is true when `activeAfter > now`. A missing
     row means false
     (`android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableActionService.kt:13-14`,
     `android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableAction.kt:10`).
  2. `defer` loads the action, or creates one with `activeAfter = now` and
     `increment = 24 h`
     (`android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableActionService.kt:17`,
     `android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableAction.kt:5-9`,
     `android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableAction.kt:39-42`).
     If it is not currently deferred:
     - `activeAfter = now + increment`;
     - if `increment < 16 days`, then `increment = increment × ratio`, where the ratio is
       2 for `ProgressiveDaily` and 1 for `UniformDaily`

     (`android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableAction.kt:12-20`,
     `android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableAction.kt:34-47`).
  3. In the background, upsert the action. Then delete every action whose `active_after`
     is older than 30 days
     (`android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableActionService.kt:19-22`,
     `android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableActionService.kt:33-35`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/DeferredActionDAO.kt:15-16`).
- **Rules:**
  - For `ProgressiveDaily`, the **minimum** waits between notifications are 24 h, 48 h,
    96 h, 192 h, then 384 h (16 days) from then on. The actual time depends on when the
    next `periodicUpdateCheck` run happens after `activeAfter`. The increment stops
    doubling once it reaches 16 days
    (`android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableAction.kt:16`,
    `android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableAction.kt:35`).
  - The `deferrable_actions.type` column stores 1 (UNIFORM) or 2 (PROGRESSIVE). Any other
    value is read as `UniformDaily`
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/entity/DeferrableAction.kt:24-25`,
    `android/legacy/src/main/java/com/mas/mobile/repository/settings/DeferrableActionMapper.kt:9-28`).
- **Edge cases:**
  - **`UniformDaily` is never constructed** except when mapping stored rows.
  - **Purge resets the back-off.** A row whose `active_after` is more than 30 days old is
    purged. The next defer for that key starts again at 24 h.
  - **`remove(key)` exists** but has no callers
    (`android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableActionService.kt:25-31`).
- **Data touched:** `deferrable_actions`: `key`, `type`, `increment`, `active_after`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableAction.kt:5-47`,
  `android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableActionService.kt:8-36`

## UC-L7.5: Post the "new version" notification

- **Actor:** system
- **Trigger:** UC-L7.3 step 3
  (`android/legacy/src/main/java/com/mas/mobile/service/AppUpdateCheckWorker.kt:35`).
- **Preconditions:** `POST_NOTIFICATIONS` is granted. Otherwise nothing is posted
  (`android/legacy/src/main/java/com/mas/mobile/service/NotificationService.kt:55-59`).
- **Main flow:**
  1. When `NotificationService` is created, it creates the notification channel `MasMoney`
     (id, name and description all `MasMoney`), with default importance. The code guards
     this with an API ≥ 26 check, which always passes because `minSdkVersion` is 26
     (`android/legacy/build.gradle:15`)
     (`android/legacy/src/main/java/com/mas/mobile/service/NotificationService.kt:62-76`,
     `android/legacy/src/main/java/com/mas/mobile/service/NotificationService.kt:78-80`).
  2. Build the notification
     (`android/legacy/src/main/java/com/mas/mobile/service/NotificationService.kt:25-53`,
     `android/legacy/src/main/res/values/strings.xml:204-205`):
     - title "New Version Available";
     - text "A new version of the app is available. Tap to update now!";
     - default priority, auto-cancel;
     - tapping it opens `market://details?id=<package>` in the Play Store app.
  3. Post it with notification id 1
     (`android/legacy/src/main/java/com/mas/mobile/service/NotificationService.kt:58`,
     `android/legacy/src/main/java/com/mas/mobile/service/NotificationService.kt:80`).
- **Rules:** This is the only notification the app posts.
- **Edge cases:**
  - **Permission never requested.** No code requests `POST_NOTIFICATIONS` at runtime.
  - **Event still logged.** If the permission is not granted, the notification is
    silently not posted, but the action is still deferred and the event is still logged
    (UC-L7.3).
  - **Fixed id.** Every post uses id 1. By platform behaviour, a repost with the same id
    replaces the existing notification.
- **Data touched:** None.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/service/NotificationService.kt:18-82`

## UC-L7.6: Reload the active budget on date change

- **Actor:** system
- **Trigger:** `ACTION_TIME_TICK` while `MainActivity` exists (see UC-L1.8).
- **Preconditions:** None.
- **Main flow:**
  1. Register a new `DateListener` on the application context in `onCreate`, and
     unregister it in `onDestroy`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:38`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:72`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:77-80`).
  2. On a tick after the date has changed, reload the active budget
     (`android/legacy/src/main/java/com/mas/mobile/service/DateListener.kt:17-23`).
- **Rules:** See UC-L1.8.
- **Edge cases:** Nothing reacts to a date change while `MainActivity` does not exist.
- **Data touched:** See UC-L1.8.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/service/DateListener.kt:11-24`

## UC-L7.7: Run background tasks and report errors

- **Actor:** system
- **Trigger:** Any `TaskService.backgroundTask { … }` call. Examples are saves from
  ViewModels, message intake and scheduled spendings
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ItemViewModel.kt:24-26`,
  `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:28`).
- **Preconditions:** None.
- **Main flow:**
  1. Launch the block in a new `CoroutineScope(Dispatchers.IO)`, with an exception handler
     (`android/legacy/src/main/java/com/mas/mobile/service/CoroutineService.kt:11-15`).
  2. On an uncaught exception, write it to the error log and post the toast "Ups,
     something went wrong" on the main thread
     (`android/legacy/src/main/java/com/mas/mobile/service/CoroutineService.kt:23-25`,
     `android/legacy/src/main/java/com/mas/mobile/service/ErrorHandler.kt:18-29`,
     `android/legacy/src/main/res/values/strings.xml:193`).
- **Rules:**
  - Each call creates its own scope. Nothing tracks or cancels these coroutines.
  - Callers do not wait for them. For example, a form save closes the form before the
    data is written
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ItemViewModel.kt:19-29`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ItemFragment.kt:60-64`).
  - `blockingTask` (`runBlocking`) is defined, but nothing calls it
    (`android/legacy/src/main/java/com/mas/mobile/service/CoroutineService.kt:17-21`).
- **Edge cases:**
  - **Errors after the form closes.** A failed save shows only the generic toast, after
    the form has already closed.
  - **Order not guaranteed.** Two background tasks started one after the other can run
    concurrently (UC-L5.11).
- **Data touched:** None directly.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/service/CoroutineService.kt:7-26`

## Open questions

1. **Effect of `REPLACE`.** Both periodic works are re-enqueued with
   `ExistingPeriodicWorkPolicy.REPLACE` on every `MainActivity.onCreate`
   (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:160`,
   `android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:167`).
   Does that reset the 2-hour schedule on each launch, so that frequent launches delay
   the runs? That is WorkManager behaviour.
2. **Asynchronous results.** `AppUpdateCheckWorker` and `ScheduledSpendingWorker` return
   `success()` before their asynchronous work finishes. Whether that work completes if
   the process is stopped right after `doWork` is platform behaviour.
3. **Old Android versions.** On API levels below 33, `POST_NOTIFICATIONS` is not a runtime
   permission. The result of `checkSelfPermission` for it there is platform behaviour, so
   it is unclear whether the notification ever appears on older devices. `minSdkVersion`
   is 26 and `targetSdkVersion` is 34, so devices running API 26–32 are in scope
   (`android/legacy/src/main/java/com/mas/mobile/service/NotificationService.kt:55-57`,
   `android/legacy/build.gradle:15-16`).

## Surprises

- **Success too early.** The scheduled-spending worker reports success without waiting
  for its coroutine to complete, so failures are never retried
  (`android/legacy/src/main/java/com/mas/mobile/service/ScheduledSpendingWorker.kt:14-17`,
  `android/legacy/src/main/java/com/mas/mobile/domain/budget/BudgetService.kt:131-132`).
- **Permission never requested.** The only notification depends on a permission the app
  never requests
  (`android/legacy/src/main/java/com/mas/mobile/service/NotificationService.kt:55-59`).
- **Workers need a launch.** Workers are first enqueued only when the main screen opens.
  No other app code, such as a boot receiver, enqueues them
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:74`,
  `android/legacy/src/main/AndroidManifest.xml:26-66`).
- **Save errors after closing.** Saves run as untracked background coroutines. A save
  error appears as a generic toast after the form has closed
  (`android/legacy/src/main/java/com/mas/mobile/service/CoroutineService.kt:11-25`).
- **Unused code.** `UniformDaily`, `DeferrableActionService.remove` and `blockingTask` are
  not used by any feature
  (`android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableAction.kt:44-47`,
  `android/legacy/src/main/java/com/mas/mobile/domain/settings/DeferrableActionService.kt:25-31`,
  `android/legacy/src/main/java/com/mas/mobile/service/CoroutineService.kt:17-21`).
