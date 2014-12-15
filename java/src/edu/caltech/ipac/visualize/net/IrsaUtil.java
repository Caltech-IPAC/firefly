package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.ThreadedService;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.client.net.HostPort;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Xiuqin Wu
 * @version $Id: IrsaUtil.java,v 1.6 2012/08/21 21:30:41 roby Exp $
 */
public class IrsaUtil {


   public static String getURL(boolean          isImage,
                               HostPort         hp,
                               String           app,
		                       URLParms         parms,
                               String           fileName,
                               ThreadedService  ts)
                                   throws IOException,
                                          FailedRequestException
   {
      String                retval= null;
      URL                   url;
      URLConnection         conn;
      File                  file= new File(fileName);
      String                req;

      req = "http://" + hp.getHost() + ":" + hp.getPort() + app;

      if(parms.getLength() > 0)
      {
	 req = req + "?";

	 for(int i=0; i<parms.getLength(); ++i)
	 {
	    if(i != 0)
	       req = req + "&";

	    req = req + parms.getKeyword(i);
	    req = req + "=";
	    req = req + parms.getValue(i);
	 }
      }

      try {
	 url  = new URL(req);
	 conn = url.openConnection();
         retval= URLDownload.getSugestedFileName(conn);
	 String contentType = conn.getContentType();
//         URLDownload.logHeader(conn);
         ClientLog.message(true,"fileName: " + fileName);

         if (isImage) {
	    if (contentType != null && contentType.startsWith("text/")) {
               String htmlErr= URLDownload.getStringFromOpenURL(conn,ts);
               throw new FailedRequestException(
                         htmlErr,
                         "The IRSA server is reporting an error- " +
                         "the IRSA error message was displayed to the user.", 
                         true, null );
	    }
	 } //isImage

         URLDownload.getDataToFile(conn, file, ts);

          if (!isImage) {
              checkCatFileForErrors(file);
          }

      } catch (MalformedURLException me){
          ClientLog.warning(true,me.toString());
          throw new FailedRequestException(
                         FailedRequestException.SERVICE_FAILED,
                         "detail in exception", me );
      }

      return retval;
   }

    static void checkCatFileForErrors(File fileToCheck)
                   throws FailedRequestException {
        String errMessage= null;
        BufferedReader in= null;
        try {
            in= new BufferedReader(new FileReader(fileToCheck));
        } catch (FileNotFoundException e) {
            errMessage= "Catalog file was not created";
            throw new FailedRequestException(errMessage);
        }
        try {
            String line= in.readLine();
            //System.out.println("IrsaUtil. first line in cat: " + line);
            if (line.indexOf("ERROR")!= -1) {
                boolean found= false;
                for(line= in.readLine(); (line != null); line= in.readLine()) {
                    if (line.indexOf("UserError") != -1) {
                        found= true;
                        break;
                    }
                    else if (line.indexOf("SystemError") != -1) {
                        found= true;
                        break;
                    }
                }
                if (found) {
                    int startMsg;
                    StringBuffer tstr= new StringBuffer(200);
                    for(line= in.readLine(); (line != null);
                        line= in.readLine()) {
                        if ((startMsg = line.indexOf("Message:")) != -1) {
                            startMsg += 8;
                            //endMsg = line.indexOf("</va");
                            //tstr.append(line.substring(startMsg, endMsg));
                            tstr.append(line.substring(startMsg));
                            tstr.append("\n");
                        }
                    }
                    errMessage= tstr.toString();
                }
                else {
                    errMessage= "A problem was found but no detail";
                }
                in.close();
                throw new FailedRequestException(errMessage);
            } // end if
            in.close();
        } catch (IOException e) {
            try {
                in.close();
            } catch (IOException closeE) {}
            errMessage= "Error parsing failed";
            throw new FailedRequestException(errMessage);

        }
    }






//    static void checkFileForErrors(File fileToCheck)
//                                    throws FailedRequestException {
//        String errMessage= null;
//        BufferedReader in= null;
//        try {
//            in= new BufferedReader(new FileReader(fileToCheck));
//        } catch (FileNotFoundException e) {
//           errMessage= "Catalog file was not created";
//           throw new FailedRequestException(errMessage);
//        }
//        try {
//           String line= in.readLine();
//           if (line.indexOf("<?xml")!= -1) {
//               boolean found= false;
//               boolean userError = false;
//               for(line= in.readLine(); (line != null); line= in.readLine()) {
//                  if (line.indexOf("UserError") != -1) {
//                      found= true;
//                      userError= true;
//                      break;
//                      }
//                  else if (line.indexOf("SystemError") != -1) {
//                      found= true;
//                      userError= false;
//                      break;
//                      }
//               }
//               if (found) {
//                  int startMsg, endMsg;
//                  StringBuffer tstr= new StringBuffer(200);
//                  for(line= in.readLine(); (line != null);
//                                              line= in.readLine()) {
//                     if ((startMsg = line.indexOf("\"message\">")) != -1) {
//                        startMsg += 10;
//                        endMsg = line.indexOf("</va");
//                        tstr.append(line.substring(startMsg, endMsg));
//                        tstr.append("\n");
//                     }
//                  }
//                  errMessage= tstr.toString();
//               }
//               else {
//                   errMessage= "A problem was found but no detail";
//               }
//               in.close();
//               boolean succ= fileToCheck.delete();
//               throw new FailedRequestException(errMessage);
//           } // end if
//           in.close();
//        } catch (IOException e) {
//           try {
//               in.close();
//           } catch (IOException closeE) {}
//           boolean succ= fileToCheck.delete();
//           errMessage= "Error parsing failed";
//           throw new FailedRequestException(errMessage);
//
//        }
//    }

//    static void checkImgFileForErrors(File fileToCheck)
//                                    throws FailedRequestException {
//        String errMessage= null;
//        BufferedReader in= null;
//        try {
//            in= new BufferedReader(new FileReader(fileToCheck));
//        } catch (FileNotFoundException e) {
//           errMessage= "Catalog file was not created";
//           throw new FailedRequestException(errMessage);
//        }
//        try {
//           String line= in.readLine(); // type/plain line
//           ClientLog.message(true, "line " + line);
//           if (line.indexOf("text")!= -1) {
//               boolean found= true;
//	       StringBuffer tstr= new StringBuffer(200);
//	       for(line= in.readLine(); (line != null); line= in.readLine()) {
//		  tstr.append(line);
//		  tstr.append("\n");
//	       }
//	       errMessage= tstr.toString();
//               in.close();
//               boolean succ= fileToCheck.delete();
//               throw new FailedRequestException(errMessage);
//           } // end if
//           in.close();
//        } catch (IOException e) {
//           try {
//               in.close();
//           } catch (IOException closeE) {}
//           boolean succ= fileToCheck.delete();
//           errMessage= "Error parsing failed";
//           throw new FailedRequestException(errMessage);
//
//        }
//    }


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
