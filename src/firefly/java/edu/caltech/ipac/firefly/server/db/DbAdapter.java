package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.SelectionInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.ResourceProcessor;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_TYPE;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * Date: 9/8/17
 *
 * @author loi
 * @version $Id: $
 */
public interface DbAdapter {

    String MAIN_DATA_TBL = "DATA";
    List<String> MAIN_TABLES = Arrays.asList("DATA", "DATA_DD", "DATA_META", "DATA_AUX");
    String NULL_TOKEN = "%NULL";

    String DEF_DB_TYPE = AppProperties.getProperty("DbAdapter.type", DuckDbAdapter.NAME);

    List<String> ignoreCols = Arrays.asList(DataGroup.ROW_IDX, DataGroup.ROW_NUM, "\"" + DataGroup.ROW_IDX + "\"", "\"" + DataGroup.ROW_NUM + "\"");

    /**
     * @return the name of this database
     */
    String getName();

    /**
     * @return the name of the main table data.  it's currently fixed to DATA, with related tables; DATA_DD, DATA_META, and DATA_AUX
     */
    default String getDataTable() { return MAIN_DATA_TBL; }

    /**
     * Ensures that each adapter is associated with a functional database file.
     * @return  The database file being adapted to
     */
    File getDbFile();

    /**
     * @return a new DbInstance for this database
     */
    DbInstance getDbInstance();

    /**
     * @param type DataGroup's equivalent of a column
     * @return the database data type for the given column.  e.g. double, varchar, bigint, etc.
     */
    String toDbDataType(DataType type);

    /**
     * @return true if transaction should be used during batch import of the table data.
     */
    boolean useTxnDuringLoad();

    /**
     * @param forTable  table to query
     * @param inclCols  only for these columns.  null to get all columns
     * @return a DataGroup with all the headers without data.  This includes info from DD, META, and AUX.
     */
    DataGroup getHeaders(String forTable, String ...inclCols) throws DataAccessException;

    List<String> getColumnNames(String tblName, String enclosedBy);
    List<String> getTableNames();
    DbAdapter.DbStats getDbStats();

    default boolean hasTable(String tableName) {
        try {
            if (isEmpty(tableName) || getDbFile() == null || !getDbFile().exists()) return false;
            return getTableNames().stream()
                    .anyMatch(s -> s.equalsIgnoreCase(tableName));
        } catch (Exception e) {
            return false;
        }
    }


//===============================================================================
//  Supported actions; each database may need to perform them slightly different
//===============================================================================

    /**
     * create and initiate the database
     * @throws IOException
     */
    File initDbFile() throws IOException;

    /**
     * Creates DATA, DATA_DD, and DATA_META tables.
     * If the data is not already in the dbFile, a dataGroupSupplier may be used to retrieve it.
     * * @param dataGroupSupplier  a lambda to retrieve a DataGroup as needed.
     */
    FileInfo ingestData(DataGroupSupplier dataGroupSupplier, String forTable) throws DataAccessException;

    /**
     * Creates DATA_XXX, DATA_XXX_DD, and DATA_XXX_META tables from the original DATA.
     * These tables are created to make subsequent actions faster, like paging
     */
    void createTempResults(TableServerRequest treg, String resultSetID);


    /**
     * Similar to execQuery, except this method creates the SQL statement from the given request object.
     * It needs to take filter, sort, and paging into consideration.
     * @param treq      request parameters used for select, where, order by, and limit
     * @param forTable  table to run the query on.
     * @return
     */
    DataGroupPart execRequestQuery(TableServerRequest treq, String forTable) throws DataAccessException;

    /**
     * Executes the give sql and returns the results as a DataGroup.  If refTable is provided, it will query the
     * ?_DD and ?_META tables of this refTable and add the information into the returned DataGroup.
     * @param sql           complete SQL statement
     * @param refTable      use meta information from this table
     * @return
     */
    DataGroup execQuery(String sql, String refTable) throws DataAccessException;


