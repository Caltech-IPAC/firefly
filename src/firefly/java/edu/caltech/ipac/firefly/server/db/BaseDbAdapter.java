/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.SelectionInfo;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.GroupInfo;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.LinkInfo;
import edu.caltech.ipac.table.ParamInfo;
import edu.caltech.ipac.table.ResourceInfo;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.data.TableServerRequest.INCL_COLUMNS;
import static edu.caltech.ipac.firefly.data.TableServerRequest.parseSqlFilter;
import static edu.caltech.ipac.firefly.server.db.DbMonitor.getDbInstances;
import static edu.caltech.ipac.firefly.server.db.DbMonitor.getRuntimeStats;
import static edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil.*;
import static edu.caltech.ipac.table.DataGroup.ROW_IDX;
import static edu.caltech.ipac.table.DataGroup.ROW_NUM;
import static edu.caltech.ipac.util.StringUtils.*;
import static edu.caltech.ipac.util.StringUtils.groupMatch;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
abstract public class BaseDbAdapter implements DbAdapter {
    static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    private File dbFile;


//====================================================================
// Implementations
//====================================================================

    public BaseDbAdapter(File dbFile) { this.dbFile = dbFile; }
    public File getDbFile() { return dbFile; }

    public DbInstance getDbInstance() {
        return getDbInstance(true);
    }

    public DbInstance getDbInstance(boolean create) {
        if (getDbFile() == null) return createDbInstance();

        EmbeddedDbInstance ins = getDbInstances().get(getDbFile().getPath());
        if (ins == null && create) {
            ins = createDbInstance();
            getDbInstances().put(getDbFile().getPath(), ins);
            getRuntimeStats().totalDbs++;
            getRuntimeStats().peakMemDbs = Math.max(getDbInstances().size(), getRuntimeStats().peakMemDbs);
        }
        if (ins != null && create) {        // only update access time when create is requested.
            try {
                ins.getLock().lock();
                ins.touch();
            } finally {
                ins.getLock().unlock();
            }
        }
        return ins;

    }

    /**
     * @param forTable  table to query
     * @param inclCols  only for these columns.  null to get all columns
     * @return a DataGroup with all the headers without data.  This includes info from DD, META, and AUX.
     */
    public DataGroup getHeaders(String forTable, String ...inclCols) throws DataAccessException {
        String cols = inclCols.length == 0 ? "*" : StringUtils.toString(inclCols, ", ");
        DataGroup table = execQuery("Select %s from %s limit 1".formatted(cols, forTable), forTable);
        table.clearData();     // remove the one row fetch earlier because some database returns the whole table when limit is 0;
        return table;
    }

    /**
     * _DD table must exists
     * @param forTable a table name
     * @param enclosedBy enclose each column with this string
     * @return the correctly ordered column names of the given table
     */
    public List<String> getColumnNames(String forTable, String enclosedBy) {
        String sql = "SELECT * from %s_DD order by order_index".formatted(forTable);
        return getJdbc().query(sql, (rs, i) -> (enclosedBy == null) ? rs.getString(1) : enclosedBy + rs.getString(1) + enclosedBy);
    }

    /**
     * return column names using system DB info.  this can be called without _DD dependency
     * @param forTable a table name
     * @param enclosedBy enclose each column with this string
     * @return  all the columns names of a forTable
     */
    List<String> getColumnNamesFromSys(String forTable, String enclosedBy) {
        String sql = "SELECT column_name FROM INFORMATION_SCHEMA.SYSTEM_COLUMNS where table_name = '%s'".formatted(forTable.toUpperCase());
        return getJdbc().query(sql, (rs, i) -> (enclosedBy == null) ? rs.getString(1) : enclosedBy + rs.getString(1) + enclosedBy);
    }

    public List<String> getTableNames() {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES where table_schema = 'PUBLIC'";
        return getJdbc().query(sql, (rs, i) -> rs.getString(1));
    }

    public boolean useTxnDuringLoad() {
        return false;
    }

    public File initDbFile() throws IOException {
        close(true);              // in case database exists in memory, close it and remove all files related to it.
        if (!getDbFile().getParentFile().exists()) getDbFile().getParentFile().mkdirs();
        getDbFile().createNewFile();                     // creates the file
        createUDFs();   // add user defined functions
        return getDbFile();
    }

    public FileInfo ingestData(DataGroupSupplier dataGroupSupplier, String forTable) throws DataAccessException {
        StopWatch.getInstance().start("%s:ingestData for %s".formatted(getName(), forTable));

        StopWatch.getInstance().start("  ingestData: getDataGroup");
        var dg = dataGroupSupplier.get();
        StopWatch.getInstance().printLog("  ingestData: getDataGroup");
        if (dg != null) {
            // remove ROW_IDX or ROW_NUM if exists
            // these are transient values and should not be persisted.
            dg.removeDataDefinition(DataGroup.ROW_IDX);
            dg.removeDataDefinition(DataGroup.ROW_NUM);

            IpacTableUtil.consumeColumnInfo(dg);

            StopWatch.getInstance().start("  ingestData: load data for " + forTable);
            createDataTbl(dg, forTable);

            ddToDb(dg, forTable);
            metaToDb(dg, forTable);
            auxDataToDb(dg, forTable);
        }
        FileInfo finfo = new FileInfo(getDbFile());
        StopWatch.getInstance().printLog("  ingestData: load data for " + forTable);

        StopWatch.getInstance().printLog("%s:ingestData for %s".formatted(getName(), forTable));
        return finfo;
    }
    

