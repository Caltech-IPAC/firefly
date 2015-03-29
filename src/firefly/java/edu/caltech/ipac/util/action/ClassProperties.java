/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.action;

import edu.caltech.ipac.util.AppProperties;

import java.util.StringTokenizer;


/**
 * You set the base of the property in the constructor.
 * You should only use this class when you know that the properties you are
 * looking for must exist.  If any of the methods do not find a property 
 * value then they return the property string as the value.  This is very 
 * useful for debugging.  It shows you on the widget the actual string that 
 * you are searching for.
 * @author Trey Roby
 */
public class ClassProperties  {

    protected final String     _root;
    protected final String     _base;
    protected final Class      _loaderClass;
    protected boolean    _loaded= false;

//=========================================================================
//------------------------- Constructors -----------------------------------
//=========================================================================
    public ClassProperties(String base, Class c) {
        if (base==null) base= getShortName(c);
        _root= base;
        _base= base + ".";
        _loaderClass= c;
        load(); // TODO: this is all set up to turn into a lazy load-- need to evaluate
    }

    public ClassProperties(Class c) { this(null,c); }


//========================================================================
//------------------------- Public Methods --------------------------------
//=========================================================================


    public String getBase() {
        load();
        return _root;
    }

    public String makeBase(String s) {
        load();
        return (_base+s);
    }

    public String getProperty(String prop) {
        load();
        return AppProperties.getProperty(_base+prop, _base+prop);
    }

    public String getName() {
        load();
        return Prop.getName(_root);
    }

    public String getName(String prop) {
        load();
        return Prop.getName(_base+prop);
    }
    public String getName(String prop, String defValue) {
        load();
        return Prop.getName(_base+prop, defValue, null);
    }

    public String[] getNames(String prop[]) {
        load();
        return Prop.getNames(_root, prop);
    }

    public String getTip(String prop) {
        load();
        return Prop.getTip(_base+prop);
    }
    public String getTip() {
        load();
        return Prop.getTip(_root);
    }

    public String getTitle() {
        load();
        return Prop.getTitle(_root);
    }

    public String getTitle(String prop) {
        load();
        return Prop.getTitle(_base+prop);
    }

    public String getColumnName(String prop) {
        load();
        return Prop.getColumnName(_base+prop);
    }

    public boolean getSelected(String prop) {
        load();
        return Prop.getSelected(_base+prop);
    }

    public float getFloatValue(String prop) {
        load();
        return Prop.getFloatValue(_base+prop);
    }

    public int getIntValue(String prop) {
        load();
        return Prop.getIntValue(_base+prop);
    }

    public long getLongValue(String prop) {
        load();
        return Prop.getLongValue(_base+prop);
    }

    public String getDefault(String prop) {
        load();
        return Prop.getDefault(_base+prop);
    }

    public float getFloatDefault(String prop) {
        load();
        return Prop.getFloatDefault(_base+prop);
    }

    public double getDoubleDefault(String prop) {
        load();
        return Prop.getDoubleDefault(_base+prop);
    }

    public int getIntDefault(String prop) {
        load();
        return Prop.getIntDefault(_base+prop);
    }

    public long getLongDefault(String prop) {
        load();
        return Prop.getLongDefault(_base+prop);
    }


    public String getError(String prop) {
        load();
        return Prop.getError(_base+prop);
    }

    public String getErrorDescription(String prop) {
        load();
        return Prop.getErrorDescription(_base+prop);
    }

    public String getDataType(String prop) {
        load();
        return Prop.getDataType(_base+prop);
    }

    public String[] getItems() {
        load();
        return Prop.getItems(_root);
    }

    public String[] getItems(String prop) {
        load();
        return Prop.getItems(_base+prop);
    }



    private static String getShortName(Class c) {
        StringTokenizer st=new StringTokenizer(c.getName(), ".");
        String retval="";
        int len=st.countTokens();
        for(int i=0; (i<len); retval=st.nextToken(), i++) ;
        return retval;
    }

    public void load() {
        if(!_loaded) {
            _loaded=true;

            if(_loaderClass!=null) {
                AppProperties.loadClassPropertiesToPdb(_loaderClass, null, false);
            }
        }
    }
}

