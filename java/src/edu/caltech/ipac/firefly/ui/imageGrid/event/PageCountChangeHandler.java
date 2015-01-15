/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.imageGrid.event;

import com.google.gwt.event.shared.EventHandler;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jul 27, 2010
 * Time: 12:35:11 PM
 * To change this template use File | Settings | File Templates.
 */
public interface PageCountChangeHandler extends EventHandler {
    /**
    * Called when a {@link PageCountChangeEvent} is fired.
    *
    * @param event the event that was fired
    */
    void onPageCountChange(PageCountChangeEvent event);
}
