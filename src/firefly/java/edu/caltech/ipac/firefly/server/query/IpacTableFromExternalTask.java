package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ExternalTaskHandlerImpl;
import edu.caltech.ipac.firefly.server.ExternalTaskLauncher;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;

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

        String launcher = request.getParam("launcher");
        ExternalTaskLauncher taskLauncher = new ExternalTaskLauncher(launcher);

        try {

            ExternalTaskHandlerImpl handler = new ExternalTaskHandlerImpl(request.getParam("task"), request.getParam("taskParams"));
            taskLauncher.setHandler(handler);

            taskLauncher.execute();
            File outFile = handler.getOutfile();

            boolean isFixedLength = request.getBooleanParam(TableServerRequest.FIXED_LENGTH, true);


            if (!ServerContext.isFileInPath(outFile)) {
                throw new SecurityException("Access is not permitted.");
            }

            DataGroupReader.Format format = DataGroupReader.guessFormat(outFile);

            File inf = outFile;
            if (format == DataGroupReader.Format.IPACTABLE && isFixedLength) {
                // file is already in ipac table format
            } else {
                if (format != DataGroupReader.Format.UNKNOWN) {
                    // convert it into ipac table format
                    DataGroup dg = DataGroupReader.readAnyFormat(outFile);
                    if (format == DataGroupReader.Format.IPACTABLE) {
                        inf = FileUtil.createUniqueFileFromFile(outFile);
                    } else {
                        inf = FileUtil.modifyFile(outFile, "tbl");
                    }
                    DataGroupWriter.write(inf, dg, 0);
                } else {
                    // format is unknown
                    throw new DataAccessException("Source file has an unknown format: " + ServerContext.replaceWithPrefix(outFile));
                }
            }
            return inf;

        } finally {
       }
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
