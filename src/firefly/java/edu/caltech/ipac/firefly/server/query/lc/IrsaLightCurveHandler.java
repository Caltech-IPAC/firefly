/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.lc;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.io.IpacTableException;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.firefly.server.util.multipart.MultiPartPostBuilder;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static edu.caltech.ipac.firefly.server.query.lc.PeriodogramAPIRequest.LC_FILE;
import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;


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
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    /**
     * IRSA lc handler constructor, sets the root api url to be used
     */
    public IrsaLightCurveHandler() {

        rootApiUrl = AppProperties.getProperty("irsa.gator.service.periodogram.url", "https://irsa.ipac.caltech.edu/cgi-bin/periodogram/nph-periodogram_api");
        apiKey = AppProperties.getArrayProperties("irsa.gator.service.periodogram.keys", "\\s+", "x y");// At least x,y , rest are optional
    }

    public DataGroup getPeriodogramTable(PeriodogramAPIRequest request) {

        // handle case when 'getLcSource' is a JSON TableServerRequest
        applyIfNotEmpty(getSourceFileFromJsonReqest(request), f -> request.setParam(LC_FILE, f.getPath()));


        return getDataFromAPI(request, RESULT_TABLES_IDX.PERIODOGRAM);
    }


    /**
     * @return peaks table (default: 50 rows)
     */
    public DataGroup getPeaksTable(PeriodogramAPIRequest request) {

        // handle case when 'getLcSource' is a JSON TableServerRequest
        applyIfNotEmpty(getSourceFileFromJsonReqest(request), f -> request.setParam(LC_FILE, f.getPath()));

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

    protected DataGroup extractTblFrom(File votableResult, RESULT_TABLES_IDX resultTable) {
        try {
            DataGroup[] dataGroups = VoTableReader.voToDataGroups(votableResult.getAbsolutePath());

            return dataGroups[resultTable.ordinal()];
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected DataGroup getDataFromAPI(PeriodogramAPIRequest request, RESULT_TABLES_IDX resultTable) {
        try {
            File apiResult = apiDownlaod(request);

            return extractTblFrom(apiResult, resultTable);

        } catch (IOException | FailedRequestException e) {
            e.printStackTrace();
        }
        return null;
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

    File getSourceFileFromJsonReqest(TableServerRequest request) {
        TableServerRequest tsr = QueryUtil.convertToServerRequest(request.getParam(LC_FILE));
        if (!tsr.getRequestId().equals(ServerRequest.ID_NOT_DEFINED)) {
            try {
                FileInfo fi = new SearchManager().getFileInfo(tsr);
                return fi.getFile();
            } catch (DataAccessException e) {
                LOG.error(e);
            }
        }
        return null;
    }

}
