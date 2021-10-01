/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.events;

import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.util.StringUtils;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

@ServerEndpoint(value = "/sticky/firefly/events")
public class WebsocketConnector implements ServerEventQueue.EventConnector {
    public static final String CHANNEL_ID = "channelID";
    public static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final String CONN_UPDATED = "app_data.wsConnUpdated";
    private Session session;
    private String channelID;
    private String userKey;
    private ServerEventQueue eventQueue;
    private final ReentrantLock lock = new ReentrantLock();

    private static final long WS_TIMEOUT = 30*1000;  // give up after 30 sec when sending msg
    private static final long WS_MAX_IDLE = 10*60*1000; // drop session when idled for over 10 mins.  keep-alive ping happens every 5 mins.
    private static final String PREFIX= "Channel: WS";

    @OnOpen
    public void onOpen(final Session session) {
        this.session = session;
        this.session.getContainer().setAsyncSendTimeout(WS_TIMEOUT);
        this.session.getContainer().setDefaultMaxSessionIdleTimeout(WS_MAX_IDLE);
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
            LOG.info(PREFIX+" open "+makeChannelStr(session));
        } catch (Exception e) {
            LOG.error(e, PREFIX+" Unable to open websocket connection:" + makeChannelStr(session));
        }
    }

    @OnMessage
    public void onMessage(String message) {
        try {

            if (StringUtils.isEmpty(message)) {
                LOG.trace(PREFIX+" PING from "+makeChannelStr(session));
                return;  // ignore empty messages
            }

            ServerEvent event = ServerEventQueue.parseJsonEvent(message);
            event.setFrom(session.getId());
            if (event.getTarget().getScope() == ServerEvent.Scope.CHANNEL ) {
                event.getTarget().setChannel(channelID);
            } else {
                event.getTarget().setConnID(session.getId());
            }
            ServerEventManager.fireEvent(event);
            LOG.trace(String.format("%s msg from %s: %s", PREFIX, makeChannelStr(session), message));
        } catch (Exception e) {
            LOG.error(e, PREFIX+" Error while interpreting incoming json message:" + message);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.trace(String.format("%s error %s: %s", PREFIX, makeChannelStr(session), throwable.getMessage()));
        onClose(null, null);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        ServerEventManager.removeEventQueue(eventQueue);
        updateClientConnections(CONN_UPDATED, channelID, userKey);
        if (closeReason!=null && session!=null) {
            String reason= closeReason.getCloseCode().toString() +
                    (closeReason.getReasonPhrase()!=null ? " - "+closeReason.getReasonPhrase() : "");

            LOG.info(String.format("%s closed %s: %s", PREFIX, makeChannelStr(session), reason));
        }
        else {
            LOG.trace(PREFIX+" closed (after onError)");
        }
    }

//====================================================================
//  ServerEventQueue.EventTerminal implementation
//====================================================================

    public void send(String message) throws Exception {
        if (session == null) {
            throw new IOException("No longer available");
        }
        lock.lock();
        try {
            Future<Void> f= session.getAsyncRemote().sendText(message);     // use async so it will timed out after WS_TIMEOUT is reached.
            f.get();                                                        // wait until websocket send completes, ignore the return
        } finally {
            lock.unlock();
        }
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
     * send a ping to the given user with the given WS connection ID
     * @param userKey
     */
    public static void pingClient(String userKey, String eventConnId) {
        List<ServerEventQueue> conns = ServerEventManager.getEvQueueList();
        for (ServerEventQueue seq : conns) {
            // need to notify clients that are affected by update
            if (seq.getConnID().equals(eventConnId) || seq.getUserKey().equals(userKey)) {
                ServerEvent.EventTarget target = new ServerEvent.EventTarget(ServerEvent.Scope.SELF, seq.getConnID(), null, null);
                ServerEvent sev = new ServerEvent(Name.PING, target, "");
                ServerEventManager.fireEvent(sev);
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
//        List<ServerEventQueue> conns = ServerEventManager.getEvQueueList();
        List<ServerEventQueue> conns = ServerEventManager.getAllServerEvQueueList();
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
    }

    private String makeChannelStr(final Session session) {
        return String.format("(%s_%s)", session.getId(),channelID);
    }

}

