/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.servlets.ServerStatus;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.GroupInfo;
import edu.caltech.ipac.table.LinkInfo;
import edu.caltech.ipac.table.ParamInfo;
import edu.caltech.ipac.table.ResourceInfo;
import edu.caltech.ipac.util.AppProperties;
import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBConnection;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil.deserialize;
import static edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil.serialize;
import static edu.caltech.ipac.util.StringUtils.*;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class DuckDbAdapter extends BaseDbAdapter implements DbAdapter.DbAdapterCreator {
    public static final String NAME = "duckdb";
    public static String maxMemory = AppProperties.getProperty("duckdb.max.memory");        // in GB; 2G, 5.5G, etc

    static {
        if (DEF_DB_TYPE.equals(NAME)) {
            if (isEmpty(maxMemory)) {
                long bytes = ServerStatus.getTotalRam();
                maxMemory = String.format("%.1fG", Math.max(bytes/5.0/(1024.0 * 1024 * 1024), 1));       // 20% or RAM or 1G if less.
            }
            String[] v = groupMatch("^([0-9]*\\.?[0-9]*)G$", maxMemory);
            if (v != null && v.length > 0) {
                DbMonitor.MAX_MEMORY = (long) (Float.parseFloat(v[0]) * 1024 * 1024 * 1024);
            }
            DbMonitor.MAX_MEM_ROWS = 1_000_000_000;
        }
    }

    private static final String [] customFunctions = {"""
            CREATE FUNCTION decimate_key(xVal, yVal, xMin, yMin, nX, nY, xUnit, yUnit) AS
            TRUNC((xVal-xMin)/xUnit)::INT || ':' || TRUNC((yVal-yMin)/yUnit)::INT
            """         // make sure this matches DecimateKey.getKey()
            ,
            "CREATE FUNCTION lg(val) AS LOG10(val)"
    };

    private static final List<String> SUPPORTS = List.of("duckdb");

    public DuckDbAdapter(File dbFile) { super(dbFile); }

    public String getName() { return NAME; }

    List<String> getSupportedExts () { return SUPPORTS; }

    protected EmbeddedDbInstance createDbInstance() {
        File db = getDuckDbFile();
        String dbUrl = "jdbc:duckdb:" + db.getAbsolutePath();          // open an in-memory db
        EmbeddedDbInstance dbInst = new EmbeddedDbInstance(getName(), this, dbUrl, "org.duckdb.DuckDBDriver");
        return dbInst;
    }

    void createUDFs() {
        for (String cf : customFunctions) {
            try {
                execUpdate(cf);
            } catch (Exception ex) {
                LOGGER.error("Fail to create custom function:" + cf);
            }
        }
        execUpdate(String.format("SET memory_limit = '%s'", maxMemory));
    }

    protected void renameColumn(String from, String to) {
        execUpdate(String.format("ALTER TABLE %s RENAME COLUMN \"%s\" TO \"%s\"", getDataTable(), from, to));
        execUpdate(String.format("UPDATE %s_DD SET cname='%s' WHERE cname='%s'", getDataTable(), to, from));
    }

    protected boolean useIndexWhenUpdateColumnValue() { return false; }

    public File initDbFile() throws IOException {
        close(true);              // if database exists in memory, close it and remove all files related to it.
        File duckdbFile = getDuckDbFile();
        if (!duckdbFile.getParentFile().exists()) duckdbFile.getParentFile().mkdirs();
        createUDFs();   // add user defined functions
        return getDbFile();
    }

    public void compact() {
        // no need to compact.  it will automatically push out of memory.
        ((EmbeddedDbInstance) getDbInstance()).setCompact(true);
    }

    protected void shutdown(EmbeddedDbInstance db) {}
    protected void removeDbFile() {
        if (!getDuckDbFile().delete()) {
            LOGGER.warn("Unable to remove duckdb file:" + getDbFile().getAbsolutePath());
        }
    }
    /*------------------*/

    protected String rownumSql() {
        return "row_number() over()";
    }

    public List<String> getTableNames() {
        String sql = "SELECT table_name FROM duckdb_tables()";
        return JdbcFactory.getSimpleTemplate(getDbInstance()).query(sql, (rs, i) -> rs.getString(1));
    }

    public DbAdapter.DbStats getDbStats() {
        DbStats dbStats = new DbStats();
        try {
            var db = getDbInstance(false);
            if (db == null)  return dbStats;

            SimpleJdbcTemplate jdbc = JdbcFactory.getSimpleTemplate(db);
            jdbc.queryForObject("SELECT count(*), sum(estimated_size) from duckdb_tables() where not REGEXP_MATCHES(table_name,'.*_DD$|.*_META$|.*_AUX$')", (rs, i) -> {
                dbStats.tblCnt = rs.getInt(1);
                dbStats.totalRows = rs.getInt(2);
                return null;
            });
            jdbc.queryForObject("SELECT column_count, estimated_size from duckdb_tables() where table_name = 'DATA'", (rs, i) -> {
                dbStats.colCnt = rs.getInt(1);
                dbStats.rowCnt = rs.getInt(2);
                return null;
            });
            dbStats.memory = jdbc.queryForLong("select sum(memory_usage_bytes) from duckdb_memory()");

        } catch (Exception ignored) {}
        return dbStats;
    }

    /* Use DuckDb Appender for improved performance. */
    protected int createDataTbl(DataGroup dg, String tblName) throws DataAccessException {

        DataType[] colsAry = EmbeddedDbUtil.makeDbCols(dg);
        int totalRows = dg.size();

        String createDataSql = createDataSql(colsAry, tblName);

        try (DuckDBConnection conn = (DuckDBConnection) JdbcFactory.getDataSource(getDbInstance()).getConnection();){
            Statement  stmt = conn.createStatement();
            stmt.execute(createDataSql);

            // using try-with-resources to automatically close the appender at the end of the scope
            try (var appender = conn.createAppender(DuckDBConnection.DEFAULT_SCHEMA, tblName)) {

                for (int r=0; r < totalRows; r++) {
                    Object[] row = dg.get(r).getData();
                    addRow(appender, row, r);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            LOGGER.error(e, "Failed to create table: " + tblName);
            throw new DataAccessException(e);
        }
        return totalRows;
    }

    private void addRow(DuckDBAppender appender, Object[] row, int ridx) throws SQLException {
        appender.beginRow();
        for (Object d : row) {
            if (d == null) {
                appender.append(null);
            } else if (d instanceof Boolean v) {
                appender.append(v);
            } else if (d instanceof Byte v) {
                appender.append(v);
            } else if (d instanceof Short v) {
                appender.append(v);
            } else if (d instanceof Integer v) {
                appender.append(v);
            } else if (d instanceof Long v) {
                appender.append(v);
            } else if (d instanceof Float v) {
                appender.append(v);
            } else if (d instanceof Double v) {
                appender.append(v);
            } else if (d instanceof String v) {
                appender.append(v);
            } else if (d instanceof BigDecimal v) {
                appender.appendBigDecimal(v);
            } else if (d instanceof Date v) {
                appender.appendLocalDateTime(LocalDateTime.ofInstant(v.toInstant(), ZoneId.of("UTC")));  // date/time should be stored as utc.
            }
            else {
                throw new IllegalStateException("Unexpected value: " + d);
            }
        }
        appender.append(ridx);         // add ROW_IDX
        appender.append(ridx);         // add ROW_NUM
        appender.endRow();
    }

    public String translateSql(String sql) {
        // duckdb does not support BEFORE.  new column will always be added to the end.
        // when a column data type is changed, we delete the old column and add the new one.
        // this will look a bit weird in the UI.  TODO: need to revisit
        if (sql.matches("ALTER\\s+TABLE .+ ADD\\s+COLUMN .+")) {
            return sql.replaceAll("BEFORE .+$", "");
        }
        return sql;
    }

//====================================================================
//
//====================================================================

    /* implement DbAdapterCreator */
    public DuckDbAdapter() { super(null); }
    public DbAdapter create(File dbFile) {
        return canHandle(dbFile) ? new DuckDbAdapter(dbFile) : null;
    }

    File getDuckDbFile() { return getDbFile(); }

}
