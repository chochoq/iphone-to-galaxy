package com.chocho.airpodsglance;

/** A user's widget choice, not a side effect of resizing the same widget. */
public enum WidgetVariant {
    SMALL("미니 가로","2 × 1",217,102),
    WIDE("가로 원형","3 × 1",346,102),
    LARGE("이전 큰 원형","3 × 1",346,102),
    SINGLE("한 칸 원형","1 × 1",88,102);
    public final String title,cells;
    public final int previewWidth,previewHeight;
    WidgetVariant(String title,String cells,int width,int height){this.title=title;this.cells=cells;previewWidth=width;previewHeight=height;}
}
