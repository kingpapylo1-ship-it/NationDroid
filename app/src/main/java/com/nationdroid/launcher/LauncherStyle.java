package com.nationdroid.launcher;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Typeface;

final class LauncherStyle {
    private LauncherStyle() {}
    static boolean dark(SharedPreferences p) {return p.getBoolean("dark", true);}
    static int ink(SharedPreferences p) {return dark(p)?0xfff5f6fc:0xff161c2c;}
    static int card(SharedPreferences p) {return dark(p)?0xff242934:0xfffafbff;}
    static int accent(SharedPreferences p) {return p.getInt("accent",0xff2766e9);}
    static Typeface font(SharedPreferences p) {
        String family=p.getString("font","sans-serif");
        return Typeface.create(family,Typeface.NORMAL);
    }
    static Path mask(String shape,float left,float top,float size) {
        Path path=new Path();
        fillMask(path,shape,left,top,size);
        return path;
    }
    static void fillMask(Path path,String shape,float left,float top,float size) {
        path.reset();
        if ("circle".equals(shape)) path.addCircle(left+size/2,top+size/2,size/2,Path.Direction.CW);
        else if ("heptagon".equals(shape)) {
            for(int i=0;i<7;i++) {
                double angle=-Math.PI/2+i*Math.PI*2/7;
                float x=left+size/2+(float)Math.cos(angle)*size/2;
                float y=top+size/2+(float)Math.sin(angle)*size/2;
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            path.close();
        } else path.addRoundRect(left,top,left+size,top+size,size*.20f,size*.20f,Path.Direction.CW);
    }
}
