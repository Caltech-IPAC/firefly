package edu.caltech.ipac.firefly.server.query.mos;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.MOSRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WiseRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.dyn.DynServerUtils;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.util.*;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;


@SearchProcessorImpl(id = "MOSQuery", params = {
        @ParamDoc(name = WiseRequest.HOST, desc = "(optional) the hostname, including port")
})
public class QueryMOS extends DynQueryProcessor {

    String DEF_URL = AppProperties.getProperty("most.host", "default_most_host_url");

    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final String RESULT_TABLE_NAME = "imgframes_matched_final_table.tbl";
    private static final String ORBITAL_PATH_TABLE_NAME = "orbital_path.tbl";
    private static final String HEADER_ONLY_PARAM = "header_only";




    @Override
    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {
        File retFile;
        try {
            setXmlParams(request);
            MOSRequest req = QueryUtil.assureType(MOSRequest.class, request);
            String tblType = req.getParam(MOSRequest.TABLE_NAME);
            boolean headerOnly = isHeaderOnlyRequest(req);

            String tblName = (tblType != null && tblType.equalsIgnoreCase(MOSRequest.ORBITAL_PATH_TABLE))
                                ? ORBITAL_PATH_TABLE_NAME : RESULT_TABLE_NAME;

            retFile = doSearch(req, tblName, headerOnly);

        } catch (Exception e) {
            throw makeException(e, "WISE MOS Query Failed.");
        }

        return retFile;
    }

    private File doSearch(MOSRequest req, String tblName, boolean headerOnly) throws IOException, DataAccessException, EndUserException {
        File votable = getMOSResult(req);
        DataGroup[] groups = VoTableUtil.voToDataGroups(votable.getAbsolutePath(), headerOnly);
        if (groups != null) {
            for (DataGroup dg : groups) {
                if (dg.getTitle().equalsIgnoreCase(tblName)) {
                    File f = File.createTempFile(tblName + "-", ".tbl", ServerContext.getTempWorkDir());
                    IpacTableWriter.save(f, dg);
                    return f;
                }
            }
        }
        return null;
    }

    private File getMOSResult(MOSRequest req) throws IOException, DataAccessException, EndUserException {

        URL url;
        try {
            url = createURL(req);
        } catch (EndUserException e) {
            _log.error(e, e.toString());
            throw new EndUserException(e.getEndUserMsg(), e.getMoreDetailMsg());
        }
        StringKey cacheKey = new StringKey(url);
        File f = (File) getCache().get(cacheKey);
        if (f != null && f.canRead()) {
            return f;
        } else {
            File outFile = null;
            URLConnection conn = null;

            try {
                outFile = makeFileName(req);
                conn = URLDownload.makeConnection(url);
                conn.setRequestProperty("Accept", "*/*");

                URLDownload.getDataToFile(conn, outFile);
                getCache().put(cacheKey, outFile, 60 * 60 * 24);    // 1 day

            } catch (MalformedURLException e) {
                _log.error(e, "Bad URL");
                throw makeException(e, "WISE MOS Query Failed - bad url.");

            } catch (IOException e) {
                _log.error(e, e.toString());
                if (conn != null && conn instanceof HttpURLConnection) {
                    HttpURLConnection httpConn = (HttpURLConnection) conn;
                    int respCode = httpConn.getResponseCode();
                    if (respCode == 400 || respCode == 404 || respCode == 500) {
                        InputStream is = httpConn.getErrorStream();
                        if (is != null) {
                            String msg = parseMessageFromServer(DynServerUtils.convertStreamToString(is));
                            throw new EndUserException("WISE MOS Search Failed: " + msg, msg);

                        } else {
                            String msg = httpConn.getResponseMessage();
                            throw new EndUserException("WISE MOS Search Failed: " + msg, msg);
                        }
                    }

                } else {
                    throw makeException(e, "WISE MOS Query Failed - network error.");
                }

            } catch (Exception e) {
                throw makeException(e, "WISE MOS Query Failed.");
            }
            return outFile;
        }

    }


    private String parseMessageFromServer(String response) {
        // no html, so just return
        return response.replaceAll("<br ?/?>", "");
    }

    private URL createURL(MOSRequest req) throws EndUserException,
            IOException {
        String url = req.getUrl();
        if (url == null || url.length() < 5) {
            url = DEF_URL;
        }

        String paramStr = getParams(req);
        if (paramStr.startsWith("&")) {
            paramStr = paramStr.substring(1);
        }
        url += "?" + paramStr;

        _log.info("querying URL:" + url);

        return new URL(url);
    }


