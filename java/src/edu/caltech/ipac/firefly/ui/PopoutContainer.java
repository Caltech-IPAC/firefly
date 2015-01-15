/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.util.Dimension;

/**
 * User: roby
 * Date: 9/9/11
 * Time: 10:31 AM
 */
public interface PopoutContainer {

    public void setPopoutWidget(PopoutWidget popout);
    public void show();
    public void hide();
    public void hideOnlyDisplay();
    public void setTitle(String title);
    public void setTitle(Widget title);
    public Dimension getAvailableSize();
    public boolean isExpanded();
    public Panel getHeaderBar();
    public boolean isCloseShowing();
    public boolean isViewControlShowing();
    public boolean isImageSelectionShowing();
    public void freeResources();
}
