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
import edu.caltech.ipac.util.cache.Cleanupable;
import edu.caltech.ipac.util.cache.FileHolder;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
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
    private static final int cleanupIntervalMin = AppProperties.getIntProperty("ehcache.cleanup.internal.minutes", 5);
    private static final String cleanupTypes[] = findCleanupCacheTypes();
//    private static final MemCleanup cleanup= new MemCleanup();
    private static HashMap<String, Boolean> fileListenersReg = new HashMap<String, Boolean>();
    private static HashMap<String, Boolean> logListenersReg = new HashMap<String, Boolean>();

    static {
        URL url = null;
        File f = ServerContext.getConfigFile("ehcache.xml");
        if (f != null && f.canRead()) {
            try {
                url = f.toURI().toURL();
            } catch (MalformedURLException e) {
                Logger.error(e, "bad ehcache file location: " + f.getAbsolutePath());
            }
            _log.info("cache manager config file: " + url);
        }
        if (url == null) {
            url = EhcacheImpl.class.getResource("/edu/caltech/ipac/firefly/server/cache/resources/ehcache.xml");
        }

        _log.info("loading ehcache config file: " + url);

        manager = net.sf.ehcache.CacheManager.newInstance(url);


        File sharedConfig = ServerContext.getConfigFile("shared_ehcache.xml");
        if (sharedConfig == null) {
            sharedConfig = new File(ServerContext.getWebappConfigDir(), "shared_ehcache.xml");
            try {
                URLDownload.getDataToFile(EhcacheImpl.class.getResource("/edu/caltech/ipac/firefly/server/cache/resources/shared_ehcache.xml"), sharedConfig);
            } catch (Exception e) {
                _log.error("Unable to extract shared_ehcache.xml from jar file");
            }
        }

        File ignoreSizeOf = ServerContext.getConfigFile("ignore_sizeof.txt");
        if (ignoreSizeOf == null) {
            ignoreSizeOf = new File(ServerContext.getWebappConfigDir(), "ignore_sizeof.txt");
            try {
                URLDownload.getDataToFile(EhcacheImpl.class.getResource("/edu/caltech/ipac/firefly/server/cache/resources/ignore_sizeof.txt"), ignoreSizeOf);
            } catch (Exception e) {
                _log.error("Unable to extract ignore_sizeof.txt from jar file");
            }
        }

        if (sharedConfig.exists()) {
            if (ignoreSizeOf.exists()) {
                System.setProperty("net.sf.ehcache.sizeof.filter", ignoreSizeOf.getAbsolutePath());
            } else {
                _log.error("Unable to locate ignore_sizeof.txt file.");
            }
            String sharedMemSize = System.getProperty("vis.shared.mem.size");

            sharedManager = CacheManager.create(sharedConfig.getAbsolutePath());
            if (!StringUtils.isEmpty(sharedMemSize)) {
                sharedManager.getCache(Cache.TYPE_VIS_SHARED_MEM).getCacheConfiguration().setMaxBytesLocalHeap(sharedMemSize);
            }
            _log.info("shared cache manager config file: " + sharedConfig);
        }


//        if (cleanupTypes.length>0)  cleanup.start();

        if (enableJMX) {
//            // enable JMX monitoring for ehcache
//            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
//            ManagementService.registerMBeans(manager, mBeanServer, false, false, false, true);
        }
    }

    private static String[] findCleanupCacheTypes() {
        String ctypes[]= new String[0];
        String in= AppProperties.getProperty("ehcache.cleanup.cache.types", null);
        if (!StringUtils.isEmpty(in)) {
            ctypes= in.split(" ");
        }
        return ctypes;
    }

    public Cache getSharedCache(String type) {
        EhcacheImpl cache;
        Ehcache ehcache = sharedManager.getCache(type);
        if (ehcache == null) {
            throw new IllegalArgumentException("Unknow cache type.  Make sure cache type '" +
                    type + "' is defined in your shared_ehcache.xml file");
        }
        cache = new EhcacheImpl(ehcache);
        ensureLoggingEventListener(ehcache);
        return cache;
    }

    public Cache getCache(String type) {
        EhcacheImpl cache;
        Ehcache ehcache = manager.getCache(type);
        if (ehcache == null) {
            throw new IllegalArgumentException("Unknow cache type.  Make sure cache type '" +
                    type + "' is defined in your ehcache.xml file");
        }
        if (type.equals(Cache.TYPE_PERM_FILE)) {
            cache = new FileCache(ehcache);
            ensureFileEventListener(ehcache);
        } else if (type.equals(Cache.TYPE_TEMP_FILE)) {
            cache = new FileCache(ehcache);
            ensureFileEventListener(ehcache);
        } else {
            cache = new EhcacheImpl(ehcache);
            ensureLoggingEventListener(ehcache);
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


    private static class MemCleanup implements Runnable {


        private volatile Thread _thread= null;

        MemCleanup() {
        }

        public void stop() {
            synchronized (this) {
                Thread t= _thread;
                _thread= null;
                if (t!=null) t.interrupt();
            }
        }

        public void start() {
            synchronized (this) {
                if (_thread==null) {
                    _thread= new Thread(this, "EhcacheProvider-cleanup");
                }
                _thread.setDaemon(true);
                _thread.start();
            }
        }

        public void run() {
            try {
                boolean loop = true;
                while (loop) {

                    Element el;
                    Ehcache ehcache;
                    List list;
                    long cleanUpTime = System.currentTimeMillis() - (cleanupIntervalMin * 1000 * 60);
                    for (String ctype : cleanupTypes) {
                        ehcache = manager.getCache(ctype);
                        list = ehcache.getKeys();
//                        Logger.briefDebug("MemCleanup awake, list size: " + list.size());
                        for (Object key : list) {
                            el = ehcache.getQuiet(key);
                            if (el != null && el.getValue() instanceof Cleanupable &&
                                    el.getLastAccessTime() < cleanUpTime) {
                                ((Cleanupable) el.getValue()).cleanup();
                            }
                        }
                    }

                    synchronized (this) {
                        loop = (_thread != null);
                    }

                    if (loop) {
                        try {
                            TimeUnit.SECONDS.sleep(120);
                        } catch (InterruptedException e) { /* ignore*/ }
                    }


                    synchronized (this) {
                        loop = (_thread != null);
                    }

                }
            } catch (Throwable e) {
                _log.error(e, "MemCleanup encountered unexpected exception.  This should not happen.");
            }
            _log.briefDebug("MemCleanup exiting");
        }
    }

}
