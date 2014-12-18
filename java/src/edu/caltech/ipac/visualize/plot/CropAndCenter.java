package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.SUTDebug;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.Cursor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class CropAndCenter
{

    public static FitsRead do_crop(FitsRead in_fits_read, 
		double ra, double dec, double radius)
	throws FitsException
    {
	int min_x, min_y, max_x, max_y;

	ImageHeader imageHeader= in_fits_read.getImageHeader();
	CoordinateSys in_coordinate_sys = CoordinateSys.makeCoordinateSys(
	    imageHeader.getJsys(), imageHeader.file_equinox);
	Projection proj = imageHeader.createProjection(in_coordinate_sys);
    try
    {
	ProjectionPt proj_pt = proj.getImageCoords( ra, dec);
	int center_x = (int) proj_pt.getFsamp();
	int center_y = (int) proj_pt.getFline();
	double cdelt2 = imageHeader.cdelt2;
	int radius_pixels = (int) (radius / cdelt2);
	min_x = center_x - radius_pixels;
	max_x = center_x + radius_pixels;
	min_y = center_y - radius_pixels;
	max_y = center_y + radius_pixels;
	

    }
    catch (ProjectionException pe)
    {
	if (SUTDebug.isDebug())
	{
	    System.out.println("CropAndCenter: got ProjectionException: " + pe.getMessage());
	}
	throw new FitsException("Could not crop image.\n -  got ProjectionException: " + pe.getMessage());
    }

        if (SUTDebug.isDebug())
	{
	    System.out.println("RBH do_crop  min_x = " + min_x +
	    "  min_y = " + min_y + "  max_x = " + max_x + "  max_y = " + max_y);
	}
	Fits inFits = in_fits_read.getFits();
	BasicHDU myHDU = in_fits_read.getHDU();

	BasicHDU out_HDU = null;
	Fits ret_fits = new Fits();

	out_HDU = split_FITS_cube(myHDU, min_x, min_y, max_x, max_y);
	ret_fits.addHDU(out_HDU);
	FitsRead[] fits_read_array = FitsRead.createFitsReadArray(ret_fits);
	FitsRead fits_read_0 = fits_read_array[0];
	return(fits_read_0);
    }

    private static BasicHDU split_FITS_cube(BasicHDU hdu,
	int min_x, int min_y, int max_x, int max_y)
	throws FitsException
    {
	int x,y; 
	int x_out, y_out;
	byte[][] data8;
	byte[][][] data8x3;
	byte[][][][] data8x4;
	int[][] data32;
	int[][][] data32x3;
	int[][][][] data32x4;
	short[][] data16;
	short[][][] data16x3;
	short[][][][] data16x4;
	float[][] datam32;
	float[][][] datam32x3;
	float[][][][] datam32x4;
	double[][] datam64;
	double[][][] datam64x3;
	double[][][][] datam64x4;
	Header header = hdu.getHeader();
	ImageData new_image_data = null;
	int bitpix = header.getIntValue("BITPIX",-1);
	int naxis = header.getIntValue("NAXIS",0);
	int naxis1 = header.getIntValue("NAXIS1",0);
	int naxis2 = header.getIntValue("NAXIS2",0);
	int naxis3 = 0;
	if (naxis > 2)
	    naxis3 = header.getIntValue("NAXIS3",0);
	int naxis4 = 0;
	if (naxis > 3)
	    naxis4 = header.getIntValue("NAXIS4",0);
	int blank_value = header.getIntValue("BLANK", 0);
	float crpix1 = header.getFloatValue("CRPIX1",Float.NaN);
	float crpix2 = header.getFloatValue("CRPIX2",Float.NaN);
	float cnpix1 = header.getFloatValue("CNPIX1",Float.NaN);
	float cnpix2 = header.getFloatValue("CNPIX2",Float.NaN);
	float cdelt2 = header.getFloatValue("CDELT2",Float.NaN);
	if (cdelt2 < 0)
	{
	    min_y = naxis2 - min_y - 1;
	    max_y = naxis2 - max_y - 1;
	}
	//System.out.println("crpix1 = " + crpix1 + "  cnpix1 = " + cnpix1);
	if ((naxis3 > 1) || (naxis4 > 1))
	    throw new FitsException("cannot crop a FITS cube");
	if (min_x > max_x)
	{
	    int temp_x = min_x;
	    min_x = max_x;
	    max_x = temp_x;
	}
	if (min_y > max_y)
	{
	    int temp_y = min_y;
	    min_y = max_y;
	    max_y = temp_y;
	}

	int new_naxis1 = max_x - min_x + 1;
	int new_naxis2 = max_y - min_y + 1;
	BasicHDU retval ;
	Header new_header = clone_header(header);
	/*
	new_header.setNaxis(1, new_naxis1);
	new_header.setNaxis(2, new_naxis2);
	*/
	new_header.addValue("NAXIS1" , new_naxis1, null);
	new_header.addValue("NAXIS2" , new_naxis2, null);
	if (header.containsKey("PLTRAH"))
	{
	    /* it's a PLATE projection */
	    new_header.addValue("CNPIX1" , cnpix1 + min_x, null);
	    new_header.addValue("CNPIX2" , cnpix2 + min_y, null);
	}
	else
	{
	    /* it's some non-PLATE projection */
	float new_crpix1 = crpix1 - min_x;
    //System.out.println("setting CRPIX1 from " + crpix1 + "  to " + new_crpix1 );
	    new_header.addValue("CRPIX1" , crpix1 - min_x, null);
	    new_header.addValue("CRPIX2" , crpix2 - min_y, null);
	}

		switch (bitpix)
		{
		case 32:
		    if (naxis4 == 1)
		    {
		    data32x4 = (int[][][][]) hdu.getData().getData();
		    int[][][][] new_data32x4 = new int[1][1][new_naxis2][new_naxis1];

		    y_out = 0;
		    for (y = min_y; y <= max_y; y++)
		    {
			x_out = 0;
			for (x = min_x; x <= max_x; x++)
			{
			    if ((x < 0) || (x >= naxis1) ||
				(y < 0) || (y >= naxis2))
			    {
				new_data32x4[0][0][y_out][x_out] = blank_value;
			    }
			    else
			    {
			    new_data32x4[0][0][y_out][x_out] = data32x4[0][0][y][x];
			    }
			    x_out++;
			}
			y_out++;
		    }
		    new_image_data =
			new ImageData(new_data32x4);
		    }
		    else if (naxis3 == 1)
		    {
		    data32x3 = (int[][][]) hdu.getData().getData();
		    int[][][] new_data32x3 = new int[1][new_naxis2][new_naxis1];

		    y_out = 0;
		    for (y = min_y; y <= max_y; y++)
		    {
			x_out = 0;
			for (x = min_x; x <= max_x; x++)
			{
			    if ((x < 0) || (x >= naxis1) ||
				(y < 0) || (y >= naxis2))
			    {
				new_data32x3[0][y_out][x_out] = blank_value;
			    }
			    else
			    {
				//System.out.println("x = " + x + "  y = " + y +
				//"  pixel = " + data32x3[0][y][x]);
				new_data32x3[0][y_out][x_out] = data32x3[0][y][x];
			    }
			    x_out++;
			}
			y_out++;
		    }
		    //System.out.println("first pixel = " + new_data32x3[0][0][0]);
		    new_image_data =
			new ImageData(new_data32x3);
		    }
		    else
		    {
		    data32 = (int[][]) hdu.getData().getData();
		    int[][] new_data32 = new int[new_naxis2][new_naxis1];

		    y_out = 0;
		    for (y = min_y; y <= max_y; y++)
		    {
			x_out = 0;
			for (x = min_x; x <= max_x; x++)
			{
			    if ((x < 0) || (x >= naxis1) ||
				(y < 0) || (y >= naxis2))
			    {
				new_data32[y_out][x_out] = blank_value;
			    }
			    else
			    {
				new_data32[y_out][x_out] = data32[y][x];
			    }
			    x_out++;
			}
			y_out++;
		    }
		    new_image_data =
			new ImageData(new_data32);
		    }
		    break;
		case 16:
		    if (naxis4 == 1)
		    {
		    data16x4 = (short[][][][]) hdu.getData().getData();
		    short[][][][] new_data16x4 = new short[1][1][new_naxis2][new_naxis1];

		    y_out = 0;
		    for (y = min_y; y <= max_y; y++)
		    {
			x_out = 0;
			for (x = min_x; x <= max_x; x++)
			{
			    if ((x < 0) || (x >= naxis1) ||
				(y < 0) || (y >= naxis2))
			    {
				new_data16x4[0][0][y_out][x_out] = (short) blank_value;
			    }
			    else
			    {
			    new_data16x4[0][0][y_out][x_out] = data16x4[0][0][y][x];
			    }
			    x_out++;
			}
			y_out++;
		    }
		    new_image_data =
			new ImageData(new_data16x4);
		    }
		    else if (naxis3 == 1)
		    {
		    data16x3 = (short[][][]) hdu.getData().getData();
		    short[][][] new_data16x3 = new short[1][new_naxis2][new_naxis1];

		    y_out = 0;
		    for (y = min_y; y <= max_y; y++)
		    {
			x_out = 0;
			for (x = min_x; x <= max_x; x++)
			{
			    if ((x < 0) || (x >= naxis1) ||
				(y < 0) || (y >= naxis2))
			    {
				new_data16x3[0][y_out][x_out] = (short) blank_value;
			    }
			    else
			    {
				new_data16x3[0][y_out][x_out] = data16x3[0][y][x];
			    }
			    x_out++;
			}
			y_out++;
		    }
		    new_image_data =
			new ImageData(new_data16x3);
		    }
		    else
		    {
		    data16 = (short[][]) hdu.getData().getData();
		    short[][] new_data16 = new short[new_naxis2][new_naxis1];

		    y_out = 0;
		    for (y = min_y; y <= max_y; y++)
		    {
			x_out = 0;
			for (x = min_x; x <= max_x; x++)
			{
			    if ((x < 0) || (x >= naxis1) ||
				(y < 0) || (y >= naxis2))
			    {
				new_data16[y_out][x_out] = (short) blank_value;
			    }
			    else
			    {
				new_data16[y_out][x_out] = data16[y][x];
			    }
			    x_out++;
			}
			y_out++;
		    }
		    new_image_data =
			new ImageData(new_data16);
		    }
		    break;
		case 8:
		    if (naxis4 == 1)
		    {

		    data8x4 = (byte[][][][]) hdu.getData().getData();
		    byte[][][][] new_data8x4 = new byte[1][1][new_naxis2][new_naxis1];

		    y_out = 0;
		    for (y = min_y; y <= max_y; y++)
		    {
			x_out = 0;
			for (x = min_x; x <= max_x; x++)
			{
			    if ((x < 0) || (x >= naxis1) ||
				(y < 0) || (y >= naxis2))
			    {
				new_data8x4[0][0][y_out][x_out] = (byte) blank_value;
			    }
			    else
			    {
				new_data8x4[0][0][y_out][x_out] = data8x4[0][0][y][x];
			    }
			    x_out++;
			}
			y_out++;
		    }
		    new_image_data =
			new ImageData(new_data8x4);
		    }
		    else if (naxis3 == 1)
		    {

		    data8x3 = (byte[][][]) hdu.getData().getData();
		    byte[][][] new_data8x3 = new byte[1][new_naxis2][new_naxis1];

		    y_out = 0;
		    for (y = min_y; y <= max_y; y++)
		    {
			x_out = 0;
			for (x = min_x; x <= max_x; x++)
			{
			    if ((x < 0) || (x >= naxis1) ||
				(y < 0) || (y >= naxis2))
			    {
				new_data8x3[0][y_out][x_out] = (byte) blank_value;
			    }
			    else
			    {
				new_data8x3[0][y_out][x_out] = data8x3[0][y][x];
			    }
			    x_out++;
			}
			y_out++;
		    }
		    new_image_data =
			new ImageData(new_data8x3);
		    }
		    else
		    {
		    data8 = (byte[][]) hdu.getData().getData();
		    byte[][] new_data8 = new byte[new_naxis2][new_naxis1];

		    y_out = 0;
		    for (y = min_y; y <= max_y; y++)
		    {
			x_out = 0;
			for (x = min_x; x <= max_x; x++)
			{
			    if ((x < 0) || (x >= naxis1) ||
				(y < 0) || (y >= naxis2))
			    {
				new_data8[y_out][x_out] = (byte) blank_value;
			    }
			    else
			    {
				new_data8[y_out][x_out] = data8[y][x];
			    }
			    x_out++;
			}
			y_out++;
		    }

		    new_image_data =
			new ImageData(new_data8);
		    }
		    break;
		case -32:
		    if (naxis4 == 1)
		    {
		    datam32x4 = (float[][][][]) hdu.getData().getData();
		    float[][][][] new_datam32x4 = new float[1][1][new_naxis2][new_naxis1];

		    y_out = 0;
		    for (y = min_y; y <= max_y; y++)
		    {
			x_out = 0;
			for (x = min_x; x <= max_x; x++)
			{
			    if ((x < 0) || (x >= naxis1) ||
				(y < 0) || (y >= naxis2))
			    {
				new_datam32x4[0][0][y_out][x_out] = Float.NaN;
			    }
			    else
			    {
				new_datam32x4[0][0][y_out][x_out] = datam32x4[0][0][y][x];
			    }
			    x_out++;
			}
			y_out++;
		    }
		    new_image_data =
			new ImageData(new_datam32x4);
		    }
		    else if (naxis3 == 1)
		    {

		    datam32x3 = (float[][][]) hdu.getData().getData();
		    float[][][] new_datam32x3 = new float[1][new_naxis2][new_naxis1];

		    y_out = 0;
		    for (y = min_y; y <= max_y; y++)
		    {
			x_out = 0;
			for (x = min_x; x <= max_x; x++)
			{
			    if ((x < 0) || (x >= naxis1) ||
				(y < 0) || (y >= naxis2))
			    {
				new_datam32x3[0][y_out][x_out] = Float.NaN;
			    }
			    else
			    {
				new_datam32x3[0][y_out][x_out] = datam32x3[0][y][x];
			    }
			    x_out++;
			}
			y_out++;
		    }
		    new_image_data =
			new ImageData(new_datam32x3);
		    }
		    else
		    {
		    datam32 = (float[][]) hdu.getData().getData();
		    float[][] new_datam32 = new float[new_naxis2][new_naxis1];

		    y_out = 0;
		    for (y = min_y; y <= max_y; y++)
		    {
			x_out = 0;
			for (x = min_x; x <= max_x; x++)
			{
			    if ((x < 0) || (x >= naxis1) ||
				(y < 0) || (y >= naxis2))
			    {
				new_datam32[y_out][x_out] = Float.NaN;
			    }
			    else
			    {
				new_datam32[y_out][x_out] = datam32[y][x];
			    }
			    x_out++;
			}
			y_out++;
		    }

		    new_image_data =
			new ImageData(new_datam32);
		    }

		    break;
		case -64:
		    if (naxis4 == 1)
		    {

		    datam64x4 = (double[][][][]) hdu.getData().getData();
		    double[][][][] new_datam64x4 = new double[1][1][new_naxis2][new_naxis1];

		    y_out = 0;
		    for (y = min_y; y <= max_y; y++)
		    {
			x_out = 0;
			for (x = min_x; x <= max_x; x++)
			{
			    if ((x < 0) || (x >= naxis1) ||
				(y < 0) || (y >= naxis2))
			    {
				new_datam64x4[0][0][y_out][x_out] = Double.NaN;
			    }
			    else
			    {
			    new_datam64x4[0][0][y_out][x_out] = datam64x4[0][0][y][x];
			    }
			    x_out++;
			}
			y_out++;
		    }
		    new_image_data =
			new ImageData(new_datam64x4);
		    }
		    else if (naxis3 == 1)
		    {

		    datam64x3 = (double[][][]) hdu.getData().getData();
		    double[][][] new_datam64x3 = new double[1][new_naxis2][new_naxis1];

		    y_out = 0;
		    for (y = min_y; y <= max_y; y++)
		    {
			x_out = 0;
			for (x = min_x; x <= max_x; x++)
			{
			    if ((x < 0) || (x >= naxis1) ||
				(y < 0) || (y >= naxis2))
			    {
				new_datam64x3[0][y_out][x_out] = Double.NaN;
			    }
			    else
			    {
			    new_datam64x3[0][y_out][x_out] = datam64x3[0][y][x];
			    }
			    x_out++;
			}
			y_out++;
		    }
		    new_image_data =
			new ImageData(new_datam64x3);
		    }
		    else
		    {
		    datam64 = (double[][]) hdu.getData().getData();
		    double[][] new_datam64 = new double[new_naxis2][new_naxis1];

		    y_out = 0;
		    for (y = min_y; y <= max_y; y++)
		    {
			x_out = 0;
			for (x = min_x; x <= max_x; x++)
			{
			    if ((x < 0) || (x >= naxis1) ||
				(y < 0) || (y >= naxis2))
			    {
				new_datam64[y_out][x_out] = Double.NaN;
			    }
			    else
			    {
				new_datam64[y_out][x_out] = datam64[y][x];
			    }
			    x_out++;
			}
			y_out++;
		    }

		    new_image_data =
			new ImageData(new_datam64);
		    }
		    break;
		default:
		    Assert.tst(false, 
			"FitsRead.split_FITS_cube:  Unimplemented bitpix = " +
			bitpix);
		}
	    retval = new ImageHDU(new_header, new_image_data);

	return retval;
    }

    static Header clone_header(Header header)
    {
	// first collect cards from old header
	Cursor iter = header.iterator();
	String cards[] = new String[header.getNumberOfCards()];
	int i = 0;
	while (iter.hasNext())
	{
	    HeaderCard card = (HeaderCard) iter.next();
	    //System.out.println("RBH card.toString() = " + card.toString());
	    cards[i] = card.toString();
	    i++;
	}
	return(new Header(cards));
    }


static void usage()
{
   System.out.println(
       "Usage: java edu.caltech.ipac.visualize.plot.CropAndCenter source ra dec radius destination");
   System.exit(0);
}

    // main is for testing only
    public static void main(String[] args)
    {
	int i;
	double ra = Double.NaN;
	double dec = Double.NaN;
	double radius = Double.NaN;
	Fits inFits = null;
	Fits refFits = null;
	Fits newFits;
	CropAndCenter crop;
	FitsRead[] fits_read_array;
	FitsRead fits_read_0 = null;

	if (args.length < 5)
	    usage();

	String in_name = args[0];
	try
	{
	    ra = Double.parseDouble(args[1]);
	    dec = Double.parseDouble(args[2]);
	    radius = Double.parseDouble(args[3]);
	}
	catch (NumberFormatException nfe)
	{
	    System.out.println("bad number for ra or dec or radius");
	    usage();
	}
	String out_name = args[4];

	File file = new File(in_name);
	if (!file.exists())
	{
	    System.out.println("ERROR: file does not exist: " + in_name);
	    System.exit(1);
	}

	try
	{
	    inFits = new Fits(in_name);
	    fits_read_array = FitsRead.createFitsReadArray(inFits);
	    fits_read_0 = fits_read_array[0];

	}
	catch (FitsException e)
	{
	    System.out.println("got FitsException: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(1);
	}


	try
	{
	    FitsRead newFitsRead = CropAndCenter.do_crop(fits_read_0, ra, dec, radius);
	    newFits = newFitsRead.getFits();

	    FileOutputStream fo = new java.io.FileOutputStream(out_name);
	    BufferedDataOutputStream o = new BufferedDataOutputStream(fo);
	    newFits.write(o);
	}
	catch (FileNotFoundException e)
	{
	    System.out.println("got FileNotFoundException: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(1);
	}
	catch (FitsException e)
	{
	    System.out.println("got FitsException: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(1);
	}
	catch (IOException e)
	{
	    System.out.println("got IOException: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(1);
	}
	catch (OutOfMemoryError e)
	{
	    System.out.println("got OutOfMemoryError");
	    e.printStackTrace();
	    System.exit(1);
	}
	
    }
}

