package edu.caltech.ipac.firefly.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.EphPair;
import edu.caltech.ipac.astro.net.Resolver;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;

import java.util.ArrayList;

/**
 * @author tatianag
 *         Date: May 19, 2008
 *         Time: 2:39:00 PM
 */
public interface TargetServicesAsync {
    void resolveName(String objName, Resolver resolver, AsyncCallback<ResolvedWorldPt> async);

    void resolveNameToNaifIDs(String targetName, AsyncCallback<ArrayList<EphPair>> async);
}
