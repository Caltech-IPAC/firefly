/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


package edu.caltech.ipac.firefly.server.visualize.hips;

import edu.caltech.ipac.firefly.data.HttpResultInfo;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.hips.HiPSMasterListEntry.PARAMS;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.LinkInfo;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static edu.caltech.ipac.firefly.data.ServerParams.ENSURE_SOURCE;
import static edu.caltech.ipac.firefly.data.ServerParams.HIPS_LIST_SOURCE;
import static edu.caltech.ipac.firefly.data.ServerParams.HIPS_LIST_SOURCE_NAME;

/**
 * @author Cindy Wang
 */
@SearchProcessorImpl(id = "HiPSSearch", params =
        {@ParamDoc(name = ServerParams.HIPS_DATATYPES, desc = "types of HiPS data to search"),
         @ParamDoc(name = ServerParams.HIPS_SOURCES, desc = "HiPS sources"),
         @ParamDoc(name = HIPS_LIST_SOURCE, desc = "HiPS list source url"),
         @ParamDoc(name = ENSURE_SOURCE, desc = "if true then only ensure the source and an empty table is return"),
         @ParamDoc(name = ServerParams.ADHOC_SOURCE, desc = "a comma list of IVOA ids to make the source, the HIPS_SOURCE parameter much include adhoc"),
         @ParamDoc(name = ServerParams.SORT_ORDER, desc = "HiPS order, source based"),
         @ParamDoc(name = ServerParams.HIPS_TABLE_TYPE, desc = "hips or moc, default hips")
        })
public class HiPSMasterList extends EmbeddedDbProcessor {
    public final static String INFO_ICON_STUB = "<img data-src='info'/>";
    public static String[] HiPSDataType = new String[]{ServerParams.IMAGE, ServerParams.CUBE, ServerParams.CATALOG};

    private static final ConcurrentHashMap<String, HiPSMasterListSourceType> sources= new ConcurrentHashMap<>();
    public static String[] defaultSourceOrder = new String[]{ServerParams.IRSA, ServerParams.LSST, ServerParams.CDS};

    public static final List<String> noMocList = Arrays.asList(
            "ivo://CDS/P/HST/H",
            "ivo://CDS/P/ISOPHOT/170",
            "ivo://CDS/P/VISTA/VVV/DR4/ColorJYZ",
            "ivo://jvo/P/spcam/ALL",
            "ivo://jvo/P/spcam/W-A-Y",
            "ivo://jvo/P/spcam/W-C-IC",
            "ivo://jvo/P/spcam/W-C-RC",
            "ivo://jvo/P/spcam/W-J-B",
            "ivo://jvo/P/spcam/W-J-U",
            "ivo://jvo/P/spcam/W-J-V",
            "ivo://jvo/P/spcam/W-J-VR",
            "ivo://jvo/P/spcam/W-S-G+",
            "ivo://jvo/P/spcam/W-S-I+",
            "ivo://jvo/P/spcam/W-S-R+",
            "ivo://jvo/P/spcam/W-S-Z+",
            "ivo://jvo/P/spcam/W-S-ZB",
            "ivo://jvo/P/spcam/W-S-ZR",
            "ivo://nasa.heasarc/P/Swift/UVOT/int_U_UVW1_UVW2",
            "ivo://CDS/P/ZTF/DR7/color",
            "ivo://CDS/P/ZTF/DR7/g",
            "ivo://CDS/P/ZTF/DR7/i",
            "ivo://CDS/P/ZTF/DR7/r"
    );



    static {
        sources.put(ServerParams.IRSA, new IrsaHiPSListSource());
        sources.put(ServerParams.CDS, new CDSHiPSListSource());
        sources.put(ServerParams.LSST, new LsstHiPSListSource());
    }

    private static final Logger.LoggerImpl _log= Logger.getLogger();
    private static final String errMsg = "HiPS Map search: no HiPS maps found";

