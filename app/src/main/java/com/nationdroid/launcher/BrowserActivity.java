package com.nationdroid.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.print.PrintManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewDatabase;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Independent Android System WebView browser. No JavaScript-to-app bridge or contacts access. */
public final class BrowserActivity extends Activity {
    private final List<Tab> tabs=new ArrayList<>();
    private int selected;
    private LinearLayout tabStrip;
    private FrameLayout page;
    private EditText address;
    private ProgressBar progress;
    private SharedPreferences bookmarks;
    private ValueCallback<Uri[]> fileChoice;
    private boolean destroyed;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);bookmarks=getSharedPreferences("browser",MODE_PRIVATE);buildUi();
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::back);
        ArrayList<String> saved=state==null?null:state.getStringArrayList("tabs");
        if(saved!=null){for(String url:saved)if(tabs.size()<6){String safe=BrowserUrls.input(url);newTab(safe==null?BrowserUrls.HOME:safe);}selected=Math.max(0,Math.min(tabs.size()-1,state.getInt("selected",0)));}
        if(tabs.isEmpty()){String requested=BrowserUrls.input(getIntent().getDataString());newTab(requested==null?BrowserUrls.HOME:requested);}else showSelected();
    }
    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xff202329);
        root.setOnApplyWindowInsetsListener((v,i)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets s=i.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout()|android.view.WindowInsets.Type.ime());v.setPadding(s.left,s.top,s.right,s.bottom);}
            else v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;
        });setContentView(root);root.requestApplyInsets();
        LinearLayout title=row();title.addView(button("‹","Return to launcher",()->open(new Intent(this,MainActivity.class))),new LinearLayout.LayoutParams(dp(44),dp(46)));
        HorizontalScrollView stripScroll=new HorizontalScrollView(this);tabStrip=row();stripScroll.addView(tabStrip);title.addView(stripScroll,new LinearLayout.LayoutParams(0,dp(46),1));
        title.addView(button("+","New tab",()->newTab(BrowserUrls.HOME)),new LinearLayout.LayoutParams(dp(48),dp(46)));root.addView(title);
        LinearLayout navigation=row();
        navigation.addView(button("‹","Back",()->{Tab t=current();if(t!=null&&t.web.canGoBack())t.web.goBack();}),new LinearLayout.LayoutParams(dp(38),dp(48)));
        navigation.addView(button("›","Forward",()->{Tab t=current();if(t!=null&&t.web.canGoForward())t.web.goForward();}),new LinearLayout.LayoutParams(dp(38),dp(48)));
        navigation.addView(button("↻","Reload",()->{Tab t=current();if(t!=null){clearError(t);t.web.reload();}}),new LinearLayout.LayoutParams(dp(38),dp(48)));
        address=new EditText(this);address.setSingleLine(true);address.setTextSize(13);address.setTextColor(0xfff4f5fa);address.setHintTextColor(0xffb7bec9);address.setHint("Search DuckDuckGo or type a URL");address.setSelectAllOnFocus(true);
        address.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_GO||(event!=null&&event.getKeyCode()==android.view.KeyEvent.KEYCODE_ENTER)){navigate(address.getText().toString());address.clearFocus();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0);return true;}return false;});
        navigation.addView(address,new LinearLayout.LayoutParams(0,dp(48),1));TextView menu=button("⋮","Browser menu",()->{});menu.setOnClickListener(v->menu(v));navigation.addView(menu,new LinearLayout.LayoutParams(dp(40),dp(48)));root.addView(navigation);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
        page=new FrameLayout(this);root.addView(page,new LinearLayout.LayoutParams(-1,0,1));
    }
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    private void newTab(String url){
        if(tabs.size()>=6){toast("Close a tab before opening another (maximum six).");return;}
        Tab tab=new Tab();
        try{tab.web=new WebView(this);}catch(RuntimeException e){toast("Android System WebView is unavailable. Enable or update it in Android settings.");return;}
        WebSettings settings=tab.web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setGeolocationEnabled(false);settings.setSafeBrowsingEnabled(true);settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);settings.setSupportMultipleWindows(true);settings.setJavaScriptCanOpenWindowsAutomatically(false);
        CookieManager.getInstance().setAcceptThirdPartyCookies(tab.web,false);
        tab.web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView web,WebResourceRequest request){
                String raw=request.getUrl().toString(),safe=BrowserUrls.navigation(raw);
                if(safe==null){if(request.isForMainFrame())toast("Only secure web addresses are supported; app and local-file links are blocked.");return true;}
                if(!raw.equals(safe)){web.loadUrl(safe);return true;}return false;
            }
            @Override public void onPageStarted(WebView web,String url,Bitmap icon){clearError(tab);if(current()==tab&&!address.hasFocus())address.setText(url);}
            @Override public void onPageFinished(WebView web,String url){tab.title=web.getTitle()==null?"New tab":web.getTitle();renderTabs();if(current()==tab&&!address.hasFocus())address.setText(url);}
            @Override public void onReceivedError(WebView web,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame())showError(tab,"Couldn't load this page. Check your connection, update Android System WebView, then retry.");}
            @Override public void onReceivedSslError(WebView web,SslErrorHandler handler,SslError error){handler.cancel();showError(tab,"The website's secure connection could not be verified. NationDroid did not bypass the certificate error.");}
            @Override public void onFormResubmission(WebView web,Message dontResend,Message resend){new AlertDialog.Builder(BrowserActivity.this).setTitle("Resubmit this form?").setMessage("Reloading may repeat a form submission or transaction.").setNegativeButton("Cancel",(d,w)->dontResend.sendToTarget()).setPositiveButton("Resubmit",(d,w)->resend.sendToTarget()).setOnCancelListener(d->dontResend.sendToTarget()).show();}
        });
        tab.web.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView web,int value){if(current()==tab){progress.setProgress(value);progress.setVisibility(value>=100?View.INVISIBLE:View.VISIBLE);}}
            @Override public void onReceivedTitle(WebView web,String title){tab.title=title==null?"New tab":title;renderTabs();}
            @Override public void onPermissionRequest(PermissionRequest request){request.deny();}
            @Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback callback){callback.invoke(origin,false,false);}
            @Override public boolean onCreateWindow(WebView web,boolean dialog,boolean gesture,Message result){
                if(!gesture||tabs.size()>=6)return false;int before=tabs.size();newTab(null);if(tabs.size()==before)return false;Tab opened=current();if(opened==null)return false;
                ((WebView.WebViewTransport)result.obj).setWebView(opened.web);result.sendToTarget();return true;
            }
            @Override public boolean onShowFileChooser(WebView web,ValueCallback<Uri[]> callback,FileChooserParams params){
                if(fileChoice!=null)fileChoice.onReceiveValue(null);fileChoice=callback;
                Intent choose=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*").putExtra(Intent.EXTRA_ALLOW_MULTIPLE,params.getMode()==FileChooserParams.MODE_OPEN_MULTIPLE);
                try{startActivityForResult(choose,420);}catch(android.content.ActivityNotFoundException e){fileChoice.onReceiveValue(null);fileChoice=null;toast("No document provider is available.");}return true;
            }
        });
        tab.web.setDownloadListener((link,agent,disposition,mime,length)->confirmDownload(link,disposition,mime));
        tabs.add(tab);selected=tabs.size()-1;showSelected();if(url!=null)tab.web.loadUrl(url);
    }
    private void navigate(String input){String url=BrowserUrls.input(input);if(url==null){toast("That address type is blocked.");return;}if(current()!=null){clearError(current());current().web.loadUrl(url);}}
    private Tab current(){return selected>=0&&selected<tabs.size()?tabs.get(selected):null;}
    private void showSelected(){
        page.removeAllViews();for(Tab tab:tabs)tab.web.onPause();Tab tab=current();if(tab!=null){page.addView(tab.web,new FrameLayout.LayoutParams(-1,-1));tab.web.onResume();address.setText(tab.web.getUrl()==null?BrowserUrls.HOME:tab.web.getUrl());if(tab.error!=null)page.addView(tab.error,new FrameLayout.LayoutParams(-1,-1));}renderTabs();
    }
    private void renderTabs(){
        tabStrip.removeAllViews();for(int i=0;i<tabs.size();i++){final int index=i;LinearLayout item=row();item.setBackgroundColor(i==selected?0xff424852:0xff262b32);
            String title=tabs.get(i).title;TextView label=button(title.length()>18?title.substring(0,18)+"…":title,"Select tab "+(i+1),()->{selected=index;showSelected();});label.setTextSize(12);item.addView(label);
            item.addView(button("×","Close tab "+(i+1),()->closeTab(index)),new LinearLayout.LayoutParams(dp(32),dp(44)));tabStrip.addView(item);}
    }
    private void closeTab(int index){if(index<0||index>=tabs.size())return;Tab tab=tabs.remove(index);if(tab.web.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)tab.web.getParent()).removeView(tab.web);tab.web.stopLoading();tab.web.destroy();selected=Math.max(0,Math.min(selected,tabs.size()-1));if(tabs.isEmpty())newTab(BrowserUrls.HOME);else showSelected();}
    private void showError(Tab tab,String message){
        LinearLayout error=new LinearLayout(this);error.setOrientation(LinearLayout.VERTICAL);error.setPadding(dp(24),dp(24),dp(24),dp(24));error.setBackgroundColor(0xff202329);
        error.addView(label("Page unavailable",22));error.addView(label(message,15));error.addView(button("Retry","Retry page",()->{clearError(tab);tab.web.reload();}));tab.error=error;
        if(current()==tab){page.removeAllViews();page.addView(tab.web,new FrameLayout.LayoutParams(-1,-1));page.addView(error,new FrameLayout.LayoutParams(-1,-1));}
    }
    private void clearError(Tab tab){if(tab.error!=null&&tab.error.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)tab.error.getParent()).removeView(tab.error);tab.error=null;}
    private void menu(View anchor){
        PopupMenu menu=new PopupMenu(this,anchor);
        add(menu,"DuckDuckGo home",()->navigate(BrowserUrls.HOME));add(menu,"New tab",()->newTab(BrowserUrls.HOME));
        add(menu,"Bookmark this page",this::saveBookmark);add(menu,"Bookmarks",this::showBookmarks);
        add(menu,"Find in page",this::find);add(menu,"Zoom",this::zoom);add(menu,"Downloads",()->open(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)));
        add(menu,"Share page",()->{if(current()!=null){String url=BrowserUrls.navigation(current().web.getUrl());if(url!=null)open(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,url),"Share page"));}});
        add(menu,"Print",()->{if(current()!=null)((PrintManager)getSystemService(PRINT_SERVICE)).print("NationDroid page",current().web.createPrintDocumentAdapter("NationDroid page"),null);});
        add(menu,"Clear browsing data",this::clearData);add(menu,"Set as default browser",this::defaultBrowser);
        add(menu,"About NationDroid Browser",()->new AlertDialog.Builder(this).setTitle("NationDroid Browser")
                .setMessage("Built with Android System WebView and DuckDuckGo web search. Third-party cookies, local-file access, camera/microphone and location requests are blocked. It is not DuckDuckGo's official browser and does not include its tracking-protection engine or anonymous-network guarantees. Websites and search providers can see your visits and queries. Only HTTPS navigation is supported; typed HTTP addresses are upgraded.").setPositiveButton("OK",null).show());menu.show();
    }
    private void add(PopupMenu menu,String title,Runnable action){menu.getMenu().add(title).setOnMenuItemClickListener(item->{action.run();return true;});}
    private JSONArray bookmarkList(){try{return new JSONArray(bookmarks.getString("items","[]"));}catch(Exception e){return new JSONArray();}}
    private void saveBookmark(){Tab tab=current();if(tab==null)return;String url=BrowserUrls.navigation(tab.web.getUrl());if(url==null)return;JSONArray list=bookmarkList();for(int i=0;i<list.length();i++)if(url.equals(list.optJSONObject(i)==null?null:list.optJSONObject(i).optString("url"))){toast("Already bookmarked.");return;}if(list.length()>=200){toast("Bookmark limit reached. Remove bookmarks first.");return;}
        try{list.put(new JSONObject().put("url",url).put("title",tab.title.substring(0,Math.min(100,tab.title.length()))));bookmarks.edit().putString("items",list.toString()).apply();toast("Bookmark saved.");}catch(Exception ignored){toast("Couldn't save bookmark.");}}
    private void showBookmarks(){JSONArray list=bookmarkList();String[] names=new String[list.length()];for(int i=0;i<names.length;i++){JSONObject b=list.optJSONObject(i);names[i]=b==null?"Invalid bookmark":b.optString("title","Bookmark");}
        new AlertDialog.Builder(this).setTitle("Bookmarks").setItems(names,(d,i)->{JSONObject item=list.optJSONObject(i);if(item!=null)navigate(item.optString("url"));}).setNegativeButton("Close",null).setNeutralButton("Remove all",(d,w)->new AlertDialog.Builder(this).setTitle("Remove all bookmarks?").setNegativeButton("Cancel",null).setPositiveButton("Remove",(a,b)->bookmarks.edit().remove("items").apply()).show()).show();}
    private void find(){if(current()==null)return;EditText term=new EditText(this);term.setSingleLine(true);new AlertDialog.Builder(this).setTitle("Find in page").setView(term).setPositiveButton("Find",(d,w)->{if(current()!=null){current().web.findAllAsync(term.getText().toString());new AlertDialog.Builder(this).setTitle("Find matches").setMessage("Use Next or Previous to move through matches.").setPositiveButton("Next",(a,b)->{if(current()!=null)current().web.findNext(true);}).setNeutralButton("Previous",(a,b)->{if(current()!=null)current().web.findNext(false);}).setNegativeButton("Close",null).show();}}).setNegativeButton("Cancel",null).show();}
    private void zoom(){if(current()==null)return;new AlertDialog.Builder(this).setTitle("Text zoom").setItems(new String[]{"75%","100%","125%","150%","200%"},(d,i)->{if(current()!=null)current().web.getSettings().setTextZoom(new int[]{75,100,125,150,200}[i]);}).show();}
    private void clearData(){new AlertDialog.Builder(this).setTitle("Clear browsing data?").setMessage("Closes all tabs and clears cookies, cached pages, in-tab history, site storage and saved web authentication. Bookmarks and downloaded files are retained.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{
        for(Tab tab:tabs){tab.web.stopLoading();tab.web.clearCache(true);tab.web.clearHistory();}page.removeAllViews();for(Tab tab:tabs)tab.web.destroy();tabs.clear();selected=0;
        WebStorage.getInstance().deleteAllData();WebViewDatabase.getInstance(this).clearHttpAuthUsernamePassword();
        CookieManager.getInstance().removeAllCookies(done->{CookieManager.getInstance().flush();if(!destroyed){newTab(BrowserUrls.HOME);toast("Browsing data cleared.");}});
    }).show();}
    private void confirmDownload(String input,String disposition,String mime){String url=BrowserUrls.navigation(input);if(url==null){toast("Only HTTPS downloads are supported.");return;}String name=BrowserUrls.filename(URLUtil.guessFileName(url,disposition,mime));new AlertDialog.Builder(this).setTitle("Download file?").setMessage(name+"\nFrom "+Uri.parse(url).getHost()+"\nAuthenticated or blob downloads may require a different browser.").setNegativeButton("Cancel",null).setPositiveButton("Download",(d,w)->{
        try{DownloadManager.Request request=new DownloadManager.Request(Uri.parse(url)).setTitle(name).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            if(Build.VERSION.SDK_INT>=29)request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,name);else request.setDestinationInExternalFilesDir(this,Environment.DIRECTORY_DOWNLOADS,name);
            ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(request);toast("Download requested. Check Downloads for its status.");
        }catch(RuntimeException e){toast("Android couldn't start this download.");}
    }).show();}
    private void defaultBrowser(){if(Build.VERSION.SDK_INT>=29){RoleManager roles=getSystemService(RoleManager.class);if(roles!=null&&roles.isRoleAvailable(RoleManager.ROLE_BROWSER)&&!roles.isRoleHeld(RoleManager.ROLE_BROWSER)){open(roles.createRequestRoleIntent(RoleManager.ROLE_BROWSER));return;}}open(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));}
    private void back(){if(current()!=null&&current().web.canGoBack()){clearError(current());current().web.goBack();}else finish();}
    @Override public void onBackPressed(){back();}
    @Override protected void onSaveInstanceState(Bundle state){ArrayList<String> urls=new ArrayList<>();for(Tab tab:tabs)urls.add(tab.web.getUrl()==null?BrowserUrls.HOME:tab.web.getUrl());state.putStringArrayList("tabs",urls);state.putInt("selected",selected);super.onSaveInstanceState(state);}
    @Override protected void onResume(){super.onResume();if(current()!=null){current().web.onResume();current().web.resumeTimers();}}
    @Override protected void onPause(){if(current()!=null){current().web.onPause();current().web.pauseTimers();}if(!tabs.isEmpty())CookieManager.getInstance().flush();super.onPause();}
    @Override protected void onDestroy(){destroyed=true;if(fileChoice!=null)fileChoice.onReceiveValue(null);page.removeAllViews();for(Tab tab:tabs){tab.web.stopLoading();tab.web.destroy();}tabs.clear();super.onDestroy();}
    @Override protected void onActivityResult(int code,int result,Intent data){super.onActivityResult(code,result,data);if(code==420&&fileChoice!=null){Uri[] chosen=null;if(result==RESULT_OK&&data!=null){if(data.getClipData()!=null){chosen=new Uri[data.getClipData().getItemCount()];for(int i=0;i<chosen.length;i++)chosen[i]=data.getClipData().getItemAt(i).getUri();}else if(data.getData()!=null)chosen=new Uri[]{data.getData()};}fileChoice.onReceiveValue(chosen);fileChoice=null;}}
    private LinearLayout row(){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);return row;}
    private TextView label(String value,int size){TextView text=new TextView(this);text.setText(value);text.setTextSize(size);text.setTextColor(0xfff5f6fa);text.setPadding(dp(10),dp(10),dp(10),dp(10));return text;}
    private TextView button(String value,String description,Runnable action){TextView view=label(value,20);view.setGravity(Gravity.CENTER);view.setMinHeight(dp(44));view.setContentDescription(description);view.setFocusable(true);view.setOnClickListener(v->action.run());return view;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    private void open(Intent intent){try{startActivity(intent);}catch(android.content.ActivityNotFoundException|SecurityException e){toast("No app is available for that action.");}}
    private static final class Tab{WebView web;String title="New tab";View error;}
}
