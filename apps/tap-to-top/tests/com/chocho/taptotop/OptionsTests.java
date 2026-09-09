package com.chocho.taptotop;

public final class OptionsTests {
    private static int assertions;
    static void check(boolean condition,String why) { assertions++; if(!condition) throw new AssertionError(why); }
    public static void main(String[] args) {
        ScrollOptions defaults=ScrollOptions.restore(null,null,null);
        check(defaults.taps==1 && defaults.strength==100 && defaults.distance==62,"migration defaults");
        check(defaults.durationMillis(false)==100 && defaults.durationMillis(true)==45,"existing timings retained");
        check(Math.abs(defaults.startFraction()-.28f)<.0001f,"existing stroke start retained");
        ScrollOptions invalid=ScrollOptions.restore(0,"bad",90);
        check(invalid.taps==1 && invalid.strength==100 && invalid.distance==62,"corrupt values default individually");
        long previousGeneral=Long.MAX_VALUE,previousX=Long.MAX_VALUE;
        for(int s=60;s<=160;s++) {
            for(int d=40;d<=75;d++) {
                ScrollOptions o=new ScrollOptions(2,s,d);
                check(o.durationMillis(false)>0 && o.durationMillis(true)>0,"positive bounded duration");
                check(o.startFraction()>=.15f && o.startFraction()<=.5f,"safe endpoints");
            }
            ScrollOptions o=new ScrollOptions(1,s,62);
            check(o.durationMillis(false)<=previousGeneral && o.durationMillis(true)<=previousX,"stronger means faster input");
            previousGeneral=o.durationMillis(false); previousX=o.durationMillis(true);
        }
        for(int[] values:new int[][]{{0,100,62},{3,100,62},{1,59,62},{1,161,62},{1,100,39},{1,100,76}}) {
            boolean threw=false;
            try {new ScrollOptions(values[0],values[1],values[2]);} catch(IllegalArgumentException expected) {threw=true;}
            check(threw,"out of range rejected");
        }
        TapTrigger t=new TapTrigger();
        check(t.accept(0,10,10,1,32),"single fires immediately");
        check(!t.accept(100,10,10,2,32),"double first never fires");
        check(t.accept(450,10,10,2,32),"inclusive double boundary");
        check(!t.accept(500,10,10,2,32),"no duplicate third tap");
        check(!t.accept(851,10,10,2,32),"expired tap never fires");
        check(t.accept(900,10,10,2,32),"expired second becomes new first");
        check(!t.accept(1000,10,10,2,32),"new first");
        check(!t.accept(1100,99,10,2,32),"distant taps never combine");
        t.reset();
        check(!t.accept(1200,99,10,2,32),"drag or outside reset prevents fire");
        check(!t.accept(100,99,10,2,32),"backward clock doesn't match");
        check(t.accept(101,99,10,1,32),"switch to single removes pending");
        check(!t.accept(102,99,10,2,32),"single pending not reused");
        System.out.println("OptionsTests: "+assertions+" assertions passed");
    }
}
