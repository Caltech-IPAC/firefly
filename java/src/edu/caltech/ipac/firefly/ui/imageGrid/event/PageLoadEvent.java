/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.imageGrid.event;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jul 27, 2010
 * Time: 12:37:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class PageLoadEvent extends GwtEvent<PageLoadHandler> {
    public static final Type TYPE = new Type<PageLoadHandler>();

  /**
    * The page that was loaded.
    */
    private int page;

    /**
    * Construct a new {@link PageLoadEvent}.
    *
    * @param page the page that was loaded
    */
    public PageLoadEvent(int page) {
        this.page = page;
    }

    /**
    * @return the page that has finished loading
    */
    public int getPage() {
        return page;
    }

//------------ Implementation for GwtEvent ------------
    
    protected void dispatch(PageLoadHandler h) {
        h.onPageLoad(this);
    }

    public GwtEvent.Type getAssociatedType() {
        return TYPE;
    }
}
