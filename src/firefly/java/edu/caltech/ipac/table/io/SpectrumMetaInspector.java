package edu.caltech.ipac.table.io;


import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
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
public class SpectrumMetaInspector {
    private static final List<String> wlColNames= Arrays.asList(
            "wave", "wavelength", "wavelengths", "wl", "wls", "lambda", "lambda_eff", "v_lsr", "centralwavelng");
    private static final List<String> enColNames= Arrays.asList("ener", "energy", "lsr_velocity");
    private static final List<String> freqColNames= Arrays.asList("freq", "frequency", "nu");
    private static final String[] specColNames;
    private static final String[] waveLoColName= new String[] {"wave_lo"};
    private static final String[] waveHiColName= new String[] {"wave_hi"};
    private static final String[] fluxColNames= new String[] {"flux", "fluxdensity", "flux_density", "flux",
            "flx", "fl", "fls", "flu", "data", "value", "signal",
            "fullap and psf (two different extractions)", "c(lambda)",
            "t_mb", "t(k)", "main beam temperature",
            "flam", "pl_trandep", "especlipdep"};

    private static final String[] nsTransFluxColNames= new String[] {"pl_trandep"};
    private static final String[] nsTransErrColNames= new String[] {"pl_trandeperr"};
    private static final String[] nsTransErrHiColNames= new String[] {"pl_trandeperr1"};
    private static final String[] nsTransErrLowColNames= new String[] {"pl_trandeperr2"};

    private static final String[] nsDirFluxColNames= new String[] {"flam"};
    private static final String[] nsDirErrColNames= new String[] {"flamerr"};
    private static final String[] nsDirErrHiColNames= new String[] {"flamerr1"};
    private static final String[] nsDirErrLowColNames= new String[] {"flamerr2"};

    private static final String[] nsEclFluxColNames= new String[] {"especlipdep"};
    private static final String[] nsEclErrColNames= new String[] {"especlipdeperr"};
    private static final String[] nsEclErrHiColNames= new String[] {"especlipdeperr1"};
    private static final String[] nsEclErrLowColNames= new String[] {"especlipdeperr2"};


    private static final String NS_SPEC_TYPE_TRANS= "transmission";
    private static final String NS_SPEC_TYPE_ECLIPSE= "eclipse";
    private static final String NS_SPEC_TYPE_DIR= "direct imaging";


    private static final String[] errColNames= new String[] {"err", "error", "errors", "flerr", "flerrs", "flux_error", "flamerr"};
    private static final String[] errHiColNames= new String[] {"err_hi", "error_hi", "err_high", "error_high","flerr_high", "flerrs_hi", "flamerr1"};
    private static final String[] errLowColNames= new String[] {"err_lo", "error_lo", "err_low", "error_low","flerr_low", "flerrs_lo", "flamerr2"};
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

    public static void searchForSpectrum(DataGroup dg, TableServerRequest request) {
        searchForSpectrum(dg,hasSpectrumHint(request));
    }


    public static void searchForSpectrum(DataGroup dg, boolean spectrumHint) {
        searchForSpectrum(dg,null,spectrumHint);
    }

    public static boolean hasSpectrumHint(TableServerRequest request) {
        if (request==null) return false;
        var passedMetaInfo= request.getMeta();
        if (passedMetaInfo==null) return false;
        return passedMetaInfo.getOrDefault(MetaConst.DATA_TYPE_HINT,"").equalsIgnoreCase("spectrum");
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
            String voclass= h.getStringValue(VOCLASS);
            if (voclass==null) voclass= "";
            utype= FitsReadUtil.getUtype(h);
            if (utype==null && !spectrumHint &&
                    !voclass.equals(spec10Version) &&
                    !voclass.toLowerCase().startsWith("spectrum") ) {
                utype= SPEC_SPECTRUM;
            }
            if (utype!=null) {
                dg.getTableMeta().addKeyword(TableMeta.UTYPE,utype);
                return;
            }
        }
        else {
            utype= dg.getAttribute(TableMeta.UTYPE);
            if (utype==null && !spectrumHint) return;
        }

        List<DataType> dtAry= Arrays.asList(dg.getDataDefinitions());
        List<GroupInfo> groupInfosList= new ArrayList<>();
        var nonStandardSpecType= dg.getAttribute("SPEC_TYPE","").toLowerCase();
        var nonStandardFacility= dg.getAttribute("FACILITY","").toLowerCase();
        boolean foundX= false;
        boolean foundY= false;

