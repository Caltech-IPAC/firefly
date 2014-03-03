package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.dyn.DynServerUtils;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.hydra.data.PtfMOSRequest;
import edu.caltech.ipac.hydra.data.PtfRequest;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.StringUtils;

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
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: wmi Date: Sep 15, 2011 Time: 1:40:11 PM To change this template use File | Settings |
 * File Templates.
 */


@SearchProcessorImpl(id = "PtfMOSQuery", params = {
        @ParamDoc(name = PtfRequest.HOST, desc = "(optional) the hostname, including port")
})
public class QueryPtfMOS extends DynQueryProcessor {

    private static final Logger.LoggerImpl _log = Logger.getLogger();


    @Override
    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {
        File retFile = null;
        try {
            setXmlParams(request);
            PtfMOSRequest req = QueryUtil.assureType(PtfMOSRequest.class, request);
            retFile = searchPtf(req);

        } catch (Exception e) {
            throw makeException(e, "PTF MOS Query Failed.");
        }

        return retFile;
    }


    private File searchPtf(PtfMOSRequest req) throws IOException, DataAccessException, EndUserException {
        File outFile = null;
        URLConnection conn = null;

        try {
            outFile = makeFileName(req);

            URL url = createURL(req);
            conn = URLDownload.makeConnection(url);
            conn.setRequestProperty("Accept", "*/*");

            URLDownload.getDataToFile(conn, outFile);

        } catch (MalformedURLException e) {
            _log.error(e, "Bad URL");
            throw makeException(e, "PTF MOS Query Failed - bad url.");

        } catch (IOException e) {
            _log.error(e, e.toString());
            if (conn != null && conn instanceof HttpURLConnection) {
                HttpURLConnection httpConn = (HttpURLConnection) conn;
                int respCode = httpConn.getResponseCode();
                if (respCode == 400 || respCode == 404 || respCode == 500) {
                    InputStream is = httpConn.getErrorStream();
                    if (is != null) {
                        String msg = parseMessageFromServer(DynServerUtils.convertStreamToString(is));
                        throw new EndUserException("PTF MOS Search Failed: " + msg, msg);

                    } else {
                        String msg = httpConn.getResponseMessage();
                        throw new EndUserException("PTF MOS Search Failed: " + msg, msg);
                    }
                }

            } else {
                throw makeException(e, "PTF MOS Query Failed - network error.");
            }

        } catch (EndUserException e) {
            _log.error(e, e.toString());
            throw new EndUserException(e.getEndUserMsg(), e.getMoreDetailMsg());

        } catch (Exception e) {
            throw makeException(e, "PTF MOS Query Failed.");
        }

        return outFile;
    }


    private String parseMessageFromServer(String response) {
        // no html, so just return
        return response.replaceAll("<br ?/?>", "");
    }

    private URL createURL(PtfMOSRequest req) throws EndUserException,
            IOException {
        String url = req.getUrl();

        String paramStr = getParams(req);
        if (paramStr.startsWith("&")) {
            paramStr = paramStr.substring(1);
        }
        url += "?" + paramStr;

        _log.info("querying URL:" + url);

        return new URL(url);
    }


