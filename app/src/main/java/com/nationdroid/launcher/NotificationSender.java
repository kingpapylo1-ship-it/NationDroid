package com.nationdroid.launcher;

import android.app.Notification;
import android.app.Person;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;

/** Extracts sender identifiers only; never reads message text, images or remote views. */
final class NotificationSender {
    private NotificationSender() {}
    static String display(Notification notification,boolean contactsAllowed,SenderPrivacy.Contacts lookup){
        if(!contactsAllowed||notification==null)return SenderPrivacy.UNKNOWN;
        try {
            Bundle extras=notification.extras;if(extras==null)return SenderPrivacy.UNKNOWN;
            Parcelable[] messages=extras.getParcelableArray(Notification.EXTRA_MESSAGES);
            if(messages!=null&&messages.length>0){
                Parcelable last=messages[messages.length-1];if(!(last instanceof Bundle))return SenderPrivacy.UNKNOWN;
                Bundle message=(Bundle)last;
                if(Build.VERSION.SDK_INT>=28){
                    Parcelable value=message.getParcelable("sender_person");
                    if(value instanceof Person){Person sender=(Person)value;return SenderPrivacy.display(sender.getUri(),string(sender.getName()),true,lookup);}
                }
                return SenderPrivacy.display(null,string(message.getCharSequence("sender")),true,lookup);
            }
            // Generic notification titles may be message content. Use a title as a sender
            // candidate only for message/call categories, and display only a contacts match.
            if(Notification.CATEGORY_MESSAGE.equals(notification.category)||Notification.CATEGORY_CALL.equals(notification.category))
                return SenderPrivacy.display(null,string(extras.getCharSequence(Notification.EXTRA_TITLE)),true,lookup);
        }catch(RuntimeException ignored){}
        return SenderPrivacy.UNKNOWN;
    }
    private static String string(CharSequence value){return value==null?null:value.toString();}
}
