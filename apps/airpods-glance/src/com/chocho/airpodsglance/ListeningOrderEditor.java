package com.chocho.airpodsglance;

import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import com.chocho.airpodsglance.ListeningProtocol.Mode;
import com.chocho.ui.Ui;
import java.util.ArrayList;

/** App-owned editor. Drag changes the draft on drop, never persistent options. */
public final class ListeningOrderEditor extends LinearLayout {
    public static final int MOVE_BEFORE=0x01026001,MOVE_AFTER=0x01026002;
    private final ListeningOrderDraft draft;
    private final boolean confirmDefault;
    private final LinearLayout group,body;
    private final ScrollView scroll;
    private final TextView error;
    private final Button save;
    private final ArrayList<Row> rows=new ArrayList<>();
    private Drag active;
    public ListeningOrderEditor(Context c,ListeningOrderDraft draft,boolean individual,Runnable cancel,Runnable commit){
        this(c,draft,individual,false,cancel,commit);
    }
    public ListeningOrderEditor(Context c,ListeningOrderDraft draft,boolean individual,boolean confirmDefault,Runnable cancel,Runnable commit){
        super(c);this.draft=draft;this.confirmDefault=confirmDefault;setOrientation(VERTICAL);setBackgroundColor(Ui.BG);setTag("listening-order-editor");
        LinearLayout header=new LinearLayout(c);header.setOrientation(VERTICAL);header.setPadding(dp(8),dp(8),dp(8),dp(8));
        LinearLayout bar=new LinearLayout(c);bar.setGravity(Gravity.CENTER_VERTICAL);
        Button back=Ui.button(c,"취소",Ui.BLUE);save=Ui.button(c,"저장",Ui.BLUE);
        back.setBackground(Ui.pressed(c,Ui.BG,10));save.setBackground(Ui.pressed(c,Ui.BG,10));
        back.setOnClickListener(v->{abortDrag();cancel.run();});save.setOnClickListener(v->{if(active==null&&canCommit())commit.run();});
        TextView title=Ui.text(c,"전환 순서",17,Ui.INK,true);title.setGravity(Gravity.CENTER);title.setAccessibilityHeading(true);
        boolean stacked=c.getResources().getConfiguration().fontScale>1.3f||c.getResources().getConfiguration().screenWidthDp<360;
        if(stacked){title.setPadding(dp(8),dp(8),dp(8),dp(4));header.addView(title,new LayoutParams(-1,-2));}
        bar.addView(back,new LayoutParams(stacked?-2:dp(80),-2));
        if(stacked)bar.addView(new Space(c),new LayoutParams(0,1,1));else bar.addView(title,new LayoutParams(0,-2,1));
        bar.addView(save,new LayoutParams(stacked?-2:dp(80),-2));header.addView(bar);addView(header,new LayoutParams(-1,-2));
        scroll=new ScrollView(c);scroll.setId(0x1008001);scroll.setFillViewport(false);scroll.setOverScrollMode(OVER_SCROLL_NEVER);scroll.setVerticalScrollBarEnabled(false);
        FrameLayout frame=new FrameLayout(c);
        body=new LinearLayout(c){@Override protected void onMeasure(int w,int h){super.onMeasure(MeasureSpec.makeMeasureSpec(Math.min(MeasureSpec.getSize(w),dp(632)),MeasureSpec.EXACTLY),h);}};
        body.setOrientation(VERTICAL);body.setPadding(dp(16),dp(20),dp(16),dp(24));
        group=new LinearLayout(c){@Override protected void dispatchDraw(Canvas canvas){
            int saved=canvas.save();Path clip=new Path();clip.addRoundRect(new RectF(0,0,getWidth(),getHeight()),dp(12),dp(12),Path.Direction.CW);canvas.clipPath(clip);super.dispatchDraw(canvas);canvas.restoreToCount(saved);
        }};
        group.setOrientation(VERTICAL);group.setBackground(Ui.rounded(c,Ui.PAPER,12));body.addView(group,new LayoutParams(-1,-2));
        body.addView(new Note(c,"두 모드 이상 선택하세요.\n손잡이를 끌어 순서를 바꿔요."));
        body.addView(new Note(c,individual?"이 위젯에만 적용해요.":"위젯별로 정한 순서는 유지해요."));
        error=new Note(c,"");error.setTextColor(Ui.RED);error.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);error.setVisibility(GONE);body.addView(error);
        frame.addView(body,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL));scroll.addView(frame);addView(scroll,new LayoutParams(-1,0,1));refresh();
    }
    public void showError(String text){((Note)error).lastWidth=-1;error.setText(text);error.setVisibility(VISIBLE);}
    public String errorText(){return error.getVisibility()==VISIBLE?error.getText().toString():"";}
    public View selection(Mode mode){for(Row row:rows)if(row.mode==mode)return row.select;return null;}
    public View handle(Mode mode){for(Row row:rows)if(row.mode==mode)return row.handle;return null;}
    public ListeningOrderDraft draft(){return draft;}
    private void changed(Mode focus,boolean handleFocus){
        error.setVisibility(GONE);refresh();View target=handleFocus?handle(focus):selection(focus);
        if(handleFocus&&target!=null&&target.isShown())target.requestFocus();
    }
    private void refresh(){
        group.removeAllViews();rows.clear();Mode[] modes=draft.rows();
        for(int i=0;i<modes.length;i++){Row row=new Row(modes[i],i<modes.length-1);rows.add(row);group.addView(row,new LayoutParams(-1,-2));}
        save.setEnabled(canCommit());save.setAlpha(canCommit()?1:.35f);
    }
    private boolean move(Mode mode,int direction){
        if(active!=null)return false;if(!draft.move(mode,draft.position(mode)+direction))return false;
        changed(mode,true);handle(mode).announceForAccessibility(mode.label+", "+(draft.position(mode)+1)+"번째");return true;
    }
    private final class Row extends LinearLayout {
        final Mode mode;final LinearLayout select;final View handle;final Paint line=new Paint();final boolean separator;
        Row(Mode mode,boolean separator){
            super(ListeningOrderEditor.this.getContext());this.mode=mode;this.separator=separator;setWillNotDraw(false);setGravity(Gravity.CENTER_VERTICAL);setMinimumHeight(dp(56));
            select=new LinearLayout(getContext());select.setGravity(Gravity.CENTER_VERTICAL);select.setMinimumHeight(dp(56));select.setPadding(dp(16),dp(12),dp(4),dp(12));
            select.setClickable(true);select.setFocusable(true);select.setBackground(Ui.pressed(getContext(),Ui.PAPER,0));select.setContentDescription(mode.label);
            View check=new Mark(getContext(),false,draft.contains(mode));check.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);select.addView(check,new LayoutParams(dp(24),dp(24)));
            TextView name=Ui.text(getContext(),mode.label,17,Ui.INK,false);name.setPadding(dp(12),0,dp(4),0);name.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);select.addView(name,new LayoutParams(0,-2,1));
            select.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View v,AccessibilityNodeInfo node){super.onInitializeAccessibilityNodeInfo(v,node);node.setClassName("android.widget.CheckBox");node.setCheckable(true);node.setChecked(draft.contains(mode));}});
            select.setOnClickListener(v->{if(active!=null)return;if(!draft.toggle(mode)){showError("전환할 모드를 두 개 이상 선택해 주세요.");return;}changed(mode,false);});
            addView(select,new LayoutParams(0,-2,1));
            handle=new Mark(getContext(),true,true);handle.setMinimumHeight(dp(56));handle.setFocusable(true);handle.setClickable(true);
            handle.setContentDescription(mode.label+" 순서, "+(draft.position(mode)+1)+"번째");
            handle.setVisibility(draft.contains(mode)?VISIBLE:INVISIBLE);handle.setImportantForAccessibility(draft.contains(mode)?IMPORTANT_FOR_ACCESSIBILITY_YES:IMPORTANT_FOR_ACCESSIBILITY_NO);
            handle.setBackground(Ui.pressed(getContext(),Ui.PAPER,0));
            handle.setOnClickListener(v->handle.announceForAccessibility("끌어서 순서를 바꾸세요. 접근성 동작이나 위아래 방향키로도 이동할 수 있어요."));
            handle.setAccessibilityDelegate(new View.AccessibilityDelegate(){
                @Override public void onInitializeAccessibilityNodeInfo(View v,AccessibilityNodeInfo node){
                    super.onInitializeAccessibilityNodeInfo(v,node);node.setClassName("android.widget.Button");int at=draft.position(mode);
                    if(at>0)node.addAction(new AccessibilityNodeInfo.AccessibilityAction(MOVE_BEFORE,"앞으로 이동"));
                    if(at>=0&&at<draft.size()-1)node.addAction(new AccessibilityNodeInfo.AccessibilityAction(MOVE_AFTER,"뒤로 이동"));
                }
                @Override public boolean performAccessibilityAction(View v,int action,Bundle args){if(action==MOVE_BEFORE)return move(mode,-1);if(action==MOVE_AFTER)return move(mode,1);return super.performAccessibilityAction(v,action,args);}
            });
            handle.setOnKeyListener((v,key,event)->{if(key!=KeyEvent.KEYCODE_DPAD_UP&&key!=KeyEvent.KEYCODE_DPAD_DOWN)return false;if(event.getAction()==KeyEvent.ACTION_UP)move(mode,key==KeyEvent.KEYCODE_DPAD_UP?-1:1);return true;});
            handle.setOnTouchListener((v,event)->drag(this,event));addView(handle,new LayoutParams(dp(48),-1));
        }
        @Override protected void dispatchDraw(Canvas c){super.dispatchDraw(c);if(separator){line.setColor(Ui.LINE);line.setStrokeWidth(Math.max(1,dp(.5f)));c.drawLine(dp(52),getHeight()-1,getWidth(),getHeight()-1,line);}}
    }
    private final class Note extends TextView {
        int lastWidth=-1;
        Note(Context c,String text){super(c);setText(text);setTextSize(13);setTextColor(Ui.MUTED);setIncludeFontPadding(false);setLineSpacing(dp(3),1);setPadding(dp(16),dp(8),dp(16),dp(4));setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);}
        @Override protected void onMeasure(int w,int h){super.onMeasure(w,h);int available=getMeasuredWidth()-getPaddingLeft()-getPaddingRight();if(available>0&&available!=lastWidth){lastWidth=available;Ui.keepAlertWords(this,available);super.onMeasure(w,h);}}
    }
    private final class Mark extends View {
        final boolean grip,checked;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Mark(Context c,boolean grip,boolean checked){super(c);this.grip=grip;this.checked=checked;}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float x=getWidth()/2f,y=getHeight()/2f;p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            if(grip){p.setColor(0xff8e8e93);p.setStrokeWidth(dp(1.5f));for(int i=-1;i<=1;i++)c.drawLine(x-dp(8),y+i*dp(5),x+dp(8),y+i*dp(5),p);}
            else if(checked){p.setColor(Ui.BLUE);p.setStrokeWidth(dp(2.2f));Path path=new Path();path.moveTo(x-dp(6),y);path.lineTo(x-dp(2),y+dp(4));path.lineTo(x+dp(7),y-dp(5));c.drawPath(path,p);}
            else {p.setColor(0xffb6b6bc);p.setStrokeWidth(dp(1.2f));c.drawCircle(x,y,dp(9),p);}
        }
    }
    private final class Drag {
        final Row row;final int from,pointer,scrollY;final float rawY;int target;boolean moved;
        Drag(Row row,MotionEvent e){this.row=row;from=draft.position(row.mode);target=from;pointer=e.getPointerId(0);rawY=e.getRawY();scrollY=scroll.getScrollY();}
    }
    private boolean drag(Row row,MotionEvent e){
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){if(!draft.contains(row.mode))return false;abortDrag();active=new Drag(row,e);scroll.requestDisallowInterceptTouchEvent(true);save.setEnabled(false);return true;}
        if(active==null)return true;
        if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN||e.findPointerIndex(active.pointer)<0){abortDrag();return true;}
        if(action==MotionEvent.ACTION_MOVE){
            Drag d=active;float delta=e.getRawY()-d.rawY+scroll.getScrollY()-d.scrollY;
            if(!d.moved&&Math.abs(delta)<ViewConfiguration.get(getContext()).getScaledTouchSlop())return true;
            d.moved=true;int[] location=new int[2];scroll.getLocationOnScreen(location);float y=e.getRawY()-location[1];
            if(y<dp(36))scroll.scrollBy(0,-dp(10));else if(y>scroll.getHeight()-dp(36))scroll.scrollBy(0,dp(10));
            delta=e.getRawY()-d.rawY+scroll.getScrollY()-d.scrollY;
            Row last=rows.get(draft.size()-1);float top=Math.max(0,Math.min(last.getBottom()-row.getHeight(),row.getTop()+delta));
            float center=row.getTop()+delta+row.getHeight()/2f;d.target=d.from;
            for(int i=0;i<draft.size();i++){if(i==d.from)continue;Row other=rows.get(i);float mid=other.getTop()+other.getHeight()/2f;if(i<d.from&&center<=mid)d.target=Math.min(d.target,i);else if(i>d.from&&center>=mid)d.target=Math.max(d.target,i);}
            row.setTranslationY(top-row.getTop());row.setTranslationZ(dp(4));row.setAlpha(.94f);
            for(int i=0;i<draft.size();i++){if(i==d.from)continue;Row other=rows.get(i);float shift=i>=d.target&&i<d.from?row.getHeight():i>d.from&&i<=d.target?-row.getHeight():0;other.setTranslationY(shift);}
            return true;
        }
        if(action==MotionEvent.ACTION_UP){Drag d=active;boolean moved=d.moved;int target=d.target;abortDrag();if(moved&&draft.move(row.mode,target))changed(row.mode,false);else if(!moved)row.handle.performClick();return true;}
        return true;
    }
    private boolean canCommit(){return confirmDefault||draft.changed();}
    public void abortDrag(){if(active==null)return;active=null;for(Row row:rows){row.setTranslationY(0);row.setTranslationZ(0);row.setAlpha(1);}scroll.requestDisallowInterceptTouchEvent(false);save.setEnabled(canCommit());}
    @Override protected void onDetachedFromWindow(){abortDrag();super.onDetachedFromWindow();}
    private int dp(float value){return Ui.dp(getContext(),value);}
}
