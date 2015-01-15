/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;

import com.google.gwt.user.client.ui.Widget;

import java.util.Map;

import edu.caltech.ipac.firefly.ui.RangePanel;

public class RangePanelCreator implements FormWidgetCreator {
    public Widget create(Map<String, String> params) {
        return new RangePanel(params);
    }
}

