package edu.caltech.ipac.firefly.core.layout;

import com.google.gwt.user.client.ui.Widget;


/**
 * Date: Nov 15, 2007
 *
 * @author loi
 * @version $Id: RegionWrapper.java,v 1.11 2011/10/12 17:28:53 loi Exp $
 */
public abstract class RegionWrapper implements Region {

    private String id;
    private Region region;
    private Widget wrapper;

    public RegionWrapper(Region region) {
        this(region.getId(), region);
    }

    public RegionWrapper(String id, Region region) {
        this.id = id;
        this.region = region;
        wrapper = makeDisplay();
    }

    protected Region getRegion() {
        return region;
    }

    abstract protected Widget makeDisplay();

//====================================================================
//  Implementing Region through delegration
//====================================================================

    public void clear() {
        region.clear();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return region.getTitle();
    }

    public void setExpandedTitle(String title) {
        region.setExpandedTitle(title);
    }

    public void setCollapsedTitle(String title) {
        region.setCollapsedTitle(title);
    }

    public boolean isCollapsible() {
        return region.isCollapsible();
    }

    public void collapse() {
        region.collapse();
    }

    public void expand() {
        region.expand();
    }

    public Widget getDisplay() {
        return wrapper;
    }

    public Widget getContent() {
        return region.getContent();
    }

    public void setDisplay(Widget display) {
        region.setDisplay(display);
        show();
    }

    public void show() {
        wrapper.setVisible(true);
        region.show();
    }

    public void hide() {
        wrapper.setVisible(false);
        region.hide();
    }

    public int getMinHeight() {
        return region.getMinHeight();
    }

    public void setMinHeight(int minHeight) {
        region.setMinHeight(minHeight);
    }

    public void setInlineBlock(boolean inlineBlock) {
        region.setInlineBlock(inlineBlock);
    }
}

