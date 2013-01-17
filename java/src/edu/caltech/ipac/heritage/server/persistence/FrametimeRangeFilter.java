package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.heritage.searches.HeritageRequest;
import edu.caltech.ipac.heritage.ui.InstrumentPanel;

import java.text.DecimalFormat;

/**
 * @author tatianag
 *         $Id: FrametimeRangeFilter.java,v 1.2 2010/04/24 01:00:45 tatianag Exp $
 */
public class FrametimeRangeFilter {

    private static final String MIN_FRAMETIME = InstrumentPanel.FRAMETIME_MIN;
    private static final String MAX_FRAMETIME = InstrumentPanel.FRAMETIME_MAX;

    boolean hasFilters;
    private boolean hasMinFrametimeFilter;
    private boolean hasMaxFrametimeFilter;
    float minFT = 0, maxFT = 480; // frametime - min/max

    private HeritageRequest req;

    public FrametimeRangeFilter(HeritageRequest req)  {
        this.req = req;
        init();
    }

    private void init() {

        String minStr =  req.getParam(MIN_FRAMETIME);
        if (minStr == null) {
            hasMinFrametimeFilter = false;
        } else {
            hasMinFrametimeFilter = true;
            minFT = req.getFloatParam(MIN_FRAMETIME);
            if (minFT == Float.NaN) throw new IllegalArgumentException("Invalid min frametime: "+minStr);
        }

        String maxStr = req.getParam(MAX_FRAMETIME);
        if (maxStr == null) {
            hasMaxFrametimeFilter = false;
        } else {
            hasMaxFrametimeFilter = true;
            maxFT = req.getFloatParam(MAX_FRAMETIME);
            if (maxFT == Float.NaN) throw new IllegalArgumentException("Invalid max frametime: "+maxStr);
        }


        hasFilters = hasMinFrametimeFilter || hasMaxFrametimeFilter;
    }

    public boolean hasFilters() {
        return hasFilters;
    }

    public boolean hasMinFrametimeFilter() {
        return hasMinFrametimeFilter;
    }

    public boolean hasMaxFrametimeFilter() {
        return hasMaxFrametimeFilter;
    }

    public float getMinFrametime() { return minFT; }
    public float getMaxFrametime() { return maxFT; }


    public String toString() {
        DecimalFormat nf = new DecimalFormat("0.###");
        return ("Frametime Filter: "+
                (hasMinFrametimeFilter ? " minFT="+nf.format(minFT) : "")+
                (hasMaxFrametimeFilter ? "maxFT="+nf.format(maxFT) : ""));
    }

    public static boolean isDefinedOn(HeritageRequest req) {
        return req.containsParam(MAX_FRAMETIME) || req.containsParam(MIN_FRAMETIME);    
    }
}
