/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.server.ws.WsServerUtils;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author tatianag
 *         $Id: UserCatalogQuery.java,v 1.1 2011/09/23 23:37:51 tatianag Exp $
 */
@SearchProcessorImpl(id ="userCatalogFromFile")
public class UserCatalogQuery extends IpacTablePartProcessor {

    @Override
    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        String filePath = req.getParam("filePath");

        if (StringUtils.isEmpty(filePath)) throw new DataAccessException("filePath parameter is not found");

        File userCatFile;
        if (ServerParams.IS_WS.equals(req.getParam(ServerParams.SOURCE_FROM))) {
            userCatFile = WsServerUtils.getFileFromWorkspace(filePath);
        } else {
            userCatFile = ServerContext.convertToFile(filePath);
        }
        try {
            int tblIdx = req.getIntParam(TableServerRequest.TBL_INDEX, 0);
            return TableUtil.readAnyFormat(userCatFile, tblIdx, req);
        } catch (IOException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    protected File loadDataFile(TableServerRequest req) throws IOException, DataAccessException {
        return loadDataFileImpl(req);
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        String filePath= request.getParam("filePath");
        if (filePath!=null) {
            UploadFileInfo uFi=(UploadFileInfo)UserCache.getInstance().get(new StringKey(filePath));
            if (uFi!=null) {
                meta.setAttribute("title", uFi.getFileName());
            }
        }
        meta.setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, "TRUE");
        super.prepareTableMeta(meta, columns, request);
    }

}
