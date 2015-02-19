/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core.background;

import com.google.gwt.user.client.ui.Widget;
/**
 * User: roby
 * Date: Dec 16, 2009
 * Time: 11:25:21 AM
 */


/**
 * @author Trey Roby
 */
public interface BackgroundActivation {

    public Widget buildActivationUI(MonitorItem monItem, int idx, boolean markAlreadyActivated);
    public void activate(MonitorItem monItem, int idx, boolean byAutoActivation);

}

