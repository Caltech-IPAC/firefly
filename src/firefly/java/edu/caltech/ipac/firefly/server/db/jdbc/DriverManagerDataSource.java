/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db.jdbc;

import edu.caltech.ipac.firefly.server.db.DbInstance;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * DataSource implementation backed by a {@link DbInstance}, creating a new connection per call via DriverManager.
 */
public class DriverManagerDataSource implements DataSource {

    private final DbInstance dbInstance;

    public DriverManagerDataSource(DbInstance dbInstance) {
        this.dbInstance = dbInstance;
        if (dbInstance.jdbcDriver != null) {
            try { Class.forName(dbInstance.jdbcDriver); } catch (ClassNotFoundException ignored) {}
        }
    }

    public Connection getConnection() throws SQLException {
        return getConnection(dbInstance.userId, dbInstance.password);
    }

    public Connection getConnection(String username, String password) throws SQLException {
        Properties props = new Properties();
        if (!isEmpty(username))           props.setProperty("user", username);
        if (!isEmpty(password))           props.setProperty("password", password);
        if (dbInstance.props != null)     props.putAll(dbInstance.props);

        Connection conn = DriverManager.getConnection(dbInstance.dbUrl, props);
        if (!dbInstance.testConn(conn)) {
            try { conn.close(); } catch (SQLException ignored) {}
            return null;
        }
        return conn;
    }

    public PrintWriter getLogWriter() { return null; }
    public void setLogWriter(PrintWriter out) {}
    public void setLoginTimeout(int seconds) {}
    public int getLoginTimeout() { return 0; }
    public Logger getParentLogger() { return Logger.getLogger(getClass().getName()); }
    public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("Not a wrapper for " + iface); }
    public boolean isWrapperFor(Class<?> iface) { return false; }
}
