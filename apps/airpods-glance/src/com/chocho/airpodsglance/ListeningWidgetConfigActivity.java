package com.chocho.airpodsglance;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/** One draft, one list, one explicit save. Never sends listening commands. */
public final class ListeningWidgetConfigActivity extends Activity {
    private ListeningOrderDraft draft;
    private ListeningOrderEditor editor;
    private int widgetId;
    private boolean configureRequest;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);setResult(RESULT_CANCELED);
        widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,0);
        configureRequest=AppWidgetManager.ACTION_APPWIDGET_CONFIGURE.equals(getIntent().getAction());
        if(widgetId<0||widgetId>0&&!ListeningWidgetProvider.owns(this,widgetId)
                ||AppWidgetManager.ACTION_APPWIDGET_CONFIGURE.equals(getIntent().getAction())&&widgetId==0){finish();return;}
        ListeningWidgetOptions initial=new ListeningWidgetStore(this).options(widgetId);
        draft=saved==null?new ListeningOrderDraft(initial):new ListeningOrderDraft(
            ListeningWidgetOptions.decode(saved.getString("original",initial.encode())),
            ListeningWidgetOptions.decode(saved.getString("draft")));
        editor=new ListeningOrderEditor(this,draft,widgetId>0,configureRequest,this::finish,this::save);
        editor.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
        });
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setContentView(editor);
    }
    @Override protected void onSaveInstanceState(Bundle state){
        editor.abortDrag();state.putString("draft",draft.options().encode());state.putString("original",draft.original());super.onSaveInstanceState(state);
    }
    private void save(){
        if(!draft.changed()&&!configureRequest)return;
        if(widgetId>0&&!ListeningWidgetProvider.owns(this,widgetId)){editor.showError("삭제된 위젯이에요. 다시 추가해 주세요.");return;}
        ListeningWidgetStore store=new ListeningWidgetStore(this);
        if(!draft.original().equals(store.options(widgetId).encode())){editor.showError("다른 곳에서 설정이 바뀌었어요. 취소한 뒤 다시 열어 주세요.");return;}
        if(draft.changed())store.saveOptions(widgetId,draft.options());ListeningWidgetProvider.updateAll(this);
        setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId));finish();
    }
}
