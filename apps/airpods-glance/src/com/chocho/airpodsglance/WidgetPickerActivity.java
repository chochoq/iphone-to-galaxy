package com.chocho.airpodsglance;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.chocho.ui.SettingsPage;
import com.chocho.ui.SettingsSheet;
import com.chocho.ui.Ui;

/** Preview-only gallery. Choosing a card opens the launcher's confirmation, never auto-places it. */
public final class WidgetPickerActivity extends Activity {
    @Override public void onCreate(Bundle state){
        super.onCreate(state);SettingsPage page=new SettingsPage(this,"위젯 고르기");
        Button back=Ui.button(this,"‹ 뒤로",Ui.BLUE);back.setBackground(Ui.pressed(this,Ui.BG,8));back.setOnClickListener(v->finish());
        page.root.addView(back,0,new LinearLayout.LayoutParams(-2,-2));
        page.footer("배터리와 소음 제어를 따로 골라 추가할 수 있어요.\n미리보기는 예시이며, 실제 상태가 아니에요.");
        addListening(page);
        for(WidgetVariant variant:new WidgetVariant[]{WidgetVariant.WIDE,WidgetVariant.SMALL,WidgetVariant.SINGLE}){
            LinearLayout card=page.group("배터리 · "+variant.title+"  ·  "+variant.cells);
            String detail=variant==WidgetVariant.SINGLE?"한 부품을 크게. 누를 때마다 왼쪽 → 오른쪽 → 케이스.":variant==WidgetVariant.SMALL?"두 칸에 양쪽 이어버드와 케이스 잔량을.":"세 칸에 원과 잔량을 나란히.";
            page.note(card,detail);
            FrameLayout margin=new FrameLayout(this);margin.setPadding(Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,16));
            margin.addView(new Preview(this,variant),new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER));card.addView(margin);
            page.separator(card);Button add=page.row(card,variant.title+" 추가",()->pin(variant));add.setTextColor(Ui.BLUE);
        }
        page.footer("3×1과 2×1은 왼쪽부터 왼쪽 이어버드 · 오른쪽 이어버드 · 케이스예요. 누르면 앱이 열려요.\n1×1은 누를 때마다 표시할 부품이 바뀌어요.\n실제 잔량은 에어팟이 보낸 값으로 표시해요.");
        LinearLayout lock=page.group("잠금화면 시계 아래");
        page.note(lock,"위젯 하나를 누르면 왼쪽 → 오른쪽 → 케이스로 바뀌어요. 이어버드 아이콘에서 밝게 표시된 쪽의 잔량이에요.\n잠금화면을 길게 눌러 편집 → 시계 아래 위젯 → 에어팟 한눈에에서 처음 볼 부품 하나를 골라 주세요.");
        page.footer("작은 시계 표시는 마지막으로 받은 잔량이라는 뜻이에요. 위젯을 눌러도 새로 측정하거나 연결하지 않아요.\n이미 넣어둔 위젯도 각각 전환되고, 마지막으로 본 부품을 기억해요.\nFold8 · One UI 9.0에서 확인한 방식이라 다른 기기에서는 목록에 없을 수 있어요.");page.install();
    }
    private void addListening(SettingsPage page){
        for(ListeningWidgetProvider.Kind kind:new ListeningWidgetProvider.Kind[]{ListeningWidgetProvider.Kind.WIDE,ListeningWidgetProvider.Kind.SMALL,ListeningWidgetProvider.Kind.SINGLE}){
            LinearLayout card=page.group("소음 제어 · "+kind.title);
            page.note(card,kind.cycle()?"원형 아이콘 하나. 누를 때마다 고른 순서대로 바꿔요.":kind==ListeningWidgetProvider.Kind.SMALL?"아이콘만 간결하게. 노캔 · 적응형 · 주변음을 바로 선택해요.":"아이콘과 이름을 함께. 원하는 모드를 바로 선택해요.");
            FrameLayout margin=new FrameLayout(this);margin.setPadding(Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,16));
            margin.addView(new ListeningPreview(this,kind),new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER));card.addView(margin);
            page.separator(card);page.row(card,"소음 제어 "+kind.title+" 추가",()->pinListening(kind)).setTextColor(Ui.BLUE);
        }
        LinearLayout settings=page.group("한 칸 위젯 설정");
        page.row(settings,"기본 전환 순서  ›",()->startActivity(new Intent(this,ListeningWidgetConfigActivity.class)));
        AppWidgetManager manager=AppWidgetManager.getInstance(this);int number=0;
        for(ListeningWidgetProvider.Kind kind:new ListeningWidgetProvider.Kind[]{ListeningWidgetProvider.Kind.SINGLE,ListeningWidgetProvider.Kind.LOCK}){
            for(int id:manager.getAppWidgetIds(new ComponentName(this,ListeningWidgetProvider.provider(kind)))){final int widget=id;number++;page.separator(settings);
                page.row(settings,(kind==ListeningWidgetProvider.Kind.LOCK?"잠금":"홈")+" 위젯 "+number+"의 전환 순서  ›",()->startActivity(new Intent(this,ListeningWidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widget)));
            }
        }
        page.footer("위젯에는 마지막으로 확인한 모드가 표시돼요. 계속 연결해 상태를 읽지는 않아요.\nAirPods를 연결한 상태에서 눌러 주세요. 응답을 확인한 뒤 전환하며, 연타는 쌓아두지 않아요.");
        LinearLayout lock=page.group("소음 제어 · 잠금화면");page.note(lock,"잠금화면 편집 → 시계 아래 위젯 → 에어팟 한눈에 → ‘소음 제어 · 잠금 한 칸’을 골라 주세요.\n한 칸 위젯과 같은 전환 순서를 써요. 잠금 해제 없이 실행되는지는 기기와 잠금화면 설정에 따라 달라요.");
    }
    private void pinListening(ListeningWidgetProvider.Kind kind){
        AppWidgetManager manager=AppWidgetManager.getInstance(this);
        if(!manager.isRequestPinAppWidgetSupported()){SettingsSheet.message(this,"홈 화면을 길게 눌러 위젯에서 추가해 주세요.");return;}
        manager.requestPinAppWidget(new ComponentName(this,ListeningWidgetProvider.provider(kind)),null,null);
    }
    private static final class ListeningPreview extends FrameLayout {
        private final View widget;private final int logicalWidth,logicalHeight;
        ListeningPreview(Context c,ListeningWidgetProvider.Kind kind){super(c);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            Configuration config=new Configuration(c.getResources().getConfiguration());config.fontScale=1;Context preview=c.createConfigurationContext(config);
            logicalWidth=Ui.dp(c,kind.width);logicalHeight=Ui.dp(c,kind.height);
            widget=ListeningWidgetRenderer.render(preview,new ListeningWidgetStore.Record(ListeningProtocol.Mode.ANC,System.currentTimeMillis(),""),ListeningWidgetOptions.defaults(),kind,kind.width,kind.height).apply(preview,this);
            // RemoteViews previews have no PendingIntents and must never control the device.
            addView(widget,new FrameLayout.LayoutParams(logicalWidth,logicalHeight));widget.setPivotX(0);widget.setPivotY(0);
        }
        @Override protected void onMeasure(int w,int h){int available=MeasureSpec.getSize(w);float scale=Math.min(1,available/(float)logicalWidth);widget.measure(MeasureSpec.makeMeasureSpec(logicalWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(logicalHeight,MeasureSpec.EXACTLY));setMeasuredDimension(available,Math.round(logicalHeight*scale));widget.setScaleX(scale);widget.setScaleY(scale);}
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){int left=Math.round((getWidth()-logicalWidth*widget.getScaleX())/2);widget.layout(left,0,left+logicalWidth,logicalHeight);}
    }
    private void pin(WidgetVariant variant){
        AppWidgetManager manager=AppWidgetManager.getInstance(this);
        if(!manager.isRequestPinAppWidgetSupported()){SettingsSheet.message(this,"홈 화면을 길게 눌러 위젯에서 추가해 주세요.");return;}
        Intent intent=new Intent(this,WidgetPickerActivity.class);
        PendingIntent success=PendingIntent.getActivity(this,710+variant.ordinal(),intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        manager.requestPinAppWidget(new ComponentName(this,AirPodsWidgetProvider.providerFor(variant)),null,success);
    }
    private static final class Preview extends FrameLayout {
        private final View widget;
        private final int logicalWidth,logicalHeight;
        Preview(Context context,WidgetVariant variant){
            super(context);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            Configuration config=new Configuration(context.getResources().getConfiguration());config.fontScale=1;
            // A thumbnail of a widget, not a live reading and not the gallery's user-sized labels.
            Context previewContext=context.createConfigurationContext(config);
            long now=System.currentTimeMillis();AirPodsSnapshot example=new AirPodsSnapshot("AirPods",true,AirPodsSnapshot.IdentityConfidence.EXACT,null,
                new BatteryComponent(80,false),new BatteryComponent(65,false),new BatteryComponent(50,false),now,AirPodsSnapshot.Source.AAP_EXACT_PERCENT);
            logicalWidth=Ui.dp(context,variant.previewWidth);logicalHeight=Ui.dp(context,variant.previewHeight);
            widget=WidgetRenderer.render(previewContext,example,now,variant.previewWidth,variant.previewHeight,1,20,20,variant).apply(previewContext,this);
            addView(widget,new FrameLayout.LayoutParams(logicalWidth,logicalHeight));widget.setPivotX(0);widget.setPivotY(0);
        }
        @Override protected void onMeasure(int width,int height){
            int available=MeasureSpec.getSize(width);float scale=Math.min(1,available/(float)logicalWidth);
            widget.measure(MeasureSpec.makeMeasureSpec(logicalWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(logicalHeight,MeasureSpec.EXACTLY));
            setMeasuredDimension(available,Math.round(logicalHeight*scale));widget.setScaleX(scale);widget.setScaleY(scale);
        }
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){int left=Math.round((getWidth()-logicalWidth*widget.getScaleX())/2);widget.layout(left,0,left+logicalWidth,logicalHeight);}
    }
}
