package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.ReqConst;
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
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.hydra.data.PlanckTOITAPRequest;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: wmi
 * Date: 5/13/14
 * Time: 10:33 AM
 * To change this template use File | Settings | File Templates.
 */


@SearchProcessorImpl(id = "planckTOITAPQuery", params = {
        @ParamDoc(name = PlanckTOITAPRequest.TOITAP_HOST, desc = "(optional) the hostname, including port")
})
public class QueryPlanckTOITAP extends DynQueryProcessor {

    private static final Logger.LoggerImpl _log = Logger.getLogger();

    private final static String detc030_all = "27m,27s,28m,28s";
    private final static String detc044_all = "24m,24s,25m,25s,26m,26s";
    private final static String detc070_all = "18m,18s,19m,19s,20m,20s,21m,21s,22m,22s,23m,23s";
    private final static String detc100_all = "1a,1b,2a,2b,3a,3b,4a,4b";
    private final static String detc143_all = "1a,1b,2a,2b,3a,3b,4a,4b,5,6,7";
    private final static String detc217_all = "1,2,3,4,,5a,5b,6a,6b,7a,7b,8a,8b";
    private final static int secondInMillis = 1000;
    private final static int minuteInMillis = secondInMillis * 60;
    private final static int runlimit = 3 * minuteInMillis;
    private boolean overlimit = false;

    @Override
    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {
        File retFile = null;
        long start = System.currentTimeMillis();

        try {
            setXmlParams(request);
            PlanckTOITAPRequest req = QueryUtil.assureType(PlanckTOITAPRequest.class, request);
            retFile = searchPlanck(req);

        } catch (Exception e) {

            throw makeException(e, "Planck TOI Query Failed.");
        }

        return retFile;
    }


