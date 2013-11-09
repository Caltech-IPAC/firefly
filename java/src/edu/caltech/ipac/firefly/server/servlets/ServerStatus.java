package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.cache.EhcacheProvider;
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
import java.util.List;
import java.util.Map;

/**
 * Date: Jun 3, 2009
 *
 * @author loi
 * @version $Id: ServerStatus.java,v 1.1 2009/06/04 00:12:42 loi Exp $
 */
public class ServerStatus extends BaseHttpServlet {

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        res.addHeader("content-type", "text/plain");
        PrintWriter writer = res.getWriter();
        try {
            showCountStatus(writer);
            skip(writer);

            showPackagingStatus(writer);
            skip(writer);

            EhcacheProvider prov = (EhcacheProvider) edu.caltech.ipac.util.cache.CacheManager.getCacheProvider();

            displayCacheInfo(writer, prov.getEhcacheManager());
            displayCacheInfo(writer, prov.getSharedManager());

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
            writer.println("\t" + c.getName());
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

    private static void showPackagingStatus(PrintWriter w) {
        w.println("Packaging Controller Information");
        w.println(StringUtils.toString(PackagingController.getInstance().getStatus(), "\n"));
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
                    Cache testcache = edu.caltech.ipac.util.cache.CacheManager.getSharedCache(Cache.TYPE_VIS_SHARED_MEM);
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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
