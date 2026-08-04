/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.core.background.JobInfo;
import edu.caltech.ipac.firefly.core.background.JobManager;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.DbDataIngestor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.util.FormatUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotEmpty;
import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.util.StringUtils.isEmpty;


/**
 * Abstract Processor to ingest data from a file into the database.  Opposite of EmbeddedDbToFileProcessor where data is imported from a DataGroup into the database.
 * It does not register itself as a SearchProcessorImpl; subclasses should do so.
 *
 * @author loi
 * $Id: DbFromFileProcessor.java,v 1.6 2012/09/25 21:13:23 loi Exp $
 */
public abstract class DbFromFileProcessor extends EmbeddedDbProcessor {
    public static final String FORMAT = "format";
    public static final String TBL_INDEX = TableServerRequest.TBL_INDEX;

    @Override
    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        // if getDataFile returns null, then subclasses should override fetchDataGroup to provide the data
        return null;
    }

    /**
     * Get the data file to ingest into the database.
     * This allows loading data into the database without loading the entire data into memory first.
     * @param req the table request used to get the data file
     * @return the data file to ingest
     * @throws DataAccessException if there is an error getting the data file
     */
    public abstract File getDataFile(TableServerRequest req) throws DataAccessException;

    @Override
    protected FileInfo ingestDataIntoDb(TableServerRequest req, DbAdapter dbAdapter) throws DataAccessException {
        try {
            dbAdapter.initDbFile();
            int tblIdx = req.getIntParam(TBL_INDEX, 0);
            String fmt = req.getParam(FORMAT);
            FormatUtil.Format format = isEmpty(fmt) ? null : FormatUtil.Format.valueOf(fmt);

            File dataFile = ifNotNull(getCachedFile(req)).getOrElse(getDataFile(req));
            if (dataFile != null) {
                return DbDataIngestor.ingestData(req, dbAdapter, (dg) -> applyExtraMeta(dg, req), dataFile, tblIdx, format);
            } else {
                return dbAdapter.ingestData(makeDgSupplier(req, () -> getOrFetchDataGroup(req)), dbAdapter.getDataTable());
            }

        } catch (IOException e) {
            Logger.getLogger().error(e,"Failed to ingest data into the database:" + req.getRequestId());
            throw new DataAccessException(e);
        }
    }

    public File getCachedFile(TableServerRequest req) throws DataAccessException {
        String jobId = req.getJobId();
        if (isEmpty(jobId)) return null;

        // a previously submitted job with local file result
        List<JobInfo.Result> results = ifNotNull(JobManager.getJobInfo(req.getJobId()))
                .get(JobInfo::getResults);
        if (results != null && results.size() == 1) {
            File rval = ifNotEmpty(results.getFirst().href())
                            .get(ServerContext::convertToFile);
            if (rval != null && rval.isFile() && rval.canRead()) {       // if not a readable file, then ignore.
                return rval;
            }
        }
        return null;
    }

}

