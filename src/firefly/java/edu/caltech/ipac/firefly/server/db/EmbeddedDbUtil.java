/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.db.spring.mapper.DataGroupUtil;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.firefly.server.util.ipactable.TableDef;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_PATH;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_TYPE;
import static edu.caltech.ipac.firefly.util.DataSetParser.*;
import static edu.caltech.ipac.util.DataGroup.ROW_IDX;
import static edu.caltech.ipac.util.DataGroup.ROW_NUM;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class EmbeddedDbUtil {
    private static final Logger.LoggerImpl logger = Logger.getLogger();

    /**
     * setup a database
     * @param dbFile  the file to save the database to.
     * @param dbAdapter DbAdapter to use.. ie sqlite, h2, etc.
     */
    public static void createDbFile(File dbFile, DbAdapter dbAdapter) throws IOException {
        dbAdapter.close(dbFile);                    // in case database exists in memory.
        File[] toRemove = dbFile.getParentFile().listFiles((dir, name) -> name.startsWith(dbFile.getName()));
        if (toRemove != null && toRemove.length > 0) {
            Arrays.stream(toRemove).forEach(f -> f.delete());
        }
        dbFile.createNewFile();                     // creates the file
        createCustomFunctions(dbFile, dbAdapter);   // add custom functions
    }

    /**
     * ingest the given datagroup into a database file using the provided DbAdpater.
     * @param dbFile  the file to save the database to.
     * @param dg the datagroup containing the data
     * @param dbAdapter DbAdapter to use.. ie sqlite, h2, etc.
     * @param forTable the name of the table to ingest to
     * @return  a FileInfo with sizeInBytes representing to the number of rows.
     */
    public static FileInfo ingestDataGroup(File dbFile, DataGroup dg, DbAdapter dbAdapter, String forTable) {

        // remove ROW_IDX or ROW_NUM if exists
        dg.removeDataDefinition(DataGroup.ROW_IDX);
        dg.removeDataDefinition(DataGroup.ROW_NUM);

        createDataTbl(dbFile, dg, dbAdapter, forTable);
        createDDTbl(dbFile, dg, dbAdapter, forTable);
        createMetaTbl(dbFile, dg, dbAdapter, forTable);
        FileInfo finfo = new FileInfo(dbFile);
        return finfo;
    }

    public static void setDbMetaInfo(TableServerRequest treq, DbAdapter dbAdapter, File dbFile) {
        treq.setMeta(TBL_FILE_PATH, ServerContext.replaceWithPrefix(dbFile));
        treq.setMeta(TBL_FILE_TYPE, dbAdapter.getName());
    }


    @NotNull
    public static String getUniqueID(TableServerRequest treq) {
        return SearchProcessor.getUniqueIDDef(treq);
    }

    public static int createDataTbl(File dbFile, DataGroup dg, DbAdapter dbAdapter, String tblName) {

        DataType[] colsAry = makeDbCols(dg);
        int totalRows = dg.size();

        String createDataSql = dbAdapter.createDataSql(colsAry, tblName);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).update(createDataSql);
        if (totalRows > 0) {
            String[] var = new String[colsAry.length];
            Arrays.fill(var , "?");

            String insertDataSql = dbAdapter.insertDataSql(colsAry, tblName);

            JdbcTemplate jdbc = JdbcFactory.getTemplate(dbAdapter.getDbInstance(dbFile));
            if (dbAdapter.useTxnDuringLoad()) {
                TransactionTemplate txnJdbc = JdbcFactory.getTransactionTemplate(jdbc.getDataSource());
                txnJdbc.execute(new TransactionCallbackWithoutResult() {
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        doTableLoad(jdbc, insertDataSql, dg);
                    }
                });
            } else {
                doTableLoad(jdbc, insertDataSql, dg);
            }
        }

        return totalRows;
    }

    /**
     * Similar to execQuery, except this method creates the SQL statement from the given request object.
     * It need to take filter, sort, and paging into consideration.
     * @param treq      request parameters used for select, where, order by, and limit
     * @param dbFile    database file
     * @param forTable  table to run the query on.  the from part of the statement
     * @return
     */
    public static DataGroupPart execRequestQuery(TableServerRequest treq, File dbFile, String forTable) {
        DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
        String selectPart = dbAdapter.selectPart(treq);
        String wherePart = dbAdapter.wherePart(treq);
        String orderByPart = dbAdapter.orderByPart(treq);
        String pagingPart = dbAdapter.pagingPart(treq);

        String sql = String.format("%s from %s %s %s %s", selectPart, forTable, wherePart, orderByPart, pagingPart);
        DataGroup data = EmbeddedDbUtil.execQuery(dbAdapter, dbFile, sql, forTable);

        int rowCnt = data.size();
        if (!StringUtils.isEmpty(pagingPart)) {
            // fetch total row count for the query.. datagroup may contain partial results(paging)
            String cntSql = String.format("select count(*) from %s %s", forTable, wherePart);
            rowCnt = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).queryForInt(cntSql);
        }

        DataGroupPart page = EmbeddedDbUtil.toDataGroupPart(data, treq);
        page.setRowCount(rowCnt);
        page.getTableDef().setAttribute(TableServerRequest.RESULTSET_ID, forTable);
        if (!StringUtils.isEmpty(treq.getTblTitle())) {
            page.getData().setTitle(treq.getTblTitle());  // set the datagroup's title to the request title.
        }
        return page;
    }


    /**
     * Executes the give sql and returns the results as a DataGroup.  If refTable is provided, it will query the
     * ?_dd and ?_meta tables of this refTable and add the information into the returned DataGroup.
     * @param dbAdapter     adapter to use
     * @param dbFile        database file
     * @param sql           complete SQL statement
     * @param refTable      use meta information from this table
     * @return
     */
    public static DataGroup execQuery(DbAdapter dbAdapter, File dbFile, String sql, String refTable) {
        DbInstance dbInstance = dbAdapter.getDbInstance(dbFile);
        sql = dbAdapter.translateSql(sql);

        DataGroup dg = (DataGroup)JdbcFactory.getTemplate(dbInstance).query(sql, rs -> {
            return DataGroupUtil.processResults(rs);
        });

        SimpleJdbcTemplate jdbc = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile));

        if (refTable != null) {
            // insert DD info into the results
            try {
                String ddSql = dbAdapter.getDDSql(refTable);
                jdbc.query(ddSql, (rs, i) -> EmbeddedDbUtil.insertDD(dg, rs));
            } catch (Exception e) {
                // ignore.. may not have DD table
            }

            // insert table meta info into the results
            try {
                String metaSql = dbAdapter.getMetaSql(refTable);
                jdbc.query(metaSql, (rs, i) -> EmbeddedDbUtil.insertMeta(dg, rs));
            } catch (Exception e) {
                // ignore.. may not have meta table
            }
        }

        return dg;
    }


