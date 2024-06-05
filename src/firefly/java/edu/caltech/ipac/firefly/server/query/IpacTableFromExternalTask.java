package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.server.ExternalTaskHandler;
import edu.caltech.ipac.firefly.server.ExternalTaskHandlerImpl;
import edu.caltech.ipac.firefly.server.ExternalTaskLauncher;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.table.TableUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_INDEX;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */
@SearchProcessorImpl(id = "TableFromExternalTask")
public class IpacTableFromExternalTask extends IpacTablePartProcessor {

    public DataGroup fetchDataGroup(TableServerRequest request) throws DataAccessException {
        try {
            String launcher = request.getParam(ExternalTaskHandler.LAUNCHER);
            ExternalTaskLauncher taskLauncher = new ExternalTaskLauncher(launcher);
            int tblIdx = request.getIntParam(TBL_INDEX, 0);

            ExternalTaskHandlerImpl handler = new ExternalTaskHandlerImpl(request.getParam(ExternalTaskHandler.TASK), request.getParam(ExternalTaskHandler.TASK_PARAMS));
            taskLauncher.setHandler(handler);

            taskLauncher.execute();
            File outFile = handler.getOutfile();

            if (!ServerContext.isFileInPath(outFile)) {
                throw new SecurityException("Access is not permitted.");
            }
            return TableUtil.readAnyFormat(outFile, tblIdx, request);
        } catch (IOException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {
        return loadDataFileImpl(request);
    }

    @Override
    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        for (Param p : request.getParams()) {
            if (request.isInputParam(p.getName())) {
                defaults.setAttribute(p.getName(), p.getValue());
            }
        }
        Map reqMeta= ((TableServerRequest) request).getMeta();
        if (reqMeta==null || !reqMeta.containsKey(MetaConst.CATALOG_OVERLAY_TYPE)) {
            defaults.setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, "TRUE");
        }
    }



}