    protected String getParams(MOSRequest req) throws EndUserException, IOException {
        StringBuffer sb = new StringBuffer(100);
        
        String catalog = req.getParam(MOSRequest.CATALOG);
        if (StringUtils.isEmpty(catalog)) {
            catalog = getMosCatalog(req);
        }

        requiredParam(sb, MOSRequest.CATALOG, catalog);
        requiredParam(sb, MOSRequest.OUTPUT_MODE, "VOTable");

        // object name
        String objectName = req.getParam(MOSRequest.OBJ_NAME);
        String naifID = req.getNaifID();
        if (!StringUtils.isEmpty(naifID)) {
            requiredParam(sb, MOSRequest.INPUT_TYPE, "naifid_input");
            requiredParam(sb, MOSRequest.OBJ_TYPE, "all");
            requiredParam(sb, MOSRequest.OBJ_NAIF_ID, naifID);
        }
        else if (!StringUtils.isEmpty(objectName)) {
            requiredParam(sb, MOSRequest.INPUT_TYPE, "name_input");
            requiredParam(sb, MOSRequest.OBJ_TYPE, req.getParam(MOSRequest.OBJ_TYPE + "_1"));
            requiredParam(sb, MOSRequest.OBJ_NAME, URLEncoder.encode(objectName.trim(), "ISO-8859-1"));

        } else {
            // mpc 1-line input
            String mpcData = req.getParam(MOSRequest.MPC_DATA);
            if (!StringUtils.isEmpty(mpcData)) {
                requiredParam(sb, MOSRequest.INPUT_TYPE, "mpc_input");
                requiredParam(sb, MOSRequest.OBJ_TYPE, req.getParam(MOSRequest.OBJ_TYPE + "_2"));
                requiredParam(sb, MOSRequest.MPC_DATA, URLEncoder.encode(mpcData.trim(), "ISO-8859-1"));

            } else {
                // manual input
                requiredParam(sb, MOSRequest.INPUT_TYPE, "manual_input");
                requiredParam(sb, MOSRequest.BODY_DESIGNATION,
                        URLEncoder.encode(req.getParam(MOSRequest.BODY_DESIGNATION).trim(), "ISO-8859-1"));
                requiredParam(sb, MOSRequest.EPOCH, req.getParam(MOSRequest.EPOCH));
                requiredParam(sb, MOSRequest.ECCENTRICITY, req.getParam(MOSRequest.ECCENTRICITY));
                requiredParam(sb, MOSRequest.INCLINATION, req.getParam(MOSRequest.INCLINATION));
                requiredParam(sb, MOSRequest.ARG_PERIHELION, req.getParam(MOSRequest.ARG_PERIHELION));
                requiredParam(sb, MOSRequest.ASCEND_NODE, req.getParam(MOSRequest.ASCEND_NODE));

                String objType = req.getParam(MOSRequest.OBJ_TYPE + "_3");
                requiredParam(sb, MOSRequest.OBJ_TYPE, objType);
                if (objType.equalsIgnoreCase("asteroid")) {
                    requiredParam(sb, MOSRequest.SEMIMAJOR_AXIS, req.getParam(MOSRequest.SEMIMAJOR_AXIS));
                    requiredParam(sb, MOSRequest.MEAN_ANOMALY, req.getParam(MOSRequest.MEAN_ANOMALY));

                } else if (objType.equalsIgnoreCase("comet")) {
                    requiredParam(sb, MOSRequest.PERIH_DIST, req.getParam(MOSRequest.PERIH_DIST));
                    requiredParam(sb, MOSRequest.PERIH_TIME, req.getParam(MOSRequest.PERIH_TIME));
                }
            }
        }

        String obsBegin = req.getParam(MOSRequest.OBS_BEGIN);
        if (!StringUtils.isEmpty(obsBegin)) {
            optionalParam(sb, MOSRequest.OBS_BEGIN, URLEncoder.encode(convertDate(obsBegin.trim()), "ISO-8859-1"));
        }

        String obsEnd = req.getParam(MOSRequest.OBS_END);
        if (!StringUtils.isEmpty(obsEnd)) {
            optionalParam(sb, MOSRequest.OBS_END, URLEncoder.encode(convertDate(obsEnd.trim()), "ISO-8859-1"));
        }

        // no longer part of hydra interface
        //optionalParam(sb, MOSRequest.EPHEM_STEP, req.getParam(MOSRequest.EPHEM_STEP));
        //optionalParam(sb, MOSRequest.SEARCH_REGION_SIZE, req.getParam(MOSRequest.SEARCH_REGION_SIZE));

        return sb.toString();
    }

    protected String getMosCatalog(MOSRequest req) {
        // TG is it even used? return req.getServiceSchema();
        return req.getParam(MOSRequest.CATALOG);
    }

    private String convertDate(String msecStr) {
        long msec = Long.parseLong(msecStr);
        Date resultdate = new Date(msec);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy M d HH:mm:ss.SSS");

        return sdf.format(resultdate);
    }

    private static File makeFileName(MOSRequest req) throws IOException {
        return File.createTempFile("mos-result", ".xml", ServerContext.getPermWorkDir());
    }

    protected static void requiredParam(StringBuffer sb, String name, double value) throws EndUserException {
        if (!Double.isNaN(value)) {
            requiredParam(sb, name, value + "");

        } else {
            throw new EndUserException("MOS search failed, Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + name);
        }
    }

    protected static void requiredParam(StringBuffer sb, String name, String value) throws EndUserException {
        if (!StringUtil.isEmpty(value)) {
            sb.append(param(name, value));

        } else {
            throw new EndUserException("MOS search failed, Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + name);
        }
    }

    protected static void optionalParam(StringBuffer sb, String name) {
        sb.append(param(name));
    }

    protected static void optionalParam(StringBuffer sb, String name, String value) {
        if (!StringUtil.isEmpty(value)) {
            sb.append(param(name, value));
        }
    }

    protected static void optionalParam(StringBuffer sb, String name, boolean value) {
        sb.append(param(name, value));
    }


    protected static String param(String name) {
        return "&" + name;
    }

    protected static String param(String name, String value) {
        return "&" + name + "=" + value;
    }

    protected static String param(String name, int value) {
        return "&" + name + "=" + value;
    }

    protected static String param(String name, double value) {
        return "&" + name + "=" + value;
    }

    protected static String param(String name, boolean value) {
        return "&" + name + "=" + (value ? "1" : "0");
    }

    protected static boolean isHeaderOnlyRequest(TableServerRequest req) {
        return req.getBooleanParam(HEADER_ONLY_PARAM);
    }

    private static IOException makeException(Exception e, String reason) {
        IOException eio = new IOException(reason);
        eio.initCause(e);
        return eio;
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

