package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ExternalTaskHandler;
import edu.caltech.ipac.firefly.server.ExternalTaskHandlerImpl;
import edu.caltech.ipac.firefly.server.ExternalTaskLauncher;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.DataType;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */
@SearchProcessorImpl(id = "FileFromExternalTask")
public class FileFromExternalTask implements SearchProcessor<FileInfo> {

    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    public ServerRequest inspectRequest(ServerRequest request) {
        return request;
    }

    public String getUniqueID(ServerRequest request) {
        String uid = request.getRequestId() + "-";

        for (String p : ExternalTaskHandler.ALL_PARAMS) {
            String v = request.getParam(p);
            if (v != null) {
                uid += "|" + p;
            }
        }
        return uid;
    }

    public String getDatasetID(ServerRequest request) {
        return getUniqueID(request);
    }

    public FileInfo getData(ServerRequest request) throws DataAccessException {
        String launcher = request.getParam(ExternalTaskHandler.LAUNCHER);
        ExternalTaskLauncher taskLauncher = new ExternalTaskLauncher(launcher);

        try {

            ExternalTaskHandlerImpl handler = new ExternalTaskHandlerImpl(request.getParam(ExternalTaskHandler.TASK), request.getParam(ExternalTaskHandler.TASK_PARAMS));
            taskLauncher.setHandler(handler);

            taskLauncher.execute();
            File outFile = handler.getOutfile();

            if (!ServerContext.isFileInPath(outFile)) {
                throw new SecurityException("Access is not permitted.");
            }

            return new FileInfo(outFile.getAbsolutePath(), outFile.getName(), outFile.length());
        } catch (Exception e) {
            LOGGER.error(e, "Unable to get file from external task: "+request.toString());
            throw new DataAccessException("Unable to get file from external task: "+e.getMessage());
        }
    }

    public FileInfo writeData(OutputStream out, ServerRequest request) throws DataAccessException {
        /* does not apply.. do nothing */
        return null;
    }

    public boolean doCache() {
        return false;
    }

    public void onComplete(ServerRequest request, FileInfo results) throws DataAccessException {
    }

    public boolean doLogging() {
        return false;
    }

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
      /* this only applies to table-based results... do nothing here */
    }

    public QueryDescResolver getDescResolver() {
        return null;
    }
}
