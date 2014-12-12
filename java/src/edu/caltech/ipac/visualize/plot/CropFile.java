package edu.caltech.ipac.visualize.plot;

import nom.tam.image.ImageTiler;
import nom.tam.util.ArrayFuncs;
import nom.tam.fits.Header;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.ImageData;
import nom.tam.util.BufferedDataOutputStream;
import edu.caltech.ipac.util.SUTDebug;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
* Crop an image directly from disk
*/


public class CropFile {
    
    static public boolean isCropable(Fits fits)
    {
	BasicHDU hdus[];
	try
	{
	    hdus = fits.read();
	}
	catch (FitsException fe)
	{
	    return(false);
	}
	if (hdus.length > 1)
	{
	    return (false);
	}
	Header header = hdus[0].getHeader();
	int naxis = header.getIntValue("NAXIS");
	if (naxis < 3)
	{
	    return (true);
	}
	int naxis3 = header.getIntValue("NAXIS3");
	if (naxis3 > 1)
	{
	    return (false);
	}
	else
	{
	    return (true);
	}
    }

    /**
    * Crop an image directly from a file on disk, given world coordinates
    * @param fits Fits object for file on disk
    * @param wpt WorldPt of center of desired crop box
    * @param radius Radius in degrees of desired area
    * @return Fits object with cropped image
    */

    static public Fits do_crop(Fits fits, WorldPt wpt, double radius)
	throws FitsException, IOException, ProjectionException
    {
	ImageHDU h = (ImageHDU) fits.readHDU();
	Header old_header = h.getHeader();
	ImageHeader temp_hdr = new ImageHeader(old_header);
	CoordinateSys in_coordinate_sys = CoordinateSys.makeCoordinateSys(
	    temp_hdr.getJsys(), temp_hdr.file_equinox);
	Projection in_proj = temp_hdr.createProjection(in_coordinate_sys);
	ProjectionPt ipt = in_proj.getImageCoords(wpt.getLon(), wpt.getLat());
	double x = ipt.getFsamp();
	double y = ipt.getFline();
	double x_size = 2 * radius / Math.abs(temp_hdr.cdelt1);
	if (SUTDebug.isDebug())
	{
	System.out.println("x = " + x + "  y = " + y + "  x_size = " + x_size);

	}
	Fits out_fits = common_crop(h, old_header, 
	    (int) x, (int) y, (int) x_size, (int) x_size);
	return (out_fits);
    }

    /**
    * Crop an image directly from a file on disk, given image coordinates
    * @param inFits Fits object for file on disk
    * @param min_x first pixel of crop box
    * @param min_y first line of crop box
    * @param max_x last pixel of crop box
    * @param max_y last line of crop box
    * @return Fits object with cropped image
    */

    static public Fits do_crop(Fits inFits, int min_x, int min_y, int max_x, int max_y)
	throws FitsException, IOException
    {
	int x_center = (min_x + max_x ) / 2;
	int y_center = (min_y + max_y ) / 2;
	int x_size = Math.abs(max_x - min_x);
	int y_size = Math.abs(max_y - min_y);

	ImageHDU h = (ImageHDU) inFits.readHDU();
	Header old_header = h.getHeader();
	
	Fits out_fits = 
	    common_crop(h, old_header, x_center, y_center, x_size, y_size);
	return (out_fits);
    }

    /**
    * Crop images from a FITS file with extensions, given image coordinates
    * Read the images directly from the FITS file on disk and write the
    * output to disk
    * @param in_filename FITS file on disk
    * @param out_filename output FITS file on disk
    * @param min_x first pixel of crop box
    * @param min_y first line of crop box
    * @param max_x last pixel of crop box
    * @param max_y last line of crop box
    */

