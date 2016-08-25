/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.FailedRequestException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class takes care of the LC api call and return result IpacTable Data.
 */

@SearchProcessorImpl(id = "LightCurveProcessor")

public class LightCurveProcessor extends IpacTablePartProcessor {

    private static final String PERIODOGRAM_API_URL = AppProperties.getProperty("periodogram.host", "default_periodogram_host_url");

    private static final Logger.LoggerImpl _log = Logger.getLogger();

    // API will return votable, depending on the request, return either peaks or periodogram table, which names are predefined here:
    private static final String PERIODOGRAM_TABLE_NAME = "periodogram_table.tbl";
    private static final String PEAKS_TABLE_NAME = "peaks_table.tbl";

    /**
     * Class handling the API call and returning LC result table
     */
    public LightCurveProcessor() {

        // TODO enable the nadler in constructor when the NexsciHandler is ready
        //        LightCurveHandler h = new IrsaLightCurveHandler() {
    }

    /**
     * This method is defined as an abstract in the IpacTablePartProcessor and it is implemented here.
     * The TableServerRequest is passed here and processed.  Only when the "searchRequest" is set, the request
     * is processed.
     *
     * @return File with statistics on a table
     * @throws IOException
     * @throws DataAccessException
     */
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        PeriodogramAPIRequest req = QueryUtil.assureType(PeriodogramAPIRequest.class, request);
        String tblType = req.getParam(PeriodogramAPIRequest.TABLE_NAME);
        String tblName = (tblType != null && tblType.equalsIgnoreCase(PeriodogramAPIRequest.RESULT_TABLE))
                ? PERIODOGRAM_TABLE_NAME : PEAKS_TABLE_NAME;

        //In order to get any of those tables, computing the periodogram need to happen:
        // Result is a Votable containing 2 tables:periodogram and peaks
        File resTable = null;
        try {
            resTable = computePeriodogram(req, tblName);
        } catch (FailedRequestException e) {
            e.printStackTrace();
        }

//        if (tblType.equalsIgnoreCase(PeriodogramAPIRequest.PEAKS_TABLE)) {
//
//        }

        return resTable;
    }

    /**
     * From the request, get the file and the algorithm to compute the peridogram by calling external API
     *
     * @param req     request
     * @param tblName table name to distinguish them
     * @return table file result either peaks por periodogram
     * @throws FailedRequestException
     */
    public File computePeriodogram(PeriodogramAPIRequest req, String tblName) throws FailedRequestException {

        //Fake call API, parse VOTable result. See for example QueryMOS
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //TODO this is used with overwritten method. Once API known, remove and use the handler directly
        LightCurveHandler h = new IrsaLightCurveHandler() {

            /**
             * For testing purposes returned periodogram from here:
             * PeriodogramAPIRequest.RESULT_TABLE = "http://web.ipac.caltech.edu/staff/ejoliet/demo/vo-nexsci-result-sample.xml"
             * TODO remove after implementing NexsciHandler
             * @param req
             * @return url api
             * @throws MalformedURLException
             */
            @Override
            protected URL buildUrl(PeriodogramAPIRequest req) throws MalformedURLException {
                /**
                 * For now just download the file from the url from req.getResultTable()
                 * and stream it out
                 */
                String SAMPLE_URL = req.getResultTable();
                return new URL(SAMPLE_URL);
            }
        };

//        LightCurveHandler h = new IrsaLightCurveHandler();
        if (tblName.equalsIgnoreCase(PERIODOGRAM_TABLE_NAME)) {
            return h.getPeriodogramTable(req);
        } else if (tblName.equalsIgnoreCase(PEAKS_TABLE_NAME)) {
            return h.getPeaksTable(req);
        } else {
            throw new FailedRequestException("Unable to deal with the request table name " + tblName);
        }
    }

    private static File makeFileName(PeriodogramAPIRequest req) throws IOException {
        return File.createTempFile("lc-result", ".xml", ServerContext.getPermWorkDir());
    }

    private URL createURL(PeriodogramAPIRequest req) throws EndUserException, IOException {
        PeriodogramAPIRequest request = (PeriodogramAPIRequest) req.cloneRequest();
        String url = req.getUrl();
        if (url == null || url.length() < 5) {
            url = PERIODOGRAM_API_URL;
        }
        String paramStr = buildParamFrom(request);
        if (paramStr.startsWith("&")) {
            paramStr = paramStr.substring(1);
        }
        url += "?" + paramStr;

        return new URL(url);
    }

    private String buildParamFrom(PeriodogramAPIRequest request) {
        String outputMode = request.getParam(PeriodogramAPIRequest.OUTPUT_MODE);
        if (StringUtils.isEmpty(outputMode)) {
            outputMode = "VOTable";
        }
        return "min_period=0&n_peaks=50";
    }

    protected class PeriodSample implements Period {

        @Override
        public float getMin() {
            return 0;
        }

        @Override
        public float getMax() {
            return 10;
        }

        @Override
        public float getPeakValue() {
            return 2;
        }
    }

    class LombScargle implements Periodogram {


        @Override
        public AlgorithmDefinition getAlgoDef() {
            return AlgorithmDefinition.LS;
        }

        @Override
        public int getNPeaks() {
            return 50;
        }

        @Override
        public Period getPeriod() {
            return new PeriodSample();
        }

        @Override
        public double[] getAlgoValues() {
            return new double[0];
        }

        @Override
        public StepMethod getStepMethod(StepMethod.STEPMETHOD_NAME sName) {
            return new FixedPeriodMethod(0.1f);
        }
    }

    class FixedPeriodMethod implements StepMethod {

        private final float val;

        FixedPeriodMethod(float step) {
            this.val = step;
        }

        @Override
        public String getName() {
            return STEPMETHOD_NAME.FIXED_PERIOD.name();
        }

        @Override
        public float getValue() {
            return val;
        }
    }
}
