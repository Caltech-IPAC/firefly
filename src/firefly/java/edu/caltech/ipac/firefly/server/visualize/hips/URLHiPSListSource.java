package edu.caltech.ipac.firefly.server.visualize.hips;

import edu.caltech.ipac.firefly.data.ServerParams;

import java.util.Arrays;
import java.util.List;

public class URLHiPSListSource implements HiPSMasterListSourceType {

    private final String sourceName;
    private final String urlStr;

    public URLHiPSListSource(String sourceName, String urlStr) {
        this.sourceName = sourceName;
        this.urlStr= urlStr;
    }

    public List<HiPSMasterListEntry> getHiPSListData(String[] dataTypes, String source) {
        try {
            if (!Arrays.asList(dataTypes).contains(ServerParams.IMAGE)) return null;
            return HiPSListUtil.createHiPSListFromUrl(urlStr, sourceName, false, null);
        }
        catch (Exception e) {
            HiPSListUtil.warn("get " + source + " HiPS failed - " + e.getMessage());
            return null;
        }
    }
    public String getUrl() { return urlStr; }
}
