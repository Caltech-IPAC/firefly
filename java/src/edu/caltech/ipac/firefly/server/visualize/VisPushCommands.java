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

            String jsonData = "[{" +
                    "\"success\" :  \"" + true + "\",  " +
                    "\"id\" : \"" + id + "\",  " +
                    "\"example\" :  \"" + "fftools/app.html?#id=Loader&BID="+ id + "\"" +
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

            String jsonData = "[{" +
                    "\"success\" :  \"" + success + "\",  " +
                    "\"id\" :  \"" + bid + "\",  " +
                    "\"file\" :  \"" + file + "\"" +
                    "}]";

            return jsonData;
        }

    }

    public static class PushRegion extends BaseVisPushCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {


            SrvParam sp= new SrvParam(paramMap);
            String file= sp.getRequired(ServerParams.FILE);
            String id= sp.getRequired(ServerParams.BID);
            boolean success= VisPushJob.pushRegion(id, file);

            String jsonData = "[{" +
                    "\"success\" :  \"" + success + "\",  " +
                    "\"id\" :  \"" + id + "\",  " +
                    "\"file\" :  \"" + file + "\"" +
                    "}]";

            return jsonData;
        }

    }
}
