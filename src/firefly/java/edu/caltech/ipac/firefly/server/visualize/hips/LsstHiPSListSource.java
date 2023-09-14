package edu.caltech.ipac.firefly.server.visualize.hips;

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.util.AppProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Created by cwang on 2/27/18.
 */
public class LsstHiPSListSource implements HiPSMasterListSourceType {

    private static final String lsstHipsListUrl = AppProperties.getProperty(
            "lsst.hips.masterUrl",
            "https://irsa.ipac.caltech.edu/data/hips/list");

    public List<HiPSMasterListEntry> getHiPSListData(String[] dataTypes, String source) {
        try {
            if (!Arrays.asList(dataTypes).contains(ServerParams.IMAGE)) return null;
            return HiPSListUtil.createHiPSListFromUrl(lsstHipsListUrl, source, false, null);
        }
        catch (Exception e) {
            HiPSListUtil.warn("get " + source + " HiPS failed - " + e.getMessage());
            return null;
        }
    }
}
