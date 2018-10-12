/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.util.HREF;
import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import static org.apache.commons.lang.StringEscapeUtils.escapeJava;
import static org.apache.commons.lang.StringEscapeUtils.unescapeJava;

public class DataType implements Serializable, Cloneable {

    public enum Visibility {show, hide, hidden};

    private static final String DOUBLE = "double";
    private static final String REAL = "real";

    private static final String FLOAT = "float";
    private static final String INTEGER = "int";
    private static final String LONG = "long";
    private static final String CHAR = "char";
    private static final String BOOL = "bool";
    private static final String S_DOUBLE = "d";
    private static final String S_REAL = "r";

    private static final String S_FLOAT = "f";
    private static final String S_INTEGER = "i";
    private static final String S_LONG = "l";
    private static final String S_CHAR = "c";
    private static final String S_BOOL = "b";
    public static final String LONG_STRING = "long_string";
    public static final List<String> NUMERIC_TYPES = Arrays.asList(DOUBLE, REAL, FLOAT, INTEGER, LONG, S_DOUBLE, S_REAL, S_FLOAT, S_INTEGER, S_LONG);
    public static final List<String> REAL_TYPES = Arrays.asList(DOUBLE, REAL, FLOAT, S_DOUBLE, S_REAL, S_FLOAT);
    public static final List<String> INT_TYPES = Arrays.asList(INTEGER, LONG, S_INTEGER, S_LONG);


    private       String keyName;
    private       String label;
    private       String typeDesc;
    private       Class type;
    private       String units;
    private       String nullString = "";
    private       String desc;
    private       int width;
    private       int prefWidth;
    private       boolean sortable = true;
    private       boolean filterable = true;
    private       Visibility visibility = Visibility.show;   // show, hide, or hidden
    private       String format;           // format string used for formating
    private       String fmtDisp;          // format string for diplay ... this is deprecated
    private       String sortByCols;       // comma-separated of column names
    private       String enumVals;         // comma-separated of distinct values
    private       String ID;
    private       String precision;
    private       String ucd;
    private       String utype;
    private       String ref = "";
    private       List<LinkInfo> links = new ArrayList<>();
    private       String maxValue = "";
    private       String minValue = "";
    private       String staticValue;


//    private transient PrimitiveList data;       // column-based data
    private transient int maxDataWidth = 0;     // this is the max width of the data...from reading the file.  only used by shrinkToFit
    private transient Boolean isNumeric;        // cached to improve formatting performance.


    public DataType(String keyName,
                    Class  type) {
        this(keyName, type, null, null, null, null);
    }

    public DataType(String keyName,
                    String label,
                    Class  type) {
        this(keyName, type, label, null, null, null);
    }

