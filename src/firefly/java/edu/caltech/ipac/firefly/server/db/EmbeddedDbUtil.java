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
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.caltech.ipac.firefly.server.ServerContext.SHORT_TASK_EXEC;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_PATH;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_TYPE;
import static edu.caltech.ipac.firefly.server.db.DbAdapter.NULL_TOKEN;
import static edu.caltech.ipac.firefly.server.db.DbAdapter.ignoreCols;
import static edu.caltech.ipac.firefly.server.db.DbInstance.USE_REAL_AS_DOUBLE;
import static edu.caltech.ipac.table.DataGroup.ROW_IDX;
import static edu.caltech.ipac.table.TableMeta.DERIVED_FROM;
import static edu.caltech.ipac.util.StringUtils.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class EmbeddedDbUtil {
    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private static final int MAX_COL_ENUM_COUNT = AppProperties.getIntProperty("max.col.enum.count", 32);
    static final String DD_INSERT_SQL = "insert into %s_DD values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    static final String DD_CREATE_SQL = "create table %s_DD "+
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
            ", description varchar(64000)" +
            ", enumVals varchar(64000)" +
            ", ID       varchar(64000)" +
            ", precision varchar(64000)" +
            ", ucd      varchar(64000)" +
            ", utype    varchar(64000)" +
            ", ref      varchar(64000)" +
            ", maxValue varchar(64000)" +
            ", minValue varchar(64000)" +
            ", links    TEXT" +
            ", dataOptions varchar(64000)" +
            ", arraySize varchar(255)" +
            ", cellRenderer varchar(64000)" +
            ", sortByCols varchar(64000)" +
            ", order_index    int" +
            ")";
    static final String META_INSERT_SQL = "insert into %s_META values (?,?,?)";
    static final String META_CREATE_SQL = "create table %s_META "+
                    "(" +
                    "  key      varchar(1024)" +
                    ", value    varchar(64000)" +
                    ", isKeyword boolean" +
                    ")";
    static final String AUX_DATA_INSERT_SQL = "insert into %s_AUX values (?,?,?,?,?,?)";
    static final String AUX_DATA_CREATE_SQL = "create table %s_AUX "+
                            "(" +
                            "  title     varchar(64000)" +
                            ", size      int" +
                            ", groups    TEXT" +                        // we'll put serializable Java objects for these
                            ", links     TEXT" +
                            ", params    TEXT" +
                            ", resources TEXT" +
                            ")";

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
        return dbToDataGroup(rs, dg);
    }

    public static DataGroup dbToDataGroup(ResultSet rs, DataGroup dg) throws SQLException {
        try {
            advanceCursor(rs);
        } catch (Exception ignored) {
            return dg;                // no row found
        }
        List<Boolean> isAryType = isColumnTypeArray(dg.getDataDefinitions());
        do {
            DataObject row = new DataObject(dg);
            for (int i = 0; i < dg.getDataDefinitions().length; i++) {
                DataType dt = dg.getDataDefinitions()[i];
                Object val = convertToType(dt.getDataType(), rs, i, isAryType.get(i));  // ResultSet index starts from 1
                row.setDataElement(dt, val);
            }
            dg.add(row);
        } while (rs.next()) ;
        logger.trace("converting a %,d rows ResultSet into a DataGroup".formatted(dg.size()));
        return dg;
    }

    private static Object convertToType(Class clz, ResultSet rs, int idx, boolean isAry) throws SQLException {
        int cIdx = idx+1;      // ResultSet index starts from 1
        Object val = rs.getObject(cIdx);
        if (val == null)         return null;
        if (clz.isInstance(val)) return val;

        if (isAry) {
            if (val instanceof Array)   return val;    // we will assume the data type matches
            return deserialize(val.toString());        // handles base64 encoded Java serialized objects
        } else if (clz == String.class) {
            if (val instanceof Blob b) {
                return new String(b.getBytes(1, (int) b.length()), UTF_8);   // handles binary UTF-8 encoded string
            } else {
                return val.toString();
            }
        }else if (clz == Double.class) {
            return rs.getDouble(cIdx);
        } else if (clz == Float.class) {
            return rs.getFloat(cIdx);
        } else if (clz == Integer.class) {
            return rs.getInt(cIdx);
        } else if (clz == Long.class) {
            return rs.getLong(cIdx);
        } else if (clz == Short.class) {
            return rs.getShort(cIdx);
        } else if (clz == Byte.class) {
            return rs.getByte(cIdx);
        } else if (clz == Boolean.class) {
            return rs.getBoolean(cIdx);
        } else if (clz == Date.class) {
            if (val instanceof LocalDate d) return java.sql.Date.valueOf(d);        // date only
            if (val instanceof LocalDateTime d) return Date.from(d.atZone(UTC).toInstant());
        }
        throw new SQLException("Can't convert " + rs.getObject(cIdx) + " to " + clz);
    }

    public static List<DataType> getCols(ResultSet rs, DbInstance dbInstance) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        List<DataType> cols = new ArrayList<>();
        boolean useRealAsDouble = dbInstance.getBoolProp(USE_REAL_AS_DOUBLE, false);
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            String cname = rsmd.getColumnLabel(i);
            JDBCType type = JDBCType.valueOf(rsmd.getColumnType(i));
            boolean isArray = type == JDBCType.ARRAY;
            if (isArray) {
                String typeDesc = rsmd.getColumnTypeName(i).replace("[]", "");
                type = JDBCType.valueOf(typeDesc);
            }
            Class clz = convertToClass(type, useRealAsDouble);
            DataType dt = new DataType(cname, clz);
            if (isArray)    dt.setArraySize("*");
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

        String sql = "select %s from %s where %s in (%s)".formatted(selCols, tblName, DataGroup.ROW_NUM, inRows);
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
        return "(%s=%s) %s".formatted(DERIVED_FROM, derivedFrom, desc);     // prepend DERIVED_FROM value into the description.  This is how we determine if a column is derived.
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
                    .map(dt -> "count(distinct \"%s\") as \"%s\"".formatted(dt.getKeyName(), dt.getKeyName()))
                    .collect(Collectors.joining(", "));

            List<Map<String, Object>> rs = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance())
                    .queryForList("SELECT %s FROM %s limit 500".formatted(cols, dbAdapter.getDataTable()));

            List<Object[]> params = new ArrayList<>();
            rs.get(0).forEach( (cname,v) -> {
                Long count = (Long) v ;
                if (count > 0 && count <= MAX_COL_ENUM_COUNT) {
                    List<Map<String, Object>> vals = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance())
                            .queryForList("SELECT distinct \"%s\" FROM data order by 1".formatted(cname));

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
//  Using duckdb appender greatly improve performance when ingesting large volume of data.
//  But, direct BLOB support is not available.  Therefore, we will serialize Java object
//  into base64 string for storage.
//====================================================================

    public static String serialize(Object obj) {
        if (obj == null) return null;
        try {
            ByteArrayOutputStream bstream = new ByteArrayOutputStream();
            ObjectOutputStream ostream = new ObjectOutputStream(bstream);
            ostream.writeObject(obj);
            ostream.flush();
            byte[] bytes =  bstream.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            logger.warn(e);
            return null;
        }
    }

    public static Object deserialize(ResultSet rs, String cname) {
        return getSafe(() -> deserialize(rs.getString(cname)));
    }
    public static Object deserialize(ResultSet rs, int cidx) {
        return getSafe(() -> deserialize(rs.getString(cidx)));
    }

    public static Object deserialize(String base64) {
        try {
            if (base64 == null) return null;
            byte[] bytes = Base64.getDecoder().decode(base64);
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

    static Class convertToClass(JDBCType type, boolean useRealAsDouble) {
        return switch (type) {
            case CHAR, VARCHAR, LONGVARCHAR -> String.class;
            case TINYINT    -> Byte.class;
            case SMALLINT   -> Short.class;
            case INTEGER    -> Integer.class;
            case BIGINT     -> Long.class;
            case FLOAT  -> Float.class;
            case REAL -> useRealAsDouble ? Double.class : Float.class;
            case DOUBLE, NUMERIC, DECIMAL   -> Double.class;
            case BIT, BOOLEAN -> Boolean.class;
            case DATE, TIME, TIMESTAMP  -> Date.class;
            case BINARY, VARBINARY, LONGVARBINARY -> String.class;
            default -> String.class;
        };
    }

    public static DataType[] makeDbCols(DataGroup dg) {
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

    static void loadDataToDb(JdbcTemplate jdbc, String insertDataSql, DataGroup data) {
        int loaded = 0;
        int rows = data.size();
        while (loaded < rows) {
            int batchSize = Math.min(rows-loaded, 10000);   // set batchSize limit to 10k to  ensure HUGE table do not require unnecessary amount of memory to load
            final int roffset = loaded;
            loaded += batchSize;
            jdbc.batchUpdate(insertDataSql, new BatchPreparedStatementSetter() {
                final DataType[] cols = data.getDataDefinitions();
                List<Boolean> cIsAry = isColumnTypeArray(cols);
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    int ridx = roffset+i;
                    for (int cidx = 0; cidx < cols.length; cidx++)  {
                        Object v = data.getData(cols[cidx].getKeyName(), ridx);
                        if (cIsAry.get(cidx)) {
                            v = serialize(v);
                        }
                        ps.setObject(cidx+1, v);
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

    /**
     * Checks if the specified columns contain array data types
     * @param cols columns to search
     * @return  a list of boolean indicating if a column contains array data type
     */
    public static List<Boolean> isColumnTypeArray(DataType[] cols) {
        return Arrays.stream(cols).map(c -> c.isArrayType()).toList();
    }

    /**
     * @param cols columns to search
     * @return  a list of
     */
    public static List<Integer> colIdxWithArrayData(DataType[] cols) {
        return IntStream.range(0, cols.length)
                .mapToObj(i -> cols[i].isArrayType() ? i : -1)
                .filter(i -> i >= 0)
                .collect(Collectors.toList());
    }

}
