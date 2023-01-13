/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.messaging;

import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.util.Ref;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private boolean isOffline;

    @Before
    public void checkAlive() {
        if (Messenger.isOffline()) {
            System.out.println("Messenger is offline; skipping test.");
            isOffline = true;
        }
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

        Message testMsg = new Message();
        String topic1 = "test1";
        String topic2 = "test2";

        Ref<CountDownLatch> tester = new Ref<>();       // using Ref due to inner class references
        Subscriber sub11 = Messenger.subscribe(topic1, msg -> tester.get().countDown());
        Subscriber sub12 = Messenger.subscribe(topic1, msg -> tester.get().countDown());
        Subscriber sub21 = Messenger.subscribe(topic2, msg -> tester.get().countDown());
        Subscriber sub22 =Messenger.subscribe(topic2, msg -> tester.get().countDown());

        LOG.debug("1 msg to each topic x 2 subs per topic.. = 4");
        tester.set(new CountDownLatch(4));
        Messenger.publish(topic1, testMsg);
        Messenger.publish(topic2, testMsg);
        tester.get().await(1, TimeUnit.SECONDS);       // wait up to 1s for msg delivery..
        assertEquals("latch(4) should drain", 0, tester.get().getCount());

        LOG.debug("same as above, but 1 sub removed from topic1... = 3");
        tester.set(new CountDownLatch(3));
        Messenger.unSubscribe(sub11);
        Messenger.publish(topic1, testMsg);
        Messenger.publish(topic2, testMsg);
        tester.get().await(1, TimeUnit.SECONDS);       // wait up to 1s for msg delivery..
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

        LOG.debug("2 topics, 2 subs per topic.. = 2 connections");
        Thread.sleep(100);
        assertEquals("init", 2, Messenger.getConnectionCount());

        LOG.debug("remove 1 sub from topic 1.. = 2 connections");
        Messenger.unSubscribe(sub11);
        Thread.sleep(100);
        assertEquals("3 subs left", 2, Messenger.getConnectionCount());

        LOG.debug("remove both subs from topic 1.. = 1 connections");
        Messenger.unSubscribe(sub12);
        Thread.sleep(100);
        assertEquals("only topic2 left", 1, Messenger.getConnectionCount());

        LOG.debug("remove both topic.. = 0 connections");
        Messenger.unSubscribe(sub21);
        Messenger.unSubscribe(sub22);
        Thread.sleep(100);
        assertEquals("no subs", 0, Messenger.getConnectionCount());

        LOG.debug("Messenger stats: " + Messenger.getStats());
        LOG.debug("testSubscribe.. done!");

    }


    @Category({TestCategory.Perf.class})
    @Test
    public void perfTest() throws InterruptedException {
        int numSent = 1000*100;
        long startTime = System.currentTimeMillis();
        final CountDownLatch numRevc = new CountDownLatch(numSent);

        Messenger.subscribe("perfTest", (msg) -> {
            numRevc.countDown();
            if (numRevc.getCount() == 0) {
                long stopTime = System.currentTimeMillis();
                System.out.println("\t elapsed time: " + (stopTime - startTime)/1000.0 + "s");
                System.out.println("\t Messenger stats: " + Messenger.getStats());
            }
        });

        // This is a performance as well as a stress test.
        // Sending a total of 100k messages using 50 simultaneous threads
        // This will exhaust Messenger's pool size of 25 so we can see how it does under stress.
        ExecutorService exec = Executors.newFixedThreadPool(50);

        System.out.printf("Test sending %d number of messages...\n", numSent);
        for(int i = 0; i < numSent; i++) {
            int finalI = i;
            exec.submit(() -> Messenger.publish("perfTest", new Message().setValue("idx" + finalI)));
        }
        numRevc.await();
    }

}
