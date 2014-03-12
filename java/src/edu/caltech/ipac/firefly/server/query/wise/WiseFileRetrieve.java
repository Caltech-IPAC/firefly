package edu.caltech.ipac.firefly.server.query.wise;

import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.WiseRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.URLFileInfoProcessor;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.visualize.LockingVisNetwork;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@SearchProcessorImpl(id = "WiseFileRetrieve")
public class WiseFileRetrieve extends URLFileInfoProcessor {

    public static final boolean USE_HTTP_AUTHENTICATOR = false;
    public static final String WISE_DATA_RETRIEVAL_TYPE = AppProperties.getProperty("wise.data_retrieval_type", "firesystem");  // url or filesystem
    public static final String WISE_FILESYSTEM_BASEPATH = AppProperties.getProperty("wise.filesystem_basepath");
    public static final String DEFAULT_SCHEMA = AppProperties.getProperty("wise.schema.default", WiseRequest.ALLWISE_MULTIBAND);

    public static enum IMG_TYPE {
        INTENSITY, MASK, UNCERTAINTY, COVERAGE, DIFF_SPIKES, HALOS, OPT_GHOSTS, LATENTS
    }

    private static final String WISE_URL_USERNAME = AppProperties.getProperty("wise.url.username");
    private static final String WISE_URL_PASSWORD = AppProperties.getProperty("wise.url.password");

    private static final Map<String, String> PROD_LEVEL_MAP = new HashMap<String, String>();

    static {
        PROD_LEVEL_MAP.put(WiseRequest.PRELIM+"|1b", "links-prelim/l1b/");
        PROD_LEVEL_MAP.put(WiseRequest.PRELIM+"|3a", "links-prelim/l3a/");
        PROD_LEVEL_MAP.put(WiseRequest.PRELIM_POSTCRYO +"|1b", "links-postcryo-prelim/l1b-2band/");
        PROD_LEVEL_MAP.put(WiseRequest.POSTCRYO +"|1b", "links-postcryo/l1b-2band/");
        PROD_LEVEL_MAP.put(WiseRequest.CRYO_3BAND+"|1b", "links-3band/l1b-3band/");
        PROD_LEVEL_MAP.put(WiseRequest.CRYO_3BAND+"|3a", "links-3band/l3a-3band/");
        PROD_LEVEL_MAP.put(WiseRequest.ALLSKY_4BAND+"|1b", "links-allsky/l1b-4band/");
        PROD_LEVEL_MAP.put(WiseRequest.ALLSKY_4BAND+"|3a", "links-allsky/l3a-4band/");
        PROD_LEVEL_MAP.put(WiseRequest.ALLWISE_MULTIBAND+"|3a", "links-allwise/l3a/");
        PROD_LEVEL_MAP.put(WiseRequest.MERGE+"|1b", "links-allsky/l1b-merge/");  // exists under links-allsky
        PROD_LEVEL_MAP.put(WiseRequest.MERGE+"|3a", "links-allwise/l3a-merge/");  // exists under links-allwise


        PROD_LEVEL_MAP.put(WiseRequest.PASS1+"|1b", "links-pass1/l1b/");
        PROD_LEVEL_MAP.put(WiseRequest.PASS1+"|3a", "links-pass1/l3a/");
        PROD_LEVEL_MAP.put(WiseRequest.PASS1+"|3o", "links-pass1/l3o/");
        PROD_LEVEL_MAP.put(WiseRequest.PASS2_2BAND+"|1b", "links-pass2/l1b-2band/");
        PROD_LEVEL_MAP.put(WiseRequest.PASS2_3BAND+"|1b", "links-pass2/l1b-3band/");
        PROD_LEVEL_MAP.put(WiseRequest.PASS2_3BAND+"|3a", "links-pass2/l3a-3band/");
        PROD_LEVEL_MAP.put(WiseRequest.PASS2_4BAND+"|1b", "links-pass2/l1b-4band/");
        PROD_LEVEL_MAP.put(WiseRequest.PASS2_4BAND+"|3a", "links-pass2/l3a-4band/");
        PROD_LEVEL_MAP.put(WiseRequest.NEOWISER +"|1b", "links-nprov/l1b/");

    }


