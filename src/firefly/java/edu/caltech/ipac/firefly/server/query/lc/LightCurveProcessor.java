/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.lc;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.FailedRequestException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * This class takes care of the LC api call and return result IpacTable Data.
 */

@SearchProcessorImpl(id = "LightCurveProcessor")

public class LightCurveProcessor extends IpacTablePartProcessor {

    private static final String PERIODOGRAM_API_URL = AppProperties.getProperty("periodogram.host", "default_periodogram_host_url");

    // API will return votable, depending on the request, return either peaks or periodogram table, which names are predefined here:
    private static final String PERIODOGRAM_TABLE_NAME = "periodogram_table.tbl";
    private static final String PEAKS_TABLE_NAME = "peaks_table.tbl";
    private final IrsaLightCurveHandler h;

    /**
     * Class handling the API call and returning LC result table
     */
    public LightCurveProcessor() {
        h = new IrsaLightCurveHandler();
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
        String tblName = (tblType != null && tblType.equalsIgnoreCase(LightCurveHandler.RESULT_TABLES_IDX.PERIODOGRAM.name()))
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

        if (tblName.equalsIgnoreCase(PERIODOGRAM_TABLE_NAME)) {
            return h.getPeriodogramTable(req);
        } else if (tblName.equalsIgnoreCase(PEAKS_TABLE_NAME)) {
            return h.getPeaksTable(req);
        } else {
            throw new FailedRequestException("Unable to deal with the request table name " + tblName);
        }
    }
}
