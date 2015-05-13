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


import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.server.vispush.VisPushJob;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

import java.util.Map;

/**
 * @author Trey Roby
 */
public class VisPushCommands {

    private static final String JSTART= "[{\n";
    private static final String JLINE_END= "\",   \n";
    private static final String JLAST_END= "\"   \n";
    private static final String JEND= "}]";

    public static abstract class BaseVisPushCommand extends ServerCommandAccess.ServCommand {
        public boolean getCanCreateJson() {
            return true;
        }
    }

//    public static class GetPushID extends BaseVisPushCommand {
//
//        public String doCommand(Map<String, String[]> paramMap) throws Exception {
//
//            SrvParam sp= new SrvParam(paramMap);
//            String bid= sp.getOptional(ServerParams.BID);
//            String id= VisPushJob.makeNewJob(bid);
//
//            String jsonData = JSTART +
//                    "\"success\" :  \"" + true + JLINE_END +
//                    "\"id\" : \"" + id + JLINE_END +
//                    "\"example\" :  \"" + "fftools/app.html?#id=Loader&BID="+ id + JLAST_END +
//                    "}]";
//
//            return jsonData;
//        }
//    }

    public static class PushFITS extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {


            SrvParam sp= new SrvParam(paramMap);
            String file= sp.getRequired(ServerParams.FILE);
            String plotID= sp.getOptional(ServerParams.PLOT_ID);
            WebPlotRequest wpr= WebPlotRequest.makeFilePlotRequest(file);
            if (plotID!=null) {
                wpr.setPlotId(plotID);
            }
            boolean success= VisPushJob.pushFits(wpr, new ServerEvent.EventTarget(ServerEvent.Scope.CHANNEL));

            String jsonData = JSTART +
                    "\"success\" :  \"" + success + JLINE_END +
//                    "\"id\" :  \"" + bid + JLINE_END +
                    "\"file\" :  \"" + file + JLAST_END +
                    JEND;

            return jsonData;
        }

    }

    public static class PushRegion extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {

            SrvParam sp= new SrvParam(paramMap);
            String file= sp.getRequired(ServerParams.FILE);
            boolean success= VisPushJob.pushRegion(file, new ServerEvent.EventTarget(ServerEvent.Scope.CHANNEL));

            String jsonData = JSTART +
                    "\"success\" :  \"" + success + JLINE_END +
//                    "\"id\" :  \"" + id + JLINE_END +
                    "\"file\" :  \"" + file + JLAST_END +
                    JEND;

            return jsonData;
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
            boolean success= VisPushJob.pushExtension(id,plotId,extType, title, image, toolTip,
                        new ServerEvent.EventTarget(ServerEvent.Scope.CHANNEL));

            String jsonData = JSTART +
                    "\"success\" :  \"" + success + JLINE_END +
                    "\"id\" :  \"" + id + JLAST_END +
                    "\"plotId\" :  \"" + plotId + JLAST_END +
                    JEND;

            return jsonData;
        }

    }

    public static class PushTable extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {

            SrvParam sp= new SrvParam(paramMap);
            String file= sp.getRequired(ServerParams.FILE);
            boolean success= VisPushJob.pushTable(file, new ServerEvent.EventTarget(ServerEvent.Scope.CHANNEL));

            String jsonData = JSTART +
                    "\"success\" :  \"" + success + JLINE_END +
                    "\"file\" :  \"" + JLAST_END +
                    JEND;

            return jsonData;
        }

    }

//    public static class QueryAction extends BaseVisPushCommand {
//
//        public String doCommand(Map<String, String[]> paramMap) throws Exception {
//
//            SrvParam sp= new SrvParam(paramMap);
//            String id= sp.getRequired(ServerParams.BID);
//            VisPushJob.UserResponse response= VisPushJob.queryAction(id);
//
//            String jsonData = JSTART +
//                    "\"success\" :  \"" + (response.getData()!=null) + JLINE_END +
//                    "\"id\" :  \"" + id + JLINE_END +
//                    "\"result\" :  " + response.getData() + ",   \n"+
//                    "\"desc\" :  \"" + response.getDesc() + JLAST_END +
//                    JEND;
//
//            return jsonData;
//        }
//
//    }


}