    private static String getFilePath_1b(String scanId, String frameNum, String band, IMG_TYPE type) {

        frameNum = StringUtils.pad(3, frameNum, StringUtils.Align.RIGHT, '0');
        String path = scanId.substring(scanId.length() - 2) + "/" + scanId + "/" + frameNum + "/" + scanId + frameNum;

        if (type == IMG_TYPE.INTENSITY) {
            path += "-w" + band + "-int-1b.fits";
        } else if (type == IMG_TYPE.MASK) {
            path += "-w" + band + "-msk-1b.fits.gz";
        } else if (type == IMG_TYPE.UNCERTAINTY) {
            path += "-w" + band + "-unc-1b.fits.gz";
        } else if (type == IMG_TYPE.DIFF_SPIKES) {
            path += "-art-w" + band + "-D.tbl";
        } else if (type == IMG_TYPE.HALOS) {
            path += "-art-w" + band + "-H.tbl";
        } else if (type == IMG_TYPE.OPT_GHOSTS) {
            path += "-art-w" + band + "-O.tbl";
        } else if (type == IMG_TYPE.LATENTS) {
            path += "-art-w" + band + "-P.tbl";
        }

        return path;
    }

    private static String getFilePath_3(String coaddId, String band, IMG_TYPE type) {

        String filepath = coaddId.substring(0, 2) + "/" + coaddId.substring(0, 4) + "/" + coaddId + "/";

        if (type == IMG_TYPE.INTENSITY) {
            filepath += coaddId + "-w" + band + "-int-3.fits";
        } else if (type == IMG_TYPE.MASK) {
            filepath += coaddId + "-w" + band + "-msk-3.fits.gz";
        } else if (type == IMG_TYPE.COVERAGE) {
            filepath += coaddId + "-w" + band + "-cov-3.fits.gz";
        } else if (type == IMG_TYPE.UNCERTAINTY) {
            filepath += coaddId + "-w" + band + "-unc-3.fits.gz";
        } else if (type == IMG_TYPE.DIFF_SPIKES) {
            filepath += coaddId + "-art-w" + band + "-D.tbl";
        } else if (type == IMG_TYPE.HALOS) {
            filepath += coaddId + "-art-w" + band + "-H.tbl";
        } else if (type == IMG_TYPE.OPT_GHOSTS) {
            filepath += coaddId + "-art-w" + band + "-O.tbl";
        } else if (type == IMG_TYPE.LATENTS) {
            filepath += coaddId + "-art-w" + band + "-P.tbl";
        }

        return filepath;
    }


    // example: http://***REMOVED***/wise/pass1/i1bm_frm/{last 2 digits of scan_id}/{scan_id}/{frame_num}/{band}/{product}
    public static String createURLString_1b(String baseUrl, String scanId, String frameNum, String band, IMG_TYPE type) {

        String url = baseUrl;
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url + getFilePath_1b(scanId, frameNum, band, type);
    }

    // example: http://***REMOVED***/wise/pass1/i3om_cdd/{coadd_id}/{band}/{product}
    public static String createURLString_3(String baseUrl, String coaddId, String band, IMG_TYPE type) {

        String url = baseUrl;
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url + getFilePath_3(coaddId, band, type);
    }

    // example: L1b/{scan_id}/{frame_num}/...
    public static String createZipPath_1b(String productLevel, String scanId, String frameNum) {
        return "L" + productLevel + "/" + scanId + "/" + StringUtils.pad(3, frameNum, StringUtils.Align.RIGHT, '0') + "/";
    }

    // example: (***REMOVED***irsa-wise-links-ops/links-pass1-new)/l1b/{last 2 digits of scan_id}/{scan_id}/{frame_num}/{product}
    public static String createFilepath_1b(String schema, String productLevel, String scanId, String frameNum, String band, IMG_TYPE type) {

        String basepath = getBaseFilePath(schema, productLevel);
        return basepath + getFilePath_1b(scanId, frameNum, band, type);
    }

