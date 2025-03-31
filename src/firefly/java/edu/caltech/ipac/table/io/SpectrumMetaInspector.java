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
            "flx", "fl", "fls", "flu", "data", "value", "signal", "SurfBrtness",
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


    private static final String[] errColNames= new String[] {
            "err", "error", "errors", "flerr", "flerrs",
            "flux_error", "flamerr", "err_Flux", "flux_error", "flux_unc"};
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
    private static final String spec10Version= "spectrum v1.0";
    private static final String VOCLASS= "VOCLASS";

    private static final String SPEC_SPECTRUM= "spec:Spectrum";
    private static final String SPEC_DATA= "spec:Data";
    private static final String SPEC_SPECTRUM_DATA= "spec:Spectrum.Data";
    private static final String SPEC= "spec:";

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
     * Look at the metadata for spectrum data model information. If found insert it into
     * the Data group.
     * <br>
     * spectrumHint only applies to non-FITS files
     * Guesser will look for columns that could possibly be spectral columns and build spectral data model
     * <br>
     * if fits file
     *    op 1: if claims to be a spectrum (utype or VOCLASS) then search the columns for the correct utypes, if found build spectral data model
     *    op 2: use guesser: always if no claim to be a spectrum
     * <br>
     * if a votable (we treat is as a votable if it has any groups)
     *    use guesser: only if no utype and spectrumHint is true, otherwise just return
     * <br>
     * if this file another type of table, probably ipac table
     *    op 1: if utype set, then search the columns for the correct utypes, if found build spectral data model
     *    op 2: if no spectrumHint and not utype then return
     *    op 3. use guesser: if spectrumHint and no utype
     *
     *
     *
     * @param dg the data group to insert spectral data model information.
     * @param hdu the HDU that is the source of this DataGroup
     * @param spectrumHint if true, then do not require the utype to be set in the file and do more guessing
     */
    public static void searchForSpectrum(DataGroup dg, BasicHDU<?> hdu, boolean spectrumHint) {

        String utype= (hdu==null) ? dg.getAttribute(TableMeta.UTYPE) : FitsReadUtil.getUtype(hdu.getHeader());
        if (hdu!=null) { // fits will either use guesser or look for UCDs only
            String voClass= hdu.getHeader().getStringValue(VOCLASS,"").toLowerCase();
            if (utype==null && (voClass.equals(spec10Version) || voClass.startsWith("spectrum") )) {
                utype= SPEC_SPECTRUM;
            }
        }
        else if (!spectrumHint) { // determine if we are to continue or just return
            var treatAsVoTable= hasSpecGroups(dg); // note- a VOTable does not need any further data model building
            if (treatAsVoTable || utype==null) return;
        }

        // if useOnlyUType is true then all the matching will be by utype (not guessing of columns)
        boolean useOnlyUType= utype!=null; // if table claims to be a spectrum, so only use utype

        // if I have reached this point in the code then I am trying to build the spectral model
        // the model will be built by either
        //    - guessing columns or checking utype
        //    - checking utype only

        List<DataType> dtAry= Arrays.asList(dg.getDataDefinitions());
        List<GroupInfo> groupInfosList= new ArrayList<>();
        var nonStandardSpecType= dg.getAttribute("SPEC_TYPE","").toLowerCase();
        var nonStandardFacility= dg.getAttribute("FACILITY","").toLowerCase();
        boolean foundX= false;
        boolean foundY= false;

        // look for wavelength
        if (hasCol(specColNames,dtAry, SPEC_SPECT_AXIS+".Value", useOnlyUType)) {
            List<GroupInfo.RefInfo> l= new ArrayList<>();
            addSpectrumRef(dg, dtAry,l, useOnlyUType);
            findAndAddRef(orderColNames,dtAry,l, SPEC_ORDER, useOnlyUType);
            findAndAddRef(relOrderColNames,dtAry,l, SPEC_REL_ORDER, useOnlyUType);
            findAndAddRef(waveLoColName,dtAry,l, SPEC_SPECT_AXIS+ACCURACY+".BinLow", useOnlyUType);
            findAndAddRef(waveHiColName,dtAry,l, SPEC_SPECT_AXIS+ACCURACY+".BinHigh", useOnlyUType);
            groupInfosList.add(new GroupInfo(SPEC_SPECT_AXIS, "",l));
            foundX= true;
        }

        // look for flux
        if (hasCol(fluxColNames,dtAry, SPEC_FL_AXIS+VALUE, useOnlyUType)) {
            addFluxColumn(groupInfosList,dg,dtAry,nonStandardSpecType, useOnlyUType);
            foundY= true;
        }

        // look for time
        if (hasCol(timeColNames,dtAry, SPEC_TI_AXIS+".Value", useOnlyUType)) {
            List<GroupInfo.RefInfo> l= new ArrayList<>();
            findAndAddRef(timeColNames,dtAry,l,  SPEC_TI_AXIS+VALUE, useOnlyUType);
            findAndAddRef(timeErrColNames,dtAry,l, SPEC_TI_AXIS_ACCURACY+".TimeError", useOnlyUType);
            findAndAddRef(timeErrLoColNames,dtAry,l, SPEC_TI_AXIS_ACCURACY+".TimeErrLow", useOnlyUType);
            findAndAddRef(timeErrHiColNames,dtAry,l, SPEC_TI_AXIS_ACCURACY+".TimeErrHigh", useOnlyUType);
            findAndAddRef(timeLoColName,dtAry,l, SPEC_TI_AXIS_ACCURACY+".BinLow", useOnlyUType);
            findAndAddRef(timeHiColName,dtAry,l, SPEC_TI_AXIS_ACCURACY+".BinHigh", useOnlyUType);
            groupInfosList.add(new GroupInfo(SPEC_TI_AXIS, "",l));
            foundY= true;
        }

        // finish - there must be two axis to insert anything
        if (!foundX || !foundY) return;

        var newGroupList= new ArrayList<>(dg.getGroupInfos());
        newGroupList.addAll(groupInfosList);
        dg.setGroupInfos(newGroupList);
        TableMeta meta= dg.getTableMeta();
        if (utype!=null) meta.addKeyword(TableMeta.UTYPE, utype.startsWith("spec:") ? utype : "spec:"+utype);
        else meta.addKeyword(TableMeta.UTYPE,SPEC_SPECTRUM);
    }

    public static boolean hasSpecGroups(DataGroup dg) {
        return dg.getGroupInfos().stream()
                .filter(g -> g!=null && g.getName()!=null && g.getName().toLowerCase().startsWith(SPEC))
                .anyMatch(g -> true);
    }

    public static void addFluxColumn(List<GroupInfo> groupInfosList, DataGroup dg,
                                     List<DataType> dtAry, String nonStandardSpecType, boolean useOnlyUType) {
        List<GroupInfo.RefInfo> l= new ArrayList<>();
        boolean found= false;

        if (nonStandardSpecType.contains(NS_SPEC_TYPE_ECLIPSE)) nonStandardSpecType= NS_SPEC_TYPE_ECLIPSE;


        switch (nonStandardSpecType) {
            case NS_SPEC_TYPE_DIR:
                found= findAndAddRef(nsDirFluxColNames,dtAry,l, SPEC_FL_AXIS+VALUE, false);
                findAndAddRef(nsDirErrColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatError", false);
                findAndAddRef(nsDirErrLowColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrLow", false);
                findAndAddRef(nsDirErrHiColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrHigh", false);
                break;
            case NS_SPEC_TYPE_ECLIPSE:
                found= findAndAddRef(nsEclFluxColNames,dtAry,l, SPEC_FL_AXIS+VALUE, false);
                findAndAddRef(nsEclErrColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatError", false);
                findAndAddRef(nsEclErrLowColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrLow", false);
                findAndAddRef(nsEclErrHiColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrHigh", false);
                break;
            case NS_SPEC_TYPE_TRANS:
                found= findAndAddRef(nsTransFluxColNames,dtAry,l, SPEC_FL_AXIS+VALUE, false);
                findAndAddRef(nsTransErrColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatError", false);
                findAndAddRef(nsTransErrLowColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrLow", false);
                findAndAddRef(nsTransErrHiColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrHigh", false);
                break;
        }

        if (!found) {
            addFluxRef(dg,dtAry,l,useOnlyUType);
            addErrorRef(dg,dtAry,l,useOnlyUType);
            findAndAddRef(errLowColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrLow", useOnlyUType);
            findAndAddRef(errHiColNames,dtAry,l, SPEC_FL_AXIS_ACCURACY+".StatErrHigh", useOnlyUType);
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


    private static String findColbyFallBack(String[] options, List<DataType> dtAry) {
        return dtAry.stream()
                .filter(dt -> dt.getUType()==null && find(Arrays.asList(options),dt.getKeyName())!=null)    // only guess at column if it does not have a utype
                .map(DataType::getKeyName)
                .findAny()
                .orElse(null);
    }

    private static String findColByUtypeList(List<DataType> dtAry, List<String> utypeList) {
        return dtAry.stream()
                .filter(dt -> dt.getUType() != null && utypeList.contains(dt.getUType()))
                .map(DataType::getKeyName)
                .findAny()
                .orElse(null);
    }

    private static String findColByUtype(List<DataType> dtAry, String utype) {
        var utypeList = new ArrayList<>(Collections.singletonList(utype));
        if (utype.startsWith(SPEC_DATA)) utypeList.add(utype.replace(SPEC_DATA, SPEC_SPECTRUM_DATA));
        return findColByUtypeList(dtAry, utypeList);
    }

    private static String findCol(List<DataType> dtAry, String utype,
                                  String[] possibleNames, boolean useOnlyUType) {
        String utypeCol= findColByUtype(dtAry,utype);
        if (useOnlyUType || utypeCol!=null) return utypeCol;
        return findColbyFallBack(possibleNames,dtAry);
    }

    private static boolean hasCol(String[] possibleNames, List<DataType> dtAry, String utype, boolean useOnlyUtype) {
        return findCol(dtAry,utype,possibleNames,useOnlyUtype)!=null;
    }

    private static String getUCD(List<DataType> dtAry, String colName) {
        if (colName==null) return null;
        DataType foundDt= dtAry.stream().filter(dt -> colName.equals(dt.getKeyName())).findAny().orElse(null);
        return (foundDt!=null) ? foundDt.getUCD() : null;
    }

    private static boolean findAndAddRef(String [] possibleNames, List<DataType> dtAry,
                                      List<GroupInfo.RefInfo> list, String utype, boolean useOnlyUtype) {
        String colName= findCol(dtAry,utype,possibleNames,useOnlyUtype);
        if (colName==null) return false;
        String ucd= getUCD(dtAry,colName);
        if (StringUtils.isEmpty(ucd)) ucd= "";
        list.add(new GroupInfo.RefInfo(colName, ucd,utype));
        return true;
    }

    private static void addSpectrumRef(DataGroup dg, List<DataType> dtAry, List<GroupInfo.RefInfo> list, boolean useOnlyUtype) {
        String utype= SPEC_SPECT_AXIS+VALUE;
        String colName= findCol(dtAry,utype,specColNames,useOnlyUtype);
        if (colName==null) return;
        insertWlUnitsIfEmpty(dg,dtAry,colName);
        String ucd= getUCD(dtAry,colName);
        if (StringUtils.isEmpty(ucd)) ucd= guessSpectrumUCD(colName);
        list.add(new GroupInfo.RefInfo(colName, ucd,utype));
    }

    private static void addErrorRef(DataGroup dg, List<DataType> dtAry, List<GroupInfo.RefInfo> list, boolean useOnlyUType) {
        String utype= SPEC_FL_AXIS_ACCURACY+".StatError";
        String colName= findCol(dtAry,utype,errColNames,useOnlyUType);
        if (colName==null) return;
        insertErrorUnitsIfEmpty(dg,dtAry,colName);
        String ucd= getUCD(dtAry,colName);
        if (StringUtils.isEmpty(ucd)) ucd= "";
        list.add(new GroupInfo.RefInfo(colName, ucd,utype));
    }

    private static void addFluxRef(DataGroup dg, List<DataType> dtAry, List<GroupInfo.RefInfo> list, boolean useOnlyUType) {
        String utype= SPEC_FL_AXIS+VALUE;
        String colName= findCol(dtAry,utype,fluxColNames,useOnlyUType);
        if (colName==null) return;
        insertFluxUnitsIfEmpty(dg,dtAry,colName);
        String ucd= getUCD(dtAry,colName);
        if (StringUtils.isEmpty(ucd)) ucd= "";
        list.add(new GroupInfo.RefInfo(colName, ucd,utype));
    }


    private static void insertWlUnitsIfEmpty(DataGroup dg, List<DataType> dtAry, String colName) {
        DataType specDt= getDataTypeWithColName(dtAry,colName);
        if (specDt==null || !StringUtils.isEmpty(specDt.getUnits())) return;
        // a list of metadata checks to set units, this can keep growing over time
        if (anyMatchingAttribute(dg, "INSTRUM","IRSX")) { // IRSX attribute is always microns
            specDt.setUnits("microns");
        }
        else if (anyMatchingAttribute(dg, "Instrument","SPIRE") || anyMatchingAttribute(dg, "Instrument","PACS")) {
            setUnit(dg,specDt,"waveunit", "microns");
        }
        // add more unit checks here
    }

    private static void insertFluxUnitsIfEmpty(DataGroup dg, List<DataType> dtAry, String colName) {
        DataType fluxDt= getDataTypeWithColName(dtAry,colName);
        if (fluxDt==null || !StringUtils.isEmpty(fluxDt.getUnits())) return;
        // a list of metadata checks to set units, this can keep growing over time
        if (anyMatchingAttribute(dg, "Instrument","SPIRE")) { // IRSX attribute is always microns
            setUnit(dg,fluxDt,"surfacebrightnessunit", "Jy/arcsec^2");
        }
        else if (anyMatchingAttribute(dg, "Instrument","PACS")) {
            setUnit(dg,fluxDt,"fluxunit", "Jy");
        }
        // add more unit checks here
    }

    private static void insertErrorUnitsIfEmpty(DataGroup dg, List<DataType> dtAry, String colName) {
        DataType fluxDt= getDataTypeWithColName(dtAry,colName);
        if (fluxDt==null || !StringUtils.isEmpty(fluxDt.getUnits())) return;
            // a list of metadata checks to set units, this can keep growing over time
        if (anyMatchingAttribute(dg, "Instrument","PACS")) {
            setUnit(dg,fluxDt,"errorunit", "Jy");
        }
        // add more unit checks here
    }

    private static DataType getDataTypeWithColName(List<DataType> dtAry, String colName) {
        return dtAry.stream().filter(dt -> colName.equals(dt.getKeyName())).findAny().orElse(null);
    }

    private static void setUnit(DataGroup dg, DataType dt, String unitAttr, String def) {
        dt.setUnits(dg.getAttribute(unitAttr,def));
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