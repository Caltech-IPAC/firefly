/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.FileHolder;
import edu.caltech.ipac.util.download.URLDownload;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Date: Jul 17, 2008
 *
 * @author loi
 * @version $Id: EhcacheProvider.java,v 1.28 2012/09/25 23:50:59 loi Exp $
 */
public class EhcacheProvider implements Cache.Provider {

    private static final Logger.LoggerImpl _log= Logger.getLogger();
    private static final net.sf.ehcache.CacheManager manager;
    private static net.sf.ehcache.CacheManager sharedManager;
    private static final boolean enableJMX = AppProperties.getBooleanProperty("ehcache.jmx.monitor", true);
    private static HashMap<String, Boolean> fileListenersReg = new HashMap<String, Boolean>();
    private static HashMap<String, Boolean> logListenersReg = new HashMap<String, Boolean>();
    private static long curConfModTime = 0;

    /*
      CLEANUP POLICY:
        - cached data expires after 60 minute of inactivity
        - force eviction check once every 1 minute.
     */
    private static int DEF_TTI_SEC       = 60*60;     // default vis.shared maximum time to idle in seconds before evicted
    private static int EVICT_CHECK_INTVL = 1;         // interval in minutes to check for expired vis.shared cache items

    static {

        URL url = null;
        File f = getConfFile("ehcache.xml");
        if (f != null && f.canRead()) {
            try {
                url = f.toURI().toURL();
            } catch (MalformedURLException e) {
                Logger.error(e, "bad ehcache file location: " + f.getAbsolutePath());
            }
            _log.info("cache manager config file: " + url);
        }

        _log.info("loading ehcache config file: " + url);

        manager = net.sf.ehcache.CacheManager.newInstance(url);


        // Due to the shared nature of this file, we only want to pick up the latest version.
        // Latest is based on when ehcache.xml is modified.
        if (f.lastModified() > curConfModTime) {
            curConfModTime = f.lastModified();

            File sharedConfig = getConfFile("shared_ehcache.xml");
            File ignoreSizeOf = getConfFile("ignore_sizeof.txt");
            System.setProperty("net.sf.ehcache.sizeof.filter", ignoreSizeOf.getAbsolutePath());

            // Two 2 tries to start cache manager:
            //   1. The first time will only work in the single app deployment such the firefly standalone version.
            //      Ehcahce is not deployed in the tomcat lib directory.
            //
            //   2. In the typical multi app production case there will be an exception, because ehcache is in the tomcat lib
            //      directory and has a different class loader. Then the cache manager will start without
            //      the sizeofEngine override. To use the sizeofEngine wrapper in multi app production case it would need to
            //      be a jar that is placed in the tomcat lib directory alone with EHcache.
            try {
                String sizeEngName= ObjectSizeEngineWrapper.class.getName();
                System.setProperty("net.sf.ehcache.sizeofengine.shared.VIS_SHARED_MEM", sizeEngName);
                sharedManager = CacheManager.create(sharedConfig.getAbsolutePath());
            } catch (RuntimeException e) {
                System.clearProperty("net.sf.ehcache.sizeofengine.shared.VIS_SHARED_MEM");
                sharedManager = CacheManager.create(sharedConfig.getAbsolutePath());
            }
            float pctVisSharedMemSize = AppProperties.getFloatProperty("pct.vis.shared.mem.size", 0F);

            // check to see if vis.shared.mem.size is setup in the environment or setup for auto-config.
            String sharedMemSize = System.getProperty("vis.shared.mem.size");
            if (!StringUtils.isEmpty(sharedMemSize) || pctVisSharedMemSize > 0) {
                if (StringUtils.isEmpty(sharedMemSize)) {
                    sharedMemSize =  String.format("%dM", (int)(Runtime.getRuntime().maxMemory() * pctVisSharedMemSize/1024/1024));
                }
                sharedManager.getCache(Cache.TYPE_VIS_SHARED_MEM).getCacheConfiguration().setMaxBytesLocalHeap(sharedMemSize);

                // setup cleanup task
                int ttiSecs = AppProperties.getIntProperty("vis.shared.tti.secs", DEF_TTI_SEC);  // defaults to expire after 60 mins of inactivity.
                sharedManager.getCache(Cache.TYPE_VIS_SHARED_MEM).getCacheConfiguration().setTimeToIdleSeconds(ttiSecs);
                Executors.newSingleThreadScheduledExecutor()
                        .scheduleAtFixedRate( () -> sharedManager.getCache(Cache.TYPE_VIS_SHARED_MEM).getKeysWithExpiryCheck()        // this forces eviction
                                              , EVICT_CHECK_INTVL, EVICT_CHECK_INTVL, TimeUnit.MINUTES);      // check every n minutes
            }
            _log.info("shared cache manager config file: " + sharedConfig);
        }


        if (enableJMX) {
//            // enable JMX monitoring for ehcache
//            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
//            ManagementService.registerMBeans(manager, mBeanServer, false, false, false, true);
        }
    }

