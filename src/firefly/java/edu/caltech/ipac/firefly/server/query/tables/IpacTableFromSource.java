/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.tables;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.DbDataIngestor;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.ws.WsServerUtils;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.TableUtil;

import java.io.File;
import java.io.IOException;

import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_INDEX;
import static edu.caltech.ipac.firefly.server.query.tables.IpacTableFromSource.PROC_ID;
import static edu.caltech.ipac.firefly.server.util.QueryUtil.SEARCH_REQUEST;
import static edu.caltech.ipac.table.TableUtil.Format.*;
import static edu.caltech.ipac.table.TableUtil.guessFormat;
import static edu.caltech.ipac.util.StringUtils.isEmpty;


@SearchProcessorImpl(id = PROC_ID)
public class IpacTableFromSource extends EmbeddedDbProcessor {
    public static final String PROC_ID = "IpacTableFromSource";
    private static final String TBL_TYPE = "tblType";
    private static final String TYPE_CATALOG = "catalog";

    /**
     * This method should not be called anymore because ingestDataIntoDb is overridden.
     */
    @Deprecated
    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        String processor = req.getParam("processor");
        String jsonSearchRequest = req.getParam(SEARCH_REQUEST);

        // by processor ID
        if (!isEmpty(processor)) {
            return getByProcessor(processor, req);
        }

        // by a TableRequest as json string
        if (!isEmpty(jsonSearchRequest)) {
            return getByTableRequest(jsonSearchRequest);
        }

        var srcFile = fetchSourceFile(req);
        return fetchDataFromFile(req, srcFile);
    }

    DataGroup fetchDataFromFile(TableServerRequest req, File srcFile) throws DataAccessException {
        try {
            int tblIdx = req.getIntParam(TBL_INDEX, 0);
            return TableUtil.readAnyFormat(srcFile, tblIdx, req);
        } catch (IOException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    protected DataGroup collectMeta(TableServerRequest req) {
        DataGroup meta = null;
        if (req.getParam(TBL_TYPE, TYPE_CATALOG).equals(TYPE_CATALOG)) {        // if catalog and overlay is not set, set it to "TRUE"
            if (isEmpty(req.getMeta(MetaConst.CATALOG_OVERLAY_TYPE))) {
                meta = new DataGroup();        // used only for meta
                meta.getTableMeta().setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, "TRUE");
            }
        }
        return meta;
    }

    @Override
    protected FileInfo ingestDataIntoDb(TableServerRequest req, DbAdapter dbAdapter) throws DataAccessException {
        try {
            dbAdapter.initDbFile();

            String processor = req.getParam("processor");
            String jsonSearchRequest = req.getParam(SEARCH_REQUEST);
            File srcFile = null;
            DbAdapter.DataGroupSupplier fetchDataGroup = null;
            DataGroup meta = collectMeta(req);

            if (!isEmpty(processor))  {
                fetchDataGroup = () -> getByProcessor(processor, req);
            } else if (!isEmpty(jsonSearchRequest)) {
                fetchDataGroup = () -> getByTableRequest(jsonSearchRequest);
            } else {
                srcFile = fetchSourceFile(req);
            }
            return DbDataIngestor.ingestData(req, dbAdapter, srcFile, meta, makeDgSupplier(req, fetchDataGroup));

        } catch (IOException e) {
            Logger.getLogger().error(e,"Failed to ingest data into the database:" + req.getRequestId());
            throw new DataAccessException(e);
        }
    }
    private File fetchSourceFile (TableServerRequest req) throws DataAccessException {

        String source = req.getParam(ServerParams.SOURCE);
        String altSource = req.getParam(ServerParams.ALT_SOURCE);

        File inf = null;
        if (isWorkspace(req)) {
            // by workspace
            inf = getFromWorkspace(source, altSource);
        } else {
            // by source/altSource
            inf = QueryUtil.resolveFileFromSource(source, req);
            if (inf == null) {
                inf = QueryUtil.resolveFileFromSource(altSource, req);
            }
        }
        if (inf == null) {
            throw new DataAccessException(String.format("Unable to fetch file from path[alt_path]: %s[%s]", source, altSource));
        }
        return inf;
    }

//====================================================================
//
//====================================================================

    private DataGroup getByProcessor(String processor, TableServerRequest request) throws DataAccessException {

        TableServerRequest nReq = new TableServerRequest(processor, request);
        nReq.setPageSize(Integer.MAX_VALUE);    // to ensure we're getting all the data
        nReq.setStartIndex(0);
        SearchProcessor<DataGroupPart> proc = SearchManager.getProcessor(processor);
        if (proc != null) {
            return (proc instanceof CanFetchDataGroup) ? ((CanFetchDataGroup)proc).fetchDataGroup(nReq) : proc.getData(nReq).getData();
        } else {
            throw new DataAccessException("Unable to find a suitable SearchProcessor for the given ID: " + processor);
        }
    }


    private DataGroup getByTableRequest(String jsonSearchRequest) throws DataAccessException {

        TableServerRequest req = QueryUtil.convertToServerRequest(jsonSearchRequest);
        if (isEmpty(req.getRequestId())) {
            throw new DataAccessException("Search request must contain " + ServerParams.ID);
        }
        return getByProcessor(req.getRequestId(), req);
    }

    private File getFromWorkspace(String source, String altSource) throws DataAccessException {

        File file = WsServerUtils.getFileFromWorkspace(source);
        if (file == null) {
            file = WsServerUtils.getFileFromWorkspace(altSource);
        }

        if (file == null) {
            String altSourceDesc= isEmpty(altSource) ? "" : " [" + altSource + "]";
            throw new DataAccessException("File not found for workspace path[alt_path]:" + source + altSourceDesc);
        }

        return ServerContext.convertToFile(file.getPath());
    }

    private boolean isWorkspace(ServerRequest r) {
        return ServerParams.IS_WS.equals(r.getParam(ServerParams.SOURCE_FROM));
    }
}

