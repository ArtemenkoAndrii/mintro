# Area 5: Automatic capture

Scope:
- Intake of SMS and app notifications.
- Classification into message statuses.
- Pattern parsing and message templates.
- Automatic spending creation.
- The recommendation heuristic: qualifiers and discovery mode.
- Manual matching from the message list.
- GPT template generation and currency detection.
- Unit-test coverage of `domain/message`.
- Template and qualifier management, blacklist, and message deletion.

Covered elsewhere:
- The spending form itself (area 3).
- Merchant lists on categories (area 4).
- The capture switches and permissions in Settings (area 6).

All citations use paths relative to the repository root.

### Message statuses

| Domain status | Stored `spending_messages.status` | Meaning | Source |
|---|---|---|---|
| `Matched(templateId, amount, merchant?)` | `MATCHED`, with `rule_id`, `suggested_amount` and `suggested_merchant` | Parsed by an enabled template | `android/legacy/src/main/java/com/mas/mobile/domain/message/Message.kt:20-27`, `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageMapper.kt:30-44` |
| `Recommended` | `RECOMMENDED` | Not parsed, but it looks like a payment (UC-L5.6) | `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageMapper.kt:24-26`, `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageMapper.kt:37` |
| `Rejected` | not stored | Neither of the above, or matched by a disabled template | `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:32-48` |

Any stored status other than `MATCHED` is read back as `Recommended`
(`android/legacy/src/main/java/com/mas/mobile/repository/message/MessageMapper.kt:18-26`).

## UC-L5.1: Capture an incoming SMS

- **Actor:** system
- **Trigger:** The `SMS_RECEIVED` broadcast. The receiver is declared with priority 999
  and protected by `BROADCAST_SMS`
  (`android/legacy/src/main/AndroidManifest.xml:50-58`).
- **Preconditions:** The `RECEIVE_SMS` permission is granted. Settings requests it (area 6).
- **Main flow:**
  1. Check that the action is `SMS_RECEIVED` and that extras exist
     (`android/legacy/src/main/java/com/mas/mobile/service/SmsListener.kt:23`).
  2. For **each PDU** in the intent, build an `SmsMessage` and call
     `MessageService.handleRawMessage` with these values
     (`android/legacy/src/main/java/com/mas/mobile/service/SmsListener.kt:27-36`):
     - `sender = originatingAddress` (or `""`)
     - `text = trimmed body`
     - `date = LocalDateTime.now()`
  3. Classify and store the message (UC-L5.3).
- **Rules:**
  - The sender is the originating address, as delivered.
  - The time is the processing time, not the SMS timestamp.
- **Edge cases:**
  - A message delivered as several PDUs is processed as several separate messages, one
    per PDU
    (`android/legacy/src/main/java/com/mas/mobile/service/SmsListener.kt:27-35`).
  - `SmsListener` does not read the `captureSms` setting
    (`android/legacy/src/main/java/com/mas/mobile/service/SmsListener.kt:20-38`).
- **Data touched:** See UC-L5.3.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/service/SmsListener.kt:16-39`

## UC-L5.2: Capture an app notification

- **Actor:** system
- **Trigger:** `onNotificationPosted` on the notification listener service
  (`android/legacy/src/main/AndroidManifest.xml:59-66`,
  `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:21`).
- **Preconditions:** Notification-listener access is granted in system settings (area 6).
- **Main flow:**
  1. Ignore notifications from packages whose name starts with `com.android.`
     (`android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:24`,
     `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:40-41`).
  2. `date` is the notification's post time, in the device time zone
     (`android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:25`,
     `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:76-79`).
  3. `sender` is the posting app's label, found by listing installed applications
     (`android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:26`,
     `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:70-74`).
  4. `text` depends on the title and text extras
     (`android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:27-34`,
     `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:44-54`).
     - If at least one is non-null, `text` is `"$title\n$text"`.
     - If both are null, `text` is assembled by reflection from the `setText` calls of the
       custom content view, or `""` on failure.
  5. Call `handleRawMessage(sender.trim(), text.trim(), date)` (UC-L5.3)
     (`android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:36`).
- **Rules:** The sender is an app's display name (for example `Revolut`), not a package
  name.
- **Edge cases:**
  - **One extra missing.** If one of title or text is null, the string `null` appears in
    the text, because of string interpolation
    (`android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:33`).
  - **App not found.** If the app is not in the installed list, the sender becomes the
    string `"null"`. `extractSender` returns null, and `toString()` is called on it
    (`android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:26`,
    `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:70-74`).
  - **No deduplication.** Every post is processed, including the app's own notifications.
    Only `com.android.*` is excluded
    (`android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:21-38`).
  - **Setting not read.** The `captureNotifications` setting is not read here
    (`android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:21-38`).
- **Data touched:** See UC-L5.3.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:17-80`

## UC-L5.3: Classify and store an incoming message

