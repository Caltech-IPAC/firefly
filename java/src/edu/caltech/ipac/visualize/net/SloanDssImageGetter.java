package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.client.net.CacheHelper;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.FileRetrieveException;
import edu.caltech.ipac.client.net.HostPort;
import edu.caltech.ipac.client.net.NetworkManager;
import edu.caltech.ipac.client.net.ThreadedService;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.VoTableUtil;
import edu.caltech.ipac.util.action.ClassProperties;

import java.awt.Window;
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
public class SloanDssImageGetter extends ThreadedService {

    private SloanDssImageParams _params;
    private File _outFile;
    private static final ClassProperties _prop = new ClassProperties(
            SloanDssImageGetter.class);
    private static final String OP_DESC = _prop.getName("desc");
    private static final String SEARCH_DESC = _prop.getName("searching");
    private static final String LOAD_DESC = _prop.getName("loading");


    /**
     * @param params  the parameter for the query
     * @param outFile file to write to
     * @param w       a Window
     */
    private SloanDssImageGetter(SloanDssImageParams params, File outFile, Window w) {
        super(w);
        _params = params;
        _outFile = outFile;
        setOperationDesc(OP_DESC);
        setProcessingDesc(SEARCH_DESC);
    }

    protected void doService() throws Exception {
        lowlevelGetSloanDssImage(_params, _outFile, this);
    }

    public static void getSloanDssImage(SloanDssImageParams params,
                                        File outFile,
                                        Window w) throws FailedRequestException {

        SloanDssImageGetter action = new SloanDssImageGetter(params, outFile, w);
        action.execute(true);
    }


    public static void lowlevelGetSloanDssImage(SloanDssImageParams params,
                                                File outFile) throws FailedRequestException,
                                                                     IOException {
        lowlevelGetSloanDssImage(params, outFile, null);
    }

    public static void lowlevelGetSloanDssImage(SloanDssImageParams params,
                                                File outFile,
                                                ThreadedService ts) throws FailedRequestException,
                                                                           IOException {
        ClientLog.message("Retrieving Sloan Dss image");

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
                    String htmlErr = URLDownload.getStringFromOpenURL(conn, ts);
                    throw new FailedRequestException(
                            htmlErr,
                            "The SDss server is reporting an error", null);
                }

                if (ts != null) ts.setProcessingDesc(LOAD_DESC);
                //todo

                //----------------

                String newfile= qParam.getUniqueString() + ".xml";
                f= CacheHelper.makeFile(newfile);
                URLDownload.getDataToFile(conn, f, ts);
            }
            DataGroup dgAry[]= VoTableUtil.voToDataGroups(f.getAbsolutePath());
            DataGroup dataGroup= dgAry[0];
            if (dataGroup.size() >0) {
                String urlString= (String)dataGroup.get(0).getDataElement("url");
                URLDownload.getDataToFile(new URL(urlString), outFile, ts, false, true);
            }
            else {
                throw new FileRetrieveException("Area not covered",
                                                "votable returned not results, probably area is not covered: ", "SDSS");
            }


        } catch (SocketTimeoutException timeOutE) {
            if (outFile.exists() && outFile.canWrite()) {
                outFile.delete();
            }
            throw new FailedRequestException(
                    FailedRequestException.SERVICE_FAILED,
                    "Timeout", timeOutE);
        } catch (MalformedURLException me) {
            ClientLog.warning(me.toString());
            throw new FailedRequestException(
                    FailedRequestException.SERVICE_FAILED,
                    "Details in exception", me);
        }

        ClientLog.message("Done");
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
