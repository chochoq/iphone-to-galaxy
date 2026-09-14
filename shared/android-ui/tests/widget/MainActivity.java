package com.chocho.widgettest;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcel;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.chocho.airpodsglance.*;
import java.util.Locale;

public final class MainActivity extends Activity {
    public View widget;
    public Context widgetContext;
    public RemoteViews remote;
    public float width,height,font,scale;
    public boolean dark;
    public String state;
    public WidgetVariant variant=WidgetVariant.WIDE;
    public static final long NOW=1789104600000L;
    @Override public void onCreate(Bundle b){super.onCreate(b);String kind=getIntent().getStringExtra("variant");if(kind!=null)variant=WidgetVariant.valueOf(kind);show(getIntent().getFloatExtra("width",320),getIntent().getFloatExtra("height",180),getIntent().getFloatExtra("font",1),getIntent().getBooleanExtra("dark",false),getIntent().getStringExtra("state"),getIntent().getFloatExtra("scale",1));}
    public void show(float w,float h,float f,boolean night,String kind,float hostScale){
        width=w;height=h;font=f;dark=night;state=kind==null?"fresh":kind;scale=hostScale;
        Configuration config=new Configuration(getResources().getConfiguration());
        config.fontScale=f;config.uiMode=(config.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|(night?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO);config.setLocale(Locale.KOREAN);
        widgetContext=createConfigurationContext(config);
        LinearLayout screen=new LinearLayout(this);screen.setOrientation(LinearLayout.VERTICAL);screen.setGravity(Gravity.CENTER);screen.setPadding(dp(16),dp(16),dp(16),dp(16));screen.setBackgroundColor(night?0xff000000:0xffdddfe5);
        TextView caption=new TextView(this);caption.setText("격리 시험 · 실제 잔량 아님\n"+w+" × "+h+"dp · 글씨 "+Math.round(f*100)+"% · "+state);caption.setTextSize(14);caption.setGravity(Gravity.CENTER);caption.setTextColor(night?0xffeeeeee:0xff333333);caption.setPadding(0,0,0,dp(20));screen.addView(caption);
        FrameLayout frame=new FrameLayout(widgetContext);
        remote=WidgetRenderer.render(widgetContext,snapshot(state),NOW,w,h,hostScale,20,20,variant);
        Parcel parcel=Parcel.obtain();remote.writeToParcel(parcel,0);parcel.setDataPosition(0);remote=RemoteViews.CREATOR.createFromParcel(parcel);parcel.recycle();
        widget=remote.apply(widgetContext,frame);frame.addView(widget,new FrameLayout.LayoutParams(dp(w),dp(h)));
        frame.setScaleX(hostScale);frame.setScaleY(hostScale);
        screen.addView(frame,new LinearLayout.LayoutParams(dp(w),dp(h)));setContentView(screen);
    }
    public void reapply(String kind){state=kind;remote=WidgetRenderer.render(widgetContext,snapshot(kind),NOW,width,height,scale,20,20,variant);remote.reapply(widgetContext,widget);}
    public static AirPodsSnapshot snapshot(String s){
        if("empty".equals(s))return AirPodsSnapshot.empty(null);
        boolean uncertain="uncertain".equals(s),disconnected="disconnected".equals(s),stale="stale".equals(s),charging="charging".equals(s),future="future".equals(s),edge="edge".equals(s);
        return new AirPodsSnapshot("AirPods Pro",!disconnected,uncertain?AirPodsSnapshot.IdentityConfidence.NEEDS_CALIBRATION:disconnected?AirPodsSnapshot.IdentityConfidence.NONE:AirPodsSnapshot.IdentityConfidence.EXACT,null,new BatteryComponent(edge?100:charging?20:87,charging),new BatteryComponent(edge?1:charging?15:86,charging),"partial".equals(s)?null:new BatteryComponent(edge?0:56,false),future?NOW+1000:stale||disconnected?NOW-86400000:NOW-5000,AirPodsSnapshot.Source.AAP_EXACT_PERCENT);
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
