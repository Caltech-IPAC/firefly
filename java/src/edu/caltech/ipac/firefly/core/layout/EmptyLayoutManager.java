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
