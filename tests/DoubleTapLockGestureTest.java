import com.nationdroid.launcher.DoubleTapLockGesture;

public final class DoubleTapLockGestureTest {
    public static void main(String[] args){
        DoubleTapLockGesture g=new DoubleTapLockGesture(300,250,10,50);
        check(!tap(g,100,20,20,true),"single tap does not lock");
        check(tap(g,220,22,21,true),"nearby second tap locks");
        check(!tap(g,300,22,21,true),"third tap begins a new pair");g.reset();
        tap(g,100,20,20,true);check(!tap(g,600,20,20,true),"slow taps do not lock");g.reset();
        tap(g,100,20,20,true);check(!tap(g,220,200,200,true),"distant taps do not lock");g.reset();
        tap(g,100,20,20,true);check(!tap(g,220,20,20,false),"app tap cannot complete pair");
        check(!tap(g,300,20,20,true),"app tap clears prior empty tap");g.reset();
        g.down(100,20,20,true);g.move(40,20);check(!g.up(160,40,20,true),"swipe is not a tap");
        check(!tap(g,220,40,20,true),"swipe resets pair");g.reset();
        g.down(100,20,20,true);check(!g.up(500,20,20,true),"long press does not lock");
        check(!tap(g,600,20,20,true),"long press resets pair");g.reset();
        tap(g,100,20,20,true);g.reset();check(!tap(g,220,20,20,true),"cancel or pause resets pair");g.reset();
        g.down(100,20,20,true);check(!g.up(130,20,20,false),"moving onto a control cancels tap");
        check(!tap(g,200,20,20,true),"control interaction resets pair");g.reset();
        check(!g.up(120,20,20,true),"up without down cannot lock");
        System.out.println("15 double-tap lock gesture checks passed.");
    }
    private static boolean tap(DoubleTapLockGesture g,long time,float x,float y,boolean empty){g.down(time,x,y,empty);return g.up(time+40,x,y,empty);}
    private static void check(boolean result,String label){if(!result)throw new AssertionError(label);}
}
