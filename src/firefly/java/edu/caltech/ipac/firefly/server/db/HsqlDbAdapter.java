/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;

import java.io.File;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class HsqlDbAdapter extends BaseDbAdapter{

    public String getName() {
        return HSQL;
    }
    private static final String[] DB_FILES = new String[]{".properties",".script",".log",".data",".lck",".backup"};

    protected EmbeddedDbInstance createDbInstance(File dbFile) {
        String dbUrl = String.format("jdbc:hsqldb:file:%s;hsqldb.log_data=false;sql.syntax_ora=true;sql.ignore_case=true;sql.double_nan=false", dbFile.getPath());
        EmbeddedDbInstance dbInst = new EmbeddedDbInstance(getName(), dbFile, dbUrl, "org.hsqldb.jdbc.JDBCDriver");
        dbInst.consumeProps(String.format("%s=%s", DbInstance.USE_REAL_AS_DOUBLE, true));
        return dbInst;
    }

    public String createTableFromSelect(String tblName, String selectSql) {
        return String.format("CREATE TABLE %s AS (%s) WITH DATA", tblName, selectSql);
    }

    protected void shutdown(EmbeddedDbInstance db) {
        JdbcFactory.getTemplate(db).execute("SHUTDOWN");
        File f = new File(db.getDbFile() + ".lck");
        if (f.exists()) f.delete();
    }

    protected void removeDbFiles(File dbFile) {
        if (dbFile.exists()) dbFile.delete();
        for(String fname : DB_FILES) {
            File f = new File(dbFile + fname);
            if (f.exists()) f.delete();
        }
    }


    // this is a list of HSQLDB keywords
//    private static final List<String> KEYWORDS = Arrays.asList("ALL", "AND", "ANY", "AS", "AT", "AVG", "BETWEEN", "BOTH", "BY", "CALL", "CASE", "CAST", "COALESCE", "CORRESPONDING", "CONVERT", "COUNT", "CREATE",
//            "CROSS", "DEFAULT", "DISTINCT", "DROP", "ELSE", "EVERY", "EXISTS", "EXCEPT", "FOR", "FROM", "FULL", "GRANT", "GROUP", "HAVING", "IN", "INNER", "INTERSECT", "INTO", "IS", "JOIN", "LEFT", "LEADING",
//            "LIKE", "MAX", "MIN", "NATURAL", "NOT", "NULLIF", "ON", "ORDER", "OR", "OUTER", "PRIMARY", "REFERENCES", "RIGHT", "SELECT", "SET", "SOME", "STDDEV_POP", "STDDEV_SAMP", "SUM", "TABLE", "THEN", "TO",
//            "TRAILING", "TRIGGER", "UNION", "UNIQUE", "USING", "VALUES", "VAR_POP", "VAR_SAMP", "WHEN", "WHERE", "WITH");
}
