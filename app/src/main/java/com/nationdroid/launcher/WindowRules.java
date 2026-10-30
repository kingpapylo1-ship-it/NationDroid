package com.nationdroid.launcher;

/** Keeps internal desktop windows inside the usable area, including while the IME is open. */
public final class WindowRules {
    private WindowRules() {}
    public static int size(int requested, int minimum, int available) {
        int limit = Math.max(1, available);
        return Math.min(limit, Math.max(Math.min(minimum, limit), requested));
    }
    public static int position(int requested, int size, int available) {
        return Math.max(0, Math.min(requested, Math.max(0, available - size)));
    }
}
