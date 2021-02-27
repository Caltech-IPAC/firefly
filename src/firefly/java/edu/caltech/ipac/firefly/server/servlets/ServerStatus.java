/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.cache.EhcacheProvider;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.packagedata.PackagingController;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.StringKey;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CachePeer;
import net.sf.ehcache.statistics.StatisticsGateway;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Date: Jun 3, 2009
 *
 * @author loi
 * @version $Id: ServerStatus.java,v 1.1 2009/06/04 00:12:42 loi Exp $
 */
public class ServerStatus extends BaseHttpServlet {

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        boolean showHeaders = Boolean.parseBoolean(req.getParameter("headers"));

        res.addHeader("content-type", "text/plain");
        PrintWriter writer = res.getWriter();
        try {
            showCountStatus(writer);
            skip(writer);

            showPackagingStatus(writer);
            skip(writer);

            showEventsStatus(writer);
            skip(writer);

            EhcacheProvider prov = (EhcacheProvider) edu.caltech.ipac.util.cache.CacheManager.getCacheProvider();

            displayCacheInfo(writer, prov.getEhcacheManager());
            displayCacheInfo(writer, prov.getSharedManager());

            if (showHeaders) {
                skip(writer);
                showHeaders(writer, req);
            }

            // show optional parameters
            writer.println("\n\nAvailable Parameters");
            writer.println(    "--------------------");
            writer.println("headers=[true|false]        Display all request's headers");

        } finally {
            writer.flush();
            writer.close();
        }

    }

    private static void displayCacheInfo(PrintWriter writer, CacheManager cm) {
        writer.println(cm.getName() + " EHCACHE INFORMATION:");
        writer.println("-------------------:");
        writer.println("Manager Status: " + cm.getStatus());
        writer.println("DiskStore Path: " + cm.getConfiguration().getDiskStoreConfiguration().getPath());
        writer.println();

        writer.println("Caches: ");
        Map<String, CacheManagerPeerProvider> peerProvs = cm.getCacheManagerPeerProviders();
        String[] cacheNames = cm.getCacheNames();
        for(String n : cacheNames) {
            Ehcache c = cm.getCache(n);
            writer.println("\t" + c.getName() + " @" + c.hashCode());
            writer.println("\tCache Status    : " + c.getStatus());
            writer.println("\tMax Heap       : " + c.getCacheConfiguration().getMaxBytesLocalHeap()/(1024 * 1024) + "MB");
            writer.println("\tMax Entries    : " + c.getCacheConfiguration().getMaxEntriesLocalHeap());
            writer.println("\tStatistics     : " + getStats(c));
            for (CacheManagerPeerProvider peerProv : peerProvs.values()) {
                List peers = peerProv.listRemoteCachePeers(c);
                for(Object o : peers) {
                    CachePeer cp = (CachePeer) o;
                    try {
                        writer.println("\tReplicating with: " + cp.getUrl());
                    } catch (RemoteException e) {}
                }
            }
            writer.println();
        }
    }

    private static String getStats(Ehcache c) {
        StatisticsGateway sg = c.getStatistics();
        String s = "[" +
                "  Size:" + sg.getSize() +
                "  Expired:" + sg.cacheExpiredCount() +
                "  Evicted:" + sg.cacheEvictedCount() +
                "  Hits:" + sg.cacheHitCount() +
                "  Hit-Ratio:" + sg.cacheHitRatio() +
                "  Heap-Size:" + sg.getLocalHeapSizeInBytes()/(1024 * 1024) + "MB" +
                "  ]";
        return s;
    }

    private static void skip(PrintWriter w) { w.println("\n\n"); }

    private static void showCountStatus(PrintWriter w) {
        w.println(StringUtils.toString(Counters.getInstance().reportStatus(), "\n"));
    }

    private static void showEventsStatus(PrintWriter w) {
        w.println("Server Events Information");
        w.println("  - Total events fired:" + ServerEventManager.getTotalEventCnt());
        w.println("  - Total events delivered:" + ServerEventManager.getDeliveredEventCnt());
        w.println("  - Total active queues:" + ServerEventManager.getActiveQueueCnt());
    }

    private static void showPackagingStatus(PrintWriter w) {
        w.println("Packaging Controller Information");
        w.println(StringUtils.toString(PackagingController.getInstance().getStatus(), "\n"));
    }

    private static void showHeaders(PrintWriter w, HttpServletRequest req) {

        w.println("Request Headers");
        w.println("---------------");
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = Collections.list(req.getHeaders(name)).stream().collect(Collectors.joining(", "));
            w.println(String.format("    %s: %s", name, value));
        }

        String jwt = req.getParameter("jwt");
        if (!StringUtils.isEmpty(jwt)) {
            // these are JWT tokens
            skip(w);
            w.println("  Expanding JWT tokens: " + jwt);
            Arrays.stream(jwt.split(","))
                    .forEach((header) -> {
                                w.println("  " + header + "---->");
                                String idTokenBase64 = req.getHeader(header);
                                if (!StringUtils.isEmpty(idTokenBase64)) {
                                    try {
                                        String[] parts = idTokenBase64.split("\\.");        // 3-part: [how_signed:claims:signature]
                                        if (parts.length == 3) {
                                            w.println("      JWT:" + parts[1]);
                                            String jwtString = new String(Base64.getDecoder().decode(parts[1]));
                                            w.println(jwtString);
                                        } else {
                                            w.println("      not valid JWT:" + idTokenBase64);
                                        }
                                    } catch (Exception e) {
                                        w.println("      not valid JWT:" + idTokenBase64);
                                        w.println(e.getMessage());
                                    }
                                }
                            }
                    );
        }
    }



    boolean isTestRunning = false;
    private void testSharedCache() {

        if (!isTestRunning) {
            isTestRunning = true;
            new Thread(new Runnable(){
                public void run() {
                    // test shared cache
                    Runtime runtime = Runtime.getRuntime();

                    System.out.println("Beginning:");
                    System.out.println("----------------------------");
                    System.out.print("max:" + runtime.maxMemory() / 1024);
                    System.out.print("    alloc:" + runtime.totalMemory()/1024);
                    System.out.println("    free:" + runtime.freeMemory()/1024);
                    System.out.println("----------------------------");
                    Cache testcache = edu.caltech.ipac.util.cache.CacheManager.getCache(Cache.TYPE_VIS_SHARED_MEM);
                    for (int i = 0; i < 1000; i++) {
                        double[] data = new double[1000000];
                        Arrays.fill(data, 12F);
                        testcache.put(new StringKey(System.currentTimeMillis()), data);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {}

                        System.out.println("after " + i + ":");
                        System.out.println("----------------------------");
                        System.out.print("max:" + runtime.maxMemory() / 1024);
                        System.out.print("    alloc:" + runtime.totalMemory() / 1024);
                        System.out.println("    free:" + runtime.freeMemory() / 1024);
                        System.out.println("----------------------------");
                        System.out.flush();
                    }
                    System.out.println("End");
                    System.out.println("----------------------------");
                    System.out.print("max:" + runtime.maxMemory()/1024);
                    System.out.print("    alloc:" + runtime.totalMemory()/1024);
                    System.out.println("    free:" + runtime.freeMemory()/1024);
                    System.out.println("----------------------------");
                }
            }).start();
        }

    }

}
