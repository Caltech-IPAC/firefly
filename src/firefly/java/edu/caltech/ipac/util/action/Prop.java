/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.action;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.ServerStringUtil;

import javax.swing.Action;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Property utilities for setting properties that <i>must</i> exist
 * on swing components.
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
public class Prop  {

  public static String getTip(String propBase, Properties pdb) {
     String prop= propBase + "." + Action.SHORT_DESCRIPTION;
     return AppProperties.getProperty(prop,prop,pdb);
  }

  public static String getTip(String propBase) {
      return getTip(propBase,null);
  }

    public static String getName(String propBase, String defValue, Properties pdb) {
        String prop= propBase + "." + Action.NAME;
        return AppProperties.getProperty(prop,defValue,pdb);
    }

  public static String getName(String propBase, Properties pdb) {
     String prop= propBase + "." + Action.NAME;
     return AppProperties.getProperty(prop,prop,pdb);
  }
  public static String getName(String propBase) {
     return getName(propBase,null);
  }

  public static String[] getNames(String base, String prop[], Properties pdb) {
     String retval[]= null;
     if (base != null) {
         if (prop != null) {
            retval= new String[prop.length];
            for (int i= 0; (i<prop.length); i++)
                     retval[i]= getName(base + "." + prop[i],pdb);
         }
     }
     else {
         retval= getNames(prop);
     }
     return retval;
  }
  public static String[] getNames(String base, String prop[]) {
     return getNames(base,prop,null);
  }

  public static String[] getNames(String propBase[], Properties pdb) {
     String retval[]= null;
     if (propBase != null) {
        retval= new String[propBase.length];
        for (int i= 0; (i<propBase.length); i++)
                retval[i]= getName(propBase[i],pdb);
     }
     return retval;
  }
  public static String[] getNames(String propBase[]) {
     return getNames(propBase,null);
  }



  public static String getTitle(String propBase, Properties pdb) {
     String prop= propBase + "." + ActionConst.TITLE;
     return AppProperties.getProperty(prop,prop,pdb);
  }
  public static String getTitle(String propBase) {
     return getTitle(propBase,null);
  }


    public static URL getURL(String propBase, Properties pdb) {
        String prop= propBase + "." + ActionConst.URL;
        String s= AppProperties.getProperty(prop,prop,pdb);
        URL url= null;
        if (s!=null) {
            try {
                url= new URL(s);
            } catch (MalformedURLException e) {
                url= null;
            }
        }
        return url;
    }
    public static URL getURL(String propBase) {
        return getURL(propBase,null);
    }



  public static String getColumnName(String propBase, Properties pdb) {
     String prop= propBase + "." + ActionConst.COLUMN_NAME;
     return AppProperties.getProperty(prop,prop,pdb);
  }
  public static String getColumnName(String propBase) {
     return getColumnName(propBase,(Properties)null);
  }

  public static String getColumnName(String propBase, String def) {
     String prop= propBase + "." + ActionConst.COLUMN_NAME;
     return AppProperties.getProperty(prop,def);
  }


  public static boolean getSelected(String propBase, Properties pdb) {
     String prop= propBase + "." + ActionConst.SELECTED;
     return AppProperties.getBooleanProperty(prop,false,pdb);
  }
  public static boolean getSelected(String propBase) {
     return getSelected(propBase,null);
  }


  public static float getFloatValue(String propBase, Properties pdb) {
     String prop= propBase + "." + ActionConst.VALUE;
     return AppProperties.getFloatProperty(prop,0.0F,pdb);
  }
  public static float getFloatValue(String propBase) {
     return getFloatValue(propBase,null);
  }

   public static double getDoubleValue(String propBase, Properties pdb) {
      String prop= propBase + "." + ActionConst.VALUE;
      return AppProperties.getDoubleProperty(prop,0.0F,pdb);
   }
   public static double getDoubleValue(String propBase) {
      return getDoubleValue(propBase,null);
   }

  public static int getIntValue(String propBase, Properties pdb) {
     String prop= propBase + "." + ActionConst.VALUE;
     return AppProperties.getIntProperty(prop,0,pdb);
  }
  public static int getIntValue(String propBase) {
     return getIntValue(propBase,null);
  }

  public static long getLongValue(String propBase, Properties pdb) {
     String prop= propBase + "." + ActionConst.VALUE;
     return AppProperties.getLongProperty(prop,0L,pdb);
  }
  public static long getLongValue(String propBase) {
     return getLongValue(propBase,null);
  }

    public static String getDefault(String propBase, Properties pdb) {
       String prop= propBase + "." + ActionConst.DEFAULT;
       return AppProperties.getProperty(prop,prop,pdb);
    }
    public static String getDefault(String propBase) {
       return getDefault(propBase,null);
    }

   public static float getFloatDefault(String propBase, Properties pdb) {
      String prop= propBase + "." + ActionConst.DEFAULT;
      return AppProperties.getFloatProperty(prop,0.0F,pdb);
   }
   public static float getFloatDefault(String propBase) {
      return getFloatDefault(propBase,null);
   }

   public static double getDoubleDefault(String propBase, Properties pdb) {
      String prop= propBase + "." + ActionConst.DEFAULT;
      return AppProperties.getDoubleProperty(prop,0.0D,pdb);
   }
   public static double getDoubleDefault(String propBase) {
      return getDoubleDefault(propBase,null);
   }

   public static int getIntDefault(String propBase, Properties pdb) {
      String prop= propBase + "." + ActionConst.DEFAULT;
      return AppProperties.getIntProperty(prop,0,pdb);
   }
   public static int getIntDefault(String propBase) {
      return getIntDefault(propBase,null);
   }


   public static long getLongDefault(String propBase, Properties pdb) {
      String prop= propBase + "." + ActionConst.DEFAULT;
      return AppProperties.getLongProperty(prop,0L,pdb);
   }
   public static long getLongDefault(String propBase) {
      return getLongDefault(propBase,null);
   }


    public static String getDataType(String propBase, Properties pdb) {
        String prop= propBase + "." + ActionConst.DATA_TYPE;
        return AppProperties.getProperty(prop,null,pdb);
    }

    public static String getDataType(String propBase) {
        return getDataType(propBase,null);
    }




  public static String getError(String propBase, Properties pdb) {
     String prop= propBase + "." + ActionConst.ERROR;
     return AppProperties.getProperty(prop,prop,pdb);
  }
  public static String getError(String propBase) {
      return getError(propBase,null);
  }


  public static String getErrorDescription(String propBase, Properties pdb) {
     String prop= propBase + "." + ActionConst.ERROR_DESCRIPTION;
     return AppProperties.getProperty(prop,prop,pdb);
  }

  public static String getExtension(String propBase, Properties pdb) {
        String prop= propBase + "." + ActionConst.EXTENSION;
        return AppProperties.getProperty(prop,prop,pdb);
  }

    public static String getExtension(String propBase) {
        return getExtension(propBase,null);
    }

  public static String getErrorDescription(String propBase) {
      return getErrorDescription(propBase, null);
  }


  public static String[] getItems(String propBase) {
      return getItems(propBase, null);
  }

  public static String[] getItems(String propBase, Properties pdb) {
      String tokens[]= null;
      String prop= propBase + "." + ActionConst.ITEMS;
      String value= AppProperties.getProperty(prop, null, pdb);
      if (value!=null) tokens= ServerStringUtil.strToStrings(value);
      return tokens;
  }



}