    protected String getParams(PtfMOSRequest req) throws EndUserException, IOException {
        StringBuffer sb = new StringBuffer(100);

        String catalog = req.getParam(PtfMOSRequest.CATALOG);
        if (StringUtils.isEmpty(catalog)) {
            catalog = "ptf";
        }

        requiredParam(sb, PtfMOSRequest.CATALOG, catalog);
        requiredParam(sb, PtfMOSRequest.OUTPUT_MODE, "Brief");

        // object name
        String objectName = req.getParam(PtfMOSRequest.OBJ_NAME);
        if (!StringUtils.isEmpty(objectName)) {
            requiredParam(sb, PtfMOSRequest.INPUT_TYPE, "name_input");
            //requiredParam(sb, PtfMOSRequest.OBJ_TYPE, req.getParam(PtfMOSRequest.OBJ_TYPE + "_1"));
            requiredParam(sb, PtfMOSRequest.OBJ_NAME, URLEncoder.encode(objectName.trim(), "ISO-8859-1"));
            requiredParam(sb, PtfMOSRequest.EPHEM_STEP, req.getParam(PtfMOSRequest.EPHEM_STEP));
            requiredParam(sb, PtfMOSRequest.SEARCH_REGION_SIZE, req.getParam(PtfMOSRequest.SEARCH_REGION_SIZE));

        } else {
            // mpc 1-line input
            String mpcData = req.getParam(PtfMOSRequest.MPC_DATA);
            if (!StringUtils.isEmpty(mpcData)) {
                requiredParam(sb, PtfMOSRequest.INPUT_TYPE, "mpc_input");
                requiredParam(sb, PtfMOSRequest.OBJ_TYPE, req.getParam(PtfMOSRequest.OBJ_TYPE + "_2"));
                requiredParam(sb, PtfMOSRequest.MPC_DATA, URLEncoder.encode(mpcData.trim(), "ISO-8859-1"));

            } else {
                // manual input
                requiredParam(sb, PtfMOSRequest.INPUT_TYPE, "manual_input");
                requiredParam(sb, PtfMOSRequest.BODY_DESIGNATION,
                        URLEncoder.encode(req.getParam(PtfMOSRequest.BODY_DESIGNATION).trim(), "ISO-8859-1"));
                requiredParam(sb, PtfMOSRequest.EPOCH, req.getParam(PtfMOSRequest.EPOCH));
                requiredParam(sb, PtfMOSRequest.ECCENTRICITY, req.getParam(PtfMOSRequest.ECCENTRICITY));
                requiredParam(sb, PtfMOSRequest.INCLINATION, req.getParam(PtfMOSRequest.INCLINATION));
                requiredParam(sb, PtfMOSRequest.ARG_PERIHELION, req.getParam(PtfMOSRequest.ARG_PERIHELION));
                requiredParam(sb, PtfMOSRequest.ASCEND_NODE, req.getParam(PtfMOSRequest.ASCEND_NODE));

                String objType = req.getParam(PtfMOSRequest.OBJ_TYPE + "_3");
                requiredParam(sb, PtfMOSRequest.OBJ_TYPE, objType);
                if (objType.equalsIgnoreCase("asteroid")) {
                    requiredParam(sb, PtfMOSRequest.SEMIMAJOR_AXIS, req.getParam(PtfMOSRequest.SEMIMAJOR_AXIS));
                    requiredParam(sb, PtfMOSRequest.MEAN_ANOMALY, req.getParam(PtfMOSRequest.MEAN_ANOMALY));

                } else if (objType.equalsIgnoreCase("comet")) {
                    requiredParam(sb, PtfMOSRequest.PERIH_DIST, req.getParam(PtfMOSRequest.PERIH_DIST));
                    requiredParam(sb, PtfMOSRequest.PERIH_TIME, req.getParam(PtfMOSRequest.PERIH_TIME));
                }
            }
        }

        String obsBegin = req.getParam(PtfMOSRequest.OBS_BEGIN);
        if (!StringUtils.isEmpty(obsBegin)) {
            optionalParam(sb, PtfMOSRequest.OBS_BEGIN, URLEncoder.encode(convertDate(obsBegin.trim()), "ISO-8859-1"));
        }

        String obsEnd = req.getParam(PtfMOSRequest.OBS_END);
        if (!StringUtils.isEmpty(obsEnd)) {
            optionalParam(sb, PtfMOSRequest.OBS_END, URLEncoder.encode(convertDate(obsEnd.trim()), "ISO-8859-1"));
        }

        // no longer part of hydra interface
        //optionalParam(sb, PtfMOSRequest.EPHEM_STEP, req.getParam(PtfMOSRequest.EPHEM_STEP));
        //optionalParam(sb, PtfMOSRequest.SEARCH_REGION_SIZE, req.getParam(PtfMOSRequest.SEARCH_REGION_SIZE));

        return sb.toString();
    }

    private String convertDate(String msecStr) {
        long msec = Long.parseLong(msecStr);
        Date resultdate = new Date(msec);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy M d HH:mm:ss.SSS");

        return sdf.format(resultdate);
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        PtfRequest req = QueryUtil.assureType(PtfRequest.class, request);
        setXmlParams(req);

        // add cutout parameters, if applicable
        if (request.containsParam("subsize")) {
            meta.setAttribute("subsize", request.getParam("subsize"));
            meta.setAttribute("usingSubsize", "subSizeTrue");
        } else {
            meta.setAttribute("usingSubsize", "subSizeFalse");
        }
    }


    private static File makeFileName(PtfMOSRequest req) throws IOException {
        return File.createTempFile("ptf-mos-catalog-original", ".tbl", ServerContext.getPermWorkDir());
    }

    protected static void requiredParam(StringBuffer sb, String name, double value) throws EndUserException {
        if (!Double.isNaN(value)) {
            requiredParam(sb, name, value + "");

        } else {
            throw new EndUserException("PTF MOS search failed, PTF Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + name);
        }
    }

    protected static void requiredParam(StringBuffer sb, String name, String value) throws EndUserException {
        if (!StringUtil.isEmpty(value)) {
            sb.append(param(name, value));

        } else {
            throw new EndUserException("PTF MOS search failed, PTF Catalog is unavailable",
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

