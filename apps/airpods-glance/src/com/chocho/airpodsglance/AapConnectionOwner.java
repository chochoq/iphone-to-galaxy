package com.chocho.airpodsglance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import com.chocho.airpodsglance.ListeningProtocol.Mode;
import com.chocho.airpodsglance.ListeningState.End;

/** One bounded transport shared by battery and foreground control leases. No Android dependency. */
public final class AapConnectionOwner {
    public static final long WINDOW_MS=30_000;
    public interface Clock {long now();}
    public interface Transport {void start();void stop();boolean write(Mode mode);}
    public interface Factory {Transport create(String address,Receiver receiver);}
    public static final class BatteryReport {
        public final AapBatteryDecoder.Decoded value;
        public final long receivedAt,observedAtEpochMillis;
        public BatteryReport(AapBatteryDecoder.Decoded value,long receivedAt,long observedAtEpochMillis){
            this.value=value;this.receivedAt=receivedAt;this.observedAtEpochMillis=observedAtEpochMillis;
        }
    }
    public interface Receiver {
        void ready();void stage(String stage);void battery(BatteryReport battery);
        void listening(ListeningProtocol.Report report,long receivedAt);void failed();
    }
    public interface Listener {
        default void state(ListeningState.Snapshot state){}
        default void stage(String stage){}
        default void battery(BatteryReport battery){}
    }
    private static final class Client {
        final Listener listener;final boolean control;
        Client(Listener l,boolean c){listener=l;control=c;}
    }
    private final Clock clock;private final Factory factory;
    private final Map<Long,Client> clients=new LinkedHashMap<>();
    private Transport transport;private ListeningState state=new ListeningState();
    private BatteryReport lastBattery;private boolean ready;
    private String address;private long generation,leaseSequence,openedAt,pendingLease;
    public AapConnectionOwner(Clock clock,Factory factory){this.clock=clock;this.factory=factory;state.close(End.RELEASED);}
    public synchronized long acquire(String next,boolean control,Listener listener){
        if(next==null||next.isEmpty()||listener==null)throw new IllegalArgumentException("Selected device required");
        tick();
        if(active()&&!next.equals(address))close(End.TARGET_CHANGED);
        boolean newConnection=!active();
        if(newConnection){address=next;openedAt=clock.now();generation++;state=new ListeningState();lastBattery=null;ready=false;}
        long lease=++leaseSequence;clients.put(lease,new Client(listener,control));
        listener.state(state.snapshot());
        if(clients.containsKey(lease)&&lastBattery!=null&&clock.now()-lastBattery.receivedAt<WINDOW_MS)
            listener.battery(lastBattery);
        if(newConnection&&clients.containsKey(lease)){
            final long current=generation;
            try {
                transport=factory.create(next,new Receiver(){
                    @Override public void ready(){synchronized(AapConnectionOwner.this){if(valid(current)){ready=true;state.ready();notifyState();}}}
                    @Override public void stage(String event){synchronized(AapConnectionOwner.this){for(Client c:copy()){if(!valid(current))return;if(clients.containsValue(c))c.listener.stage(event);}}}
                    @Override public void battery(BatteryReport b){synchronized(AapConnectionOwner.this){
                        tick();long now=clock.now();
                        if(!valid(current)||!ready||b==null||b.value==null||b.observedAtEpochMillis<=0
                                ||b.receivedAt<openedAt||b.receivedAt>now||now-b.receivedAt>=WINDOW_MS
                                ||(lastBattery!=null&&b.receivedAt<=lastBattery.receivedAt))return;
                        lastBattery=b;
                        for(Client c:copy()){if(!valid(current))return;if(clients.containsValue(c))c.listener.battery(b);}
                    }}
                    @Override public void listening(ListeningProtocol.Report r,long at){synchronized(AapConnectionOwner.this){
                        tick();if(valid(current)&&state.observe(r,at))notifyState();
                    }}
                    @Override public void failed(){synchronized(AapConnectionOwner.this){if(valid(current))close(End.FAILED);}}
                });
                if(!clients.isEmpty()&&generation==current)transport.start();else {transport.stop();transport=null;}
            }catch(RuntimeException e){close(End.FAILED);}
        }
        return lease;
    }
    public synchronized void release(long lease){
        // A queued write may not outlive the foreground owner that authorized it.
        // Closing this one window is safer than letting a battery lease keep that write alive.
        if(lease==pendingLease&&state.snapshot().phase==ListeningState.Phase.APPLYING){close(End.RELEASED);return;}
        if(clients.remove(lease)!=null&&clients.isEmpty())close(End.RELEASED);
    }
    public synchronized boolean contains(long lease){return clients.containsKey(lease)&&active();}
    public synchronized boolean request(long lease,Mode mode){
        tick();Client c=clients.get(lease);
        if(c==null||!c.control||!active()||!state.request(mode,clock.now(),openedAt+WINDOW_MS))return false;
        pendingLease=lease;
        notifyState();
        // The listener may release its lease while receiving the pending-state notification.
        if(!contains(lease)||transport==null){if(active())close(End.RELEASED);return false;}
        boolean sent;
        try{sent=transport.write(mode);}catch(RuntimeException e){sent=false;}
        if(!sent)close(End.FAILED);
        return sent;
    }
    public synchronized void tick(){
        if(!active())return;
        long now=clock.now();
        if(state.timedOut(now))close(End.TIMEOUT);
        else if(now-openedAt>=WINDOW_MS)close(End.EXPIRED);
    }
    public synchronized void close(End reason){
        if(!active()&&clients.isEmpty())return;
        state.close(reason);ListeningState.Snapshot ended=state.snapshot();
        ArrayList<Client> previous=copy();clients.clear();address=null;lastBattery=null;ready=false;
        Transport old=transport;transport=null;generation++;
        if(old!=null)old.stop();
        for(Client c:previous)c.listener.state(ended);
    }
    public synchronized boolean active(){return state.snapshot().live();}
    public synchronized int clientCount(){return clients.size();}
    public synchronized long remainingMillis(){return active()?Math.max(0,openedAt+WINDOW_MS-clock.now()):0;}
    public synchronized ListeningState.Snapshot snapshot(){return state.snapshot();}
    private boolean valid(long current){return current==generation&&active();}
    private ArrayList<Client> copy(){return new ArrayList<>(clients.values());}
    private void notifyState(){
        long current=generation;ListeningState.Snapshot s=state.snapshot();
        for(Client c:copy()){if(current!=generation)return;if(clients.containsValue(c))c.listener.state(s);}
    }
}
