/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static edu.caltech.ipac.util.cache.Cache.*;
import static edu.caltech.ipac.util.cache.CacheManager.EmptyCache;

/**
 * 11/21/2024
 * With the move to containerized deployments, each App Server instance will typically host only one application.
 * Additionally, we are no longer replicating cache buckets. These changes allow us to simplify the caching strategy.
 * It is no longer necessary to differentiate between cache buckets shared across applications within the same App Server
 * and those that are not. Instead, we can assume a single shared cache for all applications.
 * <p/>
 * Created: Jul 17, 2008
 * @author loi
 * @version $Id: EhcacheProvider.java,v 1.28 2012/09/25 23:50:59 loi Exp $
 */
public class EhcacheProvider implements Cache.Provider {

    /*
     * ehcache buckets defined in ehcache.xml configuration file.
     */
    public static final String VIS_SHARED_MEM = "VIS_SHARED_MEM";
    public static final String PERM_SMALL = "PERM_SMALL";

    private static final Logger.LoggerImpl _log= Logger.getLogger();
    private static net.sf.ehcache.CacheManager manager = null;
    private static final boolean enableJMX = AppProperties.getBooleanProperty("ehcache.jmx.monitor", true);
    private static long curConfModTime = 0;

    /*
      CLEANUP POLICY:
        - cached data expires after 60 minute of inactivity
        - force eviction check once every 1 minute.
     */
    private static final int DEF_TTI_SEC       = 60*60;     // default vis.shared maximum time to idle in seconds before evicted
    private static final int EVICT_CHECK_INTVL = 1;         // interval in minutes to check for expired vis.shared cache items

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

        // Due to the shared nature of this file, we only want to pick up the latest version.
        // Latest is based on when ehcache.xml is modified.
        if (url != null && (manager == null || f.lastModified() > curConfModTime)) {
            curConfModTime = f.lastModified();
            _log.info("loading ehcache config file: " + url);

            File ignoreSizeOf = ServerContext.getConfigFile("ignore_sizeof.txt");
            System.setProperty("net.sf.ehcache.sizeof.filter", ignoreSizeOf.getAbsolutePath());

            try {
                String sizeEngName= ObjectSizeEngineWrapper.class.getName();
                System.setProperty("net.sf.ehcache.sizeofengine.localCache.VIS_SHARED_MEM", sizeEngName);
                manager = net.sf.ehcache.CacheManager.newInstance(url);
            } catch (RuntimeException e) {
                _log.error(e, "unable to create net.sf.ehcache.CacheManager");
                throw new CacheException("unable to create net.sf.ehcache.CacheManager");
            }
            float pctVisSharedMemSize = AppProperties.getFloatProperty("pct.vis.shared.mem.size", 0F);

            // check to see if vis.shared.mem.size is setup in the environment or setup for auto-config.
            String sharedMemSize = System.getProperty("vis.shared.mem.size");
            if (!StringUtils.isEmpty(sharedMemSize) || pctVisSharedMemSize > 0) {
                if (StringUtils.isEmpty(sharedMemSize)) {
                    sharedMemSize =  String.format("%dM", (int)(Runtime.getRuntime().maxMemory() * pctVisSharedMemSize/1024/1024));
                }
                manager.getCache(VIS_SHARED_MEM).getCacheConfiguration().setMaxBytesLocalHeap(sharedMemSize);

                // setup cleanup task
                int ttiSecs = AppProperties.getIntProperty("vis.shared.tti.secs", DEF_TTI_SEC);  // defaults to expire after 60 mins of inactivity.
                manager.getCache(VIS_SHARED_MEM).getCacheConfiguration().setTimeToIdleSeconds(ttiSecs);
                ServerContext.SCHEDULE_TASK_EXEC.scheduleAtFixedRate(
                                () -> manager.getCache(VIS_SHARED_MEM).getKeysWithExpiryCheck(),       // forces expiry check
                                EVICT_CHECK_INTVL,
                                EVICT_CHECK_INTVL,
                                TimeUnit.MINUTES);      // check every EVICT_CHECK_INTVL minutes
            }
        }


        if (enableJMX) {
//            // enable JMX monitoring for ehcache
//            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
//            ManagementService.registerMBeans(manager, mBeanServer, false, false, false, true);
        }
    }

    public <T> Cache<T> getCache(String type) {
        Ehcache ehcache = getEhcacheManager(type).getCache(type);
        if (ehcache == null) {
            throw new IllegalArgumentException("Unknown cache type.  Make sure cache type '" +
                    type + "' is defined in your ehcache.xml file");
        }
        return new EhcacheImpl<>(ehcache);
    }

    public void shutdown() {
        if (manager != null) {
            manager.shutdown();
        }
    }

    public CacheManager getEhcacheManager() {
        return manager;
    }

    private net.sf.ehcache.CacheManager getEhcacheManager(String name) {
        return manager;
    }

}
