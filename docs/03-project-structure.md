# 03 — Project Structure

## Phase 1: Android only, two modules

Two Gradle modules enforce the core/shell split at compile time. `:core` is a **pure Kotlin/JVM module**, so it physically can't import Android APIs. Its tests run anywhere, including the headless VM.

```
app-v1/
├── settings.gradle.kts
├── build.gradle.kts                  # root: plugin versions only (apply false)
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml            # version catalog — single source of versions
│   └── wrapper/                      # gradlew pins the Gradle version
├── gradlew  gradlew.bat
│
├── core/                             # ★ functional core — pure Kotlin, no Android
│   ├── build.gradle.kts              # plugin: kotlin("jvm")
│   └── src/
│       ├── main/kotlin/io/github/grenas405/tasks/core/
│       │   ├── fp/
│       │   │   ├── Compose.kt        # andThen
│       │   │   └── Validation.kt     # Valid / Invalid / flatMap
│       │   ├── model/
│       │   │   ├── Task.kt
│       │   │   ├── TaskList.kt       # addTask, toggleTask, removeTask, ...
│       │   │   └── TitleRules.kt     # normalizeTitle, validateTitle
│       │   └── tasks/
│       │       ├── TasksState.kt     # State, Action, Effect, Next
│       │       └── Reduce.kt         # reduce(state, action): Next
│       └── test/kotlin/io/github/grenas405/tasks/core/
│           ├── model/TitleRulesTest.kt
│           └── tasks/ReduceTest.kt
│
├── app/                              # imperative shell + UI
│   ├── build.gradle.kts              # plugin: com.android.application
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/io/github/grenas405/tasks/
│       │   │   ├── MainActivity.kt   # composition root
│       │   │   ├── data/
│       │   │   │   ├── TaskJson.kt   # DTO + encode/decode (pure)
│       │   │   │   └── TaskStore.kt  # fileTaskStore, inMemoryTaskStore
│       │   │   └── ui/
│       │   │       ├── theme/        # Color.kt, Type.kt, Theme.kt
│       │   │       ├── components/   # small reusable stateless composables
│       │   │       └── tasks/
│       │   │           ├── TasksRoute.kt      # ViewModel → state (stateful edge)
│       │   │           ├── TasksScreen.kt     # stateless screen
│       │   │           ├── TaskRow.kt
│       │   │           ├── DraftField.kt
│       │   │           └── TasksViewModel.kt
│       │   └── res/                  # icons, strings.xml, themes.xml (splash only)
│       ├── test/                     # JVM tests for data/ (encode/decode round-trip)
│       └── androidTest/              # Compose UI tests (need emulator/device)
│
├── docs/                             # these planning docs
├── README.md
└── CHANGES.md
```

> **Package-by-feature** (`ui/tasks/`, `core/tasks/`): when you add a second screen, it gets its own folder. Avoid package-by-type layouts like `viewmodels/` and `screens/`, because they scatter a single feature across the tree.

## Gradle files (shape, not exact versions)

> Create the project with Android Studio's **New Project → Empty Activity** wizard and keep the versions it generates. They'll be newer than anything written here. The snippets below show the *shape* to aim for.

### `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "app-v1"
include(":app", ":core")
```

### `gradle/libs.versions.toml`

```toml
[versions]
agp = "9.1.1"            # AGP 9.x: Kotlin is built in; Compose 1.12 needs >= 9.1.1
kotlin = "2.4.0"         # used for the compose + serialization compiler plugins and :core
composeBom = "…"         # take from the Studio template / Compose BOM page
lifecycle = "…"
activityCompose = "…"
serialization = "…"
junit = "4.13.2"

[libraries]
compose-bom              = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-material3        = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling       = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
activity-compose         = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose   = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
kotlinx-serialization-json  = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }
junit                    = { group = "junit", name = "junit", version.ref = "junit" }

[plugins]
android-application  = { id = "com.android.application", version.ref = "agp" }
kotlin-jvm           = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-compose       = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

### `core/build.gradle.kts`: pure JVM

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin { jvmToolchain(17) }

dependencies {
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}
```

### `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    // No org.jetbrains.kotlin.android here: AGP 9 has Kotlin built in,
    // and Kotlin 2.3+ errors if you apply both.
    alias(libs.plugins.kotlin.compose)        // Compose compiler
    alias(libs.plugins.kotlin.serialization)  // for TaskDto
}

android {
    namespace = "io.github.grenas405.tasks"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.grenas405.tasks"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures { compose = true }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
```

**SDK level choices**

| Setting | Value | Why |
|---------|-------|-----|
| `minSdk` | 26 (Android 8) | Covers nearly all active devices, and many current Jetpack/ML Kit APIs require it |
| `compileSdk` | 37 (Android 17) | Compose 1.12 compiles against 37 |
| `targetSdk` | 37 | New project, so take on Android 17 behavior now (large-screen resizability can't be opted out). Play's minimum since 31 Aug 2026 is 36 |

## Phase 2: after adding iOS (KMP)

`:core` becomes a Kotlin Multiplatform module, and its pure code moves to `commonMain` **unchanged**. That is the payoff of keeping it free of `android.*` imports.

```
app-v1/
├── core/                              # was kotlin("jvm") → now kotlin("multiplatform")
│   └── src/
│       ├── commonMain/kotlin/…        # fp/, model/, tasks/ — moved as-is
│       ├── commonTest/kotlin/…        # tests run on JVM *and* iOS simulator
│       ├── androidMain/kotlin/…       # (rarely needed) expect/actual impls
│       └── iosMain/kotlin/…
├── app/                               # Android shell (unchanged)
└── iosApp/                            # Xcode project (created on a Mac)
    ├── iosApp.xcodeproj
    └── iosApp/
        ├── iOSApp.swift
        └── ContentView.swift          # SwiftUI, or hosts Compose Multiplatform
```

Details are in [06 — iOS path](06-ios-path-kmp.md).

## `.gitignore` essentials

```
.gradle/
build/
local.properties        # machine-specific SDK path — never commit
.idea/
*.iml
.kotlin/
captures/
*.apk
*.aab
*.jks                   # signing keys — never commit
```
