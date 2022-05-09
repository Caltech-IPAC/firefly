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

    private static final boolean USE_CACHE_EVENT_WORKER = true;
    private static final EventWorker eventWorker = USE_CACHE_EVENT_WORKER ?
                                                    new CacheEventWorker() : new SimpleEventWorker();
    private static final List<ServerEventQueue> evQueueList= new CopyOnWriteArrayList<ServerEventQueue>();
    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static long totalEventCnt;
    private static long deliveredEventCnt;
    private static ReplicatedQueueList repQueueList= new ReplicatedQueueList();


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
        fireJsonAction(action.toString(), scope);
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
     * Send this JSON string action to the client based on the given scope
     * @param actionStr
     * @param scope
     */
    public static void fireJsonAction(String actionStr, ServerEvent.Scope scope) {
        ServerEvent sev = new ServerEvent(Name.ACTION, scope, ServerEvent.DataType.JSON, actionStr);
        ServerEventManager.fireEvent(sev);
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
            ServerEvent.Scope scope = sev.getTarget().getScope();
            // if target is missing key information, get it from RequestOwner
            if (scope == ServerEvent.Scope.CHANNEL && sev.getTarget().getChannel() == null) {
                sev.getTarget().setChannel(ServerContext.getRequestOwner().getEventChannel());
            } else if (scope == ServerEvent.Scope.USER && sev.getTarget().getUserKey() == null) {
                sev.getTarget().setUserKey(ServerContext.getRequestOwner().getUserKey());
            } else if (scope == ServerEvent.Scope.SELF && sev.getTarget().getConnID() == null) {
                sev.getTarget().setConnID(ServerContext.getRequestOwner().getEventConnID());
            }
            eventWorker.deliver(sev);
        }
    }

    public static void addEventQueue(ServerEventQueue queue) {
        Logger.briefInfo("Channel: create new Queue for: "+ queue.getQueueID() );
        evQueueList.add(queue);
        repQueueList.setQueueListForNode(evQueueList);
    }

    static List<ServerEventQueue> getEvQueueList() {
        return evQueueList;
    }

    static List<ServerEventQueue> getAllServerEvQueueList() {
        return repQueueList.getCombinedNodeList();
    }

    static void processEvent(ServerEvent ev) {
        totalEventCnt++;
        boolean delivered = false;
        for(ServerEventQueue queue : evQueueList) {
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
        evQueueList.remove(queue);
        repQueueList.setQueueListForNode(evQueueList);
    }

//====================================================================
//  For stats
//====================================================================

    public static int getActiveQueueCnt() {
        int cnt = 0;
        for(ServerEventQueue queue : evQueueList) {
            if (queue.getEventConnector().isOpen()) {
                cnt++;
            }
        }
        return cnt;
    }

    public static List<ServerEventQueue.QueueDescription> getQueueDescriptionList(int limit) {
        return evQueueList.stream()
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
        for(ServerEventQueue queue : evQueueList) {
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
        public void deliver(ServerEvent sev);
    }

    private static class SimpleEventWorker implements EventWorker {
        public void deliver(ServerEvent sev) {
            ServerEventManager.processEvent(sev);
        }
    }


}

