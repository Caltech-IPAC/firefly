/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.SelectionInfo;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.*;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import org.json.simple.JSONObject;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.server.ServerContext.SHORT_TASK_EXEC;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_PATH;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_TYPE;
import static edu.caltech.ipac.firefly.server.db.DbAdapter.MAIN_DB_TBL;
import static edu.caltech.ipac.firefly.server.db.DbAdapter.NULL_TOKEN;
import static edu.caltech.ipac.firefly.server.db.DbCustomFunctions.createCustomFunctions;
import static edu.caltech.ipac.firefly.server.db.DbInstance.USE_REAL_AS_DOUBLE;
import static edu.caltech.ipac.table.DataGroup.ROW_IDX;
import static edu.caltech.ipac.table.DataType.descToType;
import static edu.caltech.ipac.table.TableMeta.DERIVED_FROM;
import static edu.caltech.ipac.util.StringUtils.*;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class EmbeddedDbUtil {
    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private static final int MAX_COL_ENUM_COUNT = AppProperties.getIntProperty("max.col.enum.count", 32);

    /**
     * setup a database
     * @param dbFile  the file to save the database to.
     * @param dbAdapter DbAdapter to use.. ie sqlite, h2, etc.
     */
    public static void createDbFile(File dbFile, DbAdapter dbAdapter) throws IOException {
        dbAdapter.close(dbFile, true);              // in case database exists in memory, close it and remove all files related to it.
        if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();
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

        IpacTableUtil.consumeColumnInfo(dg);

        createDataTbl(dbFile, dg, dbAdapter, forTable);
        ddToDb(dbFile, dg, dbAdapter, forTable);
        metaToDb(dbFile, dg, dbAdapter, forTable);
        auxDataToDb(dbFile, dg, dbAdapter, forTable);
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
            JdbcTemplate jdbc = JdbcFactory.getTemplate(dbAdapter.getDbInstance(dbFile));

            String insertDataSql = dbAdapter.insertDataSql(colsAry, tblName);
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
    public static DataGroupPart execRequestQuery(TableServerRequest treq, File dbFile, String forTable) throws DataAccessException {
        DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
        String selectPart = dbAdapter.selectPart(treq);
        String wherePart = dbAdapter.wherePart(treq);
        String orderByPart = dbAdapter.orderByPart(treq);
        String pagingPart = dbAdapter.pagingPart(treq);

        String sql = String.format("%s from %s %s %s %s", selectPart, forTable, wherePart, orderByPart, pagingPart);
        DataGroup data = EmbeddedDbUtil.execQuery(dbAdapter, dbFile, sql, forTable);

        int rowCnt = data.size();
        if (!isEmpty(pagingPart)) {
            // fetch total row count for the query.. datagroup may contain partial results(paging)
            String cntSql = String.format("select count(*) from %s %s", forTable, wherePart);
            rowCnt = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).queryForInt(cntSql);
        }

        DataGroupPart page = EmbeddedDbUtil.toDataGroupPart(data, treq);
        page.setRowCount(rowCnt);
        if (!isEmpty(treq.getTblTitle())) {
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
    public static DataGroup execQuery(DbAdapter dbAdapter, File dbFile, String sql, String refTable) throws DataAccessException {
        DbInstance dbInstance = dbAdapter.getDbInstance(dbFile);
        sql = dbAdapter.translateSql(sql);
        String ddSql = refTable == null ? null : dbAdapter.getDDSql(refTable);

        logger.trace("execQuery => SQL: " + sql);
        try {
            DataGroup dg = (DataGroup)JdbcFactory.getTemplate(dbInstance).query(sql, rs -> {
                return dbToDataGroup(rs, dbInstance, ddSql);
            });

            SimpleJdbcTemplate jdbc = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile));

            if (refTable != null) {
                // insert table meta info into the results
                try {
                    String metaSql = dbAdapter.getMetaSql(refTable);
                    jdbc.query(metaSql, (rs, i) -> EmbeddedDbUtil.dbToMeta(dg, rs));
                } catch (Exception e) {
                    // ignore.. may not have meta table
                }
                // insert aux data info into the results
                try {
                    String auxDataSqlSql = dbAdapter.getAuxDataSql(refTable);
                    jdbc.query(auxDataSqlSql, (rs, i) -> EmbeddedDbUtil.dbToAuxData(dg, rs));
                } catch (Exception e) {
                    // ignore.. may not have meta table
                }
            }

            return dg;
        } catch (Exception e) {
            // catch for debugging
            logger.debug(String.format("execQuery failed with error: %s \n\t sql: %s \n\t refTable: %s \n\t dbFile: %s", e.getMessage(), sql, refTable, dbFile.getAbsolutePath()) );
            throw handleSqlExp(e);
        }
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
    public static DataGroup getSelectedData(ServerRequest searchRequest, List<Integer> selRows, String... cols) throws DataAccessException {
        TableServerRequest treq = (TableServerRequest)searchRequest;
        EmbeddedDbProcessor proc = (EmbeddedDbProcessor) SearchManager.getProcessor(searchRequest.getRequestId());
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
    public static MappedData getSelectedMappedData(ServerRequest searchRequest, List<Integer> selRows, String... cols) throws DataAccessException {
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

    public static DataGroupPart getSelectedDataAsDGPart(ServerRequest searchRequest, List<Integer> selRows, String... cols) throws DataAccessException{
        return toDataGroupPart(getSelectedData(searchRequest, selRows, cols), (TableServerRequest) searchRequest);
    }

    public static DataGroupPart toDataGroupPart(DataGroup data, TableServerRequest treq) {
        return new DataGroupPart(data, treq.getStartIndex(), data.size());
    }

    public static void updateColumn(File dbFile, DbAdapter dbAdapter, DataType dtype, String expression, String editColName, String preset, String resultSetID, SelectionInfo si) throws DataAccessException {

        if (isEmpty(editColName)) return;

        DbAdapter.EmbeddedDbInstance dbInstance = (DbAdapter.EmbeddedDbInstance) dbAdapter.getDbInstance(dbFile);
        JdbcTemplate jdbc = JdbcFactory.getTemplate(dbInstance);

        try {
            // change column type if different
            String oldType = String.valueOf(jdbc.queryForObject(String.format("SELECT type from DATA_DD where cname='%s'", editColName), String.class));
            if (!oldType.equals(dtype.getTypeDesc())) {
                // race condition; cannot change double values into boolean even when they are NULLs
                // add new column, drop the old instead
                List cols = jdbc.query("SELECT COLUMN_NAME  FROM INFORMATION_SCHEMA.COLUMNS where table_name = 'DATA'", (rs, i) -> rs.getString(1));
                String nextCname = (String) cols.get(cols.indexOf(editColName)+1);
                deleteColumn(dbFile, dbAdapter, editColName, false);
                addColumn(dbFile, dbAdapter, dtype, expression, preset, resultSetID, si, nextCname);
                return;
            } else {
                // rename column if different
                if (!dtype.getKeyName().equals(editColName)) {
                    jdbc.update(String.format("ALTER TABLE %s ALTER COLUMN \"%s\" RENAME TO \"%s\"", MAIN_DB_TBL, editColName, dtype.getKeyName()));
                }
            }
        } catch (Exception e) {
            // DDL statement are transactionally isolated, therefore need to manually rollback if this succeed but the next few statements failed.
            throw handleSqlExp(e);
        }
        try {
            TransactionTemplate txnJdbc = JdbcFactory.getTransactionTemplate(jdbc.getDataSource());
            txnJdbc.execute((st) -> {
                // update DD table
                String sql = "UPDATE DATA_DD SET cname=?, type=?, precision=?, units=?, ucd=?, desc=? WHERE cname=?";
                String desc = getFieldDesc(dtype, expression, preset);
                Object[] params = {dtype.getKeyName(), dtype.getTypeDesc(), dtype.getPrecision(), dtype.getUnits(), dtype.getUCD(), desc, editColName};
                jdbc.update(sql, params);

                populateColumnValues(jdbc, dtype, expression, preset, resultSetID, si);

                // purge all cached tables
                BaseDbAdapter.compact(dbInstance);
                return st;
            });
            enumeratedValuesCheck(dbFile, dbAdapter, new DataType[]{dtype});        // if successful, check for enum values of this new updated column
        } catch (Exception e) {
            // manually revert the name change
            if (!dtype.getKeyName().equals(editColName)) {
                jdbc.update(String.format("ALTER TABLE %s ALTER COLUMN \"%s\" RENAME TO \"%s\"", MAIN_DB_TBL, dtype.getKeyName(), editColName));
            }
            throw handleSqlExp(e);
        }
    }

    public static void addColumn(File dbFile, DbAdapter dbAdapter, DataType dtype, String expression) throws DataAccessException {
        addColumn(dbFile, dbAdapter, dtype, expression, null, null, null, null);
    }
    public static void addColumn(File dbFile, DbAdapter dbAdapter, DataType dtype, String expression, String preset, String resultSetID, SelectionInfo si) throws DataAccessException {
        addColumn(dbFile, dbAdapter, dtype, expression, preset, resultSetID, si, null);

    }
    static void addColumn(File dbFile, DbAdapter dbAdapter, DataType dtype, String expression, String preset, String resultSetID, SelectionInfo si, String beforeCname) throws DataAccessException {
        DbAdapter.EmbeddedDbInstance dbInstance = (DbAdapter.EmbeddedDbInstance) dbAdapter.getDbInstance(dbFile);
        JdbcTemplate jdbc = JdbcFactory.getTemplate(dbInstance);
        beforeCname = isEmpty(beforeCname) ? "ROW_IDX" : beforeCname;

        try {
            // add column to main table
            jdbc.update(String.format("ALTER TABLE %s ADD COLUMN \"%s\" %s BEFORE \"%s\"", MAIN_DB_TBL, dtype.getKeyName(), dbAdapter.toDbDataType(dtype), beforeCname));
        } catch (Exception e) {
            // DDL statement are transactionally isolated, therefore need to manually rollback if this succeed but the next few statements failed.
            throw handleSqlExp(e);
        }
        try {
            TransactionTemplate txnJdbc = JdbcFactory.getTransactionTemplate(jdbc.getDataSource());
            txnJdbc.execute((st) -> {
                // add a record to DD table
                String sql = dbAdapter.insertDDSql(MAIN_DB_TBL);
                String desc = getFieldDesc(dtype, expression, preset);
                dtype.setDesc(desc);
                jdbc.update(sql, getDDfrom(dtype));

                populateColumnValues(jdbc, dtype, expression, preset, resultSetID, si);

                // purge all cached tables
                BaseDbAdapter.compact(dbInstance);
                return st;
            });
            enumeratedValuesCheck(dbFile, dbAdapter, new DataType[]{dtype});        // if successful, check for enum values of this new column
        } catch (Exception e) {
            // manually remove the added column
            jdbc.update(String.format("ALTER TABLE %s DROP COLUMN \"%s\"", MAIN_DB_TBL, dtype.getKeyName()));
            throw handleSqlExp(e);
        }
    }

    static String getFieldDesc(DataType dtype, String expression, String preset) {
        String desc = isEmpty(dtype.getDesc()) ? "" : dtype.getDesc();
        String derivedFrom = isEmpty(preset) ? expression : "preset:" + preset;
        return String.format("(%s=%s) ", DERIVED_FROM, derivedFrom) + desc;     // prepend DERIVED_FROM value into the description.  This is how we determine if a column is derived.
    }

    static void populateColumnValues(JdbcTemplate jdbc, DataType dtype, String expression, String preset, String resultSetID, SelectionInfo si) {
        // populate column with new values
        if (isEmpty(preset)) {
            jdbc.update(String.format("UPDATE %s SET \"%s\" = %s", MAIN_DB_TBL, dtype.getKeyName(), expression));
        } else {
            jdbc.update(String.format("CREATE INDEX  IF NOT EXISTS data_idx ON %s (row_idx)", MAIN_DB_TBL));
            if (!resultSetID.equals(MAIN_DB_TBL)) {
                jdbc.update(String.format("CREATE INDEX  IF NOT EXISTS %s_idx ON %s (row_idx)", resultSetID, resultSetID));
            }
            if (preset.equals("filtered")) {
                String sql = resultSetID.equals(MAIN_DB_TBL) ? "TRUE" : String.format("(SELECT 1 from %s as t WHERE t.ROW_IDX = d.ROW_IDX)", resultSetID);
                jdbc.update(String.format("UPDATE %s as d SET \"%s\" = %s", MAIN_DB_TBL, dtype.getKeyName(), sql));
            } else if (preset.equals("selected")) {
                String sql = "FALSE";
                if (si != null && si.getSelectedCount() > 0) {
                    if (resultSetID.equals(MAIN_DB_TBL)) {
                        sql =  si.isSelectAll() ? "TRUE" : String.format("(ROW_IDX in (%s))", StringUtils.toString(si.getSelected()));
                    } else {
                        String rowNums = StringUtils.toString(si.getSelected());
                        List<Integer> rowIdxs = new SimpleJdbcTemplate(jdbc).query(String.format("Select ROW_IDX from %s where ROW_NUM in (%s)", resultSetID, rowNums), (resultSet, i) -> resultSet.getInt(1));
                        sql = String.format("(ROW_IDX in (%s))", StringUtils.toString(rowIdxs));
                    }
                }
                jdbc.update(String.format("UPDATE %s SET \"%s\" = %s", MAIN_DB_TBL, dtype.getKeyName(), sql));
            } else if (preset.equals("ROW_NUM")) {
                String sql = resultSetID.equals(MAIN_DB_TBL) ? "(ROW_NUM + 1)" : String.format("(SELECT t.ROW_NUM+1 from %s as t WHERE t.ROW_IDX = d.ROW_IDX)", resultSetID);
                jdbc.update(String.format("UPDATE %s as d SET \"%s\" = %s", MAIN_DB_TBL, dtype.getKeyName(), sql));
            }
        }
    }

    public static void deleteColumn(File dbFile, DbAdapter dbAdapter, String cname) {
        deleteColumn(dbFile, dbAdapter, cname, true);
    }
    public static void deleteColumn(File dbFile, DbAdapter dbAdapter, String cname, boolean doCompact) {
        DbAdapter.EmbeddedDbInstance dbInstance = (DbAdapter.EmbeddedDbInstance) dbAdapter.getDbInstance(dbFile);
        JdbcTemplate jdbc = JdbcFactory.getTemplate(dbInstance);

        // drop column from DATA table
        jdbc.update(String.format("ALTER TABLE %s DROP COLUMN \"%s\"", MAIN_DB_TBL, cname));

        // remove column from DD table
        String sql = "DELETE FROM DATA_DD WHERE cname=?";
        Object[] params = {cname};
        jdbc.update(sql, params);

        // purge all cached tables
        if (doCompact) BaseDbAdapter.compact(dbInstance);
    }


//====================================================================
//  O-R mapping functions
//====================================================================

    public static DataGroup dbToDataGroup(ResultSet rs, DbInstance dbInstance, String ddSql) throws SQLException {

        DataGroup dg = new DataGroup(null, getCols(rs, dbInstance));

        if (ddSql != null) {
            try {
                JdbcFactory.getSimpleTemplate(dbInstance).query(ddSql, (ddrs, i) -> dbToDD(dg, ddrs));
            } catch (Exception e) {
                // ignore.. may not have DD table
            }
        }

        if (rs.isBeforeFirst()) rs.next();
        if (!rs.isFirst()) return dg;    // no row found
        do {
            DataObject row = new DataObject(dg);
            for (int i = 0; i < dg.getDataDefinitions().length; i++) {
                DataType dt = dg.getDataDefinitions()[i];
                int idx = i + 1;
                Object val = rs.getObject(idx);
                if (dt.getDataType() == Float.class && val instanceof Double) {
                    // this is needed because hsql stores float as double.
                    val = ((Double) val).floatValue();
                } else if (dt.getDataType() == Double.class && val instanceof BigDecimal) {
                        // When expression involved big number like, long(bigint), BigDecimal is returned. Need to convert that back to double
                        val = ((BigDecimal) val).doubleValue();
                    }
                row.setDataElement(dt, val);
            }
            dg.add(row);
        } while (rs.next()) ;
        logger.trace(String.format("converting a %,d rows ResultSet into a DataGroup", dg.size()));
        return dg;
    }

    private static void metaToDb(File dbFile, DataGroup dg, DbAdapter dbAdapter, String forTable) {
        TableMeta meta = dg.getTableMeta();
        if (meta.isEmpty()) return;

        String createMetaSql = dbAdapter.createMetaSql(forTable);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).update(createMetaSql);

        List<Object[]> data = new ArrayList<>();
        // take all keywords
        meta.getKeywords().forEach(kw -> data.add(new Object[]{kw.getKey(), kw.getValue(), kw.isKeyword()}));
        // then take only meta that's not keywords
        meta.getAttributeList().stream()
                .filter(kw -> !kw.isKeyword())
                .forEach(kw -> data.add(new Object[]{kw.getKey(), kw.getValue(), kw.isKeyword()}));
        String insertDDSql = dbAdapter.insertMetaSql(forTable);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).batchUpdate(insertDDSql, data);
    }

    private static int dbToMeta(DataGroup dg, ResultSet rs) {
        try {
            do {
                String key = rs.getString("key");
                String value = rs.getString("value");
                boolean isKeyword = rs.getBoolean("isKeyword");
                if (isKeyword) {
                    dg.getTableMeta().addKeyword(key, value);
                } else {
                    dg.getTableMeta().setAttribute(key, value);
                }
            } while (rs.next());
        } catch (SQLException e) {
            logger.error(e);
        }
        return 0;
    }

    private static void auxDataToDb(File dbFile, DataGroup dg, DbAdapter dbAdapter, String tblName) {

        String createAuxDataSql = dbAdapter.createAuxDataSql(tblName);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).update(createAuxDataSql);

        List<Object[]> data = new ArrayList<>();
        data.add( new Object[]
                {
                        dg.getTitle(),
                        dg.size(),
                        dg.getGroupInfos(),
                        dg.getLinkInfos(),
                        dg.getParamInfos(),
                        dg.getResourceInfos()
                });
        String insertDDSql = dbAdapter.insertAuxDataSql(tblName);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).batchUpdate(insertDDSql, data);
    }

    private static int dbToAuxData(DataGroup dg, ResultSet rs) {
        try {
            do {
                dg.setTitle(rs.getString("title"));
                dg.setLinkInfos((List<LinkInfo>) rs.getObject("links"));
                dg.setGroupInfos((List<GroupInfo>) rs.getObject("groups"));
                dg.setParamInfos((List<ParamInfo>) rs.getObject("params"));
                dg.setResourceInfos((List<ResourceInfo>) rs.getObject("resources"));
            } while (rs.next());
        } catch (SQLException e) {
            logger.error(e);
        }
        return 0;
    }

    private static void ddToDb(File dbFile, DataGroup dg, DbAdapter dbAdapter, String tblName) {

        DataType[] colsAry = makeDbCols(dg);
        String createDDSql = dbAdapter.createDDSql(tblName);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).update(createDDSql);

        List<Object[]> data = new ArrayList<>();
        for(DataType dt : colsAry) {
            data.add( getDDfrom(dt) );
        }
        String insertDDSql = dbAdapter.insertDDSql(tblName);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).batchUpdate(insertDDSql, data);
    }

    private static Object[] getDDfrom(DataType dt) {
        return new Object[] {
                        dt.getKeyName(),
                        dt.getLabel(),
                        dt.getTypeDesc(),
                        dt.getUnits(),
                        dt.getNullString(),
                        dt.getFormat(),
                        dt.getFmtDisp(),
                        dt.getWidth(),
                        dt.getVisibility().name(),
                        dt.isSortable(),
                        dt.isFilterable(),
                        dt.isFixed(),
                        dt.getDesc(),
                        dt.getEnumVals(),
                        dt.getID(),
                        dt.getPrecision(),
                        dt.getUCD(),
                        dt.getUType(),
                        dt.getRef(),
                        dt.getMaxValue(),
                        dt.getMinValue(),
                        dt.getLinkInfos(),
                        dt.getDataOptions(),
                        dt.getArraySize(),
                        dt.getCellRenderer(),
                        dt.getSortByCols()
                };
    }

    private static int dbToDD(DataGroup dg, ResultSet rs) {
        try {
            do {
                String cname = rs.getString("cname");
                DataType dtype = dg.getDataDefintion(cname, true);

                if (dtype == null) return 0;          // this column is not in DataGroup.  no need to update the info

                String typeDesc = rs.getString("type");
                dtype.setTypeDesc(typeDesc);
                dtype.setDataType(descToType(typeDesc));

                applyIfNotEmpty(rs.getString("label"), dtype::setLabel);
                applyIfNotEmpty(rs.getString("units"), dtype::setUnits);
                dtype.setNullString(rs.getString("null_str"));
                applyIfNotEmpty(rs.getString("format"), dtype::setFormat);
                applyIfNotEmpty(rs.getString("fmtDisp"), dtype::setFmtDisp);
                applyIfNotEmpty(rs.getInt("width"), dtype::setWidth);
                applyIfNotEmpty(rs.getString("visibility"), v -> dtype.setVisibility(DataType.Visibility.valueOf(v)));
                applyIfNotEmpty(rs.getString("desc"), dtype::setDesc);
                applyIfNotEmpty(rs.getBoolean("sortable"), dtype::setSortable);
                applyIfNotEmpty(rs.getBoolean("filterable"), dtype::setFilterable);
                applyIfNotEmpty(rs.getBoolean("fixed"), dtype::setFixed);
                applyIfNotEmpty(rs.getString("enumVals"), dtype::setEnumVals);
                applyIfNotEmpty(rs.getString("ID"), dtype::setID);
                applyIfNotEmpty(rs.getString("precision"), dtype::setPrecision);
                applyIfNotEmpty(rs.getString("ucd"), dtype::setUCD);
                applyIfNotEmpty(rs.getString("utype"), dtype::setUType);
                applyIfNotEmpty(rs.getString("ref"), dtype::setRef);
                applyIfNotEmpty(rs.getString("maxValue"), dtype::setMaxValue);
                applyIfNotEmpty(rs.getString("minValue"), dtype::setMinValue);
                applyIfNotEmpty(rs.getObject("links"), v -> dtype.setLinkInfos((List<LinkInfo>) v));
                applyIfNotEmpty(rs.getString("dataOptions"), dtype::setDataOptions);
                applyIfNotEmpty(rs.getString("arraySize"), dtype::setArraySize);
                applyIfNotEmpty(rs.getString("cellRenderer"), dtype::setCellRenderer);
                applyIfNotEmpty(rs.getString("sortByCols"), dtype::setSortByCols);

            } while (rs.next());
        } catch (SQLException e) {
            logger.error(e);
        }
        return 0;
    }


    public static List<DataType> getCols(ResultSet rs, DbInstance dbInstance) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        List<DataType> cols = new ArrayList<>();
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            String cname = rsmd.getColumnLabel(i);
            Class type = convertToClass(rsmd.getColumnType(i), dbInstance);
            DataType dt = new DataType(cname, type);
            cols.add(dt);
        }
        return cols;
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


    public static void enumeratedValuesCheckBG(File dbFile, DataGroupPart results, TableServerRequest treq) {
        RequestOwner owner = ServerContext.getRequestOwner();
        ServerEvent.EventTarget target = new ServerEvent.EventTarget(ServerEvent.Scope.SELF, owner.getEventConnID(),
                owner.getEventChannel(), owner.getUserKey());
        SHORT_TASK_EXEC.submit(() -> {
            enumeratedValuesCheck(dbFile, results, treq);
            DataGroup updates = new DataGroup(null, results.getData().getDataDefinitions());
            updates.getTableMeta().setTblId(results.getData().getTableMeta().getTblId());
            JSONObject changes = JsonTableUtil.toJsonDataGroup(updates);
            changes.remove("totalRows");        //changes contains only the columns with 0 rows.. we don't want to update totalRows

            FluxAction action = new FluxAction(FluxAction.TBL_UPDATE, changes);
            ServerEventManager.fireAction(action, target);
        });
    }
    public static void enumeratedValuesCheck(File dbFile, DataGroupPart results, TableServerRequest treq) {
        StopWatch.getInstance().start("enumeratedValuesCheck: " + treq.getRequestId());
        enumeratedValuesCheck(dbFile, DbAdapter.getAdapter(treq), results.getData().getDataDefinitions());
        StopWatch.getInstance().stop("enumeratedValuesCheck: " + treq.getRequestId()).printLog("enumeratedValuesCheck: " + treq.getRequestId());
    }

    public static void enumeratedValuesCheck(File dbFile, DbAdapter dbAdapter, DataType[] inclCols) {
        if (inclCols != null && inclCols.length > 0)
        try {
            String cols = Arrays.stream(inclCols)
                    .filter(dt -> maybeEnums(dt))
                    .map(dt -> String.format("count(distinct \"%s\") as \"%s\"", dt.getKeyName(), dt.getKeyName()))
                    .collect(Collectors.joining(", "));

            List<Map<String, Object>> rs = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile))
                    .queryForList(String.format("SELECT %s FROM data where rownum < 500", cols));

            rs.get(0).forEach( (cname,v) -> {
                Long count = (Long) v ;
                if (count > 0 && count <= MAX_COL_ENUM_COUNT) {
                    List<Map<String, Object>> vals = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile))
                            .queryForList(String.format("SELECT distinct \"%s\" FROM data order by 1", cname));

                    DataType col = findColByName(inclCols, cname);
                    if (col != null && vals.size() <= MAX_COL_ENUM_COUNT) {
                        String enumVals = vals.stream()
                                .map(m -> m.get(cname) == null ? NULL_TOKEN : m.get(cname).toString())   // convert to list of value as string
                                .collect(Collectors.joining(","));                              // combine the values into a comma separated values string.
                        if (col != null)  col.setEnumVals(enumVals);
                        // update dd table
                        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile))
                                .update("UPDATE data_dd SET enumVals = ? WHERE cname = ?", enumVals, cname);
                    }
                }
            });
        } catch (Exception ex) {
            // do nothing.. ok to ignore errors.
        }
    }
    private static DataType findColByName(DataType[] cols, String name) {
        for(DataType dt : cols) {
            if (dt.getKeyName().equals(name)) return dt;
        }
        return  null;
    }

    private static DataAccessException handleSqlExp(Exception e) {
        String msg = e.getMessage();
        if (e instanceof BadSqlGrammarException) {
            // org.springframework.jdbc.BadSqlGrammarException: StatementCallback; bad SQL grammar [select * from xyz order by aab]; nested exception is java.sql.SQLSyntaxErrorException: user lacks privilege or object not found: XYZ\n
            String[] parts = groupMatch(".*\\[(.+)\\].* object not found: (.+)", msg);
            if (parts != null && parts.length == 2) {
                if (parts[1].equals("PUBLIC.DATA")) {
                    return new DataAccessException("TABLE out-of-sync; Reload table to resume");
                } else {
                    return new DataAccessException(String.format("[%s] not found; SQL=[%s]", parts[1], parts[0]));
                }
            }
            //org.springframework.jdbc.BadSqlGrammarException: StatementCallback; bad SQL grammar [invalid sql]; nested exception is java.sql.SQLSyntaxErrorException: unexpected token: INVALID
            parts = groupMatch(".*\\[(.+)\\].* unexpected token: (.+)", msg);
            if (parts != null && parts.length == 2) {
                return new DataAccessException(String.format("Unexpected token [%s]; SQL=[%s]", parts[1], parts[0]));
            }
        }
        if (e instanceof DataIntegrityViolationException) {
            String[] parts = groupMatch(".*\\[(.+)\\].*", msg);
            if (parts != null && parts.length == 1) {
                return new DataAccessException(String.format("Type mismatch; SQL=[%s]", parts[0]));
            }
        }

        return new DataAccessException(e);
    }


