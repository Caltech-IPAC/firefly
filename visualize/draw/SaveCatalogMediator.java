package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

/**
 * This class handles an action to save a catalog in IPAC table format to local file. 
 * @author Michael Nguyen
 * @author Xiuqin Wu
 * @see edu.caltech.ipac.visualize.draw.FixedObjectGroup
 * @see edu.caltech.ipac.visualize.draw.FixedObject
 * @see edu.caltech.ipac.util.DataType
 */

@Deprecated
public class SaveCatalogMediator
{
  private final static String PROP  = "SaveCatalogMediator";
  private final static ClassProperties _prop = 
               new ClassProperties(PROP, SaveCatalogMediator.class);

  // constants
  private final static int DECIMAL_MAX = 
     AppProperties.getIntProperty("SaveCatalogMediator.precision.max",0);
  private final static int DECIMAL_MIN = 
     AppProperties.getIntProperty("SaveCatalogMediator.precision.min",0);
  private final static int COL_LENGTH =
     AppProperties.getIntProperty("SaveCatalogMediator.column.length",0);

  // header names & constants
  private final static String BACKSLASH = "\\";
  private final static String FIXLEN = BACKSLASH + "fixlen = ";
  private final static String FIXLEN_VALUE = "T";
  private final static String ROWS_RETRIEVED = 
                                             BACKSLASH + "RowsRetreived = ";
  private final static String CAT_NAME = BACKSLASH + "CatName = ";
  private final static String NULL_STRING = "null";
  private final static String DOUBLE = "double";
  private final static String INTEGER = "int";
  private final static String CHAR = "char";
  private final static String TARGET = "target";
  private final static String RA = "ra";
  private final static String DEC = "dec";
  private final static String DEGREE = "degrees";
  private final static String SEPARATOR = "|";
  private final static String EMPTY_SEPARATOR = " ";
  private final static String NOTHING = " ";

    private DataType[] _fixedDataType = null;
  private String _maxColumn, _catalogName = null;
  private int _totalRow = 0, _totalColumn = 0, _totalFixedDataType = 0;
  private NumberFormat _decimal = new DecimalFormat();

  /**
   * constructor
   */
  public SaveCatalogMediator() 
  { 
    char[] maxColumn = new char[COL_LENGTH];

    _decimal.setMaximumFractionDigits(DECIMAL_MAX); 
    _decimal.setMinimumFractionDigits(DECIMAL_MIN); 
    Arrays.fill(maxColumn, ' ');
    _maxColumn = new String(maxColumn);
  }

  /**
   * save the catalogs to a file
   * @param file the file name to be saved
   */
  public void save(File file, FixedObjectGroup fixedObjectGroup)
                                           throws IOException {
    PrintWriter out = null;

    _fixedDataType = fixedObjectGroup.getExtraDataDefs();
    _totalRow = fixedObjectGroup.size();
    _totalColumn = fixedObjectGroup.getColumnCount();
    if (_fixedDataType != null) _totalFixedDataType = _fixedDataType.length;

    try {
      out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
      writeHeader(out);
      for (int i = 0; i < _totalRow; i++)
          out.println(extractData(fixedObjectGroup.get(i)));
    } finally { if (out != null) out.close(); }
  }

  /**
   * set the catalog name
   * @param catalogName the name of the catalog
   */
  public void setCatalogName(String catalogName) 
  {
    _catalogName = catalogName;
  }

  // ============================================================
  // ----------------------------------- Private Methods ---------------------------------------
  // ============================================================

