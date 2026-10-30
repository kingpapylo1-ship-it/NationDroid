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
import android.os.StatFs;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
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

/** Desktop shell. Only this launcher's own panels run inside its windows. */
public final class MainActivity extends Activity {
    private static final int WHITE = 0xfff4f5f7, MUTED = 0xffb4b8c0, BLUE = 0xff65ceff;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<AppEntry> apps = new ArrayList<>();
    private SharedPreferences prefs;
    private Set<String> favorites;
    private FrameLayout screen, desktop, workspace;
    private LinearLayout taskbar;
    private TextView tray, startButton, settingsButton;
    private ShellWindow startWindow, settingsWindow;
    private GridLayout startGrid;
    private EditText search;
    private TextView appStatus;
    private boolean registered, loading, reloadRequested;
    private HomeModesView homeModes;
    private boolean resumed;
    private final Runnable welcomeTask=this::showBootWelcome;
    private DoubleTapLockGesture lockGesture;
    private final int[] touchLocation=new int[2];
    private int optionsRevision;
    private AppEntry pendingLaunch;
    private final BroadcastReceiver badgesChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { invalidateTree(screen); }
    };
    private String settingsSection = "System";
    private LinearLayout settingsContent;
    private final List<TextView> sidebarItems = new ArrayList<>();

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (tray != null) {
                String pattern = android.text.format.DateFormat.is24HourFormat(MainActivity.this) ? "HH:mm" : "h:mm a";
                tray.setText(new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date())
                        + "\n" + new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date()));
            }
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
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        android.view.ViewConfiguration gestures=android.view.ViewConfiguration.get(this);
        lockGesture=new DoubleTapLockGesture(android.view.ViewConfiguration.getDoubleTapTimeout(),250,
                gestures.getScaledTouchSlop(),gestures.getScaledDoubleTapSlop());
        optionsRevision = prefs.getInt("revision",0);
        buildDesktop();
        handlePinning(getIntent());
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::backToDesktop);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(packages, filter, Context.RECEIVER_EXPORTED);
        else registerReceiver(packages, filter);
        registerBadgesReceiver();
        registered = true;
    }

    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerBadgesReceiver() {
        IntentFilter badgesFilter = new IntentFilter(NotificationBadges.ACTION);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(badgesChanged, badgesFilter, Context.RECEIVER_NOT_EXPORTED);
        // Android 8–12 lacks NOT_EXPORTED. A signature permission restricts senders instead.
        else registerReceiver(badgesChanged, badgesFilter, "com.nationdroid.launcher.INTERNAL", null);
    }

    private void buildDesktop() {
        screen = new FrameLayout(this);
        if (!prefs.getBoolean("wallpaper", false)) screen.addView(new DesktopBackdrop(this), new FrameLayout.LayoutParams(-1, -1));
        desktop = new FrameLayout(this);
        screen.addView(desktop, new FrameLayout.LayoutParams(-1, -1));
        screen.setOnApplyWindowInsetsListener((v, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(android.view.WindowInsets.Type.systemBars()
                        | android.view.WindowInsets.Type.displayCutout() | android.view.WindowInsets.Type.ime());
                left = safe.left; top = safe.top; right = safe.right; bottom = safe.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft(); top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight(); bottom = insets.getSystemWindowInsetBottom();
            }
            desktop.setPadding(left + dp(6), top + dp(4), right + dp(6), bottom + dp(4));
            return insets;
        });
        setContentView(screen);
        screen.requestApplyInsets();
        LinearLayout layout = column();
        desktop.addView(layout, new FrameLayout.LayoutParams(-1, -1));
        workspace = new FrameLayout(this);
        layout.addView(workspace, new LinearLayout.LayoutParams(-1, 0, 1));
        workspace.setOnLongClickListener(v -> { desktopMenu(); return true; });
        workspace.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r-l != or-ol || b-t != ob-ot) {
                renderDesktop();
                if (startWindow != null) startWindow.fit();
                if (settingsWindow != null) settingsWindow.fit();
            }
        });
        taskbar = row();
        taskbar.setPadding(dp(8), dp(3), dp(8), dp(3));
        taskbar.setBackground(panel("glass".equals(prefs.getString("surface","solid")) ? 0xb32b3548 : 0xff252b36, 9));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(-1, dp(56));
        barParams.topMargin = dp(4);
        layout.addView(taskbar, barParams);
        View leftSpacer = new View(this);
        taskbar.addView(leftSpacer, new LinearLayout.LayoutParams(0, 1, 1));
        startButton = iconButton("▦", "Open Start menu", 29);
        startButton.setTextColor(BLUE);
        startButton.setOnClickListener(v -> toggleStart());
        taskbar.addView(startButton, new LinearLayout.LayoutParams(dp(52), -1));
        TextView files = iconButton("▰", "Open files", 31);
        files.setTextColor(0xffffca53);
        files.setOnClickListener(v -> openFiles());
        taskbar.addView(files, new LinearLayout.LayoutParams(dp(52), -1));
        TextView notifications = iconButton("🔔", "Private notifications", 23);
        notifications.setOnClickListener(v -> open(new Intent(this,PrivateNotificationsActivity.class)));
        taskbar.addView(notifications,new LinearLayout.LayoutParams(dp(48),-1));
        settingsButton = iconButton("⚙", "Open launcher settings", 28);
        settingsButton.setOnClickListener(v -> openOptions());
        settingsButton.setOnLongClickListener(v -> { showSettings("System"); return true; });
        taskbar.addView(settingsButton, new LinearLayout.LayoutParams(dp(52), -1));
        LinearLayout right = row();
        right.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        tray = text("", 10, WHITE);
        tray.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        tray.setContentDescription("Clock. Tap for date and time settings.");
        tray.setOnClickListener(v -> open(new Intent(Settings.ACTION_DATE_SETTINGS)));
        tray.setPadding(dp(2), 0, dp(2), 0);
        right.addView(tray);
        taskbar.addView(right, new LinearLayout.LayoutParams(0, -1, 1));
        renderDesktop();
    }

    private void renderDesktop() {
        // The icon layer is separate from the windows, so app refreshes preserve open panels.
        View old = workspace.findViewWithTag("desktop-icons");
        if (old != null) workspace.removeView(old);
        homeModes = null;
        if (!"desktop".equals(prefs.getString("mode","desktop"))) {
            homeModes = new HomeModesView(this, visibleApps(), favorites, prefs, this::launch,
                    app -> appMenu(workspace, app), this::openOptions, this::toggleStart);
            homeModes.setActive(resumed);
            homeModes.setTag("desktop-icons");
            workspace.addView(homeModes, 0, new FrameLayout.LayoutParams(-1,-1));
            return;
        }
        ScrollView scroll = new ScrollView(this);
        scroll.setTag("desktop-icons");
        scroll.setFillViewport(true);
        GridLayout icons = new GridLayout(this);
        int usableHeight = workspace.getHeight() > 0 ? workspace.getHeight() : dp(560);
        int rows = Math.max(1, (usableHeight - dp(8)) / dp(94));
        int usableWidth = workspace.getWidth() > 0 ? workspace.getWidth() : dp(340);
        int cols = Math.max(1, usableWidth / dp(88));
        icons.setOrientation(GridLayout.VERTICAL);
        int count = 3;
        for (AppEntry app : visibleApps()) if (favorites.contains(app.key())) count++;
        if (count > rows * cols) rows = (count + cols - 1) / cols;
        icons.setRowCount(rows);
        icons.setColumnCount(cols);
        addDesktopIcon(icons, "This device", "▣", 0xff56d9ff, null, () -> showSettings("System"), null);
        addDesktopIcon(icons, "Files", "▰", 0xffffcb57, null, this::openFiles, null);
        addDesktopIcon(icons, "Browser", "◎", 0xff8bc9ff, null,
                () -> open(new Intent(this,BrowserActivity.class)), null);
        for (AppEntry app : visibleApps()) if (favorites.contains(app.key())) {
            addDesktopIcon(icons, app.label, null, WHITE, app.icon, () -> launch(app), app);
        }
        scroll.addView(icons);
        scroll.setOnLongClickListener(v -> { desktopMenu(); return true; });
        workspace.addView(scroll, 0, new FrameLayout.LayoutParams(-1, -1));
    }

    private void addDesktopIcon(GridLayout grid, String label, String glyph, int color, Drawable image, Runnable click, AppEntry app) {
        LinearLayout tile = appTile(label, glyph, color, image, app);
        tile.setOnClickListener(v -> click.run());
        tile.setOnLongClickListener(v -> {
            if (app != null) appMenu(tile, app); else desktopMenu();
            return true;
        });
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = dp(88); params.height = dp(94);
        grid.addView(tile, params);
    }

    private LinearLayout appTile(String label, String glyph, int color, Drawable image, AppEntry entry) {
        LinearLayout tile = column();
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(3), dp(5), dp(3), dp(3));
        tile.setBackground(selectable());
        tile.setFocusable(true);
        if (image != null) {
            View icon;
            if (entry != null) icon = new StyledIconView(this,entry,prefs);
            else { ImageView fallback = new ImageView(this); fallback.setImageDrawable(image); icon=fallback; }
            int iconSize=dp(Math.round(30+prefs.getInt("iconSize",55)*.25f));
            tile.addView(icon, new LinearLayout.LayoutParams(iconSize,iconSize));
        } else {
            String kind = "▣".equals(glyph) ? "device" : "▰".equals(glyph) ? "files" : "browser";
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(new ShellIcon(kind));
            tile.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));
        }
        TextView name = text(label, Math.round(8+prefs.getInt("labelSize",50)*.1f), WHITE);
        name.setVisibility(prefs.getBoolean("labels",true)?View.VISIBLE:View.GONE);
        name.setGravity(Gravity.CENTER);
        name.setShadowLayer(dp(2), 0, dp(1), 0xff000000);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tile.addView(name, new LinearLayout.LayoutParams(-1, dp(32)));
        tile.setContentDescription(label + ". Long press for options.");
        return tile;
    }

    private void toggleStart() {
        if (startWindow != null && startWindow.getVisibility() == View.VISIBLE) { startWindow.minimize(); return; }
        if (startWindow != null) { startWindow.restore(); return; }
        startWindow = new ShellWindow("Start", "▦", false, () -> { startWindow = null; startGrid = null; search = null; });
        LinearLayout body = column();
        body.setPadding(dp(16), dp(8), dp(16), dp(12));
        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(WHITE); search.setHintTextColor(MUTED);
        search.setHint("Search your apps");
        search.setPadding(dp(12), 0, dp(12), 0);
        search.setBackground(panel(0xff171b23, 7));
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        body.addView(search, new LinearLayout.LayoutParams(-1, dp(46)));
        LinearLayout heading = row();
        heading.addView(text("All apps", 15, WHITE), new LinearLayout.LayoutParams(0, dp(44), 1));
        TextView refresh = action("Refresh", this::loadApps);
        heading.addView(refresh);
        body.addView(heading);
        appStatus = text("Loading apps…", 11, MUTED);
        body.addView(appStatus);
        ScrollView scroll = new ScrollView(this);
        startGrid = new GridLayout(this);
        scroll.addView(startGrid);
        body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        View divider = new View(this); divider.setBackgroundColor(0xff3a414c);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
        dividerParams.topMargin = dp(8); body.addView(divider, dividerParams);
        LinearLayout footer = row();
        footer.addView(text(prefs.getString("profileName", "King Nation") + "\nLocal device", 12, WHITE), new LinearLayout.LayoutParams(0, dp(50), 1));
        footer.addView(action("Settings", () -> showSettings("System")));
        body.addView(footer);
        startWindow.setBody(body);
        workspace.addView(startWindow);
        startWindow.fit();
        startWindow.requestFocus();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int f) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { renderStart(); }
            @Override public void afterTextChanged(Editable e) {}
        });
        startGrid.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> { if (r-l != or-ol) renderStart(); });
        renderStart();
        startButton.setBackground(panel(0xff414957, 7));
    }

    private void renderStart() {
        if (startGrid == null || search == null) return;
        startGrid.removeAllViews();
        int width = startGrid.getWidth() > 0 ? startGrid.getWidth() : Math.max(dp(180), workspace.getWidth() - dp(52));
        int cols = AppRules.columns(Math.round(width / getResources().getDisplayMetrics().density), 6);
        startGrid.setColumnCount(cols);
        int count = 0;
        for (AppEntry app : visibleApps()) {
            if (!AppRules.matches(app.label, app.component.getPackageName(), search.getText().toString())) continue;
            LinearLayout tile = appTile(app.label, null, WHITE, app.icon, app);
            tile.setOnClickListener(v -> { startWindow.minimize(); launch(app); });
            tile.setOnLongClickListener(v -> { appMenu(tile, app); return true; });
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0; p.height = dp(94); p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            p.setGravity(Gravity.FILL_HORIZONTAL);
            startGrid.addView(tile, p); count++;
        }
        appStatus.setText(count == 0 ? (loading ? "Loading apps…" : "No matching apps. Try Refresh.")
                : "Long press an app to pin it to the desktop.");
    }

    private void showSettings(String section) {
        if (startWindow != null) startWindow.minimize();
        settingsSection = section;
        if (settingsWindow != null) { settingsWindow.restore(); renderSettings(); return; }
        settingsWindow = new ShellWindow("Settings", "⚙", true, () -> { settingsWindow = null; settingsContent = null; });
        LinearLayout body = row();
        body.setGravity(Gravity.TOP);
        ScrollView navScroll = new ScrollView(this);
        LinearLayout navigation = column();
        navigation.setPadding(dp(8), dp(8), dp(6), dp(8));
        TextView account = text(prefs.getString("profileName", "King Nation") + "\nLocal profile", 13, WHITE);
        account.setPadding(dp(7), dp(10), dp(3), dp(20));
        navigation.addView(account);
        sidebarItems.clear();
        String[] labels = {"System", "Bluetooth & devices", "Personalize", "Apps", "Storage", "Time & language", "Privacy & security", "About"};
        String[] symbols = {"▣", "ᛒ", "✦", "▦", "▰", "◷", "◇", "ⓘ"};
        for (int i=0; i<labels.length; i++) {
            String title = labels[i];
            TextView item = action(symbols[i] + "  " + title, () -> { settingsSection = title; renderSettings(); });
            item.setTag(title); sidebarItems.add(item);
            item.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            item.setTextSize(12);
            item.setMinHeight(dp(52));
            navigation.addView(item, new LinearLayout.LayoutParams(-1, -2));
        }
        navScroll.addView(navigation);
        body.addView(navScroll, new LinearLayout.LayoutParams(0, -1, .31f));
        ScrollView contentScroll = new ScrollView(this);
        settingsContent = column();
        settingsContent.setPadding(dp(8), dp(8), dp(10), dp(20));
        contentScroll.addView(settingsContent);
        body.addView(contentScroll, new LinearLayout.LayoutParams(0, -1, .69f));
        settingsWindow.setBody(body);
        workspace.addView(settingsWindow);
        settingsWindow.fit();
        settingsWindow.requestFocus();
        settingsButton.setBackground(panel(0xff414957, 7));
        renderSettings();
    }

    private void renderSettings() {
        if (settingsContent == null) return;
        settingsContent.removeAllViews();
        for (TextView item : sidebarItems) {
            item.setBackground(settingsSection.equals(item.getTag()) ? panel(0xff343438, 5) : selectable());
            item.setTextColor(settingsSection.equals(item.getTag()) ? BLUE : WHITE);
        }
        TextView title = text(settingsSection, 24, WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, dp(4), 0, dp(18));
        settingsContent.addView(title);
        switch (settingsSection) {
            case "System":
                LinearLayout device = row();
                FrameLayout preview = new FrameLayout(this);
                preview.setBackground(panel(0xff06090e, 7));
                DesktopBackdrop waves = new DesktopBackdrop(this);
                FrameLayout.LayoutParams wavesP = new FrameLayout.LayoutParams(-1, -1);
                wavesP.setMargins(dp(4), dp(4), dp(4), dp(4)); preview.addView(waves, wavesP);
                LinearLayout.LayoutParams previewP = new LinearLayout.LayoutParams(dp(70), dp(54));
                previewP.rightMargin = dp(10); device.addView(preview, previewP);
                device.addView(text(Build.MODEL + "\nAndroid " + Build.VERSION.RELEASE, 13, WHITE), new LinearLayout.LayoutParams(0, -2, 1));
                settingsContent.addView(device);
                card("Display", "Brightness, resolution and screen size", () -> open(new Intent(Settings.ACTION_DISPLAY_SETTINGS)));
                card("Storage", "Device space and documents", () -> { settingsSection="Storage"; renderSettings(); });
                card("Default launcher", "Choose your Android home screen", this::defaultHome);
                card("Personalize", "Wallpaper and desktop appearance", () -> { settingsSection="Personalize"; renderSettings(); });
                break;
            case "Bluetooth & devices":
                card("Bluetooth", "Pair headphones, keyboards and mice", () -> open(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
                card("Wi-Fi", "Manage wireless networks", () -> open(new Intent(Settings.ACTION_WIFI_SETTINGS)));
                card("Connected devices", "Open Android connection settings", () -> open(new Intent(Settings.ACTION_WIRELESS_SETTINGS)));
                break;
            case "Personalize":
                card("Launcher modes & home options", "Desktop, Classic, Minimal and Holo Sphere", this::openOptions);
                card("Profile name", prefs.getString("profileName", "King Nation"), this::renameProfile);
                card("Blue waves", "Use the built-in NationDroid wallpaper", () -> { prefs.edit().putBoolean("wallpaper", false).apply(); recreate(); });
                card("System wallpaper", "Show your current Android wallpaper", () -> { prefs.edit().putBoolean("wallpaper", true).apply(); recreate(); });
                card("Choose wallpaper", "Open Android's wallpaper picker", () -> open(new Intent(Intent.ACTION_SET_WALLPAPER)));
                card("Desktop shortcuts", "Long press an app in Start to pin it", () -> { settingsWindow.minimize(); toggleStart(); });
                break;
            case "Apps":
                card("Installed apps", apps.size() + " launchable activities on this device", () -> { settingsWindow.minimize(); toggleStart(); });
                card("Manage applications", "Permissions, storage and uninstall", () -> open(new Intent(Settings.ACTION_APPLICATION_SETTINGS)));
                card("Refresh apps", "Reload names and icons", this::loadApps);
                break;
            case "Storage":
                StatFs storage = new StatFs(getFilesDir().getAbsolutePath());
                card("Device storage", Formatter.formatFileSize(this, storage.getAvailableBytes()) + " available of "
                        + Formatter.formatFileSize(this, storage.getTotalBytes()), () -> open(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)));
                card("Browse files", "Open Android's document browser", this::openFiles);
                break;
            case "Time & language":
                card("Date & time", "Time zone and clock format", () -> open(new Intent(Settings.ACTION_DATE_SETTINGS)));
                card("Language", "Android system languages", () -> open(new Intent(Settings.ACTION_LOCALE_SETTINGS)));
                card("Keyboard", "Manage input methods", () -> open(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
                break;
            case "Privacy & security":
                card("Security", "Open Android security settings", () -> open(new Intent(Settings.ACTION_SECURITY_SETTINGS)));
                card("Local preferences", "Pins stay on this device. No account or analytics. The built-in browser uses internet access.",
                        () -> new AlertDialog.Builder(this).setTitle("Local preferences")
                                .setMessage("NationDroid stores pinned app identifiers and wallpaper settings in app-private storage. Clearing app storage removes them. Android confirms uninstall requests.")
                                .setPositiveButton("OK", null).show());
                break;
            default:
                card("NationDroid 1.6.0", "Independent native Android desktop launcher", () -> {});
                card("Android " + Build.VERSION.RELEASE, Build.MANUFACTURER + " " + Build.MODEL, () -> open(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)));
                break;
        }
        TextView note = text("NationDroid is an independent launcher. Settings outside this panel open in Android.", 11, MUTED);
        note.setPadding(dp(3), dp(18), dp(3), 0); settingsContent.addView(note);
    }

    private void card(String title, String subtitle, Runnable click) {
        LinearLayout card = column();
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(panel(0xff2d2d30, 5));
        TextView heading = text(title + "  ›", 14, WHITE);
        TextView detail = text(subtitle, 11, MUTED);
        card.addView(heading); card.addView(detail);
        card.setOnClickListener(v -> click.run()); card.setFocusable(true);
        card.setContentDescription(title + ". " + subtitle);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(8); settingsContent.addView(card, p);
    }

    private void renameProfile() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(prefs.getString("profileName", "King Nation"));
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(64)});
        new AlertDialog.Builder(this).setTitle("Local profile name").setView(input)
                .setNegativeButton("Cancel", null).setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) { prefs.edit().putString("profileName", name).apply(); recreate(); }
                }).show();
    }

    private void desktopMenu() {
        new AlertDialog.Builder(this).setTitle("Desktop")
                .setItems(new String[]{"Open Start menu", "Launcher settings", "Set as default launcher", "Refresh apps"}, (d, i) -> {
                    if (i==0) toggleStart(); else if (i==1) openOptions();
                    else if (i==2) defaultHome(); else loadApps();
                }).show();
    }

    private void loadApps() {
        if (loading) { reloadRequested=true; return; }
        loading=true;
        worker.execute(() -> {
            List<AppEntry> found = new ArrayList<>();
            boolean failed=false;
            try {
                PackageManager pm=getPackageManager();
                Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                Set<String> seen=new HashSet<>();
                for (ResolveInfo info:pm.queryIntentActivities(query, 0)) {
                    if (info.activityInfo==null || getPackageName().equals(info.activityInfo.packageName)) continue;
                    ComponentName component=new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                    if (seen.add(component.flattenToString())) found.add(new AppEntry(info.loadLabel(pm).toString(),component,info.loadIcon(pm),info.activityInfo.applicationInfo.category));
                }
                Collator collator=Collator.getInstance(); found.sort((a,b)->collator.compare(a.label,b.label));
            } catch (RuntimeException e) { failed=true; }
            boolean error=failed;
            handler.post(() -> {
                loading=false;
                if (isDestroyed()) return;
                if (!error) { apps.clear(); apps.addAll(found); renderDesktop(); renderStart(); }
                else toast("Couldn't load apps. Try Refresh in Settings.");
                if (reloadRequested) { reloadRequested=false; loadApps(); }
            });
        });
    }

    private void appMenu(View anchor, AppEntry app) {
        PopupMenu menu=new PopupMenu(this,anchor);
        menu.getMenu().add(favorites.contains(app.key()) ? "Remove from desktop" : "Pin to desktop").setOnMenuItemClickListener(item -> {
            if (!favorites.remove(app.key())) favorites.add(app.key());
            prefs.edit().putStringSet("favorites",new HashSet<>(favorites)).apply(); renderDesktop(); return true;
        });
        menu.getMenu().add("App info").setOnMenuItemClickListener(item -> {
            open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+app.component.getPackageName())));return true;
        });
        menu.getMenu().add("Uninstall").setOnMenuItemClickListener(item -> {
            open(new Intent(Intent.ACTION_DELETE,Uri.parse("package:"+app.component.getPackageName())));return true;
        });
        menu.show();
    }
    private void launch(AppEntry app) {
        if (prefs.getStringSet("hidden", new HashSet<>()).contains(app.key())) return;
        if (prefs.getStringSet("locked", new HashSet<>()).contains(app.key())) {
            android.app.KeyguardManager keyguard = (android.app.KeyguardManager)getSystemService(KEYGUARD_SERVICE);
            Intent confirm = keyguard.createConfirmDeviceCredentialIntent("Open " + app.label, "Confirm your device credential to launch this app from NationDroid.");
            if (confirm == null) { toast("Set up a device screen lock to open apps with launch confirmation."); return; }
            pendingLaunch = app;
            try { startActivityForResult(confirm,200); } catch (android.content.ActivityNotFoundException | SecurityException e) { pendingLaunch=null; toast("Device confirmation is unavailable."); }
            return;
        }
        launchConfirmed(app);
    }
    private void launchConfirmed(AppEntry app) {
        ActivityHistoryActivity.record(this,"App open requested: "+app.label);
        open(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
    }
    private void openFiles() {
        Intent documents=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        try { startActivityForResult(documents,100); }
        catch (android.content.ActivityNotFoundException | SecurityException e) { toast("No document browser is available on this device."); }
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if (request==200) {
            AppEntry app=pendingLaunch; pendingLaunch=null;
            if (result==RESULT_OK && app!=null && !prefs.getStringSet("hidden",new HashSet<>()).contains(app.key())) launchConfirmed(app);
            return;
        }
        if (request==100 && result==RESULT_OK && data!=null && data.getData()!=null) {
            String type=getContentResolver().getType(data.getData());
            open(new Intent(Intent.ACTION_VIEW).setDataAndType(data.getData(), type==null ? "application/octet-stream" : type)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        }
    }
    private void defaultHome() {
        if (Build.VERSION.SDK_INT>=29) {
            RoleManager roles=getSystemService(RoleManager.class);
            if (roles!=null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roles.isRoleHeld(RoleManager.ROLE_HOME)) { toast("NationDroid is your default launcher.");return; }
                open(roles.createRequestRoleIntent(RoleManager.ROLE_HOME));return;
            }
        }
        open(new Intent(Settings.ACTION_HOME_SETTINGS));
    }
    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (android.content.ActivityNotFoundException | SecurityException e) { toast("Android couldn't open that action."); }
    }
    private void backToDesktop() {
        if (startWindow!=null && startWindow.getVisibility()==View.VISIBLE) startWindow.minimize();
        else if (settingsWindow!=null && settingsWindow.getVisibility()==View.VISIBLE) settingsWindow.minimize();
        hideKeyboard();
    }
    private void hideKeyboard() {
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(screen.getWindowToken(),0);
        screen.clearFocus();
    }
    private int dp(int v) { return Math.round(v*getResources().getDisplayMetrics().density); }
    private void toast(String value) { Toast.makeText(this,value,Toast.LENGTH_LONG).show(); }
    private LinearLayout column() { LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v; }
    private LinearLayout row() { LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v; }
    private TextView text(String value,int size,int color) {
        TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setTypeface(LauncherStyle.font(prefs));v.setGravity(Gravity.CENTER_VERTICAL);return v;
    }
    private TextView action(String label,Runnable click) {
        TextView v=text(label,13,WHITE);v.setGravity(Gravity.CENTER);v.setPadding(dp(8),dp(8),dp(8),dp(8));
        v.setMinHeight(dp(48));v.setFocusable(true);v.setBackground(selectable());v.setOnClickListener(w->click.run());
        v.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName("android.widget.Button");
            }
        });return v;
    }
    private TextView iconButton(String glyph,String description,int size) {
        TextView v=action(glyph,()->{});v.setTextSize(size);v.setContentDescription(description);
        String kind = "▦".equals(glyph) ? "start" : "▰".equals(glyph) ? "files" : "⚙".equals(glyph) ? "settings" : null;
        if (kind != null) {
            v.setText(""); ShellIcon icon = new ShellIcon(kind); icon.setBounds(0, 0, dp(28), dp(28));
            v.setCompoundDrawables(icon, null, null, null);
            v.setPadding(dp(12), dp(6), dp(12), dp(6));
        }
        return v;
    }
    private Drawable selectable() {
        android.util.TypedValue v=new android.util.TypedValue();getTheme().resolveAttribute(android.R.attr.selectableItemBackground,v,true);return getDrawable(v.resourceId);
    }
    private Drawable panel(int color,int radius) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }

    private final class ShellWindow extends LinearLayout {
        final boolean settings;
        final Runnable onClose;
        private final FrameLayout content;
        private boolean maximized;
        private int[] restoreRect;
        private float downX,downY;
        private int originalX,originalY,originalW,originalH;
        ShellWindow(String title,String symbol,boolean settings,Runnable closed) {
            super(MainActivity.this);this.settings=settings;onClose=closed;
            setOrientation(VERTICAL);setBackground(panel(0xff212121,9));setElevation(dp(14));
            setClipToOutline(true);setFocusableInTouchMode(true);
            LinearLayout bar=row();bar.setPadding(dp(5),0,dp(3),0);bar.setBackgroundColor(0xff242426);
            TextView caption=text(symbol+"  "+title,12,WHITE);caption.setPadding(dp(7),0,0,0);
            bar.addView(caption,new LinearLayout.LayoutParams(0,dp(44),1));
            TextView minimize=iconButton("−","Minimize "+title,19);minimize.setOnClickListener(v->minimize());
            TextView maximize=iconButton("□","Maximize or restore "+title,17);maximize.setOnClickListener(v->toggleMaximize());
            TextView close=iconButton("×","Close "+title,21);close.setOnClickListener(v->{workspace.removeView(this);onClose.run();updateTaskbar();hideKeyboard();});
            bar.addView(minimize,new LinearLayout.LayoutParams(dp(40),dp(44)));
            bar.addView(maximize,new LinearLayout.LayoutParams(dp(40),dp(44)));
            bar.addView(close,new LinearLayout.LayoutParams(dp(40),dp(44)));
            addView(bar);
            content=new FrameLayout(MainActivity.this);addView(content,new LinearLayout.LayoutParams(-1,0,1));
            TextView grip=text("◢",12,MUTED);grip.setGravity(Gravity.RIGHT);grip.setContentDescription("Resize window by dragging");
            grip.setPadding(0,0,dp(5),0);addView(grip,new LinearLayout.LayoutParams(-1,dp(18)));
            caption.setOnTouchListener((v,e)->drag(e,false));
            grip.setOnTouchListener((v,e)->drag(e,true));
            setOnClickListener(v->bringToFront());
        }
        void setBody(View v) { content.addView(v,new FrameLayout.LayoutParams(-1,-1)); }
        private void toggleMaximize() {
            FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)getLayoutParams();
            if (p==null) return;
            if (!maximized) {
                restoreRect=new int[]{p.width,p.height,p.leftMargin,p.topMargin};
                maximized=true;
            } else {
                maximized=false;
                if (restoreRect!=null) {
                    p.width=restoreRect[0];p.height=restoreRect[1];p.leftMargin=restoreRect[2];p.topMargin=restoreRect[3];
                    setLayoutParams(p);
                }
            }
            fit();
        }
        void fit() {
            int w=Math.max(1,workspace.getWidth()), h=Math.max(1,workspace.getHeight());
            FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)getLayoutParams();
            if (p==null || p.width<=0) {
                p=new FrameLayout.LayoutParams(Math.min(w-dp(6),dp(settings?880:580)),
                        Math.min(h-dp(8),dp(settings?780:540)));
                p.leftMargin=Math.max(0,(w-p.width)/2);
                p.topMargin=settings?dp(4):Math.max(0,h-p.height-dp(4));
            }
            if (maximized) { p.width=w;p.height=h;p.leftMargin=0;p.topMargin=0; }
            bound(p,w,h);setLayoutParams(p);
        }
        private void bound(FrameLayout.LayoutParams p,int w,int h) {
            p.width=WindowRules.size(p.width,dp(270),w);
            p.height=WindowRules.size(p.height,dp(200),h);
            p.leftMargin=WindowRules.position(p.leftMargin,p.width,w);
            p.topMargin=WindowRules.position(p.topMargin,p.height,h);
        }
        private boolean drag(MotionEvent e,boolean resize) {
            FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)getLayoutParams();
            if (p==null) return true;
            if (e.getActionMasked()==MotionEvent.ACTION_DOWN) {
                bringToFront();downX=e.getRawX();downY=e.getRawY();originalX=p.leftMargin;originalY=p.topMargin;originalW=p.width;originalH=p.height;return true;
            }
            if (e.getActionMasked()==MotionEvent.ACTION_MOVE) {
                if (maximized) return true;
                int dx=Math.round(e.getRawX()-downX),dy=Math.round(e.getRawY()-downY);
                if (resize) {p.width=originalW+dx;p.height=originalH+dy;}
                else {p.leftMargin=originalX+dx;p.topMargin=originalY+dy;}
                bound(p,workspace.getWidth(),workspace.getHeight());setLayoutParams(p);return true;
            }
            if (e.getActionMasked()==MotionEvent.ACTION_UP) {performClick();return true;}
            return true;
        }
        void minimize() {setVisibility(GONE);hideKeyboard();updateTaskbar();}
        void restore() {setVisibility(VISIBLE);bringToFront();fit();updateTaskbar();}
    }
    private List<AppEntry> visibleApps() {
        Set<String> hidden=prefs.getStringSet("hidden",new HashSet<>());
        int category=prefs.getInt("category",-2);
        List<AppEntry> visible=new ArrayList<>();
        for(AppEntry app:apps) if(!hidden.contains(app.key()) && (category==-2 || (category==-1?app.category<0 || app.category>7:app.category==category)))visible.add(app);
        return visible;
    }
    private void openOptions() { open(new Intent(this,LauncherOptionsActivity.class)); }
    private void animateHome() {
        if(workspace==null)return;
        String animation=prefs.getString("animation","fade");
        if("fade".equals(animation)){workspace.setAlpha(.25f);workspace.animate().alpha(1f).setDuration(220).start();}
        else if("zoom".equals(animation)){workspace.setScaleX(.94f);workspace.setScaleY(.94f);workspace.animate().scaleX(1f).scaleY(1f).setDuration(240).start();}
    }
    private void invalidateTree(View v) {
        if(v==null)return;v.invalidate();
        if(v instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)v;for(int i=0;i<group.getChildCount();i++)invalidateTree(group.getChildAt(i));}
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if(lockGesture!=null) {
            android.view.accessibility.AccessibilityManager accessibility=(android.view.accessibility.AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE);
            boolean enabled=Build.VERSION.SDK_INT>=28 && resumed && prefs.getBoolean("doubleTapLock",false)
                    && (accessibility==null || !accessibility.isTouchExplorationEnabled());
            if(!enabled)lockGesture.reset();
            else if(event.getPointerCount()!=1)lockGesture.reset();
            else {
                float x=event.getRawX(),y=event.getRawY();
                switch(event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: lockGesture.down(event.getEventTime(),x,y,isEmptyHomeAt(x,y)); break;
                    case MotionEvent.ACTION_MOVE: lockGesture.move(x,y); break;
                    case MotionEvent.ACTION_UP:
                        if(lockGesture.up(event.getEventTime(),x,y,isEmptyHomeAt(x,y))) {
                            if(!LockScreenService.requestLock() && !NationDeviceAdmin.lock(this))toast("Enable NationDroid's Accessibility lock service or optional Device Admin locking.");
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL: lockGesture.reset(); break;
                    default: break;
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
    private boolean isEmptyHomeAt(float x,float y) {
        if(workspace==null || (startWindow!=null && startWindow.getVisibility()==View.VISIBLE)
                || (settingsWindow!=null && settingsWindow.getVisibility()==View.VISIBLE))return false;
        View welcome=workspace.findViewWithTag("welcome-banner");
        if(welcome!=null&&welcome.getVisibility()==View.VISIBLE){welcome.getLocationOnScreen(touchLocation);if(x>=touchLocation[0]&&x<touchLocation[0]+welcome.getWidth()&&y>=touchLocation[1]&&y<touchLocation[1]+welcome.getHeight())return false;}
        View home=workspace.findViewWithTag("desktop-icons");
        return home!=null && emptyPoint(home,x,y);
    }
    private boolean emptyPoint(View view,float x,float y) {
        if(view.getVisibility()!=View.VISIBLE)return false;
        view.getLocationOnScreen(touchLocation);
        float localX=x-touchLocation[0],localY=y-touchLocation[1];
        if(localX<0 || localY<0 || localX>=view.getWidth() || localY>=view.getHeight())return false;
        if(view instanceof HoloSphereView)return ((HoloSphereView)view).isEmptyAt(localX,localY);
        if(view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group=(android.view.ViewGroup)view;
            for(int i=group.getChildCount()-1;i>=0;i--) {
                View child=group.getChildAt(i);
                child.getLocationOnScreen(touchLocation);
                if(child.getVisibility()==View.VISIBLE && x>=touchLocation[0] && y>=touchLocation[1]
                        && x<touchLocation[0]+child.getWidth() && y<touchLocation[1]+child.getHeight()) {
                    if(!emptyPoint(child,x,y))return false;
                    break;
                }
            }
        }
        // Scroll containers may support a desktop context menu; blank content is still empty.
        boolean scroll=view instanceof ScrollView || view instanceof android.widget.HorizontalScrollView;
        return scroll || (!view.isClickable() && !view.isLongClickable());
    }
    private void showBootWelcome() {
        if(!resumed||isDestroyed()||workspace==null)return;
        int boot=Settings.Global.getInt(getContentResolver(),Settings.Global.BOOT_COUNT,-1);
        if(!WelcomeRules.shouldShow(prefs.getBoolean("welcomeEnabled",true),boot,prefs.getInt("lastWelcomeBoot",-1),prefs.getBoolean("welcomePending",true)))return;
        prefs.edit().putInt("lastWelcomeBoot",boot).putBoolean("welcomePending",false).apply();
        View old=workspace.findViewWithTag("welcome-banner");if(old!=null)workspace.removeView(old);
        LiveBootView greeting=new LiveBootView(this,WelcomeRules.message(prefs.getString("welcomeMessage",WelcomeRules.DEFAULT),prefs.getString("profileName","King Nation")));
        greeting.setTag("welcome-banner");greeting.setElevation(dp(30));
        workspace.addView(greeting,new FrameLayout.LayoutParams(-1,-1));
        greeting.setOnClickListener(v->workspace.removeView(greeting));handler.postDelayed(()->{if(greeting.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)greeting.getParent()).removeView(greeting);},6500);
    }
    private void updateTaskbar() {
        startButton.setBackground(startWindow!=null && startWindow.getVisibility()==View.VISIBLE?panel(0xff414957,7):selectable());
        settingsButton.setBackground(settingsWindow!=null && settingsWindow.getVisibility()==View.VISIBLE?panel(0xff414957,7):selectable());
    }
    @Override protected void onResume() {
        super.onResume(); resumed=true;
        if (prefs.getInt("revision",0)!=optionsRevision) { recreate(); return; }
        if (homeModes!=null) homeModes.setActive(true);
        handler.removeCallbacks(tick);handler.post(tick);loadApps();
        animateHome();handler.removeCallbacks(welcomeTask);handler.postDelayed(welcomeTask,450);
    }
    @Override protected void onPause() {resumed=false;handler.removeCallbacks(welcomeTask);if(workspace!=null){View welcome=workspace.findViewWithTag("welcome-banner");if(welcome!=null)workspace.removeView(welcome);}if(lockGesture!=null)lockGesture.reset();if(homeModes!=null)homeModes.setActive(false);handler.removeCallbacks(tick);super.onPause();}
    @Override protected void onNewIntent(Intent intent) {super.onNewIntent(intent);setIntent(intent);if(startWindow!=null)startWindow.minimize();if(settingsWindow!=null)settingsWindow.minimize();handlePinning(intent);}
    private void handlePinning(Intent intent) {
        if(intent.getBooleanExtra(DeviceControlsActivity.STOP_PINNING,false)) {
            intent.removeExtra(DeviceControlsActivity.STOP_PINNING);
            new AlertDialog.Builder(this).setTitle("Exit screen pinning?")
                    .setMessage("Stop pinning the NationDroid task? Android's standard unpin gesture is also available.")
                    .setNegativeButton("Cancel",null).setPositiveButton("Exit pinning",(d,w)->{
                        try{stopLockTask();toast("Screen pinning exit requested.");}catch(SecurityException|IllegalStateException e){toast("Use Android's standard unpin gesture to exit.");}
                    }).show();
        }
        if(intent.getBooleanExtra(DeviceControlsActivity.PIN_HOME,false)) {
            intent.removeExtra(DeviceControlsActivity.PIN_HOME);
            new AlertDialog.Builder(this).setTitle("Confirm screen-pinning request")
                    .setMessage("Request Android's user-confirmed pinning for NationDroid? You can exit using Android's unpin gesture. This does not create a managed kiosk.")
                    .setNegativeButton("Cancel",null).setPositiveButton("Request pinning",(d,w)->{
                        try{startLockTask();}catch(SecurityException|IllegalStateException e){toast("Enable screen pinning in Android security settings, then try again.");}
                    }).show();
        }
    }
    @Override public void onBackPressed() {backToDesktop();}
    @Override protected void onDestroy() {if(registered){unregisterReceiver(packages);unregisterReceiver(badgesChanged);}handler.removeCallbacksAndMessages(null);worker.shutdownNow();super.onDestroy();}
    static final class AppEntry {
        final String label;final ComponentName component;final Drawable icon;final int category;
        AppEntry(String label,ComponentName component,Drawable icon,int category) {this.label=label;this.component=component;this.icon=icon;this.category=category;}
        String key() {return component.flattenToString();}
    }
}