//====================================================================
//  common util functions
//====================================================================

    /**
     * returns a DataGroup containing of the given cols from the selRows
     * @param searchRequest     search request to query
     * @param selRows           a list of selected rows
     * @param cols              columns to return.  Will return all columns if not given.
     * @return
     */
    public static DataGroup getSelectedData(ServerRequest searchRequest, List<Integer> selRows, String... cols) {
        TableServerRequest treq = (TableServerRequest)searchRequest;
        EmbeddedDbProcessor proc = (EmbeddedDbProcessor) new SearchManager().getProcessor(searchRequest.getRequestId());
        String selCols = cols == null || cols.length == 0 ? "*" : Arrays.stream(cols).map( c -> (c.contains("\"") ? c : "\"" + c + "\"")).collect(Collectors.joining(","));
        DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
        File dbFile = proc.getDbFile(treq);
        String tblName = proc.getResultSetID(treq);
        String inRows = selRows != null && selRows.size() > 0 ? StringUtils.toString(selRows) : "-1";

        try {
            DbInstance dbInstance = dbAdapter.getDbInstance(dbFile);
            JdbcFactory.getSimpleTemplate(dbInstance).queryForInt(String.format("select count(*) from %s", tblName));
        } catch (Exception e) {
            try {
                // data does not exists.. recreate it
                new SearchManager().getDataGroup(treq);
            } catch (DataAccessException e1) {
                logger.error(e1);
            }
        }

        String sql = String.format("select %s from %s where %s in (%s)", selCols, tblName, DataGroup.ROW_NUM, inRows);
        return EmbeddedDbUtil.execQuery(dbAdapter, dbFile, sql ,tblName);
    }

    /**
     * Same as getSelectedData but in a MappedData structure.
     * @param searchRequest     search request to query
     * @param selRows           a list of selected rows
     * @param cols              columns to return.  Will return all columns if not given.
     * @return
     */
    public static IpacTableParser.MappedData getSelectedMappedData(ServerRequest searchRequest, List<Integer> selRows, String... cols) {
        if (cols != null && cols.length > 0) {
            ArrayList<String> colsAry = new ArrayList<>(Arrays.asList(cols));
            if (!colsAry.contains(DataGroup.ROW_NUM)) {
                // add ROW_NUM into the returned results if not asked
                colsAry.add(DataGroup.ROW_NUM);
                cols = colsAry.toArray(new String[colsAry.size()]);
            }
        }
        IpacTableParser.MappedData results = new IpacTableParser.MappedData();
        DataGroup data = getSelectedData(searchRequest, selRows, cols);
        for (DataObject row : data) {
            int idx = row.getIntData(DataGroup.ROW_NUM);
            for (DataType dt : data.getDataDefinitions()) {
                if (!dt.getKeyName().equals(DataGroup.ROW_NUM)) {
                    results.put(idx, dt.getKeyName(), row.getDataElement(dt));
                }
            }
        }
        return results;
    }

    public static DataGroupPart getSelectedDataAsDGPart(ServerRequest searchRequest, List<Integer> selRows, String... cols) {
        return toDataGroupPart(getSelectedData(searchRequest, selRows, cols), (TableServerRequest) searchRequest);
    }

    public static DataGroupPart toDataGroupPart(DataGroup data, TableServerRequest treq) {
        TableDef tm = new TableDef();
        tm.setStatus(DataGroupPart.State.COMPLETED);
        tm.setRowCount(data.size());
        return new DataGroupPart(tm, data, treq.getStartIndex(), data.size());
    }


    public static void createMetaTbl(File dbFile, DataGroup dg, DbAdapter dbAdapter, String forTable) {
        if (dg.getAttributeKeys().size() == 0) return;

        String createMetaSql = dbAdapter.createMetaSql(forTable);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).update(createMetaSql);

        List<Object[]> data = new ArrayList<>();
        Map<String, DataGroup.Attribute> meta = dg.getAttributes();
        for (String key : meta.keySet()) {
            String val = meta.get(key).getValue();
            data.add(new Object[]{key, val});
        }
        String insertDDSql = dbAdapter.insertMetaSql(forTable);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).batchUpdate(insertDDSql, data);
    }

    /**
     * @param dbFile
     * @param dg
     * @param dbAdapter
     * @param tblName
     */
    public static void createDDTbl(File dbFile, DataGroup dg, DbAdapter dbAdapter, String tblName) {

        DataType[] colsAry = makeDbCols(dg);
        String createDDSql = dbAdapter.createDDSql(tblName);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).update(createDDSql);

        Map<String, DataGroup.Attribute> meta = dg.getAttributes();
        List<Object[]> data = new ArrayList<>();
        for(DataType dt : colsAry) {
            int width = getIntVal(meta, WIDTH_TAG, dt, 0);
            width = width > 0 ? width : dt.getFormatInfo().getWidth();
            String format = getStrVal(meta, FORMAT_TAG, dt, null);
            format = format == null ? dt.getFormatInfo().getDataFormatStr() : format;
            String visi = dt.getKeyName().equals(ROW_IDX) ? VISI_HIDDEN :
                            getStrVal(meta, VISI_TAG, dt, VISI_SHOW);

            data.add( new Object[]
                    {
                            dt.getKeyName(),
                            getStrVal(meta, LABEL_TAG, dt, dt.getKeyName()),
                            dt.getTypeDesc(),
                            dt.getDataUnit(),
                            format,
                            width,
                            visi,
                            Boolean.valueOf(getStrVal(meta, SORTABLE_TAG, dt, "true")),
                            Boolean.valueOf(getStrVal(meta, FILTERABLE_TAG, dt, "true")),
                            getStrVal(meta, DESC_TAG, dt, "")
                    }
            );
        }
        String insertDDSql = dbAdapter.insertDDSql(tblName);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).batchUpdate(insertDDSql, data);
    }

