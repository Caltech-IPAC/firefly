package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 10/10/13
 * Time: 9:50 AM
 */


import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.AppProperties;

import java.util.ConcurrentModificationException;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class OptimizeForSpeedByUserPurger implements MemoryPurger {

    public static final long USER_ALLOWED_SIZE_MB= AppProperties.getLongProperty("visualize.fits.UserAllowedSizeMB",
                                                                                 700);

    public void purgeOtherPlots(PlotState excludeState) {
        PlotClientCtx ctx= VisContext.getPlotCtx(excludeState.getContextString());
        if (ctx!=null) {
            String excludeKey= ctx.getKey();
            synchronized (VisContext.class) {
                try {
                    long cnt= 0;
                    PlotClientCtx testCtx;
                    boolean freed;
                    for(Map.Entry<String,PlotClientCtx> entry : VisContext.getMap().entrySet()) {
                        testCtx= entry.getValue();
                        if (!testCtx.getKey().equals(excludeKey)) {
                            if (testCtx.getPlot()!=null) {  // if we are using memory
                                if (cnt>USER_ALLOWED_SIZE_MB) {
                                    freed= testCtx.freeResources(PlotClientCtx.Free.YOUNG);
                                    if (!freed) cnt+= testCtx.getDataSizeMB();
                                }
                                else {
                                    freed= entry.getValue().freeResources(PlotClientCtx.Free.OLD);
                                    if (!freed) cnt+= testCtx.getDataSizeMB();
                                }
                            }
                        }
                    }
                } catch (ConcurrentModificationException e) {
                    // just abort the purging - another thread is updating the map
                }
            }
        }
    }
}

