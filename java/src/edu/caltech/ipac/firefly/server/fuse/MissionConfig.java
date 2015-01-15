/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.fuse;

import edu.caltech.ipac.firefly.data.fuse.config.MissionTag;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Date: 2/13/14
 *
 * @author loi
 * @version $Id: $
 */
public class MissionConfig implements Serializable {
    private long lastModified = 0;
    private Map<String, MissionTag> missionList = new HashMap<String, MissionTag>();

    public void addMission(MissionTag ds) {
        missionList.put(ds.getName(), ds);
    }

    public void clearAll() {
        missionList.clear();
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public MissionTag getMission(String name) {
        return missionList.get(name);
    }

    public Collection<MissionTag> getAllMissions() {
        return missionList.values();
    }
}
