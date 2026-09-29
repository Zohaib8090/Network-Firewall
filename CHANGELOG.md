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

### Changed
- New app icon: a shield with Wi-Fi waves on the app's emerald green, including a monochrome version for Android 13+ themed icons.
- Live network speed is only measured while the Monitoring tab is open.

### Added
- README, license (MIT), contributing guide, security policy, changelog, and GitHub issue/pull request templates.
- Unit tests for packet parsing, timers and the app list.

## [1.0] - 2026-09-20

### Added
- First version: per-app Wi-Fi and mobile blocking, schedules, profiles, global lock, temporary access, usage monitoring, data-limit warnings, block log, JSON backup, and pinning apps to the top of the list.
