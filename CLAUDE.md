# CLAUDE.md

Smart Network Guard: a no-root Android firewall (Kotlin, Jetpack Compose, Room, DataStore, WorkManager) that blocks network access per app, separately for Wi-Fi and mobile data, using `VpnService`. Scaffolded from a Google AI Studio template, which is why the namespace is `com.example` and the application ID is `com.aistudio.networkfirewall.app`.

## Build and test

- No Gradle wrapper is committed. CI (`.github/workflows/build_apk.yml`) runs `gradle wrapper` and then `./gradlew assembleDebug`. Locally, use Gradle 9.3.1 (see `gradle/wrapper/gradle-wrapper.properties`) and an SDK containing `platforms;android-36.1`, with `sdk.dir` set in `local.properties`.
- Unit tests (JVM and Robolectric): `gradle :app:testDebugUnitTest`. Compile only: `gradle :app:compileDebugKotlin`.
- Debug signing expects `debug.keystore` at the repo root. It is gitignored, so packaging tasks fail without it, but compiling and unit tests work.
- Known failing test: `ExampleRobolectricTest."read string from context"` expects `app_name` "Smart Network Guard", but `strings.xml` has "Network-Firewall".
- Screenshot test (`GreetingScreenshotTest`) uses Roborazzi and writes to `app/src/test/screenshots/`.

## Architecture

All code lives under `app/src/main/java/com/example/`.

- `vpn/FirewallVpnService.kt`: the core of the app. `reconfigureVpn()` (serialized by a mutex) works out the set of blocked packages from app rules, active schedules, Global Lock and the pause setting. It then builds a tunnel with `addAllowedApplication` for **only the blocked apps**, routing IPv4 and IPv6, and a packet-drop loop discards their traffic. Unblocked apps never enter the tunnel.
  - `reload(context)` does nothing unless the service is running, and `reconfigureVpn()` tears down the tunnel if the user has the firewall switched off (`AppPreferences.isVpnEnabled`). Rule changes must never start the firewall by themselves.
  - Wi-Fi/mobile changes come from `registerDefaultNetworkCallback`, ignoring networks with `TRANSPORT_VPN`.
  - Time-based changes (pause end, temporary-allow expiry, schedule start and end) are handled by an `AlarmManager` alarm to `ScheduleAlarmReceiver`. The alarm is exact when `SCHEDULE_EXACT_ALARM` is granted and inexact otherwise. The wakeup time comes from `vpn/FirewallTimers.kt`.
  - The app that sent a dropped packet is worked out by `resolveOwnerPackage`: certain when only one app is blocked; otherwise it uses `ConnectivityManager.getConnectionOwnerUid` (Android 10+). Packets it can't attribute are not logged. Block logs are throttled per app, destination and port.
  - `vpn/PacketParser.kt`: IPv4/IPv6 header parsing (pure, unit-tested).
- `data/`: Room DB `smart_network_guard.db` (version 2, `fallbackToDestructiveMigration`; bumping the version **wipes user rules**). It has three entities: `AppRule` (keyed by package name), `ScheduleRule` (days as a CSV of `MON..SUN`, targets as a CSV or `"ALL"`, end minute inclusive), and `BlockLog`. `AppPreferences` wraps DataStore settings.
- `data/repository/`: `FirewallRepository` (rule CRUD, profiles, install sync, JSON export/import) and `DataUsageRepository` (`NetworkStatsManager` usage, which needs the usage-access permission, plus live speed).
- `receiver/`: boot auto-start, notification actions (allow 10 min and so on), package install/remove/replace sync, and the alarm receiver.
- `service/ScheduleWorker.kt`: 15-minute periodic job that reloads the firewall and sends data-limit notifications.
- `ui/`: a single `MainViewModel` with `MainScreen`, which has 4 tabs (Firewall, Monitoring, Schedules, Settings/Logs), plus dialogs in `ui/components/`.

## Conventions

- After changing anything that affects blocking (rules, schedules, preferences), call `FirewallVpnService.reload(context)`.
- Keep pure logic (parsing, time calculations, rule evaluation) out of Android classes so it can be unit-tested, as `PacketParser` and `FirewallTimers` are; tests go in `app/src/test/java/com/example/`.
- New apps default to **mobile data blocked** (`syncInstalledApps`/`onPackageAdded` with `blockMobile...ByDefault = true`). This is intentional in the current code; don't change it silently.
- Several dependencies (Firebase AI/AppCheck, Retrofit, OkHttp, Moshi) come from the template and are unused. They add the INTERNET permission to the merged manifest despite the "zero internet" comment in `AndroidManifest.xml`.

## Known issues (not yet fixed)

- `MainViewModel.exportRules()` returns before its coroutine finishes, so Export always shows an empty rule list.
- `allowSession` is never reset, and weekly/monthly data limits are stored but never enforced. The daily limit only sends notifications.
- Profiles only add blocks: `NORMAL` doesn't unblock anything, and switching profiles doesn't undo the previous one.
- Without usage access, per-app usage figures are estimates, and the 7-day chart shows made-up data when real data is zero.
