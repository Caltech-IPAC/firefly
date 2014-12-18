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
