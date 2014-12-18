package edu.caltech.ipac.firefly.ui.creator;

import com.google.gwt.user.client.ui.Widget;

import java.util.Map;

import edu.caltech.ipac.firefly.ui.CatalogPopupPanel;
/**
 * User: roby
 * Date: Aug 13, 2010
 * Time: 1:42:43 PM
 */


/**
 * @author Trey Roby
 */
public class CatalogPopupPanelCreator implements FormWidgetCreator {
    public Widget create(Map<String, String> params) {
        return new CatalogPopupPanel(params);
    }
}

