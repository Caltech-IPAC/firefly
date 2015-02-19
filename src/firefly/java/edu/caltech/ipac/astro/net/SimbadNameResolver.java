/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.astro.simbad.SimbadClient;
import edu.caltech.ipac.astro.simbad.SimbadException;
import edu.caltech.ipac.astro.simbad.SimbadObject;
import edu.caltech.ipac.astro.target.PositionJ2000;
import edu.caltech.ipac.astro.target.ProperMotion;
import edu.caltech.ipac.astro.target.SimbadAttribute;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.HostPort;
import edu.caltech.ipac.util.download.NetworkManager;
import edu.caltech.ipac.util.action.ClassProperties;

import java.io.IOException;
import java.net.UnknownHostException;



/**
 * @author Xiuqin Wu, based on Trey Roby's CoordConvert
 */
public class SimbadNameResolver {


    private final static ClassProperties _prop= new ClassProperties(
                                           SimbadNameResolver.class);
    private static final String SERVER_SCRIPT= _prop.getName("script");
    private static final boolean USE_SIMBAD4_CLIENT = _prop.getSelected("use.simbad4");





    public static SimbadAttribute lowlevelNameResolver(String objname)
                                         throws  FailedRequestException {
        SimbadObject simbadObject;
        PositionJ2000 positionOut = null;

        try {
            if (USE_SIMBAD4_CLIENT) {
                Simbad4Client simbad4Client = new Simbad4Client();
                simbadObject = simbad4Client.searchByName(objname);
            } else {
                HostPort server= NetworkManager.getInstance().getServer(
                        NetworkManager.SIMBAD_NAME_RESOLVER);
                SimbadClient simbadClient = new SimbadClient(server.getHost(), SERVER_SCRIPT);
                simbadObject = simbadClient.searchByName(objname);
            }
        }
        catch (UnknownHostException uhe) {
            throw new FailedRequestException(uhe.getMessage(), null, uhe);
        }
        catch (SimbadException ne) {
            throw new FailedRequestException(
                    "Simbad did not find the object: " + objname,
                    ne.getMessage(), ne);
        }
        catch (IOException ioe) {
            throw new FailedRequestException(
                    "Simbad Service Unavailable",  null, ioe);
        }


        ProperMotion pm= PositionJ2000.DEFAULT_PM;
        if (!Float.isNaN(simbadObject.getRaPM()) &&
                !Float.isNaN(simbadObject.getDecPM()) ) {
            pm=   new ProperMotion(simbadObject.getRaPM()/1000.0F,
                    simbadObject.getDecPM()/1000.0F);
        }

        positionOut = new PositionJ2000( simbadObject.getRa(),
                simbadObject.getDec(), pm);

        SimbadAttribute sa= new SimbadAttribute(positionOut);
        sa.setName(simbadObject.getName());
        sa.setType(simbadObject.getType());
        sa.setBMagnitude(simbadObject.getBMagnitude());
        if ( simbadObject.getMagBand().equalsIgnoreCase("V") ) {
            sa.setVMagnitude(simbadObject.getMagnitude());
        }
        sa.setSpectralType(simbadObject.getSpectralType());
        sa.setParallax(simbadObject.getParallax());
        return sa;
    }
}
