package edu.caltech.ipac.firefly.core.background;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.packagedata.PackagedReport;
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


    public enum DataUseType {Download, Custom }

    private static final String CLIENT_ID = "CompositeReport-";
    private static int _cnt= 0;

    private ActivationFactory.Type _aType= null;
    private final String _title;
    private CanCancel _canceler= null;
    private final BackgroundActivation _activation;
    private Map<Integer,Boolean> _activated= new HashMap<Integer,Boolean>(5);
    private BackgroundReport _report= null;
    private boolean _watch= true;
    private boolean _showParts= true;
    private boolean recreated= false;
    private List<MonitorItem> _groupMonitorList= null;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public MonitorItem(String title,
                       ActivationFactory.Type type,
                       boolean immediate,
                       boolean watchable) throws IllegalArgumentException {
        this(title, getActivation(type,immediate));
        _aType= type;
        _watch= watchable;
    }

    public MonitorItem(String title,
                       ActivationFactory.Type type,
                       boolean immediate) throws IllegalArgumentException {
        this(title,type,immediate,true);
    }



    public MonitorItem(String title,
                       BackgroundActivation activation) {
        _title= title;
        _activation = activation;
    }

    public boolean isRecreated() { return recreated; }

    public void setRecreated(boolean recreated) {
        this.recreated = recreated;
    }

    private MonitorItem() {
        this("hidden",null);
        setWatchable(false);
    }

    public String getReportDesc() {
        String retval;
        if (_report instanceof PackagedReport) {
            retval= "Packaging Task";
        }
        else {
            retval= "Background Task";
        }
        return retval;
    }


    private static BackgroundActivation getActivation(ActivationFactory.Type type, boolean immediate) {
        ActivationFactory fact= ActivationFactory.getInstance();
        BackgroundActivation retval;
        if (fact.isSupported(type)) {
            retval= fact.createActivation(type,immediate);
        }
        else {
            throw new IllegalArgumentException("Type is not support in ActivationFactory");
        }
        return retval;
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public boolean isRecreatable() {
        return _aType!=null && ActivationFactory.getInstance().isSupported(_aType) && !getReport().isFail();
    }

    public ActivationFactory.Type getActivationType() { return isRecreatable() ? _aType : null; }

    public void initReportList(List<BackgroundReport> reportList) {
        if (_groupMonitorList==null) {
            _groupMonitorList= new ArrayList<MonitorItem>(reportList.size());
            MonitorItem mi;
            for(BackgroundReport r : reportList) {
                mi= new MonitorItem();
                _groupMonitorList.add(mi);
                mi.setReport(r);

            }
            String taskID= CLIENT_ID + (_cnt++);
            BackgroundReport report= new CompositeReport(taskID,reportList);
            setReport(report);
        }
        else {
            WebAssert.argTst(false, "You cannot call initReportList more then once");
        }
    }

    public void setCanceller(CanCancel canceler) { _canceler= canceler; }
    public CanCancel getCanceller() { return _canceler; }

    public void setReport(BackgroundReport report) {
        _report= report;
        fireUpdate();
    }

    public Widget makeActivationUI(int idx) {
        return _activation==null ?
               new Label("hidden") :
               _activation.buildActivationUI(this,idx, isActivated(idx));
    }

    public void activate(int idx) { activate(idx,false); }

    public void activate(int idx, boolean byAutoActivation) {
        if (_activation!=null) _activation.activate(this,idx, byAutoActivation);
    }

    public boolean getActivateOnCompletion() {
        return _activation.getActivateOnCompletion();
    }


    public boolean isComposite() { return _groupMonitorList!=null &&_groupMonitorList.size()>0;}

    public List<MonitorItem> getCompositeList() {
        return _groupMonitorList;
    }

    public void cancel() {
        if (_canceler!=null) _canceler.cancelTask();
        else new DefaultCanceler(this).cancelTask();
    }
    public BackgroundState getState() { return _report.getState(); }
    public String getID() {
        WebAssert.argTst(_report!=null, "You have not yet set a report to monitor, use setReport");
        return _report.getID();
    }
    public boolean isDone() { return _report.isDone(); }

    public boolean getImmediately() { return _activation!=null && _activation.getImmediately(); }

    public boolean getShowParts() { return _showParts; }
    public void setShowParts(boolean showParts) { _showParts= showParts; }

    public boolean isActivated(int idx) {
        boolean retval= false;
        if (_activated.containsKey(idx)) {
            retval= _activated.get(idx);
        }
        return retval;
    }

    public void setActivated(int idx, boolean activated) {
        if (!_activated.containsKey(idx) ||
                (_activated.containsKey(idx) && _activated.get(idx)!=activated))  {
            _activated.put(idx,activated);
            fireUpdate();
        }
    }

    public void setWatchable(boolean watch) {
        boolean oldWatch= _watch;
        _watch= watch;
        if (_watch!=oldWatch) {
            fireUpdate();
        }
    }
    public boolean isWatchable() { return _watch;}



    public String getWaitingMsg() {
        String retval;
        if (_activation!=null) {
            retval= _activation.getWaitingMsg();
        }
        else {
            retval= "Working...";
        }
        return retval;
    }
    public String getTitle() { return _title; }

    public BackgroundReport getReport() { return _report; }
    public CompositeReport getCompositeReport() {
        return (isComposite() && _report instanceof CompositeReport) ? (CompositeReport)_report : null; 
    }


    private void fireUpdate() {
        if (_report!=null) {
            WebEvent<MonitorItem> ev= new WebEvent<MonitorItem>(this, Name.MONITOR_ITEM_UPDATE,
                                                                this);
            WebEventManager.getAppEvManager().fireEvent(ev);
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
            BackgroundReport report= _monItem.getReport();
            report= report.cloneWithState(BackgroundState.USER_ABORTED);
            _monItem.setReport(report);
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
