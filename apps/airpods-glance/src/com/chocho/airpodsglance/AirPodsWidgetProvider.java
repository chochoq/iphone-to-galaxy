package com.chocho.airpodsglance;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.os.Bundle;
import android.util.SizeF;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class AirPodsWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_REFRESH =
            "com.chocho.airpodsglance.action.WIDGET_REFRESH";
    private static final String ACTION_CYCLE="com.chocho.airpodsglance.action.WIDGET_CYCLE_PART";
    private static int selectedPart(Context context,int id){return Math.floorMod(context.getSharedPreferences("single_widget_parts",Context.MODE_PRIVATE).getInt("widget_"+id,0),3);}
    protected WidgetVariant variant(){return WidgetVariant.WIDE;}
    public static Class<? extends AirPodsWidgetProvider> providerFor(WidgetVariant variant){
        return variant==WidgetVariant.SINGLE?AirPodsSingleWidgetProvider.class:variant==WidgetVariant.SMALL?AirPodsSmallWidgetProvider.class:variant==WidgetVariant.LARGE?AirPodsLargeWidgetProvider.class:AirPodsWidgetProvider.class;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) manager.updateAppWidget(id, render(context,manager.getAppWidgetOptions(id),variant(),id));
    }

    @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int id,Bundle options){
        manager.updateAppWidget(id,render(context,options,variant(),id));
    }

    @Override public void onDeleted(Context context,int[] ids){
        if(variant()!=WidgetVariant.SINGLE)return;
        android.content.SharedPreferences.Editor edit=context.getSharedPreferences("single_widget_parts",Context.MODE_PRIVATE).edit();
        for(int id:ids)edit.remove("widget_"+id);edit.apply();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(ACTION_CYCLE.equals(intent.getAction())){
            int id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
            AppWidgetManager manager=AppWidgetManager.getInstance(context);
            android.appwidget.AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);
            if(variant()!=WidgetVariant.SINGLE||info==null||!new ComponentName(context,AirPodsSingleWidgetProvider.class).equals(info.provider))return;
            context.getSharedPreferences("single_widget_parts",Context.MODE_PRIVATE).edit().putInt("widget_"+id,(selectedPart(context,id)+1)%3).apply();
            manager.updateAppWidget(id,render(context,manager.getAppWidgetOptions(id),variant(),id));return;
        }
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppSettings settings = new AppSettings(context);
            if (settings.selectedAddress() != null && settings.monitorEnabled()) {
                new ConnectionSessionStore(context).beginIfNeeded();
                AirPodsMonitorService.start(context, false);
            } else {
                Intent open = new Intent(context, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(open);
            }
        }
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for(WidgetVariant variant:WidgetVariant.values()){
            ComponentName provider = new ComponentName(context,providerFor(variant));
            int[] ids = manager.getAppWidgetIds(provider);
            for (int id : ids) manager.updateAppWidget(id, render(context,manager.getAppWidgetOptions(id),variant,id));
        }
        AirPodsLockWidgetProvider.updateAll(context);
    }

    private static RemoteViews render(Context context,Bundle options,WidgetVariant variant,int id) {
        AirPodsSnapshot snapshot = new BatteryStateStore(context).load();
        long now = System.currentTimeMillis();
        UserOptions settings=new AppSettings(context).options();
        ArrayList<SizeF> sizes=options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
        if(sizes==null||sizes.isEmpty()){
            sizes=new ArrayList<>();sizes.add(new SizeF(Math.max(1,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,variant.previewWidth)),Math.max(1,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,variant.previewHeight))));
        }
        Map<SizeF,RemoteViews> mapping=new LinkedHashMap<>();
        for(SizeF size:sizes){
            if(mapping.size()>=8)break;
            if(size==null||!Float.isFinite(size.getWidth())||!Float.isFinite(size.getHeight())||size.getWidth()<=0||size.getHeight()<=0)continue;
            RemoteViews view=WidgetRenderer.render(context,snapshot,now,size.getWidth(),size.getHeight(),options.getFloat("hsResizeRatio",1),settings.budThreshold,settings.caseThreshold,variant,selectedPart(context,id));
            bindClicks(context,view,id,WidgetLayout.forVariant(variant,size.getWidth(),size.getHeight(),context.getResources().getConfiguration().fontScale).mode==WidgetLayout.Mode.SINGLE);mapping.put(size,view);
        }
        if(mapping.isEmpty()){
            RemoteViews view=WidgetRenderer.render(context,snapshot,now,variant.previewWidth,variant.previewHeight,1,settings.budThreshold,settings.caseThreshold,variant,selectedPart(context,id));bindClicks(context,view,id,WidgetLayout.forVariant(variant,variant.previewWidth,variant.previewHeight,context.getResources().getConfiguration().fontScale).mode==WidgetLayout.Mode.SINGLE);return view;
        }
        return mapping.size()==1?mapping.values().iterator().next():new RemoteViews(mapping);
    }
    private static void bindClicks(Context context,RemoteViews views,int id,boolean cyclePart){
        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent open = PendingIntent.getActivity(context, 601, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, open);
        if(cyclePart){
            Intent cycle=new Intent(context,AirPodsSingleWidgetProvider.class).setAction(ACTION_CYCLE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
            views.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getBroadcast(context,id,cycle,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        }

        Intent refreshIntent = new Intent(context, AirPodsWidgetProvider.class)
                .setAction(ACTION_REFRESH);
        PendingIntent refresh = PendingIntent.getBroadcast(context, 602, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_refresh, refresh);
    }
}
