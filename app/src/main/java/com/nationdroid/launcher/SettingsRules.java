package com.nationdroid.launcher;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Strictly validates supported local settings before a backup can change storage. */
public final class SettingsRules {
    private SettingsRules() {}
    private static final Set<String> BOOLEANS=new HashSet<>(Arrays.asList("wallpaper","dark","autoSpin","motion","surfaceClip","labels","topWidget","bottomDock","badges","sphereCustom"));
    private static final Set<String> SLIDERS=new HashSet<>(Arrays.asList("iconSize","labelSize","contrast","sensitivity","damping","scale"));
    private static final Set<String> SETS=new HashSet<>(Arrays.asList("favorites","hidden","locked","sphereApps"));
    public static String type(String key){
        if(BOOLEANS.contains(key))return "boolean";if(SLIDERS.contains(key)||"accent".equals(key)||"category".equals(key))return "integer";
        if(SETS.contains(key))return "set";
        if(Arrays.asList("mode","font","effect","surface","animation","iconShape","profileName").contains(key))return "string";
        return "unknown";
    }
    public static boolean valid(String key,Object value){
        switch(type(key)){
            case "boolean":return value instanceof Boolean;
            case "integer":
                if(!(value instanceof Integer))return false;int number=(Integer)value;
                return "accent".equals(key)||("category".equals(key)?number>=-2&&number<=7:number>=0&&number<=100);
            case "string":
                if(!(value instanceof String))return false;String s=(String)value;
                switch(key){
                    case "mode":return Arrays.asList("desktop","classic","minimal","sphere").contains(s);
                    case "font":return Arrays.asList("sans-serif","serif","monospace").contains(s);
                    case "effect":return Arrays.asList("smooth","linear","exponential").contains(s);
                    case "surface":return Arrays.asList("solid","glass").contains(s);
                    case "animation":return Arrays.asList("none","fade","zoom").contains(s);
                    case "iconShape":return Arrays.asList("rounded","circle","heptagon").contains(s);
                    default:return !s.trim().isEmpty()&&s.length()<=64;
                }
            default:return false;
        }
    }
    public static boolean component(String value){return value!=null&&value.length()<=512&&value.indexOf('/')>0&&value.indexOf('/')<value.length()-1&&!value.contains("\n");}
}