    public void createTempResults(TableServerRequest treq, String resultSetID) {
        StopWatch.getInstance().start("%s:createTempResults for %s".formatted(getName(), resultSetID));
        try {
            List<String> cols = isEmpty(treq.getInclColumns()) ? getColumnNames(getDataTable(), "\"")
                    : StringUtils.asList(treq.getInclColumns(), ",");
            cols = cols.stream().filter((s) -> !ignoreCols.contains(s)).collect(Collectors.toList());   // remove rowIdx and rowNum because it will be automatically added

            String selectPart = (cols.size() == 0 ? "*" : StringUtils.toString(cols) + ", " )+ DataGroup.ROW_IDX;
            String wherePart = wherePart(treq);
            String orderBy = orderByPart(treq);

            // copy data
            String datasetSql = "select %s FROM %s %s %s".formatted(selectPart, getDataTable(), wherePart, orderBy);
            String datasetSqlWithIdx = "select b.*, (%s -1) as %s from (%s) as b".formatted(rowNumSql(), DataGroup.ROW_NUM, datasetSql);
            String sql = createTableFromSelect(resultSetID, datasetSqlWithIdx);
            execUpdate(sql);

            // copy dd
            List<String> cnames = getColumnNamesFromSys(resultSetID, "'");
            String ddSql = "select * from DATA_DD" + (cnames.size() > 0 ? " where cname in (%s)".formatted(StringUtils.toString(cnames)) : "");
            ddSql = createTableFromSelect(resultSetID + "_DD", ddSql);
            execUpdate(ddSql);

            // copy meta
            String metaSql = "select * from DATA_META";
            metaSql = createTableFromSelect(resultSetID + "_META", metaSql);
            try {
                getJdbc().update(metaSql);
            } catch (Exception mx) {/*ignore table may not exist*/}

            // copy aux
            String auxSql = "select * from DATA_AUX";
            auxSql = createTableFromSelect(resultSetID + "_AUX", auxSql);
            try {
                getJdbc().update(auxSql);
            } catch (Exception ax) {/*ignore table may not exist*/}

        }catch (RuntimeException e) {
            LOGGER.error("createTempResults failed with error: " + e.getMessage(),
                    "resultSetID: " + resultSetID,
                    "dbFile: " + getDbFile().getAbsolutePath());
            throw e;
        } finally {
            StopWatch.getInstance().printLog("%s:createTempResults for ".formatted(getName(), resultSetID));
        }
    }
    
    protected String buildSqlFrom(TableServerRequest treq, String forTable) {
        String selectPart = selectPart(treq);
        String wherePart = wherePart(treq);
        String orderByPart = orderByPart(treq);
        String pagingPart = pagingPart(treq);

        if (forTable.equals(getDataTable())) {
            // fix select * so that it selects the columns in its supposed order
            if (selectPart.toLowerCase().replaceAll("\\s", "").equals("select*")) {
                selectPart = "select " + StringUtils.toString(getColumnNames(forTable, "\""));
            }
        }
        return  "%s FROM %s %s %s %s".formatted(selectPart, forTable, wherePart, orderByPart, pagingPart);
    }
    
    /**
     * Similar to execQuery, except this method creates the SQL statement from the given request object.
     * It needs to take filter, sort, and paging into consideration.
     * @param treq      request parameters used for select, where, order by, and limit
     * @param forTable  table to run the query on.
     * @return
     */
    public DataGroupPart execRequestQuery(TableServerRequest treq, String forTable) throws DataAccessException {
        String sql = buildSqlFrom(treq, forTable);
        DataGroup data = execQuery(sql, forTable);

        int rowCnt = data.size();
        if (!isEmpty(pagingPart(treq))) {
            // fetch total row count for the query; datagroup may contain partial results(paging)
            String cntSql = "select count(*) FROM %s %s".formatted(forTable, wherePart(treq));
            rowCnt = getJdbc().queryForInt(cntSql);
        }

        DataGroupPart page = EmbeddedDbUtil.toDataGroupPart(data, treq);
        page.setRowCount(rowCnt);
        if (!isEmpty(treq.getTblTitle())) {
            page.getData().setTitle(treq.getTblTitle());  // set the datagroup's title to the request title.
        }

        return page;
    }