    static public void crop_extensions(String in_filename, String out_filename,
	int min_x, int min_y, int max_x, int max_y)
	throws FitsException, IOException
    {
	Fits in_fits = new Fits(in_filename);
	Fits out_fits = new Fits();

	int x_center = (min_x + max_x ) / 2;
	int y_center = (min_y + max_y ) / 2;
	int x_size = Math.abs(max_x - min_x);
	int y_size = Math.abs(max_y - min_y);

	int extension = 0;
	while (true)
	{
	    BasicHDU hdu = in_fits.getHDU(extension);
	    BasicHDU new_hdu;
	    if (hdu == null)
		break;
	    if (hdu instanceof ImageHDU)
	    {
		ImageHDU h = (ImageHDU) hdu;
		Header old_header = h.getHeader();
		int naxis = old_header.getIntValue("NAXIS");
		if (naxis == 0)
		{
		    /* it's a null image - probably the primary image */
		    new_hdu = hdu;
		}
		else
		{
		    Fits temp_fits = common_crop(h, old_header, 
			x_center, y_center, x_size, y_size);
		    new_hdu = temp_fits.getHDU(0);
		}
	    }
	    else
	    {
		/* not an ImageHDU - just copy input to output */
		new_hdu = hdu;
	    }
	    out_fits.addHDU(new_hdu);
	    extension++;
	}

	FileOutputStream fo = new java.io.FileOutputStream(out_filename);
	BufferedDataOutputStream o = new BufferedDataOutputStream(fo);
	out_fits.write(o);
    }


    /**
    * Crop an image directly from a file on disk with extension images, given image coordinates
    * @param inFits Fits object for file on disk
    * @param extension 0=primary header, 1 = first extension HDU, ...
    * @param min_x first pixel of crop box
    * @param min_y first line of crop box
    * @param max_x last pixel of crop box
    * @param max_y last line of crop box
    * @return Fits object with cropped image
    */

    static public Fits do_crop(Fits inFits, int extension, 
	int min_x, int min_y, int max_x, int max_y)
	throws FitsException, IOException
    {
	int x_center = (min_x + max_x ) / 2;
	int y_center = (min_y + max_y ) / 2;
	int x_size = Math.abs(max_x - min_x);
	int y_size = Math.abs(max_y - min_y);

	ImageHDU h = (ImageHDU) inFits.getHDU(extension);
	Header old_header = h.getHeader();
	
	Fits out_fits = 
	    common_crop(h, old_header, x_center, y_center, x_size, y_size);
	return (out_fits);
    }

//    /**
//    * Crop an image directly from a file on disk, given image coordinates
//    * @param fits Fits object for file on disk
//    * @param x_center center of crop box
//    * @param y_center center of crop box
//    * @param x_size width in pixels of crop box
//    * @param y_size height in pixels of crop box
//    * @return Fits object with cropped image
//    */
//
//    public Fits do_crop(Fits fits, int x_center, int y_center, 
//	int x_size, int y_size)
//	throws FitsException, IOException
//    {
//	ImageHDU h = (ImageHDU) fits.readHDU();
//	Header old_header = h.getHeader();
//	
//	Fits out_fits = 
//	    common_crop(h, old_header, x_center, y_center, x_size, y_size);
//	return (out_fits);
//    }

