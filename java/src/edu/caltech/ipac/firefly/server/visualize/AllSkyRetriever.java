package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.net.AnyFitsParams;
import edu.caltech.ipac.visualize.plot.GeomException;

import java.io.File;
import java.net.URL;
/**
 * User: roby
 * Date: Feb 26, 2010
 * Time: 10:43:21 AM
 */


/**
 * @author Trey Roby
 */
public class AllSkyRetriever implements FileRetriever {

     private static final ReservedImage RESERVED_IMAGES[] = {
                  new ReservedImage("e90gal07.fits",
                                    "All Sky Image -- DIRBE 60 micron", "sky"),
                  new ReservedImage("e90gal09.fits",
                                    "All Sky Image -- DIRBE 140 micron", "sky"),
                  new ReservedImage("e90gal04.fits",
                                    "All Sky Image -- DIRBE 4.9 micron", "sky"),
                  new ReservedImage("allsky.fits",
                                    "ISSA All Sky Image", "sky")
            };

     public static final int DEFAULT_ALLSKY        = 0;



    public FileData getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
        String urlStr= RESERVED_IMAGES[DEFAULT_ALLSKY].getURLString();

        File fitsFile;
        try {
            URL url= this.getClass().getClassLoader().getResource(urlStr);
            fitsFile= LockingVisNetwork.getImage(new AnyFitsParams(url));
        }  catch (FailedRequestException e) {
            throw e;
        }  catch (Exception e) {
            throw new FailedRequestException("No data",null,e);
        }
        return new FileData(fitsFile,
                           RESERVED_IMAGES[DEFAULT_ALLSKY].getDescription());
    }

//===================================================================
//------------------------- Public Inner classes --------------------
//===================================================================
   public static class ReservedImage {
      private String _urlName;
      private String _desc;
      private String _shortDesc;

      ReservedImage( String urlName, String desc, String shortDesc) {
          _urlName  = urlName;
          _desc     = desc;
          _shortDesc= shortDesc;
      }
      public String getURLString()        { return _urlName; }
      public String getDescription()      { return _desc; }
      public String getShortDescription() { return _shortDesc; }
   }


}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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

