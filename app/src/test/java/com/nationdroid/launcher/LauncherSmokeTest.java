package com.nationdroid.launcher;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.json.JSONObject;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35})
public class LauncherSmokeTest {
    private SharedPreferences prefs;
    @Before public void reset() {
        prefs=RuntimeEnvironment.getApplication().getSharedPreferences("launcher",Context.MODE_PRIVATE);
        prefs.edit().clear().putBoolean("autoSpin",false).putBoolean("motion",false).commit();
    }
    @Test public void allHomeModesCreateAndCompleteLifecycle() {
        for(String mode:new String[]{"desktop","classic","minimal","sphere"}) {
            prefs.edit().putString("mode",mode).commit();
            try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
                View root=controller.get().getWindow().getDecorView();
                root.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY));
                root.layout(0,0,1080,1920);
                assertNotNull(find(root,"Open launcher settings"));
                if("sphere".equals(mode))assertNotNull(findType(root,HoloSphereView.class));
                if("minimal".equals(mode)||"classic".equals(mode))assertNotNull(find(root,"Search apps"));
            }
        }
    }
    @Test public void modeSelectionWaitsForApply() {
        try(ActivityController<LauncherOptionsActivity> controller=Robolectric.buildActivity(LauncherOptionsActivity.class).setup()) {
            View root=controller.get().getWindow().getDecorView();
            View mode=find(root,"Launcher mode");assertNotNull(mode);((View)mode.getParent()).performClick();
            View sphere=find(root,"Holo Sphere");assertNotNull(sphere);((View)sphere.getParent()).performClick();
            assertEquals("desktop",prefs.getString("mode","desktop"));
            find(root,"Apply now").performClick();assertEquals("sphere",prefs.getString("mode","desktop"));
        }
    }
    @Test public void homeOptionsBuildPreviewAndControls() {
        try(ActivityController<LauncherOptionsActivity> controller=Robolectric.buildActivity(LauncherOptionsActivity.class).setup()) {
            View root=controller.get().getWindow().getDecorView();
            View home=find(root,"Home screen options");assertNotNull(home);((View)home.getParent()).performClick();
            assertNotNull(find(root,"Auto spin"));assertNotNull(find(root,"Motion sensing"));
            assertNotNull(find(root,"Scale"));assertNotNull(find(root,"Bottom dock"));
        }
    }
    @Test public void backupRoundTripsPinsAndRejectsInvalidSettingsBeforeApplying() throws Exception {
        prefs.edit().putString("mode","sphere").putInt("scale",80).putStringSet("favorites",Collections.singleton("com.example/.Main")).commit();
        JSONObject backup=PreferenceBackup.export(prefs);
        JSONObject values=PreferenceBackup.validate(backup.toString());
        prefs.edit().clear().commit();PreferenceBackup.apply(prefs,values);
        assertEquals("sphere",prefs.getString("mode","desktop"));
        assertTrue(prefs.getStringSet("favorites",Collections.emptySet()).contains("com.example/.Main"));
        values.put("scale",500);
        try { PreferenceBackup.apply(prefs,values);fail("Invalid slider must be rejected"); }catch(IllegalArgumentException expected){}
        assertEquals(80,prefs.getInt("scale",0));
    }
    private static View find(View root,String value) {
        if(root.getContentDescription()!=null&&root.getContentDescription().toString().equals(value))return root;
        if(root instanceof TextView){TextView text=(TextView)root;if(text.getText().toString().contains(value)||(text.getHint()!=null&&text.getHint().toString().contains(value)))return root;}
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View result=find(group.getChildAt(i),value);if(result!=null)return result;}}
        return null;
    }
    private static View findType(View root,Class<?> type) {
        if(type.isInstance(root))return root;
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View result=findType(group.getChildAt(i),type);if(result!=null)return result;}}
        return null;
    }
}
