/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.hips;

import java.util.List;

/**
 * @author Cindy Wang
 */
public interface HiPSMasterListSourceType {

    List<HiPSMasterListEntry> getHiPSListData(String[] dataTypes, String source);
    String getUrl();
}
