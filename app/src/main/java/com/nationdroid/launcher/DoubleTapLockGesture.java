package com.nationdroid.launcher;

/** Recognizes two short stationary taps; callers decide whether each point is empty home space. */
public final class DoubleTapLockGesture {
    private final long interval, tapDuration;
    private final float touchSlop, doubleTapSlop;
    private long downTime, previousUp=-1;
    private float downX,downY,previousX,previousY;
    private boolean candidate;
    public DoubleTapLockGesture(long interval,long tapDuration,float touchSlop,float doubleTapSlop){
        this.interval=interval;this.tapDuration=tapDuration;this.touchSlop=touchSlop;this.doubleTapSlop=doubleTapSlop;
    }
    public void down(long time,float x,float y,boolean empty){
        if(!empty){reset();return;}
        candidate=true;downTime=time;downX=x;downY=y;
        if(previousUp>=0 && (time<previousUp || time-previousUp>interval || distance(x,y,previousX,previousY)>doubleTapSlop))previousUp=-1;
    }
    public void move(float x,float y){if(candidate && distance(x,y,downX,downY)>touchSlop)reset();}
    public boolean up(long time,float x,float y,boolean empty){
        if(!candidate || !empty || time<downTime || time-downTime>tapDuration || distance(x,y,downX,downY)>touchSlop){reset();return false;}
        candidate=false;
        if(previousUp>=0 && downTime-previousUp<=interval){reset();return true;}
        previousUp=time;previousX=x;previousY=y;return false;
    }
    public void reset(){candidate=false;previousUp=-1;}
    private static double distance(float x,float y,float otherX,float otherY){return Math.hypot(x-otherX,y-otherY);}
}
