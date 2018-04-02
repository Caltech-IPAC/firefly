/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


package edu.caltech.ipac.firefly.server.visualize.hips;

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.visualize.hips.HiPSMasterListEntry.PARAMS;

import java.io.File;
import java.util.*;
import java.io.IOException;

import static edu.caltech.ipac.util.IpacTableUtil.VISI_TAG;
import static edu.caltech.ipac.util.IpacTableUtil.WIDTH_TAG;
import static edu.caltech.ipac.util.IpacTableUtil.makeAttribKey;

/**
 * @author Cindy Wang
 */
@SearchProcessorImpl(id = "HiPSSearch", params =
        {@ParamDoc(name = ServerParams.HIPS_DATATYPES, desc = "types of HiPS data to search"),
         @ParamDoc(name = ServerParams.HIPS_SOURCES, desc = "HiPS sources"),
         @ParamDoc(name = ServerParams.SORT_ORDER, desc = "HiPS order, source based")
        })
public class HiPSMasterList extends EmbeddedDbProcessor {
    public static String[] HiPSDataType = new String[]{ServerParams.IMAGE,
                                                       ServerParams.CUBE,
                                                       ServerParams.CATALOG};

    private static Map<String, HiPSMasterListSourceType> sources= new HashMap<>();
    public static String[] defaultSourceOrder = new String[]{ServerParams.CDS, ServerParams.IRSA, ServerParams.LSST,
                                                            ServerParams.EXTERNAL};

    static {
        sources.put(ServerParams.IRSA, new IrsaHiPSListSource());
        sources.put(ServerParams.CDS, new CDSHiPSListSource());
        sources.put(ServerParams.EXTERNAL, new ExternalHiPSListSource());
        sources.put(ServerParams.LSST, new LsstHiPSListSource());
    }

    private static final Logger.LoggerImpl _log= Logger.getLogger();

    public FileInfo ingestDataIntoDb(TableServerRequest request, File dbFile) throws DataAccessException {
        String hipsSources = request.getParam(ServerParams.HIPS_SOURCES);
        String sortOrder = request.getParam(ServerParams.SORT_ORDER);
        String hipsDataTypes = request.getParam(ServerParams.HIPS_DATATYPES);
        String workingSources[] = (hipsSources != null) ? hipsSources.split(",") : null;
        String workingOrder[] = (sortOrder != null) ? sortOrder.split(",") : null;
        String workingTypes[] = (hipsDataTypes != null) ? hipsDataTypes.split(",") : null;
        List<String> orderedSources = new ArrayList<>();
        List<HiPSMasterListEntry> allSourceData = new ArrayList<>();
        DbAdapter dbAdapter = DbAdapter.getAdapter(request);

        if (workingSources == null || workingSources.length == 0 ||
                (workingSources.length == 1 && workingSources[0].equalsIgnoreCase(ServerParams.ALL))) {
            workingSources = sources.keySet().toArray(new String[sources.size()]);
        }
        if (workingTypes == null || workingTypes.length == 0 ||
                (workingTypes.length == 1 && workingTypes[0].equalsIgnoreCase(ServerParams.ALL))) {
            workingTypes = new String[]{ServerParams.IMAGE, ServerParams.CUBE};
        }

        // define the sources in searching order based on defined order or default source order
        if (workingOrder == null || workingOrder.length == 0 ||
                (workingOrder.length == 1 && workingOrder[0].equalsIgnoreCase(ServerParams.ALL))) {
            // add elements defined in sources in default order
            for (int i = 0; i < defaultSourceOrder.length; i++) {
                if (Arrays.asList(workingSources).contains(defaultSourceOrder[i])) {
                    orderedSources.add(defaultSourceOrder[i]);
                }
            }
        } else {
            // add element defined in sort order first
            for (int i = 0; i < workingOrder.length; i++) {
                if (Arrays.asList(workingSources).contains(workingOrder[i])) {
                    orderedSources.add(workingOrder[i]);
                }
            }
            // add elements defined in sources but not defined in sort order
            for (int i = 0; i < defaultSourceOrder.length; i++) {
                if (!orderedSources.contains(defaultSourceOrder[i]) &&
                        Arrays.asList(workingSources).contains(defaultSourceOrder[i])) {
                    orderedSources.add(defaultSourceOrder[i]);
                }
            }
        }

        try {

            for (String source : orderedSources) {
                HiPSMasterListSourceType hipsls = sources.get(source);

                if (hipsls != null) {
                    List<HiPSMasterListEntry> hipsL = hipsls.getHiPSListData(workingTypes, source);
                    if (hipsL != null) {
                        allSourceData.addAll(hipsL);
                    }
                }
            }

            if (allSourceData.size() == 0) {
                throw new IOException("[HiPS_LIST]: no HiPS found or HiPS service connection error");
            }

            DataGroup dg = createTableDataFromListEntry(allSourceData);
            dg.shrinkToFitData();

            setupMeta(dg, (orderedSources.size() > 1));

            FileInfo finfo = EmbeddedDbUtil.ingestDataGroup(dbFile, dg, dbAdapter, "data");
            return finfo;
        } catch (Exception e) {
            String msg = e.getMessage();

            if (!msg.startsWith("[HiPS_")) {
                msg = "[HiPS_LIST]: " + msg;
            }
            throw new DataAccessException(msg);
        }
    }

    private List<HiPSMasterListEntry> cleanHiPSList(List<HiPSMasterListEntry> listToClean, List<String> ids) {
        List<HiPSMasterListEntry> cleanedList = new ArrayList<>();

        for (HiPSMasterListEntry oneEntry : listToClean) {
            if (!ids.contains(oneEntry.getMapInfo().get(PARAMS.ID.getKey()))) {
                cleanedList.add(oneEntry);
            }
        }
        return cleanedList;
    }

    private void setupMeta(DataGroup dg, boolean bMulti) {
        int    sWidth = 30;

        for (DataType colDT : dg.getDataDefinitions()) {
            String colName = colDT.getKeyName();

            if (colDT.getDataType() != String.class) continue;

            int crtWidth = colDT.getFormatInfo().getWidth();
            if (crtWidth > sWidth && !colName.equals(PARAMS.PROPERTIES.getKey())) {
                dg.addAttribute(makeAttribKey(WIDTH_TAG, colName), Integer.toString(sWidth));
            }

            if ((!bMulti && colName.equals(PARAMS.SOURCE.getKey())) || colName.equals(PARAMS.URL.getKey())) {
                dg.addAttribute(makeAttribKey(VISI_TAG, colName), "hidden");
            }
        }
    }


    public static File createFile(String[] dataTypes, String fileExt, String hipsSource) throws IOException {
        String filePrefix = hipsSource + "_" + String.join("_", dataTypes);
        File file = null;

        file = File.createTempFile(filePrefix, fileExt, ServerContext.getPermWorkDir());
        return file;
    }

    private static DataGroup createTableDataFromListEntry(List<HiPSMasterListEntry> hipsMaps) {
        List<DataType> cols = HiPSMasterListEntry.getHiPSEntryColumns();
        DataGroup dg = new DataGroup("HiPS Maps", cols);

        for (HiPSMasterListEntry entry : hipsMaps) {
            DataObject row = new DataObject(dg);
            Map<String, String> mapInfo = entry.getMapInfo();

            for (DataType col : cols) {
                String val = mapInfo.get(col.getKeyName());

                row.setDataElement(col, col.convertStringToData(val));
            }
            dg.add(row);
        }
        return dg;
    }
}


