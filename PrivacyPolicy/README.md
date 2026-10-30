# NationDroid privacy notes

These notes describe the native NationDroid implementation in this repository, not HyperDroid or another reference launcher.

- NationDroid queries launchable apps to display names, icons, declared Android categories and launch activities. The inventory is held in memory.
- Pinned, hidden, launch-confirmation and sphere app selections; the optional profile display name; and UI preferences are stored in app-private SharedPreferences. The app disables automatic backup and excludes these preferences from Android data extraction.
- This implementation has no internet permission, advertising, analytics or crash-reporting SDK. It does not transmit inventory, settings or sensor readings to a server.
- Motion sensing uses gravity/accelerometer readings while the visible sphere home is active. Readings affect rotation only and are not saved. Sensors are unregistered when the home is detached, hidden or paused.
- Optional notification dots require Android notification-listener access. The service receives system notification callbacks but retains only aggregate active counts by package in memory. It does not retain notification titles, message bodies or history. Disable the option and revoke access from Android Settings to stop this feature.
- Launch confirmation delegates authentication to Android's device-credential screen. NationDroid does not receive or store the PIN, password or pattern. This only gates launches from the launcher app-list/pin path; it is not system-wide app locking or a secure private space. Hidden apps are hidden only inside NationDroid.
- Settings export writes a user-chosen JSON document containing preferences and selected app identifiers. Import validates the document before replacing local preferences. The chosen document provider controls storage/synchronization of that file; back up only to a destination you trust. App data and authentication credentials are not part of the backup.
- App launch, app info, uninstall, wallpaper, document opening, default Home selection and Android settings delegate to Android or another app, whose own data practices apply.
- `REQUEST_DELETE_PACKAGES` permits a user-requested uninstall confirmation dialog. The signature-level internal permission protects notification-dot update broadcasts within the application.
- Uninstalling NationDroid or clearing its storage removes its local preferences. Exported JSON backups remain wherever you saved them until you remove them yourself.

For questions or bug reports, use the repository's GitHub Issues. Do not include personal device data in public issues.

The historical HyperDroid document is retained in [HYPERDROID-ORIGINAL.md](HYPERDROID-ORIGINAL.md). It does not describe NationDroid.
