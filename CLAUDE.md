# CLAUDE.md

Network-Firewall: a no-root Android firewall (Kotlin, Jetpack Compose, Room, DataStore, WorkManager) that blocks network access per app, separately for Wi-Fi and mobile data, using `VpnService`. The namespace (code package) and the application ID are both `dev.zohaib.networkfirewall`. They came from a Google AI Studio template as `com.example` and `com.aistudio.networkfirewall.app` and were changed on purpose; don't bring those back. Changing the application ID again would make Android treat the app as a different app (separate data, no in-place update), so only do it if the owner asks.

## Build and test

- No Gradle wrapper is committed. CI (`.github/workflows/build_apk.yml`) pins Gradle 9.3.1 with `gradle/actions/setup-gradle`, then runs `gradle :app:testDebugUnitTest` and `gradle :app:assembleDebug`. Use JDK 21: Robolectric refuses to run SDK 36 tests on Java 17 (`requires Java 21`), which is what failed CI after the keystore fix. Locally, use Gradle 9.3.1 and an SDK containing `platforms;android-36.1`, with `sdk.dir` set in `local.properties`.
- Unit tests (JVM and Robolectric): `gradle :app:testDebugUnitTest`. Compile only: `gradle :app:compileDebugKotlin`.
- Two APKs are built and uploaded by CI: `app-debug` and `app-release`. The release build has R8 minification and resource shrinking on and is signed with the debug key when no upload key is supplied (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`), so it installs like the debug one. **Judge speed (scrolling, startup) only on `app-release`**: debug builds of Compose lists are several times slower, and the owner reported scroll lag on the debug build. Anything Android loads by name must survive R8; the current ones (Room database, worker, service, receivers, activity) are kept by their libraries' and the manifest's rules, which was checked against `mapping.txt`. Run `gradle :app:assembleRelease` before pushing changes that add reflection or new manifest components.
- Signing configs are only created when their keystore exists (`debug.keystore` at the repo root, gitignored; release upload key via `KEYSTORE_PATH`). Otherwise debug builds use AGP's auto-generated debug key and release APKs are unsigned. Don't make a missing keystore fail the build again: that is what broke every CI run before.
- The app is called "Network-Firewall" everywhere it is shown (`app_name`, top bar, notification titles, the VPN name in Android settings, docs), matching the repository; tests assert `app_name`. It used to be "Smart Network Guard", which survives only in internal identifiers that must not be renamed: the Room file `smart_network_guard.db` and DataStore `smart_guard_settings` (a new name would silently wipe users' saved rules and settings), the `smart_guard_*` notification channel IDs and work name (a new ID resets users' notification settings), and `SmartNetworkGuardTheme`.
- Launcher icon: vector adaptive icon (`drawable/ic_launcher_{background,foreground,monochrome}.xml`) plus WebP mipmaps for API 24–25; `docs/icon.png` is the 512px version used in the README.
- Screenshot test (`GreetingScreenshotTest`) uses Roborazzi and writes to `app/src/test/screenshots/`.

## Workflow

- Commit and push straight to `main`; don't open pull requests unless asked. This is the owner's standing instruction.
- Nothing checks a change before it lands on `main` (CI only runs after the push), so run what CI runs first: `gradle :app:testDebugUnitTest :app:assembleDebug` on JDK 21. If the `main` build turns red, fixing it comes before any other work.
- Commits are authored as the owner (`zohaib.dev <zohaibbaig144@gmail.com>`, their GitHub profile identity) with Claude as `Co-Authored-By`. Never rewrite the history of `main`.

## Architecture

All code lives under `app/src/main/java/dev/zohaib/networkfirewall/`.

- `vpn/FirewallVpnService.kt`: the core of the app. `reconfigureVpn()` (serialized by a mutex) works out the set of blocked packages from app rules, active schedules, Global Lock and the pause setting. It then builds a tunnel with `addAllowedApplication` for **only the blocked apps**, routing IPv4 and IPv6, and a packet-drop loop discards their traffic. Unblocked apps never enter the tunnel.
  - `reload(context)` does nothing unless the service is running, and `reconfigureVpn()` tears down the tunnel if the user has the firewall switched off (`AppPreferences.isVpnEnabled`). Rule changes must never start the firewall by themselves.
  - Wi-Fi/mobile changes come from `registerDefaultNetworkCallback`, ignoring networks with `TRANSPORT_VPN`.
  - Time-based changes (pause end, temporary-allow expiry, schedule start and end) are handled by an `AlarmManager` alarm to `ScheduleAlarmReceiver`. The alarm is exact when `SCHEDULE_EXACT_ALARM` is granted and inexact otherwise. The wakeup time comes from `vpn/FirewallTimers.kt`.
  - The app that sent a dropped packet is worked out by `resolveOwnerPackage`: certain when only one app is blocked; otherwise it uses `ConnectivityManager.getConnectionOwnerUid` (Android 10+). Packets it can't attribute are not logged. Block logs are throttled per app, destination and port.
  - The UI must never show the firewall as active when it isn't. Whenever the service stops itself (`onRevoke` because another VPN took over, `establish()` returning null, or an exception while building the tunnel) it goes through `stopWithReason(VpnStopReason)`: it clears `isVpnEnabled`, posts a "Firewall stopped" notification, and sets `FirewallVpnService.stopReason`, which `MainScreen` shows as a snackbar with a "Turn on" action. A tunnel is only built if at least one blocked app could be added, because a tunnel with no allowed apps captures every app.
  - Which apps are blocked is decided by the pure `vpn/BlockPlanner.kt` (rules, schedules, Global Lock). An app with a temporary or "while open" allowance (`AppRule.isTemporarilyAllowed`) is never blocked, including during Global Lock and schedules.
  - Prompts: a blocked app only triggers the "Allow while open / Allow 10 min / Keep blocked" notification when it is the app in front. `ForegroundTracker` (pure `ForegroundLogic` over `UsageStatsManager` events, polled every 2 s by the service's foreground watcher) finds the front app; background traffic is logged but never notifies. "Keep blocked" suppresses prompts for that app until it has left the screen (`suppressPrompt`).
  - "Allow while open" is `AppRule.allowSession`. The watcher ends it through the pure `SessionEndPolicy`: 15 s after the app leaves the screen, or 10 min if it was never opened. All of this needs Usage access (`data/UsageAccess.kt`); without it there are no prompts, and `FirewallRepository.allowWhileOpen` falls back to a 30-minute timed allowance so nothing stays allowed forever.
  - `vpn/PacketParser.kt`: IPv4/IPv6 header parsing (pure, unit-tested).
- `data/`: Room DB `smart_network_guard.db` (version 2, `fallbackToDestructiveMigration`; bumping the version **wipes user rules**). It has three entities: `AppRule` (keyed by package name), `ScheduleRule` (days as a CSV of `MON..SUN`, targets as a CSV or `"ALL"`, end minute inclusive), and `BlockLog`. `AppPreferences` wraps DataStore settings.
- `data/repository/`: `FirewallRepository` (rule CRUD, profiles, install sync, JSON export/import) and `DataUsageRepository` (`NetworkStatsManager` usage, which needs the usage-access permission, plus live speed).
- The block log (`block_logs`) must stay small: writes go through the pure `BlockLogThrottle` (front app once per destination per 10 s, background apps once per app per minute) and the service trims it to the newest 2000 rows every minute (`BlockLogDao.keepNewest`). Its flows are only collected while the Logs tab is open. Room re-runs every observed query on each insert, so a big or always-observed log makes the whole phone feel slow.
- `receiver/`: boot auto-start, notification actions (allow 10 min and so on), package install/remove/replace sync, and the alarm receiver.
- `service/ScheduleWorker.kt`: 15-minute periodic job that reloads the firewall and sends data-limit notifications.
- `ui/`: a single `MainViewModel` with `MainScreen`, which has 4 tabs (Firewall, Monitoring, Schedules, Settings/Logs), plus dialogs in `ui/components/`.
  - The list loads in stages so the screen is usable immediately: app names first (`isLoadingApps` drives a "Scanning installed apps…" state, and a "No apps found" help state if the phone hides the app list), then usage numbers, then icons in batches, while rules for new apps are created separately. `syncInstalledApps` reads the known packages once instead of querying per app.
  - The app list (`MainViewModel.appUsages`) is an in-memory join (`buildAppUsageList`) of three separately updated sources: installed apps with icons already converted to bitmaps in the background (reloaded only when the set of packages changes or on pull-to-refresh), per-UID usage (refreshed on resume and on refresh), and rules from Room. Don't go back to re-querying PackageManager on every rule change; that was the cause of UI lag.
  - Wi-Fi/mobile toggles, pin and reset show up at once through `PendingEdit` overrides, which are cleared field by field once Room confirms the change.
  - On Android 13+ `MainScreen` asks for `POST_NOTIFICATIONS` when the user turns the firewall on (then continues to the VPN permission whatever the answer, since the firewall doesn't depend on it) and shows a one-time message with a link to notification settings if it was declined.
  - Live speed (`speedMetrics`) is only sampled while the Monitoring tab collects it. Don't collect it at the top of `MainScreen`.

## Conventions

- After changing anything that affects blocking (rules, schedules, preferences), call `FirewallVpnService.reload(context)`.
- Robolectric Compose tests hang ("Compose did not get idle") when a text field sits inside a dialog window, whatever the graphics mode or clock settings. Test dialogs through their parts instead (see `DialogPartsTest`: `limitsToSave`, `TimeField`, `TimePickerDialog`). Robolectric can also register fake installed apps (`shadowOf(packageManager).installPackage`), see `FirewallRepositoryTest`.
- Keep pure logic (parsing, time calculations, rule evaluation) out of Android classes so it can be unit-tested, as `PacketParser` and `FirewallTimers` are; tests go in `app/src/test/java/dev/zohaib/networkfirewall/`.
- New apps start with **nothing blocked** (`syncInstalledApps`/`onPackageAdded`); only the user's own choices, profiles and schedules block anything. The owner asked for this after the old "block mobile data for every user app" default surprised them. Don't reintroduce a default block.
- Several dependencies (Firebase AI/AppCheck, Retrofit, OkHttp, Moshi) come from the template and are unused. They add the INTERNET permission to the merged manifest despite the "zero internet" comment in `AndroidManifest.xml`.

## Known issues (not yet fixed)

- `MainViewModel.exportRules()` returns before its coroutine finishes, so Export always shows an empty rule list.
- Weekly/monthly data limits are stored but never enforced. The daily limit only sends notifications.
- The in-app on-demand bottom sheet (`OnDemandAccessBottomSheet`, `onDemandEvent`) is now unused: prompts are notifications and nothing emits `blockedEventsFlow` any more.
- Profiles only add blocks: `NORMAL` doesn't unblock anything, and switching profiles doesn't undo the previous one.
- Without usage access, per-app usage figures are estimates, and the 7-day chart shows made-up data when real data is zero.
