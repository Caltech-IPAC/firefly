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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */
@SearchProcessorImpl(id = "IpacTableFromExternalTask")
public class IpacTableFromExternalTask extends IpacTablePartProcessor {
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        String launcher = request.getParam("launcher");
        String task = request.getParam("task");

        ExternalTaskLauncher taskLauncher = new ExternalTaskLauncher(launcher);
        Path workingDir = getWorkDir(task);

        try {
            taskLauncher.setWorkDir(workingDir.toFile());
            taskLauncher.addParam("-n", task);
            taskLauncher.addParam("-d", workingDir.toString());
            File outFile = getOutFile(task);
            taskLauncher.addParam("-o", outFile.getAbsolutePath());
            for (Param p : request.getParams()) {
                if (p.getName().startsWith("-")) {
                    taskLauncher.addParam(p.getName(), p.getValue());
                }
            }
            ExternalTaskHandlerImpl handler = new ExternalTaskHandlerImpl();
            taskLauncher.setHandler(handler);

            int status = taskLauncher.execute();

            if (status != ExternalTaskLauncher.NORMAL_EXIT) {
                throw new DataAccessException("Failed to obtain data. " + handler.getError());
            }

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
            if (isEmptyDir(workingDir)) {
                Files.deleteIfExists(workingDir);
            }
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


    java.nio.file.Path getWorkDir(String task) throws IOException {
        return Files.createTempDirectory(ServerContext.getExternalTempWorkDir().toPath(), task);
    }

    File getOutFile(String task) throws IOException {
        // expecting binary fits table
        return File.createTempFile(task, ".fits", ServerContext.getExternalPermWorkDir());
    }

    boolean isEmptyDir(Path dir) {
        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
            return (!stream.iterator().hasNext());
        } catch (Exception e) {
            return false;
        }
    }
}
