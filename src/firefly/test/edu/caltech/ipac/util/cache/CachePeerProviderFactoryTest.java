/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util.cache;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.messaging.Messenger;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CachePeer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * This test suite requires a running Redis.  To start one locally..
 * $ docker run --name test-redis -p 6379:6379 -d redis
 *
 * Date: 2019-04-11
 * @author loi
 * @version $Id: $
 */
public class CachePeerProviderFactoryTest extends ConfigTest {

    private static CacheManager peer1, peer2, peer3;
    private static CacheManager mcPeer1, mcPeer2, mcPeer3;

    /**
     * 2 replicating caches: cache_1 and cache_2.  Both using the default settings.  This is a minimal config
     * intended for connection test.  Not meant to be used in production.
     * change peerDiscovery=PubSub to peerDiscovery=MultiCast to test using multicast
     *
     * Change to DEBUG level to see output.  i.e. Logger.setProLog(Level.DEBUG, null);
     *
     */
    private static String ehcacheConfig =
            "<ehcache xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n" +
                    "         xsi:noNamespaceSchemaLocation='ehcache.xsd' name='${CACHE_NAME}'>\n" +
                    "    <diskStore path='java.io.tmpdir/ehcache/firefly'/>\n" +
                    "    <cacheManagerEventListenerFactory class='' properties=''/>\n" +
                    "    <cacheManagerPeerProviderFactory\n" +
                    "            class='edu.caltech.ipac.util.cache.CachePeerProviderFactory'\n" +
                    "            properties='peerDiscovery=${DISCOVERY_TYPE},\n" +
                    "                        multicastGroupPort=4446,\n" +
                    "                        multicastGroupAddress=239.255.0.1, timeToLive=1'\n" +
                    "            propertySeparator=',' />\n" +
                    "    <cacheManagerPeerListenerFactory\n" +
                    "            class='net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory'\n" +
                    "            properties='socketTimeoutMillis=300000'\n" +
                    "            propertySeparator=',' />\n" +
                    "    <defaultCache\n" +
                    "            maxElementsInMemory='100'\n" +
                    "            eternal='false'\n" +
                    "            memoryStoreEvictionPolicy='LRU' />\n" +
                    "    <cache name='cache_1' maxElementsInMemory='200'>\n" +
                    "        <cacheEventListenerFactory\n" +
                    "            class='net.sf.ehcache.distribution.RMICacheReplicatorFactory'\n" +
                    "            properties='asynchronousReplicationIntervalMillis=100,\n" +
                    "            replicateAsynchronously=true'\n" +
                    "            propertySeparator=',' />\n" +
                    "    </cache>\n" +
                    "    <cache name='cache_2' maxElementsInMemory='100'>\n" +
                    "        <cacheEventListenerFactory\n" +
                    "            class='net.sf.ehcache.distribution.RMICacheReplicatorFactory'\n" +
                    "            properties='asynchronousReplicationIntervalMillis=100,\n" +
                    "            replicateAsynchronously=true'\n" +
                    "            propertySeparator=',' />\n" +
                    "    </cache>\n" +
                    "</ehcache>";

    @BeforeClass
    public static void setUp() throws InterruptedException {

        if (Messenger.isOffline()) {
            System.out.println("Messenger is offline; skipping all tests in CachePeerProviderFactoryTest.");
            return;
        }

        LOG.debug("Initial setup for PubSub, creating 3 peers");
        peer1 = createCM("PubSub", "peer1");
        peer2 = createCM("PubSub", "peer2");
        peer3 = createCM("PubSub", "peer3");

        LOG.debug("Initial setup for MultiCast, also the similar 3 peers");
        mcPeer1 = createCM("MultiCast", "mcPeer1");
        mcPeer2 = createCM("MultiCast", "mcPeer2");
        mcPeer3 = createCM("MultiCast", "mcPeer3");

        LOG.debug("wait for a few seconds for things to load.. not the best approach for async unit tests.. should revisit later");
        Thread.sleep(5000);
    }


