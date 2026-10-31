package com.nationdroid.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Marks a welcome for the next foreground home visit; never launches UI from the background. */
public final class BootWelcomeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))context.getSharedPreferences("launcher",Context.MODE_PRIVATE).edit().putBoolean("welcomePending",true).apply();
    }
}
