/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import java.io.File;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class SqliteDbAdapter extends BaseDbAdapter {

    public String getName() {
        return SQLITE;
    }

    public DbInstance getDbInstance(File dbFile) {
        String dbUrl = String.format("jdbc:sqlite:%s", dbFile.getPath());
        return new EmbeddedDbInstance(getName(), dbUrl, "org.sqlite.JDBC");
    }

    public boolean useTxnDuringLoad() {
        return true;
    }

    public String translateSql(String sql) {
        return sql.replaceAll("ROWNUM", "ROWID");
    }
}
