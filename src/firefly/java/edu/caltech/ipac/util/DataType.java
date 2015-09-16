/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;

public class DataType implements Serializable, Cloneable {

    public enum Importance { HIGH, MEDIUM, LOW, IGNORE}

    private final static int UNKNOWN_COLUMN_IDX= -1;
    private static final String DOUBLE = "double";
    private static final String FLOAT = "float";
    private static final String INTEGER = "int";
    private static final String LONG = "long";
    private static final String CHAR = "char";
    private static final String S_DOUBLE = "d";
    private static final String S_FLOAT = "f";
    private static final String S_INTEGER = "i";
    private static final String S_LONG = "l";
    private static final String S_CHAR = "c";

    private       Class      _type;
    private       String     _units;
    private final boolean    _editable  = false;
    private       String     _keyName;
    private       String     _prefix;
    private       String     _defTitle;
    private       Importance _importance;
    private       boolean    _mayBeNull;
    private       int        _columnIdx;
    private       int        _outputIdx;
    private FormatInfo       _formatInfo;
    private       String     _typeDesc;
    private       String     _shortDesc;
    private       String     _nullString;
    private       int        _maxDataWidth = 0;     // this is the max width of the data...from reading the file.


    public DataType(String keyName,
                    Class  type) {
        this(keyName, keyName, type, Importance.HIGH, null, false);
    }

    public DataType(String keyName,
                    String defTitle,
                    Class  type) {
        this(keyName, defTitle, type, Importance.HIGH, null, false);
    }



    public DataType(String     keyName,
                    String     defTitle,
                    Class      type,
                    Importance importance) {
       this(keyName, defTitle, type, importance, null, false);
    }

    public DataType(String     keyName,
                    String     defTitle,
                    Class      type,
                    Importance importance,
                    String     units,
                    boolean    mayBeNull) {
        _keyName  = keyName;
        _prefix   = null;
        _defTitle  = defTitle;
        _importance= importance;
        _type= type;
        _units= units;
        _mayBeNull = mayBeNull;
        _columnIdx= UNKNOWN_COLUMN_IDX;
        _outputIdx= UNKNOWN_COLUMN_IDX;
        Assert.tst(keyName!=null, "keyName cannot be null");
    }

//    public DataType copyWithDifferentType(Class type) {
//        return new DataType(_keyName,   _defTitle, _columnIdx, type,
//                            _importance, _units,    _mayBeNull);
//    }

    public DataType copyWithNoColumnIdx(int idx) {
        return new DataType(_keyName,   _defTitle, _type,
                            _importance, _units,    _mayBeNull);
    }


    public Class   getDataType()     { return _type;       }
    public Importance getImportance()   { return _importance; }
    public boolean isEditable()      { return _editable;   }
    public String  getKeyName()      { return (_prefix == null) ? _keyName : _prefix+_keyName;   }
    public String  getDefaultTitle() { return _defTitle;   }
    public String  getDataUnit()     { return _units;      }
    public boolean getMayBeNull()    { return _mayBeNull;  }
    public int     getColumnIdx()    { return _columnIdx;  }

    public void setKeyNamePrefix(String prefix) {
        _prefix = prefix;
        if (hasFormatInfo()) {
            FormatInfo finfo = getFormatInfo();
            if (finfo.getWidth() < getKeyName().length()) {
                finfo.setWidth(getKeyName().length());
//                setFormatInfo(finfo);
            }
        }
    }
    
    public void setKeyName(String keyName) {
        _keyName = keyName;
    }

    public int getMaxDataWidth() {
        return _maxDataWidth;
    }

    public void setMaxDataWidth(int maxDataWidth) {
        _maxDataWidth = maxDataWidth;
    }

    public String getNullString() {
        return _nullString;
    }

    public void setNullString(String nullString) {
        _nullString = nullString;
    }

    public void setDataType(Class type)     { _type= type;       }

    public void setUnits(String units) {
        _units = units;
    }

    public void setMayBeNull(boolean mayBeNull) {
        _mayBeNull = mayBeNull;
    }

    public void setDefaultTitle(String title) { _defTitle= title;   }

    void setColumnIdx(int i)    {
        _columnIdx= i;
        if (_outputIdx==UNKNOWN_COLUMN_IDX) _outputIdx= i;
    }

    public void setImportance(Importance importance) {
       _importance=  importance;
    }

    public boolean isKnownType() {
        return (_type==Boolean.class ||
                _type==String.class  ||
                _type==Double.class  ||
                _type==Float.class   ||
                _type==Integer.class ||
                _type==Short.class ||
                _type==Long.class ||
                _type==HREF.class
        );
    }

