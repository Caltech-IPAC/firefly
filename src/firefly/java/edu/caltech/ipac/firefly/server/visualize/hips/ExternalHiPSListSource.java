package edu.caltech.ipac.firefly.server.visualize.hips;
import edu.caltech.ipac.firefly.server.util.Logger;

import java.util.List;
/**
 * Created by cwang on 2/27/18.
 */
public class ExternalHiPSListSource implements HiPSMasterListSourceType {
    private static final Logger.LoggerImpl _log = Logger.getLogger();

    public List<HiPSMasterListEntry> getHiPSListData(String[] dataTypes, String source) {
        return null;
    }
}
