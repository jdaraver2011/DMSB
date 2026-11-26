# SpamShield

An Android app that automatically blocks spam calls and makes it easy to report them to your carrier (7726) and the FTC.

## Features

- **Automatic call blocking** using Android's Call Screening role (Android 10+). No need to replace your phone/dialer app.
- **Area code blocking.** Block every call from one or more area codes (e.g. `876, 809`). Includes a one-tap preset for the Caribbean area codes the FCC has flagged for "one ring" callback scams.
- **Number and prefix blocking.** Block a single number, or everything starting with some digits (e.g. `900`, `1800555`).
- **Allow list.** Numbers that always ring, overriding every block rule.
- **Smart rules (toggle each):**
  - Hidden / private / unknown caller ID
  - Carrier STIR/SHAKEN verification failed (spoofed caller ID, Android 11+)
  - Neighbor spoofing (same area code + exchange as your own number)
  - Block everyone not in contacts (aggressive mode)
  - Contacts always ring, even if they match an area code or prefix rule
- **Reject or silence.** Send blocked callers away, or let the call ring out silently.
- **Blocked-call history** with the reason each call was blocked.
- **Recent calls tab** (from your call log) so you can report or block spam that got through.
- **One-tap reporting:**
  - **Carrier (7726 / "SPAM")**: opens your SMS app addressed to 7726 with the message pre-filled; the number is copied to your clipboard for when the carrier replies asking for it. The message template is editable.
  - **FTC Do Not Call**: opens the [FTC complaint form](https://www.donotcall.gov/report.html) with the number copied to your clipboard.
  - Notifications for blocked calls include **Report 7726** and **Report FTC** buttons.

Everything stays on the device. There is no server, no account, and no data leaves your phone except the reports you choose to send.

## Rule precedence

1. Screening switched off → allow
2. Allow list → allow
3. Saved contact (if "Contacts always ring") → allow
4. Hidden caller ID → block (if enabled)
5. Blocked number → block
6. Blocked area code → block
7. Blocked prefix → block
8. Failed caller ID verification → block (if enabled)
9. Neighbor spoofing → block (if enabled)
10. Not in contacts → block (if enabled)
11. Otherwise → allow

The logic lives in [`CallRuleEngine.kt`](app/src/main/java/com/proteksolutions/spamshield/screening/CallRuleEngine.kt) and is covered by unit tests.

## Building

Requirements: Android Studio (Ladybug or newer) or JDK 17 + Android SDK 35.

```bash
./gradlew testDebugUnitTest   # run unit tests
./gradlew assembleDebug       # build app/build/outputs/apk/debug/app-debug.apk
```

Or just open the folder in Android Studio and press Run.

### CI

Every push to `main` and every pull request runs the GitHub Actions workflow in `.github/workflows/android.yml`, which runs the tests and builds a debug APK. Download the APK from the workflow run's **Artifacts** section.

## First run on the phone

1. Install the APK and open SpamShield.
2. Tap **Enable call screening** and choose SpamShield as your *Caller ID & spam app*.
3. Tap **Grant permissions** (contacts, call log, notifications). Each is optional, but some features need them.
4. Go to **Block list → Add** to add area codes, numbers or prefixes.

## Notes and limitations

- Requires Android 10 (API 29) or newer.
- Area code and neighbor-spoofing rules apply to North American (+1) numbers.
- On many phones Android doesn't send calls from saved contacts to call screening apps at all, so contacts may ring regardless of rules.
- The FTC and carriers don't offer a public API for consumer reports, so reporting hands off to their own SMS/web flow and you confirm the send.
- `READ_CALL_LOG` is a restricted permission on Google Play. Sideloading is fine; publishing to Play would need a policy declaration or removing the Recent tab.

## Project layout

```
app/src/main/java/com/proteksolutions/spamshield/
├── MainActivity.kt                 # Single activity, handles notification actions
├── SpamShieldApp.kt                # Creates notification channel
├── BlockNotifier.kt                # "Call blocked" notification with Report buttons
├── data/
│   ├── PhoneNumbers.kt             # Normalization, area code parsing/matching
│   ├── Rule.kt                     # Rule / BlockedCall models
│   ├── SpamDb.kt                   # SQLite: rules + blocked-call history
│   └── Settings.kt                 # SharedPreferences toggles
├── screening/
│   ├── CallRuleEngine.kt           # Pure block/allow decision logic
│   └── SpamCallScreeningService.kt # Android CallScreeningService
├── report/
│   └── SpamReporter.kt             # 7726 SMS + FTC hand-off
└── ui/                             # Jetpack Compose screens
```
