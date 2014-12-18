package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 10/10/13
 * Time: 9:50 AM
 */


import edu.caltech.ipac.firefly.visualize.PlotState;

import java.util.ConcurrentModificationException;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class OptimizeForMemoryPurger implements MemoryPurger {

    public void purgeOtherPlots(PlotState excludeState) {
        PlotClientCtx ctx= VisContext.getPlotCtx(excludeState.getContextString());
        if (ctx!=null) {
            String excludeKey= ctx.getKey();
            synchronized (VisContext.class) {
                try {
                    for(Map.Entry<String,PlotClientCtx> entry : VisContext.getMap().entrySet()) {
                        if (!entry.getKey().equals(excludeKey)) {
                            entry.getValue().freeResources(PlotClientCtx.Free.YOUNG);
                        }
                    }
                } catch (ConcurrentModificationException e) {
                    // just abort the purging - another thread is updating the map
                }
            }
        }
    }
}

