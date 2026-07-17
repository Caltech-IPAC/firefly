/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.jdbc.DbInstanceDataSource;
import edu.caltech.ipac.firefly.server.db.jdbc.JdbcFactory;
import edu.caltech.ipac.firefly.server.db.jdbc.JdbcTemplate;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBConnection;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.core.Util.Try;
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
    public static final String EXT_DIR = AppProperties.getProperty("duckdb.ext.dir", System.getProperty("java.io.tmpdir"));
    public static final String ALLOWED_DIRS = "duckdb.allowed.dirs";
    public static String maxMemory = AppProperties.getProperty("duckdb.max.memory");        // in GB; 2G, 5.5G, etc
    private static int threadCnt=1;    // min 125mb per thread.  recommend 5gb per thread; we will config 1gb per thread but not more than 4.
    private static String allowedDirs = null;

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
    DuckDbAdapter() { super(null) ;}

    public String getName() { return NAME; }

    protected EmbeddedDbInstance createDbInstance() {
        String filePath = getDbFile() == null ? "" : getDbFile().getAbsolutePath();
        String dbUrl = "jdbc:duckdb:" + filePath;
        var db = new EmbeddedDbInstance(getName(), this, dbUrl, DRIVER) {
            @Override
            public DataSource createDataSource() {
                EmbeddedDbInstance self = this;
                return new DbInstanceDataSource(this) {
                    @Override
                    public Connection getConnection(String username, String password) throws SQLException {
                        synchronized (self) {
                            if (self.rootConn == null || self.rootConn.isClosed()) {
                                self.rootConn = super.getConnection(username, password);
                            }
                            try {
                                return newTrackedConn(self, self.rootConn);
                            } catch (SQLException e) {
                                // rootConn may be in a dirty state; reset and retry once
                                try { self.rootConn.close(); } catch (Exception ignored) {}
                                self.rootConn = super.getConnection(username, password);
                                return newTrackedConn(self, self.rootConn);
                            }
                        }
                    }
                };
            }
        };
        db.consumeProps("memory_limit=%s,threads=%d,extension_directory=%s".formatted(maxMemory, threadCnt, EXT_DIR));
        db.getProps().put("errors_as_json", "true");
        // As of v1.5.2.1, security can only be enforced if external access is disabled.
        // When we need to query from s3 or other external sources, we will need enforce security at a different layer.
        db.getProps().put("enable_external_access", "false");
        db.getProps().put("allowed_directories", "[%s]".formatted(getAllowedDirs()));
        return db;
    }

    private static String getAllowedDirs() {
        if (allowedDirs == null) {
            List<File> dirs = new ArrayList<>();
            dirs.add(ServerContext.getWorkingDir());
            dirs.add(ServerContext.getSharedWorkingDir());
            String addtlAllowed = AppProperties.getProperty(ALLOWED_DIRS);
            if (!isEmpty(addtlAllowed)) {
                for (String d : addtlAllowed.split(",")) {
                    File f = new File(d.trim());
                    if (f.exists() && f.isDirectory()) dirs.add(f);
                }
            }

            allowedDirs = dirs.stream()
                    .map(f -> "'" + f.getAbsolutePath() + "'")
                    .collect(Collectors.joining(", "));
        }
        return allowedDirs;
    }

    void createUDFs() {
        JdbcTemplate jdbc = getJdbc();
        for (String cf : customFunctions) {
            try {
                jdbc.update(cf);
            } catch (Exception ex) {
                LOGGER.error("Fail to create custom function:" + cf);
            }
        }
    }

    @Override
    List<String> getColumnNamesFromSys(String forTable, String enclosedBy) {
        String sql = "select column_name from duckdb_columns() where table_name = '%s'".formatted(forTable.toUpperCase());
        return JdbcFactory.getTemplate(getDbInstance()).query(sql, (rs, i) -> (enclosedBy == null) ? rs.getString(1) : enclosedBy + rs.getString(1) + enclosedBy);
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

    @Override
    protected void shutdown(EmbeddedDbInstance db) {
        synchronized (db) {
            try {
                if (db.rootConn != null && !db.rootConn.isClosed()) db.rootConn.close();
            } catch (SQLException ignored) {}
            db.rootConn = null;
            db.activeConns.set(0);
        }
    }

    /** Wraps a duplicate connection in a proxy that closes the root when the last active connection is released. */
    private static Connection newTrackedConn(EmbeddedDbInstance db, Connection rootConn) throws SQLException {
        DuckDBConnection dup = ((DuckDBConnection) rootConn).duplicate();
        db.activeConns.incrementAndGet();
        return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class[]{Connection.class},
            (proxy, method, args) -> {
                if ("close".equals(method.getName())) {
                    releaseConn(db, dup);
                    return null;
                }
                if ("isWrapperFor".equals(method.getName()) && args != null && args[0] == DuckDBConnection.class) {
                    return true;
                }
                if ("unwrap".equals(method.getName()) && args != null && args[0] == DuckDBConnection.class) {
                    return dup;
                }
                try {
                    return method.invoke(dup, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        );
    }

    private static void releaseConn(EmbeddedDbInstance db, DuckDBConnection dup) {
        try { dup.close(); } catch (Exception ignored) {}
        synchronized (db) {
            if (db.activeConns.decrementAndGet() <= 0) {
                db.activeConns.set(0);
                if (db.rootConn != null) {
                    try { db.rootConn.close(); } catch (Exception ignored) {}
                    db.rootConn = null;
                }
            }
        }
    }

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
        // includes views because resultset tables (DATA_<hash>) are materialized as views;
        String sql = "SELECT table_name FROM duckdb_tables() UNION ALL SELECT view_name FROM duckdb_views() WHERE NOT internal";
        return JdbcFactory.getTemplate(getDbInstance()).query(sql, (rs, i) -> rs.getString(1));
    }

    private List<String> getViewNames() {
        String sql = "SELECT view_name FROM duckdb_views() WHERE NOT internal";
        return JdbcFactory.getTemplate(getDbInstance()).query(sql, (rs, i) -> rs.getString(1));
    }

    /**
     * Instead of materializing the full resultset, which duplicates every requested
     * column for every matching row, and is what makes this expensive on a big table.
     * This builds:
     *   1. resultSetID_IDX: a small temp table of just (ROW_IDX, ROW_NUM), capturing the row's identity
     *      and its position in the requested order.
     *   2. resultSetID: a view joining that thin index back to DATA, projecting only the requested columns.
     *      Every existing caller keeps working unchanged. A view is still just a named, queryable object with
     *      the expected columns.  ROW_IDX/ROW_NUM are exposed from the index side so point lookups on them
     *      (highlighted row, selection remap) don't need the join at all.
     * See execRequestQuery() for the other half of this: paging against resultSetID must filter explicitly
     * on ROW_NUM rather than relying on plain LIMIT/OFFSET, or DuckDB ends up joining everything before it
     * can slice out a page.
     */
    @Override
    protected void buildResultSet(TableServerRequest treq, String resultSetID) {
        List<String> cols = getResultSetCols(treq);

        String wherePart = wherePart(treq);
        // without an explicit sort, fall back to ROW_IDX so ROW_NUM is deterministic and matches natural
        // ingest order. DuckDB does not otherwise guarantee scan order is preserved without an ORDER BY.
        String orderBy = treq.getSortInfo() != null ? orderByPart(treq) : "ORDER BY ROW_IDX";

        String idxTable = getIdxTable(resultSetID);
        String idxSql = "select ROW_IDX, (%s -1) as %s from (select ROW_IDX FROM %s %s %s) as b"
                .formatted(rowNumSql(), DataGroup.ROW_NUM, getDataTable(), wherePart, orderBy);
        execUpdate("CREATE TABLE IF NOT EXISTS %s AS (%s)".formatted(idxTable, idxSql));

        String selectCols = cols.isEmpty() ? "" : StringUtils.toString(cols) + ",";
        String viewSql = "select %s i.ROW_IDX, i.ROW_NUM from %s i join %s d on d.ROW_IDX = i.ROW_IDX"
                .formatted(selectCols, idxTable, getDataTable());
        execUpdate("CREATE VIEW IF NOT EXISTS %s AS (%s)".formatted(resultSetID, viewSql));
    }

    /**
     * A join has no inherent row order the way a materialized, physically-sorted table does, so any read of a
     * resultset backed by our thin (ROW_IDX, ROW_NUM) index needs an explicit ORDER BY ROW_NUM to be correct.
     * For paging specifically, ROW_NUM also needs to be filtered (not just sorted) so
     * DuckDB can prune the (small) index before joining to DATA; plain LIMIT/OFFSET against the view instead
     * joins everything first and slices afterward.
     * Only applies when there's no filter/sort of its own (ie. plain reads of an already-resolved resultset);
     * anything else falls back to normal.
     */
    @Override
    public DataGroupPart execRequestQuery(TableServerRequest treq, String forTable) throws DataAccessException {
        if (isEmpty(wherePart(treq)) && treq.getSortInfo() == null && hasTable(getIdxTable(forTable))) {
            return execIndexedPage(treq, forTable);
        }
        return super.execRequestQuery(treq, forTable);
    }

    private DataGroupPart execIndexedPage(TableServerRequest treq, String forTable) throws DataAccessException {
        String selectPart = selectPart(treq);
        boolean isPaged = !isEmpty(pagingPart(treq));
        String rowNumFilter = isPaged
                ? "WHERE ROW_NUM >= %d AND ROW_NUM < %d".formatted(treq.getStartIndex(), treq.getStartIndex() + treq.getPageSize())
                : "";
        String sql = "%s FROM %s %s ORDER BY ROW_NUM".formatted(selectPart, forTable, rowNumFilter);
        DataGroup data = execQuery(sql, forTable);

        int rowCnt = data.size();
        if (isPaged) {
            rowCnt = getJdbc().queryForInt("select count(*) FROM %s".formatted(getIdxTable(forTable)));
        }

        DataGroupPart page = toDataGroupPart(data, treq);
        page.setRowCount(rowCnt);
        if (!isEmpty(treq.getTblTitle())) {
            page.getData().setTitle(treq.getTblTitle());
        }
        return page;
    }

    private static String getIdxTable(String resultSetID) {
        return resultSetID + "_IDX";
    }

    @Override
    protected String[] dropStatementsFor(List<String> names) {
        List<String> views = getViewNames();
        return names.stream()
                .map(n -> (views.contains(n) ? "drop view IF EXISTS " : "drop table IF EXISTS ") + n)
                .toArray(String[]::new);
    }

    public DbAdapter.DbStats getDbStats() {
        DbStats dbStats = new DbStats();
        try {
            var db = getDbInstance(false);
            if (db == null)  return dbStats;

            JdbcTemplate jdbc = JdbcFactory.getTemplate(db);
            jdbc.query("SELECT count(*), sum(estimated_size) from duckdb_tables() where not REGEXP_MATCHES(table_name,'.*_DD$|.*_META$|.*_AUX$')", (rs, i) -> {
                dbStats.tblCnt = rs.getInt(1);
                dbStats.totalRows = rs.getInt(2);
                return null;
            });
            jdbc.query("SELECT column_count, estimated_size from duckdb_tables() where table_name = 'DATA'", (rs, i) -> {
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

        try (Connection wrapper = JdbcFactory.getDataSource(getDbInstance()).getConnection()) {
            DuckDBConnection conn = wrapper.unwrap(DuckDBConnection.class);
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createDataSql);
            }

            if (totalRows > 0) {
                // using try-with-resources to automatically close the appender at the end of the scope
                try (var appender = conn.createAppender(DuckDBConnection.DEFAULT_SCHEMA, tblName)) {
                    List<Integer> aryIdx = colIdxWithArrayData(colsAry);
                    for (int r = 0; r < totalRows; r++) {
                        Object[] row = dg.get(r).getData();
                        aryIdx.forEach(idx -> row[idx] = Util.serialize(row[idx]));      // serialize array data if necessary
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
            switch (d) {
                case null -> appender.appendNull();
                case Boolean v -> appender.append(v);
                case Byte v -> appender.append(v);
                case Short v -> appender.append(v);
                case Integer v -> appender.append(v);
                case Long v -> appender.append(v);
                case Float v -> appender.append(v);
                case Double v -> appender.append(v);
                case String v -> appender.append(v);
                case Character v -> appender.append(String.valueOf(v));
                case BigDecimal v -> appender.append(v.doubleValue());
                case java.sql.Date v -> appender.append(v.toLocalDate().atStartOfDay());
                case LocalDate v -> appender.append(v.atStartOfDay());
                case LocalDateTime v -> appender.append(v.atZone(ZoneOffset.UTC).toLocalDateTime());
                case Date v -> appender.append(LocalDateTime.ofInstant(v.toInstant(), ZoneOffset.UTC));    // date/time should be stored as utc.
                default -> throw new IllegalStateException("Unexpected value: " + d);
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
                String raw = ex.getMessage();
                int jsonStart = raw.indexOf('{');
                if (jsonStart > 0) raw = raw.substring(jsonStart);
                JSONObject json = (JSONObject) JSONValue.parse(raw);
                String msg = json.get("exception_message").toString().split("\n")[0];
                String type = Try.it(() -> json.get("error_subtype").toString())
                                    .getOrElse(json.get("exception_type").toString());
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