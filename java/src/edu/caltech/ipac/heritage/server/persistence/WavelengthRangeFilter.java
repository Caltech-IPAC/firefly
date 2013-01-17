package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.heritage.searches.HeritageRequest;
import edu.caltech.ipac.heritage.ui.InstrumentPanel;
import edu.caltech.ipac.util.StringUtils;

import java.text.DecimalFormat;

/**
 * @author tatianag
 *         $Id: WavelengthRangeFilter.java,v 1.2 2010/04/24 01:00:45 tatianag Exp $
 */
public class WavelengthRangeFilter {

    private static final String FILTER_KEY = InstrumentPanel.RADIO_PANEL;
    private static final String MIN_WAVELENGTH = InstrumentPanel.WAVE_MIN;
    private static final String MAX_WAVELENGTH = InstrumentPanel.WAVE_MAX;

    boolean hasFilters;
    boolean hasMinWavelengthFilter;
    boolean hasMaxWavelengthFilter;
    float min = 0, max = 180;

    private HeritageRequest req;
        public WavelengthRangeFilter(HeritageRequest req)  {
        this.req = req;
        init();
    }

    private void init() {

        String minStr = req.getParam(MIN_WAVELENGTH);
        if (minStr == null) {
            hasMinWavelengthFilter = false;
        } else {
            hasMinWavelengthFilter = true;
            min = req.getFloatParam(MIN_WAVELENGTH);
            if (min == Float.NaN) throw new IllegalArgumentException("Invalid min. wavelength: "+minStr);
        }

        String maxStr = req.getParam(MAX_WAVELENGTH);
        if (maxStr == null) {
            hasMaxWavelengthFilter = false;
        } else {
            hasMaxWavelengthFilter = true;
            max = req.getFloatParam(MAX_WAVELENGTH);
            if (max == Float.NaN) throw new IllegalArgumentException("Invalid max. wavelength: "+maxStr);
        }


        hasFilters = hasMinWavelengthFilter || hasMaxWavelengthFilter; 
    }

    public boolean hasFilters() {
        return hasFilters;
    }

    public boolean hasMinWavelengthFilter() {
        return hasMinWavelengthFilter;
    }

    public boolean hasMaxWavelengthFilter() {
        return hasMaxWavelengthFilter;
    }

    public float getMinWavelength() { return min; }
    public float getMaxWavelength() {return max; }

    public String toString() {
        DecimalFormat nf = new DecimalFormat("0.###");
        return ("Wavelength Filter: min="+nf.format(min)+"; max="+nf.format(max));
    }

    public static boolean isDefinedOn(HeritageRequest req) {
        String f = req.getParam(FILTER_KEY);
        return !StringUtils.isEmpty(f) && f.equals("wavelength");
    }
}