- **Actor:** system
- **Trigger:** `MessageService.handleRawMessage(sender, text, date)` from UC-L5.1 or
  UC-L5.2. It runs as a background task
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:28`).
- **Preconditions:** None.
- **Main flow:**
  1. **Match.** Load all templates, in database order with no `ORDER BY`. For each one in
     turn
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:134-139`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/MessageTemplateDAO.kt:15-16`,
     `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:98-114`):
     - If the sender differs from the template sender, ignoring case, skip it.
     - If parsing the text fails (UC-L5.4), skip it.
     - If the template is enabled, the result is `Matched`. Otherwise it is `Rejected`.

     The first non-skipped result wins.
  2. **Recommend.** If nothing matched, the result is `Recommended` when the heuristic
     passes (UC-L5.6) **and** discovery mode is on. Otherwise it is `Rejected`
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:131-146`).
  3. **Log.** Log `message_evaluated` with one of these statuses
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:148-160`):
     - `Spent`: Matched with a merchant
     - `Pending`: Matched without a merchant
     - `Recommended`
     - `Rejected`
  4. **Rejected:** stop, after a log line. Nothing is stored
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:46-48`).
  5. **Otherwise:** create a message with a new id from `generator`, setting `sender`,
     `text`, `receivedAt = date`, `status` and `isNew = true`
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:33-39`,
     `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageRepositoryImpl.kt:25-31`).
     If the status is Matched, try UC-L5.5. Then upsert the message
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:41-45`).
- **Rules:**
  - Sender comparison is a whole-string match that ignores case.
  - A disabled template whose pattern parses the text produces `Rejected`, so the
    heuristic does not run for that message.
  - When several templates match, the first one returned by the query wins.
- **Edge cases:**
  - **Errors** inside the background task are shown as a generic toast
    (`android/legacy/src/main/java/com/mas/mobile/service/CoroutineService.kt:11-25`).
  - **No duplicate detection.** The same text from the same sender is stored again each
    time it arrives
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:28-48`).
  - **A template whose fallback throws** (UC-L5.4) aborts classification for its sender's
    messages, because the sender check comes before parsing. Nothing is stored, and a
    generic toast is shown
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:99-103`).
- **Data touched:**
  - Writes `spending_messages`: `id`, `name` (sender), `message`, `receivedAt`, `status`,
    `rule_id`, `suggested_amount`, `suggested_merchant`, `spending_id`, `is_new`.
  - Writes `generator`.
  - Reads `message_template`, `qualifiers` and `settings`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:28-160`

## UC-L5.4: Parse a message with a template pattern

- **Actor:** system
- **Trigger:** One of:
  - Template matching (UC-L5.3).
  - Template validation and preview (UC-L5.10).
  - Promotion (UC-L5.9).

  Source: `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageTemplate.kt:14-21`.
- **Preconditions:** The pattern contains `{amount}` (case-insensitive) and is at most 100
  characters. The constructor enforces both with `require`
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:6-10`,
  `android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:95-97`).
- **Main flow:**
  1. Build a regex from the pattern
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:25-30`,
     `android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:36-52`).
     - Escape `. $ ^ [ ] ( ) | * + ? \`.
     - Replace `{amount}` with `(\d+(?:[.,]\d+)?)` and `{merchant}` with `(.*)`.
     - For each placeholder, if the pattern starts with it, prefix `^`. Otherwise, if the
       pattern ends with it, suffix `$`. These checks are case-sensitive
       (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:47-52`).
     - Wrap the result as `.*<regex>.*`.
  2. Normalise the text: newlines become spaces, and runs of whitespace become one space
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:43-45`).
  3. Search the normalised text with the regex.
     - **Two groups:** amount and merchant. Their order follows the pattern.
     - **One group:** amount only, with no merchant.

     Source: `android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:18-20`,
     `android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:65-89`.
  4. If that finds nothing, retry with the pattern cut down to the amount part, using
     `cutToAmount`
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:21`,
     `android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:54-63`).
     - If `{amount}` comes before `{merchant}`, keep the text before `{merchant}`, minus
       one character.
     - Otherwise keep the text after `{merchant}`.
  5. Otherwise the result is `Empty`.
- **Rules:**
  - The amount text is converted by replacing `,` with `.` and calling `toDoubleOrNull()`,
    with `0.0` if that fails
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:91`).
  - The merchant is the raw captured text, with no trimming
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:84-89`).
  - The pattern itself is not whitespace-normalised. Only the text is
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:25-52`).
  - Regex construction errors are caught and logged, and count as no match
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:31-34`).
- **Edge cases:**
  - **Thousands separators.** The amount regex allows only one separator group, so
    `1,234.56` is never captured whole.
    - If nothing constrains the character after the amount, for example a bare `{amount}`
      at the start of the text, it is captured as `1,234`, which becomes `1.234`.
    - If literal text or the end anchor follows the amount, as in all seed templates, the
      match fails and the result is `Empty`
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:99`,
    `android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:91`).
  - **Anchors.** Because of the `.*` wrapper, a leading `^` means the placeholder must be
    at the very start of the text, and a trailing `$` that it must be at the very end.
  - **Repeated spaces in a pattern** never match, because the text is normalised to single
    spaces and the pattern is not.
  - **Unescaped braces.** `{` and `}` other than the placeholders are not escaped. They are
    passed into the regex, and a construction error counts as no match.
  - **Pattern without `{merchant}`.** In `cutToAmount`, `merchantIndex = −1`, so the
    "after merchant" branch runs `substring(9, length)`
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:55-61`).
    - For patterns shorter than 9 characters, such as exactly `{amount}` (8 characters,
      also the default `Pattern.SIMPLE`), this throws `StringIndexOutOfBoundsException`.
      `cutToAmount` runs outside the `try`, so the exception is not caught
      (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:21`,
      `android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:94`).
    - For longer patterns, the first 9 characters are dropped before the retry.
  - **Case of placeholders.** `cutToAmount` locates placeholders case-sensitively, while
    `parse` locates them case-insensitively
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:13`,
    `android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:18`,
    `android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:55-56`).
