/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.data.TableServerRequest.INCL_COLUMNS;
import static edu.caltech.ipac.firefly.data.TableServerRequest.parseSqlFilter;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
abstract public class BaseDbAdapter implements DbAdapter {
    private static ConcurrentHashMap<String, EmbeddedDbInstance> dbInstances = new ConcurrentHashMap<>();
    private static Logger.LoggerImpl LOGGER = Logger.getLogger();
    private static EmbeddedDbStats dbStats = new EmbeddedDbStats();

    private static final String DD_INSERT_SQL = "insert into %s_dd values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String DD_CREATE_SQL = "create table %s_dd "+
            "(" +
            "  cname    varchar(64000)" +
            ", label    varchar(64000)" +
            ", type     varchar(255)" +
            ", units    varchar(255)" +
            ", null_str varchar(255)" +
            ", format   varchar(255)" +
            ", fmtDisp  varchar(64000)" +
            ", width    int" +
            ", visibility varchar(255)" +
            ", sortable   boolean" +
            ", filterable boolean" +
            ", fixed      boolean" +
            ", desc     varchar(64000)" +
            ", enumVals varchar(64000)" +
            ", ID       varchar(64000)" +
            ", precision varchar(64000)" +
            ", ucd      varchar(64000)" +
            ", utype    varchar(64000)" +
            ", ref      varchar(64000)" +
            ", maxValue varchar(64000)" +
            ", minValue varchar(64000)" +
            ", links    other" +
            ", dataOptions varchar(64000)" +
            ", arraySize varchar(255)" +
            ", cellRenderer varchar(64000)" +
            ", sortByCols varchar(64000)" +
            ")";

    private static final String META_INSERT_SQL = "insert into %s_meta values (?,?,?)";
    private static final String META_CREATE_SQL = "create table %s_meta "+
            "(" +
            "  key      varchar(1024)" +
            ", value    varchar(64000)" +
            ", isKeyword boolean" +
            ")";

    private static final String AUX_DATA_INSERT_SQL = "insert into %s_aux values (?,?,?,?,?,?)";
    private static final String AUX_DATA_CREATE_SQL = "create table %s_aux "+
            "(" +
            "  title     varchar(64000)" +
            ", size      int" +
            ", groups    other" +                        // type 'other' is hsqldb specific.. serializable Java Object
            ", links     other" +
            ", params    other" +
            ", resources other" +
            ")";

    public String createAuxDataSql(String forTable) { return String.format(AUX_DATA_CREATE_SQL, forTable);}
    public String insertAuxDataSql(String forTable) { return String.format(AUX_DATA_INSERT_SQL, forTable);}

    public String createMetaSql(String forTable) { return String.format(META_CREATE_SQL, forTable);}
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
        tblName = isEmpty(tblName) ? MAIN_DB_TBL : tblName;
        List<String> coldefs = new ArrayList<>();
        for(DataType dt : dtTypes) {
            coldefs.add( String.format("\"%s\" %s", dt.getKeyName(), toDbDataType(dt)));       // add quotes to avoid reserved words clashes
        }

