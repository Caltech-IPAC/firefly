package edu.caltech.ipac.firefly.core.background;

import com.google.gwt.storage.client.StorageEvent;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import edu.caltech.ipac.firefly.util.BrowserCache;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.ComparisonUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
/**
 * User: roby
 * Date: Oct 27, 2008
 * Time: 4:09:07 PM
 */


/**
 * @author Trey Roby
 */
public class BackgroundMonitor {

    private static final int ID_POS = 0;
    private static final int TITLE_POS = 1;
    private static final int WATCH_POS = 2;
    private static final int ATYPE_POS = 3;
    private static final int ASTAT_POS = 4;
    private static final int SUBREP_POS= 5;

    private static final String STATE_KEY = "PackagerControlKeys";
    private static final long TWO_WEEKS_IN_SECS= 60L * 60L  * 24L * 14L ;   //  1 hour * 24 hours * 14 days = 2 weeks
    private static final String DELIM= "-:::-";
    private static final String SUB_DELIM = ":";
    private static final String ACTDELIM= "-";
    private final Map<String, Monitor> _monitorMap = new HashMap<String, Monitor>();
    private final Set<String> _deletedItems= new HashSet<String>(10);
    private boolean mustReadFromCache = true;
    public static final int WAITS[]=
                                    {1,1,1,2,2,2,2,5,5,5,5,
                                     10,4,10,10,6,10,4,10,6,
                                     10,4,10,10,6,10,4,10,10};

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public BackgroundMonitor() {
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

    public int getCount() { return _monitorMap.size(); }

    public void addItem(MonitorItem item) {
        Monitor monitor= new Monitor(item);
        _monitorMap.put(item.getID(),monitor);
        monitor.startMonitoring();
        syncWithCache();
    }

    public void removeItem(MonitorItem item) {
        Monitor monitor= _monitorMap.get(item.getID());
        if (monitor!=null) {
            _monitorMap.remove(item.getID());
            _deletedItems.add(item.getID());
            monitor.stop();
        }

        syncWithCache();
        WebEvent<MonitorItem> ev= new WebEvent<MonitorItem>(this, Name.MONITOR_ITEM_REMOVED, item);
        WebEventManager.getAppEvManager().fireEvent(ev);
    }



    public void forceUpdates() {
        for(Monitor m :  _monitorMap.values()) m.forceMonitoring();
    }

//======================================================================
//------------------ StatefulWidget Methods ----------------------------
//======================================================================


    public void syncWithCache() {
        if (mustReadFromCache)  {
            String serState= BrowserCache.get(STATE_KEY);
            if (serState!=null)  deserializeStateAndLoad(serState);
            mustReadFromCache= false;
        }
        BrowserCache.put(STATE_KEY, getSerializeState(), TWO_WEEKS_IN_SECS);
    }




    private String getSerializeState() {
        StringBuilder sb= new StringBuilder(200);

        for(Iterator<Map.Entry<String, Monitor>> i= _monitorMap.entrySet().iterator();(i.hasNext());) {
            Map.Entry<String, Monitor> entry= i.next();
            MonitorItem item= entry.getValue().getMonitorItem();
            if (item.isRecreatable()) {
                sb.append(entry.getKey());
                sb.append(DELIM);
                sb.append(item.getTitle());
                sb.append(DELIM);
                sb.append(item.isWatchable());
                sb.append(DELIM);
                sb.append(item.getActivationType().toString());
                sb.append(DELIM);
                int cnt= item.getReport().getPartCount();
                for(int k=0; (k<cnt); k++) {
                    sb.append(item.isActivated(k));
                    if (k<cnt-1) sb.append(ACTDELIM);
                }
                if (item.isComposite()) {
                    sb.append(DELIM);
                    for(Iterator<MonitorItem> j= item.getCompositeList().iterator(); (j.hasNext());) {
                        MonitorItem subi= j.next();
                        sb.append(subi.getID());
                        if (j.hasNext()) sb.append(SUB_DELIM);
                    }
                }
                if (i.hasNext()) sb.append(",");
            }
        }
        return sb.toString();
    }



    private void deserializeStateAndLoad(String serState) {
        String keyAry[]= serState.split(",");
        if (keyAry.length>0) {
            for(String entry : keyAry) {
                String s[]= entry.split(DELIM,6);
                String idAry[]= null;
                if (s.length==6) {
                    idAry= s[SUBREP_POS].split(SUB_DELIM);
                }
                if (s.length==5 || s.length==6) {
                    String actStr[]= s[ASTAT_POS].split(ACTDELIM);
                    boolean act[]= new boolean[actStr.length];
                    for(int m=0; (m<act.length); m++)  act[m]= Boolean.parseBoolean(actStr[m]);
                    try {

                        boolean watch= Boolean.parseBoolean(s[WATCH_POS]);
                        ActivationFactory.Type atype= Enum.valueOf(ActivationFactory.Type.class ,s[ATYPE_POS]);
                        if (idAry==null) {
                            String id= s[ID_POS];
                            if (!_monitorMap.containsKey(id) && !_deletedItems.contains(id)) {
                                MonitorFunctions.checkStatusThenMakeNew(s[ID_POS],atype,
                                                                        s[TITLE_POS],  watch, act);
                            }
                        }
                        else {
                            boolean check= true;
                            for(String id : idAry)  {
                                if (_monitorMap.containsKey(id) || _deletedItems.contains(id)) {
                                    check= false;
                                    break;
                                }
                            }
                            if (check) {
                                MonitorFunctions.checkGroupStatusThenMakeNew(atype,s[TITLE_POS],
                                                                             watch, idAry, act);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        // do nothing, just ignore and move on
                    }
                }
            }
        }
    }


//======================================================================
//------------------ Inner Classes -------------------------------------
//======================================================================

    public static class Monitor extends Timer implements CanCancel {
        private final MonitorItem _monItem;
        private int _waitIdx= 0;
        private boolean _enabled= true;

        private Monitor(MonitorItem item) {
            _monItem= item;
            if (_monItem.getCanceller()==null) {
                _monItem.setCanceller(this);
            }
        }

        private void startMonitoring() {
            scheduleCheck(false);
        }

        private void forceMonitoring() {
            scheduleCheck(true);
        }

        private MonitorItem getMonitorItem() { return _monItem;}

        public void run() {
            if (_enabled) {
                if (_monItem.isComposite()) {
                    for(MonitorItem mi : _monItem.getCompositeList()) {
                        MonitorFunctions.checkStatus(this, mi.getReport());
                    }
                }
                else {
                    MonitorFunctions.checkStatus(this, _monItem.getReport());
                }
            }
        }

        public void updateReport(BackgroundReport report) {
            if (_monItem.isComposite()) {
                boolean found= false;
                for(MonitorItem mi : _monItem.getCompositeList()) {
                    if (ComparisonUtil.equals(mi.getID(),report.getID())) {
                        found= true;
                        mi.setReport(report);
                        CompositeReport cR= _monItem.getCompositeReport();
                        CompositeReport newComposite= cR.makeDeltaReport(report);
                        _monItem.setReport(newComposite);

                        break;
                    }
                }
                if (!found) {
                    WebAssert.tst(false,
                                  "id could not be found in composite monitor item, id: "+
                                          report.getID());

                }
            }
            else {
                WebAssert.tst(ComparisonUtil.equals(_monItem.getID(),report.getID()),
                              "id not same on update: old id: " + _monItem.getID()+
                                      " new id: " + report.getID());
                _monItem.setReport(report);
            }

            scheduleCheck(false);
        }


        private void cancelDownload() {

            cancel();
            if (_monItem.isComposite()) {
                for(MonitorItem mi : _monItem.getCompositeList()) {
                    MonitorFunctions.cancel(mi.getID());
                }
            }
            else {
                MonitorFunctions.cancel(_monItem.getID());
            }
            BackgroundReport report= _monItem.getReport();
            report= report.cloneWithState(BackgroundState.USER_ABORTED);
            _monItem.setReport(report);
        }


        private void scheduleCheck(boolean force) {
            cancel();
            if (force || !_monItem.isDone()) {
                schedule( WAITS[_waitIdx] * 1000);
                if (_waitIdx<WAITS.length-1) _waitIdx++;
            }
        }

        public void cancelTask() { cancelDownload(); }

        public void stop() {
            _enabled= false;
            this.cancel();
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
