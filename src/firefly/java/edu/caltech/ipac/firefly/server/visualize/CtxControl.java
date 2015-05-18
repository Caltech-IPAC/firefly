/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 5/7/15
 * Time: 1:16 PM
 */


import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.Cleanupable;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.GeomException;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Trey Roby
 */
public class CtxControl {

    private static final Logger.LoggerImpl log = Logger.getLogger();
    private final static AtomicLong _lastDirCheck= new AtomicLong(0) ;
    private final static long EXPIRE_DAYS= 3;
    private final static long EXPIRE_DIR_DELTA = 1000 * 60 * 60 * 24 * EXPIRE_DAYS;
    private final static long CHECK_DIR_DELTA = 1000 * 60 * 60 * 12; // 12 hours
    private static Counters counters= Counters.getInstance();

    static ActiveCallCtx revalidatePlot(PlotClientCtx ctx)  {
        ActiveCallCtx retval= null;
        synchronized (ctx)  { // keep the test from happening at the same time with this ctx
            try {
                ImagePlot plot= ctx.getCachedPlot();
                PlotState state= ctx.getPlotState();
                if (plot==null) {
                    long start= System.currentTimeMillis();
                    boolean first= true;
                    StringBuilder lenStr= new StringBuilder(30);
                    ActiveFitsReadGroup frGroup= new ActiveFitsReadGroup();
                    for(Band band : state.getBands()) {
                        if (lenStr.length()>0) lenStr.append(", ");
                        File fitsFile=   PlotStateUtil.getWorkingFitsFile(state, band);

                        boolean blank= PlotServUtils.isBlank(state, band);

                        if (fitsFile.canRead() || blank) {
                            VisContext.purgeOtherPlots(state);
                            FitsRead fr[];
                            if (blank) fr= PlotServUtils.createBlankFITS(state.getWebPlotRequest(band));
                            else       fr= FitsCacher.readFits(fitsFile);
                            RangeValues rv= state.getRangeValues(band);
                            int imageIdx= state.getImageIdx(band);
                            frGroup.setFitsRead(band,fr[imageIdx]);
                            if (first) {
                                plot= PlotServUtils.makeImagePlot(frGroup,
                                                                  state.getZoomLevel(),
                                                                  state.isThreeColor(),
                                                                  band,
                                                                  state.getColorTableId(), rv);
                                plot.getPlotGroup().setZoomTo(state.getZoomLevel());
                                if (state.isThreeColor()) plot.setThreeColorBand(fr[imageIdx],band,frGroup);
                                ctx.setPlot(plot);
                                first= false;
                            }
                            else {
                                plot.setThreeColorBand(fr[imageIdx],band,frGroup);
                                plot.getHistogramOps(band,frGroup).recomputeStretch(rv);
                            }
                            Counters.getInstance().incrementVis("Revalidate");
                            lenStr.append(FileUtil.getSizeAsString(fitsFile.length()));
                        }
                    }
                    ctx.setPlot(plot);
                    plot.getPlotGroup().setZoomTo(state.getZoomLevel());
                    retval= new ActiveCallCtx(ctx,plot,frGroup);
                    String sizeStr= (state.isThreeColor() ? ", 3 Color: file sizes: " : ", file size: ");
                    long elapse= System.currentTimeMillis()-start;
                    log.info("revalidation success: " + ctx.getKey(),
                              "time: " + UTCTimeUtil.getHMSFromMills(elapse) +
                                      sizeStr + lenStr);

                    if (frGroup.getFitsRead(Band.NO_BAND)==null) {
                        Logger.info("frGroup 0 band is null after recreate");
                    }
                }
                else {
                    ActiveFitsReadGroup frGroup= new ActiveFitsReadGroup();
                    FitsRead fr[];
                    for(Band band : state.getBands()) {
                        boolean blank= PlotServUtils.isBlank(state, band);
                        File fitsFile=   PlotStateUtil.getWorkingFitsFile(state, band);
                        int imageIdx= state.getImageIdx(band);

                        if (blank) fr= PlotServUtils.createBlankFITS(state.getWebPlotRequest(band));
                        else       fr= FitsCacher.readFits(fitsFile);  //this call should get data from cache if it exist
                        frGroup.setFitsRead(band, fr[imageIdx]);
                    }

                    if (frGroup.getFitsRead(state.firstBand())==null) {
                        Logger.info("frGroup "+ state.firstBand()+ " is null after reread");
                    }
                    retval= new ActiveCallCtx(ctx,plot,frGroup);
                }
            } catch (Exception e) {
                log.warn(e, "revalidation failed: this should rarely happen: ",
                          e.toString());
                retval= null;
            }
            ctx.updateAccessTime();

        }
        return retval;
    }

