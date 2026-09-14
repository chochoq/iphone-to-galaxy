package com.chocho.airpodsglance;

import android.content.Context;
import android.content.res.Configuration;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Calendar;
import java.util.Date;

/** Native widget text and small code-drawn images; reusable by an isolated RemoteViews host. */
public final class WidgetRenderer {
    private WidgetRenderer(){}
    public static RemoteViews render(Context context,AirPodsSnapshot snapshot,long now,float width,float height,float hostScale,int budThreshold,int caseThreshold){
        return render(context,snapshot,now,width,height,hostScale,budThreshold,caseThreshold,WidgetVariant.WIDE);
    }
    public static RemoteViews render(Context context,AirPodsSnapshot snapshot,long now,float width,float height,float hostScale,int budThreshold,int caseThreshold,WidgetVariant variant){
        return render(context,snapshot,now,width,height,hostScale,budThreshold,caseThreshold,variant,0);
    }
    public static RemoteViews render(Context context,AirPodsSnapshot snapshot,long now,float width,float height,float hostScale,int budThreshold,int caseThreshold,WidgetVariant variant,int selectedPart){
        WidgetState state=WidgetState.from(snapshot,now);
        WidgetLayout layout=WidgetLayout.forVariant(variant,width,height,context.getResources().getConfiguration().fontScale);
        boolean large=layout.mode==WidgetLayout.Mode.LARGE_RINGS,rings=layout.mode==WidgetLayout.Mode.RINGS||large,list=layout.mode==WidgetLayout.Mode.LIST,open=layout.mode==WidgetLayout.Mode.OPEN_APP;
        boolean dark=(context.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        WidgetSizing sizing=WidgetSizing.of(variant,width,height,context.getResources().getConfiguration().fontScale,hostScale,layout.refresh);
        if(layout.mode==WidgetLayout.Mode.SINGLE)return single(context,state,now,dark,budThreshold,caseThreshold,sizing,Math.floorMod(selectedPart,3));
        if(layout.mode==WidgetLayout.Mode.SQUARE)return square(context,state,now,hostScale,dark,budThreshold,caseThreshold,sizing);
        RemoteViews view=new RemoteViews(context.getPackageName(),list?R.layout.airpods_widget_list:R.layout.airpods_widget);
        int padX=dp(context,rings?sizing.padX:width>=240?16:8),padY=dp(context,rings?sizing.padY:height<80?2:6);
        view.setViewPadding(R.id.widget_root,padX,padY,padX,padY);
        view.setViewVisibility(R.id.widget_title,View.GONE);
        view.setTextViewText(R.id.widget_title,state.title);view.setViewPadding(R.id.widget_title,0,0,0,dp(context,8));
        view.setViewVisibility(R.id.widget_body,open?View.GONE:View.VISIBLE);
        view.setViewVisibility(R.id.widget_status,open||(rings&&sizing.strip)?View.GONE:View.VISIBLE);
        view.setTextViewTextSize(R.id.widget_status,TypedValue.COMPLEX_UNIT_SP,rings?14:11);
        view.setViewVisibility(R.id.widget_open,open?View.VISIBLE:View.GONE);
        view.removeAllViews(R.id.widget_parts);
        for(int i=0;i<3;i++){
            RemoteViews part=new RemoteViews(context.getPackageName(),list?R.layout.widget_part_list:R.layout.widget_part);
            part.setTextViewText(R.id.part_percent,state.percent(i));
            part.setTextViewTextSize(R.id.part_percent,TypedValue.COMPLEX_UNIT_SP,rings?sizing.percent:22);
            part.setTextViewTextSize(R.id.part_label,TypedValue.COMPLEX_UNIT_SP,rings?sizing.label:11);
            part.setViewVisibility(R.id.part_label,rings&&sizing.strip?View.GONE:View.VISIBLE);
            part.setTextViewText(R.id.part_label,state.labels[i]+(state.charging[i]&&!rings?" ⚡\uFE0E":""));
            part.setViewVisibility(R.id.part_ring,rings?View.VISIBLE:View.GONE);
            if(rings){
                float ring=sizing.ring;part.setViewLayoutHeight(R.id.part_ring,ring,TypedValue.COMPLEX_UNIT_DIP);
                part.setImageViewBitmap(R.id.part_ring,WidgetArtwork.ring(Math.min(384,dp(context,ring)),i,state.parts[i]==null?null:state.parts[i].percent,state.charging[i],dark,state.fresh,i==2?caseThreshold:budThreshold));
            }
            view.addView(R.id.widget_parts,part);
        }
        // Preserve the visible 48dp action target; content uses the full host area above.
        float scale=WidgetSizing.hostScale(hostScale);
        int hit=(int)Math.ceil(48/scale);
        view.setViewVisibility(R.id.widget_refresh,layout.refresh?View.VISIBLE:View.GONE);
        view.setViewVisibility(R.id.widget_refresh_space,layout.refresh?View.VISIBLE:View.GONE);
        view.setViewLayoutWidth(R.id.widget_refresh_space,hit,TypedValue.COMPLEX_UNIT_DIP);
        view.setViewLayoutWidth(R.id.widget_refresh,hit,TypedValue.COMPLEX_UNIT_DIP);
        view.setViewLayoutHeight(R.id.widget_refresh,hit,TypedValue.COMPLEX_UNIT_DIP);
        view.setImageViewBitmap(R.id.widget_refresh,WidgetArtwork.refresh(dp(context,32/scale),dark));
        view.setViewPadding(R.id.widget_refresh,dp(context,12),dp(context,12),dp(context,12),dp(context,12));
        String stamp=timestamp(context,state.observed,now);
        String status=stamp.isEmpty()?state.connection:(width<260?stamp+" 확인":state.connection+" · "+stamp+" 확인");
        view.setTextViewText(R.id.widget_status,status);
        view.setViewPadding(R.id.widget_status,dp(context,width<240?8:0),dp(context,height<80?2:5),layout.refresh?dp(context,hit+4):0,0);
        String description=state.title+". "+state.description(0)+", "+state.description(1)+", "+state.description(2)+". "+status;
        if(open)description+=". 위젯을 넓히면 잔량이 보여요";
        view.setContentDescription(R.id.widget_root,description+". 눌러서 앱 열기");
        return view;
    }
    private static RemoteViews single(Context context,WidgetState state,long now,boolean dark,int budThreshold,int caseThreshold,WidgetSizing sizing,int i){
        RemoteViews view=new RemoteViews(context.getPackageName(),R.layout.airpods_widget_single);
        RemoteViews part=new RemoteViews(context.getPackageName(),R.layout.widget_part);
        part.setViewVisibility(R.id.part_ring,View.VISIBLE);part.setViewLayoutHeight(R.id.part_ring,sizing.ring,TypedValue.COMPLEX_UNIT_DIP);
        part.setImageViewBitmap(R.id.part_ring,WidgetArtwork.ring(Math.min(384,dp(context,sizing.ring)),i,state.parts[i]==null?null:state.parts[i].percent,state.charging[i],dark,state.fresh,i==2?caseThreshold:budThreshold));
        part.setTextViewText(R.id.part_percent,state.percent(i));part.setTextViewTextSize(R.id.part_percent,TypedValue.COMPLEX_UNIT_SP,sizing.percent);
        part.setTextViewText(R.id.part_label,state.labels[i]);part.setTextViewTextSize(R.id.part_label,TypedValue.COMPLEX_UNIT_SP,sizing.label);
        view.removeAllViews(R.id.widget_parts);view.addView(R.id.widget_parts,part);
        String stamp=timestamp(context,state.observed,now);
        view.setContentDescription(R.id.widget_root,state.title+". "+state.description(i)+". "+state.connection+(stamp.isEmpty()?"":". "+stamp+" 확인")+". 눌러서 다음 잔량 보기: "+state.labels[(i+1)%3]);
        return view;
    }
    private static RemoteViews square(Context context,WidgetState state,long now,float hostScale,boolean dark,int budThreshold,int caseThreshold,WidgetSizing sizing){
        RemoteViews view=new RemoteViews(context.getPackageName(),R.layout.airpods_widget_square);
        view.removeAllViews(R.id.widget_parts);view.removeAllViews(R.id.widget_bottom);
        for(int i=0;i<3;i++){
            RemoteViews part=new RemoteViews(context.getPackageName(),R.layout.widget_part);
            part.setTextViewText(R.id.part_percent,state.percent(i));part.setTextViewTextSize(R.id.part_percent,TypedValue.COMPLEX_UNIT_SP,sizing.percent);
            part.setTextViewText(R.id.part_label,state.labels[i]);part.setTextViewTextSize(R.id.part_label,TypedValue.COMPLEX_UNIT_SP,sizing.label);part.setViewVisibility(R.id.part_ring,View.VISIBLE);
            part.setViewLayoutHeight(R.id.part_ring,sizing.ring,TypedValue.COMPLEX_UNIT_DIP);
            part.setImageViewBitmap(R.id.part_ring,WidgetArtwork.ring(Math.min(384,dp(context,sizing.ring)),i,state.parts[i]==null?null:state.parts[i].percent,state.charging[i],dark,state.fresh,i==2?caseThreshold:budThreshold));
            view.addView(i<2?R.id.widget_parts:R.id.widget_bottom,part);
        }
        RemoteViews info=new RemoteViews(context.getPackageName(),R.layout.widget_square_info);
        float scale=WidgetSizing.hostScale(hostScale);int hit=(int)Math.ceil(48/scale);
        info.setViewLayoutWidth(R.id.widget_refresh,hit,TypedValue.COMPLEX_UNIT_DIP);info.setViewLayoutHeight(R.id.widget_refresh,hit,TypedValue.COMPLEX_UNIT_DIP);
        info.setImageViewBitmap(R.id.widget_refresh,WidgetArtwork.refresh(dp(context,32/scale),dark));
        info.setTextViewTextSize(R.id.widget_status,TypedValue.COMPLEX_UNIT_SP,13);
        String stamp=timestamp(context,state.observed,now),shortStamp=stamp;
        if(state.observed>0){Calendar a=Calendar.getInstance(),b=Calendar.getInstance();a.setTimeInMillis(state.observed);b.setTimeInMillis(now);
            if(a.get(Calendar.YEAR)!=b.get(Calendar.YEAR)||a.get(Calendar.DAY_OF_YEAR)!=b.get(Calendar.DAY_OF_YEAR))shortStamp=android.text.format.DateFormat.format("M.d",state.observed).toString();}
        info.setTextViewText(R.id.widget_status,shortStamp.isEmpty()?"확인 필요":shortStamp);view.addView(R.id.widget_bottom,info);
        view.setContentDescription(R.id.widget_root,state.title+". "+state.description(0)+", "+state.description(1)+", "+state.description(2)+". "+state.connection+(stamp.isEmpty()?"":". "+stamp+" 확인")+". 눌러서 앱 열기");
        return view;
    }
    public static String timestamp(Context context,long observed,long now){
        if(observed<=0||observed>now)return "";
        Calendar a=Calendar.getInstance(),b=Calendar.getInstance();a.setTimeInMillis(observed);b.setTimeInMillis(now);
        String time=android.text.format.DateFormat.getTimeFormat(context).format(new Date(observed));
        return a.get(Calendar.YEAR)==b.get(Calendar.YEAR)&&a.get(Calendar.DAY_OF_YEAR)==b.get(Calendar.DAY_OF_YEAR)?time:android.text.format.DateFormat.getDateFormat(context).format(new Date(observed))+" "+time;
    }
    private static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
}
