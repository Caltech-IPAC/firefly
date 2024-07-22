/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.core.background.JobManager;
import edu.caltech.ipac.firefly.messaging.Messenger;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.EhcacheProvider;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.DbMonitor;
import edu.caltech.ipac.firefly.server.db.DuckDbAdapter;
import edu.caltech.ipac.firefly.server.db.HsqlDbAdapter;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.CachePeerProviderFactory;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CachePeer;
import net.sf.ehcache.statistics.StatisticsGateway;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.PrintWriter;
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
        boolean execGC = Boolean.parseBoolean(req.getParameter("execGC"));
        boolean showJobDetails = Boolean.parseBoolean(req.getParameter("job.details"));

        if (execGC)     System.gc();            // force garbage collection.

        ServerContext.Info sInfo = ServerContext.getSeverInfo();
        res.addHeader("content-type", "text/html");
        PrintWriter writer = res.getWriter();
        writer.println("<pre style='font-size: -1'>");
        try {

            // show optional parameters
            writer.println("Available Actions");
            writer.println("--------------------");
            writer.println("<li><a href=./status>Default View</a>:   Default set of information");
            writer.println("<li><a href=./status?headers=true>Full Headers</a>:   Display all request's headers");
            writer.println("<li><a href=./status?job.details=true>Job Details</a>:    View detailed Async Job Information");
            writer.println("<li><a href=./status?execGC=true>Trigger GC</a>:     Invoke JVM garbage collection");
            skip(writer);

            showCountStatus(writer);
            skip(writer);

            showPackagingStatus(writer, showJobDetails);
            skip(writer);

            showMessagingStatus(writer);
            skip(writer);

            showEventsStatus(writer);
            skip(writer);

            showDatabaseStatus(writer);
            skip(writer);

            showWorkAreaStatus(writer);
            skip(writer);

            EhcacheProvider prov = (EhcacheProvider) edu.caltech.ipac.util.cache.CacheManager.getCacheProvider();

            displayCacheInfo(writer, prov.getEhcacheManager(), sInfo);
            displayCacheInfo(writer, prov.getSharedManager(), sInfo);

            if (showHeaders) {
                skip(writer);
                showHeaders(writer, req);
            }

            writer.println("</pre>");

        } finally {
            writer.flush();
            writer.close();
        }

    }

    private static void displayCacheInfo(PrintWriter writer, CacheManager cm, ServerContext.Info sInfo) {
        writer.println(cm.getName() + " EHCACHE INFORMATION:");
        writer.println("-------------------:");
        writer.println("Manager Status: " + cm.getStatus());
        writer.println("DiskStore Path: " + cm.getConfiguration().getDiskStoreConfiguration().getPath());
        writer.println();
        writer.println("Host IP Address: " + sInfo.ip());

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
            if (peerProvs.size()>0) {
                for (CacheManagerPeerProvider peerProv : peerProvs.values()) {
                    List<?> peers = peerProv.listRemoteCachePeers(c);
                    for(Object o : peers) {
                        CachePeer cp = (CachePeer) o;
                        try {
                            writer.println("\tReplicating with: " + cp.getUrl());
                        } catch (RemoteException e) {
                            writer.println("\tFail to connect: " + cp);
                        }
                    }
                }
            }
            else {
                writer.println("\tNot replicating");
            }
            writer.println();
        }
    }

    private static void showDatabaseStatus(PrintWriter writer) {
        DbAdapter.EmbeddedDbStats stats = DbMonitor.getRuntimeStats(true);
        String driver;
        if (DbAdapter.DEF_DB_TYPE.equals(DuckDbAdapter.NAME)) {
            duckDbConfig(writer);
            driver = DuckDbAdapter.DRIVER;
        } else {
            hsqldbConfig(writer, stats);
            driver = HsqlDbAdapter.DRIVER;
        }

        writer.printf(""" 
            <div style="font-size:small">
            To browse the data in these databases, follow these steps:            
            1. Open the <a href=%sadmin/db/ target='_blank'>Database Console</a>.
            2. Enter the Driver Class: %s
            3. Enter the JDBC URL: Copy and paste a JDBC URL from the options below that you wish to browse.
            4. Click "Connect"
            </div>
            """, ServerContext.getRequestOwner().getBaseUrl(), driver);
        writer.println("Idled   Age     Rows        Columns  Tables  Total Rows       Memory  JDBC URL     (elapsed time are in min:sec; memory is in MB)");
        writer.println("------  ------  ----------  -------  ------  ----------       ------  ---------");
        DbMonitor.getDbInstances().values().stream()
            .sorted((db1, db2) -> Long.compare(db2.getLastAccessed(), db1.getLastAccessed()))
            .forEach((db) -> writer.printf("%7$tM:%7$tS   %8$tM:%8$tS   %,10d  %7d  %6d  %,10d  %11.1f  %s\n",
                db.getDbStats().rowCnt(),
                db.getDbStats().colCnt(),
                db.getDbStats().tblCnt(),
                db.getDbStats().totalRows(),
                db.getDbStats().memory()/1024/1024.0,
                db.getDbUrl(),
                System.currentTimeMillis() - db.getLastAccessed(),
                System.currentTimeMillis() - db.getCreated()
        ));
    }

    private static void hsqldbConfig(PrintWriter writer, DbAdapter.EmbeddedDbStats stats) {
        writer.println("DATABASE INFORMATION");
        writer.println("--------------------");
        writer.printf("MAX_IDLE(min):     %,10d  MAX_IDLE_RSC(min):   %,10d\n", DbMonitor.MAX_IDLE_TIME/1000/60, DbMonitor.MAX_IDLE_TIME_RSC/1000/60);
        writer.printf("MAX_MEM_ROWS(m):   %,10d  COMPACT_FACTOR:      %10.2f\n", stats.maxMemRows/1_000_000, stats.compactFactor);
        writer.printf("DB In Memory:      %,10d  Total DB count:      %,10d\n", stats.memDbs, stats.totalDbs);
        writer.printf("Rows In Memory:    %,10d  Peak Rows In Memory: %,10d\n", stats.memRows, stats.peakMemRows);
        writer.println("Cleanup Last Ran:  " + new SimpleDateFormat("HH:mm:ss").format(stats.lastCleanup));
    }

    private static void duckDbConfig(PrintWriter w) {
        w.println("DUCKDB CONFIGURATION");
        w.println("-".repeat(136));
        w.printf("| %20s | %20s | %60s | %10s | %10s |\n".formatted("name", "value", "description", "input_type", "scope" ));
        w.println("-".repeat(136));
        var dg = DuckDbAdapter.getDuckDbSettings();
        if (dg != null) {
            dg.forEach(r -> {
                w.printf("| %20s | %20s | %60s | %10s | %10s |\n".formatted(r.getData()));
            });
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
        int qCnt= ServerEventManager.getActiveQueueCnt();
        w.println("  - Total active queues:" + qCnt);
        if(qCnt>0) {
            w.println("  - "+ (qCnt>10? "10 Most recently used channels:" :  "Channel list, ordered by last use:"));
            w.println(makeQueueList());
        }
    }

    private static String makeQueueList() {
        return ServerEventManager.getQueueDescriptionList(10).stream()
                .map( d -> String.format("     - %s, %s\n",d.channel(), new Date(d.lastPutTime())))
                .reduce("", (all, entry) -> all+entry);
    }

    private static void showMessagingStatus(PrintWriter w) {
        w.println("Messenger: Redis host: " + Messenger.getRedisHostPortDesc());
        w.println("Messaging Pool: " + Messenger.getStats());
    }

    private static void showPackagingStatus(PrintWriter w, boolean details) {
        w.println("Async Job Information");
        w.println();
        w.println(JobManager.getStatistics(details));
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
