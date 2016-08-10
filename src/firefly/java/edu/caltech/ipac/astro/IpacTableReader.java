/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.ServerStringUtil;
import edu.caltech.ipac.util.action.ActionConst;
import edu.caltech.ipac.util.action.ClassProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * read in the file in IPAC table format 
 * @author Xiuqin Wu
 */

//TODO: must work with IrsaAncilDataGetter

public final class IpacTableReader {

    private static ClassProperties _prop= new ClassProperties(
                                                  IpacTableReader.class);

    private static final String WRONG_FORMAT = _prop.getError("wrongFormat");
    private static final String NO_DATA = _prop.getError("noData");

    private static final String DOUBLE_TYPE[]= { "doub.*", "d", "real",
                                                 "r", "float", "f"};
    private static final String INT_TYPE[]= { "int.*", "i"} ;
    private static final String LONG_TYPE[]= { "long", "l"} ;
    private static final String BOOL_TYPE[]= { "bool", "b"} ;
    private static final String STRING_TYPE[]= {"cha.*", "str.*", "s", "c"};

    private String      _line;      // to keep the line read from file
    private DataGroup   _dataGroup;
    private String      _onlyColumns[];
    private final boolean  _useFloatsForDoubles;
    private boolean _isHeadersOnlyAllow;


  private IpacTableReader(Reader fr,
                          String catName,
                          String onlyColumns[],
                          boolean useFloatsForDoubles,
                          long   estFileSize,
                          boolean isHeadersOnlyAllow )
                                   throws IpacTableException {
      _useFloatsForDoubles= useFloatsForDoubles;
      int rowsRead = 0;
      _onlyColumns= onlyColumns;
      _isHeadersOnlyAllow = isHeadersOnlyAllow;
      List<IpacTableColumn>  columnsDesc;
      DataType [] extraData;
      List<DataGroup.Attribute> attributes = null;

      //String line;
      //System.out.println("readIpacTable........: ");

      try {
          LineNumberReader reader= new LineNumberReader(fr);
          attributes = getAttributes(reader);
          columnsDesc = getHeaderInfo(reader);

          //cols = columnsDesc.size();
          //extraData= new DataType[cols];
          extraData= setupExtraData(columnsDesc);
          _dataGroup= new DataGroup(catName, extraData);

          _dataGroup.setAttributes(attributes);

          if ( columnsDesc.size() > 0 && _line != null) {
              if (estFileSize>0 && estFileSize>(_line.length()+1) ) {
                  int estLines= (int)(estFileSize / (_line.length()+1));
                  _dataGroup.ensureCapacity(estLines);
              }

              while (_line != null) {
                  addToGroup(_line, columnsDesc, _dataGroup, extraData);
                  rowsRead++;
                  _line= reader.readLine();
              }
              guessTypesIfNecessary(_dataGroup, columnsDesc);
          }
      } catch (IOException e) {
          System.out.println("IO Exception");
      }
      if (rowsRead == 0) {
          if (_isHeadersOnlyAllow) {
              //there are headers info, therefore it is not an exception
          } else {
              String name = AppProperties.getProperty("CatalogDialog.cats."
                                                      +catName+ ".ShortName");
              if (name == null)
                  name = catName;
              throw new IpacTableException(NO_DATA + ": " + name);
          }
      }
      //System.out.println("right before return ");

  }


    /**
     * Parse the file in IPAC table format and put the data in a
     * DataObjectGroup. If there is no data in the file, throw
     * IpacTableException
     */
    public static DataGroup readIpacTable(Reader fr, String catName)
                   throws IpacTableException {
        return readIpacTable(fr,catName,null, false, 0);
    }


    public static DataGroup readIpacTable(Reader fr,
                                          String catName,
                                          long estFileSize)
                   throws IpacTableException {
        return readIpacTable(fr,catName,null,  false, estFileSize);
    }