  /**
   * extract data
   * @param fixedObject a fixed object
   */
  private String extractData(FixedObject fixedObject)
  {
    WorldPt pt = fixedObject.getEqJ2000Position();
    String extraData = "";
    String data = 
              EMPTY_SEPARATOR + validate(fixedObject.getTargetName()) +      
              EMPTY_SEPARATOR + validate(_decimal.format(pt.getLon())) +
              EMPTY_SEPARATOR + validate(_decimal.format(pt.getLat()));
    Class  classType;
    Object value;

    for (int i = 0; i < _totalFixedDataType; i++)
    {
        extraData += EMPTY_SEPARATOR;
        classType = _fixedDataType[i].getDataType();
        value = fixedObject.getExtraData(_fixedDataType[i]);
        if (classType.equals(Double.class)) {
          if (value == null)
	     extraData += validate(NULL_STRING);
	  else
	     extraData += validate(_decimal.format(value));
	  }
        else {
          if (value == null)
	     extraData += validate(NULL_STRING);
	  else
	     extraData += validate(String.valueOf(value));
	  }
    }
    if (extraData != null) data += extraData;
    return data;
  }

  /**
   * write out the header of the catalog
   * @param out the writer
   */
  private void writeHeader(PrintWriter out)
  {
    out.println(FIXLEN + FIXLEN_VALUE);
    out.println(ROWS_RETRIEVED + _totalRow);
    out.println(CAT_NAME + _catalogName);
    writeName(out);
    writeDataType(out);
    writeDataUnit(out);
    writeIsNullAllowed(out);
  }

  /**
   * write out the header (data name) of the catalog
   * @param out the writer
   */
  private void writeName(PrintWriter out)
  {
    out.print(SEPARATOR + fitColumn(TARGET));
    out.print(SEPARATOR + fitColumn(RA));
    out.print(SEPARATOR + fitColumn(DEC));
    for (int i = 0; i < _totalFixedDataType; i++)
      out.print(SEPARATOR + validate(_fixedDataType[i].getKeyName()));
    out.println(SEPARATOR);
  }
  
  /**
   * write out the header (data type) of the catalog
   * @param out the writer
   */
  private void writeDataType(PrintWriter out)
  {
    out.print(SEPARATOR + fitColumn(CHAR));
    out.print(SEPARATOR + fitColumn(DOUBLE));
    out.print(SEPARATOR + fitColumn(DOUBLE));
    Class type;
    for (int i = 0; i < _totalFixedDataType; i++)
    {
        type = _fixedDataType[i].getDataType();
        if (type.equals(Double.class))
          out.print(SEPARATOR + fitColumn(DOUBLE)); 
        else if (type.equals(Integer.class))
          out.print(SEPARATOR + fitColumn(INTEGER)); 
        else if (type.equals(String.class))
          out.print(SEPARATOR + fitColumn(CHAR)); 
        else
          out.print(SEPARATOR + fitColumn(NOTHING)); 
    }
    out.println(SEPARATOR);
  }

  /**
   * write out the header (data unit) of the catalog
   * @param out the writer
   */
  private void writeDataUnit(PrintWriter out)
  {
    out.print(SEPARATOR + fitColumn(NOTHING));
    out.print(SEPARATOR + fitColumn(DEGREE));
    out.print(SEPARATOR + fitColumn(DEGREE));
    for (int i = 0; i < _totalFixedDataType; i++)
      out.print(SEPARATOR + validate(_fixedDataType[i].getDataUnit())); 
    out.println(SEPARATOR);
  }

  /**
   * write out the header (may be null) of the catalog
   * @param out the writer
   */
  private void writeIsNullAllowed(PrintWriter out)
  {
    out.print(SEPARATOR + fitColumn(NULL_STRING));
    out.print(SEPARATOR + fitColumn(NULL_STRING));
    out.print(SEPARATOR + fitColumn(NULL_STRING));
    for (int i = 0; i < _totalFixedDataType; i++)
      if (_fixedDataType[i].getMayBeNull()) 
	 out.print(SEPARATOR + fitColumn(NULL_STRING));
      else
	 out.print(SEPARATOR + fitColumn(NOTHING));
    out.println(SEPARATOR);
  }

  /**
   * format the data
   * @param value the string to be formatted
   */
  private String fitColumn(String value)
  {
    return _maxColumn.substring(0, COL_LENGTH - value.length()) + value;
  }

  /**
   * validate the value and fit it in the column
   * @param value the value
   */
  private String validate(String value)
  {
    return ((value != null) ? fitColumn(value) : fitColumn(NOTHING)); 
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
