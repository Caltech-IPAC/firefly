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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
