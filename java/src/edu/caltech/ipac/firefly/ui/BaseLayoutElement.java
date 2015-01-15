/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.LayoutElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 7/2/14
 *
 * @author loi
 * @version $Id: $
 */
public class BaseLayoutElement implements LayoutElement {

    private ResizeLayoutPanel display = new ResizeLayoutPanel();
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
