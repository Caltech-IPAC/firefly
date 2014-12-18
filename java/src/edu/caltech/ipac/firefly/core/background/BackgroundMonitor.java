package edu.caltech.ipac.firefly.core.background;
/**
 * User: roby
 * Date: 5/30/14
 * Time: 10:46 AM
 */


import com.google.gwt.storage.client.StorageEvent;

/**
 * @author Trey Roby
 */
public interface BackgroundMonitor {

    public int getCount();
    public void addItem(MonitorItem item);
    public void removeItem(MonitorItem item);
    public void setStatus(BackgroundStatus bgStat);
    public void syncWithCache(StorageEvent ev);
    public boolean isDeleted(String id);
    public boolean isMonitored(String id);
    public void pollAll();


}