    private static File getConfFile(String fname) {
        File confFile = ServerContext.getConfigFile(fname);
        if (confFile == null) {
            confFile = new File(ServerContext.getWebappConfigDir(), fname);
            try {
                URLDownload.getDataToFile(EhcacheImpl.class.getResource("/edu/caltech/ipac/firefly/server/cache/resources/" + fname), confFile);
            } catch (Exception e) {
                _log.error("Unable to extract " + fname + " from jar file");
            }
        }
        return confFile;
    }

    private static String[] findCleanupCacheTypes() {
        String ctypes[]= new String[0];
        String in= AppProperties.getProperty("ehcache.cleanup.cache.types", null);
        if (!StringUtils.isEmpty(in)) {
            ctypes= in.split(" ");
        }
        return ctypes;
    }

    public Cache getCache(String type) {
        EhcacheImpl cache;
        Ehcache ehcache = getEhcacheManager(type).getCache(type);
        if (ehcache == null) {
            throw new IllegalArgumentException("Unknow cache type.  Make sure cache type '" +
                    type + "' is defined in your ehcache.xml file");
        }
        if (type.equals(Cache.TYPE_PERM_FILE)) {
            cache = new FileCache(ehcache);
//            ensureFileEventListener(ehcache);
        } else if (type.equals(Cache.TYPE_TEMP_FILE)) {
            cache = new FileCache(ehcache);
//            ensureFileEventListener(ehcache);
        } else {
            cache = new EhcacheImpl(ehcache);
//            ensureLoggingEventListener(ehcache);
        }

        return cache;
    }

    public void shutdown() {
        if (manager != null) {
            manager.shutdown();
        }
    }

    public CacheManager getEhcacheManager() {
        return manager;
    }

    public CacheManager getSharedManager() {
        return sharedManager;
    }

    private net.sf.ehcache.CacheManager getEhcacheManager(String name) {
        if (name != null && name.equals(Cache.TYPE_VIS_SHARED_MEM)) {
            return sharedManager;
        } else {
            return manager;
        }
    }

    /**
     * make sure that a cache has a FileEventListener registered to it.
     * @param ehcache
     */
    private void ensureFileEventListener(Ehcache ehcache) {
        if (!fileListenersReg.containsKey(ehcache.getName())) {
            ehcache.getCacheEventNotificationService().registerListener(new FileEventListener(ehcache));
            fileListenersReg.put(ehcache.getName(), true);
        }
    }

    /**
     * make sure that a cache has a LoggingEventListener registered to it.
     * @param ehcache
     */
    private void ensureLoggingEventListener(Ehcache ehcache) {
        if (!logListenersReg.containsKey(ehcache.getName())) {
            ehcache.getCacheEventNotificationService().registerListener(new LoggingEventListener());
            logListenersReg.put(ehcache.getName(), true);
        }
    }

//====================================================================
//  inner classes
//====================================================================

    static class FileCache extends EhcacheImpl {

        public FileCache(Ehcache cache) {
            super(cache);
        }

        @Override
        public Object get(CacheKey key) {
            Object o = super.get(key);

            if (o==null) return null;
            else if (o instanceof File)         o= exist(key,(File)o);
            else if (o instanceof FileHolder)   o= exist(key,(FileHolder)o);
            else if (o instanceof File[])       o= exist(key,(File[])o);
            else if (o instanceof FileHolder[]) o= exist(key,(FileHolder[])o);
            else  o= null;

            return o;
        }

        private Object exist(CacheKey key, File f) {
            if (f != null && !f.exists()){
                super.cache.remove(key);
                f= null;
            }
            return f;
        }

        private Object exist(CacheKey key, FileHolder f) {
            Object retval= f;
            if (exist(key,f.getFile())==null) {
                retval= null;
            }
            return retval;
        }

        private Object exist(CacheKey key, File fAry[]) {
            Object retval= fAry;
            for(File f : fAry) {
                if (exist(key,f)==null) {
                    retval= null;
                    break;
                }
            }
            return retval;
        }

        private Object exist(CacheKey key, FileHolder fhAry[]) {
            Object retval= fhAry;
            for(FileHolder fh : fhAry) {
                if (exist(key,fh.getFile())==null) {
                    retval= null;
                    break;
                }
            }
            return retval;
        }


    }

    static class FileEventListener extends LoggingEventListener {
        private static final Logger.LoggerImpl logger = Logger.getLogger();
        private Ehcache cache;

        FileEventListener(final Ehcache ehcache) {
            this.cache = ehcache;
        }

        @Override
        public void notifyElementRemoved(Ehcache ehcache, Element element) throws CacheException {
            super.notifyElementRemoved(ehcache, element);
            deleteFile(element);
        }

        @Override
        public void notifyElementExpired(Ehcache ehcache, Element element) {
            super.notifyElementExpired(ehcache, element);
            deleteFile(element);
        }

        @Override
        public void notifyElementEvicted(Ehcache ehcache, Element element) {
            super.notifyElementEvicted(ehcache, element);
            deleteFile(element);
        }

        @Override
        public void notifyRemoveAll(Ehcache ehcache) {
            super.notifyRemoveAll(ehcache);
            deleteAllFiles(ehcache);
        }

        private void deleteAllFiles(Ehcache ehcache) {
            List keys = ehcache.getKeys();
            for (Object key : keys) {
                deleteFile(ehcache.get(key));
            }


        }

        private void deleteFile(Element el) {
            if(el != null && el.getValue()!=null) {
                Object val= el.getValue();
                if (val instanceof File) {
                    deleteFile((File)val);
                } else if (val instanceof FileHolder) {
                    deleteFile(((FileHolder)val).getFile());
                } else if (val instanceof FileHolder[]) {
                    for(FileHolder fh : (FileHolder[])val) deleteFile(fh.getFile());
                } else if (val instanceof File[]) {
                    for(File f : (File[])val) deleteFile(f);
                }
            }
        }

        private void deleteFile(File f) {
            if (f != null && f.exists()) {
                boolean success= f.delete();
                if (success) logger.debug("EHCACHE: delete file=" + f);
                else logger.warn("EHCACHE: FAILED to delete file: " + f);
            }
        }


    }


    static class LoggingEventListener implements CacheEventListener {
        private static final Logger.LoggerImpl logger = Logger.getLogger();

        public void notifyElementRemoved(Ehcache ehcache, Element element) throws CacheException {
            logEvent("Removed", ehcache, element);
        }

        public void notifyElementPut(Ehcache ehcache, Element element) throws CacheException {
            logEvent("Put", ehcache, element);
        }

        public void notifyElementUpdated(Ehcache ehcache, Element element) throws CacheException {
            logEvent("Updated", ehcache, element);
        }

        public void notifyElementExpired(Ehcache ehcache, Element element) {
            logEvent("Expired", ehcache, element);
//            ehcache.evictExpiredElements();
        }

        public void notifyElementEvicted(Ehcache ehcache, Element element) {
            logEvent("Evicted", ehcache, element);
        }

        public void notifyRemoveAll(Ehcache ehcache) {
            logEvent("RemoveAll", ehcache, null);
        }

        public void dispose() {
        }

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        private void logEvent(String event, Ehcache cache, Element element) {

//            logger.debug("EHCACHE event: " + event,
//                         "Cache Name: " + cache.getName(),
//                         "key-value: " + element.getKey() + "-" +
//                         StringUtils.toString(element.getValue()));
        }
    }
}
