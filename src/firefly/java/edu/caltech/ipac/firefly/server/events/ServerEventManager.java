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
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.util.StringUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Trey Roby
 */
public class ServerEventManager {

    private static final boolean USE_MESSAGE_EVENT_WORKER = true;
    private static final EventWorker eventWorker = USE_MESSAGE_EVENT_WORKER ?
                                                    new MessageEventWorker() : new LocalEventWorker();
    private static final List<ServerEventQueue> localEventQueues = new CopyOnWriteArrayList<>();
    private static final ReplicatedQueueList allEventQueues = new ReplicatedQueueList();
    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static long totalEventCnt;
    private static long deliveredEventCnt;


    /**
     * Send this action to the calling client, i.e Scope.SELF.
     * @param action
     */
    public static void fireAction(FluxAction action) {
        fireAction(action, ServerEvent.Scope.SELF);
    }

    /**
     * Send this action to the calling client, i.e Scope.SELF.
     * @param action
     */
    public static void fireAction(FluxAction action, ServerEvent.Scope scope) {
        fireAction(action, makeTarget(scope));
    }

    /**
     * Send this action to a specific client based on the given target.
     * @param action
     * @param target
     */
    public static void fireAction(FluxAction action, ServerEvent.EventTarget target) {
        fireJsonAction(action.toString(), target);
    }

    /**
     * Send this JSON string action to a specific client based on the given target.
     * @param actionStr
     * @param target
     */
    public static void fireJsonAction(String actionStr, ServerEvent.EventTarget target) {
        ServerEvent sev = new ServerEvent(Name.ACTION, target, ServerEvent.DataType.JSON, actionStr);
        ServerEventManager.fireEvent(sev);
    }



    public static void fireEvent(ServerEvent sev) {
        if (sev == null || sev.getTarget() == null || !sev.getTarget().hasDestination()) {
            LOG.warn("Something is wrong with this ServerEvent: " + String.valueOf(sev));
        } else {
            autoFillTarget(sev.getTarget());
            eventWorker.deliver(sev);
        }
    }

    public static ServerEvent.EventTarget makeTarget(ServerEvent.Scope scope) {
        var target = new ServerEvent.EventTarget(scope);
        autoFillTarget(target);
        return target;
    }

    private static void autoFillTarget(ServerEvent.EventTarget target) {
        if (target == null) return;
        ServerEvent.Scope scope = target.getScope();
        // if target is missing key information, get it from RequestOwner
        if (scope == ServerEvent.Scope.CHANNEL && target.getChannel() == null) {
            target.setChannel(ServerContext.getRequestOwner().getEventChannel());
        } else if (scope == ServerEvent.Scope.USER && target.getUserKey() == null) {
            target.setUserKey(ServerContext.getRequestOwner().getUserKey());
        } else if (scope == ServerEvent.Scope.SELF && target.getConnID() == null) {
            target.setConnID(ServerContext.getRequestOwner().getEventConnID());
        }
    }

    public static void addEventQueue(ServerEventQueue queue) {
        Logger.briefInfo("Channel: create new Queue for: "+ queue.getQueueID() );
        localEventQueues.add(queue);
        allEventQueues.setQueueListForNode(localEventQueues);
    }

    /**
     * Get the list of ServerEventQueue that are local to this node.
     * @return list of ServerEventQueue
     */
    static List<ServerEventQueue> getLocalEventQueues() {
        return localEventQueues;
    }

    /**
     * Get the list of all ServerEventQueue across all nodes(multiple instances of Firefly).
     * @return list of ServerEventQueue
     */
    static List<ServerEventQueue> getAllEventQueue() {
        return  allEventQueues.getCombinedNodeList();
    }

    static void processEvent(ServerEvent ev) {
        totalEventCnt++;
        boolean delivered = false;
        for(ServerEventQueue queue : localEventQueues) {
            try {
                if (queue.matches(ev)) {
                    try {
                        queue.putEvent(ev);
                        delivered = true;
                    } catch (Exception e) {
                        // queue is bad..  release it.
                        LOG.warn("Event queue is bad.. releasing it:" + queue.getQueueID(), "Exception: "+e.getMessage());
                        if (queue.getEventConnector() != null) {
                            queue.getEventConnector().close();
                        }
                        removeEventQueue(queue);  // cleanup.. but only as a precaution.  WebsocketConnector should have done it already.
                    }
                }
            }catch (Exception e) {
                LOG.warn(e, "Unexpected exception while processing event: " + ev + " for queue:" + queue == null ? "null" : queue.getQueueID());
            }
        }
        if (delivered) deliveredEventCnt++;
    }

    public static void removeEventQueue(ServerEventQueue queue) {
        localEventQueues.remove(queue);
        allEventQueues.setQueueListForNode(localEventQueues);
    }

//====================================================================
//  For stats
//====================================================================

    public static int getActiveQueueCnt() {
        int cnt = 0;
        for(ServerEventQueue queue : localEventQueues) {
            if (queue.getEventConnector().isOpen()) {
                cnt++;
            }
        }
        return cnt;
    }

    public static List<ServerEventQueue.QueueDescription> getQueueDescriptionList(int limit) {
        return localEventQueues.stream()
                .map(ServerEventQueue::convertToDescription)
                .sorted((d1,d2) -> (int)(d2.lastPutTime()-d1.lastPutTime()))
                .limit(limit)
                .toList();
    }


    /**
     * bad logic.. removing good eventQueue(ws connetions)..
     * this is still used by python.  should use websocket communication to
     * determine these info.
     */
    @Deprecated
    public static int getActiveQueueChannelCnt(String channel) {
        int cnt = 0;
        if (StringUtils.isEmpty(channel)) return 0;
        for(ServerEventQueue queue : localEventQueues) {
            if (channel.equals(queue.getChannel()) && queue.getEventConnector().isOpen()) {
                cnt++;
            } else {
//                removeEventQueue(queue);
            }
        }
        return cnt;
    }


    public static long getTotalEventCnt() {
        return totalEventCnt;
    }

    public static long getDeliveredEventCnt() {
        return deliveredEventCnt;
    }


//====================================================================
//
//====================================================================

    public interface EventWorker {
        void deliver(ServerEvent sev);
    }

    private static class LocalEventWorker implements EventWorker {
        public void deliver(ServerEvent sev) {
            ServerEventManager.processEvent(sev);
        }
    }


}

