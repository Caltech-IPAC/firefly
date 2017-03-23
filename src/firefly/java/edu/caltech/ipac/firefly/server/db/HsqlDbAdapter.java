/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.TableServerRequest;

import java.io.File;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class HsqlDbAdapter extends BaseDbAdapter{

    public String getName() {
        return HSQL;
    }

    public DbInstance getDbInstance(File dbFile) {
        String dbUrl = String.format("jdbc:hsqldb:file:%s;sql.syntax_ora=true;hsqldb.cache_rows=1000000;hsqldb.cache_size=1048576", dbFile.getPath());
        return new DbInstance(false, null, dbUrl, null, null, "org.hsqldb.jdbc.JDBCDriver", getName());
    }

    public String createTableFromSelect(String tblName, String selectSql) {
        return String.format("CREATE TABLE IF NOT EXISTS %s AS (%s) WITH DATA", tblName, selectSql);
    }

    public String translateSql(String sql) {
        return sql.replaceAll("ROWID", "ROWNUM");
    }
}
