package edu.caltech.ipac.frontpage.ui;
/**
 * User: roby
 * Date: 11/1/13
 * Time: 9:01 AM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
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

    public enum ToolBarType { FRONT_PAGE, LARGE, SMALL}
    private final int COL= 4;
    private static final String miniIrsaIcon= "mini-irsa.png";
    private FlowPanel panel= new FlowPanel();
    private final ToolBarType tbType;


    public ToolbarPanel(String id, JsArray<DisplayData> dataAry, ToolBarType tbType) {
        this.tbType= tbType;
//        GwtUtil.getClientLogger().log(Level.INFO, "tbType= "+tbType);
        if (tbType==ToolBarType.LARGE) {
            makeLarge(id,dataAry);

        }
        else {
            makeMiniAndApp(id, dataAry);
        }
    }



    private void makeLarge(String id, JsArray<DisplayData> dataAry) {

        RootPanel root= FFToolEnv.getRootPanel(id);

        String logoStyle= "irsa_logo-midsize";


        HTML topStrip= new HTML();
        topStrip.setStyleName("large-top-strip");



        HTML bigHTitle= new HTML(
                "            <div class=\"title-text\">\n" +
                "                           <a href=\"http://irsa.ipac.caltech.edu/\">\n" +
                "                            <div class=\""+logoStyle+"\">\n" +
                "                                 &nbsp;\n" +
                "                            </div>\n" +
                "                            <div class=\"big_cap uppercase \">\n" +
                "                                NASA/IPAC\n" +
                "                            </div>\n" +
                "                            <div class=\"big_cap \">\n" +
                "                                Infrared\n" +
                "                            </div>\n" +
                "                            <div class=\"big_cap \">\n" +
                "                                Science\n" +
                "                            </div>\n" +
                "                            <div class=\"big_cap \">\n" +
                "                                Archive\n" +
                "                            </div>\n" +
                "                        </a>" +
                        "<div>\n");
        bigHTitle.setStyleName("large-mission_title");
        bigHTitle.addStyleName("large-mission-title-font");
        bigHTitle.addStyleName("large-toolbar-center-layout");

        FlowPanel titleLine= new FlowPanel();
        titleLine.add(topStrip);
        titleLine.add(bigHTitle);
        titleLine.add(panel);

        panel.addStyleName("large-bar");
        HorizontalPanel hp= new HorizontalPanel();
        FlowPanel toolbarContainer= new FlowPanel();
        toolbarContainer.add(hp);
        toolbarContainer.addStyleName("large-toolbar-center-layout");
        panel.add(toolbarContainer);

        root.add(titleLine);

//        FlowPanel tmp= new FlowPanel();
//        titleLine.add(tmp);
//        GwtUtil.setStyles(tmp, "width", "180px", "display", "inline-block");




        FlowPanel entries= new FlowPanel();
        insertTopToolbar(entries, dataAry);

        hp.addStyleName("front-noborder");
        hp.addStyleName("large-menu-offset");
        hp.add(entries);


        LoginManager lm= Application.getInstance().getLoginManager();
        if (lm!=null) {
            toolbarContainer.add(lm.getToolbar());
//            GwtUtil.setStyle(lm.getToolbar(), "display", "inline-block");
            lm.getToolbar().addStyleName("frontpage-large-LoginBar");
            lm.getToolbar().addStyleName("loginBarFontStuff");
            lm.refreshUserInfo();
        }

        root.setStyleName("large-bar-root");
        setStyle(hp, "largeToolBarMenuWrapper", "appToolBarMenuWrapper");
        hp.addStyleName("front-noborder");
            GwtUtil.setStyle(panel, "position", "relative");
        panel.setStyleName("largeToolBarMenu");
        panel.addStyleName("large-bar");
    }


    private void insertTopToolbar(FlowPanel entries, JsArray<DisplayData> dataAry) {
        DataType dType;
        List<DisplayData> menuList= new ArrayList<DisplayData>(dataAry.length());
        String abstractText= "";

        for(int i=0; (i<dataAry.length()); i++) {
            dType= dataAry.get(i).getType();
            if (dType==DataType.LINK  || dType==DataType.MENU) {
                menuList.add(dataAry.get(i));
            }
            else if (dType==DataType.ONLY_ABSTRACT) {
                abstractText= dataAry.get(i).getAbstract();
            }
        }


        int i= 0;
        for(DisplayData d : menuList) {
            if (d.getType()== DataType.LINK) {
                entries.add(makeBarLink(d));
            }
            else if (d.getType()==DataType.MENU) {
                entries.add(makeBarMenu(d, abstractText));
            }

            if (i<menuList.size()-1)  entries.add(makeSeparator());
            i++;
        }
    }

    private void makeMiniAndApp(String id, JsArray<DisplayData> dataAry) {

        RootPanel root= FFToolEnv.getRootPanel(id);
        setStyle(root, "largeToolBar", "appToolBar");
        HorizontalPanel hp= new HorizontalPanel();
        hp.addStyleName("front-noborder");
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
            insertTopToolbar(entries,dataAry);
            entriesWrapper.add(entries);
            GwtUtil.setStyle(entries, "display", "inline-block");
            hp.add(entriesWrapper);
        }
        else {
            FlowPanel tmp= new FlowPanel();
            entries.add(tmp);
            insertTopToolbar(entries,dataAry);
            GwtUtil.setStyles(tmp, "width", "180px", "display", "inline-block");
        }

        root.add(panel);
        panel.add(hp);

        //TODO: need to work on the layout, should float on the right for resize
        LoginManager lm= Application.getInstance().getLoginManager();
        if (lm!=null) {
            panel.add(lm.getToolbar());
            lm.refreshUserInfo();
            addStyle(lm.getToolbar(),"frontpageLoginBar", "frontpageAppLoginBar" );
        }

        setStyle(hp, "largeToolBarMenuWrapper", "appToolBarMenuWrapper");
        hp.addStyleName("front-noborder");
        if (tbType==ToolBarType.FRONT_PAGE) {
            GwtUtil.setStyle(panel, "position", "absolute");
            panel.setStyleName("largeToolBarMenu");
        }
        else {
            GwtUtil.setStyles(panel, "position", "absolute",
                              "width", "100%");
            root.addStyleName("appToolBarRoot");
        }
    }




    private String makeMiniIconLink(String url, String iconURL, String tip) {
        String image= "<img alt=\""+ tip +"\" title=\""+ tip+" \" src=\""+ GWT.getModuleBaseURL()+ iconURL+ "\">";
        String anchor= "<a href=\""+ FrontpageUtils.refURL(url) +"\">" + image + "</a>";
        return anchor;
    }

    private String makeSubMissionLink(String url, String iconURL, String tip) {
        String image= "<img class=\"appSubIcon\" alt=\""+ tip +"\" title=\""+
                tip+" \" src=\""+FrontpageUtils.componentURL(iconURL)+ "\">";
        String anchor= "<a href=\""+ FrontpageUtils.refURL(url) +"\">" + image + "</a>";
        return anchor;
    }


    private void setStyle(Widget w, String largeStyle, String appStyle)  {
        switch (tbType) {
            case FRONT_PAGE:
                w.setStyleName(largeStyle);
                break;
            case SMALL:
                w.setStyleName(appStyle);
                break;
        }

    }

    private void addStyle(Widget w, String largeStyle, String appStyle)  {
        switch (tbType) {
            case FRONT_PAGE:
                w.addStyleName(largeStyle);
                break;
            case SMALL:
                w.addStyleName(appStyle);
                break;
        }

    }



    private HTML makeBarLink(DisplayData d) {
        String secondStyle= tbType==ToolBarType.LARGE ?
                            "toolbarText-border-large" : "toolbarText-border-main";
        String linkStr= "<a title=\""+ d.getTip() +
                         "\" class=\"toolbarText\" href=\""+ FrontpageUtils.refURL(d.getHref())+
                         "\">"+ d.getName()+"</a>";
        HTML html= new HTML(linkStr);
        html.addStyleName("toolbarElement");
        html.addStyleName(secondStyle);
        return html;
    }

    private HTML makeBarMenu(final DisplayData d, String abstractText) {
        final HTML html= new HTML(d.getName());
        String secondStyle= tbType==ToolBarType.LARGE ?
                            "toolbarText-border-large" : "toolbarText-border-main";
        html.setStyleName("toolbarElement");
        html.addStyleName("toolbarText");
        html.addStyleName(secondStyle);
//        Widget content= makeDropDownContent(d);
        Widget content;
        if (isOnlyLinks(d)) {
             content= makeSimpleDropDownContent(d);
        }
        else {
            DropDownContent ddCont= new DropDownContent(d, abstractText);
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
        vp.addStyleName("front-noborder");
        HTML title= new HTML(d.getName());
        title.setStyleName("dropDownTitle");

        vp.add(title);


        JsArray<DisplayData> ddAry= d.getDrop();

        int rows= ddAry.length()/COL;
        if (ddAry.length() % COL > 0) rows++;

        Grid grid= new Grid(rows,4);
        grid.addStyleName("front-noborder");
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


    public static HTML makeItem(DisplayData d) {
        String linkStr= "<a title=\""+ d.getTip() +
                "\" class=\"dropDownTableItem \" href=\""+ FrontpageUtils.refURL(d.getHref())+
                "\">"+ d.getName()+"</a>";
        HTML html= new HTML(linkStr);
        html.setStyleName("dropDownItem");
        html.addStyleName("dropDownMainTableItem");
        return html;
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
