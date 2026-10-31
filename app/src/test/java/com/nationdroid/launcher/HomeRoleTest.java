package com.nationdroid.launcher;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.role.RoleManager;
import android.content.Intent;
import android.provider.Settings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
public class HomeRoleTest {
    @Test @Config(sdk = 35, shadows = AvailableHomeRole.class)
    public void homeRoleChooserReceivesCallingActivity() throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            requestHome(activity);
            ShadowActivity.IntentForResult request = shadowOf(activity).getNextStartedActivityForResult();
            assertNotNull(request);
            assertEquals("test.REQUEST_HOME_ROLE", request.intent.getAction());
            assertTrue("A result request is required to provide the calling package", request.requestCode >= 0);
            assertEquals(RoleManager.ROLE_HOME, request.intent.getStringExtra("role"));
        }
    }

    @Test @Config(sdk = 28)
    public void olderAndroidOpensHomeSettings() throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            requestHome(activity);
            assertEquals(Settings.ACTION_HOME_SETTINGS, shadowOf(activity).getNextStartedActivity().getAction());
        }
    }

    private static void requestHome(MainActivity activity) throws Exception {
        java.lang.reflect.Method method = MainActivity.class.getDeclaredMethod("defaultHome");
        method.setAccessible(true);
        method.invoke(activity);
    }

    @Implements(RoleManager.class)
    public static class AvailableHomeRole {
        @Implementation protected boolean isRoleAvailable(String role) { return RoleManager.ROLE_HOME.equals(role); }
        @Implementation protected boolean isRoleHeld(String role) { return false; }
        @Implementation protected Intent createRequestRoleIntent(String role) {
            return new Intent("test.REQUEST_HOME_ROLE").putExtra("role", role);
        }
    }
}
