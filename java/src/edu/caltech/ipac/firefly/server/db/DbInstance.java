package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.util.AppProperties;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class DbInstance {
    public static final DbInstance archive = new DbInstance("archive");
    public static final DbInstance operation = new DbInstance("operation");
    public static final DbInstance josso = new DbInstance("josso");

    private static final String POOL_PATH = "connection.datasource";
    private static final String USE_POOL = "use.connection.pool";

    private static final String DB_URL = "db.url";
    private static final String USER_NAME = "db.userId";
    private static final String USER_PASSWORD = "db.password";
    private static final String JDBC_DRIVER = "db.driver";

    public boolean isPooled;
    public String datasourcePath;
    public String dbUrl;
    public String userId;
    public String password;
    public String jdbcDriver;
    public String name;

    /**
     * convenience constructor to create a DbInstance using properties based on
     * the given name.
     * @param name
     */
    public DbInstance(String name) {
        this.name = name;
        isPooled = AppProperties.getBooleanProperty(name + "." + USE_POOL, true);
        datasourcePath = AppProperties.getProperty(name + "." + POOL_PATH);
        dbUrl = AppProperties.getProperty(name + "." + DB_URL);
        userId = AppProperties.getProperty(name + "." + USER_NAME);
        password = AppProperties.getProperty(name + "." + USER_PASSWORD);
        jdbcDriver = AppProperties.getProperty(name + "." + JDBC_DRIVER);
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

    public String name() {
        return name;
    }
}
