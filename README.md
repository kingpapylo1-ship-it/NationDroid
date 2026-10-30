# NationDroid

A native, non-root Android desktop launcher for Android 8.0 and newer. The new interface follows a desktop layout: shortcuts at the left, a centered bottom taskbar, a searchable Start panel, and a window-style Settings app.

This repository previously held only HyperDroid documentation and images. The native application in `app/` is an independent implementation. The blue wave wallpaper and shell icons are drawn by this application, not extracted from the reference screenshots.

## Working features

- Desktop with This device, Files and Browser shortcuts, plus your pinned Android apps.
- Bottom taskbar with Start, Files, Settings and a live clock/date.
- Searchable Start menu showing installed app icons and names.
- Long press an app to pin/remove its desktop shortcut, view app info or request uninstall.
- Pins persist across restarts and upgrades from NationDroid 1.0.
- Native internal Start and Settings windows with title-bar minimize, maximize/restore and close controls. Drag the title caption to move a window; drag the bottom-right grip to resize it. Minimized panels reopen from the taskbar.
- Settings navigation for System, Bluetooth & devices, Personalize, Apps, Storage, Time & language, Privacy & security and About. Device information and storage totals come from Android. System controls open the corresponding Android settings screen.
- Editable local profile display name (no online account).
- Built-in blue wave background, current system wallpaper option and Android wallpaper picker.
- Files opens Android's document browser; selecting a document opens it in an installed viewer. This is not a private file manager.
- Default Home role selection on Android 10+ and Home settings on older versions.
- No account, root, internet permission, advertising or analytics SDK.

Only NationDroid's own Start and Settings panels use its internal windows. Installed Android apps open using normal system activity/window behavior. Arbitrary floating Android app windows, widgets, work-profile app launching, app icon/name editing and a built-in file manager are not implemented. The retained `src/assets/` images depict the original HyperDroid product, not this implementation.

## Install from GitHub on Android

1. Open the repository's **Actions** tab, then **Build NationDroid APK**.
2. Open a successful run for the branch containing this implementation.
3. Download **NationDroid-debug-apk**, unzip it and install `app-debug.apk`. Allow installation from your browser/file manager if Android asks.
4. Open NationDroid. Click **Settings → System → Default launcher** to select it as Home.
5. Open Start and long press an installed app to pin it to the desktop.

The workflow generates a debug APK, not a Play Store release. GitHub runners may generate different debug signing keys across builds; Android may require uninstalling a previous APK signed with another key, which clears its pins. Use a stable private release signing key for distribution. No signing secrets are committed.

## Build locally

Requires JDK 17 and Android SDK platform 35/build-tools 35.0.0. Open the project in Android Studio or use the included checksum-pinned Gradle 8.9 wrapper.

Set `ANDROID_HOME` to the SDK directory, or create an untracked `local.properties` containing `sdk.dir=/absolute/path/to/android-sdk`.

```sh
bash scripts/check.sh
./gradlew --no-daemon clean lintDebug assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. The APK is version 1.1.0 (version code 2), with package `com.nationdroid.launcher`.

Standard Android SDK build tools target desktop hosts. A local build in ARM64 Termux needs a separately supported Android build environment; GitHub Actions provides the desktop build host.

## Validation and device checks

The Java checks cover app search, adaptive app-grid sizing, and window movement/resize bounds including reduced space from the on-screen keyboard. Lint and APK compilation run in GitHub Actions. Compilation is not a substitute for device/visual testing.

Before release, check on a phone: launch and search apps; pin/unpin and restart; rotate; open Settings sections; minimize, restore, maximize, drag and resize both panels; open the keyboard and close it; choose a document; test wallpaper, app info and uninstall; set and switch default Home apps; verify navigation gestures and accessibility text scaling.

To switch back, open Android Settings → Apps → Default apps → Home app and select the previous launcher.

## Privacy and attribution

See [NationDroid privacy notes](PrivacyPolicy/README.md). Original image assets, license attribution and the historical HyperDroid privacy document remain in the repository.
