package com.chocho.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.OverScroller;
import java.util.function.IntConsumer;

/** Cyclic column with real touch interruption and range actions, without OEM NumberPicker chrome. */
public final class WheelColumn extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final OverScroller scroller;
    private final int count;
    private final String label;
    private final String[] labels;
    private final IntConsumer changed;
    private final float rowHeight;
    private float position,lastY,downY;
    private int notified,phase;
    private boolean moved;
    private VelocityTracker velocity;
    public WheelColumn(Context c,String name,int count,int value,String[] labels,IntConsumer changed){
        super(c);this.label=name;this.count=count;this.labels=labels;this.changed=changed;
        rowHeight=Math.max(Ui.dp(c,40),Ui.sp(c,26)*1.35f);position=value*rowHeight;notified=value;
        scroller=new OverScroller(c);setFocusable(true);setClickable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription(name);setMinimumWidth(Ui.dp(c,48));paint.setTextAlign(Paint.Align.CENTER);
    }
    public int rowPixels(){return Math.round(rowHeight);}
    public int value(){int value=Math.round(position/rowHeight);return count==2?Math.max(0,Math.min(1,value)):EditorModel.wrap(value,count);}
    public void settle(){scroller.forceFinished(true);phase=0;position=value()*rowHeight;emit();invalidate();}
    private void emit(){int v=value();if(v!=notified){notified=v;changed.accept(v);sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);}}
    private void snap(){
        int from=Math.round(position),to=Math.round((count==2?value():Math.round(position/rowHeight))*rowHeight);
        if(!ValueAnimator.areAnimatorsEnabled()||Math.abs(to-from)<1){settle();return;}
        phase=2;scroller.startScroll(0,from,0,to-from,140);postInvalidateOnAnimation();
    }
    @Override protected void onMeasure(int w,int h){setMeasuredDimension(resolveSize(Ui.dp(getContext(),64),w),resolveSize(Math.round(rowHeight*5),h));}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float index=position/rowHeight;int center=Math.round(index);
        for(int offset=-3;offset<=3;offset++){
            int slot=center+offset;if(count==2&&(slot<0||slot>=2))continue;float displacement=(slot-index)*rowHeight,y=getHeight()/2f+displacement;
            float distance=Math.abs(displacement)/rowHeight;
            paint.setTextSize(Ui.sp(getContext(),distance<.5f?25:distance>1.5f?20:23));
            paint.setColor(distance<.5f?Ui.INK:Ui.MUTED);paint.setAlpha(distance>2?36:distance>1.5?96:distance>.5?180:255);
            String text=labels==null?String.format(java.util.Locale.KOREAN,"%02d",EditorModel.wrap(slot,count)):labels[EditorModel.wrap(slot,count)];
            Paint.FontMetrics fm=paint.getFontMetrics();canvas.drawText(text,getWidth()/2f,y-(fm.ascent+fm.descent)/2,paint);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!isEnabled())return false;
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){
            scroller.forceFinished(true);phase=0;lastY=downY=event.getY();moved=false;
            if(velocity!=null)velocity.recycle();velocity=VelocityTracker.obtain();velocity.addMovement(event);
            getParent().requestDisallowInterceptTouchEvent(true);return true;
        }
        if(velocity==null)return true;
        velocity.addMovement(event);
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_MOVE:
                moved|=Math.abs(event.getY()-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop();
                position-=event.getY()-lastY;if(count==2)position=Math.max(-rowHeight*.25f,Math.min(rowHeight*1.25f,position));lastY=event.getY();emit();invalidate();return true;
            case MotionEvent.ACTION_UP:
                if(!moved){int delta=Math.round((event.getY()-getHeight()/2f)/rowHeight);position+=delta*rowHeight;performClick();snap();}
                else{
                    velocity.computeCurrentVelocity(1000,ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity());
                    int speed=Math.round(-velocity.getYVelocity());
                    if(Math.abs(speed)>ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity()&&ValueAnimator.areAnimatorsEnabled()){
                        phase=1;scroller.fling(0,Math.round(position),0,speed,0,0,count==2?0:-10000000,count==2?Math.round(rowHeight):10000000);postInvalidateOnAnimation();
                    }else snap();
                }
                releaseTouch();return true;
            case MotionEvent.ACTION_CANCEL:snap();releaseTouch();return true;
            case MotionEvent.ACTION_POINTER_DOWN:snap();releaseTouch();return true;
        }
        return true;
    }
    private void releaseTouch(){if(velocity!=null){velocity.recycle();velocity=null;}getParent().requestDisallowInterceptTouchEvent(false);}
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public void computeScroll(){
        if(scroller.computeScrollOffset()){position=scroller.getCurrY();emit();postInvalidateOnAnimation();}
        else if(phase==1){snap();}else if(phase==2){phase=0;position=value()*rowHeight;emit();invalidate();}
    }
    private void step(int delta){settle();position+=delta*rowHeight;if(count==2)position=value()*rowHeight;emit();invalidate();announceForAccessibility(label+" "+(labels==null?value():labels[value()]));}
    @Override public boolean onKeyDown(int code,KeyEvent e){if(code==KeyEvent.KEYCODE_DPAD_UP){step(1);return true;}if(code==KeyEvent.KEYCODE_DPAD_DOWN){step(-1);return true;}return super.onKeyDown(code,e);}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){
        super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.NumberPicker");
        info.setContentDescription(label);info.setText(labels==null?String.valueOf(value()):labels[value()]);
        info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT,0,count-1,value()));
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
    }
    @Override public boolean performAccessibilityAction(int action,Bundle args){
        if(!isEnabled())return false;
        if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD){step(1);return true;}
        if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){step(-1);return true;}
        if(action==AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()&&args!=null){
            int v=Math.round(args.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE));
            if(v>=0&&v<count){settle();position=v*rowHeight;emit();invalidate();return true;}return false;
        }
        return super.performAccessibilityAction(action,args);
    }
    @Override protected void onDetachedFromWindow(){scroller.forceFinished(true);if(velocity!=null){velocity.recycle();velocity=null;}super.onDetachedFromWindow();}
}
