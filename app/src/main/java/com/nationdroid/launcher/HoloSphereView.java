package com.nationdroid.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Perspective app sphere, rendered with installed app icons and lifecycle-scoped sensors. */
public final class HoloSphereView extends View implements SensorEventListener, Choreographer.FrameCallback {
    private final SharedPreferences prefs;
    private final List<Node> nodes=new ArrayList<>();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.Path iconMask=new android.graphics.Path();
    private static final Comparator<Node> DEPTH=Comparator.comparingDouble(n->n.rotated[2]);
    private final Consumer<MainActivity.AppEntry> launch;
    private final Consumer<MainActivity.AppEntry> menu;
    private final SensorManager sensors;
    private Sensor gravity,shakeSensor;
    private long lastShake;
    private boolean attached,active=true,registered,scheduled,touching,moved,longPressed;
    private double yaw=.3,pitch=.12,velocityX,velocityY;
    private float lastX,lastY,downX,downY,tiltX,tiltY;
    private long lastFrame,lastTouch;
    private MainActivity.AppEntry pressed;
    private final Runnable hold=this::holdApp;
    private void holdApp(){if(pressed!=null && !moved){longPressed=true;menu.accept(pressed);}}

    public HoloSphereView(Context context,List<MainActivity.AppEntry> apps,SharedPreferences prefs,
                          Consumer<MainActivity.AppEntry> launch,Consumer<MainActivity.AppEntry> menu) {
        super(context);this.prefs=prefs;this.launch=launch;this.menu=menu;
        for(int i=0;i<apps.size();i++)nodes.add(new Node(apps.get(i),SphereMath.point(i,apps.size())));
        sensors=(SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        if(sensors!=null){gravity=sensors.getDefaultSensor(Sensor.TYPE_GRAVITY);if(gravity==null)gravity=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);if(gravity!=null&&gravity.getType()!=Sensor.TYPE_ACCELEROMETER)shakeSensor=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);}
        setFocusable(true);setClickable(true);
        setContentDescription("App sphere. Swipe to rotate, tap an icon to launch, or long press for options. Use Apps list above for an accessible list. Arrow keys rotate; Enter opens the center app.");
    }
    public void setActive(boolean value){active=value;updateMotion();}
    private void updateMotion() {
        boolean visible=attached && active && getWindowVisibility()==VISIBLE;
        if(visible && prefs.getBoolean("motion",true) && gravity!=null && !registered) {
            registered=sensors.registerListener(this,gravity,SensorManager.SENSOR_DELAY_GAME);
            if(shakeSensor!=null)registered=sensors.registerListener(this,shakeSensor,SensorManager.SENSOR_DELAY_GAME)||registered;
        }
        if(!visible && registered){sensors.unregisterListener(this);registered=false;}
        if(visible)schedule();else {Choreographer.getInstance().removeFrameCallback(this);scheduled=false;lastFrame=0;removeCallbacks(hold);}
    }
    private void schedule(){if(attached && active && getWindowVisibility()==VISIBLE && !scheduled){scheduled=true;Choreographer.getInstance().postFrameCallback(this);}}
    @Override public void doFrame(long nanos) {
        scheduled=false;
        double dt=lastFrame==0?0:Math.min(.05,(nanos-lastFrame)/1e9);lastFrame=nanos;
        if(!touching) {
            yaw+=velocityX*dt;pitch+=velocityY*dt;
            double damping=.8+prefs.getInt("damping",45)/14.0;
            velocityX=SphereMath.damping(velocityX,damping,dt);velocityY=SphereMath.damping(velocityY,damping,dt);
            if(prefs.getBoolean("autoSpin",true))yaw+=dt*.12;
        }
        pitch=Math.max(-Math.PI,Math.min(Math.PI,pitch));
        invalidate();
        if(prefs.getBoolean("autoSpin",true) || touching || Math.abs(velocityX)+Math.abs(velocityY)>.003)schedule();
        else lastFrame=0;
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;updateMotion();}
    @Override protected void onDetachedFromWindow(){attached=false;updateMotion();super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(sensors!=null)updateMotion();}
    @Override public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            double magnitude=Math.sqrt(event.values[0]*event.values[0]+event.values[1]*event.values[1]+event.values[2]*event.values[2])/SensorManager.GRAVITY_EARTH;
            if(magnitude>1.6 && event.timestamp-lastShake>900000000L && !touching){lastShake=event.timestamp;velocityX+=.65;velocityY+=.12;schedule();}
            if(gravity!=null && gravity.getType()!=Sensor.TYPE_ACCELEROMETER)return;
        }
        float x=event.values[0]/SensorManager.GRAVITY_EARTH,y=event.values[1]/SensorManager.GRAVITY_EARTH;
        int rotation=((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        if(rotation==Surface.ROTATION_90){float old=x;x=-y;y=old;}
        else if(rotation==Surface.ROTATION_180){x=-x;y=-y;}
        else if(rotation==Surface.ROTATION_270){float old=x;x=y;y=-old;}
        tiltX=tiltX*.88f+x*.12f;tiltY=tiltY*.88f+y*.12f;
        invalidate();
    }
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(nodes.isEmpty()){paint.setColor(LauncherStyle.ink(prefs));paint.setTextSize(dp(14));paint.setTextAlign(Paint.Align.CENTER);canvas.drawText("Select apps in Settings → Home screen options",getWidth()/2f,getHeight()/2f,paint);return;}
        float radius=Math.min(getWidth()*.40f,getHeight()*.39f)*(.55f+prefs.getInt("scale",75)/140f);
        float cx=getWidth()/2f,cy=getHeight()/2f-dp(10);
        float icon=dp(20+prefs.getInt("iconSize",55)*.38f);
        float contrast=prefs.getInt("contrast",45)/100f;
        String effect=prefs.getString("effect","smooth");
        for(Node n:nodes) {
            SphereMath.rotate(n.unit,yaw+tiltX*.26,pitch+tiltY*.20,n.rotated);
            float depth=(float)(n.rotated[2]+1)/2;
            float curve="exponential".equals(effect)?depth*depth:"linear".equals(effect)?depth:depth*depth*(3-2*depth);
            float size=icon*(.28f+(1-contrast*.45f)*curve);
            float perspective=2.8f/(2.8f-(float)n.rotated[2]*.35f);
            n.x=cx+(float)n.rotated[0]*radius*perspective;
            n.y=cy+(float)n.rotated[1]*radius*perspective;
            n.size=size;n.alpha=(int)(45+210*depth);
            n.bounds.set(n.x-size/2,n.y-size/2,n.x+size/2,n.y+size/2);
        }
        nodes.sort(DEPTH);
        boolean clip=prefs.getBoolean("surfaceClip",false);
        boolean labels=prefs.getBoolean("labels",true);
        for(Node n:nodes) {
            n.visible=!clip || n.rotated[2]>=0;
            if(!n.visible)continue;
            int save=canvas.save();
            LauncherStyle.fillMask(iconMask,prefs.getString("iconShape","rounded"),n.bounds.left,n.bounds.top,n.size);
            canvas.clipPath(iconMask);
            n.app.icon.setBounds(Math.round(n.bounds.left),Math.round(n.bounds.top),Math.round(n.bounds.right),Math.round(n.bounds.bottom));
            n.app.icon.setAlpha(n.alpha);n.app.icon.draw(canvas);n.app.icon.setAlpha(255);canvas.restoreToCount(save);
            if(prefs.getBoolean("badges",false) && NotificationBadges.count(n.app.component.getPackageName())>0){
                paint.setColor(LauncherStyle.accent(prefs));paint.setAlpha(n.alpha);
                canvas.drawCircle(n.bounds.right-n.size*.08f,n.bounds.top+n.size*.08f,n.size*.12f,paint);paint.setAlpha(255);
            }
            if(labels && n.rotated[2]>.05) {
                paint.setTypeface(LauncherStyle.font(prefs));paint.setColor(LauncherStyle.ink(prefs));paint.setAlpha(n.alpha);
                paint.setTextSize(dp(7+prefs.getInt("labelSize",50)*.12f));paint.setTextAlign(Paint.Align.CENTER);
                String label=n.app.label.length()>16?n.app.label.substring(0,15)+"…":n.app.label;
                canvas.drawText(label,n.x,n.bounds.bottom+dp(13),paint);paint.setAlpha(255);
            }
        }
    }
    boolean isEmptyAt(float x,float y) { return hit(x,y)==null; }
    private MainActivity.AppEntry hit(float x,float y) {
        for(int i=nodes.size()-1;i>=0;i--) {
            Node n=nodes.get(i);
            if(n.visible && n.rotated[2]>-.35 && n.bounds.contains(x,y))return n.app;
        }
        return null;
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        float x=e.getX(),y=e.getY();
        switch(e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);touching=true;moved=false;longPressed=false;
                lastX=downX=x;lastY=downY=y;lastTouch=e.getEventTime();velocityX=velocityY=0;
                pressed=hit(x,y);postDelayed(hold,ViewConfiguration.getLongPressTimeout());return true;
            case MotionEvent.ACTION_MOVE:
                if(Math.hypot(x-downX,y-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop()){moved=true;removeCallbacks(hold);}
                if(moved){float sensitivity=.002f+prefs.getInt("sensitivity",55)*.00010f;
                    double dx=(x-lastX)*sensitivity,dy=(y-lastY)*sensitivity;
                    yaw+=dx;pitch-=dy;long elapsed=Math.max(8,e.getEventTime()-lastTouch);
                    velocityX=dx*1000/elapsed;velocityY=-dy*1000/elapsed;invalidate();}
                lastX=x;lastY=y;lastTouch=e.getEventTime();return true;
            case MotionEvent.ACTION_UP:
                removeCallbacks(hold);touching=false;
                if(!moved && !longPressed){performClick();MainActivity.AppEntry app=hit(x,y);if(app!=null)launch.accept(app);}
                schedule();return true;
            case MotionEvent.ACTION_CANCEL:removeCallbacks(hold);touching=false;velocityX=velocityY=0;schedule();return true;
            default:return true;
        }
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public boolean onKeyDown(int key,android.view.KeyEvent event){
        if(key==KeyEvent.KEYCODE_DPAD_LEFT)yaw-=.2;else if(key==KeyEvent.KEYCODE_DPAD_RIGHT)yaw+=.2;
        else if(key==KeyEvent.KEYCODE_DPAD_UP)pitch-=.2;else if(key==KeyEvent.KEYCODE_DPAD_DOWN)pitch+=.2;
        else if(key==KeyEvent.KEYCODE_ENTER || key==KeyEvent.KEYCODE_DPAD_CENTER){
            Node best=null;for(Node n:nodes)if(best==null || n.rotated[2]>best.rotated[2])best=n;
            if(best!=null)launch.accept(best.app);return true;
        }else return super.onKeyDown(key,event);invalidate();return true;
    }
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private static final class Node {
        final MainActivity.AppEntry app;final double[] unit,rotated=new double[3];final RectF bounds=new RectF();
        float x,y,size;int alpha;boolean visible;
        Node(MainActivity.AppEntry app,double[] unit){this.app=app;this.unit=unit;}
    }
}
