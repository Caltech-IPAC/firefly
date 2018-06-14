/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.table.io.IpacTableException;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class wraps an IpacTablePartProcessor to adapt to the EmbeddedDbProcessor interface
 * It will take the original ipac table then create a datbase from it for future interactions
 */
public class EmbeddedDbProcessorWrapper extends EmbeddedDbProcessor {
    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    private IpacTablePartProcessor processor;

    public EmbeddedDbProcessorWrapper(IpacTablePartProcessor processor) {
        this.processor = processor;
    }


    public FileInfo ingestDataIntoDb(TableServerRequest treq, File dbFile) throws DataAccessException {
        try {
            DbAdapter dbAdapter = DbAdapter.getAdapter(treq);

            TableServerRequest nreq = (TableServerRequest) treq.cloneRequest();
            StopWatch.getInstance().start("getBaseData: " + treq.getRequestId());
            File dataFile = processor.loadDataFile(nreq);       // this should fetch the data directly without any caching, sorting, filtering, etc.
            DataGroup dg = IpacTableReader.read(dataFile);
            StopWatch.getInstance().stop("getBaseData: " + treq.getRequestId()).printLog("getBaseData: " + treq.getRequestId());

            setupMeta(dg, treq);

            FileInfo finfo = EmbeddedDbUtil.ingestDataGroup(dbFile, dg, dbAdapter, "data");
            return finfo;
        } catch (DataAccessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new DataAccessException(ex);
        }
    }

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        // no need to do it again.. already did it in createDbFile
    }

    public void onComplete(ServerRequest request, DataGroupPart results) throws DataAccessException {
        processor.onComplete(request, results);
    }

    private void setupMeta(DataGroup dg, ServerRequest req) {
        // merge meta into datagroup from post-processing
        Map<String, DataGroup.Attribute> cmeta = dg.getAttributes();
        TableMeta meta = new TableMeta();
        processor.prepareTableMeta(meta, Arrays.asList(dg.getDataDefinitions()), req);
        for (String key : meta.getAttributes().keySet()) {
            if (!cmeta.containsKey(key)) {
                dg.addAttribute(key, meta.getAttribute(key));
            }
        }
    }

}

