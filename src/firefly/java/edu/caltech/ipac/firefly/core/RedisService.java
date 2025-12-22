/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core;

import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.servlets.ServerStatus;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ConnectionActivatedEvent;
import io.lettuce.core.event.connection.ConnectionDeactivatedEvent;
import io.lettuce.core.resource.DefaultClientResources;
import redis.embedded.RedisServer;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.core.background.JobManager.*;
import static edu.caltech.ipac.firefly.server.cache.DistributedCache.DEF_TTL;
import static edu.caltech.ipac.util.StringUtils.isEmpty;
import static edu.caltech.ipac.util.StringUtils.isUUID;

/**
 * RedisService provides a connection management interface for interacting with a Redis server.
 * This service is responsible for establishing and managing the Redis connection,
 * checking the status of the connection, and ensuring that the connection is healthy.
 *
 * <p>The class abstracts the complexity of connecting to Redis, allowing other components of
 * the application to interact with Redis without worrying about low-level connection details.</p>
 *
 * <p>Common use cases include checking the availability of the Redis server, monitoring
 * connection health, and ensuring proper connectivity before performing Redis operations.</p>
 * Connections:
 *  • mainConn     – regular GET/SET/HGET/HSET operations
 *  • scanConn     – long-running SCAN/HSCAN operations
 *  • subPubConn   – one-time subscription for Pub/Sub messaging.  Do not use for anything else.
 *
 * Date: 2024-11-18
 *
 * @author loi
 * @version $Id: $
 */
public class RedisService {

    public static final long SCAN_BATCH_SIZE = AppProperties.getLongProperty("redis.scan.batch.size", 10_000);      // batch size for scanning keys in Redis.  Default to 10,000.  Larger value return more keys per call but use more CPU and memory per iteration.  this is a good size for larger redis store.
    private static final String REDIS_HOST = AppProperties.getProperty("redis.host", "localhost");
    private static final int REDIS_PORT = AppProperties.getIntProperty("redis.port", 6379);
    private static final String DB_DIR = AppProperties.getProperty("redis.db.dir", System.getProperty("java.io.tmpdir") + "/redis");
    private static final String REDIS_PASSWORD = getRedisPassword();
    private static final String LOCAL_MAX_MEM = AppProperties.getProperty("local.redis.max.mem", "128M");
    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final CacheKey VersionKey = new StringKey(SchemaVersion.class.getSimpleName());
    private enum SchemaVersion { V1_0, V1_1 }


    private static Instant failSince;

    private static RedisClient client;
    private static StatefulRedisConnection<String, byte[]> mainConn;
    private static StatefulRedisConnection<String, byte[]> scanConn;
    private static StatefulRedisPubSubConnection<String, byte[]> subPubConn;

    private static final List<String> RESERVED_KEYS = List.of(ALL_JOB_CACHE_KEY);
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicBoolean localStartupTriggered = new AtomicBoolean(false);
    private static RedisCodec<String, byte[]> codec =
            RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    public static synchronized void init() throws Exception {
        if (initialized.getAndSet(true)) return;

        LOG.info("Initializing RedisService; waiting for connection to establish...");
        RedisURI.Builder uriBuilder = RedisURI.builder()
                .withHost(REDIS_HOST)
                .withPort(REDIS_PORT);
        if (REDIS_PASSWORD != null && !REDIS_PASSWORD.isBlank()) {
            uriBuilder.withPassword(REDIS_PASSWORD.toCharArray());
        }

        client = RedisClient.create(DefaultClientResources.create(), uriBuilder.build());
        client.setOptions(ClientOptions.builder()
                .autoReconnect(true)            // Exponential delay begins with 1s and is capped at 30s
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS) // queue commands while disconnected
                .pingBeforeActivateConnection(true)
                .socketOptions(SocketOptions.builder()
                        .keepAlive(true) // enable TCP keepalive packets
                        .build())
                .build());

        // subscribe to Lettuce connection events → update setConnectionOk()
        EventBus bus = client.getResources().eventBus();
        bus.get().subscribe(event -> {
            if (event instanceof ConnectionActivatedEvent ev) {
                LOG.info("Redis connection established");
                setConnectionOk(true);
            } else if (event instanceof ConnectionDeactivatedEvent) {
                LOG.info("Redis connection lost");
                setConnectionOk(false);
            }
        });

        // Wait until connected; Redis is required for proper operation.
        // Once connected, Lettuce will auto-reconnect as needed.
        long delaySec = 1;
        while (!isConnected()) {
            try {
                tryConnect();
                setConnectionOk(true);
                LOG.info("Connected to Redis at " + REDIS_HOST + ":" + REDIS_PORT);
            } catch (Exception e) {
                LOG.error(e, "Unable to connect to Redis, retrying after a short delay...");
                Thread.sleep(delaySec);
                delaySec = Math.min(delaySec * 2, 30); // exponential backoff
            }
        }

