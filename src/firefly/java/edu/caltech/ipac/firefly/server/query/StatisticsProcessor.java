package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.server.db.DuckDbAdapter;
import edu.caltech.ipac.table.DataType.Visibility;
import edu.caltech.ipac.table.io.IpacTableException;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static edu.caltech.ipac.firefly.server.util.QueryUtil.SEARCH_REQUEST;

/**
 * Created by zhang on 10/14/15.
 * This class calculates the statistics of a IpacTable Data.
 */

@SearchProcessorImpl(id = "StatisticsProcessor")
public class StatisticsProcessor extends TableFunctionProcessor {
    private static DataType[] columns = new DataType[]{
            new DataType("columnName", String.class),
            new DataType("description", String.class),
            new DataType("unit", String.class),
            new DataType("min", Double.class),
            new DataType("max", Double.class),
            new DataType("numPoints", Long.class),
    };

    protected String getResultSetTablePrefix() {
        return "STATS";
    }

    protected DataGroup fetchData(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {

        TableServerRequest sreq = QueryUtil.getSearchRequest(treq);
        EmbeddedDbProcessor proc = getSearchProcessor(sreq);
        String origDataTblName = proc.getResultSetID(sreq);
        // check to see if a resultset table exists... if not, use original data table.
        if (!dbAdapter.hasTable(origDataTblName)) {
            origDataTblName = dbAdapter.getDataTable();
        }

        // get all column info from DATA table
        DataGroup dd = dbAdapter.getHeaders(origDataTblName);
        var cols = dd.getDataDefinitions();

        //generate one sql for all the columns.  each as cname_min, cname_max, cname_count
        DataGroup stats = new DataGroup("stats", columns);
        List<String> sqlCols = new ArrayList<>();
        for (DataType col : cols) {
            if (col.isNumeric() && col.getVisibility() != Visibility.hidden) {
                String cname = col.getKeyName();
                String desc = col.getDesc();
                String units = col.getUnits();
                DataObject row = new DataObject(stats);
                row.setDataElement(columns[0], cname);
                row.setDataElement(columns[1], desc);
                row.setDataElement(columns[2], units);
                sqlCols.add("min(\"%1$s\") as \"%1$s_min\"".formatted(cname));
                sqlCols.add("max(\"%1$s\") as \"%1$s_max\"".formatted(cname));
                sqlCols.add("count(\"%1$s\") as \"%1$s_count\"".formatted(cname));
                stats.add(row);
            }

        }
        if (!sqlCols.isEmpty()) {
            DataObject data = dbAdapter.execQuery("select %s from %s".formatted(StringUtils.toString(sqlCols), origDataTblName), null).get(0);
            for (int i = 0; i < stats.size(); i++) {
                DataObject col = stats.get(i);
                String cname = col.getStringData("columnName");
                col.setDataElement(columns[3], getDouble(data.getDataElement(cname + "_min")));
                col.setDataElement(columns[4], getDouble(data.getDataElement(cname + "_max")));
                col.setDataElement(columns[5], data.getDataElement(cname + "_count"));
            }
        }
        return stats;
    }

    private Double getDouble(Object v) {
        if (v instanceof Double) {
            return (Double) v;
        } else {
            return v == null ? null : Double.valueOf(v.toString());
        }
    }
}



