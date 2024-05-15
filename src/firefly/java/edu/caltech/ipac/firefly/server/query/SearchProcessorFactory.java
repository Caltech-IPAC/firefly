/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import org.reflections.Reflections;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author tatianag
 * $Id: SearchProcessorFactory.java,v 1.7 2010/08/04 20:18:51 roby Exp $
 */
public class SearchProcessorFactory {

    static Map<String, Class> searchProcessors = null;

    public static void init() {
        if (searchProcessors == null) {
            try {
                searchProcessors = new LinkedHashMap<>();
                long cTime = System.currentTimeMillis();

                Reflections reflections = new Reflections("edu.caltech.ipac");
                Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(SearchProcessorImpl.class);

                for (Class<?> sproc : annotated) {
                    SearchProcessorImpl sprocAnna = sproc.getAnnotation(SearchProcessorImpl.class);
                    if (sprocAnna != null) {
                        String requestId = sprocAnna.id();
                        searchProcessors.put(requestId, sproc);
                    }
                }
                Logger.debug("Getting search processors took "+(System.currentTimeMillis()-cTime)+"ms");
                logSearchProcessors();
            } catch (Exception e) {
                Logger.error("FATAL ERROR: unable to get search processors: "+e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static SearchProcessor getProcessor(String requestId) {
        if (searchProcessors == null) {
            init();
        }
        Assert.argTst(searchProcessors != null, "Unable to get SearchProcessorFactory");
        try {
            return (SearchProcessor)searchProcessors.get(requestId).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to get query processor for "+requestId+": "+e.getMessage());
        }
    }

    private static void logSearchProcessors() {
        if (searchProcessors != null) {
            StringBuffer sb = new StringBuffer("\nSEARCH PROCESSORS (search request id - processor class)\n");
            for (String key : searchProcessors.keySet()) {
                sb.append("   \"").append(key).append("\" - ").append(searchProcessors.get(key).getName()).append("\n");
                Class c = searchProcessors.get(key);
                SearchProcessorImpl ann = (SearchProcessorImpl) c.getAnnotation(SearchProcessorImpl.class);
                if (ann != null) {
                    ParamDoc params[] = ann.params();
                    for (ParamDoc p : params) {
                        sb.append(String.format("%15s%-20s - %s%n", "param: ", p.name(), p.desc()));
                    }

                }
            }
            Logger.info(sb.toString());
        }
    }
}
