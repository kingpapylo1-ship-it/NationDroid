import com.nationdroid.launcher.BrowserUrls;
import com.nationdroid.launcher.WelcomeRules;

public final class BrowserWelcomeRulesTest {
    public static void main(String[] args){
        eq("https://duckduckgo.com/",BrowserUrls.input(""));
        eq("https://example.com/path",BrowserUrls.input("example.com/path"));
        eq("https://example.com/a?q=b",BrowserUrls.input("http://example.com/a?q=b"));
        eq("https://duckduckgo.com/?q=hello+world",BrowserUrls.input("hello world"));
        eq("https://duckduckgo.com/?q=C%2B%2B",BrowserUrls.input("C++"));
        eq("https://duckduckgo.com/?q=site%3Aexample.com",BrowserUrls.input("site:example.com"));
        eq("https://duckduckgo.com/?q=site%3Aexample.com+hello",BrowserUrls.input("site:example.com hello"));
        for(String blocked:new String[]{"javascript:alert(1)","file:///sdcard/private.txt","content://contacts/1","data:text/html,test","intent://anything"})check(BrowserUrls.input(blocked)==null,"blocked scheme");
        check(BrowserUrls.navigation("https://user:password@example.com/")==null,"credentials in URL blocked");
        check(BrowserUrls.navigation("https://example.com:90000/")==null,"invalid port blocked");
        eq("https://example.com:443/",BrowserUrls.input("example.com:443/"));
        check(!BrowserUrls.filename("../a\nb.apk").contains("/"),"filename path sanitized");
        eq("download",BrowserUrls.filename(".."));
        check(WelcomeRules.shouldShow(true,5,4,false),"new boot welcomes");
        check(!WelcomeRules.shouldShow(true,5,5,true),"late boot broadcast does not repeat welcome");
        check(!WelcomeRules.shouldShow(false,5,4,true),"disabled welcome");
        check(WelcomeRules.shouldShow(true,-1,-1,true),"broadcast fallback");
        check(!WelcomeRules.shouldShow(true,-1,-1,false),"fallback already shown");
        eq("Welcome King",WelcomeRules.message("Welcome {name}","King"));
        check(WelcomeRules.message("","").contains("King"),"empty template defaults");
        System.out.println("24 browser URL and boot-welcome checks passed.");
    }
    private static void eq(String expected,String actual){check(expected.equals(actual),expected+" != "+actual);}
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
}
