package edu.caltech.ipac.frontpage.ui;
/**
 * User: roby
 * Date: 12/2/13
 * Time: 12:28 PM
 */


import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.frontpage.core.FrontpageUtils;
import edu.caltech.ipac.frontpage.data.DataType;
import edu.caltech.ipac.frontpage.data.DisplayData;

import java.util.ArrayList;
import java.util.List;

/**
* @author Trey Roby
*/
class ToolbarDropDownContent {

    private enum TertiaryGridColumns {AUTO, FORCE_ONE, EXTERNAL_OVERRIDE}

    private Grid subGrid= new Grid();
    private Widget activeWidget= null;
    SimplePanel container= new SimplePanel();

    public ToolbarDropDownContent(DisplayData d, ToolbarPanel.ToolBarType tbType) {

        subGrid.addStyleName("front-noborder");
        subGrid.addStyleName("subgird-style");


//        GwtUtil.setStyles(subGrid, "margin", "20px 0 20px 20px");


        SimplePanel subGridWrapper= new SimplePanel(subGrid);
        GwtUtil.setStyles(subGridWrapper, "margin", "20px 0 20px 20px");
        SimplePanel subGridContainer= new SimplePanel(subGridWrapper);
        GwtUtil.setStyles(subGridContainer, "overflow", "hidden",
                          "width", "100%");


        HorizontalPanel zones= new HorizontalPanel();
        FlowPanel left= new FlowPanel();
        zones.add(left);
        subGrid.resize(1, 1);
        zones.add(subGridContainer);

        zones.setWidth("100%");
        zones.setCellWidth(left,"220px");
        subGrid.setWidth("100%");

        Element zonesTD = DOM.getParent(left.getElement());
        zonesTD.setClassName("zonesTdBackground");


        SimplePanel wrapper = new SimplePanel(zones);
        container.setWidget(wrapper);
        HTML title= new HTML(d.getName());
        title.setStyleName("dropDownTitle");


        zones.addStyleName("front-noborder");
        List<DisplayData> menuList= toList(d.getDrop());
        FlowPanel mainVP= new FlowPanel();
        SimplePanel mainDropWrapper= new SimplePanel(mainVP);
        mainDropWrapper.setStyleName("mainDropWrapper");

        mainVP.addStyleName("front-noborder");
        DataType dType;

        for(DisplayData data : menuList) {
            dType= data.getType();
            if (dType==DataType.LINK) {
                mainVP.add(ToolbarPanel.makeItem(data));
            }
            else if (dType==DataType.MENU || dType==DataType.ONLY_ABSTRACT) {
                mainVP.add(makeSecondaryMenuItem(data));
            }

        }

//        subGrid.setWidget(0,0,new HTML(abstractText));


        left.add(title);
        left.add(mainDropWrapper);

        if (tbType!=ToolbarPanel.ToolBarType.MIXED) {
            wrapper.setStyleName("dropDownFixedAndCentered");
        }
        else {
            wrapper.setStyleName("dropDownFixedLeft");
        }

        wrapper.addStyleName("front-noborder");

    }

    private List<DisplayData> toList(JsArray<DisplayData> ddAry) {
        DataType dType;
        List<DisplayData> menuList= new ArrayList<DisplayData>(ddAry.length());
        for(int i=0; (i<ddAry.length()); i++) {
            dType= ddAry.get(i).getType();
            if (dType==DataType.LINK  || dType==DataType.MENU || dType==DataType.ONLY_ABSTRACT) {
                menuList.add(ddAry.get(i));
            }
        }
        return menuList;
    }

    private static HTML makeSmallItem(DisplayData d) {
        String linkStr= "<a title=\""+ d.getTip() +
                "\" class=\"dropDownTableItemSmall\" href=\""+ FrontpageUtils.refURL(d.getHref())+
                "\">"+ d.getName()+"</a>";
        HTML html= new HTML(linkStr);
//        html.setStyleName("dropDownItem");
        return html;
    }

