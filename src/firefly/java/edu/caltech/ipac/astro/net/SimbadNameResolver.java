/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.astro.target.PositionJ2000;
import edu.caltech.ipac.astro.target.ProperMotion;
import edu.caltech.ipac.astro.target.SimbadAttribute;
import edu.caltech.ipac.util.download.FailedRequestException;

import java.io.IOException;
import java.net.UnknownHostException;



/**
 * @author Xiuqin Wu, based on Trey Roby's CoordConvert
 */
public class SimbadNameResolver {

    public static SimbadAttribute lowlevelNameResolver(String objname)
                                         throws  FailedRequestException {
        SimbadObject simbadObject;
        PositionJ2000 positionOut = null;

        try {
            Simbad4Client simbad4Client = new Simbad4Client();
            simbadObject = simbad4Client.searchByName(objname);
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
