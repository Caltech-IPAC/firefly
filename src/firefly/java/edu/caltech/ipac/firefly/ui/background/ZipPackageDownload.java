/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.background;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.background.BackgroundActivation;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.core.background.PackageProgress;
/**
 * User: roby
 * Date: Dec 17, 2009
 * Time: 11:35:05 AM
 */


/**
 * @author Trey Roby
 */
public class ZipPackageDownload implements BackgroundActivation {

    public ZipPackageDownload () {
    }

    public Widget buildActivationUI(MonitorItem monItem, int idx, boolean markAlreadyActivated) {
        return new PackageReadyWidget(monItem,idx, markAlreadyActivated);
    }

    public void activate(MonitorItem monItem, int idx, boolean byAutoActivation) {
        PackageProgress part= monItem.getStatus().getPartProgress(idx);
        String url= part.getURL();
        PackageReadyWidget.getZipFile(url);
        monItem.setActivated(idx, true);
    }


}

