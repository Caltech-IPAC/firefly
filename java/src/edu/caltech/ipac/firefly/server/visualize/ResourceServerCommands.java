package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 3/5/12
 * Time: 12:26 PM
 */


import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.Version;
import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.server.rpc.ResourceServicesImpl;

import java.util.Map;

/**
 * @author Trey Roby
 */
public class ResourceServerCommands {


    public static class UserKey extends ServerCommandAccess.ServCommand   {

        public String doCommand(Map<String,String[]> paramMap)  throws IllegalArgumentException {
            return new ResourceServicesImpl().getUserKey();
        }

        public boolean getCanCreateJson() { return false; }
    }

    public static class GetVersion extends ServerCommandAccess.ServCommand   {

        public String doCommand(Map<String,String[]> paramMap)  throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            Version v= new ResourceServicesImpl().getVersion(sp.getRequired(ServerParams.USER_AGENT));
            return v.serialize();
        }

        public boolean getCanCreateJson() { return false; }
    }

}