    public DataGroup fetchDataGroup(TableServerRequest request) throws DataAccessException {

        final String sourceUrl= request.getParam(HIPS_LIST_SOURCE);
        String sourceUrlName= request.containsParam(HIPS_LIST_SOURCE_NAME) ?
                request.getParam(HIPS_LIST_SOURCE_NAME) : sourceUrl;
        if (sourceUrl != null) {
            sources.computeIfAbsent(sourceUrlName, k -> new URLHiPSListSource(k,sourceUrl) );
        }
        if (request.getBooleanParam(ENSURE_SOURCE)) {
            return createDataGroupFromSources(sourceUrlName);
        }


        String hipsSources = request.getParam(ServerParams.HIPS_SOURCES);
        String hipsDataTypes = request.getParam(ServerParams.HIPS_DATATYPES);
        String hipsMergePriority = request.getParam(ServerParams.HIPS_MERGE_PRIORITY);
        String adhocSrcParam = request.getParam(ServerParams.ADHOC_SOURCE);
        boolean hipsTable = !"moc".equalsIgnoreCase(request.getParam(ServerParams.HIPS_TABLE_TYPE));
        String[] workingSources = (hipsSources != null) ? hipsSources.split(",") : null;
        String[] workingTypes = (hipsDataTypes != null) ? hipsDataTypes.split(",") : null;
        String[] prioritySources = (hipsMergePriority != null) ? hipsMergePriority.split(",") : null;
        List<HiPSMasterListEntry> allSourceData = new ArrayList<>();
        List<String> adhocSources= Collections.emptyList();

        if (workingSources!=null && Arrays.asList(workingSources).contains("adhoc") && adhocSrcParam!=null) {
            adhocSources= Arrays.asList(adhocSrcParam.split(","));
            workingSources= new String[] {ServerParams.ALL};
        }

        if (workingSources == null || workingSources.length == 0 ||
                (workingSources.length == 1 && workingSources[0].equalsIgnoreCase(ServerParams.ALL))) {
            workingSources = defaultSourceOrder.clone();
        }
        if (workingTypes == null || workingTypes.length == 0 ||
                (workingTypes.length == 1 && workingTypes[0].equalsIgnoreCase(ServerParams.ALL))) {
            workingTypes = new String[]{ServerParams.IMAGE, ServerParams.CUBE};
        }

        try {
            if (hipsTable) {
                HiPSMasterListEntry blankEntry= new HiPSMasterListEntry();
                blankEntry.set(PARAMS.SOURCE.getKey(), "irsa");
                blankEntry.set(PARAMS.URL.getKey(), "");
                blankEntry.set(PARAMS.TITLE.getKey(), "Blank HiPS Projection");
                blankEntry.set(PARAMS.TYPE.getKey(), ServerParams.IMAGE);
                allSourceData.add(blankEntry);
            }

            for (String source : workingSources) {
                HiPSMasterListSourceType hipsls = sources.get(source);

                if (hipsls != null) {
                    List<HiPSMasterListEntry> hipsL = hipsls.getHiPSListData(workingTypes, source);
                    if (hipsL != null) {
                        allSourceData.addAll(hipsL);
                    }
                }
            }

            if (allSourceData.isEmpty()) {
                throw new IOException(errMsg);
            }

            if (workingSources.length > 1 && prioritySources.length != 0) {
                allSourceData = mergeData(Arrays.asList(prioritySources), allSourceData, adhocSources);
            }


            DataGroup dg = createTableDataFromListEntry(allSourceData, hipsTable);

            setupMeta(dg, (workingSources.length > 1), hipsTable);
            return dg;
        } catch (Exception e) {
            _log.warn(e.getMessage());
            throw new DataAccessException(errMsg);
        }
    }

    private List<HiPSMasterListEntry> mergeData(List<String> prioritySources,
                                                List<HiPSMasterListEntry> allSourceData,
                                                List<String> adHocSources) {
        int totalS = allSourceData.size();
        HiPSMasterListEntry[] dataAry = new HiPSMasterListEntry[totalS];
        dataAry = allSourceData.toArray(dataAry);

        for (int i = 0; i < totalS; i++) {
            String ivo1 = dataAry[i].getMapInfo().get(PARAMS.IVOID.getKey());
            if (ivo1 == null) continue;

            String src1 = dataAry[i].getMapInfo().get(PARAMS.SOURCE.getKey());
            int n;

            // skip the entries from 'src1'
            for (n = i+1; n < totalS; n++) {
                if (!dataAry[n].getMapInfo().get(PARAMS.SOURCE.getKey()).equals(src1)) {
                    break;
                }
            }
            if (n >= totalS) break;

            // move to entries not from 'src1'
            for (int j = n; j < totalS; j++) {
                String ivo2 = dataAry[j].getMapInfo().get(PARAMS.IVOID.getKey());

                if (!ivo1.equals(ivo2)) continue;
                String src2 = dataAry[j].getMapInfo().get(PARAMS.SOURCE.getKey());
                String hsrc = getHigherPriority(src1, src2, prioritySources);

                if (hsrc == null) continue;
                if (hsrc.equals(src2)) {                           // skip to the one next to index i
                    dataAry[i].set(PARAMS.IVOID.getKey(), null);
                    break;
                } else {
                    dataAry[j].set(PARAMS.IVOID.getKey(), null);
                    for (int k = j+1; k < totalS; j = k, k++) {    // skip all entries from src2
                        if (!dataAry[k].getMapInfo().get(PARAMS.SOURCE.getKey()).equals(src2)) {
                            break;
                        }
                    }
                }
            }
        }

        List<HiPSMasterListEntry> newDataList = new ArrayList<>(allSourceData.size());
        for (HiPSMasterListEntry oneEntry : allSourceData) {
            Map<String,String> map= oneEntry.getMapInfo();
            String ivoaStr= map.get(PARAMS.IVOID.getKey());
            if (ivoaStr != null) {
                if (adHocSources.isEmpty()) {
                    newDataList.add(oneEntry);
                }
                else if (adHocSources.contains(ivoaStr)) {
                    map.put(PARAMS.SOURCE.getKey(), "adhoc");
                    newDataList.add(oneEntry);
                }
            }
        }
        return newDataList;
    }

