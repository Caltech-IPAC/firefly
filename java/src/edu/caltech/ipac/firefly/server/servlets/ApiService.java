package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.IpacTableUtil;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: tlau
 * Date: 3/12/13
 * Time: 5:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApiService extends BaseHttpServlet {

    private static final String PARAM_VERBOSITY = "VERB";
    private static final int    MAX_RECORDS = 5000;
    private static final String ID = "FinderChartQuery";
    public static final String HTTP_GET = "HTTP-GET";
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
       LOG.debug("Query string", req.getQueryString());
       Map origParamMap = req.getParameterMap();
       Map<String, String> paramMap = new HashMap<String,String>();
       // parameters could be upper or lower case
       for (Object p : origParamMap.keySet()) {
           if (p instanceof String) {
               paramMap.put((String)p, (((String[])origParamMap.get(p))[0]).trim());
           }
       }

       //ParamMap inspection

       //Prepare IPAC table
       DataGroupPart dgPart;
       try {
           TableServerRequest searchReq = getRequest(paramMap);
           dgPart = (new SearchManager()).getDataGroup(searchReq);
           sendTable(res, paramMap, dgPart);
       } catch (Exception e) {
           LOG.error(e);

       }
    }

    private TableServerRequest getRequest(Map<String, String> paramMap) {
        TableServerRequest searchReq = new TableServerRequest(ID);
        for (Map.Entry<String, String> e: paramMap.entrySet()) {
            searchReq.setParam(e.getKey(),e.getValue());
        }
        searchReq.setParam(HTTP_GET);
        searchReq.setPageSize(Integer.MAX_VALUE);
        return searchReq;
    }

    private void sendTable(HttpServletResponse res, Map<String,String> paramMap, DataGroupPart dgPart)
            throws IOException {
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

                String [] outColumns = getOriginalColumns();
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


                DataGroup outDg = new DataGroup("", outDataTypes);

                printWriter = new PrintWriter(res.getOutputStream());
                IpacTableUtil.writeHeader(printWriter, Arrays.asList(outDg.getDataDefinitions()));

                DataObject newDataObject = new DataObject(outDg);
                for (int r=0; r<dg.size(); r++) {
                    // return proprietary rows with no access url (by spec)
                    //if (dgPart.hasAccess(r)) {
                    DataObject dataObject = dg.get(r);
                    // return only images; no tables
                    /*if (searchType.equals(SearchType.SIAP)) {
                        String filetype = (dataObject.getDataElement(filetypeDT)).toString();
                        if (!filetype.equals("Image") ) { continue; }
                    }*/
                    for (int i=0; i<inDataTypes.length; i++) {
                        value = dataObject.getDataElement(inDataTypes[i]);
                        newDataObject.setDataElement(outDataTypes[i], value);
                    }
                    /*for (ExtraField ef : extraFields) {
                        newDataObject.setDataElement(ef.getDataType(), ef.getMappedValue(dataObject));
                    }*/
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

    public static String getAccessURL(Double ra, Double dec, Float size, String source, String band, String mode) {
/**
  * http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=163.6136&DEC=-11.784&SIZE=0.5&thumbnail_size=medium&sources=DSS,SDSS,twomass,WISE&dss_bands=poss1_blue,poss1_red,poss2ukstu_blue,poss2ukstu_red,poss2ukstu_ir&SDSS_bands=u,g,r,i,z&twomass_bands=j,h,k&wise_bands=1,2,3,4&file_type=fits
  * http://localhost:8080/applications/finderchart/servlet/ProductDownload?query=FinderChartQuery&download=FinderChartDownload&RA=163.6136&DEC=-11.784&SIZE=0.5&thumbnail_size=medium&sources=twomass&twomass_bands=j&file_type=fits
  */
        String url = ServerContext.getRequestOwner().getBaseUrl()+"servlet/ProductDownload?"+
                    "query=FinderChartQuery&download=FinderChartDownload";
        String thumbnailSize;

        if (mode.equals("jpgurl")) {
            thumbnailSize = "large";
        } else if (mode.equals("shrunkjpgurl")) {
            thumbnailSize = "small";
        } else {
            thumbnailSize = "medium";
        }
        url += "&RA="+ra;
        url += "&DEC="+dec;
        url += "&SIZE="+size;
        url += "&thumbnail_size="+thumbnailSize;
        url += "&sources="+source;
        if (source.equals("twomass"))
            url += "&twomass_bands"+band;
        else if (source.equals("DSS"))
            url += "&dss_bands="+band;
        else if (source.equals("WISE"))
            url += "&wise_bands="+band;
        else if (source.equals("SDSS"))
            url += "&SDSS_bands="+band;
        else if (source.equals("IRIS"))
            url += "&iras_bands"+band;
        url += "&mode="+mode;
        return url;
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

    private String [] getOriginalColumns() {
        return new String[] {"ra","dec","externalname","wavelength","naxis1","naxis2","accessUrl", "accessWithAnc1Url",
                "fitsurl", "jpgurl", "shrunkjpgurl"};
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
