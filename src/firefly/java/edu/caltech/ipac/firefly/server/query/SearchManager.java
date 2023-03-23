/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.table.io.IpacTableException;
import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.util.Assert;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;


/**
 * @author loi, tatianag
 * $Id: SearchManager.java,v 1.31 2012/07/27 22:23:29 tatianag Exp $
 */
public class SearchManager {
    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    /**
     * This search function is to support the CommandServices interface
     * For SearchProcessor's interface, see JsonSearchServices.
     */
    public String getJSONData(ServerRequest request) throws DataAccessException {

        if (request == null) throw new DataAccessException("Operation failed because request parameter is null.");

        SearchProcessor processor = getProcessor(request.getRequestId());
        if (processor instanceof JsonStringProcessor) {
            String jsonText = ((JsonStringProcessor)processor).getData(request);
            return jsonText;
        } else {
            throw new DataAccessException("Unexpected Exception: request id: " + request.getRequestId()
                    + " resolves to a search processor that is not an instance of JsonStringProcessor");
        }
    }

//====================================================================
//  search related funtions...
//====================================================================

    public DataGroupPart getDataGroup(TableServerRequest request) throws DataAccessException {
        SearchProcessor processor = getProcessor(request.getRequestId());
        return getDataGroup(request, processor);
    }

    public DataGroupPart getDataGroup(TableServerRequest request, SearchProcessor processor) throws DataAccessException {
        DataGroupPart dgp = (DataGroupPart) processor.getData(request);

        TableMeta meta = dgp.getData().getTableMeta();
        processor.prepareTableMeta(meta,
                Arrays.asList(dgp.getData().getDataDefinitions()),
                request);
        return dgp;
    }

    public FileInfo save(OutputStream saveTo, TableServerRequest dataRequest, TableUtil.Format format) throws DataAccessException {
        return save(saveTo, dataRequest, format, TableUtil.Mode.displayed);
    }

    public FileInfo save(OutputStream saveTo, TableServerRequest dataRequest, TableUtil.Format format, TableUtil.Mode mode) throws DataAccessException {
        try {
            SearchProcessor processor = getProcessor(dataRequest.getRequestId());
            if (dataRequest != null) {
                return processor.writeData(saveTo, dataRequest, format, mode);
            } else {
                throw new DataAccessException("Request fail inspection.  Operation aborted.");
            }
        } catch (Exception e) {
            throw new DataAccessException("Error while writing to Stream", e);
        }
    }


    static public SearchProcessor getProcessor(String requestId) {
        SearchProcessor processor = SearchProcessorFactory.getProcessor(requestId);

        if (processor instanceof IpacTablePartProcessor) {
            // switch all ipac table processor to use DbProcessor  -- there is a small overhead, but it will get added features.
            processor = new EmbeddedDbProcessorWrapper((IpacTablePartProcessor) processor);
        }

        Assert.argTst(processor != null, "Search implementation is not defined for "+requestId);
        assert processor != null;
        return processor;
    }

    public FileInfo getFileInfo(TableServerRequest request) throws DataAccessException {
        SearchProcessor processor = getProcessor(request.getRequestId());
        if (processor != null) {
            try {
                if (processor instanceof SearchProcessor.CanGetDataFile) {
                    File dgFile = ((SearchProcessor.CanGetDataFile)processor).getDataFile(request);
                    // page size will not be taken into account
                    return new FileInfo(dgFile);
                } else {
                    return (FileInfo) processor.getData(request);
                }
            } catch (ClassCastException e) {
                LOGGER.error(e, "Invalid processor mapping.  Return value is not of type FileInfo.");
                throw new DataAccessException("Request failed due to unexpected exception.", e);
            } catch (IpacTableException e) {
                LOGGER.error(e, "IPAC table exception. Unable to get IPAC table.");
                throw new DataAccessException("Request failed.", e);
            } catch (IOException e) {
                LOGGER.error(e, "IO Exception. Unable to get IPAC table.");
                throw new DataAccessException("Request failed.", e);
            }
        }
        return null;
    }

}