- **Data touched:** None. This is a pure function.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:5-115`

## UC-L5.5: Create a spending automatically for a matched message

- **Actor:** system
- **Trigger:** UC-L5.3 step 5, when the status is Matched
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:41-43`).
- **Preconditions:** All of the following. If any is missing, the step is skipped
  silently
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:116-120`):
  - The match has a merchant.
  - A category lists that merchant (UC-L4.9).
  - The template still exists, so its currency can be read.
- **Main flow:**
  1. Call `BudgetService.spend` with these values (UC-L3.9)
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:122-128`):
     - `date = receivedAt`
     - `amount = parsed amount`
     - `expenditureName = category name`
     - `comment = full message text`
     - `currency = template currency`
  2. Store the returned spending id on the message.
- **Rules:**
  - The spending goes to the active budget.
  - It is converted if the template currency differs from the budget currency (UC-L3.9).
- **Edge cases:**
  - **Matched without a merchant, or with an unknown merchant.** The message is stored
    Matched with no spending, and waits for manual matching (UC-L5.8).
  - **Amount 0 (theoretical).** `asDouble` falls back to 0.0 (UC-L5.4). Captured groups
    are digit strings, so this would need unusual input.
- **Data touched:**
  - Writes `spendings` (and possibly `expenditures`).
  - Writes `spending_messages.spending_id`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:116-129`

## UC-L5.6: Decide whether an unmatched message is "Recommended"

- **Actor:** system
- **Trigger:** UC-L5.3 step 2.
- **Preconditions:** Discovery mode (`message.discoveryMode`, default `true`) is on
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:142`,
  `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:16-17`,
  `android/legacy/src/main/java/com/mas/mobile/domain/settings/Settings.kt:16`).
- **Main flow:** `isRecommended` is true when **all** of these hold, on the lower-cased
  text
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:14-21`):
  1. The text is shorter than 250 characters
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:53`).
  2. The sender is not in the BLACKLIST qualifiers. The comparison is a whole-string match
     that ignores case
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:31-33`).
  3. The text matches `.*\d+[.,]\d+?.*`, meaning digits, a `.` or `,`, then at least one
     digit
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:35`,
     `android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:55`).
  4. The text contains a currency code or symbol from a fixed map, plus the system
     currency (substring, ignoring case)
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:37-40`,
     `android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:58-73`).
     The map holds UAH, USD, EUR, GBP, JPY, AUD, CAD, CHF, CNY and RUB.
  5. The text contains no SKIP word and at least one CATCH word (substring, ignoring case)
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:42-50`).
- **Rules:** Qualifier words are matched as substrings, not as whole words
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:42-50`).
- **Edge cases:**
  - **Whole amounts.** Messages whose only numbers have no `.` or `,` decimal part, such as
    `$55`, fail rule 3. The check runs on the whole text, so another `digit[.,]digit`,
    such as a date `09.10`, still lets them pass.
  - **"Fr" matches too much.** CHF's symbol is `Fr`. Any text containing `fr` (for
    example "from" or "free") passes rule 4
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:66`).
  - **Substring keywords.** Words such as `paid` also match inside `unpaid`, and `bill`
    matches inside `billion`.
  - **Short system-currency symbols.** The system currency's code and symbol are added to
    the currency list
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:69-73`).
    If that symbol is very short, for example one letter, almost any text contains it
    (substring, ignoring case) and passes rule 4. Which symbol applies is
    `java.util.Currency` locale data. It is platform behaviour and is not cited from app
    code.
  - **Unused constant.** `WORDS_REGEX` is defined but never used
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:56`).
- **Data touched:** Reads `qualifiers` and `settings`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:9-75`

## UC-L5.7: Browse captured messages

- **Actor:** user
- **Trigger:** The Messages tab
  (`android/legacy/src/main/res/menu/bottom_menu.xml:16-17`).
- **Preconditions:** None.
- **Main flow:**
  1. List messages with `receivedAt ≥ start of (today − 30 days)`, newest first
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageListViewModel.kt:24-26`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/SpendingMessageDAO.kt:11-12`).
  2. Each row shows the sender, the time, the text and a "new" marker. A date header
     appears when the date changes
     (`android/legacy/src/main/res/layout/message_list_row.xml:96`,
     `android/legacy/src/main/res/layout/message_list_row.xml:108`,
     `android/legacy/src/main/res/layout/message_list_row.xml:121`,
     `android/legacy/src/main/res/layout/message_list_row.xml:129`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/adapter/SpendingMessageAdapter.kt:33-40`).
  3. What else the row shows depends on its state
     (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/SpendingMessageAdapter.kt:56-88`):
     - **Has a spending:** an OK icon and the expenditure name.
     - **Matched without a spending:** a question icon and a "category" button.
     - **Recommended:** an info icon, plus "accept" and "blacklist" buttons.
  4. A tap marks the message as read and opens its spending in VIEW mode, or a new
     spending (ADD) if there is none. A long-tap opens the linked spending in EDIT mode, or a
     new spending (ADD) if there is none
     (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/SpendingMessageAdapter.kt:42-47`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/adapter/SpendingMessageAdapter.kt:91-101`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageListViewModel.kt:28-33`).
  5. On first open, if neither capture switch is on, offer once per ViewModel to open
     Settings in guided mode
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MessageListFragment.kt:92-98`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageListViewModel.kt:23`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageListViewModel.kt:37-38`).
- **Rules:** The tab badge counts **all** messages with `is_new = 1`, with no date limit.
  The `from` argument is ignored
  (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/MainActivity.kt:121-129`,
  `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageRepositoryImpl.kt:38-39`,
  `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/SpendingMessageDAO.kt:23-24`).
