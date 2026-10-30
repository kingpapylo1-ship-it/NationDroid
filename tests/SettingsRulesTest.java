import com.nationdroid.launcher.SettingsRules;

public final class SettingsRulesTest {
    public static void main(String[] args){
        check(SettingsRules.valid("mode","sphere"),"sphere mode accepted");
        check(!SettingsRules.valid("mode","unknown"),"unknown mode rejected");
        check(SettingsRules.valid("iconSize",100),"slider limit accepted");
        check(!SettingsRules.valid("iconSize",101),"oversized slider rejected");
        check(!SettingsRules.valid("scale",-1),"negative slider rejected");
        check(!SettingsRules.valid("scale",50.5),"fractional slider rejected");
        check(!SettingsRules.valid("autoSpin","true"),"wrong boolean type rejected");
        check(SettingsRules.valid("autoSpin",true),"boolean accepted");
        check(SettingsRules.valid("accent",0xff0033ff),"ARGB color accepted");
        check(SettingsRules.valid("category",-2),"all categories accepted");
        check(!SettingsRules.valid("category",9),"unknown category rejected");
        check(!SettingsRules.valid("profileName"," "),"empty profile rejected");
        check(!SettingsRules.valid("unknown",true),"unknown preference rejected");
        check(SettingsRules.component("com.example/.Main"),"component accepted");
        check(!SettingsRules.component("no-component"),"malformed component rejected");
        check(!SettingsRules.component("com.example/"),"empty activity rejected");
        System.out.println("16 preference-validation checks passed.");
    }
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
}
