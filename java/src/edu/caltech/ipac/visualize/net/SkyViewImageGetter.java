package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.HostPort;
import edu.caltech.ipac.util.download.NetworkManager;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.visualize.plot.FitsValidator;
import nom.tam.fits.FitsException;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * This class handles getting images for SkyView.
 * @author Trey Roby
 */

public class SkyViewImageGetter {



  private static final String IMAGE_STR= "/cgi-bin/images?";
//  private static final String IMAGE_STR= "/cgi-bin/pskcall?";

  private final static ClassProperties _prop = 
                          new ClassProperties(SkyViewImageGetter.class);

  private static final String   LOAD_DESC  = _prop.getName("loading");
  // constants





  /**
   * get the catalog
   * @param params the SkyView Image Parameters
   * @param outfile the File
   * @exception FailedRequestException
   */  
  public static void lowlevelGetImage(SkyViewImageParams params,
                                      File               outfile)
                                             throws FailedRequestException,
                                                    IOException {

      ClientLog.message("Retrieving SkyView Image:");
      NetworkManager manager= NetworkManager.getInstance();
      HostPort server       = manager.getServer(NetworkManager.SKYVIEW_SERVER);
      Assert.tst(server);

      try  {

//          String pos= URLEncoder.encode(params.getRaJ2000String()+ "," +
//                                        params.getDecJ2000String(),  "UTF-8");
//          String pixels= URLEncoder.encode(params.getPixelWidth()+ "," +
//                                           params.getPixelHeight(),  "UTF-8");
          String pos= params.getRaJ2000String()+ "," +
                      params.getDecJ2000String();
          String pixels= params.getPixelWidth()+ "," +
                         params.getPixelHeight();
          String survey= URLEncoder.encode(params.getSurvey(),"UTF-8");

      
//         String req= "http://" + server.getHost() + IMAGE_STR  +
//              "SCOORD=equatorial&" +
//              "MAPROJ=Gnomonic&"    +
//              "SURVEY=" + survey   + "&"  +
//              "VCOORD=" + pos      + "&"  +
//              "SFACTR=Default&"    +
//              "PIXELX="            + params.getPixelWidth()   + "&"  +
//              "PIXELY="            + params.getPixelHeight()  + "&"  +
//              "SFACTR="            + params.getSizeInDegree() + "&"  +
//              "EQUINX=2000&"       +
//              "RETURN=fits&";

          String req= "http://" + server.getHost() + IMAGE_STR  +
                      "SCOORD=J2000&"      +
                      "Projection=Tan&"    +
                      "SURVEY="            + survey   + "&"  +
                      "Position="          + pos      + "&"  +
                      "Scaling=1.0&"       +
                      "Pixels="            + pixels   + "&"  +
                      "Size="            + params.getSizeInDegree() + "&"  +
                      "Return=fits&";

         URL             url  = new URL(req);
         URLConnection   conn = url.openConnection();
         String contentType = conn.getContentType();
         URLDownload.logHeader(conn);

         if (contentType != null && contentType.startsWith("text/")) {
               String htmlErr= URLDownload.getStringFromOpenURL(conn,null);
               throw new FailedRequestException(
                         htmlErr,
                         "The Dss server is reporting an error- " +
                         "the DSS error message was displayed to the user.", 
                         true, null );
         }

         URLDownload.getDataToFile(conn, outfile, null);

          try {
              FitsValidator.validateNaxis(outfile);
          } catch (FitsException e) {
              throw new FailedRequestException(_prop.getError("notFound"),
                         "Skyview returned its image not found file", e);
          }



          BufferedInputStream inStream= null;

          byte buffer[]= new byte[80];
          try {
              inStream= new BufferedInputStream(new FileInputStream(outfile));

              inStream.read(buffer);
              String s= new String(buffer);

          } catch (IOException e) {

          } finally {
              FileUtil.silentClose(inStream);
          }

      } catch (MalformedURLException me){
          ClientLog.warning(me.toString());
          throw new FailedRequestException(
                          FailedRequestException.SERVICE_FAILED,
                          "Details in exception", me );
      } catch (IOException ioe){
         if (ioe.getCause() instanceof EOFException) {
             throw new FailedRequestException(
                              _prop.getError("noData"),
                              "No data could be downloaded from this URL.");
         }
         else {
            throw ioe;
         }
      }
      ClientLog.message("Done:");
     }




   public static void main(String args[]) {
       try {
          SkyViewImageParams p= new SkyViewImageParams();
          p.setRaJ2000(10.822);
          p.setDecJ2000(41.350);
          p.setSurvey("Digitized Sky Survey");
          p.setPixelWidth(800);
          p.setPixelHeight(800);

          File f= new File("a.fits");
          lowlevelGetImage(p,f);
      } catch (Exception e) {

           System.out.println("main: failed: " + e);
           e.printStackTrace();
      }

   }


}
