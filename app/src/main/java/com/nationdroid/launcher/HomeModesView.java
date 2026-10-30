package com.nationdroid.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Alternate native home surfaces: paged grid, alphabetic list and 3D sphere. */
final class HomeModesView extends LinearLayout {
    private final SharedPreferences prefs;
    private final List<MainActivity.AppEntry> apps;
    private final Set<String> favorites;
    private final Consumer<MainActivity.AppEntry> launch,menu;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private HoloSphereView sphere;
    private TextView clock;
    private final Runnable tick=new Runnable(){@Override public void run(){if(clock!=null){String pattern=android.text.format.DateFormat.is24HourFormat(getContext())?"HH:mm":"h:mm a";clock.setText(new SimpleDateFormat(pattern,Locale.getDefault()).format(new Date())+"\n"+new SimpleDateFormat("EEE, d MMM",Locale.getDefault()).format(new Date()));}handler.postDelayed(this,1000);}};

    HomeModesView(Context context,List<MainActivity.AppEntry> apps,Set<String> favorites,SharedPreferences prefs,
                  Consumer<MainActivity.AppEntry> launch,Consumer<MainActivity.AppEntry> menu,Runnable settings,Runnable allApps) {
        super(context);this.prefs=prefs;this.apps=new ArrayList<>(apps);this.favorites=favorites;this.launch=launch;this.menu=menu;
        setOrientation(VERTICAL);setPadding(dp(14),dp(8),dp(14),dp(6));
        boolean glass="glass".equals(prefs.getString("surface","solid"));
        setBackgroundColor(LauncherStyle.dark(prefs)?(glass?0xb3080d18:0xff080d18):(glass?0xddeef1fa:0xffeef1fa));
        LinearLayout top=row();
        if(prefs.getBoolean("topWidget",true)) {
            clock=text("",24);clock.setPadding(dp(4),0,0,dp(6));top.addView(clock,new LayoutParams(0,-2,1));
        } else top.addView(text("NationDroid",17),new LayoutParams(0,dp(48),1));
        top.addView(button("Apps list",allApps));top.addView(button("Settings",settings));addView(top);
        String mode=prefs.getString("mode","desktop");
        FrameLayout center=new FrameLayout(context);
        addView(center,new LayoutParams(-1,0,1));
        if("sphere".equals(mode)) {
            boolean custom=prefs.getBoolean("sphereCustom",false);
            Set<String> selected=prefs.getStringSet("sphereApps",new HashSet<>());
            List<MainActivity.AppEntry> sphereApps=new ArrayList<>();
            for(MainActivity.AppEntry app:apps)if(!custom || selected.contains(app.key()))sphereApps.add(app);
            sphere=new HoloSphereView(context,sphereApps,prefs,launch,menu);
            center.addView(sphere,new FrameLayout.LayoutParams(-1,-1));
        } else if("minimal".equals(mode)) buildMinimal(center);else buildClassic(center);
        if(prefs.getBoolean("bottomDock",true)) {
            HorizontalScrollView dock=new HorizontalScrollView(context);dock.setHorizontalScrollBarEnabled(false);
            LinearLayout entries=row();
            for(MainActivity.AppEntry app:apps)if(favorites.contains(app.key()))entries.addView(tile(app),new LayoutParams(dp(72),dp(82)));
            if(entries.getChildCount()==0){TextView hint=text("Long press an app to pin it to your dock.",12);entries.addView(hint,new LayoutParams(-2,dp(42)));}
            dock.addView(entries);addView(dock,new LayoutParams(-1,-2));
        }
    }
    public void setActive(boolean active){if(sphere!=null)sphere.setActive(active);if(active){handler.removeCallbacks(tick);handler.post(tick);}else handler.removeCallbacks(tick);}
    private void buildMinimal(FrameLayout center) {
        LinearLayout stack=new LinearLayout(getContext());stack.setOrientation(VERTICAL);center.addView(stack,new FrameLayout.LayoutParams(-1,-1));
        EditText query=searchBox();stack.addView(query,new LayoutParams(-1,dp(46)));
        LinearLayout listRow=row();stack.addView(listRow,new LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(getContext());LinearLayout list=new LinearLayout(getContext());list.setOrientation(VERTICAL);scroll.addView(list);
        listRow.addView(scroll,new LayoutParams(0,-1,1));
        ScrollView indexScroll=new ScrollView(getContext());LinearLayout index=new LinearLayout(getContext());index.setOrientation(VERTICAL);indexScroll.addView(index);
        listRow.addView(indexScroll,new LayoutParams(dp(28),-1));
        Runnable render=()->{
            list.removeAllViews();index.removeAllViews();Map<String,View> anchors=new LinkedHashMap<>();String previous="";
            for(MainActivity.AppEntry app:apps) {
                if(!AppRules.matches(app.label,app.component.getPackageName(),query.getText().toString()))continue;
                String letter=app.label.isEmpty()?"#":app.label.substring(0,1).toUpperCase(Locale.getDefault());
                if(!Character.isLetter(letter.charAt(0)))letter="#";
                if(!letter.equals(previous)) {
                    TextView heading=text(letter,15);heading.setTextColor(LauncherStyle.accent(prefs));heading.setPadding(dp(8),dp(14),0,dp(4));
                    list.addView(heading);anchors.put(letter,heading);previous=letter;
                }
                LinearLayout entry=row();entry.setPadding(dp(6),dp(8),dp(6),dp(8));
                StyledIconView icon=new StyledIconView(getContext(),app,prefs);entry.addView(icon,new LayoutParams(dp(36),dp(36)));
                TextView name=text(app.label,18);name.setPadding(dp(14),0,0,0);entry.addView(name,new LayoutParams(0,dp(46),1));
                entry.setFocusable(true);entry.setContentDescription(app.label);entry.setOnClickListener(v->launch.accept(app));
                entry.setOnLongClickListener(v->{menu.accept(app);return true;});list.addView(entry);
            }
            if(list.getChildCount()==0)list.addView(text("No matching apps",14));
            for(Map.Entry<String,View> anchor:anchors.entrySet()) {
                TextView letter=text(anchor.getKey(),11);letter.setGravity(Gravity.CENTER);letter.setTextColor(LauncherStyle.accent(prefs));
                letter.setContentDescription("Jump to "+anchor.getKey());letter.setOnClickListener(v->scroll.smoothScrollTo(0,anchor.getValue().getTop()));
                index.addView(letter,new LayoutParams(-1,dp(26)));
            }
        };
        query.addTextChangedListener(watcher(render));render.run();
    }
    private void buildClassic(FrameLayout center) {
        LinearLayout stack=new LinearLayout(getContext());stack.setOrientation(VERTICAL);center.addView(stack,new FrameLayout.LayoutParams(-1,-1));
        EditText query=searchBox();stack.addView(query,new LayoutParams(-1,dp(46)));
        HorizontalScrollView pages=new HorizontalScrollView(getContext());pages.setFillViewport(true);pages.setHorizontalScrollBarEnabled(true);
        LinearLayout strip=row();strip.setGravity(Gravity.TOP);pages.addView(strip);stack.addView(pages,new LayoutParams(-1,0,1));
        TextView help=text("Swipe horizontally for more pages · Long press to pin",11);help.setPadding(0,dp(6),0,dp(6));stack.addView(help);
        Runnable render=()->{
            strip.removeAllViews();List<MainActivity.AppEntry> filtered=new ArrayList<>();
            for(MainActivity.AppEntry app:apps)if(AppRules.matches(app.label,app.component.getPackageName(),query.getText().toString()))filtered.add(app);
            int width=pages.getWidth()>0?pages.getWidth():getResources().getDisplayMetrics().widthPixels-dp(40);
            int cols=Math.max(2,width/dp(82));
            int rows=Math.max(1,Math.min(5,(pages.getHeight()>0?pages.getHeight():dp(400))/dp(96)));
            int perPage=cols*rows;
            for(int first=0;first<filtered.size();first+=perPage) {
                ScrollView page=new ScrollView(getContext());GridLayout grid=new GridLayout(getContext());grid.setColumnCount(cols);
                for(int i=first;i<Math.min(first+perPage,filtered.size());i++) {
                    GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=width/cols;p.height=dp(96);
                    grid.addView(tile(filtered.get(i)),p);
                }
                page.addView(grid);strip.addView(page,new LayoutParams(width,-1));
            }
            if(filtered.isEmpty())strip.addView(text("No matching apps",14));
            pages.scrollTo(0,0);
        };
        pages.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol || b-t!=ob-ot)render.run();});
        query.addTextChangedListener(watcher(render));render.run();
    }
    private LinearLayout tile(MainActivity.AppEntry app) {
        LinearLayout tile=new LinearLayout(getContext());tile.setOrientation(VERTICAL);tile.setGravity(Gravity.CENTER);tile.setFocusable(true);tile.setContentDescription(app.label);
        StyledIconView icon=new StyledIconView(getContext(),app,prefs);int size=dp(30+prefs.getInt("iconSize",55)*.25f);tile.addView(icon,new LayoutParams(size,size));
        if(prefs.getBoolean("labels",true)) {TextView label=text(app.label,8+prefs.getInt("labelSize",50)*.1f);label.setGravity(Gravity.CENTER);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);tile.addView(label,new LayoutParams(-1,dp(30)));}
        tile.setOnClickListener(v->launch.accept(app));tile.setOnLongClickListener(v->{menu.accept(app);return true;});return tile;
    }
    private EditText searchBox(){EditText q=new EditText(getContext());q.setSingleLine(true);q.setTextSize(14);q.setTextColor(LauncherStyle.ink(prefs));q.setHintTextColor(LauncherStyle.dark(prefs)?0xff9ca8bc:0xff586174);q.setHint("Search apps");return q;}
    private TextWatcher watcher(Runnable r){return new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){r.run();}public void afterTextChanged(Editable e){}};}
    private LinearLayout row(){LinearLayout r=new LinearLayout(getContext());r.setOrientation(HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);return r;}
    private TextView text(String value,float size){TextView v=new TextView(getContext());v.setText(value);v.setTextSize(size);v.setTextColor(LauncherStyle.ink(prefs));v.setTypeface(LauncherStyle.font(prefs));return v;}
    private TextView button(String label,Runnable run){TextView v=text(label,12);v.setTextColor(LauncherStyle.accent(prefs));v.setGravity(Gravity.CENTER);v.setPadding(dp(8),dp(14),dp(8),dp(14));v.setMinHeight(dp(48));v.setOnClickListener(w->run.run());v.setFocusable(true);return v;}
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();handler.post(tick);}
    @Override protected void onDetachedFromWindow(){handler.removeCallbacksAndMessages(null);super.onDetachedFromWindow();}
}
