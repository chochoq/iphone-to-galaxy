package com.chocho.taptotop;

public final class PolicyTests {
    static int count;
    static void check(boolean value) { if(!value)throw new AssertionError("check "+count);count++; }
    public static void main(String[] args) {
        for(int bits=0;bits<64;bits++) {
            boolean enabled=(bits&1)!=0,compat=(bits&2)!=0,framework=(bits&4)!=0,
                    backward=(bits&8)!=0,up=(bits&16)!=0,vertical=(bits&32)!=0;
            ScrollPolicy.Route expected=!enabled?ScrollPolicy.Route.SINGLE_FLING:
                    compat&&vertical&&!backward?ScrollPolicy.Route.NO_UP:
                    compat&&backward&&vertical?ScrollPolicy.Route.ANDROIDX_TOP:
                    framework&&up?ScrollPolicy.Route.FRAMEWORK_TOP:ScrollPolicy.Route.SINGLE_FLING;
            check(ScrollPolicy.route(enabled,compat,framework,backward,up,vertical)==expected);
        }
        check(!ScrollPolicy.verticalCollection(1,100));
        check(ScrollPolicy.verticalCollection(1000,1));
        check(!ScrollPolicy.verticalCollection(-1,-1));
        check(!ScrollPolicy.verticalCollection(100,3)); // grid orientation needs separate proof
        check(ScrollPolicy.horizontalOnly(false,false,true,false,-1,-1));
        check(ScrollPolicy.horizontalOnly(false,false,false,false,1,100));
        check(!ScrollPolicy.horizontalOnly(true,true,false,false,100,1));
        check(ScrollPolicy.score(1080,1800,160,240,false)>ScrollPolicy.score(1080,300,160,240,false));
        check(ScrollPolicy.score(1080,300,160,240,true)==-1);
        check(ScrollPolicy.score(1080,100,160,240,false)==-1);
        check(!ScrollPolicy.COMPAT_AMOUNT.equals(ScrollPolicy.FRAMEWORK_AMOUNT));
        check(ScrollPolicy.forwardOrder(10,200,11,400));
        check(ScrollPolicy.forwardOrder(11,400,10,200));
        check(!ScrollPolicy.forwardOrder(11,200,10,400));
        check(!ScrollPolicy.forwardOrder(10,200,10,400));
        check(!ScrollPolicy.forwardOrder(-1,200,0,400));
        for(int bits=0;bits<16;bits++) {
            boolean pkg=(bits&1)!=0,clazz=(bits&2)!=0,id=(bits&4)!=0,home=(bits&8)!=0;
            String p=pkg?"com.instagram.android":"other.app";
            String c=clazz?"androidx.recyclerview.widget.RecyclerView":"android.view.View";
            String v=id?"android:id/list":"other:id/list";
            check(ScrollPolicy.verifiedVerticalOrder(ScrollPolicy.RowOrder.UNKNOWN,p,c,v,home,false)==(bits==15));
            check(!ScrollPolicy.verifiedVerticalOrder(ScrollPolicy.RowOrder.REVERSE,p,c,v,home,false));
            check(ScrollPolicy.verifiedVerticalOrder(ScrollPolicy.RowOrder.FORWARD,p,c,v,home,false));
        }
        for(int bits=0;bits<16;bits++) {
            String p=(bits&1)!=0?"com.google.android.youtube":"other.app";
            String c=(bits&2)!=0?"android.support.v7.widget.RecyclerView":"android.view.View";
            String v=(bits&4)!=0?"com.google.android.youtube:id/results":"other:id/list";
            boolean home=(bits&8)!=0;
            check(ScrollPolicy.verifiedVerticalOrder(ScrollPolicy.RowOrder.UNKNOWN,p,c,v,false,home)==(bits==15));
            check(!ScrollPolicy.verifiedVerticalOrder(ScrollPolicy.RowOrder.REVERSE,p,c,v,false,home));
        }
        check(!ScrollPolicy.verifiedVerticalOrder(ScrollPolicy.RowOrder.UNKNOWN,null,null,null,true,true));
        check(ScrollPolicy.knownHomeLabel("홈"));check(ScrollPolicy.knownHomeLabel("Home"));
        check(!ScrollPolicy.knownHomeLabel(null));check(!ScrollPolicy.knownHomeLabel("Home video"));
        check(!ScrollPolicy.knownHomeLabel("홈 추천"));
        for(int bits=0;bits<8;bits++)check(ScrollPolicy.delegateWrapper((bits&1)!=0,(bits&2)!=0,(bits&4)!=0,
                1248,1972,1248,1610)==(bits==7));
        check(ScrollPolicy.delegateWrapper(true,true,true,1000,2000,900,1500));
        check(ScrollPolicy.delegateWrapper(true,true,true,1000,2000,1000,2000));
        check(!ScrollPolicy.delegateWrapper(true,true,true,1000,2000,899,1600));
        check(!ScrollPolicy.delegateWrapper(true,true,true,1000,2000,1000,1499));
        check(!ScrollPolicy.delegateWrapper(true,true,true,1000,2000,1001,2000));
        check(!ScrollPolicy.delegateWrapper(true,true,true,1000,2000,1000,2001));
        check(!ScrollPolicy.delegateWrapper(true,true,true,0,0,0,0));
        for(int bits=0;bits<64;bits++)check(ScrollPolicy.mayExpandYouTubeHeader((bits&1)!=0,(bits&2)!=0,(bits&4)!=0,
                (bits&8)!=0?"com.google.android.youtube":"other.app",
                (bits&16)!=0?"com.google.android.youtube:id/results":"other:id/list",
                (bits&32)!=0?"com.google.android.youtube:id/browse_fragment_layout_coordinator_layout":"other:id/header")== (bits==63));
        for(String s:new String[]{"추천","For you","팔로우 중","Following"})check(ScrollPolicy.knownXFeedLabel(s));
        for(String s:new String[]{null,"추천 게시물","For YouTube","게시물","답글"})check(!ScrollPolicy.knownXFeedLabel(s));
        for(int bits=0;bits<256;bits++) {
            check(ScrollPolicy.xRoute((bits&1)!=0,(bits&2)!=0,(bits&4)!=0?"com.twitter.android":"other",
                    (bits&8)!=0?"android.view.View":"other",(bits&16)!=0,(bits&32)!=0,(bits&64)!=0,(bits&128)==0,
                    1,1,true,true)==(bits==255?ScrollPolicy.XRoute.RESELECT:ScrollPolicy.XRoute.SKIP));
        }
        for(int homes=0;homes<3;homes++)for(int tabs=0;tabs<3;tabs++)for(int bits=0;bits<4;bits++) {
            boolean up=(bits&1)!=0,back=(bits&2)!=0;
            ScrollPolicy.XRoute expected=homes>1||tabs>1?ScrollPolicy.XRoute.SKIP:
                    homes==1&&tabs==1?(up||back?ScrollPolicy.XRoute.RESELECT:ScrollPolicy.XRoute.NO_UP):
                    homes==0&&up?ScrollPolicy.XRoute.PREPARE:ScrollPolicy.XRoute.SKIP;
            check(ScrollPolicy.xRoute(true,true,"com.twitter.android","android.view.View",true,true,true,false,homes,tabs,up,back)==expected);
        }
        for(int bits=0;bits<32;bits++)for(int attempt=-1;attempt<5;attempt++)for(long elapsed:new long[]{-1,0,80,599,600,601}) {
            boolean expected=bits==31&&attempt>=0&&attempt<3&&elapsed>=0&&elapsed<=600;
            check(ScrollPolicy.xMayContinue(8,(bits&1)!=0?8:9,(bits&2)!=0?4:-1,(bits&4)!=0?4:5,
                    (bits&8)!=0,(bits&16)!=0,attempt,elapsed)==expected);
        }
        System.out.println("PolicyTests: "+count+" assertions passed");
    }
}
