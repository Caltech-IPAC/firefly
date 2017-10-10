package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ExternalTaskHandler;
import edu.caltech.ipac.firefly.server.ExternalTaskHandlerImpl;
import edu.caltech.ipac.firefly.server.ExternalTaskLauncher;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.FileUtil;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */
@SearchProcessorImpl(id = "JsonFromExternalTask")
public class JsonFromExternalTask implements SearchProcessor {

    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    @Override
    public ServerRequest inspectRequest(ServerRequest request) {
        return request;
    }

    @Override
    public String getUniqueID(ServerRequest request) {
        String uid = request.getRequestId() + "-";

        for (String p : ExternalTaskHandler.ALL_PARAMS) {
            String v = request.getParam(p);
            if (v != null) {
                uid += "|" + v;
            }
        }
        return uid;
    }

    @Override
    public String getData(ServerRequest request) throws DataAccessException {
        String launcher = request.getParam(ExternalTaskHandler.LAUNCHER);
        if (launcher == null) {
            throw new DataAccessException(ExternalTaskHandler.LAUNCHER+" parameter is not found in request.");
        }
        ExternalTaskLauncher taskLauncher = new ExternalTaskLauncher(launcher);

        try {

            ExternalTaskHandlerImpl handler = new ExternalTaskHandlerImpl(request.getParam(ExternalTaskHandler.TASK), request.getParam(ExternalTaskHandler.TASK_PARAMS));
            taskLauncher.setHandler(handler);

            taskLauncher.execute();
            File outFile = handler.getOutfile();
            if (outFile == null) {
                return handler.getResult();
            }
            // get result from outfile

            if (!ServerContext.isFileInPath(outFile)) {
                throw new SecurityException("Access to "+outFile.getAbsolutePath()+" is not permitted.");
            }

            return FileUtil.readFile(outFile);
        } catch (Exception e) {
            LOGGER.error(e, "Unable get to data from external task: "+request.toString());
            throw new DataAccessException("Unable to get data from external task: "+e.getMessage());
        }
    }

    @Override
    public FileInfo writeData(OutputStream out, ServerRequest request) throws DataAccessException {
        /* does not apply.. do nothing */
        return null;
    }

    @Override
    public boolean doCache() {
        return false;
    }

    @Override
    public void onComplete(ServerRequest request, Object results) throws DataAccessException {

    }

    @Override
    public boolean doLogging() {
        return false;
    }

    @Override
    public QueryDescResolver getDescResolver() {
        return null;
    }

    @Override
    public void prepareTableMeta(TableMeta defaults, List columns, ServerRequest request) {
        /* this only applies to table-based results... do nothing here */
    }
}