    /**
     * A wrapper around jdbc update call for logging and error handling.
     * It may also apply any discrepancies among the different DbAdapters.
     * @param sql       the sql to execute update on
     * @param params    parameters used by sql
     * @return the number of rows affected by this update
     * @throws RuntimeException exception thrown by jdbc call
     */
    int execUpdate(String sql, Object ...params) throws RuntimeException;

    /**
     * A wrapper around jdbc batchUpdate call for logging and error handling.
     * It may also apply any discrepancies among the different DbAdapters.
     * @param sql       the sql to execute update on
     * @param params    a list of values to supply to batchUpdate
     * @throws RuntimeException exception thrown by jdbc call
     */
    void batchUpdate(String sql, List<Object[]> params) throws RuntimeException;


    /**
     * Add a column to the DATA table
     * @param col           column to add
     * @param atIndex       add this column at the given index.  If out-of-range, it will be added to the end.
     * @param expression    used to populate the column's value.  required if preset is not selected.
     * @param preset        if preset is used, resultSetID and si may be needed as well.
     * @param resultSetID   the temp table name to use
     * @param si            current selection info of this temp table
     */
    void addColumn(DataType col, int atIndex, String expression, String preset, String resultSetID, SelectionInfo si) throws DataAccessException;

    /**
     * update a column
     * @param editColName   name of column to update
     * @param newCol        new column info to update to
     * @param expression    used to populate the column's value.  required if preset is not selected.
     * @param preset        if preset is used, resultSetID and si may be needed as well.
     * @param resultSetID   the temp table name to use
     * @param si            current selection info of this temp table
     */
    void updateColumn(DataType newCol, String expression, String editColName, String preset, String resultSetID, SelectionInfo si) throws DataAccessException;

    /**
     * Delete a column from the DATA table
     * @param cname         column to delete
     */
    void deleteColumn(String cname);

    DataAccessException handleSqlExp(String msg, Exception e);

//====================================================================
//  management functions
//====================================================================

    /**
     * clear all temp data created and keep only the original data.  e.g. temp tables, indexes, etc.
     */
    void clearCachedData();

    /**
     * remove what's necessary to free up memory
     */
    void compact();

    /**
     *  closes the database and release resources used by it.
     * @param deleteFile    if true, also remove the file itself.
     */
    void close(boolean deleteFile);

//=====================================================================
//  static functions for resolving or finding suitable database adapter
//=====================================================================

    static DbAdapter getAdapter(TableServerRequest treq, DbFileCreator dbFileCreator) {
        return getAdapter(treq.getMeta(TableServerRequest.TBL_FILE_TYPE), dbFileCreator);
    }

    static DbAdapter getAdapter(String name, DbFileCreator dbFileCreator) {
        if (isEmpty(name))  name = DEF_DB_TYPE;
        return switch (name) {
            case HsqlDbAdapter.NAME -> new HsqlDbAdapter(dbFileCreator);
            case H2DbAdapter.NAME -> new H2DbAdapter(dbFileCreator);
            case SqliteDbAdapter.NAME -> new SqliteDbAdapter(dbFileCreator);
            case DuckDbReadable.Parquet.NAME -> new DuckDbReadable.Parquet(dbFileCreator);
            case DuckDbReadable.Csv.NAME -> new DuckDbReadable.Csv(dbFileCreator);
            case DuckDbReadable.Tsv.NAME -> new DuckDbReadable.Tsv(dbFileCreator);
            default -> new DuckDbAdapter(dbFileCreator);
        };
    }

    /**
     * @param dbFile
     * @return a DbAdapter that supports the given dbFile; otherwise, return null.
     */
    static DbAdapter getAdapter(File dbFile) {
        String ext = FilenameUtils.getExtension(dbFile.getName());
        return getAdapter(ext, (s) -> dbFile);
    }

