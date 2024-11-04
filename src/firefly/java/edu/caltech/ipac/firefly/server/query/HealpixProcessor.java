/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;

import java.util.Arrays;

import static edu.caltech.ipac.firefly.server.query.HealpixProcessor.*;
import static edu.caltech.ipac.util.StringUtils.split;

@SearchProcessorImpl(id = HealpixProcessor.ID, params = {
    @ParamDoc(name = SEARCH_REQUEST,        desc = "string, JSON string of the request that maps to the data table"),
    @ParamDoc(name = MODE, desc = "map|points, return the pixel map at the given order, or return the ROW_ID of the points at the given ORDER and PIXELS.  defaults to map"),
    @ParamDoc(name = ORDER, desc = "int, pixel map for the given the resolution (or order).  defaults to BASE_ORDER"),
    @ParamDoc(name = PIXELS, desc = "int[], one or more pixel separated by comma"),
    @ParamDoc(name = RA, desc = "string, ra column name"),
    @ParamDoc(name = DEC, desc = "int[], dec column name"),
})
/**
 * Handles indexing of data and generates the pixel map for BASE_ORDER.
 * See healpy-java.sql for implementation of deg2pix() function.
 * The added index column is called healpix_idx
 * The pixel map contains two columns: pixel and count.
 * Supports two modes: map and points.
 *   map: Generates and returns the pixel map.
 *   points: Returns the ROW_NUM of all points within specified pixel(s).
 * Both modes allow for down sampling of the Healpix resolution (order).
 */
public class HealpixProcessor extends TableFunctionProcessor {
    public static final String ID = "HealpixIndex";
    public static final int BASE_ORDER = AppProperties.getIntProperty("healpix_base_order", 12);        // the resolution (or order) at which the table is indexed
    public static final String SEARCH_REQUEST = QueryUtil.SEARCH_REQUEST;
    public static final String MODE = "mode";
    public static final String ORDER = "order";
    public static final String PIXELS = "pixels";
    public static final String RA = "ra";
    public static final String DEC = "dec";
    public static final String MAP = "map";
    public static final String POINTS = "points";

    protected String getResultSetTablePrefix() { return "HEALPIX"; }

    protected DataGroup fetchData(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {
        return null;        // does not get called because getResultSet is overridden.
    }

    @Override
    protected DataGroupPart getResultSet(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {


        TableServerRequest sreq = QueryUtil.getSearchRequest(treq);
        String mode = treq.getParam(MODE, MAP);
        int order = treq.getIntParam(ORDER, BASE_ORDER);
        String ra = treq.getParam(RA);
        String dec = treq.getParam(DEC);
        String[] pixels = split(treq.getParam(PIXELS), ",");
        int orderDelta = BASE_ORDER - order;

        StopWatch.getInstance().start("HealpixProcessor: %s".formatted(mode));

        EmbeddedDbProcessor proc = (EmbeddedDbProcessor) SearchManager.getProcessor(sreq.getRequestId());
        String dataTable = proc.getResultSetID(sreq);
        String healpixTable = getResultSetTablePrefix() + "_" + dataTable;

        // create healpix table if it does not exist
        ensureHealpixExists(dbAdapter, sreq, ra, dec, dataTable, healpixTable);

        DataGroup results;
        if (mode.equals(MAP)) {
            String wherePart = "";
            if (pixels != null && pixels.length > 0) {
                String lhs = orderDelta > 0 ? "TRUNC(pixel/4^%s)".formatted(orderDelta) : "pixel";
                String rhs =  pixels.length == 1 ? " = %s".formatted(pixels[0]) : " IN (%s)".formatted(String.join(",", pixels));
                wherePart = "WHERE %s %s".formatted(lhs, rhs);
            }
            String sql = "SELECT * from %s %s".formatted(healpixTable, wherePart);
            if (orderDelta > 0) {
                sql = "SELECT TRUNC(pixel/4^%s)::LONG AS 'pixel', SUM(count)::LONG AS 'count' from %s %s GROUP BY 1".formatted(orderDelta, healpixTable, wherePart);
            }
            results = dbAdapter.execQuery(sql, null);
        } else if (mode.equals(POINTS)) {
            if (pixels == null || pixels.length == 0) throw new DataAccessException("POINTS mode: pixels parameter is missing");
            String lhs = orderDelta > 0 ? "TRUNC(healpix_idx/4^%s)".formatted(orderDelta) : "healpix_idx";
            String rhs =  pixels.length == 1 ? " = %s".formatted(pixels[0]) : " IN (%s)".formatted(String.join(",", pixels));
            String sql = "SELECT %s, %s, ROW_NUM from %s WHERE %s %s".formatted(ra, dec, dataTable, lhs, rhs);   // need ra,dec(?)
            results = dbAdapter.execQuery(sql, null);
        } else {
            throw new DataAccessException("Unsupported mode: " + mode);
        }
        StopWatch.getInstance().printLog("HealpixProcessor: %s".formatted(mode));

        return new DataGroupPart(results,0, results.size());
    }

    private void ensureHealpixExists( DbAdapter dbAdapter, TableServerRequest sreq, String ra, String dec, String dataTable, String healpixTable) throws DataAccessException {

        // if search data table does not exist; load it.
        if (!dbAdapter.hasTable(dataTable)) {
            sreq.setPageSize(1);    // load table into database; ignore results.
            new SearchManager().getDataGroup(sreq);
        }
        // if healpix map doesn't exist, index the data table, then create the map
        if (!dbAdapter.hasTable(healpixTable)) {

            // create healpix index at BASE_ORDER
            StopWatch.getInstance().start("HealpixProcessor: create index");
            dbAdapter.execUpdate("ALTER TABLE %s ADD COLUMN healpix_idx LONG".formatted(dataTable));
            dbAdapter.execUpdate("UPDATE %s SET healpix_idx = deg2pix(%s, %s, %s)".formatted(dataTable, BASE_ORDER, ra, dec));
            StopWatch.getInstance().printLog("HealpixProcessor: create index");

            // create healpix map at BASE_ORDER
            StopWatch.getInstance().start("HealpixProcessor: create pixel map");
            dbAdapter.execUpdate("create table %s as (select healpix_idx as 'pixel', count() as 'count' from %s group by 1)".formatted(healpixTable, dataTable));
            StopWatch.getInstance().printLog("HealpixProcessor: create pixel map");

        }
    }

}
