/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util.cache;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.core.RedisService;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static edu.caltech.ipac.util.cache.Cache.fileCheck;
import static org.junit.Assert.*;
import static edu.caltech.ipac.firefly.core.Util.Try;

/**
 * This test suite requires a running Redis.  To start one locally..
 * $ docker run --name test-redis -p 6379:6379 -d redis
 *
 * Date: 2019-04-11
 * @author loi
 * @version $Id: $
 */
public class CacheTest extends ConfigTest {

    @BeforeClass
    public static void setUp() throws InterruptedException {
        setupServerContext(null);
        if (false) Logger.setLogLevel(Level.TRACE);			// for debugging.
    }

    @After
    public void tearDown() {
        RedisService.teardown();
    }

    @Test
    public void localCache() {
        Cache cache = CacheManager.getLocal();
        testObject(cache);
    }

    @Test
    public void distributedCache() {
        Cache cache = CacheManager.getDistributed();
        testObject(cache);
    }

    @Test
    public void localGetValidator() {
        Cache<File> local = CacheManager.<File>getLocal().validateOnGet(fileCheck);
        var key = new StringKey("1");
        local.put(key, new File("bad/path/not-found.txt"));
        assertNull(local.get(key));

        Cache<File> dist = CacheManager.<File>getDistributed().validateOnGet(fileCheck);;
        key = new StringKey("1");
        dist.put(key, new File("bad/path/not-found.txt"));
        assertNull(dist.get(key));

        File a = Try.it(() -> File.createTempFile("test", ".txt")).get();
        dist.put(key, a);
        assertNotNull(dist.get(key));
        a.delete();
    }

    @Test
    public void userCache() {
        Cache cache = CacheManager.getUserCache();
        testObject(cache);
    }

    @Test
    public void localMap() {
        Cache cache = CacheManager.getLocalMap("test");
        testObject(cache);
    }

    @Test
    public void distributedMap() {
        Cache cache = CacheManager.getDistributedMap("test");
        testObject(cache);
    }


    private void testObject(Cache cache) {
        cache.put(new StringKey("1"), 1);
        assertEquals(1, cache.get(new StringKey("1")));

        cache.put(new StringKey("string"), "string");
        assertEquals("string", cache.get(new StringKey("string")));

        cache.put(new StringKey("string"), "string");
        assertEquals("string", cache.get(new StringKey("string")));

        UserInfo u = new UserInfo("userId", "password");
        u.setEmail("me@acme.com");
        cache.put(new StringKey(u.getLoginName()), u);
        Object ru = cache.get(new StringKey(u.getLoginName()));
        assertTrue(ru instanceof UserInfo);
        assertEquals(u.toString(), ru.toString());

        File f = new File("bad/path/not-found.txt");
        cache.put(new StringKey(f.getName()), f);
        Object rf = cache.get(new StringKey(f.getName()));
        assertTrue(rf instanceof File);
        assertEquals(f.getAbsolutePath(), ((File) rf).getAbsolutePath());
    }

}
