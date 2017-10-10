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
    private static long MAX_IDLE_TIME = 1000 * 60 * 5;      // cleanup every 5 minutes.
    private static Map<String, EmbeddedDbInstance> dbInstances = new HashMap<>();
    private static ScheduledFuture cleanupFuture = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> cleanup(), MAX_IDLE_TIME, MAX_IDLE_TIME, TimeUnit.MILLISECONDS);
    private static Logger.LoggerImpl LOGGER = Logger.getLogger();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
                                 public void run() {
                                     LOGGER.info("Closing any open database before shutdown.");
                                     cleanup(true);
                                 }
                             }
                        );
    }
    private static final String DD_INSERT_SQL = "insert into %s_dd values (?,?,?,?,?,?,?,?,?,?)";
    private static final String DD_CREATE_SQL = "create table %s_dd "+
            "(" +
            "  cname    varchar(1023)" +
            ", label    varchar(1023)" +
            ", type     varchar(1023)" +
            ", units    varchar(1023)" +
            ", format   varchar(1023)" +
            ", width    int" +
            ", visibility varchar(1023)" +
            ", sortable boolean" +
            ", filterable boolean" +
            ", desc     varchar(1023)" +
            ")";

    private static final String META_INSERT_SQL = "insert into %s_meta values (?,?)";
    private static final String META_CREATE_SQL = "create table %s_meta "+
            "(" +
            "  key      varchar(1023)" +
            ", value    varchar(2023)" +
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
            coldefs.add( String.format("\"%s\" %s", dt.getKeyName().toUpperCase(),getDataType(dt.getDataType())));
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

    public String fromPart(TableServerRequest treq) {
        String from = treq.getParam(TableServerRequest.SQL_FROM);
        from = from == null ? EmbeddedDbUtil.getResultSetID(treq) : from;
        from = "from " + (StringUtils.isEmpty(from) ? "data" : from);
        return from;
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
            return "varchar(1023)";
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
            return "varchar(1023)";
        }
    }

    public DbInstance getDbInstance(File dbFile) {
        EmbeddedDbInstance ins = dbInstances.get(dbFile.getPath());
        if (ins == null) {
            ins = createDbInstance(dbFile);
            dbInstances.put(dbFile.getPath(), ins);
        }
        ins.touch();
        return ins;

    }

    public void close(File dbFile) {}          // subclass should override this to properly closes the database and cleanup resources.

    protected abstract EmbeddedDbInstance createDbInstance(File dbFile);

    public static Map<String, EmbeddedDbInstance> getDbInstances() { return dbInstances; }

    static void cleanup() {
        cleanup(false);
    }

    static void cleanup(boolean force) {
        List<EmbeddedDbInstance> toBeRemove = dbInstances.values().stream()
                                                    .filter((db) -> db.hasExpired() || force).collect(Collectors.toList());

        LOGGER.info(String.format("There are currently %d databases open.  Of which, %d will be closed.", dbInstances.size(), toBeRemove.size()));
        if (toBeRemove.size() > 0) {
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
