/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.sse;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.util.event.Name;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/sticky/firefly/events")
public class WebsocketEndpoint {
    private ServerSentEventQueue eventQueue;

    @OnOpen
    public void onOpen(final Session session) {
        System.out.print(">>>> new websocket session: " + session.getId());
        System.out.print("     > userkey: " + ServerContext.getRequestOwner().getUserKey());
         eventQueue = new ServerSentEventQueue(ServerContext.getRequestOwner().getUserKey(), new EventMatchCriteria(EventTarget.ALL), new ServerSentEventQueue.Sender() {
            @Override
            public void send(String message) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    onClose(null, new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, "Unable to send message."));
                }
            }
        });
        ServerEventManager.addEventQueueForClient(eventQueue);

    }

    @OnMessage
    public void onMessage(String message, Session session) {
        ServerEventManager.fireEvent(new ServerSentEvent(Name.APP_ONLOAD, EventTarget.ALL, new EventData(message)));
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        if (eventQueue != null) {
            ServerEventManager.removeEventQueue(eventQueue);
        }
    }
}