    // example: L3a/{coadd_id}/...
    public static String createZipPath_3(String productLevel, String coaddId) {
        return "L" + productLevel + "/" + coaddId + "/";
    }

    // example: (***REMOVED***irsa-wise-links-ops/links-pass1-new)/l3o/{1st 2 digits of coadd_id}/{1st 4 digits of coadd_id}/{coadd_id}/{product}
    public static String createFilepath_3(String schema, String productLevel, String coaddId, String band, IMG_TYPE type) {

        String basepath = getBaseFilePath(schema, productLevel);
        return basepath + getFilePath_3(coaddId, band, type);
    }

    // example: http://***REMOVED***/applications/WISE/IM/pass1/l1b/{scan_id}/{frame_num}/{band}/{product}?lon={center lon}&lat={center lat}&size={subsize}
    public static String createCutoutURLString_1b(String baseUrl, String scanId, String frameNum, String band, IMG_TYPE type, String lon, String lat, String size) {
        String url = createURLString_1b(baseUrl, scanId, frameNum, band, type);
        String gz = "&gzip=" + url.endsWith("gz");
        url += "?center=" + lon + "," + lat + "&size=" + size + "deg" + gz;

        return url;
    }

    // example: http://***REMOVED***/applications/WISE/IM/pass1/l3o/{coadd_id}/{band}/{product}?lon={center lon}&lat={center lat}&size={subsize}
    public static String createCutoutURLString_3(String baseUrl, String coaddId, String band, IMG_TYPE type, String lon, String lat, String size) {
        String url = createURLString_3(baseUrl, coaddId, band, type);
        String gz = "&gzip=" + url.endsWith("gz");
        url += "?center=" + lon + "," + lat + "&size=" + size + "deg" + gz;

        return url;
    }

