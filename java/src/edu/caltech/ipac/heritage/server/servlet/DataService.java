package edu.caltech.ipac.heritage.server.servlet;

import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.heritage.commands.SearchByNaifIDCmd;
import edu.caltech.ipac.heritage.commands.SearchByPositionCmd;
import edu.caltech.ipac.heritage.commands.SearchByProgramCmd;
import edu.caltech.ipac.heritage.commands.SearchByRequestIDCmd;
import edu.caltech.ipac.heritage.searches.HeritageRequest;
import edu.caltech.ipac.heritage.searches.SearchByNaifID;
import edu.caltech.ipac.heritage.searches.SearchByPosition;
import edu.caltech.ipac.heritage.searches.SearchByProgramID;
import edu.caltech.ipac.heritage.searches.SearchByRequestID;
import edu.caltech.ipac.heritage.server.persistence.HeritageSecurityModule;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author tatianag
 *         $Id: DataService.java,v 1.8 2011/12/08 19:34:02 loi Exp $
 */
public class DataService extends HttpServlet {

    private static final double DtoR = Math.PI/180.0;
    private static final double RtoD = 180.0/Math.PI;
    private static final int    MAX_RECORDS = 5000;
    private static final double MAX_RADIUS_DEGREES = 1.5; // degrees
    private static final double MAX_SIZE_DEGREES = 2.13; // degrees  sqrt((MAX_RADIUS_DEGREES*2)^2/2)
    private static final double MIN_SIZE_DEGREES = 0.002; // degrees (min radius = 4 arcsec)


    private static final String PARAM_POS = "POS";
    private static final String PARAM_SIZE = "SIZE";
    private static final String PARAM_DATASET = "DATASET";
    private static final String PARAM_FORMAT = "FORMAT";
    private static final String PARAM_VERBOSITY = "VERB";

    private static final String PARAM_PID = "PID";
    private static final String PARAM_REQKEY = "REQKEY";
    private static final String PARAM_RA="RA";
    private static final String PARAM_DEC="DEC";
    private static final String PARAM_NAIFID="NAIFID";

    private static final String PARAM_ORIGINATOR="Originator"; // for statistics

    private enum SearchType {SIAP, ByFixedTarget, ByMovingTarget, ByPid, ByReqkey }

    private static String BAD_POS_PARAMETER = "BAD PARAMETER: ra and dec should be in degrees and separated by a comma, ex. POS=12.821,-33.4. No embedded whitespace is allowed.";
    private static String BAD_SIZE_PARAMETER = "BAD PARAMETER: angular size of the region should be in degrees (max. "+MAX_SIZE_DEGREES+" degrees) and should be one or two values, separated by a comma, ex. SIZE=2.0,1.5";
    private static String BAD_RADIUS_PARAMETER = "BAD PARAMETER: SIZE should be in degrees (max. "+MAX_RADIUS_DEGREES+" degrees)";


    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    private Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        LOG.debug("Query string", req.getQueryString());
        Map origParamMap = req.getParameterMap();
        Map<String, String> paramMap = new HashMap<String,String>();
        // parameters could be upper or lower case
        for (Object p : origParamMap.keySet()) {
            if (p instanceof String) {
                paramMap.put(((String)p).toUpperCase(), (((String[])origParamMap.get(p))[0]).trim());
            }
        }

        boolean isLevel1 = true;
        if(paramMap.containsKey(PARAM_DATASET)) {
            String ds =  paramMap.get(PARAM_DATASET);
            if (ds.toLowerCase().endsWith("level1")) {
                isLevel1 = true;
            } else if (ds.toLowerCase().endsWith("level2")) {
                isLevel1 = false;
            } else {
                sendError(res, "Unrecognized data set; only level1 and level2 sets are supported");
                return;
            }
        }

        SearchType searchType = null;
        if (paramMap.containsKey(PARAM_POS)) {
            searchType = SearchType.SIAP;
        } else if (paramMap.containsKey(PARAM_RA) || paramMap.containsKey(PARAM_DEC)) {
            searchType = SearchType.ByFixedTarget;
        } else if (paramMap.containsKey(PARAM_NAIFID)) {
            searchType = SearchType.ByMovingTarget;
        } else if (paramMap.containsKey(PARAM_PID)) {
            searchType = SearchType.ByPid;
        } else if (paramMap.containsKey(PARAM_REQKEY)) {
            searchType = SearchType.ByReqkey;
        }
        if (searchType == null) {
            sendError(res, "Please provide "+PARAM_POS+" or "+
                "("+PARAM_RA+" and "+PARAM_DEC+") or "+
                PARAM_NAIFID+" or "+
                PARAM_PID+" or "+
                PARAM_REQKEY+".");
            return;
        }

