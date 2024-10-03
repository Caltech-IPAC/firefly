/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.DecimateInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.BaseDbAdapter;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.DuckDbAdapter;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.util.decimate.DecimateKey;

import static edu.caltech.ipac.firefly.server.util.QueryUtil.*;
import static edu.caltech.ipac.table.DataGroup.ROW_IDX;
import static edu.caltech.ipac.table.DataGroup.ROW_NUM;


@SearchProcessorImpl(id = DecimationProcessor.ID)
public class DecimationProcessor extends TableFunctionProcessor {
    public static final String ID = "DecimateTable";
    public static final String DECIMATE_INFO = "decimate";


    protected String getResultSetTablePrefix() {
        return "DECI";
    }

    protected DataGroup fetchData(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {
        if (dbAdapter instanceof DuckDbAdapter) {
            createDecimateTable(treq, dbAdapter);
            return null;
        } else {
            return createDecimateData(treq, dbAdapter);
        }
    }

    void createDecimateTable(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {
        DecimateInfo deciInfo = getDecimateInfo(treq);
        if (deciInfo == null)  return;

        TableServerRequest sreq = QueryUtil.getSearchRequest(treq);
        String[] inclCols = sreq.getInclColumns() == null ? new String[]{} : sreq.getInclColumns().split(",");
        sreq.setInclColumns();      // used as a transport; should fix if we're no longer using old code.
        // separate the expression from the column name
        if ( inclCols.length == 1 ) {
            String[] parts = inclCols[0].split(" as ");
            if (parts.length == 2) {
                deciInfo.setxExp(parts[0]);
                deciInfo.setyExp(parts[0]);
            }
        } else if(inclCols.length == 2) {
            String[] xParts = inclCols[0].split(" as ");
            String[] yParts = inclCols[1].split(" as ");
            if (xParts.length == 2) deciInfo.setxExp(xParts[0]);
            if (yParts.length == 2) deciInfo.setyExp(yParts[0]);
        } else if(inclCols.length > 2) {
            // this is a parsing issue because we use ',' as a separator.  but, comma is often used in function argument.  need to revisit
            throw new DataAccessException("Unsupported expression");
        }

        EmbeddedDbProcessor proc = (EmbeddedDbProcessor) SearchManager.getProcessor(sreq.getRequestId());
        String dataTbl = proc.getResultSetID(sreq);
        if (!dbAdapter.hasTable(dataTbl)) {
            // if table does not exist; load it.
            sreq.setPageSize(1);    // load table into database; ignore results.
            new SearchManager().getDataGroup(sreq);
        }

        DecimateKey deciKey = getDeciKey(deciInfo, dbAdapter, dataTbl);
        DecimateKey deciFunc = deciKey.clone();     // decimate_key function to execute over the dataTbl
        deciFunc.setCols(deciInfo.getxExp(), deciInfo.getyExp());

        int dataPoints = Math.min(deciKey.getxCount(), deciKey.getyCount());
        int deciEnableSize = deciInfo.getDeciEnableSize() > 0 ? deciInfo.getDeciEnableSize() : DECI_ENABLE_SIZE;
        String tblName = getResultSetTable(treq);
        if (dataPoints < deciEnableSize) {
            String sql = """
                CREATE TABLE %s as (
                SELECT %s as "%s", %s as "%s", ROW_NUM as "rowidx", ROW_NUMBER() OVER () AS %s, ROW_NUMBER() OVER () AS %s,
                FROM %s
                """.formatted(tblName, deciInfo.getxExp(), deciKey.getXCol(), deciInfo.getyExp(), deciKey.getYCol(), ROW_NUM, ROW_IDX, dataTbl);
            dbAdapter.execUpdate(sql);
        } else {
            String sql = """
                CREATE TABLE %s as (
                SELECT FIRST(%s) as "%s", FIRST(%s) as "%s", FIRST(ROW_NUM) as "rowidx", count(*) as "weight", %s as "dkey", ROW_NUMBER() OVER () AS %s, ROW_NUMBER() OVER () AS %s,
                FROM %s
                GROUP BY "dkey" )
                """.formatted(tblName, deciInfo.getxExp(), deciKey.getXCol(), deciInfo.getyExp(), deciKey.getYCol(), deciFunc, ROW_NUM, ROW_IDX, dataTbl);
            dbAdapter.execUpdate(sql);

            // add decimation info to the returned table
            var minMax = dbAdapter.execQuery("""
                            SELECT MIN("weight") as "minWeight", MAX("weight") as "maxWeight"
                            FROM %s""".formatted(tblName),null);
            long minW = (long) minMax.getData("minWeight", 0);
            long maxW = (long) minMax.getData("maxWeight", 0);
            DataGroup meta = new DataGroup();
            insertDecimateInfo(meta, deciInfo, deciKey, minW, maxW);
            ((BaseDbAdapter)dbAdapter).metaToDb(meta, tblName);
        }
    }

    /* Old method of doing decimation */
    DataGroup createDecimateData(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {
        DecimateInfo decimateInfo = getDecimateInfo(treq);
        TableServerRequest sreq = QueryUtil.getSearchRequest(treq);
        sreq.setPageSize(Integer.MAX_VALUE);        // we want all of the data.  no paging
        // the "inclCols" in search request should be set by caller and handle column expressions if any
        if (sreq.getInclColumns() == null) {
            String x = decimateInfo.getxColumnName();
            String y = decimateInfo.getyColumnName();
            String [] requestedCols = x.equals(y) ? new String[]{"\""+x+"\""} : new String[]{"\""+x+"\",\""+y+"\""};
            sreq.setInclColumns(requestedCols);
        }

        DataGroupPart sourceData = new SearchManager().getDataGroup(sreq);
        if (sourceData == null) {
            throw new DataAccessException("Unable to get source data");
        } else if (sourceData.getErrorMsg() != null) {
            throw new DataAccessException(sourceData.getErrorMsg());
        }
        DataGroup dg = sourceData.getData();

        if (decimateInfo != null) {
            DataGroup retval = QueryUtil.doDecimation(dg, decimateInfo);
            return retval;
        } else {
            return dg;
        }

    }

    public static DecimateInfo getDecimateInfo(ServerRequest req) {
        return req.containsParam(DECIMATE_INFO) ? DecimateInfo.parse(req.getParam(DECIMATE_INFO)) : null;
    }

    public static void setDecimateInfo(ServerRequest req, DecimateInfo decimateInfo) {
        if (decimateInfo == null) {
            req.removeParam(DECIMATE_INFO);
        } else {
            req.setParam(DECIMATE_INFO, decimateInfo.toString());
        }
    }

    public static DecimateKey getDeciKey(DecimateInfo deciInfo, DbAdapter dbAdapter, String tblName) throws DataAccessException {
        String sql = """
                SELECT MAX(%1$s) as "xMax", MIN(%1$s) as "xMin", COUNT(%1$s) as "xCount", MAX(%2$s) as "yMax", MIN(%2$s) as "yMin", COUNT(%2$s) as "yCount" from %3$s
                """.formatted(deciInfo.getxExp(), deciInfo.getyExp(), tblName);

        DataGroup stats = dbAdapter.execQuery(sql, null);

        double xMin = toDouble(stats.getData("xMin", 0));
        double xMax = toDouble(stats.getData("xMax", 0));
        double yMin = toDouble(stats.getData("yMin", 0));
        double yMax = toDouble(stats.getData("yMax", 0));
        int xCount = toInt(stats.getData("xCount", 0));
        int yCount = toInt(stats.getData("yCount", 0));

        var deciKey = getDecimateKey(deciInfo, xMax, xMin, yMax, yMin);
        deciKey.setxCount(xCount);
        deciKey.setyCount(yCount);
        return deciKey;
    }

    private static double toDouble(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        } else return Double.NaN;
    }

    private static int toInt(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        } else return Integer.MIN_VALUE;
    }
}
