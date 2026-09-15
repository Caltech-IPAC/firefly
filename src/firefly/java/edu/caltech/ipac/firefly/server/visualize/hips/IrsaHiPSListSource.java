package edu.caltech.ipac.firefly.server.visualize.hips;

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.util.AppProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Created by cwang on 2/27/18.
 */
public class IrsaHiPSListSource implements HiPSMasterListSourceType {

    private static final String irsaBaseUrl= AppProperties.getProperty("irsa.base.url","https://irsa.ipac.caltech.edu");
    private static final String irsaAddMocPath = AppProperties.getProperty("irsa.moc.list.path","/irsadata/hips/irsa_moc.list");

    private static final String irsaHipsListUrl = AppProperties.getProperty(
            "irsa.hips.masterUrl",
            "https://irsa.ipac.caltech.edu/data/hips/list");

    public List<HiPSMasterListEntry> getHiPSListData(String[] dataTypes, String source) {
        try {
            if (!Arrays.asList(dataTypes).contains(ServerParams.IMAGE)) return null;
            return HiPSListUtil.createHiPSListFromUrl(irsaHipsListUrl, source, false, null);
        }
        catch (Exception e) {
            HiPSListUtil.warn("get " + source + " HiPS failed - " + e.getMessage());
            return null;
        }
    }

    public List<HiPSMasterListEntry> getAdditionalMOCS(String source) {
        return HiPSListUtil.getAdditionalMOCS(getMocUrl(), source);
    }

    public String getUrl() { return irsaHipsListUrl; }
    public String getMocUrl() {
        if (irsaAddMocPath.startsWith("https")) {
            return irsaAddMocPath;
        }
        else {
            if (irsaBaseUrl.endsWith("/") && irsaAddMocPath.startsWith("/")) {
                return irsaBaseUrl.substring(0,irsaBaseUrl.length()-1)+irsaAddMocPath;
            }
            else {
                return irsaBaseUrl+irsaAddMocPath;
            }
        }
    }
}
