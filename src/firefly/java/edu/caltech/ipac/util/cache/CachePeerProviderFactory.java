/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util.cache;

import edu.caltech.ipac.firefly.messaging.Message;
import edu.caltech.ipac.firefly.messaging.Messenger;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.StringUtils;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.distribution.CacheManagerPeerListener;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CacheManagerPeerProviderFactory;
import net.sf.ehcache.distribution.CachePeer;
import net.sf.ehcache.distribution.RMICacheManagerPeerProvider;
import net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory;
import net.sf.ehcache.util.PropertyUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Custom factory for deciding the discovery method to use when creating cache replication.
 * Currently, peerDiscovery may be "PubSub" or "MultiCast".  When not given, "MultiCast" is used.
 * Similar to MultiCast, multicastGroupPort is used in PubSub to group related caches for replication.
 *
 * Date: 2019-04-08
 *
 * @author loi
 * @version $Id: $
 */
public class CachePeerProviderFactory extends CacheManagerPeerProviderFactory {
    static final Logger.LoggerImpl LOG = Logger.getLogger();
    static final String PEER_DISCOVERY = "peerDiscovery";

    public CacheManagerPeerProvider createCachePeerProvider(CacheManager cacheManager, Properties properties) throws CacheException {

        String discoveryType = PropertyUtil.extractAndLogProperty(PEER_DISCOVERY, properties);
        if (discoveryType != null && discoveryType.equals("PubSub")) {
            return new PubSubCachePeerProvider(cacheManager, properties);
        } else {
            properties.put(PEER_DISCOVERY, "automatic");        // for multicast
            return new RMICacheManagerPeerProviderFactory().createCachePeerProvider(cacheManager, properties);
        }
    }


    public static CachePeer getFirstLocalRmiCachePeer(CacheManager cm) {
        CacheManagerPeerListener cpl = cm.getCachePeerListener("RMI");
        if (cpl != null) {
            List bcp = cpl.getBoundCachePeers();
            if (bcp.size() > 0) return (CachePeer) bcp.get(0);
        }
        return null;
    }

//=====================================================================================
//  PubSubCachePeerProvider implementation based on Firefly's PubSub messaging
//=====================================================================================

    static class PubSubCachePeerProvider extends RMICacheManagerPeerProvider {
        private static final int HEARTBEAT_INTERVAL = 2000;                         // every 2 seconds
        private static final String MULTICAST_GROUP_PORT = "multicastGroupPort";
        private static final String CACHE_ALIVE = "cache-alive::";
        private static final String URL_DELIMITER = "|";

        CacheManager cacheManager;
        private String cacheGroup;
        private int cachePeersHash;     // current hash of local cachePeers
        private List<String> localRmiUrls;

        PubSubCachePeerProvider(CacheManager cacheManager, Properties properties) {
            super(cacheManager);

            // like multicast, use MULTICAST_GROUP_PORT to group related caches
            cacheGroup = PropertyUtil.extractAndLogProperty(MULTICAST_GROUP_PORT, properties);
            this.cacheManager = cacheManager;
        }

        @Override
        public void init() {
            String topic = CACHE_ALIVE + cacheGroup;

            // listen for new peers
            Messenger.subscribe(topic, (msg) -> processMsg(msg));

            // sending heartbeats
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    () -> Messenger.publish(topic, new Message().setValue(getSelfRmiUrls())),
                    1000, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);           // delay for 1 second to give time for ecache to fully initialized.
        }

        private void processMsg(Message msg) {
            String rmiUrls = msg.getValue("").trim();

            if (isSelf(rmiUrls)) {
                return;
            }

            for(String url : rmiUrls.split(Pattern.quote(URL_DELIMITER))) {
                registerPeer(url);
            };

        }

        /**
         * @param rmiUrls
         * @return true if our own hostname and listener port are found in the list. This then means we have
         * caught our onw multicast, and should be ignored.
         */
        private boolean isSelf(String rmiUrls) {
            CachePeer peer = getFirstLocalRmiCachePeer(cacheManager);
            String cacheManagerUrlBase = null;
            try {
                cacheManagerUrlBase = peer.getUrlBase();
            } catch (RemoteException e) {
                LOG.error("Error geting url base");
            }
            int baseUrlMatch = rmiUrls.indexOf(cacheManagerUrlBase);
            return baseUrlMatch != -1;
        }

