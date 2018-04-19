package edu.caltech.ipac.firefly.server.visualize.hips;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.hips.HiPSMasterListEntry.PARAMS;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.firefly.server.util.DsvToDataGroup;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.firefly.server.servlets.AnyFileDownload;
import org.apache.commons.csv.CSVFormat;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.StringBuilder;
import java.io.FileReader;


/**
 * Created by cwang on 2/27/18.
 */
public class IrsaHiPSListSource implements HiPSMasterListSourceType {
    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final String irsaHiPSSource = AppProperties.getProperty("irsa.hips.list.source", "file");
    private static final String irsaHipsTable = "/edu/caltech/ipac/firefly/resources/irsa-hips-master-table.csv";
    private static final String irsaHipsUrl = "https://irsa.ipac.caltech.edu/data/hips/list";
    private static final String irsaHiPSListFrom = irsaHiPSSource.equals("file") ?
                                                    AppProperties.getProperty("irsa.hips.masterFile",irsaHipsTable):
                                                    AppProperties.getProperty("irsa.hips.masterUrl", irsaHipsUrl);
    private static int TIMEOUT  = new Integer( AppProperties.getProperty("HiPS.timeoutLimit" , "30")).intValue();
    private static String PROP = PARAMS.PROPERTIES.getKey().toLowerCase();

