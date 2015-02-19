/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
/**
 * User: roby
 * Date: Apr 9, 2009
 * Time: 9:41:02 AM
 */


/**
 * This is experimental.  I don't think it has been tested
 * @author Trey Roby
 */
public class ResizablePanel extends SimplePanel implements RequiresResize, ProvidesResize {


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public ResizablePanel(Widget child) {
        super(child);
    }

    public ResizablePanel() {
        super();
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    public void setWidget(Widget w) {
        super.setWidget(w);
    }

//=======================================================================
//-------------- Method from ResizableWidget Interface ------------------
//=======================================================================

    public void onResize() {
        Widget w= getWidget();
        if (w!=null) {
            if (w instanceof RequiresResize) ((RequiresResize)w).onResize();
        }
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================



}