    @Test
    public void pubSub_InitialSetup() throws InterruptedException, RemoteException {
        if (Messenger.isOffline()) return;

        assertEquals("Number of caches", 2, peer1.getCacheNames().length);

        LOG.debug("check that peer1 has 4 peer caches it replicates with (2 peers x 2 caches)");
        assertEquals("Replicated cache count", 4, getReplicatedCacheCnt(peer1));

        LOG.debug("check that peer2 has 4 peer caches it replicates with (2 peers x 2 caches)");
        assertEquals("Replicated cache count", 4, getReplicatedCacheCnt(peer2));

        LOG.debug("check that peer3 has 4 peer caches it replicates with (2 peers x 2 caches)");
        assertEquals("Replicated cache count", 4, getReplicatedCacheCnt(peer3));
    }


    @Test
    public void pubSub_CacheReplication() throws InterruptedException, RemoteException {
        if (Messenger.isOffline()) return;

        LOG.debug("put a few entries into peer1... it should be replicated to the rest of the peers");
        peer1.getCache("cache_1").put(new Element("key1", "value1"));
        peer1.getCache("cache_1").put(new Element("key2", 2));
        peer1.getCache("cache_2").put(new Element("key3", 3.14));

        LOG.debug("wait for replication");
        Thread.sleep(1000);

        LOG.debug("test the entries in peer2");
        assertEquals("peer2 key1:", "value1", getCacheVal(peer2, "cache_1", "key1"));
        assertEquals("peer2 key2:", 2, getCacheVal(peer2, "cache_1", "key2"));
        assertEquals("peer2 key3:", 3.14, getCacheVal(peer2, "cache_2", "key3"));

        LOG.debug("test the entries in peer3");
        assertEquals("peer3 key1:", "value1", getCacheVal(peer3, "cache_1", "key1"));
        assertEquals("peer3 key2:", 2, getCacheVal(peer3, "cache_1", "key2"));
        assertEquals("peer3 key3:", 3.14, getCacheVal(peer3, "cache_2", "key3"));
    }

    @Test
    public void pubSub_PeerAddDrop() throws InterruptedException, RemoteException {
        if (Messenger.isOffline()) return;

        LOG.debug("testing drop-off.... shutdown peer1");
        peer1.shutdown();
        LOG.debug("waiting for heatbeats to register...");
        Thread.sleep(5000);     // heabeats interval is declared here: PubSubCachePeerProvider.HEARTBEAT_INTERVAL + the 1s delay

        LOG.debug("there should only be 2 replicating caches for each peer (1 peers x 2 caches)");
        assertEquals("Replicated cache count", 2, getReplicatedCacheCnt(peer2));
        assertEquals("Replicated cache count", 2, getReplicatedCacheCnt(peer3));


        LOG.debug("testing new peers... start peer1 back up");
        LOG.debug("waiting for heatbeats to register...");
        peer1 = createCM("PubSub", "peer1");

        LOG.debug("waiting for heatbeats to register...");
        Thread.sleep(5000);     // heabeats interval is declared here: PubSubCachePeerProvider.HEARTBEAT_INTERVAL + the 1s delay

        LOG.debug("there should 4 replicating caches for each peer again (2 peers x 2 caches)");
        assertEquals("Replicated cache count", 4, getReplicatedCacheCnt(peer1));
        assertEquals("Replicated cache count", 4, getReplicatedCacheCnt(peer2));
        assertEquals("Replicated cache count", 4, getReplicatedCacheCnt(peer3));
    }

//====================================================================
//  tests for MultiCasts
//====================================================================


    @Test
    public void multicast_InitialSetup() throws InterruptedException, RemoteException {
        if (Messenger.isOffline()) return;

        assertEquals("Number of caches", 2, mcPeer1.getCacheNames().length);

        LOG.debug("check that mcPeer1 has 4 peer caches it replicates with (2 peers x 2 caches)");
        assertEquals("Replicated cache count", 4, getReplicatedCacheCnt(mcPeer1));

        LOG.debug("check that mcPeer2 has 4 peer caches it replicates with (2 peers x 2 caches)");
        assertEquals("Replicated cache count", 4, getReplicatedCacheCnt(mcPeer2));

        LOG.debug("check that mcPeer3 has 4 peer caches it replicates with (2 peers x 2 caches)");
        assertEquals("Replicated cache count", 4, getReplicatedCacheCnt(mcPeer3));
    }