    private String getHigherPriority(String src1, String src2, List<String> prioritySources) {
        if (src1.equals(src2)) return null;

        int idx1 = prioritySources.indexOf(src1);
        int idx2 = prioritySources.indexOf(src2);

        if (idx2 > idx1) {
            return idx1 == -1 ? src2 : src1;
        } else if (idx2 < idx1) {
            return idx2 == -1 ? src1 : src2;
        } else {
            return null;
        }
    }


    private void setupMeta(DataGroup dg, boolean bMulti, boolean hipsTable) {

        for (DataType colDT : dg.getDataDefinitions()) {
            String colName = colDT.getKeyName();

            if (colDT.getDataType() != String.class && colDT.getDataType()!= Boolean.class) continue;

            if ((!bMulti && colName.equals(PARAMS.SOURCE.getKey())) || colName.equals(PARAMS.URL.getKey())) {
                colDT.setVisibility(DataType.Visibility.hidden);
            }
            if (colName.equals(PARAMS.HAS_MOC.getKey())) colDT.setVisibility(DataType.Visibility.hide);
            if (!hipsTable && colName.equals(PARAMS.IVOID.getKey())) colDT.setVisibility(DataType.Visibility.hide);
            if (!hipsTable && colName.equals(PARAMS.SOURCE.getKey())) colDT.setVisibility(DataType.Visibility.hide);
        }

        // turn Properties column into a link.
        DataType col = dg.getDataDefintion("Properties");
        col.setWidth(4);
        col.setFilterable(false);
        col.setSortable(false);
        col.setLinkInfos(Collections.singletonList(new LinkInfo(null, INFO_ICON_STUB, "${Properties}", "link to HiPS properties", null, null, null)));
    }

    static boolean findHasMOCUsingLocalData= true; // this should always be true except for testing the services

    private static DataGroup createDataGroupFromSources(String sName) {
        DataType sourceId= new DataType("source","source", String.class);
        DataType url= new DataType("url","url", String.class);
        DataGroup dg = new DataGroup("HiPS list", new DataType[] {sourceId, url});
        HiPSMasterListSourceType sType= sources.get(sName);
        if (sName!=null && sources.containsKey(sName)) {
            DataObject row = new DataObject(dg);
            row.setDataElement(sourceId, sName);
            row.setDataElement(url, sType.getUrl());
            dg.add(row);
        }
        return dg;
    }

    private static DataGroup createTableDataFromListEntry(List<HiPSMasterListEntry> hipsMaps, boolean hipsTable) {
        List<DataType> cols = hipsTable ?
                HiPSMasterListEntry.getHiPSEntryColumns() : HiPSMasterListEntry.getMOCEntryColumns();
        DataGroup dg = new DataGroup("HiPS Maps", cols);

        for (HiPSMasterListEntry entry : hipsMaps) {
            DataObject row = new DataObject(dg);
            Map<String, String> mapInfo = entry.getMapInfo();

            for (DataType col : cols) {
                String val = mapInfo.get(col.getKeyName());
                row.setDataElement(col, col.convertStringToData(val));
            }
            boolean hasMoc= findHasMOCUsingLocalData ?
                    !noMocList.contains(entry.getMapInfo().get(PARAMS.IVOID.getKey())) : queryForHasMoc(entry);

            // change all the infrared to IR
            DataType wlKey= dg.getDataDefintion(PARAMS.WAVELENGTH.getKey());
            String wlstr= (String)row.getDataElement(wlKey);
            if (wlstr!=null && wlstr.toLowerCase().startsWith("infrared"))  row.setDataElement(wlKey,"IR");

             // make coverage a percent
            DataType fracKey= dg.getDataDefintion(PARAMS.FRACTION.getKey());
            Float f= (Float)row.getDataElement(fracKey);
            if (f!=null) row.setDataElement(fracKey,rnd(f * 100, 2, 5));

            if (hipsTable) {
                row.setDataElement(dg.getDataDefintion(PARAMS.HAS_MOC.getKey()), hasMoc);
                dg.add(row);
            }
            else {
                if (hasMoc) dg.add(row);
            }
        }
        return dg;
    }

    private static float rnd(float d, int placeIfOverOne, int placesIfunderOne) {
        int decimalPlaces= d>1 ? placeIfOverOne : placesIfunderOne;
        float factor= (float)Math.pow(10,decimalPlaces);
        return (float)Math.round(d*factor)/factor;
    }
    /**
     * query the services to see if the HiPS survey has a moc. Querying this 1000 times will take 5 or 6 minutes
     * Only for testing.
     */
    private static boolean queryForHasMoc(HiPSMasterListEntry entry) {
        String urlStr= entry.getMapInfo().get(PARAMS.URL.getKey()) + "/Moc.fits";
        try {
            HttpResultInfo r= URLDownload.getHeaderFromURL(new URL(urlStr), null, null, 1);
            return (r.getResponseCode()==200);
        } catch (FailedRequestException | MalformedURLException e) {
            return false;
        }
    }
}