    public DataType(String keyName, Class type, String label, String units, String nullString, String desc) {
        // our db engine does not allow quotes in column names
        this.keyName = keyName.replace("\"","");
        setDataType(type);
        this.units = units;
        this.label = label;
        this.nullString = nullString;
        this.desc = desc;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getTypeDesc() {
        if (typeDesc == null) {
            typeDesc = resolveTypeDesc();
        }
        return typeDesc;
    }

    public void setTypeDesc(String typeDesc) {
        this.typeDesc = typeDesc;
    }

    public Class getDataType() {
        return type;
    }

    public void setDataType(Class type) {
        this.type = type;
        if (typeDesc == null) {
            typeDesc = resolveTypeDesc();
        }
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getNullString() {
        return nullString;
    }

    public void setNullString(String nullString) {
        this.nullString = nullString;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getPrefWidth() {
        return prefWidth;
    }

    public void setPrefWidth(int prefWidth) {
        this.prefWidth = prefWidth;
    }

    public boolean isSortable() {
        return sortable;
    }

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    public boolean isFilterable() {
        return filterable;
    }

    public void setFilterable(boolean filterable) {
        this.filterable = filterable;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFmtDisp() {
        return fmtDisp;
    }

    public void setFmtDisp(String fmtDisp) {
        this.fmtDisp = fmtDisp;
    }

    public String getSortByCols() {
        return sortByCols;
    }

    public void setSortByCols(String sortByCols) {
        this.sortByCols = sortByCols;
    }

    public int getMaxDataWidth() { return maxDataWidth; }

    public void setMaxDataWidth(int maxDataWidth) { this.maxDataWidth = maxDataWidth; }


    public void setID (String id) {
        this.ID = id;
    }

    public String getID () {
        return ID;
    }

    /**
     * set precision for numeric data, En or Fn
     *          En: n means number of significant figures
     *          Fn: n means the significant figures after decimal point
     * @param prec precision string
     */
    public void setPrecision(String prec) {
        this.precision = prec;
    }

    public String getPrecision() {
        return precision;
    }

    public void setUCD(String ucd) {
        this.ucd = ucd;
    }

    public String getUCD() {
        return ucd;
    }

    public void setUType(String utype) {
        this.utype = utype;
    }

    public String getUType() {
        return utype;
    }

    public void setMaxValue(String max) {
        this.maxValue = max;
    }

    public String getMaxValue() {
        return maxValue;
    }

    public void setMinValue(String min) {
        this.minValue = min;
    }

    public String getMinValue() {
        return minValue;
    }

    public void setValue(String value) {
        this.staticValue = value;
    }

    public String getValue() {
        return staticValue;
    }

    public void setRef(String value) {
        this.ref = value;
    }

    public String getRef() {
        return ref;
    }

    public String getEnumVals() { return enumVals; }

    public void setEnumVals(String enumVals) { this.enumVals = enumVals;}


    /**
     * returns the formatted header of this column padded to max width
     * @return
     */
    public String formatHeader(String val) {
        val = val == null ? "" : val;
        return fitValueInto(val, getMaxDataWidth(), false);
    }

    /**
     * returns the formatted string of the given value padded to the column's defined width
     * @param value
     * @return
     */
    public String formatData(Object value) {
        return formatData(value, false);
    }

    /**
     * returns the formatted string of the given value padded to the column's defined width
     * @param value
     * @param useEscape apply Java escaping to all strings to take care of control characters and quotes
     * @return
     */
    public String formatData(Object value, boolean useEscape) {
        String sval;
        if (value == null) {
            sval = getNullString() == null ? "" : getNullString();
        } else {
            if (Date.class.isAssignableFrom(getDataType())) {
                // format Date into string first.
                value = String.format("%1$tF %1$tT", value);
            }

            if (useEscape && type == String.class) {
                value = escapeJava((String)value);
            }
            String formatStr = getFmtDisp();
            formatStr = formatStr == null ? getFormat() : formatStr;
            if (formatStr == null) {
                formatStr = this.getFormatStr(0);
                setFormat(formatStr);
            }
            sval = formatStr.equals("%s") ? String.valueOf(value) : String.format(formatStr, value);
        }
        return sval;
    }

    boolean isNumeric() {
        if (isNumeric == null) {
            isNumeric = NUMERIC_TYPES.contains(typeDesc);
        }
        return isNumeric;
    }

    public static String fitValueInto(String value, int width, boolean isNumeric) {
        if (width == 0 || value.length() == width) return value;
        if (value.length() < width) {
            StringUtils.Align align = isNumeric ? StringUtils.Align.RIGHT : StringUtils.Align.LEFT;
            return StringUtils.pad(width, value, align);
        } else {
            value = value.trim();
            if (value.length() > width) value = value.substring(0, width);
            StringUtils.Align align = isNumeric ? StringUtils.Align.RIGHT : StringUtils.Align.LEFT;
            return StringUtils.pad(width, value, align);
        }
    }

    public String getFormatStr(int precision) {
        String fmtStr = "%s";
        if (REAL_TYPES.contains(getTypeDesc())) {
            if (precision <= 0) {
                precision =  (StringUtils.areEqual(getUnits(), "rad")) ? 8 : 6;
            }
            fmtStr = "%." + precision + "f";
        } else if (INT_TYPES.contains(getTypeDesc())) {
            fmtStr = "%d";
        }
        return fmtStr;
    }

    public boolean isKnownType() {
        return (type ==Boolean.class ||
                type ==String.class  ||
                type ==Double.class  ||
                type ==Float.class   ||
                type ==Integer.class ||
                type ==Short.class ||
                type ==Long.class ||
                type ==HREF.class
        );
    }

    public Object convertStringToData(String s) {
        return convertStringToData(s, false);
    }

    /**
     * Convert a string into an object
     * @param s string representation of an object
     * @param useUnescape - if true Java unescaping is applied to all strings
     * @return an object
     */
    public Object convertStringToData(String s, boolean useUnescape) {
        if (s == null || s.length() == 0 || s.equalsIgnoreCase("null")) return null;
        if (nullString != null && nullString.equals(s)) return null;

        if (useUnescape && type==String.class) {
            return unescapeJava(s);
        }

        Object retval= s;
        try {
             if (type ==Boolean.class) {
                 retval= Boolean.valueOf(s);
             }
             else if (type ==String.class) {
                 retval= useUnescape ? unescapeJava(s) : s;
             }
             else if (type ==Double.class) {
                 retval= new Double(s);
             }
             else if (type ==Float.class) {
                 retval= new Float(s);
             }
             else if (type ==Integer.class) {
                 retval= new Integer(s);
             }
             else if (type ==Short.class) {
                 retval = new Short(s);
             }
             else if (type ==Long.class) {
                 retval = new Long(s);
             }
             else if (type ==Byte.class) {
                 retval = new Byte(s);
             }
             else if (type ==HREF.class) {
                 retval = HREF.parseHREF(s);
             }
        } catch (NumberFormatException e) {
            retval= null;
        } catch (IllegalArgumentException iae) {
            retval = null;
        }
        return retval;
    }

    private String resolveTypeDesc() {

        int w = getWidth() > 0 ? getWidth() : getMaxDataWidth();
        boolean useShortType = w > 0 && w < 6;
        if (type == null) type = String.class;

        String typeDesc = useShortType ? S_CHAR : CHAR;
        if (type.equals(Double.class))
            typeDesc = useShortType ? S_DOUBLE : DOUBLE;
        else if (type.equals(Float.class))
            typeDesc = useShortType ? S_FLOAT : FLOAT;
        else if (type.equals(Integer.class) || type.equals(Short.class))
            typeDesc = useShortType ? S_INTEGER : INTEGER;
        else if (type.equals(Long.class))
            typeDesc = useShortType ? S_LONG : LONG;
        else if (type.equals(Boolean.class))
            typeDesc = useShortType ? S_BOOL : BOOL;

        return typeDesc;
    }

    public static Class parseDataType(String type) {
        switch (type) {
            case DOUBLE:
            case S_DOUBLE:
            case REAL:
            case S_REAL:
                return Double.class;
            case FLOAT:
            case S_FLOAT:
                return Float.class;
            case LONG:
            case S_LONG:
                return Long.class;
            case INTEGER:
            case S_INTEGER:
                return Integer.class;
            case BOOL:
            case S_BOOL:
                return Boolean.class;
            default:
                return String.class;
        }
    }

    public String toString() {
        return String.format("{Key: %s, Label: %s, Type: %s, TypeDesc: %s, Units: %s}",
                getKeyName(), getLabel(), getDataType(), getTypeDesc(), getDesc(), getUnits());
    }

    public Object clone() throws CloneNotSupportedException {
        DataType rval = (DataType) super.clone();
        rval.isNumeric = null;
        return rval;
    }

    /**
     * convenience clone method to avoid catching exception and casting
     * @return
     */
    public DataType newCopyOf() {
        try {
            return (DataType) clone();
        } catch (CloneNotSupportedException e) {
            return null; // should not happen;
        }
    }


    /**
     * get LinkInfo list
     * @return a list of LinkInfo
     */
    public List<LinkInfo> getLinkInfos() {
        return links;
    }

}
