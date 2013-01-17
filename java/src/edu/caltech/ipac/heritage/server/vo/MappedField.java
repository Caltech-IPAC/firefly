package edu.caltech.ipac.heritage.server.vo;

/**
 * @author tatianag
 *         $Id: MappedField.java,v 1.2 2010/07/01 20:21:12 tatianag Exp $
 */
public class MappedField {
    private String ucd;
    private String datatype;
    private String arraysize;
    private String mappedColumn;
    private String unit;

    // sometimes value is created using several values
    private VOFieldValueMapper mapper = null;

    public MappedField(String ucd, String datatype, String arraysize) {
        this(ucd, datatype, arraysize, null);
    }

    public MappedField(String ucd, String datatype, String arraysize, String mappedColumn) {
        this(ucd, datatype, arraysize, mappedColumn, null);
    }


    public MappedField(String ucd, String datatype, String arraysize, String mappedColumn, String unit) {
        this.ucd = ucd;
        this.datatype = datatype;
        this.arraysize = arraysize;
        this.mappedColumn = mappedColumn;
        this.unit = unit;
    }

    public void setMapper(VOFieldValueMapper mapper) {
        this.mapper = mapper;
    }

    public String getUcd() { return ucd; }
    public String getDatatype() { return datatype; }
    public String getArraysize() { return arraysize; }
    public String getMappedColumn() {return mappedColumn; }
    public String getUnit() {return unit; }
    public VOFieldValueMapper getMapper() { return mapper; }

}
