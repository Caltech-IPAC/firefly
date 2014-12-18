package edu.caltech.ipac.firefly.ui.panels;

import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;

/**
 * Date: Jun 20, 2008
 *
 * @author loi
 * @version $Id: StyledCollapsiblePanel.java,v 1.2 2011/06/29 17:03:49 roby Exp $
 */
public class StyledCollapsiblePanel extends CollapsiblePanel {

    private Widget collapsedHeader;
    private Widget expandedHeader;
    private String collapsedHeaderStyle;
    private String expandedHeaderStyle;
    private Image collapsedIcon;
    private Image expandedIcon;
    private boolean highlightCollasped;

    public StyledCollapsiblePanel(String title) {
        this(new Label(title), null, new Label(title), null, true, false);
        this.collapsedHeader.addStyleName("collapsible-panel-deflabel");
        this.expandedHeader.addStyleName("collapsible-panel-deflabel");
    }

    public StyledCollapsiblePanel(Widget collapsedHeader,
                            String collapsedHeaderStyle,
                            Widget expandedHeader,
                            String expandedHeaderStyle,
                            boolean isOpen,
                            boolean highlightCollasped) {

        super("", null, isOpen);
        this.collapsedHeader = collapsedHeader;
        this.expandedHeader = expandedHeader;
        this.highlightCollasped= highlightCollasped;
        getDisclosurePanel().setHeader(expandedHeader);
        getDisclosurePanel().setOpen(isOpen);

        addHeaderStyle();

        IconCreator ic= IconCreator.Creator.getInstance();
        collapsedIcon = new Image(ic.getCyanRightArrow());
        collapsedIcon.setPixelSize(12, 12);
        collapsedIcon.addStyleName("collapsible-panel-icon");
        this.collapsedHeaderStyle = collapsedHeaderStyle;
        expandedIcon = new Image(ic.getCyanDownArrow());
        expandedIcon.setPixelSize(12, 12);
        expandedIcon.addStyleName("collapsible-panel-icon");
        this.expandedHeaderStyle = expandedHeaderStyle;


        updateStyle();
        setupHeader();
    }

    @Override
    protected void onOpen() {
        setupHeader();
    }

    @Override
    protected void onClose() {
        setupHeader();
    }

    private void updateStyle() {
        if (isCollapsed() && highlightCollasped) {
            getDisclosurePanel().setStylePrimaryName("collapsible-panel-standout");
//            mainPanel.removeStyleName("standard-border");
//            mainPanel.addStyleName("standard-border-color-only");
            getDisclosurePanel().removeStyleName("standard-border");
//            mainPanel.addStyleName("collapsible-panel-standout-border");
        }
        else {
            getDisclosurePanel().setStylePrimaryName("collapsible-panel");
            getDisclosurePanel().addStyleName("standard-border");
        }
    }

    private void addHeaderStyle() {
        getDisclosurePanel().setHeader(new Label());
        Widget realHeader = getDisclosurePanel().getHeader().getParent();
        realHeader.addStyleName("collapsiblePanel-header");
    }

    public void setCollapsedHeader(String collapsedHeader) {
        setCollapsedHeader(new Label(collapsedHeader));
    }

    public void setCollapsedHeader(Widget collapsedHeader) {
        this.collapsedHeader = collapsedHeader;
        setupHeader();
    }

    public void setExpandedHeader(String expandedHeader) {
        setExpandedHeader(new Label(expandedHeader));
    }

    public void setHeaderStyle(String collapsedStyle, String expandedStyle) {
        collapsedHeaderStyle = collapsedStyle;
        expandedHeaderStyle = expandedStyle;
        setupHeader();
    }

    public void setExpandedHeader(Widget expandedHeader) {
        this.expandedHeader = expandedHeader;
        setupHeader();
    }

    public Widget getHeader() {
        return getDisclosurePanel().getHeader();
    }

    protected void setupHeader() {
        Widget header;
        Image image;
        updateStyle();
        Widget realHeader = getDisclosurePanel().getHeader().getParent();
        if (getDisclosurePanel().isOpen()) {
            header = expandedHeader;
            image = expandedIcon;
            if (expandedHeaderStyle != null) {
                realHeader.setStyleName(expandedHeaderStyle);
            }
        } else {
            header = collapsedHeader;
            image = collapsedIcon;
            if (collapsedHeaderStyle != null) {
                realHeader.setStyleName(collapsedHeaderStyle);
            }
        }
        HorizontalPanel p = new HorizontalPanel();
        p.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

        p.add(image);
        Label spacer = new Label();
        spacer.setWidth("2px");
        p.add(spacer);
        p.add(header);
        getDisclosurePanel().setHeader(p);
    }

//====================================================================
//
//====================================================================
}