        DataGroupPart dgPart;
        try {
            HeritageRequest searchReq = getSearchRequest(res, paramMap, searchType, isLevel1);
            if (searchReq == null) {
                //sendError(res, "Unable to construct search request");
                return;
            }
            searchReq.setPageSize(MAX_RECORDS+1);
            searchReq.setParam(PARAM_ORIGINATOR, "HTTP_"+searchType.name());  // to gather statistics
            dgPart = (new SearchManager()).getDataGroup(searchReq);

            sendTable(res, paramMap, dgPart, searchType, isLevel1);

        } catch (Exception e) {
            LOG.error(e);
            sendError(res, e.getMessage());
        }

    }


    private HeritageRequest getSearchRequest(HttpServletResponse res, Map<String,String> paramMap, SearchType searchType, boolean isLevel1) throws IOException {


        double radius = 0;
        double ra = 0, dec=0;

        // Fixed Target Search
        if (searchType.equals(SearchType.ByFixedTarget)) {
            if(!paramMap.containsKey(PARAM_RA)) {
                sendError(res,"MISSING PARAMETER: "+PARAM_RA+" (degrees) is required to process this request");
                return null;
            }
            if(!paramMap.containsKey(PARAM_DEC)) {
                sendError(res,"MISSING PARAMETER: "+PARAM_DEC+" (degrees) is required to process this request");
                return null;
            }
            if(!paramMap.containsKey(PARAM_SIZE)) {
                sendError(res, "MISSING PARAMETER: "+PARAM_SIZE+" (degrees) is required to process this request");
                return null;
            }
            try {
                ra = Double.parseDouble(paramMap.get(PARAM_RA));
                if (ra < 0 || ra > 360) {
                    sendError(res, "Invalid "+PARAM_RA);
                    return null;
                }
                dec = Double.parseDouble(paramMap.get(PARAM_DEC));
                if (dec < -90 || dec > 90) {
                    sendError(res, "Invalid "+PARAM_DEC);
                    return null;
                }
            } catch (Exception e) {
                sendError(res, "Invalid position");
                return null;
            }
            try {
                radius = Double.parseDouble(paramMap.get(PARAM_SIZE));
                if (radius < 0 || radius > MAX_RADIUS_DEGREES) {
                    sendError(res, BAD_RADIUS_PARAMETER);
                    return null;
                }
                if (radius < MIN_SIZE_DEGREES) radius = MIN_SIZE_DEGREES;
            } catch (Exception e) {
                sendError(res, BAD_SIZE_PARAMETER);
                return null;
            }
        }

        // SIAP search
        if (searchType.equals(SearchType.SIAP)) {

            if(!paramMap.containsKey(PARAM_POS)) {
                sendError(res,"MISSING PARAMETER: "+PARAM_POS+" (ra,dec in degrees) is required to process this request");
                return null;
            }
            if(!paramMap.containsKey(PARAM_SIZE)) {
                sendError(res, "MISSING PARAMETER: "+PARAM_SIZE+" (degrees) is required to process this request");
                return null;
            }

            // The service must support a parameter with the name FORMAT to indicate the desired format
            // or formats of the images referenced by the output table. The value is a comma-delimited list
            // where each element can be any recognized MIME-type.
            if(paramMap.containsKey(PARAM_FORMAT)) {
                String format = paramMap.get(PARAM_FORMAT);
                // format can be colon delimited list
                if (!format.contains("ALL") && (!format.contains("image/fits"))) {
                    sendError(res, "UNSUPPORTED FORMAT: only ALL and image/fits are supported.");
                    return null;
                }
            }

            // parse ra and dec
            String pos = paramMap.get(PARAM_POS);
            int posLen = pos.length();
            for (int i = 0; i < posLen; i++) {
                if (Character.isWhitespace(pos.charAt(i))) {
                    sendError(res, BAD_POS_PARAMETER);
                    return null; 
                }
            }
            String [] radec = pos.split(",");
            if (radec.length < 2) {
                sendError(res, BAD_POS_PARAMETER);
                return null;
            } else {
                try {
                    ra = Double.parseDouble(radec[0]);
                    dec = Double.parseDouble(radec[1]);
                } catch (Exception e) {
                    sendError(res, BAD_POS_PARAMETER);
                    return null;
                }
                if (ra < 0 || ra > 360) {
                    sendError(res, BAD_POS_PARAMETER+"; invalid ra");
                    return null;
                }
                if (dec < -90 || dec > 90) {
                    sendError(res, BAD_POS_PARAMETER+"; invalid dec");
                    return null;
                }
            }
            // parse size
            String size = paramMap.get(PARAM_SIZE);
            String szRaDec [] = size.split(",");
            double szRa=0.1, szDec=0.1;
            if (szRaDec.length >= 2) {
                try {
                    szRa = Double.parseDouble(szRaDec[0]);
                    szDec = Double.parseDouble(szRaDec[1]);
                } catch (Exception e) {
                    sendError(res, BAD_SIZE_PARAMETER);
                    return null;
                }
                if (szRa < 0 || szRa > MAX_SIZE_DEGREES) {
                    if (szRa < 0) { sendError(res, "Invalid SIZE (ra)"); }
                    else { sendOverflow(res, BAD_SIZE_PARAMETER); }
                    return null;
                }
                if (szDec < 0 || szDec > MAX_SIZE_DEGREES) {
                    if (szDec < 0) { sendError(res, "Invalid SIZE (dec)"); }
                    else { sendOverflow(res, BAD_SIZE_PARAMETER); }
                    return null;
                }

            } else if (szRaDec.length == 1) {
                try {
                    szDec = Double.parseDouble(szRaDec[0]);
                    if (szDec < 0 || szDec > MAX_SIZE_DEGREES) {
                        if (szDec < 0) { sendError(res, "Invalid SIZE"); }
                        else { sendOverflow(res, BAD_SIZE_PARAMETER); }
                        return null;
                    }                    
                    double cos = Math.cos(dec*DtoR);
                    if (cos != 0) {
                        szRa = szDec/cos;
                    } else {
                        szRa = 360;
                    }
                    if (szRa > 360) szRa = 360; // near the poles - cone search
                } catch (Exception e) {
                    sendError(res, BAD_SIZE_PARAMETER);
                    return null;
                }
            }

            // SIZE = 0 is special case. For an atlas or pointed image archive this tests whether the given point is in any image.
            if (szRa < MIN_SIZE_DEGREES) szRa = MIN_SIZE_DEGREES;
            if (szDec < MIN_SIZE_DEGREES) szDec = MIN_SIZE_DEGREES;

            radius = getConeSearchRadius(dec, szRa, szDec);
        }

        if (radius > 0) {
            final String RADIUS = SearchByPositionCmd.RADIUS_KEY;

            Request request = new Request();
            request.setParam(ReqConst.USER_TARGET_WORLD_PT, new WorldPt(ra, dec, CoordinateSys.EQ_J2000).toString());
            request.setParam(RADIUS, Double.toString(radius));

            SearchByPosition.SingleTargetReq searchReq;
            if (isLevel1) {
                searchReq = new SearchByPosition.SingleTargetReq(SearchByPosition.Type.BCD, request);
            } else {
                searchReq = new SearchByPosition.SingleTargetReq(SearchByPosition.Type.PBCD, request);
            }
            return searchReq;
        }


        // Moving Target Search
        if (searchType.equals(SearchType.ByMovingTarget)) {
            try {
                StringUtils.convertToArrayInt(paramMap.get(PARAM_NAIFID), ",");
            } catch (Exception e) {
                sendError(res, "Bad "+PARAM_NAIFID+" Parameter: must be an integer or comma separated list of integers.");
                return null;
            }
            Request request = new Request();
            request.setParam(SearchByNaifIDCmd.NAIFID_KEY, paramMap.get(PARAM_NAIFID));
            SearchByNaifID.Req searchReq;
            if (isLevel1) {
                searchReq = new SearchByNaifID.Req(SearchByNaifID.Type.BCD, request);
            } else {
                searchReq = new SearchByNaifID.Req(SearchByNaifID.Type.PBCD, request);
            }
            return searchReq;
        }

        // PID Search
        if (searchType.equals(SearchType.ByPid)) {
            try {
                Integer.parseInt(paramMap.get(PARAM_PID));
            } catch (Exception e) {
                sendError(res, "Bad "+PARAM_PID+" Parameter: must be an integer.");
                return null;
            }
            Request request = new Request();
            request.setParam(SearchByProgramCmd.PROGRAM_KEY, paramMap.get(PARAM_PID));
            SearchByProgramID.Req searchReq;
            if (isLevel1) {
                searchReq = new SearchByProgramID.Req(SearchByProgramID.Type.BCD, request);
            } else {
                searchReq = new SearchByProgramID.Req(SearchByProgramID.Type.PBCD, request);
            }
            return searchReq;
        }

        // REQKEY Search
        if (searchType.equals(SearchType.ByReqkey)) {
            try {
                StringUtils.convertToArrayInt(paramMap.get(PARAM_REQKEY), ",");
            } catch (Exception e) {
                sendError(res, "Bad "+PARAM_REQKEY+" Parameter: must be an integer or comma separated list of integers.");
                return null;
            }
            Request request = new Request();
            request.setParam(SearchByRequestIDCmd.REQUESTID_KEY, paramMap.get(PARAM_REQKEY));
            SearchByRequestID.Req searchReq;
            if (isLevel1) {
                searchReq = new SearchByRequestID.Req(SearchByRequestID.Type.BCD, request);
            } else {
                searchReq = new SearchByRequestID.Req(SearchByRequestID.Type.PBCD, request);
            }
            return searchReq;
        }


        return null;
    }




    /**
     * The server failed to process the query. Possible reasons include:
     *
     * - The input query contained a syntax error.
     * - The way the query was posed was invalid for some reason, e.g., due to an invalid query region specification.
     * - A constraint parameter value was given an illegal value; e.g. DEC=91.
     * - The server trapped an internal error (e.g., failed to connect to its database) preventing further processing.
     *
     * @param res   output stream
     * @param error  detailed error
     * @throws IOException io error
     */
    private void sendError(HttpServletResponse res, String error) throws IOException {
        LOG.debug("sendError", error);
        PrintWriter pw = new PrintWriter(res.getOutputStream());
        pw.println("\\ERROR = "+error);
        pw.flush();
        //res.sendError(HttpServletResponse.SC_BAD_REQUEST, error);
    }

    private void sendOverflow(HttpServletResponse res, String error) throws IOException {
        LOG.debug("sendOverflow", error);
        PrintWriter pw = new PrintWriter(res.getOutputStream());
        pw.println("\\ERROR = [OVERFLOW] "+error);
        pw.flush();
        //res.sendError(HttpServletResponse.SC_BAD_REQUEST, error);
    }


    private void sendTable(HttpServletResponse res, Map<String,String> paramMap, DataGroupPart dgPart, SearchType searchType, boolean isLevel1) throws IOException {

        PrintWriter printWriter = null;

        if (dgPart.getRowCount()>MAX_RECORDS) {
            sendOverflow(res, "The maximum number of records ("+MAX_RECORDS+") is exceeded)");
        }        

        //dg = IpacTableReader.readIpacTable((new File(dgPart.getTableDef().getSource())), (isLevel1 ? "Level1 Data" : "Level2 Data"));
        DataGroup dg = dgPart.getData();

        DataType filetypeDT = null;
        if (dg != null && dg.size()>0) {
            try {
                filetypeDT = dg.getDataDefintion("filetype");
            } catch (Exception e) {
                sendError(res, "Internal error: unable to find product type");
                return;
            }
        }
        try {
            //int verbosity = (searchType.equals(SearchType.SIAP)) ? 1 : 3;
            int verbosity = 1;
            String verbStr = paramMap.get(PARAM_VERBOSITY);
            if (verbStr != null && verbStr.length() > 0) {
                try {
                    verbosity = Integer.parseInt(verbStr);
                } catch (Exception e) {
                    verbosity = 1;
                }
            }
            // Write the data to outputStream.
            Object value;
            if (dg != null) {
                DataType [] outDataTypes;
                DataType [] inDataTypes;

                DataType [] allDT = dg.getDataDefinitions();
                if (allDT.length < 1) {
                    sendError(res, "No Matches");
                    return;
                }
                
                String [] outColumns = getOriginalColumns(verbosity);                
                if (outColumns == null) {
                    // include all columns in the output
                    outDataTypes = new DataType[allDT.length];
                    inDataTypes = new DataType[allDT.length];
                    int i=0;
                    for (DataType dt : allDT) {
                        inDataTypes[i] = dt;
                        outDataTypes[i] = new DataType(dt.getKeyName(), dt.getDataType());
                        outDataTypes[i].getFormatInfo().setWidth(dt.getFormatInfo().getWidth());
                        i++;
                    }
                } else {
                    DataType dt;
                    outDataTypes = new DataType[outColumns.length];
                    inDataTypes = new DataType[outColumns.length];
                    for (int i=0; i<outColumns.length; i++) {
                        dt = dg.getDataDefintion(outColumns[i]);
                        if (dt == null) {
                            sendError(res, "Required column "+outColumns[i]+" is not available.");
                            return;
                        }
                        inDataTypes[i] = dt;
                        outDataTypes[i] = new DataType(dt.getKeyName(), dt.getDataType());
                        outDataTypes[i].getFormatInfo().setWidth(dt.getFormatInfo().getWidth());
                    }
                }
                List<ExtraField> extraFields = getExtraFields(searchType, isLevel1, verbosity);

                DataGroup outDg = new DataGroup(paramMap.get(PARAM_DATASET), outDataTypes);
                for (ExtraField ef : extraFields) {
                    outDg.addDataDefinition(ef.getDataType());
                }

                printWriter = new PrintWriter(res.getOutputStream());
                IpacTableUtil.writeHeader(printWriter, Arrays.asList(outDg.getDataDefinitions()));

                DataObject newDataObject = new DataObject(outDg);
                for (int r=0; r<dg.size(); r++) {
                    // return proprietary rows with no access url (by spec)
                    //if (dgPart.hasAccess(r)) {
                    DataObject dataObject = dg.get(r);
                    // return only images; no tables
                    if (searchType.equals(SearchType.SIAP)) {
                        String filetype = (dataObject.getDataElement(filetypeDT)).toString();
                        if (!filetype.equals("Image") ) { continue; }
                    }
                    for (int i=0; i<inDataTypes.length; i++) {
                        value = dataObject.getDataElement(inDataTypes[i]);
                        newDataObject.setDataElement(outDataTypes[i], value);
                    }
                    for (ExtraField ef : extraFields) {
                        newDataObject.setDataElement(ef.getDataType(), ef.getMappedValue(dataObject));
                    }
                    IpacTableUtil.writeRow(printWriter, Arrays.asList(outDg.getDataDefinitions()), newDataObject);
                }
            }

        } catch (Exception e) {
            LOG.error(e, "Unable to produce data");
            sendError(res, "Error: "+e.getMessage());
        } finally {
            if (printWriter != null) {
                printWriter.flush();
                printWriter.close();
            }
        }


    }



    private double getConeSearchRadius(double dec, double szRa, double szDec) {
        // using spherical law of cosines
        // http://en.wikipedia.org/wiki/Spherical_law_of_cosines
        double dec1 = dec - szDec/2;
        double dec2 = dec + szDec/2;
        double cosine = Math.sin(dec1*DtoR)*Math.sin(dec2*DtoR)+Math.abs(Math.cos(dec1*DtoR)*Math.cos(dec2*DtoR))*Math.cos(szRa*DtoR);
        if (Math.abs(cosine) > 1.0)
           cosine = cosine/Math.abs(cosine);

        return RtoD*Math.acos(cosine)/2;
    }

    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        httpServletResponse.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    }

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        try {
            processRequest(httpServletRequest, httpServletResponse);
        } catch (Exception e) {
            LOG.debug("GET failed", e.getMessage());
            throw new ServletException(e);
        }
    }

    private String [] getOriginalColumns(int verbosity) {
        if (verbosity < 3) {
            return new String[]{"externalname", "wavelength", "ra", "dec", "naxis1", "naxis2",
                        "cdelt1", "cdelt2", "filesize", "crpix1", "crpix2", "crval1", "crval2", "crota2"};
        } else {
            return null;
        }
    }

    private List<ExtraField> getExtraFields(final SearchType searchType, final boolean isLevel1, int verbosity) {
        List<ExtraField> extraFields = new ArrayList<ExtraField>(10);

        // add access url
        DataType accessUrlDT = new DataType("accessUrl", String.class);
        accessUrlDT.getFormatInfo().setWidth(getAccessUrlWidth(isLevel1, null));
        extraFields.add(new ExtraField(accessUrlDT,
                new FieldValueMapper(){
                    public Object getMappedValue(DataObject row) {
                        String reqkey = row.getDataElement("reqkey").toString().trim();
                        if (HeritageSecurityModule.checkHasAccess(reqkey)) {
                            String idStr = row.getDataElement((isLevel1 ? "bcdid" : "pbcdid")).toString();
                            int id = Integer.parseInt(idStr);
                            return ProductDownload.getUrl(isLevel1, id);
                        } else {
                            return ("NONE");
                        }
                    }
                }));


        if (verbosity >=3 ) {
            // add access url
            DataType accessWithUrlDT = new DataType("accessWithAnc1Url", String.class);
            final List<ProductDownload.Options> options = new ArrayList<ProductDownload.Options>();
            options.add(ProductDownload.Options.anc1);
            accessWithUrlDT.getFormatInfo().setWidth(getAccessUrlWidth(isLevel1, options));
            extraFields.add(new ExtraField(accessWithUrlDT,
                    new FieldValueMapper(){
                        public Object getMappedValue(DataObject row) {
                            String reqkey = row.getDataElement("reqkey").toString().trim();
                            if (HeritageSecurityModule.checkHasAccess(reqkey)) {
                                String idStr = row.getDataElement((isLevel1 ? "bcdid" : "pbcdid")).toString();
                                int id = Integer.parseInt(idStr);
                                return ProductDownload.getUrl(isLevel1, id, options);
                            } else {
                                return ("NONE");
                            }
                        }
                    }));
        }

        // SIAP specific fields
        if (searchType.equals(SearchType.SIAP)) {

            // add data format
            DataType dataFormatDT = new DataType("dataFormat", String.class);
            dataFormatDT.getFormatInfo().setWidth(24);
            extraFields.add(new ExtraField(dataFormatDT,
                    new FieldValueMapper() {
                        public Object getMappedValue(DataObject row) {
                            return(getDataFormat(row.getDataElement("externalname").toString().trim()));
                        }
                    }));


            // add epoch as modified julian date
            DataType mjDateObsDT = new DataType("mjDateObs", Double.class);
            mjDateObsDT.getFormatInfo().setWidth(10);
            mjDateObsDT.getFormatInfo().setDataFormat("%.3f");
            extraFields.add(new ExtraField(mjDateObsDT,
                    new FieldValueMapper(){
                        public Object getMappedValue(DataObject row) {
                            return getMJDateObs((Double)row.getDataElement("epoch"));
                        }
                    }));


            // add min wavelength in meters
            DataType minWLMetersDT = new DataType("minWLMeters", Double.class);
            minWLMetersDT.getFormatInfo().setWidth(11);
            minWLMetersDT.getFormatInfo().setDataFormat("%1.3E");
            extraFields.add(new ExtraField(minWLMetersDT,
                    new FieldValueMapper(){
                        public Object getMappedValue(DataObject row) {
                            // convert microns to meters
                            return (0.000001)*(Double)row.getDataElement("minwavelength");
                        }
                    }));

            // add max wavelength in meters
            DataType maxWLMetersDT = new DataType("maxWLMeters", Double.class);
            maxWLMetersDT.getFormatInfo().setWidth(11);
            maxWLMetersDT.getFormatInfo().setDataFormat("%1.3E");
            extraFields.add(new ExtraField(maxWLMetersDT,
                    new FieldValueMapper(){
                        public Object getMappedValue(DataObject row) {
                            // convert microns to meters
                            return ((0.000001))*(Double)row.getDataElement("maxwavelength");
                        }
                    }));

            // add reference wavelength in meters
            DataType refWLMetersDT = new DataType("refWLMeters", Double.class);
            refWLMetersDT.getFormatInfo().setWidth(11);
            refWLMetersDT.getFormatInfo().setDataFormat("%1.3E");
            extraFields.add(new ExtraField(refWLMetersDT,
                    new FieldValueMapper(){
                        public Object getMappedValue(DataObject row) {
                            // convert microns to meters
                            return ((0.000001))*((Double)row.getDataElement("maxwavelength")+(Double)row.getDataElement("minwavelength"))/2;
                        }
                    }));

            // CD matrix
            //CD1_1 = CDELT1*cos(CROTA2)
            //CD1_2 = -CDELT2*sin(CROTA2)
            //CD2_1 = CDELT1*sin(CROTA2)
            //CD2_2 = CDELT2*cos(CROTA2)

            // CD1_1
            DataType cd11DT = new DataType("cd11", Double.class);
            cd11DT.getFormatInfo().setWidth(11);
            cd11DT.getFormatInfo().setDataFormat("%1.4E");
            extraFields.add(new ExtraField(cd11DT,
                    new FieldValueMapper(){
                        public Object getMappedValue(DataObject row) {
                            return (Double)row.getDataElement("cdelt1")*Math.cos(DtoR*(Double)row.getDataElement("crota2"));
                        }
                    }));

            // CD1_2
            DataType cd12DT = new DataType("cd12", Double.class);
            cd12DT.getFormatInfo().setWidth(11);
            cd12DT.getFormatInfo().setDataFormat("%1.4E");
            extraFields.add(new ExtraField(cd12DT,
                    new FieldValueMapper(){
                        public Object getMappedValue(DataObject row) {
                            return (Double)row.getDataElement("cdelt2")*Math.sin(DtoR*(Double)row.getDataElement("crota2"));
                        }
                    }));

            // CD1_1
            DataType cd21DT = new DataType("cd21", Double.class);
            cd21DT.getFormatInfo().setWidth(11);
            cd21DT.getFormatInfo().setDataFormat("%1.4E");
            extraFields.add(new ExtraField(cd21DT,
                    new FieldValueMapper(){
                        public Object getMappedValue(DataObject row) {
                            return (Double)row.getDataElement("cdelt1")*Math.sin(DtoR*(Double)row.getDataElement("crota2"));
                        }
                    }));

            // CD2_2
            DataType cd22DT = new DataType("cd22", Double.class);
            cd22DT.getFormatInfo().setWidth(11);
            cd22DT.getFormatInfo().setDataFormat("%1.4E");
            extraFields.add(new ExtraField(cd22DT,
                    new FieldValueMapper(){
                        public Object getMappedValue(DataObject row) {
                            return (Double)row.getDataElement("cdelt2")*Math.cos(DtoR*(Double)row.getDataElement("crota2"));
                        }
                    }));            

        }

        return extraFields;
    }


    public int getAccessUrlWidth(boolean isLevel1, List<ProductDownload.Options> options) {
        String sampleUrl = ProductDownload.getUrl(isLevel1, 1, options);
        return sampleUrl.length()+10;
    }

    public String getDataFormat(String name) {
        try {
            if (name.endsWith("fits"))
                return "image/fits";
            else if (name.endsWith(".tbl") || name.endsWith(".txt") || name.endsWith(".log"))
                return "text/plain";
            else if (name.endsWith(".gif"))
                return "image/gif";
            else if (name.endsWith(".jpg"))
                return "image/jpeg";
            else {
                return "application/octet-stream";
            }
        } catch (Exception e) { return ""; }
    }


    /**
     * Get Modified Julian Date from decimal year (epoch after 2000)
     * @param epoch decimal year
     * @return modified julian date
     */
    public double getMJDateObs(double epoch) {
        double MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
        double MJD_J2000 = 51544.0;
        int year = (int) Math.floor( epoch );
        double frac = epoch - year;
        calendar.clear();
        calendar.set( Calendar.YEAR, year );
        long t0 = calendar.getTimeInMillis();
        calendar.set( Calendar.YEAR, year + 1 );
        long t1 = calendar.getTimeInMillis();
        calendar.set( Calendar.YEAR, 2000);
        long t2000 = calendar.getTimeInMillis();
        long t = t0 + Math.round( frac * ( t1 - t0 ) ) - t2000;
        return MJD_J2000 + ((double)t)/MILLIS_IN_DAY;
    }


    private static class ExtraField {
        DataType dataType;
        FieldValueMapper mapper;

        public ExtraField (DataType dataType, FieldValueMapper mapper) {
            this.dataType = dataType;
            this.mapper = mapper;
        }

        DataType getDataType() { return dataType; }
        Object getMappedValue(DataObject row) { return mapper.getMappedValue(row); }
    }

    private static interface FieldValueMapper {
        public Object getMappedValue(DataObject row);
    }
}
