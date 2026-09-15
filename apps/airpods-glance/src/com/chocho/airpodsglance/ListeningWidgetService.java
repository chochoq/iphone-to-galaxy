package com.chocho.airpodsglance;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.Log;
import android.widget.Toast;
import com.chocho.airpodsglance.ListeningProtocol.Mode;

/** Explicit widget tap only. No queue, boot work, retry, media action or persisted request. */
public final class ListeningWidgetService extends Service {
    private static ListeningWidgetService running;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private ListeningController controller;private ListeningWidgetRequest request;
    private boolean active;private int widgetId;private String selected;
    public static void deleted(int id){if(running!=null&&running.widgetId==id)running.finish("위젯이 삭제되어 멈췄어요.");}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        // A second tap never replaces or queues behind the in-flight tap.
        if(active){Log.i("ListeningWidget","duplicate_ignored");return START_NOT_STICKY;}
        if(!ListeningWidgetProvider.validRequest(this,intent)){stopSelf();return START_NOT_STICKY;}
        widgetId=intent.getIntExtra("widget",0);selected=new AppSettings(this).selectedAddress();
        try{
            NotificationManager manager=getSystemService(NotificationManager.class);
            NotificationChannel channel=new NotificationChannel("listening_widget_control","위젯 소음 제어",NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null,null);channel.enableVibration(false);manager.createNotificationChannel(channel);
            PendingIntent open=PendingIntent.getActivity(this,25100,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            startForeground(2125,new Notification.Builder(this,channel.getId()).setSmallIcon(R.drawable.ic_airpods)
                    .setContentTitle("소음 제어").setContentText("AirPods 응답을 확인하고 있어요.").setContentIntent(open)
                    .setOngoing(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE).build());
        }catch(RuntimeException unavailable){
            new ListeningWidgetStore(this).note("앱에서 연결과 권한을 확인해 주세요.");ListeningWidgetProvider.updateAll(this);stopSelf();return START_NOT_STICKY;
        }
        active=true;running=this;
        request=new ListeningWidgetRequest(SystemClock.elapsedRealtime(),Mode.fromWire(intent.getIntExtra("mode",0)),new ListeningWidgetStore(this).options(widgetId));
        controller=new ListeningController(this,(state,message,enabled,pending)->{
            boolean checking=controller!=null&&controller.isCheckingConnection();
            handler.post(()->evaluate(state,message,checking));
        });
        handler.postDelayed(()->finish("응답을 확인하지 못했어요. 다시 눌러 주세요."),ListeningWidgetRequest.LIMIT_MS);
        Log.i("ListeningWidget","tap_started");
        try{controller.start();}catch(RuntimeException unavailable){finish("앱에서 연결과 권한을 확인해 주세요.");}
        return START_NOT_STICKY;
    }
    private void evaluate(ListeningState.Snapshot state,String message,boolean checking){
        if(!active)return;
        if(selected==null||!selected.equals(new AppSettings(this).selectedAddress())||!ListeningWidgetProvider.owns(this,widgetId)){
            finish("연결 대상이 바뀌어 멈췄어요.");return;
        }
        if(state==null&&!checking){
            finish(message==null?"AirPods를 연결한 뒤 다시 눌러 주세요.":message);return;
        }
        switch(request.observe(state,SystemClock.elapsedRealtime())){
            case SEND:
                Log.i("ListeningWidget","send_"+request.target());controller.choose(request.target());break;
            case SUCCESS:
                Log.i("ListeningWidget","confirmed_"+request.target());finish(request.target().label+" 확인했어요.");break;
            case FAILURE:finish("적용 여부를 확인하지 못했어요. 다시 눌러 주세요.");break;
            default:break;
        }
    }
    private void finish(String note){
        if(!active)return;active=false;
        cleanup();new ListeningWidgetStore(this).note(note);ListeningWidgetProvider.updateAll(this);
        Toast.makeText(this,note,Toast.LENGTH_SHORT).show();Log.i("ListeningWidget","tap_finished");
        stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
    }
    private void cleanup(){
        handler.removeCallbacksAndMessages(null);if(request!=null)request.cancel();
        if(controller!=null){ListeningController old=controller;controller=null;old.stop();}
        if(running==this)running=null;
    }
    @Override public void onDestroy(){active=false;cleanup();super.onDestroy();}
}
