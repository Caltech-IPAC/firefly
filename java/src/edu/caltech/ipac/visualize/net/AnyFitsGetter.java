package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.download.DownloadListener;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.FileData;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.action.ClassProperties;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


/**
 * This class gets any url object
**/
public class AnyFitsGetter {

  private final static ClassProperties _prop = 
                          new ClassProperties(AnyFitsGetter.class);




  /**
   * get the catalog
   * @param params the SkyView Image Parameters
   * @param outfile the File
   * @exception FailedRequestException
   */  
  public static FileData lowlevelGetFits(AnyFitsParams    params,
                                         File             outfile,
                                         DownloadListener dl)
                                             throws FailedRequestException,
                                                    IOException {

      ClientLog.message("Retrieving Fits Image:");
      String fileName;
      URL url  = params.getURL();
      FileData.FileType fileType= FileData.FileType.FITS;
      FileData [] fileData = null;

      try  {


          boolean allowDownload= false;
         ClientLog.message("url=" + url);
         URLConnection   conn = url.openConnection();
         // allow redirects: for ex. archive.nrao.edu redirects https to http 
         if (conn instanceof HttpURLConnection) {
             ((HttpURLConnection)conn).setFollowRedirects(true);
         }
         String contentType = conn.getContentType();
         fileName= URLDownload.getSugestedFileName(conn);
//         ClientLog.message("contentType  = " + contentType,
//                           "contentLength= " +  conn.getContentLength() );
          URLDownload.logHeader(conn);

         if (contentType != null && contentType.startsWith("text/")) {
             if (params.getAllowTables() &&
                 fileName!=null && fileName.length()>3 &&
                 FileUtil.getExtension(fileName).equalsIgnoreCase(FileUtil.TBL)) {
                 allowDownload= true;
                 fileType= FileData.FileType.TABLE;
                 outfile= FileUtil.setExtension(FileUtil.TBL,outfile,true);
             }
             else {
               allowDownload= false;
               String htmlErr= URLDownload.getStringFromOpenURL(conn,dl);
               throw new FailedRequestException(
                         htmlErr,
                         "The server is reporting an error- " +
                         "the error message was displayed to the user.", 
                         true, null );
             }
         }
         else {
             allowDownload= true;
         }


          if (allowDownload) {
              fileData = URLDownload.getDataToFile(conn, outfile, dl);
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
                              "No data could be downloaded from this URL: "+
                              url.toString());
         }
         else {
            throw ioe;
         }
      }
      if (fileData != null && fileData.length > 0)
        return fileData[0];
      else
        return new FileData(outfile, fileName, fileType );
    }
	
   public static void main(String args[]) {
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
