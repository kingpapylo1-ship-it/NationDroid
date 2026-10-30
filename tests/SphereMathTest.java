import com.nationdroid.launcher.SphereMath;

public final class SphereMathTest {
    public static void main(String[] args){
        for(int count:new int[]{1,2,25,95,500}){
            double meanX=0,meanY=0,meanZ=0;
            for(int i=0;i<count;i++){
                double[] p=SphereMath.point(i,count),r=new double[3];
                close(norm(p),1,"unit sphere");SphereMath.rotate(p,.73,-.42,r);close(norm(r),1,"rotation preserves distance");
                double[] undone=new double[3];SphereMath.rotate(p,0,0,undone);for(int a=0;a<3;a++)close(p[a],undone[a],"zero rotation");
                meanX+=p[0];meanY+=p[1];meanZ+=p[2];
            }
            if(count>=25)check(Math.abs(meanX/count)<.05&&Math.abs(meanY/count)<.05&&Math.abs(meanZ/count)<.05,"balanced app distribution");
        }
        double[] rotated=new double[3];SphereMath.rotate(new double[]{1,0,0},Math.PI/2,0,rotated);close(rotated[0],0,"quarter turn x");close(rotated[2],-1,"quarter turn depth");
        double a=SphereMath.damping(4,3,.1),b=SphereMath.damping(SphereMath.damping(4,3,.05),3,.05);close(a,b,"frame-rate independent damping");
        check(SphereMath.damping(4,9,.1)<a,"higher damping stops faster");close(SphereMath.damping(4,3,0),4,"zero elapsed time");
        check(SphereMath.clamp(150,0,100)==100,"upper clamp");
        boolean rejected=false;try{SphereMath.point(0,0);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"empty sphere rejects indexing");
        System.out.println("Sphere geometry, distribution, rotation and inertia checks passed (623 points).");
    }
    private static double norm(double[] p){return Math.sqrt(p[0]*p[0]+p[1]*p[1]+p[2]*p[2]);}
    private static void close(double a,double b,String label){check(Math.abs(a-b)<1e-9,label);}
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
}
