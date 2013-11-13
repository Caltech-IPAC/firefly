package edu.caltech.ipac.frontpage.ui;
/**
 * User: roby
 * Date: 11/1/13
 * Time: 9:01 AM
 */


import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.frontpage.core.FrontpageUtils;
import edu.caltech.ipac.frontpage.data.DataType;
import edu.caltech.ipac.frontpage.data.DisplayData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class ToolbarPanel {

    public enum ToolBarType { LARGE, SMALL}
    private final int COL= 4;
    private final int SECONDARY_COLS= 2;
    private static final String miniIrsaIcon= "mini-irsa.png";
    private FlowPanel panel= new FlowPanel();
    private final ToolBarType tbType;


    public ToolbarPanel(String id, JsArray<DisplayData> dataAry, ToolBarType tbType) {
        this.tbType= tbType;
        makeUI(id, dataAry);
    }



    private void makeUI(String id, JsArray<DisplayData> dataAry) {

        RootPanel root= FFToolEnv.getRootPanel(id);
        setStyle(root, "largeToolBar", "appToolBar");
        HorizontalPanel hp= new HorizontalPanel();
        FlowPanel entries= new FlowPanel();
        hp.add(entries);



        if (tbType==ToolBarType.SMALL) {
            HTML mi= new HTML(makeMiniIconLink("", miniIrsaIcon, "Irsa Home Page"));
            mi.setStyleName("irsaIconElement");
            hp.add(mi);

            if (FrontpageUtils.getSubIcon()!=null) {
                String subURL= FrontpageUtils.getSubIconURL();
                if (subURL==null) subURL= "/";
                HTML subLink= new HTML(makeSubMissionLink(subURL, FrontpageUtils.getSubIcon(), "Irsa Home Page"));
                subLink.addStyleName("appSubMission");
                hp.add(subLink);
            }



            FlowPanel entriesWrapper= new FlowPanel();
            entriesWrapper.setStyleName("appBarEntriesWrapper");
            entries= new FlowPanel();
            entriesWrapper.add(entries);
            GwtUtil.setStyle(entries, "display", "inline-block");
            hp.add(entriesWrapper);
        }


        for(int i= 0; (i<dataAry.length()); i++) {
            DisplayData d= dataAry.get(i);
            if (d.getType()== DataType.LINK) {
                entries.add(makeLink(d));
            }
            else if (d.getType()==DataType.MENU) {
                entries.add(makeMenu(d));
            }

            if (i<dataAry.length()-1) {
                entries.add(makeSeparator());

            }
        }

        root.add(panel);
        panel.add(hp);

        //TODO: need to work on the layout, should float on the right for resize
        LoginManager lm= Application.getInstance().getLoginManager();
        if (lm!=null) {
//            hp.add(lm.getToolbar());
            panel.add(lm.getToolbar());
            lm.refreshUserInfo();
            lm.getToolbar().addStyleName("frontpageLoginBar");
        }

        GwtUtil.setStyle(panel, "position", "relative");
        setStyle(hp, "largeToolBarMenuWrapper", "appToolBarMenuWrapper");
        if (tbType==ToolBarType.LARGE) panel.setStyleName("largeToolBarMenu");
        else root.addStyleName("appToolBarRoot");
    }



    private String makeMiniIconLink(String url, String iconURL, String tip) {
        String image= "<img alt=\""+ tip +"\" title=\""+ tip+" \" src=\""+componentURL(iconURL)+ "\">";
        String anchor= "<a href=\""+ refURL(url) +"\">" + image + "</a>";
        return anchor;
    }

    private String makeSubMissionLink(String url, String iconURL, String tip) {
        String image= "<img class=\"appSubIcon\" alt=\""+ tip +"\" title=\""+ tip+" \" src=\""+componentURL(iconURL)+ "\">";
        String anchor= "<a href=\""+ refURL(url) +"\">" + image + "</a>";
        return anchor;
    }


    private void setStyle(Widget w, String largeStyle, String appStyle)  {
        switch (tbType) {
            case LARGE:
                w.setStyleName(largeStyle);
                break;
            case SMALL:
                w.setStyleName(appStyle);
                break;
        }

    }




    private HTML makeLink(DisplayData d) {
        String linkStr= "<a title=\""+ d.getTip() +
                         "\" class=\"toolbarText\" href=\""+ FFToolEnv.modifyURLToFull(d.getHref())+
                         "\">"+ d.getName()+"</a>";
        HTML html= new HTML(linkStr);
        html.setStyleName("toolbarElement");
        return html;
    }

    private HTML makeMenu(final DisplayData d) {
        final HTML html= new HTML(d.getName());
        html.setStyleName("toolbarElement");
        html.addStyleName("toolbarText");
//        Widget content= makeDropDownContent(d);
        Widget content;
        if (isOnlyLinks(d)) {
             content= makeSimpleDropDownContent(d);
        }
        else {
            DropDownContent ddCont= new DropDownContent(d);
            content= ddCont.getWidget();
        }
        MorePullDown pd= new MorePullDown(html,content, new DataSetHighlightLook(html));
        pd.setOffset(0,tbType==ToolBarType.LARGE? 0 : -1);

        return html;
    }

    private HTML makeSeparator() {
        HTML html= new HTML("|");
        html.setStyleName("toolbarElement");
        html.addStyleName("toolbarSeparator");
        return html;
    }


    private Widget makeSimpleDropDownContent(DisplayData d) {
        VerticalPanel vp= new VerticalPanel();
        HTML title= new HTML(d.getName());
        title.setStyleName("dropDownTitle");

        vp.add(title);


        JsArray<DisplayData> ddAry= d.getDrop();

        int rows= ddAry.length()/COL;
        if (ddAry.length() % COL > 0) rows++;

        Grid grid= new Grid(rows,4);
        grid.setCellSpacing(10);

        for(int i=0; (i<ddAry.length()); i++) {
            if (ddAry.get(i).getHref()!=null) {
//                HTML w= new HTML(ddAry.get(i).getName());
                HTML w= makeItem(ddAry.get(i));
                grid.setWidget(i/COL, i % COL, w);
            }
        }
        vp.add(grid);
        return vp;
    }

    private boolean isOnlyLinks(DisplayData d) {
        boolean retval= false;
        JsArray<DisplayData> ddAry= d.getDrop();
        for(int i=0; (i<ddAry.length()); i++) {
            if (ddAry.get(i).getType()!=DataType.LINK) {
                retval= false;
                break;
            }
        }
        return retval;
    }


    private HTML makeItem(DisplayData d) {
        String linkStr= "<a title=\""+ d.getTip() +
                "\" class=\"dropDownTableItem\" href=\""+ FFToolEnv.modifyURLToFull(d.getHref())+
                "\">"+ d.getName()+"</a>";
        HTML html= new HTML(linkStr);
        html.setStyleName("dropDownItem");
        return html;
    }

    private HTML makeSmallItem(DisplayData d) {
        String linkStr= "<a title=\""+ d.getTip() +
                "\" class=\"dropDownTableItemSmall\" href=\""+ FFToolEnv.modifyURLToFull(d.getHref())+
                "\">"+ d.getName()+"</a>";
        HTML html= new HTML(linkStr);
//        html.setStyleName("dropDownItem");
        return html;
    }


    private class DropDownContent {

        private Grid subGrid= new Grid();
        private VerticalPanel vp= new VerticalPanel();
        SimplePanel container= new SimplePanel(vp);
        private Widget activeWidget= null;

        public DropDownContent(DisplayData d) {

            HTML title= new HTML(d.getName());
            title.setStyleName("dropDownTitle");


            HorizontalPanel zones= new HorizontalPanel();
            JsArray<DisplayData> ddAry= d.getDrop();
            VerticalPanel mainVP= new VerticalPanel();
            DataType dType;
            for(int i=0; (i<ddAry.length()); i++) {
                dType= ddAry.get(i).getType();
                if (dType==DataType.LINK) {
                    mainVP.add(makeItem(ddAry.get(i)));
                }
                else if (dType==DataType.MENU) {
                    mainVP.add(makeSecondaryMenuItem(ddAry.get(i)));
                }

            }
            zones.add(mainVP);
            zones.add(subGrid);

            vp.add(title);
            vp.add(zones);

            vp.setStyleName("dropDownContainer");
        }

        Widget getWidget() { return  container; }

        Widget makeSecondaryMenuItem(DisplayData d) {
            final JsArray<DisplayData> ddAry= d.getDrop();
            final HTML widget= new HTML(d.getName());
            widget.setTitle(d.getTip());
            widget.setStyleName("dropDownTableItem");

            widget.addDomHandler(new ClickHandler() {
                public void onClick(ClickEvent ev) {
                    if (activeWidget!=null) activeWidget.removeStyleName("dropDownActiveColor");
                    activeWidget= widget;
                    activeWidget.addStyleName("dropDownActiveColor");
                    populateSubGrid(ddAry);
                }
            }, ClickEvent.getType());

            return widget;
        }

        private void populateSubGrid(JsArray<DisplayData> ddAry) {
            subGrid.clear();
            int rows= (ddAry.length()/SECONDARY_COLS)+1;
            subGrid.resize(rows, SECONDARY_COLS);
            DataType dType;
            int secRow=-1;
            List<DisplayData> ddList= reorganizeSecondary(ddAry);
            int i= 0;
            for(DisplayData d : ddList) {
                dType= d.getType();
                if (i % SECONDARY_COLS==0) secRow++;
                if (dType==DataType.MENU) {
                    Widget h= new HTML(ddAry.get(i).getName());
                    h.setStyleName("dropDownTableItem");
                    VerticalPanel vp3= new VerticalPanel();
                    vp3.add(h);
                    vp3.add(makeTertiaryGrid(d));
                    subGrid.setWidget(secRow, i % SECONDARY_COLS, vp3);
                }
                else if (dType==DataType.LINK) {
                    subGrid.setWidget(secRow, i % SECONDARY_COLS, makeItem(ddAry.get(i)));
                }
                subGrid.getCellFormatter().setVerticalAlignment(secRow, i % SECONDARY_COLS, HasVerticalAlignment.ALIGN_TOP);
                i++;
            }
        }
    }

    private Grid makeTertiaryGrid(DisplayData d) {
        JsArray<DisplayData> dd3Ary= d.getDrop();
//        int g3Col= dd3Ary.length()<4 ? dd3Ary.length() : 4;
        int g3Col= computeColumns(dd3Ary.length());
        Grid tertiaryGrid= new Grid((dd3Ary.length()/g3Col) + 1, g3Col);
        tertiaryGrid.setStyleName("tertiaryGrid");
        for(int j=0; (j<dd3Ary.length()); j++) {
            if (dd3Ary.get(j).getType()==DataType.LINK) {
                tertiaryGrid.setWidget(j/g3Col, j%g3Col, makeSmallItem(dd3Ary.get(j)));
                tertiaryGrid.getCellFormatter().setVerticalAlignment(j/g3Col, j%g3Col, HasVerticalAlignment.ALIGN_TOP);
            }
        }
        tertiaryGrid.setCellSpacing(5);

        tertiaryGrid.setStyleName("tertiaryGrid");
        return tertiaryGrid;
    }

    private int computeColumns(int length) {
        int col;
        if (length<2) col= 2;
        else if (length<4) col= 2;
        else if (length<6) col= 2;
        else if (length<7) col= 3;
        else col= 4;
        return col;
    }

    private List<DisplayData> reorganizeSecondary(JsArray<DisplayData> ddAry) {
        List<DisplayData> list= new ArrayList<DisplayData>(ddAry.length());
        for(int i=0; (i<ddAry.length()); i++) {
            if (ddAry.get(i).getType()==DataType.LINK) {
                list.add(ddAry.get(i));
            }
        }
        for(int i=0; (i<ddAry.length()); i++) {
            if (ddAry.get(i).getType()==DataType.MENU) {
                list.add(ddAry.get(i));
            }
        }
        return list;
    }

    private class DataSetHighlightLook implements MorePullDown.HighlightLook {

        private final Widget dropWidget;

        private DataSetHighlightLook(Widget dropWidget) {
            this.dropWidget= dropWidget;
        }

        public void enable() {
            dropWidget.addStyleName("toolbarTextSelected");
        }
        public void disable() {
            dropWidget.removeStyleName("toolbarTextSelected");
        }
    }


    private static String componentURL(String url) {
        String cRoot= FrontpageUtils.getComponentsRoot();
        return FFToolEnv.modifyURLToFull(url,cRoot,"/");
    }
    private static String refURL(String url) {
        return FFToolEnv.modifyURLToFullAlways(url);
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
