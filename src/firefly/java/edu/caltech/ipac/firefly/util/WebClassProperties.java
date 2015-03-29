/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;


import edu.caltech.ipac.firefly.core.Application;


/**
 * You set the base of the property in the constructor.
 * You should only use this class when you know that the properties you are
 * looking for must exist.  If any of the methods do not find a property 
 * value then they return the property string as the value.  This is very 
 * useful for debugging.  It shows you on the widget the actual string that 
 * you are searching for.
 * @author Trey Roby
 */
public class WebClassProperties {

    protected final String _root;
    protected final String _base;

//=========================================================================
//------------------------- Constructors -----------------------------------
//=========================================================================

    public WebClassProperties(String base, PropFile textRes) {

        if (textRes!=null) {
            Application.getInstance().getProperties().load(null, textRes);
        }

        if (base==null) {
            _root= "";
            _base= "";
        }
        else {
            _root= base;
            _base= base + ".";
        }
    }

    public WebClassProperties(String base) { this(base,null); }
    public WebClassProperties(Class c) { this(getShortName(c),null); }
    public WebClassProperties(Class c, PropFile textRes) { this(getShortName(c),textRes); }


//========================================================================
//------------------------- Public Methods --------------------------------
//=========================================================================

    private static String getShortName(Class c) {
        String parts[]= c.getName().split("\\.");
        return parts[parts.length-1];
    }


    public String getBase() { return _root; }


    public String makeBase(String s) { return (_base+s); }

    public String getProperty(String prop, String defValue) {
        WebAppProperties props= Application.getInstance().getProperties();
        return props.getProperty(_base+prop, defValue);
    }


    public String getProperty(String prop) { return getProperty(prop, _base+prop); }

    public String getName() { return WebProp.getName(_root); }

    public String getName(String prop) { return WebProp.getName(_base+prop); }
    public String getName(String prop, String defValue) { return WebProp.getName(_base+prop, defValue); }

    public String[] getNames(String prop[]) { return WebProp.getNames(_root, prop); }

    public String getTip(String prop) { return WebProp.getTip(_base+prop); }
    public String getTip() { return WebProp.getTip(_root); }

    public String getTitle() { return WebProp.getTitle(_root); }

    public String getTitle(String prop) { return WebProp.getTitle(_base+prop); }

    public String getColumnName(String prop) { return WebProp.getColumnName(_base+prop); }

    public boolean getSelected(String prop) { return WebProp.getSelected(_base+prop); }

    public float getFloatValue(String prop) { return WebProp.getFloatValue(_base+prop); }

    public int getIntValue(String prop) { return WebProp.getIntValue(_base+prop); }

    public long getLongValue(String prop) { return WebProp.getLongValue(_base+prop); }

    public String getError(String prop) { return WebProp.getError(_base + prop); }

    public String getDataType(String prop) { return WebProp.getDataType(_base+prop); }

    public String[] getItems() { return WebProp.getItems(_root); }

    public String[] getItems(String prop) { return WebProp.getItems(_base+prop); }


}

