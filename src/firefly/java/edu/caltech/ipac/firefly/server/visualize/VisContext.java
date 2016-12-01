/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.download.CacheHelper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
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
    public static final long   FITS_MAX_SIZE = AppProperties.getLongProperty("visualize.fits.MaxSizeInBytes",
                                                                              (long)(FileUtil.GIG*2));

    private static boolean _initialized= false;
    private static boolean _initializedCounters= false;

//    public final static MemoryPurger purger;

    static {
        init();
    }





//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================



    static public void init() {
        if (!_initialized) {
            System.setProperty("java.awt.headless", "true");

            Logger.LoggerImpl log= Logger.getLogger();
            log.info("Working dir: "+ServerContext.getWorkingDir().getPath());

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

    private static final Map<String, String> footprintMap;
    static {
        footprintMap = new HashMap<String, String>();
        footprintMap.put("HST",         "footprint/Footprint_HST.reg");
        footprintMap.put("HST_NICMOS",  "footprint/Footprint_HST.reg");
        footprintMap.put("HST_WFPC2",   "footprint/Footprint_HST.reg");
        footprintMap.put("HST_ACS/WFC", "footprint/Footprint_HST.reg");
        footprintMap.put("HST_ACS/HRC", "footprint/Footprint_HST.reg");
        footprintMap.put("HST_ACS/SBC", "footprint/Footprint_HST.reg");
        footprintMap.put("HST_WFC3/UVIS","footprint/Footprint_HST.reg");
        footprintMap.put("HST_WFC3/IR", "footprint/Footprint_HST.reg");
        footprintMap.put("JWST",        "footprint/Footprint_JWST.reg");
        footprintMap.put("JWST_FGS",    "footprint/Footprint_JWST.reg");
        footprintMap.put("JWST_MIRI",   "footprint/Footprint_JWST.reg");
        footprintMap.put("JWST_NIRCAM", "footprint/Footprint_JWST.reg");
        footprintMap.put("JWST_NIS",    "footprint/Footprint_JWST.reg");
        footprintMap.put("JWST_NIRSPEC","footprint/Footprint_JWST.reg");
        footprintMap.put("SPITZER",     "footprint/Footprint_SPITZER.reg" );
        footprintMap.put("SPITZER_IRAC36", "footprint/Footprint_SPITZER.reg");
        footprintMap.put("SPITZER_IRAC45", "footprint/Footprint_SPITZER.reg");
        footprintMap.put("WFIRST",      "footprint/Footprint_WFIRST.reg");
    }

    static public void addFootprint(String key, String path) {
        footprintMap.put(key, path);
    }

    static public String getFootprint(String key) {
        return footprintMap.get(key);
    }
}

