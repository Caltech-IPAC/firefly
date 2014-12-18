package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 10/2/12
 * Time: 9:50 AM
 */


import edu.caltech.ipac.firefly.visualize.draw.Drawer;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;

import java.util.List;

/**
 * @author Trey Roby
 */
public interface PrintableOverlay {

    public void addPrintableLayer(List<StaticDrawInfo> drawInfoList,
                                  WebPlot plot,
                                  Drawer drawer,
                                  WebLayerItem item);
}

