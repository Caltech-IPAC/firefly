/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.StringUtils;
/**
 * User: roby
 * Date: Oct 21, 2009
 * Time: 3:58:35 PM
 */


/**
 * Put a shadow border around a widget.
 * Does not work very well inside a table such as VeritcalPanel, HorizonalPanel, or Grid
 * @author Trey Roby
 */
public class ShadowedPanel extends Composite implements RequiresResize {

    private final AbsolutePanel _panel= new AbsolutePanel();
    private Widget helpIcon;
    private AbsolutePanel p;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public ShadowedPanel(Widget w) {
        p = new AbsolutePanel();
        initWidget(p);
        p.add(_panel);
        _panel.setStyleName("shadow");
        setSize("100%", "100%");
        GwtUtil.setStyles(_panel, "position", "absolute", "top", "0px", "bottom", "0px", "left", "0px", "right", "0px");
        if (w!=null) setContent(w);
        WebEventManager.getAppEvManager().addListener(Name.WINDOW_RESIZE,
                new WebEventListener(){
                    public void eventNotify(WebEvent ev) {
                        if (helpIcon != null) {
                            p.setWidgetPosition(helpIcon, p.getOffsetWidth()-23, 7);
                        }
                    }
                });

    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================
    void setHelpId(String id) {
        if (helpIcon != null) {
            p.remove(helpIcon);
        }
        if (!StringUtils.isEmpty(id)) {
            helpIcon = HelpManager.makeHelpIcon(id);
            p.add(helpIcon, p.getOffsetWidth()-23, 7);
        } else {
            helpIcon = null;
        }
    }


    public void setContent(Widget w) {
        _panel.clear();
        _panel.add(w);
        w.setSize("100%","100%");
    }

    public Widget getWidget() { return _panel.getWidgetCount() > 0 ? _panel.getWidget(0) : null; }

//=======================================================================
//-------------- Method from HasWidgets Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

    public void onResize() {
        Widget w= getWidget();
        if (w!=null && w instanceof RequiresResize) {
            ((RequiresResize)w).onResize();
        }
    }
}

