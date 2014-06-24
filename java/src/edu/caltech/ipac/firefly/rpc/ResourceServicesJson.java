package edu.caltech.ipac.firefly.rpc;
/**
 * User: roby
 * Date: 3/9/12
 * Time: 1:18 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.JsonUtils;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ResourcePath;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.Version;
import edu.caltech.ipac.firefly.data.table.RawDataSet;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class ResourceServicesJson implements ResourceServicesAsync {

    private final boolean doJsonP;

    public ResourceServicesJson(boolean doJsonP) {
        this.doJsonP = doJsonP;
    }

    public void getIpacTable(ResourcePath path, Request req, AsyncCallback<RawDataSet> async) {
        // todo
        throw new IllegalArgumentException("Not yet implemented");
    }

    public void getIpacTable(String filePath, Request req, AsyncCallback<RawDataSet> async) {
        // todo
        throw new IllegalArgumentException("Not yet implemented");
    }

    public void getVersion(String ua, final AsyncCallback<Version> async) {
        List<Param> paramList = new ArrayList<Param>(3);
        paramList.add(new Param(ServerParams.USER_AGENT, ua));


        JsonUtils.doService(doJsonP, ServerParams.VERSION, paramList, async, new JsonUtils.Converter<Version>() {
            public Version convert(String s) {
                return Version.parse(s);
            }
        });

    }

    public void getUserKey(final AsyncCallback<String> async) {
        JsonUtils.doService(doJsonP, ServerParams.USER_KEY, async);
    }
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
