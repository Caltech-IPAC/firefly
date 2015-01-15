/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.firefly.server.util.Logger;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.lang.annotation.Annotation;
import java.net.URL;

import org.apache.xbean.finder.ClassFinder;

/**
 * @author tatianag
 * $Id: SearchProcessorFactory.java,v 1.7 2010/08/04 20:18:51 roby Exp $
 */
public class SearchProcessorFactory {

    public final static String FACTORY_CLASS_PROP = AppProperties.getProperty("SearchProcessor.factory.class");
    public final static String MANIFEST_MARKER_ATTRIBUTE = "hasSearchProcessors";

    static Map<String, Class> searchProcessors = null;

    public static void init() {
        if (searchProcessors == null) {
            try {
                long cTime = System.currentTimeMillis();
                List<URL> jarsWithSearchProcessors = FileUtil.getJarsWithManifestEntry(MANIFEST_MARKER_ATTRIBUTE);
                ClassLoader classLoader = SearchProcessorFactory.class.getClassLoader();

                ClassFinder classFinder;
                if (jarsWithSearchProcessors.size() < 1) {
                    Logger.error("No jars with manifest entry "+MANIFEST_MARKER_ATTRIBUTE);
                    classFinder = new ClassFinder(classLoader);
                } else {
                    classFinder = new ClassFinder(classLoader, jarsWithSearchProcessors);
                }
                List<Class> annotatedClasses = classFinder.findAnnotatedClasses(SearchProcessorImpl.class);
                if (annotatedClasses.size() == 0) {
                    Logger.error("Fail to find any SearchProcessor.  This is not normal.");
                }

                searchProcessors = new LinkedHashMap<String, Class>();
                Annotation ann;
                String requestId;
                for (Class c : annotatedClasses) {
                    ann = c.getAnnotation(SearchProcessorImpl.class);
                    if (ann instanceof SearchProcessorImpl) {
                        SearchProcessorImpl srpAnn= (SearchProcessorImpl)ann;
                        requestId = srpAnn.id();
                        searchProcessors.put(requestId, c);
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
        if (searchProcessors == null) {
            init();
        }
        StringBuffer sb = new StringBuffer("\nSEARCH PROCESSORS (search request id - processor class)\n");
        for (String key : searchProcessors.keySet()) {
            sb.append("   \"").append(key).append("\" - ").append(searchProcessors.get(key).getName()).append("\n");
            Class c= searchProcessors.get(key);
            SearchProcessorImpl ann = (SearchProcessorImpl)c.getAnnotation(SearchProcessorImpl.class);
            if (ann!=null) {
                ParamDoc params[]= ann.params();
                for(ParamDoc p : params) {
                    sb.append(String.format("%15s%-20s - %s%n","param: ",p.name(),p.desc()));
                }

            }
        }
        Logger.info(sb.toString());
    }
}