- **Edge cases:**
  - **Badge vs list.** Unread messages older than 30 days keep the badge non-zero, but
    are not listed.
  - **Missing spending.** If the linked spending no longer exists, the row shows `--` and
    clears `spendingId` in memory. The cleared value is stored only if the message is
    then saved, for example by a tap through `markAsRead`
    (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/SpendingMessageAdapter.kt:57-68`).
  - **Unreachable menu.** The list's options-menu handler references
    `spending_message_list_menu`, but that menu is never inflated
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MessageListFragment.kt:76-81`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ListFragment.kt:15-17`).
- **Data touched:** Reads `spending_messages` and `spendings`. Writes `is_new`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MessageListFragment.kt:29-103`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/adapter/SpendingMessageAdapter.kt:16-117`

## UC-L5.8: Match a message to a spending manually

- **Actor:** user
- **Trigger:** Tapping a Matched message without a spending, or its "category" button
  (UC-L5.7).
- **Preconditions:** The message has no spending.
- **Main flow:**
  1. Open the spending form in ADD mode with `message_id`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/SpendingMessageAdapter.kt:97`).
  2. Pre-fill `amount` with the parsed amount and `comment` with the full message text
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:169-174`).
  3. If the message's merchant is not listed in any category, remember it for the prompt
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:244-252`).
  4. The user picks an expenditure and saves. The spending goes into the active budget.
     The message is linked, and the merchant is optionally linked (UC-L3.10).
- **Rules:** The budget is the active one (`budget_id` default −1) (UC-L3.3).
- **Edge cases:**
  - **Date is now.** The spending date is the time the form opened. Unlike UC-L5.5 and
    UC-L5.9, it is not the message time. This path does not set `date`
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:120`,
    `android/legacy/src/main/java/com/mas/mobile/repository/budget/SpendingRepositoryImpl.kt:17`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:169-178`).
  - **Template currency ignored.** The currency stays the budget currency. Unlike UC-L5.5
    and UC-L5.9, the template's currency is not applied
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:180`).
- **Data touched:** Same as UC-L3.3 and UC-L3.10.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:137-178`

## UC-L5.9: Generate a template from a Recommended message (GPT)

- **Actor:** user
- **Trigger:** Tapping a Recommended message, or "accept". The spending form opens in
  discovery mode, showing the message text with Confirm and Cancel. The user taps Confirm
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:169-177`,
  `android/legacy/src/main/res/layout/spending_fragment.xml:31-60`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingFragment.kt:63-75`).
- **Preconditions:** Network access. The message status is Recommended
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:57-59`).
- **Main flow:**
  1. Send the message text to OpenAI chat completions with a fixed system prompt. The
     model is a hard-coded fine-tuned id, temperature 0.2. The trimmed reply becomes a
     `Pattern`
     (`android/legacy/src/main/java/com/mas/mobile/service/ai/GPTMessageAnalyzer.kt:22-41`,
     `android/legacy/src/main/java/com/mas/mobile/service/ai/GptChatConnector.kt:14-17`).
  2. Build a template
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageTemplateService.kt:14-23`):
     - id: new, from `generator`
     - `sender = message sender`
     - `pattern = reply`
     - `example = message text`
     - `currency` detected from the text (Rules)
     - `enabled = true`
  3. Run the template against this message. If the result is not Matched, log
     `message_template_failed`/`Evaluation` and fail
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:67-71`).
  4. Save the template. Set the message status to Matched and save it
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:73-76`).
  5. In the background, re-run the new template on every other **Recommended** message
     from the same sender (exact sender query), and save the ones that become Matched
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:78-96`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/SpendingMessageDAO.kt:20-21`).
  6. The form leaves discovery mode
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:226-242`):
     - `comment` = the text
     - `date` = `receivedAt`
     - `amount` = the parsed amount
     - `exchangeCurrency` = the template currency, converting if it differs
     - `expenditureName` = empty

     The user picks an expenditure and saves (UC-L3.3, UC-L3.10).
- **Rules:** Currency detection is detailed in UC-L5.14
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageTemplateService.kt:25-34`,
  `android/legacy/src/main/java/com/mas/mobile/domain/message/CurrencyExpert.kt:6-16`).
  - Find every currency whose symbol or code appears as a word, or attached to digits.
  - None found: use the Settings currency.
  - One found: use it.
  - Several found, including the Settings currency: use the Settings currency.
  - Otherwise: the first one found.
- **Edge cases:**
  - **Failures.** Any of these shows "Unable to extract spending from message" and logs
    `message_template_failed`/`Generation`:
    - an HTTP error;
    - an exception;
    - a reply without `{amount}`;
    - a reply over 100 characters.

    Sources: `android/legacy/src/main/java/com/mas/mobile/service/ai/GPTMessageAnalyzer.kt:14-20`,
    `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:61-65`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingFragment.kt:65-70`,
    `android/legacy/src/main/res/values/strings.xml:112`.
  - **Siblings** become Matched but get no spending. They wait for UC-L5.8.
  - **Logging.** The request and the response, including the full message text, are
    written to the debug log
    (`android/legacy/src/main/java/com/mas/mobile/service/ai/GPTMessageAnalyzer.kt:28`,
    `android/legacy/src/main/java/com/mas/mobile/service/ai/GPTMessageAnalyzer.kt:33`).
  - **Several currencies found.** "The first one found" follows the iteration order of
    `Currency.getAvailableCurrencies()`, which is a set
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/CurrencyExpert.kt:7`).
  - **Cancel** closes the form without changes
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SpendingFragment.kt:73-75`).
- **Data touched:**
  - Writes `message_template` (new row).
  - Writes `spending_messages.status`, `rule_id`, `suggested_amount`,
    `suggested_merchant`.
  - Writes `generator`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:56-96`,
  `android/legacy/src/main/java/com/mas/mobile/service/ai/GPTMessageAnalyzer.kt:9-43`

