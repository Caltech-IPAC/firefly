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
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.ws.WsServerUtils;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_INDEX;
import static edu.caltech.ipac.firefly.server.util.QueryUtil.SEARCH_REQUEST;
import static edu.caltech.ipac.firefly.server.query.tables.IpacTableFromSource.PROC_ID;


@SearchProcessorImpl(id = PROC_ID)
public class IpacTableFromSource extends IpacTablePartProcessor {
    public static final String PROC_ID = "IpacTableFromSource";
    private static final String TBL_TYPE = "tblType";
    private static final String TYPE_CATALOG = "catalog";


    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {

        String source = req.getParam(ServerParams.SOURCE);
        String altSource = req.getParam(ServerParams.ALT_SOURCE);
        String processor = req.getParam("processor");
        String jsonSearchRequest = req.getParam(SEARCH_REQUEST);

        // by processor ID
        if (!StringUtils.isEmpty(processor)) {
            return getByProcessor(processor, req);
        }

        // by a TableRequest as json string
        if (!StringUtils.isEmpty(jsonSearchRequest)) {
            return getByTableRequest(jsonSearchRequest);
        }

        // file based source
        File inf;

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

        try {
            int tblIdx = req.getIntParam(TBL_INDEX, 0);
            return TableUtil.readAnyFormat(inf, tblIdx, req.getMeta());
        } catch (IOException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        DataGroup dataGroup = fetchDataGroup(request);
        File ofile = createFile(request, ".tbl");
        IpacTableWriter.save(ofile, dataGroup);
        return ofile;
    }


    @Override
    public boolean doCache() {
        return false;
    }

    @Override
    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        String type = request.getParam(TBL_TYPE);
        if (type == null || type.equals(TYPE_CATALOG)) {
            Map reqMeta= ((TableServerRequest) request).getMeta();
            if (reqMeta==null || !reqMeta.containsKey(MetaConst.CATALOG_OVERLAY_TYPE)) {
                defaults.setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, "TRUE");
            }
        }
    }

//====================================================================
//
//====================================================================

    private DataGroup getByProcessor(String processor, TableServerRequest request) throws DataAccessException {

        TableServerRequest nReq = new TableServerRequest(processor, request);
        nReq.setPageSize(Integer.MAX_VALUE);    // to ensure we're getting all of the data
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
        if (StringUtils.isEmpty(req.getRequestId())) {
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
            String altSourceDesc=StringUtils.isEmpty(altSource) ? "" : " [" + altSource + "]";
            throw new DataAccessException("File not found for workspace path[alt_path]:" + source + altSourceDesc);
        }

        return ServerContext.convertToFile(file.getPath());
    }

    private boolean isWorkspace(ServerRequest r) {
        return ServerParams.IS_WS.equals(r.getParam(ServerParams.SOURCE_FROM));
    }
}

