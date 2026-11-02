# app-v1

A learning project for building mobile apps in Kotlin: Android first (Jetpack Compose), with iOS added later through Kotlin Multiplatform.

The design follows **functional core, imperative shell**. App logic is written as small, explicit, composable pure functions, and a thin outer layer (Activity, ViewModel, storage adapters) does the side effects.

## Status

Planning phase. App code hasn't been scaffolded yet.

## Docs

Start at [`docs/README.md`](docs/README.md):

1. [Toolchain & workflow](docs/01-toolchain-and-workflow.md): headless Linux VM + Windows host with Android Studio
2. [Architecture: functional core](docs/02-architecture-functional-core.md)
3. [Project structure](docs/03-project-structure.md)
4. [UI approach](docs/04-ui-approach.md)
5. [Trade-offs & limits](docs/05-tradeoffs-and-limits.md)
6. [iOS path (KMP)](docs/06-ios-path-kmp.md)
7. [Android landscape, Nov 2026](docs/07-android-landscape-2026.md)
8. [Roadmap](docs/08-roadmap.md)

## Planned stack

Kotlin 2.4 · AGP 9.x (built-in Kotlin) · Jetpack Compose + Material 3 · Navigation 3 · kotlinx.serialization · compileSdk/targetSdk 37 · minSdk 26
