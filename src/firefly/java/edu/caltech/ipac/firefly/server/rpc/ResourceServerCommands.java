/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.rpc;

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.Version;
import edu.caltech.ipac.firefly.server.ServCommand;
import edu.caltech.ipac.firefly.server.SrvParam;

import java.util.Map;

/**
 * @author Trey Roby
 */
public class ResourceServerCommands {


    public static class UserKey extends ServCommand {

        public String doCommand(Map<String,String[]> paramMap)  throws IllegalArgumentException {
            return new ResourceServicesImpl().getUserKey();
        }

        public boolean getCanCreateJson() { return false; }
    }

    public static class GetVersion extends ServCommand   {

        public String doCommand(Map<String,String[]> paramMap)  throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            Version v= new ResourceServicesImpl().getVersion(sp.getRequired(ServerParams.USER_AGENT));
            return v.serialize();
        }

        public boolean getCanCreateJson() { return false; }
    }

}