## UC-L5.10: Manage message templates

- **Actor:** user
- **Trigger:** Any of:
  - Menu → message templates
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MenuFragment.kt:27`).
  - Settings → message templates
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/SettingsFragment.kt:115-117`).
- **Preconditions:** None.
- **Main flow:**
  1. The template list screen shows:
     - the discovery-mode switch (UC-L5.11);
     - links to Blacklist and Filters;
     - all templates, each showing sender, pattern and example. Disabled templates are
       drawn at 40 % opacity.

     Sources: `android/legacy/src/main/res/layout/message_template_list_fragment.xml:56-126`,
     `android/legacy/src/main/res/layout/message_template_list_row.xml:21-39`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MessageTemplateListFragment.kt:33-48`.
  2. Tapping a row opens VIEW. Long-tapping opens EDIT
     (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/MessageTemplateAdapter.kt:17-25`).
  3. In edit mode the user changes:
     - the sender (maximum 30 characters);
     - the pattern (maximum 200 characters in the field);
     - the example (maximum 100 characters), with live highlighting of the parsed
       amount and merchant in green, or the whole text in red on failure;
     - the currency;
     - the enabled switch.

     Sources: `android/legacy/src/main/res/layout/message_template_fragment.xml:32-113`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageTemplateViewModel.kt:41-126`.
  4. Save validates the pattern (Rules). It then upserts the template and logs
     `message_template_modified`/`Save`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageTemplateViewModel.kt:74-87`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageTemplateViewModel.kt:143-146`).
  5. The form's Remove menu item deletes the template and logs `Remove`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ItemFragment.kt:78-96`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageTemplateViewModel.kt:148-151`).
- **Rules:**
  - The pattern must satisfy the `Pattern` constructor (`{amount}`, ≤ 100 characters),
    **and** it must parse the current example. Otherwise the save fails with "Pattern
    doesn't match the example"
    (`android/legacy/src/main/res/values/strings.xml:137`).
  - The sender is not validated.
  - Edits apply only to messages that arrive later. Stored messages are not re-evaluated
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageTemplateViewModel.kt:74-87`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageTemplateViewModel.kt:143-146`).
- **Edge cases:**
  - **No way to create.** There is no add button on the template list
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MessageTemplateListFragment.kt:58-60`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/ListFragment.kt:15-17`).
    New templates come only from seed data
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:35-44`) and
    from UC-L5.9.
  - **Clone opens read-only.** `CLONE` is supported by the ViewModel, but because of
    operator precedence, `enableEditing()` applies only to the `create()` fallback. A
    successful clone therefore opens read-only
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageTemplateViewModel.kt:134-138`).
    No screen offers Clone for templates. Rows offer only VIEW and EDIT
    (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/MessageTemplateAdapter.kt:17-25`).
  - **Length limits differ.** The pattern field allows 200 characters, but the model
    rejects more than 100
    (`android/legacy/src/main/res/layout/message_template_fragment.xml:51`,
    `android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:95`).
  - **Crash risk.** Validation catches only `IllegalArgumentException`. A
    `StringIndexOutOfBoundsException` from `cutToAmount` (UC-L5.4) is not caught
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageTemplateViewModel.kt:84-86`).
  - **Deleting a template** leaves the messages that reference its id (`rule_id`)
    unchanged. They stay Matched with a dangling `rule_id`. UC-L5.5 runs only at intake,
    so it never runs again for them. Manual matching (UC-L5.8) does not read the
    template, so it still works
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:41-43`).
- **Data touched:** `message_template`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageTemplateViewModel.kt:22-157`

## UC-L5.11: Manage qualifiers and discovery mode

- **Actor:** user
- **Trigger:** Any of:
  - The template list → Filters, which opens the CATCH and SKIP tabs.
  - The template list → Blacklist.
  - The discovery switch.

  Sources: `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MessageTemplateListFragment.kt:43-48`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/adapter/QualifierPagerAdapter.kt:9-16`,
  `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/QualifierListFragment.kt:93`.
- **Preconditions:** None.
- **Main flow:**
  1. **List.** Each tab lists the qualifiers of its type, ordered by name
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/QualifierListViewModel.kt:25-35`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/dao/QualifierDAO.kt:13-14`).
  2. **Add.** The add button inserts an empty row in edit mode
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/QualifierListViewModel.kt:37-43`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/adapter/QualifierAdapter.kt:20-22`).
  3. **Save a row.** Remove the old value, then save the new one
     (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/QualifierAdapter.kt:61-67`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/QualifierListViewModel.kt:45-53`).
     - The new value is trimmed.
     - If a qualifier of the same type already has that value (ignoring case), that
       existing row is reused.
     - Blank values are ignored.
  4. **Delete.** Row menu → delete removes the qualifier
     (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/QualifierAdapter.kt:35-59`,
     `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/QualifierListViewModel.kt:55-58`).
  5. **Discovery mode.** The switch on the template list writes `message.discoveryMode`
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageTemplateListViewModel.kt:17-24`,
     `android/legacy/src/main/java/com/mas/mobile/domain/settings/SettingsService.kt:16-24`).
     This is the only switch for it. `SettingsViewModel` declares a `discoveryMode` field,
     but the Settings layout does not bind it (UC-L6.2)
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SettingsViewModel.kt:46`,
     `android/legacy/src/main/res/layout/settings_fragment.xml:167-225`).
- **Rules:**
  - Types are CATCH = 1, SKIP = 2, BLACKLIST = 3
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/entity/Qualifier.kt:25-29`).
  - Seed words depend on the device language (UC-L5.13).
