# Changes

## 2026-11-05

### Added
- Hello World Compose app, meant to be opened and run from Android Studio on the host: `:app` shell (`MainActivity`, `HelloScreen` with a Preview, `AppTheme`) showing text from a pure `greeting()` function in `:core`, plus `GreetingTest`.
- Gradle build with version catalog and wrapper (Gradle 9.5.1, AGP 9.1.1, Kotlin 2.4.21, Compose BOM 2026.09.00).
- `.gitattributes` pinning line endings for the wrapper scripts, since the repo moves between Linux and Windows.

### Changed
- Build files don't use `jvmToolchain(17)`, unlike the original sketch in `docs/03-project-structure.md`. They target Java 17 bytecode directly, so the build runs on Android Studio's bundled JDK without needing a separate JDK 17.
- README status and run instructions.

## 2026-11-01

### Added
- Planning docs in `docs/`: toolchain and workflow (Linux VM + Windows/PowerShell host), functional-core architecture with Kotlin examples, project structure and Gradle layout, Compose UI approach, trade-offs where the pure-function style meets Android's limits, the KMP/iOS path, a researched Nov 2026 Android landscape with sources, and a milestone roadmap.
- `README.md` with project overview and docs index.
- `.gitignore` for Android/Gradle projects.
