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
import edu.caltech.ipac.targetgui.net.Resolver;
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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