    private static Map<String, String> paramsMap = new HashMap<>();
    static {
            HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.IVOID, "creator_did");
            HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.URL, "hips_service_url");
            HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.TITLE, "obs_title");
            HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.ORDER, "hips_order");
            HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.TYPE, "dataproduct_type");
            HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.FRACTION, "moc_sky_fraction");
            HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.FRAME, "hips_frame");
            HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.WAVELENGTH, "obs_regime");
            HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.RELEASEDATE, "hips_release_date");
            HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.PIXELSCALE, "hips_pixel_scale");
    }
    //private static String[] mandatoryKeys = new String[]{ PARAMS.ID.key, PARAMS.URL.key, PARAMS.RELEASEDATE.key, PARAMS.STATUS.key};

    public List<HiPSMasterListEntry> getHiPSListData(String[] dataTypes, String source) {
        try {

            if (!Arrays.asList(dataTypes).contains(ServerParams.IMAGE)) return null;

            if (irsaHiPSSource.equals("file")) {
                return createHiPSListFromFile(irsaHiPSListFrom, dataTypes, source);
            } else {
                return createHiPSListFromUrl(irsaHiPSListFrom, source, paramsMap, true, null);
            }
        }
        catch (FailedRequestException | IOException e) {
            _log.warn("get " + source + " HiPS failed - " + e.getMessage());
            return null;
        } catch (Exception e) {
            _log.warn("get " + source + " HiPS failed - " + e.getMessage());
            return null;
        }

    }

    public static List<HiPSMasterListEntry> createHiPSListFromUrl(String url, String source,
                                                        Map<String, String> keyMap, boolean bPropCall, String childExt)
                                                                                                throws IOException  {
        try{
            _log.briefDebug("executing " + source + " url query: " + url);

            long cTime = System.currentTimeMillis();

            File listFile = AnyFileDownload.retrieveHiPSData(url, childExt);

            _log.briefDebug("get " + source + " HiPS took " + (System.currentTimeMillis() - cTime) + "ms");

            // Open the file that is the first command line parameter
            BufferedReader br = new BufferedReader(new FileReader(listFile));
            String strLine;
            HiPSMasterListEntry oneList;
            List<HiPSMasterListEntry> lists = new ArrayList<>();

            Properties newProp;
            StringBuilder propLine = new StringBuilder();


            //Read from the link Line By Line

            while ((strLine = br.readLine()) != null) {
                String tLine = strLine.trim();
                if (tLine.startsWith("#")) continue;    // comment line
                if (tLine.length() == 0) {      // end of a HiPS block
                    newProp = startNewProperties(propLine.toString());
                    if (newProp != null) {
                        oneList = propertiesToListEntry(newProp, keyMap, source, bPropCall);
                        if (oneList != null) lists.add(oneList);
                    }
                    propLine = new StringBuilder();
                } else {
                    propLine.append(tLine+"\n");
                }
            }

            //Close the input stream
            newProp = startNewProperties(propLine.toString());
            if (newProp != null) {
                oneList = propertiesToListEntry(newProp, keyMap, source, bPropCall);
                if (oneList != null) lists.add(oneList);
            }
            br.close();
            return lists;
        } catch (Exception e){//Catch exception if any
            throw new IOException("[HiPS_LIST]:" + e.getMessage());
        }
    }

    private static Properties startNewProperties(String pLine)  throws IOException {
        if (pLine.length() == 0) return null;

        Properties newProp = new Properties();
        newProp.load(new StringReader(pLine));

        return newProp;
    }

    private static HiPSMasterListEntry propertiesToListEntry(Properties newProp,
                                                              Map<String, String> keyMap, String source, boolean bProp)
                                                throws IOException {

        HiPSMasterListEntry oneList = new HiPSMasterListEntry();

        addItemsToListEntry(keyMap, newProp, oneList);

        int colCount = oneList.getMapInfo().size();

        if (colCount == 0) {
            return null;
        } else {
            int totalCount = keyMap.size();

            oneList.set(PARAMS.SOURCE.getKey(), source);
            String url = oneList.getMapInfo().get(PARAMS.URL.getKey());

            if (url != null) {
                String pUrl = getPropertyUrl(url);

                oneList.set(PARAMS.PROPERTIES.getKey(), getPropertyUrl(url));
                if (bProp && (colCount < totalCount) && (pUrl != null)) {
                    addItemsFromProperties(oneList, keyMap);
                }

            }

            return oneList;
        }
    }

    private static void addItemsToListEntry(Map<String, String> keyMap, Properties prop, HiPSMasterListEntry oneList ) {

        for (Map.Entry<String, String> entry : keyMap.entrySet()) {
            if (oneList.getMapInfo().get(entry.getKey()) != null) continue;

            String[] propSet = entry.getValue().split(",");

            for (String s : propSet) {
                String v = prop.getProperty(s);

                if (v != null) {
                    oneList.set(entry.getKey(), v);
                    break;
                }
            }
        }
    }

    private static void addItemsFromProperties(HiPSMasterListEntry listEntry, Map<String, String> keyMap)
                                                throws IOException {
        String propUrl = listEntry.getMapInfo().get(PARAMS.PROPERTIES.getKey());

        if (propUrl == null || listEntry == null) return;

        try {
            File propFile = AnyFileDownload.retrieveHiPSData(propUrl, null);
            BufferedReader br = new BufferedReader(new FileReader(propFile));
            String strLine;
            StringBuilder sb = new StringBuilder();

            while ((strLine = br.readLine()) != null) {
                String tLine = strLine.trim();
                if (tLine.startsWith("#")) continue;    // comment line or illegal line
                sb.append(tLine + "\n");
            }

            Properties prop = startNewProperties(sb.toString());
            if (prop == null) return;

            addItemsToListEntry(keyMap, prop, listEntry);
        } catch (IOException e) {
            throw new IOException("[HiPS_LIST]:" + e.getMessage());
        } catch (Exception e) {
            throw new IOException("[HiPS_LIST]:" + e.getMessage());
        }
    }


    private static String getTitle(String titleStr) {
         int insLoc = titleStr.indexOf("//");
         if (insLoc < 0) return titleStr;

         int divLoc = titleStr.indexOf('/', insLoc+2);
         if (divLoc < 0) return titleStr;

         int titleLoc = titleStr.indexOf('/', divLoc+1);
         if (titleLoc < 0) return titleStr;

         return titleStr.substring(titleLoc+1).replaceAll("/", " ").trim();
    }

    private static String getPropertyUrl(String hipsUrl) {
         if (!hipsUrl.startsWith("http")) return null;

         return (hipsUrl.endsWith("/")) ? (hipsUrl+PROP) : (hipsUrl+"/"+PROP);
    }

    // a csv file is created to contain HiPS from IRSA
    private List<HiPSMasterListEntry> createHiPSListFromFile(String hipsMaster,
                                                     String[] dataTypes,
                                                     String source) throws IOException, FailedRequestException {

        InputStream inf= IrsaHiPSListSource.class.getResourceAsStream(hipsMaster);
        DataGroup dg = DsvToDataGroup.parse(inf, CSVFormat.DEFAULT);

        return getListData(dg, paramsMap, source);
    }

    private List<HiPSMasterListEntry> getListData(DataGroup hipsDg,
                                                  Map<String, String> keyMap, String source) {

        List<DataObject> dataRows = hipsDg.values();
        DataType[]   dataCols = hipsDg.getDataDefinitions();
        String[]     cols = new String[dataCols.length];

        for (int i = 0; i < dataCols.length; i++) {
            String colName = dataCols[i].getKeyName();
            for (Map.Entry<String, String> entry: keyMap.entrySet()) {
                if (Arrays.asList(entry.getValue().split(",")).contains(colName)) {
                    cols[i] = entry.getKey();
                    break;
                }
            }
        }

        List<HiPSMasterListEntry> lists = new ArrayList<>();
        HiPSMasterListEntry oneList;

        for (DataObject row : dataRows) {
            oneList = new HiPSMasterListEntry();
            lists.add(oneList);
            oneList.set(PARAMS.SOURCE.getKey(), source);
            oneList.set(PARAMS.TYPE.getKey(), ServerParams.IMAGE);
            for (int i = 0; i < dataCols.length; i++) {
                if (cols[i] == null) continue;
                String colName = dataCols[i].getKeyName();

                Object obj = row.getDataElement(colName);
                String val = obj != null ? obj.toString() : null;

                oneList.set(cols[i], val);
                if (colName.equals(keyMap.get(PARAMS.IVOID.getKey()))) {
                    oneList.set(PARAMS.TITLE.getKey(), getTitle(val));
                } else if (colName.equals(keyMap.get(PARAMS.URL.getKey()))) {
                    oneList.set(PARAMS.PROPERTIES.getKey(), getPropertyUrl(val));
                }
            }
        }

        
        return lists;
    }
}

