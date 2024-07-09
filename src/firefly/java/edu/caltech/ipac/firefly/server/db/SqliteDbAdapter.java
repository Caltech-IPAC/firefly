/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import java.io.File;
import java.util.List;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class SqliteDbAdapter extends BaseDbAdapter implements DbAdapter.DbAdapterCreator {
    public static final String NAME = "sqlite";
    private static final List<String> SUPPORTS = List.of(NAME);

    /**
     * used by DbAdapterCreator only
     */
    SqliteDbAdapter() {
        super(null);
    }

    public SqliteDbAdapter(File dbFile) { super(dbFile); }

    public DbAdapter create(File dbFile) {
        return canHandle(dbFile) ? new SqliteDbAdapter(dbFile) : null;
    }

    List<String> getSupportedExts() {
        return  SUPPORTS;
    }

    public String getName() {
        return NAME;
    }
    protected static List<String> supportFileExtensions() { return SUPPORTS; }

    protected EmbeddedDbInstance createDbInstance() {
        String dbUrl = String.format("jdbc:sqlite:%s", getDbFile().getPath());
        return new EmbeddedDbInstance(getName(), this, dbUrl, "org.sqlite.JDBC");
    }

    public boolean useTxnDuringLoad() {
        return true;
    }

    protected String rowNumSql() {
        return "ROWID";
    }
}
