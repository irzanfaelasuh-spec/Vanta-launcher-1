# Engine research — 3 November 2026

This note records the current public-source findings used to plan VANTA's engine phase. It is not a claim that the engine has already been integrated.

## Zalith Launcher 2

- Official repository: https://github.com/ZalithLauncher/ZalithLauncher2
- Latest release visible on 3 November 2026: **2.8.1**, published on 3 November 2026. The release notes mention SHA-1 verification for built-in component resources, Microsoft device-code token polling fixes, and renderer environment-variable fixes.
- The project README states that Zalith Launcher 2 uses PojavLauncher as its core launch engine and Jetpack Compose + Material Design 3 for its interface.
- Repository license: GNU GPL-3.0.
- The current source is modular: app module `ZalithLauncher`, rendering/native components under `LWJGL`, and a separate `LayerController` module.
- Control layouts are serializable structured JSON. The current `ControlLayout` model contains `info`, `layers`, `styles`, `joystickStyles`, and `editorVersion`. Each control layer includes normal buttons, text boxes, joystick buttons, visibility behavior, and state-aware visibility options. The layout manager validates editor versions, imports JSON streams, and saves layouts under a launcher-controlled directory.

Primary sources:

- https://github.com/ZalithLauncher/ZalithLauncher2/releases
- https://github.com/ZalithLauncher/ZalithLauncher2/blob/main/README_EN_US.md
- https://github.com/ZalithLauncher/ZalithLauncher2/blob/main/LICENSE
- https://github.com/ZalithLauncher/ZalithLauncher2/blob/main/LayerController/src/main/java/com/movtery/layer_controller/layout/ControlLayout.kt
- https://github.com/ZalithLauncher/ZalithLauncher2/blob/main/LayerController/src/main/java/com/movtery/layer_controller/layout/ControlLayer.kt
- https://github.com/ZalithLauncher/ZalithLauncher2/blob/main/ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/control/ControlManager.kt

## Amethyst Android / Pojav successor

- Repository: https://github.com/AngelAuraMC/Amethyst-Android
- The project is based on PojavLauncher. Its `v3_openjdk` branch README describes mobile OpenJDK 8/17/21 ports, OpenGL in the OpenJDK environment, OpenAL, and a range of Minecraft version families.
- The latest public stable release visible in the research pass was **1.1.7**, published 31 July 2026. Its release notes include support for 26.3-snapshot4+ and list renderer/controller limitations.
- The repository's license is LGPL-3.0.
- The Android manifest and `MainActivity` show the real native game surface, dedicated game process/service, GLFW/SDL input and touch-control handling. Its control import activity validates the older `version` + `mControlDataList` file shape.
- This makes it a viable alternative engine reference, but its layout format differs from Zalith Launcher 2.

Primary sources:

- https://github.com/AngelAuraMC/Amethyst-Android
- https://github.com/AngelAuraMC/Amethyst-Android/releases
- https://github.com/AngelAuraMC/Amethyst-Android/blob/v3_openjdk/README.md
- https://github.com/AngelAuraMC/Amethyst-Android/blob/v3_openjdk/app_pojavlauncher/src/main/AndroidManifest.xml
- https://github.com/AngelAuraMC/Amethyst-Android/blob/v3_openjdk/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/MainActivity.java
- https://github.com/AngelAuraMC/Amethyst-Android/blob/v3_openjdk/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/ImportControlActivity.java

## PojavLauncher legacy

The older `PojavLauncherTeam/PojavLauncher` repository was archived on 23 September 2025 and points users to Amethyst as its successor. VANTA should not start a new engine integration against the archived code unless a specific legacy compatibility need is established.

Source: https://github.com/PojavLauncherTeam/PojavLauncher

## Recommendation for VANTA

Use **Zalith Launcher 2.8.1 as the primary architecture and engine-integration reference**, because it most closely matches the requested UX and has the newest release in this research pass. Its GPL-3.0 license means VANTA must maintain GPL compliance if it incorporates or derives from that code. Keep VANTA's own identity and do not copy Zalith branding/assets.

Keep Amethyst Android as a secondary engine reference and fallback candidate. Do not mix control-layout JSON schemas. Build and test a dedicated migration layer before claiming cross-launcher control compatibility.
