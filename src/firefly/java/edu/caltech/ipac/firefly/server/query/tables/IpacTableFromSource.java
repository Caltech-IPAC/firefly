/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.tables;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
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
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.util.FormatUtil;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_INDEX;
import static edu.caltech.ipac.firefly.data.table.MetaConst.CATALOG_OVERLAY_TYPE;
import static edu.caltech.ipac.firefly.server.query.tables.IpacTableFromSource.PROC_ID;
import static edu.caltech.ipac.firefly.server.util.QueryUtil.SEARCH_REQUEST;
import static edu.caltech.ipac.util.StringUtils.isEmpty;
import edu.caltech.ipac.firefly.core.Util.Try;


@SearchProcessorImpl(id = PROC_ID)
public class IpacTableFromSource extends EmbeddedDbProcessor {
    public static final String PROC_ID = "IpacTableFromSource";
    private static final String TBL_TYPE = "tblType";
    private static final String TYPE_CATALOG = "catalog";
    private static final String FORMAT = "format";          // format of the source file if known.

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

    @Override
    protected Consumer<DataGroup> makeExtraMetaSetter(TableServerRequest req) {
        var sup = super.makeExtraMetaSetter(req);
        return dg -> {
            if (sup != null) sup.accept(dg);
            if (!dg.getTableMeta().contains(CATALOG_OVERLAY_TYPE)) {                    // when CATALOG_OVERLAY_TYPE is not set, apply defaults
                if (req.getParam(TBL_TYPE, TYPE_CATALOG).equals(TYPE_CATALOG)) {        // if catalog and overlay is not set, set it to "TRUE"
                    if (isEmpty(req.getMeta(CATALOG_OVERLAY_TYPE))) {
                        dg.getTableMeta().setAttribute(CATALOG_OVERLAY_TYPE, "TRUE");
                    }
                }
            }
        };
    }

    @Override
    protected FileInfo ingestDataIntoDb(TableServerRequest req, DbAdapter dbAdapter) throws DataAccessException {
        try {
            dbAdapter.initDbFile();

            String processor = req.getParam("processor");
            String jsonSearchRequest = req.getParam(SEARCH_REQUEST);
            int tblIdx = req.getIntParam(TBL_INDEX, 0);
            String fmt = req.getParam(FORMAT);
            FormatUtil.Format format = isEmpty(fmt) ? null : FormatUtil.Format.valueOf(fmt);
            File srcFile = null;
            DbAdapter.DataGroupSupplier fetchDataGroup = null;

            if (!isEmpty(processor))  {
                fetchDataGroup = () -> getByProcessor(processor, req);
            } else if (!isEmpty(jsonSearchRequest)) {
                fetchDataGroup = () -> getByTableRequest(jsonSearchRequest);
            } else {
                srcFile = fetchSourceFile(req);
            }
            if (srcFile == null) {
                return DbDataIngestor.ingestData(req, dbAdapter, makeDgSupplier(req, fetchDataGroup));
            } else {
                return DbDataIngestor.ingestData(req, dbAdapter, makeExtraMetaSetter(req), srcFile, tblIdx, format);
            }
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
            boolean isExternal = isExternalSource(source);
            inf = QueryUtil.resolveFileFromSource(source, req);
            if (inf == null) {
                isExternal = isExternalSource(source);
                inf = QueryUtil.resolveFileFromSource(altSource, req);
            }
            if (isExternal) req.setMeta(TableMeta.DATA_ORIGIN, "external");
        }
        if (inf == null) {
            throw new DataAccessException(String.format("Unable to fetch file from path[alt_path]: %s[%s]", source, altSource));
        }
        return inf;
    }

    private boolean isExternalSource(String source) {
        String sourceBase = getBaseDomain(source);
        if (sourceBase == null) return false;
        String hostBase = getBaseDomain(ServerContext.getRequestOwner().getBaseUrl());
        boolean isExternal = !sourceBase.equals(hostBase);
        Logger.getLogger().debug("Is external source: " + isExternal + " sourceBase: " + sourceBase + " hostBase: " + hostBase);
        return isExternal;
    }

    private static String getBaseDomain(String source) {
        URI uri = Try.it(() -> new URI(source.toLowerCase())).get();
        if (uri == null) return null;
        String host = uri.getHost();
        String[] parts = host.split("\\.");
        if (parts.length < 2) return host;
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
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

