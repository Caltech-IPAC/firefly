/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util.cache;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.core.RedisService;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static edu.caltech.ipac.util.cache.Cache.*;
import static org.junit.Assert.*;

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
        RedisService.connect();
        setupServerContext(null);
        if (false) Logger.setLogLevel(Level.TRACE);			// for debugging.
    }

    @AfterClass
    public static void tearDown() {
        RedisService.disconnect();
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
    public void localFileCache() {
        Cache cache = CacheManager.getLocalFile();
        testFile(cache);
    }

    @Test
    public void distributedFileCache() {
        Cache cache = CacheManager.getDistributedFile();
        testFile(cache);
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

    private void testFile(Cache cache) {
        cache.put(new StringKey("1"), 1);
        assertNull(cache.get(new StringKey("1")));

        File badf = new File("bad/path/not-found.txt");
        cache.put(new StringKey(badf.getName()), badf);
        Object bf = cache.get(new StringKey(badf.getName()));
        assertNull(bf);

        File goodf = new File(System.getProperty("java.io.tmpdir"));
        cache.put(new StringKey(goodf.getName()), goodf);
        Object gf = cache.get(new StringKey(goodf.getName()));
        assertTrue(gf instanceof File);
        assertEquals(goodf.toString(), gf.toString());

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
