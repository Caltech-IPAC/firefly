package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.HostPort;
import edu.caltech.ipac.client.net.NetworkManager;
import edu.caltech.ipac.client.net.FileRetrieveException;
import edu.caltech.ipac.client.net.ThreadedService;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.util.Assert;
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
 * @version $Id: DssImageGetter.java,v 1.9 2012/08/21 21:30:41 roby Exp $
 */
public class DssImageGetter extends  ThreadedService {

    private DssImageParams _params;
    private File           _outFile;
    private static final ClassProperties _prop= new ClassProperties(
                                                  DssImageGetter.class);
    private static final String   OP_DESC= _prop.getName("desc");
    private static final String   SEARCH_DESC= _prop.getName("searching");
    private static final String   LOAD_DESC= _prop.getName("loading");


    /**
     * @param params the parameter for the query
     * @param outFile file to write to
     * @param w a Window
     */
    private DssImageGetter(DssImageParams params, File outFile, Window w) {
        super(w);
       _params = params;
       _outFile= outFile;
       setOperationDesc(OP_DESC);
       setProcessingDesc(SEARCH_DESC);
    }

    protected void doService() throws Exception { 
        lowlevelGetDssImage(_params, _outFile,this); 
    }

    public static void getDssImage(DssImageParams params,
                                   File           outFile,
                                   Window         w) 
                                         throws FailedRequestException {
       DssImageGetter action= new DssImageGetter(params, outFile,w);
       action.execute(true);
    }



    public static void lowlevelGetDssImage(DssImageParams params,
                                           File           outFile) 
                                           throws FailedRequestException,
                                                  IOException {
       lowlevelGetDssImage(params,outFile, null);
    }

    public static void lowlevelGetDssImage(DssImageParams   params, 
                                           File             outFile,
                                           ThreadedService  ts )
                                           throws FailedRequestException,
                                                  IOException {
      ClientLog.message("Retrieving Dss image");

      String cgiapp= null;
      String req   = null;
      NetworkManager manager= NetworkManager.getInstance();
      HostPort server= manager.getServer(NetworkManager.DSS_SERVER);
      Assert.tst(server);

       cgiapp= "/cgi-bin/dss_search";
       req= makeDssRequest(server, cgiapp, params);


      try  {
         URL             url  = new URL(req);
         URLConnection   conn = url.openConnection();
          if (params.getTimeout()!=0) conn.setReadTimeout(params.getTimeout());
         String contentType = conn.getContentType();

         URLDownload.logHeader(conn);

         if (contentType != null && contentType.startsWith("text/")) {
               String htmlErr= URLDownload.getStringFromOpenURL(conn,ts);
               throw new FileRetrieveException(
                         htmlErr,
                         "The Dss server is reporting an error- " +
                         "the DSS error message was displayed to the user.", "DSS");
         }


         if (ts!=null) ts.setProcessingDesc(LOAD_DESC);
         URLDownload.getDataToFile(conn, outFile, ts);

      } catch (SocketTimeoutException timeOutE){
          if (outFile.exists() && outFile.canWrite()) {
              outFile.delete();
          }
          throw new FileRetrieveException(
                  FailedRequestException.SERVICE_FAILED,
                  "Timeout", timeOutE, "DSS" );
      } catch (MalformedURLException me){
          ClientLog.warning(me.toString());
          throw new FailedRequestException(
                          FailedRequestException.SERVICE_FAILED,
                          "Details in exception", me );
      }

      ClientLog.message("Done");
    }


    private static String makeDssRequest(HostPort       server,
                                         String         cgiapp, 
                                         DssImageParams params) {
       String retval;
       /*
       if (params.getSurvey() == DssImageParams.HST_PHASE_2_SURVEY) {
           retval=  "http://" + 
                 server.getHost() + ":" + server.getPort() + cgiapp    +
                 "?r="         + params.getRaJ2000String()  + 
                 "&d="         + params.getDecJ2000String() + 
                 "&e=J2000&h=" + params.getHeight()         + 
                 "&w="         + params.getWidth()          + 
                 "&v=4&f=fits&s=on" +
                 "&c=gz";
       }
       else {
           retval=  "http://" + 
                  server.getHost() + ":" + server.getPort() + cgiapp    +
                 "?r="         + params.getRaJ2000String()  + 
                 "&d="         + params.getDecJ2000String() + 
                 "&e=J2000&h=" + params.getHeight()         + 
                 "&w="         + params.getWidth()          + 
                 "&f=FITS&v="  + params.getSurvey()         +
                 "&s=ON"       +
                 "&c=gz";
       }
       */
       retval=  "http://" +
               server.getHost() + ":" + server.getPort() + cgiapp    +
               "?r="         + params.getRaJ2000String()  +
               "&d="         + params.getDecJ2000String() +
               "&e=J2000&h=" + params.getHeight()         +
               "&w="         + params.getWidth()          +
               "&f=FITS&v="  + params.getSurvey()         +
               "&s=ON"       +
               "&c=gz";
       return retval;
    }


   public static void main(String args[]) {
        DssImageParams params= new DssImageParams();
        params.setHeight(30.0F);
        params.setWidth(30.0F);
        params.setSurvey("poss2Red");
        params.setRaJ2000(10.672);
        params.setDecJ2000(41.259);
        try {
          lowlevelGetDssImage(params, new File("./a.fits.gz") );
        }
        catch (Exception e) {
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
