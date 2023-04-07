package edu.caltech.ipac.table.io;


import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.GroupInfo;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Header;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility to search for Spectrum Data Model information from the meta of a FITS table
 */
class SpectrumMetaInspector {
    private static final List<String> wlColNames= Arrays.asList("wave", "wavelength", "wavelengths", "wl", "wls");
    private static final List<String> enColNames= Arrays.asList("ener", "energy");
    private static final List<String> freqColNames= Arrays.asList("freq", "frequency");
    private static final String[] specColNames;
    private static final String[] waveLoColName= new String[] {"wave_lo"};
    private static final String[] waveHiColName= new String[] {"wave_hi"};
    private static final String[] fluxColNames= new String[] {"flux", "fluxdensity", "flux_density", "flux", "flx", "fl", "fls", "flu", "data", "value"};
    private static final String[] errColNames= new String[] {"err", "error", "errors", "flerr", "flerrs"};
    private static final String[] errHiColNames= new String[] {"err_hi", "error_hi", "err_high", "error_high","flerr_high", "flerrs_hi"};
    private static final String[] errLowColNames= new String[] {"err_lo", "error_lo", "err_low", "error_low","flerr_low", "flerrs_lo"};
    private static final String[] orderColNames= new String[] {"order", "ord", "spec_order"};
    private static final String[] relOrderColNames= {};
    private static final String[] timeColNames= new String[] {"time", "ti"};
    private static final String[] timeErrColNames= new String[] {"time_err", "ti_err", "terr", "terrs"};
    private static final String[] timeErrLoColNames= new String[] {"time_elo", "ti_elo"};
    private static final String[] timeErrHiColNames= new String[] {"time_ehi", "ti_ehi"};
    private static final String[] timeLoColName= new String[] {"time_lo"};
    private static final String[] timeHiColName= new String[] {"time_hi"};

    private static final String VALUE= ".Value";
    private static final String ACCURACY= ".Accuracy";
    private static final String SPEC_SPECT_AXIS= "spec:Data.SpectralAxis";
    private static final String SPEC_FL_AXIS= "spec:Data.FluxAxis";
    private static final String SPEC_FL_AXIS_ACCURACY= SPEC_FL_AXIS+ACCURACY;
    private static final String SPEC_TI_AXIS= "spec:Data.TimeAxis";
    private static final String SPEC_TI_AXIS_ACCURACY= SPEC_TI_AXIS+ACCURACY;
    private static final String SPEC_ORDER = "spec:Data.SpectralAxis.Order";
    private static final String SPEC_REL_ORDER= "spec:Data.SpectralAxis.RelOrder";
    private static final String spec10Version= "Spectrum v1.0";
    private static final String VOCLASS= "VOCLASS";

    private static final String SPEC_SPECTRUM= "spec:Spectrum";

    static {
        List<String> l= new ArrayList<>(wlColNames);
        l.addAll(enColNames);
        l.addAll(freqColNames);
        specColNames= l.toArray(new String[0]);
    }

    public static void searchForSpectrum(DataGroup dg, boolean spectrumHint) {
        searchForSpectrum(dg,null,spectrumHint);
    }

