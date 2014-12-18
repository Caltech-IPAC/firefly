package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.IpacTableUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: tlau
 * Date: 3/12/13
 * Time: 5:11 PM
 * To change this template use File | Settings | File Templates.
 * testing url:

http://localhost:8080/applications/finderchart/servlet/ApiService?RA=148.88822&DEC=69.06529&SIZE=0.5&thumbnail_size=medium&sources=DSS,SDSS,twomass,WISE&dss_bands=poss1_blue,poss1_red,poss2ukstu_blue,poss2ukstu_red,poss2ukstu_ir&SDSS_bands=u,g,r,i,z&twomass_bands=j,h,k&wise_bands=1,2,3,4
http://localhost:8080/applications/finderchart/servlet/ApiService?RA=148.88822&DEC=69.06529&SIZE=0.5&thumbnail_size=medium&sources=DSS,SDSS,twomass,IRIS,WISE&dss_bands=poss1_blue,poss1_red,poss2ukstu_blue,poss2ukstu_red,poss2ukstu_ir&SDSS_bands=u,g,r,i,z&twomass_bands=j,h,k&iras_bands=12,25,60,100&wise_bands=1,2,3,4

 */
public class ApiService extends BaseHttpServlet {

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
