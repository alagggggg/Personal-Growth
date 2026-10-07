package com.nhatkytimestiming.app;
import android.annotation.SuppressLint;import android.app.Activity;import android.content.Intent;import android.os.Bundle;import android.webkit.JavascriptInterface;import android.webkit.WebSettings;import android.webkit.WebView;import android.webkit.WebViewClient;
public class MainActivity extends Activity{
 private WebView web; private String pendingId;
 @SuppressLint({"SetJavaScriptEnabled","AddJavascriptInterface"}) public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);web=findViewById(R.id.webView);WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);web.addJavascriptInterface(new Bridge(),"AndroidBridge");read(getIntent());web.setWebViewClient(new WebViewClient(){public void onPageFinished(WebView v,String u){dispatch();}});web.loadUrl("file:///android_asset/index.html");}
 protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);read(i);if(web!=null)dispatch();}
 private void read(Intent i){pendingId=i==null?null:i.getStringExtra("activity_id");}
 private void dispatch(){if(pendingId==null)return;String id=pendingId;pendingId=null;web.evaluateJavascript("if(typeof nativeWidgetAction==='function')nativeWidgetAction("+org.json.JSONObject.quote(id)+")",null);}
 public class Bridge{@JavascriptInterface public void syncActivities(String json){getSharedPreferences(ActivityWidgetProvider.DATA_PREFS,MODE_PRIVATE).edit().putString(ActivityWidgetProvider.DATA_KEY,json==null?"[]":json).commit();ActivityWidgetProvider.refreshAll(MainActivity.this);}}
}
