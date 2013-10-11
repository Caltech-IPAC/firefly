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
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: wmi
 * Date: OCT.4,2013
 *
 * To change this template use File | Settings | File Templates.
 */



@SearchProcessorImpl(id = "PtfRefimsFileRetrieve")
public class PtfRefimsFileRetrieve extends URLFileInfoProcessor {

    public static final boolean USE_HTTP_AUTHENTICATOR = false;
    public static final String PTF_DATA_RETRIEVAL_TYPE = AppProperties.getProperty("ptf.data_retrieval_type");  // url or filesystem
    public static final String PTF_FILESYSTEM_BASEPATH = AppProperties.getProperty("ptf.filesystem_basepath");


    public FileInfo getFile(ServerRequest sr) throws DataAccessException {
    	String basePath = PTF_FILESYSTEM_BASEPATH;
        String fileName = sr.getSafeParam("filename");
        String fieldId = sr.getSafeParam("ptffield");
        String filterId = sr.getSafeParam("fid");
        String ccdId = sr.getSafeParam("ccdid");
        //String pId = sr.getSafeParam("ppid");
        //String version = sr.getSafeParam("version");

        basePath += "/refims/d" + fieldId +"/f" + filterId + "/c" +ccdId +"/";

        if (fileName!=null) {
            File f= new File(basePath,fileName);
            if (f.exists()){
                FileInfo fi = new FileInfo(f.getAbsolutePath(), f.getPath(), f.length());
                return fi;
            }
            throw new DataAccessException(("Can not find the file: " + f.getPath()));
        }
        else {
            Logger.warn("cannot find param: filename or the param returns null");
            throw new DataAccessException("Can not find the file");
        }
    }

    public static String createBaseFileString_l2(String basePath, String fieldId, String filterId, String ccdId) {
        String baseFile = basePath;
        baseFile += "/refims/d" + fieldId +"/f" + filterId + "/c" +ccdId +"/";
        return baseFile;
    }

    // example: http://***REMOVED***.ipac.caltech.edu:6001/data/ptf/dev_refims/ptf_ref_img/ptffiled/f#/c#/filename?lon={center lon}&lat={center lat}&size={subsize}
    public static String createCutoutURLString_l2(String baseUrl, String baseFile, String lon, String lat, String size) {
        String url = baseUrl + baseFile;
        url += "?center=" + lon + "," + lat + "&size=" + size;
        url += "&gzip=" + baseFile.endsWith("gz");

        return url;
    }
    // example: http://***REMOVED***:6001/search/ptf/dev_refims/ptf_ref_img?
    public static String getBaseURL(ServerRequest sr) {
        String host = sr.getSafeParam("host");
        String schemaGroup = sr.getSafeParam("schemaGroup");
        String schema = sr.getSafeParam("schema");
        String table = sr.getSafeParam("table");

        return QueryUtil.makeUrlBase(host) + "/data/" + schemaGroup + "/" + schema + "/" + table + "/";
    }

    
    public URL getURL(ServerRequest sr) throws MalformedURLException{
    	return null;
    }

    public static URL getCutoutURL(ServerRequest sr) throws MalformedURLException {
        // build service
        String baseUrl = getBaseURL(sr);
        String filename= sr.getSafeParam("filename");
        String fieldId = sr.getSafeParam("ptffield");
        String filterId = sr.getSafeParam("fid");
        String ccdId = sr.getSafeParam("ccdid");

        String baseFile = "/d" + fieldId +"/f" + filterId + "/c" +ccdId +"/" + filename;

        // look for ra_obj returned by moving object search
        String subLon = sr.getSafeParam("ra_obj");
        if (StringUtils.isEmpty(subLon)) {
            // next look for in_ra returned IBE
            subLon = sr.getSafeParam("in_ra");
            if (StringUtils.isEmpty(subLon)) {
                // all else fails, try using crval1
                subLon = sr.getSafeParam("ra");
            }
        }

        // look for dec_obj returned by moving object search
        String subLat = sr.getSafeParam("dec_obj");
        if (StringUtils.isEmpty(subLat)) {
            // next look for in_dec retuened by IBE
            subLat = sr.getSafeParam("in_dec");
            if (StringUtils.isEmpty(subLat)) {
                // all else fails, try using crval2
                subLat = sr.getSafeParam("dec");
            }
        }

        String subSize = sr.getSafeParam("subsize");
        
        return new URL(createCutoutURLString_l2(baseUrl, baseFile, subLon, subLat, subSize));

    }
    
    public FileInfo getCutoutData(ServerRequest sr) throws DataAccessException {
        FileInfo retval = null;
        StopWatch.getInstance().start("PTF cutout retrieve");
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

        StopWatch.getInstance().printLog("PTF cutout retrieve");
        return retval;
    }

    public FileInfo getData(ServerRequest sr) throws DataAccessException {
        if (sr.containsParam("subsize")) {
            return getCutoutData(sr);
        } else {
            return getFile(sr);
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
