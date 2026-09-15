package com.chocho.listeninguitest;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.chocho.airpodsglance.*;
import com.chocho.airpodsglance.ListeningProtocol.Mode;
import com.chocho.ui.Ui;
import java.util.ArrayList;
public final class Checks extends Instrumentation {
    int count;MainActivity activity;
    public void onCreate(Bundle args){super.onCreate(args);start();}
    void check(boolean value,String why){count++;if(!value)throw new AssertionError(why);}
    ArrayList<View> walk(View v){ArrayList<View> all=new ArrayList<>();all.add(v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)all.addAll(walk(g.getChildAt(i)));}return all;}
    void main(Runnable body){Throwable[] failure={null};runOnMainSync(()->{try{body.run();}catch(Throwable e){failure[0]=e;}});if(failure[0]!=null)throw new AssertionError(failure[0]);}
    void render(ListeningState.Snapshot state,boolean enabled,Mode expected,String label){
        activity.controls.render(state,null,enabled,Mode.UNKNOWN);
        int width=Ui.dp(activity,activity.getIntent().getIntExtra("width",475));
        activity.ui.root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        activity.ui.root.layout(0,0,activity.ui.root.getMeasuredWidth(),activity.ui.root.getMeasuredHeight());
        int selected=0,tiles=0;
        for(View v:walk(activity.ui.root)){
            String description=String.valueOf(v.getContentDescription());
            if(description.startsWith("노이즈 캔슬링")||description.startsWith("적응형")||description.startsWith("주변음 허용")){
                tiles++;check(v.getHeight()>=Ui.dp(activity,48),label+" touch height");check(v.getWidth()>=Ui.dp(activity,48),label+" touch width");
                check(v.isEnabled()==enabled,label+" enabled state matches connection/request state");
                check(!description.contains("검증 전"),"verified adaptive no stale gate description");
                if(v.isSelected()){selected++;check(description.startsWith(expected.label),"actual not requested selection");}
            }
            if(v instanceof TextView){TextView t=(TextView)v;android.text.Layout l=t.getLayout();if(l!=null)for(int i=0;i<l.getLineCount();i++){
                check(l.getLineMax(i)<=t.getWidth()-t.getCompoundPaddingLeft()-t.getCompoundPaddingRight()+2,label+" text width: "+t.getText());
                check(l.getEllipsisCount(i)==0,label+" no ellipsis");
            }}
        }
        check(tiles==3,"three modes");check(selected==(expected==Mode.UNKNOWN?0:1),label+" exact selected count");
    }
    public void onStart(){Bundle result=new Bundle();try{
        for(int width:new int[]{320,360,475,800})for(float font:new float[]{1f,1.3f,2f}){
            MainActivity.nextWidth=width;MainActivity.nextFont=font;
            Intent i=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("width",width).putExtra("font",font);
            activity=(MainActivity)startActivitySync(i);waitForIdleSync();
            main(()->{
                check(Math.abs(activity.getResources().getConfiguration().fontScale-MainActivity.nextFont)<0.001f,"actual font override applied");
                check(activity.getResources().getConfiguration().screenWidthDp==MainActivity.nextWidth,"actual width configuration applied");
                ListeningState s=new ListeningState();render(s.snapshot(),false,Mode.UNKNOWN,"checking");
                s.ready();s.observe(ListeningProtocol.decode(new byte[]{4,0,4,0,9,0,13,3,0,0,0}),100);
                render(s.snapshot(),true,Mode.TRANSPARENCY,"ready");
                s.request(Mode.ANC,110,30100);render(s.snapshot(),false,Mode.TRANSPARENCY,"pending");
                s.observe(ListeningProtocol.decode(new byte[]{4,0,4,0,9,0,13,2,0,0,0}),120);
                render(s.snapshot(),true,Mode.ANC,"confirmed");
                if(MainActivity.nextWidth==320&&MainActivity.nextFont==1f)GlyphChecks.run(activity,activity.ui.root,this::check);
                s.request(Mode.ADAPTIVE,130,30100);render(s.snapshot(),false,Mode.ANC,"adaptive pending");
                s.observe(ListeningProtocol.decode(new byte[]{4,0,4,0,9,0,13,4,0,0,0}),140);
                render(s.snapshot(),true,Mode.ADAPTIVE,"adaptive confirmed");s.close(ListeningState.End.EXPIRED);
                render(s.snapshot(),true,Mode.UNKNOWN,"expired");render(null,false,Mode.UNKNOWN,"disconnected");
                activity.finish();
            });waitForIdleSync();
        }
        result.putString("result","PASS "+count+" checks; 12 width/font combinations x 8 states; no Bluetooth permissions");finish(-1,result);
    }catch(Throwable error){result.putString("failure",error.toString());finish(0,result);}}
}
