/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Trey Roby
 * @version $Id: DssImageGetter.java,v 1.9 2012/08/21 21:30:41 roby Exp $
 */
public class DssImageGetter {

    private static final String server = AppProperties.getProperty("dss.host", "https://archive.stsci.edu");
    private static final String cgiapp= "/cgi-bin/dss_search";

    public static FileInfo get(DssImageParams params, File outFile) throws FailedRequestException, IOException {
        try  {
            FileInfo fi= URLDownload.getDataToFile(new URL(makeDssRequest(params)), outFile);
            if (fi.getContentType()!= null && fi.getContentType().startsWith("text/")) {
                outFile.delete();
                throw new FailedRequestException( "DSS service failed",
                    "The Dss server is reporting an error, the error text is in the file, status- " + fi.getResponseCode() );
            }
            return fi;
        } catch (MalformedURLException me){
            throw new FailedRequestException( "Invalid URL", "Details in exception", me );
        }
    }


    private static String makeDssRequest(DssImageParams params) {
       return server + cgiapp    +
               "?r="         + params.getRaJ2000String()  +
               "&d="         + params.getDecJ2000String() +
               "&e=J2000&h=" + params.getHeight()         +
               "&w="         + params.getWidth()          +
               "&f=FITS&v="  + params.getSurvey()         +
               "&s=ON"       +
               "&c=gz";
    }


   public static void main(String args[]) {
        DssImageParams params= new DssImageParams("test","test");
        params.setHeight(30.0F);
        params.setWidth(30.0F);
        params.setSurvey("poss2Red");
        params.setWorldPt(new WorldPt(10.672, 41.259));
        try {
          get(params, new File("./a.fits.gz") );
        }
        catch (Exception e) {
          System.out.println(e);
        }
   }
}
