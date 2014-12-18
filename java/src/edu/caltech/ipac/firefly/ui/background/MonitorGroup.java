package edu.caltech.ipac.firefly.ui.background;
/**
 * User: roby
 * Date: 6/16/11
 * Time: 10:20 AM
 */


import edu.caltech.ipac.firefly.core.background.MonitorItem;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Trey Roby
 */
public class MonitorGroup {

    private final Map<String,DownloadGroupPanel> _groups= new TreeMap<String, DownloadGroupPanel>();




//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public int size() { return _groups.size(); }

    public boolean containsKey(String id) { return _groups.containsKey(id); }
    public void remove(String id) { _groups.remove(id); }

    public boolean containsItem(MonitorItem monItem) {
        return monItem!=null && _groups.containsKey(monItem.getID());
    }

    public void putItem(MonitorItem monItem, DownloadGroupPanel panel) {
        if (monItem!=null) _groups.put(monItem.getID(),panel);
    }

    public DownloadGroupPanel getPanel(MonitorItem monItem) {
        return monItem==null? null : _groups.get(monItem.getID());
    }
    public DownloadGroupPanel getPanel(String id) {
        return _groups.get(id);
    }

    public int getUndownloadCnt() {
        int total= 0;
        for(DownloadGroupPanel panel : _groups.values()) {
            total+= panel.getUndownloadCnt();
        }
        return total;
    }

    public Collection<DownloadGroupPanel> panels() { return _groups.values(); }
    public Set<Map.Entry<String,DownloadGroupPanel>> getEntries() { return _groups.entrySet(); }
    public Set<String> getIDs() { return _groups.keySet(); }

    public int getWorkingCnt() {
        int total= 0;
        for(DownloadGroupPanel panel : _groups.values()) {
            total+= panel.getWorkingCnt();
        }
        return total;
    }
//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

}

