/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: Aug 4, 2010
 *
 * @author loi
 * @version $Id: BaseEventWorker.java,v 1.14 2012/09/21 23:35:38 roby Exp $
 */
public abstract class BaseEventWorker<ReturnType> implements EventWorker, WebEventListener {

    private EventHub eventHub;
    private List<String> querySources;
    private List<Name> events = new ArrayList<Name>();
    private String type;
    private String desc;
    private String id= DEFAULT_ID;
    private Map<String, String> params= null;
    private boolean enabled= true;
    private WebEvent _lastEvent= null;
    private int delayTime = 0;


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

    public void setDelayTime(int delayTime) {
        this.delayTime = delayTime;
    }

    public int getDelayTime() {
        return delayTime;
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

    public EventHub getEventHub() {
        return eventHub;
    }

    public void bind(EventHub hub) {
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

    public void setParam(String key, String value) {
        if (params==null) params= new HashMap<String, String>(5);
        params.put(key,value);
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
        if (useEvent(ev)) {
            _lastEvent= ev;
            if (enabled) {
                if (getEventHub()!=null) {
                    getEventHub().getEventManager().fireEvent(new WebEvent<ReturnType>(
                            this, EventHub.ON_EVENT_WORKER_START, null));
                }
                handleEvent(ev);
            }
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
                    this, EventHub.ON_EVENT_WORKER_COMPLETE, data));
        }
    }



    protected boolean useEvent(WebEvent ev) { return true; }

    abstract protected void handleEvent(WebEvent ev);

}
