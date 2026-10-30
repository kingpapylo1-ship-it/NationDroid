package com.nationdroid.launcher;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final class PreferenceBackup {
    private PreferenceBackup() {}
    static JSONObject export(SharedPreferences prefs) throws Exception {
        JSONObject values=new JSONObject();
        for(Map.Entry<String,?> entry:prefs.getAll().entrySet()){
            String key=entry.getKey();Object value=entry.getValue();
            if("set".equals(SettingsRules.type(key)) && value instanceof Set){JSONArray array=new JSONArray();for(Object item:(Set<?>)value)if(item instanceof String && SettingsRules.component((String)item))array.put(item);values.put(key,array);}
            else if(SettingsRules.valid(key,value))values.put(key,value);
        }
        return new JSONObject().put("app","NationDroid").put("version",1).put("values",values);
    }
    static JSONObject validate(String input) throws Exception {
        JSONObject backup=new JSONObject(input);
        if(!"NationDroid".equals(backup.getString("app"))||backup.getInt("version")!=1)throw new IllegalArgumentException("Wrong backup format");
        JSONObject values=backup.getJSONObject("values");Iterator<String> keys=values.keys();
        while(keys.hasNext()){
            String key=keys.next();Object value=values.get(key);
            if("set".equals(SettingsRules.type(key))){
                if(!(value instanceof JSONArray))throw new IllegalArgumentException("Invalid app selection");JSONArray set=(JSONArray)value;
                if(set.length()>1000)throw new IllegalArgumentException("App selection too large");
                for(int i=0;i<set.length();i++)if(!(set.get(i) instanceof String)||!SettingsRules.component(set.getString(i)))throw new IllegalArgumentException("Invalid component");
            }else if(!SettingsRules.valid(key,value))throw new IllegalArgumentException("Invalid preference");
        }
        return values;
    }
    static void apply(SharedPreferences prefs,JSONObject values) throws Exception {
        // Revalidate the entire document before creating any edits.
        validate(new JSONObject().put("app","NationDroid").put("version",1).put("values",values).toString());
        SharedPreferences.Editor editor=prefs.edit().clear().putInt("revision",prefs.getInt("revision",0)+1);Iterator<String> keys=values.keys();
        while(keys.hasNext()){
            String key=keys.next();Object value=values.get(key);
            if(value instanceof JSONArray){Set<String> set=new HashSet<>();JSONArray array=(JSONArray)value;for(int i=0;i<array.length();i++)set.add(array.getString(i));editor.putStringSet(key,set);}
            else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
            else if(value instanceof Integer)editor.putInt(key,(Integer)value);
            else if(value instanceof String)editor.putString(key,(String)value);
        }
        if(!editor.commit())throw new java.io.IOException("Preferences could not be persisted");
    }
}
