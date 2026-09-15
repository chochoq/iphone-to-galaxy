package com.chocho.airpodsglance;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

/** Process-local connection owner. All consumers and delivered events run on the main thread. */
public final class AapHub {
    private static AapHub instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AapConnectionOwner owner;
    private final Runnable ticker = new Runnable() {
        @Override public void run() { owner.tick(); if(owner.active())handler.postDelayed(this,250); }
    };
    public static synchronized AapHub get(Context context) {
        if(instance==null)instance=new AapHub(context.getApplicationContext());
        return instance;
    }
    private AapHub(Context context) {
        owner=new AapConnectionOwner(SystemClock::elapsedRealtime,(address,receiver)->{
            AapClient client=new AapClient(context,address,new AapClient.Listener(){
                public void onStage(String stage){handler.post(()->{
                    if(stage.startsWith("listening_sent_")||stage.equals("socket_connected"))
                        Log.i("AirPodsListening",stage);
                    receiver.stage(stage);
                });}
                public void onReady(){handler.post(receiver::ready);}
                public void onBattery(AapBatteryDecoder.Decoded battery){
                    AapConnectionOwner.BatteryReport report=new AapConnectionOwner.BatteryReport(
                            battery,SystemClock.elapsedRealtime(),System.currentTimeMillis());
                    handler.post(()->{Log.i("AirPodsListening","battery_received");receiver.battery(report);});
                }
                public void onListening(ListeningProtocol.Report report,long at){handler.post(()->{
                    // Enum names and monotonic time only. Never log addresses, frames or names.
                    Log.i("AirPodsListening","report_"+report.kind+"_"+report.mode+" at="+at);
                    int before=owner.snapshot().reports;
                    receiver.listening(report,at);
                    ListeningState.Snapshot accepted=owner.snapshot();
                    if(report.kind==ListeningProtocol.Kind.MODE&&accepted.reports>before&&accepted.receivedAt==at
                            &&new ListeningWidgetStore(context).record(address,report.mode,at))ListeningWidgetProvider.updateAll(context);
                });}
                public void onClosed(){handler.post(receiver::failed);}
            });
            return new AapConnectionOwner.Transport(){
                public void start(){Log.i("AirPodsListening","window_open");client.start();}
                public void stop(){Log.i("AirPodsListening","window_close");client.stop();}
                public boolean write(ListeningProtocol.Mode mode){return client.sendListeningMode(mode);}
            };
        });
    }
    private void main(){if(Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("Main thread required");}
    public long acquire(String address,boolean control,AapConnectionOwner.Listener listener){
        main();long lease=owner.acquire(address,control,listener);
        handler.removeCallbacks(ticker);if(owner.active())handler.post(ticker);return lease;
    }
    public void release(long lease){main();owner.release(lease);}
    public boolean contains(long lease){main();return owner.contains(lease);}
    public boolean request(long lease,ListeningProtocol.Mode mode){main();return owner.request(lease,mode);}
    public long remainingMillis(){main();return owner.remainingMillis();}
    public void disconnect(){main();owner.close(ListeningState.End.DISCONNECTED);}
}
