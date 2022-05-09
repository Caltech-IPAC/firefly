/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.messaging;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Protocol;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An implementation of publish-subscribe messaging pattern based on Jedis client and Redis backend.
 * This class abstract the use of threads and it ensures that there is only 1 thread used per topic.
 *
 * Topic refer here is equivalent to jedis 'channel'.  This is so it will not be confused with channel
 * used in Scope.
 *
 *
 * Date: 2019-03-15
 *
 * @author loi
 * @version $Id: $
 */
public class Messenger {
    public enum Status {CONNECTED, FAIL_TO_CONNECT, NOT_USING};
    private static final String REDIS_HOST = AppProperties.getProperty("redis.host", "127.0.0.1");
    private static final int REDIS_PORT = AppProperties.getIntProperty("redis.port", 6379);
    private static final int MAX_POOL_SIZE = AppProperties.getIntProperty("redis.max.poolsize", 25);
    private static final String REDIS_PASSWORD = getRedisPassword();
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    // message broker..  Jedis
    private static JedisPool jedisPool;
    private static Status status = Status.NOT_USING;

    // to limit one thread per topic
    private static ConcurrentHashMap<String, SubscriberHandle> pubSubHandlers = new ConcurrentHashMap<>();

    private static String getRedisPassword() {
        String passwd = System.getenv("REDIS_PASSWORD");
        if (passwd == null) passwd = AppProperties.getProperty("REDIS_PASSWORD");
        return passwd;
    }

    static boolean init() {

        if (jedisPool == null) {
            try {
                JedisPoolConfig pconfig = new JedisPoolConfig();
                pconfig.setTestOnBorrow(true);
                pconfig.setMaxTotal(MAX_POOL_SIZE);
                pconfig.setBlockWhenExhausted(true);                // wait.. if needed
                jedisPool = new JedisPool(pconfig, REDIS_HOST, REDIS_PORT, Protocol.DEFAULT_TIMEOUT, REDIS_PASSWORD);
                jedisPool.getResource().close();
                status = Status.CONNECTED;
            } catch (Exception ex) {
                LOG.error("Unable to connect to Redis at " + REDIS_HOST + ":" + REDIS_PORT);
                jedisPool = null;
                status = Status.FAIL_TO_CONNECT;
                return false;
            }
        }
        return true;
    }

    public static Status getStatus() { return status; }

    public static String getStats() {

        if (getStatus()==Status.NOT_USING) return "Messenger is offline";
        JsonHelper stats = new JsonHelper();
        if (!init()) {
            return stats.setValue("Messenger is offline").toJson();
        } else {
            String passwd = "";
            try {
                if (REDIS_PASSWORD != null) {
                    passwd = new String(MessageDigest.getInstance("MD5").digest(REDIS_PASSWORD.getBytes()));
                }
            } catch (NoSuchAlgorithmException e) {/* ignore */}

            return stats.setValue(jedisPool.getNumActive(), "active")
                    .setValue(jedisPool.getNumIdle(), "idle")
                    .setValue(MAX_POOL_SIZE, "max")
                    .setValue(jedisPool.getMaxBorrowWaitTimeMillis(), "max-wait")
                    .setValue(jedisPool.getMeanBorrowWaitTimeMillis(), "avg-wait")
                    .setValue(passwd, "password")
                    .toJson();
        }
    }

    public static String getRedisHostPortDesc() {
        return REDIS_HOST + ":" + REDIS_PORT + " ("+ getStatus() + ")";
    }

    public static boolean isOffline() {
        if (!init()) return true;

        try (Jedis ignored = jedisPool.getResource()) {    // test connection
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public static int getConnectionCount() {
        if (!init()) return -1;
        return jedisPool.getNumActive();
    }

    /**
     * @param topic         the topic to subscribe to
     * @param subscriber    the subscriber to receive the messages
     * @return the given subscriber.  Useful for functional programming.
     */
    public static Subscriber subscribe(String topic, Subscriber subscriber) {
        if (!init()) return null;

        if (pubSubHandlers.containsKey(topic)) {
            SubscriberHandle pubSub = pubSubHandlers.get(topic);
            pubSub.addSubscriber(subscriber);
        } else {
            SubscriberHandle pubSub = new SubscriberHandle(topic);
            pubSub.addSubscriber(subscriber);
            pubSubHandlers.put(topic, pubSub);
        }
        return subscriber;
    }

    /**
     * @param subscriber the subscriber to remove
     */
    public static void unSubscribe(Subscriber subscriber) {
        if (!init()) return;

        pubSubHandlers.values().stream()
                .filter(hdl -> hdl.subscribers.contains(subscriber))
                .forEach(hdl -> hdl.removeSubscriber(subscriber));
    }

    /**
     * Compose a message with the given subject and body, then send it to everyone(world)
     * @param topic   topic to publish to
     * @param msg     message to send
     */
    public static void publish(String topic, Message msg) {
        if (!init()) return;

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(topic, msg.toJson());
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    /**
     * Send the given message.  A topic will derived from the msg's headers
     * @param msg   the message to send
     */
    public static void publish(Message msg) {
        // some firefly's specific logic here...
        String topic = msg.getHeader().getScope().name();
        publish(topic, msg);
    }


    /**
     * Internal handler class used to manage the one to many relationship of Messenger's subscriber and
     * Jedis's subscriber
     */
    static class SubscriberHandle {

        private CopyOnWriteArrayList<Subscriber> subscribers = new CopyOnWriteArrayList<>();
        private String topic;
        ExecutorService executor;
        private JedisPubSub jPubSub;

        SubscriberHandle(String topic) {
            this.topic = topic;
        }

        void addSubscriber(Subscriber sub) {
            subscribers.add(sub);
            if (subscribers.size() == 1) {
                // first subscriber.. need to connect to redis
                doSubscribe();
            }
        }

        void removeSubscriber(Subscriber sub) {
            subscribers.remove(sub);
            if (subscribers.size() == 0) {
                // no more subscrber.. disconnect from redis
                cleanup();
            }
        }

        void doSubscribe() {
            jPubSub = new JedisPubSub() {
                public void onMessage(String channel, String message) {
                    Message msg = Message.parse(message);
                    subscribers.forEach((sub) -> {
                        try {
                            sub.onMessage(msg);
                        } catch (Exception e) {
                            LOG.warn("Error while processing message: " + e.getMessage());
                        }
                    });
                }
            };

            executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    while (subscribers.size() > 0) {
                        if (Messenger.init()) {
                            try (Jedis jedis = jedisPool.getResource()) {
                                jedis.subscribe(jPubSub, topic);
                            } catch (Exception e) {
                                // quietly ignores to avoid excessive error logs.
                            }
                        }

                        Thread.sleep(5000);     // if disconnected and there's still subscribers to this topic, attempt to reconnect after a brief pause.
                    }
                } catch (InterruptedException e) {
                    cleanup();
                }
            });
        }

        void cleanup() {
            if (jPubSub != null) jPubSub.unsubscribe();
            jPubSub = null;
            if (executor != null) executor.shutdown();
            executor = null;
        }
    }

}
