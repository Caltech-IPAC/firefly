/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.firefly.data.HasSizeOf;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.cache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.ResourceUnit;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.internal.statistics.DefaultStatisticsService;
import org.ehcache.core.spi.store.Store;
import org.ehcache.core.spi.store.heap.LimitExceededException;
import org.ehcache.core.spi.store.heap.SizeOfEngine;
import org.ehcache.core.spi.store.heap.SizeOfEngineProvider;
import org.ehcache.core.statistics.CacheStatistics;
import org.ehcache.expiry.ExpiryPolicy;
import org.ehcache.impl.internal.sizeof.DefaultSizeOfEngine;
import org.ehcache.spi.service.Service;
import org.ehcache.spi.service.ServiceConfiguration;
import org.ehcache.spi.service.ServiceProvider;

import java.io.File;
import java.time.Duration;

import static edu.caltech.ipac.firefly.server.cache.EhcacheImpl.makeExpiryPolicy;
import static edu.caltech.ipac.util.FileUtil.parseMemoryBytes;

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
     * ehcache cache aliases (formerly defined in ehcache.xml).
     */
    public static final String VIS_SHARED_MEM = "VIS_SHARED_MEM";
    public static final String PERM_SMALL = "PERM_SMALL";

    private static final Logger.LoggerImpl _log = Logger.getLogger();

    /*
     * Two separate CacheManagers:
     *   visManager  — VIS_SHARED_MEM only; uses CustomSizeOfEngine.Provider so byte-based heap
     *                 works and HasSizeOf objects are sized without reflection-based traversal.
     *   permManager — PERM_SMALL only; no custom SizeOfEngine so entry-count heap works, and
     *                 arbitrary objects (Semaphore, ConcurrentHashMap, …) are never deep-sized.
     */
    private static final CacheManager visManager;
    private static final CacheManager permManager;
    private static final DefaultStatisticsService visStats  = new DefaultStatisticsService();
    private static final DefaultStatisticsService permStats = new DefaultStatisticsService();

    /*
      CLEANUP POLICY for VIS_SHARED_MEM:
        - cached data expires after 60 minutes of inactivity (TTI)
     */
    private static final int DEF_TTI_SEC = 60 * 60;

    static {
        visManager = setupVisCacheManager();
        permManager = setupPermCacheManager();
    }

    public <T> Cache<T> getCache(String type) {
        org.ehcache.Cache<String, Object> ehcache = switch (type) {
            case VIS_SHARED_MEM -> visManager.getCache(type, String.class, Object.class);
            case PERM_SMALL     -> permManager.getCache(type, String.class, Object.class);
            default -> throw new IllegalArgumentException("Unknown cache type.  Make sure cache type '" +
                    type + "' is defined in EhcacheProvider.");
        };
        return new EhcacheImpl<>(ehcache);
    }

    public void shutdown() {
        visManager.close();
        permManager.close();
    }

    /** Returns live statistics for a named cache, or null if not found. */
    public CacheStatistics getCacheStats(String alias) {
        return switch (alias) {
            case VIS_SHARED_MEM -> visStats.getCacheStatistics(alias);
            case PERM_SMALL     -> permStats.getCacheStatistics(alias);
            default -> null;
        };
    }

    public CacheManager getVisManager()  { return visManager; }
    public CacheManager getPermManager() { return permManager; }

    //====================================================================
    // VIS_SHARED_MEM: byte-based heap with CustomSizeOfEngine to handle HasSizeOf objects
    //====================================================================
    private static CacheManager setupVisCacheManager() {
        // --- time-to-idle and heap size from properties ---
        int ttiSecs = AppProperties.getIntProperty("vis.shared.tti.secs", DEF_TTI_SEC);
        Duration ttiDuration = Duration.ofSeconds(ttiSecs);

        float pctVisSharedMemSize = AppProperties.getFloatProperty("pct.vis.shared.mem.size", 0F);
        String sharedMemSizeProp = System.getProperty("vis.shared.mem.size");
        long visHeapBytes = 100L * 1024 * 1024; // default 100 MB
        if (pctVisSharedMemSize > 0) {
            visHeapBytes = (long) (Runtime.getRuntime().maxMemory() * pctVisSharedMemSize);
        } else {
            long parsed = parseMemoryBytes(sharedMemSizeProp);
            if (parsed > 0) visHeapBytes = parsed;  // -1 means not set; keep default
        }

        final long finalVisHeapBytes = visHeapBytes;
        CacheManager manager = CacheManagerBuilder.newCacheManagerBuilder()
                .using(visStats)
                .using(new CustomSizeOfEngine.Provider(Long.MAX_VALUE, Long.MAX_VALUE))
                .withCache(VIS_SHARED_MEM,
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Object.class,
                                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                .heap(finalVisHeapBytes / 1024 / 1024, MemoryUnit.MB))
                                .withExpiry(makeExpiryPolicy(ttiDuration)))
                .build(true);

        _log.info("VisCacheManager initialized.  VIS_SHARED_MEM heap: %dMB, TTI: %ds".formatted(finalVisHeapBytes / 1024 / 1024, ttiSecs));

        return manager;
    }

    //====================================================================
    // PERM_SMALL: entry-count + persistent disk.
    //====================================================================
    private static CacheManager setupPermCacheManager() {
        // Disk store for PERM_SMALL persistence
        File diskDir = new File(System.getProperty("java.io.tmpdir"), "ehcache");
        PersistentCacheManager manager = CacheManagerBuilder.newCacheManagerBuilder()
                .using(permStats)
                .with(CacheManagerBuilder.persistence(diskDir))
                .withCache(PERM_SMALL,
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Object.class,
                                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                .heap(1000, EntryUnit.ENTRIES)
                                                .disk(500, MemoryUnit.MB, true))
                                .withExpiry(makeExpiryPolicy(ExpiryPolicy.INFINITE))
                                .withValueSerializer(ObjectSerializer.class))
                .build(true);

        _log.info("PermCacheManager initialized. PERM_SMALL disk: " + diskDir.getAbsolutePath());

        return manager;
    }

    //====================================================================
    // CustomSizeOfEngine: short-circuits sizing for HasSizeOf objects
    //====================================================================

    /**
     * A {@link SizeOfEngine} that short-circuits sizing for objects implementing {@link HasSizeOf}.
     * When the cached value (or a {@link EhcacheImpl.TtlValue} wrapping it) implements {@link HasSizeOf},
     * its {@code getSizeOf()} result is returned directly — bypassing the reflection-based
     * {@code UnsafeSizeOf}.
     *
     * For all other objects, sizing is delegated to {@link DefaultSizeOfEngine}.
     */
    private static class CustomSizeOfEngine implements SizeOfEngine {

        private final DefaultSizeOfEngine delegate;

        CustomSizeOfEngine(long maxObjectGraphSize, long maxObjectSize) {
            this.delegate = new DefaultSizeOfEngine(maxObjectGraphSize, maxObjectSize);
        }

        @Override
        public <K, V> long sizeof(K key, Store.ValueHolder<V> valueHolder) throws LimitExceededException {
            V value = valueHolder.get();

            // Unwrap TtlValue if present
            Object inner = (value instanceof EhcacheImpl.TtlValue<?> tv) ? tv.value() : value;

            if (inner instanceof HasSizeOf h) {
                return h.getSizeOf();
            }

            return delegate.sizeof(key, valueHolder);
        }

        static class Provider implements SizeOfEngineProvider {

            private final long maxObjectGraphSize;
            private final long maxObjectSize;

            Provider(long maxObjectGraphSize, long maxObjectSize) {
                this.maxObjectGraphSize = maxObjectGraphSize;
                this.maxObjectSize = maxObjectSize;
            }

            @Override
            public SizeOfEngine createSizeOfEngine(ResourceUnit resourceUnit, ServiceConfiguration<?, ?>... serviceConfigurations) {
                return new CustomSizeOfEngine(maxObjectGraphSize, maxObjectSize);
            }

            @Override
            public void start(ServiceProvider<Service> serviceProvider) {}

            @Override
            public void stop() {}
        }
    }
}
