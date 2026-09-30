<p align="center"><img src="docs/icon.png" alt="Network-Firewall icon" width="128"></p>

# Network-Firewall

A privacy-focused firewall for Android that controls internet access **per app**, separately for **Wi-Fi** and **mobile data**. It doesn't need root: it uses Android's built-in `VpnService` locally on the device: traffic from blocked apps is dropped there and nothing is forwarded to any server.

## Features

- **Per-app blocking**: block or allow each app on Wi-Fi and mobile data independently.
- **Schedules**: block selected apps (or all of them) during set times on set days, including overnight ranges like 22:00–06:30.
- **Profiles**: one-tap presets such as Study/Work, Gaming, Battery Saver and Offline.
- **Global lock**: cut off every app at once.
- **Asked when you open a blocked app**: when you open an app the firewall is blocking (for example WhatsApp during Global Lock), a notification asks what to do: **Allow while open** (blocked again shortly after you close the app), **Allow 10 min**, or **Keep blocked**. Apps running in the background never trigger notifications. This needs Usage access.
- **Pause**: switch blocking off for a set time; it turns back on automatically.
- **Usage monitoring**: live network speed, per-app usage for today and this month, and a 7-day chart.
- **Data-limit warnings**: notifications at 80%, 90% and 100% of an app's daily limit.
- **Block log**: a record of blocked connection attempts, with destination address and port.
- **New app handling**: newly installed apps are detected automatically and start with nothing blocked; you decide what to block.
- **Backup**: export and import rules as JSON.
- **Update check**: Settings → **Check for updates** asks GitHub for the newest release and links to its download. This is the only time the app goes online, and only when you tap the button.
- Light/dark theme with selectable accent colours.

## How it works

The app starts a local VPN, and only the apps you've blocked are routed into it (IPv4 and IPv6). Packets from those apps are dropped on the device, so they have no connection; every other app uses the network normally. The firewall re-applies its rules when you switch between Wi-Fi and mobile data, when a schedule starts or ends, when a pause or temporary allowance runs out, and when apps are installed or removed.

Because it uses the VPN slot, it can't run at the same time as another VPN app.

## Requirements

- Android 7.0 (API 24) or newer; targets Android 16 (API 36).
- Permissions you grant when asked:
  - **VPN connection**: required for blocking.
  - **Usage access** (recommended): lets the app see which app is open, so it can ask when you open a blocked app; also gives accurate per-app usage numbers. Without it there are no prompts, and "Allow while open" becomes a 30-minute allowance.
  - **Notifications**: for blocked-attempt and data-limit alerts (Android 13+ asks when you first turn the firewall on; the firewall works without it).
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

GitHub Actions runs the unit tests and builds a debug APK on every push and pull request to `main` (see `.github/workflows/build_apk.yml`); two APKs are attached to each run: `app-release` (optimised, use this one) and `app-debug`. Judge performance on the release build (`gradle :app:assembleRelease`, or the `app-release` artifact): debug builds of Jetpack Compose apps are several times slower to scroll. Release builds are signed with the debug key unless you provide an upload key via `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD`.

## Publishing a release

Push a version tag and GitHub builds and publishes the release for you (`.github/workflows/release.yml`):

```sh
git tag v1.1
git push origin v1.1
```

The tag `v1.1` becomes the app's version `1.1`, the optimised APK is attached to the release, and the app's **Check for updates** button will then offer it. The app only offers a release whose version is higher than the installed one.

Android installs an update over the installed app only when both are signed with the same key. Builds made without your own upload key are signed with a temporary debug key that differs from build to build, so until a fixed key is set up, installing a newer build means uninstalling the old one first.

## Project structure

```
app/src/main/java/dev/zohaib/networkfirewall/
├── vpn/          FirewallVpnService (the firewall), packet parser, timers
├── data/         Room database (rules, schedules, block logs), DataStore settings, repositories
├── receiver/     Boot start, install/uninstall sync, notification actions, alarms
├── service/      Periodic background job (rule refresh, data-limit alerts)
└── ui/           Jetpack Compose screens: Firewall, Monitoring, Schedules, Settings/Logs
```

Built with Kotlin, Jetpack Compose (Material 3), Room, DataStore and WorkManager. More detail for contributors is in [`CLAUDE.md`](CLAUDE.md).

## Known limitations

- On Android 9 and older, when more than one app is blocked (including Global Lock), blocked attempts aren't logged and no prompt appears, because the system can't report which app sent a packet.
- Rule export currently produces an empty list, and "allow for this session" doesn't expire yet.
- Weekly and monthly data limits are saved but not enforced; daily limits send warnings but don't block.
- Without usage access, per-app usage figures are estimates, and the 7-day chart shows placeholder data when no real data is available.

See [CHANGELOG.md](CHANGELOG.md) for recent changes.

## Contributing

Contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md). To report a security problem, see [SECURITY.md](SECURITY.md).

## Contact

Questions, bug reports or security reports: [zohaibbaig144@gmail.com](mailto:zohaibbaig144@gmail.com)

## License

[MIT](LICENSE) © 2026 zohaib.dev
