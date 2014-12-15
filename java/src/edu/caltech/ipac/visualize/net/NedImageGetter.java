package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.HostPort;
import edu.caltech.ipac.client.net.NetworkManager;
import edu.caltech.ipac.client.net.ThreadedService;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.action.ClassProperties;

import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This class gets Ned fits fi
 * es or list of available images.
**/
public class NedImageGetter extends ThreadedService {

   protected static final String NED_IMAGES_ARG = "objname";
   protected static final int    IMAGE_QUERY = 456;
   protected static final int    IMAGE       = 457;
   private HostPort           _nedServer=
             NetworkManager.getInstance().getServer(NetworkManager.NED_SERVER);
   private NedImageParams   _imageParams;
   private NedImQueryParams _queryParams;
   private File             _outFile;
   private final static ClassProperties _prop=  new ClassProperties(
                                                          NedImageGetter.class);
   private final static String   OP_DESC         = _prop.getName("desc");
   private final static String   HTML_PARSE_ERROR= _prop.getError("noImages");
   private final static String   NO_OBJECT_ERR   = _prop.getError("noObject");
   private static final String   QUERY_DESC = _prop.getName("query");
   private static final String   SEARCH_DESC= _prop.getName("searching");
   private static final String   LOAD_DESC= _prop.getName("loading");
   private final static String   NED_NO_OBJECT_ERR=
       "does not seem to match any catalogue in the EXTRAGALACTIC database";
   private int _typeOfService;
   private AstroImageInformation _imageInfo[];

   private NedImageGetter(NedImageParams params, 
                          File           outFile, 
                          Window         w) {
       super(w);
       _imageParams = params;
       _outFile= outFile;
       _typeOfService= IMAGE;
       setOperationDesc(OP_DESC);
       setProcessingDesc(SEARCH_DESC);
   }

   private NedImageGetter(NedImQueryParams params, Window w) {
       super(w);
       _queryParams = params;
       _typeOfService= IMAGE_QUERY;
       setOperationDesc(OP_DESC);
       setProcessingDesc(QUERY_DESC);
   }

   protected void doService() throws Exception { 
        Assert.tst( _typeOfService==IMAGE || _typeOfService==IMAGE_QUERY);
        if (_typeOfService==IMAGE) {
           Assert.tst(_imageParams);
           lowlevelGetNedImage(_nedServer.getHost(), _imageParams, 
                               _outFile,this); 
        }
        else if (_typeOfService==IMAGE_QUERY) {
           Assert.tst(_queryParams);
           _imageInfo= lowlevelQueryNedImages(_nedServer.getHost(),
                                              _queryParams); 
        }
   }

   public static void getNedImage(NedImageParams params, 
                                  File           outFile,
                                  Window         w)
                                         throws FailedRequestException {
       NedImageGetter action= new NedImageGetter(params, outFile,w);
       action.execute(true);
   }

  /*
   * Get a list of images that NED has for this target.
   * It is worth noting here that NED does the look up on the name and not the
   * RA/DEC of the target.  If the RA/DEC is corrent but the user names his
   * target a different name than NED, then NED  will not find any images.
   * 
   * @return AstroImageInformation[] an array of objects that contain NED
   * data.
   */
   public static AstroImageInformation [] queryNedImages(
                                                 NedImQueryParams params, 
                                                 Window           w) 
                                             throws FailedRequestException {
       NedImageGetter action= new NedImageGetter(params,w);
       action.execute(true);
       return action._imageInfo;
   }




//   public static AstroImageInformation [] lowlevelQueryNedImages(
//                                                String           nedHost,
//                                                NedImQueryParams params)
//                                           throws FailedRequestException,
//                                                  IOException {
//       return lowlevelQueryNedImages(nedHost, params,null);
//    }
    public static AstroImageInformation [] lowlevelQueryNedImages(
                                                String           nedHost,
                                                NedImQueryParams params)
                                           throws FailedRequestException,
                                                  IOException {
        AstroImageInformation info[];
        info=  searchForImagesByName(nedHost, params.getName() );
        //if (info != null) {
            //getGifs(info,imageGetter);
        //}
        return info;
    }

