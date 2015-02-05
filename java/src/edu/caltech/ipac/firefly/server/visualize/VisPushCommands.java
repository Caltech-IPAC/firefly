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

    public static class GetPushID extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {


            SrvParam sp= new SrvParam(paramMap);
            String bid= sp.getOptional(ServerParams.BID);
            String id= VisPushJob.makeNewJob(bid);

            String jsonData = JSTART +
                    "\"success\" :  \"" + true + JLINE_END +
                    "\"id\" : \"" + id + JLINE_END +
                    "\"example\" :  \"" + "fftools/app.html?#id=Loader&BID="+ id + JLAST_END +
                    "}]";

            return jsonData;
        }

    }

    public static class PushFITS extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {


            SrvParam sp= new SrvParam(paramMap);
            String file= sp.getRequired(ServerParams.FILE);
            String bid= sp.getRequired(ServerParams.BID);
            WebPlotRequest wpr= WebPlotRequest.makeFilePlotRequest(file);
            boolean success= VisPushJob.pushFits(bid, wpr);

            String jsonData = JSTART +
                    "\"success\" :  \"" + success + JLINE_END +
                    "\"id\" :  \"" + bid + JLINE_END +
                    "\"file\" :  \"" + file + JLAST_END +
                    JEND;

            return jsonData;
        }

    }

    public static class PushRegion extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {


            SrvParam sp= new SrvParam(paramMap);
            String file= sp.getRequired(ServerParams.FILE);
            String id= sp.getRequired(ServerParams.BID);
            boolean success= VisPushJob.pushRegion(id, file);

            String jsonData = JSTART +
                    "\"success\" :  \"" + success + JLINE_END +
                    "\"id\" :  \"" + id + JLINE_END +
                    "\"file\" :  \"" + file + JLAST_END +
                    JEND;

            return jsonData;
        }

    }


    public static class PushTable extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {


            SrvParam sp= new SrvParam(paramMap);
            String file= sp.getRequired(ServerParams.FILE);
            String id= sp.getRequired(ServerParams.BID);
            boolean success= VisPushJob.pushTable(id, file);

            String jsonData = JSTART +
                    "\"success\" :  \"" + success + JLINE_END +
                    "\"id\" :  \"" + id + JLINE_END +
                    "\"file\" :  \"" + JLAST_END +
                    JEND;

            return jsonData;
        }

    }

    public static class QueryAction extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {


            SrvParam sp= new SrvParam(paramMap);
            String id= sp.getRequired(ServerParams.BID);
            VisPushJob.UserResponse response= VisPushJob.queryAction(id);

            String jsonData = JSTART +
                    "\"success\" :  \"" + (response.getData()!=null) + JLINE_END +
                    "\"id\" :  \"" + id + JLINE_END +
                    "\"result\" :  \"" + response.getData() + JLINE_END +
                    "\"desc\" :  \"" + response.getDesc() + JLAST_END +
                    JEND;

            return jsonData;
        }

    }


}
