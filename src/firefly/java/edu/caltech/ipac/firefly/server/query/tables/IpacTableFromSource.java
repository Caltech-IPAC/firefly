/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.tables;

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DbFromFileProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.ws.WsServerUtils;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataGroupPart;

import java.io.File;

import static edu.caltech.ipac.firefly.data.table.MetaConst.CATALOG_OVERLAY_TYPE;
import static edu.caltech.ipac.firefly.server.query.tables.IpacTableFromSource.PROC_ID;
import static edu.caltech.ipac.firefly.server.util.QueryUtil.SEARCH_REQUEST;
import static edu.caltech.ipac.util.StringUtils.isEmpty;


@SearchProcessorImpl(id = PROC_ID)
public class IpacTableFromSource extends DbFromFileProcessor {
    public static final String PROC_ID = "IpacTableFromSource";
    private static final String TBL_TYPE = "tblType";
    private static final String TYPE_CATALOG = "catalog";
    private static final String FORMAT = "format";          // format of the source file if known.


    @Override
    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        // when SOURCE is not provided, try to get data from processor or search request
        String processor = req.getParam("processor");
        String jsonSearchRequest = req.getParam(SEARCH_REQUEST);
        if (!isEmpty(processor))  {
            return getByProcessor(processor, req);
        } else if (!isEmpty(jsonSearchRequest)) {
            return getByTableRequest(jsonSearchRequest);
        }
        return null;
    }

    @Override
    public File getDataFile(TableServerRequest req) throws DataAccessException {
        String source = req.getParam(ServerParams.SOURCE);
        String altSource = req.getParam(ServerParams.ALT_SOURCE);
        updateJob(ji -> ji.getAux().setJobUrl(source));

        if (isWorkspace(req)) {
            // by workspace
            File inf = getFromWorkspace(source, altSource);
            if (inf == null || !inf.canRead()) {
                throw new DataAccessException("Unable to read file from workspace: " + source);
            }
            return inf;
        }
        if (isEmpty(source) && isEmpty(altSource)) {
            return null;        // no source provided; return null so fetchDataGroup can be tried
        }

        // by source parameter
        File inf = QueryUtil.resolveFileFromSource(source, req);
        if (inf == null) inf = QueryUtil.resolveFileFromSource(altSource, req);
        if (inf == null) {
            throw new DataAccessException(String.format("Unable to fetch file from path[alt_path]: %s[%s]", source, altSource));
        }
        setJobResults(inf);
        return inf;
    }

    @Override
    protected void applyExtraMeta(DataGroup dg, TableServerRequest req) {
        super.applyExtraMeta(dg, req);
        if (!dg.getTableMeta().contains(CATALOG_OVERLAY_TYPE)) {                    // when CATALOG_OVERLAY_TYPE is not set, apply defaults
            if (req.getParam(TBL_TYPE, TYPE_CATALOG).equals(TYPE_CATALOG)) {        // if catalog and overlay is not set, set it to "TRUE"
                if (isEmpty(req.getMeta(CATALOG_OVERLAY_TYPE))) {
                    dg.getTableMeta().setAttribute(CATALOG_OVERLAY_TYPE, "TRUE");
                }
            }
        }
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
            return proc.getData(nReq).getData();
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

