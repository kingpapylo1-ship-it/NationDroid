package com.nationdroid.launcher;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/** Read-only notification panel: app name and contacts-matched sender only, in every device state. */
public final class PrivateNotificationsActivity extends Activity {
    private SharedPreferences prefs;
    private boolean registered;
    private final BroadcastReceiver updates=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){render();}};
    @Override public void onCreate(Bundle state){super.onCreate(state);prefs=getSharedPreferences("launcher",MODE_PRIVATE);registerUpdates();}
    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerUpdates(){
        IntentFilter filter=new IntentFilter(NotificationBadges.ACTION);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(updates,filter,Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(updates,filter,"com.nationdroid.launcher.INTERNAL",null);
        registered=true;
    }
    @Override protected void onResume(){super.onResume();render();NotificationBadges.requestRefresh();}
    @Override protected void onDestroy(){if(registered)unregisterReceiver(updates);super.onDestroy();}
    private void render(){
        boolean dark=LauncherStyle.dark(prefs);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(dark?0xff10151f:0xffeef1f9);
        root.setOnApplyWindowInsetsListener((view,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());view.setPadding(safe.left,safe.top,safe.right,safe.bottom);}
            else view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;
        });
        setContentView(root);root.requestApplyInsets();
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=button("‹",this::finish);back.setTextSize(28);back.setContentDescription("Back");header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(56)));
        header.addView(text("Private notifications",20),new LinearLayout.LayoutParams(0,dp(60),1));root.addView(header);
        ScrollView scroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(8),dp(16),dp(24));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        note(content,"NationDroid panel only: app name + saved-contact name. Unmatched senders show Unknown sender. Message text, numbers, attachments and contact photos are never displayed here, even while unlocked.");
        boolean enabled=prefs.getBoolean("privateNotifications",false);
        content.addView(button(enabled?"Turn private panel off":"Enable private panel",()->{
            if(enabled){prefs.edit().putBoolean("privateNotifications",false).apply();PrivateNotificationStore.clear();NotificationBadges.requestRefresh();render();}
            else new AlertDialog.Builder(this).setTitle("Enable private notifications?")
                    .setMessage("With Android notification access, NationDroid reads sender identifiers to match them to Contacts only when you also grant Contacts permission. This panel retains only app labels and sanitized matched names in memory. Message content is never shown or saved. Original Android notifications and other apps are unchanged.")
                    .setNegativeButton("Cancel",null).setPositiveButton("Enable",(d,w)->{prefs.edit().putBoolean("privateNotifications",true).apply();NotificationListenerService.requestRebind(new ComponentName(this,NotificationBadges.class));NotificationBadges.requestRefresh();render();}).show();
        }));
        content.addView(button("Notification access · "+(NotificationBadges.isConnected()?"connected":"not connected"),()->open(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))));
        content.addView(button(ContactNameLookup.allowed(this)?"Contacts names · allowed (manage permission)":"Allow Contacts for saved sender names",()->{
            if(ContactNameLookup.allowed(this))open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())));
            else new AlertDialog.Builder(this).setTitle("Match saved contact names")
                    .setMessage("Contacts permission is used locally to match notification sender identifiers to your saved contact names. Numbers and message bodies will not be displayed. Without access, every sender remains Unknown sender.")
                    .setNegativeButton("Cancel",null).setPositiveButton("Continue",(d,w)->requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},310)).show();
        }));
        content.addView(button("Refresh",NotificationBadges::requestRefresh));
        note(content,"ACTIVE NOTIFICATIONS");
        List<PrivateNotificationStore.Entry> entries=PrivateNotificationStore.snapshot(this);
        if(!enabled)note(content,"Private panel is off. Enable it above to show sanitized notifications.");
        else if(!NotificationBadges.isConnected())note(content,"Enable NationDroid under Android Notification access to populate this panel.");
        else if(entries.isEmpty())note(content,"No active notifications available.");
        else for(PrivateNotificationStore.Entry entry:entries){
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(14),dp(16),dp(14));
            android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();background.setColor(LauncherStyle.card(prefs));background.setCornerRadius(dp(12));card.setBackground(background);
            TextView app=text(entry.appName,16);app.setTypeface(LauncherStyle.font(prefs),android.graphics.Typeface.BOLD);
            TextView sender=text(entry.sender,15);sender.setPadding(0,dp(6),0,0);
            card.addView(app);card.addView(sender);card.setContentDescription(entry.appName+". "+entry.sender);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);content.addView(card,p);
        }
        note(content,"ANDROID PRIVACY SETUP");
        note(content,"These links open Android settings; NationDroid cannot change them automatically. Hide lock-screen content in Android, then disable previews or pop-ups in each messaging app to reduce exposure outside this panel. Unlocked notification-shade redaction and contact details inside other apps remain controlled by Android and those apps.");
        content.addView(button("Android notification settings",()->open(new Intent(Build.VERSION.SDK_INT>=33?Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS:Settings.ACTION_SETTINGS))));
        content.addView(button("Lock-screen & security settings",()->open(new Intent(Settings.ACTION_SECURITY_SETTINGS))));
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==310){PrivateNotificationStore.clear();NotificationBadges.requestRefresh();render();
        if(!ContactNameLookup.allowed(this))new AlertDialog.Builder(this).setTitle("Contacts access is off")
                .setMessage("Notifications will still hide message content, but every sender will be Unknown sender. You can grant Contacts permission from NationDroid's Android app settings.")
                .setNegativeButton("Keep unknown senders",null).setPositiveButton("App settings",(d,w)->open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())))).show();
    }}
    private void open(Intent intent){try{startActivity(intent);}catch(android.content.ActivityNotFoundException|SecurityException e){
        try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(android.content.ActivityNotFoundException|SecurityException unavailable){android.widget.Toast.makeText(this,"Android couldn't open settings.",android.widget.Toast.LENGTH_LONG).show();}
    }}
    private void note(LinearLayout parent,String value){TextView view=text(value,13);view.setPadding(dp(2),dp(10),dp(2),dp(14));parent.addView(view);}
    private TextView text(String value,int size){TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(LauncherStyle.ink(prefs));view.setTypeface(LauncherStyle.font(prefs));return view;}
    private TextView button(String title,Runnable action){TextView view=text(title,14);view.setTextColor(LauncherStyle.accent(prefs));view.setPadding(dp(12),dp(14),dp(12),dp(14));view.setMinHeight(dp(48));view.setFocusable(true);view.setOnClickListener(v->action.run());return view;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
