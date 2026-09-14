package com.chocho.airpodsglance;
public final class WidgetSizingTests {
    static int checks;static void ok(boolean v){checks++;if(!v)throw new AssertionError("check "+checks);}
    public static void main(String[] args){
        WidgetSizing small=WidgetSizing.of(WidgetVariant.SMALL,217,234,.8f,.6666667f,true);
        ok(small.ring>=64);ok(small.percent>=25);ok(!small.strip);
        WidgetSizing wide=WidgetSizing.of(WidgetVariant.WIDE,475,102,.8f,.6666667f,false);
        ok(wide.strip);ok(wide.ring>=52);ok(wide.percent>=28);
        WidgetSizing large=WidgetSizing.of(WidgetVariant.LARGE,475,234,.8f,.6666667f,true);
        ok(large.ring>=104);ok(large.percent>=30);ok(!large.strip);
        ok(WidgetLayout.forVariant(WidgetVariant.WIDE,475,102,.8f).mode==WidgetLayout.Mode.RINGS);
        ok(WidgetLayout.forVariant(WidgetVariant.LARGE,475,234,.8f).mode==WidgetLayout.Mode.LARGE_RINGS);
        ok(WidgetSizing.hostScale(Float.NaN)==1);
        WidgetSizing mini=WidgetSizing.of(WidgetVariant.SMALL,217,102,.8f,.6666667f,false);
        ok(mini.strip);ok(mini.ring>=56);ok(mini.percent>=25);
        ok(WidgetLayout.forVariant(WidgetVariant.SMALL,217,102,.8f).mode==WidgetLayout.Mode.RINGS);
        ok(WidgetLayout.forVariant(WidgetVariant.LARGE,346,102,.8f).mode==WidgetLayout.Mode.LARGE_RINGS);
        WidgetSizing single=WidgetSizing.of(WidgetVariant.SINGLE,88,102,.8f,.6666667f,false);
        ok(single.ring>=48);ok(single.percent==24);ok(WidgetLayout.forVariant(WidgetVariant.SINGLE,88,102,.8f).mode==WidgetLayout.Mode.SINGLE);
        for(WidgetVariant v:WidgetVariant.values())for(int w=168;w<=480;w+=24)for(int h=96;h<=366;h+=18)for(float f:new float[]{.8f,1,1.3f,2}){
            WidgetSizing s=WidgetSizing.of(v,w,h,f,.6666667f,w>=420&&h>=160);
            ok(Float.isFinite(s.ring)&&s.ring>=24&&s.ring<=148);ok(s.percent>=14&&s.percent<=32);
        }
        System.out.println("PASS WidgetSizingTests "+checks+" checks");
    }
}
