/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.visualize.LockingVisNetwork;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.net.AnyUrlParams;
import edu.caltech.ipac.visualize.plot.GeomException;

import java.net.URL;
import java.util.Collections;
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
                  new ReservedImage("allsky/e90gal07.fits",
                                    "All Sky Image -- DIRBE 60 micron", "sky"),
                  new ReservedImage("allsky/e90gal09.fits",
                                    "All Sky Image -- DIRBE 140 micron", "sky"),
                  new ReservedImage("allsky/e90gal04.fits",
                                    "All Sky Image -- DIRBE 4.9 micron", "sky"),
                  new ReservedImage("allsky/allsky.fits",
                                    "ISSA All Sky Image", "sky")
            };

     public static final int DEFAULT_ALLSKY        = 0;



    public FileInfo getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
        String urlStr= RESERVED_IMAGES[DEFAULT_ALLSKY].getURLString();

        FileInfo fitsFileInfo;
        try {
            URL url= this.getClass().getClassLoader().getResource(urlStr);
            AnyUrlParams p= new AnyUrlParams(url);
            p.setLocalFileExtensions(Collections.singletonList(FileUtil.FITS));
            fitsFileInfo= LockingVisNetwork.retrieve(p);
        }  catch (FailedRequestException e) {
            throw e;
        }  catch (Exception e) {
            throw new FailedRequestException("No data",null,e);
        }
        return fitsFileInfo.copyWithDesc(RESERVED_IMAGES[DEFAULT_ALLSKY].getDescription());
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
