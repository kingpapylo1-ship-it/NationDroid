package com.nationdroid.launcher;

import android.app.Notification;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.HashMap;
import java.util.Map;

/** Keeps aggregate active notification counts in memory; never retains content or writes it to disk. */
public final class NotificationBadges extends NotificationListenerService {
    public static final String ACTION="com.nationdroid.launcher.BADGES_CHANGED";
    private static final Map<String,Integer> COUNTS=new HashMap<>();
    private android.content.SharedPreferences preferences;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener preferenceChange=(p,key)->{if("badges".equals(key))refresh();};
    @Override public void onCreate(){super.onCreate();preferences=getSharedPreferences("launcher",MODE_PRIVATE);preferences.registerOnSharedPreferenceChangeListener(preferenceChange);}
    @Override public void onDestroy(){preferences.unregisterOnSharedPreferenceChangeListener(preferenceChange);super.onDestroy();}
    public static synchronized int count(String packageName){return COUNTS.getOrDefault(packageName,0);}
    @Override public void onListenerConnected(){refresh();}
    @Override public void onNotificationPosted(StatusBarNotification n){refresh();}
    @Override public void onNotificationRemoved(StatusBarNotification n){refresh();}
    @Override public void onListenerDisconnected(){synchronized(NotificationBadges.class){COUNTS.clear();}changed();}
    private void refresh(){
        Map<String,Integer> next=new HashMap<>();
        if(getSharedPreferences("launcher",MODE_PRIVATE).getBoolean("badges",false))try{
            StatusBarNotification[] active=getActiveNotifications();
            if(active!=null)for(StatusBarNotification n:active)if((n.getNotification().flags & Notification.FLAG_GROUP_SUMMARY)==0)next.put(n.getPackageName(),next.getOrDefault(n.getPackageName(),0)+1);
        }catch(SecurityException ignored){}
        synchronized(NotificationBadges.class){COUNTS.clear();COUNTS.putAll(next);}changed();
    }
    private void changed(){sendBroadcast(new Intent(ACTION).setPackage(getPackageName()),"com.nationdroid.launcher.INTERNAL");}
}
