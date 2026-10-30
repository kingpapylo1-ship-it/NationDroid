# NationDroid 1.2

A native, non-root Android launcher for Android 8.0 and newer, with four selectable home modes. No account, internet permission, advertising SDK or subscription is required.

## Launcher modes

Open the taskbar's Settings button, choose **Launcher mode**, select a layout and tap **Apply now**.

| Mode | Implemented behavior |
| --- | --- |
| Desktop | Blue wave background, desktop shortcuts, taskbar, Start panel and launcher-owned Settings window. |
| Classic layout | Horizontal pages of installed apps, search and a scrollable pinned-app dock. |
| Minimal list | Searchable alphabetic app list with a letter index. |
| Holo Sphere | Installed app icons distributed on a perspective sphere. Swipe to rotate, tap to launch, long press for app actions. Optional auto-spin, tilt/shake sensing, inertia and front-surface clipping. |

The current mode defaults to Desktop for existing installations. Pins from 1.0/1.1 are preserved and appear in the alternate modes' dock. The **Apps list** button remains available as an accessible list alternative to the sphere. Keyboard arrows rotate the sphere and Enter launches its frontmost app.

## Home-screen options

**Settings → Home screen options** includes:

- Manage app sphere: choose which launchable app activities appear, or use all apps so new installations appear automatically.
- Auto spin and motion sensing. Motion works without a special permission; if a motion sensor is absent, touch rotation still works. Sensor listeners and animation are stopped when the launcher is not visible.
- 3D surface clipping, icon/label toggle, and smooth, linear or exponential depth scaling.
- Top time/date widget and bottom pinned-app dock toggles for alternate home modes.
- Icon size, label size, edge contrast, swipe sensitivity, inertia damping and sphere scale sliders, each with a reset control.

The in-settings sphere preview uses available app icons. Preview sensing and idle animation are disabled. The top widget shows time/date; it does not access a calendar.

## Style and general settings

- Local Ocean, Violet and Forest theme presets and five accent colors.
- Light/dark appearance for alternate home modes and the customization screen.
- Rounded-square, circle or heptagon app-icon clipping; sans, serif or monospace fonts.
- Solid or translucent home/taskbar surfaces. Translucency is implemented; this is not a compositor blur or a live refraction shader.
- None, fade or zoom return-to-home animations.
- Built-in blue wave wallpaper, current Android wallpaper, or Android's wallpaper picker.
- App-category filtering based on Android's declared categories. Apps without a supported declared category appear under Other; there is no invented category assignment.
- Optional notification dots for active notifications. Enable the switch and grant NationDroid access under **Notification access**. Counts stay in memory; notification text is not retained or uploaded.
- Hidden-app selection for NationDroid's app lists and pins.
- Optional Android device-credential confirmation for selected apps launched from NationDroid's app lists/pins.
- JSON settings export/restore through Android's document provider. Backups include local settings and selected app identifiers, not app data. Imports validate the format, value types, ranges and component identifiers before replacing preferences.

**Privacy limits:** hiding is a launcher display preference, not a secure private space. Credential confirmation only gates the launcher's app-list/pin launch path; another launcher, notifications, Android system shortcuts or app-to-app intents can still open those apps. It does not lock or isolate another app's data. No third-party premium entitlement, private-space sandbox or third-party widget host is included.

## Desktop features retained

- This device, Files and Browser shortcuts, plus pinned app shortcuts.
- Bottom taskbar with Start, Files, Settings and a clock.
- Start provides installed-app search and pin/info/uninstall actions.
- Native internal Start/desktop Settings windows: minimize, close, maximize/restore, title-caption dragging and bottom-right resize grip.
- Tap the taskbar Settings button for the new customization screen. Long press it for the original desktop system panel.
- The desktop system panel displays actual device and storage information. Bluetooth, Wi-Fi, display, security, language and default Home actions open Android settings.
- Files opens Android's document browser and an installed viewer for the selected file. It is not a private file manager.

Only launcher-owned panels use NationDroid's internal windows. Installed Android apps open using normal system activity/window behavior. Arbitrary floating Android app windows, work-profile app launching, app icon/name editing, agenda access and third-party widgets are not implemented.

## Install on Android

Use the downloadable debug APK, or open GitHub **Actions → Build NationDroid APK**, choose a successful run, download **NationDroid-debug-apk**, unzip and install `app-debug.apk`.

To set it as Home, choose **Settings → Set as default launcher**. To switch back, use Android Settings → Apps → Default apps → Home app.

The application ID remains `com.nationdroid.launcher`; version 1.2.0 has version code 3. An APK signed with the same certificate upgrades the previous version and retains local preferences. GitHub runners can generate different debug signing keys across builds; installing an APK with a different key requires removing the old installation and its preferences. Use a stable private release key for distribution; no key is committed.

## Build and test

Requires JDK 17 and Android SDK platform 35/build-tools 35.0.0. Open in Android Studio or use the checksum-pinned Gradle 8.9 wrapper. Set `ANDROID_HOME`, or create an untracked `local.properties` with `sdk.dir=/absolute/path/to/android-sdk`.

```sh
bash scripts/check.sh
./gradlew --no-daemon clean testDebugUnitTest lintDebug assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Checks cover search/grid/window rules, sphere distribution/rotation/damping, settings validation, Android activity lifecycle across four modes, settings navigation and Apply behavior, and backup round-trip/rejection. Robolectric tests run at API 28 and API 35. These tests do not replace device/visual testing, real sensor testing or manual Android permission/credential flows.

Before release, test on a phone: each mode, search/pin/unpin, rotation, typing in search, sphere touch and tilt, motion disable, background/resume, custom app selection, hidden apps, credential confirm/cancel, notification access grant/revoke, wallpaper, backup/restore, and switching the default Home app.

Standard Android SDK build tools target desktop hosts. A local build in ARM64 Termux needs a supported Android build environment; GitHub Actions supplies the desktop host.

## Attribution

This repository previously held only HyperDroid documentation and images. This is an independent native implementation, with its own blue wave drawing and shell icons. The retained `src/assets/` screenshots depict HyperDroid, not NationDroid. Original assets, license attribution and the historical privacy document are retained. See [NationDroid privacy notes](PrivacyPolicy/README.md).
