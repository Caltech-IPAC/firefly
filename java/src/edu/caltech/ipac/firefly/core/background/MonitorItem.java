package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.ui.background.BackgroundTask;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: roby
 * Date: Dec 15, 2009
 * Time: 10:27:30 AM
 */


/**
 * @author Trey Roby
 */
public class MonitorItem {

    private static final String CLIENT_ID = "CompositeJob-";
    private static int _cnt= 0;

    private final String title;
    private CanCancel canceler= null;
    private BackgroundStatus bgStatus= null;
    private List<MonitorItem> _groupMonitorList= null;
    private CompositeJob compositeJob= null;
    private boolean watch= true;
    private Map<Integer,Boolean> _activated= new HashMap<Integer,Boolean>(5);
    private boolean activateImmediately= false;
    private boolean activateOnCompletion= false;
    private BackgroundUIType uiType;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public MonitorItem(String title, BackgroundUIType uiType, boolean watch) {
        this.title= title;
        this.watch= watch;
        this.uiType= uiType;
    }

    public MonitorItem(String title, BackgroundUIType uiType) { this(title,uiType,true); }
    public MonitorItem(String title) { this(title,BackgroundUIType.NONE,true); }

    private MonitorItem() { this(null,BackgroundUIType.NONE, false); }


    public String getReportDesc() {
        String retval;
        switch (bgStatus.getBackgroundType()) {
            case SEARCH: retval= "Search Task"; break;
            case PACKAGE: retval= "Packaging Task"; break;
            default: retval= "Background Task"; break;
        }
        return retval;
    }


    public void setWatchable(boolean watch) {
        if (this.watch!=watch) {
            this.watch= watch;
            fireUpdate();
        }
    }
    public boolean isWatchable() { return watch;}


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    public void initStatusList(List<BackgroundStatus> partList) {
        if (_groupMonitorList==null) {
            _groupMonitorList= new ArrayList<MonitorItem>(partList.size());
            MonitorItem mi;
            for(BackgroundStatus status : partList) {
                mi= new MonitorItem();
                _groupMonitorList.add(mi);
                mi.setStatus(status);

            }
            String taskID= CLIENT_ID + (_cnt++);
            compositeJob= new CompositeJob(taskID,partList);
        }
        else {
            WebAssert.argTst(false, "You cannot call initReportList more then once");
        }
    }

    public void setCanceller(CanCancel canceler) { this.canceler= canceler; }
    public CanCancel getCanceller() { return canceler; }

    public void setStatus(BackgroundStatus bgStatus) {
        this.bgStatus= bgStatus;
        fireUpdate();
    }

    public boolean isComposite() { return _groupMonitorList!=null &&_groupMonitorList.size()>0;}

    public List<MonitorItem> getCompositeList() {
        return _groupMonitorList;
    }

    public void cancel() {
        if (canceler!=null) canceler.cancelTask();
        else new DefaultCanceler(this).cancelTask();
    }
    public BackgroundState getState() { return bgStatus.getState(); }
    public String getID() {
        WebAssert.argTst(bgStatus!=null, "You have not yet set a report to monitor, use setReport");
        return bgStatus.getID();
    }
    public boolean isDone() { return bgStatus.isDone(); }

    public String getTitle() { return title; }

    public BackgroundStatus getStatus() { return bgStatus; }
    public CompositeJob getCompositeJob() { return compositeJob; }
    public void setCompositeJob(CompositeJob job) { compositeJob= job; }


    private void fireUpdate() {
        if (bgStatus!=null) {
            WebEvent<MonitorItem> ev= new WebEvent<MonitorItem>(this, Name.MONITOR_ITEM_UPDATE,
                                                                this);
            WebEventManager.getAppEvManager().fireEvent(ev);
        }
    }

    public boolean isActivated() { return isActivated(0); }

    public void setImmediately(boolean activateImmediately) { this.activateImmediately= activateImmediately; }
    public boolean getImmediately() { return activateImmediately; }

    public boolean getActivateOnCompletion() { return activateOnCompletion; }
    public void setActivateOnCompletion(boolean a) { this.activateOnCompletion= a; }

    public BackgroundUIType getBackgroundUIType() { return uiType; }


    public boolean isActivated(int idx) {
        boolean retval= false;
        if (_activated.containsKey(idx)) {
            retval= _activated.get(idx);
        }
        return retval;
    }


    public void setActivated(boolean activated) { setActivated(0,activated); }

    public void setActivated(int idx, boolean activated) {
        if (!_activated.containsKey(idx) ||
                (_activated.containsKey(idx) && _activated.get(idx)!=activated))  {
            _activated.put(idx,activated);
            fireUpdate();
        }
    }




    private static class DefaultCanceler implements CanCancel {
        private final MonitorItem _monItem;

        private DefaultCanceler(MonitorItem item) {
            _monItem= item;
            if (_monItem.getCanceller()==null)  _monItem.setCanceller(this);
        }

        public void cancelTask() {
            BackgroundTask.cancel(_monItem.getID());
            BackgroundStatus bgStat= new BackgroundStatus(BackgroundState.USER_ABORTED,_monItem.getStatus());
            _monItem.setStatus(bgStat);
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
