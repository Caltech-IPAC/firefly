package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.heritage.searches.HeritageRequest;
import edu.caltech.ipac.heritage.ui.InstrumentPanel;

import java.util.*;

/**
 * @author tatianag
 *         $Id: InstrumentFilter.java,v 1.11 2012/02/28 21:38:07 tatianag Exp $
 */
public class InstrumentFilter {


    private static final String FILTER_KEY = InstrumentPanel.RADIO_PANEL;

    private static String IRAC_KEY = InstrumentPanel.IRAC;
    private static String IRS_KEY = InstrumentPanel.IRS;
    private static String MIPS_KEY = InstrumentPanel.MIPS;
    private static String ALL = InstrumentPanel.ALL;
    private static String NONE = InstrumentPanel.NONE;

    private static String IRAC_READOUT_KEY = InstrumentPanel.IRAC_READOUT;
    private static String MIPS_PHOT_SCALE_KEY = InstrumentPanel.MIPS_PHOT_SCALE;
    private static String MIPS_SCAN_RATE_KEY = InstrumentPanel.MIPS_SCAN_RATE;



    public static final String [] AOTS= {"IRAC","IRACPC", "IracMap","IracMapPC",
            "IRS","IrsStare","IrsMap","IrsPeakupImage",
            "MIPS","MipsPhot","MipsScan","MipsSed","MipsTp"};
    private static final ArrayList<String> AOT_LIST = new ArrayList<String>(Arrays.asList(AOTS));

    private static String SEPARATOR = ",";


    private static final HashMap<String,String> iracDef = new HashMap<String, String>(6);

    static {
        iracDef.put("ier", "IRAC");
        iracDef.put("ierpc","IRACPC");
        iracDef.put("map", "IracMap");
        iracDef.put("post", "IracMapPC");
        iracDef.put("w3", "IRAC 3.6um");
        iracDef.put("w4", "IRAC 4.5um");
        iracDef.put("w5", "IRAC 5.8um");
        iracDef.put("w8", "IRAC 8.0um");
    }

    private static final int NUM_IRAC_WAVELENGTHS = 4;
    private static final int NUM_IRAC_REQMODES = 4;

    private static final HashMap<String,String> irsDef = new HashMap<String,String>(11);

    static {
        irsDef.put("_all_","IRS");
        irsDef.put("stare","IrsStare");
        irsDef.put("map","IrsMap");
        irsDef.put("image","IrsPeakupImage");
        irsDef.put("hi10","IRS SH 9.9-19.6um");
        irsDef.put("hi19","IRS LH 18.7-37.2um");
        irsDef.put("low5","IRS SL 5.2-8.7um"+SEPARATOR+"IRS SL 5.2-14.5um");
        irsDef.put("low7","IRS SL 7.4-14.5um"+SEPARATOR+"IRS SL 5.2-14.5um");
        irsDef.put("low14","IRS LL 14.0-21.7um"+SEPARATOR+"IRS LL 14.0-38.0um");
        irsDef.put("low20","IRS LL 19.5-38.0um"+SEPARATOR+"IRS LL 14.0-38.0um");
        irsDef.put("blue","IRS PU Blue 13.3-18.7um");
        irsDef.put("red","IRS PU Red 18.5-26.0um");

    }

    private static final int NUM_IRS_WAVELENGTHS = 10;
    private static final int NUM_IRS_REQMODES = 4;


    private static final HashMap<String,String> mipsDef = new HashMap<String,String>();

    static {
        mipsDef.put("_all_","MIPS");
        mipsDef.put("photo","MipsPhot");
        mipsDef.put("scan","MipsScan");
        mipsDef.put("sed","MipsSed");
        mipsDef.put("power","MipsTp");
        mipsDef.put("w24","MIPS 24um");
        mipsDef.put("w70","MIPS 70um");
        mipsDef.put("w160","MIPS 160um");
        
    }

    private static final int NUM_MIPS_WAVELENGTHS = 3;
    private static final int NUM_MIPS_REQMODES = 5;