        private String getSelfRmiUrls() {
            CacheManagerPeerListener cacheManagerPeerListener = cacheManager.getCachePeerListener("RMI");
            if (cacheManagerPeerListener == null) {
                LOG.warn("The RMICacheManagerPeerListener is missing. You need to configure a cacheManagerPeerListenerFactory" +
                        " with class=\"net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory\" in ehcache.xml.");
                return "";
            }
            List localCachePeers = cacheManagerPeerListener.getBoundCachePeers();
            int newCachePeersHash = localCachePeers.hashCode();
            if (cachePeersHash != localCachePeers.hashCode()) {
                cachePeersHash = newCachePeersHash;
                localRmiUrls = new ArrayList<>();
                for (Object localCachePeer : localCachePeers) {
                    CachePeer cachePeer = (CachePeer) localCachePeer;
                    try {
                        localRmiUrls.add(cachePeer.getUrl());
                    } catch (RemoteException e) {
                        LOG.error("This should never be thrown as it is called locally");
                    }
                }
            }
            return StringUtils.toString(localRmiUrls, URL_DELIMITER);
        }

        @Override
        public void dispose() throws CacheException {
            super.dispose();
        }

        /**
         * @return Time for a cluster to form. This varies considerably, set to 5s.
         */
        public long getTimeForClusterToForm() {
            return 5000;
        }

        @Override
        public void registerPeer(String rmiUrl) {
            try {
                CachePeerEntry cachePeerEntry = (CachePeerEntry) peerUrls.get(rmiUrl);
                if (cachePeerEntry == null || stale(cachePeerEntry.date)) {
                    //can take seconds if there is a problem
                    CachePeer cachePeer = lookupRemoteCachePeer(rmiUrl);
                    cachePeerEntry = new CachePeerEntry(cachePeer, new Date());
                    //synchronized due to peerUrls being a synchronizedMap
                    peerUrls.put(rmiUrl, cachePeerEntry);
                } else {
                    cachePeerEntry.date = new Date();
                }
            } catch (IOException e) {
                    LOG.debug("Unable to lookup remote cache peer for " + rmiUrl + ". Removing from peer list. Cause was: "
                            + e.getMessage());
                unregisterPeer(rmiUrl);
            } catch (NotBoundException e) {
                peerUrls.remove(rmiUrl);
                LOG.debug("Unable to lookup remote cache peer for " + rmiUrl + ". Removing from peer list. Cause was: "
                            + e.getMessage());
            } catch (Throwable t) {
                LOG.error("Unable to lookup remote cache peer for " + rmiUrl
                        + ". Cause was not due to an IOException or NotBoundException which will occur in normal operation:" +
                        " " + t.getMessage());
            }
        }

        @Override
        public List listRemoteCachePeers(Ehcache cache) throws CacheException {
            List<CachePeer> remoteCachePeers = new ArrayList<>();
            List<String> staleList = new ArrayList<>();
            synchronized (peerUrls) {
                for (Object o : peerUrls.keySet()) {
                    String rmiUrl = (String) o;
                    String rmiUrlCacheName = extractCacheName(rmiUrl);
                    try {
                        if (!rmiUrlCacheName.equals(cache.getName())) {
                            continue;
                        }
                        CachePeerEntry cachePeerEntry = (CachePeerEntry) peerUrls.get(rmiUrl);
                        Date date = cachePeerEntry.date;
                        if (!stale(date)) {
                            CachePeer cachePeer = cachePeerEntry.cachePeer;
                            remoteCachePeers.add(cachePeer);
                        } else {

                            LOG.debug("rmiUrl is stale. Either the remote peer is shutdown or the " +
                                            "network connectivity has been interrupted. Will be removed from list of remote cache peers",
                                    rmiUrl);
                            staleList.add(rmiUrl);
                        }
                    } catch (Exception exception) {
                        LOG.error(exception);
                        throw new CacheException("Unable to list remote cache peers. Error was " + exception.getMessage());
                    }
                }
                //Must remove entries after we have finished iterating over them
                for (String rmiUrl : staleList) {
                    peerUrls.remove(rmiUrl);
                }
            }
            return remoteCachePeers;
        }

        @Override
        protected boolean stale(Date date) {
            long now = System.currentTimeMillis();
            return date.getTime() < (now - HEARTBEAT_INTERVAL);
        }

        static String extractCacheName(String rmiUrl) {
            return rmiUrl.substring(rmiUrl.lastIndexOf('/') + 1);
        }

    }

    /**
     * Entry containing a looked up CachePeer and date
     */
    protected static final class CachePeerEntry {

        private final CachePeer cachePeer;
        private Date date;

        /**
         * Constructor
         *
         * @param cachePeer the cache peer part of this entry
         * @param date      the date part of this entry
         */
        public CachePeerEntry(CachePeer cachePeer, Date date) {
            this.cachePeer = cachePeer;
            this.date = date;
        }

        /**
         * @return the cache peer part of this entry
         */
        public final CachePeer getCachePeer() {
            return cachePeer;
        }


        /**
         * @return the date part of this entry
         */
        public final Date getDate() {
            return date;
        }

    }
}
