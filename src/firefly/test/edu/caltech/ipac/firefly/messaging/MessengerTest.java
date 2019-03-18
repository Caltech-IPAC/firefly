/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.messaging;

import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.ConfigTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    public void testMessenger() throws InterruptedException {

        if (Messenger.isOffline()) {
            System.out.println("Messenger is offline; skipping MessengerTest.");
            return;
        }

        String topic = "testPublish";
        String subject = "greetings";
        String body = "secrets";
        AtomicInteger receiveCnt = new AtomicInteger(0);

        LOG.debug("first subscriber");
        Messenger.subscribe(topic, (msg) -> {
            receiveCnt.incrementAndGet();
            // verify that the content of the msg is correct
            assertEquals(msg.getValue("", "subject"), subject);
            assertEquals(msg.getValue("", "body"), body);
        });

        LOG.debug("second subscriber to a different topic");
        Messenger.subscribe(topic + "diff", (msg) -> {
            receiveCnt.incrementAndGet();
            fail("Should not get any message here");
        });

        LOG.debug("third subscriber, but to same topic");
        Messenger.subscribe(topic, (msg) -> {
            receiveCnt.incrementAndGet();
        });

        Message msg = new Message()
                .setValue(subject, "subject")
                .setValue(body, "body");

        Messenger.publish(topic, msg);
        Messenger.publish(topic, msg);
        Messenger.publish(topic, msg);

        LOG.debug("wait for the test to run through.. not the best approach for async unit tests.. should revisit later.");
        Thread.sleep(500);

        LOG.debug("3 messages were sent to 2 subscribers.  The 3rd should not get any.");
        assertEquals("Number of messages received", 6, receiveCnt.get());

        LOG.debug("2 topics were subscribed to.  Expect to get 2 active connections");
        assertEquals("Number of active connections", 2, Messenger.getConnectionCount());

        LOG.debug("Messenger stats: " + Messenger.getStats());
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
