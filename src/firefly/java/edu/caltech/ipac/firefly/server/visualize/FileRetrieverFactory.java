/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.RequestType;

import java.util.HashMap;
import java.util.Map;
/**
 * User: roby
 * Date: Feb 26, 2010
 * Time: 10:45:12 AM
 */


/**
 * @author Trey Roby
 */
public final class FileRetrieverFactory {

    private static final FileRetrieverFactory _instance= new FileRetrieverFactory();
    private final Map<RequestType, FileRetriever> _types=
                      new HashMap<RequestType, FileRetriever>();

    private FileRetrieverFactory() {
        _types.put(RequestType.FILE,      new LocalFileRetriever());
        _types.put(RequestType.URL,       new URLFileRetriever());
        _types.put(RequestType.SERVICE,   new ServiceRetriever());
        _types.put(RequestType.ALL_SKY,   new AllSkyRetriever());
        _types.put(RequestType.PROCESSOR, new ProcessorFileRetriever());
        _types.put(RequestType.BLANK,     new BlankFileRetriever());
        _types.put(RequestType.TRY_FILE_THEN_URL, new TryFileThenURLRetriever());
    }

    public static FileRetriever getRetriever(WebPlotRequest request) {

        RequestType type;
        if (request.containsParam(WebPlotRequest.TYPE)) {
            type= request.getRequestType();
        }
        else {
            if (request.containsParam(WebPlotRequest.FILE))             type= RequestType.FILE;
            else if (request.containsParam(WebPlotRequest.URL))         type= RequestType.URL;
            else if (request.containsParam(WebPlotRequest.SURVEY_KEY))  type= RequestType.SERVICE;
            else if (request.containsParam(WebPlotRequest.SURVEY_KEY))  type= RequestType.SERVICE;
            else if (request.hasID())                                   type= RequestType.PROCESSOR;
            else                                                        type= RequestType.ALL_SKY;
        }
        return _instance._types.get(type);
    }

}