    public static void setAuthenticator() {
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(WISE_URL_USERNAME, WISE_URL_PASSWORD.toCharArray());
            }
        });
    }

    public static void removeAuthenticator() {
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("", "".toCharArray());
            }
        });
    }


    public URL getURL(ServerRequest sr) throws MalformedURLException {
        return getURL(sr, IMG_TYPE.INTENSITY);
    }

    public static String getBaseURL(ServerRequest sr) {

        WiseRequest req = QueryUtil.assureType(WiseRequest.class, sr);

        String host = req.getHost();
        String schemaGroup = req.getSchemaGroup();
        String table = req.getTable();
        String schema = WiseRequest.getTableSchema(req.getSchema());

        return QueryUtil.makeUrlBase(host) + "/data/" + schemaGroup + "/" + schema + "/" + table;
    }

    public static String getBaseFilePath(ServerRequest sr) {
        String schema = sr.getSafeParam(WiseRequest.SCHEMA);
        String productLevel = sr.getSafeParam("ProductLevel");
        return getBaseFilePath(schema, productLevel);
    }

    public static String getBaseFilePath(String schema, String productLevel) {

        String prodLvlKey = schema + "|" + productLevel;
        if (!PROD_LEVEL_MAP.containsKey(prodLvlKey)) {
            throw new IllegalArgumentException("image set and product level combination does not exist.  imageset:" + schema + " prodlvl:" + productLevel);
        }

        String filepath = WISE_FILESYSTEM_BASEPATH.trim();
        filepath += filepath.endsWith("/") ? "" : "/";

        filepath += PROD_LEVEL_MAP.get(prodLvlKey);
        return filepath;
    }

    public static URL getURL(ServerRequest sr, IMG_TYPE type) throws MalformedURLException {
        // build service
        String baseUrl = getBaseURL(sr);
        String productLevel = sr.getSafeParam("ProductLevel");

        if (productLevel.equalsIgnoreCase("1b")) {
            String scanId = sr.getSafeParam("scan_id");
            String frameNum = sr.getSafeParam("frame_num");
            String band = sr.getSafeParam("band");

            return new URL(createURLString_1b(baseUrl, scanId, frameNum, band, type));

        } else if (productLevel.equalsIgnoreCase("3o") || productLevel.equalsIgnoreCase("3a")) {
            String coaddId = sr.getSafeParam("coadd_id");
            String band = sr.getSafeParam("band");

            return new URL(createURLString_3(baseUrl, coaddId, band, type));
        }

        return null;
    }

    public static URL getCutoutURL(ServerRequest sr, IMG_TYPE type) throws MalformedURLException {
        // build service
        String baseUrl = getBaseURL(sr);
        String productLevel = sr.getSafeParam("ProductLevel");

        // look for ra_obj first - moving object search
        String subLon = sr.getSafeParam("ra_obj");
        if (StringUtils.isEmpty(subLon)) {
            // next look for in_ra (IBE returns this)
            subLon = sr.getSafeParam("in_ra");
            if (StringUtils.isEmpty(subLon)) {
                // all else fails, try using crval1
                subLon = sr.getSafeParam("crval1");
            }
        }

        // look for dec_obj first - moving object search
        String subLat = sr.getSafeParam("dec_obj");
        if (StringUtils.isEmpty(subLat)) {
            // next look for in_dec (IBE returns this)
            subLat = sr.getSafeParam("in_dec");
            if (StringUtils.isEmpty(subLat)) {
                // all else fails, try using crval2
                subLat = sr.getSafeParam("crval2");
            }
        }

        String subSize = sr.getSafeParam("subsize");

        if (productLevel.equalsIgnoreCase("1b")) {
            String scanId = sr.getSafeParam("scan_id");
            String frameNum = sr.getSafeParam("frame_num");
            String band = sr.getSafeParam("band");

            return new URL(createCutoutURLString_1b(baseUrl, scanId, frameNum, band, type, subLon, subLat, subSize));

        } else if (productLevel.equalsIgnoreCase("3o") || productLevel.equalsIgnoreCase("3a")) {
            String coaddId = sr.getSafeParam("coadd_id");
            String band = sr.getSafeParam("band");

            return new URL(createCutoutURLString_3(baseUrl, coaddId, band, type, subLon, subLat, subSize));
        }

        return null;
    }

    public static String getFilename(ServerRequest sr, IMG_TYPE type) throws MalformedURLException {
        // build service
        String productLevel = sr.getSafeParam("ProductLevel");

        String schema = sr.getSafeParam(WiseRequest.SCHEMA);
        if (schema.contains(",")) {
            schema = WiseRequest.MERGE;
        }

        if (productLevel.equalsIgnoreCase("1b")) {
            String scanId = sr.getSafeParam("scan_id");
            String frameNum = sr.getSafeParam("frame_num");
            String band = sr.getSafeParam("band");

            return createFilepath_1b(schema, productLevel, scanId, frameNum, band, type);

        } else if (productLevel.equalsIgnoreCase("3o") || productLevel.equalsIgnoreCase("3a")) {
            String coaddId = sr.getSafeParam("coadd_id");
            String band = sr.getSafeParam("band");

            return createFilepath_3(schema, productLevel, coaddId, band, type);
        }

        return null;
    }

    public FileInfo getData(ServerRequest sr) throws DataAccessException {
        if (!sr.containsParam(WiseRequest.SCHEMA)) sr.setSafeParam(WiseRequest.SCHEMA, DEFAULT_SCHEMA);
        if (sr.containsParam("subsize") && !StringUtil.isEmpty(sr.getParam("subsize"))) {
            return getCutoutData(sr);
        } else {
            return getNonCutoutData(sr);
        }
    }

    public FileInfo getCutoutData(ServerRequest sr) throws DataAccessException {
        FileInfo retval = null;
        StopWatch.getInstance().start("Wise cutout retrieve");
        try {
            URL url = getCutoutURL(sr, IMG_TYPE.INTENSITY);
            if (url == null) throw new MalformedURLException("computed url is null");

            // set authenticator for password-protected http requests
            if (USE_HTTP_AUTHENTICATOR) {
                setAuthenticator();
            }

//            FileData fd = VisNetwork.getAnyFits(new AnyFitsParams(url), null);

            _logger.info("retrieving URL:" + url.toString());
//            retval = new FileInfo(fd.getFile().toString(),
//                    fd.getSugestedExternalName(),
//                    fd.getFile().length());

            retval= LockingVisNetwork.getFitsFile(url);

            // remove authenticator from http requests
            if (USE_HTTP_AUTHENTICATOR) {
                removeAuthenticator();
            }

        } catch (FailedRequestException e) {
            _logger.warn(e, "Could not retrieve URL");
        } catch (MalformedURLException e) {
            _logger.warn(e, "Could not compute URL");
        }

        StopWatch.getInstance().printLog("Wise cutout retrieve");
        return retval;
    }

    public FileInfo getNonCutoutData(ServerRequest sr) throws DataAccessException {
        FileInfo retval = null;
        StopWatch.getInstance().start("Wise file retrieve");
        try {
            String retrievalType = WISE_DATA_RETRIEVAL_TYPE;

            if (retrievalType.equalsIgnoreCase("filesystem")) {
                String baseFilename = WISE_FILESYSTEM_BASEPATH;
                if (baseFilename == null || baseFilename.length() == 0) {
                    // if not configured, default to URL retrieval
                    retrievalType = "url";

                } else {
                    String fileName = getFilename(sr, IMG_TYPE.INTENSITY);

                    File f = new File(fileName);
                    if (f.exists()) {
//                        FileData fd = new FileData(f, fileName.substring(fileName.lastIndexOf("/") + 1));

                        _logger.info("retrieving local file:" + fileName);
                        retval = new FileInfo(f.toString(),
                                              fileName.substring(fileName.lastIndexOf("/") + 1),
                                              f.length());

                    } else {
                        fileName += ".gz";
                        f = new File(fileName);
                        if (f.exists()) {
                            //File unzipFile= unzipFits(f);
//                            FileData fd = new FileData(f, fileName.substring(fileName.lastIndexOf("/") + 1));
                            _logger.info("retrieving local file:" + fileName);
                            retval = new FileInfo(f.toString(),
                                                  fileName.substring(fileName.lastIndexOf("/")+1),
                                                  f.length());

                        } else {
                            retrievalType = "url";
                        }
                    }
                }
            }

            if (retrievalType.equalsIgnoreCase("url")) {
                URL url = getURL(sr, IMG_TYPE.INTENSITY);
                if (url == null) throw new MalformedURLException("computed url is null");

                // set authenticator for password-protected http requests
                if (USE_HTTP_AUTHENTICATOR) {
                    setAuthenticator();
                }

                retval= LockingVisNetwork.getFitsFile(url);
                _logger.info("retrieving URL:" + url.toString());

                // remove authenticator from http requests
                if (USE_HTTP_AUTHENTICATOR) {
                    removeAuthenticator();
                }

            }

        } catch (FailedRequestException e) {
            _logger.warn(e, "Could not retrieve URL");
        } catch (MalformedURLException e) {
            _logger.warn(e, "Could not compute URL");
        }

        StopWatch.getInstance().printLog("Wise file retrieve");
        return retval;
    }

//    public File unzipFits(File gzFile) {
//        File retval;
//        Cache fileCache = CacheManager.getCache(Cache.TYPE_PERM_FILE);
//        Object cacheObj;
//        CacheKey key = new StringKey(gzFile.getPath());
//        cacheObj = fileCache.get(key);
//        if (cacheObj != null && cacheObj instanceof File) {
//            retval = (File) cacheObj;
//        } else {
//            String base = FileUtil.getBase(gzFile);
//
//            String path = gzFile.getParent();
//            WISE_FILESYSTEM_BASEPATH.length();
//            path = path.substring(WISE_FILESYSTEM_BASEPATH.length() + 1);
//            path = path.replace("/", "-");
//
//            retval = new File(VisContext.getVisCacheDir(), path + "__" + base);
//            retval = FileUtil.setExtension(FileUtil.FITS, retval, true);
//
//            try {
//                FileUtil.gUnzipFile(gzFile, retval, (int) FileUtil.MEG);
//                fileCache.put(key, retval);
//            } catch (IOException e) {
//                Logger.warn(e, "Could not unzip: ",
//                            "input zip file:" + gzFile.getPath(),
//                            "output: " + retval.getPath());
//                return gzFile;
//            }
//        }
//
//        return retval;
//    }

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
