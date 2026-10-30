package com.nationdroid.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import java.lang.ref.WeakReference;

/** Optional user-enabled service used for user-invoked, allowlisted Android global actions. */
public final class LockScreenService extends AccessibilityService {
    private static WeakReference<LockScreenService> connected=new WeakReference<>(null);
    public static boolean isConnected(){return connected.get()!=null;}
    public static boolean requestLock(){
        LockScreenService service=connected.get();
        if(Build.VERSION.SDK_INT<28 || service==null || !service.getSharedPreferences("launcher",MODE_PRIVATE).getBoolean("doubleTapLock",false))return false;
        return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    }
    public static boolean requestUserAction(int action){
        LockScreenService service=connected.get();
        if(service==null || !service.getSharedPreferences("launcher",MODE_PRIVATE).getBoolean("accessibilityControls",false))return false;
        boolean allowed=action==GLOBAL_ACTION_HOME||action==GLOBAL_ACTION_BACK||action==GLOBAL_ACTION_RECENTS
                ||action==GLOBAL_ACTION_NOTIFICATIONS||action==GLOBAL_ACTION_QUICK_SETTINGS||action==GLOBAL_ACTION_POWER_DIALOG;
        if(Build.VERSION.SDK_INT>=28 && action==GLOBAL_ACTION_LOCK_SCREEN)allowed=true;
        if(!allowed)return false;
        return service.performGlobalAction(action);
    }
    @Override protected void onServiceConnected(){super.onServiceConnected();connected=new WeakReference<>(this);}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){/* No event subscription or screen inspection. */}
    @Override public void onInterrupt(){}
    private void clearConnection(){if(connected.get()==this)connected.clear();}
    @Override public boolean onUnbind(Intent intent){clearConnection();return super.onUnbind(intent);}
    @Override public void onDestroy(){clearConnection();super.onDestroy();}
}
