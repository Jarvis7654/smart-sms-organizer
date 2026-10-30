# Smart SMS Organizer

**A feature-rich, open-source SMS app for Android with the smart-organization features of a modern messenger — built on [QUIK](https://github.com/quik-sms/quik) (a continuation of [QKSMS](https://github.com/moezbhatti/qksms)).**

Smart SMS Organizer automatically sorts your inbox, surfaces one-time passwords the moment they arrive, 

## Highlights

### Smart inbox with organizer tabs
The inbox opens on SMS Organizer style tabs: **All, Personal, Transactions, OTP, Offers, Upcoming, Spam** — each with a live count. Messages are classified fully on-device (no ML service, no network) using sender-ID patterns and weighted keyword scoring.

### Upcoming reminders
Bills due, train/flight/bus journeys, movies, doctor appointments and deliveries are automatically extracted into reminder cards with dates, times and amounts — like SMS Organizer's Upcoming tab. You also get custom reminders and a heads-up notification before things come due.

### OTP & offer code copy popup
Verification codes pop up over whatever you're doing in a bottom card and copy to your clipboard in one tap. Coupon/offer codes get the same treatment ("Offer Code Copied"). No overlay permission? The notification gets a one-tap **Copy code** action instead.

### Transactions that talk money
Amounts are pulled right onto the conversation rows — green for credits, red for debits, straight from the message text.

### Star messages
Star any SMS from the conversation's action bar and find it later in the **Starred** folder in the drawer.

### Move-to rules
Select conversations → **Move to** → pick a category, optionally applying it to *all future messages* from that sender ("Move future SMS").

### Everything inherited from QUIK
- Scheduled and delayed sending
- Message backup & restore
- Speech-to-text and text-to-speech
- Message blocking, archiving, sorting, pinning
- Voice messages and any-file-type attachments
- Quick reply from notifications
- Swipe actions, emoji reactions
- Auto-delete OTPs, dual SIM, delivery reports, night mode, and much more

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

Smart SMS Organizer is a fork of [QUIK](https://github.com/quik-sms/quik) by octoshrimpy and contributors, which is itself a continuation of [QKSMS](https://github.com/moezbhatti/qksms) by Moez Bhatti. All credit for the original messaging core goes to those projects; the smart-organization, OTP popup, reminders and auto-delete features introduced here are new work, licensed under the same GPLv3.