    /**
     * Executes the give sql and returns the results as a DataGroup.  If refTable is provided, it will query the
     * ?_DD and ?_META tables of this refTable and add the information into the returned DataGroup.
     * @param sql           complete SQL statement
     * @param refTable      use meta information from this table
     * @return
     */
    public DataGroup execQuery(String sql, String refTable) throws DataAccessException {

        StopWatch.getInstance().start("%s:execQuery: %s".formatted(getName(), refTable));

        sql = translateSql(sql);
        LOGGER.trace("execQuery => SQL: " + sql);
        try {
            DataGroup dg = (DataGroup)getJdbcTmpl().query(sql, rs -> {
                DataGroup tmpDg = new DataGroup(null, getCols(rs, getDbInstance()));
                applyDdToDataType(tmpDg, refTable);
                applyMetaToTable(tmpDg, refTable);
                applyAuxToTable(tmpDg, refTable);
                return EmbeddedDbUtil.dbToDataGroup(rs, tmpDg);
            });

            // if rowidx or rownum is returned, make it not visible
            applyIfNotEmpty(dg.getDataDefintion(ROW_IDX), dt -> dt.setVisibility(DataType.Visibility.hidden));
            applyIfNotEmpty(dg.getDataDefintion(ROW_NUM), dt -> dt.setVisibility(DataType.Visibility.hidden));

            StopWatch.getInstance().printLog("%s:execQuery: %s".formatted(getName(), refTable));

            return dg;
        } catch (Exception e) {
            // catch for debugging
            LOGGER.warn("execQuery failed with error: " + e.getMessage(),
                    "sql: " + sql,
                    "refTable: " + refTable,
                    "dbFile: " + getDbFile().getAbsolutePath());
            throw handleSqlExp("Query failed", e);
        }
    }

    /**
     * A wrapper around jdbc update call for logging and error handling.
     * It may also apply and discrepancies among the different DbAdapters.
     * @param sql       the sql to execute update on
     * @param params    parameters used by sql
     * @return the number of rows affected by this update
     * @throws RuntimeException exception thrown by jdbc call
     */
    public int execUpdate(String sql, Object ...params) throws RuntimeException {
        DbInstance dbInstance = getDbInstance();
        sql = translateSql(sql);
        LOGGER.trace("execUpdate => SQL: " + sql);
        try {
            return getJdbc().update(sql, params);
        } catch (Exception e) {
            // catch for debugging
            LOGGER.warn("execUpdate failed with error: " + e.getMessage(),
                    "sql: " + sql,
                    "dbFile: " + getDbFile().getAbsolutePath());
            throw e;
        }
    }

    public void batchUpdate(String sql, List<Object[]> params) throws RuntimeException {
        DbInstance dbInstance = getDbInstance();
        sql = translateSql(sql);
        LOGGER.trace("batchUpdate => SQL: " + sql);
        try {
            getJdbc().batchUpdate(sql, params);
        } catch (Exception e) {
            // catch for debugging
            LOGGER.warn("batchUpdate failed with error: " + e.getMessage(),
                    "sql: " + sql,
                    "dbFile: " + getDbFile().getAbsolutePath());
            throw e;
        }
    }


//====================================================================
//  Table functions
//====================================================================

    public void addColumn(DataType col, int atIndex, String expression, String preset, String resultSetID, SelectionInfo si)  throws DataAccessException {
        JdbcTemplate jdbc = getJdbcTmpl();

        try {
            // add column to main table
            var sql = "ALTER TABLE %s ADD COLUMN \"%s\" %s".formatted(getDataTable(), col.getKeyName(), toDbDataType(col));
            execUpdate(sql);
        } catch (Exception e) {
            // DDL statement are transactionally isolated, therefore need to manually rollback if this succeeds but the next few statements failed.
            throw handleSqlExp("Add column failed", e);
        }
        try {
            TransactionTemplate txnJdbc = JdbcFactory.getTransactionTemplate(jdbc.getDataSource());
            txnJdbc.execute((st) -> {
                // add a record to DD table
                String desc = EmbeddedDbUtil.getFieldDesc(col, expression, preset);
                col.setDesc(desc);
                addColumnToDD(jdbc, col, atIndex);

                populateColumnValues(jdbc, getDataTable(), col, expression, preset, resultSetID, si);

                // purge all cached tables
                clearCachedData();
                return st;
            });
            EmbeddedDbUtil.enumeratedValuesCheck(this, new DataType[]{col});        // if successful, check for enum values of this new column
        } catch (Exception e) {
            // manually remove the added column
            var sql = "ALTER TABLE %s DROP COLUMN \"%s\"".formatted(getDataTable(), col.getKeyName());
            execUpdate(sql);
            throw handleSqlExp("Add column failed", e);
        }

    }

