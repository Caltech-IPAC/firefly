/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.TargetPanel;

import java.util.Map;
/**
 * User: roby
 * Date: Aug 13, 2010
 * Time: 1:42:43 PM
 */


/**
 * @author Trey Roby
 */
public class TargetPanelCreator implements FormWidgetCreator {
    public Widget create(Map<String, String> params) {
        return new TargetPanel();
    }
}

