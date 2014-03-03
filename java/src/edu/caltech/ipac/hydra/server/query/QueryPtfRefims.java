package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.dyn.DynServerUtils;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IBESearchProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.multipart.MultiPartPostBuilder;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.hydra.data.PtfRequest;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA. User: wmi Date: 9/25/13 Time: 2:51 PM To change this template use File | Settings | File
 * Templates.
 */

@SearchProcessorImpl(id = "PtfRefimsQuery", params = {
        @ParamDoc(name = PtfRequest.HOST, desc = "(optional) the hostname, including port"),
        @ParamDoc(name = PtfRequest.SCHEMA_GROUP, desc = "the name of the schema group"),
        @ParamDoc(name = PtfRequest.SCHEMA, desc = "the name of the schema within the schema group"),
        @ParamDoc(name = PtfRequest.TABLE, desc = "the name of the table within the schema"),
        @ParamDoc(name = PtfRequest.POS, desc = "ra,dec: the search region center in decimal degrees, must be in equatorial J2000 (FK5)"),
        @ParamDoc(name = PtfRequest.SIZE, desc = "(optional) x[,y]: the search region size - a rectangle centered on ra,dec with a width of " +
                "x degrees and height of y degrees, where the y axis points north from (ra,dec). " +
                "if y is omitted, it defaults to the value for x. if omitted altogether, the search " +
                "region becomes a point."),
        @ParamDoc(name = PtfRequest.MCEN, desc = "(optional) mcen: search for the most centered image covering the position.  " +
                "This parameter is ignored for region searches."),
        @ParamDoc(name = PtfRequest.INTERSECT, desc = "(optional) values: OVERLAPS (default), COVERS, ENCLOSED, or CENTER. " +
                "OVERLAPS: The candidate image overlaps the search region, " +
                "COVERS: The candidate image covers or includes the entire search region, " +
                "ENCLOSED: The candidate image is entirely enclosed by the search region, " +
                "CENTER: The candidate image contains the search region center (ra,dec) (SIZE is ignored)")
})


public class QueryPtfRefims extends IBESearchProcessor {

    private static final Logger.LoggerImpl _log = Logger.getLogger();

    private MultiPartPostBuilder _postBuilder = null;


    @Override
    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {
        File retFile = null;
        try {
            setXmlParams(request);
            PtfRequest req = QueryUtil.assureType(PtfRequest.class, request);

            StopWatch.getInstance().start("Ptf Search");
            retFile = searchPtf(req);
            StopWatch.getInstance().printLog("Ptf Search");

        } catch (Exception e) {
            throw makeException(e, "PTF Query Failed.");
        }

        return retFile;
    }

    protected boolean isPost(PtfRequest req) {
        return (req.getParam(PtfRequest.FILENAME) != null);
    }

    private File searchPtf(PtfRequest req) throws IOException, DataAccessException, EndUserException {
        File outFile = null;
        URLConnection conn = null;

        try {
            outFile = makeFileName(req);

            URL url = createURL(req);
            if (isPost(req)) {
                _postBuilder = new MultiPartPostBuilder(url.toString());
                _postBuilder.addHeader("Accept", "*/*");
                insertPostParams(req);
                BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(outFile), 10240);
                MultiPartPostBuilder.MultiPartRespnse resp = _postBuilder.post(writer);
                writer.close();

                int statusCode = resp.getStatusCode();
                if (statusCode == 400 || statusCode == 404 || statusCode == 500) {
                    InputStream is = new FileInputStream(outFile);
                    String msg = parseMessageFromIBE(DynServerUtils.convertStreamToString(is));
                    is.close();

                    throw new EndUserException("PTF Search Failed: " + msg, msg);
                }

            } else {
                downloadFile(url, outFile);
            }

        } catch (MalformedURLException e) {
            _log.error(e, "Bad URL");
            throw makeException(e, "PTF Query Failed - bad url.");

        } catch (IOException e) {
            _log.error(e, e.toString());
            if (conn != null && conn instanceof HttpURLConnection) {
                HttpURLConnection httpConn = (HttpURLConnection) conn;
                int respCode = httpConn.getResponseCode();
                if (respCode == 400 || respCode == 404 || respCode == 500) {
                    InputStream is = httpConn.getErrorStream();
                    if (is != null) {
                        String msg = parseMessageFromIBE(DynServerUtils.convertStreamToString(is));
                        throw new EndUserException("PTF Search Failed: " + msg, msg);

                    } else {
                        String msg = httpConn.getResponseMessage();
                        throw new EndUserException("PTF Search Failed: " + msg, msg);
                    }
                }

            } else {
                throw makeException(e, "PTF Query Failed - network error.");
            }

        } catch (EndUserException e) {
            _log.error(e, e.toString());
            throw new EndUserException(e.getEndUserMsg(), e.getMoreDetailMsg());

        } catch (Exception e) {
            throw makeException(e, "PTF Query Failed.");
        }

