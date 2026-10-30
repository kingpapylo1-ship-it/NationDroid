# NationDroid privacy notes

These notes describe the native NationDroid implementation in this repository, not the original HyperDroid product.

- NationDroid queries launchable apps to display their names, icons and launch activities. This list is held in memory.
- Pinned component identifiers, the optional local profile display name and preferences are stored in Android's app-private SharedPreferences. Automatic Android backup is disabled for this app.
- This implementation requests no internet permission and contains no ad, analytics or crash-reporting SDK. It does not send app inventory or preferences to a server.
- Opening an app, its Android settings, the wallpaper picker, the default-home chooser or the uninstall dialog delegates that action to Android or another app, whose own data practices apply.
- The `REQUEST_DELETE_PACKAGES` permission allows the long-press Uninstall action to ask Android to show its confirmation dialog. NationDroid cannot silently uninstall apps.
- Uninstalling NationDroid or clearing its storage removes its local preferences.

For questions or bug reports, use this repository's GitHub Issues. Do not include personal device data in public issues.

The original HyperDroid document is retained in [HYPERDROID-ORIGINAL.md](HYPERDROID-ORIGINAL.md) for historical attribution. It does not describe NationDroid.
