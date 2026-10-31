package com.nationdroid.launcher;

public final class WelcomeRules {
    public static final String DEFAULT="Welcome back, {name}. Your NationDroid workspace is ready.";
    private WelcomeRules(){}
    public static boolean shouldShow(boolean enabled,int boot,int lastBoot,boolean pending){return enabled&&(boot>=0?boot!=lastBoot:pending);}
    public static String message(String template,String name){
        String value=template==null||template.trim().isEmpty()?DEFAULT:template.trim();
        if(value.length()>240)value=value.substring(0,240);
        String person=name==null||name.trim().isEmpty()?"King":name.trim();
        return value.replace("{name}",person);
    }
}
