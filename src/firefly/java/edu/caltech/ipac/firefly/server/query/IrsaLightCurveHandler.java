/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.VoTableUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;


/**
 * .
 * Should handle the LC transformations to get files out of the API result VOtable xml
 *
 * @author ejoliet
 * @see PeriodogramAPIRequest
 */
public class IrsaLightCurveHandler extends NexsciLcApiHandler {

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
     * @param tbl    orginal lc table
     * @param period period for phase folding the lc curve
     * @return phase folded curve (x2 original input table 0,2 phase)
     */
    public File toPhaseFoldedTable(File tbl, float period) {
        //get raw lcTable and phase fold on time/period
        //for now, return same table.
        //TODO change it with the implementation DM-7165
        return tbl;
    }


    protected File extractTblFrom(File votableResult, NexsciLcApiHandler.RESULT_TABLES_IDX resultTable) {
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
             * @see edu.caltech.ipac.firefly.server.query.LightCurveProcessor#computePeriodogram(edu.caltech.ipac.firefly.server.query.PeriodogramAPIRequest, java.lang.String)
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
