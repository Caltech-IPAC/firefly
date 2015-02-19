/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.ui;
/**
 * User: roby
 * Date: 9/4/14
 * Time: 3:53 PM
 */


import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;

import java.util.List;

/**
 * @author Trey Roby
 */
public interface ImageSelectPanelPlotter {
    public void doPlot(PlotWidgetOps ops, PlotTypeUI ptype, boolean createNew, boolean threeColor, Band threeColorBand);
    public void remove3ColorIDBand(Band removeBand);
    public boolean isCreateNewVisible();
    public void showing(List<PlotTypeUI> plotTypeUIList);
    public PlotTypeUI[] getExtraPanels();
}

