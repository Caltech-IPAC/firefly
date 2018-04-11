package edu.caltech.ipac.firefly.server.visualize.hips;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.firefly.server.visualize.hips.HiPSMasterListEntry.PARAMS;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.io.IOException;


/**
 * Created by cwang on 2/27/18.
 */
public class CDSHiPSListSource implements HiPSMasterListSourceType {
    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final String HiPSCDSURL = AppProperties.getProperty("HiPS.cds.hostname",
                                             "http://alasky.unistra.fr/MocServer/query?hips_service_url=*&get=record");
    private static Map<String, String> paramsMap = new HashMap<>();

    static {
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.IVOID, "creator_did,publisher_did");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.TITLE, "obs_title");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.ORDER, "hips_order");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.TYPE, "dataproduct_type");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.FRACTION, "moc_sky_fraction");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.FRAME, "hips_frame");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.URL, "hips_service_url");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.WAVELENGTH, "obs_regime");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.RELEASEDATE, "hips_release_date");
        HiPSMasterListEntry.setParamsMap(paramsMap, PARAMS.PIXELSCALE, "hips_pixel_scale");
    }


    public List<HiPSMasterListEntry> getHiPSListData(String[] dataTypes, String source) {
        try {
            return createHiPSList(dataTypes, source);
        } catch (IOException e) {
            _log.warn("get " + source + " HiPS failed - " + e.getMessage());
            return null;
        } catch (Exception e) {
            _log.warn("get " + source + " HiPS failed - " + e.getMessage());
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
                                                     String source) throws IOException {
        String url = HiPSCDSURL + dataProduct(dataTypes);

        // no call for HiPS properties
        return IrsaHiPSListSource.createHiPSListFromUrl(url, source, paramsMap, false, joinStr(dataTypes));
    }

    private String joinStr(String[] strAry) {
        String[] newAry = new String[strAry.length];

        for (int i = 0; i < strAry.length; i++) {
            newAry[i] = strAry[i].toLowerCase();
        }
        Arrays.sort(newAry);

        return String.join("_", newAry);
    }
}
