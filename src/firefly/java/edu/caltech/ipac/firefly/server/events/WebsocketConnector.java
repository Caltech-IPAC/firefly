/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.events;

import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
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
    private ServerEventQueue eventQueue;

    @OnOpen
    public void onOpen(final Session session) {
        this.session = session;
        try {
            Map<String, List<String>> params = session.getRequestParameterMap();
            channelID = params.containsKey(CHANNEL_ID) ? String.valueOf(params.get(CHANNEL_ID).get(0)) : null;
            channelID = StringUtils.isEmpty(channelID) ? ServerContext.getRequestOwner().getUserKey() : channelID;
            eventQueue = new ServerEventQueue(session.getId(), channelID, this);
            ServerEvent connected = new ServerEvent(Name.EVT_CONN_EST, ServerEvent.Scope.SELF, "{\"connID\": \"" + session.getId() + "\", \"channel\": \"" + channelID + "\"}");
            send(ServerEventQueue.convertToJson(connected));
            ServerEventManager.addEventQueue(eventQueue);
            // notify clients within the same channel
            updateClientConnections(CONN_UPDATED, channelID);

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
        updateClientConnections(CONN_UPDATED, channelID);
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
     * So, event though they have different channels, we notify them both when connections are
     * added/removed from them.
     * The event will be sent to both of them.
     * @param type  action type.  Either "app_data.wsConnAdded" or "app_data.wsConnRemoved"
     * @param channelID
     */
    private void updateClientConnections(String type, String channelID) {
        List<ServerEventQueue> conns = ServerEventManager.getEvQueueList();
        String baseChannel = channelID.replace("__viewer", "");
        String viewerChannel = baseChannel + "__viewer";
        ArrayList<String> baseList = new ArrayList<>();
        ArrayList<String> viewerList = new ArrayList<>();
        FluxAction action = new FluxAction(type);
        for (ServerEventQueue q : conns) {
            if (q.getChannel().equals(baseChannel)) {
                baseList.add(q.getConnID());
            }else if (q.getChannel().equals(viewerChannel)) {
                viewerList.add(q.getConnID());
            }
        }
        action.setValue(baseList, baseChannel);
        action.setValue(viewerList, viewerChannel);
        ServerEventManager.fireAction(action, baseChannel);
        ServerEventManager.fireAction(action, viewerChannel);

    }

}

