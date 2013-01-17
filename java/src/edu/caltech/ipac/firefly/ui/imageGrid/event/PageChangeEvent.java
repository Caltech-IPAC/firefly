package edu.caltech.ipac.firefly.ui.imageGrid.event;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jul 26, 2010
 * Time: 6:51:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class PageChangeEvent extends GwtEvent<PageChangeHandler> {
    public static final Type TYPE = new Type<PageChangeHandler>();

    /**
    * The new page.
    */
    private int newPage;

    /**
    * The previous page.
    */
    private int oldPage;

    /**
   * Construct a new {@link PageChangeEvent}.
   *
   * @param oldPage the previous page
   * @param newPage the page that was requested
   */
    public PageChangeEvent(int oldPage, int newPage) {
        this.oldPage = oldPage;
        this.newPage = newPage;
    }

    /**
    * @return the new page that was requested
    */
    public int getNewPage() {
        return newPage;
    }

    /**
    * @return the old page
    */
    public int getOldPage() {
        return oldPage;
    }

//------------ Implementation for GwtEvent ------------ 

    protected void dispatch(PageChangeHandler h) {
        h.onPageChange(this);
    }

    public Type getAssociatedType() {
        return TYPE;
    }
}
