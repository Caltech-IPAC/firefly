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
import edu.caltech.ipac.util.dd.VOResourceEndpoint;

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

    @Override
    public void getVOResources(String type, String keywords, AsyncCallback<List<VOResourceEndpoint>> async) {
        // todo
        throw new IllegalArgumentException("Not yet implemented");
    }

    public void getUserKey(final AsyncCallback<String> async) {
        JsonUtils.doService(doJsonP, ServerParams.USER_KEY, async);
    }
}

