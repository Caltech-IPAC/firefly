/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by zhang on 10/14/15.
 * This class calculates the statistics of a IpacTable Data.
 */

@SearchProcessorImpl(id = "PhaseFoldedProcessor")

public class PhaseFoldedCurveProcessor extends IpacTablePartProcessor {

    private static final Logger.LoggerImpl _log = Logger.getLogger();

    private static final String FOLDED_TABLE_NAME = "folded_table.tbl";
    private final IrsaLightCurveHandler irsaLcHandler;

    /**
     * Class handling the API call and returning LC result table
     */
    public PhaseFoldedCurveProcessor() {
        irsaLcHandler = new IrsaLightCurveHandler();
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
        String tblName = (tblType != null && tblType.equalsIgnoreCase(PeriodogramAPIRequest.FOLDED_TABLE))
                ? FOLDED_TABLE_NAME : "ERROR.tbl";

        float period = req.getFloatParam("period");
        String lcTable = req.getParam("original_table");


        //Votable containing 2 tables:periodogram and peaks
        File resTable = null;
        try {
            resTable = phaseFoldedTable(req);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return resTable;
    }

    protected File phaseFoldedTable(PeriodogramAPIRequest req) throws InterruptedException {
        //Fake building phase folded
        Thread.sleep(5000);


        File phaseFoldedTable = irsaLcHandler.toPhaseFoldedTable(getSourceFile(req.getLcSource(), req), req.getPeriod());


        return phaseFoldedTable;
    }

    private File getSourceFile(String source, TableServerRequest request) {
        File inf = null;
        try {
            URL url = makeUrl(source);
            if (url == null) {
                inf = ServerContext.convertToFile(source);
            } else {
                HttpURLConnection conn = (HttpURLConnection) URLDownload.makeConnection(url);
                int rcode = conn.getResponseCode();
                if (rcode >= 200 && rcode < 400) {
                    String sfname = URLDownload.getSugestedFileName(conn);
                    if (sfname == null) {
                        sfname = url.getPath();
                    }
                    String ext = sfname == null ? null : FileUtil.getExtension(sfname);
                    ext = StringUtils.isEmpty(ext) ? ".ul" : "." + ext;
                    inf = createFile(request, ext);
                    URLDownload.getDataToFile(conn, inf, null, false, true, true, Long.MAX_VALUE);
                }
            }
        } catch (Exception ex) {
            inf = null;
        }
        if (inf != null && inf.canRead()) {
            return inf;
        }

        return null;
    }

    private URL makeUrl(String source) {
        try {
            return new URL(source);
        } catch (MalformedURLException e) {
            return null;
        }
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

}