    String interpretError(Throwable e);

//====================================================================
//  Inner classes or interfaces used by this class
//====================================================================

    @FunctionalInterface
    public interface DbFileCreator {
        File create(String dbFileExtension);
    }

    /**
     * Interface to delegate the action of data fetching to DbAdapter.
     * This allow DuckDB to interact with the input file directly without
     * the need to convert to DataGroup
     */
    interface DataGroupSupplier {
        DataGroup get() throws DataAccessException;
    }

    /**
     * Info for each database
     * rowCnt and colCnt are based on the original DATA table
     * tblCnt is total number of tables, including DD, META, and AUX
     * totalRows is the total number of row for all tables
     */
    class DbStats {
        int tblCnt = -1;
        int colCnt = -1;
        int rowCnt = -1;
        int totalRows = -1;
        long memory = -1;

        public int tblCnt() { return tblCnt; }
        public int colCnt() { return colCnt;}
        public int rowCnt() { return rowCnt; }
        public int totalRows() { return totalRows;}
        public long memory() { return memory;}
    }

    /**
     * Extended version of DbInstance to contains additional information
     * so Firefly can enforce policy to manage memory usage.
     */
    class EmbeddedDbInstance extends DbInstance {
        ReentrantLock lock = new ReentrantLock();
        long lastAccessed;
        long created;
        DbAdapter dbAdapter;
        boolean isCompact;
        DbStats dbStats;
        boolean isResourceDb;

        EmbeddedDbInstance(String type, DbAdapter dbAdapter, String dbUrl, String driver) {
            this(type, dbAdapter, dbUrl, driver, System.currentTimeMillis());
        }

        EmbeddedDbInstance(String type, DbAdapter dbAdapter, String dbUrl, String driver, long created) {
            super(false, null, dbUrl, null, null, driver, type);
            lastAccessed = System.currentTimeMillis();
            this.dbAdapter = dbAdapter;
            this.created = created;
            isResourceDb = dbAdapter.getDbFile() != null && dbAdapter.getDbFile().getParentFile().getName().equals(ResourceProcessor.SUBDIR_PATH);
        }

        public boolean equals(Object obj) {
            return StringUtils.areEqual(this.dbUrl,((EmbeddedDbInstance)obj).dbUrl);
        }

        public int hashCode() {
            return this.dbUrl.hashCode();
        }

        public long getLastAccessed() {
            return lastAccessed;
        }

        public long getCreated() { return created; }

        public boolean hasExpired() {
            return System.currentTimeMillis() - lastAccessed > maxIdle();
        }

        public boolean mayCompact() {
            return !isCompact && getDbStats().totalRows > 0 && System.currentTimeMillis() - lastAccessed > maxIdle() * DbMonitor.COMPACT_FACTOR;
        }

        public File getDbFile() {
            return dbAdapter.getDbFile();
        }

        public void touch() {
            lastAccessed = System.currentTimeMillis();
            isCompact = false;
        }

        private long maxIdle() {
            return isResourceDb ? DbMonitor.MAX_IDLE_TIME_RSC : DbMonitor.MAX_IDLE_TIME;
        }

        public ReentrantLock getLock() {
            return lock;
        }
        public void setCompact(boolean compact) { isCompact = compact;}
        public boolean isCompact() { return isCompact; }

        public void updateStats() { this.dbStats = dbAdapter.getDbStats(); }
        public DbStats getDbStats() { return dbStats == null ? new DbStats() : dbStats; }
    }

    /**
     * Info on Firefly embedded database usage.
     */
    class EmbeddedDbStats {
        public long maxMemRows = DbMonitor.MAX_MEM_ROWS;
        public long maxMemory = DbMonitor.MAX_MEMORY;
        public float compactFactor = DbMonitor.COMPACT_FACTOR;
        public long memDbs;
        public long totalDbs;
        public long memRows;
        public long peakMemDbs;
        public long peakMemRows;
        public long memory;
        public long peakMemory;
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
