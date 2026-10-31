import com.nationdroid.launcher.WindowRules;

public final class WindowRulesTest {
    public static void main(String[] args) {
        check(WindowRules.size(800,270,360)==360,"desktop width limit");
        check(WindowRules.size(100,270,360)==270,"minimum width");
        check(WindowRules.size(600,200,140)==140,"keyboard leaves less than minimum");
        check(WindowRules.size(600,200,0)==1,"layout not measured");
        check(WindowRules.size(310,270,360)==310,"retain resized width");
        check(WindowRules.position(-30,300,360)==0,"drag past left edge");
        check(WindowRules.position(200,300,360)==60,"drag past right edge");
        check(WindowRules.position(40,600,360)==0,"oversized window origin");
        check(WindowRules.position(30,300,360)==30,"retain valid origin");
        System.out.println("9 desktop window checks passed.");
    }
    private static void check(boolean result,String message) {if(!result)throw new AssertionError(message);}
}
