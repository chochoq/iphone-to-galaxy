package com.chocho.airpodsglance;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.chocho.ui.Ui;
import com.chocho.ui.SettingsPage;
import com.chocho.airpodsglance.ListeningProtocol.Mode;

/** App-owned line icons and settings styling; no platform AlertDialog or copied Apple assets. */
public final class ListeningControlsView implements ListeningController.Screen {
    private final Context context;private final TextView status;private final Button refresh;
    private final Mode[] modes={Mode.ANC,Mode.ADAPTIVE,Mode.TRANSPARENCY};
    private final LinearLayout[] tiles=new LinearLayout[3];
    private final TextView[] labels=new TextView[3];private final ModeIcon[] icons=new ModeIcon[3];
    private ListeningController controller;
    public ListeningControlsView(Context context,SettingsPage ui){
        this.context=context;
        LinearLayout group=ui.group("소음 제어");
        status=ui.note(group,"현재 모드를 확인하고 있어요.");
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        boolean stacked=context.getResources().getConfiguration().screenWidthDp<360
                ||context.getResources().getConfiguration().fontScale>1.25f;
        LinearLayout outer=new LinearLayout(context);outer.setPadding(d(12),d(4),d(12),d(12));
        LinearLayout tray=new LinearLayout(context);tray.setOrientation(stacked?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        tray.setPadding(d(4),d(4),d(4),d(4));tray.setBackground(Ui.rounded(context,Ui.BG,12));
        outer.addView(tray,new LinearLayout.LayoutParams(-1,-2));group.addView(outer);
        for(int i=0;i<modes.length;i++){
            final Mode mode=modes[i];
            LinearLayout tile=new LinearLayout(context){
                @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){
                    super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.Button");
                }
            };
            tile.setOrientation(stacked?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);
            tile.setPadding(d(8),d(12),d(8),d(12));tile.setMinimumHeight(d(stacked?56:104));
            tile.setFocusable(true);tile.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            icons[i]=new ModeIcon(context,mode);icons[i].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            tile.addView(icons[i],new LinearLayout.LayoutParams(d(26),d(26)));
            labels[i]=Ui.text(context,mode.label,14,Ui.INK,false);
            labels[i].setGravity(Gravity.CENTER);labels[i].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            if(!stacked)labels[i].setMinLines(2);
            LinearLayout.LayoutParams labelParams=new LinearLayout.LayoutParams(stacked?-2:-1,-2);
            if(stacked)labelParams.leftMargin=d(12);else labelParams.topMargin=d(7);
            tile.addView(labels[i],labelParams);
            LinearLayout.LayoutParams tileParams=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);
            if(i>0){if(stacked)tileParams.topMargin=d(4);else tileParams.leftMargin=d(4);}
            tray.addView(tile,tileParams);tiles[i]=tile;
            tile.setOnClickListener(v->{if(controller!=null)controller.choose(mode);});
        }
        ui.separator(group);refresh=ui.row(group,"현재 상태 확인",()->{if(controller!=null)controller.refresh();});
        ui.footer("AirPods가 연결돼 있을 때 바로 바꿀 수 있어요.\n적응형은 지원하는 AirPods에서 사용할 수 있어요.");
        render(null,"현재 모드를 확인하고 있어요.",false,Mode.UNKNOWN);
    }
    public void bind(ListeningController value){controller=value;}
    @Override public void render(ListeningState.Snapshot state,String message,boolean enabled,Mode pending){
        boolean live=state!=null&&state.live();
        Mode actual=state==null?Mode.UNKNOWN:state.observed;
        boolean applying=state!=null&&state.phase==ListeningState.Phase.APPLYING;
        String text=message;
        if(applying)text=state.requested.label+" 적용 확인 중…";
        else if(pending!=Mode.UNKNOWN)text=pending.label+"으로 바꾸기 전에 현재 상태를 확인해요.";
        else if(text==null){
            if(actual!=Mode.UNKNOWN)text=(live?"현재 · ":"마지막 확인 · ")+actual.label;
            else text="현재 모드를 확인하고 있어요.";
        }
        if(!text.contentEquals(status.getText()))status.setText(text);
        for(int i=0;i<modes.length;i++){
            boolean selected=live&&modes[i]==actual;
            boolean supported=ListeningProtocol.canWrite(modes[i]);
            tiles[i].setEnabled(enabled&&supported);tiles[i].setSelected(selected);
            tiles[i].setBackground(selected?Ui.rounded(context,Ui.PAPER,9):Ui.pressed(context,Ui.BG,9));
            int color=selected?Ui.BLUE:supported?Ui.INK:Ui.MUTED;
            labels[i].setTextColor(color);icons[i].color=color;icons[i].invalidate();
            tiles[i].setAlpha(!supported?0.5f:enabled||applying?1f:0.55f);
            tiles[i].setContentDescription(modes[i].label+(selected?", 현재 선택됨":"")
                    +(!supported?", 전환 검증 전, 사용할 수 없음":!enabled?", 현재 변경할 수 없음":""));
        }
        refresh.setEnabled(!applying&&pending==Mode.UNKNOWN);
    }
    private int d(float value){return Ui.dp(context,value);}
    private static final class ModeIcon extends View {
        private final Mode mode;private final Paint paint=new Paint(3);int color=Ui.INK;
        private final Path shoulders=new Path(),largeSparkle=new Path(),smallSparkle=new Path();
        ModeIcon(Context c,Mode mode){
            super(c);this.mode=mode;paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
            // One independently drawn person silhouette, shared by all three listening modes.
            shoulders.moveTo(4.2f,22.2f);shoulders.cubicTo(4.2f,18.4f,8.3f,16.4f,13,16.4f);
            shoulders.cubicTo(17.7f,16.4f,21.8f,18.4f,21.8f,22.2f);
            shoulders.quadTo(21.8f,23.1f,20.9f,23.1f);shoulders.lineTo(5.1f,23.1f);
            shoulders.quadTo(4.2f,23.1f,4.2f,22.2f);shoulders.close();
            sparkle(largeSparkle,21,5.7f,3.3f);sparkle(smallSparkle,16.3f,2.7f,1.25f);
        }
        private static void sparkle(Path path,float x,float y,float radius){
            float waist=radius*0.14f;
            path.moveTo(x,y-radius);path.quadTo(x+waist,y-waist,x+radius,y);
            path.quadTo(x+waist,y+waist,x,y+radius);path.quadTo(x-waist,y+waist,x-radius,y);
            path.quadTo(x-waist,y-waist,x,y-radius);path.close();
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/26f,getHeight()/26f);
            paint.setColor(color);paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(13,10.2f,3.85f,paint);canvas.drawPath(shoulders,paint);
            if(mode==Mode.ANC){
                // Leave the lower arc open: the enclosure must not touch the shoulders.
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.75f);
                canvas.drawArc(4.8f,2.1f,21.2f,18.5f,150,240,false,paint);
            }else if(mode==Mode.TRANSPARENCY){
                for(int i=0;i<11;i++){
                    double angle=Math.toRadians(150+i*24);
                    canvas.drawCircle(13+8.2f*(float)Math.cos(angle),10.3f+8.2f*(float)Math.sin(angle),0.95f,paint);
                }
            }else if(mode==Mode.ADAPTIVE){
                canvas.drawPath(largeSparkle,paint);canvas.drawPath(smallSparkle,paint);
            }
            canvas.restore();
        }
    }
}
