package com.chocho.widgettest;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.util.SizeF;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import com.chocho.airpodsglance.WidgetRenderer;
import com.chocho.airpodsglance.WidgetVariant;
import com.chocho.airpodsglance.R;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public final class WidgetChecks extends Instrumentation {
    private int checks=0,cases=0;
    private final ArrayList<String> failures=new ArrayList<>();
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    private void check(boolean ok,String message){checks++;if(!ok)failures.add(message);}
    @Override public void onStart(){
        MainActivity a=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{
            for(WidgetVariant variant:WidgetVariant.values())for(float font:new float[]{1,1.3f,2})for(boolean dark:new boolean[]{false,true})for(float width:new float[]{110,180,210,240,300,320,360,420,475})for(float height:new float[]{64,172,180,194,198,218,234,260,280,366}){
                final float f=font,w=width,h=height;final boolean d=dark;
                runOnMainSync(()->{a.variant=variant;a.show(w,h,f,d,"edge",1);});waitForIdleSync();
                runOnMainSync(()->inspect(a,variant+" "+w+"x"+h+" f"+f+" dark"+d));cases++;
            }
            runOnMainSync(()->a.variant=WidgetVariant.WIDE);
            for(String state:new String[]{"fresh","charging","partial","empty","uncertain","disconnected","stale","future"}){
                runOnMainSync(()->a.show(320,180,1,false,state,1));waitForIdleSync();
                runOnMainSync(()->{inspect(a,state);String desc=String.valueOf(a.widget.getContentDescription());check(desc.contains("눌러서 앱 열기"),state+" open description");if(state.equals("uncertain"))check(!desc.contains("87%")&&desc.contains("기기 확인 필요"),"uncertain hides cached reading");if(state.equals("partial"))check(desc.contains("케이스 잔량 알 수 없음"),"unknown case spoken");});cases++;
            }
            for(float[] s:new float[][]{{180,64,1},{210,102,1.3f},{240,194,1.3f},{210,240,2},{320,240,2},{475,128,2}}){
                runOnMainSync(()->a.show(s[0],s[1],s[2],false,"charging",1));waitForIdleSync();runOnMainSync(()->inspect(a,"charging "+s[0]+"x"+s[1]+" f"+s[2]));cases++;
            }
            runOnMainSync(()->a.show(475,234,.8f,false,"fresh",.6666667f));waitForIdleSync();
            runOnMainSync(()->{inspect(a,"Samsung scaled");View refresh=a.widget.findViewById(R.id.widget_refresh);float den=a.getResources().getDisplayMetrics().density;check(refresh.getWidth()/den*a.scale>=48&&refresh.getHeight()/den*a.scale>=48,"scaled refresh min48dp");});cases++;
            runOnMainSync(()->a.reapply("uncertain"));waitForIdleSync();
            runOnMainSync(()->{inspect(a,"reapply uncertain");check(!String.valueOf(a.widget.getContentDescription()).contains("87%"),"reapply removed previous exact reading");});cases++;
            for(WidgetVariant variant:WidgetVariant.values()){
                final float w=variant==WidgetVariant.SMALL?217:475,h=variant==WidgetVariant.LARGE?366:234;
                runOnMainSync(()->{a.variant=variant;a.show(w,h,.8f,false,"partial",.6666667f);});waitForIdleSync();
                runOnMainSync(()->{inspect(a,variant+" Samsung partial");View refresh=a.widget.findViewById(R.id.widget_refresh);float den=a.getResources().getDisplayMetrics().density;if(variant!=WidgetVariant.SINGLE){check(refresh.getWidth()/den*a.scale>=48&&refresh.getHeight()/den*a.scale>=48,variant+" refresh min48dp");check(String.valueOf(a.widget.getContentDescription()).contains("케이스 잔량 알 수 없음"),variant+" unknown case spoken");}});cases++;
                runOnMainSync(()->a.reapply("uncertain"));waitForIdleSync();
                runOnMainSync(()->{inspect(a,variant+" uncertain reapply");check(!String.valueOf(a.widget.getContentDescription()).contains("87%"),variant+" reapply hides cached value");});cases++;
            }
            runOnMainSync(()->{
                check(a.widget.getImportantForAccessibility()==View.IMPORTANT_FOR_ACCESSIBILITY_YES,"root accessibility description exposed");
                check(a.widget.findViewById(R.id.widget_parts).getImportantForAccessibility()==View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,"parts avoid duplicate announcements");
                for(WidgetVariant variant:WidgetVariant.values()){
                    View preview=WidgetRenderer.render(a.widgetContext,MainActivity.snapshot("edge"),MainActivity.NOW,variant.previewWidth,variant.previewHeight,1,20,20,variant).apply(a.widgetContext,new FrameLayout(a.widgetContext));
                    float den=a.widgetContext.getResources().getDisplayMetrics().density;
                    int w=Math.round(variant.previewWidth*den),h=Math.round(variant.previewHeight*den);
                    preview.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));preview.layout(0,0,w,h);
                    check(preview instanceof ViewGroup,variant+" renderer preview inflates");walk(preview,preview,variant+" renderer preview");cases++;
                }
                LinkedHashMap<SizeF,RemoteViews> sizes=new LinkedHashMap<>();
                sizes.put(new SizeF(320,102),WidgetRenderer.render(a.widgetContext,MainActivity.snapshot("edge"),MainActivity.NOW,320,102,1,20,20));
                sizes.put(new SizeF(320,180),WidgetRenderer.render(a.widgetContext,MainActivity.snapshot("edge"),MainActivity.NOW,320,180,1,20,20));
                RemoteViews responsive=new RemoteViews(sizes);Parcel p=Parcel.obtain();responsive.writeToParcel(p,0);check(p.dataSize()<900000,"responsive parcel stays below binder margin");p.setDataPosition(0);RemoteViews copy=RemoteViews.CREATOR.createFromParcel(p);p.recycle();
                check(copy.apply(a.widgetContext,new FrameLayout(a.widgetContext))!=null,"responsive size-map round trip applies");
            });
        }catch(Throwable t){failures.add(t.toString());}
        Bundle result=new Bundle();result.putString("stream","Widget native checks "+checks+", cases "+cases+", failures "+failures.size()+"\n"+String.join("\n",failures)+"\n");finish(failures.isEmpty()?0:1,result);
    }
    private void inspect(MainActivity a,String tag){
        check(a.widget.getWidth()>0&&a.widget.getHeight()>0,tag+" root measured");walk(a.widget,a.widget,tag);
        View open=a.widget.findViewById(R.id.widget_open);View parts=a.widget.findViewById(R.id.widget_parts);
        if(open.getVisibility()==View.VISIBLE)check(open.getHeight()>=0,tag+" open fallback");
        else check(count(a.widget,R.id.part_percent)==(com.chocho.airpodsglance.WidgetLayout.forVariant(a.variant,a.width,a.height,a.font).mode==com.chocho.airpodsglance.WidgetLayout.Mode.SINGLE?1:3),tag+" expected component count");
    }
    private int count(View v,int id){int n=v.getId()==id?1:0;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)n+=count(g.getChildAt(i),id);}return n;}
    private void walk(View v,View root,String tag){
        if(v.getVisibility()!=View.VISIBLE)return;
        if(v instanceof TextView){TextView t=(TextView)v;Layout l=t.getLayout();if(l!=null){int available=t.getWidth()-t.getCompoundPaddingLeft()-t.getCompoundPaddingRight();int height=t.getHeight()-t.getCompoundPaddingTop()-t.getCompoundPaddingBottom();boolean allowEllipsis=t.getId()==R.id.widget_title||t.getId()==R.id.widget_status;for(int i=0;i<l.getLineCount();i++)check(allowEllipsis||l.getLineWidth(i)<=available+1,tag+" clipped width "+t.getText()+" "+l.getLineWidth(i)+">"+available);check(l.getHeight()<=height+1,tag+" clipped height "+t.getText()+" "+l.getHeight()+">"+height);}
            int[] p=new int[2],r=new int[2];t.getLocationOnScreen(p);root.getLocationOnScreen(r);check(p[1]>=r[1]-1&&p[1]+t.getHeight()<=r[1]+root.getHeight()+1,tag+" text outside root "+t.getText());}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)walk(g.getChildAt(i),root,tag);}
    }
}
