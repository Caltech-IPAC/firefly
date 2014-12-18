package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.visualize.PlotState;

/**
 * User: roby
 * Date: 10/10/13
 * Time: 9:49 AM
 */
public interface MemoryPurger {
    public void purgeOtherPlots(PlotState state);
}
