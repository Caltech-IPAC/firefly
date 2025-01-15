/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import static edu.caltech.ipac.firefly.core.RedisService.MAX_POOL_SIZE;
import static edu.caltech.ipac.firefly.core.RedisService.REDIS_HOST;
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
    public static void setup() {
        RedisService.connect();
        if (RedisService.isOffline()) {
            System.out.println("Messenger is offline; skipping test.");
        }
        if (false) Logger.setLogLevel(Level.TRACE);			// for debugging.
    }

    @AfterClass
    public static void teardown() {
        RedisService.disconnect();
        LOG.trace("tear down");
    }

    @Test
    public void testExternalRedis() {
        AppProperties.setProperty(REDIS_HOST, "localhost");     // setup for external Redis
        testRedis();
    }

    @Test
    public void testEmbeddedRedis() {
        // without REDIS_HOST set, an internal Redis will start up.
        testRedis();
    }

    @Test
    public void testExceedMaxConnections() {
        AppProperties.setProperty(MAX_POOL_SIZE, "20");
        try {
            for (int i=0; i<25; i++) {
                Jedis conn = RedisService.getConnection();
                assertEquals("PONG", conn.ping());
            }
            Assert.assertTrue(true);    // should finish with some wait time.
        } catch (Exception e) {
            Assert.fail("Expected to fail");
        }
    }

    private void testRedis() {

        if (RedisService.isOffline()) return;

        assertEquals(RedisService.Status.ONLINE, RedisService.getStatus());

        // ping test
        try (Jedis conn = RedisService.getConnection()) {
            assertEquals("PONG", conn.ping());
        } catch (Exception e) {
            Assert.fail("Can't connect: " + e);
        }

        // set with expiry
        try (Jedis conn = RedisService.getConnection()) {
            conn.setex("key1", 1, "val1");
            assertEquals("val1", conn.get("key1"));
            Thread.sleep(1_000);        // expired after 1s
            assertEquals(false, conn.exists("key1"));
        } catch (Exception e) {
            Assert.fail("Can't connect: " + e);
        }

        // lots of connections test
        for (int i=0; i<100; i++) {
            try (Jedis conn = RedisService.getConnection()) {
                assertEquals("PONG", conn.ping());
            } catch (Exception e) {
                Assert.fail("Can't connect: " + e);
            }
        }
    }


}
