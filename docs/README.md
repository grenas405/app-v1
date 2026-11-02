# Planning Docs

These docs plan a learning project: an Android app in Kotlin first, with an iOS path added later through Kotlin Multiplatform (KMP).

The running example is a small **Tasks** app (add, toggle, and remove to-dos, saved to disk). It's small enough to finish, and it still touches every layer: UI, state, validation, persistence, and lifecycle.

## Guiding principle

> **Functional core, imperative shell.**
> Most of the code is small, explicit, pure functions (data in, data out, no I/O). A thin outer layer, made of the Activity, ViewModel, and file and network adapters, does the side effects and calls into the pure core.

## Reading order

| # | Doc | What it covers |
|---|-----|----------------|
| 01 | [Toolchain & workflow](01-toolchain-and-workflow.md) | Headless Linux VM + Windows host (PowerShell, Android Studio), building, installing APKs |
| 02 | [Architecture: functional core](02-architecture-functional-core.md) | Pure functions, composition, reducer, effects-as-data, code examples |
| 03 | [Project structure](03-project-structure.md) | File trees, Gradle setup, package layout, now and after KMP |
| 04 | [UI approach](04-ui-approach.md) | Compose options compared, stateless composables, navigation, theming, adaptive layouts |
| 05 | [Trade-offs & limits](05-tradeoffs-and-limits.md) | Where "pure and composable" runs into Android's limits, and what to do about it |
| 06 | [iOS path (KMP)](06-ios-path-kmp.md) | Sharing the pure core with iOS, and what needs a Mac |
| 07 | [Android landscape, Nov 2026](07-android-landscape-2026.md) | Research notes: current versions, policies, and news, with sources |
| 08 | [Roadmap](08-roadmap.md) | Milestones and what each one teaches |

## Status

Planning only. No app code has been scaffolded yet. See [Roadmap](08-roadmap.md) for the first milestone.
