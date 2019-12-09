/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db.spring.mapper;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


public class DataGroupUtil {
    public static DataGroup createDataGroup(ResultSet rs) throws SQLException {
        List<DataType> extraData = getExtraData(rs.getMetaData());
        return new DataGroup("DataGroupTitle", extraData);
    }
    public static DataGroup processResults(ResultSet rs) throws SQLException {
        return processResults(rs, null, Integer.MIN_VALUE);
    }
    public static DataGroup processResults(ResultSet rs, DataGroup dg) throws SQLException {
        return processResults(rs, dg, Integer.MIN_VALUE);
    }
    public static DataGroup processResults(ResultSet rs, DataGroup dg, int nullNum) throws SQLException {
        DataGroup dataGroup = dg == null ? createDataGroup(rs) : dg;
        DataObject dataObj;
        int numColumns = dataGroup.getDataDefinitions().length;
        if (rs.isBeforeFirst()) {
            rs.next();
        }
        if (!rs.isFirst()) return dataGroup;    // no row found
        Object obj;
        do {
            dataObj = new DataObject(dataGroup);
            for (int i = 0; i < numColumns; i++) {
                DataType dt = dataGroup.getDataDefinitions()[i];
                int idx = i+1;
                // from edu.caltech.ipac.util.DataType.isKnownType()
                switch (dt.getDataType().getSimpleName()) {
                    case "Boolean":
                        obj = rs.getBoolean(idx);
                        break;
                    case "Date":// OR should we get instead String:
                        obj = String.format(dt.getFormat(),rs.getObject(idx));//rs.getTimestamp(idx);
                        break;
                    case "String":
                        obj = rs.getString(idx);
                        break;
                    case "Double":
                        obj = rs.getDouble(idx);
                        break;
                    case "Float":
                        obj = rs.getFloat(idx);
                        break;
                    case "Integer":
                        obj = rs.getInt(idx);
                        break;
                    case "Short":
                        obj = rs.getShort(idx);
                        break;
                    case "Long":
                        obj = rs.getLong(idx);
                        break;
                    case "HREF":
                        obj = rs.getString(idx);
                        break;
                    default:
                        obj = rs.getObject(idx);
                }
                if (rs.wasNull()) {
                    obj = (nullNum != Integer.MIN_VALUE && obj instanceof Number) ? nullNum : null;
                }
                dataObj.setDataElement(dt, obj);
            }
            dataGroup.add(dataObj);
        } while (rs.next());
        return dataGroup;
    }
    public static List<DataType> getExtraData(ResultSetMetaData rsmd) throws SQLException {
        int numCols = rsmd.getColumnCount();
        List<DataType> extraData = new ArrayList<DataType>(numCols);
        DataType dataType = null;
        String columnName = null;
        Class columnClass = null;
        for (int i = 1; i <= numCols; i++) {
            columnName = rsmd.getColumnName(i);
            columnClass = convertToClass(rsmd.getColumnType(i));
            dataType = new DataType(columnName, columnName, columnClass);
            // format info - for numeric types only
            if (!(columnClass == String.class)) {
                if (columnClass == Double.class || columnClass == Float.class) {
                    int scale = Math.max(rsmd.getScale(i), columnClass == Double.class ? 10 : 7);
                    dataType.setFormat("%." + scale + "e"); // double or float
                } else if (Date.class.isAssignableFrom(columnClass)) {
                    //this doesn't work in the ui, it will display trueY-truem-etc...
                    //dataType.setFormat("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS"); // date
                    // dataType.setDataType(String.class);
                }
            }
            extraData.add(dataType);
        }
        return extraData;
    }
    private static Class convertToClass(int columnType) {
        switch (columnType) {
            case Types.BIGINT:
            case Types.ROWID:
                return Long.class;
            case Types.BIT:
            case Types.INTEGER:
            case Types.SMALLINT:
                return Integer.class;
            case Types.BOOLEAN:
                return Boolean.class;
            case Types.DECIMAL:
            case Types.DOUBLE:
            case Types.REAL:
                return Double.class;
            case Types.FLOAT:
            case Types.NUMERIC:
                return Float.class;
            default:
                return String.class;
        }
    }
    public static Comparator<String> getComparator(final DataType dt) {
        Comparator<String> comparator = new Comparator<String>(){
            public int compare(String s, String s1) {
                Comparable c1, c2;
                if (Number.class.isAssignableFrom(dt.getDataType())) {
                    s = StringUtils.isEmpty(s) || s.equalsIgnoreCase("null") ? "0" : s;
                    s1 = StringUtils.isEmpty(s1) || s1.equalsIgnoreCase("null") ? "0" : s1;
                    c1 = Double.parseDouble(s);
                    c2 = Double.parseDouble(s1);
                } else if (Date.class.isAssignableFrom(dt.getDataType())) {
                    c1 = StringUtils.isEmpty(s) || s.equalsIgnoreCase("null") ? new Date(0) : new Date(s);
                    c2 = StringUtils.isEmpty(s1) || s1.equalsIgnoreCase("null") ? new Date(0) : new Date(s1);
                } else {
                    s = s == null || s.equalsIgnoreCase("null") ? "" : s;
                    s1 = s1 == null || s1.equalsIgnoreCase("null") ? "" : s1;
                    c1 = s;
                    c2 = s1;
                }
                return c1.compareTo(c2);
            }
        };
        return comparator;
    }
}