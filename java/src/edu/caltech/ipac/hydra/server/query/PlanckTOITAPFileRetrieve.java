package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.URLFileInfoProcessor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.visualize.LockingVisNetwork;
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;
import edu.caltech.ipac.util.AppProperties;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: wmi
 * Date: July 16, 2014
 * Time: 1:40:11 PM
 * To change this template use File | Settings | File Templates.
 */



@SearchProcessorImpl(id = "planckTOITAPFileRetrieve")
public class PlanckTOITAPFileRetrieve extends URLFileInfoProcessor {

    public static final boolean USE_HTTP_AUTHENTICATOR = false;
    public static final String Planck_FILESYSTEM_BASEPATH = AppProperties.getProperty("planck.filesystem_basepath");
    
    public FileInfo getData(ServerRequest sr) throws DataAccessException {

        return getTOITAPData(sr);

    }
    // http://***REMOVED***/TAP/sync?LANG=ADQL&REQUEST=doQuery&QUERY=SELECT+*+FROM+planck_toi_100_2b+WHERE+CONTAINS(POINT('J2000',ra,dec),CIRCLE('J2000',121.17440,-21.57294,1.0))=1+and+(round(mjd,0)=55135)&format=fits
    // http://***REMOVED***/TAP/sync?LANG=ADQL&REQUEST=doQuery&QUERY=SELECT+*+FROM+planck_toi_100+WHERE+CONTAINS(POINT('J2000',ra,dec),CIRCLE('J2000',121.17440,-21.57294,1.0))=1+and+(detector='23m'+or+detecot='23s')+(round(mjd,0)=55135)&format=fits

    public static String createTOITAPURLString(String baseUrl, String pos, String type, String size, String optBand, String detc_constr,String ssoflag, String timeStr, String targetStr, String detcStr) {
        String url = baseUrl;
        url += "/TAP/sync?LANG=ADQL&REQUEST=doQuery&QUERY=SELECT+*+FROM+planck_toi_"+ optBand;
        //url += "+WHERE+CONTAINS(POINT('J2000',ra,dec),"+ type+"('J2000',"+ pos+"," + size +"))=1+and+("+detector+ssoflag+"(round(mjd,0)="+rmjd + "))&format=fits";
        url += "+WHERE+CONTAINS(POINT('J2000',ra,dec),"+ type+"('J2000',"+ pos+"," + size +"))=1+and+("+ detc_constr + ssoflag + timeStr + ")&format=fits&user_metadata={OBJECT:'" + targetStr + "'"+",DETNAME:'" + detcStr + "'}";
        return url;
    }

    public static String getBaseURL(ServerRequest sr) {
        String host = sr.getSafeParam("toitapHost");

        return QueryUtil.makeUrlBase(host);
    }

    public static URL getTOITAPURL(ServerRequest sr) throws MalformedURLException {
        // build service
        String baseUrl = getBaseURL(sr);
        String Size = sr.getSafeParam("radius");
        String pos = sr.getParam("pos");
        String type = sr.getParam("type");
        String ssoflag = sr.getParam("ssoflag");
        String optBand = sr.getParam("optBand");
        String targetStr = "";
        String detcStr= "";
        String timeStr = "";
        String detc_constr = "";


        return new URL(createTOITAPURLString(baseUrl, pos, type, Size, optBand,detc_constr,ssoflag,timeStr,targetStr,detcStr));

    }

    public URL getURL(ServerRequest sr) throws MalformedURLException{
        return null;
    }

    
    public FileInfo getTOITAPData(ServerRequest sr) throws DataAccessException {
        FileInfo retval = null;
        StopWatch.getInstance().start("TOI search retrieve");
        try {
            URL url = getTOITAPURL(sr);
            if (url == null) throw new MalformedURLException("computed url is null");

            _logger.info("retrieving URL:" + url.toString());

            retval= LockingVisNetwork.getFitsFile(url);

        } catch (FailedRequestException e) {
            _logger.warn(e, "Could not retrieve URL");
        } catch (MalformedURLException e) {
            _logger.warn(e, "Could not compute URL");
        } catch (IOException e) {
            _logger.warn(e, "Could not retrieve URL");
        }

        StopWatch.getInstance().printLog("Planck TOI retrieve");
        return retval;
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
