/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.imageGrid.event;

import com.google.gwt.event.shared.EventHandler;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jul 26, 2010
 * Time: 6:50:20 PM
 * To change this template use File | Settings | File Templates.
 */
public interface PageChangeHandler extends EventHandler {
  /**
   * Called when a {@link PageChangeEvent} is fired.
   *
   * @param event the event that was fired
   */
  void onPageChange(PageChangeEvent event);
}