    private HeritageRequest req;
    private boolean hasFilters = false;
    private boolean hasWavelengthFilter = false;
    private boolean hasReqmodeFilter = false;
    private boolean hasInstrumentFilter = false;
    private boolean hasIracFilters = false;
    private boolean hasMipsFilters = false;
    private boolean hasIracFullArrayFilter = false;
    private boolean hasIracStellarModeFilter = false;
    private boolean hasIracHDRFilter = false;
    private boolean hasMipsPhotScaleFilter = false;
    private boolean hasMipsScanRateFilter = false;
    private HashSet<String> reqmodes;
    private HashSet<String> wavelengths;
    private ArrayList<String> instruments;
    private short iracFullArray = -1;
    private short mipsScaleFine = -1;
    private List<String> mipsScanRates;

    public InstrumentFilter() {}

    public InstrumentFilter(HeritageRequest req)  {
        this.req = req;
        init();
    }

    public boolean hasFilters() {
        return hasFilters;
    }

    public boolean hasWavelenthFilter() {
        return hasWavelengthFilter;
    }

    public boolean hasReqmodeFilter() {
        return hasReqmodeFilter;
    }

    public boolean hasInstrumentFilter() {
        return hasInstrumentFilter;
    }

    public boolean hasIracFilters() {
        return hasIracFilters;
    }

    public boolean hasMipsFilters() {
        return hasMipsFilters;
    }

    public boolean hasIracFullArrayFilter() {
        return hasIracFullArrayFilter;
    }

    public boolean hasIracStellarModeFilter() {
        return hasIracStellarModeFilter;
    }

    public boolean hasIracHDRFilter() {
        return hasIracHDRFilter;
    }

    public boolean hasMipsPhotScaleFilter() {
        return hasMipsPhotScaleFilter;
    }

    public boolean hasMipsScanRateFilter() {
        return hasMipsScanRateFilter;
    }

    public short getIracFullArray() {
        return iracFullArray;
    }

    public short getMipsScaleFine() {
        return mipsScaleFine;
    }

    public String getMipsScanRatesAsString() {
        return toQuotedStringList(mipsScanRates);
    }


    private void init() {

        hasFilters = true;

        // check weather filter could be omitted
        String iracSelections = req.getParam(IRAC_KEY);
        String irsSelections = req.getParam(IRS_KEY);
        String mipsSelections = req.getParam(MIPS_KEY);
        if (iracSelections == null && irsSelections == null && mipsSelections == null ) hasFilters = false;

        // IRAC filters
        if (iracSelections != null) {
            String readoutSelections = req.getParam(IRAC_READOUT_KEY);
            if (readoutSelections != null) {
                // if both full and sub are selected, apply no filters
                if (readoutSelections.contains("full") && !readoutSelections.contains("sub")) {
                    hasIracFullArrayFilter = true;
                    iracFullArray = 1;
                } else {
                    /*
                        because stellar and HDR both read out the full array,
                        i think that only the true spitzer afficionados will
                        understand its behavior if picking 'full array'
                        didn't return all of the full array (plain vanilla full
                        array plus stellar plus hdr) data - Loisa Rebull
                     */
                    if (readoutSelections.contains("sub")) {
                        hasIracFullArrayFilter = true;
                        iracFullArray = 0;
                    }
                    if (readoutSelections.contains("stellar")) {
                        hasIracStellarModeFilter = true;
                    }
                    if (readoutSelections.contains("hdr")) {
                        hasIracHDRFilter = true;
                    }
                }
                hasIracFilters = hasIracFullArrayFilter || hasIracStellarModeFilter || hasIracHDRFilter;
            }
        }

        // MIPS Filters
        if (mipsSelections != null) {
            String scaleSelections = req.getParam(MIPS_PHOT_SCALE_KEY);
            if (scaleSelections != null) {
                if (scaleSelections.equals("fine")) {
                    mipsScaleFine = 1;
                    hasMipsPhotScaleFilter = true;
                } else if (scaleSelections.equals("default")){
                    mipsScaleFine = 0;
                    hasMipsPhotScaleFilter = true;
                }
            }
            String scanSelections = req.getParam(MIPS_SCAN_RATE_KEY);
            if (scanSelections != null) {
                mipsScanRates = Arrays.asList(StringUtils.split(scanSelections, ","));
                // do not filter if all rates are included
                if (mipsScanRates.size() < 3) {
                    hasMipsScanRateFilter = true;
                }
            }
            hasMipsFilters = hasMipsPhotScaleFilter || hasMipsScanRateFilter;
        }

        if (iracSelections != null && irsSelections != null && mipsSelections != null) {
            if (iracSelections.contains(NONE) && irsSelections.contains(NONE) && mipsSelections.contains(NONE)) hasFilters = false;
            if (iracSelections.contains(ALL) && irsSelections.contains(ALL) && mipsSelections.contains(ALL) && !hasIracFilters && !hasMipsFilters) hasFilters = false;
        }

        if (!hasFilters) return;

        // find which reqmodes/wavelengths/instruments are requested
        reqmodes = new HashSet<String>();
        wavelengths = new HashSet<String>();
        instruments = new ArrayList<String>(3);
        int allWLNum = 0;
        int allRMNum = 0;
        if (parseSelections(iracSelections, iracDef)) {
            allWLNum += NUM_IRAC_WAVELENGTHS;
            allRMNum += NUM_IRAC_REQMODES;
            if (!reqmodes.contains(iracDef.get("map"))) {
                allWLNum -= 2; // only two possible wavelengths for IRAC PC    
            }
            instruments.add("IRAC");
        }
        if (parseSelections(irsSelections, irsDef)) {
            allWLNum += NUM_IRS_WAVELENGTHS;
            allRMNum += NUM_IRS_REQMODES;
            instruments.add("IRSX");
        }
        if (parseSelections(mipsSelections, mipsDef)) {
            allWLNum += NUM_MIPS_WAVELENGTHS;
            allRMNum += NUM_MIPS_REQMODES;
            instruments.add("MIPS");
        }


        hasWavelengthFilter = wavelengths.size() != 0 && wavelengths.size() != allWLNum;
        hasReqmodeFilter = reqmodes.size() != 0 && reqmodes.size() != allRMNum;
        hasInstrumentFilter = (!hasWavelengthFilter && !hasReqmodeFilter);


    }