    public static void lowlevelGetNedImage(String         nedHost,
                                           NedImageParams params, 
                                           File           outFile) 
                                           throws FailedRequestException,
                                                  IOException {
       lowlevelGetNedImage(nedHost, params,outFile, null);
    }
    public static void lowlevelGetNedImage(String           nedHost, 
                                           NedImageParams   params, 
                                           File             outFile,
                                           ThreadedService  ts )
                                           throws FailedRequestException,
                                                  IOException {
      ClientLog.message("Retrieving Ned image");
      try  {
         URL url  = new URL( "http://" + nedHost + params.getURL());
         URLConnection conn = url.openConnection();
         String contentType = conn.getContentType();
         URLDownload.logHeader(conn);

         if (contentType.startsWith("text/")) {
               throw new FailedRequestException( "Ned request Failed");
         }

         if (ts!=null) ts.setProcessingDesc(LOAD_DESC);
         URLDownload.getDataToFile(conn, outFile, ts);
      } catch (MalformedURLException me){
          ClientLog.message(me.toString());
          throw new FailedRequestException("Service failed", 
                                           "Detail in Excpetion", me);
      }
      ClientLog.message("Done.");
    }


    public static void lowlevelGetPreviewGif(String         nedHost,
                                             NedImageParams params,
                                             File           outFile)
                                                   throws IOException {
      try {
          URL url= new URL( "http://" + nedHost + params.getURL() );
          URLDownload.getDataToFile(url, outFile, false);

      } catch (Exception e) {
          throw new IOException("download failed");
      }
    }
//=======================================
//=======================================
//=======================================
// The following code was written by Jeremy Jones of the SEA project.
// I did do some tweeking and reformating
//=======================================
//=======================================


/**
 * Searches for all available images for a particular object.
 * Results include ImageInformation instances that describe
 * the images, rather than the images themselves.
 * The search will be performed synchronously with the results
 * returned immediately.
 *
 * @param	nedHost		search for images for this object
 * @return      array of AstroImageInformation objects that 
 *              match the search criteria
**/
   public static AstroImageInformation[] searchForImagesByName(
                                                      String nedHost, 
                                                      String nedName)
                                           throws FailedRequestException,
                                                  IOException {
    //int lineCnt= 0;

       AstroImageInformation[] images;
       Vector<AstroImageInformation>  imageVector =
                                     new Vector<AstroImageInformation>();
 		
       ClientLog.message("Requesting list of images for \"" + 
                           nedName + "\"...");
		
       String urlString = createImageSearchURL(nedHost,nedName);
       ClientLog.message("url= " + urlString);

           // Open an input stream for the URL
       URL imagesUrl = new URL(urlString);
       BufferedReader queryStream = new BufferedReader(new InputStreamReader(
                                        imagesUrl.openStream()));
		    
       ClientLog.message("URL stream opened.");
		
		// Store contents as string buffer
       StringBuffer contents = new StringBuffer();		
       String line;
       int count = 0;
       while ((line = queryStream.readLine()) != null) {
           contents.append(line);
           ++count;
       }
       String contentsStr= contents.toString();
		
		// Close the input stream
       queryStream.close();
       ClientLog.message( "URL stream closed.");
       ClientLog.message( "Read " + count + " lines from stream.");
		
		// Search the buffer for images
       String previewUrl, fileSize, fileUrl, resolution;
       String band, width, height, telescope, refcode;
       String token;
       boolean gotimage;
       StringTokenizer tokens = new StringTokenizer(contentsStr, " \t\n\r<>");
       try {
           skipToToken(tokens, "img");  // new for new ned version
           while (tokens.hasMoreTokens()) {
              token= tokens.nextToken();
              gotimage = true;
              try {
                 skipToToken(tokens, "img"); 
              } catch (NoSuchElementException ex) {
                 gotimage = false;
              }
              if (gotimage) {
                  token = tokens.nextToken();
              } 

              if (gotimage && token.toLowerCase().startsWith("src=")) {
                  previewUrl = token.substring(4);
					
                  skipToToken(tokens, "/a");
					
                  fileSize = skipTableTags(tokens);
                  fileSize = fileSize.substring(0, fileSize.length() - 2);
										
                  skipToToken(tokens, "a");
					
                  token = tokens.nextToken();
					
                  if (token.toLowerCase().startsWith("href=")  &&
                      token.toLowerCase().endsWith(".fits.gz")) { // only keep fits files
                      fileUrl = token.substring(5);
						
		   try {
                      skipToToken(tokens, "Display");
                      skipToToken(tokens, "/a");
                      skipToToken(tokens, "td"); // new for new ned version
                      skipToToken(tokens, "td"); // new for new ned version

                      band      = skipTableTags(tokens);
                      skipToToken(tokens, "td");      // new for new ned version
                      width     = skipTableTags(tokens);
                      token     = skipTableTags(tokens); // 'x'
                      height    = skipTableTags(tokens);
                      skipToToken(tokens, "td");      // new for new ned version
                      resolution= skipTableTagsUntilTD(tokens);
                      if (resolution==null)  {
                          resolution= "";
                      }
                      else if (resolution.toLowerCase().equals("img")) {
                          // Skip the "Change Size" stuff
                          skipToToken(tokens, "/a");
                          resolution = skipTableTags(tokens);
                      }
                      telescope = skipTableTags(tokens);

                      refcode = "";
                      skipToToken(tokens, "a");
                      token = tokens.nextToken();
                      if (token.toLowerCase().startsWith("href=")) {
                          tokens.nextToken();
                          refcode = tokens.nextToken();
                      }

                      double resArcsec;
                      try {
                         resArcsec = Double.valueOf(resolution);
                      }
                      catch (NumberFormatException ex) {
                         resArcsec = Double.NaN;
                      }

                       /*System.out.println("line= " + ++lineCnt+
                                               "   refcode="+ refcode +
                                               "   resolution=" + resolution);
                       */
                      AstroImageInformation info = new AstroImageInformation(
                      	   fileUrl, "FITS",
                      	   0,
                      	   0,
                      	   Integer.valueOf(fileSize),
                      	   previewUrl,
                      	   nedName,
                      	   Double.valueOf(width) / 60.0,
                      	   Double.valueOf(height) / 60.0,
                      	   telescope,
                      	   band,
                      	   resArcsec,
                      	   refcode);

                      imageVector.addElement(info);
                   } catch (NoSuchElementException noE) {/* ignore this */}
                  } // end if
              } // end if
           } // end while
       } catch (Exception ex) {			
           //ex.printStackTrace(System.out);
           ClientLog.warning( 
               "Exception occurred in parsing image list: " + ex.toString());

            if (contentsStr.indexOf(NED_NO_OBJECT_ERR) > -1) {
                throw new FailedRequestException( NO_OBJECT_ERR,
                       "Parse found the \"no object\" message", ex);
            }
            else {
                throw new FailedRequestException( HTML_PARSE_ERROR,
                                    "Exception occured durring parse.", ex);
            }
       }
		
       // Create the array from the vector
       if (imageVector.size() > 0) {
          images = new AstroImageInformation[imageVector.size()];
          int i = 0;
          for (Enumeration e = imageVector.elements(); e.hasMoreElements();) {
             images[i] = (AstroImageInformation) e.nextElement();
             ++i;
          }
       }
       else {
          throw new FailedRequestException( HTML_PARSE_ERROR,
              "The image vector size was zero after the html parse.");
       }
		
       return images;
   }
	
