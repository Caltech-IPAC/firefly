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

/**
 * @author Trey Roby
 */
public class ActiveCallCtx {

    private final PlotClientCtx plotClientCtx;
    private final ActiveFitsReadGroup fitsReadGroup;

    public ActiveCallCtx(PlotClientCtx plotClientCtx, ActiveFitsReadGroup fitsReadGroup) {
        this.plotClientCtx = plotClientCtx;
        this.fitsReadGroup = fitsReadGroup;
    }

    public ActiveFitsReadGroup getFitsReadGroup() { return fitsReadGroup; }

    public String getKey() { return plotClientCtx.getKey(); }
}
