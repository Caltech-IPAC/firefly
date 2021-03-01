/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.messaging.Messenger;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.EhcacheProvider;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.packagedata.PackagingController;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CachePeerProviderFactory;
import edu.caltech.ipac.util.cache.StringKey;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CachePeer;
import net.sf.ehcache.statistics.StatisticsGateway;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.server.ServerContext.ACCESS_TEST_EXT;


/**
 * Display server's information, i.e. jvm, ehcache, counters, packaging queue, messaging status, event queue, embeded db
 *
 * optional parameters:
 *  headers=true    : show request headers, including JWT if exists
 *
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

            showWorkAreaStatus(writer);
            skip(writer);

            showPackagingStatus(writer);
            skip(writer);

            showMessagingStatus(writer);
            skip(writer);

            showEventsStatus(writer);
            skip(writer);

            EhcacheProvider prov = (EhcacheProvider) edu.caltech.ipac.util.cache.CacheManager.getCacheProvider();

            displayCacheInfo(writer, prov.getEhcacheManager());
            displayCacheInfo(writer, prov.getSharedManager());
            skip(writer);

            showDatabaseStatus(writer);

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

        try {
            writer.println("Host IP Address: " + InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            writer.println("Host IP Address: n/a" );
        }

        writer.println("Caches: ");
        Map<String, CacheManagerPeerProvider> peerProvs = cm.getCacheManagerPeerProviders();
        String[] cacheNames = cm.getCacheNames();
        CachePeer cachePeer = CachePeerProviderFactory.getFirstLocalRmiCachePeer(cm);
        for(String n : cacheNames) {
            Ehcache c = cm.getCache(n);
            try {
                writer.println("\t" + c.getName() + " @" + (cachePeer == null? c.hashCode() : cachePeer.getUrlBase()));
            } catch (RemoteException e) {
                // should not happen
            }
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
                    } catch (RemoteException e) {
                        writer.println("\tFail to connect: " + cp.toString());
                    }
                }
            }
            writer.println();
        }
    }

    private static void showDatabaseStatus(PrintWriter writer) {

        DbAdapter.EmbeddedDbStats stats = DbAdapter.getAdapter().getRuntimeStats();
        writer.println("DATABASE INFORMATION");
        writer.println("--------------------");
        writer.printf("CHECK_INTVL(secs): %,10d  MAX_IDLE(min):       %,10d\n", DbAdapter.CLEANUP_INTVL/1000, DbAdapter.MAX_IDLE_TIME/1000/60);
        writer.printf("DB In Memory:      %,10d  Total DB count:      %,10d\n", stats.memDbs, stats.totalDbs);
        writer.printf("MAX_MEM_ROWS:      %,10d  PEAK_MAX_MEM_ROWS:   %,10d\n", stats.maxMemRows, stats.peakMaxMemRows);
        writer.printf("Rows In Memory:    %,10d  Peak Rows In Memory: %,10d\n", stats.memRows, stats.peakMemRows);
        writer.println(              "Cleanup Last Ran:  " + new SimpleDateFormat("HH:mm:ss").format(stats.lastCleanup));
        writer.println("");
        writer.println("Idled   Age     Tables  Rows        Columns  File Path         (elapsed time are in min:sec)");
        writer.println("------  ------  ------  ----------  -------  ---------");
        Collections.unmodifiableCollection(DbAdapter.getAdapter().getDbInstances().values()).stream()
                    .sorted((db1, db2) -> Long.compare(db2.getLastAccessed(), db1.getLastAccessed()))
                    .forEach((db) -> writer.printf("%5$tM:%5$tS   %6$tM:%6$tS   %6d  %,10d  %,7d  %s\n",
                                                        db.getTblCount(),
                                                        db.getRowCount(),
                                                        db.getColCount(),
                                                        db.getDbFile().getPath(),
                                                        System.currentTimeMillis() - db.getLastAccessed(),
                                                        System.currentTimeMillis() - db.getCreated()
                    ));
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


    private static void showWorkAreaStatus(PrintWriter w) {
        w.println("Work Areas");
        w.println("  - Internal: " + ServerContext.getWorkingDir().toString());
        File sharedDir= ServerContext.getSharedWorkingDir();
        if (sharedDir.equals(ServerContext.getWorkingDir())) {
            File sharedDirRequested= ServerContext.getSharedWorkingDirRequested();
            if (sharedDirRequested!=null && !sharedDirRequested.canWrite()) {
                w.println("  - Shared working dir does not have write access, using Internal work area");
                w.println("  - Shared (failed): " + ServerContext.getSharedWorkingDirRequested());
            }
            else {
                w.println("  - No Shared work area defined.");
            }
        }
        else {
            w.println("  - Shared:   " + sharedDir.toString());
            File [] matchFiles= FileUtil.listFilesWithExtension(sharedDir, ACCESS_TEST_EXT);
            if (matchFiles.length==0) return;
            int maxLen= 5;
            Arrays.sort(matchFiles, (f1,f2) -> (int)(f2.lastModified()-f1.lastModified()));
            w.println("              Most recent host using the shared working directory:");
            int len= Math.min(maxLen,matchFiles.length);
            for(int i=0; (i<len); i++) {
                File f= matchFiles[i];
                String hostname= f.getName().substring(0,f.getName().length()- ACCESS_TEST_EXT.length()-1);
                String detailStr= hostname.equals(FileUtil.getHostname()) ? "(self) " : "";
                if (isDocker(hostname)) detailStr+= "(probably Docker)";
                w.println(String.format( "%19s%-20s  --  %s %s",
                        "- ", hostname, new Date(f.lastModified()).toString(), detailStr ));
            }
            if (matchFiles.length>maxLen) w.println("              and " + (matchFiles.length-maxLen) + " more");
        }
    }

    private static boolean isDocker(String hostname) {
        try {
            Long.parseLong(hostname, 16);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static void showEventsStatus(PrintWriter w) {
        w.println("Server Events Information");
        w.println("  - Total events fired:" + ServerEventManager.getTotalEventCnt());
        w.println("  - Total events delivered:" + ServerEventManager.getDeliveredEventCnt());
        w.println("  - Total active queues:" + ServerEventManager.getActiveQueueCnt());
    }

    private static void showMessagingStatus(PrintWriter w) {
        w.println("Messaging Pool: " + Messenger.getStats());
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
}
