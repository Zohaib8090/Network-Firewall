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

### Changed
- New app icon: a shield with Wi-Fi waves on the app's emerald green, including a monochrome version for Android 13+ themed icons.
- Live network speed is only measured while the Monitoring tab is open.

### Added
- README, license (MIT), contributing guide, security policy, changelog, and GitHub issue/pull request templates.
- Unit tests for packet parsing, timers, the app list, the time picker parts and the firewall's revoke handling.

## [1.0] - 2026-09-20

### Added
- First version: per-app Wi-Fi and mobile blocking, schedules, profiles, global lock, temporary access, usage monitoring, data-limit warnings, block log, JSON backup, and pinning apps to the top of the list.
