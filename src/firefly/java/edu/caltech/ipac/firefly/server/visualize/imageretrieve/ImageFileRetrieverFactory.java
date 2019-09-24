/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import org.reflections.Reflections;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Trey Roby
 * Date: Feb 26, 2010
 */
public final class ImageFileRetrieverFactory {

    private static final ImageFileRetrieverFactory _instance= new ImageFileRetrieverFactory();
    private final Map<RequestType, FileRetriever> _types= new HashMap<>();
    private final Map<String, FileRetriever> reqType= new HashMap<>();

    private ImageFileRetrieverFactory() {
        Reflections reflections = new Reflections("edu.caltech.ipac");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(FileRetrieverImpl.class);

        for (Class<?> fileRetrieve: annotated) {
            FileRetrieverImpl rAnna = fileRetrieve.getAnnotation(FileRetrieverImpl.class);
            String requestId = rAnna.id();
            try {
                reqType.put(requestId, (FileRetriever) fileRetrieve.newInstance());
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    public static FileRetriever getRetriever(WebPlotRequest request) {

        RequestType rType;
        if (request.containsParam(WebPlotRequest.TYPE)) {
            rType= request.getRequestType();
        }
        else {
            if (request.containsParam(WebPlotRequest.FILE))             rType= RequestType.FILE;
            else if (request.containsParam(WebPlotRequest.SURVEY_KEY))  rType= RequestType.SERVICE;
            else if (request.containsParam(WebPlotRequest.SERVICE))     rType= RequestType.SERVICE;
            else if (request.containsParam(WebPlotRequest.URL))         rType= RequestType.URL;
            else if (request.hasID())                                   rType= RequestType.PROCESSOR;
            else                                                        rType= RequestType.ALL_SKY;
        }

        String fileRetrieverKey= rType.toString();
        if (rType==RequestType.SERVICE && request.getServiceType()== WebPlotRequest.ServiceType.UNKNOWN) {
            fileRetrieverKey= request.getServiceTypeString();
        }
        return _instance.reqType.get(fileRetrieverKey);
    }

    public static void addRetriever(String key, FileRetriever retriever) {
        _instance.reqType.put(key, retriever);
    }
}