    public static DataGroup readIpacTable(Reader fr,
                                          String catName,
                                          String onlyColumns[],
                                          boolean useFloatsForDoubles,
                                          long   estFileSize)
                   throws IpacTableException {
        return readIpacTable(fr, catName, onlyColumns, useFloatsForDoubles,
                             estFileSize, false);
    }
    /**
     * Parse the file in IPAC table format and put the data in a
     * DataObjectGroup. If there is no data in the file, throw
     * IpacTableException
     *
     * @param isHeadersOnlyAllow  set to true to allow ipac table with only headers(refer to as
     *                            attributes in this class) information.
     *                            don't confuse this with column's headers(refer to as headers in this class).
     */
    public static DataGroup readIpacTable(Reader fr,
                                          String catName,
                                          String onlyColumns[],
                                          boolean useFloatsForDoubles,
                                          long   estFileSize,
                                          boolean isHeadersOnlyAllow)
                   throws IpacTableException {
        IpacTableReader tableReader= new IpacTableReader(fr,catName,
                                                         onlyColumns,
                                                         useFloatsForDoubles,
                                                         estFileSize,
                                                         isHeadersOnlyAllow);
        return tableReader._dataGroup;
    }



    public static DataGroup readIpacTable(File f, String catName)
                                                  throws IpacTableException {
        return readIpacTable(f,null, false, catName);
    }

    public static DataGroup readIpacTable(File f,
                                          String onlyColumns[],
                                          boolean useFloatsForDoubles,
                                          String catName)
                                         throws IpacTableException {
        return readIpacTable(f, onlyColumns, useFloatsForDoubles, catName, false);
    }

  /**
   * Parse the file in IPAC table format and put the data in a
   * DataObjectGroup. If there is no data in the file, throw
   * IpacTableException
   */
  public static DataGroup readIpacTable(File f,
                                        String onlyColumns[],
                                        boolean useFloatsForDoubles,
                                        String catName,
                                        boolean isHeadersOnlyAllow )
                                       throws IpacTableException {
      DataGroup retval= null;
      BufferedReader br= null;
      try {
          br= new BufferedReader(new FileReader(f),8192) ;
         retval= readIpacTable(br,
                               catName, onlyColumns, useFloatsForDoubles,
                               f.length(), isHeadersOnlyAllow);
      } catch (FileNotFoundException fnfe) {
         System.out.println("File not found Exception");
         throw new IpacTableException("File or object not found");
      } finally {
          FileUtil.silentClose(br);
      }
      return retval;
  }

    private List<DataGroup.Attribute> getAttributes(LineNumberReader reader)
                        throws IOException {

        List<DataGroup.Attribute> attributes =
                                   new ArrayList<DataGroup.Attribute>();
        reader.mark(1024);
        _line = reader.readLine();
        while (_line != null) {
            if ( _line.startsWith("\\") ) {  // line starting with '\' is a keyword line.
                DataGroup.Attribute attrib = DataGroup.Attribute.parse(_line);
                if (attrib != null) {
                    attributes.add(attrib);
                }
            } else if ( _line.indexOf("|") >= 0) {
                reader.reset(); // return the reader to the previous line.. allow getHeaderInfo code to run without changes.
                return attributes;
            }
            reader.mark(1024);
            _line = reader.readLine();
        }
        return attributes;
    }

