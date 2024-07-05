/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.tables;

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.DuckDbReadable;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.ws.WsServerUtils;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.TableUtil;

import java.io.File;
import java.io.IOException;

import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_TYPE;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_INDEX;
import static edu.caltech.ipac.firefly.server.query.tables.IpacTableFromSource.PROC_ID;
import static edu.caltech.ipac.firefly.server.util.QueryUtil.SEARCH_REQUEST;
import static edu.caltech.ipac.util.StringUtils.isEmpty;


@SearchProcessorImpl(id = PROC_ID)
public class IpacTableFromSource extends EmbeddedDbProcessor {
    public static final String PROC_ID = "IpacTableFromSource";
    private static final String TBL_TYPE = "tblType";
    private static final String TYPE_CATALOG = "catalog";

    // because a SearchProcessor is created on each request,
    // it's okay to save it as a member variable once it's fetched.
    private File inf;
    private boolean noSourceFile = false;
    private DataAccessException fetchError;

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
        if (noSourceFile) throw fetchError;

        try {
            int tblIdx = req.getIntParam(TBL_INDEX, 0);
            DataGroup dataGroup = TableUtil.readAnyFormat(srcFile, tblIdx, req);

            String type = req.getParam(TBL_TYPE, TYPE_CATALOG);
            if (type.equals(TYPE_CATALOG)) {        // if catalog and overlay is not set, set it to "TRUE"
                if (isEmpty(req.getMeta(MetaConst.CATALOG_OVERLAY_TYPE))) {
                    dataGroup.getTableMeta().setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, "TRUE");
                }
            }
            return dataGroup;

        } catch (IOException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    public DbAdapter getDbAdapter(TableServerRequest treq) {
        var srcFile = fetchSourceFile(treq);
        DbAdapter test = DbAdapter.getAdapter(srcFile);     // test to see if srcFile can be imported directly by DuckDB
        if ( test instanceof DuckDbReadable dr) {
            dr.useDbFileFrom(makeDbFile(treq));
            return dr;
        }else {
            return super.getDbAdapter(treq);
        }
    }

    private File fetchSourceFile (TableServerRequest req) {

        if (noSourceFile || inf != null) return inf;        // already resolved

        String processor = req.getParam("processor");
        String jsonSearchRequest = req.getParam(SEARCH_REQUEST);
        String source = req.getParam(ServerParams.SOURCE);
        String altSource = req.getParam(ServerParams.ALT_SOURCE);

        if (isEmpty(processor) && isEmpty(jsonSearchRequest)) {
            try {

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
                    noSourceFile = true;
                    fetchError = new DataAccessException(String.format("Unable to fetch file from path[alt_path]: %s[%s]", source, altSource));
                }
            } catch (DataAccessException e) {
                fetchError = e;
                noSourceFile = true;
            }
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

