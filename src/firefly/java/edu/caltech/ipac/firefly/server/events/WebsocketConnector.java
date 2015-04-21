/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.events;

import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.event.Name;

import java.io.IOException;
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
    private Session session;
    private String channelID;

    @OnOpen
    public void onOpen(final Session session) {
        this.session = session;
        try {
            Map<String, List<String>> params = session.getRequestParameterMap();
            channelID = params.containsKey(CHANNEL_ID) ? String.valueOf(params.get(CHANNEL_ID)) :
                                        ServerContext.getRequestOwner().getUserKey();
            ServerEventQueue eventQueue = new ServerEventQueue(session.getId(), channelID, this);
            ServerEvent connected = new ServerEvent(Name.EVT_CONN_EST, ServerEvent.Scope.SELF, "{\"connID\": \"" + session.getId() + "\", \"channel\": \"" + channelID + "\"}");
            send(ServerEventQueue.convertToJson(connected));
            ServerEventManager.addEventQueue(eventQueue);
        } catch (Exception e) {
            LOG.error(e, "Unable to open websocket connection:" + session.getId());
        }
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            ServerEvent event = ServerEventQueue.parseJsonEvent(message);
            event.setFrom(session.getId());
            if (event.getTarget().getScope() == ServerEvent.Scope.CHANNEL ) {
                event.getTarget().setChannel(channelID);
            } else {
                event.getTarget().setConnID(ServerEvent.SERVER_CONN_ID);
            }
            ServerEventManager.fireEvent(event);
        } catch (Exception e) {
            LOG.error(e, "Error while interpreting incoming json message:" + message);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
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
}

