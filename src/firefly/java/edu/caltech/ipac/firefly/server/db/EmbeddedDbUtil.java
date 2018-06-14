/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.table.MappedData;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.db.spring.mapper.DataGroupUtil;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.TableDef;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
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

import static edu.caltech.ipac.firefly.server.db.DbCustomFunctions.createCustomFunctions;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_PATH;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_TYPE;
import static edu.caltech.ipac.table.DataGroup.ROW_IDX;

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
        dbAdapter.close(dbFile, true);              // in case database exists in memory, close it and remove all files related to it.
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
        // these are transient values and should not be persisted.
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

        if (!hasTable(treq, dbFile, tblName)) {
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
    public static MappedData getSelectedMappedData(ServerRequest searchRequest, List<Integer> selRows, String... cols) {
        if (cols != null && cols.length > 0) {
            ArrayList<String> colsAry = new ArrayList<>(Arrays.asList(cols));
            if (!colsAry.contains(DataGroup.ROW_NUM)) {
                // add ROW_NUM into the returned results if not asked
                colsAry.add(DataGroup.ROW_NUM);
                cols = colsAry.toArray(new String[colsAry.size()]);
            }
        }
        MappedData results = new MappedData();
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

        List<Object[]> data = new ArrayList<>();
        for(DataType dt : colsAry) {
            data.add( new Object[]
                    {
                            dt.getKeyName(),
                            dt.getLabel(),
                            dt.getTypeDesc(),
                            dt.getUnits(),
                            dt.getNullString(),
                            dt.getFormat(),
                            dt.getWidth(),
                            dt.getVisibility().name(),
                            dt.isSortable(),
                            dt.isFilterable(),
                            dt.getDesc()
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
                String nullStr = rs.getString("null_str");
                String format = rs.getString("format");
                int width = rs.getInt("width");
                String visibility = rs.getString("visibility");
                String desc = rs.getString("desc");
                boolean sortable = rs.getBoolean("sortable");
                boolean filterable = rs.getBoolean("filterable");

                DataType dtype = dg.getDataDefintion(cname, true);

                if (dtype != null) {
                    if (!StringUtils.isEmpty(label)) dtype.setLabel(label);
                    if (!StringUtils.isEmpty(units)) dtype.setUnits(units);
                    if (!StringUtils.isEmpty(nullStr)) dtype.setNullString(nullStr);
                    if (!StringUtils.isEmpty(format)) dtype.setFormat(format);
                    if (!StringUtils.isEmpty(visibility)) dtype.setVisibility(DataType.Visibility.valueOf(visibility));
                    if (!StringUtils.isEmpty(desc)) dtype.setDesc(desc);
                    if (width > 0) dtype.setWidth(width);
                    if (!sortable) dtype.setSortable(false);
                    if (!filterable) dtype.setFilterable(false);
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
        return cols;
    }

    private static String getStrVal(Map<String, DataGroup.Attribute> meta, String tag, DataType col, String def) {
        DataGroup.Attribute val = meta.get(TableMeta.makeAttribKey(tag, col.getKeyName()));
        return val == null ? def : val.getValue();
    }

    private static int getIntVal(Map<String, DataGroup.Attribute> meta, String tag, DataType col, int def) {
        String v = getStrVal(meta, tag, col, null);
        return v == null ? def : Integer.parseInt(v);
    }


    /**
     * This function is to test if a table exists in the given database.
     * It's using a get count and catches exception to determine if the given table exists.
     * It will work across all databases, but it's not optimal.  Change to specific implementation when needed.
     * @param treq
     * @param dbFile
     * @param tblName
     * @return
     */
    public static boolean hasTable(TableServerRequest treq, File dbFile, String tblName) {
        try {
            DbInstance dbInstance = DbAdapter.getAdapter(treq).getDbInstance(dbFile);
            JdbcFactory.getSimpleTemplate(dbInstance).queryForInt(String.format("select count(*) from %s", tblName));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
