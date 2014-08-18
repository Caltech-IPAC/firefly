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
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.hydra.data.PlanckTOITAPRequest;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: wmi
 * Date: 8/11/14
 * Time: 1:33 P
 * To change this template use File | Settings | File Templates.
 */


@SearchProcessorImpl(id = "planckTOIMiniandHMapQuery", params = {
        @ParamDoc(name = PlanckTOITAPRequest.TOIMinimap_HOST, desc = "(optional) the hostname, including port")
})
public class QueryPlanckTOIMiniandHMap extends DynQueryProcessor {

    private static final Logger.LoggerImpl _log = Logger.getLogger();


    @Override
    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {
        File retFile = null;
        try {
            setXmlParams(request);
            PlanckTOITAPRequest req = QueryUtil.assureType(PlanckTOITAPRequest.class, request);
            retFile = searchPlanck(req);

        } catch (Exception e) {
            throw makeException(e, "Planck TOI Minimap Query Failed.");
        }

        return retFile;
    }


    private File searchPlanck(PlanckTOITAPRequest req) throws IOException, DataAccessException, EndUserException {
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
            throw makeException(e, "Planck TOI Minimap Query Failed - bad url.");

        } catch (IOException e) {
            _log.error(e, e.toString());
            if (conn != null && conn instanceof HttpURLConnection) {
                HttpURLConnection httpConn = (HttpURLConnection) conn;
                int respCode = httpConn.getResponseCode();
                if (respCode == 400 || respCode == 404 || respCode == 500) {
                    InputStream is = httpConn.getErrorStream();
                    if (is != null) {
                        String msg = parseMessageFromServer(DynServerUtils.convertStreamToString(is));
                        throw new EndUserException("Planck TOI Search Failed: " + msg, msg);

                    } else {
                        String msg = httpConn.getResponseMessage();
                        throw new EndUserException("Planck TOI Search Failed: " + msg, msg);
                    }
                }

            } else {
                throw makeException(e, "Planck TOI Query Failed - network error.");
            }

        } catch (EndUserException e) {
            _log.error(e, e.toString());
            throw new EndUserException(e.getEndUserMsg(), e.getMoreDetailMsg());

        } catch (Exception e) {
            throw makeException(e, "Planck TOI Query Failed.");
        }

        return outFile;
    }


    private String parseMessageFromServer(String response) {
        // no html, so just return
        return response.replaceAll("<br ?/?>", "");
    }

    private URL createURL(PlanckTOITAPRequest req) throws EndUserException,
            IOException {
        String url = req.getParam(PlanckTOITAPRequest.TOIMinimap_HOST);

        String paramStr = getParams(req);
        if (paramStr.startsWith("&")) {
            paramStr = paramStr.substring(1);
        }
        url += "?" + paramStr;

        _log.info("querying URL:" + url);

        return new URL(url);
    }


    protected String getParams(PlanckTOITAPRequest req) throws EndUserException, IOException {
        StringBuffer sb = new StringBuffer(100);

        //http://irsa.ipac.caltech.edu/cgi-bin/Planck_TOI/nph-planck_toi_sia?POS=[0.053,-0.062]&CFRAME=’GAL’&
        // ROTANG=90&SIZE=1&CDELT=0.05&FREQ=44000&ITERATIONS=20&DETECTORS=[’24m’,’24s’]&TIME=[[0,55300],[55500,Infinity]]

        // object name
        String objectName = req.getParam(PlanckTOITAPRequest.OBJ_NAME);
        if (!StringUtils.isEmpty(objectName)) {
            //requiredParam(sb, PlanckTOITAPRequest.OBJ_TYPE, req.getParam(PlanckTOITAPRequest.OBJ_TYPE + "_1"));
            requiredParam(sb, PlanckTOITAPRequest.OBJ_NAME, URLEncoder.encode(objectName.trim(), "ISO-8859-1"));
            requiredParam(sb, PlanckTOITAPRequest.SEARCH_REGION_SIZE, req.getParam(PlanckTOITAPRequest.SEARCH_REGION_SIZE));

        }

        String userTargetWorldPt = req.getParam("UserTargetWorldPt");
        if (userTargetWorldPt != null) {
            WorldPt pt = WorldPt.parse(userTargetWorldPt);
            if (pt != null) {
                pt = VisUtil.convertToJ2000(pt);
                String pos = "[" +pt.getLon() + "," + pt.getLat() + "]";
                requiredParam(sb, PlanckTOITAPRequest.POS, pos);
            }
        }


        String t_begin = req.getParam(PlanckTOITAPRequest.TIMESTART);
        if (!StringUtils.isEmpty(t_begin)) {
            requiredParam(sb, PlanckTOITAPRequest.TIMESTART, req.getParam(PlanckTOITAPRequest.TIMESTART));
        }

        String t_end = req.getParam(PlanckTOITAPRequest.TIMEEND);
        if (!StringUtils.isEmpty(t_end)) {
            requiredParam(sb, PlanckTOITAPRequest.TIMEEND, req.getParam(PlanckTOITAPRequest.TIMEEND));
        }

        String type = req.getParam(PlanckTOITAPRequest.TYPE);
        if (!StringUtils.isEmpty(type)) {
            requiredParam(sb, PlanckTOITAPRequest.TYPE, req.getParam(PlanckTOITAPRequest.TYPE));
        }

        String size = req.getParam(PlanckTOITAPRequest.SEARCH_REGION_SIZE);
        if (!StringUtils.isEmpty(size)) {
            requiredParam(sb, PlanckTOITAPRequest.SEARCH_REGION_SIZE, req.getParam(PlanckTOITAPRequest.SEARCH_REGION_SIZE));
        }

        String selectband = req.getParam(PlanckTOITAPRequest.OPTBAND);
        if (!StringUtils.isEmpty(selectband)) {
            requiredParam(sb, PlanckTOITAPRequest.OPTBAND, req.getParam(PlanckTOITAPRequest.OPTBAND));
        }

        String detector = req.getParam(PlanckTOITAPRequest.DETECTOR);
        if (!StringUtils.isEmpty(detector)) {
            requiredParam(sb, PlanckTOITAPRequest.DETECTOR, req.getParam(PlanckTOITAPRequest.DETECTOR));
        }


        return sb.toString();
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        PlanckTOITAPRequest req = QueryUtil.assureType(PlanckTOITAPRequest.class, request);
        setXmlParams(req);

        // add cutout parameters, if applicable
        if (request.containsParam("subsize")) {
            meta.setAttribute("subsize", request.getParam("subsize"));
            meta.setAttribute("usingSubsize", "subSizeTrue");
        } else {
            meta.setAttribute("usingSubsize", "subSizeFalse");
        }
    }


    private static File makeFileName(PlanckTOITAPRequest req) throws IOException {
        return File.createTempFile("planck-toi-timerange", ".tbl", ServerContext.getPermWorkDir());
    }

    protected static void requiredParam(StringBuffer sb, String name, double value) throws EndUserException {
        if (!Double.isNaN(value)) {
            requiredParam(sb, name, value + "");

        } else {
            throw new EndUserException("Planck TOI search failed, Planck Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + name);
        }
    }

    protected static void requiredParam(StringBuffer sb, String name, String value) throws EndUserException {
        if (!StringUtil.isEmpty(value)) {
            sb.append(param(name, value));

        } else {
            throw new EndUserException("Planck TOI search failed, Planck Catalog is unavailable",
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

