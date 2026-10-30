# NationDroid

A native, non-root Android home launcher for Android 8.0 and newer. This repository previously contained only HyperDroid documentation and images. The `app/` directory is a new, independent implementation; it does not contain HyperDroid's source code.

## Implemented

- Home screen with clock, date and pinned applications.
- Installed app drawer with name and package search.
- Long press to pin/unpin, open Android app info or request uninstall.
- Pins and grid preferences saved locally across restarts.
- Adaptive grid, portrait/landscape support and keyboard-focusable controls.
- Default home selection using Android's Home role on Android 10+.
- System wallpaper picker and optional wallpaper background.
- Refresh after returning to the launcher and after app installation/removal.
- No account, root, internet permission, analytics or advertising SDK.

Android apps open using the system's normal activity/window behavior. This version does not implement arbitrary floating app windows, widgets, a file manager, icon editing, or work-profile app launching. The retained `src/assets/` screenshots show the original HyperDroid product, **not this implementation**.

## Get an APK from GitHub (no PC required)

1. Open this repository's **Actions** tab and select **Build NationDroid APK**.
2. Open a successful run for the branch containing this implementation. If necessary, use **Run workflow** after the workflow is on the default branch.
3. Download **NationDroid-debug-apk**, unzip it, and open `app-debug.apk` on Android.
4. Allow installation from the browser/file manager when Android asks.
5. Open NationDroid, then choose **Settings → Set as default launcher**.

The workflow produces an installable debug APK, not a Play Store release. GitHub's hosted runner may generate a different debug signing key between builds; updating an installed APK may require uninstalling the earlier version (which removes its pins). Use a stable private release key for distribution. No signing secrets are included.

## Build locally

Requires JDK 17 and Android SDK platform 35/build-tools 35.0.0. Android Studio can open the project. The included Gradle wrapper downloads Gradle 8.9 and verifies its distribution checksum.

Set `ANDROID_HOME` to the installed Android SDK directory, or create an untracked `local.properties` containing `sdk.dir=/absolute/path/to/android-sdk`.

```sh
bash scripts/check.sh
./gradlew --no-daemon lintDebug assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

The standard Android SDK build tools target desktop hosts. A complete local build directly in Termux on ARM64 requires a separately supported Android build environment; the GitHub workflow avoids that requirement.

## Device checks before release

The pure Java checks cover search behavior and grid sizing. Lint and compilation run in GitHub Actions. A real-device test is still required: choose NationDroid as Home; open/search/pin an app; rotate the screen; restart the launcher; install/remove an app; try wallpaper, app info and uninstall; verify system gestures and keyboard insets. To switch back, open Android Settings → Apps → Default apps → Home app and choose your previous launcher.

## Privacy and attribution

See [NationDroid privacy notes](PrivacyPolicy/README.md). The original HyperDroid privacy document is preserved separately as historical material. Original image assets and the existing MIT license attribution remain in the repository.
