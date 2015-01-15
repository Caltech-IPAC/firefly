/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.VisibleListener;
/**
 * User: roby
 * Date: Apr 19, 2010
 * Time: 4:22:08 PM
 */


/**
 * @author Trey Roby
 */
public interface ResultUIComponent extends VisibleListener {


    public void bind(EventHub hub);
    public int getPrefHeight();
    public int getPrefWidth();
    Widget getDisplay();
    String getShortDesc();
    String getName();
    String getTitle();
}