- **Edge cases:**
  - **Wrong delete event.** Deleting logs `qualifier_modified` with status **Save**, not
    Remove
    (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/QualifierListViewModel.kt:57`).
  - **Concurrent tasks.** Edit-save runs "remove old" and "save new" as two separate
    background tasks
    (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/QualifierAdapter.kt:63-64`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/QualifierListViewModel.kt:60-67`).
    Their order is not guaranteed by the code.
  - **Unknown type codes.** An unknown stored type is read as SKIP
    (`android/legacy/src/main/java/com/mas/mobile/repository/message/QualifierMapper.kt:17-23`).
- **Data touched:** `qualifiers`, `settings` (`message.discoveryMode`).
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/QualifierListViewModel.kt:15-73`

## UC-L5.12: Blacklist a sender or delete a message

- **Actor:** user
- **Trigger:** One of:
  - **Blacklist:** the "blacklist" button on a Recommended message, with confirmation
    "Do you really want to add <sender> to the blacklist?"
    (`android/legacy/src/main/java/com/mas/mobile/presentation/adapter/SpendingMessageAdapter.kt:83-85`,
    `android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MessageListFragment.kt:83-90`,
    `android/legacy/src/main/res/values/strings.xml:105`).
  - **Delete:** swiping a message left or right, with confirmation
    (`android/legacy/src/main/java/com/mas/mobile/presentation/activity/fragment/MessageListFragment.kt:49-61`).
- **Preconditions:** None.
- **Main flow:**
  1. **Blacklist.** Create a BLACKLIST qualifier holding the trimmed sender. Delete this
     message
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageListViewModel.kt:40-45`,
     `android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:23-29`).
  2. **Delete.** Delete the message row
     (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/ListViewModel.kt:12-16`,
     `android/legacy/src/main/java/com/mas/mobile/repository/message/MessageRepositoryImpl.kt:45-47`).
- **Rules:**
  - The blacklist stops future **recommendations** from that sender (UC-L5.6). It does
    not stop template matching (UC-L5.3).
- **Edge cases:**
  - **Duplicates.** The blacklist has no duplicate check, so blacklisting the same sender
    twice creates two rows
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:23-29`).
  - **Other messages kept.** Other messages from the blacklisted sender remain.
  - **Spending kept.** Deleting a message does not delete its spending.
- **Data touched:** `qualifiers` (insert) and `spending_messages` (delete).
- **Source:** `android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageListViewModel.kt:40-45`

## UC-L5.13: Seed templates and qualifiers

- **Actor:** system
- **Trigger:** One of:
  - Creation of a new database
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:61-67`).
  - Migrations 4→5 (qualifiers) and 8→9 (templates)
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:117-124`,
    `android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:162`).
- **Preconditions:** None.
- **Main flow:**
  1. Insert 4 bank templates with ids 1–4. Their senders are bank or app names, they are
     enabled, and each has a currency
     (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:35-44`).
  2. Insert the English qualifiers with ids 1–38 (CATCH, SKIP, BLACKLIST)
     (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:47-86`).
  3. Add more words when the device language code, upper-cased, is `UA`, `RU` or `ES`
     (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:23`,
     `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:87-134`).
- **Rules:** The seed BLACKLIST entries are messenger and social apps, such as
  `whatsapp` and `telegram`
  (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:74-86`).
- **Edge cases:**
  - **Ukrainian words never seeded.** The Ukrainian list is guarded by `language == "UA"`.
    Java reports the Ukrainian language code as `uk`, so this branch does not fire for
    Ukrainian locales
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:23`,
    `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:87`).
  - **ING template doesn't parse its own example.** Seed template 4 has the pattern
    `is {amount} EUR afgeschreven van rekening *007. {merchant}`. Its example contains
    `*003.` instead of `*007.`
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:42-43`).
    The literal account suffix is part of the pattern, so the template matches only
    messages for account `*007`. Its own example parses to `Empty`. That was traced
    through the code, and confirmed with a Python copy of the parser that was not run on
    the JVM. Saving that template unchanged in the editor therefore fails
    validation (UC-L5.10). The other three seed templates parse their examples.
  - **Language fixed at first run.** The language is evaluated once, when `DML` is
    initialised, so later language changes are not reflected
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:46`).
  - **Migration 8→9** reads `qualifiers` with integer ids built by `ROW_NUMBER()`. The seed
    qualifier ids from migration 4→5 do not apply after the rebuild
    (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:164-178`).
- **Data touched:** `message_template`, `qualifiers`.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:35-134`

## UC-L5.14: Detect currencies in a message text

- **Actor:** system
- **Trigger:** Building a template from a Recommended message (UC-L5.9).
  `MessageTemplateService.findCurrency` calls `CurrencyExpert.detectCurrencies(text)`
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageTemplateService.kt:25-34`).
- **Preconditions:** None.
- **Main flow:**
  1. For every currency in `Currency.getAvailableCurrencies()`, test the text against two
     regexes: one built from the currency's `symbol` and one from its `currencyCode`
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/CurrencyExpert.kt:6-10`).
  2. Each regex is `.*(\b<escaped>\b|\d+<raw>|<escaped>\d+).*` with `DOT_MATCHES_ALL`,
     where `<escaped>` is the value passed through `Regex.escape` and `<raw>` is the
     **unescaped** value
     (`android/legacy/src/main/java/com/mas/mobile/domain/message/CurrencyExpert.kt:12-15`).
  3. Return every currency for which either regex matches the whole text. The caller then
     picks one (UC-L5.9 Rules).
