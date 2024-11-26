/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.AppProperties;
import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBConnection;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static edu.caltech.ipac.firefly.server.db.DuckDbUDF.*;
import static edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil.*;
import static edu.caltech.ipac.util.StringUtils.*;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class DuckDbAdapter extends BaseDbAdapter {
    public static final String NAME = "duckdb";
    public static final String DRIVER = "org.duckdb.DuckDBDriver";
    public static String maxMemory = AppProperties.getProperty("duckdb.max.memory");        // in GB; 2G, 5.5G, etc
    private static int threadCnt=1;    // min 125mb per thread.  recommend 5gb per thread; we will config 1gb per thread but not more than 4.

    static {
        if (DEF_DB_TYPE.equals(NAME)) {
            // no need manual cleanup; let DuckDB handles it.
            DbMonitor.MAX_MEMORY = 1_000_000_000_000L;
            DbMonitor.MAX_MEM_ROWS = DbMonitor.MAX_MEMORY;
            DbMonitor.MAX_IDLE_TIME = 60 * 1000 * 60;       // (60 minutes) since we don't compact duckdb, this is the time before info is removed from DB Monitor
        }
        if (isEmpty(maxMemory)) {
            ServerContext.Info sInfo = ServerContext.getSeverInfo();
            var dbMaxMem = Math.max(sInfo.pMemory() - sInfo.jvmMax(), 500*1024*1024);     // Greater of available RAM or 500MB.
            var maxMemInGb = dbMaxMem/(1024.0 * 1024 * 1024);
            maxMemory = "%.1fG".formatted(maxMemInGb);
            threadCnt = Math.max(Math.min(4, (int)maxMemInGb), 1);
        }
    }

    private static final String [] customFunctions = {
            decimate_key, lg, nvl2, deg2pix
    };

    private static final List<String> SUPPORTS = List.of("duckdb");

    public DuckDbAdapter(DbFileCreator dbFileCreator) { this(dbFileCreator.create(NAME)); }
    public DuckDbAdapter(File dbFile) { super(dbFile); }

    public String getName() { return NAME; }

    protected EmbeddedDbInstance createDbInstance() {
        String filePath = getDbFile() == null ? "" : getDbFile().getAbsolutePath();
        String dbUrl = "jdbc:duckdb:" + filePath;
        var db = new EmbeddedDbInstance(getName(), this, dbUrl, DRIVER) {
            public boolean testConn(Connection conn) {
                // test connection plus additional session-scoped properties
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("SET errors_as_json = true");
                    return true;
                } catch (SQLException e) { return false; }
            }
        };
        db.consumeProps("memory_limit=%s,threads=%d".formatted(maxMemory, threadCnt));
        return db;
    }

    void createUDFs() {
        for (String cf : customFunctions) {
            try {
                execUpdate(cf);
            } catch (Exception ex) {
                LOGGER.error("Fail to create custom function:" + cf);
            }
        }
    }

    @Override
    List<String> getColumnNamesFromSys(String forTable, String enclosedBy) {
        String sql = "select column_name from duckdb_columns() where table_name = '%s'".formatted(forTable.toUpperCase());
        return JdbcFactory.getSimpleTemplate(getDbInstance()).query(sql, (rs, i) -> (enclosedBy == null) ? rs.getString(1) : enclosedBy + rs.getString(1) + enclosedBy);
    }

    protected void renameColumn(String from, String to) {
        execUpdate("ALTER TABLE %s RENAME COLUMN \"%s\" TO \"%s\"".formatted(getDataTable(), from, to));
        execUpdate("UPDATE %s_DD SET cname='%s' WHERE cname='%s'".formatted(getDataTable(), to, from));
    }

    protected boolean useIndexWhenUpdateColumnValue() { return false; }

    public File initDbFile() throws IOException {
        close(true);              // if database exists in memory, close it and remove all files related to it.
        if (!getDbFile().getParentFile().exists()) getDbFile().getParentFile().mkdirs();
        createUDFs();   // add user defined functions
        return getDbFile();
    }

    public void compact() {
        // no need to compact.  it will automatically push out of memory.
        ((EmbeddedDbInstance) getDbInstance()).setCompact(true);
    }

    protected void shutdown(EmbeddedDbInstance db) {}
    protected void removeDbFile() {
        var dbFile = getDbFile();
        if (dbFile.exists()) {
            if (!dbFile.delete()) {
                LOGGER.trace("Unable to remove duckdb file:" + dbFile.getAbsolutePath());
            }
        }
    }
    /*------------------*/

    protected String rowNumSql() {
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

        try (DuckDBConnection conn = (DuckDBConnection) JdbcFactory.getDataSource(getDbInstance()).getConnection();
             Statement  stmt = conn.createStatement() ) {

            conn.setAutoCommit(false);
            stmt.execute(createDataSql);

            if (totalRows > 0) {
                // using try-with-resources to automatically close the appender at the end of the scope
                try (var appender = conn.createAppender(DuckDBConnection.DEFAULT_SCHEMA, tblName)) {
                    List<Integer> aryIdx = colIdxWithArrayData(colsAry);
                    for (int r = 0; r < totalRows; r++) {
                        Object[] row = dg.get(r).getData();
                        aryIdx.forEach(idx -> row[idx] = serialize(row[idx]));      // serialize array data if necessary
                        addRow(appender, row, r);
                    }
                    appender.flush();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            LOGGER.error(e, "Failed to create table: " + tblName);
            throw new DataAccessException(e);
        }
        return totalRows;
    }

    public static void addRow(DuckDBAppender appender, Object[] row, int ridx) throws SQLException {
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
            } else if (d instanceof java.sql.Date v) {
                appender.appendLocalDateTime(v.toLocalDate().atStartOfDay());
            } else if (d instanceof LocalDate v) {
                appender.appendLocalDateTime(v.atStartOfDay());
            } else if (d instanceof LocalDateTime v) {
                appender.appendLocalDateTime(v.atZone(ZoneOffset.UTC).toLocalDateTime());
            } else if (d instanceof Date v) {
                appender.appendLocalDateTime(LocalDateTime.ofInstant(v.toInstant(), ZoneOffset.UTC));  // date/time should be stored as utc.
            } else {
                throw new IllegalStateException("Unexpected value: " + d);
            }
        }
        appender.append(ridx);         // add ROW_IDX
        appender.append(ridx);         // add ROW_NUM
        appender.endRow();
    }

    @Override
    // DuckDB do not have a global property to make all LIKE operations case-insensitive
    String wherePart(TableServerRequest treq) {
        String where = super.wherePart(treq);
        return replaceLike(where);
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

    @Override
    public String interpretError(Throwable e) {
        try {
            if (e instanceof SQLException ex) {
                JSONObject json = (JSONObject) JSONValue.parse(ex.getMessage().replace("%s:".formatted(ex.getClass().getName()), ""));
                String msg = json.get("exception_message").toString().split("\n")[0];
                String type = getSafe(() -> json.get("error_subtype").toString(), json.get("exception_type").toString());
                return type + ":" + msg;
            }
            return super.interpretError(e);
        } catch (Exception ex) { return e.getMessage(); }
    }

    public static DataGroup getDuckDbSettings() {
        var db = new DuckDbAdapter((File) null);
        try {
            return db.execQuery("SELECT * FROM duckdb_settings() WHERE name in ('external_threads','max_memory','memory_limit','threads', 'worker_threads','TimeZone')", null);
        } catch (DataAccessException e) {
            LOGGER.error(e);
            return null;
        }
    }

    public static String replaceLike(String input) {
        return replaceUnquoted(input, "like", "ILIKE");
    }

}
