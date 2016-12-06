/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.rpc;
/**
 * User: roby
 * Date: 3/5/12
 * Time: 12:26 PM
 */


import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.ServCommand;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Map;

/**
 * @author Trey Roby
 */
public class PushCommands {

    public static abstract class BaseVisPushCommand extends ServCommand {
        public boolean getCanCreateJson() {
            return true;
        }
    }

    public static class PushAliveCheck extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String channel= sp.getOptional(ServerParams.CHANNEL_ID);
            boolean active= getBrowserClientActiveCount(channel,0)>1;
            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", true);
            map.put("active", active);
            return outJson.toJSONString();
        }
    }

    public static class PushAliveCount extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String channel= sp.getOptional(ServerParams.CHANNEL_ID);
            int tryTime= sp.getOptionalInt(ServerParams.TRY_MS,0);
            int activeCount= getBrowserClientActiveCount(channel,tryTime);
            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", true);
            map.put("activeCount", activeCount);
            return outJson.toJSONString();
        }
    }


    public static class PushAction extends BaseVisPushCommand {
        public String doCommand(Map<String, String[]> paramMap) throws Exception {

            SrvParam sp= new SrvParam(paramMap);
            String action= sp.getRequired(ServerParams.ACTION);
            String channel= sp.getRequired(ServerParams.CHANNEL_ID);
            ServerEventManager.fireJsonAction(action, new ServerEvent.EventTarget(ServerEvent.Scope.CHANNEL, null, channel, null));
            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", true);
            return outJson.toJSONString();
        }

    }

    private static int getBrowserClientActiveCount(String channel, int tryTime) {
        if (channel==null) channel= ServerContext.getRequestOwner().getEventChannel();
        int cnt=  ServerEventManager.getActiveQueueChannelCnt(channel);
        long endTry= System.currentTimeMillis()+tryTime;

        try {
            while (tryTime>0 && cnt==0 && System.currentTimeMillis()<endTry) {
                Thread.sleep(200);
                cnt=  ServerEventManager.getActiveQueueChannelCnt(channel);
            }
        } catch (InterruptedException e) {
        }

        return cnt;
    }
}
