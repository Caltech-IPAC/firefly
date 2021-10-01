/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.lc;


import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.io.IpacTableException;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.VoTableReader;
import java.io.*;
import static edu.caltech.ipac.firefly.data.TableServerRequest.INCL_COLUMNS;


/**
 * .
 * Should handle the LC transformations to get files out of the API result VOtable xml
 * Dev API can be found here:
 * http://irsa.ipac.caltech.edu/applications/periodogram/Periodogram.html
 * <p>
 * TODO update api url and doc.
 *
 * @author ejoliet
 * @see PeriodogramAPIRequest
 */
public class IrsaLightCurveHandler implements LightCurveHandler {

    private final String[] apiKey;
    public String rootApiUrl;
    //public HttpServiceInput httpInputs; //this is to be submitted to the external api
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    /**
     * IRSA lc handler constructor, sets the root api url to be used
     */
    public IrsaLightCurveHandler() {

        rootApiUrl = AppProperties.getProperty("irsa.gator.service.periodogram.url", "https://irsa.ipac.caltech.edu/cgi-bin/periodogram/nph-periodogram_api");
        apiKey = AppProperties.getArrayProperties("irsa.gator.service.periodogram.keys", "\\s+", "x y");// At least x,y , rest are optional
    }

    public DataGroup getPeriodogramTable(PeriodogramAPIRequest request) {

        return getDataFromAPI(request, RESULT_TABLES_IDX.PERIODOGRAM);
    }


    /**
     * @return peaks table (default: 50 rows)
     */
    public DataGroup getPeaksTable(PeriodogramAPIRequest request) {

        return getDataFromAPI(request, RESULT_TABLES_IDX.PEAKS);
    }

    /**
     * Return a phase folded curve form original light-curve
     *
     * @param tbl         orginal lc table
     * @param period      period for phase folding the lc curve
     * @param timeColName
     * @return phase folded curve (x2 original input table 0,2 phase)
     */
    public File toPhaseFoldedTable(File tbl, float period, String timeColName) {
        //get raw lcTable and phase fold on time/period
        //for now, return same table.
        File tempFile = null;

        try {
            DataGroup dg = TableUtil.readAnyFormat(tbl);
            PhaseFoldedLightCurve plc = new PhaseFoldedLightCurve();
            plc.addPhaseCol(dg, period, timeColName);
            tempFile = createPhaseFoldedTempFile();
            IpacTableWriter.save(tempFile, dg);
        }
        catch (IOException  | IpacTableException e) {
            LOG.error(e);
        }

        return tempFile;
    }

    protected File createPhaseFoldedTempFile() throws IOException {
        return File.createTempFile("phase-folded", ".tbl", QueryUtil.getTempDir());
    }

    protected DataGroup extractTblFrom(File votableResult, RESULT_TABLES_IDX resultTable) {
        try {
            DataGroup[] dataGroups = VoTableReader.voToDataGroups(votableResult.getAbsolutePath());

            return dataGroups[resultTable.ordinal()];
        }
        catch (IOException e) {
            LOG.error(e);
        }
        return null;
    }

    protected DataGroup getDataFromAPI(PeriodogramAPIRequest request, RESULT_TABLES_IDX resultTable) {

        try {
            File apiResult = apiDownload(request);

            return extractTblFrom(apiResult, resultTable);

        }
        catch (IOException e) {
             LOG.error(e);
        }
        return null;
    }

    public TableServerRequest getTableServerRequest(PeriodogramAPIRequest request) {

          TableServerRequest sreq = QueryUtil.convertToServerRequest(request.getLcSource());
          sreq.setPageSize(Integer.MAX_VALUE);               // ensure you're getting the full table
          sreq.removeParam(INCL_COLUMNS);
          return sreq;
    }
    /**
     * This method will get the "origin_table"'s parameter value from request.  The value can be a json string passing
     * from the TimeSeries UI or passed from Gator.  This json string is converted to a TableServerRequest.
     * The data is found and saved to a file.  The file name is lcInputTable.tbl.  After testing, the same file can be
     * overwritten.  Thus, there is no need to have different file name.  After the file is saved, set the LC_FILE key
     * parameter in the request to the "lcInputTable.tbl".
     *
     * Periodogram and peak table calculations are all based on this input table.  There is no need to handle the saving
     * in other places.
     *
     * @param request
     */
    void saveInputToTable(PeriodogramAPIRequest request){

        TableServerRequest sreq  = getTableServerRequest(request);

        try {
            File file = File.createTempFile("lcInputTable", ".tbl", QueryUtil.getTempDir(request));
            OutputStream out = new FileOutputStream(file, false);
            new SearchManager().save(out, sreq, TableUtil.Format.IPACTABLE);
            out.close();
            request.setLcSource(file.getAbsolutePath());
        }
        catch (IOException | DataAccessException e) {
            LOG.error(e);
        }

    }
    protected File apiDownload(PeriodogramAPIRequest request) throws IOException{
       File apiResultTempFile = File.createTempFile("lc-api-result-", ".xml", QueryUtil.getTempDir(request));

        /**
         * @see LightCurveProcessor#computePeriodogram(PeriodogramAPIRequest, java.lang.String)
         */
        //save the the input to a table first and then use it to calculate the peak table or periodogram table
        saveInputToTable(request);

        //do a HTTP post
        HttpServiceInput httpInputs = getHttpInput(request).setRequestUrl(rootApiUrl);
        BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(apiResultTempFile), 10240);
        HttpServices.postData(httpInputs, writer);
        writer.close();

        return apiResultTempFile;
    }



    /**
     * create the HttpServiceInput to call Http's post method
     * @param request
     * @return
     */
    public HttpServiceInput getHttpInput(PeriodogramAPIRequest request) {
        HttpServiceInput httpInputs = new HttpServiceInput();

        String src = ServerContext.convertToFile(request.getLcSource()).getAbsolutePath();
        httpInputs.setFile("upload", new File(src));

        for (String key : apiKey) {
            String val = request.getParam(key);
            if (val != null) {
                httpInputs.setParam(key, val);
            }
        }
       // httpInputs.setParam("peaks", "" + request.getNumberPeaks());
        return httpInputs;
    }



}
