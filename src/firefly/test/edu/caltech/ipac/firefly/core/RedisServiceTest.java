/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.server.servlets.ServerStatus;
import edu.caltech.ipac.firefly.server.util.Logger;
import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * This test suite requires a running Redis.  To start one locally..
 * $ docker run --name test-redis -p 6379:6379 -d redis
 *
 * Date: 2019-03-15
 * @author loi
 * @version $Id: $
 */
public class RedisServiceTest extends ConfigTest {

    @BeforeClass
    public static void setup() throws Exception {
        if (false) Logger.setLogLevel(Level.TRACE);			// for debugging.
        RedisService.init();
    }

    @AfterClass
    public static void teardown() {
        RedisService.teardown();
        LOG.trace("tear down");
    }

    @Ignore("Does not work reliably in CI")
    @Test
    public void testExternalRedis() {
        testRedis();
    }

    @Test
    public void testEmbeddedRedis() {
        // without REDIS_HOST set, an internal Redis will start up.
        testRedis();
    }

    @Test
    public void testExceedMaxConnections() {
        try {
            for (int i=0; i<25; i++) {
                assertEquals("PONG", RedisService.mainConn().sync().ping());
                assertEquals("PONG", RedisService.scanConn().sync().ping());
                assertEquals("PONG", RedisService.pubSubConn().sync().ping());
            }
            Assert.assertTrue(true);    // should finish with some wait time.
        } catch (Exception e) {
            Assert.fail("Expected to fail");
        }
    }

    @Test
    public void testFullStats() {
        ServerStatus.EntryList stats = RedisService.getFullStats();
        Assert.assertNotNull(stats);
    }

    private void testRedis() {

        // ping test
        try {
            var redis = RedisService.mainConn().sync();
            assertEquals("PONG", redis.ping());
        } catch (Exception e) {
            Assert.fail("Can't connect: " + e);
        }

        try {
            var redis = RedisService.mainConn().sync();
            redis.setex("key1", 1, "val1");
            assertEquals("val1", redis.get("key1"));
            Thread.sleep(3_000);        // should be expired after 3 seconds
            assertEquals(0, (long) redis.exists("key1"));
        } catch (Exception e) {
            Assert.fail("Can't connect: " + e);
        }


        // lots of connections test
        for (int i=0; i<100; i++) {
            try {
                var redis = RedisService.mainConn().sync();
                assertEquals("PONG", redis.ping());
            } catch (Exception e) {
                Assert.fail("Can't connect: " + e);
            }
        }
    }


}
