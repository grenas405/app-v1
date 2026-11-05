# app-v1

A learning project for building mobile apps in Kotlin: Android first (Jetpack Compose), with iOS added later through Kotlin Multiplatform.

The design follows **functional core, imperative shell**. App logic is written as small, explicit, composable pure functions, and a thin outer layer (Activity, ViewModel, storage adapters) does the side effects.

## Status

Hello World. A Compose screen shows a greeting produced by a pure function in `:core`. The task-list features in the docs aren't built yet.

## Run it

On the Windows host, in Android Studio:

1. `git pull`, then **File → Open** and select the repo folder. Let the Gradle sync finish.
2. If Studio asks, install **Android SDK Platform 37** through its SDK Manager prompt.
3. Pick an emulator or connected phone and press **Run ▶**. To see the screen without a device, open `HelloScreen.kt` and use its Preview.

The core tests need only a JDK, not the Android SDK, so they also run on the Linux VM:

```sh
./gradlew :core:test
```

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
