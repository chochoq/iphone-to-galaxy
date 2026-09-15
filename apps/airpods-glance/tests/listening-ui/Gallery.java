package com.chocho.listeninguitest;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.chocho.airpodsglance.ListeningControlsView;
import com.chocho.airpodsglance.ListeningProtocol;
import com.chocho.airpodsglance.ListeningProtocol.Mode;
import com.chocho.airpodsglance.ListeningState;
import com.chocho.ui.SettingsPage;
import com.chocho.ui.Ui;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

/** Documentation samples: real product Views, fixture state, no controller or private inputs. */
public final class Gallery extends Instrumentation {
    private int checks;
    private void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public void onCreate(Bundle args){super.onCreate(args);start();}
    public void onStart(){
        Bundle result=new Bundle();MainActivity activity=null;
        try{
            MainActivity.nextWidth=475;MainActivity.nextFont=1f;
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
            final MainActivity screen=activity;final Throwable[] failure={null};
            runOnMainSync(()->{try{
                SettingsPage ui=new SettingsPage(screen,"에어팟 한눈에");
                // Capture just the real controls, not a synthetic full app screen with fake battery/device data.
                ui.root.removeAllViews();ListeningControlsView controls=new ListeningControlsView(screen,ui);ui.install();
                Mode[] modes={Mode.ANC,Mode.ADAPTIVE,Mode.TRANSPARENCY};
                int[] codes={2,4,3};String[] names={"anc","adaptive","transparency"};
                StringBuilder records=new StringBuilder("Actual product View renders; fixture selection only; no Bluetooth controller.\n");
                for(int n=0;n<modes.length;n++){
                    ListeningState state=new ListeningState();state.ready();
                    state.observe(ListeningProtocol.decode(new byte[]{4,0,4,0,9,0,13,(byte)codes[n],0,0,0}),100);
                    controls.render(state.snapshot(),null,true,Mode.UNKNOWN);
                    int width=Ui.dp(screen,475);
                    ui.root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
                    ui.root.layout(0,0,ui.root.getMeasuredWidth(),ui.root.getMeasuredHeight());
                    ArrayList<View> all=new ArrayList<>();collect(ui.root,all);int selected=0,tiles=0;boolean status=false;
                    for(View v:all){
                        String desc=String.valueOf(v.getContentDescription());
                        if(desc.startsWith("노이즈 캔슬링")||desc.startsWith("적응형")||desc.startsWith("주변음 허용")){
                            tiles++;check(v.isEnabled(),"enabled fixture tile");
                            if(v.isSelected()){selected++;check(desc.startsWith(modes[n].label),"correct selected fixture");}
                        }
                        if(v instanceof TextView){TextView t=(TextView)v;
                            if(t.getText().toString().equals("현재 · "+modes[n].label))status=true;
                            android.text.Layout layout=t.getLayout();if(layout!=null)for(int line=0;line<layout.getLineCount();line++){
                                check(layout.getEllipsisCount(line)==0,"no ellipsis");
                                check(layout.getLineMax(line)<=t.getWidth()-t.getCompoundPaddingLeft()-t.getCompoundPaddingRight()+2,"text fits");
                            }
                        }
                    }
                    check(tiles==3&&selected==1,"three modes with one selection");check(status,"status matches fixture");
                    Bitmap bitmap=Bitmap.createBitmap(ui.root.getWidth(),ui.root.getHeight(),Bitmap.Config.ARGB_8888);
                    Canvas canvas=new Canvas(bitmap);canvas.drawColor(Ui.BG);ui.root.draw(canvas);
                    File dir=screen.getExternalFilesDir(null);check(dir!=null,"test export directory");
                    File file=new File(dir,"air-listening-"+names[n]+".png");
                    try(FileOutputStream out=new FileOutputStream(file)){check(bitmap.compress(Bitmap.CompressFormat.PNG,100,out),"PNG written");}
                    records.append(names[n]).append(" ").append(bitmap.getWidth()).append("x").append(bitmap.getHeight()).append(" selected=1\n");
                    bitmap.recycle();
                }
                records.append("PASS ").append(checks).append(" checks\n");
                try(FileOutputStream out=new FileOutputStream(new File(screen.getExternalFilesDir(null),"gallery-evidence.txt"))){out.write(records.toString().getBytes("UTF-8"));}
            }catch(Throwable error){failure[0]=error;}});
            if(failure[0]!=null)throw new AssertionError(failure[0]);
            result.putString("result","PASS "+checks+" gallery checks; 3 real product View renders; fixture states, no Bluetooth permissions");
            finish(-1,result);
        }catch(Throwable error){result.putString("failure",error.toString());finish(0,result);}
        finally{if(activity!=null){final MainActivity end=activity;runOnMainSync(end::finish);}}
    }
    private static void collect(View v,ArrayList<View> all){all.add(v);if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++)collect(group.getChildAt(i),all);}}
}
