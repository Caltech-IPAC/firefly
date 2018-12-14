/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.io.IpacTableException;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.packagedata.PackageMaster;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.JsonTableUtil;
import edu.caltech.ipac.util.Assert;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static edu.caltech.ipac.firefly.core.background.BackgroundStatus.CLIENT_REQ;
import static edu.caltech.ipac.firefly.core.background.BackgroundStatus.SERVER_REQ;
import static edu.caltech.ipac.table.TableMeta.IS_FULLY_LOADED;


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
        SearchProcessor processor = getProcessor(request.getRequestId());
        if (request != null) {
            String jsonText = (String) processor.getData(request);
            // validate JSON and replace file paths with prefixes
            JSONParser parser = new JSONParser();
            try{
                Object obj = parser.parse(jsonText);
                Object json = ServerContext.replaceWithPrefixes(obj);
                jsonText = JSONValue.toJSONString(json);
                return jsonText;
            }
            catch(ParseException pe){
                LOGGER.error(processor.getUniqueID(request) + " Can not parse returned JSON: " + pe.toString() + "\n" + jsonText);
                throw new DataAccessException(request.getRequestId()+" Can not parse returned JSON: " + pe.toString());
            }
        } else {
            throw new DataAccessException("Request fail inspection.  Operation aborted.");
        }
    }

//====================================================================
//  search related funtions...
//====================================================================
    public DataGroupPart getDataGroup(TableServerRequest request) throws DataAccessException {

        SearchProcessor processor = getProcessor(request.getRequestId());
        DataGroupPart dgp = null;
        dgp = (DataGroupPart) processor.getData(request);

        TableMeta meta = dgp.getData().getTableMeta();
        processor.prepareTableMeta(meta,
                Arrays.asList(dgp.getData().getDataDefinitions()),
                request);
        return dgp;
    }

    public FileInfo save(OutputStream saveTo, TableServerRequest dataRequest) throws DataAccessException {
        try {
            SearchProcessor processor = getProcessor(dataRequest.getRequestId());
            if (dataRequest != null) {
                return processor.writeData(saveTo, dataRequest);
            } else {
                throw new DataAccessException("Request fail inspection.  Operation aborted.");
            }
        } catch (Exception e) {
            throw new DataAccessException("Error while writing to Stream", e);
        }
    }

    public SearchProcessor getProcessor(String requestId) {
        SearchProcessor processor = SearchProcessorFactory.getProcessor(requestId);

        if (processor instanceof IpacTablePartProcessor) {
            // switch all ipac table processor to use DbProcessor  -- there is a small overhead, but it will get added features.
            processor = new EmbeddedDbProcessorWrapper((IpacTablePartProcessor) processor);
        }

        Assert.argTst(processor != null, "Search implementation is not defined for "+requestId);
        assert processor != null;
        return processor;
    }

    public BackgroundStatus packageRequest(final DownloadRequest request) throws DataAccessException {

        SearchProcessor<List<FileGroup>> processor = getProcessor(request.getRequestId());
        if (processor != null)  {
            return new PackageMaster().packageData(request, processor);
        }
        else {
            return BackgroundStatus.createUnknownFailStat();
        }
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



    public BackgroundStatus submitBackgroundSearch(TableServerRequest request, Request clientRequest, int waitMillis) throws RPCException {

        Logger.briefDebug("Backgrounded search started:" + waitMillis + " wait, req:" + request);
        String email= request.getMeta(ServerParams.EMAIL);
        SearchWorker worker= new SearchWorker(request, clientRequest);
        String title = request.getTblTitle() == null ? request.getRequestId() : request.getTblTitle();
        BackgroundEnv.BackgroundProcessor processor=
                              new BackgroundEnv.BackgroundProcessor(worker,  null,
                                                                    title, request.getMeta(BackgroundStatus.DATA_TAG),
                                                                    email, request.getRequestId(),
                                                                    ServerContext.getRequestOwner() );
        return BackgroundEnv.backgroundProcess(waitMillis, processor, BackgroundStatus.BgType.SEARCH);
    }

//====================================================================
//  inner classes
//====================================================================
    private class SearchWorker implements BackgroundEnv.Worker {

        private final TableServerRequest request;
        private Request clientRequest;

        public SearchWorker(TableServerRequest request, Request clientRequest) {
            this.request= request;
            this.clientRequest = clientRequest;
        }

        public BackgroundStatus work(BackgroundEnv.BackgroundProcessor p)  throws Exception {
            DataGroupPart data= getDataGroup(request);

            BackgroundStatus bgStat= new BackgroundStatus(p.getBID(), BackgroundState.SUCCESS, BackgroundStatus.BgType.SEARCH);
            if (request != null) {
                bgStat.setParam(SERVER_REQ, JsonTableUtil.toJsonTableRequest(request).toJSONString());
            }
            if (clientRequest != null) {
                bgStat.setParam(CLIENT_REQ, JsonTableUtil.toJsonTableRequest(clientRequest).toJSONString());
            }
            return bgStat;
        }
    }


}
