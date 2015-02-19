/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.dyn;

import edu.caltech.ipac.firefly.data.dyn.xstream.XidBaseTag;

import java.util.Map;
import java.util.HashMap;

public class DynServerData {

    private static DynServerData data;
    private Map<String, XidBaseTag> xids;


    /**
     * singleton; use getInstance().
     */
    private DynServerData() {
        xids = new HashMap<String, XidBaseTag>();
    }

    public static DynServerData getInstance() {
        if (data == null) {
            data = new DynServerData();
        }
        return data;
    }

    public void clearAll() {
        xids.clear();
    }

    public void addProjectXid(String id, XidBaseTag obj) {
        xids.put(id, obj);
    }
    public XidBaseTag getProjectXid(String id) {
        return xids.get(id);
    }

}

