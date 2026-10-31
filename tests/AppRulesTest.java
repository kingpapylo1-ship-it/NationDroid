import com.nationdroid.launcher.AppRules;
import java.util.Locale;

public final class AppRulesTest {
    public static void main(String[] args) {
        check(AppRules.matches("Camera", "com.vendor.camera", "  CAM  "), "trimmed label match");
        check(AppRules.matches("Photos", "com.vendor.gallery", "gallery"), "package match");
        check(!AppRules.matches("Camera", "com.vendor.camera", "music"), "exclude unrelated app");
        check(AppRules.matches("Camera", "com.vendor.camera", ""), "empty query includes all apps");
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            check(AppRules.matches("INSTALLED", "com.example.app", "installed"), "locale-independent search");
        } finally { Locale.setDefault(original); }
        check(AppRules.columns(320, 7) == 3, "narrow screen caps grid");
        check(AppRules.columns(900, 7) == 7, "tablet uses preferred columns");
        check(AppRules.columns(180, 4) == 2, "split-screen grid");
        check(AppRules.columns(900, 100) == 7, "invalid setting capped");
        check(AppRules.columns(900, -1) == 3, "invalid setting bounded");
        System.out.println("10 launcher rule checks passed.");
    }
    private static void check(boolean result, String name) {
        if (!result) throw new AssertionError(name);
    }
}
