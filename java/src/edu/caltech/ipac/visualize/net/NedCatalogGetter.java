package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.astro.ned.NedException;
import edu.caltech.ipac.astro.ned.NedObject;
import edu.caltech.ipac.astro.ned.NedReader;
import edu.caltech.ipac.astro.ned.NedResultSet;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.AppProperties;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Enumeration;

/**
 * This class handles to get catalogs for NED.
 * @author Michael Nguyen
 * @see edu.caltech.ipac.astro.ned.NedReader
 * @see edu.caltech.ipac.astro.ned.NedResultSet
 * @see edu.caltech.ipac.astro.ned.NedObject
 */

public class NedCatalogGetter
{
  private final static String BACKSLASH = "\\";
  private final static String FIXLEN = BACKSLASH + "fixlen = ";
  private final static String FIXLEN_VALUE = "T";
  private final static String ROWS_RETRIEVED = 
                                             BACKSLASH + "RowsRetreived = ";
  private final static String CAT_NAME = BACKSLASH + "CatName = ";

  private final static String DOUBLE = "double";
  private final static String INTEGER = "int";
  private final static String CHAR = "char";

  private final static String DEGREE = "degrees";
  private final static String NOTHING = " ";

  private final static String SEPARATOR = "|";
  private final static String EMPTY_SEPARATOR = " ";
  private final static String NULL_IS_ALLOWED = "null";

  /*
   * Temporary: num_note, num_photo, num_ref, distance, and ref_code
   * are not needed right now.
   */
  private final static String[] _names = 
  { 
    "target", "ra", "dec", /*"num_note", "num_photo", "num_ref", "distance",*/ "type", 
    "unc_major", "unc_minor", "unc_angle" /*, "ref_code"*/ 
  };

  private final static String[] _dataTypes =
  { 
    CHAR, DOUBLE, DOUBLE, /*INTEGER, INTEGER, 
    INTEGER, DOUBLE,*/ CHAR, DOUBLE, DOUBLE, DOUBLE /*, CHAR*/ 
  };

  private final static String[] _dataUnits =
  {
    NOTHING, DEGREE, DEGREE, /*NOTHING, NOTHING, NOTHING, 
    NOTHING , */ NOTHING, NOTHING, NOTHING, NOTHING /*, NOTHING*/
  };

  private final static int NED_OBJECT_TOTAL = _names.length;


  // constants
  private final static int COL_LENGTH =
     AppProperties.getIntProperty("NedCatalogGetter.column.length",0);

  private static String _maxColumn;
  private static NumberFormat _decimal = new DecimalFormat();


  /**
   * get the catalog
   * @param params the Ned Catalog Parameters
   * @param outFile the File
   * @exception FailedRequestException
   */  
  public static void lowlevelGetCatalog(NedCatalogParams params,
                                        File outFile)
              throws FailedRequestException
  {
    NedReader nedReader = new NedReader();
    NedResultSet nedObjects = null;
    try
    {
      nedReader.connect();
      nedObjects = nedReader.searchNearPosition(params.getRaJ2000(), 
                            params.getDecJ2000(), params.getSize()*60.0);
                            // NED radius is in arcmin, Spot in degree
      nedReader.disconnect();
    }  
    catch (UnknownHostException uhe) { uhe.printStackTrace(); }
    catch (NedException ne) { ne.printStackTrace(); }
    catch (IOException ioe) { ioe.printStackTrace(); }
    catch (Exception x) { x.printStackTrace(); }
 
     if ((nedObjects != null) && (nedObjects.size() > 0))
       lowlevelGetCatalog(nedObjects, params, outFile);
  }


  // ============================================================
  // ----------------------------------- Private Methods --------
  // ============================================================


  /**
   * get the catalog
   * @param nedObjects the Ned Result Set
   * @param params the Ned Catalog Parameters
   * @param outFile the File
   */  
  public static void lowlevelGetCatalog(NedResultSet nedObjects,
					 NedCatalogParams params, File outFile)
  {
    PrintWriter out = null;
    try
    {
      out = new PrintWriter(new BufferedWriter(new FileWriter(outFile))); 
      writeHeader(nedObjects, params, out);
      for (Enumeration i = nedObjects.elements(); i.hasMoreElements(); )
        out.println(extractData((NedObject)i.nextElement()));     
    }
    catch (IOException ioe) { ioe.printStackTrace(); }
    finally { if (out != null) out.close(); }
  }

