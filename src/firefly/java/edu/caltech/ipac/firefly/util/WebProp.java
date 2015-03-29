/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;


import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.util.action.ActionConst;


/**
 * You should only use this class when you know that the properties you are
 * looking for must exist.  If any of the String methods do not find a property
 * value then they return the property string as the value.  This is very 
 * useful for debugging.  It shows you on the widget the actual string that 
 * you are searching for. If any of the number methods do not find a value
 * they will return a 0.
 *
 * @author Trey Roby
 *
 */
public class WebProp {

    public static String getTip(String propBase) {
        String prop= propBase + "." + ActionConst.SHORT_DESCRIPTION;
        return getProp(prop,prop);
    }

    public static String getName(String propBase, String defValue) {
        String prop= propBase + "." + ActionConst.NAME;
        return getProp(prop,defValue);
    }

    public static String getName(String propBase) {
        String prop= propBase + "." + ActionConst.NAME;
        return getProp(prop, null);
    }

    public static String[] getNames(String base, String prop[]) {
        String retval[]= null;
        if (base != null) {
            if (prop != null) {
                retval= new String[prop.length];
                for (int i= 0; (i<prop.length); i++)
                    retval[i]= getName(base + "." + prop[i]);
            }
        }
        else {
            retval= getNames(prop);
        }
        return retval;
    }

    public static String[] getNames(String propBase[]) {
        String retval[]= null;
        if (propBase != null) {
            retval= new String[propBase.length];
            for (int i= 0; (i<propBase.length); i++)
                retval[i]= getName(propBase[i]);
        }
        return retval;
    }


    public static String getTitle(String propBase) {
        String prop= propBase + "." + ActionConst.TITLE;
        return getProp(prop,prop);
    }


    public static String getURL(String propBase) {
        return getProp(propBase + "." + ActionConst.URL,null);
    }

    public static String getIcon(String propBase) {
        String prop= propBase + "." + ActionConst.ICON;
        return getProp(prop,null);
    }

    public static boolean getIconOnlyHint(String propBase, boolean defValue) {
        String prop= propBase + "." + ActionConst.ICON_ONLY_HINT;
        return getAppProps().getBooleanProperty(prop,defValue);
    }

    public static boolean isImportant(String propBase, boolean defValue) {
        String prop= propBase + "." + ActionConst.IMPORTANT;
        return getAppProps().getBooleanProperty(prop,defValue);
    }



    public static String getColumnName(String propBase) {
        String prop= propBase + "." + ActionConst.COLUMN_NAME;
        return getProp(prop,prop);
    }

    public static String getColumnName(String propBase, String def) {
        String prop= propBase + "." + ActionConst.COLUMN_NAME;
        return getProp(prop,def);
    }


    public static boolean getSelected(String propBase) {
        return getSelected(propBase,false);
    }

    public static boolean getSelected(String propBase, boolean defValue) {
        String prop= propBase + "." + ActionConst.SELECTED;
        return getAppProps().getBooleanProperty(prop,defValue);
    }


  public static float getFloatValue(String propBase) {
     String prop= propBase + "." + ActionConst.VALUE;
     return getAppProps().getFloatProperty(prop,0.0F);
  }

   public static double getDoubleValue(String propBase) {
      String prop= propBase + "." + ActionConst.VALUE;
      return getAppProps().getDoubleProperty(prop,0.0F);
   }

    public static int getIntValue(String propBase) {
        String prop= propBase + "." + ActionConst.VALUE;
        return getAppProps().getIntProperty(prop,0);
    }

    public static long getLongValue(String propBase) {
        String prop= propBase + "." + ActionConst.VALUE;
        return getAppProps().getLongProperty(prop,0L);
    }

    public static String getDataType(String propBase) {
        String prop= propBase + "." + ActionConst.DATA_TYPE;
        return getAppProps().getProperty(prop,null);
    }

    public static String getError(String propBase) {
        String prop= propBase + "." + ActionConst.ERROR;
        return getAppProps().getProperty(prop,prop);
    }

    public static String getErrorDescription(String propBase) {
        String prop= propBase + "." + ActionConst.ERROR_DESCRIPTION;
        return getAppProps().getProperty(prop,prop);
    }

    public static String getExtension(String propBase) {
        String prop= propBase + "." + ActionConst.EXTENSION;
        return getAppProps().getProperty(prop,prop);
    }

    public static String[] getItems(String propBase) {
        String tokens[]= null;
        String prop= propBase + "." + ActionConst.ITEMS;
        String value= getAppProps().getProperty(prop, null);
        if (value!=null) tokens= value.split(" ");
        return tokens;
    }

    private static String getProp(String key, String def) {
        return Application.getInstance().getProperties().getProperty(key,def);
    }

    private static WebAppProperties getAppProps() {
        return Application.getInstance().getProperties();
    }

}

