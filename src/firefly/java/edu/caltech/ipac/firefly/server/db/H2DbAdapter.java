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
public class H2DbAdapter extends BaseDbAdapter {
    public static final String NAME = "mv.db";

    private static final List<String> SUPPORTS = List.of(NAME, "h2.db", "db");

    /**
     * used by DbAdapterCreator only
     */
    public H2DbAdapter (DbFileCreator dbFileCreator) { this(dbFileCreator.create(NAME)); }
    public H2DbAdapter(File dbFile) {
        super(dbFile);
    }

    public String getName() { return NAME; }
    protected static List<String> supportFileExtensions() { return SUPPORTS; }

    protected EmbeddedDbInstance createDbInstance() {
        String dbUrl = "jdbc:h2:%s;CACHE_SIZE=1048576;LOG=0;UNDO_LOG=0;MVCC=true".formatted(getDbFile().getPath());
        return new EmbeddedDbInstance(getName(), this, dbUrl, "org.h2.Driver");
    }

    public String createTableFromSelect(String tblName, String selectSql) {
        return "CREATE TABLE %s AS (%s)".formatted(tblName, selectSql);
    }

}
