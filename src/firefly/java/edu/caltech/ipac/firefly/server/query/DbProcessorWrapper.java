/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.TableDbUtil;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DbProcessorWrapper extends DbProcessor {
    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    private IpacTablePartProcessor processor;

    public DbProcessorWrapper(IpacTablePartProcessor processor) {
        this.processor = processor;
    }


    public DataGroupPart getData(ServerRequest request) throws DataAccessException {

        TableServerRequest treq = (TableServerRequest) request;
        if (treq.getDecimateInfo() == null) {
            return super.getData(treq);
        } else {
            // this is bad.. need to fix
            return processor.getData(treq);
        }
    }


    public FileInfo createDbFile(TableServerRequest req) throws DataAccessException {
        try {
            DbAdapter dbAdapter = DbAdapter.getAdapter(req);

            File dbFile = createTempFile(req, "." + dbAdapter.getName());
            TableServerRequest nreq = (TableServerRequest) req.cloneRequest();
            nreq.keepBaseParamOnly();
            StopWatch.getInstance().start("getBaseData");
            File dataFile = processor.getDataFile(nreq);
            DataGroup dg = IpacTableReader.readIpacTable(dataFile, "temp");
            StopWatch.getInstance().stop("getBaseData").printLog("getBaseData");

            // merge meta into datagroup from post-processing
            Map<String, DataGroup.Attribute> cmeta = dg.getAttributes();
            TableMeta meta = new TableMeta();
            processor.prepareTableMeta(meta, Arrays.asList(dg.getDataDefinitions()), req);
            for (String key : meta.getAttributes().keySet()) {
                if (!cmeta.containsKey(key)) {
                    dg.addAttribute(key, meta.getAttribute(key));
                }
            }

            FileInfo finfo = TableDbUtil.createDbFile(dbFile, dg, dbAdapter);
            return finfo;
        } catch (IpacTableException | IOException | DataAccessException ex) {
            throw new DataAccessException(ex);
        }
    }

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        // no need to do it again.. already did it in createDbFile
    }

    public void onComplete(ServerRequest request, DataGroupPart results) throws DataAccessException {
        processor.onComplete(request, results);
    }

    public boolean doLogging() {
        return false;
    }
}

