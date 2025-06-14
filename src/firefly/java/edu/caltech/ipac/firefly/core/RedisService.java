/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core;

import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.embedded.RedisServer;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static edu.caltech.ipac.firefly.core.Util.Try;


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
 *
 * Date: 2024-11-18
 *
 * @author loi
 * @version $Id: $
 */
public class RedisService {
    public static final String REDIS_HOST = "redis.host";
    public static final String MAX_POOL_SIZE = "redis.max.poolsize";
    private static final int REDIS_PORT = AppProperties.getIntProperty("redis.port", 6379);
    private static final String MAX_MEM = AppProperties.getProperty("redis.max.mem", "128M");
    private static final String DB_DIR = AppProperties.getProperty("redis.db.dir", System.getProperty("java.io.tmpdir")+ "/redis");
    private static final String REDIS_PASSWORD = getRedisPassword();
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    // message broker..  Jedis
    private static final String redisHost = AppProperties.getProperty(REDIS_HOST, "localhost");
    public static final int maxPoolSize = AppProperties.getIntProperty(MAX_POOL_SIZE, 100);
    private static JedisPool jedisPool;
    private static Instant failSince;

    private static String getRedisPassword() {
        String passwd = System.getenv("REDIS_PASSWORD");
        if (passwd == null) passwd = AppProperties.getProperty("REDIS_PASSWORD");
        return passwd;
    }

    static JedisPool createJedisPool() {
        try {
            JedisPoolConfig pconfig = new JedisPoolConfig();
            pconfig.setTestOnBorrow(true);
            pconfig.setMaxTotal(maxPoolSize);
            pconfig.setBlockWhenExhausted(true);                // wait; if needed
            pconfig.setMaxWait(Duration.of(5, ChronoUnit.SECONDS));
            JedisPool pool = new JedisPool(pconfig, redisHost, REDIS_PORT, Protocol.DEFAULT_TIMEOUT, REDIS_PASSWORD);
            pool.getResource().close();     // test connection
            return pool;
        } catch(Exception ignored) {}
        return null;
    }

    static void startLocal() {
        try {
            new File(DB_DIR).mkdirs();      // ensure the directory exists
            RedisServer redisServer = RedisServer.newRedisServer()
                    .port(REDIS_PORT)
                    .setting("maxmemory %s".formatted(MAX_MEM))
                    .setting("dir %s".formatted(DB_DIR))            // Directory where redis database files are stored
                    .setting("dbfilename redis.rdb")                // RDB file name
                    .setting("save 600 1")                // RDB file name
                    .build();
            redisServer.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Try.it(redisServer::stop).getOrElse((e) -> LOG.error(e,"Failed to stop Redis"));
            }));


        } catch (IOException ignored) {}
    }

    public static Instant getFailSince() { return failSince; }

    public static Jedis getConnection() throws Exception {
        try {
            if (jedisPool == null || jedisPool.isClosed()) {
                jedisPool = createJedisPool();
                if (jedisPool == null && redisHost.equals("localhost")) {
                    // can't connect; will start up embedded version if localhost
                    startLocal();
                    jedisPool = createJedisPool();
                }
                if (jedisPool == null || jedisPool.isClosed()) {
                    if (jedisPool != null) {
                        Try.it(jedisPool::close);
                        jedisPool = null;
                    }
                    throw new RuntimeException("Unable to connect to Redis at " + redisHost + ":" + REDIS_PORT);
                }
            }
            Jedis jedis = jedisPool.getResource();
            if (failSince != null) updateConnectionStatus(false);
            return jedis;
        } catch (Exception e) {
            if (failSince == null) {
                updateConnectionStatus(true);
            }
            throw e;
        }
    }

    // because Redis may be down, we need to use processEvent to directly send it to clients currently connected
    // to this server and not rely on distributed event messaging.
    public static void updateConnectionStatus(boolean lost) {
        failSince = lost ? Instant.now() : null;
        String reason = lost ? "A critical system component is currently unavailable" : "";

        FluxAction action = new FluxAction("app_data.appUpdate");
        action.setValue(lost, "connectionStatus", "lost");
        action.setValue(reason, "connectionStatus", "reason");
        ServerEventManager.processEvent(ServerEventManager.convertTo(action, ServerEvent.Scope.WORLD));
    }

    public static void teardown() {
        disconnect();
        File[] files = new File(DB_DIR).listFiles();
        if (files!= null) Arrays.stream(files).forEach(File::delete);
    }

    public static void disconnect() {
        if (jedisPool != null) {
            jedisPool.close();
            jedisPool = null;
        }
    }

    public static Map<String, Object> getStats() {

        LinkedHashMap<String, Object> stats = new LinkedHashMap<>();
        stats.put("status", getRedisHostPortDesc());
        String passwd = "";
        try {
            if (REDIS_PASSWORD != null) {
                passwd = new String(MessageDigest.getInstance("MD5").digest(REDIS_PASSWORD.getBytes()));
            }
        } catch (NoSuchAlgorithmException e) {/* ignore */}
        try (Jedis redis = getConnection()) {

            var infos = redis.info().split("\r\n");
            Arrays.stream(infos).filter(s -> s.contains("version")).findFirst()
                    .ifPresent(s -> stats.put("version", s.split(":")[1].trim()));

            stats.put("active conn", jedisPool.getNumActive());
            stats.put("idle conn", jedisPool.getNumIdle());
            stats.put("max conn", maxPoolSize);
            stats.put("max-wait", jedisPool.getMaxBorrowWaitTimeMillis());
            stats.put("avg-wait", jedisPool.getMeanBorrowWaitTimeMillis());
            stats.put("password", passwd);
            stats.put("db-size", redis.dbSize());
            addStat(stats, redis, "maxmemory");
            addStat(stats, redis, "save");
            addStat(stats, redis, "dir");
            addStat(stats, redis, "dbfilename");
            addStat(stats, redis, "appendfilename");
            stats.put("---MEMORY STATS----", "");
            var mem = redis.memoryStats();
            stats.put("Total memory used", mem.get("dataset.bytes"));
            stats.put("Total memory allocated", mem.get("allocator.allocated"));
            stats.put("Fragmented memory", mem.get("fragmentation"));
            stats.put("Fragmentation ratio", mem.get("allocator-fragmentation.ratio"));
            stats.put("Number of keys stored", mem.get("keys.count"));
            stats.put("Avg per key", mem.get("keys.bytes-per-key"));
            stats.put("Pct of memory used", mem.get("dataset.percentage"));
            stats.put("Peak memory used", mem.get("peak.allocated"));
        } catch (Exception ignored) {}
        return stats;
    }

    public static void addStat(Map<String, Object> stats, Jedis redis, String key) {
        var c = redis.configGet(key);
        if (c.size() > 1) stats.put(key, c.get(1));
    }

    public static String getRedisHostPortDesc() {
        String status = failSince == null ? "OK" :
                "Failed since " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(failSince);
        return redisHost + ":" + REDIS_PORT + " (%s)".formatted(status);
    }
}
