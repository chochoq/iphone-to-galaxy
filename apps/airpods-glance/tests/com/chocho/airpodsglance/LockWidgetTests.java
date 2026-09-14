package com.chocho.airpodsglance;

public final class LockWidgetTests {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("Lock check " + checks); }
    public static void main(String[] args) {
        long now=10_000_000;
        for(int part=0;part<3;part++) for(int p=0;p<=100;p++) for(boolean connected:new boolean[]{false,true}) {
            BatteryComponent component=new BatteryComponent(p,true);
            AirPodsSnapshot snapshot=new AirPodsSnapshot("PRIVATE NAME",connected,connected?AirPodsSnapshot.IdentityConfidence.EXACT:AirPodsSnapshot.IdentityConfidence.NONE,null,component,component,component,now-5_000_000,AirPodsSnapshot.Source.AAP_EXACT_PERCENT);
            LockWidgetState state=LockWidgetState.from(snapshot,now,part);
            check(state.value().equals(p+"%"));check(state.caption().endsWith(" 기록"));
            check(state.observed==snapshot.observedAtEpochMillis);check(!state.description().contains("PRIVATE"));
            check(state.description().contains("마지막 수신"));check(!state.description().contains("충전"));
            for(long timestamp:new long[]{0,-1,now+1}) {
                AirPodsSnapshot invalid=new AirPodsSnapshot("name",true,AirPodsSnapshot.IdentityConfidence.EXACT,null,component,component,component,timestamp,AirPodsSnapshot.Source.AAP_EXACT_PERCENT);
                check(LockWidgetState.from(invalid,now,part).percent==null);
            }
            AirPodsSnapshot uncertain=new AirPodsSnapshot("name",true,AirPodsSnapshot.IdentityConfidence.NEEDS_CALIBRATION,null,component,component,component,now,AirPodsSnapshot.Source.AAP_EXACT_PERCENT);
            check(LockWidgetState.from(uncertain,now,part).value().equals("—"));
            check(LockWidgetState.from(null,now,part).observed==0);
            check(LockWidgetState.from(AirPodsSnapshot.empty(null),now,part).description().contains("알 수 없음"));
        }
        AirPodsSnapshot partial=new AirPodsSnapshot("name",true,AirPodsSnapshot.IdentityConfidence.EXACT,null,
                new BatteryComponent(0,false),new BatteryComponent(100,false),null,now,AirPodsSnapshot.Source.AAP_EXACT_PERCENT);
        check(LockWidgetState.from(partial,now,0).value().equals("0%"));
        check(LockWidgetState.from(partial,now,1).value().equals("100%"));
        check(LockWidgetState.from(partial,now,2).value().equals("—"));
        check(LockWidgetState.from(partial,now,0).label.equals("왼쪽"));
        check(LockWidgetState.from(partial,now,1).label.equals("오른쪽"));
        check(LockWidgetState.from(partial,now,2).label.equals("케이스"));
        for(int part:new int[]{-1,3}) {boolean rejected=false;try{LockWidgetState.from(partial,now,part);}catch(IllegalArgumentException expected){rejected=true;}check(rejected);}
        for(int initial=0;initial<3;initial++) {
            int current=initial;
            for(int tap=1;tap<=30;tap++){current=LockWidgetSelection.next(current);check(current==(initial+tap)%3);}
            check(LockWidgetSelection.valid(-1,initial)==initial);
            check(LockWidgetSelection.valid(3,initial)==initial);
            check(LockWidgetState.from(partial,now,initial).part==initial);
            check(LockWidgetState.from(partial,now,initial).nextLabel().equals(new String[]{"오른쪽","케이스","왼쪽"}[initial]));
        }
        check(LockWidgetSelection.accepts(1,"ours/Left","ours/Left"));
        check(!LockWidgetSelection.accepts(0,"ours/Left","ours/Left"));
        check(!LockWidgetSelection.accepts(-1,"ours/Left","ours/Left"));
        check(!LockWidgetSelection.accepts(1,"ours/Left",null));
        check(!LockWidgetSelection.accepts(1,"ours/Left","ours/Right"));
        check(!LockWidgetSelection.accepts(1,"ours/Left","other/Left"));
        System.out.println("PASS LockWidgetTests "+checks+" checks");
    }
}
