package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.FileUtil;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */
public class ExternalTaskHandlerImpl implements ExternalTaskHandler {

    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    /**
        There might be debugging output in standard output stream
        Discard everything until STATUS_KEYWORD
     */
    private final String STATUS_KEYWORD = "___TASK STATUS___";

    /**
       A StringBuffer containing JSON with status info
       JSON is supposed to start on the following line after STATUS_KEYWORD
     */
    private StringBuffer status = new StringBuffer();
    private String error = null;
    private String outfile = null;
    private int statusCode = -1;

    @Override
    public void setup(ExternalTaskLauncher launcher, Map<String, String> env) throws InterruptedException {
    }

    @Override
    public void finish(int status) {
        statusCode = status;
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
                    Object errorObj = statusJSON.get("error");
                    if (errorObj != null) {
                        error = errorObj.toString();
                    }
                    Object outfileObj = statusJSON.get("outfile");
                    if (outfileObj != null) {
                        outfile = outfileObj.toString();
                    }

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

    public String getOutfile() {
        return outfile;
    }
}
