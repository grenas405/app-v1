# 08 — Roadmap

Each milestone is small, ends in something runnable, and teaches one main concept.

| # | Milestone | Done when | Teaches |
|---|-----------|-----------|---------|
| **M0** | Toolchain | Host: Studio installed, template "Hello" app runs on emulator **and** phone via `adb`. VM: `git push`/`pull` works | Gradle, SDK, adb, signing (debug key) |
| **M1** | Pure core | `:core` module with `Task`, list functions, `validateTitle`, `reduce` + tests passing in the VM (`./gradlew :core:test`) | Pure functions, composition, sealed types, JUnit |
| **M2** | Static UI | `TasksScreen` + previews rendering hard-coded `TasksState` | Compose basics, state hoisting, Material 3 |
| **M3** | Wire it up | ViewModel + `dispatch`, in-memory store; adding/toggling works on device | UDF, StateFlow, lifecycle-aware collection |
| **M4** | Persistence | `fileTaskStore` + JSON; data survives app restart; draft survives process death (`SavedStateHandle`) | Effects, coroutines, Dispatchers, process death |
| **M5** | Adaptive + polish | Dark mode, dynamic color, wide-screen layout preview, edge-to-edge, accessibility labels | Window size classes, theming, a11y |
| **M6** | Second screen | Task detail via Navigation 3 with pure `push`/`pop` | Navigation, back stack as data, predictive back |
| **M7** | Release build | Signed release APK with R8 enabled runs correctly | Signing configs, R8, versioning |
| **M8** | Background work | Daily reminder via WorkManager (thin worker → pure `reminderMessage`) + notification permission | Framework-class adapters, permissions-as-effects |
| **M9** | KMP | `:core` converted to multiplatform, JVM tests still pass in VM | KMP source sets, Android-KMP plugin |
| **M10** | iOS (needs Mac) | SwiftUI screen calling shared `reduce` on the iOS simulator | Kotlin/Native framework, Swift interop |

## Definition of done (every milestone)

- [ ] `./gradlew test` passes
- [ ] App runs on a real phone
- [ ] `CHANGES.md` updated, and `README.md` too if setup changed
- [ ] Committed and pushed to `main`

## Open decisions (revisit later)

| Decision | Default now | Revisit at |
|----------|-------------|------------|
| DI framework | Manual composition root | > ~10 dependencies or M8 |
| Storage | JSON file | When querying/sorting large data → Room (KMP-capable) |
| iOS UI | SwiftUI over shared core | M10, compare with Compose Multiplatform |
| App-wide state | ViewModel per screen | When Jetpack AppState reaches beta |
