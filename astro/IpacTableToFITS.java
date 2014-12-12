package edu.caltech.ipac.astro;

import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.FitsException;
import nom.tam.util.BufferedFile;

import java.io.File;
import java.io.IOException;


/**
* Convert an Ipac table file to a FITS binary table file 
*/
public final class IpacTableToFITS 
{
    public static boolean debug = false;

    static void usage()
    {
	System.out.println("usage java edu.caltech.ipac.astro.IpacTableToFITS <Ipac_filename> <FITS_filename>");
	System.exit(1);
    }

    public static void main(String[] args)
    {
	if (args.length != 2)
	{
	    usage();
	}
	String Ipac_filename = args[0];
	String FITS_filename = args[1];

	IpacTableToFITS ipac_to_fits = new IpacTableToFITS();
	try
	{
	    ipac_to_fits.convertIpacToFITS(Ipac_filename, FITS_filename);
	}
	catch (FitsException fe)
	{
	    System.out.println("got FitsException: " + fe.getMessage());
	    fe.printStackTrace();
	}
	catch (IpacTableException ite)
	{
	    System.out.println("XXX");
	    System.out.println(ite.getMessage());
	    System.out.println("YYY");
	}
	catch (IOException ioe)
	{
	    System.out.println("got IOException: " + ioe.getMessage());
	    ioe.printStackTrace();
	}
    }

    /**
    * Convert an Ipac table file on disk to a FITS binary table file on disk
    * @param Ipac_filename input filename
    * @param FITS_filename output_filename
    */
    public void convertIpacToFITS(String Ipac_filename, String FITS_filename)
	throws FitsException, IOException, IpacTableException
    {

	DataGroup data_group = null;

	File file = new File(Ipac_filename);

	data_group = IpacTableReader.readIpacTable(file, null);

	Fits f = convertToFITS(data_group);
	    if (debug)
	    {
		BinaryTableHDU bhdu = (BinaryTableHDU) f.getHDU(1);
		Header header = bhdu.getHeader();
		header.dumpHeader(System.out);
	    }

	    BufferedFile bf = new BufferedFile(FITS_filename, "rw");
	    f.write(bf);
	    bf.flush();
	    bf.close();
    }

    /**
    * Convert an Ipac table file to a FITS binary table 
    * @param data_group DataGroup preloaded with the data from the Ipac tablefile
    * @return Fits object with the data and header filled
    */
    public Fits convertToFITS(DataGroup data_group) throws FitsException, IOException
    {
	Fits f = null;

	BinaryTable btab = new BinaryTable();

	if (debug)
	{
	    System.out.println("TITLE: " + data_group.getTitle());
	    System.out.println("size: " + data_group.size());
	}
	DataType[] data_type = data_group.getDataDefinitions();
	for (int column = 0; column < data_type.length; column++)
	{
	    Class cl = data_type[column].getDataType();
	    if (debug)
	    {
		System.out.println("column = " + column);
		System.out.println("  title = " + data_type[column].getDefaultTitle());
		System.out.println("  TypeDesc = " + data_type[column].getTypeDesc());
		System.out.println("  datatype = " + cl);
	    }
	    if (cl.equals(Integer.class))
	    {
		//System.out.println("column " + column + " is an Integer");
		int int_data[] = new int[data_group.size()];
		for (int row = 0; row < data_group.size(); row++)
		{
		    DataObject data_object = data_group.get(row);
		    Object[] data = data_object.getData();
		    Integer in = (Integer) data[column];
		    int_data[row] = in.intValue();
		    //System.out.println("int_data[" + row + "] = " + int_data[row]);
		}
		btab.addColumn(int_data);

	    }
	    else if (cl.equals(Double.class))
	    {
		//System.out.println("column " + column + " is a Double");
		double dbl_data[] = new double[data_group.size()];
		for (int row = 0; row < data_group.size(); row++)
		{
		    DataObject data_object = data_group.get(row);
		    Object[] data = data_object.getData();
		    Double in = (Double) data[column];
		    dbl_data[row] = in.doubleValue();
		    //System.out.println("dbl_data[" + row + "] = " + dbl_data[row]);
		}
		btab.addColumn(dbl_data);
	    }
	    else if (cl.equals(String.class))
	    {
		//System.out.println("column " + column + " is a String");
		String str_data[] = new String[data_group.size()];
		for (int row = 0; row < data_group.size(); row++)
		{
		    DataObject data_object = data_group.get(row);
		    Object[] data = data_object.getData();
		    String in = (String) data[column];
		    str_data[row] = in;
		    //System.out.println("str_data[" + row + "] = " + str_data[row]);
		}
		btab.addColumn(str_data);
	    }
	}

	f = new Fits();
	f.addHDU(Fits.makeHDU(btab));
	BinaryTableHDU bhdu = (BinaryTableHDU) f.getHDU(1);

	for (int column = 0; column < data_type.length; column++)
	{
	    bhdu.setColumnName(column, 
	     data_type[column].getDefaultTitle(), null);
	}
	return(f);


    }
}

