/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.download.CacheHelper;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ImagePlot;

import java.io.File;
/**
 * User: roby
 * Date: Jul 29, 2008
 * Time: 3:36:31 PM
 */


/**
 * @author Trey Roby
 */
public class VisContext {

    public static final String PLOT_ABORTED= "Plot aborted by client";
    public static final long    FITS_MAX_SIZE = AppProperties.getLongProperty("visualize.fits.MaxSizeInBytes",
                                                                              (long)(FileUtil.GIG*2));

    private static final Logger.LoggerImpl _log= Logger.getLogger();

    private static boolean _initialized= false;
    private static boolean _initializedCounters= false;

    public final static MemoryPurger purger;

    static {
        boolean speed=AppProperties.getBooleanProperty("visualize.fits.OptimizeForSpeed",true);
//        if (speed) {
//            purger= new OptimizeForSpeedPurger();
//        }
//        else {
//            purger= new OptimizeForMemoryPurger();
//
//        }
        purger=  null;

        init();
    }





//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    public static boolean isFileInPath(File f) { return ServerContext.validateFileInPath(f.getPath(), true)!=null; }


    public static void shouldContinue(PlotClientCtx ctx, ImagePlot plot) throws FailedRequestException {
        shouldContinue(ctx.getKey(), plot);
    }

    public static void shouldContinue(String ctxStr) throws FailedRequestException {
        shouldContinue(ctxStr,null);
    }

    public static void shouldContinue(String ctxStr, ImagePlot plot) throws FailedRequestException {
        if (ctxStr!=null && CtxControl.getPlotCtx(ctxStr)==null) {
            if (plot!=null) plot.freeResources();
            throw new FailedRequestException(PLOT_ABORTED,PLOT_ABORTED);
        }
    }

    public static void purgeOtherPlots(PlotState state) {
        if (purger!=null) purger.purgeOtherPlots(state);
    }



//    private static long getTotalMBUsedByUser() {
//        long totalMB= 0;
//        for(Map.Entry<String,PlotClientCtx> entry : getMap().entrySet()) {
//            totalMB+= entry.getValue().getDataSizeMB();
//        }
//        return totalMB;
//    }


    static public void init() {
        if (!_initialized) {
            System.setProperty("java.awt.headless", "true");

            _log.info("Working dir: "+ServerContext.getWorkingDir().getPath());

            File cacheDir= ServerContext.getVisCacheDir();

            Cache objCache= CacheManager.getCache(Cache.TYPE_PERM_LARGE);
            Cache fileCache=  CacheManager.getCache(Cache.TYPE_PERM_FILE);

            CacheHelper.setFileCache(fileCache);
            CacheHelper.setObjectCache(objCache);
            CacheHelper.setCacheDir(cacheDir);
            CacheHelper.setSupportsLifespan(true);

            _initialized= true;
        }

    }

    public static void initCounters() {
        if (!_initializedCounters) {
            Counters c= Counters.getInstance();
            c.initKey(Counters.Category.Visualization, "New Plots");
            c.initKey(Counters.Category.Visualization, "New 3 Color Plots");
            c.initKey(Counters.Category.Visualization, "3 Color Band");
            c.initKey(Counters.Category.Visualization, "Revalidate");
            c.initKey(Counters.Category.Visualization, "Zoom");
            c.initKey(Counters.Category.Visualization, "Crop");
            c.initKey(Counters.Category.Visualization, "Flip");
            c.initKey(Counters.Category.Visualization, "Rotate");
            c.initKey(Counters.Category.Visualization, "Color change");
            c.initKey(Counters.Category.Visualization, "Stretch change");
            c.initKey(Counters.Category.Visualization, "Fits header");
            c.initKey(Counters.Category.Visualization, "Region read");
            c.initKey(Counters.Category.Visualization, "Region save");
            c.initKey(Counters.Category.Visualization, "Area Stat");
            c.initKey(Counters.Category.Visualization, "Total Read", Counters.Unit.KB,0);
            _initializedCounters= true;
        }
    }



}

