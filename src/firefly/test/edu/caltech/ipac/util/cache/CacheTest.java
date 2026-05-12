/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util.cache;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.core.RedisService;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.FileUtil;
import org.apache.logging.log4j.Level;
import org.junit.After;
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


    // -----------------------------------------------------------------------
    // VIS_SHARED_MEM — TTI expiry by default, per-entry TTL via put(key, value, secs)
    // -----------------------------------------------------------------------

    @Test
    public void visMemPutGet() {
        Cache<String> cache = CacheManager.getVisMemCache();
        cache.put(new StringKey("vis-basic"), "hello");
        assertEquals("hello", cache.get(new StringKey("vis-basic")));
    }

    @Test
    public void visMemRemove() {
        Cache<String> cache = CacheManager.getVisMemCache();
        cache.put(new StringKey("vis-remove"), "gone");
        cache.remove(new StringKey("vis-remove"));
        assertNull(cache.get(new StringKey("vis-remove")));
    }

    @Test
    public void visMemPerEntryTtlExpires() throws InterruptedException {
        Cache<String> cache = CacheManager.getVisMemCache();
        cache.put(new StringKey("vis-ttl"), "short-lived", 1);
        Thread.sleep(2000);
        assertNull("entry should have expired after TTL", cache.get(new StringKey("vis-ttl")));
    }

    @Test
    public void visMemSurvivesShortIdle() throws InterruptedException {
        Cache<String> cache = CacheManager.getVisMemCache();
        cache.put(new StringKey("vis-tti"), "stays");
        Thread.sleep(500);
        assertEquals("stays", cache.get(new StringKey("vis-tti")));
    }

    // -----------------------------------------------------------------------
    // PERM_SMALL — eternal by default, per-entry TTL via put(key, value, secs)
    // -----------------------------------------------------------------------

    @Test
    public void permPutGet() {
        Cache<String> cache = CacheManager.getLocal();
        cache.put(new StringKey("perm-basic"), "world");
        assertEquals("world", cache.get(new StringKey("perm-basic")));
    }

    @Test
    public void permRemove() {
        Cache<String> cache = CacheManager.getLocal();
        cache.put(new StringKey("perm-remove"), "gone");
        cache.remove(new StringKey("perm-remove"));
        assertNull(cache.get(new StringKey("perm-remove")));
    }

    @Test
    public void permPerEntryTtlExpires() throws InterruptedException {
        Cache<String> cache = CacheManager.getLocal();
        cache.put(new StringKey("perm-ttl"), "short-lived", 1);
        Thread.sleep(2000);
        assertNull("entry should have expired after TTL", cache.get(new StringKey("perm-ttl")));
    }

    @Test
    public void permEntryIsEternal() throws InterruptedException {
        Cache<String> cache = CacheManager.getLocal();
        cache.put(new StringKey("perm-eternal"), "forever");
        Thread.sleep(500);
        assertEquals("forever", cache.get(new StringKey("perm-eternal")));
    }

    // -----------------------------------------------------------------------
    // FileUtil.parseMemoryBytes
    // -----------------------------------------------------------------------

    @Test
    public void parseMemoryBytes() {
        assertEquals(1L * 1024 * 1024 * 1024, FileUtil.parseMemoryBytes("1G"));
        assertEquals(1L * 1024 * 1024 * 1024, FileUtil.parseMemoryBytes("1GB"));
        assertEquals(500L * 1024 * 1024,       FileUtil.parseMemoryBytes("500M"));
        assertEquals(500L * 1024 * 1024,       FileUtil.parseMemoryBytes("500MB"));
        assertEquals(512L * 1024,              FileUtil.parseMemoryBytes("512K"));
        assertEquals(512L * 1024,              FileUtil.parseMemoryBytes("512KB"));
        assertEquals(1234L,                    FileUtil.parseMemoryBytes("1234"));
    }

    @Test
    public void parseMemoryBytesCaseInsensitive() {
        assertEquals(FileUtil.parseMemoryBytes("2G"),   FileUtil.parseMemoryBytes("2g"));
        assertEquals(FileUtil.parseMemoryBytes("2GB"),  FileUtil.parseMemoryBytes("2gb"));
        assertEquals(FileUtil.parseMemoryBytes("100M"), FileUtil.parseMemoryBytes("100m"));
    }

    @Test
    public void parseMemoryBytesNullOrBlank() {
        assertEquals(-1, FileUtil.parseMemoryBytes(null));
        assertEquals(-1, FileUtil.parseMemoryBytes(""));
        assertEquals(-1, FileUtil.parseMemoryBytes("   "));
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
