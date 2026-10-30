package com.nationdroid.launcher;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import java.text.DateFormat;
import java.util.*;

/** User-visible, local history. No screen, message or keystroke capture. */
public final class ActivityHistoryActivity extends Activity {
    private LinearLayout content;
    static void record(Context c,String action){
        android.content.SharedPreferences p=c.getSharedPreferences("activity_history",0);
        if(!p.getBoolean("enabled",false))return;
        String line=System.currentTimeMillis()+"|"+action.replace("\n"," ")+"\n";
        String old=p.getString("events","");
        String[] rows=(line+old).split("\n");StringBuilder out=new StringBuilder();
        for(int i=0;i<Math.min(rows.length,500);i++)out.append(rows[i]).append('\n');
        p.edit().putString("events",out.toString()).apply();
    }
    @Override public void onCreate(Bundle b){super.onCreate(b);}
    @Override protected void onResume(){super.onResume();render();}
    private void text(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(16);t.setPadding(12,12,12,12);content.addView(t);}
    private void button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setOnClickListener(v->r.run());content.addView(b);}
    private void render(){
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);setContentView(scroll);
        android.content.SharedPreferences p=getSharedPreferences("activity_history",0);
        text("Activity history — on this device only");
        text("Optional launcher app-open requests (up to 500 entries) and Android app usage from the last 24 hours. This cannot record everything inside apps. No messages, passwords, screen contents or keystrokes are collected. Usage entries are read from Android only when this page opens.");
        Switch enabled=new Switch(this);enabled.setText("Keep NationDroid activity history");enabled.setChecked(p.getBoolean("enabled",false));content.addView(enabled);
        enabled.setOnCheckedChangeListener((v,on)->{p.edit().putBoolean("enabled",on).apply();render();});
        button("Grant or revoke Android Usage Access",()->startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        button("Delete NationDroid history",()->{p.edit().remove("events").apply();render();});
        text("Deletion clears NationDroid records only. Android controls its own usage history. Turn off logging and revoke Usage Access to stop both sources here.");
        if(!p.getBoolean("enabled",false)){text("History is off.");return;}
        text("NationDroid app-open requests");
        for(String row:p.getString("events","").split("\n")){
            int split=row.indexOf('|');if(split<0)continue;
            try{text(DateFormat.getDateTimeInstance().format(new Date(Long.parseLong(row.substring(0,split))))+" — "+row.substring(split+1));}catch(NumberFormatException ignored){}
        }
        text("Android app usage — last 24 hours (latest 300 events)");
        AppOpsManager ops=(AppOpsManager)getSystemService(APP_OPS_SERVICE);
        if(ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),getPackageName())!=AppOpsManager.MODE_ALLOWED){text("Usage Access is not granted.");return;}
        try{
            long now=System.currentTimeMillis();UsageEvents events=((UsageStatsManager)getSystemService(USAGE_STATS_SERVICE)).queryEvents(now-86400000L,now);
            ArrayList<String> rows=new ArrayList<>();UsageEvents.Event e=new UsageEvents.Event();
            while(events!=null&&events.hasNextEvent()){events.getNextEvent(e);int type=e.getEventType();
                if(type!=1&&type!=2)continue;
                String name=e.getPackageName();try{name=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(name,0)).toString();}catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
                rows.add(DateFormat.getDateTimeInstance().format(new Date(e.getTimeStamp()))+" — "+name+(type==1?" entered foreground":" left foreground"));if(rows.size()>300)rows.remove(0);
            }
            Collections.reverse(rows);for(String row:rows)text(row);
            if(rows.isEmpty())text("No app usage events available.");
        }catch(SecurityException ex){text("Usage Access is unavailable or was revoked.");}
    }
}
