package com.chocho.taptotop;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.List;

public final class TapToTopService extends AccessibilityService {
    private AppSettings settings;
    private final TapTrigger tapTrigger=new TapTrigger();
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener settingsListener=(p,k) -> {
        tapTrigger.reset();
        cancelPendingRetry();
        if("enabled".equals(k)) this.handler.post(this::installOverlay);
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingRetry;
    private int pendingWindowId=-1;
    private long pendingGeneration;
    private WindowManager windowManager;
    private View overlay;
    private float downX;
    private float downY;
    private long downTime;
    private boolean dragHandled;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        settings=new AppSettings(this);
        settings.prefs.registerOnSharedPreferenceChangeListener(settingsListener);
        installOverlay();
    }

    private void installOverlay() {
        removeOverlay();
        if(settings==null || !settings.enabled()) return;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) return;

        overlay = new View(this);
        overlay.setBackgroundColor(Color.TRANSPARENT);
        overlay.setContentDescription("맨 위로 이동");
        overlay.setOnTouchListener(this::handleOverlayTouch);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                statusBarHeight(),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP;
        params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        params.setTitle("TapToTopStatusBarTarget");
        windowManager.addView(overlay, params);
    }

    private boolean handleOverlayTouch(View view, MotionEvent event) {
        float movementThreshold = dp(14);
        float notificationThreshold = dp(24);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                cancelPendingRetry();
                downX = event.getRawX();
                downY = event.getRawY();
                downTime = SystemClock.uptimeMillis();
                dragHandled = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                openNotificationsForDownwardDrag(event, notificationThreshold);
                return true;

            case MotionEvent.ACTION_UP:
                float totalX = Math.abs(event.getRawX() - downX);
                float totalY = Math.abs(event.getRawY() - downY);
                long elapsed = SystemClock.uptimeMillis() - downTime;
                if (!dragHandled && totalX < movementThreshold && totalY < movementThreshold
                        && elapsed < 500) {
                    ScrollOptions options=settings.options();
                    if(tapTrigger.accept(SystemClock.uptimeMillis(),event.getRawX(),event.getRawY(),
                            options.taps,dp(32))) scrollToTop();
                    view.performClick();
                } else tapTrigger.reset();
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                cancelPendingRetry();
                dragHandled=true;
                tapTrigger.reset();
                return true;

            case MotionEvent.ACTION_CANCEL:
                tapTrigger.reset();
                openNotificationsForDownwardDrag(event, notificationThreshold);
                cancelPendingRetry();
                return true;

            case MotionEvent.ACTION_OUTSIDE:
                tapTrigger.reset();
                cancelPendingRetry();
                return true;

            default:
                return true;
        }
    }

    private void openNotificationsForDownwardDrag(
            MotionEvent event, float notificationThreshold) {
        float dx = event.getRawX() - downX;
        float dy = event.getRawY() - downY;
        if (!dragHandled && dy > notificationThreshold && Math.abs(dy) > Math.abs(dx)) {
            dragHandled = true;
            tapTrigger.reset();
            cancelPendingRetry();
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
        }
    }

    private void scrollToTop() {
        cancelPendingRetry();
        attemptScroll(0,activeApplicationWindowId());
    }

    private void cancelPendingRetry() {
        pendingGeneration++;
        if(pendingRetry!=null)ScrollAttempt.record("대기 중인 추가 동작을 취소했어요.");
        if(pendingRetry!=null)handler.removeCallbacks(pendingRetry);
        pendingRetry=null;pendingWindowId=-1;
    }

    private int activeApplicationWindowId() {
        int focused=-1;
        for(AccessibilityWindowInfo w:getWindows()) {
            if(w.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION)continue;
            if(w.isActive())return w.getId();
            if(w.isFocused())focused=w.getId();
        }
        return focused;
    }

    private void attemptScroll(int attempt,int windowId) {
        if(windowManager==null||settings==null||!settings.enabled())return;
        if(attempt>0) {
            if(activeApplicationWindowId()!=windowId) {
                ScrollAttempt.record("화면이 바뀌어 이동하지 않았어요.");return;
            }
            if(Build.VERSION.SDK_INT>=33)clearCache();
        }
        NodeCandidate scan=findBestScrollableNode();
        AccessibilityNodeInfo target=scan.node;
        // Never act on a formerly visible X navigation node left in the service cache.
        if(target!=null&&"com.twitter.android".contentEquals(target.getPackageName()==null?"":target.getPackageName())) {
            if(Build.VERSION.SDK_INT>=33)clearCache();
            scan=findBestScrollableNode();target=scan.node;
        }
        if (target==null) {
            // A fresh WebView constructs its virtual tree asynchronously on first inspection.
            // Retry only the read, within the original window. Never retry a movement request.
            if(scan.webTreePending&&attempt<2&&windowId>=0) {
                pendingWindowId=windowId;
                pendingRetry=()->{pendingRetry=null;pendingWindowId=-1;attemptScroll(attempt+1,windowId);};
                handler.postDelayed(pendingRetry,120);
                return;
            }
            ScrollAttempt.record("움직일 세로 영역을 찾지 못했어요.");
            return;
        }
        if (!target.refresh()) {
            ScrollAttempt.record("화면이 바뀌어 이동하지 않았어요. 다시 눌러 주세요.");
            return;
        }
        AccessibilityNodeInfo.CollectionInfo collection=target.getCollectionInfo();
        boolean vertical=collection!=null&&ScrollPolicy.verticalCollection(
                collection.getRowCount(),collection.getColumnCount())&&ScrollPolicy.verifiedVerticalOrder(
                        rowOrder(target),target.getPackageName()==null?null:target.getPackageName().toString(),
                        target.getClassName()==null?null:target.getClassName().toString(),
                        target.getViewIdResourceName(),scan.selectedInstagramHome,scan.selectedYouTubeHome);
        boolean compat=(target.getExtras().getInt(ScrollPolicy.COMPAT_PROPERTIES,0)
                & ScrollPolicy.COMPAT_GRANULAR)!=0;
        boolean framework=Build.VERSION.SDK_INT>=35&&target.isGranularScrollingSupported();
        ScrollPolicy.Route route=ScrollPolicy.route(settings.nativeTop(),compat,framework,
                supportsAction(target,AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD),
                supportsAction(target,AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.getId()),vertical);
        if (route==ScrollPolicy.Route.NO_UP) {
            ScrollAttempt.record(expandYouTubeHeader(scan,target)?"상단 메뉴 펼치기를 요청했어요.":
                    "위로 이동할 동작이 없어 추가로 움직이지 않았어요.");
            return;
        }
        if (route!=ScrollPolicy.Route.SINGLE_FLING) {
            Bundle args=new Bundle();
            boolean androidX=route==ScrollPolicy.Route.ANDROIDX_TOP;
            args.putFloat(androidX?ScrollPolicy.COMPAT_AMOUNT:ScrollPolicy.FRAMEWORK_AMOUNT,
                    Float.POSITIVE_INFINITY);
            // The outer scroll action can supersede the list's motion. Prepare it first,
            // so the single destination request is the last movement requested by us.
            boolean preparedHeader=expandYouTubeHeader(scan,target);
            boolean accepted=target.performAction(androidX?AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD:
                    AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.getId(),args);
            ScrollAttempt.record(accepted?"앱에 맨 위 이동을 요청했어요.\n실제 도착 여부는 화면에서 확인해 주세요.":
                    preparedHeader?"상단 영역을 펼쳤지만 목록이 맨 위 이동 요청을 받지 않았어요.\n추가 동작은 하지 않았어요.":
                    "앱이 맨 위 이동 요청을 받지 않았어요.\n동작이 겹치지 않도록 추가로 쓸어 올리지는 않았어요.");
            // A true return is acceptance, not arrival. Never add a gesture or retry here.
            return;
        }
        if(handleXHome(scan,target))return;
        ScrollOptions options=settings.options();

        Rect bounds = gestureBounds(target);
        float x = bounds.centerX();
        float startY = bounds.top + bounds.height() * options.startFraction();
        float endY = bounds.top + bounds.height() * 0.90f;
        if (endY - startY < dp(160)) {
            ScrollAttempt.record("움직일 영역이 너무 작아 이동하지 않았어요.");
            return;
        }

        Path path = new Path();
        path.moveTo(x, startY);
        path.lineTo(x, endY);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(
                        path, 0, flingDuration(target)))
                .build();
        boolean sent=dispatchGesture(gesture, null, null);
        ScrollAttempt.record(!sent?"쓸어 올리기를 시작하지 못했어요.":settings.nativeTop()?
                "이 화면은 맨 위 이동 지원을 확인하지 못했어요.\n기존 방식으로 한 번 쓸어 올렸어요.":
                "기존 방식으로 한 번 쓸어 올렸어요.");
    }

    private ScrollPolicy.XRoute xRoute(NodeCandidate scan,AccessibilityNodeInfo target) {
        AccessibilityNodeInfo.CollectionInfo c=target.getCollectionInfo();
        return ScrollPolicy.xRoute(settings.nativeTop(),true,
                target.getPackageName()==null?null:target.getPackageName().toString(),
                target.getClassName()==null?null:target.getClassName().toString(),
                c!=null&&ScrollPolicy.verticalCollection(c.getRowCount(),c.getColumnCount()),
                hasLargeHorizontalAncestor(target),target.getWindowId()==activeApplicationWindowId(),
                rowOrder(target)==ScrollPolicy.RowOrder.REVERSE,scan.xHomes,scan.xFeedTabs,
                supportsAction(target,AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.getId()),
                supportsAction(target,AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD));
    }

    private boolean handleXHome(NodeCandidate scan,AccessibilityNodeInfo target) {
        if(!settings.nativeTop()||!"com.twitter.android".contentEquals(target.getPackageName()==null?"":target.getPackageName()))return false;
        ScrollPolicy.XRoute route=xRoute(scan,target);
        if(route==ScrollPolicy.XRoute.SKIP)return false;
        if(route==ScrollPolicy.XRoute.NO_UP) {
            ScrollAttempt.record("이미 위쪽이어서 홈을 다시 누르지 않았어요.");return true;
        }
        long token=pendingGeneration,start=SystemClock.uptimeMillis();
        int window=target.getWindowId();
        if(route==ScrollPolicy.XRoute.RESELECT) {
            queueXHome(scan,target,token,window,start);return true;
        }
        boolean accepted=target.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.getId());
        if(!accepted) {ScrollAttempt.record("X가 위로 이동 요청을 받지 않았어요. 추가 동작은 하지 않았어요.");return true;}
        ScrollAttempt.record("X에서 한 번 위로 이동하고 홈 동작을 확인하고 있어요.");
        scheduleXRead(target,token,window,start,0);
        return true;
    }

    private void scheduleXRead(AccessibilityNodeInfo original,long token,int window,long start,int attempt) {
        pendingWindowId=window;
        pendingRetry=()->{
            if(!ScrollPolicy.xMayContinue(token,pendingGeneration,window,activeApplicationWindowId(),
                    settings!=null&&settings.enabled()&&settings.nativeTop(),true,attempt,SystemClock.uptimeMillis()-start)) {
                pendingRetry=null;pendingWindowId=-1;
                ScrollAttempt.record("X의 홈 확인을 중단했어요. 추가 동작은 하지 않았어요.");return;
            }
            pendingRetry=null;
            if(Build.VERSION.SDK_INT>=33)clearCache();
            NodeCandidate next=findBestScrollableNode();
            AccessibilityNodeInfo current=next.node;
            if(current==null||!current.equals(original)||!current.refresh()
                    ||!ScrollPolicy.xMayContinue(token,pendingGeneration,window,activeApplicationWindowId(),
                        settings.enabled()&&settings.nativeTop(),true,attempt,SystemClock.uptimeMillis()-start)) {
                pendingWindowId=-1;ScrollAttempt.record("화면이 바뀌거나 확인 시간이 지나 추가 동작을 하지 않았어요.");return;
            }
            ScrollPolicy.XRoute route=xRoute(next,current);
            if(route==ScrollPolicy.XRoute.RESELECT)queueXHome(next,current,token,window,start);
            else if(route==ScrollPolicy.XRoute.NO_UP) {
                pendingWindowId=-1;ScrollAttempt.record("위쪽에 도착해 홈을 다시 누르지 않았어요.");
            } else if(attempt<2&&SystemClock.uptimeMillis()-start<600)scheduleXRead(original,token,window,start,attempt+1);
            else {pendingWindowId=-1;ScrollAttempt.record("X의 홈을 확인하지 못해 한 번만 위로 이동했어요.");}
        };
        handler.postDelayed(pendingRetry,80);
    }

    private void queueXHome(NodeCandidate scan,AccessibilityNodeInfo target,long token,int window,long start) {
        final AccessibilityNodeInfo home=scan.xHome,tab=scan.xFeedTab;
        pendingWindowId=window;
        // Allow already queued outside touches/window changes to cancel before navigation.
        pendingRetry=()->{
            pendingRetry=null;pendingWindowId=-1;
            if(!ScrollPolicy.xMayContinue(token,pendingGeneration,window,activeApplicationWindowId(),
                    settings!=null&&settings.enabled()&&settings.nativeTop(),true,0,SystemClock.uptimeMillis()-start))return;
            if(home==null||tab==null||!home.refresh()||!tab.refresh()||!target.refresh()
                    ||!home.isSelected()||!tab.isSelected()||!xFixedLabel(home,0,false)||!xFixedLabel(tab,0,true)
                    ||home.getWindowId()!=window||tab.getWindowId()!=window||target.getWindowId()!=window
                    ||isDescendantOf(home,target)||isDescendantOf(tab,target)) {
                ScrollAttempt.record("X의 현재 홈을 확인하지 못해 추가 동작을 하지 않았어요.");return;
            }
            if(!supportsAction(target,AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                    &&!supportsAction(target,AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.getId())) {
                ScrollAttempt.record("위쪽에 도착해 홈을 다시 누르지 않았어요.");return;
            }
            boolean accepted=home.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            ScrollAttempt.record(accepted?"X의 홈에 맨 위 이동을 요청했어요.\n실제 도착 여부는 화면에서 확인해 주세요.":
                    "X의 홈이 요청을 받지 않았어요. 추가 동작은 하지 않았어요.");
        };
        handler.post(pendingRetry);
    }

    private boolean hasLargeHorizontalAncestor(AccessibilityNodeInfo target) {
        Rect body=new Rect();target.getBoundsInScreen(body);
        AccessibilityNodeInfo p=target.getParent();
        for(int depth=0;p!=null&&depth<12;depth++,p=p.getParent()) {
            AccessibilityNodeInfo.CollectionInfo c=p.getCollectionInfo();
            if(c==null||c.getRowCount()!=1||c.getColumnCount()<2)continue;
            Rect b=new Rect();p.getBoundsInScreen(b);
            if(b.contains(body))return true;
        }
        return false;
    }

    private boolean xFixedLabel(AccessibilityNodeInfo n,int depth,boolean feed) {
        if(n==null||!n.isVisibleToUser())return false;
        if(feed?(ScrollPolicy.knownXFeedLabel(n.getText())||ScrollPolicy.knownXFeedLabel(n.getContentDescription())):
                (ScrollPolicy.knownHomeLabel(n.getText())||ScrollPolicy.knownHomeLabel(n.getContentDescription())))return true;
        if(depth>=2)return false;
        for(int i=0;i<Math.min(8,n.getChildCount());i++)if(xFixedLabel(n.getChild(i),depth+1,feed))return true;
        return false;
    }

    private boolean expandYouTubeHeader(NodeCandidate scan,AccessibilityNodeInfo target) {
        AccessibilityNodeInfo header=scan.youtubeHeader;
        if(!scan.selectedYouTubeHome||header==null||!header.refresh())return false;
        if(!ScrollPolicy.mayExpandYouTubeHeader(true,
                activeApplicationWindowId()==target.getWindowId()&&header.getWindowId()==target.getWindowId(),
                isDescendantOf(target,header),target.getPackageName()==null?null:target.getPackageName().toString(),
                target.getViewIdResourceName(),header.getViewIdResourceName()))return false;
        if(!supportsAction(header,AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD))return false;
        // Outer scroll preparation, once before the destination; no completion timer or retry.
        return header.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }

    private long flingDuration(AccessibilityNodeInfo target) {
        if (target != null && target.getPackageName() != null
                && "com.twitter.android".contentEquals(target.getPackageName())) {
            return settings.options().durationMillis(true);
        }
        return settings.options().durationMillis(false);
    }

    private int preferredUpAction(AccessibilityNodeInfo node) {
        int scrollUp = AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.getId();
        if (supportsAction(node, scrollUp)) return scrollUp;
        int backward = AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD.getId();
        return supportsAction(node, backward) ? backward : 0;
    }

    private boolean supportsAction(AccessibilityNodeInfo node, int actionId) {
        for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList()) {
            if (action.getId() == actionId) return true;
        }
        return false;
    }

    private NodeCandidate findBestScrollableNode() {
        List<AccessibilityNodeInfo> roots = new ArrayList<>();
        boolean activeApplication=false;
        for (AccessibilityWindowInfo window : getWindows()) {
            if(window.getType()==AccessibilityWindowInfo.TYPE_APPLICATION&&window.isActive()) activeApplication=true;
        }
        for (AccessibilityWindowInfo window : getWindows()) {
            if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) continue;
            if (activeApplication?!window.isActive():!window.isFocused()) continue;
            AccessibilityNodeInfo root = window.getRoot();
            if (root != null && !isOwnOrSystemUi(root)) roots.add(root);
        }
        if (roots.isEmpty()) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            AccessibilityWindowInfo window=root==null?null:root.getWindow();
            if (window!=null&&window.getType()==AccessibilityWindowInfo.TYPE_APPLICATION
                    && !isOwnOrSystemUi(root)) roots.add(root);
        }

        NodeCandidate best = new NodeCandidate();
        for (AccessibilityNodeInfo root : roots) {
            collectBestScrollable(root, 0, best);
        }
        return best;
    }

    /** Adapter position zero is not visual top in reverseLayout lists. Require observed order. */
    private ScrollPolicy.RowOrder rowOrder(AccessibilityNodeInfo node) {
        int previousRow=-1,previousY=0;
        boolean pair=false;
        for(int i=0;i<Math.min(node.getChildCount(),100);i++) {
            AccessibilityNodeInfo child=node.getChild(i);
            if(child==null||!child.isVisibleToUser()) continue;
            AccessibilityNodeInfo.CollectionItemInfo item=child.getCollectionItemInfo();
            if(item==null||item.getRowIndex()<0) continue;
            Rect bounds=new Rect();child.getBoundsInScreen(bounds);
            int row=item.getRowIndex(),y=bounds.centerY();
            if(previousRow>=0&&row!=previousRow) {
                if(!ScrollPolicy.forwardOrder(previousRow,previousY,row,y)) return ScrollPolicy.RowOrder.REVERSE;
                pair=true;
            }
            previousRow=row;previousY=y;
        }
        return pair?ScrollPolicy.RowOrder.FORWARD:ScrollPolicy.RowOrder.UNKNOWN;
    }

    private boolean isOwnOrSystemUi(AccessibilityNodeInfo node) {
        CharSequence packageName = node.getPackageName();
        if (packageName == null) return false;
        String value = packageName.toString();
        return getPackageName().equals(value) || "com.android.systemui".equals(value);
    }

    private void collectBestScrollable(
            AccessibilityNodeInfo node, int depth, NodeCandidate best) {
        if (node == null || depth > 45 || ++best.visited>600 || !node.isVisibleToUser()) return;

        if(node.isSelected()&&"com.twitter.android".contentEquals(node.getPackageName()==null?"":node.getPackageName())
                &&"android.view.View".contentEquals(node.getClassName()==null?"":node.getClassName())) {
            Rect b=new Rect();node.getBoundsInScreen(b);
            Rect display=windowManager.getCurrentWindowMetrics().getBounds();
            if(b.height()<=dp(100)&&b.width()<display.width()/2) {
                if(b.centerY()>display.top+display.height()*2/3&&xFixedLabel(node,0,false)) {
                    best.xHome=node;best.xHomes++;
                } else if(b.centerY()<display.centerY()&&xFixedLabel(node,0,true)) {
                    best.xFeedTab=node;best.xFeedTabs++;
                }
            }
        }

        if("com.instagram.android:id/feed_tab".equals(node.getViewIdResourceName())
                &&"com.instagram.android".contentEquals(node.getPackageName()==null?"":node.getPackageName())
                &&selectedTab(node,0))best.selectedInstagramHome=true;
        if(node.isSelected()&&"android.widget.Button".contentEquals(node.getClassName()==null?"":node.getClassName())
                &&"com.google.android.youtube".contentEquals(node.getPackageName()==null?"":node.getPackageName())
                &&selectedHomeLabel(node,0))best.selectedYouTubeHome=true;
        if("com.google.android.youtube:id/browse_fragment_layout_coordinator_layout".equals(node.getViewIdResourceName())
                &&"com.google.android.youtube".contentEquals(node.getPackageName()==null?"":node.getPackageName())
                &&"android.widget.ScrollView".contentEquals(node.getClassName()==null?"":node.getClassName()))best.youtubeHeader=node;

        if("android.webkit.WebView".contentEquals(node.getClassName()==null?"":node.getClassName())
                &&node.getChildCount()==0&&!node.isScrollable())best.webTreePending=true;

        boolean hasUpAction = preferredUpAction(node) != 0;
        boolean canJump = supportsAction(node,
                AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_TO_POSITION.getId());
        if (node.isScrollable() || hasUpAction || canJump) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            Rect display=windowManager.getCurrentWindowMetrics().getBounds();
            if (!bounds.intersect(display)) return;
            AccessibilityNodeInfo.CollectionInfo c=node.getCollectionInfo();
            boolean horizontal=ScrollPolicy.horizontalOnly(
                    supportsAction(node,AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.getId()),
                    supportsAction(node,AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.getId()),
                    supportsAction(node,AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.getId()),
                    supportsAction(node,AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.getId()),
                    c==null?-1:c.getRowCount(),c==null?-1:c.getColumnCount());
            long score=ScrollPolicy.score(bounds.width(),bounds.height(),dp(160),dp(240),horizontal);
            boolean granular=(node.getExtras().getInt(ScrollPolicy.COMPAT_PROPERTIES,0)&ScrollPolicy.COMPAT_GRANULAR)!=0
                    ||(Build.VERSION.SDK_INT>=35&&node.isGranularScrollingSupported());
            boolean verticalList=c!=null&&ScrollPolicy.verticalCollection(c.getRowCount(),c.getColumnCount());
            boolean delegate=score>=0&&best.node!=null&&best.bareWrapper&&granular&&verticalList
                    &&best.bounds.contains(bounds)&&ScrollPolicy.delegateWrapper(true,isDescendantOf(node,best.node),true,
                            best.bounds.width(),best.bounds.height(),bounds.width(),bounds.height());
            if (score >=0 && (score > best.score||delegate)) {
                best.score = delegate?Math.max(score,best.score):score;
                best.node = node;
                best.bounds=new Rect(bounds);
                best.bareWrapper=c==null&&!granular;
            }
        }

        for (int i = 0; i < node.getChildCount() && best.visited<600; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) collectBestScrollable(child, depth + 1, best);
        }
    }

    private boolean isDescendantOf(AccessibilityNodeInfo node,AccessibilityNodeInfo ancestor) {
        AccessibilityNodeInfo p=node.getParent();
        for(int depth=0;p!=null&&depth<45;depth++,p=p.getParent())if(p.equals(ancestor))return true;
        return false;
    }

    private boolean selectedTab(AccessibilityNodeInfo node,int depth) {
        if(node==null||!node.isVisibleToUser())return false;
        if(node.isSelected())return true;
        if(depth>=2)return false;
        for(int i=0;i<Math.min(node.getChildCount(),8);i++)if(selectedTab(node.getChild(i),depth+1))return true;
        return false;
    }

    /** Inspect only an already-selected YouTube navigation button, not video content. */
    private boolean selectedHomeLabel(AccessibilityNodeInfo node,int depth) {
        if(node==null||!node.isVisibleToUser())return false;
        if(ScrollPolicy.knownHomeLabel(node.getText())||ScrollPolicy.knownHomeLabel(node.getContentDescription()))return true;
        if(depth>=2)return false;
        for(int i=0;i<Math.min(node.getChildCount(),8);i++)if(selectedHomeLabel(node.getChild(i),depth+1))return true;
        return false;
    }

    private Rect gestureBounds(AccessibilityNodeInfo target) {
        Rect display = windowManager.getCurrentWindowMetrics().getBounds();
        Rect bounds = new Rect(display);
        bounds.top = Math.max(bounds.top + statusBarHeight() + dp(12), bounds.top);
        bounds.bottom -= dp(28);

        if (target != null) {
            Rect targetBounds = new Rect();
            target.getBoundsInScreen(targetBounds);
            if (targetBounds.width() >= dp(160) && targetBounds.height() >= dp(240)) {
                if (targetBounds.intersect(bounds)) bounds = targetBounds;
            }
        }
        return bounds;
    }

    private int statusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        int systemHeight = resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
        return Math.max(systemHeight, dp(28));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void removeOverlay() {
        if (overlay == null || windowManager == null) return;
        try {
            windowManager.removeView(overlay);
        } catch (RuntimeException ignored) {
            // The system may already have detached the accessibility overlay.
        }
        overlay = null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Only window identity is used while a deferred read is pending; no event content is read.
        if(pendingRetry!=null&&event.getEventType()==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                &&event.getWindowId()!=pendingWindowId)cancelPendingRetry();
    }

    @Override
    public void onInterrupt() {
        tapTrigger.reset();
        cancelPendingRetry();
    }

    @Override
    public void onDestroy() {
        if(settings!=null) settings.prefs.unregisterOnSharedPreferenceChangeListener(settingsListener);
        cancelPendingRetry();
        handler.removeCallbacksAndMessages(null);
        removeOverlay();
        super.onDestroy();
    }

    @Override public void onConfigurationChanged(android.content.res.Configuration configuration) {
        super.onConfigurationChanged(configuration);
        tapTrigger.reset();
        cancelPendingRetry();
        installOverlay();
    }

    private static final class NodeCandidate {
        AccessibilityNodeInfo node;
        int visited;
        boolean webTreePending;
        boolean selectedInstagramHome;
        boolean selectedYouTubeHome;
        AccessibilityNodeInfo youtubeHeader;
        AccessibilityNodeInfo xHome,xFeedTab;
        int xHomes,xFeedTabs;
        Rect bounds;
        boolean bareWrapper;
        long score = Long.MIN_VALUE;
    }
}