    static private Fits common_crop(ImageHDU h, Header old_header, 
	int x_center, int y_center, int x_size, int y_size)
	throws FitsException, IOException
    {
	if (SUTDebug.isDebug())
	{
	System.out.println("entering common_crop:  x_center = " + x_center +
	"  y_center = " + y_center + "  x_size = " + x_size + "  y_size = " + y_size);
	}
	int naxis, naxis1, naxis2;
	int x_in, y_in;
	int min_x, min_y, max_x, max_y;
	int new_naxis1, new_naxis2;

	ImageTiler tiler = h.getTiler();

	/* first, do the header */
	Header new_header = Crop.clone_header(old_header);

	int bitpix = old_header.getIntValue("BITPIX");
	naxis = old_header.getIntValue("NAXIS");
	naxis1 = old_header.getIntValue("NAXIS1");
	naxis2 = old_header.getIntValue("NAXIS2");

	min_x = x_center - x_size/2;
	max_x = x_center + x_size/2;
	min_y = y_center - y_size/2;
	max_y = y_center + y_size/2;
	if (min_x < 0)
	    min_x = 0;
	if (max_x >= naxis1)
	    max_x = naxis1 - 1;
	if (min_y < 0)
	    min_y = 0;
	if (max_y >= naxis2)
	    max_y = naxis2 - 1;
	new_naxis1 = max_x - min_x;
	new_naxis2 = max_y - min_y;

	if (SUTDebug.isDebug())
	{
	    System.out.println("min_x = " + min_x +
	    " max_x = " + max_x +
	    " min_y = " + min_y +
	    " max_y = " + max_y +
	    " new_naxis1 = " + new_naxis1 +
	    " new_naxis2 = " + new_naxis2);
	}

	new_header.addValue("NAXIS1" , new_naxis1, null);
	new_header.addValue("NAXIS2" , new_naxis2, null);

	float crpix1 = old_header.getFloatValue("CRPIX1",Float.NaN);
	new_header.addValue("CRPIX1" , (crpix1-min_x), null);
	float crpix2 = old_header.getFloatValue("CRPIX2",Float.NaN);
	new_header.addValue("CRPIX2" , (crpix2 - min_y), null);

    if (new_header.containsKey("PLTRAH"))
    {
	/* it's a PLATE projection  */
	double x_pixel_offset, y_pixel_offset;
	double x_pixel_size, y_pixel_size;
	double plt_scale;
	double ppo_coeff[], amd_x_coeff[], amd_y_coeff[];
	
	x_pixel_offset = new_header.getDoubleValue( "CNPIX1");
	new_header.addValue("CNPIX1" , (x_pixel_offset+min_x), null);  
	y_pixel_offset = new_header.getDoubleValue( "CNPIX2");
	new_header.addValue("CNPIX2" , (y_pixel_offset+min_y), null); 
    }


	/* now do the pixels */
	int[] tile_offset;
	int[] tile_size;
	int naxis3 = 0;
	if (naxis == 2)
	{
	    tile_offset = new int[]{min_y, min_x};
	    tile_size = new int[]{max_y - min_y, max_x - min_x};
		//t.getTile(tile, new int[]{y_out * decimate_factor,0}, 
		 //   new int[]{1, naxis1});
	}
	else if (naxis == 3)
	{
	    naxis3 = new_header.getIntValue("NAXIS3");
	    tile_offset = new int[]{0, min_y, min_x };
	    tile_size = new int[]{naxis3, max_y - min_y, max_x - min_x};
	}
	else
	{
	    throw new FitsException(
		"Cannot crop images with NAXIS other than 2 or 3");
	}

	int dims2[] = new int[]{new_naxis1, new_naxis2};
	int dims3[] = new int[]{new_naxis1, new_naxis2, naxis3};
	ImageData new_image_data = null;
	switch (bitpix)
	{
	case -32:
	    float[] objm32 = (float[]) tiler.getTile(tile_offset, tile_size);
	    if (naxis == 2)
	    {
		float[][] new_datam32 = new float[new_naxis2][new_naxis1];
		// make 2dim
		new_datam32 = (float[][]) ArrayFuncs.curl(objm32, dims2); 
		new_image_data = new ImageData(new_datam32);
	    }
	    else
	    {
		float[][][] new_datam32 = new float[naxis3][new_naxis2][new_naxis1];
		// make 3dim
		new_datam32 = (float[][][]) ArrayFuncs.curl(objm32, dims3); 
		new_image_data = new ImageData(new_datam32);
	    }
	    break;
	case -64:
	    double[] objm64 = (double[]) tiler.getTile(tile_offset, tile_size);
	    if (naxis == 2)
	    {
		double[][] new_datam64 = new double[new_naxis2][new_naxis1];
		// make 2dim
		new_datam64 = (double[][]) ArrayFuncs.curl(objm64, dims2); 
		new_image_data = new ImageData(new_datam64);
	    }
	    else
	    {
		double[][][] new_datam64 = new double[naxis3][new_naxis2][new_naxis1];
		// make 3dim
		new_datam64 = (double[][][]) ArrayFuncs.curl(objm64, dims3); 
		new_image_data = new ImageData(new_datam64);
	    }
	    break;
	case 32:
	    int[] obj32 = (int[]) tiler.getTile(tile_offset, tile_size);
	    if (naxis == 2)
	    {
		int[][] new_data32 = new int[new_naxis2][new_naxis1];
		// make 2dim
		new_data32 = (int[][]) ArrayFuncs.curl(obj32, dims2); 
		new_image_data = new ImageData(new_data32);
	    }
	    else
	    {
		int[][][] new_data32 = new int[naxis3][new_naxis2][new_naxis1];
		// make 3dim
		new_data32 = (int[][][]) ArrayFuncs.curl(obj32, dims3); 
		new_image_data = new ImageData(new_data32);
	    }
	    break;
	case 16:
	    short[] obj16 = (short[]) tiler.getTile(tile_offset, tile_size);
	    if (naxis == 2)
	    {
		short[][] new_data16 = new short[new_naxis2][new_naxis1];
		// make 2dim
		new_data16 = (short[][]) ArrayFuncs.curl(obj16, dims2); 
		new_image_data = new ImageData(new_data16);
	    }
	    else
	    {
		short[][][] new_data16 = new short[naxis3][new_naxis2][new_naxis1];
		// make 3dim
		new_data16 = (short[][][]) ArrayFuncs.curl(obj16, dims3); 
		new_image_data = new ImageData(new_data16);
	    }
	    break;
	case 8:
	    byte[] obj8 = (byte[]) tiler.getTile(tile_offset, tile_size);
	    if (naxis == 2)
	    {
		byte[][] new_data8 = new byte[new_naxis2][new_naxis1];
		// make 2dim
		new_data8 = (byte[][]) ArrayFuncs.curl(obj8, dims2); 
		new_image_data = new ImageData(new_data8);
	    }
	    else
	    {
		byte[][][] new_data8 = new byte[naxis3][new_naxis2][new_naxis1];
		// make 3dim
		new_data8 = (byte[][][]) ArrayFuncs.curl(obj8, dims3); 
		new_image_data = new ImageData(new_data8);
	    }
	    break;
	}
	ImageHDU new_image_HDU = new ImageHDU(new_header, new_image_data);
	Fits out_fits = new Fits();
	out_fits.addHDU(new_image_HDU);
	return (out_fits);
    }


