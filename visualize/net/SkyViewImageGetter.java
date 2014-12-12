package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.client.ClientLog;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.HostPort;
import edu.caltech.ipac.client.net.NetworkManager;
import edu.caltech.ipac.client.net.ThreadedService;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.action.ClassProperties;
import nom.tam.fits.FitsException;
import edu.caltech.ipac.visualize.plot.FitsValidator;

import java.awt.Window;
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
 * @see edu.caltech.ipac.client.net.ThreadedService
 */

public class SkyViewImageGetter extends ThreadedService {



  private static final String IMAGE_STR= "/cgi-bin/images?";
//  private static final String IMAGE_STR= "/cgi-bin/pskcall?";

  private final static ClassProperties _prop = 
                          new ClassProperties(SkyViewImageGetter.class);

  private static final String   SEARCH_DESC= _prop.getName("searching");
  private static final String   LOAD_DESC  = _prop.getName("loading");
  // constants

  private final static String   OP_DESC = _prop.getName("desc");
  private SkyViewImageParams   _params;
  private File                 _outfile;

  /**
   * constructor
   * @param params the SkyView Image Parameters
   * @param outfile the File
   */
  private SkyViewImageGetter(SkyViewImageParams params, 
                             Window             w,
                             File               outfile) {
    super(w);
    _params  = params;
    _outfile = outfile;
    setOperationDesc(OP_DESC);
    setProcessingDesc(String.format(SEARCH_DESC, params.getSurvey()));
  }

  /**
   * get the catalog
   * @exception Exception
   */
  protected void doService() throws Exception {
     lowlevelGetImage(_params, _outfile, this);
  }

  /**
   * get the catalog
   * @param params the SkyView Image Parameters
   * @param outfile the File
   * @param w the Window
   * @exception FailedRequestException
   */
  public static void getImage(SkyViewImageParams params,
                              File               outfile,
                              Window             w)
                                             throws FailedRequestException {
    SkyViewImageGetter action = new SkyViewImageGetter(params, w, outfile);
    action.execute(true);
  }

  /**
   * get the catalog
   * @param params the SkyView Image Parameters
   * @param outfile the File
   * @exception FailedRequestException
   */  
  public static void lowlevelGetImage(SkyViewImageParams params,
                                      File               outfile,
                                      ThreadedService    ts)
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
               String htmlErr= URLDownload.getStringFromOpenURL(conn,ts);
               throw new FailedRequestException(
                         htmlErr,
                         "The Dss server is reporting an error- " +
                         "the DSS error message was displayed to the user.", 
                         true, null );
         }

         if (ts!=null) ts.setProcessingDesc(String.format(LOAD_DESC, params.getSurvey()));
         URLDownload.getDataToFile(conn, outfile, ts);

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
          lowlevelGetImage(p,f,null);
      } catch (Exception e) {

           System.out.println("main: failed: " + e);
           e.printStackTrace();
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
