package com.nationdroid.launcher;

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/** Opt-in counts and sanitized notification metadata only; never stores message bodies. */
public final class NotificationBadges extends NotificationListenerService {
    public static final String ACTION="com.nationdroid.launcher.BADGES_CHANGED";
    private static final Map<String,Integer> COUNTS=new HashMap<>();
    private static WeakReference<NotificationBadges> instance=new WeakReference<>(null);
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicLong generation=new AtomicLong();
    private final Object updateLock=new Object();
    private SharedPreferences preferences;
    private volatile boolean connected,destroyed;
    private boolean observing;
    private final ContentObserver contactsObserver=new ContentObserver(new Handler(Looper.getMainLooper())){
        @Override public void onChange(boolean selfChange){invalidateEntries();refresh();}
    };
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChange=(p,key)->{
        if("badges".equals(key)||"privateNotifications".equals(key)){invalidateEntries();refresh();}
    };
    @Override public void onCreate(){
        super.onCreate();preferences=getSharedPreferences("launcher",MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(preferenceChange);instance=new WeakReference<>(this);
    }
    @Override public void onDestroy(){
        synchronized(updateLock){destroyed=true;connected=false;generation.incrementAndGet();clear();}
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceChange);
        if(observing)getContentResolver().unregisterContentObserver(contactsObserver);
        worker.shutdownNow();clear();if(instance.get()==this)instance.clear();super.onDestroy();
    }
    public static boolean isConnected(){NotificationBadges service=instance.get();return service!=null&&service.connected;}
    public static void requestRefresh(){NotificationBadges service=instance.get();if(service!=null)service.refresh();}
    public static synchronized int count(String packageName){return COUNTS.getOrDefault(packageName,0);}
    @Override public void onListenerConnected(){connected=true;refresh();}
    @Override public void onNotificationPosted(StatusBarNotification n){refresh();}
    @Override public void onNotificationRemoved(StatusBarNotification n){refresh();}
    @Override public void onListenerDisconnected(){
        synchronized(updateLock){connected=false;generation.incrementAndGet();clear();}
        if(observing){getContentResolver().unregisterContentObserver(contactsObserver);observing=false;}
    }
    private void clear(){synchronized(NotificationBadges.class){COUNTS.clear();}PrivateNotificationStore.clear();changed();}
    private void invalidateEntries(){synchronized(updateLock){generation.incrementAndGet();PrivateNotificationStore.clear();changed();}}
    private void refresh(){
        if(destroyed)return;
        final long ticket; synchronized(updateLock){ticket=generation.incrementAndGet();}
        boolean privatePanel=preferences.getBoolean("privateNotifications",false),dots=preferences.getBoolean("badges",false);
        boolean contacts=connected&&privatePanel&&ContactNameLookup.allowed(this);
        if(contacts&&!observing)try{getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI,true,contactsObserver);observing=true;}catch(SecurityException ignored){}
        if(!contacts&&observing){getContentResolver().unregisterContentObserver(contactsObserver);observing=false;}
        if(!connected||(!privatePanel&&!dots)){clear();return;}
        worker.execute(()->{
            if(destroyed||!connected||generation.get()!=ticket)return;
            Map<String,Integer> counts=new HashMap<>();List<PrivateNotificationStore.Entry> entries=new ArrayList<>();
            ContactNameLookup lookup=new ContactNameLookup(this);
            try{
                StatusBarNotification[] active=getActiveNotifications();
                if(active!=null)for(StatusBarNotification n:active){
                    if(destroyed||generation.get()!=ticket)return;
                    if((n.getNotification().flags&Notification.FLAG_GROUP_SUMMARY)!=0)continue;
                    if(dots)counts.put(n.getPackageName(),counts.getOrDefault(n.getPackageName(),0)+1);
                    if(privatePanel&&entries.size()<200){
                        String appName=n.getPackageName();
                        try{appName=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(n.getPackageName(),0)).toString();}catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
                        String sender=NotificationSender.display(n.getNotification(),contacts&&ContactNameLookup.allowed(this),lookup);
                        entries.add(new PrivateNotificationStore.Entry(n.getPackageName(),appName,sender,n.getPostTime()));
                    }
                }
            }catch(RuntimeException ignored){counts.clear();entries.clear();}
            synchronized(updateLock){if(!destroyed&&connected&&generation.get()==ticket){
                synchronized(NotificationBadges.class){COUNTS.clear();COUNTS.putAll(counts);}
                PrivateNotificationStore.replace(entries);changed();
            }}
        });
    }
    private void changed(){sendBroadcast(new Intent(ACTION).setPackage(getPackageName()),"com.nationdroid.launcher.INTERNAL");}
}
