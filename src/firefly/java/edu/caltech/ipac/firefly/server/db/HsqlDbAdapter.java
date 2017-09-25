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
        String dbUrl = String.format("jdbc:hsqldb:file:%s;hsqldb.cache_size=256000;hsqldb.log_size=256;sql.syntax_ora=true", dbFile.getPath());
        return new EmbeddedDbInstance(getName(), dbUrl, "org.hsqldb.jdbc.JDBCDriver");
    }

    public File getStorageFile(File dbFile) {
        return dbFile == null ? null : new File(dbFile.getParent(), dbFile.getName() + ".log");
    }

    public String createTableFromSelect(String tblName, String selectSql) {
        return String.format("CREATE TABLE IF NOT EXISTS %s AS (%s) WITH DATA", tblName, selectSql);
    }

}