    private static Grid makeTertiaryGrid(DisplayData d, boolean oneColumnHint) {
        JsArray<DisplayData> dd3Ary= d.getDrop();
        int rows;
        int columns;
        TertiaryGridColumns columnMode= getColumnMode(d,oneColumnHint);

        if (columnMode==TertiaryGridColumns.FORCE_ONE) {
            columns= 1;
            rows= dd3Ary.length();
        }
        else {
            columns= (columnMode==TertiaryGridColumns.AUTO) ? computeColumns(dd3Ary.length()) : d.getColumnCount();
            rows= (dd3Ary.length()/columns) + dd3Ary.length()%2;
        }

        boolean orderByColumn= true;

        Grid tertiaryGrid= new Grid(rows, columns);
        int rowIdx;
        int colIdx;
        for(int j=0; (j<dd3Ary.length()); j++) {
            if (dd3Ary.get(j).getType()==DataType.LINK) {
                if (columnMode==TertiaryGridColumns.FORCE_ONE) {
                    tertiaryGrid.setWidget(j, 0, makeSmallItem(dd3Ary.get(j)));
                    tertiaryGrid.getCellFormatter().setVerticalAlignment(j, 0, HasVerticalAlignment.ALIGN_TOP);
                }
                else {
                    if (orderByColumn) {
                        rowIdx= j%rows;
                        colIdx= j/rows;
                    }
                    else {
                        rowIdx= j/columns;
                        colIdx= j%columns;
                    }
                    tertiaryGrid.setWidget(rowIdx,colIdx, makeSmallItem(dd3Ary.get(j)));
                    tertiaryGrid.getCellFormatter().setVerticalAlignment(rowIdx,colIdx, HasVerticalAlignment.ALIGN_TOP);
                }
            }
        }
        tertiaryGrid.addStyleName("front-noborder");
        return tertiaryGrid;
    }

    private static  TertiaryGridColumns getColumnMode(DisplayData d, boolean oneColumnHint) {
        TertiaryGridColumns columnMode;
        int columns=d.getColumnCount();
        if (columns>0)          columnMode= TertiaryGridColumns.EXTERNAL_OVERRIDE;
        else if (oneColumnHint) columnMode= TertiaryGridColumns.FORCE_ONE;
        else                    columnMode= TertiaryGridColumns.AUTO;

        if (columnMode==TertiaryGridColumns.FORCE_ONE && d.getDrop().length()>15) {
            columnMode= TertiaryGridColumns.AUTO;
        }

        if (columnMode==TertiaryGridColumns.EXTERNAL_OVERRIDE && d.getDrop().length()==1) {
            columnMode= TertiaryGridColumns.FORCE_ONE;
        }


        return columnMode;

    }

