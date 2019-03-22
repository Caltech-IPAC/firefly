/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.table.GroupInfo;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.LinkInfo;
import edu.caltech.ipac.table.MappedData;
import edu.caltech.ipac.table.ParamInfo;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;
import nom.tam.fits.Data;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.server.db.DbCustomFunctions.createCustomFunctions;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_PATH;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_TYPE;
import static edu.caltech.ipac.table.DataGroup.ROW_IDX;
import static edu.caltech.ipac.util.StringUtils.*;

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
    public static DataGroupPart execRequestQuery(TableServerRequest treq, File dbFile, String forTable) {
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
    public static DataGroup execQuery(DbAdapter dbAdapter, File dbFile, String sql, String refTable) {
        DbInstance dbInstance = dbAdapter.getDbInstance(dbFile);
        sql = dbAdapter.translateSql(sql);
        String ddSql = refTable == null ? null : dbAdapter.getDDSql(refTable);

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
        return new DataGroupPart(data, treq.getStartIndex(), data.size());
    }


//====================================================================
//  O-R mapping functions
//====================================================================

    private static DataGroup dbToDataGroup(ResultSet rs, DbInstance dbInstance, String ddSql) throws SQLException {

        DataGroup dg = new DataGroup(null, getCols(rs));

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
                Object val = dt.getDataType() == Float.class ? rs.getFloat(idx) : rs.getObject(idx);        // this is needed because hsql store float as double.
                row.setDataElement(dt, val);
            }
            dg.add(row);
        } while (rs.next()) ;
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
                        dg.getParamInfos()
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
            data.add( new Object[]
                    {
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
                    }
            );
        }
        String insertDDSql = dbAdapter.insertDDSql(tblName);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).batchUpdate(insertDDSql, data);
    }

    private static int dbToDD(DataGroup dg, ResultSet rs) {
        try {
            do {
                String cname = rs.getString("cname");
                DataType dtype = dg.getDataDefintion(cname, true);

                if (dtype == null) return 0;          // this column is not in DataGroup.  no need to update the info

                String typeDesc = rs.getString("type");
                dtype.setTypeDesc(typeDesc);
                dtype.setDataType(DataType.descToType(typeDesc));

                applyIfNotEmpty(rs.getString("label"), dtype::setLabel);
                applyIfNotEmpty(rs.getString("units"), dtype::setUnits);
                applyIfNotEmpty(rs.getString("null_str"), dtype::setNullString);
                applyIfNotEmpty(rs.getString("format"), dtype::setFormat);
                applyIfNotEmpty(rs.getString("fmtDisp"), dtype::setFmtDisp);
                applyIfNotEmpty(rs.getInt("width"), dtype::setWidth);
                applyIfNotEmpty(rs.getString("visibility"), v -> dtype.setVisibility(DataType.Visibility.valueOf(v)));
                applyIfNotEmpty(rs.getString("desc"), dtype::setDesc);
                applyIfNotEmpty(rs.getBoolean("sortable"), dtype::setSortable);
                applyIfNotEmpty(rs.getBoolean("filterable"), dtype::setFilterable);
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

            } while (rs.next());
        } catch (SQLException e) {
            logger.error(e);
        }
        return 0;
    }


    public static List<DataType> getCols(ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        List<DataType> cols = new ArrayList<>();
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            String cname = rsmd.getColumnName(i);
            Class type = convertToClass(rsmd.getColumnType(i));
            DataType dt = new DataType(cname, type);

            // apply defaults to decimal numbers...
            // TODO: this need to be revisited..  it's a workaround to the %.6f default later down in the code if format is not given.
            if (type == Double.class || type == Float.class) {
                int scale = Math.max(rsmd.getScale(i), type == Double.class ? 10 : 7);
                dt.setPrecision("e" + scale); // double or float
            }
            cols.add(dt);
        }
        return cols;
    }


//====================================================================
//  privates functions
//====================================================================

    private static Class convertToClass(int val) {
        JDBCType type = JDBCType.valueOf(val);
        switch (type) {
            case CHAR:
            case VARCHAR:
            case LONGVARCHAR:
                return String.class;
            case TINYINT:
            case SMALLINT:
            case INTEGER:
                return Integer.class;
            case BIGINT:
                return Long.class;
            case FLOAT:
                return Float.class;
            case REAL:
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
