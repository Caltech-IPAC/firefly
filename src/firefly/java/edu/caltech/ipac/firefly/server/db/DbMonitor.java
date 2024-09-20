/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Date: 5/3/24
 *
 * @author loi
 * @version : $
 */
public
class DbMonitor {
    /*
      CLEAN UP POLICY:
        A rough estimate:
        1m rows; 30 cols table when displayed in triview(table+chart+image) uses 800mb -> 4 tables; 2m rows
        After 2 sort, uses 1.5g ->  10 tables; 6m rows
        2 more sort, uses 2.2g ->  16 tables; 10m rows

        There are two stages of clean-up; compact(remove all temp tables), then shutdown(remove from memory)
        - Compact DB once it has idled longer than a time based on COMPACT_FACTOR
        - Shutdown DB once it has expired; idle longer than MAX_IDLE_TIME
        - Shutdown DB based on LRU(Least Recently Used) once the total rows have exceeded MAX_MEMORY_ROWS

        Default settings:
          - CLEANUP_INTVL:  1 minutes
          - MAX_IDLE_TIME: 15 minutes
          - MAX_IDLE_TIME_RSC: MAX_IDLE_TIME
          - COMPACT_FACTOR: .5
          - MAX_MEMORY_ROWS:  1m rows for every 1GB of memory at startup. minimum of 2 million.
     */
    public static final long MAX_IDLE_PROP  = AppProperties.getLongProperty("dbTbl.maxIdle", 15);                       // idle time before DB is shutdown.  Defaults to 15 minutes.
    public static final long MAX_IDLE_TIME_RSC = AppProperties.getLongProperty("dbRsc.maxIdle", MAX_IDLE_PROP) * 1000 * 60;  // same as dbTbl.maxIdle, but for Resource tables.
    public static final float COMPACT_FACTOR = AppProperties.getFloatProperty("dbTbl.compactFactor", 0.5f);             // when to compact the DB as a factor of MAX_IDLE.  defaults to 1/2 of MAX_IDLE_TIME
    public static final int  CLEANUP_INTVL  = 1000 * 60;        // check every 1 minutes

    public static long MAX_MEM_ROWS   = AppProperties.getLongProperty("dbTbl.maxMemRows", maxMemRows());
    public static long MAX_MEMORY   = AppProperties.getLongProperty("dbTbl.maxMemory", maxMemory());
    public static long MAX_IDLE_TIME  = MAX_IDLE_PROP * 1000 * 60;                                                          // max idle time in ms

    /**
     * When met, system will aggressively shutdown DBs even before expiry time starting with the
     * earliest last modified date.
     * @return total number of rows allowed in memory at startup.
     */
    private static long maxMemRows() {
        long freeMem = Runtime.getRuntime().maxMemory();                // using designated memory(-Xmx) to simplify the logic.
        return Math.max(2000000, freeMem/1024/1024/1024 * 1000000);     // minimum 2m rows
    }

    private static long maxMemory() {
        return (long) (Runtime.getRuntime().maxMemory() * .75);         // force cleanup when memory exceeded this number
    }

    private static final ConcurrentHashMap<String, DbAdapter.EmbeddedDbInstance> dbInstances = new ConcurrentHashMap<>();
    private static final DbAdapter.EmbeddedDbStats dbStats = new DbAdapter.EmbeddedDbStats();
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    public static ConcurrentHashMap<String, DbAdapter.EmbeddedDbInstance> getDbInstances() {
        return dbInstances;
    }

    public static DbAdapter.EmbeddedDbStats getDbStats() {
        return dbStats;
    }

    public static DbAdapter.EmbeddedDbStats getRuntimeStats() {
        return getRuntimeStats(false);
    }

    public static DbAdapter.EmbeddedDbStats getRuntimeStats(boolean doUpdate) {
        if (doUpdate) updateDbStats();

        int memRows = dbInstances.values().stream().mapToInt((db) -> db.getDbStats().totalRows).sum();
        long memory = dbInstances.values().stream().mapToLong((db) -> db.getDbStats().memory).sum();
        dbStats.maxMemRows = MAX_MEM_ROWS;
        dbStats.maxMemory = MAX_MEMORY;
        dbStats.memDbs = dbInstances.size();
        dbStats.memRows = memRows;
        dbStats.peakMemRows = Math.max(memRows, dbStats.peakMemRows);
        dbStats.memory = Math.max(-1,memory);
        dbStats.peakMemory = Math.max(memory, dbStats.peakMemory);
        return dbStats;
    }

    public static void updateDbStats() {
        LOGGER.trace("DbAdapter -> updateDbStats");
        for (DbAdapter.EmbeddedDbInstance db : dbInstances.values()) {
            db.updateStats();
        }
    }

//====================================================================
//  cleanup related functions
//====================================================================
    public void cleanup() {
        cleanup(false);
    }

    public static void cleanup(boolean force) {
        cleanup(force, false);
    }

    public static void cleanup(boolean force, boolean deleteFile) {

        try {
            LOGGER.trace("DbAdapter -> cleanup");

            // remove expired search results
            List<DbAdapter.EmbeddedDbInstance> toBeRemove = dbInstances.values().stream()
                    .filter((db) -> db.hasExpired() || force).toList();
            if (!toBeRemove.isEmpty()) {
                LOGGER.info("There are currently %d databases open.  Of which, %d will be closed.".formatted(dbInstances.size(), toBeRemove.size()));
                toBeRemove.forEach((db) -> DbAdapter.getAdapter(db.getDbFile()).close(deleteFile));
            }

            // compact idled databases
            dbInstances.values().stream()
                    .filter((db) -> db.mayCompact())
                    .forEach((db) -> db.dbAdapter.compact());

            var sysStats = getRuntimeStats(true);
            boolean useMemory = sysStats.memory > 0;

            // remove search results based on LRU(least recently used) when count is greater than the high-water mark
            if (useMemory && sysStats.memory > MAX_MEMORY) {
                doMemoryCleanup(deleteFile);
            } else if (sysStats.memRows > MAX_MEM_ROWS) {
                doRowCleanup(deleteFile);
            }
            dbStats.lastCleanup = System.currentTimeMillis();
        } catch (Exception e) {
            LOGGER.error(e);
        }

    }

    private static void doRowCleanup(boolean deleteFile) {
        long cRows = 0;
        List<DbAdapter.EmbeddedDbInstance> active = new ArrayList<>(dbInstances.values());
        Collections.sort(active, (db1, db2) -> Long.compare(db2.getLastAccessed(), db1.getLastAccessed()));  // sorted descending..
        for (DbAdapter.EmbeddedDbInstance db : active) {
            cRows += db.getDbStats().totalRows;
            if (cRows > MAX_MEM_ROWS) {
                db.dbAdapter.close(deleteFile);
            }
        }
    }

    private static void doMemoryCleanup(boolean deleteFile) {
        long memory = 0;
        List<DbAdapter.EmbeddedDbInstance> active = new ArrayList<>(dbInstances.values());
        Collections.sort(active, (db1, db2) -> Long.compare(db2.getLastAccessed(), db1.getLastAccessed()));  // sorted descending..
        for (DbAdapter.EmbeddedDbInstance db : active) {
            memory += db.getDbStats().memory;
            if (memory > MAX_MEMORY) {
                db.dbAdapter.close(deleteFile);
            }
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
