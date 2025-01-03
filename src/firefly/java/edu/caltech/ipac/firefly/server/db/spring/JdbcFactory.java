/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db.spring;

import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.util.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

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
        TransactionTemplate txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        txTemplate.setPropagationBehavior(DefaultTransactionDefinition.PROPAGATION_REQUIRED);
        return txTemplate;
    }

    /**
     * return a simplified version of the JdbcTemplate, allowing
     * easy access to common tasks.  Plus, it takes advantage of
     * java 5 generics.
     * @param dbInstance
     * @return
     */
    public static SimpleJdbcTemplate getSimpleTemplate(DbInstance dbInstance) {
        DataSource datasource = Try.until( () -> getDataSource(dbInstance), Objects::nonNull,3)
                                    .getOrElse((e) -> logger.info("Failed to get DataSource after 3 tries"));
        return datasource == null ? null :  new SimpleJdbcTemplate(datasource);
    }

    /**
     * Returns a DataSource wrapper that ensures the underlying connection is not closed automatically.
     * The connection will remain open until explicitly closed by the caller,
     * allowing multiple tasks to be executed on the same connection.
     * Note: This implementation is not thread-safe.
     *
     * @param dbInstance the database to connect to
     * @return a {@link SharedDS} instance for the specified database instance
     */
    public static SharedDS getSharedDS(DbInstance dbInstance) {
        return new SharedDS(dbInstance);
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
        DriverManagerDataSource driver = new DataSourceWithProps(dbInstance);

        driver.setDriverClassName(dbInstance.jdbcDriver);
        logger.trace("Getting a new database connection for " + dbInstance.dbUrl + " using DriverManager",
                "DataSource returned: " + driver);
        return driver;
    }

    private static DataSource getPooledDataSource(DbInstance dbInstance) throws Exception {
        Context initContext = new InitialContext();
        Context envContext  = (Context)initContext.lookup("java:/comp/env");
        DataSource ds = (DataSource)envContext.lookup(dbInstance.datasourcePath);
        logger.trace("Getting pooled database connection from " + dbInstance.datasourcePath,
                "returned DataSource:" + ds);
        return ds;
    }

    static class DsMapThreadLocal extends InheritableThreadLocal<Map<DbInstance, DataSource>> {

        @Override
        protected Map<DbInstance, DataSource> initialValue() {
            return new HashMap<DbInstance, DataSource>();
        }
    }

    /**
     * This class will add all the properties defined in props to the connection.
     * Super class's setConnectionProperties will only set them as default values which will
     * not be used unless it is specifically queried.
     */
    static class DataSourceWithProps extends DriverManagerDataSource {
        DbInstance dbInstance;

        public DataSourceWithProps(DbInstance dbi) {
            super(dbi.dbUrl, dbi.userId, dbi.password);
            dbInstance = dbi;
        }

        @Override
        protected Connection getConnectionFromDriverManager(String url, Properties props) throws SQLException {
            Connection conn = super.getConnectionFromDriverManager(url, addProps(props));
            return (dbInstance.testConn(conn) ? conn : null);
        }

        private Properties addProps(Properties props) {
            if (dbInstance.props != null) props.putAll(dbInstance.props);
            return props;
        }

    }

    public static class SharedDS implements AutoCloseable {
        DataSource ds;
        public SharedDS(DbInstance di) {
            try {
                ds = new SingleConnectionDataSource(getDataSource(di).getConnection(), false);
            } catch (Exception e) {
                logger.error(e);
                throw new IllegalArgumentException("Failed to get DataSource");
            }
        }
        public void close() throws Exception {
            Try.it(() -> ds.getConnection().close());
        }
        public DataSource get() {
            return ds;
        }
        public SimpleJdbcTemplate getJdbc() {
            return new SimpleJdbcTemplate(ds);
        }
        public JdbcTemplate getTmpl() {
            return new JdbcTemplate(ds);
        }
    }
}