    public Object convertStringToData(String s) {
        if (s == null || s.length() == 0 || s.equalsIgnoreCase("null")) return null;
        if (_nullString != null && _nullString.equals(s)) return null;

        Object retval= s;
        try {
             if (_type==Boolean.class) {
                 retval= Boolean.valueOf(s);
             }
             else if (_type==String.class) {
                 retval= s;
             }
             else if (_type==Double.class) {
                 retval= new Double(s);
             }
             else if (_type==Float.class) {
                 retval= new Float(s);
             }
             else if (_type==Integer.class) {
                 retval= new Integer(s);
             }
             else if (_type==Short.class) {
                 retval = new Short(s);
             }
             else if (_type==Long.class) {
                 retval = new Long(s);
             }
             else if (_type==Byte.class) {
                 retval = new Byte(s);
             }
             else if (_type==HREF.class) {
                 retval = HREF.parseHREF(s);
             }
        } catch (NumberFormatException e) {
            retval= null;
        } catch (IllegalArgumentException iae) {
            retval = null;
        }
        return retval;
    }

    public void setTypeDesc(String typeDesc) {
        _typeDesc = typeDesc;
    }

    public String getTypeDesc() {
        if (_typeDesc == null) {
            Class dt = getDataType();
            dt = dt == null ? String.class : dt;
            boolean useShortType = getFormatInfo().getWidth() < 6;

            if (dt.equals(Double.class))
                _typeDesc = useShortType ? S_DOUBLE : DOUBLE;
            else if (dt.equals(Float.class))
                _typeDesc = useShortType ? S_FLOAT : FLOAT;
            else if (dt.equals(Integer.class) || dt.equals(Short.class))
                _typeDesc = useShortType ? S_INTEGER : INTEGER;
            else if (dt.equals(Long.class))
                _typeDesc = useShortType ? S_LONG : LONG;
            else
                _typeDesc = useShortType ? S_CHAR : CHAR;
        }

        return _typeDesc;
    }

    public void setShortDesc(String shortDesc) {
        _shortDesc = shortDesc;
    }

    public String getShortDesc() {
        return _shortDesc;
    }

    public boolean hasFormatInfo() {
        return _formatInfo != null;
    }

    public FormatInfo getFormatInfo() {
        if ( _formatInfo == null ) {
            _formatInfo = FormatInfo.createDefaultFormat(getDataType());
        }
        return _formatInfo;
    }

    public void setFormatInfo(FormatInfo info) {
        _formatInfo = info;
    }

    private String importanceAsString() {
        String retval= null;
        switch (_importance) {
            case HIGH   : retval= "High"; break;
            case MEDIUM : retval= "Medium"; break;
            case LOW    : retval= "Low"; break;
            case IGNORE : retval= "Ignore"; break;
            default: Assert.tst(false,
                                "unrecognized importance: " +_importance);
        }
        return retval;

    }

    public String toString() {
        return "Key: "        + getKeyName()   + "\n" +
               "Type= "       + _type      + "\n" +
               "TypeDesc= "   + _typeDesc  + "\n" +
               "ShortDesc= "  + _shortDesc + "\n" +
               "Units= "      + _units     + "\n" +
               "Editable: "   + _editable  + "\n" +
               "Title: "      + _defTitle  + "\n" +
               "Importance: " + importanceAsString() + "\n" +
               "May Be Null:" + _mayBeNull + "\n" +
               "Column idx: " + _columnIdx + "\n" +
               "Output idx:"  + _outputIdx;
    }

    public Object clone() throws CloneNotSupportedException {
        DataType dt = (DataType) super.clone();
        dt._formatInfo = _formatInfo == null ? null : (FormatInfo) _formatInfo.clone();
        return dt;
    }

    /**
     * FormatInfo defines the formatting behavior of this DataType.
     * This includes column width, column header alignment, column data alignment,
     * and data format.
     * When providing a format string, java.util.Formatter must be able to interpret this string.
     */

    public static class FormatInfo implements Serializable, Cloneable {

        public enum Align {LEFT, RIGHT}
        private static final Align DEF_HD_ALIGN = Align.LEFT;
        private static final Align DEF_DATA_ALIGN = Align.LEFT;


        private static final String DEF_FMT_STR = "%s";

        private static final int DEF_WIDTH = 30;
        private static final String NULL_STR = "";
        private Align _headerAlign;
        private Align _dataAlign;
        private int _width;
        private String _headerFormat;
        private String _dataFormat;
        private boolean _isDefault;

        /**
         * Creates a new FormatInfo good for formatting String.
         * It has a default width of 30, headers are left-align,
         * and data are right-align.
         */
        public FormatInfo() {
            this(DEF_WIDTH);
        }

        /**
         * Creates a new FormatInfo good for formatting String
         * with the given width.  Headers are left-align,
         * and data are right-align.
         * @param width  column's width
         */
        public FormatInfo(int width) {
            this(width, DEF_HD_ALIGN, DEF_DATA_ALIGN);
        }

