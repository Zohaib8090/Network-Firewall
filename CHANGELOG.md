# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Fixed
- Blocked connection attempts are now logged under the app that actually made them, instead of whichever blocked app came first in the list. The log records each app and destination at most once every 10 seconds, instead of one entry per packet.
- Blocked apps can no longer reach the internet over IPv6.
- Changing a rule while the firewall is switched off no longer turns it on.
- Fixed a possible loop where the firewall kept rebuilding itself right after starting.
- Pauses, temporary allowances and schedules now take effect on time, instead of up to 15 minutes late.
- Toggling Wi-Fi or mobile access, pinning or resetting an app now updates the screen immediately, instead of after the whole app list reloads.
- Reduced stutter when scrolling the app list; app icons are now prepared in the background.

- The GitHub Actions build, which had failed on every run because the gitignored `debug.keystore` was missing. Builds now fall back to the standard debug key, use a fixed Gradle version (9.3.1) and JDK 21 (required by the Robolectric tests), and run the unit tests.
- A unit test that still expected the app's old name.

- On Android 13 and newer the app now asks for notification permission when you turn the firewall on. Before, it never asked, so blocked-app alerts, the "Allow 10 min" button and data-limit warnings could never appear. If you decline, a message explains that alerts are off and links to the notification settings; the firewall still starts.
- The app no longer keeps showing the firewall as active after it has stopped. If another VPN app takes over, the VPN permission is removed, or the firewall can't start, it now switches itself off, posts a "Firewall stopped" notification, and shows a "Turn on" message in the app.
- The firewall no longer builds a tunnel when none of the blocked apps can be found, which would have cut off every app.
- The data-limit dialog no longer claims traffic is stopped automatically; it now says what alerts need and that monthly alerts aren't active yet. Saving it also no longer erases a stored weekly limit.
- Schedule start and end times are picked with a time picker instead of being typed into boxes that rewrote themselves on every keystroke.

- Nothing is blocked by default any more. Every non-system app used to start with mobile data blocked (new apps, first launch, "Sync Apps" and "Reset all rules" all did this), which surprised users. Existing choices are never touched when syncing.
- The app list now appears right away on first launch. It used to wait until every app's name and icon had loaded (while showing "No apps found"); now names appear first, with a "Scanning installed apps…" state, and icons fill in afterwards. If the phone hides its app list from the app, it says so and offers "App settings" and "Sync again". Syncing installed apps is also much faster (one database read instead of one per app).
- Global Lock no longer floods you with "tried to use the internet" notifications. You are now asked only when you open a blocked app, and only for that app, with "Allow while open", "Allow 10 min" or "Keep blocked". This needs Usage access, and the app offers to open its settings.
- "Allow 10 min" and "Allow while open" now actually work during Global Lock and scheduled blocks (they were ignored there).
- "Allow while open" (formerly "Allow for this session") now ends by itself shortly after the app is closed. Before, it never ended.

### Changed
- The app's package name is now `dev.zohaib.networkfirewall`, replacing the AI Studio template's `com.aistudio.networkfirewall.app` (application ID) and `com.example` (code package). Android sees this as a different app, so uninstall the old build before installing this one; settings and rules don't carry over.
- The app is now called Network-Firewall everywhere it is shown (top bar, notification titles, the VPN name in Android settings, README and docs), matching the repository and the launcher label. It was shown as "Smart Network Guard" in places.
- New app icon: a shield with Wi-Fi waves on the app's emerald green, including a monochrome version for Android 13+ themed icons.
- Live network speed is only measured while the Monitoring tab is open.

### Added
- README, license (MIT), contributing guide, security policy, changelog, and GitHub issue/pull request templates.
- Unit tests for packet parsing, timers, the app list, the time picker parts, the firewall's revoke handling, what gets blocked, which app is in front, when "allow while open" ends, and the no-default-blocking rule.

## [1.0] - 2026-09-20

### Added
- First version: per-app Wi-Fi and mobile blocking, schedules, profiles, global lock, temporary access, usage monitoring, data-limit warnings, block log, JSON backup, and pinning apps to the top of the list.
