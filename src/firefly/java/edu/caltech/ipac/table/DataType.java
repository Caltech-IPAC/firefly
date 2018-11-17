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
import java.util.regex.Pattern;

import static edu.caltech.ipac.util.StringUtils.isEmpty;
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
    private static final Pattern precisiontPattern = Pattern.compile("([EFG]?)(\\d*)", Pattern.CASE_INSENSITIVE);


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
        this.keyName = keyName == null ? null : keyName.replace("\"","");
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
        if (type != null && typeDesc == null) {
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

    public void setID (String id) {
        this.ID = id;
    }

    public String getID () {
        return ID;
    }

    /**
     * see edu.caltech.ipac.table.DataType#format for details
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

    public void setRef(String value) {
        this.ref = value;
    }

    public String getRef() {
        return ref;
    }

    public String getEnumVals() { return enumVals; }

    public void setEnumVals(String enumVals) { this.enumVals = enumVals;}


    /**
     * Firefly has 3 metas that affect the formatting of the column's data.  They are
     * listed below in order of highest to lowest precedence.
     *
     * fmtDisp		: A Java format string.  It can be used to format any type.  i.e.  "cost $%.2f"
     * format		: Same as fmtDisp
     * precision	: This only applies to floating point numbers.
     *                A string Tn where T is either F, E, or G
     *                If T is not present, it defaults to F.
     *                When T is F or E, n is the number of significant figures after the decimal point.
     *                When T is G, n is the number of significant digits
     *
     * @param value         the value to be formatted
     * @param useEscape     apply Java escaping to all strings to take care of control characters and quotes
     * @return
     */
    public String format(Object value, boolean useEscape) {
        if (value == null) {
            return getNullString() == null ? "" : getNullString();
        }
        // do escaping if requested
        if (useEscape && type == String.class) {
            value = escapeJava((String)value);
        }

        if (!isEmpty(getFmtDisp())) {
            return String.format(getFmtDisp(), value);

        } else if (!isEmpty(getFormat())) {
            return String.format(getFormat(), value);

        } else if (REAL_TYPES.contains(getTypeDesc())) {
            // use precision
            String prec = getPrecision();
            if (isEmpty(prec)) {
                String pattern = StringUtils.areEqual(getUnits(), "rad") ? "%.8f" : "%.6f";
                return String.format(pattern, value);
            }
            String[] tp = StringUtils.groupMatch(precisiontPattern, prec);    // T in group 0, and precision in group 1.
            if (tp != null) {
                String c = isEmpty(tp[0]) || tp[0].equals("F") ? "f" : tp[0];
                String p = isEmpty(tp[1]) ? "" : "." + tp[1];
                return String.format("%" + p + c, value);
            }
        } else if (Date.class.isAssignableFrom(getDataType())) {
            // default Date format:  yyyy-mm-dd hh:mm:ss ie. 2018-11-16 14:55:12
            return String.format("%1$tF %1$tT", value);
        }
        return String.valueOf(value);
    }

    /**
     * useEscape is true.  This is a common use case, since you don't want control characters to affect formatting
     * @param value
     * @return
     */
    public String format(Object value) {
        return format(value, true);
    }

    public String formatFixedWidth(Object value) {
        return fitValueInto(format(value), getWidth(), isNumeric());
    }

    /**
     * returns the formatted header of this column padded to max width
     * @return
     */
    public String formatHeader(String val) {
        val = val == null ? "" : val;
        return fitValueInto(val, getWidth(), false);
    }

    boolean isNumeric() {
        if (isNumeric == null) {
            isNumeric = NUMERIC_TYPES.contains(typeDesc);
        }
        return isNumeric;
    }

    /**
     * returns a string the size of the given width.  If the value is longer then the given width,
     * it will be truncated.  Values will be left-padded if numeric and right-padded otherwise.
     * @param value
     * @param width
     * @param isNumeric
     * @return
     */
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
                type ==Byte.class ||
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

        int w = getWidth();
        boolean useShortType = w > 0 && w < 6;
        if (type == null) type = String.class;

        String typeDesc = useShortType ? S_CHAR : CHAR;
        if (type.equals(Double.class))
            typeDesc = useShortType ? S_DOUBLE : DOUBLE;
        else if (type.equals(Float.class))
            typeDesc = useShortType ? S_FLOAT : FLOAT;
        else if (type.equals(Integer.class) || type.equals(Short.class) || type.equals(Byte.class))
            typeDesc = useShortType ? S_INTEGER : INTEGER;
        else if (type.equals(Long.class))
            typeDesc = useShortType ? S_LONG : LONG;
        else if (type.equals(Boolean.class))
            typeDesc = useShortType ? S_BOOL : BOOL;

        return typeDesc;
    }

    public static Class descToType(String type) {
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
    public void setLinkInfos(List<LinkInfo> vals) {
        links.clear();
        links.addAll(vals);
    }

}
