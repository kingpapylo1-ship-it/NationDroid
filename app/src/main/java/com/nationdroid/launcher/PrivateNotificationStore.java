package com.nationdroid.launcher;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Only safe labels are retained, in process memory. No bodies, raw sender identifiers or intents. */
final class PrivateNotificationStore {
    private static final List<Entry> ENTRIES=new ArrayList<>();
    private PrivateNotificationStore(){}
    static synchronized void replace(List<Entry> entries){ENTRIES.clear();ENTRIES.addAll(entries);ENTRIES.sort(Comparator.comparingLong((Entry e)->e.time).reversed());}
    static synchronized void clear(){ENTRIES.clear();}
    static synchronized List<Entry> snapshot(Context context){
        if(!context.getSharedPreferences("launcher",Context.MODE_PRIVATE).getBoolean("privateNotifications",false))return Collections.emptyList();
        boolean allowed=ContactNameLookup.allowed(context);List<Entry> result=new ArrayList<>();
        for(Entry e:ENTRIES)result.add(new Entry(e.packageName,e.appName,allowed?e.sender:SenderPrivacy.UNKNOWN,e.time));
        return result;
    }
    static final class Entry {
        final String packageName,appName,sender;final long time;
        Entry(String packageName,String appName,String sender,long time){this.packageName=packageName;this.appName=appName;this.sender=SenderPrivacy.safeName(sender);this.time=time;}
    }
}
