package com.chocho.airpodsglance;

/** Pure sizing policy. Never changes a launcher placement or a user's font setting. */
public final class WidgetLayout {
    public enum Mode { COMPACT, RINGS, SQUARE, LARGE_RINGS, SINGLE, LIST, OPEN_APP }
    public final Mode mode;
    public final boolean refresh;
    private WidgetLayout(Mode mode,boolean refresh){this.mode=mode;this.refresh=refresh;}
    public static WidgetLayout forVariant(WidgetVariant variant,float width,float height,float font){
        if(!Float.isFinite(width)||!Float.isFinite(height))return new WidgetLayout(Mode.OPEN_APP,false);
        if(font>1.4f)return width>=210&&height>=230?new WidgetLayout(Mode.LIST,width>=320):new WidgetLayout(Mode.OPEN_APP,false);
        if(variant==WidgetVariant.SINGLE)return new WidgetLayout(width>=72&&height>=88?Mode.SINGLE:Mode.OPEN_APP,false);
        if(variant==WidgetVariant.SMALL&&width>=168&&height>=(font>1.15f?218:198))return new WidgetLayout(Mode.SQUARE,true);
        if(variant==WidgetVariant.SMALL&&width>=210&&height>=(font>1.15f?112:96))return new WidgetLayout(Mode.RINGS,height>=160&&width>=320);
        if(variant==WidgetVariant.WIDE&&width>=210&&height>=(font>1.15f?112:96))return new WidgetLayout(Mode.RINGS,height>=160&&width>=320);
        if(variant==WidgetVariant.LARGE&&width>=210&&height>=(font>1.15f?112:96))return new WidgetLayout(Mode.LARGE_RINGS,height>=160&&width>=420);
        return new WidgetLayout(Mode.OPEN_APP,false);
    }
    public static WidgetLayout forSize(float width,float height,float font){
        if(!Float.isFinite(width)||!Float.isFinite(height)||width<110||height<48)
            return new WidgetLayout(Mode.OPEN_APP,false);
        if(font>1.4f){
            if(width>=210&&height>=230)return new WidgetLayout(Mode.LIST,width>=320);
            if(width>=360&&height>=128)return new WidgetLayout(Mode.COMPACT,width>=420);
            return new WidgetLayout(Mode.OPEN_APP,false);
        }
        if(width>=240&&height>=(font>1.15f?194:172))return new WidgetLayout(Mode.RINGS,width>=320);
        if(width>=(font>1.15f?210:168)&&height>=(font>1.15f?92:64))
            return new WidgetLayout(Mode.COMPACT,width>=300&&height>=88);
        return new WidgetLayout(Mode.OPEN_APP,false);
    }
}
