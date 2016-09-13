/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.lc;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.VoTableUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import org.apache.commons.lang.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


/**
 * .
 * Should handle the LC transformations to get files out of the API result VOtable xml
 *
 * @author ejoliet
 * @see PeriodogramAPIRequest
 */
public class IrsaLightCurveHandler implements LightCurveHandler {


    /**
     * TODO Update with correct API root url
     */
    public final String rootApiUrl = "http://irsa.ipac.caltech.edu/periodogram";

    public File getPeriodogramTable(PeriodogramAPIRequest request) {

        return ipacTableFromAPI(request, RESULT_TABLES_IDX.PERIODOGRAM);

    }


    /**
     * @return peaks table (default: 50 rows)
     */
    public File getPeaksTable(PeriodogramAPIRequest request) {

        return ipacTableFromAPI(request, RESULT_TABLES_IDX.PEAKS);
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
            DataGroup dg = DataGroupReader.readAnyFormat(tbl);
            PhaseFoldedLightCurve plc = new PhaseFoldedLightCurve();
            plc.addPhaseCol(dg, period, timeColName);
            tempFile = createPhaseFoldedTempFile();
            IpacTableWriter.save(tempFile, dg);
        } catch (IpacTableException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tempFile;
    }

    protected File createPhaseFoldedTempFile() throws IOException {
        return File.createTempFile("phase-folded", ".tbl", ServerContext.getTempWorkDir());
    }

    /**
     * Return the API URL to be called to get VOTable from Nexsci, which will contain 2 tables: periodogram and peaks tables.
     * <p>
     * TODO need to be implemented
     *
     * @param request object to contain the required paramter to make the API call
     * @return the URL object
     * @throws MalformedURLException
     * @see LightCurveProcessor#computePeriodogram(PeriodogramAPIRequest, java.lang.String)
     * @see IrsaLightCurveHandler#buildUrl(PeriodogramAPIRequest)
     */
    protected URL buildUrl(PeriodogramAPIRequest request) throws MalformedURLException {
        //loop other request and append to rootApiUrl
        throw new NotImplementedException("Not yet implemented");
    }

    protected File extractTblFrom(File votableResult, RESULT_TABLES_IDX resultTable) {
        File resultTblFile = null;
        try {
            resultTblFile = makeResultTempFile(resultTable);
            DataGroup[] dataGroups = VoTableUtil.voToDataGroups(votableResult.getAbsolutePath());

            IpacTableWriter.save(resultTblFile, dataGroups[resultTable.ordinal()]);
            return resultTblFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultTblFile;
    }

    protected File ipacTableFromAPI(PeriodogramAPIRequest request, RESULT_TABLES_IDX resultTable) {
        File tempFile = null;
        try {
            /**
             * @see LightCurveProcessor#computePeriodogram(PeriodogramAPIRequest, java.lang.String)
             */
            URL url = buildUrl(request);

            File apiResult = apiDownlaod(url);

            tempFile = extractTblFrom(apiResult, resultTable);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (FailedRequestException e) {
            e.printStackTrace();
        }
        return tempFile;
    }

    protected File apiDownlaod(URL url) throws IOException, FailedRequestException {

        File apiResultTempFile = makeApiResultTempFile();

        URLConnection aconn = URLDownload.makeConnection(url);
        aconn.setRequestProperty("Accept", "*/*");
        URLDownload.getDataToFile(aconn, apiResultTempFile); //TODO Get from cache

        return apiResultTempFile;
    }


    protected File makeResultTempFile(RESULT_TABLES_IDX resultTable) throws IOException {
        String prefix = "error";
        switch (resultTable) {
            case PERIODOGRAM:
                prefix = "periodogram-";
                break;
            case PEAKS:
                prefix = "peaks-";
                break;
        }

        return File.createTempFile(prefix, ".tbl", ServerContext.getTempWorkDir());
    }

    protected File makeApiResultTempFile() throws IOException {
        return File.createTempFile("lc-api-result-", ".xml", ServerContext.getTempWorkDir());
    }
}
