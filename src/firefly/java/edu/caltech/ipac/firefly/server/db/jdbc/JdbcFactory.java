/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db.jdbc;

import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.util.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static edu.caltech.ipac.firefly.core.Util.Try;

/**
 * Date: Oct 7, 2008
 *
 * @author loi
 * @version $Id: JdbcFactory.java,v 1.7 2011/01/10 19:34:18 tatianag Exp $
 */
public class JdbcFactory {
    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private static final DsMapThreadLocal dataSourceMap = new DsMapThreadLocal();

    /**
     * return the central facade class of the Spring JDBC library.
     * @param dbInstance
     * @return
     */
    public static JdbcTemplate getTemplate(DbInstance dbInstance) {
        DataSource datasource = Try.until( () -> getDataSource(dbInstance), Objects::nonNull,3)
                                    .getOrElse((e) -> logger.info("Failed to get DataSource after 3 tries"));
        return  datasource == null ? null : new JdbcTemplate(datasource);
    }

    /**
     * This template handles the transaction lifecycle and possible exceptions
     * such that neither the TransactionCallback implementation nor the calling
     * code needs to explicitly handle transactions.
     * @param dataSource
     * @return
     */
    public static TransactionTemplate getTransactionTemplate(DataSource dataSource) {
        return new TransactionTemplate(dataSource);
    }

    public static DataSource getDataSource(DbInstance dbInstance) {
        try {
            if (!dbInstance.isPooled()) return getDirectDataSource(dbInstance);

            DataSource ds = dataSourceMap.get().get(dbInstance);
            if (ds == null) {
                ds = getDirectDataSource(dbInstance);
                dataSourceMap.get().put(dbInstance, ds);
            }
            return ds;
        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    private static DataSource getDirectDataSource(DbInstance dbInstance) {
        DriverManagerDataSource ds = new DriverManagerDataSource(dbInstance);
        logger.trace("Getting a new database connection for " + dbInstance.dbUrl + " using DriverManager",
                "DataSource returned: " + ds);
        return ds;
    }

    public static Connection getConnection(DataSource dataSource) throws SQLException {
        return dataSource.getConnection();
    }

    public static void releaseConnection(Connection conn, DataSource dataSource) {
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    static class DsMapThreadLocal extends InheritableThreadLocal<Map<DbInstance, DataSource>> {
        @Override
        protected Map<DbInstance, DataSource> initialValue() {
            return new HashMap<>();
        }
    }
}
