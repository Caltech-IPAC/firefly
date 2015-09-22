package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ExternalTaskHandler;
import edu.caltech.ipac.firefly.server.ExternalTaskHandlerImpl;
import edu.caltech.ipac.firefly.server.ExternalTaskLauncher;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.DataType;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */
@SearchProcessorImpl(id = "TableFromExternalTask")
public class IpacTableFromExternalTask extends IpacTablePartProcessor {
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        String launcher = request.getParam(ExternalTaskHandler.LAUNCHER);
        ExternalTaskLauncher taskLauncher = new ExternalTaskLauncher(launcher);


        ExternalTaskHandlerImpl handler = new ExternalTaskHandlerImpl(request.getParam(ExternalTaskHandler.TASK), request.getParam(ExternalTaskHandler.TASK_PARAMS));
        taskLauncher.setHandler(handler);

        taskLauncher.execute();
        File outFile = handler.getOutfile();

        if (!ServerContext.isFileInPath(outFile)) {
            throw new SecurityException("Access is not permitted.");
        }

        return convertToIpacTable(outFile, request);
    }

    @Override
    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        for (Param p : request.getParams()) {
            if (request.isInputParam(p.getName())) {
                defaults.setAttribute(p.getName(), p.getValue());
            }
        }
        UserCatalogQuery.addCatalogMeta(defaults,columns,request);
    }



}