    private static void usage()
    {
	System.out.println(
"usage: java CropFile <input_file> <output_file> <min_x> <min_y> <max_x> <max_y>");
	System.out.println(
"or: java CropFile <input_file> <output_file> <extension> <min_x> <min_y> <max_x> <max_y>");
	System.out.println(
"or: java CropFile <input_file> <output_file> <ra> <dec> <radius>");
	System.exit(1);
    }

    public static void main(String args[]) 
    throws FitsException, IOException, ProjectionException
    {
	String in_name;
	String out_name;
	int min_x = 0;
	int min_y = 0;
	int max_x = 0;
	int max_y = 0;
	int extension = -1;
	double ra = 0.0;
	double dec = 0.0;
	double radius = 0.0;
	Fits out_fits;

	if (args.length == 6)
	{
	    min_x = Integer.parseInt(args[2]);
	    min_y = Integer.parseInt(args[3]);
	    max_x = Integer.parseInt(args[4]);
	    max_y = Integer.parseInt(args[5]);
	}
	else if (args.length == 7)
	{
	    extension = Integer.parseInt(args[2]);
	    min_x = Integer.parseInt(args[3]);
	    min_y = Integer.parseInt(args[4]);
	    max_x = Integer.parseInt(args[5]);
	    max_y = Integer.parseInt(args[6]);
	}
	else if (args.length == 5)
	{
	    ra = Double.parseDouble(args[2]);
	    dec = Double.parseDouble(args[3]);
	    radius = Double.parseDouble(args[4]);
	}
	else
	    usage();

	in_name = args[0];
	out_name = args[1];

	File file = new File(args[0]);
	if (!file.canRead())
	{
	    System.err.println("Cannot open file " + args[0]);
	    System.exit(1);
	}

	Fits f = new Fits(args[0]);

	System.out.println("CropFile.isCropable  = " + CropFile.isCropable(f));

	f = new Fits(args[0]);
	if (args.length == 5)
	{
	out_fits = CropFile.do_crop(f, new WorldPt(ra, dec), radius);
	}
	else if (args.length == 6)
	{
	out_fits = CropFile.do_crop(f, min_x, min_y, max_x, max_y);
	}
	else
	{
	out_fits = CropFile.do_crop(f, extension, min_x, min_y, max_x, max_y);
	}
	FileOutputStream fo = new java.io.FileOutputStream(out_name);
	BufferedDataOutputStream o = new BufferedDataOutputStream(fo);
	out_fits.write(o);
    }
}
