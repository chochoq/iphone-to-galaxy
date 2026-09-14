package com.chocho.airpodsglance;

/** Content fills the actual host area. Font preference is respected, never changed globally. */
public final class WidgetSizing {
    public final float ring,percent,label,padX,padY;
    public final boolean strip;
    private WidgetSizing(float ring,float percent,float label,float x,float y,boolean strip){
        this.ring=ring;this.percent=percent;this.label=label;padX=x;padY=y;this.strip=strip;
    }
    public static float hostScale(float scale){return Float.isFinite(scale)&&scale>=.5f&&scale<=1?scale:1;}
    public static WidgetSizing of(WidgetVariant variant,float width,float height,float font,float scale,boolean refresh){
        float f=Float.isFinite(font)&&font>0?font:1;
        boolean single=variant==WidgetVariant.SINGLE,square=WidgetLayout.forVariant(variant,width,height,f).mode==WidgetLayout.Mode.SQUARE,strip=!single&&!square&&height<160;
        float x=single?4:square?8:strip&&width<240?6:12,y=single?4:strip?6:square?8:10;
        float hit=refresh&&!square?(float)Math.ceil(48/hostScale(scale)):0;
        float cell=(width-2*x-hit)/(single?1:square?2:3);
        float percent=Math.min(single?24:square?26:strip?30:variant==WidgetVariant.LARGE?32:28,Math.max(14,(cell-8)/(2.9f*f)));
        float label=single?12:square?13:14;
        // Reserve conservative font metrics, label padding, and a full footer before growing artwork.
        float available=(height-2*y)/(square?2:1)-percent*f*1.32f;
        if(!strip)available-=label*f*1.32f+3;
        if(!single&&!square&&!strip)available-=14*f*1.32f+8;
        float ring=Math.max(24,Math.min(Math.min(cell-8,available),square?92:148));
        return new WidgetSizing(ring,percent,label,x,y,strip);
    }
}
