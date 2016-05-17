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


    public static class PushPan extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String plotID= sp.getRequired(ServerParams.PLOT_ID);
            String xStr= sp.getRequired(ServerParams.SCROLL_X);
            String yStr= sp.getRequired(ServerParams.SCROLL_Y);


            boolean success= PushJob.pushPan(plotID,xStr,yStr);
            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", success);
            return outJson.toJSONString();
        }
    }

    public static class PushZoom extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String plotID= sp.getRequired(ServerParams.PLOT_ID);
            String zoomFactStr= sp.getRequired(ServerParams.ZOOM_FACTOR);


            boolean success= PushJob.pushZoom(plotID,zoomFactStr);
            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", success);
            return outJson.toJSONString();
        }
    }

    public static class PushRangeValues extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String plotID= sp.getRequired(ServerParams.PLOT_ID);
            String rvString= sp.getRequired(ServerParams.RANGE_VALUES);
            RangeValues rv= RangeValues.parse(rvString);


            boolean success= false;
            if (rv!=null) {
                success= PushJob.pushRangeValues(plotID,rv);
            }
            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", success);
            if (!success) map.put("reason", "could not parse range values string");
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
            ServerRequest req = new ServerRequest();
            for (String p : paramMap.keySet()) {
                req.setParam(p, paramMap.get(p)[0]);
            }
            req.setParam(ServerParams.SOURCE, file);
            boolean success= PushJob.pushTable(req);
            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", success);
            map.put("file", "");
            return outJson.toJSONString();
        }

    }

    public static class PushXYPlot extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {

            SrvParam sp= new SrvParam(paramMap);
            String file= sp.getRequired(ServerParams.FILE);
            ServerRequest req = new ServerRequest();
            for (String p : paramMap.keySet()) {
                req.setParam(p, paramMap.get(p)[0]);
            }
            req.setParam(ServerParams.SOURCE, file);
            boolean success= PushJob.pushXYPlot(req);
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
            String plotIDAry= sp.getOptional(ServerParams.PLOT_ID);
            boolean success= PushJob.pushRegionFile(file, id, plotIDAry);

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
            String plotId= sp.getOptional(ServerParams.PLOT_ID);
            boolean success= PushJob.pushRemoveRegionFile(id, plotId);
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
            String plotIDAry= sp.getOptional(ServerParams.PLOT_ID);
            boolean success= PushJob.pushRegionData(title, id, regData, plotIDAry);

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


    public static class PushAddMask extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);

            String maskId= sp.getRequired(ServerParams.ID);
            int bitNumber= sp.getRequiredInt(ServerParams.BIT_NUMBER);
            int imageNumber= sp.getRequiredInt(ServerParams.IMAGE_NUMBER);
            String color= sp.getRequired(ServerParams.COLOR);
            String bitDesc= sp.getOptional(ServerParams.BIT_DESC);
            String fileKey= sp.getOptional(ServerParams.FILE);
            String plotIdStr= sp.getRequired(ServerParams.PLOT_ID);
            boolean success= PushJob.pushAddMask(maskId,bitNumber,imageNumber,color,bitDesc,fileKey,plotIdStr);
            JSONObject map = new JSONObject();
            JSONArray outJson = new JSONArray();
            outJson.add(map);
            map.put("success", success);
            return outJson.toJSONString();
        }
    }

    public static class PushRemoveMask extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String maskId= sp.getRequired(ServerParams.ID);
            boolean success= PushJob.pushRemoveMask(maskId);
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
