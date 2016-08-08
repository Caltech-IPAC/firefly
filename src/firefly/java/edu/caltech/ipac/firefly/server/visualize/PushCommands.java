/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 3/5/12
 * Time: 12:26 PM
 */


import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.vispush.PushJob;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.RangeValues;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Map;

/**
 * @author Trey Roby
 */
public class PushCommands {

    public static abstract class BaseVisPushCommand extends ServerCommandAccess.ServCommand {
        public boolean getCanCreateJson() {
            return true;
        }
    }

    public static class PushAliveCheck extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String channel= sp.getOptional(ServerParams.CHANNEL_ID);
            boolean active= PushJob.getBrowserClientActiveCount(channel,0)>1;
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
            int activeCount= PushJob.getBrowserClientActiveCount(channel,tryTime);
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
            ServerEventManager.fireJsonAction(action,channel);
            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", true);
            return outJson.toJSONString();
        }

    }
}
