/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util.event;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import edu.caltech.ipac.util.ComparisonUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/**
 * User: roby
 * Date: Dec 19, 2007
 * Time: 10:28:03 AM
 */


/**
 * This class is an event manager class.  It is a way to centralize all event handling
 * either at the application level or a some subset of classes.
 * You can add and fire listeners through this class.
 * @author Trey Roby
 */
public class WebEventManager {

    private final List<EvListenerContainer> _evListeners;
    private final List<EvListenerContainer> _vetoEvListeners;
    private static WebEventManager _applicationInstance= null;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public WebEventManager() {this(10,2);}

    private WebEventManager(int evSize, int vetoSize) {
        _evListeners= new ArrayList<EvListenerContainer>(evSize);
        _vetoEvListeners= new ArrayList<EvListenerContainer>(vetoSize);
    }

    public static WebEventManager getAppEvManager() {
        if (_applicationInstance==null) _applicationInstance= new WebEventManager(50,10);
        return _applicationInstance;
    }

    public void clear() {
        _evListeners.clear();
        _vetoEvListeners.clear();
    }

    /**
     * Add a WebEventListener.
     * This methods adds events for all sources and names
     * @param l the listener
     */
    public void addListener(WebEventListener l) { addListener(null, null, l); }

    /**
     * Add a WebEventListener.
     * This methods adds events for all sources
     * @param eventName limit the event name to only this event, null means all events name
     * @param l the listener
     */
    public void addListener(Name eventName, WebEventListener l) {
        addListener(eventName, null, l);
    }

    public void addListener(int idx, Name eventName, WebEventListener l) {
        addListener(idx, eventName, null, l);
    }
    /**
     * Add a VetoableWebEventListener.
     * @param eventName limit the event name to only this event, null means all events name
     * @param fromSource limit the source to only this source, null means all sources. You may spectify
 *         an instance Object for the source or a Class for the source.  If you specify a class then
     * @param l the listener
     */
    public void addListener(Name eventName, Object fromSource, WebEventListener l) {
        addListener(-1, eventName, fromSource, l);
    }

    /**
     * override to allow adding at a specify position.
     * @param idx   the position to add to.  item with out-of-bound idx will be added to the end of the list.
     * @param eventName
     * @param fromSource
     * @param l
     */
    public void addListener(int idx, Name eventName, Object fromSource, WebEventListener l) {
        if (idx > _evListeners.size() || idx < 0) {
            _evListeners.add(new EvListenerContainer(l,eventName, fromSource));
        } else {
            _evListeners.add(idx, new EvListenerContainer(l,eventName, fromSource));
        }
    }






    public void removeListener(WebEventListener l) {
        removeListener(null, null, l);
    }

    public void removeListener(Name eventName, WebEventListener l) {
        removeListener(eventName, null, l);
    }
    public void removeListener(Name eventName, Object fromSource, WebEventListener l) {
        EvListenerContainer lc= findEvListener(l,eventName,fromSource);
        if (lc!=null) _evListeners.remove(lc);
    }

    public void fireDeferredEvent(final WebEvent ev) {
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                WebEventManager.getAppEvManager().fireEvent(ev);
            }
        });

    }

    /**
     * Fire an event
     * @param ev the event to fire
     */
    public void fireEvent(WebEvent ev) {
        List<EvListenerContainer> newlist;

        WebEventListener listener;
        synchronized (this) {
            newlist = new ArrayList<EvListenerContainer>(_evListeners);
        }

        List<EvListenerContainer> toDel= new ArrayList<EvListenerContainer>(2);
        boolean fireBySource;
        boolean fireByName;
        for(EvListenerContainer lc : newlist)  {
            listener = lc.getListener();
            if (listener!=null) {
                fireBySource= (lc.getSource()==null ||
                               ev.getSource()==lc.getSource() ||
                               ev.getSource().getClass() == lc.getSource());
                fireByName= (lc.getEventName()==null ||
                             ComparisonUtil.equals(ev.getName(),lc.getEventName()));
                if (fireBySource && fireByName) {
                        listener.eventNotify(ev);
                }
            }
            else {
                toDel.add(lc);
            }
        }
        for(EvListenerContainer lc : toDel) _evListeners.remove(lc);
    }


    public synchronized void purgeSource(Object source) {
        for(Iterator<EvListenerContainer> i= _evListeners.iterator(); i.hasNext();) {
            if (i.next().getSource()==source) i.remove();
        }
        for(Iterator<EvListenerContainer> i= _vetoEvListeners.iterator(); i.hasNext();) {
            if (i.next().getSource()==source) i.remove();
        }
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private EvListenerContainer findEvListener(WebEventListener listener,
                                                      Name eventName,
                                                      Object fromSource) {
        EvListenerContainer retval= null;

        if (listener!=null) {
            for(EvListenerContainer lc : _evListeners) {
                if (listener==lc.getListener() &&
                    ComparisonUtil.equals(eventName,lc.getEventName()) &&
                    fromSource==lc.getSource() ) {
                    retval= lc;
                    break;
                }
            }
        }
        return retval;
    }


// =====================================================================
// -------------------- Static Inner Classes ---------------------------
// =====================================================================

    private static class EvListenerContainer {
        private final Object _source;
        private final WebEventListener _listener;
        private final Name  _eventName;

        public EvListenerContainer(WebEventListener listener,
                                  Name eventName,
                                  Object source) {
            _eventName= eventName;
            _source= source;
            _listener= listener;

        }

        public WebEventListener getListener() {
            return _listener;
        }


        public Name getEventName() {  return _eventName; }
        public Object getSource() {  return _source; }
    }

}

