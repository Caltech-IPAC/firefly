package edu.caltech.ipac.firefly.server.visualize.hips;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;

import java.util.Arrays;
import java.util.List;


/**
 * Created by cwang on 2/27/18.
 */
public class CDSHiPSListSource implements HiPSMasterListSourceType {
    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final String HiPSCDSURL = AppProperties.getProperty("HiPS.cds.hostname",
                                             "http://alasky.unistra.fr/MocServer/query?hips_service_url=*&get=record");

    public List<HiPSMasterListEntry> getHiPSListData(String[] dataTypes, String source) {
        try {
            String url = HiPSCDSURL + dataProduct(Arrays.asList(dataTypes));
            return HiPSListUtil.createHiPSListFromUrl(url, source, false, joinStr(dataTypes));
        } catch (Exception e) {
            _log.warn("get " + source + " HiPS failed - " + e.getMessage());
            return null;
        }
    }
    
    public String getUrl() { return HiPSCDSURL; }

    private String dataProduct(List<String> dtList) {
        return Arrays.stream(HiPSMasterList.HiPSDataType)
                .reduce("", (str,t) -> dtList.contains(t) ? str+="&dataproduct_type=" +t : str);
    }

    private String joinStr(String[] strAry) {
        String[] newAry = Arrays.stream(strAry).map(String::toLowerCase).sorted().toList().toArray(new String[0]);
        return String.join("_", newAry);
    }

}
