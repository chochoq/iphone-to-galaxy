package com.chocho.taptotop;

/** Pure capability policy. Keep AndroidX and framework contracts distinct (D-006). */
public final class ScrollPolicy {
    public enum Route { ANDROIDX_TOP, FRAMEWORK_TOP, SINGLE_FLING, NO_UP }
    public enum RowOrder { UNKNOWN, FORWARD, REVERSE }
    public enum XRoute { SKIP, NO_UP, RESELECT, PREPARE }
    public static final String COMPAT_PROPERTIES =
            "androidx.view.accessibility.AccessibilityNodeInfoCompat.BOOLEAN_PROPERTY_KEY";
    public static final int COMPAT_GRANULAR = 1 << 26;
    public static final String COMPAT_AMOUNT =
            "androidx.core.view.accessibility.action.ARGUMENT_SCROLL_AMOUNT_FLOAT";
    public static final String FRAMEWORK_AMOUNT =
            "android.view.accessibility.action.ARGUMENT_SCROLL_AMOUNT_FLOAT";

    public static Route route(boolean enabled, boolean compat, boolean framework,
            boolean backward, boolean up, boolean verticalCollection) {
        if (!enabled) return Route.SINGLE_FLING;
        if (compat && verticalCollection && !backward) return Route.NO_UP;
        // RecyclerView 1.4.0 handles infinity in BACKWARD, not directional UP.
        if (compat && backward && verticalCollection) return Route.ANDROIDX_TOP;
        if (framework && up) return Route.FRAMEWORK_TOP;
        return Route.SINGLE_FLING;
    }

    public static boolean verticalCollection(int rows, int columns) {
        return rows > 1 && columns == 1;
    }

    public static boolean forwardOrder(int firstRow, int firstY, int nextRow, int nextY) {
        return firstRow>=0 && nextRow>=0 && ((long)nextRow-firstRow)*((long)nextY-firstY)>0;
    }

    /** Device-verified HOME feeds only; never override an observed reverse order. */
    public static boolean verifiedVerticalOrder(RowOrder order, String packageName,
            String className, String viewId, boolean selectedInstagramHome, boolean selectedYouTubeHome) {
        if(order==RowOrder.REVERSE)return false;
        if(order==RowOrder.FORWARD)return true;
        return (selectedInstagramHome&&"com.instagram.android".equals(packageName)
                &&"androidx.recyclerview.widget.RecyclerView".equals(className)
                &&"android:id/list".equals(viewId))
                ||(selectedYouTubeHome&&"com.google.android.youtube".equals(packageName)
                &&"android.support.v7.widget.RecyclerView".equals(className)
                &&"com.google.android.youtube:id/results".equals(viewId));
    }

    public static boolean knownHomeLabel(CharSequence label) {
        return label!=null&&("홈".contentEquals(label)||"Home".contentEquals(label));
    }

    public static boolean knownXFeedLabel(CharSequence label) {
        return label!=null&&("추천".contentEquals(label)||"For you".contentEquals(label)
                ||"팔로우 중".contentEquals(label)||"Following".contentEquals(label));
    }

    public static XRoute xRoute(boolean enabled, boolean fallback, String pkg, String clazz,
            boolean vertical, boolean pagerAncestor, boolean sameWindow, boolean reverse,
            int homes, int feedTabs, boolean up, boolean backward) {
        if(!enabled||!fallback||!"com.twitter.android".equals(pkg)||!"android.view.View".equals(clazz)
                ||!vertical||!pagerAncestor||!sameWindow||reverse||homes>1||feedTabs>1)return XRoute.SKIP;
        if(homes==1&&feedTabs==1)return up||backward?XRoute.RESELECT:XRoute.NO_UP;
        // Preparation is one normal upward action, never navigation. Re-identify home after it.
        if(homes==0&&up)return XRoute.PREPARE;
        return XRoute.SKIP;
    }

    public static boolean xMayContinue(long expectedToken,long currentToken,int expectedWindow,int currentWindow,
            boolean enabled,boolean sameTarget,int attempt,long elapsed) {
        return expectedToken==currentToken&&expectedWindow>=0&&expectedWindow==currentWindow
                &&enabled&&sameTarget&&attempt>=0&&attempt<3&&elapsed>=0&&elapsed<=600;
    }

    public static boolean mayExpandYouTubeHeader(boolean selectedHome, boolean sameWindow,
            boolean descendant, String packageName, String listId, String headerId) {
        return selectedHome&&sameWindow&&descendant&&"com.google.android.youtube".equals(packageName)
                &&"com.google.android.youtube:id/results".equals(listId)
                &&"com.google.android.youtube:id/browse_fragment_layout_coordinator_layout".equals(headerId);
    }

    public static boolean horizontalOnly(boolean up, boolean down, boolean left,
            boolean right, int rows, int columns) {
        return (rows == 1 && columns > 1) || (!up && !down && (left || right));
    }

    public static long score(int width, int height, int minimumWidth, int minimumHeight,
            boolean horizontalOnly) {
        if (horizontalOnly || width < minimumWidth || height < minimumHeight) return -1;
        return (long) width * height;
    }
    public static boolean delegateWrapper(boolean bareWrapper, boolean descendant,
            boolean supportedVerticalList, int wrapperWidth, int wrapperHeight, int listWidth, int listHeight) {
        return bareWrapper&&descendant&&supportedVerticalList&&wrapperWidth>0&&wrapperHeight>0
                &&listWidth<=wrapperWidth&&listHeight<=wrapperHeight
                &&(long)listWidth*100>=(long)wrapperWidth*90
                &&(long)listHeight*100>=(long)wrapperHeight*75;
    }
    private ScrollPolicy() {}
}
