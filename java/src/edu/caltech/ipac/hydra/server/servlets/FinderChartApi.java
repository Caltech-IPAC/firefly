package edu.caltech.ipac.hydra.server.servlets;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.servlets.BaseHttpServlet;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.hydra.server.query.QueryFinderChart;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 /**
 * Date: 9/25/13
 *
 * Finder Chart API implementation based on specification written here:
 * http://irsa.ipac.caltech.edu/applications/FinderChart/docs/finderProgramInterface.html
 *
 * @author loi
 * @version $Id: $
 */
public class FinderChartApi extends BaseHttpServlet {

    private enum Param {mode, locstr, subsetsize("subsize"), thumbnail_size, survey("sources"), orientation,
                        reproject, grid_orig, grid_shrunk, markervis_orig, markervis_shrunk;
                        private final String iname;
                        Param() { this.iname = name();}
                        Param(String iname) {this.iname = iname;}
                    }

    private static final int    MAX_RECORDS = 5000;
    public static final String HTTP_GET = "HTTP-GET";
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
       LOG.debug("Query string", req.getQueryString());
       Map origParamMap = req.getParameterMap();
       Map<String, String> paramMap = new HashMap<String,String>();
       // parameters could be upper or lower case
       for (Object p : origParamMap.keySet()) {
           if (p instanceof String) {
               handleNamesXref(paramMap, (String) p, origParamMap.get(p));
           }
       }

       //ParamMap inspection

       //Prepare IPAC table
       DataGroupPart dgPart;
       try {
           TableServerRequest searchReq = getRequest(paramMap);
           dgPart = (new SearchManager()).getDataGroup(searchReq);

           if (paramMap.containsKey("votable")) {
               //todo: output votable?
               /*String mimeType = "text/xml";
               res.setContentType(mimeType);

               TableMapper tableMapper = new TableMapper();
               tableMapper.getServiceUrl();
               VODataProvider dataProvider = new RemoteDataProvider(tableMapper, paramMap);
               VOTableWriter voWriter = new VOTableWriter(dataProvider);
               voWriter.sendData(new PrintStream(res.getOutputStream()), paramMap);*/
           } else if (paramMap.containsKey("xml")) {
              //todo: output xml?
           } else {
               //IPAC table by default
               sendTable(res, paramMap, dgPart);
           }
       } catch (Exception e) {
           LOG.error(e);

       }
    }

    private void handleNamesXref(Map<String, String> paramMap, String k, Object values) {
        if (k == null) return;

        String v = values == null ? "" : String.valueOf(values);
        if (values != null && values.getClass().isArray()) {
            StringUtils.toString((Object[])values, ",");
        }
        try {
            Param p = Param.valueOf(k.toLowerCase());
            paramMap.put(p.iname, v);
        } catch (Exception e) {}
    }

    private TableServerRequest getRequest(Map<String, String> paramMap) {
        TableServerRequest searchReq = new TableServerRequest(QueryFinderChart.PROC_ID);
        searchReq.setPageSize(Integer.MAX_VALUE);

        paramMap.get(Param.locstr.name());








        for (Map.Entry<String, String> e: paramMap.entrySet()) {
            searchReq.setParam(e.getKey(),e.getValue());



        }
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
                    DataObject dataObject = dg.get(r);
                    for (int i=0; i<inDataTypes.length; i++) {
                        value = dataObject.getDataElement(inDataTypes[i]);
                        newDataObject.setDataElement(outDataTypes[i], value);
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

    private static String [] getOriginalColumns() {
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
