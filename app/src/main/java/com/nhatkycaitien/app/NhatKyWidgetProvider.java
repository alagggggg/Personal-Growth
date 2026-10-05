package com.nhatkycaitien.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NhatKyWidgetProvider extends AppWidgetProvider {
    public static final String PREFS="widget_data";
    public static final String KEY="cases";
    public static final String EXTRA_CASE_ID="open_case_id";

    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids){for(int id:ids)update(c,m,id);}
    @Override public void onEnabled(Context c){refreshAll(c);}

    public static void refreshAll(Context c){
        AppWidgetManager m=AppWidgetManager.getInstance(c);
        int[] ids=m.getAppWidgetIds(new ComponentName(c,NhatKyWidgetProvider.class));
        for(int id:ids)update(c,m,id);
    }

    private static PendingIntent appIntent(Context c,int requestCode,String caseId){
        Intent i=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if(caseId!=null&&!caseId.isEmpty()){
            i.putExtra(EXTRA_CASE_ID,caseId);
            i.setData(Uri.parse("nhatkycaitien://case/"+Uri.encode(caseId)));
        }else i.setData(Uri.parse("nhatkycaitien://home/"+requestCode));
        return PendingIntent.getActivity(c,requestCode,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }

    private static void update(Context c,AppWidgetManager m,int id){
        try {
        RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget_nhat_ky);
        PendingIntent home=appIntent(c,id*10,null);
        v.setOnClickPendingIntent(R.id.widget_root,home);
        v.setOnClickPendingIntent(R.id.widget_header,home);
        v.setOnClickPendingIntent(R.id.widget_add,home);
        List<JSONObject> items=new ArrayList<>();
        try{
            JSONArray a=new JSONArray(c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"[]"));
            for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)items.add(o);}
        }catch(Exception ignored){}
        Collections.sort(items,new Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){return b.optString("updatedAt","").compareTo(a.optString("updatedAt",""));}});
        int fresh=0,processing=0,done=0;
        for(JSONObject o:items){String s=o.optString("status","new");if("new".equals(s))fresh++;else if(isDone(s))done++;else processing++;}
        v.setTextViewText(R.id.count_new,fresh+"\nMới");
        v.setTextViewText(R.id.count_processing,processing+"\nXử lý");
        v.setTextViewText(R.id.count_done,done+"\nHoàn tất");
        bind(c,v,id,1,items.size()>0?items.get(0):null);
        bind(c,v,id,2,items.size()>1?items.get(1):null);
        bind(c,v,id,3,items.size()>2?items.get(2):null);
        m.updateAppWidget(id,v);
    
        } catch(Throwable error){
            RemoteViews fallback=new RemoteViews(c.getPackageName(),R.layout.widget_nhat_ky);
            fallback.setTextViewText(R.id.count_new,"0\nMới");fallback.setTextViewText(R.id.count_processing,"0\nXử lý");fallback.setTextViewText(R.id.count_done,"0\nHoàn tất");
            fallback.setTextViewText(R.id.title1,"Mở ứng dụng để đồng bộ");
            PendingIntent home=appIntent(c,id*10,null);fallback.setOnClickPendingIntent(R.id.widget_root,home);fallback.setOnClickPendingIntent(R.id.widget_add,home);
            m.updateAppWidget(id,fallback);
        }
    }
    private static boolean isDone(String s){return "closed".equals(s)||"done".equals(s)||"completed".equals(s);}
    private static String status(String s){if("new".equals(s))return "Mới";if("analysis".equals(s))return "Phân tích";if("plan".equals(s))return "Kế hoạch";if("testing".equals(s)||"experiment".equals(s))return "Thử nghiệm";if("review".equals(s))return "Đánh giá";if(isDone(s))return "Đã đóng";return "Xử lý";}
    private static String category(String s){if("work".equals(s))return "Công việc";if("study".equals(s))return "Học tập";if("time".equals(s))return "Quản lý thời gian";if("focus".equals(s))return "Tập trung";if("family".equals(s))return "Gia đình & sinh hoạt";if("finance".equals(s))return "Tài chính";if("health".equals(s))return "Sức khỏe";if("relation".equals(s))return "Quan hệ";if("travel".equals(s))return "Di chuyển";if("tech".equals(s))return "Công nghệ & thiết bị";if("process".equals(s))return "Quy trình";return "Tùy chỉnh";}
private static void bind(Context c,RemoteViews v,int widgetId,int n,JSONObject o){
        int row=n==1?R.id.row1:n==2?R.id.row2:R.id.row3;
        int accent=n==1?R.id.accent1:n==2?R.id.accent2:R.id.accent3;
        int title=n==1?R.id.title1:n==2?R.id.title2:R.id.title3;
        int st=n==1?R.id.status1:n==2?R.id.status2:R.id.status3;
        int meta=n==1?R.id.meta1:n==2?R.id.meta2:R.id.meta3;
        int dots=n==1?R.id.dots1:n==2?R.id.dots2:R.id.dots3;
        if(o==null){
            v.setTextViewText(title,n==1?"Chưa có hồ sơ":"");v.setTextViewText(st,"");v.setTextViewText(meta,n==1?"Nhấn + để ghi nhận":"");v.setTextViewText(dots,"");
            v.setOnClickPendingIntent(row,appIntent(c,widgetId*10+n,null));return;
        }
        String categoryCode=o.optString("category","");
        int accentColor=categoryColor(categoryCode);
        v.setInt(accent,"setBackgroundColor",accentColor);
        String loc=o.optString("location","");
        String line=category(categoryCode);
        if(!loc.isEmpty())line+=" - "+loc;
        int sev=Math.max(0,Math.min(5,o.optInt("severity",0)));
        StringBuilder d=new StringBuilder();for(int i=0;i<5;i++)d.append(i<sev?'●':'○');
        String caseId=o.optString("id","");
        v.setTextViewText(title,o.optString("title","Chưa đặt tên"));
        String statusCode=o.optString("status","new");
        v.setTextViewText(st,status(statusCode));
        v.setTextColor(st,statusColor(statusCode));
        v.setTextViewText(meta,line);
        v.setTextViewText(dots,d.toString());
        v.setOnClickPendingIntent(row,appIntent(c,widgetId*10+n,caseId));
    }
    private static int categoryColor(String s){if("work".equals(s))return 0xFF1477EA;if("study".equals(s))return 0xFF8B5CF6;if("time".equals(s))return 0xFFF59E0B;if("focus".equals(s))return 0xFF7C3AED;if("family".equals(s))return 0xFFEC4899;if("finance".equals(s))return 0xFF16A34A;if("health".equals(s))return 0xFFEF4444;if("relation".equals(s))return 0xFF0EA5E9;if("travel".equals(s))return 0xFF0891B2;if("tech".equals(s))return 0xFF2563EB;if("process".equals(s))return 0xFF64748B;return 0xFF94A3B8;}
    private static int statusColor(String s){if("new".equals(s))return 0xFF1477EA;if("closed".equals(s)||"done".equals(s)||"completed".equals(s))return 0xFF16855B;if("analysis".equals(s))return 0xFF7C3AED;if("plan".equals(s))return 0xFFB45309;if("testing".equals(s)||"experiment".equals(s))return 0xFF0F766E;if("review".equals(s))return 0xFF475569;return 0xFF1477EA;}

}
