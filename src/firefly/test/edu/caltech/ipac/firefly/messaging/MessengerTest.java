/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.messaging;

import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.core.RedisService;
import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.Ref;
import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * This test suite requires a running Redis.  To start one locally..
 * $ docker run --name test-redis -p 6379:6379 -d redis
 *
 * Date: 2019-03-15
 * @author loi
 * @version $Id: $
 */
public class MessengerTest extends ConfigTest {
    private static boolean isOffline;

    @Before
    public void setup() {
        if (!RedisService.connect()) {
            System.out.println("Messenger is offline; skipping test.");
            isOffline = true;
        }
        if (false) Logger.setLogLevel(Level.TRACE);			// for debugging.
    }

    @After
    public void teardown() {
        RedisService.teardown();
        LOG.trace("tear down");
    }

    @Test
    public void testMsgContent() throws InterruptedException {

        if (isOffline) return;
        LOG.debug("testMsgContent");

        String topic = "testPublish";
        String subject = "greetings";
        String body = "secrets";
        CountDownLatch tester = new CountDownLatch(1);

        Subscriber sub = Messenger.subscribe(topic, msg -> {
                            LOG.debug("message received, testing content..");
                            assertEquals(msg.getValue("", "subject"), subject);
                            assertEquals(msg.getValue("", "body"), body);
                            tester.countDown();
                        });

        Message msg = new Message()
                .setValue(subject, "subject")
                .setValue(body, "body");

        LOG.debug("sending message..");
        Messenger.publish(topic, msg);

        tester.await(1, TimeUnit.SECONDS);      // wait up to 1s for test to run.

        // clean up
        Messenger.unSubscribe(sub);
        LOG.debug("testMsgContent.. done!");
    }

    @Test
    public void testMsgCount() throws InterruptedException {

        if (isOffline) return;
        LOG.debug("testMsgCount");

        Message testMsg = new Message().setValue("abc", "a");
        String topic1 = "test1";
        String topic2 = "test2";

        Ref<CountDownLatch> tester = new Ref<>();       // using Ref due to inner class references
        Supplier<Subscriber> gen = () -> (Subscriber) (msg) -> {
            tester.get().countDown();
            LOG.debug("message received: " + tester.get().getCount());
        };

        Subscriber sub11 = Messenger.subscribe(topic1, gen.get());
        Subscriber sub12 = Messenger.subscribe(topic1, gen.get());
        Subscriber sub21 = Messenger.subscribe(topic2, gen.get());
        Subscriber sub22 = Messenger.subscribe(topic2, gen.get());

        LOG.debug("1 msg to each topic x 2 subs per topic.. = 4");
        tester.set(new CountDownLatch(4));
        Messenger.publish(topic1, testMsg);
        Messenger.publish(topic2, testMsg);
        tester.get().await(1, TimeUnit.SECONDS);       // wait up to 1s for msg delivery.
        assertEquals("latch(4) should drain", 0, tester.get().getCount());

        LOG.debug("same as above, but 1 sub removed from topic1... = 3");
        tester.set(new CountDownLatch(3));
        Messenger.unSubscribe(sub11);
        Messenger.publish(topic1, testMsg);
        Messenger.publish(topic2, testMsg);
        tester.get().await(1, TimeUnit.SECONDS);       // wait up to 1s for msg delivery.
        assertEquals("latch(3)3 should drain", 0, tester.get().getCount());

        LOG.debug("same as above, but remove both subs from topic1... = 2");
        tester.set(new CountDownLatch(2));
        Messenger.unSubscribe(sub12);
        Messenger.publish(topic1, testMsg);
        Messenger.publish(topic2, testMsg);
        tester.get().await(1, TimeUnit.SECONDS);       // wait up to 1s for msg delivery..
        assertEquals("latch(2) should drain", 0, tester.get().getCount());

        // clean up
        Messenger.unSubscribe(sub21);
        Messenger.unSubscribe(sub22);
        LOG.debug("testMsgCount.. done!");
    }

    @Test
    public void testSubscribe() throws InterruptedException {

        if (isOffline) return;
        LOG.debug("testSubscribe");

        String topic1 = "test1";
        String topic2 = "test2";

        Subscriber sub11 = Messenger.subscribe(topic1, msg -> msg = null);
        Subscriber sub12 = Messenger.subscribe(topic1, msg -> msg = null);

        Subscriber sub21 = Messenger.subscribe(topic2, msg -> msg = null);
        Subscriber sub22 = Messenger.subscribe(topic2, msg -> msg = null);

        Supplier<List<String>> topics = () -> Util.Try.it(() -> RedisService.getConnection().pubsubChannels())
                                                     .getOrElse(Collections.emptyList());

        LOG.debug("2 topics, 2 subs per topic.");
        Thread.sleep(300);          // give time for pool to update its stats
        assertTrue("has topic1", topics.get().contains(topic1));
        assertTrue("has topic2", topics.get().contains(topic2));

        LOG.debug("remove 1 sub from topic 1.");
        Messenger.unSubscribe(sub11);
        Thread.sleep(100);
        assertTrue("has topic1", topics.get().contains(topic1));

        LOG.debug("remove both subs from topic 1.");
        Messenger.unSubscribe(sub12);
        Thread.sleep(100);
        assertFalse("has topic1", topics.get().contains(topic1));

        LOG.debug("remove both topic.. = 0 connections");
        Messenger.unSubscribe(sub21);
        Messenger.unSubscribe(sub22);
        Thread.sleep(100);
        assertFalse("has topic1", topics.get().contains(topic1));
        assertFalse("has topic2", topics.get().contains(topic2));

        LOG.debug("Messenger stats: " + RedisService.getStats());
        LOG.debug("testSubscribe.. done!");

    }


    @Category({TestCategory.Perf.class})
    @Test
    public void perfTest() throws InterruptedException {

        if (isOffline) return;

        int numSent = 100_000;
        long startTime = System.currentTimeMillis();
        final CountDownLatch numRevc = new CountDownLatch(numSent);

        Messenger.subscribe("perfTest", (msg) -> {
            numRevc.countDown();
            if (numRevc.getCount() == 0) {
                long stopTime = System.currentTimeMillis();
                System.out.println("\t elapsed time: " + (stopTime - startTime)/1000.0 + "s");
                System.out.println("\t Messenger stats: " + RedisService.getStats());
            }
        });

        // This is a performance as well as a stress test.
        // Sending a total of 100k messages using 50 simultaneous threads
        // This will exhaust Messenger's pool size of 100, so we can see how it does under stress.
        ExecutorService exec = Executors.newFixedThreadPool(200);

        System.out.printf("Test sending %d number of messages...\n", numSent);
        for(int i = 0; i < numSent; i++) {
            int finalI = i;
            exec.submit(() -> Messenger.publish("perfTest", new Message().setValue("idx" + finalI)));
        }
        numRevc.await();
        Thread.sleep(100);
    }

}
