/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.imageGrid.event;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jul 27, 2010
 * Time: 12:38:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class PageCountChangeEvent  extends GwtEvent<PageCountChangeHandler> {
    public static final Type TYPE = new Type<PageCountChangeHandler>();


    /**
    * The new page count.
    */
    private int newPageCount;

    /**
    * The previous page count.
    */
    private int oldPageCount;

    /**
    * Construct a new {@link PageCountChangeEvent}.
    *
    * @param oldPageCount the previous page
    * @param newPageCount the page that was requested
    */
    public PageCountChangeEvent(int oldPageCount, int newPageCount) {
        this.oldPageCount = oldPageCount;
        this.newPageCount = newPageCount;
    }

    /**
    * @return the new page count
    */
    public int getNewPageCount() {
        return newPageCount;
    }

    /**
    * @return the old page count
    */
    public int getOldPageCount() {
        return oldPageCount;
    }


//------------ Implementation for GwtEvent ------------
 
    protected void dispatch(PageCountChangeHandler h) {
        h.onPageCountChange(this);
    }

    public Type getAssociatedType() {
        return TYPE;
    }
}
