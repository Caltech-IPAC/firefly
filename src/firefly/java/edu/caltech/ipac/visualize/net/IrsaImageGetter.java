/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.HostPort;
import edu.caltech.ipac.util.download.NetworkManager;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ClientLog;

import java.io.File;
import java.io.IOException;

import static edu.caltech.ipac.visualize.net.IrsaImageParams.IrsaTypes.IRIS;
import static edu.caltech.ipac.visualize.net.IrsaImageParams.IrsaTypes.ISSA;
import static edu.caltech.ipac.visualize.net.IrsaImageParams.IrsaTypes.MSX;
import static edu.caltech.ipac.visualize.net.IrsaImageParams.IrsaTypes.TWOMASS;

/**
 * @author Trey Roby
 * @version $Id: IrsaImageGetter.java,v 1.5 2007/10/16 23:19:01 roby Exp $
 */
public class IrsaImageGetter {


    public static String lowlevelGetIrsaImage(IrsaImageParams params,
                                            File            outFile) 
                                           throws FailedRequestException,
                                                  IOException {
        ClientLog.message("Retrieving image from IRSA");

        HostPort hp= NetworkManager.getInstance().getServer(NetworkManager.IRSA);
        String cgiapp= null;

        if (params.getType() == ISSA)
            cgiapp = "/cgi-bin/Oasis/ISSAImg/nph-issaimg";
        else if (params.getType() == TWOMASS)
            cgiapp = "/cgi-bin/Oasis/2MASSImg/nph-2massimg";
        else if (params.getType() == MSX)
            cgiapp = "/cgi-bin/Oasis/MSXImg/nph-msximg";
        else if (params.getType() == IRIS)
            cgiapp = "/cgi-bin/Oasis/ISSAImg/nph-irisimg";
        else
            Assert.tst(false);

        URLParms parms  = new URLParms();

        parms.add(  "objstr", params.getIrsaObjectString() );
        parms.add(    "size", params.getSize() + ""  );
        parms.add(    "band", params.getBand()  + "" );

        String file = outFile.getPath();

        String sugestedFileName=
                       IrsaUtil.getURL(true, hp, cgiapp, parms, file);

        ClientLog.message("Done.");
        return sugestedFileName;
    }

}
