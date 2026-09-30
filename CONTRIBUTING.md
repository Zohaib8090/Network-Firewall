# Contributing

Thanks for helping improve Network-Firewall!

## Getting set up

Follow the build steps in the [README](README.md#building). Before opening a pull request, make sure these pass:

```sh
gradle :app:compileDebugKotlin
gradle :app:testDebugUnitTest
```

## Guidelines

- **Keep pull requests focused**: one fix or feature per pull request.
- **Match the existing style**: Kotlin, Jetpack Compose, 4-space indentation, and the naming used in nearby code.
- **Test the logic**: put parsing, time calculations and rule decisions in plain Kotlin classes (like `vpn/PacketParser.kt` and `vpn/FirewallTimers.kt`) and add tests in `app/src/test/java/com/example/`.
- **Re-apply rules after changes**: after changing anything that affects blocking (rules, schedules, settings), call `FirewallVpnService.reload(context)`.
- **Be careful with the database**: the Room database uses destructive migration, so bumping its version **deletes users' rules**. If you change an entity, add a proper migration.
- **Keep the UI smooth**: don't do PackageManager queries, icon loading or disk I/O on the main thread, and don't collect fast-changing state (like live speed) at the top of `MainScreen`.
- **Test on a device** if you touch the VPN service, and say in the pull request which Android version you used.

## Commit messages

Use a short prefix describing the type of change, as in the existing history: `feat:`, `fix:`, `perf:`, `docs:`, `test:`, `chore:`.

## Reporting bugs and ideas

Open an issue using the bug report or feature request template. For bugs, include your Android version, device, and steps to reproduce.

For anything else, email [zohaibbaig144@gmail.com](mailto:zohaibbaig144@gmail.com).
