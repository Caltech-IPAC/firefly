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
public class SqliteDbAdapter extends BaseDbAdapter {
    public static final String NAME = "sqlite";
    private static final List<String> SUPPORTS = List.of(NAME);

    public SqliteDbAdapter(DbFileCreator dbFileCreator) { this(dbFileCreator.create(NAME)); }
    public SqliteDbAdapter(File dbFile) { super(dbFile); }

    public String getName() {
        return NAME;
    }
    protected static List<String> supportFileExtensions() { return SUPPORTS; }

    protected EmbeddedDbInstance createDbInstance() {
        String dbUrl = "jdbc:sqlite:%s".formatted(getDbFile().getPath());
        return new EmbeddedDbInstance(getName(), this, dbUrl, "org.sqlite.JDBC");
    }

    public boolean useTxnDuringLoad() {
        return true;
    }

    protected String rowNumSql() {
        return "ROWID";
    }
}
