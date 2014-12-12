package edu.caltech.ipac.astro;

import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.DataType.FormatInfo;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.FitsException;
import nom.tam.util.BufferedFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
* Convert an Ipac table file to a FITS binary table file 
*/
public final class FITSTableReader
{
    public static boolean debug = true;

    private static final int TABLE_INT = 1;
    private static final int TABLE_DOUBLE = 2;
    private static final int TABLE_FLOAT = 3;
    private static final int TABLE_STRING = 4;
    private static final int TABLE_SHORT = 5;


    static void usage()
    {
	System.out.println("usage java edu.caltech.ipac.astro.FITSTableReader <FITS_filename> <ipac_filename>");
	System.exit(1);
    }

    public static void main(String[] args)
    {
	if (args.length != 2)
	{
	    usage();
	}
	String FITS_filename = args[0];
	String ipac_filename = args[1];


	FITSTableReader fits_to_ipac = new FITSTableReader();
	try
	{
	    List<DataGroup> dgList = fits_to_ipac.convertFITSToDataGroup(
		FITS_filename, null);

	    File output_file = new File(ipac_filename);
	    DataGroup dg = dgList.get(0);
	    IpacTableWriter.save(output_file, dg);
	}
	catch (FitsException fe)
	{
	    System.out.println("got FitsException: " + fe.getMessage());
	    fe.printStackTrace();
	}
	catch (IpacTableException ite)
	{
	    System.out.println(ite.getMessage());
	}
	catch (IOException ioe)
	{
	    System.out.println("got IOException: " + ioe.getMessage());
	    ioe.printStackTrace();
	}
    }

    /**
    * Convert a FITS binary table file on disk to an ipac table file on disk
    * @param FITS_filename input_filename
    * @param Ipac_filename output filename
    */
    public static List<DataGroup> convertFITSToDataGroup(String FITS_filename,
	String catName)
	throws FitsException, IOException, IpacTableException
    {

	DataGroup   _dataGroup;
	List<DataGroup> _dataGroupList = new ArrayList<DataGroup>();

	Fits fits_file = new Fits(FITS_filename);
	while (true)
	{
	BasicHDU current_hdu = fits_file.readHDU();
	if (current_hdu == null)
	{
	    break;
	}
	if (current_hdu instanceof BinaryTableHDU)
	{
	    BinaryTableHDU bhdu = (BinaryTableHDU) current_hdu;
	    int nrows = bhdu.getNRows();
	    //System.out.println("nrows = " + nrows);
	    int ncolumns = bhdu.getNCols();
	    //System.out.println("getNCols() = " + ncolumns);
	    int format[] = new int[ncolumns];
	    List<DataType> extraData_list = new ArrayList<DataType>(ncolumns);
	    DataType extraData[];
	    DataType dataType= null;
	    String primitive_type;
	    Class java_class;
	    int width = 0;
	    for (int i = 0; i < ncolumns; i++)
	    {
		String table_column_format = bhdu.getColumnFormat(i);
		String table_column_name = bhdu.getColumnName(i);
		DataType.FormatInfo.Align data_align;
		//System.out.println("Column " + i + ":  format = " +
		//    table_column_format + " Name = " + table_column_name); 
		if (table_column_format.contains("A"))
		{
		    format[i] = TABLE_STRING;
		    java_class = String.class;
		    primitive_type = "char";
		    String string_width = table_column_format.substring(
			 0, table_column_format.indexOf('A'));
		    //System.out.println("width string = [" + string_width + "]");
		    width = Integer.valueOf(string_width);
		    data_align = DataType.FormatInfo.Align.LEFT;

		}
		else if (table_column_format.contains("J"))
		{
		    format[i] = TABLE_INT;
		    java_class = Integer.class;
		    primitive_type = "int";
		    width = 10;
		    data_align = DataType.FormatInfo.Align.RIGHT;
		}
		else if (table_column_format.contains("I"))
		{
		    format[i] = TABLE_SHORT;
		    java_class = Short.class;
		    primitive_type = "short";
		    width = 10;
		    data_align = DataType.FormatInfo.Align.RIGHT;
		}
		else if (table_column_format.contains("D")) 
		{
		    format[i] = TABLE_DOUBLE;
		    java_class = Double.class;
		    primitive_type = "double";
		    width = 10;
		    data_align = DataType.FormatInfo.Align.RIGHT;
		}
		else if (table_column_format.contains("E"))
		{
		    format[i] = TABLE_FLOAT;
		    java_class = Float.class;
		    primitive_type = "float";
		    width = 10;
		    data_align = DataType.FormatInfo.Align.RIGHT;
		}
		else
		{
		    throw new FitsException(
		    "Unrecognized format character in FITS table file: " + 
		    format[i]);
		}
		if (width < table_column_name.length())
		{
		    width = table_column_name.length();
		}
		dataType= new DataType(table_column_name, java_class);
		dataType.setTypeDesc(primitive_type);
		DataType.FormatInfo fi = new DataType.FormatInfo(width + 1);
		fi.setDataAlign(data_align);
		//String format_string = "%s";
		//fi.setDataFormat(format_string);
		dataType.setFormatInfo(fi);
		extraData_list.add(dataType);
	    }
	    extraData = extraData_list.toArray(new DataType[extraData_list.size()]);
	    _dataGroup= new DataGroup(catName, extraData);

	    int int_value;
	    double double_value;
	    float float_value;
	    String string_value;
	    short short_value;
	    DataObject dataObj = null;
	    for (int row_number = 0; row_number < nrows; row_number++)
	    {
		//System.out.println("Starting row " + row_number );
		Object[] row = bhdu.getRow(row_number);
		dataObj = new DataObject(_dataGroup);
	    for (int i = 0; i < row.length; i++)
	    {
		//System.out.print("Starting column " + i + ":  " );

		switch(format[i])
		{
		    case TABLE_INT:
			int int_array[] = (int[])(row[i]);
			int_value = int_array[0];
			//System.out.println("got int value : " + int_value);
			Integer java_int = new Integer(int_value);
			dataObj.setDataElement(extraData[i], java_int);
			break;
		    case TABLE_SHORT:
			short short_array[] = (short[])(row[i]);
			short_value = short_array[0];
			//System.out.println("got short value : " + short_value);
			Short java_short = new Short(short_value);
			dataObj.setDataElement(extraData[i], java_short);
			break;
		    case TABLE_STRING:
			string_value = (String)row[i];
			//System.out.println("got string value : " + string_value);
			dataObj.setDataElement(extraData[i], string_value);
			break;
		    case TABLE_DOUBLE:
			double double_array[] = (double[])(row[i]);
			double_value = double_array[0];
			//System.out.println("got double value : "+ double_value);
			Double v = new Double(double_value);
			dataObj.setDataElement(extraData[i], v);
			break;
		    case TABLE_FLOAT:
			float float_array[] = (float[])(row[i]);
			float_value = float_array[0];
			//System.out.println("got float value : "+ float_value);
			Float vfloat = new Float(float_value);
			dataObj.setDataElement(extraData[i], vfloat);
			break;
		}
	    }
            _dataGroup.add(dataObj);
	    }
	    _dataGroupList.add(_dataGroup);
	}
	}
	return _dataGroupList;
    }
}

