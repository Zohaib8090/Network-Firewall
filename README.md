<p align="center"><img src="docs/icon.png" alt="Smart Network Guard icon" width="128"></p>

# Smart Network Guard

A privacy-focused firewall for Android that controls internet access **per app**, separately for **Wi-Fi** and **mobile data**. It doesn't need root: it uses Android's built-in `VpnService` locally on the device, and no traffic is sent to any server.

## Features

- **Per-app blocking**: block or allow each app on Wi-Fi and mobile data independently.
- **Schedules**: block selected apps (or all of them) during set times on set days, including overnight ranges like 22:00–06:30.
- **Profiles**: one-tap presets such as Study/Work, Gaming, Battery Saver and Offline.
- **Global lock**: cut off every app at once.
- **Temporary access**: allow a blocked app for a limited time, from the "blocked" notification ("Allow 10 min") or the in-app prompt.
- **Pause**: switch blocking off for a set time; it turns back on automatically.
- **Usage monitoring**: live network speed, per-app usage for today and this month, and a 7-day chart.
- **Data-limit warnings**: notifications at 80%, 90% and 100% of an app's daily limit.
- **Block log**: a record of blocked connection attempts, with destination address and port.
- **New app handling**: newly installed apps are detected automatically and have mobile data blocked by default.
- **Backup**: export and import rules as JSON.
- Light/dark theme with selectable accent colours.

## How it works

The app starts a local VPN, and only the apps you've blocked are routed into it (IPv4 and IPv6). Packets from those apps are dropped on the device, so they have no connection; every other app uses the network normally. The firewall re-applies its rules when you switch between Wi-Fi and mobile data, when a schedule starts or ends, when a pause or temporary allowance runs out, and when apps are installed or removed.

Because it uses the VPN slot, it can't run at the same time as another VPN app.

## Requirements

- Android 7.0 (API 24) or newer; targets Android 16 (API 36).
- Permissions you grant when asked:
  - **VPN connection**: required for blocking.
  - **Usage access** (optional): needed for accurate per-app usage numbers.
  - **Notifications**: for blocked-attempt and data-limit alerts.
  - **Alarms & reminders** (optional, Android 12+): lets schedules and timers switch on the exact minute; without it they may run a few minutes late.

## Building

The repository doesn't include the Gradle wrapper script, so use a local Gradle install:

1. Install **JDK 21** or newer (the Robolectric unit tests need it for Android SDK 36), **Gradle 9.3.1**, and the Android SDK with **platform `android-36.1`**.
2. Create `local.properties` in the repo root containing `sdk.dir=/path/to/Android/sdk`.
3. Optional: debug builds use `debug.keystore` from the repo root if it exists (it's gitignored); otherwise Android's standard auto-generated debug key is used. To make your own:
   ```sh
   keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey \
     -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
   ```
4. Build and test:
   ```sh
   gradle wrapper              # optional: generates ./gradlew
   gradle :app:assembleDebug   # APK in app/build/outputs/apk/debug/
   gradle :app:testDebugUnitTest
   ```

GitHub Actions runs the unit tests and builds a debug APK on every push and pull request to `main` (see `.github/workflows/build_apk.yml`); the APK is attached to each run as the `app-debug` artifact. For judging performance, use a release build: debug builds of Jetpack Compose apps are noticeably slower.

## Project structure

```
app/src/main/java/com/example/
├── vpn/          FirewallVpnService (the firewall), packet parser, timers
├── data/         Room database (rules, schedules, block logs), DataStore settings, repositories
├── receiver/     Boot start, install/uninstall sync, notification actions, alarms
├── service/      Periodic background job (rule refresh, data-limit alerts)
└── ui/           Jetpack Compose screens: Firewall, Monitoring, Schedules, Settings/Logs
```

Built with Kotlin, Jetpack Compose (Material 3), Room, DataStore and WorkManager. More detail for contributors is in [`CLAUDE.md`](CLAUDE.md).

## Known limitations

- On Android 9 and older, blocked attempts aren't logged when more than one app is blocked, because the system can't report which app sent a packet.
- Rule export currently produces an empty list, and "allow for this session" doesn't expire yet.
- Weekly and monthly data limits are saved but not enforced; daily limits send warnings but don't block.
- Without usage access, per-app usage figures are estimates, and the 7-day chart shows placeholder data when no real data is available.

See [CHANGELOG.md](CHANGELOG.md) for recent changes.

## Contributing

Contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md). To report a security problem, see [SECURITY.md](SECURITY.md).

## Contact

Questions, bug reports or security reports: [zohaibbaig144@gmail.com](mailto:zohaibbaig144@gmail.com)

## License

[MIT](LICENSE) © 2026 Zohaib8090
