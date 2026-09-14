package com.chocho.airpodsglance;
public final class WidgetVariantTests {
    static int checks;
    static void ok(boolean v){checks++;if(!v)throw new AssertionError("check "+checks);}
    public static void main(String[]args){
        ok(WidgetLayout.forVariant(WidgetVariant.SMALL,217,234,.8f).mode==WidgetLayout.Mode.SQUARE);
        ok(WidgetLayout.forVariant(WidgetVariant.WIDE,475,234,.8f).mode==WidgetLayout.Mode.RINGS);
        ok(WidgetLayout.forVariant(WidgetVariant.LARGE,475,366,.8f).mode==WidgetLayout.Mode.LARGE_RINGS);
        for(WidgetVariant v:WidgetVariant.values()){
            ok(WidgetLayout.forVariant(v,475,102,1).mode!=WidgetLayout.Mode.COMPACT);
            for(float w:new float[]{110,168,180,210,217,240,300,320,360,420,475})for(float h:new float[]{64,172,194,198,218,234,236,260,280,366})for(float f:new float[]{.8f,1,1.3f,2}){
                WidgetLayout l=WidgetLayout.forVariant(v,w,h,f);ok(l.mode!=WidgetLayout.Mode.COMPACT);ok(l.mode!=null);
            }
        }
        System.out.println("PASS WidgetVariantTests "+checks+" checks");
    }
}
