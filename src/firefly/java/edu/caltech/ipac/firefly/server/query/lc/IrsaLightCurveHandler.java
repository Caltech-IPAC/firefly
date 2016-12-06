/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.lc;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.multipart.MultiPartPostBuilder;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.VoTableUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


/**
 * .
 * Should handle the LC transformations to get files out of the API result VOtable xml
 * Dev API can be found here:
 * http://bacchus.ipac.caltech.edu:9027/applications/periodogram/Periodogram.html
 * <p>
 * TODO update api url and doc.
 *
 * @author ejoliet
 * @see PeriodogramAPIRequest
 */
public class IrsaLightCurveHandler implements LightCurveHandler {

    private final String[] apiKey;
    public String rootApiUrl;
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    /**
     * IRSA lc handler constructor, sets the root api url to be used
     */
    public IrsaLightCurveHandler() {

        rootApiUrl = AppProperties.getProperty("irsa.gator.service.periodogram.url", "http://bacchus.ipac.caltech.edu:9027/cgi-bin/periodogram/nph-periodogram_api");
        apiKey = AppProperties.getArrayProperties("irsa.gator.service.periodogram.keys", "\\s+", "x y");// At least x,y , rest are optional
    }

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
            File apiResult = apiDownlaod(request);

            tempFile = extractTblFrom(apiResult, resultTable);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (FailedRequestException e) {
            e.printStackTrace();
        }
        return tempFile;
    }

    protected File apiDownlaod(PeriodogramAPIRequest request) throws IOException, FailedRequestException {
        File apiResultTempFile = makeApiResultTempFile();
        /**
         * @see LightCurveProcessor#computePeriodogram(PeriodogramAPIRequest, java.lang.String)
         */
        if (isPost(request)) {
            MultiPartPostBuilder _postBuilder = new MultiPartPostBuilder(rootApiUrl);
            insertPostParams(request, _postBuilder);
            BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(apiResultTempFile), 10240);
            _postBuilder.post(writer);
            writer.close();

        } else {
            // Col definition time, data
            String tmpUrl = rootApiUrl;

            tmpUrl += "?" + getSourceFile(request.getLcSource()) + "&";
            int i = 0;
            for (String key : apiKey) {
                String val = request.getParam(key);
                if (val != null) {
                    tmpUrl += key + "=" + val;
                }
                if (i < apiKey.length-1) {
                    tmpUrl += "&";
                }
                i++;
            }

            URLConnection aconn = URLDownload.makeConnection(new URL(tmpUrl));
            aconn.setRequestProperty("Accept", "*/*");
            URLDownload.getDataToFile(aconn, apiResultTempFile);
        }
        return apiResultTempFile;
    }

    boolean isPost(PeriodogramAPIRequest url) {
        return url.getLcSource().indexOf("http") < 0;
    }

    void insertPostParams(PeriodogramAPIRequest request, MultiPartPostBuilder posBuilder) {
        //        rootApiUrl+= "alg=ls"+"&"; //Optional
        // Col definition time, data
        String src = getSourceFile(request.getLcSource());
        posBuilder.addFile("upload", new File(src));

        for (String key : apiKey) {
            String val = request.getParam(key);
            if (val != null) {
                posBuilder.addParam(key, val);
            }
        }
//        posBuilder.addParam("x", request.getTimeColName());
//        posBuilder.addParam("y", request.getDataColName());
//        posBuilder.addParam("alg", request.getAlgoName());
//        posBuilder.addParam("peaks", "" + request.getNumberPeaks());
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

    private String getSourceFile(String source) {
        String inf = null;
        URL url = null;
        try {
            url = new URL(source);
            inf = "input=" + url.toString();
        } catch (MalformedURLException e) {
            inf = ServerContext.convertToFile(source).getAbsolutePath();
        }
        return inf;
    }
}
