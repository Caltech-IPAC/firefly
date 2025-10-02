/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Trey Roby
 * Date: Feb 26, 2010
 */
public final class ImageFileRetrieverFactory {

    private static final ImageFileRetrieverFactory _instance= new ImageFileRetrieverFactory();
    private final Map<String, FileRetriever> reqType= new HashMap<>();

    private ImageFileRetrieverFactory() {
        Reflections reflections = new Reflections("edu.caltech.ipac");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(FileRetrieverImpl.class);

        for (Class<?> fileRetrieve: annotated) {
            FileRetrieverImpl rAnna = fileRetrieve.getAnnotation(FileRetrieverImpl.class);
            String requestId = rAnna.id();
            try {
                reqType.put(requestId, (FileRetriever) fileRetrieve.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                Logger.error(e, "Error instantiating FileRetrieverImpl");
            }
        }
    }

    public static FileRetriever getRetriever(WebPlotRequest request) {

        RequestType rType;
        if (request.containsParam(WebPlotRequest.TYPE)) {
            rType= request.getRequestType();
        }
        else {
            rType= guessRequestType(request);
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

    public static RequestType guessRequestType(WebPlotRequest r) {

        var hasURL= r.containsParam(WebPlotRequest.URL);
        var hasS3URI= r.containsParam(WebPlotRequest.S3_URI);
        var hasS3BucketKey= r.containsParam(WebPlotRequest.S3_BUCKET) && r.containsParam(WebPlotRequest.S3_KEY);
        var hasGcsBucketKey= r.containsParam(WebPlotRequest.GCS_BUCKET) && r.containsParam(WebPlotRequest.GCS_OBJ_NAME);


        if (r.containsParam(WebPlotRequest.FILE))             return RequestType.FILE;
        else if (r.containsParam(WebPlotRequest.SURVEY_KEY))  return RequestType.SERVICE;
        else if (r.containsParam(WebPlotRequest.SERVICE))     return RequestType.SERVICE;
        else if (hasURL && !hasS3URI && !hasS3BucketKey)      return RequestType.URI;
        else if (hasURL || hasS3URI || hasS3BucketKey)        return RequestType.URI;
        else if (hasGcsBucketKey)                             return RequestType.URI;
        else if (r.hasID())                                   return RequestType.PROCESSOR;
        else                                                  return RequestType.ALL_SKY;
    }
}
