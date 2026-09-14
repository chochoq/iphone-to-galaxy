package com.chocho.ui;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.*;

/** A full-window information screen. Does not imitate an Android permission prompt. */
public final class DetailsScreen extends DialogFragment {
    public interface Host { View createDetails(); }
    public static void open(Activity a){if(a.getFragmentManager().isStateSaved()||a.getFragmentManager().findFragmentByTag("settings.details.008")!=null)return;new DetailsScreen().show(a.getFragmentManager(),"settings.details.008");}
    @Override public Dialog onCreateDialog(Bundle state){Activity a=getActivity();Dialog dialog=new Dialog(a);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout page=Ui.column(a);page.setBackgroundColor(Ui.BG);page.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(i.left,i.top,i.right,i.bottom);return insets;});
        Button back=Ui.button(a,"‹ 뒤로",Ui.BLUE);back.setBackground(Ui.pressed(a,Ui.BG,8));back.setOnClickListener(v->dismiss());
        TextView title=Ui.text(a,"권한 및 정보",17,Ui.INK,true);title.setAccessibilityHeading(true);title.setGravity(Gravity.CENTER);title.setPadding(0,Ui.dp(a,12),0,Ui.dp(a,12));
        if(getResources().getConfiguration().fontScale>1.25f){page.addView(back);page.addView(title,new LinearLayout.LayoutParams(-1,-2));}
        else{FrameLayout bar=new FrameLayout(a);FrameLayout.LayoutParams centered=new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER);centered.leftMargin=centered.rightMargin=Ui.dp(a,80);bar.addView(title,centered);bar.addView(back,new FrameLayout.LayoutParams(-2,-2,Gravity.START|Gravity.CENTER_VERTICAL));page.addView(bar,new LinearLayout.LayoutParams(-1,-2));}
        ScrollView scroll=new ScrollView(a);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);View content=((Host)a).createDetails();content.setPadding(Ui.dp(a,16),Ui.dp(a,20),Ui.dp(a,16),Ui.dp(a,24));
        FrameLayout frame=new FrameLayout(a);LinearLayout bounded=new LinearLayout(a){@Override protected void onMeasure(int w,int h){super.onMeasure(MeasureSpec.makeMeasureSpec(Math.min(MeasureSpec.getSize(w),Ui.dp(a,632)),MeasureSpec.EXACTLY),h);}};bounded.setOrientation(LinearLayout.VERTICAL);bounded.addView(content,new LinearLayout.LayoutParams(-1,-2));frame.addView(bounded,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL));scroll.addView(frame);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));dialog.setContentView(page);return dialog;}
    @Override public void onStart(){super.onStart();Window w=getDialog().getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(-1,-1);w.setDimAmount(0);}}
}
