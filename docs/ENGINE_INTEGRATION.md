# Minecraft Java engine integration plan

## Current state

The VANTA foundation app does **not** launch Minecraft. There is no bundled JVM, Minecraft library/assets resolver, LWJGL native runtime, renderer, account service, or in-game input bridge in this archive.

The `LAUNCH` button displays this state. It must remain gated until a real engine adapter is compiled into the application and successfully tested on ARM64 Android hardware.

## Preferred integration route

1. Pin the exact upstream Zalith Launcher 2 release/tag that will be used (the current research candidate is `2.8.1`). Do not track an unpinned moving branch for release builds.
2. Review and preserve the GPL-3.0 notices and all third-party component licenses.
3. Integrate the existing Pojav-based runtime, Java runtime packaging/installer, LWJGL/GLFW, renderer modules, and native libraries. Do not write a replacement JVM or renderer in the VANTA UI module.
4. Keep VANTA branding in a separate presentation layer: app name, icons, theme, dashboard, and local-profile UI.
5. Replace/bridge the engine account model carefully. A VANTA local profile is only a local profile; it is not a Microsoft-owned Minecraft account and must not be treated as one by multiplayer authentication.
6. Adapt the control editor to the engine's actual control layout model. For Zalith 2 the model is `ControlLayout(info, layers, styles, joystickStyles, editorVersion)` and the editor handles button and joystick layer types. The current VANTA JSON is only an editable transport format and is not directly executable by that engine.
7. Add a tested converter between VANTA portable JSON and the pinned engine schema. Preserve unknown engine fields when round-tripping; reject layouts that cannot be represented safely.
8. Wire the control layout to the runtime's touch/keyboard/mouse input pipeline. A button that only appears in the VANTA preview is not an in-game control.
9. Verify Minecraft installation, Java selection, game launch, real logs, renderer, touch controls, and background process persistence on a physical ARM64 Android device.

## Acceptance gate before enabling LAUNCH

- [ ] Pinned engine source and license audit committed.
- [ ] ARM64 debug APK builds in CI.
- [ ] A supported Minecraft Java version installs from official metadata.
- [ ] Required JRE is installed and selected.
- [ ] The real Java process starts and renders a world.
- [ ] Touch controls send real in-game key/mouse events.
- [ ] Imported control JSON is converted and validated against the engine schema.
- [ ] Crash logs and game logs are captured from the real process.
- [ ] Returning from Android Home preserves the game process where the OS allows it.

Do not mark any checkbox complete based only on a rendered screen or a mock engine callback.
