package edu.caltech.ipac.firefly.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.NetworkMode;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.data.EphPair;
import edu.caltech.ipac.astro.net.Resolver;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;

import java.util.ArrayList;

/**
 * @author tatianag
 * $Id: TargetServices.java,v 1.11 2012/08/22 20:31:43 roby Exp $
 */
public interface TargetServices extends RemoteService {

    /**
     * Utility/Convenience class.
     * Use TargetServices.App.getInstance() to access static instance of TargetServicesAsync
     */
    public static class App extends ServiceLocator<TargetServicesAsync> {
        private static final App locator = new App();

        private App() {
            super("rpc/FireFly_TargetServices");
        }

        protected TargetServicesAsync createService() {
            NetworkMode mode= Application.getInstance().getNetworkMode();
            TargetServicesAsync retval= null;
            switch (mode) {
                case RPC:    retval= (TargetServicesAsync) GWT.create(TargetServices.class); break;
                case WORKER: retval= new TargetServicesJson(false); break;
                case JSONP:  retval= new TargetServicesJson(true); break;
            }
            return retval;
        }

        public static TargetServicesAsync getInstance() {
            return locator.getService();
        }
    }


    public ResolvedWorldPt resolveName(String objName, Resolver resolver) throws RPCException;

    public ArrayList<EphPair> resolveNameToNaifIDs(String targetName) throws RPCException;
}