  /**
   * extract data
   * @param nedObject the Ned Object
   */
  private static String extractData(NedObject nedObject)
  {
    String data = 
    EMPTY_SEPARATOR + validate(nedObject.getName()) +      
    EMPTY_SEPARATOR + validate(_decimal.format(nedObject.getRA())) +
    EMPTY_SEPARATOR + validate(_decimal.format(nedObject.getDec())) +
    /* Temporary: num_nodes, num_photos, num_refs, distance, and ref_code are not
       needed right now.
    EMPTY_SEPARATOR + validate(String.valueOf(nedObject.getNumberOfNotes())) +
    EMPTY_SEPARATOR + validate(String.valueOf(nedObject.getNumberOfPhotos())) +
    EMPTY_SEPARATOR + validate(String.valueOf(nedObject.getNumberOfReferences())) +
    EMPTY_SEPARATOR + validate(_decimal.format(nedObject.getDistanceToSearchCenter())) + */
    EMPTY_SEPARATOR + validate(nedObject.getType()) +
    EMPTY_SEPARATOR + validate(_decimal.format(nedObject.getUncertaintyMajor())) +
    EMPTY_SEPARATOR + validate(_decimal.format(nedObject.getUncertaintyMinor())) +
    EMPTY_SEPARATOR + validate(_decimal.format(nedObject.getUncertaintyAngle()));
    /*EMPTY_SEPARATOR + validate(nedObject.getReferenceCode());*/
    return data;
  }

  /**
   * write out the header of the catalog
   * @param nedObjects the Ned Result Set
   * @param params the Ned Catalog Parameters
   * @param out the PrintWriter
   */
  private static void writeHeader(NedResultSet nedObjects, 
                                  NedCatalogParams params, PrintWriter out)
  {
    out.println(FIXLEN + FIXLEN_VALUE);
    out.println(ROWS_RETRIEVED + nedObjects.size());
    out.println(CAT_NAME + params.getCatalogName());
    writeName(out);
    writeDataType(out);
    writeDataUnit(out);
    writeIsNullAllowed(out);
  }

  /**
   * write out the header (data name) of the catalog
   * @param out the PrintWriter
   */
  private static void writeName(PrintWriter out) { write(out, _names); }

  /**
   * write out the header (data type) of the catalog
   * @param out the PrintWriter
   */
  private static void writeDataType(PrintWriter out) { write(out, _dataTypes); }

  /**
   * write out the header (data unit) of the catalog
   * @param out the PrintWriter
   */
  private static void writeDataUnit(PrintWriter out) { write(out, _dataUnits); }

  /**
   * write out the header (data unit) of the catalog
   * @param out the PrintWriter
   * @param data the array string data objects
   */
  private static void write(PrintWriter out, String[] data)
  {
    for (int i = 0; i < data.length; i++)
      out.print(SEPARATOR + fitColumn(data[i]));
    out.println(SEPARATOR);
  }

  /**
   * write out the header (may be null) of the catalog
   * @param out the PrintWriter
   */
  private static void writeIsNullAllowed(PrintWriter out)
  {
    for (int i = 0; i < NED_OBJECT_TOTAL; i++)
      out.print(SEPARATOR + fitColumn(NULL_IS_ALLOWED));
    out.println(SEPARATOR);
  }

  /**
   * format the data
   * @param value the string to be formatted
   */
  private static String fitColumn(String value)
  {
    String retValue;
    if (value == null || value.trim().length() == 0)
       retValue = new String(_maxColumn);
    else
       retValue =  _maxColumn.substring(0, COL_LENGTH - value.length()) + value;
    return retValue;
  }

  /**
   * validate the value
   * @param valid the requirement
   * @param value the input string
   */
  private static String validate(boolean valid, String value)
  {
    return ((valid) ? fitColumn(value) : fitColumn(NOTHING)); 
  }

  /**
   * validate the value
   * @param value the input string
   */
  private static String validate(String value)
  {
    return ((value != null) ? fitColumn(value) : fitColumn(NOTHING)); 
  }
}
