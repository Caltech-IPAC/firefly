package edu.caltech.ipac.firefly.ui;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalSplitPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Date: Feb 25, 2009
 *
 * @author loi
 * @version $Id: SplitPanel.java,v 1.9 2009/12/09 00:42:35 loi Exp $
 */
public class SplitPanel extends Composite {
    /**
     * layout the main widget with respect to the secondary one.
     */
    public enum Layout {TOP, BOTTOM, LEFT, RIGHT, CENTER;
                        public boolean isHorizontal() {
                            return equals(LEFT) || equals(RIGHT);
                        }
                        public boolean isVertical() {
                            return equals(TOP) || equals(BOTTOM);
                        }
                    }

    private SimplePanel mainPanel;
    private Layout layout;
    private HorizontalSplitPanel hSplitter;
    private VerticalSplitPanel vSplitter;
    private Widget mainWidget;
    private Widget secondaryWidget;

    public SplitPanel() {
        this(null, null);
    }


    public SplitPanel(Widget mainWidget, Widget secondaryWidget) {
        mainPanel = new SimplePanel();
//        DOM.setStyleAttribute(mainPanel.getElement(), "border", "2px solid rgb(208, 228, 246)");
        initWidget(mainPanel);
        setLayout(Layout.LEFT);
        setMainWidget(mainWidget);
        setSecondaryWidget(secondaryWidget);

        layout();
    }

    public void setMainWidget(Widget mainWidget) {
        this.mainWidget = mainWidget;
        layout();
    }

    public void setSecondaryWidget(Widget secondaryWidget) {
        this.secondaryWidget = secondaryWidget;
        layout();
    }

    public Widget getMainWidget() {
        return mainWidget;
    }

    public Widget getSecondaryWidget() {
        return secondaryWidget;
    }

    public Layout getLayout() {
        return layout;
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
        layout();
    }

    public void setSplitPosition(String pos) {
        if (layout.isVertical()) {
            getVertSplit().setSplitPosition(pos);
        } else {
            getHoriSplit().setSplitPosition(pos);
        }
    }

    public void setScrollMode(Widget widget, String mode) {
        Element elem = widget.getElement().getParentElement();
        elem.getStyle().setProperty("overflow", mode);
    }


    @Override
    public void setHeight(String height) {
        super.setHeight(height);
        if (layout.isVertical()) {
            getVertSplit().setHeight(height);
        } else {
            getHoriSplit().setHeight(height);
        }
    }

    @Override
    public void setWidth(String width) {
        super.setWidth(width);
        if (layout.isVertical()) {
            getVertSplit().setWidth(width);
        } else {
            getHoriSplit().setWidth(width);
        }
    }

    protected void layout() {
        if (mainWidget == null) return;

        
//        DOM.setStyleAttribute(secondaryWidget.getElement(), "margin", "0px 0px 0px 5px");
//        DOM.setStyleAttribute(mainWidget.getElement(), "margin", "0px 10px, 0px 0px");

        if (secondaryWidget == null) {
            mainPanel.setWidget(mainWidget);
        } else if (layout.equals(Layout.LEFT)) {
            getHoriSplit().setLeftWidget(mainWidget);
            getHoriSplit().setRightWidget(secondaryWidget);
            mainPanel.setWidget(getHoriSplit());
        } else if (layout.equals(Layout.RIGHT)) {
            getHoriSplit().setLeftWidget(secondaryWidget);
            getHoriSplit().setRightWidget(mainWidget);
            mainPanel.setWidget(getHoriSplit());
        } else if (layout.equals(Layout.TOP)) {
            getVertSplit().setTopWidget(mainWidget);
            getVertSplit().setBottomWidget(secondaryWidget);
            mainPanel.setWidget(getVertSplit());
        } else if (layout.equals(Layout.BOTTOM)) {
            getVertSplit().setTopWidget(secondaryWidget);
            getVertSplit().setBottomWidget(mainWidget);
            mainPanel.setWidget(getVertSplit());
        } else if (layout.equals(Layout.CENTER)) {
            getHoriSplit().setLeftWidget(mainWidget);
            getHoriSplit().setRightWidget(null);
            getHoriSplit().setSplitPosition("100%");
            mainPanel.setWidget(getHoriSplit());
        }
    }

    private HorizontalSplitPanel getHoriSplit() {
        if (hSplitter == null) {
            hSplitter = new HorizontalSplitPanel();
        }
        return hSplitter;
    }

    private VerticalSplitPanel getVertSplit() {
        if (vSplitter == null) {
            vSplitter = new VerticalSplitPanel();
        }
        return vSplitter;
    }
}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
