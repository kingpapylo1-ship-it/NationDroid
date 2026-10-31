import com.nationdroid.launcher.SenderPrivacy;

public final class SenderPrivacyTest {
    public static void main(String[] args){
        SenderPrivacy.Contacts book=new SenderPrivacy.Contacts(){
            public String byNumber(String number){return "+2348012345678".equals(number)?"Dolapo":null;}
            public String byName(String name){return "Dolapo".equalsIgnoreCase(name)?"Dolapo":null;}
            public String byContactUri(String uri){return uri.endsWith("/42")?"Chioma":null;}
        };
        eq("Dolapo",SenderPrivacy.display("tel:+2348012345678","+2348012345678",true,book),"saved number");
        eq("Dolapo",SenderPrivacy.display("tel:%2B2348012345678",null,true,book),"encoded saved number");
        eq("Unknown sender",SenderPrivacy.display("tel:+2348099999999","Dolapo",true,book),"unsaved number cannot fall back to claimed name");
        eq("Unknown sender",SenderPrivacy.display(null,"+2348099999999",true,book),"unsaved raw number");
        eq("Dolapo",SenderPrivacy.display(null,"Dolapo",true,book),"saved exact name");
        eq("Unknown sender",SenderPrivacy.display(null,"A stranger",true,book),"unmatched name");
        eq("Unknown sender",SenderPrivacy.display("tel:+2348012345678","Dolapo",false,book),"denied permission");
        eq("Chioma",SenderPrivacy.display("content://com.android.contacts/contacts/42",null,true,book),"contact URI");
        eq("Unknown sender",SenderPrivacy.display("tel:not-a-number","Dolapo",true,book),"invalid identifier is fail closed");
        eq("Dolapo",SenderPrivacy.safeName("Dolapo +234 8012345678"),"number suffix stripped");
        eq("Unknown sender",SenderPrivacy.safeName("+234 8012345678"),"numeric contact name");
        eq("Unknown sender",SenderPrivacy.safeName("person@example.com"),"email contact name");
        eq("Unknown sender",SenderPrivacy.safeName("content://contacts/42"),"URI contact name");
        eq("Chioma",SenderPrivacy.safeName("Chioma ١٢٣٤٥٦"),"Unicode digits stripped");
        eq("张伟",SenderPrivacy.safeName("张伟"),"Unicode names preserved");
        eq("Anne-Marie O'Neil",SenderPrivacy.safeName("Anne-Marie O'Neil"),"name punctuation preserved");
        System.out.println("16 private-sender masking checks passed.");
    }
    private static void eq(String expected,String actual,String label){if(!expected.equals(actual))throw new AssertionError(label+": "+actual);}
}
