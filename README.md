# Smart SMS Organizer

**A feature-rich, open-source SMS app for Android with the smart-organization features of a modern messenger — built on [QUIK](https://github.com/quik-sms/quik) (a continuation of [QKSMS](https://github.com/moezbhatti/qksms)).**

Smart SMS Organizer automatically sorts your inbox, surfaces one-time passwords the moment they arrive, and gives you full control over every RCS-related behavior with a single master switch.

## Highlights

### Smart inbox auto-organization
Incoming messages are classified on-device into **Personal**, **Transactions**, **OTP**, **Offers** and **Spam**. Filter your inbox instantly with the category chips at the top of the conversation list — no cloud, no ML service, fully offline heuristics (sender-ID patterns such as DLT-style transactional sender IDs plus weighted keyword scoring).

### OTP copy popup
When a verification code arrives, Smart SMS Organizer shows an SMS Organizer-style popup over whatever you're doing and copies the code to your clipboard — one tap to copy, tap outside to dismiss. If the overlay permission isn't granted, the notification gets a **"Copy code"** action instead, so the code is always one tap away.

### Auto-delete OTPs
Verification codes are useless 24 hours later. Enable auto-delete and stale OTP messages are cleaned up daily in the background.

### RCS chats with a real off switch
A single **RCS chats** master switch in Settings → General. **When it's off, every RCS code path in the app is disabled completely** — no capability probes, no indicators, nothing. When it's on, the app does a best-effort probe of the device's RCS provisioning state and reports it in Settings.

> **Honest note on RCS:** Android does not expose a public API for third-party apps to *transport* RCS messages — that requires carrier/Google certification. Smart SMS Organizer therefore treats RCS as an indicator layer on top of SMS/MMS and always delivers over SMS/MMS. The master switch guarantees zero RCS behavior when disabled.

### Everything inherited from QUIK
- Scheduled and delayed sending
- Message backup & restore
- Speech-to-text and text-to-speech
- Message blocking, archiving, sorting, pinning
- Voice messages and any-file-type attachments
- Quick reply from notifications
- Swipe actions, emoji reactions
- Dual SIM, delivery reports, night mode, and much more

## Download

Grab the latest APK from the [releases page](https://github.com/Jarvis7654/smart-sms-organizer/releases).

## Building from source

Requirements: JDK 17, Android SDK (platform 34, build-tools 34.0.0).

```bash
git clone https://github.com/Jarvis7654/smart-sms-organizer.git
cd smart-sms-organizer
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew :presentation:assembleDebug
```

The debug APK lands in `presentation/build/outputs/apk/debug/`.

For a release build, create `./.gradle/.gradlerc` with `storePassword` / `keyPassword` and place a keystore at `presentation/my-release-key.keystore` (alias `quik_release`), then run `./gradlew :presentation:assembleRelease`.

## Contributing

Contributions are welcome — open an issue or a pull request. Please keep new features fully on-device and license-compatible (GPLv3).

## License & credits

This project is licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE).

Smart SMS Organizer is a fork of [QUIK](https://github.com/quik-sms/quik) by octoshrimpy and contributors, which is itself a continuation of [QKSMS](https://github.com/moezbhatti/qksms) by Moez Bhatti. All credit for the original messaging core goes to those projects; the smart-organization, OTP popup, OTP auto-delete and RCS-gating features introduced here are new work, licensed under the same GPLv3.
