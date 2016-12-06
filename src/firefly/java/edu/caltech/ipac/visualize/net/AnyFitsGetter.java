/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.DownloadListener;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.FileData;
import edu.caltech.ipac.util.download.URLDownload;

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

      String fileName;
      URL url  = params.getURL();
      FileData fileData = null;

      try  {


          boolean allowDownload= false;
         URLConnection   conn = url.openConnection();
         if (conn instanceof HttpURLConnection) {
             ((HttpURLConnection)conn).setFollowRedirects(true);
         }
         String contentType = conn.getContentType();
         fileName= URLDownload.getSugestedFileName(conn);
          URLDownload.logHeader(conn);

         if (contentType != null && contentType.startsWith("text/")) {
             if (params.getAllowTables() &&
                 fileName!=null && fileName.length()>3 &&
                 FileUtil.getExtension(fileName).equalsIgnoreCase(FileUtil.TBL)) {
                 allowDownload= true;
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
                              "Your retrieve request could not find any data",
                              "No data could be downloaded from this URL: "+
                              url.toString());
         }
         else {
            throw ioe;
         }
      }
      if (fileData != null)  return fileData;
      else                    return new FileData(outfile, fileName);
    }
	
   public static void main(String args[]) {
   }

}
