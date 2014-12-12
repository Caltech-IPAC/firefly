package edu.caltech.ipac.util.action;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.OSInfo;
import edu.caltech.ipac.util.StringUtil;

import javax.swing.Action;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Properties;

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

    public static String getPlatformPref( String prop, String def) {
        String retval= def;
        int dot= prop.lastIndexOf('.');
        if (dot>-1) {
            if (dot<prop.length()-1) {
                String part1= prop.substring(0,dot);
                String part2= prop.substring(dot+1);
                retval= getPlatformProp(part1,part2,def, true, true, null);
            }
        }
        return retval;

    }


    public static String getPlatformPref( String base,
                                          String prop,
                                          String def) {
        return getPlatformProp(base,prop,def, true, true, null);
    }


    public static String getPlatformProp( String base,
                                          String prop,
                                          String def) {
        return getPlatformProp(base,prop,def, true, null);
    }

    public static String getPlatformProp( String base,
                                          String prop,
                                          String def,
                                          Properties pdb) {
        return getPlatformProp(base,prop,def, true, pdb);
    }



    public static String getPlatformProp( String base,
                                          String prop,
                                          String def,
                                          boolean useFallback,
                                          Properties pdb) {
        return getPlatformProp(base,prop,def,useFallback,false, pdb);
    }

    /**
     *
     * Look for a property that is platform specific.  The property name is
     * created by combine base and prop with the platform name between.  Therefore
     * is base is "xxx" and prop is "Name" on solaris it will look for a property
     * xxx.solaris.Name.
     * What it does next is based on the useFallback parameter.
     * if useFallback is true then the method will first look for a property
     * base.platform.prop.  If it is not found it wil then
     * look for base.prop.  If that is not found it will return
     * def.  if useFallback is false it will only look for base.platform.prop
     * and then return def.
     *
     * The supported platforms are:
     * <ul>
     * <li>mac
     * <li>solaris
     * <li>windows
     * <li>linux
     * <li>unix
     * </ul>
     *
     *
     * @param base the beginning part of the property
     * @param prop the final part of the property
     * @param def the default value
     * @param useFallback if true do the fallback query
     * @param pdb a alternate property database
     * @return results of the lookup or the default value is no results are found
     */
    public static String getPlatformProp( String base,
                                          String prop,
                                          String def,
                                          boolean useFallback,
                                          boolean isPref,
                                          Properties pdb) {
        String value= null;

        if (OSInfo.isPlatform(OSInfo.MAC)) {
            if (OSInfo.isPlatform(OSInfo.MAC_LION_OR_LATER) || OSInfo.isPlatform(OSInfo.MAC64)) {
                value= AppProperties.getProp( base + ActionConst.MAC64_PROP + prop,
                                              null, isPref, pdb);
                if (value==null) {
                    value= AppProperties.getProp( base + ActionConst.MAC_PROP + prop,
                                                  null, isPref, pdb);
                }

            } else {
                value= AppProperties.getProp( base + ActionConst.MAC_PROP + prop,
                                          null, isPref, pdb);
            }
        }
        else if (OSInfo.isPlatform(OSInfo.ANY_WINDOWS)) {
            value= AppProperties.getProp( base + ActionConst.WINDOWS_PROP + prop,
                                          null, isPref, pdb);
        }
        else if (OSInfo.isPlatform(OSInfo.ANY_UNIX)) {
            if (OSInfo.isPlatform(OSInfo.LINUX64)) {
                value= AppProperties.getProp( base + ActionConst.LINUX64_PROP + prop,
                                              null, isPref, pdb);
                if (value==null) {
                    value= AppProperties.getProp( base + ActionConst.LINUX_PROP + prop,
                                                  null, isPref, pdb);
                }
            }
            else if (OSInfo.isPlatform(OSInfo.LINUX)) {
                value= AppProperties.getProp( base + ActionConst.LINUX_PROP + prop,
                                              null, isPref, pdb);
            }
            else if (OSInfo.isPlatform(OSInfo.SUN)){
                value= AppProperties.getProp( base + ActionConst.SUN_PROP + prop,
                                              null, isPref, pdb);

            }
            if (value==null) {
                value= AppProperties.getProp(
                         base + ActionConst.UNIX_PROP + prop, null, isPref, pdb);
            }
        }

        if (value==null) {
            if (useFallback) {
                value= AppProperties.getProp(base +"."+ prop, def, isPref, pdb);
            }
            else {
                value= def;
            }
        }

        return value;
    }


    public static String documentPlatformProp(String base, String prop) {
        String result= null;
        if (OSInfo.isPlatform(OSInfo.MAC)) {
            if (OSInfo.isPlatform(OSInfo.MAC_LION_OR_LATER) || OSInfo.isPlatform(OSInfo.MAC64)) {
                result=  base + ActionConst.MAC64_PROP + prop;
            } else {
                result=  base + ActionConst.MAC_PROP + prop;
            }
        }
        else if (OSInfo.isPlatform(OSInfo.ANY_WINDOWS)) {
            result=  base + ActionConst.WINDOWS_PROP + prop;
        }
        else if (OSInfo.isPlatform(OSInfo.ANY_UNIX)) {
            if (OSInfo.isPlatform(OSInfo.LINUX64)) {
                result=  base + ActionConst.LINUX64_PROP + prop;
            }
            else if (OSInfo.isPlatform(OSInfo.LINUX)) {
                result=  base + ActionConst.LINUX_PROP + prop;
            }
            else if (OSInfo.isPlatform(OSInfo.SUN)){
                 result= base + ActionConst.SUN_PROP + prop;
            }
            if (result==null) {
                result=  base + ActionConst.UNIX_PROP + prop;
            }
            else {
                result=  " or "  +base + ActionConst.UNIX_PROP + prop;
            }
        }
        return result;
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
      if (value!=null) tokens= StringUtil.strToStrings(value);
      return tokens;
  }

  public static boolean isOptional(String propBase, Properties pdb) {
      boolean optional = false;
      if (propBase != null) {
          optional = AppProperties.getBooleanProperty(propBase, false, pdb);
      }
      return optional;
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
