/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.data.TableServerRequest.INCL_COLUMNS;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
abstract public class BaseDbAdapter implements DbAdapter {
    private static long MAX_IDLE_TIME = 1000 * 60 * 15;      // will be purged up if idle more than 5 minutes.
    private static Map<String, EmbeddedDbInstance> dbInstances = new HashMap<>();
    private static Logger.LoggerImpl LOGGER = Logger.getLogger();

    private static final String DD_INSERT_SQL = "insert into %s_dd values (?,?,?,?,?,?,?,?,?,?)";
    private static final String DD_CREATE_SQL = "create table %s_dd "+
            "(" +
            "  cname    varchar(1024)" +
            ", label    varchar(1024)" +
            ", type     varchar(1024)" +
            ", units    varchar(1024)" +
            ", format   varchar(1024)" +
            ", width    int" +
            ", visibility varchar(1024)" +
            ", sortable boolean" +
            ", filterable boolean" +
            ", desc     varchar(64000)" +
            ")";

    private static final String META_INSERT_SQL = "insert into %s_meta values (?,?)";
    private static final String META_CREATE_SQL = "create table %s_meta "+
            "(" +
            "  key      varchar(1024)" +
            ", value    varchar(64000)" +
            ")";


    public String createMetaSql(String forTable) {
        return String.format(META_CREATE_SQL, forTable);
    }

    public String insertMetaSql(String forTable) {
        return String.format(META_INSERT_SQL, forTable);
    }

    public String createDDSql(String forTable) {
        return String.format(DD_CREATE_SQL, forTable);
    }

    public String insertDDSql(String forTable) {
        return String.format(DD_INSERT_SQL, forTable);
    }

    public String createDataSql(DataType[] dtTypes, String tblName) {
        tblName = StringUtils.isEmpty(tblName) ? "data" : tblName;
        List<String> coldefs = new ArrayList<>();
        for(DataType dt : dtTypes) {
            coldefs.add( String.format("\"%s\" %s", dt.getKeyName(), getDataType(dt.getDataType())));       // add quotes to avoid reserved words clashes
        }

        return String.format("create table %s (%s)", tblName, StringUtils.toString(coldefs, ","));
    }

    public String insertDataSql(DataType[] dtTypes, String tblName) {
        tblName = StringUtils.isEmpty(tblName) ? "data" : tblName;

        String[] var = new String[dtTypes.length];
        Arrays.fill(var , "?");
        return String.format("insert into %s values(%s)", tblName, StringUtils.toString(var, ","));
    }

    public String getMetaSql(String forTable) {
        return String.format("select * from %s_meta", forTable);
    }

    public String getDDSql(String forTable) {
        return String.format("select * from %s_dd", forTable);
    }

    public String selectPart(TableServerRequest treq) {
        String cols = treq.getParam(INCL_COLUMNS);
        cols = "select " + (StringUtils.isEmpty(cols) ? "*" : cols);
        return cols;
    }

    public String wherePart(TableServerRequest treq) {
        String where = "";
        if (treq.getFilters() != null && treq.getFilters().size() > 0) {
            where = "";
            for (String cond :treq.getFilters()) {
                if (where.length() > 0) {
                    where += " and ";
                }
                where += "(" + cond + ")";
            }
            where = "where " + where;
        }

        return where;
    }

    public String orderByPart(TableServerRequest treq) {
        if (treq.getSortInfo() != null) {
            String dir = treq.getSortInfo().getDirection() == SortInfo.Direction.DESC ? " desc" : "";
            return  "order by " + treq.getSortInfo().getPrimarySortColumn() + dir;
        }
        return "";
    }

    public String pagingPart(TableServerRequest treq) {
        if (treq.getPageSize() < 0 || treq.getPageSize() == Integer.MAX_VALUE) return "";
        String page = String.format("limit %d offset %d", treq.getPageSize(), treq.getStartIndex());
        return page;
    }

    public String createTableFromSelect(String tblName, String selectSql) {
        return String.format("CREATE TABLE %s AS %s", tblName, selectSql);
    }

    public String translateSql(String sql) {
        return sql;
    }

    public boolean useTxnDuringLoad() {
        return false;
    }

    public String getDataType(Class type) {
        if (String.class.isAssignableFrom(type)) {
            return "varchar(64000)";
        } else if (Integer.class.isAssignableFrom(type)) {
            return "int";
        } else if (Long.class.isAssignableFrom(type)) {
            return "bigint";
        } else if (Float.class.isAssignableFrom(type)) {
            return "real";
        } else if (Double.class.isAssignableFrom(type)) {
            return "double";
        } else if (Date.class.isAssignableFrom(type)) {
            return "date";
        } else {
            return "varchar(64000)";
        }
    }

    public DbInstance getDbInstance(File dbFile) {
        return getDbInstance(dbFile, true);
    }

    public DbInstance getDbInstance(File dbFile, boolean create) {
        EmbeddedDbInstance ins = dbInstances.get(dbFile.getPath());
        if (ins == null && create) {
            ins = createDbInstance(dbFile);
            dbInstances.put(dbFile.getPath(), ins);
        }
        if (ins != null ) ins.touch();
        return ins;

    }

    public void close(File dbFile) {}          // subclass should override this to properly closes the database and cleanup resources.

    protected abstract EmbeddedDbInstance createDbInstance(File dbFile);

    public static Map<String, EmbeddedDbInstance> getDbInstances() { return dbInstances; }

    public static void cleanup() {
        cleanup(false);
    }

    public static void cleanup(boolean force) {
        List<EmbeddedDbInstance> toBeRemove = dbInstances.values().stream()
                                                    .filter((db) -> db.hasExpired() || force).collect(Collectors.toList());
        if (toBeRemove.size() > 0) {
            LOGGER.info(String.format("There are currently %d databases open.  Of which, %d will be closed.", dbInstances.size(), toBeRemove.size()));
            toBeRemove.forEach((db) -> {
                DbAdapter.getAdapter(db.name).close(db.dbFile);
                dbInstances.remove(db.dbFile.getPath());
            });
        }
    }

    public static class EmbeddedDbInstance extends DbInstance {
        long lastAccessed;
        File dbFile;

        public EmbeddedDbInstance(String type, File dbFile, String dbUrl, String driver) {
            super(false, null, dbUrl, null, null, driver, type);
            lastAccessed = System.currentTimeMillis();
            this.dbFile = dbFile;
        }

        @Override
        public boolean equals(Object obj) {
            return StringUtils.areEqual(this.dbUrl,((EmbeddedDbInstance)obj).dbUrl);
        }

        @Override
        public int hashCode() {
            return this.dbUrl.hashCode();
        }

        public long getLastAccessed() {
            return lastAccessed;
        }

        public boolean hasExpired() {
            return System.currentTimeMillis() - lastAccessed > MAX_IDLE_TIME;
        }

        public File getDbFile() {
            return dbFile;
        }

        public void touch() {
            lastAccessed = System.currentTimeMillis();
        }
    }
}
