package edu.caltech.ipac.frontpage.ui;
/**
 * User: roby
 * Date: 11/1/13
 * Time: 9:01 AM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.frontpage.core.FrontpageUtils;
import edu.caltech.ipac.frontpage.data.DisplayData;

import java.util.Iterator;
import java.util.List;

/**
 * @author Trey Roby
 */
public class DataSetPanel {

    public static final int MAX_PANELS= 7;
    private AbsolutePanel labPanel= new AbsolutePanel();
    private SimplePanel moreLabel= new SimplePanel();
    private Image moreIcon= new Image(GWT.getModuleBaseURL()+"mission_more.png");
    private Image lessIcon= new Image(GWT.getModuleBaseURL()+"mission_less.png");
    private MorePullDown pd;
    private Widget content;
    Iterator<DisplayData> iterator;


    public DataSetPanel(String id, List<DisplayData> dsList) {

        makeUI(id, dsList);

        Timer t= new Timer() {
            @Override
            public void run() {
                buildPopPanel();
            }
        };
        t.schedule(3000);
    }



    private void makeUI(String id, List<DisplayData> dsList) {

        RootPanel root= FFToolEnv.getRootPanel(id);
        HorizontalPanel hp= new HorizontalPanel();
        root.add(hp);
        iterator= dsList.iterator();
        int i=0;

        for(; (i<MAX_PANELS && iterator.hasNext()); i++) {

            DisplayData d= iterator.next();
            String div= makeLink(d);
            HTML entry= new HTML(div);
            hp.add(entry);
        }

        if (i<dsList.size()) {
//            labPanel.add(moreLabel, 10, 25);
            labPanel.add(moreLabel, 0, 0);
            labPanel.addStyleName("mission-icon-size");
            labPanel.addStyleName("mission-icon");
            GwtUtil.setStyle(labPanel, "marginRight", "9px");
            hp.add(labPanel);
//            moreLabel.setStyleName("mission-more-label");
            moreLabel.setWidget(moreIcon);

//            GwtUtil.setStyle(l,"color", "white");
//            GwtUtil.setStyle(l,"fontSize", "24pt");

            pd= new MorePullDown(labPanel, null, new DataSetHighlighLook(), MorePullDown.ShowType.Centered, false);
            pd.setOffset(0,-5);
            pd.getWidget().addStyleName("more-label-pop-border");
        }
    }

    private void buildPopPanel() {
        if (content==null) {
            HorizontalPanel hpPop= new HorizontalPanel();
            for(; (iterator.hasNext());) {

                DisplayData d= iterator.next();
                String div= makeLink(d);
                HTML entry= new HTML(div);
                hpPop.add(entry);
            }
            GwtUtil.setStyles(hpPop, "marginLeft", "auto", "marginRight", "auto");
            content= new SimplePanel(hpPop);
        }
    }

    private void changeToHighlight(boolean on) {
        if (on) {
            moreLabel.setWidget(lessIcon);
            if (pd.getContent()==null) {
                buildPopPanel();
                pd.setContent(content);

            }
        }
        else {
            moreLabel.setWidget(moreIcon);
        }
    }

    private String makeLink(DisplayData d) {
        String image= "<img alt=\""+ d.getTip() +"\" title=\""+ d.getTip()+" \" src=\""+
                FrontpageUtils.componentURL(d.getImage())+ "\">";
        String anchor= "<a href=\""+ FrontpageUtils.refURL(d.getHref()) +"\">" + image + "</a>";
        String div= "<div class=\"mission-icon\">"+anchor+"</div>";
        return div;
    }



    private class DataSetHighlighLook implements MorePullDown.HighlightLook {
        public void enable() {
            changeToHighlight(true);
        }
        public void disable() { changeToHighlight(false); }
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
