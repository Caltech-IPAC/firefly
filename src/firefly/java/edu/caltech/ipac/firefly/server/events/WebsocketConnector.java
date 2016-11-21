/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.events;

import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.server.AlertsMonitor;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.BackgroundEnv;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/sticky/firefly/events")
public class WebsocketConnector implements ServerEventQueue.EventConnector {
    public static final String CHANNEL_ID = "channelID";
    public static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final String CONN_UPDATED = "app_data.wsConnUpdated";
    private Session session;
    private String channelID;
    private String userKey;
    private ServerEventQueue eventQueue;

    @OnOpen
    public void onOpen(final Session session) {
        this.session = session;
        try {
            Map<String, List<String>> params = session.getRequestParameterMap();
            userKey = ServerContext.getRequestOwner().getUserKey();
            channelID = params.containsKey(CHANNEL_ID) ? String.valueOf(params.get(CHANNEL_ID).get(0)) : null;
            channelID = StringUtils.isEmpty(channelID) ? userKey : channelID;
            eventQueue = new ServerEventQueue(session.getId(), channelID, userKey, this);
            ServerEvent connected = new ServerEvent(Name.EVT_CONN_EST, ServerEvent.Scope.SELF, "{\"connID\": \"" + session.getId() + "\", \"channel\": \"" + channelID + "\"}");
            send(ServerEventQueue.convertToJson(connected));
            ServerEventManager.addEventQueue(eventQueue);
            onClientConnect(session.getId(), channelID, userKey);
        } catch (Exception e) {
            LOG.error(e, "Unable to open websocket connection:" + session.getId());
        }
    }

    @OnMessage
    public void onMessage(String message) {
        try {

            if (StringUtils.isEmpty(message)) return;  // ignore empty messages

            ServerEvent event = ServerEventQueue.parseJsonEvent(message);
            event.setFrom(session.getId());
            if (event.getTarget().getScope() == ServerEvent.Scope.CHANNEL ) {
                event.getTarget().setChannel(channelID);
            } else {
                event.getTarget().setConnID(session.getId());
            }
            ServerEventManager.fireEvent(event);
        } catch (Exception e) {
            LOG.error(e, "Error while interpreting incoming json message:" + message);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        ServerEventManager.removeEventQueue(eventQueue);
        updateClientConnections(CONN_UPDATED, channelID, userKey);
        LOG.error("Websocket connection closed:" + session.getId() + " - " + closeReason.getReasonPhrase());
    }

//====================================================================
//  ServerEventQueue.EventTerminal implementation
//====================================================================

    public void send(String message) throws Exception {
        if (session == null) {
            throw new IOException("No longer available");
        }
        session.getBasicRemote().sendText(message);
    }

    public boolean isOpen() {
        return session != null && session.isOpen();
    }

    public void close() {
        if (session != null) {
            try {
                session.close();
            } catch (IOException e) {
                // can ignore
                LOG.error("Fail to close " + this.getClass().getName() + " with ID:" + session.getId());
            }
        }
    }

    /**
     * notify the clients that connections have been updated from the given channel.
     * In order for external viewer to work correctly, we have to treat them as pair.
     * So, even though they have different channels, we notify them both when connections are
     * added/removed from them.
     * The event will be sent to both of them.
     * @param type  action type.  Either "app_data.wsConnAdded" or "app_data.wsConnRemoved"
     * @param channelID
     */
    private void updateClientConnections(String type, String channelID, String userKey) {
        List<ServerEventQueue> conns = ServerEventManager.getEvQueueList();
        for (ServerEventQueue seq : conns) {
            // need to notify clients that are affected by update
            if (seq.getChannel().equals(channelID) || seq.getUserKey().equals(userKey)) {
                // Creates a map of all the channels and its connections that is visible to this user.
                // This user has knowledge of all connections started by this user, as well as connections associated with this user's channel.
                Map<String, List<String>> connInfo = new HashMap<>();
                conns.stream()
                        .filter(q -> q.getChannel().equals(channelID) || q.getUserKey().equals(userKey))
                        .forEach(q -> {
                            List<String> l = connInfo.get(q.getChannel());
                            if (l == null) {
                                l = new ArrayList<>();
                                connInfo.put(q.getChannel(), l);
                            }
                            if (!l.contains(q.getConnID())) l.add(q.getConnID());
                        });
                FluxAction action = new FluxAction(type, connInfo);
                ServerEventManager.fireAction(action, new ServerEvent.EventTarget(ServerEvent.Scope.SELF, seq.getConnID(), null, null));
            }
        }
    }

    private void onClientConnect(String connId, String channel, String userKey) {
        ServerContext.getRequestOwner().setWsConnInfo(connId, channel);
        // notify clients within the same channel
        updateClientConnections(CONN_UPDATED, channel, userKey);
        // check for alerts
        AlertsMonitor.checkAlerts(true);
        // check for background jobs
        BackgroundEnv.getUserBackgroundInfo().stream()
                    .forEach(bgc -> bgc.fireBackgroundJobAdd());

    }

}

