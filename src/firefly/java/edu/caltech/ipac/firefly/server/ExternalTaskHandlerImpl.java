package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.FileUtil;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */
public class ExternalTaskHandlerImpl implements ExternalTaskHandler {

    /**
        There might be debugging result in standard result stream
        Discard everything until STATUS_KEYWORD
     */
    private final static String STATUS_KEYWORD = "___TASK STATUS___";
    private final static String OUTFILE_KEY = "outfile";
    private final static String ERROR_KEY = "error";
    private final static String RESULT_KEY = "result";


    private String task;
    private String taskParams; // JSON task parameters as a string
    private Path workingDir;
    private File jsonParamsFile;

    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    /**
       A StringBuffer containing JSON with status info
       JSON is supposed to start on the following line after STATUS_KEYWORD
     */
    private StringBuffer status = new StringBuffer();
    private String error = null;
    private String outfile = null;
    private String result = null;
    private int statusCode = -1;

    public ExternalTaskHandlerImpl(String task, String jsonTaskParams) {
        this.workingDir = null;
        this.jsonParamsFile = null;
        this.task = task;
        this.taskParams = jsonTaskParams;
    }

    @Override
    public void setup(ExternalTaskLauncher launcher, Map<String, String> env) throws InterruptedException {
        try {
            launcher.addParam("-n", task);
            if (taskParams != null) {
                // resolve path prefixes in taskParams
                Object parsedParams = JSONValue.parseWithException(taskParams);
                Object paramsResolved = ServerContext.convertFilePaths(parsedParams);
                taskParams = JSONValue.toJSONString(paramsResolved);

                jsonParamsFile = File.createTempFile(task, ".json", ServerContext.getTempWorkDir());
                FileUtil.writeStringToFile(jsonParamsFile, taskParams);
                launcher.addParam("-i", jsonParamsFile);
            }

            workingDir = createWorkDir(task);
            launcher.setWorkDir(workingDir.toFile());
            launcher.addParam("-d", workingDir.toString());
            launcher.addParam("-o", ServerContext.getExternalPermWorkDir());

        } catch (Exception e) {
            String err = null;
            if (e instanceof ParseException) {
                ParseException pe = (ParseException)e;
                err = "Invalid JSON in taskParams: " + pe.toString() + " " +taskParams;
                LOGGER.error(e, err);
            } else {
                LOGGER.error(e);
            }
            throw new InterruptedException(task+" launcher setup failed: " + (err==null?e.getMessage():err));
        }
    }

    @Override
    public void finish(int status) {
        statusCode = status;
        // cleanup created directory and input file if work directory is empty
        // this is a precaution: if the task is not allowed to be executed
        // there should be no leftovers
        if (workingDir != null && isEmptyDir(workingDir)) {
            try {
                Files.deleteIfExists(workingDir);
            } catch (Exception e) {
                LOGGER.warn("Unable to delete "+workingDir.toString());
            }
            if (jsonParamsFile != null) {
                try {
                    Files.deleteIfExists(jsonParamsFile.toPath());
                } catch (Exception e) {
                    LOGGER.warn("Unable to delete "+jsonParamsFile.toString());
                }
            }
        }
    }

    @Override
    public void handleOut(InputStream is) throws InterruptedException {
        BufferedReader reader= new BufferedReader(new InputStreamReader(is));
        boolean collectStatus = false;
        try {
            for (String line = reader.readLine(); (line != null); line = reader.readLine()) {
                if (collectStatus) {
                    status.append(line);
                } else {
                    if (line.contains(STATUS_KEYWORD)) {
                        collectStatus = true;
                    } else {
                        LOGGER.debug(line);
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger().error(e);
            throw new InterruptedException("ExternalTask out handler: "+e.getMessage());
        } finally {
            FileUtil.silentClose(reader);
            if (status.length() > 0) {
                JSONObject statusJSON = (JSONObject) JSONValue.parse(status.toString());
                if (statusJSON != null) {
                    Object errorObj = statusJSON.get(ERROR_KEY);
                    if (errorObj != null) {
                        error = errorObj.toString();
                    }
                    Object outfileObj = statusJSON.get(OUTFILE_KEY);
                    if (outfileObj != null) {
                        outfile = outfileObj.toString();
                    } else {
                        Object resultObj = statusJSON.get(RESULT_KEY);
                        if (resultObj != null) {
                            result = resultObj.toString();
                        } else {
                            result = status.toString();
                        }
                    }
                } else {
                    setErrorIfEmpty("Unable to parse task status.");
                }
            } else {
                if (collectStatus) {
                    setErrorIfEmpty("No lines after "+STATUS_KEYWORD+".");
                } else {
                    setErrorIfEmpty("No "+STATUS_KEYWORD+" in external task standard output.");
                }

            }

        }
    }

    @Override
    public void handleError(InputStream is) throws InterruptedException {
        BufferedReader reader= new BufferedReader(new InputStreamReader(is));
        try {
            for (String line = reader.readLine(); (line != null); line = reader.readLine()) {
                LOGGER.warn(line);
                addError(line);
            }
        } catch (Exception e) {
            Logger.getLogger().error(e);
            throw new InterruptedException("ExternalTask error handler: "+e.getMessage());
        } finally {
            FileUtil.silentClose(reader);
        }

    }

    @Override
    public boolean abortExecution() {
        return false;
    }

    public String getError() {
        if (statusCode == ExternalTaskLauncher.ABORTED_BY_INTERRUPT) {
            return "Aborted by interrupt";
        } else {
            return error;
        }
    }

    public File getOutfile() throws DataAccessException {
        if (statusCode != ExternalTaskLauncher.NORMAL_EXIT) {
            throw new DataAccessException("Failed to obtain data. " + getError());
        } else {
            if (outfile == null) {
                throw new DataAccessException("Output file is not returned from the task.");
            } else {
                File ofile = new File(outfile);
                if (!ofile.canRead()) {
                    throw new DataAccessException("No read access to " + outfile);
                }
                return ofile;
            }
        }
    }

    public String getResult() throws DataAccessException {
        if (statusCode != ExternalTaskLauncher.NORMAL_EXIT) {
            throw new DataAccessException("Failed to obtain data. " + getError());
        } else {
            return result;
        }
    }


//============================================================================
//---------------------------- Private / Protected Methods -------------------
//============================================================================


    private void setErrorIfEmpty(String error) {
        if (this.error == null || this.error.trim().length() == 0) {
            this.error = error;
        }
    }

    private void addError(String error) {
        if (this.error == null || this.error.trim().length() == 0) {
            this.error = "";
        } else {
            this.error += " ";
        }
        this.error += error;
    }

    private static java.nio.file.Path createWorkDir(String task) throws IOException {
        return Files.createTempDirectory(ServerContext.getExternalTempWorkDir().toPath(), task);
    }


    private boolean isEmptyDir(Path dir) {
        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
            return (!stream.iterator().hasNext());
        } catch (Exception e) {
            return false;
        }
    }


}
