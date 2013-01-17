package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.core.background.BackgroundReport;
import edu.caltech.ipac.firefly.core.background.BackgroundSearchReport;
import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.FileStatus;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.packagedata.PackageMaster;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;


/**
 * @author loi, tatianag
 * $Id: SearchManager.java,v 1.31 2012/07/27 22:23:29 tatianag Exp $
 */
public class SearchManager {
    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();


    public RawDataSet getRawDataSet(TableServerRequest request) throws DataAccessException {
        SearchProcessor processor = getProcessor(request.getRequestId());
        ServerRequest req = processor.inspectRequest(request);
        if (req != null) {
            DataGroupPart dgp = (DataGroupPart) processor.getData(req);
            RawDataSet ds = QueryUtil.getRawDataSet(dgp);
            DataGroupPart.State status = dgp.getTableDef().getStatus();
            ds.getMeta().setIsLoaded(!status.equals(DataGroupPart.State.INPROGRESS));

            processor.prepareTableMeta(ds.getMeta(),
                    Collections.unmodifiableList(dgp.getTableDef().getCols()),
                    req);

            return ds;
        } else {
            throw new DataAccessException("Request fail inspection.  Operation aborted.");
        }

    }

    public DataGroupPart getDataGroup(TableServerRequest request) throws DataAccessException {

        SearchProcessor processor = getProcessor(request.getRequestId());
        ServerRequest req = processor.inspectRequest(request);
        if (req != null) {
            DataGroupPart dgp = (DataGroupPart) processor.getData(req);
            return dgp;
        } else {
            throw new DataAccessException("Request fail inspection.  Operation aborted.");
        }
    }

    public void save(OutputStream saveTo, TableServerRequest dataRequest) throws DataAccessException {
        try {
            SearchProcessor processor = getProcessor(dataRequest.getRequestId());
            ServerRequest req = processor.inspectRequest(dataRequest);
            if (req != null) {
                processor.writeData(saveTo, req);
            } else {
                throw new DataAccessException("Request fail inspection.  Operation aborted.");
            }
        } catch (Exception e) {
            throw new DataAccessException("Error while writing to Stream", e);
        }
    }

    public FileStatus getFileStatus(File inf) {
        try {
            DataGroupPart.TableDef headers = IpacTableParser.getMetaInfo(inf);
            FileStatus fs = new FileStatus();
            fs.setState(FileStatus.State.valueOf(headers.getStatus().name()));
            fs.setRowCount(headers.getRowCount());

            return fs;
        } catch (IOException e) {
            e.printStackTrace();
            return new FileStatus(FileStatus.State.FAILED, 0);
        }
    }

    public List<String> getDataFileValues(File file, List<Integer> rows, String colName) throws DataAccessException, IOException {

        if (!file.canRead() ||
                !file.getAbsolutePath().startsWith(ServerContext.getWorkingDir().getAbsolutePath())) {
            throw new DataAccessException("Unable to access this file:" + file.getAbsolutePath());
        }
        IpacTableParser.MappedData data = IpacTableParser.getData(file, rows, colName);
        if (data != null) {
            return data.getValues(colName);
        }
        return null;
    }

    public SearchProcessor getProcessor(String requestId) {
        SearchProcessor processor = SearchProcessorFactory.getProcessor(requestId);
        Assert.argTst(processor != null, "Search implementation is not defined for "+requestId);
        assert processor != null;
        return processor;
    }

    public BackgroundReport packageRequest(final DownloadRequest request) throws DataAccessException {

        SearchProcessor<List<FileGroup>> processor = getProcessor(request.getRequestId());
        if (processor != null)  {
            return new PackageMaster().packageData(request, processor);
        }
        else {
            return BackgroundReport.createUnknownReport();
        }
    }

    public FileInfo getFileInfo(TableServerRequest request) throws DataAccessException {
        SearchProcessor processor = getProcessor(request.getRequestId());
        if (processor != null) {
            try {
                if (processor instanceof IpacTablePartProcessor) {
                    File dgFile = ((IpacTablePartProcessor)processor).getDataFile(request);
                    // page size will not be taken into account
                    return new FileInfo(dgFile.getPath(), dgFile.getName(), dgFile.length());
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



    public BackgroundReport getRawDataSetBackground(TableServerRequest request, Request clientRequest, int waitMillis) throws RPCException {

        Logger.briefDebug("Backgrounded search started:" + waitMillis + " wait, req:" + request);
        String email= request.containsParam(ReqConst.EMAIL)? request.getParam(ReqConst.EMAIL) : "";
        SearchWorker worker= new SearchWorker(request, clientRequest);
        BackgroundEnv.BackgroundProcessor processor=
                              new BackgroundEnv.BackgroundProcessor(worker,  null,
                                                                    request.getRequestId(),
                                                                    email, request.getRequestId(),
                                                                    ServerContext.getRequestOwner() );
        return BackgroundEnv.backgroundProcess(waitMillis, processor);
    }



    private class SearchWorker implements BackgroundEnv.Worker {

        private final TableServerRequest request;
        private Request clientRequest;

        public SearchWorker(TableServerRequest request, Request clientRequest) {
            this.request= request;
            this.clientRequest = clientRequest;
        }

        public BackgroundReport work(BackgroundEnv.BackgroundProcessor p)  throws Exception {
            RawDataSet data= getRawDataSet(request);
            Logger.briefDebug("Backgrounded search completed.  req:" + request);
            BackgroundSearchReport bsr = new BackgroundSearchReport(p.getBID(), BackgroundState.SUCCESS, clientRequest, request);
            if (data.getTotalRows() > 0) {
                bsr.setFilePath(data.getMeta().getSource());
            }
            return bsr;
        }
    }

}
