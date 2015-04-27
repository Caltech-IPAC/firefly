/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.events;
/**
 * User: roby
 * Date: 2/25/14
 * Time: 4:04 PM
 */


import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.util.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.Serializable;

/**
 * @author Trey Roby
 */
public class ServerEventQueue {
    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    public static String SERVER_TERM_ID = "-1";
    private final EventTerminal eventTerminal;
    private String termID;
    private String channel;

    public ServerEventQueue(String connID, String channel, EventTerminal terminal) {
        this.termID = connID;
        this.channel = channel;
        this.eventTerminal = terminal;

    }

    public void putEvent(ServerEvent ev) throws Exception{
        if (eventTerminal ==null){
            throw new IllegalStateException("Event terminal is null.. should remove this queue.");
        }
        String message = convertToJson(ev);
        if (message != null) {
            eventTerminal.send(message);
        }
    }

    public EventTerminal getEventTerminal() {
        return eventTerminal;
    }

    public String getQueueID() {
        return termID + "_" + channel;
    }

    public boolean matches(ServerEvent sEvent) {
        if (true) return true;
        try {
            ServerEvent.EventTarget evTarget = sEvent.getTarget();

            if (termID.equals(ServerEvent.SERVER_TERM_ID) && !String.valueOf(sEvent.getFrom()).equals(SERVER_TERM_ID)) {
                // if there is a server's terminal, it'll get all the events.
                return true;
            }

            if (!StringUtils.isEmpty(evTarget.getTermID()) && !evTarget.getTermID().equals(termID)) {
                return false;
            }
            if (!StringUtils.isEmpty(evTarget.getChannel()) && !evTarget.getChannel().equals(channel)) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String convertToJson(ServerEvent ev) {
        try {
            ServerEvent.EventTarget target = ev.getTarget();
            ServerEvent.Scope scope = (target == null || target.getScope() == null) ?
                    ServerEvent.Scope.CLIENT : ServerEvent.Scope.CHANNEL;
            JSONObject eventJ = new JSONObject();
            eventJ.put("name", ev.getName().getName());
            eventJ.put("scope", scope.name());
            eventJ.put("dataType", ev.getDataType().name());
            if (ev.getData() instanceof BackgroundStatus) {
                eventJ.put("data", ((BackgroundStatus) ev.getData()).serialize());
            } else if (ev.getData().toString().trim().startsWith("{") ) {
                eventJ.put("data", JSONValue.parse(ev.getData().toString()));
            } else {
                eventJ.put("data", ev.getData());
            }
            if (!StringUtils.isEmpty(ev.getFrom())) {
                eventJ.put("from", ev.getFrom());
            }
            return eventJ.toJSONString();
        } catch (Exception e) {
            LOG.warn(e, "Fail to convert ServerEvent to json: " + ev);
            return null;
        }
    }

    public static ServerEvent parseJsonEvent(String message) {
        try {
            JSONObject eventJ= (JSONObject) JSONValue.parse(message);
            Name name = Name.parse(String.valueOf(eventJ.get("name")));
            String scope = (String)eventJ.get("scope");
            ServerEvent.DataType dtype = eventJ.get("dataType") == null ? ServerEvent.DataType.STRING : ServerEvent.DataType.valueOf((String) eventJ.get("dataType"));
            Serializable data;
            if (dtype == ServerEvent.DataType.BG_STATUS) {
                data = BackgroundStatus.parse((String) eventJ.get("data"));
            } else if (dtype == ServerEvent.DataType.JSON) {
                data = (JSONObject)eventJ.get("data");
            } else {
                data = (String) eventJ.get("data");
            }
            return new ServerEvent(name, ServerEvent.Scope.valueOf(scope), dtype, data);
        } catch (Exception e) {
            LOG.warn(e, "Fail to parse json event string: " + message);
            return null;
        }
    }


//====================================================================
//
//====================================================================

    public static interface EventTerminal {
        public void send(String message) throws Exception;
        public void close();
    }
}