        // perform data migration when needed
        dataMigration();

    }

    private static void tryConnect() throws Exception{
        try {
            mainConn = client.connect(codec);
            scanConn = client.connect(codec);
            subPubConn = client.connectPubSub(codec);
            LOG.info("Lettuce connections created. Auto-reconnect enabled.");
        } catch (Exception e) {
            LOG.error("Error connecting to Redis...");
            if ("localhost".equals(REDIS_HOST) && !localStartupTriggered.getAndSet(true)) {  // start embedded just once
                    LOG.warn("Redis not running locally — starting embedded Redis...");
                    startLocal();
                    tryConnect();
            } else {
                throw e;  // re-throw to trigger retry
            }
        }
    }

    private static void startLocal() throws Exception {
        new File(DB_DIR).mkdirs();
        RedisServer localRedis = RedisServer.newRedisServer()
                .port(REDIS_PORT)
                .setting("maxmemory %s".formatted(LOCAL_MAX_MEM))
                .setting("dir %s".formatted(DB_DIR))
                .setting("dbfilename redis.rdb")
                .setting("save 600 1")
                .build();
        localRedis.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                localRedis.stop();
                LOG.info("Local Redis stopped.");
            } catch (Exception e) {
                LOG.error(e, "Failed to stop local Redis");
            }
        }));
        Thread.sleep(2_000);  // wait a bit for Redis to start
        LOG.info("Local Redis started on port " + REDIS_PORT);
    }

    public static boolean isConnected() {
        return mainConn != null && mainConn.isOpen() &&
                scanConn != null && scanConn.isOpen() &&
                subPubConn != null && subPubConn.isOpen();
    }

    // -------------------------------------------------------------------------
    // Connection accessors
    // -------------------------------------------------------------------------

    public static StatefulRedisConnection<String, byte[]> mainConn() throws Exception {
        return checkConn(mainConn);
    }

    public static StatefulRedisConnection<String, byte[]> scanConn() throws Exception {
        return checkConn(scanConn);
    }

    public static StatefulRedisPubSubConnection<String, byte[]> pubSubConn() throws Exception {
        return checkConn(subPubConn);
    }

    private static <T extends StatefulRedisConnection<String, byte[]>> T checkConn(T conn)
            throws Exception {
        if (conn == null) init();
        if (conn != null && conn.isOpen()) return conn;
        throw new RedisConnectionException("Redis connection not available");
    }

    // -------------------------------------------------------------------------
    // Status & cleanup
    // -------------------------------------------------------------------------

    public static synchronized void teardown() {
        LOG.info("Disconnecting RedisService...");
        try {
            if (subPubConn != null) subPubConn.close();
            if (scanConn != null) scanConn.close();
            if (mainConn != null) mainConn.close();
            if (client != null) client.shutdown();
        } catch (Exception e) {
            LOG.error(e, "Error closing Redis connections");
        } finally {
            subPubConn = null;
            scanConn = null;
            mainConn = null;
            client = null;
            initialized.set(false);
            localStartupTriggered.set(false);
        }
    }

    private static String getRedisPassword() {
        String passwd = System.getenv("REDIS_PASSWORD");
        if (passwd == null) passwd = AppProperties.getProperty("REDIS_PASSWORD");
        return passwd;
    }

    // -------------------------------------------------------------------------
    // Monitoring / health
    // -------------------------------------------------------------------------
    
    public static void setConnectionOk(boolean ok) {
        if (ok == (failSince == null)) return;
        failSince = ok ? null : Instant.now();
        sendConnectionStatus();     // notify clients of the update
    }

    public static void sendConnectionStatusIfFailed () {
        if (failSince != null) sendConnectionStatus();          // probably not needed anymore
    }

    public static void sendConnectionStatus() {
        boolean lost = failSince != null;
        String reason = lost ? "A critical system component is currently unavailable" : "";

        FluxAction action = new FluxAction("app_data.appUpdate");
        action.setValue(lost, "connectionStatus", "lost");
        action.setValue(reason, "connectionStatus", "reason");
        ServerEventManager.processEvent(ServerEventManager.convertTo(action, ServerEvent.Scope.WORLD));
    }

    public static String getRedisHostPortDesc() {
        String status = failSince == null ? "OK" :
                "Failed since " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault()).format(failSince);
        return REDIS_HOST + ":" + REDIS_PORT + " (" + status + ")";
    }

    // -------------------------------------------------------------------------
    // Maintenance utilities
    // -------------------------------------------------------------------------

    public static long cleanupStaleKeys() {
        long deleted = 0;

        try {
            var redis = scanConn().sync();
            ScanArgs scanArgs = new ScanArgs().limit(5000); // same as JOB_SCAN_BATCH_SIZE if you want
            ScanCursor cursor = ScanCursor.INITIAL;

            do {
                // Scan synchronously
                KeyScanCursor<String> scan = redis.scan(cursor, scanArgs);
                List<String> keys = scan.getKeys();

                for (String key : keys) {
                    if (RESERVED_KEYS.contains(key)) continue; // skip reserved keys

                    Long ttl = redis.ttl(key);
                    Long idle = redis.objectIdletime(key);

                    // ttl == -1 means no expiry
                    // idle >= DEF_TTL means stale
                    if (ttl != null && ttl == -1 && idle != null && idle >= DEF_TTL) {
                        Long removed = redis.del(key);
                        deleted += (removed != null ? removed : 0);
                    }
                }

                cursor = scan;
            } while (!cursor.isFinished());

        } catch (Exception e) {
            LOG.error(e, "cleanupStaleKeys failed");
        }

        LOG.info("cleanupStaleKeys deleted " + deleted + " stale keys from Redis");
        return deleted;
    }

    public static ServerStatus.EntryList getFullStats() {
        ServerStatus.EntryList stats = getStats(true);

        try {
            var redis = RedisService.scanConn().sync();

            // Count keys and TTL stats
            long totalKeys = 0;
            long keysWithTTL = 0;
            long keysWithoutTTL = 0;
            long sessionKeys = 0;
            long staleKeys = 0;

            ScanArgs scanArgs = ScanArgs.Builder.limit(SCAN_BATCH_SIZE);
            ScanCursor cursor = ScanCursor.INITIAL;

            do {
                KeyScanCursor<String> scanResult = redis.scan(cursor, scanArgs);
                List<String> keys = scanResult.getKeys();

                for (String key : keys) {
                    totalKeys++;
                    if (isUUID(key)) sessionKeys++;

                    long ttl = redis.ttl(key);
                    if (ttl > 0) {
                        keysWithTTL++;
                    } else {
                        keysWithoutTTL++;
                    }

                    if (ttl == -1) { // Only consider keys without TTL
                        Long idle = redis.objectIdletime(key);
                        if (idle != null && idle >= DEF_TTL) {
                            staleKeys++;
                        }
                    }
                }

                cursor = scanResult;  // advance cursor
            } while (!cursor.isFinished());

            stats.add(null, "\n> CACHE USAGE SUMMARY :")
                 .add("ALL_JOB_INFOS", redis.hlen(ALL_JOB_CACHE_KEY))
                 .add("Total keys", totalKeys)
                 .add("Session keys", sessionKeys)
                 .add("Keys with TTL", keysWithTTL)
                 .add("Keys without TTL", keysWithoutTTL)
                 .add("Staled keys", staleKeys);

        } catch (Exception e) {
            stats.add("error", e.getMessage());
        }

        return stats;
    }

    public static ServerStatus.EntryList getStats(boolean detailed) {
        ServerStatus.EntryList stats = new ServerStatus.EntryList();

        // obfuscate password
        String passwd = "";
        try {
            if (REDIS_PASSWORD != null) {
                passwd = new String(MessageDigest.getInstance("MD5").digest(REDIS_PASSWORD.getBytes()));
            }
        } catch (NoSuchAlgorithmException ignore) {}

        try {
            var redis = RedisService.scanConn().sync(); // your wrapper for mainConn.sync()

            String info = redis.info();
            Map<String, String> map = parseInfo(info);

            stats.add(null, "> REDIS STATUS :");
            stats.add("status", getRedisHostPortDesc());

            // --- CONFIGURATION ---
            Map<String, String> configMap = null;
            try {
                configMap = redis.configGet(
                        "maxmemory",
                        "save",
                        "dir",
                        "dbfilename",
                        "appendfilename"
                );
            } catch (Exception e) {
                // fallback for restricted servers
                String memoryInfo = redis.info("memory");
                String serverInfo = redis.info("server");
                configMap = new LinkedHashMap<>();
                configMap.put("maxmemory", extract(memoryInfo, "maxmemory"));
                configMap.put("dir", extract(serverInfo, "dir"));
                configMap.put("dbfilename", extract(serverInfo, "dbfilename"));
                configMap.put("appendfilename", extract(serverInfo, "appendfilename"));
            }
            stats.add(null, "\n> CONFIGURATION :");
            stats.add("PASSWORD-USED", isEmpty(passwd) ? "no" : "yes");
            if (configMap != null) configMap.forEach(stats::add);

            // --- Connection health ---
            stats.add(null, "\n> CONNECTION :");
            stats.add("connection ok", RedisService.isConnected());
            stats.add("client name", redis.clientGetname() != null ? "connected" : "unknown");
            stats.add("db size", redis.dbsize());

            if (detailed) {     // dump all info
                map.forEach(stats::add);
                return stats;
            }

            // --- Basic server info ---
            stats.add("version", map.getOrDefault("redis_version", "unknown"));
            stats.add("mode", map.getOrDefault("redis_mode", "standalone"));
            stats.add("uptime", map.getOrDefault("uptime_in_days", "?") + " days");


            // --- Memory summary ---
            stats.add(null, "\n> MEMORY :");
            stats.add("used memory", map.getOrDefault("used_memory_human", "?"));
            stats.add("peak memory", map.getOrDefault("used_memory_peak_human", "?"));
            stats.add("fragmentation", map.getOrDefault("mem_fragmentation_ratio", "?"));
            stats.add("max memory", map.getOrDefault("maxmemory_human", "unlimited"));
            stats.add("policy", map.getOrDefault("maxmemory_policy", "noeviction"));

            // --- Clients ---
            stats.add(null, "\n> CLIENTS :");
            stats.add("connected clients", map.getOrDefault("connected_clients", "?"));
            stats.add("blocked clients", map.getOrDefault("blocked_clients", "?"));

            // --- Keyspace & dataset ---
            stats.add(null, "\n> KEYSPACE :");
            stats.add("db0 keys", extractKeyspaceValue(map, "keys"));
            stats.add("expires", extractKeyspaceValue(map, "expires"));
            stats.add("dataset size", map.getOrDefault("used_memory_dataset_perc", "?") + "%");

            // --- Performance ---
            stats.add(null, "\n> PERFORMANCE :");
            stats.add("ops/sec", map.getOrDefault("instantaneous_ops_per_sec", "?"));
            stats.add("hits", map.getOrDefault("keyspace_hits", "?"));
            stats.add("misses", map.getOrDefault("keyspace_misses", "?"));
            stats.add("evicted", map.getOrDefault("evicted_keys", "?"));
        } catch (Exception e) {
            LOG.error(e, "RedisService getStats failed");
        }
        return stats;
    }

    private static String extract(String info, String key) {
        for (String line : info.split("\\r?\\n")) {
            if (line.startsWith(key + ":")) {
                return line.substring(key.length() + 1).trim();
            }
        }
        return null;
    }

    private static String extractKeyspaceValue(Map<String, String> map, String key) {
        // Keyspace line looks like: db0:keys=4,expires=3,avg_ttl=1156410888
        return map.entrySet().stream()
                .filter(e -> e.getKey().startsWith("db"))
                .map(Map.Entry::getValue)
                .findFirst()
                .map(val -> {
                    for (String kv : val.split(",")) {
                        if (kv.startsWith(key + "=")) return kv.split("=")[1];
                    }
                    return "?";
                })
                .orElse("?");
    }

    private static Map<String, String> parseInfo(String info) {
        return Arrays.stream(info.split("\r\n"))
                .filter(line -> line.contains(":") && !line.startsWith("#"))
                .map(line -> line.split(":", 2))
                .collect(Collectors.toMap(
                        parts -> parts[0],
                        parts -> parts[1],
                        (a, b) -> b,  // handle duplicate keys (keep last)
                        LinkedHashMap::new
                ));
    }

    //====================================================================
    //  Redis Data Structure Migration
    //====================================================================

    private static void dataMigration() {
        LOG.info("Ensure Redis data structure is up to date");
        Cache<String> redisCache = CacheManager.getDistributed();
        String cVersion = redisCache.get(VersionKey);

        // pre-versioned to 1.0:
        // switch to composite job key; jobId:userKey
        String jobCacheVersion = redisCache.get(JOB_CACHE_VERSION_KEY);
        if (isEmpty(cVersion) && isEmpty(jobCacheVersion)) {
            LOG.info("Migrating unversioned Redis data schema to version 1.0");
            LOG.info("  - change to composite job key; jobId:userKey");
            int count = appendUserKeyToJobId();
            LOG.info("Migrated " + count + " job keys to new format");
            CacheManager.getDistributed().put(JOB_CACHE_VERSION_KEY, "1.0");
            jobCacheVersion = "1.0";
        }

        // moving 1.0 to V1_1:
        // Rename key to SchemaVersion.  Apply version to the full Redis data structure and not just JobInfo.
        if (!isEmpty(jobCacheVersion)) {
            LOG.info("Updating Redis data structure version from 1.0 to V1_1");
            redisCache.remove(JOB_CACHE_VERSION_KEY);
            redisCache.put(VersionKey, SchemaVersion.V1_1.name());
        }
    }


}
