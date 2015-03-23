/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
      FileData fileData = null;

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
      if (fileData != null)  return fileData;
      else                    return new FileData(outfile, fileName, fileType );
    }
	
   public static void main(String args[]) {
   }

}
