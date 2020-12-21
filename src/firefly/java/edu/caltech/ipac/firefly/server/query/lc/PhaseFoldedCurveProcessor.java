/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.lc;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;

import java.io.File;
import java.io.IOException;

/**
 * Created by zhang on 10/14/15.
 * This class calculates the statistics of a IpacTable Data.
 */

@SearchProcessorImpl(id = "PhaseFoldedProcessor")

public class PhaseFoldedCurveProcessor extends IpacTablePartProcessor {

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

        File phaseFoldedTable = irsaLcHandler.toPhaseFoldedTable(ServerContext.convertToFile(req.getLcSource()), req.getPeriod(), req.getTimeColName());


        return phaseFoldedTable;
    }


}