    public void updateColumn(DataType newCol, String expression, String editColName, String preset, String resultSetID, SelectionInfo si) throws DataAccessException {

        if (isEmpty(editColName)) return;

        JdbcTemplate jdbc = getJdbcTmpl();

        String swapCname = null;     // set if swap is in play; use for rollback.

        try {
            // rename column if different
            if (!newCol.getKeyName().equals(editColName)) {
                renameColumn(editColName, newCol.getKeyName());
            }

            // change column type if different
            String oldType = String.valueOf(jdbc.queryForObject("SELECT type from DATA_DD where cname='%s'".formatted(newCol.getKeyName()), String.class));
            if (!oldType.equals(newCol.getTypeDesc())) {
                // race condition; cannot change double values into boolean even when they are NULLs
                // instead, we will add tmp column, populate values, drop original column, then rename tmp column back to original cname
                swapCname = newCol.getKeyName();
                newCol.setKeyName(newCol.getKeyName() + "_tmp");
                var sql = "ALTER TABLE %s ADD COLUMN \"%s\" %s".formatted(getDataTable(), newCol.getKeyName(), toDbDataType(newCol));
                execUpdate("UPDATE %s_DD SET cname='%s' WHERE cname='%s'".formatted(getDataTable(), newCol.getKeyName(), swapCname));
                execUpdate(sql);
            }
        } catch (Exception e) {
            // DDL statement are transactionally isolated, therefore need to manually rollback if this succeeds but the next few statements failed.
            throw handleSqlExp("Update column failed", e);
        }
        try {
            TransactionTemplate txnJdbc = JdbcFactory.getTransactionTemplate(jdbc.getDataSource());
            txnJdbc.execute((st) -> {
                // update DD table
                String sql = "UPDATE DATA_DD SET type=?, precision=?, units=?, UCD=?, description=? WHERE cname=?";
                String desc = EmbeddedDbUtil.getFieldDesc(newCol, expression, preset);
                execUpdate(sql, newCol.getTypeDesc(), newCol.getPrecision(), newCol.getUnits(), newCol.getUCD(), desc, newCol.getKeyName());

                populateColumnValues(jdbc, getDataTable(), newCol, expression, preset, resultSetID, si);

                // purge all cached tables
                clearCachedData();
                return st;
            });
            EmbeddedDbUtil.enumeratedValuesCheck(this, new DataType[]{newCol});        // if successful, check for enum values of this new updated column

            // if swapName is used; rename column back to swapName
            if (swapCname != null) {
                execUpdate("ALTER TABLE %s DROP COLUMN \"%s\"".formatted(getDataTable(), swapCname));
                renameColumn(newCol.getKeyName(), swapCname);
            }

        } catch (Exception e) {
            if (swapCname != null) {
                execUpdate("ALTER TABLE %s DROP COLUMN \"%s\"".formatted(getDataTable(), newCol.getKeyName()));
            }
            // manually revert the name change
            if (!newCol.getKeyName().equals(editColName)) {
                renameColumn(newCol.getKeyName(), editColName);
            }
            throw handleSqlExp("Update column failed", e);
        }
    }

    /**
     * Delete a column from DATA table
     * @param cname     column to delete
     */
    public void deleteColumn(String cname) {

        // drop column from DATA table
        execUpdate("ALTER TABLE %s DROP COLUMN \"%s\"".formatted(getDataTable(), cname));

        int atIndex = getColumnNames(getDataTable(), null).indexOf(cname);
        if (atIndex >= 0) {
        } else {
            shiftColsAt(null, atIndex, -1);       // added to the middle, need to shift the other cols;
        }

        // remove column from DD table
        String sql = "DELETE FROM DATA_DD WHERE cname=?";
        execUpdate(sql, cname);

        // purge all cached tables
        clearCachedData();
    }

    /**
     * Add a record into the DATA_DD table
     * @param jdbc  a JdbcTemplate.  If not given, a new one will be created.
     *              This useful for transaction related
     * @param col       the column to add
     * @param atIndex   the order index to add this column at.
     */
    protected void addColumnToDD(JdbcTemplate jdbc, DataType col, int atIndex) {
        jdbc = jdbc == null ? getJdbcTmpl() : jdbc;
        int colCnt = getColumnNames(getDataTable(), null).stream()
                        .filter(cname -> !CollectionUtil.exists(cname, ROW_IDX, ROW_NUM))
                        .toList().size();
        if (atIndex<0 || atIndex>colCnt) {
            atIndex = colCnt;
        } else {
            shiftColsAt(jdbc, atIndex, 1);       // added to the middle, need to shift the other cols;
        }

        String sql = insertDDSql(getDataTable());
        jdbc.update(sql, getDdFrom(col, atIndex));
    }

    /**
     * Shift all the column(s) at the given index by a number of position.
     * @param jdbc      JdbcTemplate to use.  If null, a new one will be created.
     * @param atIndex   the index where shifting is needed
     * @param shiftBy   the number of position to shift.  Normally, it's either +1 or -1.
     */
    private void shiftColsAt(JdbcTemplate jdbc, int atIndex, int shiftBy) {
        jdbc = jdbc == null ? getJdbcTmpl() : jdbc;
        jdbc.update("UPDATE %s_DD SET order_index = order_index + (%d) WHERE order_index >= %d".formatted(getDataTable(), shiftBy, atIndex));
    }

    protected void renameColumn(String from, String to) {
        execUpdate("ALTER TABLE %s ALTER COLUMN \"%s\" RENAME TO \"%s\"".formatted(getDataTable(), from, to));
        execUpdate("UPDATE %s_DD SET cname='%s' WHERE cname='%s'".formatted(getDataTable(), to, from));
    }

