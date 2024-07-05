/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

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

import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.server.ServerContext.SHORT_TASK_EXEC;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_PATH;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_TYPE;
import static edu.caltech.ipac.firefly.server.db.DbAdapter.NULL_TOKEN;
import static edu.caltech.ipac.firefly.server.db.DbAdapter.ignoreCols;
import static edu.caltech.ipac.firefly.server.db.DbInstance.USE_REAL_AS_DOUBLE;
import static edu.caltech.ipac.table.DataGroup.ROW_IDX;
import static edu.caltech.ipac.table.TableMeta.DERIVED_FROM;
import static edu.caltech.ipac.util.StringUtils.*;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class EmbeddedDbUtil {
    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private static final int MAX_COL_ENUM_COUNT = AppProperties.getIntProperty("max.col.enum.count", 32);

    public static void setDbMetaInfo(TableServerRequest treq, DbAdapter dbAdapter) {
        treq.setMeta(TBL_FILE_PATH, ServerContext.replaceWithPrefix(dbAdapter.getDbFile()));
        treq.setMeta(TBL_FILE_TYPE, dbAdapter.getName());
    }


    @NotNull
    public static String getUniqueID(TableServerRequest treq) {
        return SearchProcessor.getUniqueIDDef(treq);
    }



//====================================================================
//  common util functions
//====================================================================

    public static DataGroup dbToDataGroup(ResultSet rs, DbInstance dbInstance) throws SQLException {

        DataGroup dg = new DataGroup(null, getCols(rs, dbInstance));

        try {
            advanceCursor(rs);
        } catch (Exception ignored) {
            return dg;                // no row found
        }

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

    public static List<DataType> getCols(ResultSet rs, DbInstance dbInstance) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        List<DataType> cols = new ArrayList<>();
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            String cname = rsmd.getColumnLabel(i);
            Class type = EmbeddedDbUtil.convertToClass(rsmd.getColumnType(i), dbInstance);
            DataType dt = new DataType(cname, type);
            cols.add(dt);
        }
        return cols;
    }


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
        DbAdapter dbAdapter = proc.getDbAdapter(treq);
        String tblName = proc.getResultSetID(treq);
        String inRows = selRows != null && selRows.size() > 0 ? StringUtils.toString(selRows) : "-1";

        if (!dbAdapter.hasTable(tblName)) {
            try {
                // data does not exists.. recreate it
                new SearchManager().getDataGroup(treq);
            } catch (DataAccessException e1) {
                logger.error(e1);
            }
        }

        String sql = String.format("select %s from %s where %s in (%s)", selCols, tblName, DataGroup.ROW_NUM, inRows);
        return dbAdapter.execQuery(sql ,tblName);
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

    public static void updateColumn(DbAdapter dbAdapter, DataType dtype, String expression, String editColName, String preset, String resultSetID, SelectionInfo si) throws DataAccessException {
        dbAdapter.updateColumn(dtype, expression, editColName, preset, resultSetID, si);
    }

    public static void addColumn(DbAdapter dbAdapter, DataType dtype, String expression) throws DataAccessException {
        addColumn(dbAdapter, dtype, expression, null, null, null);
    }

    public static void addColumn(DbAdapter dbAdapter, DataType dtype, String expression, String preset, String resultSetID, SelectionInfo si) throws DataAccessException {
        dbAdapter.addColumn(dtype, -1, expression, preset, resultSetID, si);

    }

    static String getFieldDesc(DataType dtype, String expression, String preset) {
        String desc = isEmpty(dtype.getDesc()) ? "" : dtype.getDesc();
        String derivedFrom = isEmpty(preset) ? expression : "preset:" + preset;
        return String.format("(%s=%s) ", DERIVED_FROM, derivedFrom) + desc;     // prepend DERIVED_FROM value into the description.  This is how we determine if a column is derived.
    }

    public static void deleteColumn(DbAdapter dbAdapter, String cname) {
        dbAdapter.deleteColumn(cname);
    }

    public static void enumeratedValuesCheckBG(DbAdapter dbAdapter, DataGroupPart results, TableServerRequest treq) {
        RequestOwner owner = ServerContext.getRequestOwner();
        ServerEvent.EventTarget target = new ServerEvent.EventTarget(ServerEvent.Scope.SELF, owner.getEventConnID(),
                owner.getEventChannel(), owner.getUserKey());
        SHORT_TASK_EXEC.submit(() -> {
            enumeratedValuesCheck(dbAdapter, results, treq);
            DataGroup updates = new DataGroup(null, results.getData().getDataDefinitions());
            updates.getTableMeta().setTblId(results.getData().getTableMeta().getTblId());
            JSONObject changes = JsonTableUtil.toJsonDataGroup(updates);
            changes.remove("totalRows");        //changes contains only the columns with 0 rows.. we don't want to update totalRows

            FluxAction action = new FluxAction(FluxAction.TBL_UPDATE, changes);
            ServerEventManager.fireAction(action, target);
        });
    }
    public static void enumeratedValuesCheck(DbAdapter dbAdapter, DataGroupPart results, TableServerRequest treq) {
        StopWatch.getInstance().start("enumeratedValuesCheck: " + treq.getRequestId());
        enumeratedValuesCheck(dbAdapter, results.getData().getDataDefinitions());
        StopWatch.getInstance().stop("enumeratedValuesCheck: " + treq.getRequestId()).printLog("enumeratedValuesCheck: " + treq.getRequestId());
    }

    public static void enumeratedValuesCheck(DbAdapter dbAdapter, DataType[] inclCols) {
        if (inclCols != null && inclCols.length > 0)
        try {
            String cols = Arrays.stream(inclCols)
                    .filter(dt -> maybeEnums(dt))
                    .map(dt -> String.format("count(distinct \"%s\") as \"%s\"", dt.getKeyName(), dt.getKeyName()))
                    .collect(Collectors.joining(", "));

            List<Map<String, Object>> rs = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance())
                    .queryForList(String.format("SELECT %s FROM %s limit 500", cols, dbAdapter.getDataTable()));

            List<Object[]> params = new ArrayList<>();
            rs.get(0).forEach( (cname,v) -> {
                Long count = (Long) v ;
                if (count > 0 && count <= MAX_COL_ENUM_COUNT) {
                    List<Map<String, Object>> vals = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance())
                            .queryForList(String.format("SELECT distinct \"%s\" FROM data order by 1", cname));

                    DataType col = findColByName(inclCols, cname);
                    if (col != null && vals.size() <= MAX_COL_ENUM_COUNT) {
                        String enumVals = vals.stream()
                                .map(m -> m.get(cname) == null ? NULL_TOKEN : m.get(cname).toString())  // convert to list of value as string
                                .map(cn -> cn.contains(",") ? "'" + cn + "'" : cn)                      // if there's comma in the column name, enclose it with single quotes
                                .collect(Collectors.joining(","));                             // combine the values into a comma separated values string.
                        col.setEnumVals(enumVals);
                        params.add(new Object[]{enumVals, cname});
                    }
                }
            });
            // update dd table
            if (params.size() > 0)  dbAdapter.batchUpdate("UPDATE DATA_DD SET enumVals = ? WHERE cname = ?", params);
        } catch (Exception ex) {
            // do nothing.. ok to ignore errors.
        }
    }

    public static int dbToDD(DataGroup dg, ResultSet rs) {
        try {
            do {
                String cname = rs.getString("cname");
                DataType dtype = dg.getDataDefintion(cname, true);
                // if this column is not in DataGroup.  no need to update the info
                if (dtype != null) {
                    dbToDataType(dtype, rs);
                }
            } while (rs.next());
        } catch (SQLException e) {
            logger.error(e);
        }
        return 0;
    }

    public static void dbToDataType(DataType dtype, ResultSet rs) {
        try {
            applyIfNotEmpty(rs.getString("type"), (s) -> {
                dtype.setTypeDesc(s);
                dtype.setDataType(DataType.descToType(s, dtype.getDataType()));     // if desc is unknown, use what's in the database.
            });

            applyIfNotEmpty(rs.getString("label"), dtype::setLabel);
            applyIfNotEmpty(rs.getString("units"), dtype::setUnits);
            dtype.setNullString(rs.getString("null_str"));
            applyIfNotEmpty(rs.getString("format"), dtype::setFormat);
            applyIfNotEmpty(rs.getString("fmtDisp"), dtype::setFmtDisp);
            applyIfNotEmpty(rs.getInt("width"), dtype::setWidth);
            applyIfNotEmpty(rs.getString("visibility"), v -> dtype.setVisibility(DataType.Visibility.valueOf(v)));
            applyIfNotEmpty(rs.getString("description"), dtype::setDesc);
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
            applyIfNotEmpty(rs.getString("dataOptions"), dtype::setDataOptions);
            applyIfNotEmpty(rs.getString("arraySize"), dtype::setArraySize);
            applyIfNotEmpty(rs.getString("cellRenderer"), dtype::setCellRenderer);
            applyIfNotEmpty(rs.getString("sortByCols"), dtype::setSortByCols);

            if (ignoreCols.contains(dtype.getKeyName())) {
                dtype.setVisibility(DataType.Visibility.hide);
            }
        } catch (Exception e) {
            logger.warn(e);
        }
    }