        /**
         * Creates a new FormatInfo good for formatting String
         * using the given parameters.
         * @param width     column's width
         * @param headerAlign   header's alignment
         * @param dataAlign     data's alignment
         */
        public FormatInfo(int width, Align headerAlign, Align dataAlign) {
            _width = width;
            _headerAlign = headerAlign;
            _dataAlign = dataAlign;

        }

        void setIsDefault(boolean aDefault) {
            _isDefault = aDefault;
        }

        public boolean isDefault() {
            return _isDefault;
        }

        public Align getHeaderAlign() {
            return _headerAlign;
        }

        public void setHeaderAlign(Align headerAlign) {
            _headerAlign = headerAlign;
            setIsDefault(false);
        }

        public Align getDataAlign() {
            return _dataAlign;
        }

        public void setDataAlign(Align dataAlign) {
            _dataAlign = dataAlign;
        }

        public int getWidth() {
            return _width;
        }

        public void setWidth(int width) {
            _width = width;
        }

        public String getHeaderFormatStr() {
            return _headerFormat == null ? DEF_FMT_STR : _headerFormat;
        }

        public void setHeaderFormatStr(String headerFormatStr) {
            _headerFormat = headerFormatStr;
        }

        public String getDataFormatStr() {
            return _dataFormat == null ? DEF_FMT_STR : _dataFormat;
        }

        public void setDataFormat(String dataFormatStr) {
            _dataFormat = dataFormatStr;
        }

        /**
         * Overloaded to use the default locale.
         * @param value
         */
        public String formatDataOnly(Object value) {
            return formatDataOnly(Locale.getDefault(), value);
        }

        /**
         * Returns a String representation of the given data object
         * using the given Locale and its data's format string.
         * @param locale
         * @param value
         */
        public String formatDataOnly(Locale locale, Object value) {
            if (value == null) return NULL_STR;
            String s = String.format(locale, getDataFormatStr(), value);
            if (s.length() > getWidth()) {
                s = s.substring(0, getWidth());
            }
            return s;
        }

        /**
         * Returns a fixed-width String representation of the given
         * data object based on the defined width, alignment,
         * and data format of this class.
         * @param value the data object containing the data.
         * @return a string
         */
        public String formatData(Object value) {
            return formatData(Locale.getDefault(), value);
        }

        public String formatData(Object value, String strForNull) {
            return formatData(Locale.getDefault(), value, strForNull);
        }

        /**
         * Overloaded to format the data using a given locale.
         * @param locale
         * @param value
         */
        public String formatData(Locale locale, Object value) {
            return formatData(locale, value, "");
        }

        public String formatData(Locale locale, Object value, String strForNull) {
            String fmtStr = (getDataAlign() == Align.LEFT ? "%-" : "%") + getWidth() + "s";
            return String.format(locale, fmtStr, (value == null) ? strForNull : formatDataOnly(locale, value));

        }

        /**
         * Returns a fixed-width String representation of the given
         * data object based on the header's format information of this class.
         * @return a string
         */
        public String formatHeader(String value) {
            String v = value == null ? "" : String.format(getHeaderFormatStr(), value);
            String fmtStr = (getHeaderAlign() == Align.LEFT ? "%-" : "%") + getWidth() + "s";
            return String.format(fmtStr, v);
        }

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        private static boolean isFloatingPoint(Class dt) {
            return Double.class.isAssignableFrom(dt) ||
                   Float.class.isAssignableFrom(dt);
        }



        //=========================================================================
        //  Convenience factory method to create common FormatInfo
        //=========================================================================

        /**
         * Creates a dafault FormatInfo based on its data type.
         * If data is a String, it will be left-aligned.
         * @param dt  the class of the object to format.
         */
        public static FormatInfo createDefaultFormat(Class dt) {

            FormatInfo fi = null;
            dt = dt == null ? String.class : dt;
            if (isFloatingPoint(dt)) {
                fi = createFloatFormat();
            } else if (Date.class.isAssignableFrom(dt)) {
                fi = createDateFormat();
            } else if (Integer.class.isAssignableFrom(dt)) {
                fi = new FormatInfo();
                fi.setDataFormat("%d");
            } else {
                fi = new FormatInfo();
                fi.setDataAlign(Align.LEFT);
            }
            fi.setIsDefault(true);
            return fi;
        }

        /**
         * Creates a default FormatInfo good for formating
         * floating-point numbers.  The default precision is set to 4.
         */
        public static FormatInfo createFloatFormat() {
            return createFloatFormat(DEF_WIDTH, 4);
        }

        /**
         * Creates a FormatInfo good for formating floating-point numbers
         * with the given width and precision.
         * @param width
         * @param precision
         */
        public static FormatInfo createFloatFormat(int width, int precision) {
            return createFloatFormat(width, precision, DEF_HD_ALIGN, DEF_DATA_ALIGN);
        }

