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
    private static int TIMEOUT  = new Integer( AppProperties.getProperty("HiPS.timeoutLimit" , "30")).intValue();
    private static Map<String, String> paramsMap = new HashMap<>();

    static {
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.ID, "creator_did");
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
                                                     String source) throws IOException, DataAccessException, FailedRequestException {
        String url = HiPSCDSURL + dataProduct(dataTypes);
        return IrsaHiPSListSource.createHiPSListFromUrl(url, dataTypes, source, null);
    }
}