    public static PlotClientCtx makeAndCachePlotCtx() {
        PlotClientCtx ctx= new PlotClientCtx();
        putPlotCtx(ctx);
        return ctx;
    }


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

                log.info("Plot context not found, creating new context.",
                         "Old context string: " + oldCtxStr,
                         "New context string: " + ctxStr);
                WebPlotFactory.recreate(state);
                retval= revalidatePlot(ctx);
                counters.incrementVis("Revalidate");

            }
            else {
                ctx.setPlotState(state);
                boolean success;
                retval= revalidatePlot(ctx);
//                else                  success= confirmFileData(ctx);
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

    private static boolean confirmFileData(PlotClientCtx ctx)  {
        boolean retval= true;
        try {
            PlotState state= ctx.getPlotState();

            for(Band band : state.getBands()) {
                if (!PlotStateUtil.getWorkingFitsFile(state, band).canRead() ||
                    !PlotStateUtil.getOriginalFile(state, band).canRead()) {
                    retval= false;
                    break;
                }
            }
        } catch (Exception e) {
            retval= false;
        }
        ctx.updateAccessTime();
        return retval;
    }

    /**
     * This cache key will be different for each user.  Even though it is static it compute a unique key
     * per session id.
     */
    private static CacheKey getKey() {
        return new StringKey("VisContext-OnePerUser-"+ ServerContext.getRequestOwner().getUserKey());
    }

    public static PlotClientCtx getPlotCtx(String ctxStr) {
        return (ctxStr!=null) ? getMap().get(ctxStr) : null;
    }

    public static void putPlotCtx(PlotClientCtx ctx) {
        if (ctx!=null && ctx.getKey()!=null) {
            synchronized (VisContext.class) {
                getMap().put(ctx.getKey(),ctx);
            }
        }
    }

    public static void deletePlotCtx(PlotClientCtx ctx) {
        if (ctx!=null) {
            String key= ctx.getKey();
            ctx.deleteCtx();
            synchronized (VisContext.class) {
                Map<String, PlotClientCtx> map= getMap();
                if (map.containsKey(key)) map.remove(key);
            }
            log.info("deletePlotCtx: Deleted plot context: " + key);
        }
    }

    /**
     * There is one map per PlotClientContextContainer.  Should work out to one map per user.
     * Each map contains all the PlotClientCtx for that user.
     * @return the map of plot of PlotClientCtx
     */
    static Map<String,PlotClientCtx> getMap() {

        Cache cache= getCache();
        PlotClientCtxContainer ctxContainer;
        boolean created= false;
        CacheKey key= getKey();
        synchronized (VisContext.class) {
            ctxContainer = (PlotClientCtxContainer)cache.get(key);
            if (ctxContainer ==null) {
                created= true;
                ctxContainer = new PlotClientCtxContainer();
                cache.put(key, ctxContainer);
            }
        }
        if (created) {
            log.info("New session or cache was cleared: Creating new PlotClientCtxContainer",
                      "key: " + key.getUniqueString());
        }
        return ctxContainer.getMap();
    }

    static Cache getCache() { return CacheManager.getCache(Cache.TYPE_VISUALIZE); }

    private static void cleanupOldDirs() {
        long now= System.currentTimeMillis();
        long lastDelta= now-_lastDirCheck.get();
        if (lastDelta>CHECK_DIR_DELTA) {
            for(Map.Entry<File,Long> entry : ServerContext._visSessionDirs.entrySet()) {
                if (now - entry.getValue() > EXPIRE_DIR_DELTA) {
                    FileUtil.deleteDirectory(entry.getKey());
                    log.briefInfo("Removed " + EXPIRE_DAYS + " day old directory: " + entry.getKey().getPath());
                }
            }
            _lastDirCheck.getAndSet(System.currentTimeMillis());
        }
    }


// =====================================================================
// -------------------- Inner classes ----------------------------------
// =====================================================================

    public static class PlotClientCtxContainer implements Serializable, Cleanupable {
        private final Map<String,PlotClientCtx> _map= new ConcurrentHashMap<String,PlotClientCtx>(217);

        Map<String,PlotClientCtx> getMap() { return _map; }

        public void cleanup() {
            PlotClientCtx ctx;
            Logger.briefDebug("PlotClientCtxContainer.cleanup, Entries:"+_map.size() );
            cleanupOldDirs();
            for(Map.Entry<String,PlotClientCtx> entry : getMap().entrySet()) {
                    ctx= entry.getValue();
                    ctx.freeResources(PlotClientCtx.Free.OLD);
            }
        }
    }
}
