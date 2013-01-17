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
    private final Widget _popoutButton;
    private final HorizontalPanel _mainPanel= new HorizontalPanel();
    private static PopoutToolbar _lastToolbar= null;
    private static HideToolTimer _hideToolTimer= new HideToolTimer();


    public PopoutToolbar (ClickHandler h) {
        _popoutButton= GwtUtil.makeImageButton(new Image(_ic.getExpandIcon()),
                                                   "Expand this panel to take up a larger area",
                                                   h);
        addToolbarButton(_popoutButton);

        initWidget(_mainPanel);
        if (!BrowserUtil.isTouchInput())  GwtUtil.setHidden(_mainPanel, true);
    }



    public void addToolbarButton(Widget w) {
        _mainPanel.insert(w, 0);
        GwtUtil.setStyle(w, "marginRight", "4px");
    }

    public void setPopoutButtonVisible(boolean v) {
        _popoutButton.setVisible(v);
    }


    public void showToolbar(boolean show) {
        if (!BrowserUtil.isTouchInput()) {
            _hideToolTimer.cancel();
            if (show) {
                setBackgroundColor(_mainPanel);
                GwtUtil.setHidden(_mainPanel, false);
                if (_lastToolbar!=this) {
                    if (_lastToolbar!=null) GwtUtil.setHidden(_lastToolbar, true);
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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