        return String.format("create table %s (%s)", tblName, StringUtils.toString(coldefs, ","));
    }

    public String insertDataSql(DataType[] dtTypes, String tblName) {
        tblName = isEmpty(tblName) ? MAIN_DB_TBL : tblName;

        String[] var = new String[dtTypes.length];
        Arrays.fill(var , "?");
        return String.format("insert into %s values(%s)", tblName, StringUtils.toString(var, ","));
    }

    public String getMetaSql(String forTable) {
        return String.format("select * from %s_meta", forTable);
    }

    public String getAuxDataSql(String forTable) {
        return String.format("select * from %s_aux", forTable);
    }

    public String getDDSql(String forTable) {
        return String.format("select * from %s_dd", forTable);
    }

    public String selectPart(TableServerRequest treq) {
        String cols = treq.getParam(INCL_COLUMNS);
        cols = "select " + (isEmpty(cols) ? "*" : cols);
        return cols;
    }

    public String wherePart(TableServerRequest treq) {
        String where = "";
        if (treq.getFilters() != null && treq.getFilters().size() > 0) {
            for (String cond :treq.getFilters()) {
                cond = Arrays.stream(cond.split("(?i)(?= and | or )"))                        // because each filter may contains multiple conditions... apply cleanup logic to each one.
                        .map(eCond -> {
                            if (eCond.matches("(?i).* LIKE .*(\\\\_|\\\\%|\\\\\\\\).*")) {       // search for LIKE with  \_, \%, or \\ in the condition.
                                // for LIKE, to search for '%', '\' or '_' itself, an escape character must also be specified using the ESCAPE clause
                                eCond += " ESCAPE '\\'";
                            }
                            String[] parts = StringUtils.groupMatch("(.+) IN (.+)", eCond, Pattern.CASE_INSENSITIVE);
                            if (parts != null && eCond.contains(NULL_TOKEN)) {
                                eCond = String.format("%s OR %s IS NULL", eCond.replace(NULL_TOKEN, NULL_TOKEN.substring(1)), parts[0]);
                            }
                            return eCond;
                        }).collect(Collectors.joining(""));

                if (where.length() > 0) {
                    where += " and ";
                }
                where += "(" + cond + ")";
            }
            where = "where " + where;
        }

        String[] opSql = parseSqlFilter(treq.getSqlFilter());
        if (!isEmpty(opSql[1])) {
            if (where.length() > 0) {
                where += String.format(" %s (%s)", opSql[0], opSql[1]);
            } else {
                where = String.format("where %s", opSql[1]);
            }
        }

        return where;
    }

    public String orderByPart(TableServerRequest treq) {
        if (treq.getSortInfo() != null) {
            String dir = treq.getSortInfo().getDirection() == SortInfo.Direction.DESC ? " desc" : "";
            String cols = treq.getSortInfo().getSortColumns().stream()
                    .map(c -> c.contains("\"") ? c : "\"" + c + "\"")
                    .collect(Collectors.joining(","));
            String nullsOrder = dir.equals("") ? " NULLS FIRST" : " NULLS LAST";     // this may be HSQL specific.  move it to subclass if when it becomes a problem.
            return  "order by " + cols + dir + nullsOrder;
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

    public String toDbDataType(DataType dataType) {
        if (dataType.isArrayType()) return "other";

        Class type = dataType.getDataType();
        if (type == null || String.class.isAssignableFrom(type)) {
            return "varchar(4000000)";                           // to ensure it can accommodate any length
        } else if (Byte.class.isAssignableFrom(type)) {
            return "tinyint";
        } else if (Short.class.isAssignableFrom(type)) {
            return "smallint";
        } else if (Integer.class.isAssignableFrom(type)) {
            return "int";
        } else if (Long.class.isAssignableFrom(type)) {
            return "bigint";
        } else if (Float.class.isAssignableFrom(type)) {
            return "float";
        } else if (Double.class.isAssignableFrom(type)) {
            return "double";
        } else if (Boolean.class.isAssignableFrom(type)) {
            return "boolean";
        } else if (Date.class.isAssignableFrom(type)) {
            return "date";
        } else if (Character.class.isAssignableFrom(type)) {
            return "char";
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
            getRuntimeStats().totalDbs++;
            getRuntimeStats().peakMemDbs = Math.max(dbInstances.size(), getRuntimeStats().peakMemDbs);
        }
        if (ins != null) {
            try {
                ins.getLock().lock();
                ins.touch();
            } finally {
                ins.getLock().unlock();
            }
        }
        return ins;

    }

    public void close(File dbFile, boolean deleteFile) {
        LOGGER.debug(String.format("DbAdapter -> closing DB, delete(%s): %s", deleteFile, dbFile.getPath()));
        EmbeddedDbInstance db = dbInstances.get(dbFile.getPath());
        if (db != null) {
            try {
                db.getLock().lock();
                if (!deleteFile) {
                    compact(db);
                }
                shutdown(db);
            } finally {
                db.getLock().unlock();
            }
            dbInstances.remove(db.dbFile.getPath());
        }
        if (deleteFile) removeDbFiles(dbFile);
    }

    protected void shutdown(EmbeddedDbInstance db) {}
    protected void removeDbFiles(File dbFile) {}

    public Map<String, EmbeddedDbInstance> getDbInstances() { return dbInstances; }

    protected abstract EmbeddedDbInstance createDbInstance(File dbFile);


    /**
     * returns the column names of the given table.
     * This is hsql specific implementation and may not work with other databases.
     * @param dbInstance
     * @param forTable
     * @return
     */
    public List<String> getColumnNames(DbInstance dbInstance, String forTable, String enclosedBy) {
        String sql = String.format("SELECT column_name FROM INFORMATION_SCHEMA.SYSTEM_COLUMNS where table_name = '%s'", forTable.toUpperCase());
        return JdbcFactory.getSimpleTemplate(dbInstance).query(sql, (rs, i) -> (enclosedBy == null) ? rs.getString(1) : enclosedBy + rs.getString(1) + enclosedBy);
    }


//====================================================================
//  cleanup related functions
//====================================================================
    public void cleanup() {
        cleanup(false);
    }

    public EmbeddedDbStats getRuntimeStats(boolean doUpdate) {
        if (doUpdate) updateDbStats();

        int memRows = dbInstances.values().stream().mapToInt((db) -> db.getDbStats().totalRows()).sum();
        dbStats.maxMemRows = MAX_MEM_ROWS;
        dbStats.memDbs = dbInstances.size();
        dbStats.memRows = memRows;
        dbStats.peakMemRows = Math.max(memRows, dbStats.peakMemRows);
        return dbStats;
    }

    public EmbeddedDbStats getRuntimeStats() {
        return getRuntimeStats(false);
    }

    public void cleanup(boolean force) {
        cleanup(force, false);
    }

    public void updateDbStats() {
        LOGGER.trace("DbAdapter -> updateDbStats");
        for(EmbeddedDbInstance db : dbInstances.values()) {
            db.setDbStats(getDbStats(db));
        }
    }

    public void cleanup(boolean force, boolean deleteFile) {

        try {
            LOGGER.trace("DbAdapter -> cleanup");

            // remove expired search results
            List<EmbeddedDbInstance> toBeRemove = dbInstances.values().stream()
                    .filter((db) -> db.hasExpired() || force).collect(Collectors.toList());
            if (toBeRemove.size() > 0) {
                LOGGER.info(String.format("There are currently %d databases open.  Of which, %d will be closed.", dbInstances.size(), toBeRemove.size()));
                toBeRemove.forEach((db) -> close(db.getDbFile(), deleteFile));
            }

            // compact idled databases
            dbInstances.values().stream()
                    .filter((db) -> db.mayCompact())
                    .forEach((db) -> compact(db));

            updateDbStats();

            // remove search results based on LRU(least recently used) when count is greater than the high-water mark
            long totalRows = dbInstances.values().stream().mapToInt((db) -> db.getDbStats().totalRows()).sum();
            if (totalRows > MAX_MEM_ROWS) {
                long cRows = 0;
                List<EmbeddedDbInstance> active = new ArrayList<>(dbInstances.values());
                Collections.sort(active, (db1, db2) -> Long.compare(db2.getLastAccessed(), db1.getLastAccessed()));  // sorted descending..
                for(EmbeddedDbInstance db : active) {
                    cRows += db.getDbStats().totalRows();
                    if (cRows > MAX_MEM_ROWS) {
                        close(db.getDbFile(), deleteFile);
                    }
                }
            }
            dbStats.lastCleanup = System.currentTimeMillis();
        }catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public static void compact(EmbeddedDbInstance db) {
        LOGGER.debug(String.format("DbAdapter -> compacting DB: %s", db.getDbFile().getPath()));
        List<String> tables = getTempTables(db);
        if (tables.size() > 0) {
            // do compact.. remove all temporary tables
            for (String tblName : tables) {
                String dataSql = String.format("drop table %s if exists", tblName);
                String metaSql = String.format("drop table %s if exists", tblName + "_meta");
                String ddSql = String.format("drop table %s if exists", tblName + "_dd");
                String idxSql = String.format("drop index %s_idx IF EXISTS", tblName);
                Arrays.asList(dataSql, metaSql, ddSql, idxSql).stream().forEach(
                        (sql) -> JdbcFactory.getSimpleTemplate(db).update(sql));
            }
        }
        db.setCompact(true);
    }

    private static DbStats getDbStats(EmbeddedDbInstance db) {
        try {
            final Ref<Integer> rows = new Ref<>();
            final Ref<Integer> cols = new Ref<>();
            final Ref<Integer> tables = new Ref<>();
            final Ref<Integer> totalRows = new Ref<>();

            SimpleJdbcTemplate jdbc = JdbcFactory.getSimpleTemplate(db);
            jdbc.queryForObject("SELECT count(*), sum(cardinality) from INFORMATION_SCHEMA.SYSTEM_TABLESTATS where table_schema = 'PUBLIC' and not REGEXP_MATCHES(table_name,'.*_DD$|.*_META$|.*_AUX$')", (rs, i) -> {
                tables.set(rs.getInt(1));
                totalRows.set(rs.getInt(2));
                return null;
            });
            jdbc.queryForObject("SELECT count(column_name), cardinality from INFORMATION_SCHEMA.SYSTEM_COLUMNS c, INFORMATION_SCHEMA.SYSTEM_TABLESTATS t" +
                    " where c.table_name = t.table_name" +
                    " and t.table_name = 'DATA'" +
                    " group by cardinality", (rs,i) -> {
                cols.set(rs.getInt(1));
                rows.set(rs.getInt(2));
                return null;
            });
            return  new DbStats(tables.get(), cols.get(), rows.get(), totalRows.get());
        } catch (Exception e) {
            return new DbStats();
        }
    }

    private static List<String> getTempTables(EmbeddedDbInstance db) {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.SYSTEM_TABLESTATS \n" +
                "where table_schema = 'PUBLIC' \n" +
                "and TABLE_NAME NOT IN ('DATA', 'DATA_DD', 'DATA_META', 'DATA_AUX')";

        return JdbcFactory.getSimpleTemplate(db).query(sql, (rs, i) -> rs.getString(1));
    }

}
