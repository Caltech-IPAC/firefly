/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.astro.CoordUtil;
import edu.caltech.ipac.util.HREF;
import edu.caltech.ipac.util.StringUtils;
import org.json.simple.JSONArray;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.caltech.ipac.table.TableMeta.DERIVED_FROM;
import static edu.caltech.ipac.table.TableUtil.foldAry;
import static edu.caltech.ipac.util.StringUtils.*;
import static org.apache.commons.lang.StringUtils.stripEnd;

/**
 * Table below taken from https://www.ivoa.net/documents/VOTable/20191021/REC-VOTable-1.4-20191021.html#ToC11
 * Added IPACTable and Firefly to show how these data types are mapped
 * VOTable         FITS  Bytes   Meaning              IPACTable   Firefly   NOTES
 * --------        ----  -----   -------              ---------   -------   -----------
 * boolean         L     1       Logical              char        boolean
 * bit             X     *       Bit                  int         boolean
 * unsignedByte    B     1       Byte (0 to 255)      int         short
 * short           I     2       Short Integer        int         short
 * int             J     4       Integer              int         int
 * long            K     8       Long integer         long        long
 * char            A     1       ASCII Character      char        char
 * unicodeChar           2       Unicode Character    char        char
 * float           E     4       Floating point       float       float
 * double          D     8       Double               double      double
 * floatComplex    C     8       Float Complex        char        float[]   ordered pair of float [real, imag]   (not implemented, yet)
 * doubleComplex   M     16      Double Complex       char        double[]  ordered pair of double [real, imag]  (not implemented, yet)
 * char            A     8       Date                 date        date      Date is not a type for VOTable FITS.
 */


public class DataType implements Serializable, Cloneable {

    public enum Visibility {show, hide, hidden};

    // firefly data types
    public static final String BOOLEAN = "boolean";
    public static final String BYTE = "byte";
    public static final String SHORT = "short";
    public static final String INT = "int";
    public static final String INTEGER = "integer";
    public static final String LONG = "long";
    public static final String CHAR = "char";
    public static final String FLOAT = "float";
    public static final String DOUBLE = "double";
    public static final String DATE = "date";
    public static final String TIME = "time";

    // VOTable mapped types; use lower case for comparison,
    public static final String BIT = "bit";
    public static final String UNSIGNED_BYTE = "unsignedbyte";
    public static final String UNI_CHAR = "unicodechar";
    public static final String COMPLEX_FLOAT = "floatcomplex";
    public static final String COMPLEX_DOUBLE = "doublecomplex";

    // IPAC-Table mapped types
    public static final String REAL = "real";      // IPAC table
    public static final String LOCATION = "location";

    // database mapped types
    public static final String BIGINT = "bigint";      // IPAC table
    public static final String TINYINT = "tinyint";
    public static final String SMALLINT = "smallint";

    private static final List<Class<?>> FLOATING_TYPES = List.of(Double.class, Float.class);
    private static final List<Class<?>> INT_TYPES = List.of(Short.class, Integer.class, Long.class);
    public static final List<Class<?>> NUMERIC_TYPES = Stream.concat(FLOATING_TYPES.stream(), INT_TYPES.stream()).collect(Collectors.toList());
    private static final Pattern precisiontPattern = Pattern.compile("^(HMS|DMS|[EFG])?(\\d*)$", Pattern.CASE_INSENSITIVE);


    private       String keyName;
    private       String label;
    private       String typeDesc;
    private       Class<?> type;
    private       String units;
    private       String nullString;
    private       String desc;
    private       int width;
    private       int prefWidth;
    private       boolean sortable = true;
    private       boolean filterable = true;
    private       boolean fixed = false;
    private       Visibility visibility = Visibility.show;   // show, hide, or hidden
    private       String format;           // format string used for formating
    private       String fmtDisp;          // format string for diplay ... this is deprecated
    private       String sortByCols;       // comma-separated of column names
    private       String enumVals;         // comma-separated of distinct values
    private       String ID;
    private       String precision;
    private       String ucd;
    private       String utype;
    private       String xtype;
    private       String ref = "";
    private       List<LinkInfo> links = new ArrayList<>();
    private       String maxValue = "";
    private       String minValue = "";
    private       String dataOptions;
    private       String cellRenderer;      // use custom predefined cell renderer.  See TableRenderer.js for usage.

    /**
     * follows section 2.2. of the "VOTable Format Definition" convention where arraySize can be
     *      '*': variable length
     *      'n': where n is an integer corresponding to the fixed length of the array
     *      'n*': array with length of 0 to n
     *      'nxnxn*' : multidimensional array where the last dimension may be variable, ie. 64x64x5
     *  the data will be stored as a single array.  The information in arraySize can be used to "fold/unfold" the value.
     */
    private       String   arraySize;

    private transient Boolean isFloatingPoint;      // cached to improve formatting performance.
    private transient Boolean isWholeNumber;        // cached to improve formatting performance.


    public DataType(String keyName,
                    Class<?>  type) {
        this(keyName, type, null, null, null, null);
    }

    public DataType(String keyName,
                    String label,
                    Class<?>  type) {
        this(keyName, type, label, null, null, null);
    }

    public DataType(String keyName, Class<?> type, String label, String units, String nullString, String desc) {
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

    public Class<?> getDataType() {
        return type;
    }

    public void setDataType(Class<?> type) {
        this.type = type;
        if (type != null && typeDesc == null) {
            typeDesc = typeToDesc(type);
        }
    }

    public String getUnits() {
        return units;
    }
    public void setUnits(String units) {
        this.units = units;
    }

    public String getNullString() {
        return nullString == null ? "" : nullString;
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

    public boolean isFilterable() { return filterable;}
    public void setFilterable(boolean filterable) {
        this.filterable = filterable;
    }

    public boolean isFixed() { return fixed;}
    public void setFixed(boolean fixed) {
        this.fixed = fixed;
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

    public void setXType(String xtype) {
        this.xtype = xtype;
    }

    public String getXType() {
        return this.xtype;
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

    public String getDataOptions() { return dataOptions; }

    public void setDataOptions(String options) {this.dataOptions = options;}

    public String getArraySize() { return arraySize; }

    public void setArraySize(String arraySize) { this.arraySize = arraySize; }

    public String getCellRenderer() { return cellRenderer; }

    public void setCellRenderer(String cellRenderer) { this.cellRenderer = cellRenderer; }

    /**
     * Firefly has 3 metas that affect the formatting of the column's data.  They are
     * listed below in order of highest to lowest precedence.
     *
     * fmtDisp      : A Java format string.  It can be used to format any type.  i.e.  "cost $%.2f"
     * format       : Same as fmtDisp
     * precision    : This only applies to floating point numbers.
     *                A string Tn where T is either F, E, G, HMS, or DMS
     *                If T is not present, it defaults to F.
     *                When T is F, E, HMS, or DMS, n is the number of significant figures after the decimal point.
     *                When T is G, n is the number of significant digits
     *
     * @param value         the value to be formatted
     * @param replaceCtrl   replace control characters in strings with a replacement character
     * @param aryAsJson     true to format array as JSON string.  Otherwise, it will be formatted as space separated.
     * @return
     */
    public String format(Object value, boolean replaceCtrl, boolean aryAsJson) {
        if (value == null) return getNullString();

        if (value.getClass().isArray()) {
            if (aryAsJson) {
                JSONArray ary = new JSONArray();
                ary.addAll(foldAry(this, value));
                value = ary.toJSONString();
            } else {
                if (value instanceof String[]) {
                    // because starlink auto right-trim char arrays, we need to right pad them
                    int[] shape = getShape();
                    if (shape.length > 0) {
                        value = Arrays.stream((String[])value).map( s -> StringUtils.pad(shape[0], s)).toArray();
                    }
                    value = StringUtils.toString(TableUtil.aryToList(value), "");
                } else {
                    value = StringUtils.toString(TableUtil.aryToList(value), " ");
                }
            }
        }

        // do escaping if requested
        if (replaceCtrl && value instanceof String) {
            value = replaceCtrl((String)value);
        }

        if (!isEmpty(getFmtDisp())) {
            return String.format(getFmtDisp(), value);

        } else if (!isEmpty(getFormat())) {
            return String.format(getFormat(), value);

        } else if (FLOATING_TYPES.contains(getDataType())) {
            // use precision
            String prec = getPrecision();
            if (!isEmpty(prec)) {
                String[] tp = StringUtils.groupMatch(precisiontPattern, prec);    // T in group 0, and precision in group 1.
                if (tp != null) {
                    String c = isEmpty(tp[0]) || tp[0].equals("F") ? "f" : tp[0];
                    int p = isEmpty(tp[1]) ? 0 : Integer.parseInt(tp[1]);
                    try {
                        if (c.toUpperCase().equals("HMS")) {
                            return CoordUtil.dd2sex(Double.parseDouble(String.valueOf(value)), false, true, 5);  // set to 5 to matches client's default
                        } else if (c.toUpperCase().equals("DMS")) {
                            return CoordUtil.dd2sex(Double.parseDouble(String.valueOf(value)), true, true, 5);   // set to 5 to matches client's default
                        } else {
                            return String.format("%" + "." + p + c, value);
                        }
                    } catch (Exception e) { return "NaN"; }
                }
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
        return format(value, false, true);
    }

    public String formatFixedWidth(Object value) {
        return fitValueInto(format(value, true, false), getWidth(), isNumeric());
    }

    /**
     * returns the formatted header of this column padded to max width
     * @return
     */
    public String formatHeader(String val) {
        val = val == null ? "" : val;
        return fitValueInto(val, getWidth(), false);
    }

    public boolean isNumeric() {
        return isFloatingPoint() || isWholeNumber();
    }

    public boolean isFloatingPoint() {
        if (isFloatingPoint == null) {
            if (type == null) {
                System.out.println();
            }
            isFloatingPoint = FLOATING_TYPES.contains(type);
        }
        return isFloatingPoint;
    }

    public boolean isWholeNumber() {
        if (isWholeNumber == null) {
            isWholeNumber = INT_TYPES.contains(type);
        }
        return isWholeNumber;
    }

    public int[] getShape() {
        if (StringUtils.isEmpty(getArraySize())) return new int[0];
        return Arrays.stream(getArraySize().split("x"))
                .mapToInt(d -> StringUtils.getInt(d, -1))
                .toArray();
    }

    public boolean isArrayType() {
        int[] shape = getShape();
        if (getDataType() == String.class) {
            return shape.length -1 > 0;
        } else {
            return shape.length > 0;
        }
    }

    public String getTypeLabel() {
        return getTypeDesc() + (isArrayType() ? "[" + getArraySize() + "]" : "");
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

    /**
     * Convert a string into an object
     * @param s string representation of an object
     * @return an object
     */
    public Object convertStringToData(String s) {
        if (s == null || getNullString().equals(s)) return null;

        Object retval = s;
        if (StringUtils.isEmpty(getArraySize())) {
            retval = strToObject(s);
        } else if (type == String.class) {
            // char array.. fold first dimension into a string
            int[] shape = getShape();
            if (shape.length > 1 && shape[0] > 0) {
                return split(s, shape[0]).stream().map( v -> stripEnd(v, " ")).toArray(String[]::new);
            } else {
                return s;
            }
        } else {
            return strToPrimitiveAry(s.split(" "));
        }

        return retval;
    }

    private Object strToPrimitiveAry(String[] strAry) {

        try {
            if (type == Boolean.class) {
                boolean[] ary = new boolean[strAry.length];
                for (int i=0; i<strAry.length; i++) ary[i] = Boolean.parseBoolean(strAry[i]);
                return ary;
            } else if (type == Double.class) {
                double[] ary = new double[strAry.length];
                for (int i=0; i<strAry.length; i++) ary[i] = Double.parseDouble(strAry[i]);
                return ary;
            } else if (type == Float.class) {
                float[] ary = new float[strAry.length];
                for (int i=0; i<strAry.length; i++) ary[i] = Float.parseFloat(strAry[i]);
                return ary;
            } else if (type == Integer.class) {
                int[] ary = new int[strAry.length];
                for (int i=0; i<strAry.length; i++) ary[i] = Integer.parseInt(strAry[i]);
                return ary;
            } else if (type == Short.class) {
                short[] ary = new short[strAry.length];
                for (int i=0; i<strAry.length; i++) ary[i] = Short.parseShort(strAry[i]);
                return ary;
            } else if (type == Long.class) {
                long[] ary = new long[strAry.length];
                for (int i=0; i<strAry.length; i++) ary[i] = Long.parseLong(strAry[i]);
                return ary;
            } else if (type == Byte.class) {
                byte[] ary = new byte[strAry.length];
                for (int i=0; i<strAry.length; i++) ary[i] = Byte.parseByte(strAry[i]);
                return ary;
            }
        } catch (Exception ignored) {}  // ignore
        return strAry;
    }


    private Object strToObject(String s) {
        try {
            if (s.isEmpty() || type == String.class) {
                return s;
            } else if (type ==Boolean.class) {
                return Boolean.valueOf(s);
            }
            else if (type ==Double.class) {
                return Double.parseDouble(s);
            }
            else if (type ==Float.class) {
                return Float.parseFloat(s);
            }
            else if (type ==Integer.class) {
                return Integer.parseInt(s);
            }
            else if (type ==Short.class) {
                return Short.parseShort(s);
            }
            else if (type ==Long.class) {
                return Long.parseLong(s);
            }
            else if (type ==Byte.class) {
                return Byte.parseByte(s);
            }
            else if (type ==HREF.class) {
                return HREF.parseHREF(s);
            }
        } catch (IllegalArgumentException ignored) {} // ok to ignore
        return null;
    }

    public String getDerivedFrom() {
        if (getDesc() == null) return null;
        String[] derived = groupMatch(String.format("^\\(%s=(.*)\\).*", DERIVED_FROM), getDesc(), Pattern.DOTALL);  // allow new-line in description.
        return derived == null ? null : derived[0];
    }

    public static String typeToDesc(Class<?> type) {

        if (type == String.class)   return CHAR;
        if (type == Double.class)   return DOUBLE;
        if (type == Float.class)    return FLOAT;
        if (type == Long.class)     return LONG;
        if (type == Integer.class)  return INTEGER;
        if (type == Short.class)    return SHORT;
        if (type == Boolean.class)  return BOOLEAN;
        if (type == Byte.class)     return BYTE;
        if (type == Date.class)     return DATE;

        return CHAR;
    }

    public static Class<?> descToType(String desc) {
        return descToType(desc, String.class);
    }

    public static Class<?> descToType(String desc, Class<?> defaultVal) {
        return switch (desc.toLowerCase()) {
            case DOUBLE, COMPLEX_DOUBLE, REAL       -> Double.class;
            case FLOAT, COMPLEX_FLOAT               -> Float.class;
            case LONG, BIGINT                       -> Long.class;
            case INT, INTEGER                       -> Integer.class;
            case UNI_CHAR, LOCATION, CHAR           -> String.class;
            case SHORT, UNSIGNED_BYTE, SMALLINT     -> Short.class;
            case BOOLEAN, BIT                       -> Boolean.class;
            case BYTE, TINYINT                      -> Byte.class;
            case DATE, TIME                         -> Date.class;
            default -> defaultVal;
        };
    }

    public String toString() {
        return String.format("{Key: %s, Label: %s, Type: %s, TypeDesc: %s, Units: %s}",
                getKeyName(), getLabel(), getDataType(), getTypeDesc(), getUnits());
    }

    public Object clone() throws CloneNotSupportedException {
        DataType rval = (DataType) super.clone();
        rval.links = new ArrayList<>(links);
        return rval;
    }

    /**
     * convenience clone method to avoid catching exception and casting
     * @return a clone copy of this object
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

    private static final int REPLACEMENT_CODE_POINT = 0xbf; // 191 in LATIN-1

    /**
     * Faster version of s.replaceAll("\\p{Cntrl}", Character.toChars(REPLACEMENT_CODE_POINT))
     * @param s input string
     * @return a string with control characters replaced
     */
    private static String replaceCtrl(String s) {
        if (s == null) { return null; }
        return s.codePoints().map(c->(c>0x1F&&c!=0x7F?c:REPLACEMENT_CODE_POINT))
                .collect(StringBuilder::new,StringBuilder::appendCodePoint, StringBuilder::append).toString();
    }
}
