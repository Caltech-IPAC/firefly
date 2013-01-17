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