  /**
   * Creates a URL string for the NED CGI script that searches for images for a
   * given object name.
   *
   * @param	targetName	name of object to search for
   * @return				the formatted URL string
  **/
   protected static String createImageSearchURL(String nedHost, 
                                                String targetName) {
      return encodeCgiString(
                            "http://" + nedHost +"/"+ "/cgi-bin/nph-imgdata" +
                            "?" + NED_IMAGES_ARG + "=" + targetName);
   }
	
   protected static String skipToToken(StringTokenizer tokens, String skipTo) 
			                throws NoSuchElementException {
      String token = tokens.nextToken();
      String skipToLower = skipTo.toLowerCase();

      while (!token.toLowerCase().equals(skipToLower)) {
          token = tokens.nextToken();
      }

      return token;
   }


    protected static String skipTableTagsUntilTD(StringTokenizer tokens) {
        String newToken = tokens.nextToken().toLowerCase();
        while (newToken.equals("td") || newToken.startsWith("valign") ) {
            if (newToken.equals("td")) {
                    newToken= null;
                    break;
            }
            newToken = tokens.nextToken().toLowerCase();
        }
        return newToken;
    }



   protected static String skipTableTags(StringTokenizer tokens) {
      String newToken = tokens.nextToken().toLowerCase();
      while (newToken.equals("td") || newToken.startsWith("valign") ) {
         newToken = tokens.nextToken().toLowerCase();
      }
      return newToken;
   }
      
   /**
     * Encodes illegal characters in a string to hex values 
     * for transmission via CGI.
     **/
   public static String encodeCgiString(String input) {
      StringBuffer outBuffer = new StringBuffer(64); 
      char c;
      for (int i= 0; i < input.length(); ++i) {
          c = input.charAt(i); 
          if (Character.isWhitespace(c)) {
              outBuffer.append("%20");
          }
          else if (c == '+') {
              outBuffer.append("%2B");
          }
          else {
              outBuffer.append(c);
          }
      }
      
      return outBuffer.toString();
   }

   public static void main(String args[]) {
      try {
       NetworkManager manager= NetworkManager.getInstance();
       HostPort server= manager.getServer(NetworkManager.NED_SERVER);
        searchForImagesByName(server.getHost(), "m31");
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
