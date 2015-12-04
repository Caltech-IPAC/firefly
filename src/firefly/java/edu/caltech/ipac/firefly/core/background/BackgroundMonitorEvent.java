/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core.background;

import com.google.gwt.storage.client.StorageEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.SearchServicesAsync;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.BrowserCache;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.ServerSentEventNames;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.ComparisonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
/**
 * User: roby
 * Date: Oct 27, 2008
 * Time: 4:09:07 PM
 */


/**
 * @author Trey Roby
 */
public class BackgroundMonitorEvent implements BackgroundMonitor {

    private static final String STATE_KEY = "PackagerControlKeys";
    private static final long TWO_WEEKS_IN_SECS= 60L * 60L  * 24L * 14L ;   //  1 hour * 24 hours * 14 days = 2 weeks
    private final Map<String, MonitorItem> _monitorMap = new HashMap<String, MonitorItem>();
    private final Set<String> _deletedItems= new HashSet<String>(10);
    private boolean firstTimeReadFromCache = true;
//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public BackgroundMonitorEvent() {
        WebEventManager.getAppEvManager().addListener(ServerSentEventNames.SVR_BACKGROUND_REPORT,new WebEventListener<String>() {
            public void eventNotify(WebEvent<String> ev) {
                BackgroundStatus bgStat= BackgroundStatus.parse(ev.getData());
                if (bgStat!=null) setStatus(bgStat);
                else GwtUtil.getClientLogger().log(Level.INFO, "failed to parse BackgroundStatus:" + ev.getData());
            }
        });
    }

    public void addItem(MonitorItem item) {
        _monitorMap.put(item.getID(), item);
        syncWithCache(null);
        SearchServicesAsync dserv= SearchServices.App.getInstance();
        dserv.addIDToPushCriteria(item.getID(), new AsyncCallback<Boolean>() {
            public void onFailure(Throwable caught) { }
            public void onSuccess(Boolean success) { }
        });
    }

    public void removeItem(MonitorItem item) {
        if (_monitorMap.containsKey(item.getID())) {
            _monitorMap.remove(item.getID());
            _deletedItems.add(item.getID());
        }

        syncWithCache(null);
        WebEvent<MonitorItem> ev= new WebEvent<MonitorItem>(this, Name.MONITOR_ITEM_REMOVED, item);
        WebEventManager.getAppEvManager().fireEvent(ev);
    }

    public void setStatus(BackgroundStatus bgStat) {
        MonitorItem monItem= _monitorMap.get(bgStat.getID());
        if (monItem!=null) {
            monItem.setStatus(bgStat);
        }
        else { // look for composite
            for(Map.Entry<String,MonitorItem> entry: _monitorMap.entrySet()) {
                monItem= entry.getValue();
                if (monItem.isComposite()) {
                    for(MonitorItem m : monItem.getCompositeList()) {
                        if (m.getID().equals(bgStat.getID())) {
                            m.setStatus(bgStat);
                            CompositeJob cR= monItem.getCompositeJob();
                            CompositeJob newComposite= cR.makeDeltaJob(bgStat);
                            monItem.setCompositeJob(newComposite);
                            break;
                        }
                    }
                }
            }
        }

    }


    public int getCount() { return _monitorMap.size(); }
    public boolean isDeleted(String id) { return _deletedItems.contains(id); }
    public boolean isMonitored(String id) { return _monitorMap.containsKey(id); }

    public void pollAll() {
        for(MonitorItem item : _monitorMap.values()) {
            if (!item.isDone()) {
                if (item.isComposite()) {
                    for(MonitorItem mi : item.getCompositeList()) {
                        checkStatus(item,mi.getStatus());
                    }
                }
                else {
                    checkStatus(item,item.getStatus());
                }
            }
        }
    }

//======================================================================
//------------------ StatefulWidget Methods ----------------------------
//======================================================================


    public void syncWithCache(StorageEvent ev) {
        if (firstTimeReadFromCache)  {
            String serState= BrowserCache.get(STATE_KEY);
            if (serState!=null)  MonitorRecoveryFunctions.deserializeAndLoadMonitor(this, serState);
            firstTimeReadFromCache = false;
        }

        List<MonitorItem> itemList= new ArrayList<MonitorItem>(_monitorMap.values());
        String currentEntry= (ev==null) ? null : ev.getNewValue();
        String newEntry= MonitorRecoveryFunctions.serializeMonitorList(itemList);
        if (currentEntry==null || !currentEntry.equals(newEntry)) {
            BrowserCache.put(STATE_KEY, newEntry, TWO_WEEKS_IN_SECS);
        }
    }



    private void checkStatus(final MonitorItem monItem, final BackgroundStatus bgStatus) {

        SearchServicesAsync dserv= SearchServices.App.getInstance();
        dserv.getStatus(bgStatus.getID(), false, new AsyncCallback<BackgroundStatus>() {
            public void onFailure(Throwable caught) {
                //if we failed, just assumed we are offline, don't fail the report
            }

            public void onSuccess(BackgroundStatus bgStatus) {
                updateReport(monItem, bgStatus);
            }
        });
    }


    public void updateReport(MonitorItem monItem, BackgroundStatus bgStat) {
        if (monItem.isComposite()) {
            for(MonitorItem mi : monItem.getCompositeList()) {
                if (ComparisonUtil.equals(mi.getID(), bgStat.getID())) {
                    mi.setStatus(bgStat);
                    CompositeJob cR= monItem.getCompositeJob();
                    CompositeJob newComposite= cR.makeDeltaJob(mi.getStatus());
                    monItem.setCompositeJob(newComposite);
                    break;
                }
            }
        }
        else {
            monItem.setStatus(bgStat);
        }

    }


}

