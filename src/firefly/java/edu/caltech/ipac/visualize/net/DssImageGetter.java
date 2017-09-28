/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.HostPort;
import edu.caltech.ipac.util.download.NetworkManager;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.plot.WorldPt;

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
public class DssImageGetter {


    public static void lowlevelGetDssImage(DssImageParams params,
                                           File           outFile) 
                                           throws FailedRequestException,
                                                  IOException {

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
               throw new FailedRequestException(
                         "DSS service failed",
                         "The Dss server is reporting an error- " +
                         "the DSS error message was displayed to the user.");
         }


         URLDownload.getDataToFile(conn, outFile);

      } catch (SocketTimeoutException timeOutE){
          if (outFile.exists() && outFile.canWrite()) {
              outFile.delete();
          }
          throw new FailedRequestException( "DSS service timeout", "Timeout", timeOutE);
      } catch (MalformedURLException me){
          throw new FailedRequestException( "Invalid URL", "Details in exception", me );
      }
    }


    private static String makeDssRequest(HostPort       server,
                                         String         cgiapp, 
                                         DssImageParams params) {
       String retval;
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
        params.setWorldPt(new WorldPt(10.672, 41.259));
        try {
          lowlevelGetDssImage(params, new File("./a.fits.gz") );
        }
        catch (Exception e) {
          System.out.println(e);
        }
   }
}
