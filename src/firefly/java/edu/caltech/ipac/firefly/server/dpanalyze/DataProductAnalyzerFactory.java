/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.dpanalyze;

import org.reflections.Reflections;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Trey Roby
 */
public final class DataProductAnalyzerFactory {

    private volatile static DataProductAnalyzerFactory instance;
    private final Map<String, DataProductAnalyzer> analyzerMap= new ConcurrentHashMap<>();
    private final DataProductAnalyzer identityAnalyzer= new IdentityAnalyzer();

    private DataProductAnalyzerFactory() {
        Reflections reflections = new Reflections("edu.caltech.ipac");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(DataProductAnalyzerImpl.class);

        for (Class<?> fileRetrieve: annotated) {
            DataProductAnalyzerImpl rAnna = fileRetrieve.getAnnotation(DataProductAnalyzerImpl.class);
            String requestId = rAnna.id();
            try {
                analyzerMap.put(requestId, (DataProductAnalyzer) fileRetrieve.newInstance());
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    private static DataProductAnalyzerFactory getInstance() {
        if (instance!=null) return instance;
        synchronized (DataProductAnalyzerFactory.class) {
            if (instance==null) instance= new DataProductAnalyzerFactory();
        }
        return instance;
    }


    public static DataProductAnalyzer getAnalyzer(String id) {
        return hasAnalyzer(id) ? getInstance().analyzerMap.get(id) : getInstance().identityAnalyzer;
    }

    public static boolean hasAnalyzer(String id) {
        if (id==null) return false;
        return getInstance().analyzerMap.containsKey(id);
    }

    public static void addAnalyzer(String key, DataProductAnalyzer fa) { getInstance().analyzerMap.put(key, fa); }

    private static class IdentityAnalyzer implements DataProductAnalyzer {};
}
