/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.action;

/**
 * A class of constants for the action package. 
 * These constants are used with property files as part of the keys in 
 * the files.
 *
 * @author Trey Roby
 */
public class ActionConst {

    /**
     * specifies what the data type that is represented
     */
    public static final String DATA_TYPE = "DataType"; // the type of data


    /**
     * defines an accelerator in a pulldown menu
     */
    public static final String MNEMONIC = "Mnemonic";      // a Character value

    /**
     * Appends to the tooltip to describe why a function is disabled
     */
    public static final String HOW_TO_ENABLE_TIP = "HowToEnableTip";

    /**
     * defines an mnemonic in a pulldown menu
     */
    public static final String ACCELERATOR = "Accelerator";   // a KeyStroke value

    /**
     * Sepcifies the current radio value from a list
     */
    public static final String RADIO_VALUE = "RadioValue";    // a String value

    /**
     * Sepcifies that this property is selected or not.
     */
    public static final String SELECTED = "Selected";      // a boolean value

    /**
     * defines a name
     */
    public static final String NAME = "Name";    // a name

    /**
     * defines a icon
     */
    public static final String ICON = "Icon";    // a name

    /**
     * defines an identifier.. if not given, name is used
     */
    public static final String ID = "ID";    // a name


    /**
     * defines a hint to only use the icon
     */
    public static final String ICON_ONLY_HINT = "UseIconOnly";    // a hint to only shown the icon, a boolean value

    /**
     * defines a  tool tip
     */
    public static final String SHORT_DESCRIPTION = "ShortDescription";    // a name

    /**
     * defines a  tool tip
     */
    public static final String LONG_DESCRIPTION = "LongDescription";    // a name

    /**
     * defines this field as important, which will have different meaning in different context
     */
    public static final String IMPORTANT = "Important";

    /**
     * defines a file name
     */
    public static final String FILENAME = "FileName";      // a file name
    /**
     * specifies a list of items.
     */
    public static final String ITEMS = "Items";         // a String[]


    /**
     * the integer value associated with a property. Used with radio.  A
     * radio may define a string value and a integer value.
     */
    public static final String INT_VALUE = "IntValue";      // an int value

    /**
     * The minimum value a text field may contain.
     * value is a int or float depending on context
     */
    public static final String MIN = "Min";         // a int or float
    /**
     * The maximum value a text field may contain.
     * value is a int or float depending on context
     */
    public static final String MAX = "Max";         // a int or float
    /**
     * The default value of a text field.  This also may be used for
     * the initial value.
     * value is a int or float depending on context
     */
    public static final String DEFAULT = "Default";       // a int or float

    /**
     * The string key to look up the preference that a field might use
     * value is a String
     */
    public static final String PREFERENCE_KEY = "Pref-Key";       // a int or float


    /**
     * The generic value.
     * value is a int or float depending on context
     */
    public static final String VALUE = "Value";       // a int or float
    /**
     * The number of decemal places a float should be displayed with.
     * value is a int.
     */
    public static final String PRECISION = "Precision";   // a int
    /**
     * The value a float or double can increment by i.e. the value must be
     * divisible by this number with no remainder
     */
    public static final String INCREMENT = "Increment";   // a float or double
    /**
     * The exact way to display the number this overrides precision
     * value is a String
     */
    public static final String PATTERN = "Pattern";   // a String
    /**
     * How to validate a field.  The value is a string.
     */
    public static final String VALIDATION = "Validation";  // a String
    /**
       * This field may have null values.
     * value is a boolean.
     */
    public static final String NULL_ALLOWED = "NullAllowed";  // a boolean
    /**
     * value is a boolean. Hint. True is the tip, label, or desc will not change during
     * the life time of this field
     */
    public static final String TEXT_IMMUTABLE = "TextImmutable";  // a boolean
    /**
     * value is a int.  Defines the maximum width of the component
     */
    public static final String MAX_WIDTH = "MaxWidth";  // a int
    /**
     * value is a int.  Defines the preferred width of the component
     */
    public static final String PREFER_WIDTH = "PreferWidth";  // a int
    /**
     * This field will echo scientific notation if the user enters it
     * in that format.
     * value is a boolean.
     */
    public static final String SCIENTIFIC_ALLOWED = "ScientificAllowed";  // a boolean
    /**
     * This field may have null values.
     * value is a boolean.
     */

    public static final String IS_EDITABLE = "IsEditable";  // a boolean

    /**
     * Vertical or horizontal orientation
     */
    public static final String ORIENTATION = "Orient";

    /**
     * A title property
     */
    public final static String TITLE = "Title";

    /**
     * Defines whether or not to cache
     */
    public final static String CACHED = "Cached";

    /**
     * Defines element size
     */
    public final static String SIZE = "Size";

    /**
     * A URL property
     */
    public final static String URL = "URL";

    /**
     * A units property, should have value of degree, arcmin, arcsec
     */
    public final static String UNITS = "Units";
    /**
     * Units assumed by getValue/setValue
     */
    public final static String INTERNAL_UNITS = "InternalUnits";

    public final static String UNIT_DEGREE = "degree";
    public final static String UNIT_ARCMIN = "arcmin";
    public final static String UNIT_ARCSEC = "arcsec";

    /**
     * A Column Name property. Used for getting the column names of a table.
     */
    public final static String COLUMN_NAME = "ColumnName";

    /**
     * A Error String property.
     */
    public final static String ERROR = "Error";

    /**
     * A String to describe the field that is generating an error
     */
    public final static String ERROR_DESCRIPTION = "ErrorDescription";
    /**
     * A String to describe the units of the field that is generating an error
     */
    public final static String ERROR_DESCRIPTION_UNITS = "ErrorDescriptionUnits";
    /**
     * A int - the number of characters
     */
    public final static String LENGTH = "Length";

    /**
     * Font name
     */
    public final static String COLOR = "Color";

    public final static String EXTENSION = "Extension";

               // the folowing section is data types
    public final static String DATE = "Date";
    public final static String INTEGER = "Integer";
    public final static String LONG = "Long";
    public final static String FLOAT = "Float";
    public final static String DOUBLE = "Double";
    public final static String BOOLEAN = "Boolean";
    public final static String ENUM_STRING = "EnumString";
    public final static String STRING_LIST = "StringList";
    public final static String INT_LIST = "IntList";
    public final static String FILE = "File";
    public final static String MULTI_COORD = "MultiCoord";
    public final static String ENUM_STRING_INT = "EnumStringInt";
    public static final String STRING = "String";
    public static final String LABEL_STRING = "LabelString";
    public static final String MASK = "Mask";
    public static final String EMAIL = "EMail";
    public static final String PASSWORD = "Password";
    public static final String DEGREE = "DEGREE";
    public final static String LINK = "Link";
    public final static String LAT = "Lat";
    public final static String LON = "Lon";
    public static final String POS_STRING = "PositionString"; // a string that has a lon, lat a coordinate sys
    public static final String HIDDEN = "HIDDEN";

    /**
     * This property specify the minimum boundary type.  Used for range validation.
     * It can be either 'inclusive' or 'exclusive'.
     */
    public static final String MIN_BOUND_TYPE = "MinBoundType";

    /**
     * This property specify the maximum boundary type.
     * It can be either 'inclusive' or 'exclusive'.
     */
    public static final String MAX_BOUND_TYPE = "MaxBoundType";

    /**
     * This property specify the property is optional or not.
     */
    public static final String OPTIONAL = "Optional";
}
