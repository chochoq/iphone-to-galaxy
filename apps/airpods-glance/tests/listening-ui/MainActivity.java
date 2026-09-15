package com.chocho.listeninguitest;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import com.chocho.airpodsglance.*;
import com.chocho.ui.SettingsPage;
public final class MainActivity extends Activity {
    static volatile float nextFont=1f;static volatile int nextWidth=475;
    SettingsPage ui;ListeningControlsView controls;
    @Override protected void attachBaseContext(Context base){
        Configuration override=new Configuration(base.getResources().getConfiguration());
        override.fontScale=nextFont;override.screenWidthDp=nextWidth;
        super.attachBaseContext(base.createConfigurationContext(override));
    }
    public void onCreate(Bundle saved){
        super.onCreate(saved);ui=new SettingsPage(this,"소음 제어 UI 검사");
        controls=new ListeningControlsView(this,ui);ui.install();
        // Intentionally never bind a controller. This package has no Bluetooth or data permissions.
    }
}