    protected boolean useIndexWhenUpdateColumnValue() { return true; }

    /*
     * This runs within a transaction.  That's why JdbcTemplate is passed in.
     */
    protected void populateColumnValues(JdbcTemplate jdbc, String forTable, DataType dtype, String expression, String preset, String resultSetID, SelectionInfo si) {
        // populate column with new values
        if (isEmpty(preset)) {
            jdbc.update("UPDATE %s SET \"%s\" = %s".formatted(forTable, dtype.getKeyName(), expression));
        } else {
            if (useIndexWhenUpdateColumnValue()) {
                jdbc.update("CREATE INDEX  IF NOT EXISTS data_idx ON %s (row_idx)".formatted(forTable));
                if (!resultSetID.equals(forTable)) {
                    jdbc.update("CREATE INDEX  IF NOT EXISTS %s_idx ON %s (row_idx)".formatted(resultSetID, resultSetID));
                }
            }
            if (preset.equals("filtered")) {
                String sql = resultSetID.equals(forTable) ? "TRUE" : String.format("(SELECT 1 from %s as t WHERE t.ROW_IDX = d.ROW_IDX)", resultSetID);
                jdbc.update("UPDATE %s as d SET \"%s\" = %s".formatted(forTable, dtype.getKeyName(), sql));
            } else if (preset.equals("selected")) {
                String sql = "FALSE";
                if (si != null && si.getSelectedCount() > 0) {
                    if (resultSetID.equals(forTable)) {
                        sql =  si.isSelectAll() ? "TRUE" : "(ROW_IDX in (%s))".formatted(StringUtils.toString(si.getSelected()));
                    } else {
                        String rowNums = StringUtils.toString(si.getSelected());
                        List<Integer> rowIdxs = new SimpleJdbcTemplate(jdbc).query(String.format("Select ROW_IDX from %s where ROW_NUM in (%s)", resultSetID, rowNums), (resultSet, i) -> resultSet.getInt(1));
                        sql = "(ROW_IDX in (%s))".formatted(StringUtils.toString(rowIdxs));
                    }
                }
                jdbc.update("UPDATE %s SET \"%s\" = %s".formatted(forTable, dtype.getKeyName(), sql));
            } else if (preset.equals("ROW_NUM")) {
                String sql = resultSetID.equals(forTable) ? "(ROW_NUM + 1)" : "(SELECT t.ROW_NUM+1 from %s as t WHERE t.ROW_IDX = d.ROW_IDX)".formatted(resultSetID);
                jdbc.update("UPDATE %s as d SET \"%s\" = %s".formatted(forTable, dtype.getKeyName(), sql));
            }
        }
    }

    String longStringDbType() {
        return "TEXT";
    }

    public String toDbDataType(DataType dataType) {
        if (dataType.isArrayType()) return longStringDbType();

        Class type = dataType.getDataType();
        if (type == null || String.class.isAssignableFrom(type)) {
            return "varchar(4000000)";                           // to ensure it can accommodate any length
        } else if (Byte.class.isAssignableFrom(type)) {
            return "tinyint";
        } else if (Short.class.isAssignableFrom(type)) {
            return "smallint";
        } else if (Integer.class.isAssignableFrom(type)) {
            return "int";
        } else if (Long.class.isAssignableFrom(type)) {
            return "bigint";
        } else if (Float.class.isAssignableFrom(type)) {
            return "float";
        } else if (Double.class.isAssignableFrom(type)) {
            return "double";
        } else if (Boolean.class.isAssignableFrom(type)) {
            return "boolean";
        } else if (Date.class.isAssignableFrom(type)) {
            return "timestamp";         // Firefly only support util.Date which has both date and time.
        } else if (Character.class.isAssignableFrom(type)) {
            return "char";
        } else {
            return "varchar(64000)";
        }
    }

    public void clearCachedData() {
        LOGGER.debug("DbAdapter -> compacting DB: %s".formatted(getDbFile().getPath()));
        List<String> tables = getTempTables();
        if (tables.size() > 0) {
            // remove all temporary tables
            String[] stmts = tables.stream().map(s -> "drop table IF EXISTS " + s).toArray(String[]::new);
            getJdbcTmpl().batchUpdate(stmts);

        }
    }

    public void compact() {
        clearCachedData();
        ((EmbeddedDbInstance) getDbInstance()).setCompact(true);
    }

    public void close(boolean deleteFile) {
        LOGGER.debug("%s -> closing DB, delete(%s): %s".formatted(getName(), deleteFile, getDbFile().getPath()));
        EmbeddedDbInstance db = getDbInstances().get(getDbFile().getPath());
        if (db != null) {
            try {
                db.getLock().lock();
                if (!deleteFile) {
                    compact();
                }
                shutdown(db);
            } finally {
                db.getLock().unlock();
                getDbInstances().remove(db.getDbFile().getPath());
            }
        }
        if (deleteFile) removeDbFile();
    }

    protected void shutdown(EmbeddedDbInstance db) {}
    protected void removeDbFile() {}
    protected String rowNumSql() { return "ROWNUM"; }

