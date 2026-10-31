package com.nationdroid.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

/** Original vector-like blue wave wallpaper; no screenshot or third-party bitmap. */
public final class DesktopBackdrop extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public DesktopBackdrop(Context context) { super(context); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(getWidth()/400f, getHeight()/850f);
        paint.setShader(new LinearGradient(0,0,330,850,new int[]{0xff08152f,0xff143876,0xff050e23},null,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,400,850,paint);
        Path p=new Path();
        p.moveTo(-80,205);p.cubicTo(40,135,230,60,470,155);p.lineTo(470,850);p.lineTo(-80,850);p.close();
        fill(canvas,p,0,120,360,570,new int[]{0xff45dfff,0xff2864f4,0xff07194d});
        p.reset();p.moveTo(-80,260);p.cubicTo(40,90,175,140,470,340);p.lineTo(470,600);p.lineTo(-80,600);p.close();
        fill(canvas,p,50,160,160,520,new int[]{0xff52ebff,0xff339bff,0xff2149de});
        p.reset();p.moveTo(-60,330);p.cubicTo(80,470,285,320,465,410);p.lineTo(465,850);p.lineTo(-60,850);p.close();
        fill(canvas,p,100,350,260,700,new int[]{0xff307bff,0xff1440d0,0xff07152b});
        edge(canvas,-60,330,80,470,285,320,465,410,0xff67dcff);
        p.reset();p.moveTo(-80,530);p.cubicTo(100,320,245,470,470,700);p.lineTo(470,900);p.lineTo(-80,900);p.close();
        fill(canvas,p,90,430,280,830,new int[]{0xff345dd7,0xff0d2a8c,0xff050e23});
        edge(canvas,-80,530,100,320,245,470,470,700,0xff559edb);
        p.reset();p.moveTo(-100,710);p.cubicTo(110,440,250,610,470,850);p.lineTo(470,900);p.lineTo(-100,900);p.close();
        fill(canvas,p,100,580,320,890,new int[]{0xff0b214e,0xff061027,0xff010811});
        edge(canvas,-100,710,110,440,250,610,470,850,0xff345ca8);
        for(int i=0;i<3;i++) edge(canvas,-90,795+i*65,75,590+i*45,220,610+i*55,450,940+i*50,0xff1c3777);
        canvas.restore();
    }
    private void fill(Canvas c,Path p,float x,float y,float endX,float endY,int[] colors) {
        paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(x,y,endX,endY,colors,null,Shader.TileMode.CLAMP));c.drawPath(p,paint);
    }
    private void edge(Canvas c,float x,float y,float a,float b,float d,float e,float f,float g,int color) {
        Path p=new Path();p.moveTo(x,y);p.cubicTo(a,b,d,e,f,g);
        paint.setShader(null);paint.setColor(color);paint.setStrokeWidth(1.2f);paint.setStyle(Paint.Style.STROKE);c.drawPath(p,paint);paint.setStyle(Paint.Style.FILL);
    }
}
