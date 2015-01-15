/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.action;

import edu.caltech.ipac.util.AppProperties;

import java.util.Properties;
import java.util.StringTokenizer;
import java.io.File;


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
    protected final Properties _db;
    protected final Class      _loaderClass;
    protected boolean    _loaded= false;
    protected String     _stringFileName= null;
    protected final File _loadFile;

//=========================================================================
//------------------------- Constructors -----------------------------------
//=========================================================================
    public ClassProperties(String base,
                           String fileName,
                           Class c,
                           Properties alternalPdb) {
        if (base==null) base= getShortName(c);
        _root= base;
        _base= base + ".";
        _stringFileName= fileName;
        _loaderClass= c;
        _db= alternalPdb;
        _loadFile= null;
        load(); // TODO: this is all set up to turn into a lazy load-- need to evaluate
    }

  public ClassProperties(String base,
                         String fileName,
                         Class c,
                         boolean privateDB) {
      this(base,fileName,c,privateDB ? new Properties() : null);
  }



  public ClassProperties(String base,  Class c,  boolean privateDB) {
      this(base,null,c,privateDB);
  }

  public ClassProperties(String base, Class c) { this(base,c,false); }

  public ClassProperties(Class c) { this(c,false); }

  public ClassProperties(Class c, boolean privateDB) {
      this(null, c, privateDB);
  }

  public ClassProperties(String base) { this(base,null,false); }

    public ClassProperties(File file,
                           String base,
                           boolean privateDB) {
        _db= privateDB ? new Properties() : null;
        _root= base;
        _base= base + ".";
        _loaderClass= null;
        _loadFile= file;
        load(); // TODO: this is all set up to turn into a lazy load-- need to evaluate
    }

//========================================================================
//------------------------- Public Methods --------------------------------
//=========================================================================

    public Class getLoaderClass() { return _loaderClass; }


    public String getBase() {
        load();
        return _root;
    }

    public Properties getAlternatePdb() { return _db; }

    public String makeBase(String s) {
        load();
        return (_base+s);
    }

    public String getProperty(String prop) {
        load();
        return AppProperties.getProperty(_base+prop, _base+prop, _db);
    }

    public String getName() {
        load();
        return Prop.getName(_root, _db);
    }

    public String getName(String prop) {
        load();
        return Prop.getName(_base+prop, _db);
    }
    public String getName(String prop, String defValue) {
        load();
        return Prop.getName(_base+prop, defValue, _db);
    }

    public String[] getNames(String prop[]) {
        load();
        return Prop.getNames(_root, prop, _db);
    }

    public String getTip(String prop) {
        load();
        return Prop.getTip(_base+prop, _db);
    }
    public String getTip() {
        load();
        return Prop.getTip(_root, _db);
    }

    public String getTitle() {
        load();
        return Prop.getTitle(_root, _db);
    }

    public String getTitle(String prop) {
        load();
        return Prop.getTitle(_base+prop, _db);
    }

    public String getColumnName(String prop) {
        load();
        return Prop.getColumnName(_base+prop, _db);
    }

    public boolean getSelected(String prop) {
        load();
        return Prop.getSelected(_base+prop, _db);
    }

    public float getFloatValue(String prop) {
        load();
        return Prop.getFloatValue(_base+prop, _db);
    }

    public int getIntValue(String prop) {
        load();
        return Prop.getIntValue(_base+prop, _db);
    }

    public long getLongValue(String prop) {
        load();
        return Prop.getLongValue(_base+prop, _db);
    }

    public String getDefault(String prop) {
        load();
        return Prop.getDefault(_base+prop, _db);
    }

    public float getFloatDefault(String prop) {
        load();
        return Prop.getFloatDefault(_base+prop, _db);
    }

    public double getDoubleDefault(String prop) {
        load();
        return Prop.getDoubleDefault(_base+prop, _db);
    }

    public int getIntDefault(String prop) {
        load();
        return Prop.getIntDefault(_base+prop, _db);
    }

    public long getLongDefault(String prop) {
        load();
        return Prop.getLongDefault(_base+prop, _db);
    }


    public String getError(String prop) {
        load();
        return Prop.getError(_base+prop, _db);
    }

    public String getErrorDescription(String prop) {
        load();
        return Prop.getErrorDescription(_base+prop, _db);
    }

    public String getDataType(String prop) {
        load();
        return Prop.getDataType(_base+prop, _db);
    }

    public String[] getItems() {
        load();
        return Prop.getItems(_root, _db);
    }

    public String[] getItems(String prop) {
        load();
        return Prop.getItems(_base+prop, _db);
    }



    private static String getShortName(Class c) {
        StringTokenizer st=new StringTokenizer(c.getName(), ".");
        String retval="";
        int len=st.countTokens();
        for(int i=0; (i<len); retval=st.nextToken(), i++) ;
        return retval;
    }

    String getBaseNoLoad() { return _root; }

    public void load() {
        if(!_loaded) {
            _loaded=true;

            if(_loaderClass!=null) {
                //System.out.println("loading: " + _loaderClass.getName());
                if (_stringFileName==null) {
                    AppProperties.loadClassPropertiesToPdb(_loaderClass, _db,
                                                           (_db!=null));
                }
                else {
                    AppProperties.loadClassPropertiesToPdb(_loaderClass,
                                                           _stringFileName,
                                                           _db,(_db!=null));
                }
            }
            else if (_loadFile!=null && _loadFile.canRead()) {
                AppProperties.loadClassPropertiesFromFileToPdb(_loadFile, _db);
            }
        }
    }
}

