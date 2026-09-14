package com.chocho.airpodsglance;

/** Presentation only: no Bluetooth calls, timers, preference writes or synthetic measurements. */
public final class WidgetState {
    public final String title;
    public final String[] labels={"왼쪽","오른쪽","케이스"};
    public final BatteryComponent[] parts;
    public final boolean[] charging;
    public final boolean fresh,needsIdentity;
    public final long observed;
    public final String connection;
    private WidgetState(AirPodsSnapshot snapshot,long now){
        title=snapshot.selectedDeviceName==null?"에어팟 한눈에":snapshot.selectedDeviceName;
        needsIdentity=snapshot.identityConfidence==AirPodsSnapshot.IdentityConfidence.NEEDS_CALIBRATION;
        parts=needsIdentity?new BatteryComponent[3]:new BatteryComponent[]{snapshot.left,snapshot.right,snapshot.caseBattery};
        fresh=BatteryPresentation.isFresh(snapshot,now);
        charging=new boolean[3];for(int i=0;i<3;i++)charging[i]=!needsIdentity&&BatteryPresentation.showCharging(snapshot,parts[i],now);
        observed=needsIdentity||snapshot.observedAtEpochMillis>now?0:snapshot.observedAtEpochMillis;
        connection=needsIdentity?"기기 확인 필요":snapshot.connected?"연결됨":snapshot.selectedDeviceName==null?"앱에서 연결해 주세요":"연결 안 됨";
    }
    public static WidgetState from(AirPodsSnapshot snapshot,long now){return new WidgetState(snapshot==null?AirPodsSnapshot.empty(null):snapshot,now);}
    public String percent(int index){return BatteryPresentation.percent(parts[index]);}
    public String description(int index){return labels[index]+" "+(parts[index]==null||parts[index].percent==null?"잔량 알 수 없음":percent(index))+(charging[index]?", 마지막 확인 때 충전 중":"");}
}
