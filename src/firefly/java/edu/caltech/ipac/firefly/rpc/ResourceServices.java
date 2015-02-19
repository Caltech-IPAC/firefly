/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ResourcePath;
import edu.caltech.ipac.firefly.data.Version;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.util.dd.VOResourceEndpoint;

import java.util.List;

/**
 * Date: Nov 9, 2007
 *
 * @author loi
 * @version $Id: ResourceServices.java,v 1.18 2012/03/12 18:04:40 roby Exp $
 */
public interface ResourceServices extends RemoteService {

    public RawDataSet getIpacTable(ResourcePath path, Request req) throws RPCException;    

    public RawDataSet getIpacTable(String filePath, Request req) throws RPCException;

    public Version getVersion(String userAgentStr);

    String getUserKey();

    public List<VOResourceEndpoint> getVOResources(String type, String keywords) throws RPCException;


    /**
     * Utility/Convenience class. Use ResourceServices.App.getInstance() to access static instance of
     * ResourcesServiceAsync
     */
    public static class App extends ServiceLocator<ResourceServicesAsync> {
        private static final App locator = new App();

        private App() {
            super("rpc/FireFly_ResourcesService");
        }

        protected ResourceServicesAsync createService() {
            ResourceServicesAsync retval= null;
            switch (Application.getInstance().getNetworkMode()) {
                case RPC:    retval= (ResourceServicesAsync) GWT.create(ResourceServices.class); break;
                case WORKER: retval= new ResourceServicesJson(false); break;
                case JSONP:  retval= new ResourceServicesJson(true); break;
            }
            return retval;

        }

        public static ResourceServicesAsync getInstance() {
            return locator.getService();
        }
    }
}
