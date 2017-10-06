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

   private static final String HOST_NAME= FileUtil.getHostname();
   private static final StringKey REP_QUEUE_MAP = new StringKey("ReplicatedEventQueueMap");
   private static Cache getCache() { return CacheManager.getCache(Cache.TYPE_PERM_SMALL); }

   synchronized void setQueueListForNode(List<ServerEventQueue> list)  {
      Cache cache= getCache();
      List<ServerEventQueue>  replicatedList= new ArrayList<>();

      for(ServerEventQueue q : list) {
          replicatedList.add(new ServerEventQueue(q.getConnID(),q.getChannel(),q.getUserKey(),null));
      }
      Map allListMap= (Map)cache.get(REP_QUEUE_MAP);
       if (allListMap==null) allListMap= new HashMap();
       allListMap.put(HOST_NAME,replicatedList);
       cache.put(REP_QUEUE_MAP, allListMap);
   }

   synchronized List<ServerEventQueue> getCombinedNodeList()  {
       Cache cache= getCache();
       List<ServerEventQueue> retList= new ArrayList<>();
       Map allListMap= (Map)cache.get(REP_QUEUE_MAP);
       if (allListMap==null) return Collections.emptyList();

       for(Object v : allListMap.values()) retList.addAll((List)v);
       
       return retList;
   }

}
