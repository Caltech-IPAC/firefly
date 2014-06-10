package edu.caltech.ipac.firefly.core.background;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * User: roby
 * Date: Dec 15, 2009
 * Time: 10:27:30 AM
 */


/**
 * @author Trey Roby
 */
public class MonitorItem extends BaseMonitorItem {

    private ActivationFactory.Type _aType= null;
    private final BackgroundActivation _activation;
    private boolean recreated= false;
    private boolean _showParts= true;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public MonitorItem(String title,
                       ActivationFactory.Type type,
                       boolean immediate,
                       boolean watchable) throws IllegalArgumentException {
        super(title,watchable);
        _activation = getActivation(type,immediate);
        _aType= type;
    }

    public MonitorItem(String title,
                       ActivationFactory.Type type,
                       boolean immediate) throws IllegalArgumentException {
        this(title,type,immediate,true);
    }

    public MonitorItem(String title, BackgroundActivation activation) {
        super(title);
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
        return _aType!=null && ActivationFactory.getInstance().isSupported(_aType) && !getStatus().isFail();
    }

    public ActivationFactory.Type getActivationType() { return isRecreatable() ? _aType : null; }

    public Widget makeActivationUI(int idx) {
        return _activation==null ?
               new Label("hidden") :
               _activation.buildActivationUI(this,idx, isActivated(idx));
    }

    public void activate() { activate(0,false); }

    public void activate(int idx, boolean byAutoActivation) {
        if (_activation!=null) _activation.activate(this,idx, byAutoActivation);
    }

    public boolean getActivateOnCompletion() {
        return _activation.getActivateOnCompletion();
    }

    public boolean getImmediately() { return _activation!=null && _activation.getImmediately(); }

    public boolean getShowParts() { return _showParts; }
    public void setShowParts(boolean showParts) { _showParts= showParts; }


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
