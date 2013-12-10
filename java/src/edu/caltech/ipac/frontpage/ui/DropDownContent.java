package edu.caltech.ipac.frontpage.ui;
/**
 * User: roby
 * Date: 12/2/13
 * Time: 12:28 PM
 */


import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.frontpage.data.DataType;
import edu.caltech.ipac.frontpage.data.DisplayData;

import java.util.List;

/**
* @author Trey Roby
*/
class DropDownContent {

    private Grid subGrid= new Grid();
    private VerticalPanel vp= new VerticalPanel();
    SimplePanel container= new SimplePanel(vp);
    private Widget activeWidget= null;

    public DropDownContent(DisplayData d) {

        HTML title= new HTML(d.getName());
        title.setStyleName("dropDownTitle");


        HorizontalPanel zones= new HorizontalPanel();
        zones.addStyleName("front-noborder");
        JsArray<DisplayData> ddAry= d.getDrop();
//            VerticalPanel mainVP= new VerticalPanel();
        FlowPanel mainVP= new FlowPanel();
//            mainVP.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
        SimplePanel mainDropWrapper= new SimplePanel(mainVP);
        mainDropWrapper.setStyleName("mainDropWrapper");

        mainVP.addStyleName("front-noborder");
        DataType dType;
        for(int i=0; (i<ddAry.length()); i++) {
            dType= ddAry.get(i).getType();
            if (dType==DataType.LINK) {
                mainVP.add(ToolbarPanel.makeItem(ddAry.get(i)));
            }
            else if (dType==DataType.MENU) {
                mainVP.add(makeSecondaryMenuItem(ddAry.get(i)));
            }

        }

        zones.add(mainDropWrapper);
        zones.add(subGrid);

        vp.add(title);
        vp.add(zones);

        vp.setStyleName("dropDownContainer");
        vp.addStyleName("front-noborder");
        subGrid.addStyleName("front-noborder");
    }

    Widget getWidget() { return  container; }

    Widget makeSecondaryMenuItem(DisplayData d) {
        final JsArray<DisplayData> ddAry= d.getDrop();
        final HTML widget= new HTML(d.getName());
        widget.setTitle(d.getTip());
        widget.setStyleName("dropDownTableItem");
        widget.addStyleName("dropDownMainTableItem");

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
        int rows= (ddAry.length()/ ToolbarPanel.SECONDARY_COLS)+1;
        subGrid.resize(rows, ToolbarPanel.SECONDARY_COLS);
        DataType dType;
        int secRow=-1;
        List<DisplayData> ddList= ToolbarPanel.reorganizeSecondary(ddAry);
        int i= 0;
        for(DisplayData d : ddList) {
            dType= d.getType();
            if (i % ToolbarPanel.SECONDARY_COLS==0) secRow++;
            if (dType==DataType.MENU) {
                Widget h= new HTML(ddAry.get(i).getName());
                h.setStyleName("dropDownTableItem");
                VerticalPanel vp3= new VerticalPanel();
                vp3.addStyleName("front-noborder");
                vp3.add(h);
                vp3.add(ToolbarPanel.makeTertiaryGrid(d));
                subGrid.setWidget(secRow, i % ToolbarPanel.SECONDARY_COLS, vp3);
            }
            else if (dType==DataType.LINK) {
                subGrid.setWidget(secRow, i % ToolbarPanel.SECONDARY_COLS, ToolbarPanel.makeItem(ddAry.get(i)));
            }
            subGrid.getCellFormatter().setVerticalAlignment(secRow, i % ToolbarPanel.SECONDARY_COLS, HasVerticalAlignment.ALIGN_TOP);
            i++;
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