        // look for wavelength
        if (hasCol(specColNames,dtAry, SPEC_SPECT_AXIS+".Value")) {
            List<GroupInfo.RefInfo> l= new ArrayList<>();
            addSpectrumRef(dg, dtAry,l);
            findAndAddRef(orderColNames,dtAry,l, SPEC_ORDER);
            findAndAddRef(relOrderColNames,dtAry,l, SPEC_REL_ORDER);
            findAndAddRef(waveLoColName,dtAry,l, SPEC_SPECT_AXIS+ACCURACY+".BinLow");
            findAndAddRef(waveHiColName,dtAry,l, SPEC_SPECT_AXIS+ACCURACY+".BinHigh");
            groupInfosList.add(new GroupInfo(SPEC_SPECT_AXIS, "",l));
            foundX= true;
        }

        // look for flux
        if (hasCol(fluxColNames,dtAry, SPEC_FL_AXIS+VALUE)) {
            addFluxColumn(groupInfosList,dtAry,nonStandardSpecType);
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

    public static void addFluxColumn(List<GroupInfo> groupInfosList, List<DataType> dtAry, String nonStandardSpecType) {
        List<GroupInfo.RefInfo> l= new ArrayList<>();
        boolean found= false;

        if (nonStandardSpecType.contains(NS_SPEC_TYPE_ECLIPSE)) nonStandardSpecType= NS_SPEC_TYPE_ECLIPSE;


        switch (nonStandardSpecType) {
            case NS_SPEC_TYPE_DIR:
                found= findAndAddRef(nsDirFluxColNames,dtAry,l, SPEC_FL_AXIS+VALUE);
                findAndAddRef(nsDirErrColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatError");
                findAndAddRef(nsDirErrLowColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrLow");
                findAndAddRef(nsDirErrHiColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrHigh");
                break;
            case NS_SPEC_TYPE_ECLIPSE:
                found= findAndAddRef(nsEclFluxColNames,dtAry,l, SPEC_FL_AXIS+VALUE);
                findAndAddRef(nsEclErrColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatError");
                findAndAddRef(nsEclErrLowColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrLow");
                findAndAddRef(nsEclErrHiColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrHigh");
                break;
            case NS_SPEC_TYPE_TRANS:
                found= findAndAddRef(nsTransFluxColNames,dtAry,l, SPEC_FL_AXIS+VALUE);
                findAndAddRef(nsTransErrColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatError");
                findAndAddRef(nsTransErrLowColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrLow");
                findAndAddRef(nsTransErrHiColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrHigh");
                break;
        }

        if (!found) {
            findAndAddRef(fluxColNames,dtAry,l, SPEC_FL_AXIS+VALUE);
            findAndAddRef(errColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatError");
            findAndAddRef(errLowColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrLow");
            findAndAddRef(errHiColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrHigh");
        }
        groupInfosList.add(new GroupInfo(SPEC_FL_AXIS, "", l));
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
        if (!groupList.isEmpty()) dg.setGroupInfos(groupList);
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

    private static boolean findAndAddRef(String [] possibleNames, List<DataType> dtAry,
                                      List<GroupInfo.RefInfo> list, String utype) {
        String colName= findColByUtypeOrNames(possibleNames,dtAry,utype);
        if (colName==null) return false;
        String ucd= getUCD(dtAry,colName);
        if (StringUtils.isEmpty(ucd)) ucd= "";
        list.add(new GroupInfo.RefInfo(colName, ucd,utype));
        return true;
    }

    private static void addSpectrumRef(DataGroup dg, List<DataType> dtAry, List<GroupInfo.RefInfo> list) {
        String utype= SPEC_SPECT_AXIS+VALUE;
        String colName= findColByUtypeOrNames(specColNames,dtAry,utype);
        if (colName==null) return;
        insertWlUnitsIfEmpty(dg,dtAry,colName);
        String ucd= getUCD(dtAry,colName);
        if (StringUtils.isEmpty(ucd)) ucd= guessSpectrumUCD(colName);
        list.add(new GroupInfo.RefInfo(colName, ucd,utype));
    }

    private static void insertWlUnitsIfEmpty(DataGroup dg, List<DataType> dtAry, String colName) {
        DataType specDt= dtAry.stream().filter(dt -> colName.equals(dt.getKeyName())).findAny().orElse(null);
        if (specDt==null || !StringUtils.isEmpty(specDt.getUnits())) return;
        // Add a list of metadata checks to set units, right now only one....
        if (anyMatchingAttribute(dg, "INSTRUM","IRSX")) { // IRSX attribute is always microns
            specDt.setUnits("microns");
            return;
        }
        // add more unit checks here
    }

    private static boolean anyMatchingAttribute(DataGroup dg, String partialKey, String partialValue) {
        var objAry= dg.getAttributeList().stream().filter( a -> a.getKey().contains(partialKey)).toArray();
        for (Object o : objAry) {
            if (o instanceof DataGroup.Attribute specAtt) {
                String v = specAtt.getValue();
                if (v != null && v.contains(partialValue)) return true;
            }
        }
        return false;
    }

    private static String find(List<String> l, String name) {
        return l.stream().filter(s -> s.equalsIgnoreCase(name)).findAny().orElse(null);
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