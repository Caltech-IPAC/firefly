/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.messaging;

import edu.caltech.ipac.firefly.core.RedisService;
import edu.caltech.ipac.firefly.server.util.Logger;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotEmpty;

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
    private static ConcurrentHashMap<String, List<Subscriber>> subscribers = new ConcurrentHashMap<>();

    public static void init() {
        try {
            RedisService.pubSubConn().addListener(new RedisPubListener());
            LOG.info("Messenger is ready to use.");
        } catch (Exception e) {
            LOG.error(e, "Error initializing Messenger.");
        }
    }

    /**
     * @param topic         the topic to subscribe to
     * @param subscriber    the subscriber to receive the messages
     * @return the given subscriber.  Useful for functional programming.
     */
    public static Subscriber subscribe(String topic, Subscriber subscriber) {

        if (subscribers.containsKey(topic)) {
            LOG.trace("Add subscriber to existing topic: " + topic);
            subscribers.get(topic).add(subscriber);
        } else {
            try {
                RedisService.pubSubConn().sync().subscribe(topic);
                List<Subscriber> subsList = new CopyOnWriteArrayList<>();
                subsList.add(subscriber);
                subscribers.put(topic, subsList);
                LOG.trace("Add subscriber to new topic: " + topic);
            } catch (Exception e) {
                LOG.error(e, "Error subscribing to topic: " + topic);
            }
        }
        return subscriber;
    }

    /**
     * @param subscriber the subscriber to remove
     */
    public static void unSubscribe(Subscriber subscriber) {
        subscribers.forEach((topic, subsList) -> {
            if (subsList.remove(subscriber)) {
                try {
                    LOG.info("Removed subscriber from topic: " + topic);
                    if (subsList.isEmpty()) {
                        RedisService.pubSubConn().sync().unsubscribe(topic);
                        LOG.info("No more subscriber; unsubscribe from topic: " + topic);
                        subscribers.remove(topic);
                    }
                } catch (Exception e) {
                    LOG.error(e, "Error unsubscribing subscriber.");
                }
            }
        });
    }

    public static int getSubscribedTopics() {
        return subscribers.size();
    }
    public static Map<String, List<Subscriber>> getSubscribers() {
        return Collections.unmodifiableMap(subscribers);
    }

    /**
     * Compose a message with the given subject and body, then send it to everyone(world)
     * @param topic   topic to publish to
     * @param msg     message to send
     */
    public static void publish(String topic, Message msg) {
        try {
            RedisService.mainConn().async().publish(topic, msg.toJson());
        } catch (Exception e) {
            LOG.error(e, "Error publishing message to topic: " + topic);
        }
    }

    /**
     * Publishes the given message to its topic.
     * @param msg the message to be published
     */
    public static void publish(Message msg) {
        ifNotEmpty(msg.getTopic()).apply((topic) -> publish(topic, msg));
    }

//====================================================================
// A single Listener that dispatches messages to all subscribers
//====================================================================

    private static class RedisPubListener extends RedisPubSubAdapter<String, String> {
        @Override
        public void message(String channel, String message) {
            LOG.trace("Received message from channel [" + channel + "]: " + message);
            List<Subscriber> subs = subscribers.get(channel);
            if (subs != null) {
                try {
                    Message msg = Message.parse(message);
                    subs.forEach(s -> s.onMessage(msg));
                } catch (Exception e) {
                    LOG.error(e, "Error while processing message from channel: " + channel);
                }
            } else {
                LOG.warn("No registered handler for channel: " + channel + ". Message ignored.");
            }
        }

        @Override
        public void subscribed(String channel, long count) {
            LOG.info("Subscribed to channel: " + channel + " (total: " + count + ")");
        }

        @Override
        public void unsubscribed(String channel, long count) {
            LOG.info("Unsubscribed from channel: " + channel + " (total: " + count + ")");
        }
    }
}
