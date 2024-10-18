/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.wise;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WiseRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IBESearchProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.QueryDescResolver;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.multipart.MultiPartPostBuilder;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.table.io.IpacTableException;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.table.query.DataGroupQuery;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


@SearchProcessorImpl(id = "WiseQuery", params = {
        @ParamDoc(name = WiseRequest.HOST, desc = "(optional) the hostname, including port"),
        @ParamDoc(name = WiseRequest.SCHEMA_GROUP, desc = "the name of the schema group"),
        @ParamDoc(name = WiseRequest.SCHEMA, desc = "the name of the schema within the schema group"),
        @ParamDoc(name = WiseRequest.POS, desc = "ra,dec: the search region center in decimal degrees, must be in equatorial J2000 (FK5)"),
        @ParamDoc(name = WiseRequest.SIZE, desc = "(optional) x[,y]: the search region size - a rectangle centered on ra,dec with a width of " +
                "x degrees and height of y degrees, where the y axis points north from (ra,dec). " +
                "if y is omitted, it defaults to the value for x. if omitted altogether, the search " +
                "region becomes a point."),
        @ParamDoc(name = WiseRequest.MCEN, desc = "(optional) mcen: search for the most centered image covering the position.  " +
                "This parameter is ignored for region searches."),
        @ParamDoc(name = WiseRequest.INTERSECT, desc = "(optional) values: OVERLAPS (default), COVERS, ENCLOSED, or CENTER. " +
                "OVERLAPS: The candidate image overlaps the search region, " +
                "COVERS: The candidate image covers or includes the entire search region, " +
                "ENCLOSED: The candidate image is entirely enclosed by the search region, " +
                "CENTER: The candidate image contains the search region center (ra,dec) (SIZE is ignored)")
})
public class QueryWise extends IBESearchProcessor {

    private static final Logger.LoggerImpl _log = Logger.getLogger();

    private MultiPartPostBuilder _postBuilder = null;


    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {
        File retFile;
        try {
            WiseRequest req = QueryUtil.assureType(WiseRequest.class, request);

            StopWatch.getInstance().start("Wise Search");
            retFile = searchWise(req);
            StopWatch.getInstance().printLog("Wise Search");

        } catch (Exception e) {
            throw makeException(e, "WISE Query Failed.");
        }

        return retFile;
    }

    public QueryDescResolver getDescResolver() {
        return new QueryDescResolver.DescBySearchResolver(new WiseSearchDescResolver());
    }

    protected boolean isPost(WiseRequest req) {
        return (req.getParam(WiseRequest.FILENAME) != null);
    }

    private File searchWise(WiseRequest req) throws IOException, DataAccessException, EndUserException {
        File outFile = null;
        URLConnection conn = null;

        try {

            // do we need to set parameters from an uploaded file?
            DataGroup dgToJoin = uploadedPreprocess(req);

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
                    String msg = parseMessageFromIBE(convertStreamToString(is));
                    is.close();

                    throw new EndUserException("WISE Search Failed: " + msg, msg);
                }

            } else {
                URLDownload.getDataToFile(url, outFile, null, Collections.singletonMap("Accept", "*/*"));
            }

