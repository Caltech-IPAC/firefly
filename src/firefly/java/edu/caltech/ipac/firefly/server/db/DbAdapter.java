package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_TYPE;

/**
 * Date: 9/8/17
 *
 * @author loi
 * @version $Id: $
 */
public interface DbAdapter {
    String H2 = "h2";
    String SQLITE = "sqlite";
    String HSQL = "hsql";

    String MAIN_DB_TBL = "DATA";
    String NULL_TOKEN = "%NULL";

    /*
      CLEAN UP POLICY:
        A rough estimate: A search results of one million rows displayed in the triview(chart + image + table) takes around 500 MB
        There are two stages of clean-up; compact(remove all temp tables), then shutdown(remove from memory)

        - Compact DB once it has idled longer than CLEANUP_INTVL
        - Shutdown DB once it has expired; idle longer than MAX_IDLE_TIME
        - Shutdown DB based on LRU(Least Recently Used) once the total rows have exceeded MAX_MEMORY_ROWS

        Current settings:
          - CLEANUP_INTVL:  1 minutes
          - MAX_IDLE_TIME: 15 minutes
          - MAX_MEMORY_ROWS:  250k rows for every 1GB of max heap, between the range of 250k to 10 millions.
     */
    long MAX_IDLE_TIME  = 1000 * 60 * 15;   // will shutdown database if idle more than 15 minutes.
    int  CLEANUP_INTVL  = 1000 * 60;        // check every 1 minutes
    static long maxMemRows() {
        long availMem = Runtime.getRuntime().maxMemory() - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        return Math.min(10000000, Math.max(1000000, availMem/1024/1024/1024 * 250000));
    }

    EmbeddedDbStats getRuntimeStats();

    /**
     * @return the name of this database
     */
    String getName();

    /**
     * @param dbFile
     * @return a new DbInstance for the given dbFile
     */
    DbInstance getDbInstance(File dbFile);

    /**
     *  closes the database and release resources used by it.
     * @param dbFile    the database file to disconnect from
     * @param deleteFile    if true, also remove the file itself.
     */
    void close(File dbFile, boolean deleteFile);

    /**
     * @param type
     * @return this database's datatype representation of the given java class.
     */
    String toDbDataType(DataType type);

    /**
     * @return true if transaction should be used during batch import of the table data.
     */
    boolean useTxnDuringLoad();

    String createDataSql(DataType[] dataDefinitions, String tblName);
    String insertDataSql(DataType[] dataDefinitions, String tblName);

    /**
     * contains auxiliary info in datagroup that's not in meta and column info.
     */
    String createAuxDataSql(String forTable);
    String insertAuxDataSql(String forTable);

    String createMetaSql(String forTable);
    String insertMetaSql(String forTable);

    String createDDSql(String forTable);
    String insertDDSql(String forTable);

    String getDDSql(String forTable);
    String getMetaSql(String forTable);
    String getAuxDataSql(String forTable);

    String selectPart(TableServerRequest treq);
    String wherePart(TableServerRequest treq);
    String orderByPart(TableServerRequest treq) ;
    String pagingPart(TableServerRequest treq) ;

    String createTableFromSelect(String tblName, String selectSql);
    String translateSql(String sql);

    List<String> getColumnNames(DbInstance dbInstance, String tblName, String enclosedBy);

    /**
     * perform a cleanup routine which may close inactive database to free up memory
     * @param force  true to force close all open databases
     */
    void cleanup(boolean force);
    public Map<String, BaseDbAdapter.EmbeddedDbInstance> getDbInstances();

//====================================================================
//
//====================================================================

    String DEF_DB_TYPE = AppProperties.getProperty("DbAdapter.type", HSQL);

    static DbAdapter getAdapter() {
        return getAdapter(TBL_FILE_TYPE);
    }

    static DbAdapter getAdapter(TableServerRequest treq) {
        return getAdapter(treq.getMeta(TBL_FILE_TYPE));
    }

    static DbAdapter getAdapter(String type) {
        type = StringUtils.isEmpty(type) ? DEF_DB_TYPE : type;
        switch (type) {
            case H2:
                return new H2DbAdapter();
            case SQLITE:
                return new SqliteDbAdapter();
            case HSQL:
                return new HsqlDbAdapter();
            default:
                return new HsqlDbAdapter();   // when an unrecognized type is given.
        }
    }

    class EmbeddedDbInstance extends DbInstance {
        ReentrantLock lock = new ReentrantLock();
        long lastAccessed;
        long created;
        File dbFile;
        boolean isCompact;
        int tblCount;
        int rowCount = -1;
        int colCount = -1;

        EmbeddedDbInstance(String type, File dbFile, String dbUrl, String driver) {
            this(type, dbFile, dbUrl, driver, System.currentTimeMillis());
        }

        EmbeddedDbInstance(String type, File dbFile, String dbUrl, String driver, long created) {
            super(false, null, dbUrl, null, null, driver, type);
            lastAccessed = System.currentTimeMillis();
            this.dbFile = dbFile;
            this.created = created;
        }

        @Override
        public boolean equals(Object obj) {
            return StringUtils.areEqual(this.dbUrl,((EmbeddedDbInstance)obj).dbUrl);
        }

        @Override
        public int hashCode() {
            return this.dbUrl.hashCode();
        }

        public long getLastAccessed() {
            return lastAccessed;
        }

        public long getCreated() { return created; }

        public boolean hasExpired() {
            return System.currentTimeMillis() - lastAccessed > MAX_IDLE_TIME;
        }

        public File getDbFile() {
            return dbFile;
        }

        public void touch() {
            lastAccessed = System.currentTimeMillis();
            isCompact = false;
        }

        public ReentrantLock getLock() {
            return lock;
        }
        public void setCompact(boolean compact) { isCompact = compact;}
        public boolean isCompact() { return isCompact; }
        public int getTblCount() { return tblCount; }
        public int getRowCount() { return rowCount; }
        public int getColCount() { return colCount; }
        public void setTblCount(int tblCount) { this.tblCount = tblCount; }
        public void setRowCount(int rowCount) { this.rowCount = rowCount; }
        public void setColCount(int colCount) { this.colCount = colCount; }
    }

    class EmbeddedDbStats {
        public long maxMemRows = maxMemRows();
        public long memDbs;
        public long totalDbs;
        public long memRows;
        public long peakMemDbs;
        public long peakMemRows;
        public long peakMaxMemRows;
        public long lastCleanup;
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