    private List<IpacTableColumn> getHeaderInfo(LineNumberReader reader)
				                throws IpacTableException,
				       IOException {
      List<IpacTableColumn> columnsDesc;
      IpacTableColumn itc;
      int             cols;
      String          mayBeNull;

      _line = reader.readLine();
      while (_line != null) {
	 if (_line.indexOf("|") == -1)
	    _line= reader.readLine(); 
	 else
	    break;
	 }

      if ( _line == null ) {
          if (_isHeadersOnlyAllow) {
              return new ArrayList<IpacTableColumn>(0);
          } else {
              throw new IpacTableException("This table does not have column headers information.");
          }
      }

	 // got the first header line here

      try {
	 StringTokenizer tokens = new StringTokenizer(_line, "|");
	 cols = tokens.countTokens();
         int lastEndIdx= 0;
	//System.out.println("number of columns: " + cols);
	 columnsDesc = new ArrayList<IpacTableColumn>(cols);
	 for (int i=0; i<cols; i++) {
	    itc = new IpacTableColumn();
	    itc._name = tokens.nextToken().trim();
	    if (findStartEndIndex(_line, itc, lastEndIdx)) {
            lastEndIdx= itc._endIndex;
            columnsDesc.add(i, itc);
       }
    }


    cols = columnsDesc.size();
    _line= reader.readLine();  // the second header line, types
	 if (_line != null && _line.trim().startsWith("|")) {
	    tokens = new StringTokenizer(_line, "|");
	    for (int i=0; i<cols; i++) {
	       itc = columnsDesc.get(i);
	       itc._type = tokens.nextToken().trim();
	       }

	    _line= reader.readLine();  // the third header line, unit

	    if (_line != null && _line.trim().startsWith("|")) {
	       tokens = new StringTokenizer(_line, "|");
	       for (int i=0; i<cols; i++) {
		  itc = columnsDesc.get(i);
		  itc._unit = tokens.nextToken().trim();
		  }

	       _line= reader.readLine();  // the fourth header line, mayBeNull
	       if (_line.trim().startsWith("|")) {
		  tokens = new StringTokenizer(_line, "|");
		  for (int i=0; i<cols; i++) {
		     itc = columnsDesc.get(i);
             itc._badValueString = tokens.nextToken().trim();
		     itc._mayBeNull = itc._badValueString != null;
		     }
		  _line= reader.readLine();   // the first data line
		  } // 4th header line
	       } // 3rd header line
	    } //2nd header line

	 /* now get rid of first column if it is merely an indented "|"  */
     if (cols > 0) {
         itc =  columnsDesc.get(0);
         if (itc._name.trim().equals(""))
         {
            columnsDesc.remove(0);     //remove columns if no name
            cols--;
         }
     }



	 } catch (NullPointerException npex) {
	       throw new IpacTableException(WRONG_FORMAT);
	 }
      return columnsDesc;
   }

