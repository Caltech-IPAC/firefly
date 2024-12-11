/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.util.AppProperties;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class DbInstance {
    public static final DbInstance archive = new DbInstance("archive");
    public static final DbInstance operation = new DbInstance("operation");
    public static final String USE_REAL_AS_DOUBLE = "useRealAsDouble";

    private static final String POOL_PATH = "connection.datasource";
    private static final String USE_POOL = "use.connection.pool";

    private static final String DB_URL = "db.url";
    private static final String USER_NAME = "db.userId";
    private static final String USER_PASSWORD = "db.password";
    private static final String JDBC_DRIVER = "db.driver";
    private static final String DB_PROPS = "db.props";

    public boolean isPooled;
    public String datasourcePath;
    public String dbUrl;
    public String userId;
    public String password;
    public String jdbcDriver;
    public String name;
    public Map<String,String> props;

    /**
     * convenience constructor to create a DbInstance using properties based on
     * the given name.
     * @param name  the dbInstance name.  Property is constructed using {name}.{prop}, eg. mydb.db.driver=org.duckdb.DuckDBDriver
     */
    public DbInstance(String name) {
        this.name = name;
        isPooled = AppProperties.getBooleanProperty(name + "." + USE_POOL, true);
        datasourcePath = AppProperties.getProperty(name + "." + POOL_PATH);
        dbUrl = AppProperties.getProperty(name + "." + DB_URL);
        userId = AppProperties.getProperty(name + "." + USER_NAME);
        password = AppProperties.getProperty(name + "." + USER_PASSWORD);
        jdbcDriver = AppProperties.getProperty(name + "." + JDBC_DRIVER);
        consumeProps(AppProperties.getProperty(name + "." + DB_PROPS));
    }

    public DbInstance(boolean pooled, String datasourcePath, String dbUrl,
                      String userId, String password, String jdbcDriver, String name) {
        isPooled = pooled;
        this.datasourcePath = datasourcePath;
        this.dbUrl = dbUrl;
        this.userId = userId;
        this.password = password;
        this.jdbcDriver = jdbcDriver;
        this.name = name;
    }

    public boolean getBoolProp(String key, boolean def) {
        String s = getProp(key);
        if (s == null) return def;
        return Boolean.parseBoolean(s);
    }

    public String getProp(String key) {
        return props == null ? null : props.get(key);
    }

    public Map<String, String> getProps() {
        return props;
    }

    public void consumeProps(String propsStr) {
        if (!isEmpty(propsStr)) {
            String[] parts = propsStr.split(",");
            if (parts.length > 0) {
                this.props = new HashMap<>(parts.length);
                Arrays.stream(parts).forEach((s -> {
                    String[] keyval = s.split("=");
                    if (keyval.length > 0) {
                        String val = keyval.length == 1 ? null : keyval[1];
                        props.put(keyval[0], val);
                    }
                }));
            }
        }
    }

    public String name() {
        return name;
    }
    public String getDbUrl() {
        return this.dbUrl;
    }
    public boolean isPooled() { return isPooled; }
    public void setPooled(boolean pooled) { isPooled = pooled;}
    public boolean testConn(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            return true;
        } catch (SQLException e) { return false; }
    };

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DbInstance c) {
            return (c.name + c.dbUrl).equals(name + dbUrl);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dbUrl);
    }

}
