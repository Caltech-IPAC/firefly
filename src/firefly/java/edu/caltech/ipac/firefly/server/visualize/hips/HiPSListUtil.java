package edu.caltech.ipac.firefly.server.visualize.hips;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.servlets.HiPSRetrieve;
import edu.caltech.ipac.firefly.server.util.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Trey Roby
 */
public class HiPSListUtil {
    static final Logger.LoggerImpl _log = Logger.getLogger();
    private final static String PROP = HiPSMasterListEntry.PARAMS.PROPERTIES.getKey().toLowerCase();

    private final static Map<String, String> defParamMap = new HashMap<>();

    static {
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.IVOID, "creator_did,publisher_did");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.URL, "hips_service_url");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.TITLE, "obs_title");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.ORDER, "hips_order");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.TYPE, "dataproduct_type");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.FRACTION, "moc_sky_fraction");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.FRAME, "hips_frame");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.WAVELENGTH, "obs_regime");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.RELEASEDATE, "hips_release_date");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.PIXELSCALE, "hips_pixel_scale");
    }

    /**
     * @param bPropCall - if true, then call every HiPS int list to get a complete set of hips properties,
     *                  warning this slows down HiPS initialization
     */
    public static List<HiPSMasterListEntry> createHiPSListFromUrl(String url, String source, boolean bPropCall, String childExt)
            throws IOException {
        Map<String, String> defParamMap = new HashMap<>();
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.IVOID, "creator_did,publisher_did");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.URL, "hips_service_url");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.TITLE, "obs_title");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.ORDER, "hips_order");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.TYPE, "dataproduct_type");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.FRACTION, "moc_sky_fraction");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.FRAME, "hips_frame");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.WAVELENGTH, "obs_regime");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.RELEASEDATE, "hips_release_date");
        HiPSMasterListEntry.setParamsMap(defParamMap, HiPSMasterListEntry.PARAMS.PIXELSCALE, "hips_pixel_scale");
        return createHiPSListFromUrl(url, source, defParamMap, bPropCall, childExt);
    }

    /**
     * @param bPropCall - if true, then call every HiPS int list to get a complete set of hips properties,
     *                  warning this slows down HiPS initialization
     */
    public static List<HiPSMasterListEntry> createHiPSListFromUrl(String url, String source,
                                                                  Map<String, String> keyMap, boolean bPropCall, String childExt)
                                                                                                throws IOException {
        try{
            _log.debug("executing " + source + " url query: " + url);

            long cTime = System.currentTimeMillis();

            FileInfo listFileInfo = HiPSRetrieve.retrieveHiPSData(url, childExt, false);
            File listFile= listFileInfo.getFile();
            if (listFile==null) throw new IOException("Could not retrieve file: "+ listFileInfo.getResponseCode());

            _log.debug("get " + source + " HiPS took " + (System.currentTimeMillis() - cTime) + "ms");

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
                    propLine.append(tLine).append("\n");
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

            oneList.set(HiPSMasterListEntry.PARAMS.SOURCE.getKey(), source);
            String url = oneList.getMapInfo().get(HiPSMasterListEntry.PARAMS.URL.getKey());

            if (url != null) {
                String pUrl = getPropertyUrl(url);

                oneList.set(HiPSMasterListEntry.PARAMS.PROPERTIES.getKey(), getPropertyUrl(url));
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
        String propUrl = listEntry.getMapInfo().get(HiPSMasterListEntry.PARAMS.PROPERTIES.getKey());

        if (propUrl == null) return;

        try {
            FileInfo propFileInfo = HiPSRetrieve.retrieveHiPSData(propUrl, null, false);
            File propFile= propFileInfo.getFile();
            if (propFile==null) throw new IOException("Could not retrieve file: "+ propFileInfo.getResponseCode());

            BufferedReader br = new BufferedReader(new FileReader(propFile));
            String strLine;
            StringBuilder sb = new StringBuilder();

            while ((strLine = br.readLine()) != null) {
                String tLine = strLine.trim();
                if (tLine.startsWith("#")) continue;    // comment line or illegal line
                sb.append(tLine).append("\n");
            }

            Properties prop = startNewProperties(sb.toString());
            if (prop == null) return;

            addItemsToListEntry(keyMap, prop, listEntry);
        } catch (Exception e) {
            throw new IOException("[HiPS_LIST]:" + e.getMessage());
        }
    }

    static String getPropertyUrl(String hipsUrl) {
         if (!hipsUrl.startsWith("http")) return null;

         return (hipsUrl.endsWith("/")) ? (hipsUrl+PROP) : (hipsUrl+"/"+PROP);
    }

    public static void warn(String s) { _log.warn(s); }
}