    @Test
    public void multicast_CacheReplication() throws InterruptedException, RemoteException {
        if (Messenger.isOffline()) return;

        LOG.debug("put a few entries into mcPeer1... it should be replicated to the rest of the peers");
        mcPeer1.getCache("cache_1").put(new Element("key1", "value1"));
        mcPeer1.getCache("cache_1").put(new Element("key2", 2));
        mcPeer1.getCache("cache_2").put(new Element("key3", 3.14));

        LOG.debug("wait for replication");
        Thread.sleep(2000);

        LOG.debug("test the entries in mcPeer2");
        assertEquals("peer2 key1:", "value1", getCacheVal(mcPeer2, "cache_1", "key1"));
        assertEquals("peer2 key2:", 2, getCacheVal(mcPeer2, "cache_1", "key2"));
        assertEquals("peer2 key3:", 3.14, getCacheVal(mcPeer2, "cache_2", "key3"));

        LOG.debug("test the entries in mcPeer3");
        assertEquals("peer3 key1:", "value1", getCacheVal(mcPeer3, "cache_1", "key1"));
        assertEquals("peer3 key2:", 2, getCacheVal(mcPeer3, "cache_1", "key2"));
        assertEquals("peer3 key3:", 3.14, getCacheVal(mcPeer3, "cache_2", "key3"));
    }

    @Test
    public void multicast_PeerAddDrop() throws InterruptedException, RemoteException {
        if (Messenger.isOffline()) return;

        LOG.debug("testing drop-off.... shutdown mcPeer1");
        mcPeer1.shutdown();
        LOG.debug("waiting for heatbeats to register...");
        Thread.sleep(15000);     // Multicast interval is 5s.. but, for some reason, it takes longer for peers to register/drop-off.

        LOG.debug("there should only be 2 replicating caches for each peer (1 peers x 2 caches)");
        assertEquals("Replicated cache count", 2, getReplicatedCacheCnt(mcPeer2));
        assertEquals("Replicated cache count", 2, getReplicatedCacheCnt(mcPeer3));


        LOG.debug("testing new peers... start mcPeer1 back up");
        LOG.debug("waiting for heatbeats to register...");

        mcPeer1 = createCM("MultiCast", "mcPeer1");
        LOG.debug("waiting for heatbeats to register...");
        Thread.sleep(15000);     // Multicast interval is 5s.. but, for some reason, it takes longer for peers to register/drop-off.

        LOG.debug("there should 4 replicating caches for each peer again (2 peers x 2 caches)");
        assertEquals("Replicated cache count", 4, getReplicatedCacheCnt(mcPeer1));
        assertEquals("Replicated cache count", 4, getReplicatedCacheCnt(mcPeer2));
        assertEquals("Replicated cache count", 4, getReplicatedCacheCnt(mcPeer3));
    }

//====================================================================


    private static CacheManager createCM(String type, String name) {
        return CacheManager.newInstance(new ByteArrayInputStream(
                ehcacheConfig.replace("${CACHE_NAME}", name)
                        .replace("${DISCOVERY_TYPE}", type).getBytes()));

    }

    private static Object getCacheVal(CacheManager cm, String cache, String key) {
        Element el = cm.getCache(cache).get(key);
        return el != null ? el.getObjectValue() : null;
    }

    private static int getReplicatedCacheCnt(CacheManager cm) throws RemoteException {
        CachePeer peer = (CachePeer) cm.getCachePeerListener("RMI").getBoundCachePeers().get(0);
        String thisUrl = peer.getUrlBase();
        Map<String, CacheManagerPeerProvider> peerProvs = cm.getCacheManagerPeerProviders();
        String[] cacheNames = cm.getCacheNames();

        int replicatedCnt = 0;
        LOG.debug("Details info for " + cm.getName());
        for(String n : cacheNames) {
            Ehcache c = cm.getCache(n);
            LOG.debug("\t" + c.getName() + " at " + thisUrl);
            LOG.debug("\tCache Status    : " + c.getStatus());

            for (CacheManagerPeerProvider peerProv : peerProvs.values()) {
                List peers = peerProv.listRemoteCachePeers(c);
                for(Object o : peers) {
                    CachePeer cp = (CachePeer) o;
                    try {
                        LOG.debug("\tReplicating with: " + cp.getUrl());
                        replicatedCnt++;
                    } catch (RemoteException e) {
                        LOG.debug("\tFail to connect: " + cp.toString());
                    }
                }
            }
        }
        LOG.debug("");
        return replicatedCnt;
    }
}
