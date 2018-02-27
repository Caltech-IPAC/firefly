package edu.caltech.ipac.firefly.server.visualize.hips;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.visualize.hips.HiPSMasterListEntry.PARAMS;
import edu.caltech.ipac.firefly.server.query.lsst.LSSTQuery;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;

/**
 * Created by cwang on 2/27/18.
 */
public class CDSHiPSListSource implements HiPSMasterListSourceType {
    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final String HiPSCDSURL = AppProperties.getProperty("HiPS.cds.hostname",
                                             "http://alasky.unistra.fr/MocServer/query?hips_service_url=*&get=record");
    //set default timeout to 30seconds
    private static int TIMEOUT  = new Integer( AppProperties.getProperty("HiPS.cds.timeoutLimit" , "30")).intValue();
    private static Map<String, String> paramsMap = new HashMap<>();

    static {
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.ID, "ID");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.TITLE, "obs_title");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.ORDER, "hips_order");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.TYPE, "dataproduct_type");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.FRACTION, "moc_sky_fraction");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.FRAME, "hips_frame");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.URL, "hips_service_url");
    }


    public List<HiPSMasterListEntry> getHiPSListData(String[] dataTypes, String source) {
        //URLDownLoad.getDataToFile()

        try {
            return createHiPSList(dataTypes, source);
        } catch (FailedRequestException | IOException | DataAccessException e) {
            _log.warn("get CDS HiPS failed");
            return null;
        } catch (Exception e) {
            _log.warn(e.getMessage());
            return null;
        }
    }

    private String dataProduct(String[] dataTypes) {
        String dp =  "";

        for (int i = 0; i < HiPSMasterList.HiPSDataType.length; i++) {
            if (Arrays.asList(dataTypes).contains(HiPSMasterList.HiPSDataType[i])) {
                dp += "&dataproduct_type=" + HiPSMasterList.HiPSDataType[i];
        }
        }
        return dp;
    }

    private List<HiPSMasterListEntry> createHiPSList(String[] dataTypes,
                                                     String source) throws IOException, DataAccessException, FailedRequestException  {
        String url = HiPSCDSURL + dataProduct(dataTypes);

        _log.briefDebug("executing CDS query: " + url);
        File file = HiPSMasterList.createFile(dataTypes, ".txt", source);
        Map<String, String> requestHeader=new HashMap<>();
        requestHeader.put("Accept", "application/text");
        long cTime = System.currentTimeMillis();
        FileInfo listFile = URLDownload.getDataToFileUsingPost(new URL(url), null, null, requestHeader, file, null,
                                                               TIMEOUT);
        _log.briefDebug("get CDS HiPS took " + (System.currentTimeMillis() - cTime) + "ms");

        if (listFile.getResponseCode() >= 400) {
             String err = LSSTQuery.getErrorMessageFromFile(file);
             throw new DataAccessException("[HiPS_CDS] " + (err == null ? listFile.getResponseCodeMsg() : err));
        }

        return getListData(file, paramsMap, source);
    }

    private List<HiPSMasterListEntry> getListData(File f, Map<String, String> keyMap, String source) throws IOException {
        if (f == null) return null;

        try{
            // Open the file that is the first
            // command line parameter
            BufferedReader br = new BufferedReader(new FileReader(f));
            String strLine;
            HiPSMasterListEntry oneList = null;
            List<HiPSMasterListEntry> lists = new ArrayList();
            String sProp = HiPSMasterListEntry.getParamString(keyMap, PARAMS.ID);  // first property for each record

            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                String tLine = strLine.trim();
                if (tLine.startsWith("#")) continue;    // comment line

                String[] oneKeyVal = tLine.split("=");
                if (oneKeyVal.length != 2) continue;    // not legal key=value line

                String k = oneKeyVal[0].trim();         // key
                String v = oneKeyVal[1].trim();         // value


                if (k.equalsIgnoreCase(sProp)) {        // key is 'ID'
                    oneList = new HiPSMasterListEntry();
                    lists.add(oneList);
                    oneList.set(PARAMS.ID.getKey(), v);
                    oneList.set(PARAMS.SOURCE.getKey(), source);
                } else {
                    if (oneList == null) continue;
                    for (Map.Entry<String, String> entry : keyMap.entrySet()) {
                        if (entry.getValue().equals(k)) {
                            oneList.set(entry.getKey(), v);
                            break;
                        }
                    }
                }
            }
            //Close the input stream
            br.close();
            return lists;
        }catch (Exception e){//Catch exception if any
            e.printStackTrace();
            throw new IOException("[HIPS_CDS]:" + e.getMessage());
        }
    }
}
