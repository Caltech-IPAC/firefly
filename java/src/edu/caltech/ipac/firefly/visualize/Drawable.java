package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.user.client.ui.Widget;

/**
 * User: roby
 * Date: Sep 30, 2009
 * Time: 10:01:49 AM
 */
public interface Drawable {

    public Widget addDrawingArea(Widget w);
    public void removeDrawingArea(Widget w);
    public void replaceDrawingArea(Widget old, Widget w);
    public void insertBeforeDrawingArea(Widget before, Widget w);
    public void insertAfterDrawingArea(Widget after, Widget w);
    public int getDrawingWidth();
    public int getDrawingHeight();
}
