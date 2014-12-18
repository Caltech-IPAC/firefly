package edu.caltech.ipac.firefly.rpc;
/**
 * User: roby
 * Date: 3/12/12
 * Time: 12:03 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.JsonUtils;
import edu.caltech.ipac.firefly.data.EphPair;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.astro.net.Resolver;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class TargetServicesJson implements TargetServicesAsync {

    private final boolean doJsonP;

    public TargetServicesJson(boolean doJsonP) {
        this.doJsonP = doJsonP;
    }

    public void resolveName(String objName, Resolver resolver, AsyncCallback<ResolvedWorldPt> async) {
        List<Param> paramList = new ArrayList<Param>(2);
        paramList.add(new Param(ServerParams.OBJ_NAME, objName));
        paramList.add(new Param(ServerParams.RESOLVER, resolver.toString()));
        JsonUtils.doService(doJsonP, ServerParams.RESOLVE_NAME, paramList, async, new JsonUtils.Converter<ResolvedWorldPt>() {
            public ResolvedWorldPt convert(String s) {
                return ResolvedWorldPt.parse(s);
            }
        });
    }

    public void resolveNameToNaifIDs(String targetName, AsyncCallback<ArrayList<EphPair>> async) { }
}

