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
    private static final String REDIS_HOST = AppProperties.getProperty("redis.host", "127.0.0.1");
    private static final int REDIS_PORT = AppProperties.getIntProperty("redis.port", 6379);
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    // message broker..  Jedis
    private static JedisPool jedisPool;

    // to limit one thread per topic
    private static ConcurrentHashMap<String, SubscriberHandle> pubSubHandlers = new ConcurrentHashMap<>();

    private static boolean isOffline;

    static  {
        try {
            JedisPoolConfig pconfig = new JedisPoolConfig();
            pconfig.setTestOnBorrow(true);
            pconfig.setMaxTotal(25);
            pconfig.setBlockWhenExhausted(true);                // wait.. if needed
            jedisPool = new JedisPool(pconfig, REDIS_HOST, REDIS_PORT);
            jedisPool.getResource().close();    // test connection
        } catch (Exception ex) {
            isOffline = true;
            LOG.error(ex, "Unable to connect to Redis at " + REDIS_HOST + ":" + REDIS_PORT);
        }

    }

    public static String getStats() {
        if (isOffline()) {
            return "[ Messenger is offline ]";
        } else {
            String stats =  " active:" + jedisPool.getNumActive() +
                            " idle:" + jedisPool.getNumIdle() +
                            " max-wait:" + jedisPool.getMaxBorrowWaitTimeMillis() +
                            " avg-wait:" + jedisPool.getMeanBorrowWaitTimeMillis();
            return "[ " + stats + " ]";
        }
    }

    public static boolean isOffline() {
        return isOffline;
    }

    public static int getConnectionCount() {
        return jedisPool.getNumActive();
    }

    /**
     * @param topic         the topic to subscribe to
     * @param subscriber    the subscriber to receive the messages
     * @return the given subscriber.  Useful for functional programming.
     */
    public static Subscriber subscribe(String topic, Subscriber subscriber) {
        if (pubSubHandlers.containsKey(topic)) {
            SubscriberHandle pubSub = pubSubHandlers.get(topic);
            pubSub.addSubscriber(subscriber);
        } else {
            SubscriberHandle pubSub = new SubscriberHandle();
            pubSub.addSubscriber(subscriber);
            pubSubHandlers.put(topic, pubSub);
            pubSub.executor = Executors.newSingleThreadExecutor();
            pubSub.executor.submit(() -> {
                jedisPool.getResource().subscribe(pubSub.jPubSub, topic);
            });
        }
        return subscriber;
    }

    /**
     * @param subscriber the subscriber to remove
     */
    public static void unSubscribe(Subscriber subscriber) {
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
        try {
            Jedis jedis = jedisPool.getResource();
            jedis.publish(topic, msg.toJson());
            jedis.close();
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
        ExecutorService executor;
        private JedisPubSub jPubSub = new JedisPubSub() {
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

        void addSubscriber(Subscriber sub) {
            subscribers.add(sub);
        }

        void removeSubscriber(Subscriber sub) {
            subscribers.remove(sub);
            if (subscribers.size() == 0) {
                executor.shutdown();
                unsubscribe();
            }
        }

        void unsubscribe() {
            jPubSub.unsubscribe();
        }
    }

}
