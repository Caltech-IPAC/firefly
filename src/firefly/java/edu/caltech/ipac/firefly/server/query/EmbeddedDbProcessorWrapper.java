/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.TableUtil;
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

    EmbeddedDbProcessorWrapper(IpacTablePartProcessor processor) {
        this.processor = processor;
    }

    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        TableServerRequest nreq = (TableServerRequest) req.cloneRequest();
        return processor.fetchDataGroup(nreq);
    }

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        processor.prepareTableMeta(defaults, columns, request);
    }

    public void onComplete(ServerRequest request, DataGroupPart results) throws DataAccessException {
        processor.onComplete(request, results);
    }

}

