package com.nationdroid.launcher;

import java.util.Locale;

/** Platform-independent rules shared by the drawer and its regression checks. */
public final class AppRules {
    private AppRules() {}

    public static boolean matches(String label, String packageName, String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return label.toLowerCase(Locale.ROOT).contains(normalized)
                || packageName.toLowerCase(Locale.ROOT).contains(normalized);
    }

    public static int columns(int widthDp, int preferred) {
        return Math.max(2, Math.min(Math.max(3, Math.min(7, preferred)), widthDp / 88));
    }
}
