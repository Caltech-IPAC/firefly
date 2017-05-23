/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.VoTableUtil;
import edu.caltech.ipac.util.download.CacheHelper;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.HostPort;
import edu.caltech.ipac.util.download.NetworkManager;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Trey Roby
 * @version $Id: SloanDssImageGetter.java,v 1.4 2012/08/21 21:30:41 roby Exp $
 */
public class SloanDssImageGetter {

    public static void lowlevelGetSloanDssImage(SloanDssImageParams params,
                                                File outFile) throws FailedRequestException,
                                                                           IOException {

        NetworkManager manager = NetworkManager.getInstance();
        HostPort server = manager.getServer(NetworkManager.SDSS_SERVER);
        Assert.tst(server);

        String req = makeSDssRequest(server, "/vo/dr7siap/siap.asmx/getSiapInfo", params);

        try {
            SloanDssImageParams qParam= params.makeQueryKey();
            File f= CacheHelper.getFile(qParam);
            if (f == null)  {          // if not in cache
                URL url = new URL(req);
                URLConnection conn = url.openConnection();
                if (params.getTimeout() != 0) conn.setReadTimeout(params.getTimeout());
                int statusCode = conn.getHeaderFieldInt("Status Code",200);

                URLDownload.logHeader(conn);

                if (statusCode!=200) {
                    String htmlErr = URLDownload.getStringFromOpenURL(conn, null);
                    throw new FailedRequestException(
                            htmlErr,
                            "The SDss server is reporting an error", null);
                }

                //todo

                //----------------

                String newfile= qParam.getUniqueString() + ".xml";
                f= CacheHelper.makeFile(newfile);
                URLDownload.getDataToFile(conn, f, null);
            }
            DataGroup dgAry[]= VoTableUtil.voToDataGroups(f.getAbsolutePath());
            DataGroup dataGroup= dgAry[0];
            if (dataGroup.size() >0) {
                String urlString= (String)dataGroup.get(0).getDataElement("url");
                URLDownload.getDataToFile(new URL(urlString), outFile, null, false, true);
            }
            else {
                throw new FailedRequestException("SDSS: Area not covered",
                                                "votable returned not results, probably area is not covered: ");
            }


        } catch (SocketTimeoutException timeOutE) {
            if (outFile.exists() && outFile.canWrite()) outFile.delete();
            throw timeOutE;
        } catch (MalformedURLException me) {
            throw new FailedRequestException( "Invalid URL", "Details in exception", me );
        }

    }


    private static String makeSDssRequest(HostPort server,
                                          String cgiapp,
                                          SloanDssImageParams params) {
        String retval;
        retval = "http://" + server.getHost() + ":" + server.getPort() + cgiapp +
                "?POS=" + params.getRaJ2000String() + "," + params.getDecJ2000String() +
                "&size=" + params.getSizeInDeg() +
                "&bandpass=" + params.getBand().toString().toLowerCase() +
                "&format=image/fits";
        return retval;
    }


    public static void main(String args[]) {
        SloanDssImageParams params = new SloanDssImageParams();
        params.setSizeInDeg(0.1F);
        params.setBand(SloanDssImageParams.SDSSBand.r);
        params.setRaJ2000(10.672);
        params.setDecJ2000(41.259);
        try {
            lowlevelGetSloanDssImage(params, new File("./a.fits.gz"));
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
