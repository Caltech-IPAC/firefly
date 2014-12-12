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
     * specifies that this button is the default button
     */
    public static final String DEFAULT_BUTTON = "DefaultButton"; // a boolean value

    /**
     *
     */
    public static final String ACTION_COMMAND = "ActionCommand"; // a String value

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
     * defines a class name
     */
    public static final String CLASSNAME = "ClassName";    // a class name
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
     * defines a icon - you should use ICON instead, this one will be deprecated in the future
     */
    public static final String SMALL_ICON = "SmallIcon";

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
     * defines a relative path name
     */
    public static final String RELATIVE_PATH = "relativePath";  // a relative path

    /*
    * referres to anothter property that this file is relative to
    */
    public static final String RELATIVE_TO = "RelativeTo";  // another property

    /**
     * specifies a list of items.
     */
    public static final String ITEMS = "Items";         // a String[]

    /**
     * Used with pulldowns to specify if this pulldown is a help pulldown
     */
    public static final String ISHELP = "IsHelpMenu";    // a boolean value

    /**
     * the integer value associated with a property. Used with radio.  A
     * radio may define a string value and a integer value.
     */
    public static final String INT_VALUE = "IntValue";      // an int value

    /**
     * Do not do any validation.  This constant disables all validation
     */
    public static final int NO_VALIDATION = 0;
    /**
     * The value must be greater than a minimum to validate.
     */
    public static final int MIN_VALIDATION = 1;
    /**
     * The value must be less than a maximum to validate.
     */
    public static final int MAX_VALIDATION = 2;
    /**
     * The value must be between a minimum and a maximum to validate.
     */
    public static final int RANGE_VALIDATION = 3;
    /**
     * The value must be between one a list of valid values
     */
    public static final int LIST_VALIDATION = 4;
    /**
     * Do not do any validation.  This constant disables all validation
     * This string will be used in property files.
     */
    public static final String NO_VALIDATION_STR = "none";
    /**
     * The value must be greater than a minimum to validate.
     * This string will be used in property files.
     */
    public static final String MIN_VALIDATION_STR = "min";
    /**
     * The value must be less than a maximum to validate.
     * This string will be used in property files.
     */
    public static final String MAX_VALIDATION_STR = "max";
    /**
     * The value must be between a minimum and a maximum to validate.
     * This string will be used in property files.
     */
    public static final String RANGE_VALIDATION_STR = "range";
    /**
     * The value must be one of a list of valid values
     * This string will be used in property files.
     */
    public static final String LIST_VALIDATION_STR = "list";

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
     * How to display a number
     * value is a CHAR: either I, F, E for integer, float, or exponetail
     */
    public static final String DISPLAY_TYPE = "DisplayType"; // a char I, F, E
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
     * value is a boolean. Hint. True is the tip, label, or desc will not change durring
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
     * Represent a float field that the use choose not to enter a value
     * in the GUI.
     */
    public static final float FLOAT_NULL = Float.NaN;

    /**
     * Represent a int field that the use choose not to enter a value
     * in the GUI.
     */
    public static final int INT_NULL = Integer.MIN_VALUE;

    /**
     * Used when a text field will support nulls.
     */
    public static final String NULL_STR = "null";

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
     * A String to describe all the characters a text field may not have
     */
    public final static String INVALID_CHARACTERS = "InvalidCharacters";

    /**
     * A field is allowed to have multiple consecative white spaces.
     * value is a boolean.
     */
    public final static String ALLOW_MULTIPLE_BLANKS = "AllowMultipleBlanks";

    /**
     * A int - the number of charaters
     */
    public final static String LENGTH = "Length";

    /**
     * A int - the maximum number of charaters that may be in this string
     */
    public final static String MAX_LENGTH = "MaxLength";

    /**
     * Font name
     */
    public final static String FONT = "Font";

    /**
     * Font name
     */
    public final static String COLOR = "Color";

    /**
     * Enable this property with another boolean property
     */
    public final static String ENABLED_BY = "EnabledBy";

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



            // the folowing section is data types
    public final static String MAC_PROP = ".mac.";
    public final static String MAC64_PROP = ".mac64.";
    public final static String WINDOWS_PROP = ".windows.";
    public final static String UNIX_PROP = ".unix.";
    public final static String LINUX_PROP = ".linux.";
    public final static String LINUX64_PROP = ".linux64.";
    public final static String SUN_PROP = ".sun.";

    public final static String INSTALL_DIR = "<INSTALLDIR>";
    private final static String USE_OP_SEP = ":";
    public final static String USE_OP = "USE" + USE_OP_SEP;

    /**
     * This property specify the mininum boundary type.  Used for range validation.
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
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */