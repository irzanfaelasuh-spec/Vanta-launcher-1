# VANTA Launcher 0.1.0 — Build report

**Report date:** 3 November 2026  
**Delivery type:** Android source project ZIP (foundation phase)  
**APK status:** Not built in this environment

## 1. Implemented in this delivery

- Native Kotlin + Jetpack Compose Android application shell.
- Landscape-first graphite UI with VANTA branding, vector app icon, responsive dashboard panels, animated page transitions, and a first-run `RUN CODE />` initialization screen.
- First profile state is `No account` with a muted grey pixel avatar.
- Local profile create/sign-in flow using username and password.
- Local SQLite persistence in app-private storage. Passwords are PBKDF2-HMAC-SHA256 hashes with random per-account salts (160,000 iterations); plaintext passwords are not stored.
- New local profiles start with the built-in Steve-style avatar state. A custom PNG skin texture can be imported and a profile can reset to the default Steve state.
- Control layout editor with add/edit/delete, key/action label, normalized position, button size, list reordering, reset, import and export.
- Versioned VANTA JSON schema and sample file in `examples/controls/vanta-touch.json`.
- File-size, action-name, position, control-count and duplicate-ID validation for imported layouts.
- Private launcher directory initialization and a real local setup check report.
- GitHub Actions workflow for unit tests and debug APK artifact generation.
- Project documentation for research, limitations, engine integration and the original product brief.

## 2. Important incomplete work

This archive is **not yet a functioning Minecraft Java launcher**. The following have not been integrated or tested:

- Java runtime/JRE packages for Android ARM64.
- Native Pojav/Zalith engine modules, LWJGL/GLFW, SDL and renderer libraries.
- Minecraft version metadata, asset/library download and installation.
- Actual Minecraft game process launch and background service lifecycle.
- Runtime bridge from the VANTA control JSON editor to live key/mouse events in Minecraft.
- Microsoft/Xbox/Minecraft Services authentication and official skin application.
- 3D skin model preview; PNG import currently stores the skin texture for a local preview only.
- In-game FPS overlay, renderer settings, mods, worlds, resource packs, shaders, crash logs and game overlay.

The `LAUNCH` button explicitly reports that the engine is not connected. It does not show fake progress or claim that Minecraft has launched.

## 3. Files and architecture

Main files:

- `app/src/main/java/com/vanta/launcher/MainActivity.kt` — Android entry point and local initialization.
- `.../data/LauncherDatabase.kt` — local SQLite account table and password hashing.
- `.../data/SkinStore.kt` — PNG validation and private skin storage.
- `.../data/ControlLayoutStore.kt` — versioned control JSON model, validation, local persistence and import/export.
- `.../ui/VantaApp.kt` — first-run experience, dashboard, accounts, skin import, control editor and settings UI.
- `.../ui/theme/VantaTheme.kt` — graphite visual system.
- `.github/workflows/android.yml` — CI test and debug APK artifact workflow.
- `docs/ENGINE_RESEARCH.md` and `docs/ENGINE_INTEGRATION.md` — source-based engine research and integration requirements.

## 4. Dependencies

- Android Gradle Plugin 8.7.3.
- Kotlin 2.0.21.
- Jetpack Compose using Compose BOM 2024.09.03.
- AndroidX Activity Compose, Core KTX, Lifecycle KTX.
- Material 3 and Material Icons Extended.
- JUnit 4.13.2 for unit tests.

There is no Firebase dependency or Firebase configuration.

## 5. Checks performed in this environment

- Reviewed Kotlin source structure and checked balanced braces, parentheses and brackets across all Kotlin files.
- Ran the installed Kotlin compiler parser pass. It reported unresolved Android/AndroidX/JUnit symbols because the Android SDK and dependency classpath are absent; no Kotlin parser diagnostics (`expecting`, `Unexpected tokens`, or syntax-error diagnostics) were found.
- Inspected the Gradle project configuration and GitHub Actions workflow.
- Confirmed the project requests compile SDK 35, minimum SDK 26, Java 17, AGP 8.7.3 and Gradle 8.9.
- Reviewed the control JSON example and source layout.

## 6. Checks that could not be performed

A real Android Gradle build and APK installation test could not be run: this execution environment has no Android SDK (`ANDROID_HOME`/`ANDROID_SDK_ROOT` are unset), no system Gradle executable, and no preinstalled Gradle distribution. Network access from the shell was unavailable, so these tools and the native engine source could not be fetched here.

Therefore this delivery is **not certified as compiling or installable**. The included GitHub Actions workflow is the next build-validation path once the source is pushed to a repository with Actions enabled.

## 7. Build instructions

Requirements: Android Studio with Android SDK Platform 35 and Build Tools 35.0.0, JDK 17, Gradle 8.9.

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

Debug APK output, after a successful build:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Pushing this directory to GitHub triggers `.github/workflows/android.yml`, which builds the debug APK and publishes it as a workflow artifact.

## 8. Known design/API decisions

- VANTA local accounts are local profile records only. They are not Mojang/Microsoft authenticated accounts and do not grant multiplayer access.
- VANTA control JSON is a portable VANTA schema, not a drop-in Zalith 2 or older Pojav/Amethyst control file. A safe engine-specific converter and input adapter are required before third-party control imports can drive Minecraft.
- The skin texture picker accepts standard Java PNG dimensions, but applying a skin to a Microsoft account or to an offline game process is not wired yet.
- Android may terminate a game process under memory pressure; even a future foreground service cannot guarantee unlimited background persistence.
