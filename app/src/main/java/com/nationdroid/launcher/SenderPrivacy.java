package com.nationdroid.launcher;

/** Fail-closed sender naming. No raw notification title, phone number or message body is displayed. */
public final class SenderPrivacy {
    public static final String UNKNOWN="Unknown sender";
    private SenderPrivacy() {}
    public interface Contacts {
        String byNumber(String number);
        String byName(String name);
        String byContactUri(String uri);
    }
    public static String display(String uri,String name,boolean permission,Contacts contacts){
        if(!permission||contacts==null)return UNKNOWN;
        try {
            if(uri!=null&&!uri.trim().isEmpty()){
                java.net.URI parsed=new java.net.URI(uri);
                if("tel".equalsIgnoreCase(parsed.getScheme())){
                    String number=number(parsed.getSchemeSpecificPart());
                    return number==null?UNKNOWN:safeName(contacts.byNumber(number));
                }
                if("content".equals(parsed.getScheme())&&"com.android.contacts".equals(parsed.getAuthority())&&parsed.getPath()!=null&&parsed.getPath().startsWith("/contacts/")){
                    return safeName(contacts.byContactUri(uri));
                }
            }
            String phone=number(name);
            if(phone!=null)return safeName(contacts.byNumber(phone));
            if(name==null||name.length()>256||name.trim().isEmpty())return UNKNOWN;
            return safeName(contacts.byName(name.trim()));
        }catch(Exception ignored){return UNKNOWN;}
    }
    static String number(String value){
        if(value==null||value.length()>128)return null;
        String input=value.trim();int extension=input.indexOf(';');if(extension>=0)input=input.substring(0,extension);
        StringBuilder normalized=new StringBuilder();int digits=0;
        for(int i=0;i<input.length();){int c=input.codePointAt(i);i+=Character.charCount(c);
            int digit=Character.digit(c,10);
            if(digit>=0){normalized.append((char)('0'+digit));digits++;}
            else if(c=='+'&&normalized.length()==0)normalized.append('+');
            else if(!Character.isWhitespace(c)&&c!='('&&c!=')'&&c!='-'&&c!='.')return null;
        }
        return digits>=7&&digits<=15?normalized.toString():null;
    }
    public static String safeName(String value){
        if(value==null||value.length()>512||value.contains("@")||value.contains("://")||value.toLowerCase(java.util.Locale.ROOT).startsWith("tel:"))return UNKNOWN;
        StringBuilder result=new StringBuilder();boolean letter=false;
        for(int i=0;i<value.length()&&result.length()<80;){int c=value.codePointAt(i);i+=Character.charCount(c);
            if(Character.isLetter(c)){result.appendCodePoint(c);letter=true;}
            else if(Character.getType(c)==Character.NON_SPACING_MARK||c=='\''||c=='-')result.appendCodePoint(c);
            else if(Character.isWhitespace(c)||c=='.')result.append(' ');
            // All digits, phone punctuation and other symbols are discarded.
        }
        String safe=result.toString().replaceAll("\\s+"," ").replaceAll("^[ '-]+|[ '-]+$","").trim();
        return letter&&!safe.isEmpty()?safe:UNKNOWN;
    }
}
