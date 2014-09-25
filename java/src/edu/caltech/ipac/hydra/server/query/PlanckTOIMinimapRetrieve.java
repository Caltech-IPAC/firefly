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
import edu.caltech.ipac.hydra.data.PlanckTOITAPRequest;
import edu.caltech.ipac.util.AppProperties;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: wmi
 * Date: Aug 11, 2014
 * Time: 1:40:11 PM
 * To change this template use File | Settings | File Templates.
 */



@SearchProcessorImpl(id = "planckTOIMinimapRetrieve")
public class PlanckTOIMinimapRetrieve extends URLFileInfoProcessor {

    public static final boolean USE_HTTP_AUTHENTICATOR = false;
    public static final String Planck_FILESYSTEM_BASEPATH = AppProperties.getProperty("planck.filesystem_basepath");

    
    public FileInfo getData(ServerRequest sr) throws DataAccessException {

        return getTOIMapData(sr);

    }
    //http://irsa.ipac.caltech.edu/cgi-bin/Planck_TOI/nph-planck_toi_sia?POS=[0.053,-0.062]&CFRAME=’GAL’&
    // ROTANG=90&SIZE=1&CDELT=0.05&FREQ=44000&ITERATIONS=20&DETECTORS=[’24m’,’24s’]&TIME=[[0,55300],[55500,Infinity]]
    public static String createTOIMinimapURLString(String baseUrl, String pos, String iterations, String size, String optBand, String detc_constr,String timeStr,String targetStr,String detcStr) {
        String url = baseUrl;
        url += "?POS=["+pos+"]"+"&CFRAME='GAL'"+"&SIZE="+size+"&FREQ="+optBand+"&ITERATIONS="+iterations+"&DETECTORS="+detc_constr+"&TIME="+timeStr+"&user_metadata={OBJECT:'" + targetStr + "'"+",DETNAME:'" + detcStr + "'}";

        return url;
    }

    public static String getBaseURL(ServerRequest sr) {
        String host = sr.getSafeParam("toiminimapHost");

        return QueryUtil.makeUrlBase(host);
    }

    public static URL getTOIMinimapURL(ServerRequest sr) throws MalformedURLException {
        // build service
        String baseUrl = sr.getSafeParam("baseUrl");
        String Size = sr.getSafeParam("size");
        String pos = sr.getParam("pos");
        String iterations = sr.getParam("iterations");
        String optBand = sr.getParam("optBand");
        String detc_constr = sr.getParam("detc_constr");
        String timeStr =sr.getSafeParam("timeStr");
        String targetStr=sr.getSafeParam("targetStr");
        String detcStr =sr.getSafeParam("detcStr");

        return new URL(createTOIMinimapURLString(baseUrl, pos, iterations, Size, optBand,detc_constr,timeStr,targetStr,detcStr));

    }

    public static String makeDetcConstr(String detector){
        String detectors[] = detector.split(",");
        String detc_constr;

        if (detectors[0].equals("_all_")) {
            detc_constr = "[]";
        } else {
            detc_constr = "['" + detectors[0] + "'";
            for (int j = 1; j < detectors.length; j++) {
                detc_constr += ",'" + detectors[j] + "'";
            }
            detc_constr += "]";
        }
        return detc_constr;
    }

    public URL getURL(ServerRequest sr) throws MalformedURLException{
        return null;
    }

    
    public FileInfo getTOIMapData(ServerRequest sr) throws DataAccessException {
        FileInfo retval = null;
        StopWatch.getInstance().start("TOI MiniMap&Hires retrieve");
        try {
            URL url = getTOIMinimapURL(sr);
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

        StopWatch.getInstance().printLog("Planck TOI MiniMap&Hires retrieve");
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
