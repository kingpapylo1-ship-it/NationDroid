package com.nationdroid.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

final class StyledIconView extends View {
    private final MainActivity.AppEntry app;
    private final SharedPreferences prefs;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    StyledIconView(Context context,MainActivity.AppEntry app,SharedPreferences prefs){super(context);this.app=app;this.prefs=prefs;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);int size=Math.min(getWidth(),getHeight());int save=c.save();c.clipPath(LauncherStyle.mask(prefs.getString("iconShape","rounded"),0,0,size));app.icon.setBounds(0,0,size,size);app.icon.setAlpha(255);app.icon.draw(c);c.restoreToCount(save);
        if(prefs.getBoolean("badges",false) && NotificationBadges.count(app.component.getPackageName())>0){paint.setColor(LauncherStyle.accent(prefs));c.drawCircle(size*.85f,size*.15f,size*.13f,paint);}
    }
}