    /**
     *
     * @param selections AOT and wavelength keys, selected by user, comma separated
     * @param defs hash map, which maps keys to reqmodes and wavelengths
     * @return true if selections contain any valid items, false otherwise
     */
    private boolean parseSelections(String selections, HashMap<String,String> defs) {
        if (selections == null || selections.equals(NONE)) return false;
        Collection<String> keys;
        if (selections.contains(ALL)) {
            keys = defs.keySet();
        } else {
            keys = Arrays.asList(StringUtils.split(selections, ","));
        }
        if (keys.size() < 1) return false;
        for (String key : keys) {
            String item = defs.get(key);
            if (item == null) throw new IllegalArgumentException("Invalid item in instrument filter:" + key);
            if (isReqMode(item)) {
                if (item.contains(SEPARATOR)) {
                    String [] ss = item.split(SEPARATOR);
                    reqmodes.addAll(Arrays.asList(ss));
                } else {
                    reqmodes.add(item);
                }
            } else {
                if (item.contains(SEPARATOR)) {
                    String [] ss = item.split(SEPARATOR);
                    wavelengths.addAll(Arrays.asList(ss));
                } else {
                    wavelengths.add(item);
                }
            }
        }
        return true;
    }

    private boolean isReqMode(String itemName){
        return AOT_LIST.contains(itemName);
    }

    public Collection<String> getReqmodes() {
        return reqmodes;
    }

    public String getReqmodesAsString() {
        return toQuotedStringList(reqmodes);
    }

    public Collection<String> getWavelengths() {
        return wavelengths;
    }

    public String getWavelengthsAsString() {
        return toQuotedStringList(wavelengths);
    }

    public Collection<String> getInstruments() {
        return instruments;
    }

    public String getInstrumentsAsString() {
        return toQuotedStringList(instruments);
    }
     

    public static String toQuotedStringList(Collection<String> c) {
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        for (String el : c) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append("\'").append(el).append("\'");
        }
        return sb.toString();

    }

    public String toString() {
        return "Instrument Filter: instruments ("+hasInstrumentFilter+") = "+ CollectionUtil.toString(instruments)+
                " reqmodes ("+hasReqmodeFilter+") = "+ CollectionUtil.toString(reqmodes)+
                " wavelengths ("+hasWavelengthFilter+") = "+CollectionUtil.toString(wavelengths);
    }

    public static boolean isDefinedOn(HeritageRequest req) {
        String f = req.getParam(FILTER_KEY);
        return !StringUtils.isEmpty(f) && f.equals("instrument");
    }

}
