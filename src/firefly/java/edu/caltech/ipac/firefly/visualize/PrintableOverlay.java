/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 10/2/12
 * Time: 9:50 AM
 */


import edu.caltech.ipac.firefly.visualize.draw.LayerDrawer;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;

import java.util.List;

/**
 * @author Trey Roby
 */
public interface PrintableOverlay {

    public void addPrintableLayer(List<StaticDrawInfo> drawInfoList,
                                  WebPlot plot,
                                  LayerDrawer drawer,
                                  WebLayerItem item);
}

