package com.chocho.airpodsglance;

import android.Manifest;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.chocho.airpodsglance.ListeningProtocol.Mode;

/** Foreground-only intent. Nothing persists or executes after a Bluetooth/lifecycle boundary. */
public final class ListeningController {
    public interface Screen { void render(ListeningState.Snapshot state,String message,boolean enabled,Mode pending); }
    private final Context context;private final Screen screen;private final AapHub hub;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private BluetoothAdapter adapter;private BluetoothA2dp profile;
    private boolean foreground,registered,verifying,connected;private long epoch,lease,pendingUntil;
    private String address,message="AirPods 연결을 확인하고 있어요.";
    private Mode pending=Mode.UNKNOWN;private ListeningState.Snapshot state;
    public ListeningController(Context context,Screen screen){this.context=context;this.screen=screen;hub=AapHub.get(context);}
    private final BroadcastReceiver receiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){
            String action=i.getAction();
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                int value=i.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
                if(value!=BluetoothAdapter.STATE_ON)lost("Bluetooth 연결을 확인해 주세요.");
                else if(foreground)verify(Mode.UNKNOWN);
                return;
            }
            BluetoothDevice device=i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(device==null||address==null||!address.equals(device.getAddress()))return;
            if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))lost("AirPods 연결이 끊겼어요.");
            else if(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)){
                int value=i.getIntExtra(BluetoothProfile.EXTRA_STATE,BluetoothProfile.STATE_DISCONNECTED);
                // Even a reconnection only reads. A previous explicit selection is never replayed.
                if(value==BluetoothProfile.STATE_CONNECTED)verify(Mode.UNKNOWN);
                else lost("AirPods를 연결한 뒤 다시 확인해 주세요.");
            }
        }
    };
    public void start(){
        if(foreground)return;foreground=true;
        IntentFilter filter=new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        context.registerReceiver(receiver,filter,Context.RECEIVER_EXPORTED);registered=true;
        verify(Mode.UNKNOWN);
    }
    public void stop(){
        foreground=false;invalidate();connected=false;
        if(registered){context.unregisterReceiver(receiver);registered=false;}
    }
    public void permissionsChanged(){if(foreground){stop();start();}}
    public void refresh(){if(foreground&&!verifying&&pending==Mode.UNKNOWN&&!applying())verify(Mode.UNKNOWN);}
    public void choose(Mode mode){
        if(!foreground||verifying||pending!=Mode.UNKNOWN||applying()||!ListeningProtocol.canWrite(mode))return;
        if(!targetConnected()){lost("AirPods를 연결한 뒤 다시 확인해 주세요.");return;}
        if(state!=null&&state.live()&&hub.contains(lease)&&state.phase==ListeningState.Phase.READY
                &&hub.remainingMillis()>=ListeningState.REPLY_MS){
            if(state.observed==mode)return;
            if(!hub.request(lease,mode)){message="변경하지 못했어요. 현재 상태를 다시 확인해 주세요.";draw();}
        }else verify(mode);
    }
    private boolean allowed(){return Build.VERSION.SDK_INT>=37&&context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}
    private boolean applying(){return state!=null&&state.phase==ListeningState.Phase.APPLYING;}
    private boolean targetConnected(){
        try{return allowed()&&adapter!=null&&adapter.isEnabled()&&address!=null
                &&address.equals(new AppSettings(context).selectedAddress())&&profile!=null
                &&adapter.getRemoteDevice(address).getBondState()==BluetoothDevice.BOND_BONDED
                &&profile.getConnectionState(adapter.getRemoteDevice(address))==BluetoothProfile.STATE_CONNECTED;
        }catch(RuntimeException error){return false;}
    }
    private void verify(Mode target){
        invalidate();if(!foreground)return;
        state=null;connected=false;pending=target;pendingUntil=SystemClock.elapsedRealtime()+15_000;
        if(!allowed()){pending=Mode.UNKNOWN;message=Build.VERSION.SDK_INT<37?"소음 제어는 Android 17 기기에서 확인했어요.":"‘권한 및 진단’에서 Bluetooth 권한을 허용해 주세요.";draw();return;}
        address=new AppSettings(context).selectedAddress();
        BluetoothManager manager=context.getSystemService(BluetoothManager.class);adapter=manager==null?null:manager.getAdapter();
        try {
            if(adapter==null||!adapter.isEnabled()||address==null){pending=Mode.UNKNOWN;message="AirPods를 연결한 뒤 현재 상태를 확인해 주세요.";draw();return;}
            verifying=true;message=target==Mode.UNKNOWN?"현재 모드를 확인하고 있어요.":target.label+"으로 바꾸기 전에 연결을 확인하고 있어요.";draw();
            final long current=epoch;
            boolean requested=adapter.getProfileProxy(context,new BluetoothProfile.ServiceListener(){
                public void onServiceConnected(int id,BluetoothProfile proxy){
                    if(!foreground||current!=epoch){adapter.closeProfileProxy(id,proxy);return;}
                    profile=(BluetoothA2dp)proxy;
                    if(!targetConnected()){lost("AirPods를 연결한 뒤 다시 확인해 주세요.");return;}
                    connected=true;
                    // If a battery lease owns the last <5 seconds, let its fixed window finish.
                    // This delay is tied to this foreground epoch and expires with this tap.
                    long remaining=hub.remainingMillis();
                    long delay=target!=Mode.UNKNOWN&&remaining>0&&remaining<ListeningState.REPLY_MS?remaining+80:0;
                    handler.postDelayed(()->{if(foreground&&epoch==current&&targetConnected())open(current);},delay);
                }
                public void onServiceDisconnected(int id){if(foreground&&current==epoch)lost("Bluetooth 상태를 확인하지 못했어요.");}
            },BluetoothProfile.A2DP);
            if(!requested){lost("Bluetooth 상태를 확인하지 못했어요.");return;}
            handler.postDelayed(()->{
                if(!foreground||current!=epoch)return;
                if(verifying||pending!=Mode.UNKNOWN){lost("응답을 확인하지 못했어요. 다시 확인해 주세요.");}
            },15_000);
        }catch(RuntimeException error){lost("Bluetooth 연결과 권한을 확인해 주세요.");}
    }
    private void open(long current){
        if(!foreground||current!=epoch||!targetConnected())return;
        long acquired=hub.acquire(address,true,new AapConnectionOwner.Listener(){
            public void state(ListeningState.Snapshot next){
                if(!foreground||current!=epoch)return;
                state=next;
                if(!next.live()){
                    lease=0;verifying=false;pending=Mode.UNKNOWN;
                    message=next.end==ListeningState.End.EXPIRED?null:
                            next.end==ListeningState.End.TIMEOUT?"적용 여부를 확인하지 못했어요. 다시 확인해 주세요.":
                            next.end==ListeningState.End.DISCONNECTED?"AirPods 연결이 끊겼어요.":"현재 상태를 다시 확인해 주세요.";
                }else if(next.phase==ListeningState.Phase.READY){verifying=false;message=null;}
                draw();
                // acquire() can synchronously deliver a report from a shared battery connection.
                handler.post(()->applyPending(current));
            }
        });
        lease=hub.contains(acquired)?acquired:0;
    }
    private void applyPending(long current){
        if(!foreground||current!=epoch||pending==Mode.UNKNOWN||state==null||state.phase!=ListeningState.Phase.READY)return;
        if(SystemClock.elapsedRealtime()>pendingUntil||!targetConnected()){lost("연결이 바뀌어 적용하지 않았어요. 다시 선택해 주세요.");return;}
        Mode target=pending;pending=Mode.UNKNOWN;
        if(state.observed!=target&&!hub.request(lease,target))message="변경하지 못했어요. 현재 상태를 다시 확인해 주세요.";
        draw();
    }
    private void lost(String why){invalidate();state=null;connected=false;message=why;draw();}
    private void invalidate(){
        epoch++;handler.removeCallbacksAndMessages(null);pending=Mode.UNKNOWN;verifying=false;
        long old=lease;lease=0;if(old!=0)hub.release(old);
        if(profile!=null&&adapter!=null){try{adapter.closeProfileProxy(BluetoothProfile.A2DP,profile);}catch(RuntimeException ignored){}profile=null;}
    }
    private void draw(){
        boolean busy=verifying||pending!=Mode.UNKNOWN||(state!=null&&state.phase==ListeningState.Phase.APPLYING);
        screen.render(state,message,foreground&&connected&&!busy,pending);
    }
}