    public static boolean isSpaces(String s) {
        int     length = s.length();
        boolean retval = true;

        for (int i=0; i<length; i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                retval = false;
                break;
            }
        }
        return retval;
    }

    private void addToGroup(String line, List<IpacTableColumn> columnsDesc,
                            DataGroup dataGroup,
                            DataType extraData[]) {
        DataObject dataObj = new DataObject(dataGroup);
        int cols = columnsDesc.size();
        String tStr;
        boolean wrongData = false;
        IpacTableColumn itc;
        IpacTableColumn lastItc;
        int workingEndIdx;

        if (isSpaces(line)) return;

        // -check to see if this line is shorter then expected
        int realIdx = _line.length();
        lastItc = columnsDesc.get(columnsDesc.size() - 1);
        if (realIdx <= lastItc._startIndex) {
            System.out.println("The following line is " +
                    (lastItc._endIndex - realIdx) +
                    " characters shorter then expected. " +
                    "The Catalog may be corrupt");
            System.out.println(_line);
        }

        for (int i = 0; i < cols; i++) {
            itc = columnsDesc.get(i);
            workingEndIdx = (realIdx < itc._endIndex) ? realIdx : itc._endIndex;
            if (workingEndIdx < itc._startIndex) {
                break;
            }
            String str = line.substring(itc._startIndex, workingEndIdx);
            tStr = str.trim();
            int idx = findExtraDataIdx(extraData, itc._name);
            if (itc._useColumn) {
                wrongData = !setExtraDataItem(tStr, itc, dataObj, extraData[idx]);
            }

            if (idx >= 0) {
                // guess what the format of the column should be
                setFormatInfoWhenNecessary(extraData[idx], itc, str);
            }

        } // end loop
        if (!wrongData)
            dataGroup.add(dataObj);
    }

    private void setFormatInfoWhenNecessary(DataType dataType, IpacTableColumn colDesc, String value) {

        if (!dataType.hasFormatInfo()) {
            int width = colDesc._endIndex - colDesc._startIndex - 1;
            String formatStr = guessFormatStr(dataType, value.trim());
            DataType.FormatInfo.Align align = value.endsWith(" ") ? DataType.FormatInfo.Align.LEFT
                                                : DataType.FormatInfo.Align.RIGHT;
            if (formatStr != null) {
                DataType.FormatInfo fi = new DataType.FormatInfo(width);
                fi.setDataFormat(formatStr);
                fi.setDataAlign(align);
                dataType.setFormatInfo(fi);
            }
        }
    }


    public boolean setExtraDataItem(String          tStr,
                                    IpacTableColumn itc,
                                    DataObject      dataObj,
                                    DataType        extraData) {
        boolean success= true;
        tStr= tStr.trim();
        try {
            if (tStr.length()==0 || tStr.equals("null") ||
                    (itc._badValueString!=null && tStr.equals(itc._badValueString))) {
                if (itc._mayBeNull) {
                    dataObj.setDataElement(extraData, null);
                }
                else {
                    success= false;
                }
            }
            else if (itc._foundType==null) {
                dataObj.setDataElement(extraData, tStr);
            }
            else if (itc._foundType==Float.class) {
                     tStr= tStr.replaceAll(",", "");
                     Float v = tStr.equalsIgnoreCase(Float.toString(Float.NaN)) ? Float.NaN : Float.valueOf(tStr);
                     dataObj.setDataElement(extraData, v);
            }
            else if (itc._foundType==Double.class) {
                tStr= tStr.replaceAll(",", "");
                Double v = tStr.equalsIgnoreCase(Double.toString(Float.NaN)) ? Double.NaN : Double.valueOf(tStr);
                dataObj.setDataElement(extraData, v);

            }
            else if (itc._foundType==Integer.class) {
                if (tStr.equals("null")) {
                    success= false;
                }
                else {
                    dataObj.setDataElement(extraData, Integer.valueOf(tStr));
                }
            }
            else if (itc._foundType==Long.class) {
                if (tStr.equals("null")) {
                    success= false;
                }
                else {
                    dataObj.setDataElement(extraData, Long.valueOf(tStr));
                }
            }
            else if (itc._foundType==String.class) {
                dataObj.setDataElement(extraData, tStr);
            }
            else  {
                dataObj.setDataElement(extraData, tStr);
            }
        } catch (NumberFormatException e) {
            dataObj.setDataElement(extraData, tStr);
            itc._type=null;
            itc._type=null;
        }
        return success;
    }

  /*
  public static void main(String args[]) {
       File f= new File(args[0]);
       DataObjectGroup fixedGroup= readDummyData(f);
       System.out.println("min ra=   "+ fixedGroup.minRa()  );
       System.out.println("max ra=   "+ fixedGroup.maxRa()  );
       System.out.println("min dec=  "+ fixedGroup.minDec() );
       System.out.println("max dec=  "+ fixedGroup.maxDec() );
  }
  */


   private boolean findStartEndIndex(String          line,
                                  IpacTableColumn itc,
                                  int             startingPoint) {
      int index = line.indexOf(itc._name, startingPoint);
      int indexStartBar = line.lastIndexOf("|", index);
      int indexEndBar = line.indexOf("|", index);

      itc._startIndex = indexStartBar;  // start bar included
      itc._endIndex = indexEndBar;      // end bar not, subString() does this
      return (indexEndBar > indexStartBar);
   }

   private DataType [] setupExtraData(List<IpacTableColumn> columnsDesc) {
       int cols = columnsDesc.size();
       String colName, colKey;
       IpacTableColumn itc;
       List<DataType> extraData= new ArrayList<DataType>(columnsDesc.size());
       DataType dataType= null;

       for (int i=0; i<cols; i++) {
           itc = columnsDesc.get(i);
           if (_onlyColumns==null ||
               ServerStringUtil.matchesRegExpList(itc._name, _onlyColumns)) {
               itc._useColumn= true;
               colKey = itc._name;
               String propName= _prop.makeBase(itc._name) + "." +
                                ActionConst.COLUMN_NAME;
               colName= AppProperties.getProperty(propName,itc._name);

               if (itc._type!=null &&
                   ServerStringUtil.matchesRegExpList(itc._type, DOUBLE_TYPE, true)) {
                   itc._foundType= _useFloatsForDoubles ?
                                          Float.class : Double.class;
                    }
               else  if (itc._type!=null &&
                     ServerStringUtil.matchesRegExpList(itc._type, INT_TYPE, true)) {
                   itc._foundType= Integer.class;
               }
               else  if (itc._type!=null &&
                     ServerStringUtil.matchesRegExpList(itc._type, LONG_TYPE, true)) {
                   itc._foundType= Long.class;
               }
               else  if (itc._type!=null &&
                       ServerStringUtil.matchesRegExpList(itc._type, BOOL_TYPE, true)) {
                   itc._foundType= Boolean.class;
               }
               else {
                   itc._foundType= String.class;
               }
               dataType= new DataType(colKey, colName, itc._foundType,
                                      DataType.Importance.HIGH,
                                      itc._unit, itc._mayBeNull);
               dataType.setTypeDesc(itc._type);
               extraData.add(dataType);
           }

       }
       return extraData.toArray(new DataType[extraData.size()]);
   }

  private void guessTypesIfNecessary(DataGroup  dataGroup,
                                     List<IpacTableColumn> columnsDesc) {
      int             eIdx;
      DataType   extraData[]= dataGroup.getDataDefinitions();
      String           colKey;

      for(IpacTableColumn itc: columnsDesc) {
          if (!isRecognizedType(itc)) {  // try to guess type
	        colKey = itc._name;
                eIdx    = findExtraDataIdx(extraData, colKey);
                if (eIdx>-1) {
                   boolean guessed= guessType(dataGroup, extraData[eIdx]);
                   if (guessed) {
                         setAllInColumnToType(dataGroup, extraData[eIdx]);
                   }
                }
          }
      }
  }


  private void setAllInColumnToType(DataGroup dataGroup, DataType dataType) {
       DataObject   dataObj;
       Object        data;
       Assert.tst( dataType.isKnownType());
       for(Iterator i= dataGroup.iterator(); (i.hasNext()); ) {
            dataObj= (DataObject)i.next();
            data= dataObj.getDataElement(dataType);
            dataObj.setDataElement( dataType,
                      dataType.convertStringToData(data.toString()) );
       }
  }

  private int findExtraDataIdx(DataType extraData[], String name) {

      boolean found= false;
      int     retval= -1;
      if (extraData!=null) {
          for(int i=0; (i<extraData.length && !found); i++) {
              if (name.equals(extraData[i].getKeyName())) {
                  found= true;
                  retval= i;
              }
          }
      }
      return retval;
  }

  private boolean guessType(DataGroup dataGroup, DataType  dataType) {

       Iterator i= dataGroup.iterator();
       boolean       found         = false;
       Class         guessClass    = Object.class;
       Class         lastGuessClass= null;
       String        data;
       DataObject    dataObj;


       for(int j=0;  (i.hasNext() && j<5 && !found); j++) {
            lastGuessClass= guessClass;
            dataObj      = (DataObject)i.next();
            data          = dataObj.getDataElement(dataType).toString();
            try {
                 if (data.contains(".")) {
                     Double.valueOf(data);
                     guessClass=  _useFloatsForDoubles ?
                                             Float.class : Double.class;
                 }
                 else {
                     Integer.valueOf(data);
                     guessClass= Integer.class;
                 }
            } catch (NumberFormatException e) {
                 guessClass= String.class;
            }
            found= (guessClass==lastGuessClass);
       }
       if (found) dataType.setDataType(guessClass);
       return found;
  }

    public static String guessFormatStr(DataType type, String val) {
        if (type.getTypeDesc() != null &&
                ServerStringUtil.matchesRegExpList(type.getTypeDesc(), STRING_TYPE, true)) {
            return "%s";
        } else {
            return guessFormatStr(val, type.getDataType());
        }
    }

    private static String guessFormatStr(String val, Class cls) {

        String formatStr = null;
        try {
            //first check to see if it's numeric
            double numval = Double.parseDouble(val);

            if (Double.isNaN(numval)) {
                return null;
            } else {
                if (val.matches(".+[e|E].+")) {
                    // scientific notation
                    String convStr = val.indexOf("E") >= 0 ? "E" : "e";
                    String[] ary = val.split("e|E");
                    if (ary.length == 2) {
                        int prec = ary[0].length() - ary[0].indexOf(".") - 1;
                        return "%." + prec + convStr;
                    }
                } else  if (val.indexOf(".") >= 0) {
                    // decimal format
                    int idx = val.indexOf(".");
                    int prec = val.length() - idx - 1;
                    return "%." + prec + "f";
                } else {
                    boolean isFloat= (cls==Float.class || cls==Double.class);
                    formatStr = isFloat ?  "%.0f" : "%d";
                }
            }
        } catch (NumberFormatException e) {
             formatStr = "%s";
        }
        return formatStr;
    }

  private boolean isRecognizedType(IpacTableColumn  itc) {
      return isRecongnizedType(itc._type);
  }

