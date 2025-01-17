/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


package edu.caltech.ipac.firefly.server.events;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
class ReplicatedQueueList {

   private static final StringKey HOST_NAME= new StringKey(FileUtil.getHostname());
   private static final String REP_QUEUE_MAP = "ReplicatedEventQueueMap";
   private static Cache<List<ServerEventQueue>> getCache() {
       return CacheManager.getDistributedMap(REP_QUEUE_MAP);
   }

   synchronized void setQueueListForNode(List<ServerEventQueue> list)  {
      getCache().put(HOST_NAME, list);
   }

   synchronized List<ServerEventQueue> getCombinedNodeList()  {
       List<ServerEventQueue> retList= new ArrayList<>();
       Cache<List<ServerEventQueue>> cache= getCache();
       for(String k : cache.getKeys()) {
           retList.addAll(cache.get(new StringKey(k)));
       }
       return retList;
   }

}
