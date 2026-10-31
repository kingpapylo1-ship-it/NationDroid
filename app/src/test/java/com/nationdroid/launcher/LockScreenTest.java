package com.nationdroid.launcher;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.MotionEvent;
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

@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35})
public class LockScreenTest {
    private SharedPreferences prefs;
    @Before public void reset(){
        prefs=RuntimeEnvironment.getApplication().getSharedPreferences("launcher",Context.MODE_PRIVATE);
        prefs.edit().clear().putBoolean("autoSpin",false).putBoolean("motion",false).putString("animation","none").commit();
    }
    @Test public void lockRequiresConnectedServiceAndExplicitToggle(){
        assertFalse(LockScreenService.requestLock());
        ServiceController<LockScreenService> controller=Robolectric.buildService(LockScreenService.class).create();
        LockScreenService service=controller.get();
        try{
            service.onServiceConnected();assertTrue(LockScreenService.isConnected());
            assertFalse(LockScreenService.requestLock());assertTrue(shadowOf(service).getGlobalActionsPerformed().isEmpty());
            prefs.edit().putBoolean("doubleTapLock",true).commit();
            assertTrue(LockScreenService.requestLock());
            assertEquals(Integer.valueOf(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN),shadowOf(service).getGlobalActionsPerformed().get(0));
            prefs.edit().putBoolean("doubleTapLock",false).commit();assertFalse(LockScreenService.requestLock());
            assertEquals(1,shadowOf(service).getGlobalActionsPerformed().size());
        }finally{controller.destroy();}
        assertFalse(LockScreenService.isConnected());
    }
    @Test public void doubleTapOnEmptyDesktopRequestsLockButShortcutTapsDoNot(){
        prefs.edit().putBoolean("doubleTapLock",true).commit();
        ServiceController<LockScreenService> serviceController=Robolectric.buildService(LockScreenService.class).create();
        LockScreenService service=serviceController.get();service.onServiceConnected();
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
            MainActivity activity=controller.get();View root=activity.getWindow().getDecorView();
            root=activity.findViewById(android.R.id.content);
            for(int pass=0;pass<3;pass++) {
                forceLayout(root);
                root.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY));root.layout(0,0,1080,1920);
            }
            View home=root.findViewWithTag("desktop-icons");assertNotNull(home);assertTrue("Home must be laid out: " + dimensions(home),home.getWidth()>0&&home.getHeight()>0);int[] xy=new int[2];home.getLocationOnScreen(xy);
            float x=xy[0]+home.getWidth()*.80f,y=xy[1]+home.getHeight()*.65f;long t=SystemClock.uptimeMillis()+100;
            tap(activity,t,x,y);assertTrue(shadowOf(service).getGlobalActionsPerformed().isEmpty());
            tap(activity,t+150,x,y);assertEquals(1,shadowOf(service).getGlobalActionsPerformed().size());
            View browser=find(root,"Browser");assertNotNull(browser);browser.getLocationOnScreen(xy);
            x=xy[0]+browser.getWidth()/2f;y=xy[1]+browser.getHeight()/2f;
            tap(activity,t+600,x,y);tap(activity,t+750,x,y);
            assertEquals(1,shadowOf(service).getGlobalActionsPerformed().size());
            android.view.accessibility.AccessibilityManager manager=(android.view.accessibility.AccessibilityManager)activity.getSystemService(Context.ACCESSIBILITY_SERVICE);
            shadowOf(manager).setTouchExplorationEnabled(true);
            home.getLocationOnScreen(xy);x=xy[0]+home.getWidth()*.80f;y=xy[1]+home.getHeight()*.65f;
            tap(activity,t+1100,x,y);tap(activity,t+1250,x,y);
            assertEquals("TalkBack double taps must not lock",1,shadowOf(service).getGlobalActionsPerformed().size());
        }finally{serviceController.destroy();}
    }
    @Test public void alternateHomesAlsoAcceptEmptySpaceDoubleTaps(){
        prefs.edit().putBoolean("doubleTapLock",true).commit();
        ServiceController<LockScreenService> serviceController=Robolectric.buildService(LockScreenService.class).create();
        LockScreenService service=serviceController.get();service.onServiceConnected();
        try {
            int expected=0;
            for(String mode:new String[]{"classic","minimal","sphere"}) {
                prefs.edit().putString("mode",mode).commit();
                try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
                    MainActivity activity=controller.get();View root=activity.findViewById(android.R.id.content);
                    for(int pass=0;pass<3;pass++){forceLayout(root);root.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY));root.layout(0,0,1080,1920);}
                    View home=root.findViewWithTag("desktop-icons");assertTrue(home.getWidth()>0&&home.getHeight()>0);
                    int[] xy=new int[2];home.getLocationOnScreen(xy);float x=xy[0]+home.getWidth()*.8f,y=xy[1]+home.getHeight()*.65f;
                    long t=SystemClock.uptimeMillis()+100;tap(activity,t,x,y);tap(activity,t+150,x,y);
                    assertEquals("Double tap should work in "+mode,++expected,shadowOf(service).getGlobalActionsPerformed().size());
                }
            }
        }finally{serviceController.destroy();}
    }
    @Test public void settingIsAvailableWithSeparateAccessibilitySetup(){
        try(ActivityController<LauncherOptionsActivity> controller=Robolectric.buildActivity(LauncherOptionsActivity.class).setup()){
            View root=controller.get().getWindow().getDecorView();assertNotNull(find(root,"Double-tap to lock"));assertNotNull(find(root,"Accessibility lock service"));
            assertFalse(prefs.getBoolean("doubleTapLock",false));
        }
    }
    private static void forceLayout(View view){view.forceLayout();if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)forceLayout(group.getChildAt(i));}}
    private static String dimensions(View view){String result="";while(view!=null){result+=view.getClass().getSimpleName()+":"+view.getWidth()+"x"+view.getHeight()+" ";view=view.getParent() instanceof View?(View)view.getParent():null;}return result;}
    private static void tap(MainActivity activity,long t,float x,float y){
        MotionEvent down=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,x,y,0),up=MotionEvent.obtain(t,t+40,MotionEvent.ACTION_UP,x,y,0);
        activity.dispatchTouchEvent(down);activity.dispatchTouchEvent(up);down.recycle();up.recycle();
    }
    private static View find(View root,String label){
        if(root instanceof TextView && ((TextView)root).getText().toString().contains(label))return root;
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),label);if(found!=null)return found;}}
        return null;
    }
}
