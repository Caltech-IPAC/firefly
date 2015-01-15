/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