    protected List<String> getTempTables() {
        return getTableNames().stream()
                .filter(n -> !MAIN_TABLES.contains(n)).collect(Collectors.toList());
    }

    public DbAdapter.DbStats getDbStats() {
        DbStats dbStats = new DbStats();
        try {
            var db = getDbInstance(false);
            if (db == null)  return dbStats;

            SimpleJdbcTemplate jdbc = JdbcFactory.getSimpleTemplate(db);
            jdbc.queryForObject("SELECT count(*), sum(cardinality) from INFORMATION_SCHEMA.SYSTEM_TABLESTATS where table_schema = 'PUBLIC' and not REGEXP_MATCHES(table_name,'.*_DD$|.*_META$|.*_AUX$')", (rs, i) -> {
                dbStats.tblCnt = rs.getInt(1);
                dbStats.totalRows = rs.getInt(2);
                return null;
            });
            jdbc.queryForObject("SELECT count(column_name), cardinality from INFORMATION_SCHEMA.SYSTEM_COLUMNS c, INFORMATION_SCHEMA.SYSTEM_TABLESTATS t" +
                    " where c.table_name = t.table_name" +
                    " and t.table_name = 'DATA'" +
                    " group by cardinality", (rs, i) -> {
                dbStats.colCnt = rs.getInt(1);
                dbStats.rowCnt = rs.getInt(2);
                return null;
            });
        } catch (Exception ignored) {}
        return dbStats;
    }


    protected abstract EmbeddedDbInstance createDbInstance();

//====================================================================
// functions exposed internally
//====================================================================

    String translateSql(String sql) {
        return sql;
    }

    void createUDFs() {}

    String createTableFromSelect(String tblName, String selectSql) {
        return "CREATE TABLE IF NOT EXISTS %s AS (%s)".formatted(tblName, selectSql);
    }

    int createDataTbl(DataGroup dg, String tblName) throws DataAccessException {

        DataType[] colsAry = EmbeddedDbUtil.makeDbCols(dg);
        int totalRows = dg.size();

        String createDataSql = createDataSql(colsAry, tblName);
        getJdbc().update(createDataSql);

        if (totalRows > 0) {
            JdbcTemplate jdbc = getJdbcTmpl();

            String insertDataSql = insertDataSql(colsAry, tblName);
            if (useTxnDuringLoad()) {
                TransactionTemplate txnJdbc = JdbcFactory.getTransactionTemplate(jdbc.getDataSource());
                txnJdbc.execute(new TransactionCallbackWithoutResult() {
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        EmbeddedDbUtil.loadDataToDb(jdbc, insertDataSql, dg);
                    }
                });
            } else {
                EmbeddedDbUtil.loadDataToDb(jdbc, insertDataSql, dg);
            }
        }

