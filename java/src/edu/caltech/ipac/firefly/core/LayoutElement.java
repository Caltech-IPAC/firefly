/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.VisibleListener;

/**
 * Date: 7/2/14
 *
 * @author loi
 * @version $Id: $
 */
public interface LayoutElement {

    Widget getDisplay();
    boolean hasContent();
    boolean isShown();
    void show();
    void hide();
    void addChangeListener(ChangeListner listener);



    interface ChangeListner extends VisibleListener {
        void onContentChanged();
    }

}
