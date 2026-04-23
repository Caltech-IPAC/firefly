/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db.jdbc;

import edu.caltech.ipac.firefly.server.db.jdbc.exceptions.UncategorizedSQLException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Executes a block of code within a JDBC transaction.
 * Binds the transactional connection to the current thread so that {@link JdbcTemplate}
 * instances sharing the same DataSource participate in the same transaction.
 */
public class TransactionTemplate {

    @FunctionalInterface
    public interface TransactionCallback<T> {
        T doInTransaction(Connection conn) throws Exception;
    }

    private final DataSource dataSource;

    public TransactionTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Execute the given callback within a transaction.
     * Commits on success, rolls back on any exception.
     */
    public <T> T execute(TransactionCallback<T> action) {
        Connection conn;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new UncategorizedSQLException("beginTransaction", e);
        }
        JdbcTemplate.bindConnection(dataSource, conn);
        try {
            T result = action.doInTransaction(conn);
            conn.commit();
            return result;
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        } finally {
            JdbcTemplate.unbindConnection(dataSource);
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

}
