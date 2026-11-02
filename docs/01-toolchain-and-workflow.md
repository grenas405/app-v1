# 01 — Toolchain & Workflow

## The two machines

| Machine | Role | Tools |
|---------|------|-------|
| **Linux VM (headless)** | Edit code, run pure unit tests, git, optional CLI builds | git, JDK 17+, *optional* Android SDK command-line tools |
| **Windows host** | Android Studio, emulator, Compose Previews, building and installing APKs on a phone | PowerShell, Android Studio (bundles its own JDK), Android SDK, `adb` |

Git keeps the two machines in sync: push from the VM, pull on the host (or the reverse).

```
 ┌──────────── Linux VM ────────────┐          ┌──────────── Windows host ─────────────┐
 │ edit code · ./gradlew test       │  git     │ Android Studio · emulator · previews  │
 │ (pure core tests need no device) │ ───────▶ │ ./gradlew assembleDebug · adb install │
 └──────────────────────────────────┘ ◀─────── └───────────────────────────────────────┘
                                                              │ USB / Wi-Fi adb
                                                              ▼
                                                        Android phone
```

The [architecture](02-architecture-functional-core.md) is designed to suit this setup. The **pure core is plain Kotlin with no Android imports**, so its tests run with `./gradlew test` in the VM. You don't need an emulator, a device, or a GUI for them.

## Windows host setup (PowerShell)

Run these in an elevated PowerShell:

```powershell
# Android Studio (it ships its own JetBrains Runtime / JDK)
winget install --id Google.AndroidStudio -e

# Optional: a standalone JDK for running Gradle outside Studio
winget install --id Microsoft.OpenJDK.21 -e

# Git, to sync with the VM
winget install --id Git.Git -e
```

Next, open Android Studio once and let the setup wizard install the SDK. Then set environment variables so the CLI tools work outside Studio:

```powershell
[Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:LOCALAPPDATA\Android\Sdk", "User")
$path = [Environment]::GetEnvironmentVariable("Path", "User")
[Environment]::SetEnvironmentVariable("Path", "$path;$env:LOCALAPPDATA\Android\Sdk\platform-tools", "User")
# open a new terminal, then:
adb version
```

In Studio, open **SDK Manager** and install:
- Android SDK Platform **37** (Android 17), the `compileSdk` used here
- Android SDK Platform **36** (Android 16), Play's current minimum target level
- Android SDK Platform-Tools and Build-Tools
- An emulator system image (a recent Pixel, API 37)

## Optional: build in the VM too

```bash
# JDK
sudo apt install openjdk-21-jdk-headless

# Android command-line tools (download "Command line tools only" from developer.android.com/studio)
export ANDROID_HOME="$HOME/Android/Sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"
# unzip the download into $ANDROID_HOME/cmdline-tools/latest
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-37" "build-tools;37.0.0"

./gradlew test             # pure unit tests
./gradlew assembleDebug    # -> app/build/outputs/apk/debug/app-debug.apk
```

> The build-tools version string changes over time. Run `sdkmanager --list` to see what's available.

## Getting the app onto a phone

| Method | Command / steps | Notes |
|--------|-----------------|-------|
| **Studio Run ▶** | Click Run with the phone connected | Easiest. Builds, installs, and launches |
| **USB adb** | `adb install -r app\build\outputs\apk\debug\app-debug.apk` | Needs Developer Options → USB debugging |
| **Wi-Fi adb** | Phone: Developer Options → Wireless debugging → *Pair with code*. Host: `adb pair <ip>:<port>`, then `adb connect <ip>:<port>` | No cable needed |
| **Copy the APK** | Send the file to the phone and tap it | Allow "Install unknown apps" for the file app. See the verification note below |

### Developer verification (2026 change)

Google is rolling out *developer verification* for apps installed on certified Android devices. Enforcement began **30 Sep 2026** in Brazil, Indonesia, Singapore, and Thailand, with global expansion planned for 2027. Reported details:
- **ADB installs are exempt**, so the dev loop above is unaffected.
- Non-ADB installs from unverified developers go through an "advanced flow" in Developer Options: authenticate, restart, then wait 24 hours.
- To hand APKs to friends later, register in the Android Developer Console.

See [07 — landscape](07-android-landscape-2026.md#distribution--policy) for sources.

## Daily loop

1. In the VM, edit pure code and run `./gradlew test`. Commit and push.
2. On the host, `git pull`, then check UI in Compose Previews and run on the emulator or phone.
3. UI tweaks happen wherever is convenient. Studio's live Previews make the host the better place for UI work.
