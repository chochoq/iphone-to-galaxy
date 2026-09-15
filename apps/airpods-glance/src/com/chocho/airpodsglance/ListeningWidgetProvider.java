package com.chocho.airpodsglance;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;
import com.chocho.airpodsglance.ListeningProtocol.Mode;

/** Battery and listening providers deliberately have unrelated IDs and state. */
public abstract class ListeningWidgetProvider extends AppWidgetProvider {
    public static final String ACTION="com.chocho.airpodsglance.LISTENING_WIDGET_TAP";
    public enum Kind {
        WIDE("가로 · 3×1",230,80), SMALL("미니 · 2×1",170,72), SINGLE("한 칸 · 1×1",72,72), LOCK("잠금화면 · 한 칸",72,72);
        public final String title;public final int width,height;
        Kind(String t,int w,int h){title=t;width=w;height=h;}
        public boolean cycle(){return this==SINGLE||this==LOCK;}
    }
    public static final class Wide extends ListeningWidgetProvider {protected Kind kind(){return Kind.WIDE;}}
    public static final class Small extends ListeningWidgetProvider {protected Kind kind(){return Kind.SMALL;}}
    public static final class Single extends ListeningWidgetProvider {protected Kind kind(){return Kind.SINGLE;}}
    public static final class Lock extends ListeningWidgetProvider {protected Kind kind(){return Kind.LOCK;}}
    protected abstract Kind kind();
    public static Class<?> provider(Kind k){return k==Kind.WIDE?Wide.class:k==Kind.SMALL?Small.class:k==Kind.SINGLE?Single.class:Lock.class;}
    public static Kind kindOf(Context c,int id){
        AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);if(info==null)return null;
        for(Kind k:Kind.values())if(info.provider.equals(new ComponentName(c,provider(k))))return k;return null;
    }
    public static boolean owns(Context c,int id){return id>0&&kindOf(c,id)!=null;}
    private static Uri data(int id,int mode){return Uri.parse("airpods-glance://listening-widget/"+id+"/"+mode);}
    static PendingIntent tap(Context c,int id,int mode){
        Intent i=new Intent(c,ListeningWidgetService.class).setAction(ACTION).setData(data(id,mode)).putExtra("widget",id).putExtra("mode",mode);
        return PendingIntent.getForegroundService(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    public static boolean validRequest(Context c,Intent i){
        if(i==null||!ACTION.equals(i.getAction()))return false;
        int id=i.getIntExtra("widget",0),mode=i.getIntExtra("mode",-1);Kind k=kindOf(c,id);
        return k!=null&&data(id,mode).equals(i.getData())&&(k.cycle()?mode==0:ListeningProtocol.canWrite(Mode.fromWire(mode)));
    }
    @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){for(int id:ids)update(c,manager,id,kind());}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){update(c,m,id,kind());}
    @Override public void onDeleted(Context c,int[] ids){for(int id:ids){new ListeningWidgetStore(c).deleteOptions(id);ListeningWidgetService.deleted(id);for(int mode:new int[]{0,2,3,4})tap(c,id,mode).cancel();}}
    public static void updateAll(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);for(Kind k:Kind.values())for(int id:m.getAppWidgetIds(new ComponentName(c,provider(k))))update(c,m,id,k);}
    private static void update(Context c,AppWidgetManager m,int id,Kind kind){
        Bundle options=m.getAppWidgetOptions(id);int width=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,kind.width),height=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,kind.height);
        width=Math.max(48,Math.min(600,width));height=Math.max(48,Math.min(300,height));
        ListeningWidgetStore store=new ListeningWidgetStore(c);
        RemoteViews views=ListeningWidgetRenderer.render(c,store.read(),store.options(id),kind,width,height);
        if(kind.cycle())views.setOnClickPendingIntent(R.id.listening_single,tap(c,id,0));
        else{views.setOnClickPendingIntent(R.id.listening_anc,tap(c,id,2));views.setOnClickPendingIntent(R.id.listening_adaptive,tap(c,id,4));views.setOnClickPendingIntent(R.id.listening_transparency,tap(c,id,3));}
        m.updateAppWidget(id,views);
    }
}
