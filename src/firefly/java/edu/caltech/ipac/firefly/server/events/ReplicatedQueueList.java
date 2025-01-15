/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


package edu.caltech.ipac.firefly.server.events;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
class ReplicatedQueueList {

   private static final StringKey HOST_NAME= new StringKey(FileUtil.getHostname());
   private static final String REP_QUEUE_MAP = "ReplicatedEventQueueMap";
   private static Cache getCache() { return CacheManager.getDistributedMap(REP_QUEUE_MAP); }

   synchronized void setQueueListForNode(List<ServerEventQueue> list)  {
      Cache cache= getCache();
      cache.put(HOST_NAME, list);
   }

   synchronized List<ServerEventQueue> getCombinedNodeList()  {
       List<ServerEventQueue> retList= new ArrayList<>();
       Cache cache= getCache();
       for(String k : cache.getKeys()) {
           retList.addAll((List)cache.get(new StringKey(k)));
       }
       return retList;
   }

}