    private File searchPlanck(PlanckTOITAPRequest req) throws IOException, DataAccessException, EndUserException {
        File outFile = null;
        URLConnection conn = null;
        long start = System.currentTimeMillis();
        long elapseTime = 0;
        String overLimit = "no";

        try {
            outFile = makeFileName(req);

            URL url = createURL(req);
            conn = URLDownload.makeConnection(url);
            conn.setRequestProperty("Accept", "*/*");
            conn.setReadTimeout(runlimit);
            long time = conn.getReadTimeout();
            _log.info("runtime limit:" + time);

            URLDownload.getDataToFile(conn, outFile);

        } catch (SocketTimeoutException e) {
            _log.error(e, e.toString() + ", Search is too big");
            String umsg ="We recommend you reduce the search area size and/or select fewer detectors";

            throw new EndUserException("Your Planck TOI Search is extremely large. " + umsg, umsg);
        } catch (MalformedURLException e) {
            _log.error(e, "Bad URL");
            throw makeException(e, "Planck TOI Query Failed - bad url.");

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
        String url = req.getParam(PlanckTOITAPRequest.TOITAP_HOST);

        String paramStr = getParams(req);
        if (paramStr.startsWith("&")) {
            paramStr = paramStr.substring(1);
        }
        url += "/TAP/sync?LANG=ADQL&REQUEST=doQuery&QUERY=SELECT+round(mjd,0)+as+rmjd,count(mjd)+as+counter+FROM+" + paramStr;

        _log.info("querying URL:" + url);

        return new URL(url);
    }


    protected String getParams(PlanckTOITAPRequest req) throws EndUserException, IOException {
        StringBuffer sb = new StringBuffer(100);

        // toi_index_file
        String Freq = req.getParam(PlanckTOITAPRequest.OPTBAND);
        String toi_info = "planck_toi_" +Freq;
        //sb.append(URLEncoder.encode(toi_info, "UTF-8"));
        sb.append(toi_info);



        // http://***REMOVED***.ipac.caltech.edu:9029/TAP/sync?LANG=ADQL&REQUEST=doQuery&QUERY=SELECT+*+FROM+planck_toi_100_2b+WHERE+CONTAINS(POINT('J2000',ra,dec),CIRCLE('J2000',121.17440,-21.57294,1.0))=1&begin_time=55550.0&end_time=65650.5&format=ipac_table
        // http://***REMOVED***.ipac.caltech.edu:9120/TAP/sync?LANG=ADQL&REQUEST=doQuery&QUERY=SELECT+round%28mjd,0%29+as+rmjd,count%28mjd%29+FROM+planck_toi_044+WHERE+CONTAINS%28POINT%28%27J2000%27,ra,dec%29,CIRCLE%28%27J2000%27,121.17440,-21.57294,1.0%29%29=1+group+by+rmjd&format=ipac_table
        // http://***REMOVED***.ipac.caltech.edu:9120/TAP/sync?LANG=ADQL&REQUEST=doQuery&QUERY=SELECT+round(mjd,0)+as+rmjd,count(mjd)+FROM+planck_toi_044+WHERE+CONTAINS(POINT('J2000',ra,dec),CIRCLE('J2000',121.17440,-21.57294,1.0))=1 and (detector='24m' or detector='24s') group by rmjd&format=ipac_table

        // object name
        String objectName = req.getParam(PlanckTOITAPRequest.OBJ_NAME);

        // process constraints
        String constraints = processConstraints(req);
        if (!StringUtils.isEmpty(constraints)) {
            //sb.append(URLEncoder.encode(constraints, "UTF-8"));
            sb.append(constraints);
        }

        String paramString = sb.toString();
        if (paramString.startsWith("&")) {
            return paramString.substring(1);
        } else {
            return paramString;
        }

    }

    private String processConstraints(PlanckTOITAPRequest req) throws EndUserException {
        // create constraint array
        ArrayList<String> constraints = new ArrayList<String>();
        String constrStr = "+WHERE+CONTAINS(POINT('J2000',ra,dec),";
        String size =null;

        // search type
        String type = req.getParam(PlanckTOITAPRequest.TYPE);
        if (!StringUtils.isEmpty(type)) {
            if (type.equals("circle")) {
                size = req.getParam(PlanckTOITAPRequest.SEARCH_REGION_SIZE);}
            else if (type.equals("box")) {
                size = req.getParam(PlanckTOITAPRequest.SEARCH_BOX_SIZE);}
        }

        // search size and position
        String userTargetWorldPt = req.getParam("UserTargetWorldPt");
        if (userTargetWorldPt != null) {
            WorldPt pt = WorldPt.parse(userTargetWorldPt);
            if (pt != null) {
                pt = VisUtil.convertToJ2000(pt);
                pt = VisUtil.convert(pt, CoordinateSys.GALACTIC);
                String pos = pt.getLon() + "," + pt.getLat();
                if (type.equals("circle")) {
                    constraints.add("CIRCLE('GALACTIC'," + pos + "," + size + "))=1+and+(");}
                else if (type.equals("box")) {
                    constraints.add("BOX('GALACTIC'," + pos + "," + size+"," + size +"))=1+and+(");}
                //constraints.add(pos + "," + size + "))=1+and+(");
            }
        }

        // get search obj string
        String targetStr = req.getParam(SimpleTargetPanel.TARGET_NAME_KEY);
        if (targetStr == null) {
            targetStr = req.getSafeParam("UserTargetWorldPt");
            targetStr = targetStr.replace(";", ",");
        }
        targetStr = targetStr.replace(" ", "");
        String source = "OBJECT:'"+targetStr+"'" ;


        // process detectors

        String detector = req.getParam(PlanckTOITAPRequest.DETECTOR);
        String detcStr = req.getParam(detector);
        String detcStr_all = "";
        String Freq = req.getParam(PlanckTOITAPRequest.OPTBAND);
        _log.info("detcStr:" + detcStr);

        if (detcStr.equals("_all_")){
            if (!StringUtils.isEmpty(Freq)) {
                if (Freq.equals("100")) {
                    detcStr = detc100_all;}
                else if (Freq.equals("143")) {
                    detcStr = detc143_all;}
                else if (Freq.equals("217")) {
                    detcStr = detc217_all;}
                else if (Freq.equals("030")) {
                    detcStr = detc030_all;}
                else if (Freq.equals("044")) {
                    detcStr = detc044_all;}
                else if (Freq.equals("070")) {
                    detcStr = detc070_all;}
                else if (Freq.equals("353")) {
                    detcStr = detc100_all;}
                else if (Freq.equals("545")) {
                    detcStr = detc100_all;}
                else if (Freq.equals("857")) {
                    detcStr = detc100_all;}
            }

        }
        _log.info("detcStr1:" + detcStr);


        // add detector info

        String detectors[] = detcStr.split(",");
        constraints.add("(detector='"+detectors[0]+"'");
        for(int j = 1; j < detectors.length; j++){
            constraints.add("+or+detector='"+detectors[j]+"'");
        }
        constraints.add(")");

        // process SSO flag

        String ssoflag = req.getParam(PlanckTOITAPRequest.SSOFLAG);
        String sso_constr = "";
                if (ssoflag.equals("false")){
                    sso_constr = "+and+(sso='0')";
                }
        constraints.add(sso_constr);
        _log.info("ssoflag:" +ssoflag);


        // process DATE RANGE
        String timeStart = req.getParam("timeStart");
        if (!StringUtils.isEmpty(timeStart)) {
            constraints.add("+and+(mjd>=" + DynServerUtils.convertUnixToMJD(timeStart));
        }
        String timeEnd = req.getParam("timeEnd");
        if (!StringUtils.isEmpty(timeEnd)) {
            constraints.add("+and+mjd<=" + DynServerUtils.convertUnixToMJD(timeEnd) + ")");
        }

        // ending with format of output:

        constraints.add(")+group+by+rmjd&format=ipac_table"+"&user_metadata={"+source+",DETNAM:'" + detcStr +"'}");


    // compile all constraints
        if (!constraints.isEmpty()) {

            int i = 0;
            for (String s : constraints) {
                constrStr += s;

                i++;
            }
        }

        return constrStr;

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
        return File.createTempFile("planck-toi", ".tbl", ServerContext.getPermWorkDir());
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

