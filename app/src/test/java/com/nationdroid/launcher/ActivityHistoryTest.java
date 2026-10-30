package com.nationdroid.launcher;
import static org.junit.Assert.*;
import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk={28,35})
public class ActivityHistoryTest {
 private Context c; private SharedPreferences p;
 @Before public void reset(){c=RuntimeEnvironment.getApplication();p=c.getSharedPreferences("activity_history",0);p.edit().clear().commit();}
 @Test public void offByDefaultAndPauseStopsRecording(){ActivityHistoryActivity.record(c,"one");assertEquals("",p.getString("events",""));p.edit().putBoolean("enabled",true).commit();ActivityHistoryActivity.record(c,"two");String before=p.getString("events","");assertTrue(before.contains("two"));p.edit().putBoolean("enabled",false).commit();ActivityHistoryActivity.record(c,"three");assertEquals(before,p.getString("events",""));}
 @Test public void recordsBoundedAndNewestFirst(){p.edit().putBoolean("enabled",true).commit();for(int i=0;i<510;i++)ActivityHistoryActivity.record(c,"app "+i);String rows=p.getString("events","");assertEquals(500,rows.split("\n").length);assertTrue(rows.split("\n")[0].endsWith("app 509"));}
 @Test public void pageStartsWithoutUsagePermission(){try(var a=Robolectric.buildActivity(ActivityHistoryActivity.class).setup()){assertNotNull(a.get().getWindow().getDecorView());}p.edit().putBoolean("enabled",true).commit();try(var a=Robolectric.buildActivity(ActivityHistoryActivity.class).setup()){assertNotNull(a.get().getWindow().getDecorView());}}
}
