# VANTA Launcher

**Beyond the Blocks. Beyond the Limits.**

VANTA Launcher is an Android-native Minecraft Java launcher project foundation. The first deliverable focuses on the local profile experience and the custom-control data workflow, using the supplied Zalith Launcher 2 screenshots as visual references while keeping VANTA's own branding.

> **Important status:** this archive is an early Android foundation build, not yet a complete Minecraft Java launcher. It does not include the Pojav/Zalith Java runtime, LWJGL/GLFW native libraries, renderer stack, Minecraft installation pipeline, or the in-game input bridge. The LAUNCH action reports this honestly instead of pretending to start Minecraft. See [`docs/ENGINE_RESEARCH.md`](docs/ENGINE_RESEARCH.md) and [`docs/ENGINE_INTEGRATION.md`](docs/ENGINE_INTEGRATION.md).

## Implemented in this foundation

- Native Android app written in Kotlin and Jetpack Compose; landscape orientation.
- VANTA graphite visual system with a restrained green accent and animated page transitions.
- First-run `RUN CODE />` screen with real local setup checks (private directories, SQLite database, control-layout store, Android version, and ABI).
- `No account` state with a grey pixel-character avatar.
- Local account creation and sign-in with name + password.
- Local account data stored in private SQLite; password stored only as a salted PBKDF2-HMAC-SHA256 hash (160,000 iterations), never plaintext.
- Steve-style default pixel avatar for a new local profile.
- PNG skin import into app-private storage, with dimension checks for 64×64, 64×32, 128×128, or 128×64.
- Control layout manager: add/edit/delete controls, set action, position, size, reorder entries, reset the default layout, import JSON, and export JSON.
- Strict VANTA JSON schema validation, size limit, button count limit, and atomic local save.
- GitHub Actions workflow for unit tests, debug APK build, and APK artifact upload.

## Not implemented yet

- Minecraft Java runtime and native launcher engine integration.
- Mojang version manifest installation and library/assets downloader.
- Microsoft OAuth / Xbox / Minecraft Services authentication.
- In-game skin application or custom-skin injection.
- A control-input bridge that sends the saved VANTA control layout into the Minecraft game process.
- Renderer selection (GL4ES, Vulkan/Zink, etc.), actual FPS overlay, game logs, mods, resource packs, worlds, and in-game overlay.

These are intentionally not represented as working features in this build.

## Build

Requirements:

- Android Studio with Android SDK Platform 35 and Build Tools 35.0.0.
- JDK 17.
- Gradle 8.9.

Open this directory in Android Studio and sync Gradle, or run:

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Alternatively, push the project to a GitHub repository. The included workflow at `.github/workflows/android.yml` builds the debug APK and uploads it as an Actions artifact.

## Local account behavior

- Profiles are local to this Android app installation.
- A newly created profile becomes active immediately and starts with the built-in Steve-style avatar.
- `No account` remains visible until a local profile is created or selected.
- The local password is not a Minecraft/Microsoft password and does not log into Mojang or Microsoft services.
- There is no Firebase dependency and no remote profile database.
- Deleting a profile removes its row from the app's private SQLite database. Skin files already copied into private storage may remain until a future cleanup task; no world data is touched.

## Control JSON

VANTA currently uses its own versioned portable format:

```json
{
  "schema": "vanta.control-layout",
  "version": 1,
  "name": "VANTA Touch",
  "author": "VANTA",
  "buttons": [
    {
      "id": "jump",
      "label": "JUMP",
      "action": "SPACE",
      "x": 0.84,
      "y": 0.68,
      "size": 64
    }
  ]
}
```

Positions use normalized 0.0–1.0 values. `x` and `y` are constrained to the safe editing canvas. Button actions are key/action identifiers such as `KEY_W`, `SPACE`, `SHIFT`, `MOUSE_LEFT`, and `MOUSE_RIGHT`.

**Compatibility note:** this schema is not the native Zalith Launcher 2 `.json` control-layout schema, nor the older Pojav/Amethyst format. Those formats have different structures. Do not rename one format and assume it will work in another launcher. A tested converter/engine adapter is required before VANTA can import or activate third-party engine layouts in-game.

## Project structure

```text
VANTA-Launcher/
├── app/
│   ├── src/main/java/com/vanta/launcher/
│   │   ├── MainActivity.kt
│   │   ├── data/
│   │   │   ├── LauncherDatabase.kt
│   │   │   ├── ControlLayoutStore.kt
│   │   │   └── SkinStore.kt
│   │   └── ui/
│   │       ├── VantaApp.kt
│   │       └── theme/VantaTheme.kt
│   └── src/test/
├── docs/
├── .github/workflows/android.yml
├── LICENSE
└── README.md
```

## License

The code in this foundation is released under GPL-3.0. If VANTA later incorporates or derives from Zalith Launcher 2 source, its GPL-3.0 licensing, copyright notices, modification notices, and corresponding-source obligations must be preserved. Review all bundled third-party components before distributing an APK.
