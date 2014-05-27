package edu.caltech.ipac.frontpage.ui;
/**
 * User: roby
 * Date: 11/5/13
 * Time: 10:03 AM
 */


import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.frontpage.core.FrontpageUtils;
import edu.caltech.ipac.frontpage.data.DisplayData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class FeaturePager {

    private enum Dir {NEXT, PREV}
    private final static int MOVE_INTERVAL= 10 * 1000;  // one minute
    private static final IconCreator _ic = IconCreator.Creator.getInstance();
//    private Image slidePrev= new Image(GWT.getModuleBaseURL()+"slider_prev.png");
//    private Image slideNext= new Image(GWT.getModuleBaseURL()+"slider_next.png");
    LayoutPanel layoutPanel= new LayoutPanel();
    private AbsolutePanel displayArea= new AbsolutePanel();
    private SimplePanel navBar= new SimplePanel();
//    private List<Widget> itemList = new ArrayList<Widget>(10);
    private Widget lastMovedOut= null;
    private MoveTimer moveTimer= new MoveTimer();
    private Grid currentDisplayDots = new Grid(1,1);
    private int activeIdx= 0;
    private boolean transitionActive= false;
    private final String id;
    private final JsArray<DisplayData> dataAry;
    private Widget currPage= null;

    public FeaturePager(String id, JsArray<DisplayData> dataAry) {
        this.id= id;
        this.dataAry= dataAry;
        makeUI();
    }



    private void makeUI() {

        RootPanel root= FFToolEnv.getRootPanel(id);


        root.setStyleName("featureMain");
        displayArea.addStyleName("featureDisplay");
        navBar.addStyleName("featureNavBar");

        root.add(layoutPanel);

        layoutPanel.add(displayArea);
        layoutPanel.add(navBar);

        layoutPanel.setWidgetBottomHeight(navBar, 20, Style.Unit.PX, 28, Style.Unit.PX );
        layoutPanel.setWidgetLeftRight(navBar, 22, Style.Unit.PX, 21, Style.Unit.PX);



        layoutPanel.setWidgetTopBottom(displayArea, 0, Style.Unit.PX, 15, Style.Unit.PX );
        layoutPanel.setWidgetLeftRight(displayArea, 17, Style.Unit.PX, 16, Style.Unit.PX);


        layoutPanel.setSize("100%", "100%");


        activeIdx= findPrimary();
        int randomIdx= (int)(Math.random()*1000) % dataAry.length();
        if (activeIdx==-1) { // if there is not primary
            activeIdx= randomIdx;
        }
        else {
            if ((int)(Math.random()*100) >75) { // if there is a primary use 75% of the time
                activeIdx= randomIdx;
            }
        }

//        displayArea.add(itemList.get(activeIdx),0,0);
        currPage=makeItem(dataAry.get(activeIdx));
        displayArea.add(currPage,0,0);


        populateNavBar();
        moveTimer.reset();

        Timer t= new Timer() {
            @Override
            public void run() {
                cacheItems(dataAry);
            }
        };
        t.schedule(5000);
    }

    private void populateNavBar() {

        String dotContStyle= " style= \"width:100%; margin-top:5px;\"";
        String newsURL= FrontpageUtils.refURL("news.html");
        String featured= FrontpageUtils.refURL("featured_images.html");
        String navBarStr=
                "<div id=\"dotDisplay\""+dotContStyle+"></div>" +
                "<div style=\"margin-top:-5px\">"+
                "<a href=\""+newsURL+"\""+" class=\"featurePastNews\""+">Past News</a>" +
                "<a href=\""+featured+"\""+" class=\"featureFeaturedImages\""+">Featured Images</a>" +
                "</div>";


        HTMLPanel navBarInternals= new HTMLPanel(navBarStr);

        navBar.setWidget(navBarInternals);
//        navBar.setWidget(currentDisplayDots);

        navBarInternals.add(currentDisplayDots,"dotDisplay");
        GwtUtil.setStyles(currentDisplayDots, "marginLeft", "auto", "marginRight", "auto", "marginTop", "-7px");


        navBarInternals.setStyleName("featureNavControl");
        currentDisplayDots.setCellPadding(2);
        currentDisplayDots.resize(1, dataAry.length());
        updateNavBar();


    }

    private int findPrimary() {
        int retval= -1;
        for(int i=0; (i<dataAry.length()); i++) {
            if (dataAry.get(i).isPrimary()) {
                retval= i;
                break;
            }
        }
        return retval;

    }

    private void updateNavBar() {
        for(int i= 0; (i<dataAry.length()); i++) {
            if (i==activeIdx) {
                currentDisplayDots.setWidget(0,i, new Image(_ic.getGreenDot()));
            }
            else {
                Image im= new Image(_ic.getBlueDot());
                im.addClickHandler(new DotClick(i));
                currentDisplayDots.setWidget(0, i, im);
            }
        }
    }



    private void cacheItems(JsArray<DisplayData> dataAry) {

        List<Widget> itemList=new ArrayList<Widget>(30);

        for(int i= 0; (i<dataAry.length()); i++) {
                Widget w= makeItem(dataAry.get(i));
                itemList.add(w);
        }

    }


    private Widget makeItem(DisplayData d) {

        LayoutPanel panel= new LayoutPanel();
        panel.setSize("100%", "100%");
        HTML im= makeImageLink(d.getImage(),d.getTip(),d.getHref());
        HTML title= makeTitleLink(d.getName(), d.getTip(), d.getHref());
        HTML ab= makeAbstractLink(d.getAbstractStart(), d.getAbstract(), d.getTip(), d.getHref());

        panel.add(im);
        panel.add(title);
        panel.add(ab);

        panel.setWidgetTopBottom(im, 0, Style.Unit.PX, 0, Style.Unit.PX);
        panel.setWidgetLeftRight(im, 0, Style.Unit.PX, 0, Style.Unit.PX);


        panel.setWidgetTopHeight(title,0,Style.Unit.PX, 43, Style.Unit.PX );
        panel.setWidgetLeftRight(title, 0, Style.Unit.PX, 0, Style.Unit.PX);

        panel.setWidgetBottomHeight(ab, 30, Style.Unit.PX, 90, Style.Unit.PX);
        panel.setWidgetLeftRight(ab, 0, Style.Unit.PX, 0, Style.Unit.PX);


        return panel;
    }

    private HTML makeImageLink(String image, String tip, String url) {
        url= FrontpageUtils.refURL(url);
        String img= "<img class=\"featureImage\" alt=\""+ tip +"\" title=\""+ tip+" \" src=\""+
                FrontpageUtils.componentURL(image)+ "\">";
        String anchor= "<a href=\""+ url +"\">" + img + "</a>";
        return new HTML(anchor);
    }

    private HTML makeTitleLink(String title, String tip, String url) {
        url= FrontpageUtils.refURL(url);
        String anchor= "<a title=\""+ tip+" \" href=\""+ url +"\">" + title+ "</a>";
        HTML html= new HTML(anchor);
        html.setStyleName("featureTitle");
        return html;
    }

    private HTML makeAbstractLink(String abStart, String abBody, String tip, String url) {
        url= FrontpageUtils.refURL(url);
        String abTotal= "<span class= abStart>"+ abStart+ "</span>" +
                        "<span class= abBody>"+ "&nbsp;&nbsp;" +
                abBody  + "</span>";
        String anchor= "<a title=\""+ tip+" \" href=\""+ url +"\">" + abTotal + "</a>";
        HTML html= new HTML(anchor);
        html.setStyleName("featureAbstract");
        return html;
    }

    private void movePage(Dir dir) {
        movePage(dir,-1);
    }

    private void movePage(int idx) {
        movePage(Dir.PREV,idx);
    }

    private void movePage(Dir dir, int targetIdx) {
        if (transitionActive) return;
        transitionActive= true;
        final int newIdx;
        final int offset;

        if (targetIdx<0) {
            if (dir==Dir.NEXT)  {
                newIdx= (activeIdx+1==dataAry.length()) ? 0 : activeIdx+1;
                offset= 500;
            }
            else {
                newIdx= (activeIdx-1==-1) ? dataAry.length()-1 : activeIdx-1;
                offset= -500;
            }
        }
        else {
            newIdx= targetIdx;
            offset=  newIdx<activeIdx ? -500 : 500;
        }




        final Widget newPage= makeItem(dataAry.get(newIdx));

        if (lastMovedOut!=null) {
            lastMovedOut.removeStyleName("featureMoveTransition");
            displayArea.remove(lastMovedOut);
        }

        displayArea.add(newPage,offset,0);

        newPage.setStyleName("featureMoveTransition");
        currPage.setStyleName("featureMoveTransition");



        Timer t= new Timer() {
            @Override
            public void run() {
                displayArea.setWidgetPosition(newPage,0,0);
                displayArea.setWidgetPosition(currPage, -1*offset, 0);
                lastMovedOut= currPage;
                activeIdx= newIdx;
                moveTimer.reset();
                updateNavBar();
                transitionActive= false;
                currPage= newPage;
            }
        };
        t.schedule(100);
    }

    private class MoveTimer extends Timer {
        @Override
        public void run() { movePage(Dir.NEXT);  }

        public void reset() {
            this.cancel();
            this.schedule(MOVE_INTERVAL);
        }
    }



    private class DotClick implements ClickHandler {
        private final int idx;
        public DotClick(int idx) { this.idx= idx;}
        public void onClick(ClickEvent event) { movePage(idx); }
    }

}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
