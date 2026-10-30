package com.nationdroid.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int INK = Color.rgb(237, 244, 255);
    private static final int MUTED = Color.rgb(158, 177, 201);
    private static final int ACCENT = Color.rgb(100, 228, 203);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<AppEntry> apps = new ArrayList<>();
    private SharedPreferences prefs;
    private Set<String> favorites;
    private LinearLayout root;
    private GridLayout grid;
    private TextView clock, date, heading, status;
    private EditText search;
    private boolean drawer, registered, loading, reloadRequested;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            Date now = new Date();
            String pattern = android.text.format.DateFormat.is24HourFormat(MainActivity.this) ? "HH:mm" : "h:mm a";
            clock.setText(new SimpleDateFormat(pattern, Locale.getDefault()).format(now));
            date.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now));
            handler.postDelayed(this, 1000);
        }
    };
    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { loadApps(); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        favorites = new HashSet<>(prefs.getStringSet("favorites", new HashSet<>()));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        buildUi();
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> setDrawer(false));
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(packages, filter, Context.RECEIVER_EXPORTED);
        else registerReceiver(packages, filter);
        registered = true;
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(12));
        if (prefs.getBoolean("wallpaper", false)) {
            root.setBackgroundColor(Color.argb(155, 7, 14, 26));
        } else {
            root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{0xff172e46, 0xff0b1220, 0xff12342f}));
        }
        // Android 15 enforces edge-to-edge. Keep all controls inside system/IME insets.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(
                        android.view.WindowInsets.Type.systemBars()
                                | android.view.WindowInsets.Type.displayCutout()
                                | android.view.WindowInsets.Type.ime());
                left = safe.left; top = safe.top; right = safe.right; bottom = safe.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft(); top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight(); bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(dp(20) + left, dp(12) + top, dp(20) + right, dp(12) + bottom);
            if (clock != null && date != null) {
                boolean compact = getResources().getConfiguration().screenHeightDp < 500
                        || bottom > dp(100);
                clock.setVisibility(compact ? View.GONE : View.VISIBLE);
                date.setVisibility(compact ? View.GONE : View.VISIBLE);
            }
            return insets;
        });
        setContentView(root);
        root.requestApplyInsets();

        LinearLayout top = row();
        TextView brand = text("NATION / DROID", 14, ACCENT);
        brand.setTypeface(null, Typeface.BOLD);
        top.addView(brand, new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView settings = button("Settings");
        settings.setOnClickListener(v -> settings());
        top.addView(settings);
        root.addView(top);
        clock = text("", 48, INK);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        root.addView(clock);
        date = text("", 16, MUTED);
        date.setPadding(0, 0, 0, dp(20));
        root.addView(date);
        if (getResources().getConfiguration().screenHeightDp < 500) {
            clock.setVisibility(View.GONE);
            date.setVisibility(View.GONE);
        }

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(INK);
        search.setHintTextColor(MUTED);
        search.setTextSize(16);
        search.setHint("Search installed apps");
        search.setPadding(dp(16), 0, dp(16), 0);
        search.setBackground(panel(0xff1c2a3d, 16));
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(52)));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) drawer = true;
                renderApps();
            }
            @Override public void afterTextChanged(Editable e) {}
        });
        LinearLayout section = row();
        heading = text("Pinned apps", 18, INK);
        section.addView(heading, new LinearLayout.LayoutParams(0, dp(56), 1));
        TextView all = button("All apps");
        all.setOnClickListener(v -> setDrawer(!drawer));
        section.addView(all);
        root.addView(section);
        status = text("Loading installed apps…", 14, MUTED);
        root.addView(status);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        grid = new GridLayout(this);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        scroll.addView(grid);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout dock = row();
        TextView home = button("Home");
        home.setOnClickListener(v -> setDrawer(false));
        TextView appButton = button("Apps");
        appButton.setOnClickListener(v -> setDrawer(true));
        TextView system = button("Android settings");
        system.setOnClickListener(v -> open(new Intent(Settings.ACTION_SETTINGS)));
        dock.addView(home, new LinearLayout.LayoutParams(0, dp(54), 1));
        dock.addView(appButton, new LinearLayout.LayoutParams(0, dp(54), 1));
        dock.addView(system, new LinearLayout.LayoutParams(0, dp(54), 2));
        root.addView(dock);
        grid.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol) renderApps();
        });
    }

    private void loadApps() {
        if (loading) { reloadRequested = true; return; }
        loading = true;
        worker.execute(() -> {
            List<AppEntry> found = new ArrayList<>();
            String error = null;
            try {
                PackageManager pm = getPackageManager();
                Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                Set<String> seen = new HashSet<>();
                for (ResolveInfo info : pm.queryIntentActivities(intent, 0)) {
                    if (info.activityInfo == null || getPackageName().equals(info.activityInfo.packageName)) continue;
                    ComponentName component = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                    if (!seen.add(component.flattenToString())) continue;
                    found.add(new AppEntry(info.loadLabel(pm).toString(), component, info.loadIcon(pm)));
                }
                Collator collator = Collator.getInstance();
                found.sort((a, b) -> collator.compare(a.label, b.label));
            } catch (RuntimeException e) { error = "Couldn't load apps. Open Settings → Refresh apps to retry."; }
            String message = error;
            handler.post(() -> {
                loading = false;
                if (isDestroyed()) return;
                if (message == null) {
                    apps.clear(); apps.addAll(found);
                    // Retain pins during package replacement; prune only on an explicit unpin.
                    renderApps();
                } else status.setText(message);
                if (reloadRequested) { reloadRequested = false; loadApps(); }
            });
        });
    }

    private void renderApps() {
        if (grid == null) return;
        grid.removeAllViews();
        int widthDp = (int) (getResources().getDisplayMetrics().widthPixels
                / getResources().getDisplayMetrics().density) - 40;
        if (grid.getWidth() > 0) widthDp = (int) (grid.getWidth() / getResources().getDisplayMetrics().density);
        int cols = AppRules.columns(widthDp, prefs.getInt("columns", 4));
        grid.setColumnCount(cols);
        String query = search.getText().toString();
        boolean searching = !query.trim().isEmpty();
        heading.setText(searching ? "Search results" : drawer ? "All apps" : "Pinned apps");
        int count = 0;
        for (AppEntry app : apps) {
            if (!AppRules.matches(app.label, app.component.getPackageName(), query)) continue;
            if (!drawer && !searching && !favorites.contains(app.key())) continue;
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(4), dp(10), dp(4), dp(10));
            tile.setBackground(selectable());
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(app.icon);
            tile.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
            TextView label = text(app.label, 12, INK);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tile.addView(label, new LinearLayout.LayoutParams(-1, dp(38)));
            tile.setContentDescription(app.label + ". Long press for options.");
            tile.setFocusable(true);
            tile.setOnClickListener(v -> launch(app));
            tile.setOnLongClickListener(v -> { appMenu(tile, app); return true; });
            GridLayout.LayoutParams layout = new GridLayout.LayoutParams();
            layout.width = 0;
            layout.height = dp(112);
            layout.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            layout.setGravity(Gravity.FILL_HORIZONTAL);
            grid.addView(tile, layout);
            count++;
        }
        status.setText(count == 0 ? (searching ? "No matching apps." : drawer
                ? "No launchable apps found. Refresh from Settings."
                : "Open All apps, then long press an app to pin it here.")
                : count + (count == 1 ? " app • long press for options" : " apps • long press for options"));
    }

    private void launch(AppEntry app) {
        open(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
    }

    private void appMenu(View anchor, AppEntry app) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(favorites.contains(app.key()) ? "Unpin" : "Pin to home").setOnMenuItemClickListener(item -> {
            if (!favorites.remove(app.key())) favorites.add(app.key());
            prefs.edit().putStringSet("favorites", new HashSet<>(favorites)).apply();
            renderApps(); return true;
        });
        menu.getMenu().add("App info").setOnMenuItemClickListener(item -> {
            open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.component.getPackageName())));
            return true;
        });
        menu.getMenu().add("Uninstall").setOnMenuItemClickListener(item -> {
            open(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.component.getPackageName())));
            return true;
        });
        menu.show();
    }

    private void settings() {
        String[] choices = {"Set as default launcher", "Choose Android wallpaper", "Toggle wallpaper background",
                "App grid columns", "Refresh apps", "About NationDroid"};
        new AlertDialog.Builder(this).setTitle("NationDroid settings").setItems(choices, (dialog, which) -> {
            switch (which) {
                case 0: defaultHome(); break;
                case 1: open(new Intent(Intent.ACTION_SET_WALLPAPER)); break;
                case 2:
                    prefs.edit().putBoolean("wallpaper", !prefs.getBoolean("wallpaper", false)).apply();
                    recreate(); break;
                case 3:
                    new AlertDialog.Builder(this).setTitle("Maximum columns (adapts to screen)")
                        .setSingleChoiceItems(new String[]{"3", "4", "5", "6", "7"}, prefs.getInt("columns", 4) - 3,
                            (d, selected) -> { prefs.edit().putInt("columns", selected + 3).apply(); renderApps(); d.dismiss(); })
                        .setNegativeButton("Cancel", null).show(); break;
                case 4: loadApps(); break;
                case 5:
                    new AlertDialog.Builder(this).setTitle("NationDroid 1.0.0")
                        .setMessage("A native Android home launcher. App list and pins stay on your device. No ads, analytics, account or network permission.\n\nThis is an independent implementation, not the HyperDroid app. Android apps open in their normal system windows.")
                        .setPositiveButton("OK", null).show(); break;
                default: break;
            }
        }).show();
    }

    private void defaultHome() {
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager roles = getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roles.isRoleHeld(RoleManager.ROLE_HOME)) { toast("NationDroid is already your default launcher."); return; }
                open(roles.createRequestRoleIntent(RoleManager.ROLE_HOME)); return;
            }
        }
        open(new Intent(Settings.ACTION_HOME_SETTINGS));
    }

    private void setDrawer(boolean show) {
        drawer = show;
        search.setText("");
        search.clearFocus();
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(), 0);
        renderApps();
    }

    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (android.content.ActivityNotFoundException | SecurityException e) { toast("Android couldn't open that action."); }
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout row() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }
    private TextView button(String value) {
        TextView view = text(value, 14, ACCENT);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(10), dp(10), dp(10), dp(10));
        view.setMinHeight(dp(48));
        view.setFocusable(true);
        view.setBackground(selectable());
        view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName("android.widget.Button");
            }
        });
        return view;
    }
    private Drawable selectable() {
        android.util.TypedValue value = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true);
        return getDrawable(value.resourceId);
    }
    private Drawable panel(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color); shape.setCornerRadius(dp(radius));
        return shape;
    }
    @Override protected void onResume() {
        super.onResume(); handler.removeCallbacks(tick); handler.post(tick); loadApps();
    }
    @Override protected void onPause() { handler.removeCallbacks(tick); super.onPause(); }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); setDrawer(false); }
    @Override public void onBackPressed() { setDrawer(false); }
    @Override protected void onDestroy() {
        if (registered) unregisterReceiver(packages);
        handler.removeCallbacksAndMessages(null); worker.shutdownNow(); super.onDestroy();
    }
    private static final class AppEntry {
        final String label;
        final ComponentName component;
        final Drawable icon;
        AppEntry(String label, ComponentName component, Drawable icon) {
            this.label = label; this.component = component; this.icon = icon;
        }
        String key() { return component.flattenToString(); }
    }
}
