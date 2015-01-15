/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db.spring.mapper;


import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.DataType.FormatInfo;
import edu.caltech.ipac.util.StringUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DataGroupUtil {


    public static DataGroup createDataGroup(ResultSet rs) throws SQLException {
        List<DataType> extraData = getExtraData(rs.getMetaData());
        return new DataGroup("DataGroupTitle", extraData);
    }

    public static DataGroup processResults(ResultSet rs) throws SQLException {
        return processResults(rs, null);
    }

    public static DataGroup processResults(ResultSet rs, DataGroup dg) throws SQLException {
        return processResults(rs, dg, 0);
    }

    public static DataGroup processResults(ResultSet rs, DataGroup dg, int nullNum) throws SQLException {
        DataGroup dataGroup = dg == null ? createDataGroup(rs) : dg;
        DataObject dataObj;

        int numColumns = dataGroup.getDataDefinitions().length;
        int columnWidth [] = new int[numColumns];
        Arrays.fill(columnWidth, 0);

        // avoid firing property change events for each DataObject add
        dataGroup.beginBulkUpdate();

        Object obj;


        do {
            dataObj = new DataObject(dataGroup);
            for (int i = 0; i < numColumns; i++) {
                obj = rs.getObject(i + 1);
                DataType dt = dataGroup.getDataDefinitions()[i];
                if (obj == null) {
                    if (dt.getDataType() == Short.class) {
                        short s_value = (short) nullNum;
                        obj = new Short(s_value);
                    } else if (dt.getDataType() == Double.class) {
                        obj = new Double(nullNum);
                    } else if (dt.getDataType() == Float.class) {
                        obj = new Float(nullNum);
                    } else if (dt.getDataType() == Integer.class) {
                        obj = new Integer(nullNum);
                    } else if (dt.getDataType() == String.class) {
                        obj = "";
                    }
                }
                dataObj.setDataElement(dt, obj);
                if (obj instanceof String) {
                    columnWidth[i] = Math.max(columnWidth[i], obj.toString().length());
                }
            }
            dataGroup.add(dataObj);
        } while (rs.next());

        // will fire property change event
        dataGroup.endBulkUpdate();

        // update column width (for char columns only)
        DataType dt;
        FormatInfo fi;
        int displaySize;
        for (int i = 0; i < numColumns; i++) {
            if (columnWidth[i] > 0) {
                dt = dataGroup.getDataDefinitions()[i];
                dt.setMaxDataWidth(columnWidth[i]);
            }
        }


        return dataGroup;
    }

    public static List<DataType> getExtraData(ResultSetMetaData rsmd)
        throws SQLException {
        int numCols = rsmd.getColumnCount();
        List<DataType> extraData= new ArrayList<DataType>(numCols);
        DataType dataType = null;
        String columnName = null;
        Class columnClass = null;
        int displaySize;
        FormatInfo fi;
        try {
            for (int i=1; i<=numCols; i++) {
                columnName = rsmd.getColumnName(i);
                columnClass = Class.forName(rsmd.getColumnClassName(i));
                dataType= new DataType(columnName, columnName,
                                       columnClass,
                                       DataType.Importance.HIGH,
                                       "",     // no unit info
                                       true); // columns may be null

                displaySize = Math.max(columnName.length(), 6);
                displaySize = Math.max(displaySize, rsmd.getColumnDisplaySize(i));
                fi = dataType.getFormatInfo();
                fi.setWidth(displaySize);

                // format info - for numeric types only
                if (!(columnClass==String.class)) {
                    if (columnClass == Double.class || columnClass == Float.class) {
                        int scale = Math.max(rsmd.getScale(i), 6);
                        int prec = Math.max(rsmd.getPrecision(i), displaySize);
                        fi.setDataFormat("%" + prec + "." + scale + "f"); // double or float
                    } else if (Date.class.isAssignableFrom(columnClass)) {
                        fi.setDataFormat("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS"); // date
                    }
                    fi.setDataAlign(FormatInfo.Align.LEFT);
                    dataType.setFormatInfo(fi);
                }
                extraData.add(dataType);
            }

           // System.out.println("extraData: "+ extraData.toString());
            
        } catch (ClassNotFoundException ex) {
            throw new SQLException("Unreferenceable class name for column "+columnName+": "+ex.getMessage());
        }
        return extraData;
    }


    public static Map<String,Object> getHashMap (ResultSet rs) {
        Map<String, Object> map = new HashMap();

        try {
        List<DataType> extraData = getExtraData(rs.getMetaData());


        int numColumns = extraData.size();
        int columnWidth [] = new int[numColumns];
        Arrays.fill(columnWidth, 0);

        // avoid firing property change events for each DataObject add


        Object obj;


            for(int i=0; i<numColumns; i++) {
                obj = rs.getObject(i+1);
                if (obj == null) {
                     DataType dt = extraData.get(i);
                     if (dt.getDataType() == Short.class) {
                         short s_value = 0;
                         obj = new Short(s_value);
                     } else if (dt.getDataType() == Double.class) {
                          obj = new Double(0);
                     } else if (dt.getDataType() == Float.class) {
                          obj = new Float(0);
                     } else if (dt.getDataType() == Integer.class) {
                          obj = new Integer(0);
                     } else if (dt.getDataType() == String.class) {
                          obj = new String("NA");
                     } else {
                        obj = new String("na");
                     }
                }

                map.put(extraData.get(i).getKeyName(),obj);
                if (obj instanceof String) {
                    columnWidth[i] = Math.max(columnWidth[i], obj.toString().length());
                }
          }


        } catch (Exception e ) {
            System.out.println("getHashMap: "+e.getMessage());
        }
        return map;
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
