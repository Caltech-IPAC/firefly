package edu.caltech.ipac.firefly.core.layout;

import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: 4/29/14
 *
 * @author loi
 * @version $Id: $
 */
public class EmptyLayoutManager implements LayoutManager {
    private Map<String, Region> regions;
    private Widget display;
    private LayoutSelector layoutSelector;

    public Widget getDisplay() {
        return display;
    }

    public void setDisplay(Widget display) {
        this.display = display;
    }

    public LayoutSelector getLayoutSelector() {
        return layoutSelector;
    }

    public void setLayoutSelector(LayoutSelector layoutSelector) {
        this.layoutSelector = layoutSelector;
    }

    public Region getRegion(String id) {
        return regions == null ? null : regions.get(id);
    }

    public void addRegion(Region region) {
        if (region != null) setRegion(region.getId(), region);
    }

    public void setRegion(String id, Region region) {
        if (regions == null) {
            regions = new HashMap<String, Region>();
        }
        if (region == null && regions.containsKey(id)) {
            regions.remove(regions.get(id));
        } else {
            regions.put(id, region);
        }
    }

    public List<Region> getRegions() {
        return new ArrayList<Region>(regions.values());
    }

    public void resize() {
    }

    public int getMinHeight() {
        return 0;
    }

    public int getMinWidth() {
        return 0;
    }

    public boolean isLoading() {
        return false;
    }

    public void setLoading(Boolean isLoading, String msg) {
    }

    public void layout(String loadToDiv) {
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
