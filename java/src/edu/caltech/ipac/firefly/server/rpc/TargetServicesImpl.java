package edu.caltech.ipac.firefly.server.rpc;

import edu.caltech.ipac.astro.simbad.SimbadObject;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.data.EphPair;
import edu.caltech.ipac.firefly.rpc.TargetServices;
import edu.caltech.ipac.target.PositionJ2000;
import edu.caltech.ipac.targetgui.net.HorizonsEphPairs;
import edu.caltech.ipac.targetgui.net.NedNameResolver;
import edu.caltech.ipac.targetgui.net.Resolver;
import edu.caltech.ipac.targetgui.net.Simbad4Client;
import edu.caltech.ipac.targetgui.net.TargetNetwork;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;


/**
 * @author tatianag
 * $Id: TargetServicesImpl.java,v 1.11 2012/08/22 20:31:43 roby Exp $
 */
public class TargetServicesImpl extends BaseRemoteService implements TargetServices {

    public WorldPt resolveNameORIGINAL(String objName, String resolver) throws RPCException {
        try {
            WorldPt retval;
            String key = resolver + "-" + objName;
            retval = (WorldPt) getServletContext().getAttribute(key);
            if (retval == null) {
                if (resolver.equalsIgnoreCase("NED")) {
                    PositionJ2000 pos = NedNameResolver.getPositionVOTable(objName, null);
                    if (pos != null) {
                        retval = new WorldPt(pos.getLon(), pos.getLat(), CoordinateSys.EQ_J2000);
                        throw new FailedRequestException("NED returns no matches");
                    }
                } else if (resolver.equalsIgnoreCase("SIMBAD")) {
                    Simbad4Client simbadClient = new Simbad4Client();
                    SimbadObject results = simbadClient.searchByName(objName);
                    retval = new WorldPt(results.getRa(), results.getDec(), CoordinateSys.EQ_J2000);
                } else {
                    throw new FailedRequestException("Unsupported Name Resolver");
                }
                getServletContext().setAttribute(key, retval);
            }
            return retval;
        } catch (Exception e) {
            throw new RPCException(e, this.getClass().getName(), "resolveName", "Unable to resolve name for "+objName,
                    e.getMessage());
        }
    }

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