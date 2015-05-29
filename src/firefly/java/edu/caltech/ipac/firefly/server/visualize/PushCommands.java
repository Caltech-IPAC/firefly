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
import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.server.vispush.PushJob;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
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

    public static class PushFITS extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String file= sp.getOptional(ServerParams.FILE);
            String plotID= sp.getOptional(ServerParams.PLOT_ID);
            WebPlotRequest wpr;
            if (file!=null) {
                wpr= WebPlotRequest.makeFilePlotRequest(file);
            }
            else {
                wpr= new WebPlotRequest();
            }
            for(Map.Entry<String,String[]> entry : paramMap.entrySet()) {
                wpr.setParam(entry.getKey(),entry.getValue()[0]);
            }

            if (plotID!=null) {
                wpr.setPlotId(plotID);
            }
            boolean success= PushJob.pushFits(wpr);
            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", success);
            map.put("file",file);
            return outJson.toJSONString();
        }
    }

    public static class PushExtension extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String id= sp.getRequired(ServerParams.ID);
            String plotId= sp.getRequired(ServerParams.PLOT_ID);
            String extType= sp.getRequired(ServerParams.EXT_TYPE);
            String title= sp.getRequired(ServerParams.TITLE);
            String image= sp.getOptional(ServerParams.IMAGE);
            String toolTip= sp.getOptional(ServerParams.TOOL_TIP);
            boolean success= PushJob.pushExtension(id, plotId, extType, title, image, toolTip);
            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", success);
            map.put("id", id);
            map.put("plotId", plotId);
            return outJson.toJSONString();
        }

    }

    public static class PushTable extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {

            SrvParam sp= new SrvParam(paramMap);
            String file= sp.getRequired(ServerParams.FILE);
            boolean success= PushJob.pushTable(file);
            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", success);
            map.put("file", "");
            return outJson.toJSONString();
        }

    }


    //========================================================================
    //========= Region Stuff
    //========================================================================


    public static class PushRegionFile extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {

            SrvParam sp= new SrvParam(paramMap);
            String file= sp.getRequired(ServerParams.FILE);
            String id= sp.getOptional(ServerParams.ID);
            boolean success= PushJob.pushRegionFile(file, id);

            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", success);
            map.put("file",file);
            return outJson.toJSONString();
        }

    }


    public static class PushRemoveRegionFile extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {

            SrvParam sp= new SrvParam(paramMap);
            String id= sp.getRequired(ServerParams.ID);
            boolean success= PushJob.pushRemoveRegionFile(id);
            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", success);
            return outJson.toJSONString();
        }

    }

    public static class PushRegionData extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {

            SrvParam sp= new SrvParam(paramMap);
            String regData= sp.getRequired(ServerParams.DS9_REGION_DATA);
            String id= sp.getRequired(ServerParams.ID);
            String title= sp.getOptional(ServerParams.TITLE);
            boolean success= PushJob.pushRegionData(title, id, regData);

            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", success);
            return outJson.toJSONString();
        }

    }

    public static class PushRemoveRegionData extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String regData= sp.getRequired(ServerParams.DS9_REGION_DATA);
            String id= sp.getRequired(ServerParams.ID);
            boolean success= PushJob.pushRemoveRegionData(id, regData);
            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", success);
            return outJson.toJSONString();
        }
    }

    public static class PushAliveCheck extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String ip= sp.getRequired(ServerParams.IP_ADDRESS);
            boolean active= PushJob.isBrowserClientActive(ip);
            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("active", active);
            return outJson.toJSONString();
        }
    }

}