        return totalRows;
    }

    String createAuxDataSql(String forTable) { return EmbeddedDbUtil.AUX_DATA_CREATE_SQL.formatted(forTable); }
    String insertAuxDataSql(String forTable) { return EmbeddedDbUtil.AUX_DATA_INSERT_SQL.formatted(forTable); }

    String createMetaSql(String forTable) { return EmbeddedDbUtil.META_CREATE_SQL.formatted(forTable); }
    String insertMetaSql(String forTable) { return EmbeddedDbUtil.META_INSERT_SQL.formatted(forTable); }

    String createDDSql(String forTable) { return EmbeddedDbUtil.DD_CREATE_SQL.formatted(forTable); }
    String insertDDSql(String forTable) { return EmbeddedDbUtil.DD_INSERT_SQL.formatted(forTable); }

    String createDataSql(DataType[] dtTypes, String tblName) {
        tblName = isEmpty(tblName) ? getDataTable() : tblName;
        List<String> coldefs = new ArrayList<>();
        for(DataType dt : dtTypes) {
            coldefs.add("\"%s\" %s".formatted(dt.getKeyName(), toDbDataType(dt)));       // add quotes to avoid reserved words clashes
        }

        return "create table %s (%s)".formatted(tblName, StringUtils.toString(coldefs, ","));
    }

    String insertDataSql(DataType[] dtTypes, String tblName) {
        tblName = isEmpty(tblName) ? getDataTable() : tblName;

        String[] var = new String[dtTypes.length];
        Arrays.fill(var , "?");
        return "insert into %s values(%s)".formatted(tblName, StringUtils.toString(var, ","));
    }

    protected Object[] getDdFrom(DataType dt, int colIdx) {
        return new Object[] {
                dt.getKeyName(),
                dt.getLabel(),
                dt.getTypeDesc(),
                dt.getUnits(),
                dt.getNullString(),
                dt.getFormat(),
                dt.getFmtDisp(),
                dt.getWidth(),
                dt.getVisibility().name(),
                dt.isSortable(),
                dt.isFilterable(),
                dt.isFixed(),
                dt.getDesc(),
                dt.getEnumVals(),
                dt.getID(),
                dt.getPrecision(),
                dt.getUCD(),
                dt.getUType(),
                dt.getRef(),
                dt.getMaxValue(),
                dt.getMinValue(),
                serialize(dt.getLinkInfos()),       // index(21) is used in HsqlDbAdapter.  if it changes, update.
                dt.getDataOptions(),
                dt.getArraySize(),
                dt.getCellRenderer(),
                dt.getSortByCols(),
                colIdx
        };
    }

    // insert column info into the table
    void applyDdToDataType(DataGroup dg, String refTable) {
        if (isEmpty(refTable)) return;

        String ddSql = "select * from %s_DD".formatted(refTable);
        try {
            getJdbcTmpl().query(ddSql, rs -> { return dbToDD(dg, rs); });
        } catch (Exception e) {
            LOGGER.trace("missing DD(ignored): " + refTable);
        }
    }

    // insert table meta info into the results
    void applyMetaToTable(DataGroup dg, String refTable) {
        if (isEmpty(refTable)) return;
        String metaSql = "select * from %s_META".formatted(refTable);
        try {
            getJdbcTmpl().query(metaSql, rs -> { return dbToMeta(dg, rs); });
        } catch (Exception e) {
            LOGGER.trace("missing META(ignored): " + refTable);
        }
    }

    // insert aux info into the table
    void applyAuxToTable(DataGroup dg, String refTable) {

        if (isEmpty(refTable)) return;

        String auxDataSqlSql = "select * from %s_AUX".formatted(refTable);
        try {
            if (!isEmpty(auxDataSqlSql)) {
                getJdbcTmpl().query(auxDataSqlSql, rs -> { return dbToAuxData(dg, rs); });
            }
        } catch (Exception e) {
            LOGGER.trace("missing AUX(ignored): " + refTable);
        }
    }

    String selectPart(TableServerRequest treq) {
        String cols = treq.getParam(INCL_COLUMNS);
        cols = "select " + (isEmpty(cols) ? "*" : cols);
        return cols;
    }

    String wherePart(TableServerRequest treq) {
        String where = "";
        if (treq.getFilters() != null && treq.getFilters().size() > 0) {
            for (String cond :treq.getFilters()) {
                cond = Arrays.stream(cond.split("(?i)(?= and | or )"))                        // because each filter may contains multiple conditions... apply cleanup logic to each one.
                        .map(eCond -> {
                            if (eCond.matches("(?i).* LIKE .*(\\\\_|\\\\%|\\\\\\\\).*")) {       // search for LIKE with  \_, \%, or \\ in the condition.
                                // for LIKE, to search for '%', '\' or '_' itself, an escape character must also be specified using the ESCAPE clause
                                eCond += " ESCAPE '\\'";
                            }
                            String[] parts = StringUtils.groupMatch("(.+) IN (.+)", eCond, Pattern.CASE_INSENSITIVE);
                            if (parts != null && eCond.contains(NULL_TOKEN)) {
                                eCond = "%s OR %s IS NULL".formatted(eCond.replace(NULL_TOKEN, NULL_TOKEN.substring(1)), parts[0]);
                            }
                            return eCond;
                        }).collect(Collectors.joining(""));

                if (where.length() > 0) {
                    where += " and ";
                }
                where += "(" + cond + ")";
            }
            where = "where " + where;
        }

        String[] opSql = parseSqlFilter(treq.getSqlFilter());
        if (!isEmpty(opSql[1])) {
            if (where.length() > 0) {
                where += " %s (%s)".formatted(opSql[0], opSql[1]);
            } else {
                where = "where %s".formatted(opSql[1]);
            }
        }

        return where;
    }

    String orderByPart(TableServerRequest treq) {
        if (treq.getSortInfo() != null) {
            String dir = treq.getSortInfo().getDirection() == SortInfo.Direction.DESC ? " desc" : "";
            String nullsOrder = dir.equals("") ? " NULLS FIRST" : " NULLS LAST";     // not every database support this same syntax.  override if needed.
            String orderBy = "ORDER BY " +
                    treq.getSortInfo().getSortColumns().stream()
                    .map(c -> c.contains("\"") ? c : "\"" + c + "\"")
                    .map(c -> c + dir)
                    .map(c -> c + nullsOrder)
                    .collect(Collectors.joining(","));
            orderBy = orderBy.contains(ROW_IDX) ? orderBy : orderBy + ", " + ROW_IDX;       // this ensures the query result's order is deterministic even when there are many duplicates in the sorted columns
            return  orderBy;
        }
        return "";
    }

    String pagingPart(TableServerRequest treq) {
        if (treq.getPageSize() < 0 || treq.getPageSize() == Integer.MAX_VALUE) return "";
        String page = "limit %d offset %d".formatted(treq.getPageSize(), treq.getStartIndex());
        return page;
    }

    SimpleJdbcTemplate getJdbc() {
        return JdbcFactory.getSimpleTemplate(getDbInstance());
    }

    JdbcTemplate getJdbcTmpl() {
        return JdbcFactory.getTemplate(getDbInstance());
    }