            // do we need to join the results with an uploaded table?
            if (dgToJoin != null) {
                outFile = uploadedPostprocess(req, dgToJoin, outFile);
            }

        } catch (MalformedURLException e) {
            _log.error(e, "Bad URL");
            throw makeException(e, "WISE Query Failed - bad url.");

        } catch (IOException e) {
            _log.error(e, e.toString());
            if (conn != null && conn instanceof HttpURLConnection) {
                HttpURLConnection httpConn = (HttpURLConnection) conn;
                int respCode = httpConn.getResponseCode();
                if (respCode == 400 || respCode == 404 || respCode == 500) {
                    InputStream is = httpConn.getErrorStream();
                    if (is != null) {
                        String msg = parseMessageFromIBE(convertStreamToString(is)).trim();
                        if (msg.contains("NoneType")) {
                            return null;
                        } else {
                            throw new EndUserException("WISE Search Failed: " + msg, msg);
                        }
                    } else {
                        String msg = httpConn.getResponseMessage();
                        throw new EndUserException("WISE Search Failed: " + msg, msg);
                    }
                }
            } else {
                throw makeException(e, "WISE Query Failed - network error.");
            }

        } catch (EndUserException e) {
            _log.error(e, e.toString());
            throw new EndUserException(e.getEndUserMsg(), e.getMoreDetailMsg());

        } catch (Exception e) {
            throw makeException(e, "WISE Query Failed.");
        }

        return outFile;
    }

    private DataGroup uploadedPreprocess(WiseRequest req) throws EndUserException, IOException {
        String colName;
        String paramName;
        String filenameParam;
        if (req.getParam("coaddIdFilename") != null) {
            filenameParam = "coaddIdFilename";
            colName = "coadd_id";
            paramName = "coaddId";
        } else if (req.getParam("scanIdFilename") != null) {
            filenameParam = "scanIdFilename";
            colName = "scan_id";
            paramName = "scanId";
        } else {
            return null;
        }

        DataGroup dg = IpacTableReader.read(ServerContext.convertToFile(req.getParam(filenameParam)));
        for (DataType dt : dg.getDataDefinitions()) {
            if (dt.getKeyName().equals(colName)) {
                Set<String> vals = new LinkedHashSet<String>();
                for (DataObject dObj : dg) {
                    vals.add((String) dObj.getDataElement(dt));
                }
                req.setParam(paramName, CollectionUtil.toString(vals, ","));
                return dg;
            }
        }
        throw new EndUserException("Can not find " + colName + " column in the uploaded table.", "Unable to create search request.");
    }

    private File uploadedPostprocess(WiseRequest req, DataGroup inDg, File outFile) throws IpacTableException, IOException {
        final String colName;
        if (req.getParam("coaddIdFilename") != null) {
            colName = "coadd_id";
        } else if (req.getParam("scanIdFilename") != null) {
            colName = "scan_id";
        } else {
            return outFile;
        }
        DataGroup dg = IpacTableReader.read(outFile);
        DataType[] inDefinitions = inDg.getDataDefinitions();
        DataGroup dgJoined = DataGroupQuery.join(inDg, inDefinitions, dg, dg.getDataDefinitions(),
                                                    (dataObject, dataObject1) -> ((String) dataObject.getDataElement(colName))
                                                                                    .compareTo((String) dataObject1.getDataElement(colName)));
        DataType[] outDefinitions = dgJoined.getDataDefinitions();
        for (int i = 0; i < inDefinitions.length; i++) {
            outDefinitions[i].setKeyName("in_" + outDefinitions[i].getKeyName());
        }

        File newOutFile = File.createTempFile("wise-catalog-joined-", ".tbl", QueryUtil.getTempDir(req));
        IpacTableWriter.save(newOutFile, dgJoined);
        return newOutFile;
    }

    private URL createURL(WiseRequest req) throws EndUserException,
                                                  IOException {
        String host = req.getHost();
        String schemaGroup = req.getSchemaGroup();
        String schema = req.getTableSchema();
        String table = req.getTable();

        String urlString = makeBaseSearchURL(host, schemaGroup, schema, table);
        if (!isPost(req)) {
            urlString += "?" + getParams(req);
        }

        _log.info("querying URL:" + urlString);

        return new URL(urlString);
    }


    protected void insertPostParams(WiseRequest req) throws EndUserException, IOException {
        // process POS/filename
        requiredPostFileCacheParam(_postBuilder, WiseRequest.POS, req.getFilename());

        // process INTERSECT
        optionalPostParam(_postBuilder, WiseRequest.INTERSECT, req.getParam("intersect"));

        // process MCEN / SIZE
        String mcen = req.getParam("mcenter");
        if (mcen != null && mcen.equalsIgnoreCase("mcen")) {
            requiredPostParam(_postBuilder, WiseRequest.MCEN);

        } else {
            optionalPostParam(_postBuilder, WiseRequest.SIZE, req.getParam("size"));
        }

        // process constraints
        String constraints = processConstraints(req);
        if (!StringUtils.isEmpty(constraints)) {
            optionalPostParam(_postBuilder, "where", constraints);
        }
    }

    protected String getParams(WiseRequest req) throws EndUserException, IOException {
        StringBuffer sb = new StringBuffer(100);

        // process POS - target search
        String userTargetWorldPt = req.getParam(ServerParams.USER_TARGET_WORLD_PT);
        if (userTargetWorldPt != null) {
            WorldPt pt = WorldPt.parse(userTargetWorldPt);
            if (pt != null) {
                pt = VisUtil.convertToJ2000(pt);
                String pos = pt.getLon() + "," + pt.getLat();
                requiredParam(sb, WiseRequest.POS, pos);

                optionalParam(sb, WiseRequest.INTERSECT, req.getParam("intersect"));

                String mcen = req.getParam("mcenter");
                if (mcen != null && mcen.equalsIgnoreCase("mcen")) {
                    optionalParam(sb, WiseRequest.MCEN);

                } else {
                    optionalParam(sb, WiseRequest.SIZE, req.getParam("size"));
                }
            }
        }

        // process POS - source search
        String refSourceId = req.getParam("refSourceId");
        if (refSourceId != null) {
            String schemaGroup = req.getSchemaGroup();
            String schema = req.getServiceSchema();
            String sourceTable = req.getSourceTable();

            if (sourceTable == null) {
                throw new EndUserException("WISE search failed. " +
                                                   "Unable to find source table for source ID " + refSourceId + ", schema " + schema,
                                           "Invalid parameters.");
            }
            String sourceSpec = schemaGroup + "." + schema + "." + sourceTable + "(\"source_id\":\"" + refSourceId + "\")";
            requiredParam(sb, WiseRequest.REF_BY, URLEncoder.encode(sourceSpec, "UTF-8"));

        } else {
            String sourceId = req.getParam("sourceId");
            if (sourceId != null) {
                String schemaGroup = req.getSchemaGroup();
                String sourceSchema = req.getParam("sourceSchema");
                String sourceProductLevel = WiseRequest.getProductLevelFromSourceId(sourceId);
                if (sourceProductLevel == null) {
                    throw new EndUserException("WISE Search Failed. Invalid Source ID. ", "source ID \"" + sourceId + "\"");
                }
                String sourceTable = req.getSourceTable(sourceProductLevel, sourceSchema);
                if (sourceTable == null) {
                    throw new EndUserException("WISE search failed. " +
                                                       "Unable to find source table for source ID " + sourceId + ", sourceSchema " + sourceSchema,
                                               "Invalid Parameters.");
                }

                String sourceSpec = schemaGroup + "." + WiseRequest.getServiceSchema(sourceSchema) + "." + sourceTable + "(\"source_id\":\"" + sourceId + "\")";
                requiredParam(sb, WiseRequest.POS, URLEncoder.encode(sourceSpec, "UTF-8"));

                optionalParam(sb, WiseRequest.INTERSECT, req.getParam("intersect"));

                String mcen = req.getParam("mcenter");
                if (mcen != null && mcen.equalsIgnoreCase("mcen")) {
                    optionalParam(sb, WiseRequest.MCEN);

                } else {
                    optionalParam(sb, WiseRequest.SIZE, req.getParam("size"));
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

    private String processConstraints(WiseRequest req) throws EndUserException {
        // create constraint array
        ArrayList<String> constraints = new ArrayList<String>();
        String constrStr = "";

        String productLevel = req.getSafeParam("ProductLevel");

        String schema = req.getSchema();
        List<String> imageSets = Arrays.asList(schema.split(","));
        if (WiseRequest.useMergedTable(schema) && imageSets.size() < 10) {   // 7 means all were selected.  no constraint is needed.
            int n = 0;
            String imageSetConstraint = "image_set";
            if (imageSets.size() > 1) {
                imageSetConstraint += " IN (";
            } else {
                imageSetConstraint += "=";
            }
            if (imageSets.contains(WiseRequest.ALLWISE_MULTIBAND)) {
                imageSetConstraint += "5";
                n++;
            }
            if (imageSets.contains(WiseRequest.ALLSKY_4BAND)) {
                if (n>0) imageSetConstraint += ",4";
                else imageSetConstraint += "4";
                n++;
            }
            if (imageSets.contains(WiseRequest.CRYO_3BAND)) {
                if (n>0) imageSetConstraint += ",3";
                else imageSetConstraint += "3";
                n++;
            }
            if (imageSets.contains(WiseRequest.POSTCRYO)) {
                if (n>0) imageSetConstraint += ",2";
                else imageSetConstraint += "2";
                n++;
            }
            // commented out the internal wise image set (iwise gwt version)
            /*if (imageSets.contains(WiseRequest.NEOWISER_YR1)) {
                if (n>0) imageSetConstraint += ",6";
                else imageSetConstraint += "6";
                n++;
            }
            if (imageSets.contains(WiseRequest.NEOWISER_YR2)) {
                if (n>0) imageSetConstraint += ",7";
                else imageSetConstraint += "7";
                n++;
            }
            if (imageSets.contains(WiseRequest.NEOWISER_YR3)) {
                if (n>0) imageSetConstraint += ",8";
                else imageSetConstraint += "8";
                n++;
            }
            if (imageSets.contains(WiseRequest.NEOWISER_YR4)) {
                if (n>0) imageSetConstraint += ",9";
                else imageSetConstraint += "9";
                n++;
            }
            if (imageSets.contains(WiseRequest.NEOWISER_YR5)) {
                if (n>0) imageSetConstraint += ",10";
                else imageSetConstraint += "10";
                n++;
            }
            if (imageSets.contains(WiseRequest.NEOWISER_YR6)) {
                if (n>0) imageSetConstraint += ",11";
                else imageSetConstraint += "11";
                n++;
            }
            if (imageSets.contains(WiseRequest.NEOWISER_YR7)) {
                if (n>0) imageSetConstraint += ",12";
                else imageSetConstraint += "12";
                n++;
            }
            if (imageSets.contains(WiseRequest.NEOWISER_YR8)) {
                if (n>0) imageSetConstraint += ",13";
                else imageSetConstraint += "13";
                n++;
            }
            if (imageSets.contains(WiseRequest.NEOWISER_YR9)) {
                if (n>0) imageSetConstraint += ",14";
                else imageSetConstraint += "14";
                n++;
            }*/

            if (imageSets.contains(WiseRequest.NEOWISER)) {    // public merge upto yr10
                if (n > 0) imageSetConstraint += ",6,7,8,9,10,11,12,13,14,15,16";
                else imageSetConstraint += "6,7,8,9,10,11,12,13,14,15,16";
                n++;
            }

            if (imageSets.size() > 1) {
                imageSetConstraint += ")";
            }
            if (n>0) {
                constraints.add(imageSetConstraint);
            }
        }

        // process L1b only constraints
        if (productLevel.equalsIgnoreCase("1b")) {
            // process DATE RANGE
            String timeStart = req.getParam("timeStart");
            if (!StringUtils.isEmpty(timeStart)) {
                constraints.add("mjd_obs>='" + convertUnixToMJD(timeStart) + "'");
            }
            String timeEnd = req.getParam("timeEnd");
            if (!StringUtils.isEmpty(timeEnd)) {
                constraints.add("mjd_obs<='" + convertUnixToMJD(timeEnd) + "'");
            }

            // process Scan IDs (support multiple IDs)
            String scanIds = req.getParam("scanId");
            if (!StringUtils.isEmpty(scanIds)) {
                String[] scanArray = scanIds.split("[,; ]+");
                String scanConstraint = "scan_id";
                if (scanArray.length == 1) {
                    scanConstraint += "='" + scanArray[0] + "'";
                } else {
                    scanConstraint += " IN (";
                    int cnt = 0;
                    for (String scanId : scanArray) {
                        if (StringUtils.isEmpty(scanId)) {
                            continue;
                        }

                        if (cnt > 0) {
                            scanConstraint += ",";
                        }
                        scanConstraint += "'" + scanId + "'";
                        cnt++;
                    }

                    scanConstraint += ")";
                }

                constraints.add(scanConstraint);
            }

            // process FRAME Numbers
            String frameOp = req.getParam("frameOp");
            if (!StringUtils.isEmpty(frameOp)) {
                String frameConstraint = "frame_num";
                if (frameOp.equals("eq")) {
                    String frameVal1 = req.getParam("frameVal1");
                    if (!StringUtils.isEmpty(frameVal1)) {
                        frameConstraint += "=" + frameVal1.trim();
                    }

                } else if (frameOp.equals("gt")) {
                    String frameVal1 = req.getParam("frameVal1");
                    if (!StringUtils.isEmpty(frameVal1)) {
                        frameConstraint += ">" + frameVal1.trim();
                    }

                } else if (frameOp.equals("lt")) {
                    String frameVal1 = req.getParam("frameVal1");
                    if (!StringUtils.isEmpty(frameVal1)) {
                        frameConstraint += "<" + frameVal1.trim();
                    }

                } else if (frameOp.equals("in")) {
                    String frameVal3 = req.getParam("frameVal3");
                    if (!StringUtils.isEmpty(frameVal3)) {
                        String[] frameArray = frameVal3.split("[,; ]+");
                        if (frameArray.length == 1) {
                            frameConstraint += "=" + frameArray[0];
                        } else {
                            frameConstraint += " IN (";
                            int cnt = 0;
                            for (String frameNum : frameArray) {
                                if (StringUtils.isEmpty(frameNum)) {
                                    continue;
                                }

                                if (cnt > 0) {
                                    frameConstraint += ",";
                                }
                                frameConstraint += frameNum;
                                cnt++;
                            }

                            frameConstraint += ")";
                        }
                    }

                } else if (frameOp.equals("be")) {
                    String frameVal1 = req.getParam("frameVal1");
                    String frameVal2 = req.getParam("frameVal2");
                    if (!StringUtils.isEmpty(frameVal1) && StringUtils.isEmpty(frameVal2)) {
                        throw (new EndUserException("Missing second BETWEEN constraint!", ""));
                    }
                    if (StringUtils.isEmpty(frameVal1) && !StringUtils.isEmpty(frameVal2)) {
                        throw (new EndUserException("Missing first BETWEEN constraint!", ""));
                    }
                    if (!StringUtils.isEmpty(frameVal1) && !StringUtils.isEmpty(frameVal2)) {
                        frameConstraint += " BETWEEN " + frameVal1.trim() + " AND " + frameVal2.trim();
                    }
                }

                if (frameConstraint.length() > "frame_num".length()) {
                    constraints.add(frameConstraint);
                }
            }
        }

        // process L3 only constraints
        if (productLevel.equalsIgnoreCase("3a") || productLevel.equalsIgnoreCase("3o")) {
            // process COADD IDs (support multiple IDs)
            String coaddIds = req.getParam("coaddId");
            if (!StringUtils.isEmpty(coaddIds)) {
                String[] coaddArray = coaddIds.split("[,; ]+");
                String coaddConstraint = "coadd_id";
                if (coaddArray.length == 1) {
                    coaddConstraint += "='" + coaddArray[0] + "'";
                } else {
                    coaddConstraint += " IN (";
                    int cnt = 0;
                    for (String coaddId : coaddArray) {
                        if (StringUtils.isEmpty(coaddId)) {
                            continue;
                        }

                        if (cnt > 0) {
                            coaddConstraint += ",";
                        }
                        coaddConstraint += "'" + coaddId + "'";
                        cnt++;
                    }

                    coaddConstraint += ")";
                }

                constraints.add(coaddConstraint);
            }
        }

        // process BAND - ENUMSTRING
        String bands = req.getParam("band");
        if (!StringUtils.isEmpty(bands) && bands.split(",").length < 4) {
            constraints.add("band IN (" + bands + ")");
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
        WiseRequest req = QueryUtil.assureType(WiseRequest.class, request);
        String host = req.getHost();
        String schemaGroup = req.getSchemaGroup();
        String schema = req.getTableSchema();
        String table = req.getTable();
        return makeDDURL(host, schemaGroup, schema, table);
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        WiseRequest req = QueryUtil.assureType(WiseRequest.class, request);

        meta.setAttribute(WiseRequest.SCHEMA, request.getParam(WiseRequest.SCHEMA));
        // add cutout parameters, if applicable
        String subsize = request.getParam("subsize");
        if (subsize != null) {
            meta.setAttribute("subsize", subsize);
        }


        String isFull = StringUtils.isEmpty(subsize) ? "-full" : "-sub";
        String level = req.getSafeParam("ProductLevel");
        meta.setAttribute("ProductLevelAndSize", level + isFull);


    }


    private static File makeFileName(WiseRequest req) throws IOException {
        return File.createTempFile("wise-catalog-original-", ".tbl", QueryUtil.getTempDir(req));
    }

    private static String convertUnixToMJD(String unix) {
        if (!StringUtils.isEmpty(unix)) {
            long unixL = Long.parseLong(unix);

            // derived from http://en.wikipedia.org/wiki/Julian_day
            long mdjL = (unixL / 86400000) + 40587;

            return Long.toString(mdjL);

        } else {
            return null;
        }
    }

    public static String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            StringBuilder sb = new StringBuilder();
            String line;

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } finally {
                is.close();
            }
            return sb.toString();
        } else {
            return "";
        }
    }
}