        /**
         * Creates a FormatInfo good for formating floating-point numbers.
         * @param width     the column's width
         * @param precision the number of precision
         * @param headerAlign   header's alignment.
         * @param dataAlign     data's alignment.
         */
        public static FormatInfo createFloatFormat(int width, int precision, Align headerAlign, Align dataAlign) {
            FormatInfo fi = new FormatInfo(width);
            fi.setHeaderAlign(headerAlign);
            fi.setDataAlign(dataAlign);
            fi.setDataFormat("%." + precision + "f");
            return fi;
        }

        /**
         * Creates a default FormatInfo good for displaying dates.
         */
        public static FormatInfo createDateFormat() {
            return createDateFormat(DEF_WIDTH);
        }

        public static FormatInfo createDateFormat(int width) {
            return createDateFormat(width, true, true);
        }

        /**
         * Creates a FormatInfo good for displaying dates and times
         * with default alignment.  The default format is yyyy-mm-dd h24:mm:ss.SSS.
         * If both showDate and showTime are false, and IllegalArgumentException will be thrown.
         *
         * HINT: When setting custom date/time format, be aware that there are multiple
         *       components to the date/time format string.
         *       Make sure to append the '1$' in front of each to reference the only
         *       date object being formatted.
         * @param width     column's width
         * @param showDate  show date
         * @param showTime  show time
         */
        public static FormatInfo createDateFormat(int width, boolean showDate, boolean showTime) {
            return createDateFormat(width, showDate, showTime, DEF_HD_ALIGN, DEF_DATA_ALIGN);
        }

        public static FormatInfo createDateFormat(int width, boolean showDate, boolean showTime, Align headerAlign, Align dataAlign) {
            String timeStr = "%1$tH:%1$tM:%1$tS.%1$tL";
            String dateStr = "%1$tF";
            String fmtStr;
            if (showDate && !showTime) {
                fmtStr = dateStr;
            } else if (showTime && !showDate) {
                fmtStr = timeStr;
            } else if (showTime && showDate) {
                fmtStr = dateStr + " " + timeStr;
            } else {
                throw new IllegalArgumentException("Both, showDate and showTime may not be false");
            }

            FormatInfo fi = new FormatInfo(width);
            fi.setHeaderAlign(headerAlign);
            fi.setDataAlign(dataAlign);
            fi.setDataFormat(fmtStr);
            return fi;
        }



    }

    public static void main(String args[]) {

        Double d = 987654321.123456789;
        String s = "12345678901234567890";
        Date dt = new Date();
        int i = 123456789;

        //displaying header/data of a double using default format with different locale
        System.out.println("'" + FormatInfo.createDefaultFormat(Double.class).formatHeader(d.toString()) + "'");
        System.out.println("'" + FormatInfo.createDefaultFormat(Double.class).formatData(d) + "'");
        System.out.println("'" + FormatInfo.createDefaultFormat(Double.class).formatData(Locale.FRANCE, d) + "'");

        //displaying dates
        System.out.println("'" + FormatInfo.createDateFormat(50, true, false).formatData(dt) + "'");
        System.out.println("'" + FormatInfo.createDateFormat(50, false, true, FormatInfo.Align.RIGHT, FormatInfo.Align.LEFT).formatData(dt) + "'");
        System.out.println("'" + FormatInfo.createDateFormat(50).formatData(dt) + "'");

        //displaying integers
        System.out.println("'" + FormatInfo.createDefaultFormat(Integer.class).formatData(i) + "'");

        //using %g format with overflowing width.
        FormatInfo fi = new FormatInfo(50);
        fi.setDataFormat("%55.6g");
        System.out.println("'" + fi.formatData(d) + "'");

        //truncate string
        fi.setDataFormat("%.2s");
        fi.setDataAlign(FormatInfo.Align.LEFT);
        System.out.println("'" + fi.formatData(s) + "'");


/*      // output from above
        '9.876543211234568E8           '
        '                987654321.1235'
        '                987654321,1235'
        '                                        2007-04-19'
        '11:13:16.865                                      '
        '                           2007-04-19 11:13:16.865'
        '                     123456789'
        '                                            9.87654e+08'
        '12                                                '                                        '
*/

//        doit(5.0);
//        doit("asdf");
//         int ary[]= {1,2,3};
//        Double aryf[]= {1.0,2.0,3.0};
//        doit(ary);
//        doit(aryf);
    }

    public static <T> void doit(T ary[]) {
        T total;
        for(T y : ary) {
            if (y instanceof Integer) {
                System.out.println("Integer");
            }
            else {
                System.out.println("not Integer");
            }
        }
    }
}
