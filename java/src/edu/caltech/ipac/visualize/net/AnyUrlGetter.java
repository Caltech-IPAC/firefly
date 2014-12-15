package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.download.DownloadListener;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.FileData;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.action.ClassProperties;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * This class gets any url object
 **/
public class AnyUrlGetter {

    private final static ClassProperties _prop=
                                   new ClassProperties(AnyUrlGetter.class);
  /**
   */
  public static String lowlevelGetUrlToString(URL  url, DownloadListener dl)
                                             throws FailedRequestException,
                                                    IOException {

      ClientLog.message("Retrieving Url:");

      String retval;

      try {
          byte data[]=URLDownload.getDataFromURL(url, null);
          retval=new String(data);
      } catch (MalformedURLException me) {
          ClientLog.warning(me.toString());
          throw new FailedRequestException(FailedRequestException.SERVICE_FAILED,
                                           "Details in exception", me);
      } catch (IOException ioe) {
          if(ioe.getCause() instanceof EOFException) {
              throw new FailedRequestException(_prop.getError("noData"),
                          "No data could be downloaded from this URL\n"+
                          url.toString());
          }
          else {
              throw ioe;
          }
      }
      return retval;
  }

  public static FileData[] lowlevelGetUrlToFile(AnyUrlParams     params,
                                                File             outfile,
                                                boolean          useSuggestedFilename,
                                                DownloadListener dl) throws FailedRequestException {

      FileData outFiles[];
      try {
          outFiles=URLDownload.getDataToFile(params.getURL(), outfile, params.getCookies(), null,
                                             dl, useSuggestedFilename,true,
                                             params.getMaxSizeToDownload());
      } catch (MalformedURLException me) {
          ClientLog.warning(me.toString());
          throw new FailedRequestException(FailedRequestException.SERVICE_FAILED,
                           "Details in exception", me);
      } catch (FileNotFoundException fne) {
          throw new FailedRequestException("Could not find file",
                                           "URL not found: " + params.getURL() , fne);
      } catch (IOException ioe) {
          if(ioe.getCause() instanceof EOFException) {
              throw new FailedRequestException(_prop.getError("noData"),
                                  "No data could be downloaded from this URL\n"+
                                  params.getURL().toString());
          }
          else {
              throw new FailedRequestException("IO problem",
                                               "Details in exception", ioe);
          }
      }
      return outFiles;
  }

	
   public static void main(String args[]) {
       String urlStr;
       URL url;
       AnyUrlParams p;
       try {
           try {System.in.read(); } catch (IOException e) {/*ignore*/}
             urlStr=  "http://localhost:12201/"+
                    "ArchiveDownload/download?EPPreviewH&id=1106";
//           urlStr=  "http://localhost:12201/"+
//                    "ArchiveDownload/download?EPAnc&id=1106";
//           urlStr= "http://***REMOVED***/"+
//                   "ArchiveDownload/download?EPAnc&id=1106";
//           url= new URL( "http://***REMOVED***/"+
//                         "ArchiveDownload/download?EPAnc&id=1105");
//           p= new AnyUrlParams(url);
//           getUrl(p,new File("out.dat"),null);


//           url= new URL( "http://***REMOVED***/"+
//                         "ArchiveDownload/download?EPPreviewH&id=1106");
//
           url= new URL(urlStr);
           p= new AnyUrlParams(url);
           lowlevelGetUrlToFile(p,new File("single.dat"),true, null);
       } catch (FailedRequestException e) {
           System.out.print(e.toString());
       } catch (MalformedURLException e) {
           System.out.print(e.toString());
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
 * OR CONSEQUENTIAL DAMAG
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