- **Rules:**
  - A currency is detected when its code or symbol stands between word boundaries,
    directly after digits, or directly before digits.
  - For symbols made of non-word characters, such as `€` or `$`, the word-boundary
    alternative needs a letter or digit on both sides. In practice such symbols are found
    only next to digits.
  - The match is case-sensitive. No `IGNORE_CASE` option is set
    (`android/legacy/src/main/java/com/mas/mobile/domain/message/CurrencyExpert.kt:15`).
- **Edge cases:**
  - **Unescaped middle alternative.** It uses the symbol or code without escaping. For
    the symbol `$`, `\d+$` means "digits at the end of the text". That includes digits
    followed by one final line terminator. Any such text then matches the currency whose
    symbol is `$` in the current locale, for example USD under en-US. A Python copy of
    this regex gave `Card 1234` → match and `Paid 10.00 EUR` → no match. Python's
    semantics match the JVM's for these inputs, but the copy was not run on the JVM.
  - **Upper-case words.** Upper-case words that equal an ISO code are detected as that
    currency. Examples: `TRY` (Turkish lira) in `PLEASE TRY AGAIN`, `ALL`, `TOP`.
  - **Lower-case codes.** `eur` is not detected, because the match is case-sensitive.
  - **Locale-dependent symbols.** `Currency.symbol` is the symbol for the default locale,
    so which symbols are recognised depends on the device locale. That is
    `java.util.Currency.getSymbol()` behaviour, which uses the default locale's data. It
    is platform behaviour and is not cited from app code.
- **Data touched:** None. This is a pure function.
- **Source:** `android/legacy/src/main/java/com/mas/mobile/domain/message/CurrencyExpert.kt:5-17`

## Test coverage of `domain/message`

The unit tests in `android/legacy/src/test/java/com/mas/mobile/domain/message/` were read.
They were **not run**: running them would write build output under `android/legacy/`,
which this phase forbids. The table records what each test file covers. Behaviour above
is still cited from main code.

| Test file | Tests | What is covered |
|---|---|---|
| `PatternTest.kt` | 10 | Amount and merchant in both orders, amount only through the fallback, several amounts (first wins), escaped special characters, multi-line text, non-Latin text, a non-matching pattern, an empty message, and an empty pattern (constructor error message) (`android/legacy/src/test/java/com/mas/mobile/domain/message/PatternTest.kt:10-120`) |
| `MessageTemplateTest.kt` | 2 | `parse` returns amount and merchant, or `null` for unrelated text (`android/legacy/src/test/java/com/mas/mobile/domain/message/MessageTemplateTest.kt:9-22`) |
| `CurrencyExpertTest.kt` | 4 | Detection by code, by the `€` symbol, by code after a newline, and two currencies in one text (`android/legacy/src/test/java/com/mas/mobile/domain/message/CurrencyExpertTest.kt:9-36`) |
| `QualifierServiceTest.kt` | 9 | Catch word or phrase ignoring case, skip word or phrase, missing amount, text over the length limit, missing currency, blacklisted sender ignoring case (`android/legacy/src/test/java/com/mas/mobile/domain/message/QualifierServiceTest.kt:38-83`) |
| `MessageTemplateServiceTest.kt` | 2 | A template built from the analyzer's pattern, with the currency detected as EUR, and `null` when the analyzer returns `null` (`android/legacy/src/test/java/com/mas/mobile/domain/message/MessageTemplateServiceTest.kt:29-46`) |
| `MessageServiceTest.kt` | 4 | Raw message classified as Matched, Recommended or Rejected (not saved), and promotion of a Recommended message to Matched with the template saved (`android/legacy/src/test/java/com/mas/mobile/domain/message/MessageServiceTest.kt:101-162`) |

The tests do not exercise:
- **Patterns without `{merchant}`.** These include the `{amount}` fallback crash
  (UC-L5.4). `MessageTemplateServiceTest` builds `Pattern("{amount}")`, but never parses
  with it
  (`android/legacy/src/test/java/com/mas/mobile/domain/message/MessageTemplateServiceTest.kt:63`).
- **Parsing edge cases:** amounts with thousands separators and placeholder case.
- **Anchor failures:** a leading or trailing placeholder that is not at the very start or
  end of the text. Anchored patterns are tested only where they match
  (`android/legacy/src/test/java/com/mas/mobile/domain/message/PatternTest.kt:56-57`,
  `android/legacy/src/test/java/com/mas/mobile/domain/message/PatternTest.kt:84-88`).
- **Template selection:** a disabled template (Rejected), a sender mismatch, and the
  order of several matching templates.
- **Automatic spending.** `createSpending` runs in the Matched test with relaxed mocks,
  but its preconditions, merchant lookup and result are not asserted. Sibling promotion
  runs in the promotion test, but its results are not asserted
  (`android/legacy/src/test/java/com/mas/mobile/domain/message/MessageServiceTest.kt:101-118`,
  `android/legacy/src/test/java/com/mas/mobile/domain/message/MessageServiceTest.kt:152-162`).
