package com.nationdroid.launcher;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;

/** Optional legacy Device Admin with exactly one policy: locking the screen. */
public final class NationDeviceAdmin extends DeviceAdminReceiver {
    static ComponentName component(Context context){return new ComponentName(context,NationDeviceAdmin.class);}
    static boolean active(Context context){
        DevicePolicyManager manager=(DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return manager!=null&&manager.isAdminActive(component(context));
    }
    static boolean lock(Context context){
        DevicePolicyManager manager=(DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if(manager==null||!manager.isAdminActive(component(context)))return false;
        try{manager.lockNow();return true;}catch(SecurityException ignored){return false;}
    }
}
