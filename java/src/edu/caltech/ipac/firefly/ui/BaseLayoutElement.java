package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 7/2/14
 *
 * @author loi
 * @version $Id: $
 */
public class BaseLayoutElement implements LayoutElement {

    private SimplePanel display = new SimplePanel();
    private Widget content;
    private boolean isShown;
    private List<ChangeListner> listeners;
    protected enum EventType {CONTENT_CHANGED, ON_HIDE, ON_SHOW}

    public BaseLayoutElement() {
        display.setSize("100%", "100%");
    }

    public Widget getDisplay() {
        return display;
    }

    public void setContent(Widget content) {
        this.content = content;
        if (this.content == null) {
            display.clear();
        } else {
            display.setWidget(content);
        }
    }

    public boolean hasContent() {
        return content != null;
    }

    public boolean isShown() {
        return isShown;
    }

    public void show() {
        display.setVisible(true);
        isShown = true;
        fireEvent(EventType.ON_SHOW);
    }

    public void hide() {
        display.setVisible(false);
        isShown = false;
        fireEvent(EventType.ON_HIDE);
    }

    public void addChangeListener(ChangeListner listener) {
        if (listeners == null) {
            listeners = new ArrayList<ChangeListner>();
        }
        listeners.add(listener);
    }

    protected void fireEvent(EventType etype) {

        if (listeners == null) return;

        for(ChangeListner cl : listeners) {
            if (etype == EventType.CONTENT_CHANGED) {
                cl.onContentChanged();
            } else if(etype == EventType.ON_HIDE) {
                cl.onHide();
            } else if (etype == EventType.ON_SHOW) {
                cl.onShow();
            }
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
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
