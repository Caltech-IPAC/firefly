/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize;


import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;

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

                log.briefInfo("Plot context not found, recreating, Old : " + oldCtxStr+ " New: " + ctxStr);
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
        ActiveCallCtx retval;
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

                        if (fitsFile.canRead()) {
                            FitsRead[] fr= FitsCacher.readFits(fitsFile).getFitReadAry();
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
                    String elapseStr= UTCTimeUtil.getHMSFromMills(System.currentTimeMillis()-start);
                    log.info("revalidation success: " + ctx.getKey(), "time: " + elapseStr + sizeStr + lenStr);

                    if (frGroup.getFitsRead(Band.NO_BAND)==null) Logger.info("frGroup 0 band is null after recreate");
                }
                else {
                    ActiveFitsReadGroup frGroup= new ActiveFitsReadGroup();
                    for(Band band : state.getBands()) {
                        File fitsFile=   PlotStateUtil.getWorkingFitsFile(state, band);
                        int imageIdx= state.getImageIdx(band);
                        FitsRead[] fr= FitsCacher.readFits(fitsFile).getFitReadAry();  //this call should get data from cache if it exist
                        frGroup.setFitsRead(band, fr[imageIdx]);
                    }

                    if (frGroup.getFitsRead(state.firstBand())==null) {
                        Logger.info("frGroup "+ state.firstBand()+ " is null after reread");
                    }
                    retval= new ActiveCallCtx(ctx,plot,frGroup);
                }
            } catch (Exception e) {
                log.warn(e, "revalidation failed: this should rarely happen: ", e.toString());
                retval= null;
            }
            ctx.updateAccessTime();

        }
        return retval;
    }

    public static String makeCachedCtx() {
        return makeAndCachePlotCtx().getKey();
    }

    private static PlotClientCtx makeAndCachePlotCtx() {
        PlotClientCtx ctx= new PlotClientCtx();
        putPlotCtx(ctx);
        return ctx;
    }

    static void initPlotCtx(PlotState state,
                            ImagePlot plot,
                            ActiveFitsReadGroup frGroup,
                            PlotImages images) throws FitsException, IOException {
        PlotClientCtx ctx = CtxControl.getPlotCtx(state.getContextString());
        ctx.setImages(images);
        ctx.setPlotState(state);
        ctx.setPlot(plot);
        putPlotCtx(ctx);
        PlotServUtils.createThumbnail(plot, frGroup, images, true, state.getThumbnailSize());
        state.setNewPlot(false);
    }



    public static PlotClientCtx getPlotCtx(String ctxStr) {
        return (PlotClientCtx) getCache().get(new StringKey(ctxStr));
    }

    public static boolean isCtxAvailable(String ctxStr) {
        PlotClientCtx ctx= getPlotCtx(ctxStr);
        return (ctx!=null && ctx.getCachedPlot()!=null);
    }

    public static void putPlotCtx(PlotClientCtx ctx) {
        if (ctx!=null && ctx.getKey()!=null) {
            getCache().put(new StringKey(ctx.getKey()),ctx);
        }
    }

    public static boolean isImagePlotAvailable(String ctxString) {
        PlotClientCtx ctx = getPlotCtx(ctxString);
        if (ctx==null) return false;
        return (ctx.getCachedPlot() != null);
    }

    public static void freeCtxResources(String ctxString) {
        PlotClientCtx ctx = getPlotCtx(ctxString);
        if (ctx != null) ctx.freeResources(PlotClientCtx.Free.ALWAYS);
    }


    public static void deletePlotCtx(PlotClientCtx ctx) {
        if (ctx==null) return;
        String key= ctx.getKey();
        ctx.deleteCtx();
        getCache().put(new StringKey(key),null);
        log.info("deletePlotCtx: Deleted plot context: " + key);
    }

    static public Cache getCache() { return CacheManager.getCache(Cache.TYPE_VISUALIZE); }

    public static void updateCachedPlot(ActiveCallCtx ctx) { if (ctx!=null) updateCachedPlot(ctx.getKey()); }

    public static void updateCachedPlot(String ctxStr) {
        PlotClientCtx ctx= getPlotCtx(ctxStr);
        if (ctx==null) return;
        ImagePlot p= ctx.getCachedPlot();
        if (p!=null) ctx.setPlot(p);
    }

// **** KEEP Around **** I might want to use this code somewhere else
//    private static void cleanupOldDirs() {
//        long now= System.currentTimeMillis();
//        long lastDelta= now-_lastDirCheck.get();
//        if (lastDelta>CHECK_DIR_DELTA) {
//            for(Map.Entry<File,Long> entry : ServerContext._visSessionDirs.entrySet()) {
//                if (now - entry.getValue() > EXPIRE_DIR_DELTA) {
//                    FileUtil.deleteDirectory(entry.getKey());
//                    log.briefInfo("Removed " + EXPIRE_DAYS + " day old directory: " + entry.getKey().getPath());
//                }
//            }
//            _lastDirCheck.getAndSet(System.currentTimeMillis());
//        }
//    }
}
