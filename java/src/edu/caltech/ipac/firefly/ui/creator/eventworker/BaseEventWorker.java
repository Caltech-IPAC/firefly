package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Date: Aug 4, 2010
 *
 * @author loi
 * @version $Id: BaseEventWorker.java,v 1.14 2012/09/21 23:35:38 roby Exp $
 */
public abstract class BaseEventWorker<ReturnType> implements EventWorker, WebEventListener {

    private TablePreviewEventHub eventHub;
    private List<String> querySources;
    private List<Name> events = new ArrayList<Name>();
    private String type;
    private String desc;
    private String id= DEFAULT_ID;
    private Map<String, String> params;
    private boolean enabled= true;
    private WebEvent _lastEvent= null;


    protected BaseEventWorker(String type) {
        this.type = type;
    }

    public void setQuerySources(List<String> querySources) {
        this.querySources = querySources;
    }

    public void setEvents(List<String> events) {
        if (events == null) {
            this.events.clear();
        } else {
            for (String s : events) {
                this.events.add(new Name(s, ""));
            }
        }
    }


    public void setEventsByName(List<Name> eventNames) {
        if (events == null) {
            this.events.clear();
        } else {
            this.events.addAll(eventNames);
        }
    }


    public void setType(String type) {
        this.type = type;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setID(String id) {
        this.id= id;
    }

    public List<String> getQuerySources() {
        return querySources;
    }

    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }


    public String getID() {
        return id;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled!=enabled) {
            this.enabled= enabled;
            if (enabled) {
               if (_lastEvent!=null) {
                   eventNotify(_lastEvent);
               }

            }
            else {
                fireEvent(null);
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<Name> getEvents() {
        return events;
    }

    public TablePreviewEventHub getEventHub() {
        return eventHub;
    }

    public void bind(TablePreviewEventHub hub) {
        eventHub = hub;
        for (Name n : events) {
            if (n.equals(Name.SEARCH_RESULT_END)) {
                WebEventManager.getAppEvManager().addListener(Name.SEARCH_RESULT_END, this);
                WebEventManager.getAppEvManager().addListener(Name.SEARCH_RESULT_START, new WebEventListener(){
                                            public void eventNotify(WebEvent ev) {
                                                WebEventManager.getAppEvManager().removeListener(Name.SEARCH_RESULT_END, BaseEventWorker.this);
                                                WebEventManager.getAppEvManager().removeListener(Name.SEARCH_RESULT_START, this);
                                            }
                                        });
            } else {
                eventHub.getEventManager().addListener(n, this);
            }
        }
        hub.bind(this);
    }

    protected void setParams(Map<String, String> params) {
        this.params = params;
    }

    protected String getParam(String key) {
        return params == null ? null : params.get(key);
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void eventNotify(WebEvent ev) {
        // check to see if this worker should handle this event
        if (!getEvents().contains(ev.getName()) && !ev.getName().equals(Name.BYPASS_EVENT)) {
            return;
        }
        _lastEvent= ev;
        if (enabled) {
            if (getEventHub()!=null) {
                getEventHub().getEventManager().fireEvent(new WebEvent<ReturnType>(
                        this, TablePreviewEventHub.ON_EVENT_WORKER_START, null));
            }
            handleEvent(ev);
        }
    }

    protected void handleResults(ReturnType data) {
        if (enabled)  {
            fireEvent(data);
        }
    }

    protected void setLastEvent(WebEvent ev) { _lastEvent= ev; }

    private void fireEvent(ReturnType data) {
        if (getEventHub()!=null) {
            getEventHub().getEventManager().fireEvent(new WebEvent<ReturnType>(
                    this, TablePreviewEventHub.ON_EVENT_WORKER_COMPLETE, data));
        }
    }

    abstract protected void handleEvent(WebEvent ev);

}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
