/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.CacheHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 * Date: Jul 29, 2008
 */
public class VisContext {

    public static final String PLOT_ABORTED= "Plot aborted by client";
    public static final long FITS_MAX_SIZE = AppProperties.getLongProperty("visualize.fits.MaxSizeInBytes", FileUtil.GIG*20);
    private static boolean _initialized= false;
    private static final Map<String,String> footprintMap= new HashMap<>();

    static {
        init();
    }

    static public void init() {
        if (_initialized) return;
        System.setProperty("java.awt.headless", "true");
        CacheHelper.setCacheDir(ServerContext.getVisCacheDir());
        initFootprints();
        initCounters();
        _initialized = true;
    }

    private static void initCounters() {
        Counters c= Counters.getInstance();
        c.initKey(Counters.Category.Visualization, "New Plots");
        c.initKey(Counters.Category.Visualization, "New 3 Color Plots");
        c.initKey(Counters.Category.Visualization, "Byte Data: Full");
        c.initKey(Counters.Category.Visualization, "Byte Data: Half");
        c.initKey(Counters.Category.Visualization, "Byte Data: Quarter");
        c.initKey(Counters.Category.Visualization, "Crop");
        c.initKey(Counters.Category.Visualization, "Region read");
        c.initKey(Counters.Category.Visualization, "Region save");
        c.initKey(Counters.Category.Visualization, "Area Stat");
        c.initKey(Counters.Category.Visualization, "FITS re-read");
        c.initKey(Counters.Category.Visualization, "Recreate");
        c.initKey(Counters.Category.Visualization, "Total Read", Counters.Unit.KB,0);
    }

    private static void initFootprints() {
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
        footprintMap.put("JWST_NIRISS",    "footprint/Footprint_JWST.reg");
        footprintMap.put("JWST_NIRSPEC","footprint/Footprint_JWST.reg");
        footprintMap.put("Spitzer",     "footprint/Footprint_SPITZER.reg" );
        footprintMap.put("Spitzer_IRAC36", "footprint/Footprint_SPITZER.reg");
        footprintMap.put("Spitzer_IRAC45", "footprint/Footprint_SPITZER.reg");
        footprintMap.put("Roman",      "footprint/Footprint_Roman.reg");
        footprintMap.put("SOFIA",      "footprint/Footprint_SOFIA.reg");
        footprintMap.put("SOFIA_FORCAST_IMG",      "footprint/Footprint_SOFIA.reg");
        footprintMap.put("SOFIA_FIFI-LS_Blue",      "footprint/Footprint_SOFIA.reg");
        footprintMap.put("SOFIA_FIFI-LS_Red",      "footprint/Footprint_SOFIA.reg");
        footprintMap.put("SOFIA_FORCAST_GRISMS_A",      "footprint/Footprint_SOFIA.reg");
        footprintMap.put("SOFIA_FORCAST_GRISMS_B",      "footprint/Footprint_SOFIA.reg");

        footprintMap.put("SOFIA_FLITECAM_GRISMS_ABBA",      "footprint/Footprint_SOFIA.reg");
        footprintMap.put("SOFIA_FLITECAM_GRISMS_AB",      "footprint/Footprint_SOFIA.reg");
        footprintMap.put("SOFIA_FLITECAM_IMG",      "footprint/Footprint_SOFIA.reg");

        footprintMap.put("SOFIA_HAWC_BAND_A_TOTAL",      "footprint/Footprint_SOFIA.reg");
        footprintMap.put("SOFIA_HAWC_BAND_A_POLAR",      "footprint/Footprint_SOFIA.reg");

        footprintMap.put("SOFIA_HAWC_BAND_C_TOTAL",      "footprint/Footprint_SOFIA.reg");
        footprintMap.put("SOFIA_HAWC_BAND_C_POLAR",      "footprint/Footprint_SOFIA.reg");

        footprintMap.put("SOFIA_HAWC_BAND_D_TOTAL",      "footprint/Footprint_SOFIA.reg");
        footprintMap.put("SOFIA_HAWC_BAND_D_POLAR",      "footprint/Footprint_SOFIA.reg");

        footprintMap.put("SOFIA_HAWC_BAND_E_TOTAL",      "footprint/Footprint_SOFIA.reg");
        footprintMap.put("SOFIA_HAWC_BAND_E_POLAR",      "footprint/Footprint_SOFIA.reg");

        footprintMap.put("SOFIA_FPI+",      "footprint/Footprint_SOFIA.reg");
    }

    static public void addFootprint(String key, String path) { footprintMap.put(key, path); }

    static public String getFootprint(String key) { return footprintMap.get(key); }
}