    /**
     * Look at the FITS meta data for spectrum data model information. If found insert it into
     * the Data group
     * @param dg the data group to insert spectral data model information.
     * @param hdu the HDU that is the source of this DataGroup
     * @param spectrumHint if true, then do not require the utype to be set in the file and do more guessing
     */
    public static void searchForSpectrum(DataGroup dg, BasicHDU<?> hdu, boolean spectrumHint) {

        String utype;
        if (hdu!=null) {
            Header h= hdu.getHeader();
            utype= FitsReadUtil.getUtype(h);
            String voclass= h.getStringValue(VOCLASS);
            if (voclass==null) voclass= "";
            if (utype==null && !spectrumHint &&
                    !voclass.equals(spec10Version) &&
                    !voclass.toLowerCase().startsWith("spectrum") ) return;
        }
        else {
            utype= dg.getAttribute(TableMeta.UTYPE);
            if (utype==null && !spectrumHint) return;
        }

        List<DataType> dtAry= Arrays.asList(dg.getDataDefinitions());
        List<GroupInfo> groupInfosList= new ArrayList<>();
        boolean foundX= false;
        boolean foundY= false;

        // look for wavelength
        if (hasCol(specColNames,dtAry, SPEC_SPECT_AXIS+".Value")) {
            List<GroupInfo.RefInfo> l= new ArrayList<>();
            addSpectrumRef(dtAry,l);
            findAndAddRef(orderColNames,dtAry,l, SPEC_ORDER);
            findAndAddRef(relOrderColNames,dtAry,l, SPEC_REL_ORDER);
            findAndAddRef(waveLoColName,dtAry,l, SPEC_SPECT_AXIS+ACCURACY+".BinLow");
            findAndAddRef(waveHiColName,dtAry,l, SPEC_SPECT_AXIS+ACCURACY+".BinHigh");
            groupInfosList.add(new GroupInfo(SPEC_SPECT_AXIS, "",l));
            foundX= true;
        }

        // look for flux
        if (hasCol(fluxColNames,dtAry, SPEC_FL_AXIS+VALUE)) {
            List<GroupInfo.RefInfo> l= new ArrayList<>();
            findAndAddRef(fluxColNames,dtAry,l, SPEC_FL_AXIS+VALUE);
            findAndAddRef(errColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatError");
            findAndAddRef(errLowColNames,dtAry,l, SPEC_FL_AXIS+".StatErrLow");
            findAndAddRef(errHiColNames,dtAry,l, SPEC_FL_AXIS+".StatErrHigh");
            groupInfosList.add(new GroupInfo(SPEC_FL_AXIS, "", l));
            foundY= true;
        }

        // look for time
        if (hasCol(timeColNames,dtAry, SPEC_TI_AXIS+".Value")) {
            List<GroupInfo.RefInfo> l= new ArrayList<>();
            findAndAddRef(timeColNames,dtAry,l,  SPEC_TI_AXIS+VALUE);
            findAndAddRef(timeErrColNames,dtAry,l, SPEC_TI_AXIS_ACCURACY+".TimeError");
            findAndAddRef(timeErrLoColNames,dtAry,l, SPEC_TI_AXIS_ACCURACY+".TimeErrLow");
            findAndAddRef(timeErrHiColNames,dtAry,l, SPEC_TI_AXIS_ACCURACY+".TimeErrHigh");
            findAndAddRef(timeLoColName,dtAry,l, SPEC_TI_AXIS_ACCURACY+".BinLow");
            findAndAddRef(timeHiColName,dtAry,l, SPEC_TI_AXIS_ACCURACY+".BinHigh");
            groupInfosList.add(new GroupInfo(SPEC_TI_AXIS, "",l));
            foundY= true;
        }

        // finish - there must be two axis to insert anything
        if (!foundX || !foundY) return;

        dg.setGroupInfos(groupInfosList);
        TableMeta meta= dg.getTableMeta();
        if (utype!=null) meta.addKeyword(TableMeta.UTYPE, utype.startsWith("spec:") ? utype : "spec:"+utype);
        else meta.addKeyword(TableMeta.UTYPE,SPEC_SPECTRUM);
    }

    public static void createSpectrumMeta(DataGroup dg, String wlColName, String fluxColName, String errColName) {
        dg.getTableMeta().addKeyword(TableMeta.UTYPE, SPEC_SPECTRUM);
        List<GroupInfo> groupList= new ArrayList<>();
        if (wlColName!=null) {
            GroupInfo.RefInfo wlref= new GroupInfo.RefInfo(
                    wlColName, guessSpectrumUCD(wlColName), SPEC_SPECT_AXIS+VALUE);
            GroupInfo wlGr = new GroupInfo(SPEC_SPECT_AXIS, "", Collections.singletonList(wlref));
            groupList.add(wlGr);
        }

        if (fluxColName!=null) {
            List<GroupInfo.RefInfo> fcList= new ArrayList<>();
            fcList.add(new GroupInfo.RefInfo(fluxColName, "", SPEC_FL_AXIS+VALUE));
            if (errColName!=null) fcList.add(new GroupInfo.RefInfo(errColName, "",  SPEC_FL_AXIS_ACCURACY+".StatError"));
            GroupInfo fluxGr = new GroupInfo(SPEC_FL_AXIS, "", fcList);
            groupList.add(fluxGr);
        }
        if (groupList.size()>0) dg.setGroupInfos(groupList);
    }


    private static String findCol(String[] options, List<DataType> dtAry) {
        return dtAry.stream()
                .filter(dt -> dt.getUType()==null && find(Arrays.asList(options),dt.getKeyName())!=null)    // only guess at column if it does not have a utype
                .map(DataType::getKeyName)
                .findAny()
                .orElse(null);
    }

    private static String findColByUtypeOrNames(String[] possibleNames, List<DataType> dtAry, String utype) {
        return dtAry.stream()
                .filter(dt -> dt.getUType()!=null && dt.getUType().equals(utype))
                .map(DataType::getKeyName)
                .findAny()
                .orElse(findCol(possibleNames,dtAry));
    }

    private static boolean hasCol(String[] possibleNames, List<DataType> dtAry, String utype) {
        return findColByUtypeOrNames(possibleNames,dtAry,utype)!=null;
    }

    private static String getUCD(List<DataType> dtAry, String colName) {
        if (colName==null) return null;
        DataType foundDt= dtAry.stream().filter(dt -> colName.equals(dt.getKeyName())).findAny().orElse(null);
        return (foundDt!=null) ? foundDt.getUCD() : null;
    }

    private static void findAndAddRef(String [] possibleNames, List<DataType> dtAry,
                                      List<GroupInfo.RefInfo> list, String utype) {
        String colName= findColByUtypeOrNames(possibleNames,dtAry,utype);
        if (colName==null) return;
        String ucd= getUCD(dtAry,colName);
        if (StringUtils.isEmpty(ucd)) ucd= "";
        list.add(new GroupInfo.RefInfo(colName, ucd,utype));
    }

    private static void addSpectrumRef(List<DataType> dtAry, List<GroupInfo.RefInfo> list) {
        String utype= SPEC_SPECT_AXIS+VALUE;
        String colName= findColByUtypeOrNames(specColNames,dtAry,utype);
        if (colName==null) return;
        String ucd= getUCD(dtAry,colName);
        if (StringUtils.isEmpty(ucd)) ucd= guessSpectrumUCD(colName);
        list.add(new GroupInfo.RefInfo(colName, ucd,utype));
    }

    private static String find(List<String> l, String name) {
        return l.stream().filter(s -> s.equals(name)).findAny().orElse(null);
    }

    private static boolean includes(List<String> l, String v) {
        return find(l,v)!=null;
    }

    private static String guessSpectrumUCD(String colName) {
        if (includes(wlColNames,colName)) return "WAVE";
        else if (includes(enColNames,colName)) return "ENER";
        else if (includes(freqColNames,colName)) return "FREQ";
        return null;
    }
}