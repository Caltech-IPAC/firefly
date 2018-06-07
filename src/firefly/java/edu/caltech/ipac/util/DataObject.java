/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import edu.caltech.ipac.firefly.server.util.QueryUtil;

import java.io.Serializable;

/**
 * @author Trey Roby
 * @version $Id: DataObject.java,v 1.10 2010/12/16 06:39:50 tatianag Exp $
 *
 */
public class DataObject implements Serializable, Cloneable {

//======================================================================
//----------------------- private / protected variables ----------------
//======================================================================
    private Object[] _data= null;
    private String[] _formattedData = null;
    private final DataGroup _group;
    private int _rowNum = -1;

    public DataObject(DataGroup group) {
        _group= group;
    }

    public int getRowNum() {
        return _rowNum;
    }

    public void setRowNum(int rowNum) {
        this._rowNum = rowNum;
    }

    public void setData(Object[] data) {
        _data = data;
    }

    /**
     * Returns a view of the data.  This method provide a "read-only" access to
     * this Object's data.
     *
     * @return a view of the data.
     */
    public Object[] getData() {
        checkSize();        // this expand the _data array if it's not large enough
        if (_data.length > _group.getDataDefinitions().length) {
            Object[] sparseAry = new Object[_group.getDataDefinitions().length];
            for(DataType dt : _group.getDataDefinitions()) {
                sparseAry[dt.getColumnIdx()] = _data[dt.getColumnIdx()];
            }
            return sparseAry;
        } else {
            return _data;
        }
    }

    public void setFixedFormattedData(DataType fdt, String val) {
        checkSize();
        if (_formattedData == null || _formattedData.length != _data.length) {
            _formattedData = new String[_data.length];
        }
        _formattedData[fdt.getColumnIdx()] = val;
    }

    /**
     * Returns a view of the data formated according to it format info.
     *
     * @return a view of the data.
     */
    public String[] getFormatedData() {
        Object[] data = getData();
        String[] fdata = new String[data.length];
        DataType[] types = getDataDefinitions();
        for(DataType dt : types) {
            int idx = dt.getColumnIdx();
            fdata[idx] =  dt.formatData(data[idx]);
        }
        return fdata;
    }

    public String getFormatedData(DataType dt) {
        Object v = getDataElement(dt);
        return dt.formatData(v);
    }

    public String getFixedFormatedData(DataType dt) {
        int idx = dt.getColumnIdx();
        String val = null;
        if (_formattedData != null && _formattedData.length > idx && _formattedData[idx] != null) {
            val = _formattedData[idx];
        }
        if (val == null) {
            Object v = getDataElement(dt);
            val = dt.formatData(v);
        }
        int w = dt.getWidth() > 0 ? dt.getWidth() : dt.getMaxDataWidth();
        if (val.length() != w) val = dt.fitValueInto(val, w, dt.isNumeric());
        return val;
    }

    public Object getDataElement(DataType fdt) {
        checkSize();
        Object rval = _data[fdt.getColumnIdx()];
        if (rval == null) {
            if (fdt.getKeyName().equals(DataGroup.ROW_IDX) || fdt.getKeyName().equals(DataGroup.ROW_NUM)) {
                // if these columns do not exists, return the current index of this row.
                rval = _rowNum;
            }
        }
        return rval;
    }

    public void setDataElement(String colName, Object fde) {
        setDataElement(getDataType(colName), fde);
    }

    public void setDataElement(DataType fdt, Object fde) {
        checkSize();
        Class dtypeClass= fdt.getDataType();
        if (fde !=null && dtypeClass!=null && !dtypeClass.isInstance(fde)) { // more optimal in if
            Assert.argTst(false,
                    "Parameter fde is instance of " +
                            fde.getClass().toString() +
                            ". The parameter fdt requires it to be an instance of "+
                            fdt.getDataType().toString()+ "\n" +
                            "DataType passed: " + fdt.toString() +"\n" +
                            "Data Value(fde): " + fde.toString());
        }

        _data[fdt.getColumnIdx()]=fde;
        if (_formattedData != null && _formattedData.length == _data.length) {
            _formattedData[fdt.getColumnIdx()]=null;
        }
    }

    public boolean containsKey(String key) {
        return  _group.containsKey(key);
    }

    public Object getDataElement(String name) {
        DataType dtype = getDataType(name);
        if (dtype == null) {
            Assert.argTst(false, "The data element that you ask for, " +name +
                                 ", does not exist.\n" +
                                 StringUtils.toString(_group.getKeySet()));
        }
        return getDataElement(dtype);
    }

    public DataType getDataType(String name) {
        return _group.getDataDefintion(name);
    }

    public DataType[] getDataDefinitions() {
        return _group.getDataDefinitions();
    }

    public int size() { return _group.getDataDefinitions().length; }

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




//======================================================================
//------------------ Private / Protected / Package Methods --------------
//======================================================================

    void checkSize() {
        if (_data==null) {
            _data= new Object[_group.getDataDefinitions().length];
        } else if (_data.length < _group.getDataDefinitions().length) {
            DataType[] dataDefList = _group.getDataDefinitions();
            Object newData[]= new Object[dataDefList.length];
            System.arraycopy(_data,0,newData,0,_data.length);
            _data= newData;
        }
    }

}
