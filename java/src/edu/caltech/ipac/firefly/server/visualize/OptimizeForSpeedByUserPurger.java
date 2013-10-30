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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
