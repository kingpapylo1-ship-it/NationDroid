package com.nationdroid.launcher;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.time.Duration;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35})
public class BrowserWelcomeTest {
    private Application app;
    private SharedPreferences prefs;
    @Before public void reset(){app=RuntimeEnvironment.getApplication();prefs=app.getSharedPreferences("launcher",Context.MODE_PRIVATE);prefs.edit().clear().putBoolean("autoSpin",false).putBoolean("motion",false).commit();}
    @Test public void browserStartsWithSafeSettingsAndLocalFileLinksAreBlocked(){
        try(ActivityController<BrowserActivity> controller=Robolectric.buildActivity(BrowserActivity.class,new Intent(app,BrowserActivity.class).setData(Uri.parse("https://example.com/"))).setup()){
            View root=controller.get().getWindow().getDecorView();WebView web=findWeb(root);assertNotNull(web);assertEquals("https://example.com/",web.getUrl());
            assertTrue(web.getSettings().getJavaScriptEnabled());assertFalse(web.getSettings().getAllowFileAccess());assertFalse(web.getSettings().getAllowContentAccess());assertEquals(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW,web.getSettings().getMixedContentMode());
            assertNotNull(find(root,"Browser menu"));assertNotNull(find(root,"New tab"));
        }
        try(ActivityController<BrowserActivity> controller=Robolectric.buildActivity(BrowserActivity.class,new Intent(app,BrowserActivity.class).setData(Uri.parse("file:///private/contacts"))).setup()){
            assertEquals(BrowserUrls.HOME,findWeb(controller.get().getWindow().getDecorView()).getUrl());
        }
    }
    @Test public void browserCanOpenAndCloseActualTabs(){
        try(ActivityController<BrowserActivity> controller=Robolectric.buildActivity(BrowserActivity.class).setup()){
            View root=controller.get().getWindow().getDecorView();find(root,"New tab").performClick();assertNotNull(find(root,"Close tab 2"));
            find(root,"Close tab 2").performClick();assertNull(find(root,"Close tab 2"));assertNotNull(findWeb(root));
        }
    }
    @Test public void bootReceiverMarksPendingWithoutStartingActivities(){
        new BootWelcomeReceiver().onReceive(app,new Intent(Intent.ACTION_BOOT_COMPLETED));assertTrue(prefs.getBoolean("welcomePending",false));assertNull(shadowOf(app).getNextStartedActivity());
    }
    @Test public void welcomeShowsOncePerBootAndCanBeDisabled(){
        Settings.Global.putInt(app.getContentResolver(),Settings.Global.BOOT_COUNT,8);prefs.edit().putString("profileName","King").putString("welcomeMessage","Hello {name}").commit();
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));View root=controller.get().getWindow().getDecorView();assertNotNull(root.findViewWithTag("welcome-banner"));assertEquals(8,prefs.getInt("lastWelcomeBoot",-1));
        }
        new BootWelcomeReceiver().onReceive(app,new Intent(Intent.ACTION_BOOT_COMPLETED));
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));assertNull(controller.get().getWindow().getDecorView().findViewWithTag("welcome-banner"));
        }
        Settings.Global.putInt(app.getContentResolver(),Settings.Global.BOOT_COUNT,9);prefs.edit().putBoolean("welcomeEnabled",false).commit();
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));assertNull(controller.get().getWindow().getDecorView().findViewWithTag("welcome-banner"));assertEquals(8,prefs.getInt("lastWelcomeBoot",-1));
        }
    }
    private static WebView findWeb(View v){if(v instanceof WebView)return (WebView)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){WebView result=findWeb(g.getChildAt(i));if(result!=null)return result;}}return null;}
    private static View find(View v,String label){if(label.equals(v.getContentDescription()))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View result=find(g.getChildAt(i),label);if(result!=null)return result;}}return null;}
}
