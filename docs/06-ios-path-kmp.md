# 06 — iOS Path with Kotlin Multiplatform

## The key point

iPhones can't run APKs. Kotlin reaches iOS through **Kotlin Multiplatform (KMP)**, which compiles Kotlin into a native iOS framework that Xcode links into an `.ipa`.

**Hard requirement:** building, signing, and running on an iPhone or iOS simulator needs **macOS + Xcode**. The Linux VM and the Windows host can write the shared code and run its JVM tests, but they can't produce an iOS build. Mac options: a Mac you own, a cloud Mac (e.g. MacinCloud), or CI macOS runners (e.g. GitHub Actions `macos-latest`).

## Why this architecture pays off here

The `:core` module is already free of Android and Java I/O, so it moves to `commonMain` without edits:

| Code | Shareable to iOS? |
|------|-------------------|
| `core/fp`, `core/model`, `core/tasks` (pure functions) | ✅ As-is |
| `TaskJson.kt` (kotlinx.serialization) | ✅ It's multiplatform |
| `fileTaskStore` (uses `java.io.File`) | ⚠️ Rewrite with `kotlinx-io` / Okio, or use `expect`/`actual` |
| ViewModel | ✅ `androidx.lifecycle` ViewModel is multiplatform |
| Compose UI | ✅ via Compose Multiplatform, or ❌ if you choose SwiftUI |
| `MainActivity`, Workers, permissions | ❌ Android-only shell |

## Two UI strategies for iOS

| | **A. Compose Multiplatform** | **B. Shared logic + SwiftUI** |
|---|---|---|
| UI code shared | ~All | None (rewrite UI in Swift) |
| Look and feel | Material by default. Can be styled to feel native | Fully native iOS |
| Languages to learn | Kotlin only | Kotlin + Swift/SwiftUI |
| Swift interop friction | Low | Medium. Flows and sealed classes need bridging (SKIE or KMP-NativeCoroutines help) |
| Best for | Solo learner, one codebase | Learning both platforms properly, App-Store polish |

**Recommendation for this project:** start with **B-lite**. Share `:core` only, and write a small SwiftUI screen that calls `reduce`. That teaches how KMP interop works. Move to A if you'd rather not maintain two UIs.

## Gradle changes (Phase 2)

Note: with AGP 9, the old `androidTarget {}` block in the KMP plugin is an error. Use Google's **Android-KMP library plugin** for the Android target in shared modules.

```kotlin
// core/build.gradle.kts (Phase 2 shape)
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)   // com.android.kotlin.multiplatform.library
}

kotlin {
    android {
        namespace = "io.github.grenas405.tasks.core"
        compileSdk = 37
        minSdk = 26
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "TasksCore" }
    }
    jvm()   // keeps the fast JVM test loop in the headless VM

    sourceSets {
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
```

## Calling the pure core from Swift (strategy B)

```swift
import SwiftUI
import TasksCore

@MainActor
final class TasksModel: ObservableObject {
    @Published private(set) var state = TasksState(tasks: [], draft: "", error: nil, nextId: 1, loading: false)

    func dispatch(_ action: TasksAction) {
        let next = ReduceKt.reduce(state: state, action: action)  // same Kotlin function as Android
        state = next.state
        // run next.effects here (save to file, etc.)
    }
}
```

> The generated Swift names (`ReduceKt.reduce`, constructors for default arguments) are verbose by default. Tools like SKIE improve sealed classes, enums, and Flow interop.

## Expect/actual: the escape hatch for platform APIs

Reach for it only when a pure parameter won't do:

```kotlin
// commonMain
expect fun currentEpochMillis(): Long

// androidMain / jvmMain
actual fun currentEpochMillis(): Long = System.currentTimeMillis()

// iosMain
actual fun currentEpochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
```

Prefer passing the value in (`fun markDone(task, atMillis: Long)`) and calling the platform clock in the shell. That keeps the core pure *and* avoids `expect`/`actual` altogether.

## Distribution reality check

| | Android | iOS |
|-|---------|-----|
| Install your own build | `adb install`, free | Xcode → device with a free Apple ID (expires after 7 days) |
| Share with testers | Send the APK (verification rules apply from 2026–27) or Play internal testing | TestFlight, needs the $99/yr Apple Developer Program |
| Store | Play Console ($25 one-time) | App Store ($99/yr) |
