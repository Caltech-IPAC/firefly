/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static edu.caltech.ipac.firefly.data.TableServerRequest.INCL_COLUMNS;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
abstract public class BaseDbAdapter implements DbAdapter {

    private static final String DD_INSERT_SQL = "insert into dd values (?,?,?,?,?,?,?,?,?,?)";
    private static final String DD_CREATE_SQL = "create table if not exists dd "+
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

    private static final String META_INSERT_SQL = "insert into meta values (?,?)";
    private static final String META_CREATE_SQL = "create table if not exists meta "+
            "(" +
            "  key      varchar(1023)" +
            ", value    varchar(2023)" +
            ")";


    public String createMetaSql(DataType[] dataDefinitions) {
        return META_CREATE_SQL;
    }

    public String insertMetaSql(DataType[] dataDefinitions) {
        return META_INSERT_SQL;
    }

    public String createDDSql(DataType[] dataDefinitions) {
        return DD_CREATE_SQL;
    }

    public String insertDDSql(DataType[] dataDefinitions) {
        return DD_INSERT_SQL;
    }

    public String createDataSql(DataType[] dtTypes, String tblName) {
        tblName = StringUtils.isEmpty(tblName) ? "data" : tblName;
        List<String> coldefs = new ArrayList<>();
        for(DataType dt : dtTypes) {
            coldefs.add( dt.getKeyName() + " " + getDataType(dt.getDataType()));
        }

        return String.format("create table if not exists %s (%s)", tblName, StringUtils.toString(coldefs, ","));
    }

    public String insertDataSql(DataType[] dtTypes, String tblName) {
        tblName = StringUtils.isEmpty(tblName) ? "data" : tblName;

        String[] var = new String[dtTypes.length];
        Arrays.fill(var , "?");
        return String.format("insert into %s values(%s)", tblName, StringUtils.toString(var, ","));
    }

    public String getMetaSql() {
        return "select * from meta";
    }

    public String getDDSql() {
        return "select * from dd";
    }

    public String selectPart(TableServerRequest treq) {
        String cols = treq.getParam(INCL_COLUMNS);
        cols = "select " + (StringUtils.isEmpty(cols) ? "*" : cols);
        return cols;
    }

    public String fromPart(TableServerRequest treq) {
        String from = TableDbUtil.getDatasetID(treq);
        from = StringUtils.isEmpty(from) ? treq.getParam(TableServerRequest.SQL_FROM) : from;
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
        return String.format("CREATE TABLE IF NOT EXISTS %s AS %s", tblName, selectSql);
    }

    public String translateSql(String sql) {
        return sql;
    }

    public boolean useTxnDuringLoad() {
        return false;
    }

    public File getStorageFile(File dbFile) {
        return dbFile;
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
}
