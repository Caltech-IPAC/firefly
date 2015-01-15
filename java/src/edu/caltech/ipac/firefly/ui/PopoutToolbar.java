/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;
/**
 * User: roby
 * Date: 10/12/11
 * Time: 1:44 PM
 */


import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.util.StringUtils;

/**
 * @author Trey Roby
 */
public class PopoutToolbar extends Composite {

    private static final IconCreator _ic= IconCreator.Creator.getInstance();
    private final BadgeButton _popoutButton;
    private final HorizontalPanel _mainPanel= new HorizontalPanel();
    private static PopoutToolbar _lastToolbar= null;
    private static HideToolTimer _hideToolTimer= new HideToolTimer();
    private boolean backgroundAlwaysTransparent= false;
    private static boolean allToolbarsAlwaysVisible= false;


    public PopoutToolbar (ClickHandler h, boolean backgroundDark) {
//        _popoutButton= GwtUtil.makeImageButton(new Image(_ic.getExpandIcon()),
//                                                   "Expand this panel to take up a larger area",
//                                                   h);
        _popoutButton= GwtUtil.makeBadgeButton(new Image(_ic.getExpandIcon()),
                                               "Expand this panel to take up a larger area",
                                               backgroundDark,
                                               h);
        addToolbarButton(_popoutButton.getWidget());

        initWidget(_mainPanel);
        _mainPanel.addStyleName("popout-toolbar");
        this.allToolbarsAlwaysVisible= allToolbarsAlwaysVisible;
        if (!BrowserUtil.isTouchInput() && !allToolbarsAlwaysVisible)  GwtUtil.setHidden(_mainPanel, true);
    }

    public static void setAllToolbarsAlwaysVisible(boolean always) {
        allToolbarsAlwaysVisible= always;
    }


    public void addToolbarButton(Widget w) {
        _mainPanel.insert(w, 0);
        GwtUtil.setStyle(w, "marginRight", "4px");
    }

    public void setPopoutButtonVisible(boolean v) {
        _popoutButton.getWidget().setVisible(v);
    }

    public void setExpandIconImage(Image im)  {
        _popoutButton.setIcon(im);
    }

    public void showToolbar(boolean show) {
        if (!BrowserUtil.isTouchInput()) {
            _hideToolTimer.cancel();
            if (show) {
                if (backgroundAlwaysTransparent) {
                    GwtUtil.setStyle(_mainPanel, "backgroundColor", "transparent");
                }
                else {
                    setBackgroundColor(_mainPanel);
                }
                GwtUtil.setHidden(_mainPanel, false);
                if (_lastToolbar!=this) {
                    if (_lastToolbar!=null && !allToolbarsAlwaysVisible) GwtUtil.setHidden(_lastToolbar, true);
                    _lastToolbar= this;
                }
            }
            else {
                _hideToolTimer._popoutToolbar= this;
                _hideToolTimer.schedule(2000);
            }
        }
        else {
            GwtUtil.setHidden(_mainPanel, false);
        }
    }

    public void hideToolbar() { GwtUtil.setHidden(this, true); }

    public void setBackgroundAlwaysTransparent(boolean t) {
        this.backgroundAlwaysTransparent = t;
    }

    private void setBackgroundColor(Widget w) {
        String color= null;
        for(Element e= w.getElement(); (e!=null); e= e.getParentElement() ) {
            String c= GwtUtil.getBackgroundColor(e);
            if (!StringUtils.isEmpty(c) && !c.equalsIgnoreCase("transparent")) {
                color= c;
                break;
            }

        }
        w.getElement().getParentElement();
        if (color!=null) {
            GwtUtil.setStyle(w, "backgroundColor", color);
        }
    }

    private static class HideToolTimer extends Timer {
        private PopoutToolbar _popoutToolbar;
        @Override
        public void run() {
            if (_popoutToolbar!=null) GwtUtil.setHidden(_popoutToolbar, true);
        }
    }


}

