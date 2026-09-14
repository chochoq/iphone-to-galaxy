package com.chocho.airpodsglance;

public final class WidgetTests {
    static int checks;
    static void ok(boolean value){checks++;if(!value)throw new AssertionError("check "+checks);}
    static AirPodsSnapshot snapshot(Integer p,boolean connected,AirPodsSnapshot.IdentityConfidence identity,long time){
        BatteryComponent c=p==null?null:new BatteryComponent(p,true);
        return new AirPodsSnapshot("My AirPods",connected,identity,null,c,c,null,time,AirPodsSnapshot.Source.AAP_EXACT_PERCENT);
    }
    public static void main(String[] args){
        long now=1_000_000;
        for(int p=0;p<=100;p++){
            WidgetState state=WidgetState.from(snapshot(p,true,AirPodsSnapshot.IdentityConfidence.EXACT,now-1000),now);
            ok(state.percent(0).equals(p+"%"));ok(state.charging[0]);ok(state.percent(2).equals("—"));ok(!state.charging[2]);
            state=WidgetState.from(snapshot(p,true,AirPodsSnapshot.IdentityConfidence.NEEDS_CALIBRATION,now-1000),now);
            ok(state.percent(0).equals("—"));ok(!state.charging[0]);ok(state.observed==0);
            state=WidgetState.from(snapshot(p,false,AirPodsSnapshot.IdentityConfidence.NONE,now-1000),now);
            ok(state.percent(0).equals(p+"%"));ok(!state.charging[0]);
        }
        ok(WidgetState.from(null,now).percent(0).equals("—"));
        ok(!WidgetState.from(snapshot(87,true,AirPodsSnapshot.IdentityConfidence.EXACT,now-120001),now).charging[0]);
        ok(WidgetState.from(snapshot(87,true,AirPodsSnapshot.IdentityConfidence.EXACT,now+1),now).observed==0);
        ok(WidgetLayout.forSize(475,102,.8f).mode==WidgetLayout.Mode.COMPACT);
        ok(WidgetLayout.forSize(320,180,1).mode==WidgetLayout.Mode.RINGS);
        ok(WidgetLayout.forSize(240,180,1).mode==WidgetLayout.Mode.RINGS);
        ok(!WidgetLayout.forSize(240,180,1).refresh);
        ok(WidgetLayout.forSize(320,240,2).mode==WidgetLayout.Mode.LIST);
        ok(WidgetLayout.forSize(110,64,2).mode==WidgetLayout.Mode.OPEN_APP);
        ok(WidgetLayout.forSize(475,102,2).mode==WidgetLayout.Mode.OPEN_APP);
        ok(WidgetLayout.forSize(475,108,2).mode==WidgetLayout.Mode.OPEN_APP);
        ok(WidgetLayout.forSize(475,128,2).mode==WidgetLayout.Mode.COMPACT);
        ok(!WidgetLayout.forSize(320,64,1).refresh);
        ok(WidgetLayout.forSize(180,102,1.3f).mode==WidgetLayout.Mode.OPEN_APP);
        for(float w:new float[]{110,167,168,180,240,300,320,360,420,475})
            for(float h:new float[]{48,63,64,79,80,102,108,171,172,194,230,240})
                for(float f:new float[]{.8f,1,1.3f,2})ok(WidgetLayout.forSize(w,h,f).mode!=null);
        System.out.println("PASS WidgetTests "+checks+" checks");
    }
}
