package com.nationdroid.launcher;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Visible, user-invoked controls. Never provisions device ownership or traps the user. */
public final class DeviceControlsActivity extends Activity {
    static final String PIN_HOME="com.nationdroid.launcher.PIN_HOME";
    static final String STOP_PINNING="com.nationdroid.launcher.STOP_PINNING";
    private SharedPreferences prefs;
    @Override public void onCreate(Bundle state){super.onCreate(state);prefs=getSharedPreferences("launcher",MODE_PRIVATE);}
    @Override protected void onResume(){super.onResume();render();}
    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(LauncherStyle.dark(prefs)?0xff10151f:0xffeef1f9);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());v.setPadding(safe.left,safe.top,safe.right,safe.bottom);}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;
        });setContentView(root);root.requestApplyInsets();
        getWindow().getDecorView().setSystemUiVisibility(LauncherStyle.dark(prefs)?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=button("‹",this::finish);back.setContentDescription("Back");back.setTextSize(28);header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(56)));
        header.addView(text("Device controls",22));root.addView(header);
        ScrollView scroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(8),dp(16),dp(24));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        note(content,"Optional Android controls. These permissions do not provide root, access to another app's private data, or system-wide notification redaction.");
        note(content,"ACCESSIBILITY SHORTCUTS");
        boolean enabled=prefs.getBoolean("accessibilityControls",false);
        content.addView(button(enabled?"Turn Accessibility shortcuts off":"Enable Accessibility shortcuts",()->{
            if(enabled){prefs.edit().putBoolean("accessibilityControls",false).apply();render();}
            else new AlertDialog.Builder(this).setTitle("Enable Accessibility shortcuts?")
                .setMessage("When you tap a control, NationDroid can request Home, Back, Recents, notifications, quick settings, the power menu or screen lock. The service does not read screen contents, capture input, or dispatch gestures. Android Accessibility access remains a separate user choice.")
                .setNegativeButton("Cancel",null).setPositiveButton("Enable",(d,w)->{prefs.edit().putBoolean("accessibilityControls",true).apply();render();}).show();
        }));
        content.addView(button("Accessibility access · "+(LockScreenService.isConnected()?"connected":"not connected"),()->open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
        action(content,"Home",AccessibilityService.GLOBAL_ACTION_HOME);
        action(content,"Back",AccessibilityService.GLOBAL_ACTION_BACK);
        action(content,"Recents",AccessibilityService.GLOBAL_ACTION_RECENTS);
        action(content,"Open notification shade",AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
        action(content,"Open quick settings",AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
        action(content,"Open power menu",AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);
        content.addView(button("Lock device now",()->{
            boolean locked=Build.VERSION.SDK_INT>=28&&LockScreenService.requestUserAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
            if(!locked)locked=NationDeviceAdmin.lock(this);
            if(!locked)toast("Enable Accessibility shortcuts and access, or optional Device Admin locking.");
        }));
        note(content,"DEVICE ADMIN — LOCK ONLY");
        note(content,"Optional Device Admin adds a second way to lock. It does not request wipe-data, password-reset, camera-disable or device-owner privileges. Android may require your PIN/password/pattern after an admin lock. Deactivate admin before uninstalling.");
        boolean admin=NationDeviceAdmin.active(this);
        content.addView(button(admin?"Device Admin · active — deactivate":"Enable optional Device Admin locking",()->{
            if(admin)new AlertDialog.Builder(this).setTitle("Deactivate Device Admin?").setMessage("This removes NationDroid's admin locking authority. Accessibility shortcuts can still work if separately enabled.")
                .setNegativeButton("Cancel",null).setPositiveButton("Deactivate",(d,w)->{
                    try{((DevicePolicyManager)getSystemService(DEVICE_POLICY_SERVICE)).removeActiveAdmin(NationDeviceAdmin.component(this));render();}catch(SecurityException e){open(new Intent(Settings.ACTION_SECURITY_SETTINGS));}
                }).show();
            else new AlertDialog.Builder(this).setTitle("Optional Device Admin locking")
                .setMessage("Android will ask you to activate a Device Admin that requests only permission to lock the screen. It cannot read another app's private data or provide full device access. You can deactivate it here or in Android settings.")
                .setNegativeButton("Cancel",null).setPositiveButton("Open Android activation",(d,w)->open(new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,NationDeviceAdmin.component(this))
                    .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,"Optional lock-screen control only. No wipe or password-reset policy is requested."))).show();
        }));
        note(content,"KIOSK-STYLE SCREEN PINNING");
        int state=((ActivityManager)getSystemService(ACTIVITY_SERVICE)).getLockTaskModeState();
        note(content,"Current pinning state: "+(state==ActivityManager.LOCK_TASK_MODE_NONE?"off":state==ActivityManager.LOCK_TASK_MODE_PINNED?"screen pinned":"managed lock-task mode")+". This requests Android's user-confirmed screen pinning, not an enforced device-owner kiosk. Android's standard unpin gesture remains available.");
        content.addView(button("Pin NationDroid home",()->new AlertDialog.Builder(this).setTitle("Pin NationDroid home?")
            .setMessage("Android may ask for confirmation. Screen pinning keeps this task in front and may prevent launching other apps until you unpin. To exit, use Android's unpin gesture (commonly hold Back + Recents, or swipe up and hold with gesture navigation). Your device may then require its unlock credential. This is not a secure managed kiosk.")
            .setNegativeButton("Cancel",null).setPositiveButton("Request screen pinning",(d,w)->open(new Intent(this,MainActivity.class).putExtra(PIN_HOME,true))).show()));
        TextView exit=button("Exit screen pinning",()->open(new Intent(this,MainActivity.class).putExtra(STOP_PINNING,true)));
        exit.setEnabled(state!=ActivityManager.LOCK_TASK_MODE_NONE);exit.setAlpha(exit.isEnabled()?1f:.45f);content.addView(exit);
        content.addView(button("Android security / pinning settings",()->open(new Intent(Settings.ACTION_SECURITY_SETTINGS))));
    }
    private void action(LinearLayout parent,String label,int action){parent.addView(button(label,()->{if(!LockScreenService.requestUserAction(action))toast("Enable Accessibility shortcuts and NationDroid Accessibility access first. Android can also restrict an action while pinned.");}));}
    private TextView button(String label,Runnable action){TextView view=text(label,15);view.setTextColor(LauncherStyle.accent(prefs));view.setMinHeight(dp(48));view.setPadding(dp(12),dp(14),dp(12),dp(14));view.setFocusable(true);view.setOnClickListener(v->action.run());return view;}
    private void note(LinearLayout parent,String value){TextView view=text(value,13);view.setPadding(dp(2),dp(12),dp(2),dp(12));parent.addView(view);}
    private TextView text(String value,int size){TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(LauncherStyle.ink(prefs));view.setTypeface(LauncherStyle.font(prefs));return view;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    private void open(Intent intent){try{startActivity(intent);}catch(android.content.ActivityNotFoundException|SecurityException e){toast("Android couldn't open that action.");}}
}
