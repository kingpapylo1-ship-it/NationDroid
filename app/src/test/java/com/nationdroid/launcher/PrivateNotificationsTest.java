package com.nationdroid.launcher;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.Manifest;
import android.app.Application;
import android.app.Notification;
import android.app.Person;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
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
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.util.ReflectionHelpers;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35})
public class PrivateNotificationsTest {
    private Application app;
    private SharedPreferences prefs;
    @Before public void setup(){
        app=RuntimeEnvironment.getApplication();prefs=app.getSharedPreferences("launcher",Context.MODE_PRIVATE);
        prefs.edit().clear().commit();PrivateNotificationStore.clear();shadowOf(app).denyPermissions(Manifest.permission.READ_CONTACTS);
        FakeContacts provider=new FakeContacts();ProviderInfo info=new ProviderInfo();info.authority="com.android.contacts";provider.attachInfo(app,info);
        ShadowContentResolver.registerProviderInternal("com.android.contacts",provider);
    }
    @Test public void knownSenderNeedsContactsPermissionAndUnknownPhoneNeverLeaks(){
        ContactNameLookup lookup=new ContactNameLookup(app);
        Notification known=message("+2348012345678","tel:+2348012345678","secret body 445566");
        assertEquals(SenderPrivacy.UNKNOWN,NotificationSender.display(known,ContactNameLookup.allowed(app),lookup));
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS);
        assertEquals("Dolapo",NotificationSender.display(known,true,lookup));
        Notification unknown=message("+2348099999999","tel:+2348099999999","another secret");
        assertEquals(SenderPrivacy.UNKNOWN,NotificationSender.display(unknown,true,lookup));
        assertEquals("Chioma",lookup.byContactUri("content://com.android.contacts/contacts/42"));
    }
    @Test public void latestUnknownSenderDoesNotReuseOlderKnownNameOrConversationTitle(){
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS);
        Notification n=message("Dolapo","tel:+2348012345678","older secret");
        Parcelable[] first=n.extras.getParcelableArray(Notification.EXTRA_MESSAGES);
        Notification second=message("+2348099999999","tel:+2348099999999","new secret");
        n.extras.putParcelableArray(Notification.EXTRA_MESSAGES,new Parcelable[]{first[0],second.extras.getParcelableArray(Notification.EXTRA_MESSAGES)[0]});
        n.extras.putCharSequence(Notification.EXTRA_TITLE,"Dolapo");
        assertEquals(SenderPrivacy.UNKNOWN,NotificationSender.display(n,true,new ContactNameLookup(app)));
    }
    @Test public void genericNotificationTitleAndBodyAreNeverUsedAsVisibleContent(){
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS);
        Notification n=new Notification();n.extras=new Bundle();n.extras.putString(Notification.EXTRA_TITLE,"Dolapo");n.extras.putString(Notification.EXTRA_TEXT,"private content +2348099999999");
        assertEquals(SenderPrivacy.UNKNOWN,NotificationSender.display(n,true,new ContactNameLookup(app)));
        n.category=Notification.CATEGORY_MESSAGE;
        assertEquals("Dolapo",NotificationSender.display(n,true,new ContactNameLookup(app)));
    }
    @Test public void snapshotsFailClosedOnPermissionRevocationAndPanelOptOut(){
        prefs.edit().putBoolean("privateNotifications",true).commit();
        PrivateNotificationStore.replace(Collections.singletonList(new PrivateNotificationStore.Entry("example.chat","Chat","Dolapo",1)));
        assertEquals(SenderPrivacy.UNKNOWN,PrivateNotificationStore.snapshot(app).get(0).sender);
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS);
        assertEquals("Dolapo",PrivateNotificationStore.snapshot(app).get(0).sender);
        shadowOf(app).denyPermissions(Manifest.permission.READ_CONTACTS);
        assertEquals(SenderPrivacy.UNKNOWN,PrivateNotificationStore.snapshot(app).get(0).sender);
        prefs.edit().putBoolean("privateNotifications",false).commit();assertTrue(PrivateNotificationStore.snapshot(app).isEmpty());
    }
    @Test public void actualListenerAndPanelShowOnlyAppAndMatchedNameAndDoNotCancelOriginalNotification() throws Exception {
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS);prefs.edit().putBoolean("privateNotifications",true).commit();
        ServiceController<NotificationBadges> serviceController=Robolectric.buildService(NotificationBadges.class).create();NotificationBadges service=serviceController.get();
        try {
            shadowOf(service).addActiveNotification("example.chat",1,message("+2348012345678","tel:+2348012345678","TOP SECRET BODY"));
            shadowOf(service).addActiveNotification("example.chat",2,message("+2348099999999","tel:+2348099999999","SECOND SECRET BODY"));
            service.onListenerConnected();drain(service);
            assertEquals(2,PrivateNotificationStore.snapshot(app).size());
            try(ActivityController<PrivateNotificationsActivity> controller=Robolectric.buildActivity(PrivateNotificationsActivity.class).setup()){
                drain(service);View root=controller.get().getWindow().getDecorView();
                assertNotNull(find(root,"Dolapo"));assertNotNull(find(root,SenderPrivacy.UNKNOWN));assertNotNull(find(root,"example.chat"));
                assertNull(find(root,"TOP SECRET BODY"));assertNull(find(root,"SECOND SECRET BODY"));assertNull(find(root,"+234"));
                assertEquals(2,service.getActiveNotifications().length);
            }
            service.onListenerDisconnected();assertTrue(PrivateNotificationStore.snapshot(app).isEmpty());
        }finally{serviceController.destroy();}
    }
    private static void drain(NotificationBadges service)throws Exception{
        ExecutorService executor=ReflectionHelpers.getField(service,"worker");executor.submit(()->{}).get(10,TimeUnit.SECONDS);shadowOf(Looper.getMainLooper()).idle();
    }
    private static Notification message(String name,String uri,String body){
        Person person=new Person.Builder().setName(name).setUri(uri).build();Bundle sender=new Bundle();sender.putParcelable("sender_person",person);sender.putCharSequence("sender",name);sender.putString("text",body);
        Notification n=new Notification();n.category=Notification.CATEGORY_MESSAGE;n.extras=new Bundle();n.extras.putParcelableArray(Notification.EXTRA_MESSAGES,new Parcelable[]{sender});n.extras.putString(Notification.EXTRA_TEXT,body);n.extras.putString(Notification.EXTRA_TITLE,name);return n;
    }
    private static View find(View root,String text){
        if(root instanceof TextView&&((TextView)root).getText().toString().contains(text))return root;
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),text);if(found!=null)return found;}}return null;
    }
    public static class FakeContacts extends ContentProvider {
        @Override public boolean onCreate(){return true;}
        @Override public Cursor query(Uri uri,String[] projection,String selection,String[] arguments,String sort){
            String value=null;
            if(uri.getPath().startsWith("/phone_lookup/")&&"+2348012345678".equals(uri.getLastPathSegment()))value="Dolapo";
            else if(uri.getPath().equals("/contacts/42"))value="Chioma";
            else if(arguments!=null&&arguments.length>0&&"Dolapo".equalsIgnoreCase(arguments[0]))value="Dolapo";
            MatrixCursor cursor=new MatrixCursor(projection);if(value!=null)cursor.addRow(new Object[]{value});return cursor;
        }
        @Override public String getType(Uri uri){return null;}
        @Override public Uri insert(Uri uri,ContentValues values){return null;}
        @Override public int delete(Uri uri,String selection,String[] args){return 0;}
        @Override public int update(Uri uri,ContentValues values,String selection,String[] args){return 0;}
    }
}
