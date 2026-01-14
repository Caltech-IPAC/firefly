/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


package edu.caltech.ipac.firefly.server.events;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class ReplicatedQueueList {

   private static final StringKey HOST_NAME= new StringKey(FileUtil.getHostname());
   private static final String REP_QUEUE_MAP = "ReplicatedEventQueueMap";
   private static Cache<EventQueueList> getCache() {
       return CacheManager.getDistributedMap(REP_QUEUE_MAP);
   }

   public record EventQueueList(List<ServerEventQueue> items) {}

   synchronized void setQueueListForNode(List<ServerEventQueue> list)  {
      getCache().put(HOST_NAME, new EventQueueList(list));
   }

   synchronized List<ServerEventQueue> getCombinedNodeList()  {
       List<ServerEventQueue> retList= new ArrayList<>();
       Cache<EventQueueList> cache= getCache();
       for(CacheKey k : cache.getKeys()) {
           retList.addAll(cache.get(k).items);
       }
       return retList;
   }

}