    private static List<DisplayData> reorganizeSecondary(JsArray <DisplayData> ddAry) {
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

    private static int countMenu(JsArray<DisplayData> ddAry) {
        int retval= 0;
        for(int i=0; (i<ddAry.length()); i++) {
            if (ddAry.get(i).getType()==DataType.MENU) retval++;
        }
        return retval;
    }
    private static int countSimpleLink(JsArray<DisplayData> ddAry) {
        int retval= 0;
        for(int i=0; (i<ddAry.length()); i++) {
            if (ddAry.get(i).getType()==DataType.LINK) retval++;
        }
        return retval;
    }

    Widget getWidget() { return  container; }

    Widget makeSecondaryMenuItem(final DisplayData d) {
        final HTML widget= new HTML("<div class=\"mainSpacingRight\" >"+d.getName()+"</div>");
        widget.setTitle(d.getTip());
        widget.setStyleName("dropDownMainTableItem");
        widget.addStyleName("dropDownTableItem");
        if (d.isPrimary()) select(widget,d);

        widget.addDomHandler(new ClickHandler() {
            public void onClick(ClickEvent ev) { select(widget,d); }
        }, ClickEvent.getType());

        return widget;
    }

    private void select(HTML widget, DisplayData displayData) {
        if (activeWidget!=null) {
            activeWidget.removeStyleName("dropDownMainTableItemActive");
        }
        activeWidget= widget;
        activeWidget.setStyleName("dropDownTableItem");
        activeWidget.addStyleName("dropDownMainTableItemActive");
        activeWidget.addStyleName("dropDownMainTableItem");
        if (displayData.getType()==DataType.MENU) {
            populateSubGrid(displayData.getDrop());
        }
        else if (displayData.getType()==DataType.ONLY_ABSTRACT) {
            populateSubGridAbstract(displayData.getAbstract());
        }

    }


    private void populateSubGridAbstract(String abstractText) {
        subGrid.clear();
        subGrid.resize(1, 1);
        String divText= "<div class=\"abstract-overview\">"+abstractText+"</div>";
//        String divText= "<div style=\"position:relative;\"><div class=\"abstract-overview\">"+abstractText+"</div></div>";
        subGrid.setHTML(0,0,divText);
    }


    private void populateSubGrid(JsArray <DisplayData> ddAry) {
        subGrid.clear();
//        int rows= (ddAry.length()/ SECONDARY_COLS)+1;
//        subGrid.resize(rows, SECONDARY_COLS);
        DataType dType;
        List<DisplayData> ddList= reorganizeSecondary(ddAry);
        int menuCnt= countMenu(ddAry);
        int linkCnt= countSimpleLink(ddAry);

        int gridCols;
        int gridRows;
        boolean firstMenuInOne= false;
        int length= ddAry.length();

        switch (menuCnt) {
            case 0 :
                gridCols= length>5 ? 2 : 1;
                gridRows= gridCols==2 ? length/2 +1 : length;
                break;
            case 1 :
                firstMenuInOne= false;
                gridCols= 2;
                gridRows= Math.max(1, menuCnt);
                break;
            case 2 :
            case 3 :
            case 4 :
                firstMenuInOne= true;
                gridCols= 2;
                gridRows= Math.max(2, menuCnt-1);
                break;
            default:
                firstMenuInOne= true;
                gridCols= 3;
                gridRows= Math.max(2, ((menuCnt - 1) / 2 + 1));
                break;
        }
        subGrid.resize(gridRows, gridCols);



        int row;
        int col= -1;

        if (menuCnt==0) {
            int i= 0;
            boolean under15= true;
            for(DisplayData d : ddList) {
                row= i % gridRows;
                if (row==0) col++;
                subGrid.setWidget(row, col, makeGridItem(d, col > 0));
                subGrid.getCellFormatter().setVerticalAlignment(row,col, HasVerticalAlignment.ALIGN_TOP);
                i++;
                if (d.getName().length()>15) under15= false;
            }

            if (under15) subGrid.setWidth("350px");
            else subGrid.setWidth("100%");

        }
        else {
            subGrid.setWidth("100%");
            int i= 0;
            if (linkCnt>0) {
                VerticalPanel vp= new VerticalPanel();
                vp.addStyleName("front-noborder");
                for(DisplayData d : ddList) {
                    if (d.getType()==DataType.LINK) {
                        vp.add(makeGridItem(d,false));
                    }
                    else {
                        break;
                    }
                }
                subGrid.setWidget(0,0,  vp);
                i= 1;
                col= 0;
            }
            List<Widget> tGridList= new ArrayList<Widget>(4);
            Widget tGrid;
            for(DisplayData d : ddList) {
                if (d.getType()==DataType.MENU) {
                    row= i % gridRows;
                    if (row==0) col++;

                    Widget h= new HTML(d.getName());
                    h.setStyleName("dropDownTableItemGridTitle");
                    if (col>0) h.addStyleName("dropDownTableItemGridMultiCol");
                    VerticalPanel vp3= new VerticalPanel();
                    vp3.addStyleName("front-noborder");
                    vp3.add(h);
                    tGrid= makeTertiaryGrid(d, menuCnt==1);
                    SimplePanel tGridbackgroundWrapper= new SimplePanel(tGrid);
                    SimplePanel tGridInsetWrapper= new SimplePanel(tGridbackgroundWrapper);
                    tGridbackgroundWrapper.setStyleName("tertiaryGrid");

                    tGrid.setWidth("340px");
                    GwtUtil.setStyle(tGrid, "marginLeft", "6px");
                    vp3.add(tGridInsetWrapper);
                    tGridList.add(tGrid);
                    subGrid.setWidget(row,col,  vp3);
                    subGrid.getCellFormatter().setVerticalAlignment(row,col, HasVerticalAlignment.ALIGN_TOP);
                    i++;
                }
            }

            if (menuCnt==1) tGridList.get(0).setWidth("410px");

        }


    }

    public static HTML makeGridItem(DisplayData d, boolean additionalCol) {
        String linkStr= "<a title=\""+ d.getTip() +
                "\" class=\"dropDownTableItemGrid \" href=\""+ FrontpageUtils.refURL(d.getHref())+
                "\">"+ d.getName()+"</a>";
        HTML html= new HTML(linkStr);
        html.setStyleName("dropDownItem");
        html.addStyleName("dropDownRightLinkTableItem");
        if (additionalCol) html.addStyleName("dropDownTableItemGridMultiCol");
        return html;
    }


    private static int computeColumns(int length) {
        return length>4 ? 2 : 1;


//        int col;
//        if (length<2) col= 2;
//        else if (length<4) col= 2;
//        else if (length<6) col= 2;
//        else if (length<7) col= 3;
//        else col= 4;
//        return col;

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
