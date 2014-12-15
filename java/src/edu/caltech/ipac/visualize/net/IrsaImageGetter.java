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
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
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
