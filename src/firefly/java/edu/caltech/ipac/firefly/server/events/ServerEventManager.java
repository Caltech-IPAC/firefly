/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.events;
/**
 * User: roby
 * Date: 2/25/14
 * Time: 1:06 PM
 */


import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trey Roby
 */
public class ServerEventManager {

    private static final boolean USE_CACHE_EVENT_WORKER = true;
    private static final EventWorker eventWorker = USE_CACHE_EVENT_WORKER ?
                                                    new CacheEventWorker() : new SimpleEventsContainer();
    private static final List<ServerEventQueue> evQueueList= new CopyOnWriteArrayList<ServerEventQueue>();
    private static final Logger.LoggerImpl LOG = Logger.getLogger();




    public static void fireEvent(ServerEvent sev) {
        if (!sev.getTarget().hasDestination()) {
            if (sev.getTarget().getScope() == ServerEvent.Scope.CHANNEL) {
                sev.getTarget().setChannel(ServerContext.getRequestOwner().getEventChannel());
            } else {
                sev.getTarget().setTermID(ServerContext.getRequestOwner().getEventTermID());
            }
        }
        eventWorker.deliver(sev);
    }

    public static ServerEventQueue addEventQueue(ServerEventQueue queue) {
        Logger.briefInfo("create new Queue for: "+ queue.getQueueID() );
        evQueueList.add(queue);
        return queue;
    }

    static List<ServerEventQueue> getEvQueueList() {
        return evQueueList;
    }

    static void processEvent(ServerEvent ev) {
        for(ServerEventQueue queue : evQueueList) {
            try {
                if (queue.matches(ev)) {
                    try {
                        queue.putEvent(ev);
                    } catch (Exception e) {
                        // queue is bad..  release it.
                        LOG.warn("Event queue is bad.. releasing it:" + queue.getQueueID());
                        if (queue.getEventTerminal() != null) {
                            queue.getEventTerminal().close();
                        }
                        evQueueList.remove(queue);
                    }
                }
            }catch (Exception e) {
                LOG.warn(e, "Unexpected exception while processing event: " + ev + " for queue:" + queue == null ? "null" : queue.getQueueID());
            }
        }
    }

    public static void removeEventQueue(ServerEventQueue queue) {
        evQueueList.remove(queue);
    }

///==============================================
///==============================================
///==============================================
//====================================================================
//
//====================================================================

    public interface EventWorker {
        public void deliver(ServerEvent sev);
    }

    private static class SimpleEventsContainer implements EventWorker {
        public void deliver(ServerEvent sev) {
            ServerEventManager.processEvent(sev);
        }
    }


}

