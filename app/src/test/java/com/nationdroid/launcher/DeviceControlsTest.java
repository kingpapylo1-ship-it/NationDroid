package com.nationdroid.launcher;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.accessibilityservice.AccessibilityService;
import android.app.Application;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35})
public class DeviceControlsTest {
    private Application app;
    private SharedPreferences prefs;
    @Before public void reset(){app=RuntimeEnvironment.getApplication();prefs=app.getSharedPreferences("launcher",Context.MODE_PRIVATE);prefs.edit().clear().putBoolean("motion",false).putBoolean("autoSpin",false).commit();}
    @Test public void accessibilityShortcutsRequireOptInAndRejectUnlistedActions(){
        assertFalse(LockScreenService.requestUserAction(AccessibilityService.GLOBAL_ACTION_HOME));
        ServiceController<LockScreenService> controller=Robolectric.buildService(LockScreenService.class).create();LockScreenService service=controller.get();
        try{
            service.onServiceConnected();assertFalse(LockScreenService.requestUserAction(AccessibilityService.GLOBAL_ACTION_HOME));
            prefs.edit().putBoolean("accessibilityControls",true).commit();
            int[] actions={AccessibilityService.GLOBAL_ACTION_HOME,AccessibilityService.GLOBAL_ACTION_BACK,AccessibilityService.GLOBAL_ACTION_RECENTS,AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS,AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS,AccessibilityService.GLOBAL_ACTION_POWER_DIALOG,AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN};
            for(int action:actions)assertTrue(LockScreenService.requestUserAction(action));
            assertFalse(LockScreenService.requestUserAction(99999));assertFalse(LockScreenService.requestUserAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT));
            assertEquals(actions.length,shadowOf(service).getGlobalActionsPerformed().size());
            prefs.edit().putBoolean("accessibilityControls",false).commit();assertFalse(LockScreenService.requestUserAction(AccessibilityService.GLOBAL_ACTION_HOME));
        }finally{controller.destroy();}
    }
    @Test public void adminIsOptionalRevocableAndRequestsOnlyForceLockPolicy()throws Exception{
        DevicePolicyManager manager=(DevicePolicyManager)app.getSystemService(Context.DEVICE_POLICY_SERVICE);
        assertFalse(NationDeviceAdmin.active(app));assertFalse(NationDeviceAdmin.lock(app));
        shadowOf(manager).setActiveAdmin(NationDeviceAdmin.component(app));assertTrue(NationDeviceAdmin.active(app));assertTrue(NationDeviceAdmin.lock(app));
        manager.removeActiveAdmin(NationDeviceAdmin.component(app));assertFalse(NationDeviceAdmin.active(app));assertFalse(NationDeviceAdmin.lock(app));
        ResolveInfo resolve=new ResolveInfo();resolve.activityInfo=app.getPackageManager().getReceiverInfo(NationDeviceAdmin.component(app),PackageManager.GET_META_DATA);
        DeviceAdminInfo info=new DeviceAdminInfo(app,resolve);
        int policies=0;for(int policy=0;policy<16;policy++)if(info.usesPolicy(policy))policies++;assertEquals(1,policies);assertTrue(info.usesPolicy(DeviceAdminInfo.USES_POLICY_FORCE_LOCK));assertFalse(info.usesPolicy(DeviceAdminInfo.USES_POLICY_WIPE_DATA));assertFalse(info.usesPolicy(DeviceAdminInfo.USES_POLICY_RESET_PASSWORD));
    }
    @Test public void controlsPageExposesNavigationAndExplainsUserConfirmedPinning(){
        try(ActivityController<DeviceControlsActivity> controller=Robolectric.buildActivity(DeviceControlsActivity.class).setup()){
            View root=controller.get().getWindow().getDecorView();
            for(String text:new String[]{"Home","Back","Recents","Open notification shade","Open quick settings","Open power menu","Lock device now","Pin NationDroid home","Exit screen pinning"})assertNotNull(find(root,text));
            find(root,"Pin NationDroid home").performClick();assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());
            assertFalse(prefs.getBoolean("accessibilityControls",false));
        }
    }
    @Test public void pinAndExitIntentsRequireLocalConfirmation(){
        Intent request=new Intent(app,MainActivity.class).putExtra(DeviceControlsActivity.PIN_HOME,true);
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class,request).setup()){
            android.app.AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);assertTrue(dialog.isShowing());
            assertNotNull(dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE));assertFalse(controller.get().getIntent().hasExtra(DeviceControlsActivity.PIN_HOME));
            dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick();
        }
        Intent exit=new Intent(app,MainActivity.class).putExtra(DeviceControlsActivity.STOP_PINNING,true);
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class,exit).setup()){
            assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());assertFalse(controller.get().getIntent().hasExtra(DeviceControlsActivity.STOP_PINNING));
        }
    }
    private static View find(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().equals(text))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),text);if(found!=null)return found;}}return null;}
}
