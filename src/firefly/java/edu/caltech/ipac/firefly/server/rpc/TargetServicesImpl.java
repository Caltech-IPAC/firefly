/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.rpc;

import edu.caltech.ipac.astro.net.HorizonsEphPairs;
import edu.caltech.ipac.astro.net.Resolver;
import edu.caltech.ipac.astro.net.TargetNetwork;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.data.EphPair;
import edu.caltech.ipac.firefly.rpc.TargetServices;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;

import java.util.ArrayList;


/**
 * @author tatianag
 * $Id: TargetServicesImpl.java,v 1.11 2012/08/22 20:31:43 roby Exp $
 */
public class TargetServicesImpl extends BaseRemoteService implements TargetServices {

    public ResolvedWorldPt resolveName(String objName, Resolver resolver) throws RPCException {
        try {
            return TargetNetwork.resolveToWorldPt(objName,resolver);
        } catch (Exception e) {
            throw new RPCException(e, this.getClass().getName(), "resolveName", "Unable to resolve name for "+objName,
                    e.getMessage());
        }
    }

    public ArrayList<EphPair> resolveNameToNaifIDs(String targetName) throws RPCException {
        ArrayList<EphPair> naifIDs = new ArrayList<EphPair>(5);
        try {
            HorizonsEphPairs.HorizonsResults[] results = TargetNetwork.getEphIDInfo(targetName, false, null);
            for (HorizonsEphPairs.HorizonsResults res : results) {
                naifIDs.add(new EphPair(res.getName(),res.getNaifID(),res.getPrimaryDes()));
            }
            return naifIDs;
        } catch (FailedRequestException fre) {
            throw new RPCException(fre, this.getClass().getName(), "resolveNameToNaifIDs",
                    "Unable to get alias NAIF IDs for: \""+targetName + "\"", fre.getMessage());
        }
    }
}