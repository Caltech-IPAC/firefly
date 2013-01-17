package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.URLFileInfoProcessor;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.visualize.LockingVisNetwork;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: wmi
 * Date: Feb 15, 2012
 * Time: 1:40:11 PM
 * To change this template use File | Settings | File Templates.
 */

@SearchProcessorImpl(id = "LcogtFileRetrieve")
public class LcogtFileRetrieve extends URLFileInfoProcessor {

    public static final boolean USE_HTTP_AUTHENTICATOR = false;
    public static final String LCOGT_DATA_RETRIEVAL_TYPE = AppProperties.getProperty("lcogt.data_retrieval_type");  // url or filesystem
    public static final String LCOGT_FILESYSTEM_BASEPATH = AppProperties.getProperty("lcogt.filesystem_basepath");
    public static final String LCOGT_BASEURL = AppProperties.getProperty("lcogt.baseURL");

    public static enum DL_TYPE {
        IMAGE, ANCY, SECATL,JPEG
    }

    public static String getFileName(String filename, DL_TYPE type) {

        String fnamebase = filename.substring(1,filename.length() - 5);
        String fname =fnamebase;

        if (type == DL_TYPE.IMAGE) {
            fname += ".fits";
        } else if (type == DL_TYPE.ANCY) {
            fname += ".anc";
        } else if (type == DL_TYPE.SECATL) {
            fname += "_cat.fits";
        }else if (type == DL_TYPE.JPEG) {
            fname += ".jpg";

        }

        return fname;
    }


    public FileInfo getFileData(ServerRequest sr) throws DataAccessException {
        FileInfo retval = null;
        StopWatch.getInstance().start("LCOGT image file retrieve");
        try {
            URL url = getFileURL(sr, DL_TYPE.IMAGE);
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

        StopWatch.getInstance().printLog("LCOGT image file retrieve");
        return retval;
    }

    // example: http://***REMOVED***.ipac.caltech.edu:9006/data/lcogt/test/zlco_img/{fname}?lon={center lon}&lat={center lat}&size={subsize}
    public static String createFileURLString(String baseUrl, String baseFile, DL_TYPE type) {
        String url = baseUrl;
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url + getFileName(baseFile, type);

    }

    // example: http://***REMOVED***.ipac.caltech.edu:9006/data/lcogt/test/zlco_img/{fname}?lon={center lon}&lat={center lat}&size={subsize}
    public static String createCutoutURLString_l1(String baseUrl, String baseFile, String lon, String lat, String size) {
        String url = baseUrl + baseFile;
        url += "?center=" + lon + "," + lat + "&size=" + size;
        url += "&gzip=" + baseFile.endsWith("gz");

        return url;
    }

    public static String getBaseURL(ServerRequest sr) {
        String host = sr.getSafeParam("host");
        String schemaGroup = sr.getSafeParam("schemaGroup");
        String schema = sr.getSafeParam("schema");
        String table = sr.getSafeParam("table");

        return QueryUtil.makeUrlBase(host) + "/data/" + schemaGroup + "/" + schema + "/" + table + "/";
    }

    public URL getURL(ServerRequest sr) throws MalformedURLException {
        return getFileURL(sr, DL_TYPE.IMAGE);
    }

    public static URL getFileURL(ServerRequest sr, DL_TYPE type) throws MalformedURLException {
        // build service
        String baseUrl = getBaseURL(sr);
        String baseFile= sr.getSafeParam("filehand");

        return new URL(createFileURLString(baseUrl, baseFile, type));
    }

    public static URL getCutoutURL(ServerRequest sr) throws MalformedURLException {
        // build service
        String baseUrl = getBaseURL(sr);
        String baseFile= sr.getSafeParam("filehand");

        // look for ra_obj returned by moving object search
        String subLon = sr.getSafeParam("ra_obj");
        if (StringUtils.isEmpty(subLon)) {
            // next look for in_ra returned IBE
            subLon = sr.getSafeParam("in_ra");
            if (StringUtils.isEmpty(subLon)) {
                // all else fails, try using crval1
                subLon = sr.getSafeParam("crval1");
            }
        }
        
        // look for dec_obj returned by moving object search
        String subLat = sr.getSafeParam("dec_obj");
        if (StringUtils.isEmpty(subLat)) {
            // next look for in_dec retuened by IBE
            subLat = sr.getSafeParam("in_dec");
            if (StringUtils.isEmpty(subLat)) {
                // all else fails, try using crval2
                subLat = sr.getSafeParam("crval2");
            }
        }

        String subSize = sr.getSafeParam("subsize");

        return new URL(createCutoutURLString_l1(baseUrl, baseFile, subLon, subLat, subSize));

    }

    public FileInfo getCutoutData(ServerRequest sr) throws DataAccessException {
        FileInfo retval = null;
        StopWatch.getInstance().start("LCOGT cutout retrieve");
        try {
            URL url = getCutoutURL(sr);
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

        StopWatch.getInstance().printLog("LCOGT cutout retrieve");
        return retval;
    }

    public FileInfo getData(ServerRequest sr) throws DataAccessException {
        if (sr.containsParam("subsize")) {
            return getCutoutData(sr);
        } else {
            return getFileData(sr);
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