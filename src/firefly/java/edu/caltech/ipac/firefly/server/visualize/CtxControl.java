/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize;


import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;

import java.io.File;

/**
 * @author Trey Roby
 */
public class CtxControl {

    private static final Logger.LoggerImpl log = Logger.getLogger();
    private static final Counters counters= Counters.getInstance();

    public static ActiveCallCtx prepare(PlotState state) throws FailedRequestException {
        try {
            ActiveCallCtx retval;
            PlotClientCtx ctx= getPlotCtx(state.getContextString());
            if (ctx==null) {
                String oldCtxStr= state.getContextString();
                ctx= makeAndCachePlotCtx();
                String ctxStr= ctx.getKey();
                state.setContextString(ctxStr);
                ctx.setPlotState(state);
                log.info("Plot context not found, recreating, Old : " + oldCtxStr+ " New: " + ctxStr);
                WebPlotFactory.recreate(state);
                retval= revalidatePlot(ctx);
                counters.incrementVis("Revalidate");
            }
            else {
                ctx.setPlotState(state);
                retval= revalidatePlot(ctx);
                if (retval==null) {
                    WebPlotFactory.recreate(state);
                    retval= revalidatePlot(ctx);
                    counters.incrementVis("Revalidate");
                }
            }
            return retval;
        } catch (FailedRequestException e) {
            log.warn(e, "prepare failed - failed to re-validate plot: " +
                            "User Msg: " + e.getUserMessage(),
                    "Detail Msg: " + e.getDetailMessage());
            throw e;
        } catch (GeomException e) {
            log.warn(e, "prepare failed - failed to re-validate plot: " + e.getMessage());
            throw new FailedRequestException("prepare failed, geom error", "this should almost never happen", e);
        }
    }




    private static ActiveCallCtx revalidatePlot(PlotClientCtx ctx)  {
        synchronized (ctx)  { // keep the test from happening at the same time with this ctx
            try {
                PlotState state= ctx.getPlotState();
                ActiveFitsReadGroup frGroup= new ActiveFitsReadGroup();
                for(Band band : state.getBands()) {
                    File fitsFile=   PlotStateUtil.getWorkingFitsFile(state, band);
                    int imageIdx= state.getImageIdx(band);
                    FitsRead[] fr= FitsCacher.readFits(fitsFile).getFitReadAry();  //this call should get data from cache if it exist
                    frGroup.setFitsRead(band, fr[imageIdx]);
                }

                if (frGroup.getFitsRead(state.firstBand())==null) {
                    Logger.warn("frGroup "+ state.firstBand()+ " is null after reread");
                }
                return new ActiveCallCtx(ctx,frGroup);
            } catch (Exception e) {
                log.warn(e, "revalidation failed: this should rarely happen: ", e.toString());
                return null;
            }
        }
    }

    public static String makeCachedCtx() { return makeAndCachePlotCtx().getKey(); }

    private static PlotClientCtx makeAndCachePlotCtx() {
        PlotClientCtx ctx= new PlotClientCtx();
        putPlotCtx(ctx);
        return ctx;
    }

    static void initPlotCtx(PlotState state) {
        PlotClientCtx ctx = CtxControl.getPlotCtx(state.getContextString());
        ctx.setPlotState(state);
        putPlotCtx(ctx);
        state.setNewPlot(false);
    }

    public static PlotClientCtx getPlotCtx(String ctxStr) {
        return (PlotClientCtx) getCache().get(new StringKey(ctxStr));
    }

    public static boolean isCtxAvailable(String ctxStr) {
        PlotClientCtx ctx= getPlotCtx(ctxStr);
        return (ctx!=null && ctx.getPlotState()!=null);
    }

    public static void putPlotCtx(PlotClientCtx ctx) {
        if (ctx!=null && ctx.getKey()!=null) {
            getCache().put(new StringKey(ctx.getKey()),ctx);
        }
    }

    public static void deletePlotCtx(PlotClientCtx ctx) {
        if (ctx==null) return;
        String key= ctx.getKey();
        getCache().put(new StringKey(key),null);
        log.info("deletePlotCtx: Deleted plot context: " + key);
    }

    static public Cache getCache() { return CacheManager.getCache(Cache.TYPE_VISUALIZE); }
}
