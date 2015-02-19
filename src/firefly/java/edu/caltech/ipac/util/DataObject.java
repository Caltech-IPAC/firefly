/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

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
    private Object _data[]= null;
    private String _formattedData[] = null;
    private final DataGroup _group;
    private int rowIdx = -1;

    public DataObject(DataGroup group) {
        _group= group;
    }

    public int getRowIdx() {
        DataType dtype = getDataType(DataGroup.ROWID_NAME);
        if (dtype != null && dtype.getColumnIdx() < _data.length) {
            try {
                return Integer.parseInt(String.valueOf(_data[dtype.getColumnIdx()]));
            } catch (Exception ex) {
                return rowIdx;
            }
        }
        return rowIdx;
    }

    public void setRowIdx(int rowIdx) {
        this.rowIdx = rowIdx;
    }

    public void setFormattedData(DataType fdt, String val) {
        checkSize();
        if (_formattedData == null || _formattedData.length != _data.length) {
            _formattedData = new String[_data.length];
        }
        _formattedData[fdt.getColumnIdx()] = val;
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

    public Object getDataElement(DataType fdt) {
        checkSize();
        if (fdt != null && fdt.getKeyName().equals(DataGroup.ROWID_NAME)) {
            return getRowIdx();
        } else {
            return _data[fdt.getColumnIdx()];
        }
    }

    /**
     * Returns a view of the data.  This method provide a "read-only" access to
     * this Object's data.
     *
     * @return a view of the data.
     */
    public Object[] getData() {
        checkSize();
        Object[] retval = new Object[_data.length];
        System.arraycopy(_data, 0, retval, 0, retval.length);
        return retval;
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
            fdata[idx] = dt.getFormatInfo().formatDataOnly(data[idx]);
        }
        return fdata;
    }

    public String getFormatedData(DataType dt) {
        int idx = dt.getColumnIdx();
        String val;
        if (_formattedData != null && _formattedData.length > idx && _formattedData[idx] != null) {
            val = _formattedData[idx];
        } else {
            Object v = getDataElement(dt);
            String strForNull = StringUtils.isEmpty(dt.getNullString()) ? "" : dt.getNullString();
            val = dt.getFormatInfo().formatData(v, strForNull);
        }
        return val;
    }

    public Object getDataElement(String name) {
        checkSize();
        DataType dtype = getDataType(name);
        if (dtype == null) {
            Assert.argTst(false, "The data element that you ask for, " +name +
                                 ", does not exist.\n" +
                                 _group.availableKeys());
        }
        return getDataElement(dtype);
    }

    public DataType getDataType(String name) {
        DataType[] types = getDataDefinitions();
        for (int i = 0; types != null && i < types.length; i++) {
            if ( types[i] != null && types[i].getKeyName().equals(name) ) {
                return types[i];
            }
        }
        return null;
    }

    public DataType[] getDataDefinitions() {
        return _group.getDataDefinitions();
    }

    public int size() { return _group.getDataDefinitions().length; }



//======================================================================
//------------------ Private / Protected / Package Methods --------------
//======================================================================

    private DataType findDT(String key) {
        DataType dataTypes[]= _group.getDataDefinitions();
        DataType retval= null;
        int idx= _group.getLastElementIdx();

        if (dataTypes.length>0 && dataTypes[idx].getKeyName().equals(key))  retval= dataTypes[idx];


        if (retval== null) {
            for(idx= 0; (idx<dataTypes.length && retval == null);idx++) {
                if (dataTypes[idx].getKeyName().equals(key)) {
                    retval= dataTypes[idx];
                    _group.setLastElementIdx(idx);
                }
            }
        }
        return retval;
    }

    private void checkSize() {
        if (_data==null) {
            _data= new Object[_group.getDataDefinitions().length];
        }
        else if (_data.length!=_group.getDataDefinitions().length) {
            DataType dataDefList[]= _group.getDataDefinitions();
            Assert.tst(dataDefList.length>_data.length);
            Object newData[]= new Object[dataDefList.length];
            System.arraycopy(_data,0,newData,0,_data.length);
            _data= newData;
        }
        else {
            // do nothing - everthing check outs
        }
    }

}
