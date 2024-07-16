/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.server.util.QueryUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Trey Roby
 * @version $Id: DataObject.java,v 1.10 2010/12/16 06:39:50 tatianag Exp $
 *
 */
public class DataObject implements Serializable, Cloneable {

//======================================================================
//----------------------- private / protected variables ----------------
//======================================================================
    private final DataGroup group;
    private int rowNum = -1;
    private transient HashMap<String, Object> rowData;     // used to store a row of data.  In this mode, getter/setter operate on this data, not the data in the DataGroup.

    public DataObject(DataGroup group) {
        this.group = group;
        rowData = new HashMap<>();
    }

    private DataObject(DataGroup group, int rowNum) {
        this.group = group;
        this.rowNum = rowNum;
    }

    /**
     * return a DataObject that's backed by the data in the DataGroup.  Setter/getter will directly update the data in the DataGroup
     * @param source
     * @param rowNum
     * @return
     */
    static DataObject getDataAt(DataGroup source, int rowNum) {
        if (rowNum < source.size()) {
            return new DataObject(source, rowNum);
        } else {
            throw new IndexOutOfBoundsException(rowNum + " is greater then source DataGroup size:" + source.size());
        }
    }

    public int getRowNum() {
        return rowNum;
    }


    /**
     * Returns a view of the data.  This method provide a "read-only" access to
     * this Object's data.
     *
     * @return a view of the data.
     */
    public Object[] getData() {
        if (rowData != null) {
            return rowData.values().toArray(new Object[0]);
        } else {
            Object[] rval = new Object[group.getDataDefinitions().length];
            DataType[] cols = group.getDataDefinitions();
            for (int i=0; i <cols.length; i++) {
                rval[i] = group.getData(cols[i].getKeyName(), rowNum);
            }
            return rval;
        }
    }

    /**
     * Set the data for this DataObject(row)
     * @param data the full row of data in the same order as its columns
     */
    public void setData(Object[] data) {
        DataType[] cols = group.getDataDefinitions();
        for(int i = 0; i < cols.length; i++) {
            if (rowData != null) {
                rowData.put(cols[i].getKeyName(), data[i]);
            } else {
                group.setData(cols[i].getKeyName(), rowNum, data[i]);
            }
        }
    }

    public String getFixedFormatedData(DataType dt) {
        return dt.formatFixedWidth(getDataElement(dt));      // this is used by ipac table only
    }

    /**
     * @param replaceCtrl true to replace control charters.  Use this when writing out into format where multiline row is not supported, like csv and ipac table.
     * @return this row of data as strings, formatted according to its column's info.
     */
    public String[] getFormattedData(boolean replaceCtrl) {
        return Arrays.stream(group.getDataDefinitions())
                .map(dt -> dt.format(getDataElement(dt), replaceCtrl, true))
                .toArray(String[]::new);
    }

    public String[] getFormattedData() {
        return getFormattedData(false);
    }

    public Object getDataElement(DataType fdt) {
        return getDataElement(fdt.getKeyName());
    }

    public Object getDataElement(String colName) {
        if (colName != null) {
            if (rowData != null) {
                return rowData.get(colName);
            } else {
                return group.getData(colName, rowNum);
            }
        }
        return null;
    }

    public void setDataElement(String colName, Object fde) {
        if (colName != null) {
            if (rowData != null) {
                rowData.put(colName, fde);
            } else {
                group.setData(colName, rowNum, fde);
            }
        }
    }

    public void setDataElement(DataType fdt, Object fde) {
        setDataElement(fdt.getKeyName(), fde);
    }

    public boolean containsKey(String key) {
        return  group.containsKey(key);
    }

    public DataType getDataType(String name) {
        return group.getDataDefintion(name);
    }

    public DataType[] getDataDefinitions() {
        return group.getDataDefinitions();
    }

    public int size() { return group.getDataDefinitions().length; }

    public String getStringData(String name) {
        return getStringData(name, null);
    }
    public String getStringData(String name, String def) {
        Object v = getDataElement(name);
        return v == null ? def : v.toString();
    }

    public int getIntData(String name) { return QueryUtil.getInt(getDataElement(name)); }
    public int getIntData(String name, int def) {
        int v = getIntData(name);
        return v == Integer.MIN_VALUE ? def : v;
    }

    public float getFloat(String cname, float def) {
        Object v = getDataElement(cname);
        return v == null ? def :
               (v instanceof Number d)? d.floatValue() :
               Float.parseFloat(v.toString());
    }

    public double getDouble(String cname, double def) {
        Object v = getDataElement(cname);
        return v == null ? def :
               (v instanceof Number d)? d.doubleValue() :
               Double.parseDouble(v.toString());
    }
}
