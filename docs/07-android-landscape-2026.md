# 07 — Android Development Landscape (researched 1 Nov 2026)

Snapshot of web research. Version numbers move fast, so before pinning anything **check the linked official pages**. Items sourced only from third-party write-ups are marked *(secondary)*.

## Platform

- **Android 17 (API 37)** was released **16 Jun 2026** for supported Pixels. Behavior changes for apps targeting 37:
  - No opt-out from orientation and resizability on large screens (sw ≥ 600dp).
  - Apps can't modify `static final` fields via reflection.
- **Android 16 (API 36)** is the current Google Play floor (see Distribution).

## UI: Compose-first

- **May 2026:** Google declared Android UI development **"Compose-first"**. The View components that Compose replaces are in maintenance mode, and the Layout Editor and Navigation Editor won't get new features.
- **Compose 1.11 (Apr 2026):** a `Grid` 2D layout API, trackpad events, and shared-element debug tools.
- **Compose 1.12 (Aug 2026, stable):** `MeshGradientPainter`, wide color gamut (P3) and HDR rendering. **Requires compileSdk 37 and AGP ≥ 9.1.1.**
- **Compose 1.13 (announced at Android Dev Summit, Oct 2026):** recomposition-cause tracing, `Grid` and `FlexBox` nearing stable, progressive blur, formatted text *(secondary)*.
- **Navigation 3:** 1.0 stable Nov 2025, **1.2 stable** with deep links and richer predictive back. The runtime is multiplatform (Android, iOS, desktop, web). Compose Multiplatform 1.10+ supports Nav3 on all targets. Non-JVM targets need routes registered with `SavedStateConfiguration` because there's no reflection.

## Kotlin & build

- **Kotlin 2.4.0 (Jun 2026):** context parameters, explicit backing fields, and stable `kotlin.uuid.Uuid` in common stdlib; emits Java 26 bytecode; compatible with Gradle 9.5.
- **AGP 9.x:** **Kotlin is built into AGP.** Don't apply `org.jetbrains.kotlin.android`, because Kotlin 2.3+ raises a configuration error with AGP 9 if you do. The `android.builtInKotlin=false` opt-out goes away in AGP 10.
  - KMP: the `androidTarget {}` block errors with AGP 9+. Use Google's Android-KMP library plugin.
  - Kotlin 2.4 needs AGP ≥ 9.1.0 per the compatibility table *(secondary)*.
- **AGP 9.3 (Oct 2026):** an `analyzeReleaseR8Config` task scores your R8 config. Jetpack Keep (alpha) adds type-safe keep annotations *(secondary)*.

## Tooling and AI agents (Android Dev Summit, 28–29 Oct 2026)

*(secondary, single write-up)*
- **Android Studio Rabbit 2** hosts external coding agents via the Agent Client Protocol (Claude, Codex, and others), and adds agentic **Change Review**.
- **Android CLI:** project init, Compose Preview rendering *without an emulator or IDE*, profiling, and remote physical devices. Official plugins exist for Claude Code and Codex, plus 30+ skills (github.com/android/skills). **Relevant to the headless-VM setup:** previews and device access from the terminal.
- **Migration Assistant (Canary):** right-click an XML layout → *Migrate to Compose*.
- **Jetpack AppState 1.0.0-alpha01:** Compose's state model for ViewModels and repositories (`transform`, `statestore`).
- **On-device AI:** Gemini Nano 4 (function calling), ML Kit GenAI Agent API (alpha, minSdk 26, Kotlin 2.3+), AppFunctions (expose app functions to agents via Android MCP, Android 16+).
- **Jetpack XR core stable.** Compose for XR is still alpha.

## Distribution & policy

- **Play target API:** since **31 Aug 2026**, new apps and updates must target **API 36**. Existing apps must target ≥ 35 to stay visible to new users on newer devices. A Play Billing Library 8+ requirement reportedly shares that date *(secondary)*.
- **16 KB memory pages:** apps targeting API 35+ must support 16 KB pages on 64-bit devices for Play. Pure Kotlin apps are unaffected. Native (`.so`) libraries need NDK r28+ or the right linker flags.
- **Developer verification:**
  - Rolling out to all developers via Play Console and the new Android Developer Console (Mar 2026).
  - Enforcement began **30 Sep 2026** in Brazil, Indonesia, Singapore, and Thailand, with global expansion in 2027.
  - **ADB installs are exempt.**
  - The "advanced flow" for sideloading unverified apps involves Developer Options, authentication, a restart, and a 24-hour wait. It began rolling out in Aug 2026.
- **Large screens:** Play will scrutinize foldable quality more closely in 2027 *(secondary)*.

## What this means for this project

| Finding | Decision |
|---------|----------|
| Compose-first, Views in maintenance | Compose only |
| Compose 1.12 needs compileSdk 37 / AGP 9.1.1 | `compileSdk = 37`, AGP ≥ 9.1.1 |
| AGP 9 built-in Kotlin | Don't apply `kotlin-android` |
| Android 17 large-screen rules | `targetSdk 37`, design adaptive from day one |
| Nav3 stable + multiplatform | Use Nav3 for screen #2 and beyond |
| ADB exempt from verification | Dev loop unaffected |
| Android CLI renders previews headlessly | Try it in the VM, which could make the VM self-sufficient for UI checks |

## Sources

- [Android 17 is here (Android Developers Blog)](https://android-developers.googleblog.com/2026/06/Android-17.html) · [alt](https://developer.android.com/blog/posts/android-17-is-here)
- [Android UI development is Compose First](https://android-developers.googleblog.com/2026/05/android-ui-development-is-compose-first.html)
- [Compose April '26 release](https://android-developers.googleblog.com/2026/04/jetpack-compose-april-2026-updates.html)
- [Compose August '26 release](https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html)
- [17 things to know for Android developers at Google I/O](https://developer.android.com/blog/posts/17-things-to-know-for-android-developers-at-google-i-o)
- [Android Dev Summit 2026 recap (DEV Community, secondary)](https://dev.to/diego_f_caetano/android-dev-summit-2026-google-brings-developers-back-together-after-four-years-and-puts-agents-at-3jjo)
- [Navigation 3 releases](https://developer.android.com/jetpack/androidx/releases/navigation3) · [Navigation 3 in Compose Multiplatform](https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html) · [Compose Multiplatform 1.10.0](https://blog.jetbrains.com/en/kotlin/2026/01/compose-multiplatform-1-10-0)
- [AGP 9.0 release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes) · [Update your Kotlin projects for AGP 9 (JetBrains)](https://blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/)
- [What's new in Kotlin 2.3.0](https://kotlinlang.org/docs/whatsnew23.html) · [Kotlin 2.3 compatibility guide](https://kotlinlang.org/docs/compatibility-guide-23.html)
- [Kotlin 2.4.0 released](https://blog.jetbrains.com/kotlin/2026/06/kotlin-2-4-0-released/) · [What's new in Kotlin 2.4.0](https://kotlinlang.org/docs/whatsnew24.html)
- [Play target API requirements](https://developer.android.com/google/play/requirements/target-sdk) · [Play Console help](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Support 16 KB page sizes](https://developer.android.com/guide/practices/page-sizes)
- [Android developer verification rolling out (Mar 2026)](https://android-developers.googleblog.com/2026/03/android-developer-verification-rolling-out-to-all-developers.html) · [Android Authority: first wave](https://www.androidauthority.com/android-sideloading-developer-verification-first-wave-rollout-3717921/) · [9to5Google: advanced flow](https://9to5google.com/2026/08/18/google-gradually-rolling-out-androids-advanced-sideloading-ahead-of-developer-verification/)