        return outFile;
    }

    private URL createURL(PtfRequest req) throws EndUserException,
            IOException {
        String host = req.getHost();
        String schemaGroup = req.getSchemaGroup();
        String schema = req.getSchema();
        String table = req.getTable();

        String urlString = makeBaseSearchURL(host, schemaGroup, schema, table);
        if (!isPost(req)) {
            urlString += "?" + getParams(req);
        }

        _log.info("querying URL:" + urlString);

        return new URL(urlString);
    }


    protected void insertPostParams(PtfRequest req) throws EndUserException, IOException {
        // process POS/filename
        requiredPostFileCacheParam(_postBuilder, PtfRequest.POS, req.getFilename());

        // process INTERSECT
        optionalPostParam(_postBuilder, PtfRequest.INTERSECT, req.getParam("intersect"));

        // process MCEN / SIZE
        String mcen = req.getParam("mcenter");
        if (mcen != null && mcen.equalsIgnoreCase("mcen")) {
            requiredPostParam(_postBuilder, PtfRequest.MCEN);

        } else {
            optionalPostParam(_postBuilder, PtfRequest.SIZE, req.getParam("size"));
        }

        // process constraints
        String constraints = processConstraints(req);
        if (!StringUtils.isEmpty(constraints)) {
            optionalPostParam(_postBuilder, "where", constraints);
        }
    }

    protected String getParams(PtfRequest req) throws EndUserException, IOException {
        StringBuffer sb = new StringBuffer(100);

        // process POS - target search
        String userTargetWorldPt = req.getParam("UserTargetWorldPt");
        if (userTargetWorldPt != null) {
            WorldPt pt = WorldPt.parse(userTargetWorldPt);
            if (pt != null) {
                pt = VisUtil.convertToJ2000(pt);
                String pos = pt.getLon() + "," + pt.getLat();
                requiredParam(sb, PtfRequest.POS, pos);

                optionalParam(sb, PtfRequest.INTERSECT, req.getParam("intersect"));

                String mcen = req.getParam("mcenter");
                if (mcen != null && mcen.equalsIgnoreCase("mcen")) {
                    optionalParam(sb, PtfRequest.MCEN);

                } else {
                    optionalParam(sb, PtfRequest.SIZE, req.getParam("size"));
                }
            }
        }

        // process constraints
        String constraints = processConstraints(req);
        if (!StringUtils.isEmpty(constraints)) {
            sb.append("&where=").append(URLEncoder.encode(constraints, "UTF-8"));
        }

        String paramString = sb.toString();
        if (paramString.startsWith("&")) {
            return paramString.substring(1);
        } else {
            return paramString;
        }
    }

    private String processConstraints(PtfRequest req) throws EndUserException {
        // create constraint array
        ArrayList<String> constraints = new ArrayList<String>();
        String constrStr = "";

//        String productLevel = req.getSafeParam("ProductLevel");
        String productLevel = "l2";

        // process L1 only constraints
        if (productLevel.equalsIgnoreCase("l2")) {
            // process DATE RANGE
            String timeStart = req.getParam("timeStart");
            if (!StringUtils.isEmpty(timeStart)) {
                constraints.add("obsmjd>='" + DynServerUtils.convertUnixToMJD(timeStart) + "'");
            }
            String timeEnd = req.getParam("timeEnd");
            if (!StringUtils.isEmpty(timeEnd)) {
                constraints.add("obsmjd<='" + DynServerUtils.convertUnixToMJD(timeEnd) + "'");
            }

            // process PTF Field IDs (support multiple field IDs)
            String ptfFields = req.getParam("ptfField");
            if (!StringUtils.isEmpty(ptfFields)) {
                String[] ptffieldArray = ptfFields.split("[,; ]+");
                String ptffieldConstraint = "ptffield";
                if (ptffieldArray.length == 1) {
                    ptffieldConstraint += "='" + ptffieldArray[0] + "'";
                } else {
                    ptffieldConstraint += " IN (";
                    int cnt = 0;
                    for (String ptfField : ptffieldArray) {
                        if (StringUtils.isEmpty(ptfField)) {
                            continue;
                        }

                        if (cnt > 0) {
                            ptffieldConstraint += ",";
                        }
                        ptffieldConstraint += "'" + ptfField + "'";
                        cnt++;
                    }

                    ptffieldConstraint += ")";
                }

                constraints.add(ptffieldConstraint);
            }

            //process CCD IDs (support multiple ccdids)
            String ccdIds = req.getParam("ccdId");
            if (!StringUtils.isEmpty(ccdIds)) {
                String[] ccdIdArray = ccdIds.split("[,; ]+");
                String ccdIdConstraint = "ccdid";
                if (ccdIdArray.length == 1) {
                    ccdIdConstraint += "='" + ccdIdArray[0] + "'";
                } else {
                    ccdIdConstraint += " IN (";
                    int cnt = 0;
                    for (String ccdId : ccdIdArray) {
                        if (StringUtils.isEmpty(ccdId)) {
                            continue;
                        }

                        if (cnt > 0) {
                            ccdIdConstraint += ",";
                        }
                        ccdIdConstraint += "'" + ccdId + "'";
                        cnt++;
                    }

                    ccdIdConstraint += ")";
                }

                constraints.add(ccdIdConstraint);
            }

        }

        // compile all constraints
        if (!constraints.isEmpty()) {

            int i = 0;
            for (String s : constraints) {
                if (i > 0) {
                    constrStr += " AND ";
                }
                constrStr += s;

                i++;
            }
        }

        return constrStr;
    }

    protected String getDDUrl(ServerRequest request) {
        PtfRequest req = QueryUtil.assureType(PtfRequest.class, request);
        String host = req.getHost();
        String schemaGroup = req.getSchemaGroup();
        String schema = req.getSchema();
        String table = req.getTable();
        return makeDDURL(host, schemaGroup, schema, table);
    }

    @Override
    public boolean isSecurityAware() {
        return true;
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        PtfRequest req = QueryUtil.assureType(PtfRequest.class, request);
        setXmlParams(req);

        if (request.containsParam("subsize")) {
            meta.setAttribute("subsize", request.getParam("subsize"));
            meta.setAttribute("usingSubsize", "subSizeTrue");
        } else {
            meta.setAttribute("usingSubsize", "subSizeFalse");
        }
    }


    private static File makeFileName(PtfRequest req) throws IOException {
        return File.createTempFile("ptf-catalog-original", ".tbl", ServerContext.getPermWorkDir());
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