//====================================================================
//  serialize/deserialize of Java object
//====================================================================

    public static byte[] serialize(Object obj) {
        if (obj == null) return null;
        try {
            ByteArrayOutputStream bstream = new ByteArrayOutputStream();
            ObjectOutputStream ostream = new ObjectOutputStream(bstream);
            ostream.writeObject(obj);
            ostream.flush();
            byte[] bytes =  bstream.toByteArray();
            return bytes;
        } catch (Exception e) {
            logger.warn(e);
            return null;
        }
    }

    public static Object deserialize(ResultSet rs, String cname) {
        try {
            Blob blob = rs.getBlob(cname);
            if (blob == null) return null;

            byte[] bytes = blob.getBytes(1, (int) blob.length());
            ByteArrayInputStream bstream = new ByteArrayInputStream(bytes);
            ObjectInputStream ostream = new ObjectInputStream(bstream);
            return ostream.readObject();
        } catch (Exception e) {
            logger.warn(e);
            return null;
        }
    }

//====================================================================
//  privates functions
//====================================================================


    private static DataType findColByName(DataType[] cols, String name) {
        for(DataType dt : cols) {
            if (dt.getKeyName().equals(name)) return dt;
        }
        return  null;
    }

    private static List<Class> onlyCheckTypes = Arrays.asList(String.class, Integer.class, Long.class, Character.class, Boolean.class, Short.class, Byte.class);
    private static List<String> excludeColNames = Arrays.asList(DataGroup.ROW_IDX, DataGroup.ROW_NUM);
    private static boolean maybeEnums(DataType dt) {
        return onlyCheckTypes.contains(dt.getDataType()) && !excludeColNames.contains(dt.getKeyName());

    }

    static Class convertToClass(int val, DbInstance dbInstance) {
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
            default:
                return String.class;
        }
    }

    static DataType[] makeDbCols(DataGroup dg) {
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

    static void advanceCursor(ResultSet rs) throws SQLException {
        try {
            if (rs.isBeforeFirst()) rs.next();
            if (!rs.isFirst()) throw new SQLWarning("No row found");
        } catch (Exception ignored) {
            // if jdbc driver does not support checking, assume cursor is before first row
            boolean hasData = rs.next();
            if (!hasData) throw new SQLWarning("No row found");
        }
    }
}