//====================================================================
//
//====================================================================

    private static int insertMeta(DataGroup dg, ResultSet rs) {
        try {
            do {
                dg.addAttribute(rs.getString("key"), rs.getString("value"));
            } while (rs.next());
        } catch (SQLException e) {
            logger.error(e);
        }
        return 0;
    }

    private static int insertDD(DataGroup dg, ResultSet rs) {
        try {
            do {
                String cname = rs.getString("cname");
                String label = rs.getString("label");
                String units = rs.getString("units");
                String format = rs.getString("format");
                int width = rs.getInt("width");
                String visibility = rs.getString("visibility");
                String desc = rs.getString("desc");
                boolean sortable = rs.getBoolean("sortable");
                boolean filterable = rs.getBoolean("filterable");

                DataType dtype = dg.getDataDefintion(cname, true);
                if (dtype != null) {
                    dtype.setKeyName(cname);
                    if (!StringUtils.areEqual(label, cname)) {
                        String attr = DataSetParser.makeAttribKey(DataSetParser.LABEL_TAG, cname);
                        dg.addAttribute(attr, label);
                    }
                    if (!StringUtils.isEmpty(units)) {
                        dtype.setUnits(units);
                    }
                    if (!StringUtils.isEmpty(format)) {
                        dtype.getFormatInfo().setDataFormat(format);
                    }
                    if (width > 0) {
                        dtype.getFormatInfo().setWidth(width);
                    }
                    if (!StringUtils.areEqual(visibility, VISI_SHOW)) {
                        String attr = DataSetParser.makeAttribKey(DataSetParser.VISI_TAG, cname);
                        dg.addAttribute(attr, visibility);
                    }
                    if (!StringUtils.isEmpty(desc)) {
                        String attr = DataSetParser.makeAttribKey(DataSetParser.DESC_TAG, cname);
                        dg.addAttribute(attr, desc);
                    }
                    if (!sortable) {
                        String attr = DataSetParser.makeAttribKey(DataSetParser.SORTABLE_TAG, cname);
                        dg.addAttribute(attr, String.valueOf(false));
                    }
                    if (!filterable) {
                        String attr = DataSetParser.makeAttribKey(DataSetParser.FILTERABLE_TAG, cname);
                        dg.addAttribute(attr, String.valueOf(false));
                    }
                }
            } while (rs.next());
        } catch (SQLException e) {
            logger.error(e);
        }
        return 0;
    }

    private static void doTableLoad(JdbcTemplate jdbc, String insertDataSql, DataGroup data) {

        jdbc.batchUpdate(insertDataSql, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Object[] row = data.get(i).getData();
                Object[] rowWithIdx = new Object[ row.length + 2];
                System.arraycopy(row, 0, rowWithIdx, 0, row.length);
                rowWithIdx[row.length] = i;
                rowWithIdx[row.length+1] = i;

                for (int cidx = 0; cidx < rowWithIdx.length; cidx++) ps.setObject(cidx+1, rowWithIdx[cidx]);
            }
            public int getBatchSize() {
                return data.size();
            }
        });
    }

    private static DataType[] makeDbCols(DataGroup dg) {
        DataType[] cols = new DataType[dg.getDataDefinitions().length + 2];
        if (dg.getDataDefintion(ROW_IDX) != null) {
            logger.error("Datagroup should not have ROW_IDX in it at the start.");
        }
        System.arraycopy(dg.getDataDefinitions(), 0, cols, 0, cols.length-2);
        cols[cols.length-2] = DataGroup.makeRowIdx();
        cols[cols.length-1] = DataGroup.makeRowNum();
        dg.addAttribute(DataSetParser.makeAttribKey(DataSetParser.VISI_TAG, ROW_IDX), DataSetParser.VISI_HIDDEN);
        dg.addAttribute(DataSetParser.makeAttribKey(DataSetParser.VISI_TAG, ROW_NUM), DataSetParser.VISI_HIDDEN);
        return cols;
    }

    private static String getStrVal(Map<String, DataGroup.Attribute> meta, String tag, DataType col, String def) {
        DataGroup.Attribute val = meta.get(makeAttribKey(tag, col.getKeyName()));
        return val == null ? def : val.getValue();
    }

    private static int getIntVal(Map<String, DataGroup.Attribute> meta, String tag, DataType col, int def) {
        String v = getStrVal(meta, tag, col, null);
        return v == null ? def : Integer.parseInt(v);
    }


    /**
     * This is where you create stored function or procedure.
     * @param dbFile
     * @param dbAdapter
     */
    private static void createCustomFunctions(File dbFile, DbAdapter dbAdapter) {

        String decimate_key =
                "CREATE FUNCTION decimate_key(xVal DOUBLE, yVal DOUBLE, xMin DOUBLE, yMin DOUBLE, nX INT, nY INT, xUnit DOUBLE, yUnit DOUBLE)\n" +
                        "RETURNS CHAR VARYING(20)\n" +
                        "LANGUAGE JAVA DETERMINISTIC NO SQL\n" +
                        "EXTERNAL NAME 'CLASSPATH:edu.caltech.ipac.firefly.server.db.DbCustomFunctions.getDecimateKey'\n";
        try {
            JdbcFactory.getTemplate(dbAdapter.getDbInstance(dbFile)).execute(decimate_key);
        } catch (Exception ex) {
            logger.error("Fail to create custom function:" + decimate_key);
        }

    }

}
