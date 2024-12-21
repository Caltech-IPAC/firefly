/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.events;

import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.messaging.Message;
import edu.caltech.ipac.firefly.messaging.Messenger;

import static edu.caltech.ipac.firefly.messaging.Message.Event.TOPIC;

/**
 * Use Messenger to deliver events.
 */
public class MessageEventWorker implements ServerEventManager.EventWorker {

    public MessageEventWorker() {
        Messenger.subscribe(TOPIC, msg -> {
            ServerEvent sev = Message.Event.parse(msg);
            if (sev != null) {
                processEvent(sev);
            }
        });
    }

    public void deliver(ServerEvent sev) {
        if (sev != null) {
            Messenger.publish(new Message.Event(sev));
        }
    }

    public void processEvent(final ServerEvent sev) {
        ServerEventManager.processEvent(sev);
    }
}

