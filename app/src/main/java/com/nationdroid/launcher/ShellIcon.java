package com.nationdroid.launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Small native icons sized in the desktop's coordinate system. */
public final class ShellIcon extends Drawable {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String kind;
    public ShellIcon(String kind) {this.kind=kind;}
    @Override public void draw(Canvas canvas) {
        Rect b=getBounds();canvas.save();canvas.translate(b.left,b.top);canvas.scale(b.width()/48f,b.height()/48f);
        switch(kind) {
            case "start":
                p.setColor(0xff4abafa);canvas.drawRect(3,3,23,23,p);canvas.drawRect(25,3,45,23,p);
                canvas.drawRect(3,25,23,45,p);canvas.drawRect(25,25,45,45,p);break;
            case "files":
                p.setColor(0xffd99a1d);canvas.drawRoundRect(2,9,23,39,3,3,p);
                p.setColor(0xffffd457);canvas.drawRoundRect(2,15,46,42,3,3,p);
                p.setColor(0xffeebc36);canvas.drawRect(2,17,46,21,p);
                p.setColor(0xff208ed7);canvas.drawRoundRect(16,32,33,42,3,3,p);
                p.setColor(0xff0056a3);canvas.drawRect(20,37,30,42,p);break;
            case "device":
                p.setColor(0xff8998a9);canvas.drawRoundRect(2,5,46,35,2,2,p);
                p.setColor(0xff1e9dcc);canvas.drawRect(4,7,44,32,p);
                p.setColor(0xff4fd5ed);canvas.drawRect(5,8,27,30,p);
                p.setColor(0xffaebdca);canvas.drawRect(22,35,26,42,p);canvas.drawRect(15,42,33,44,p);break;
            case "settings":
                p.setColor(0xffa2abb7);
                for(int i=0;i<8;i++){canvas.save();canvas.rotate(i*45,24,24);canvas.drawRoundRect(19,2,29,16,2,2,p);canvas.restore();}
                canvas.drawCircle(24,24,17,p);p.setColor(0xff29323f);canvas.drawCircle(24,24,10,p);
                p.setColor(0xff6bc8fc);canvas.drawCircle(24,24,6,p);break;
            default:
                p.setColor(0xff65c5ff);canvas.drawCircle(24,24,21,p);
                p.setColor(0xff153e9b);canvas.drawCircle(24,24,15,p);
                p.setColor(0xff65c5ff);canvas.drawOval(17,4,31,44,p);
                p.setColor(0xff153e9b);canvas.drawOval(20,6,28,42,p);
                p.setColor(0xff65c5ff);canvas.drawRect(4,22,44,26,p);break;
        }
        canvas.restore();
    }
    @Override public void setAlpha(int alpha) {p.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter) {p.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity() {return PixelFormat.TRANSLUCENT;}
}
