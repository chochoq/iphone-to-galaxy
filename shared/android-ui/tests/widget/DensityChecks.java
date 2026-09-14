package com.chocho.widgettest;

import android.app.Instrumentation;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.chocho.airpodsglance.*;
import java.util.ArrayList;
import java.util.Locale;

/** Detached native layouts: no Activity, Bluetooth, user data or home-screen placement. */
public final class DensityChecks extends Instrumentation {
    private int checks,cases;private final ArrayList<String> failures=new ArrayList<>();
    private void check(boolean ok,String text){checks++;if(!ok&&failures.size()<80)failures.add(text);}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){
        runOnMainSync(()->{try{
            for(WidgetVariant v:WidgetVariant.values())for(float font:new float[]{.8f,1,1.3f,2})for(boolean dark:new boolean[]{false,true})
                for(float width:new float[]{88.85714f,122.28571f,168,210,217.14285f,240,300,320,346.2857f,360,420,475.42856f})
                for(float height:new float[]{64,96,102.28571f,112,172,194,198,218,234.28572f,280,366.2857f}){
                    inspect(v,width,height,font,dark,1,"edge",false);
                }
            for(WidgetVariant v:WidgetVariant.values())for(String state:new String[]{"fresh","charging","partial","uncertain","empty","stale","future","disconnected"}){
                // Samsung's installed non-resizable 1×1 reports 122.28571, not the 89dp estimate.
                float w=v==WidgetVariant.SINGLE?122.28571f:v==WidgetVariant.SMALL?217.14285f:346.2857f;
                float h=102.28571f;
                inspect(v,w,h,.8f,true,.6666667f,state,true);
            }
            singleCycle();
        }catch(Throwable t){failures.add(t.toString());}});
        Bundle result=new Bundle();result.putString("stream","Density native checks "+checks+", cases "+cases+", failures "+failures.size()+"\n"+String.join("\n",failures)+"\n");
        finish(failures.isEmpty()?0:1,result);
    }
    private void inspect(WidgetVariant variant,float width,float height,float font,boolean dark,float scale,String state,boolean actual){
        Configuration config=new Configuration(getTargetContext().getResources().getConfiguration());config.fontScale=font;config.setLocale(Locale.KOREAN);
        config.uiMode=(config.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|(dark?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO);
        Context c=getTargetContext().createConfigurationContext(config);float density=c.getResources().getDisplayMetrics().density;
        RemoteViews remote=WidgetRenderer.render(c,MainActivity.snapshot(state),MainActivity.NOW,width,height,scale,20,20,variant);
        String tag=variant+" "+width+"x"+height+" font="+font+" "+state;
        if(actual){Parcel p=Parcel.obtain();remote.writeToParcel(p,0);check(p.dataSize()<900000,tag+" parcel exceeds safe margin");p.setDataPosition(0);remote=RemoteViews.CREATOR.createFromParcel(p);p.recycle();}
        ViewGroup root=(ViewGroup)remote.apply(c,new FrameLayout(c));measure(root,width,height,density);walk(root,root,tag);
        if(actual){
            View ring=root.findViewById(R.id.part_ring);check(ring!=null&&ring.getVisibility()==View.VISIBLE,tag+" ring visible");
            float expected=variant==WidgetVariant.SINGLE?48:variant==WidgetVariant.SMALL?56:52;
            check(ring.getHeight()/density>=expected,tag+" artwork too small: "+ring.getHeight()/density);
            check(((ImageView)ring).getScaleType()==ImageView.ScaleType.FIT_CENTER,tag+" image must grow with its view");
            TextView number=root.findViewById(R.id.part_percent);check(number.getTextSize()/density*scale>=12.5f,tag+" actual percent too small");
            View refresh=root.findViewById(R.id.widget_refresh);if(refresh.getVisibility()==View.VISIBLE){
                check(refresh.getWidth()/density*scale>=48&&refresh.getHeight()/density*scale>=48,tag+" refresh target smaller than 48dp");
            }
            String desc=String.valueOf(root.getContentDescription());check(desc.contains(variant==WidgetVariant.SINGLE?"눌러서 다음 잔량 보기":"눌러서 앱 열기"),tag+" root action");
            RemoteViews unknown=WidgetRenderer.render(c,MainActivity.snapshot("uncertain"),MainActivity.NOW,width,height,scale,20,20,variant);
            unknown.reapply(c,root);measure(root,width,height,density);walk(root,root,tag+" reapply");
            check(!String.valueOf(root.getContentDescription()).contains("87%"),tag+" stale exact value survived reapply");
        }
        cases++;
    }
    private void singleCycle(){
        Configuration config=new Configuration(getTargetContext().getResources().getConfiguration());config.fontScale=.8f;
        Context c=getTargetContext().createConfigurationContext(config);float d=c.getResources().getDisplayMetrics().density;
        String[] labels={"왼쪽","오른쪽","케이스"};
        for(float width:new float[]{88.85714f,122.28571f}){
        ViewGroup root=null;
        for(int selected:new int[]{0,1,2,3,-1}){
            RemoteViews rv=WidgetRenderer.render(c,MainActivity.snapshot("partial"),MainActivity.NOW,width,102.28571f,.6666667f,20,20,WidgetVariant.SINGLE,selected);
            if(root==null)root=(ViewGroup)rv.apply(c,new FrameLayout(c));else rv.reapply(c,root);
            measure(root,width,102.28571f,d);int part=Math.floorMod(selected,3);
            check(((TextView)root.findViewById(R.id.part_label)).getText().toString().equals(labels[part]),"single cycle label "+selected);
            if(part==2)check(((TextView)root.findViewById(R.id.part_percent)).getText().toString().equals("—"),"single unknown case does not show previous earbud value");
            walk(root,root,"single cycle "+selected);cases++;
        }
        }
        Configuration big=new Configuration(config);big.fontScale=2;Context large=c.createConfigurationContext(big);
        View fallback=WidgetRenderer.render(large,MainActivity.snapshot("fresh"),MainActivity.NOW,88,102,.6666667f,20,20,WidgetVariant.SINGLE,1).apply(large,new FrameLayout(large));
        check(String.valueOf(fallback.getContentDescription()).contains("눌러서 앱 열기"),"single oversized-font fallback describes its actual open action");cases++;
    }
    private void measure(View root,float w,float h,float density){int x=Math.round(w*density),y=Math.round(h*density);root.measure(View.MeasureSpec.makeMeasureSpec(x,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(y,View.MeasureSpec.EXACTLY));root.layout(0,0,x,y);}
    private void walk(View v,ViewGroup root,String tag){
        if(v.getVisibility()!=View.VISIBLE)return;
        if(v instanceof TextView){TextView t=(TextView)v;Layout l=t.getLayout();if(l!=null){
            boolean ellipsis=t.getId()==R.id.widget_title||t.getId()==R.id.widget_status;
            int w=t.getWidth()-t.getCompoundPaddingLeft()-t.getCompoundPaddingRight(),h=t.getHeight()-t.getCompoundPaddingTop()-t.getCompoundPaddingBottom();
            for(int i=0;i<l.getLineCount();i++)check(ellipsis||l.getLineWidth(i)<=w+1,tag+" clipped width "+t.getText());
            check(l.getHeight()<=h+1,tag+" clipped height "+t.getText());
            Rect r=new Rect();t.getDrawingRect(r);root.offsetDescendantRectToMyCoords(t,r);check(r.top>=-1&&r.bottom<=root.getHeight()+1,tag+" text outside root "+t.getText());
        }}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)walk(g.getChildAt(i),root,tag);}
    }
}
