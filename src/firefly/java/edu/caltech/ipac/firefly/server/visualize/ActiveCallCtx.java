/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 5/12/15
 * Time: 1:06 PM
 */


import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.ImagePlot;

/**
 * @author Trey Roby
 */
public class ActiveCallCtx {

    private final PlotClientCtx plotClientCtx;
    private final ImagePlot plot;
    private final ActiveFitsReadGroup fitsReadGroup;

    public ActiveCallCtx(PlotClientCtx plotClientCtx, ImagePlot plot, ActiveFitsReadGroup fitsReadGroup) {
        this.plotClientCtx = plotClientCtx;
        this.plot = plot;
        this.fitsReadGroup = fitsReadGroup;
    }

    public PlotClientCtx getPlotClientCtx() { return plotClientCtx; }

    public ImagePlot getPlot() { return plot; }

    public ActiveFitsReadGroup getFitsReadGroup() { return fitsReadGroup; }

    public String getKey() { return plotClientCtx.getKey(); }
}