- **The seed templates** against their own examples.
- **Currency detection limits:** texts ending in digits, upper-case words equal to ISO
  codes, and several currencies including the Settings currency.
- **Currency-symbol substrings** in the recommendation check, such as `Fr` and the
  system currency's symbol.

Some test expectations depend on the JVM's default locale. For example, detecting `€`
depends on `Currency.symbol` (UC-L5.14). The system currency is added to the
recommendation currency list
(`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:69-73`).

## Open questions

1. **Capture switches.** `captureSms` and `captureNotifications` are not read on the
   intake path (UC-L5.1, UC-L5.2). Whether capture actually stops when a switch is off
   depends on OS permission state, covered in area 6.
2. **Seed qualifiers via 4→5.** Migration 4→5 seeds the qualifiers with an
   `INSERT … (id, name, type)`
   (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:120-121`,
   `android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:49`). At v5,
   `qualifiers` has no `id` column (`name` is the primary key)
   (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/AppDatabase.kt:120`,
   `android/legacy/schemas/com.mas.mobile.repository.db.config.AppDatabase/5.json:382-403`).
   Does that insert fail on upgrades from v4? The code alone does not show how this
   played out on real devices.
3. **Template order.** With several matching templates, which one wins depends on SQLite
   row order, because there is no `ORDER BY`
   (`android/legacy/src/main/java/com/mas/mobile/repository/db/dao/MessageTemplateDAO.kt:15-16`).
4. **Toast from the background.** Exceptions such as the `cutToAmount` one raised during
   `handleRawMessage` are routed to a toast from a background service context
   (`android/legacy/src/main/java/com/mas/mobile/service/CoroutineService.kt:23-25`,
   `android/legacy/src/main/java/com/mas/mobile/service/ErrorHandler.kt:20-29`). What the
   user sees then is platform behaviour.

## Surprises

- **Bare `{amount}` crashes the fallback.** A pattern of exactly `{amount}`, the default
  `Pattern.SIMPLE`, throws `StringIndexOutOfBoundsException` whenever its sender's text
  does not start with a number. This aborts classification of that message. Other
  patterns without `{merchant}` lose their first 9 characters in the fallback
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:54-63`).
- **Thousands separators misparsed.** `1,234.56` is either read as 1.234 or not parsed
  at all, depending on the pattern
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:91`,
  `android/legacy/src/main/java/com/mas/mobile/domain/message/Pattern.kt:99`).
- **Multi-part SMS split.** A multi-part SMS is processed as separate messages, one per
  PDU
  (`android/legacy/src/main/java/com/mas/mobile/service/SmsListener.kt:27-35`).
- **Literal "null" text.** Notifications with only a title or only a text get the literal
  `null` in the stored text. An unknown app gives the sender `"null"`
  (`android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:26`,
  `android/legacy/src/main/java/com/mas/mobile/service/NotificationListener.kt:33`).
- **"Fr" passes the currency check.** CHF's symbol `Fr` makes any text containing "fr"
  pass the currency check
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:66`).
- **Whole amounts not recommended.** Amounts without decimals fail the recommendation
  heuristic, unless some other `digit[.,]digit` appears in the text
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/QualifierService.kt:55`).
- **Disabled templates suppress.** A disabled template that parses a message makes it
  Rejected, so the message is not recommended either
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:105-113`).
- **Manual matching drops time and currency.** Manual matching of a Matched message uses
  the current time and the budget currency, not the message time and template currency
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/SpendingViewModel.kt:169-180`).
- **Siblings get no spending.** Promoted sibling messages become Matched, but no spending
  is created for them
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/MessageService.kt:85-96`).
- **Message text in logs.** The full message text is written to the debug log, and sent
  to OpenAI on promotion
  (`android/legacy/src/main/java/com/mas/mobile/service/ai/GPTMessageAnalyzer.kt:28`,
  `android/legacy/src/main/java/com/mas/mobile/service/ai/GPTMessageAnalyzer.kt:33`).
- **Templates can't be added manually.** There is no screen action to add a template.
  Template clone exists in code but opens read-only and is unreachable
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/MessageTemplateViewModel.kt:134-138`).
- **Badge counts beyond the list.** The unread badge counts messages older than the
  30-day list window
  (`android/legacy/src/main/java/com/mas/mobile/repository/message/MessageRepositoryImpl.kt:38-39`).
- **Wrong delete event.** Qualifier deletion logs a `Save` event
  (`android/legacy/src/main/java/com/mas/mobile/presentation/viewmodel/QualifierListViewModel.kt:57`).
- **ING seed template broken.** Seed template 4 cannot parse its own example, because a
  literal account suffix is part of the pattern
  (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:42-43`).
- **Ends-in-digits means a `$` currency.** Currency detection treats any text ending in
  digits as containing the currency whose locale symbol is `$` (USD under en-US), because
  one regex part is not escaped
  (`android/legacy/src/main/java/com/mas/mobile/domain/message/CurrencyExpert.kt:15`).
- **Edge paths untested.** The unit tests assert none of the edge paths listed in "Test
  coverage of `domain/message`"
  (`android/legacy/src/test/java/com/mas/mobile/domain/message/PatternTest.kt:10-120`,
  `android/legacy/src/test/java/com/mas/mobile/domain/message/MessageServiceTest.kt:101-162`).
- **Ukrainian words never seeded.** The Ukrainian seed words are guarded by `"UA"`, which
  never matches Java's `uk`
  (`android/legacy/src/main/java/com/mas/mobile/repository/db/config/DML.kt:87`).
