package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 3/5/12
 * Time: 12:26 PM
 */


import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.server.rpc.TargetServicesImpl;
import edu.caltech.ipac.astro.net.Resolver;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;

import java.util.Map;

/**
 * @author Trey Roby
 */
public class ResolveServerCommands {



    public static class ResolveName extends ServerCommandAccess.ServCommand {


        public String doCommand(Map<String, String[]> paramMap) throws Exception {

            SrvParam sp= new SrvParam(paramMap);
            Resolver resolver= Resolver.NedThenSimbad;
            String name = sp.getRequired(ServerParams.OBJ_NAME);
            String resStr = sp.getOptional(ServerParams.RESOLVER);
            try {
                resolver= (resStr!=null) ? Resolver.parse(resStr) : Resolver.NedThenSimbad;
                if (resolver==null) resolver= Resolver.NedThenSimbad;
                ResolvedWorldPt wp= new TargetServicesImpl().resolveName(name, resolver);
                return wp.toString();
            } catch (Exception e) {
                throw new Exception("Could not resolve " +  name + " using " + resolver.toString());
            }
        }

        public boolean getCanCreateJson() { return false; }
    }
}

