package com.nationdroid.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Local launcher customization. Every visible option controls implemented behavior. */
public final class LauncherOptionsActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout content;
    private String page="root";
    private final List<MainActivity.AppEntry> inventory=new ArrayList<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private boolean loaded;
    private HoloSphereView preview;
    private static final String[] MODE_NAMES={"Desktop", "Classic layout", "Minimal list", "Holo Sphere"};
    private static final String[] MODE_IDS={"desktop", "classic", "minimal", "sphere"};

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);prefs=getSharedPreferences("launcher",MODE_PRIVATE);
        if(saved!=null)page=saved.getString("page","root");
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::back);
        render();loadInventory();
    }
    private void loadInventory(){worker.execute(()->{
        List<MainActivity.AppEntry> result=new ArrayList<>();
        try{
            Set<String> seen=new HashSet<>();
            for(ResolveInfo info:getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)){
                if(info.activityInfo==null || getPackageName().equals(info.activityInfo.packageName))continue;
                ComponentName component=new ComponentName(info.activityInfo.packageName,info.activityInfo.name);
                if(seen.add(component.flattenToString()))result.add(new MainActivity.AppEntry(info.loadLabel(getPackageManager()).toString(),component,info.loadIcon(getPackageManager()),info.activityInfo.applicationInfo.category));
            }
            Collator c=Collator.getInstance();result.sort((a,b)->c.compare(a.label,b.label));
        }catch(RuntimeException e){runOnUiThread(()->toast("Couldn't load apps. Reopen settings to retry."));}
        runOnUiThread(()->{if(isDestroyed())return;inventory.clear();inventory.addAll(result);loaded=true;render();});
    });}
    private void render(){
        preview=null;
        boolean dark=LauncherStyle.dark(prefs);int bg=dark?0xff10151f:0xffeef1f9;
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg);
        root.setOnApplyWindowInsetsListener((v,i)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=i.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout()|android.view.WindowInsets.Type.ime());v.setPadding(safe.left,safe.top,safe.right,safe.bottom);}
            else v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;
        });
        setContentView(root);root.requestApplyInsets();
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        LinearLayout bar=row();bar.setPadding(dp(12),0,dp(12),0);
        TextView back=button("‹",this::back);back.setTextSize(30);back.setContentDescription("Back");bar.addView(back,new LinearLayout.LayoutParams(dp(48),dp(56)));
        String title="home".equals(page)?"Home screen options":"modes".equals(page)?"Launcher mode":"NationDroid settings";
        TextView heading=text(title,20);heading.setTypeface(null,Typeface.BOLD);bar.addView(heading,new LinearLayout.LayoutParams(0,dp(60),1));root.addView(bar);
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(12),dp(16),dp(28));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if("home".equals(page))homePage();else if("modes".equals(page))modePage();else rootPage();
    }
    private void rootPage(){
        card("Set as default launcher","Choose NationDroid in Android's Home settings",()->open(new Intent(Settings.ACTION_HOME_SETTINGS)));
        section("LAYOUT");
        card("Launcher mode",modeName(),()->showPage("modes"));
        card("Home screen options","Sphere motion, app selection, dock and icon sizes",()->showPage("home"));
        section("STYLE");
        card("Theme presets","Choose Ocean, Violet or Forest",()->new AlertDialog.Builder(this).setTitle("Theme preset").setItems(new String[]{"Ocean","Violet","Forest"},(d,i)->{
            int[] colors={0xff2766e9,0xff7649dc,0xff098777};prefs.edit().putInt("accent",colors[i]).putBoolean("dark",i!=0).apply();changed();render();}).show());
        card("Wallpaper",prefs.getBoolean("wallpaper",false)?"Current Android wallpaper":"Built-in blue waves",()->new AlertDialog.Builder(this).setTitle("Wallpaper").setItems(new String[]{"Built-in blue waves","Current Android wallpaper","Choose Android wallpaper"},(d,i)->{
            if(i==2)open(new Intent(Intent.ACTION_SET_WALLPAPER));else{prefs.edit().putBoolean("wallpaper",i==1).apply();changed();render();}}).show());
        choice("Icon shape","iconShape",new String[]{"Rounded square","Circle","Heptagon"},new String[]{"rounded","circle","heptagon"},"rounded");
        choice("Fonts","font",new String[]{"System sans","Serif","Monospace"},new String[]{"sans-serif","serif","monospace"},"sans-serif");
        card("Theme color","Pick an accent color",()->new AlertDialog.Builder(this).setTitle("Accent color").setItems(new String[]{"Blue","Purple","Teal","Rose","Amber"},(d,i)->{
            int[] colors={0xff2766e9,0xff8658ea,0xff16a794,0xffe75283,0xffdf9617};prefs.edit().putInt("accent",colors[i]).apply();changed();render();}).show());
        section("EFFECTS");
        toggle("Dark mode","Changes alternate homes and this settings screen","dark",true,true);
        choice("Surface effect","surface",new String[]{"Solid","Translucent glass"},new String[]{"solid","glass"},"solid");
        choice("Return-to-home animation","animation",new String[]{"None","Fade","Zoom"},new String[]{"none","fade","zoom"},"fade");
        section("GENERAL");
        card("App categories",categoryName(prefs.getInt("category",-2)),()->new AlertDialog.Builder(this).setTitle("Show app category").setItems(new String[]{"All apps","Games","Audio","Video","Images","Social","News","Maps","Productivity","Other / uncategorized"},(d,i)->{
            prefs.edit().putInt("category",i==0?-2:i==9?-1:i-1).apply();changed();render();}).show());
        toggle("Notification dots","Optional access; only active notification counts are held in memory","badges",false,false);
        card("Notification access","Grant or revoke Android's notification-listener access",()->open(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        card("Device",Build.MANUFACTURER+" "+Build.MODEL+" · Android "+Build.VERSION.RELEASE,()->open(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)));
        section("PRIVACY & SECURITY");
        card("Hidden apps","Hide apps from NationDroid only",()->selectApps("Hidden apps","hidden",false));
        card("Launch confirmation","Require your device credential when opening selected apps from NationDroid",()->{
            KeyguardManager k=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
            if(!k.isDeviceSecure())new AlertDialog.Builder(this).setTitle("Set up a device screen lock first").setMessage("This uses Android's device-credential confirmation. It does not lock apps opened from other launchers, notifications or system shortcuts.").setPositiveButton("Android security",(d,i)->open(new Intent(Settings.ACTION_SECURITY_SETTINGS))).setNegativeButton("Cancel",null).show();
            else selectApps("Confirm before launch","locked",false);
        });
        note("Hidden apps are not a secure private space. Launch confirmation only protects launches through NationDroid's app list and pins; it does not isolate apps or their data.");
        section("BACKUP & RESTORE");
        card("Export settings","Save modes, pins and preferences to a JSON file",()->openForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"NationDroid-settings.json"),201));
        card("Restore settings","Import a NationDroid JSON backup",()->openForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json"),202));
        section("ABOUT");note("NationDroid 1.2.0 · All implemented options are available without a subscription. No third-party launcher code or premium entitlement is included. Device private-space management and third-party widgets are not implemented.");
    }
    private void modePage(){
        note("CHOOSE HOW YOUR HOME SCREEN LOOKS");
        final int[] selected={0};String current=prefs.getString("mode","desktop");for(int i=0;i<MODE_IDS.length;i++)if(MODE_IDS[i].equals(current))selected[0]=i;
        String[] details={"Desktop shortcuts, taskbar and launcher windows","Horizontal app pages, search and a pinned dock","Clean vertical app list with an alphabet index","3D app sphere with touch, tilt and inertia"};
        List<TextView> rows=new ArrayList<>();
        for(int i=0;i<MODE_NAMES.length;i++){
            final int index=i;LinearLayout card=cardBox();TextView label=text((selected[0]==i?"✓  ":"○  ")+MODE_NAMES[i],18);rows.add(label);card.addView(label);card.addView(subtext(details[i]));
            card.setOnClickListener(v->{selected[0]=index;for(int j=0;j<rows.size();j++)rows.get(j).setText((j==index?"✓  ":"○  ")+MODE_NAMES[j]);});
        }
        TextView apply=button("Apply now",()->{prefs.edit().putString("mode",MODE_IDS[selected[0]]).apply();changed();finish();});apply.setTextColor(Color.WHITE);apply.setBackground(round(LauncherStyle.accent(prefs),24));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.setMargins(dp(50),dp(24),dp(50),dp(12));content.addView(apply,p);
    }
    private void homePage(){
        if(loaded){
            FrameLayout frame=new FrameLayout(this);frame.setBackground(round(LauncherStyle.dark(prefs)?0xff080b13:0xffe0e6f2,18));
            List<MainActivity.AppEntry> visible=new ArrayList<>();Set<String> hidden=prefs.getStringSet("hidden",new HashSet<>()),selected=prefs.getStringSet("sphereApps",new HashSet<>());
            for(MainActivity.AppEntry app:inventory)if(!hidden.contains(app.key()) && (!prefs.getBoolean("sphereCustom",false)||selected.contains(app.key())))visible.add(app);
            preview=new HoloSphereView(this,visible,prefs,a->{},a->{});preview.setActive(false);frame.addView(preview,new FrameLayout.LayoutParams(-1,-1));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(240));p.bottomMargin=dp(14);content.addView(frame,p);
        }
        card("Manage app sphere",loaded?inventory.size()+" launchable activities available":"Loading apps…",()->selectApps("Apps in your sphere","sphereApps",true));
        card("Use all sphere apps","Include newly installed apps automatically",()->{prefs.edit().putBoolean("sphereCustom",false).apply();changed();render();});
        section("ADVANCED HOLO");
        toggle("Auto spin","Gently rotate the sphere when idle","autoSpin",true,false);
        toggle("Motion sensing","Respond to tilt and gentle shakes; touch works without sensors","motion",true,false);
        toggle("3D surface clip","Show only the front hemisphere","surfaceClip",false,false);
        toggle("Icon and label","Show app names beneath icons","labels",true,false);
        choice("Effect","effect",new String[]{"Smooth depth","Linear","Ease in exponential"},new String[]{"smooth","linear","exponential"},"smooth");
        section("BASIC");
        toggle("Top clock widget","Show current time and date on alternate homes","topWidget",true,false);
        toggle("Bottom dock","Show a scrollable row of pinned apps","bottomDock",true,false);
        slider("Icon size","iconSize",55);slider("Label size","labelSize",50);slider("Edge icon contrast","contrast",45);
        slider("Sensitivity","sensitivity",55);slider("Inertia damping","damping",45);slider("Scale","scale",75);
        note("Motion sensors and animation stop when NationDroid is not visible. If tilt sensing is unavailable, swipe to rotate. The top widget shows time/date, not calendar events.");
    }
    private void selectApps(String title,String key,boolean sphereSelection){
        if(!loaded){toast("Apps are still loading");return;}
        String[] names=new String[inventory.size()];boolean[] checked=new boolean[names.length];Set<String> selected=new HashSet<>(prefs.getStringSet(key,new HashSet<>()));
        boolean all=sphereSelection && !prefs.getBoolean("sphereCustom",false);
        for(int i=0;i<names.length;i++){names[i]=inventory.get(i).label+"\n"+inventory.get(i).component.getPackageName();checked[i]=all||selected.contains(inventory.get(i).key());}
        new AlertDialog.Builder(this).setTitle(title).setMultiChoiceItems(names,checked,(d,i,value)->checked[i]=value)
                .setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
                    Set<String> next=new HashSet<>();for(int i=0;i<checked.length;i++)if(checked[i])next.add(inventory.get(i).key());
                    SharedPreferences.Editor e=prefs.edit().putStringSet(key,next);if(sphereSelection)e.putBoolean("sphereCustom",true);e.apply();changed();render();
                }).show();
    }
    private void slider(String label,String key,int initial){
        LinearLayout card=cardBox();LinearLayout row=row();TextView title=text(label+" · "+prefs.getInt(key,initial)+"%",15);row.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        SeekBar bar=new SeekBar(this);bar.setProgressTintList(android.content.res.ColorStateList.valueOf(LauncherStyle.accent(prefs)));bar.setThumbTintList(android.content.res.ColorStateList.valueOf(LauncherStyle.accent(prefs)));bar.setMax(100);bar.setProgress(prefs.getInt(key,initial));bar.setContentDescription(label);
        TextView reset=button("↶",()->{bar.setProgress(initial);prefs.edit().putInt(key,initial).apply();changed();if(preview!=null)preview.invalidate();});reset.setContentDescription("Reset "+label);row.addView(reset,new LinearLayout.LayoutParams(dp(44),dp(44)));card.addView(row);card.addView(bar);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int value,boolean user){title.setText(label+" · "+value+"%");if(user){prefs.edit().putInt(key,value).apply();if(preview!=null)preview.invalidate();}}
            public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){changed();}
        });
    }
    private void toggle(String title,String subtitle,String key,boolean defaultValue,boolean redraw){
        LinearLayout card=cardBox(),row=row(),labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.addView(text(title,15));labels.addView(subtext(subtitle));row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        Switch toggle=new Switch(this);
        toggle.setThumbTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{LauncherStyle.accent(prefs),0xff9aa3b5}));
        toggle.setTrackTintList(android.content.res.ColorStateList.valueOf(LauncherStyle.accent(prefs)&0x55ffffff));
        toggle.setContentDescription(title);toggle.setChecked(prefs.getBoolean(key,defaultValue));toggle.setMinWidth(dp(52));row.addView(toggle);card.addView(row);
        toggle.setOnCheckedChangeListener((v,checked)->{prefs.edit().putBoolean(key,checked).apply();changed();if("badges".equals(key)&&checked){NotificationListenerService.requestRebind(new ComponentName(this,NotificationBadges.class));toast("Enable NationDroid under Notification access to show dots.");}if(redraw)render();else if(preview!=null)preview.invalidate();});
    }
    private void choice(String title,String key,String[] labels,String[] values,String initial){
        String current=prefs.getString(key,initial);int chosen=0;for(int i=0;i<values.length;i++)if(values[i].equals(current))chosen=i;
        final int checked=chosen;card(title,labels[chosen],()->new AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(labels,checked,(d,i)->{
            prefs.edit().putString(key,values[i]).apply();changed();d.dismiss();render();}).setNegativeButton("Cancel",null).show());
    }
    private void changed(){prefs.edit().putInt("revision",prefs.getInt("revision",0)+1).apply();}
    private void showPage(String value){page=value;render();}
    private String modeName(){String mode=prefs.getString("mode","desktop");for(int i=0;i<MODE_IDS.length;i++)if(MODE_IDS[i].equals(mode))return MODE_NAMES[i];return MODE_NAMES[0];}
    static String categoryName(int category){String[] labels={"Games","Audio","Video","Images","Social","News","Maps","Productivity"};return category==-2?"All apps":category>=0&&category<labels.length?labels[category]:"Other / uncategorized";}
    private void card(String title,String detail,Runnable action){LinearLayout card=cardBox();card.addView(text(title+"  ›",16));card.addView(subtext(detail));card.setFocusable(true);card.setOnClickListener(v->action.run());card.setContentDescription(title+". "+detail);}
    private LinearLayout cardBox(){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(14),dp(16),dp(14));card.setBackground(round(LauncherStyle.card(prefs),14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);content.addView(card,p);return card;}
    private void section(String value){TextView v=text(value,12);v.setTypeface(null,Typeface.BOLD);v.setTextColor(LauncherStyle.accent(prefs));v.setPadding(dp(4),dp(24),0,dp(12));content.addView(v);}
    private void note(String value){TextView v=subtext(value);v.setPadding(dp(6),dp(12),dp(6),dp(14));content.addView(v);}
    private TextView subtext(String value){TextView v=text(value,13);v.setTextColor(LauncherStyle.dark(prefs)?0xffaeb9cd:0xff596277);v.setPadding(0,dp(4),0,0);return v;}
    private TextView text(String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(LauncherStyle.ink(prefs));v.setTypeface(LauncherStyle.font(prefs));return v;}
    private TextView button(String label,Runnable action){TextView v=text(label,16);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(48));v.setOnClickListener(w->action.run());v.setFocusable(true);return v;}
    private LinearLayout row(){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);return row;}
    private GradientDrawable round(int color,int radius){GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(dp(radius));return bg;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void open(Intent intent){try{startActivity(intent);}catch(android.content.ActivityNotFoundException|SecurityException e){toast("Android couldn't open that action.");}}
    private void openForResult(Intent intent,int code){try{startActivityForResult(intent,code);}catch(android.content.ActivityNotFoundException|SecurityException e){toast("No document provider is available.");}}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    private void back(){if(!"root".equals(page))showPage("root");else finish();}
    @Override public void onBackPressed(){back();}
    @Override protected void onSaveInstanceState(Bundle state){state.putString("page",page);super.onSaveInstanceState(state);}
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}

    @Override protected void onActivityResult(int code,int result,Intent data){
        super.onActivityResult(code,result,data);if(result!=RESULT_OK || data==null || data.getData()==null)return;
        android.net.Uri uri=data.getData();
        worker.execute(()->{
            try {
                if(code==201){
                    JSONObject backup=PreferenceBackup.export(prefs);
                    try(OutputStream output=getContentResolver().openOutputStream(uri,"wt")){if(output==null)throw new java.io.IOException("No output");output.write(backup.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
                    runOnUiThread(()->toast("Settings exported."));
                }else if(code==202){
                    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                    try(InputStream input=getContentResolver().openInputStream(uri)){
                        if(input==null)throw new java.io.IOException("No input");byte[] buffer=new byte[4096];int count;
                        while((count=input.read(buffer))!=-1){if(bytes.size()+count>262144)throw new IllegalArgumentException("Backup is too large");bytes.write(buffer,0,count);}
                    }
                    JSONObject backup=PreferenceBackup.validate(new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));
                    runOnUiThread(()->{if(isDestroyed())return;new AlertDialog.Builder(this).setTitle("Restore NationDroid settings?").setMessage("This replaces your local launcher preferences and pins. App data is not included.").setNegativeButton("Cancel",null).setPositiveButton("Restore",(d,i)->{
                        try{PreferenceBackup.apply(prefs,backup);changed();render();toast("Settings restored.");}catch(Exception e){toast("This backup could not be restored.");}
                    }).show();});
                }
            }catch(Exception e){runOnUiThread(()->toast("Couldn't read or write the settings backup. Check the file and try again."));}
        });
    }
}
