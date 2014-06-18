package edu.caltech.ipac.firefly.core.background;

import com.google.gwt.storage.client.StorageEvent;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
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
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                if (BrowserCache.isPerm()) {
                    BrowserCache.addHandlerForKey(STATE_KEY, new StorageEvent.Handler() {
                        public void onStorageChange(StorageEvent ev) {
                            syncWithCache();
                        }
                    });
                }
            }
        });

    }

    public void addItem(MonitorItem item) {
        _monitorMap.put(item.getID(), item);
        syncWithCache();
    }

    public void removeItem(MonitorItem item) {
        if (_monitorMap.containsKey(item.getID())) {
            _monitorMap.remove(item.getID());
            _deletedItems.add(item.getID());
        }

        syncWithCache();
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


    public void syncWithCache() {
        if (firstTimeReadFromCache)  {
            String serState= BrowserCache.get(STATE_KEY);
            if (serState!=null)  MonitorRecoveryFunctions.deserializeAndLoadMonitor(this, serState);
            firstTimeReadFromCache = false;
        }

        List<MonitorItem> itemList= new ArrayList<MonitorItem>(_monitorMap.values());
        BrowserCache.put(STATE_KEY, MonitorRecoveryFunctions.serializeMonitorList(itemList), TWO_WEEKS_IN_SECS);
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
