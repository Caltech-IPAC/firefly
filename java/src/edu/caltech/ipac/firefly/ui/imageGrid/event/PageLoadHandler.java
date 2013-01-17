package edu.caltech.ipac.firefly.ui.imageGrid.event;

import com.google.gwt.event.shared.EventHandler;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jul 27, 2010
 * Time: 12:35:57 PM
 * To change this template use File | Settings | File Templates.
 */
public interface PageLoadHandler extends EventHandler {
    /**
    * Called when a {@link PageLoadEvent} is fired.
    *
    * @param event the event that was fired
    */
    void onPageLoad(PageLoadEvent event);
}