//====================================================================
//  O-R mapping functions
//====================================================================

    public void metaToDb(DataGroup dg, String forTable) {
        TableMeta meta = dg.getTableMeta();
        String createMetaSql = createMetaSql(forTable);
        getJdbc().update(createMetaSql);

        // for consistency, we will create the table even if no metadata exists; but no data
        if (meta.isEmpty()) return;

        List<Object[]> data = new ArrayList<>();
        // take all keywords
        meta.getKeywords().forEach(kw -> data.add(new Object[]{kw.getKey(), kw.getValue(), kw.isKeyword()}));
        // then take only meta that's not keywords
        meta.getAttributeList().stream()
                .filter(kw -> !kw.isKeyword())
                .forEach(kw -> data.add(new Object[]{kw.getKey(), kw.getValue(), kw.isKeyword()}));
        String insertMetaSql = insertMetaSql(forTable);
        getJdbc().batchUpdate(insertMetaSql, data);
    }

    private static Object dbToMeta(DataGroup dg, ResultSet rs) {
        try {
            while (rs.next()) {
                String key = rs.getString("key");
                String value = rs.getString("value");
                boolean isKeyword = rs.getBoolean("isKeyword");
                if (isKeyword) {
                    dg.getTableMeta().addKeyword(key, value);
                } else {
                    dg.getTableMeta().setAttribute(key, value);
                }
            };
        } catch (SQLException e) {
            LOGGER.error(e);
        }
        return 0;
    }

    void auxDataToDb(DataGroup dg, String tblName) {

        String createAuxDataSql = createAuxDataSql(tblName);
        getJdbc().update(createAuxDataSql);

        List<Object[]> data = new ArrayList<>();
        data.add( getAuxFrom(dg));
        String insertAuxSql = insertAuxDataSql(tblName);
        getJdbc().batchUpdate(insertAuxSql, data);
    }


    protected Object[] getAuxFrom(DataGroup dg) {
        return new Object[] {
                dg.getTitle(),
                dg.size(),
                serialize(dg.getGroupInfos()),
                serialize(dg.getLinkInfos()),
                serialize(dg.getParamInfos()),
                serialize(dg.getResourceInfos())
        };
    }

    Object dbToAuxData(DataGroup dg, ResultSet rs) {
        try {
            while (rs.next()) {
                dg.setTitle(rs.getString("title"));
                dg.setLinkInfos( (List<LinkInfo>) deserialize  (rs, "links") );
                dg.setGroupInfos( (List<GroupInfo>) deserialize(rs, "groups") );
                dg.setParamInfos( (List<ParamInfo>) deserialize(rs, "params") );
                dg.setResourceInfos( (List<ResourceInfo>) deserialize(rs, "resources") );
            };
        } catch (SQLException e) {
            LOGGER.error(e);
        }
        return 0;
    }

    Object dbToDD(DataGroup dg, ResultSet rs) {
        try {
            while (rs.next()) {
                String cname = rs.getString("cname");
                DataType dtype = dg.getDataDefintion(cname, true);
                // if this column is not in DataGroup.  no need to update the info
                if (dtype != null) {
                    EmbeddedDbUtil.dbToDataType(dtype, rs);
                    handleSpecialDTypes(dtype, dg, rs);
                }
            };
        } catch (SQLException e) {
            LOGGER.error(e);
        }
        return 0;
    }

    void handleSpecialDTypes(DataType dtype, DataGroup dg, ResultSet rs) {
        applyIfNotEmpty(deserialize(rs, "links"), v -> dtype.setLinkInfos((List<LinkInfo>) v));
    }

    void ddToDb(DataGroup dg, String tblName) {

        DataType[] colsAry = EmbeddedDbUtil.makeDbCols(dg);
        String createDDSql = createDDSql(tblName);
        getJdbc().update(createDDSql);

        List<Object[]> data = new ArrayList<>();
        for (int i = 0; i < colsAry.length; i++) {
            Object[] acol = getDdFrom(colsAry[i], i+1);     // order_index starts from 1, not 0;
            data.add(acol);
        }
        String insertDDSql = insertDDSql(tblName);
        getJdbc().batchUpdate(insertDDSql, data);
    }


//====================================================================
//
//====================================================================

    public String interpretError(Throwable e) {
        if( e instanceof UncategorizedSQLException ce) {
            //java.sql.SQLException: Binder Error: Referenced column "sldfjas" not found in FROM clause!
            //Candidate bindings: "DATA.a"
            //LINE 1: UPDATE DATA SET "a" = sldfjas > 0
            //                              ^
            String[] parts = groupMatch("java.sql.SQLException: (.*)",  ce.getSQLException().getMessage().split("\\n")[0]);
            if (parts != null && parts.length == 1) {
                return parts[0];
            }
        }
        return e.getMessage();
    }

    public DataAccessException handleSqlExp(String msg, Exception e) {
        Throwable cause = e;
        while (cause != null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof DataAccessException dax) {
            return new DataAccessException(msg, dax);   // if DataAccessException, then no need to interpret the error message
        }
        return new DataAccessException(msg, new SQLDataException(interpretError(cause)));
    }
}