//====================================================================
//  privates functions
//====================================================================

    private static List<Class> onlyCheckTypes = Arrays.asList(String.class, Integer.class, Long.class, Character.class, Boolean.class, Short.class, Byte.class);
    private static List<String> excludeColNames = Arrays.asList(DataGroup.ROW_IDX, DataGroup.ROW_NUM);
    private static boolean maybeEnums(DataType dt) {
        return onlyCheckTypes.contains(dt.getDataType()) && !excludeColNames.contains(dt.getKeyName());

    }

    private static Class convertToClass(int val, DbInstance dbInstance) {
        JDBCType type = JDBCType.valueOf(val);
        switch (type) {
            case CHAR:
            case VARCHAR:
            case LONGVARCHAR:
                return String.class;
            case TINYINT:
                return Byte.class;
            case SMALLINT:
                return Short.class;
            case INTEGER:
                return Integer.class;
            case BIGINT:
                return Long.class;
            case FLOAT:
                return Float.class;
            case REAL: {
                if (dbInstance.getBoolProp(USE_REAL_AS_DOUBLE, false)) return Double.class;
                else return Float.class;
            }
            case DOUBLE:
            case NUMERIC:
            case DECIMAL:
                return Double.class;
            case BIT:
            case BOOLEAN:
                return Boolean.class;
            case DATE:
            case TIME:
            case TIMESTAMP:
                return Date.class;
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
                return String.class;        // treat it as string for now.
            default:
                return String.class;
        }
    }



    private static void doTableLoad(JdbcTemplate jdbc, String insertDataSql, DataGroup data) {
        int loaded = 0;
        int rows = data.size();
        while (loaded < rows) {
            int batchSize = Math.min(rows-loaded, 10000);   // set batchSize limit to 10k to  ensure HUGE table do not require unnecessary amount of memory to load
            final int roffset = loaded;
            loaded += batchSize;
            jdbc.batchUpdate(insertDataSql, new BatchPreparedStatementSetter() {
                final DataType[] cols = data.getDataDefinitions();
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    int ridx = roffset+i;
                    for (int cidx = 0; cidx < cols.length; cidx++)  {
                        ps.setObject(cidx+1, data.getData(cols[cidx].getKeyName(), ridx));
                    }
                    ps.setObject(cols.length+1, ridx);         // add ROW_IDX
                    ps.setObject(cols.length+2, ridx);         // add ROW_NUM
                }
                public int getBatchSize() {
                    return batchSize;
                }
            });
        }
    }

    private static DataType[] makeDbCols(DataType[] columns) {
        return makeDbCols( new DataGroup("temp", columns));
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
}
