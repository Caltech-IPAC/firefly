/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.messaging;

import edu.caltech.ipac.firefly.core.RedisService;
import edu.caltech.ipac.firefly.server.util.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;

/**
 * An implementation of publish-subscribe messaging pattern based on Jedis client and Redis backend.
 * This class abstract the use of threads, and it ensures that there is only 1 thread used per topic.
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
    private static Logger.LoggerImpl LOG = Logger.getLogger();

    // to limit one thread per topic
    private static ConcurrentHashMap<String, SubscriberHandler> pubSubHandlers = new ConcurrentHashMap<>();

    /**
     * @param topic         the topic to subscribe to
     * @param subscriber    the subscriber to receive the messages
     * @return the given subscriber.  Useful for functional programming.
     */
    public static Subscriber subscribe(String topic, Subscriber subscriber) {
        if (pubSubHandlers.containsKey(topic)) {
            LOG.trace("Add subscriber to existing topic: " + topic);
            SubscriberHandler pubSub = pubSubHandlers.get(topic);
            pubSub.addSubscriber(subscriber);
        } else {
            LOG.trace("Add subscriber to new topic: " + topic);
            SubscriberHandler pubSub = new SubscriberHandler(topic);
            pubSubHandlers.put(topic, pubSub);
            pubSub.addSubscriber(subscriber);
        }
        return subscriber;
    }

    public static int getSubscribedTopics() {
        return pubSubHandlers.size();
    }
    public static Map<String, SubscriberHandler> getSubscribers() {
        return Collections.unmodifiableMap(pubSubHandlers);
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
        try (Jedis jedis = RedisService.getConnection()) {
            jedis.publish(topic, msg.toJson());
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    /**
     * Publishes the given message to its topic.
     * @param msg the message to be published
     */
   public static void publish(Message msg) {
        applyIfNotEmpty(msg.getTopic(), (topic) -> publish(topic, msg));
    }

    /**
     * Internal handler class used to manage the one-to-many relationship of Messenger's subscriber and
     * Jedis's subscriber
     */
    public static class SubscriberHandler {

        private final CopyOnWriteArrayList<Subscriber> subscribers = new CopyOnWriteArrayList<>();
        private final String topic;
        private final AtomicInteger retries = new AtomicInteger(5);
        private Instant failSince = Instant.now();
        private Thread subscriberThread;
        JedisPubSub jPubSub = new JedisPubSub() {
            public void onMessage(String channel, String message) {
                Message msg = Message.parse(message);
                subscribers.forEach((sub) -> {
                    try {
                        sub.onMessage(msg);
                    } catch (Exception e) {
                        LOG.error(e, "Error while processing message: " + message);
                    }
                });
            }
            public void onSubscribe(String channel, int subscribedChannels) {
                LOG.info("Subscribed to topic: " + topic);
            }
            public void onUnsubscribe(String channel, int subscribedChannels) {
                LOG.info("Unsubscribed from topic: " + channel);
            }

        };

        SubscriberHandler(String topic) {
            this.topic = topic;
        }

        void addSubscriber(Subscriber sub) {
            subscribers.add(sub);
            if (subscriberThread == null || !subscriberThread.isAlive()) {
                // Start the subscriber in a separate thread; use only one thread per topic
                subscriberThread = new Thread(() -> {
                    try {
                        subscribeToRedis();
                    } catch (Exception e) {
                        LOG.error(e, "Error while subscribing to topic: " + topic);
                    } finally {
                        subscriberThread = null;
                        LOG.trace("exiting subscribing to topic: " + topic);
                    }
                });
                subscriberThread.start();
            }
        }

        void removeSubscriber(Subscriber sub) {
            subscribers.remove(sub);
            if (subscribers.isEmpty()) {
                // no more subscriber; disconnect from redis
                jPubSub.unsubscribe();
            }
        }

        public Instant getFailSince() {
            return failSince;
        }

        void subscribeToRedis() throws InterruptedException {
            LOG.trace("start subscribing to topic: " + topic);
            try (Jedis jedis = RedisService.getConnection()) {
                failSince = null;
                jedis.subscribe(jPubSub, topic); // Blocks here
            } catch (Exception e) {
                if (failSince == null) failSince = Instant.now();
                if (!subscribers.isEmpty()) {
                    long secSinceFailed = Duration.between(getFailSince(), Instant.now()).toSeconds();
                    if (secSinceFailed % 5 == 0) {
                        LOG.info("Subscriber (%s) has been disconnected from Redis for %d seconds".formatted(topic, secSinceFailed));
                    }
                    Thread.sleep(1_000);
                    subscribeToRedis();
                }
            }
        }
    }
}
