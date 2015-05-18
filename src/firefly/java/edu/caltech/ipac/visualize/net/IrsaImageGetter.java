/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.HostPort;
import edu.caltech.ipac.util.download.NetworkManager;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

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

        String sugestedFileName= getURL(hp, cgiapp, parms, file);

        return sugestedFileName;
    }

    public static String getURL(HostPort         hp,
                                String           app,
                                URLParms         parms,
                                String           fileName) throws IOException,
                                                                  FailedRequestException {
        String                retval= null;
        URL url;
        URLConnection conn;
        File                  file= new File(fileName);
        String                req;

        req = "http://" + hp.getHost() + ":" + hp.getPort() + app;

        if(parms.getLength() > 0)
        {
            req = req + "?";

            for(int i=0; i<parms.getLength(); ++i)
            {
                if(i != 0)
                    req = req + "&";

                req = req + parms.getKeyword(i);
                req = req + "=";
                req = req + parms.getValue(i);
            }
        }

        try {
            url  = new URL(req);
            conn = url.openConnection();
            retval= URLDownload.getSugestedFileName(conn);
            String contentType = conn.getContentType();
            ClientLog.message(true,"fileName: " + fileName);

            if (contentType != null && contentType.startsWith("text/")) {
                String htmlErr= URLDownload.getStringFromOpenURL(conn,null);
                throw new FailedRequestException(
                        htmlErr,
                        "The IRSA server is reporting an error- " +
                                "the IRSA error message was displayed to the user.",
                        true, null );
            }

            URLDownload.getDataToFile(conn, file, null);


        } catch (MalformedURLException me){
            ClientLog.warning(true,me.toString());
            throw new FailedRequestException(
                    FailedRequestException.SERVICE_FAILED,
                    "detail in exception", me );
        }

        return retval;
    }

}
