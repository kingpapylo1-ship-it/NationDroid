package com.nationdroid.launcher;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

final class ContactNameLookup implements SenderPrivacy.Contacts {
    private final Context context;
    ContactNameLookup(Context context){this.context=context;}
    static boolean allowed(Context context){return context.checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED;}
    private String query(Uri uri,String column,String selection,String[] args){
        if(!allowed(context))return null;
        try(Cursor cursor=context.getContentResolver().query(uri,new String[]{column},selection,args,null)){
            return cursor!=null&&cursor.moveToFirst()?cursor.getString(0):null;
        }catch(RuntimeException ignored){return null;}
    }
    @Override public String byNumber(String number){
        Uri uri=Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(number));
        return query(uri,ContactsContract.PhoneLookup.DISPLAY_NAME,null,null);
    }
    @Override public String byName(String name){
        return query(ContactsContract.Contacts.CONTENT_URI,ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY+" = ? COLLATE NOCASE",new String[]{name});
    }
    @Override public String byContactUri(String value){
        Uri uri=Uri.parse(value);
        if(!"content".equals(uri.getScheme())||!ContactsContract.AUTHORITY.equals(uri.getAuthority()))return null;
        java.util.List<String> parts=uri.getPathSegments();
        boolean contactId=parts.size()==2&&"contacts".equals(parts.get(0))&&parts.get(1).matches("[0-9]+");
        boolean lookup=(parts.size()==3||parts.size()==4)&&"contacts".equals(parts.get(0))&&"lookup".equals(parts.get(1));
        if(!contactId&&!lookup)return null;
        return query(uri,ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,null,null);
    }
}