//===================================================================
//      public static methods
//===================================================================
    public static boolean isRecongnizedType(String type) {
        if ( type == null ) {
            return false;
        }
        return ServerStringUtil.matchesRegExpList(type, DOUBLE_TYPE, true) ||
                ServerStringUtil.matchesRegExpList(type, INT_TYPE, true) ||
                ServerStringUtil.matchesRegExpList(type, LONG_TYPE, true) ||
                ServerStringUtil.matchesRegExpList(type, BOOL_TYPE, true) ||
                ServerStringUtil.matchesRegExpList(type, STRING_TYPE, true);
    }

    public static Class resolveClass(String type) {
        if ( ServerStringUtil.matchesRegExpList(type, DOUBLE_TYPE, true) ) {
            return Double.class;
        } else if ( ServerStringUtil.matchesRegExpList(type, INT_TYPE, true) ) {
            return Integer.class;
        } else if ( ServerStringUtil.matchesRegExpList(type, LONG_TYPE, true) ) {
            return Long.class;
        } else if ( ServerStringUtil.matchesRegExpList(type, BOOL_TYPE, true) ) {
            return Boolean.class;
        }
        return String.class;
    }

    public static Object parseString(String type, String s) {
        if ( Double.class.isAssignableFrom(resolveClass(type)) ) {
            Double v = s.equalsIgnoreCase(Double.toString(Double.NaN)) ? Double.NaN : Double.valueOf(s);
            return  v;
        } else if ( Integer.class.isAssignableFrom(resolveClass(type)) ) {
            return Integer.valueOf(s);
        } else if ( Long.class.isAssignableFrom(resolveClass(type)) ) {
            return Long.valueOf(s);
        }
        return s;
    }
//===================================================================

    private static class IpacTableColumn {
        public int     _startIndex;   // inclusive
        public int     _endIndex;     // exclusive
        public String  _name;
        public String  _type;  // can be: char, double, int
        public Class   _foundType= null;  // can be: String, Double, Float, Int
        public String  _unit;
        public boolean _useColumn= false;  // true: use this column, false: ignore
        public boolean _mayBeNull;
        public String  _badValueString = null;
    }


    public static void main(String args[]) {

        if ( args.length > 0 ) {
            try {
                DataGroup dg = IpacTableReader.readIpacTable(
                                      new File(args[0]), null, false, "test" );
            } catch (IpacTableException e) {
                e.printStackTrace();
            }
        } else {
            try {System.in.read(); } catch (IOException e) {}
            DataGroup IRAC1fixedGroup;
            File f= new File("2massmag3_formatted.tbl");

            String onlyColumns[] = {"ra", "dec", "name", "mag"};
            String catName = "IRAC1";
            try
            {
                IRAC1fixedGroup =
                    IpacTableReader.readIpacTable(f, onlyColumns, true, catName );
            }
            catch (Exception e)
            {
                System.out.println("got an exception:  " + e);
                e.printStackTrace();
            }
        }
    }


}
