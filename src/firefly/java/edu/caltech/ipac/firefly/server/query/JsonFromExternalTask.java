package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.firefly.server.ExternalTaskHandler;
import edu.caltech.ipac.firefly.server.ExternalTaskHandlerImpl;
import edu.caltech.ipac.firefly.server.ExternalTaskLauncher;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.FileUtil;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */
@SearchProcessorImpl(id = "JsonFromExternalTask")
public class JsonFromExternalTask extends JsonStringProcessor {

    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();

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
    public String fetchData(ServerRequest request) throws DataAccessException {
        String launcher = request.getParam(ExternalTaskHandler.LAUNCHER);
        if (launcher == null) {
            throw new DataAccessException(ExternalTaskHandler.LAUNCHER+" parameter is not found in request.");
        }
        ExternalTaskLauncher taskLauncher = new ExternalTaskLauncher(launcher);

        String jsonText = null;
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

            jsonText = FileUtil.readFile(outFile);

            // validate JSON and replace file paths with prefixes
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(jsonText);
            Object json = ServerContext.replaceWithPrefixes(obj);
            jsonText = JSONValue.toJSONString(json);
            return jsonText;
        } catch(ParseException pe){
            LOGGER.error(getUniqueID(request) + " Can not parse returned JSON: " + pe.toString() + "\n" + jsonText);
            throw new DataAccessException(request.getRequestId()+" Can not parse returned JSON: " + pe.toString());
        } catch (Exception e) {
            LOGGER.error(e, "Unable get to data from external task: "+request.toString());
            throw new DataAccessException("Unable to get data from external task: "+e.getMessage());
        }
    }
}
