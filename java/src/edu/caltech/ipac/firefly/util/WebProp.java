package edu.caltech.ipac.firefly.util;


import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.util.action.ActionConst;

import java.util.Map;


/**
 * Property utilties for setting properties that <i>must</i> exist
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
public class WebProp {




  public static String getTip(String propBase, Map<String,String> pdb) {
     String prop= propBase + "." + ActionConst.SHORT_DESCRIPTION;
      return getProp(prop,prop,pdb);
  }

  public static String getTip(String propBase) {
      return getTip(propBase,null);
  }

    public static String getName(String propBase, String defValue, Map<String,String> pdb) {
        String prop= propBase + "." + ActionConst.NAME;
        return getProp(prop,defValue,pdb);
    }

  public static String getName(String propBase, Map<String,String> pdb) {
     String prop= propBase + "." + ActionConst.NAME;
     return getProp(prop,null,pdb);
  }
  public static String getName(String propBase) {
     return getName(propBase,null);
  }

  public static String[] getNames(String base, String prop[], Map<String,String> pdb) {
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

  public static String[] getNames(String propBase[], Map<String,String> pdb) {
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



  public static String getTitle(String propBase, Map<String,String> pdb) {
     String prop= propBase + "." + ActionConst.TITLE;
     return getProp(prop,prop,pdb);
  }
  public static String getTitle(String propBase) {
     return getTitle(propBase,null);
  }


    public static String getURL(String propBase, Map<String,String> pdb) {
        String prop= propBase + "." + ActionConst.URL;
        return getProp(prop,null,pdb);
    }

    public static String getURL(String propBase) {
        return getURL(propBase,null);
    }

    public static String getIcon(String propBase, Map<String,String> pdb) {
        String prop= propBase + "." + ActionConst.ICON;
        return getProp(prop,null,pdb);
    }

    public static String getIcon(String propBase) {
        return getIcon(propBase,null);
    }


    public static boolean getIconOnlyHint(String propBase, boolean defValue, Map<String,String> pdb) {
        String prop= propBase + "." + ActionConst.ICON_ONLY_HINT;
        return getAppProps().getBooleanProperty(prop,defValue,pdb);
    }
    public static boolean getIconOnlyHint(String propBase, boolean defValue) {
        return getIconOnlyHint(propBase,defValue, null);
    }


    public static boolean isImportant(String propBase, boolean defValue, Map<String,String> pdb) {
        String prop= propBase + "." + ActionConst.IMPORTANT;
        return getAppProps().getBooleanProperty(prop,defValue,pdb);
    }
    public static boolean isImportant(String propBase, boolean defValue) {
        return isImportant(propBase,defValue, null);
    }



  public static String getColumnName(String propBase, Map<String,String> pdb) {
     String prop= propBase + "." + ActionConst.COLUMN_NAME;
     return getProp(prop,prop,pdb);
  }
  public static String getColumnName(String propBase) {
     return getColumnName(propBase,(Map<String,String>)null);
  }

  public static String getColumnName(String propBase, String def) {
     String prop= propBase + "." + ActionConst.COLUMN_NAME;
     return getProp(prop,def,null);
  }


  public static boolean getSelected(String propBase, Map<String,String> pdb) {
      return getSelected(propBase,false, pdb);
  }
  public static boolean getSelected(String propBase) {
     return getSelected(propBase,null);
  }
    public static boolean getSelected(String propBase, boolean defValue, Map<String,String> pdb ) {
        String prop= propBase + "." + ActionConst.SELECTED;
        return getAppProps().getBooleanProperty(prop,defValue,pdb);
    }


  public static float getFloatValue(String propBase, Map<String,String> pdb) {
     String prop= propBase + "." + ActionConst.VALUE;
     return getAppProps().getFloatProperty(prop,0.0F,pdb);
  }
  public static float getFloatValue(String propBase) {
     return getFloatValue(propBase,null);
  }

   public static double getDoubleValue(String propBase, Map<String,String> pdb) {
      String prop= propBase + "." + ActionConst.VALUE;
      return getAppProps().getDoubleProperty(prop,0.0F,pdb);
   }
   public static double getDoubleValue(String propBase) {
      return getDoubleValue(propBase,null);
   }

  public static int getIntValue(String propBase, Map<String,String> pdb) {
     String prop= propBase + "." + ActionConst.VALUE;
     return getAppProps().getIntProperty(prop,0,pdb);
  }
  public static int getIntValue(String propBase) {
     return getIntValue(propBase,null);
  }

  public static long getLongValue(String propBase, Map<String,String> pdb) {
     String prop= propBase + "." + ActionConst.VALUE;
     return getAppProps().getLongProperty(prop,0L,pdb);
  }
  public static long getLongValue(String propBase) {
     return getLongValue(propBase,null);
  }

    public static String getDefault(String propBase, Map<String,String> pdb) {
       String prop= propBase + "." + ActionConst.DEFAULT;
       return getAppProps().getProperty(prop,prop,pdb);
    }
    public static String getDefault(String propBase) {
       return getDefault(propBase,null);
    }

   public static float getFloatDefault(String propBase, Map<String,String> pdb) {
      String prop= propBase + "." + ActionConst.DEFAULT;
      return getAppProps().getFloatProperty(prop,0.0F,pdb);
   }
   public static float getFloatDefault(String propBase) {
      return getFloatDefault(propBase,null);
   }

   public static double getDoubleDefault(String propBase, Map<String,String> pdb) {
      String prop= propBase + "." + ActionConst.DEFAULT;
      return getAppProps().getDoubleProperty(prop,0.0D,pdb);
   }
   public static double getDoubleDefault(String propBase) {
      return getDoubleDefault(propBase,null);
   }

   public static int getIntDefault(String propBase, Map<String,String> pdb) {
      String prop= propBase + "." + ActionConst.DEFAULT;
      return getAppProps().getIntProperty(prop,0,pdb);
   }
   public static int getIntDefault(String propBase) {
      return getIntDefault(propBase,null);
   }


   public static long getLongDefault(String propBase, Map<String,String> pdb) {
      String prop= propBase + "." + ActionConst.DEFAULT;
      return getAppProps().getLongProperty(prop,0L,pdb);
   }
   public static long getLongDefault(String propBase) {
      return getLongDefault(propBase,null);
   }


    public static String getDataType(String propBase, Map<String,String> pdb) {
        String prop= propBase + "." + ActionConst.DATA_TYPE;
        return getAppProps().getProperty(prop,null,pdb);
    }

    public static String getDataType(String propBase) {
        return getDataType(propBase,null);
    }




  public static String getError(String propBase, Map<String,String> pdb) {
     String prop= propBase + "." + ActionConst.ERROR;
     return getAppProps().getProperty(prop,prop,pdb);
  }
  public static String getError(String propBase) {
      return getError(propBase,null);
  }


  public static String getErrorDescription(String propBase, Map<String,String> pdb) {
     String prop= propBase + "." + ActionConst.ERROR_DESCRIPTION;
     return getAppProps().getProperty(prop,prop,pdb);
  }

  public static String getExtension(String propBase, Map<String,String> pdb) {
        String prop= propBase + "." + ActionConst.EXTENSION;
        return getAppProps().getProperty(prop,prop,pdb);
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

  public static String[] getItems(String propBase, Map<String,String> pdb) {
      String tokens[]= null;
      String prop= propBase + "." + ActionConst.ITEMS;
      String value= getAppProps().getProperty(prop, null, pdb);
      if (value!=null) tokens= value.split(" ");
      return tokens;
  }



    private static String getProp(String key,
                                  String def,
                                  Map<String,String> pdb) {
        return Application.getInstance().getProperties().getProperty(key,def,pdb);
    }

    private static WebAppProperties getAppProps() {
        return Application.getInstance().getProperties();
    }


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
