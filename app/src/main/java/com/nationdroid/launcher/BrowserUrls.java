package com.nationdroid.launcher;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class BrowserUrls {
    public static final String HOME="https://duckduckgo.com/";
    private BrowserUrls(){}
    public static String input(String text){
        if(text==null||text.trim().isEmpty())return HOME;
        String value=text.trim();if(value.length()>8192)return null;
        if(value.matches("(?i)^https?://.*"))return navigation(value);
        if(value.matches("(?is)^(javascript|data|file|content|intent|about|vbscript|chrome|tel|mailto):.*"))return null;
        if(value.contains("://"))return null;
        if(value.matches("(?i)^[a-z][a-z0-9+.-]*:.*")&&!value.matches(".*\\s.*")&&!value.matches("(?i)^(site|filetype|intitle|inurl|related|cache):.*")&&!value.matches("[^ /:]+:[0-9]{1,5}(/.*)?"))return null;
        if(!value.matches(".*\\s.*")&&(value.contains(".")||value.startsWith("localhost"))){String url=navigation("https://"+value);if(url!=null)return url;}
        try{return HOME+"?q="+URLEncoder.encode(value,"UTF-8");}catch(java.io.UnsupportedEncodingException impossible){throw new AssertionError(impossible);}
    }
    public static String navigation(String value){
        if(value==null||value.length()>8192)return null;
        try{
            URI uri=new URI(value.trim());String scheme=uri.getScheme();
            if(!"http".equalsIgnoreCase(scheme)&&!"https".equalsIgnoreCase(scheme))return null;
            if(uri.getHost()==null||uri.getHost().isEmpty()||uri.getUserInfo()!=null||uri.getPort()>65535)return null;
            if("http".equalsIgnoreCase(scheme))return "https"+value.trim().substring(value.trim().indexOf(':'));
            return uri.toASCIIString();
        }catch(Exception ignored){return null;}
    }
    public static String filename(String value){
        if(value==null)return "download";
        String result=value.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","_").trim();
        if(result.isEmpty()||result.equals(".")||result.equals(".."))return "download";
        return result.substring(0,Math.min(120,result.length()));
    }
}
