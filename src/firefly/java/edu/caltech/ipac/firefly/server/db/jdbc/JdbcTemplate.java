/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db.jdbc;

import edu.caltech.ipac.firefly.server.db.jdbc.exceptions.BadSqlGrammarException;
import edu.caltech.ipac.firefly.server.db.jdbc.exceptions.DataIntegrityViolationException;
import edu.caltech.ipac.firefly.server.db.jdbc.exceptions.UncategorizedSQLException;
import edu.caltech.ipac.firefly.server.util.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.util.CollectionUtil.isEmpty;

/**
 * JDBC template for executing SQL against a DataSource.
 * May participates in transactions started by {@link TransactionTemplate} via a thread-local connection registry.
 */
public class JdbcTemplate {

    // Thread-local map: DataSource → transactional Connection
    // Package-private so TransactionTemplate can bind/unbind connections.
    static final ThreadLocal<Map<DataSource, Connection>> TXN_CONNECTIONS =
            ThreadLocal.withInitial(HashMap::new);

    private final DataSource dataSource;
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    public JdbcTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    // ---------------------------------------------------------------
    // query
    // ---------------------------------------------------------------

    public <T> T query(String sql, ResultSetExtractor<T> rse) {
        LOGGER.debug("query: " + sql);
        Connection conn = acquireConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rse.extractData(rs);
        } catch (SQLException e) {
            throw translate(sql, e);
        } finally {
            releaseConnection(conn);
        }
    }

    public <T> T query(String sql, ResultSetExtractor<T> rse, Object... params) {
        if (params == null || params.length == 0) return query(sql, rse);
        LOGGER.debug("query: " + sql, "params: " + java.util.Arrays.toString(params));
        Connection conn = acquireConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rse.extractData(rs);
            }
        } catch (SQLException e) {
            throw translate(sql, e);
        } finally {
            releaseConnection(conn);
        }
    }

    public <T> List<T> query(String sql, ParameterizedRowMapper<T> mapper) {
        return query(sql, rs -> mapRows(rs, mapper));
    }

    public <T> List<T> query(String sql, ParameterizedRowMapper<T> mapper, Object... params) {
        return query(sql, rs -> mapRows(rs, mapper), params);
    }

    // ---------------------------------------------------------------
    // queryForObject
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, Class<T> type) {
        Connection conn = acquireConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (!rs.next()) return null;
            Object val = rs.getObject(1);
            if (val == null) return null;
            if (type.isInstance(val)) return type.cast(val);
            if (type == String.class) return type.cast(val.toString());
            if (type == Integer.class || type == int.class) return (T) Integer.valueOf(((Number) val).intValue());
            if (type == Long.class    || type == long.class) return (T) Long.valueOf(((Number) val).longValue());
            if (type == Double.class  || type == double.class) return (T) Double.valueOf(((Number) val).doubleValue());
            return type.cast(val);
        } catch (SQLException e) {
            throw translate(sql, e);
        } finally {
            releaseConnection(conn);
        }
    }

    public <T> T queryForObject(String sql, ParameterizedRowMapper<T> mapper) {
        List<T> results = query(sql, mapper);
        return results.isEmpty() ? null : results.getFirst();
    }

    public <T> T queryForObject(String sql, ParameterizedRowMapper<T> mapper, Object... params) {
        List<T> results = query(sql, mapper, params);
        return results.isEmpty() ? null : results.getFirst();
    }

    // ---------------------------------------------------------------
    // queryForInt / queryForLong
    // ---------------------------------------------------------------

    public int queryForInt(String sql) {
        Integer v = queryForObject(sql, Integer.class);
        return v != null ? v : 0;
    }

    public int queryForInt(String sql, Object... params) {
        return query(sql, rs -> rs.next() ? rs.getInt(1) : 0, params);
    }

    public long queryForLong(String sql) {
        Long v = queryForObject(sql, Long.class);
        return v != null ? v : 0L;
    }

    // ---------------------------------------------------------------
    // queryForMap / queryForList
    // ---------------------------------------------------------------

    public Map<String, Object> queryForMap(String sql, Object... params) {
        List<Map<String, Object>> rows = (params == null || params.length == 0)
                ? queryForList(sql)
                : query(sql, JdbcTemplate::extractRows, params);
        return rows.isEmpty() ? new LinkedHashMap<>() : rows.getFirst();
    }

    public List<Map<String, Object>> queryForList(String sql) {
        return query(sql, JdbcTemplate::extractRows);
    }

    // ---------------------------------------------------------------
    // update / execute
    // ---------------------------------------------------------------

    public int update(String sql, Object... params) {
        if (params == null || params.length == 0) LOGGER.debug("update: " + sql);
        else LOGGER.debug("update: " + sql, "params: " + java.util.Arrays.toString(params));
        Connection conn = acquireConnection();
        try {
            if (params == null || params.length == 0) {
                try (Statement stmt = conn.createStatement()) {
                    return stmt.executeUpdate(sql);
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    setParams(ps, params);
                    return ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw translate(sql, e);
        } finally {
            releaseConnection(conn);
        }
    }

    public void execute(String sql) {
        LOGGER.debug("execute: " + sql);
        Connection conn = acquireConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw translate(sql, e);
        } finally {
            releaseConnection(conn);
        }
    }

    // ---------------------------------------------------------------
    // batchUpdate
    // ---------------------------------------------------------------

    public int[] batchUpdate(String[] sqls) {
        LOGGER.debug("batchUpdate: %d statements".formatted(sqls.length));
        Connection conn = acquireConnection();
        try (Statement stmt = conn.createStatement()) {
            for (String sql : sqls) stmt.addBatch(sql);
            return stmt.executeBatch();
        } catch (SQLException e) {
            throw translate(String.join("; ", sqls), e);
        } finally {
            releaseConnection(conn);
        }
    }

    public int[] batchUpdate(String sql, BatchPreparedStatementSetter setter) {
        int batchSize = setter.getBatchSize();
        LOGGER.debug("batchUpdate: " + sql, "batchSize: " + batchSize);
        Connection conn = acquireConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < batchSize; i++) {
                setter.setValues(ps, i);
                ps.addBatch();
            }
            return ps.executeBatch();
        } catch (SQLException e) {
            throw translate(sql, e);
        } finally {
            releaseConnection(conn);
        }
    }

    public void batchUpdate(String sql, List<Object[]> paramsList) {
        if (isEmpty(paramsList)) return;
        batchUpdate(sql, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Object[] row = paramsList.get(i);
                for (int j = 0; j < row.length; j++) ps.setObject(j + 1, row[j]);
            }
            public int getBatchSize() { return paramsList.size(); }
        });
    }

    // ---------------------------------------------------------------
    // connection management (package-private for TransactionTemplate)
    // ---------------------------------------------------------------

    static void bindConnection(DataSource ds, Connection conn) {
        TXN_CONNECTIONS.get().put(ds, conn);
    }

    static void unbindConnection(DataSource ds) {
        TXN_CONNECTIONS.get().remove(ds);
    }

    private Connection acquireConnection() {
        Connection txnConn = TXN_CONNECTIONS.get().get(dataSource);
        if (txnConn != null) return txnConn;
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new UncategorizedSQLException("getConnection", e);
        }
    }

    private void releaseConnection(Connection conn) {
        if (!TXN_CONNECTIONS.get().containsKey(dataSource)) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    // ---------------------------------------------------------------
    // exception translation
    // ---------------------------------------------------------------

    public static RuntimeException translate(String sql, SQLException e) {
        LOGGER.error("SQL error: " + e.getMessage(), "sql: " + sql, "sqlState: " + e.getSQLState());
        String state = e.getSQLState();
        if (state != null) {
            if (state.startsWith("42") || state.startsWith("37")) return new BadSqlGrammarException(sql, e);
            if (state.startsWith("23")) return new DataIntegrityViolationException(sql, e);
        }
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (msg.contains("syntax error") || msg.contains("object not found")
                || msg.contains("unexpected token") || msg.contains("user lacks privilege")) {
            return new BadSqlGrammarException(sql, e);
        }
        if (msg.contains("integrity constraint") || msg.contains("unique constraint")
                || msg.contains("duplicate key") || msg.contains("not null")) {
            return new DataIntegrityViolationException(sql, e);
        }
        return new UncategorizedSQLException(sql, e);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private static <T> List<T> mapRows(ResultSet rs, ParameterizedRowMapper<T> mapper) throws SQLException {
        List<T> results = new ArrayList<>();
        int rowNum = 0;
        while (rs.next()) {
            T row = mapper.mapRow(rs, rowNum++);
            if (row != null) results.add(row);
        }
        return results;
    }

    private static List<Map<String, Object>> extractRows(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int n = meta.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= n; i++) row.put(meta.getColumnLabel(i), rs.getObject(i));
            rows.add(row);
        }
        return rows;
    }

    private static void setParams(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
    }


//====================================================================
// callback interfaces
//====================================================================

    /**
     * Callback interface used by {@link JdbcTemplate#batchUpdate(String, BatchPreparedStatementSetter)}.
     * Replaces org.springframework.jdbc.core.BatchPreparedStatementSetter.
     */
    public interface BatchPreparedStatementSetter {
        void setValues(PreparedStatement ps, int i) throws SQLException;
        int getBatchSize();
    }

    /**
     * Callback interface used by {@link JdbcTemplate} to map rows of a ResultSet.
     * Replaces org.springframework.jdbc.core.simple.ParameterizedRowMapper.
     */
    @FunctionalInterface
    public interface ParameterizedRowMapper<T> {
        T mapRow(ResultSet rs, int rowNum) throws SQLException;
    }

    /**
     * Callback for processing an entire ResultSet at once.
     * Replaces org.springframework.jdbc.core.ResultSetExtractor.
     */
    @FunctionalInterface
    public interface ResultSetExtractor<T> {
        T extractData(ResultSet rs) throws SQLException;
    }
}
