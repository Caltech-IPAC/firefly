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
import java.io.FileOutputStream;
import java.io.IOException;




/** This class tests the ImageTiler.  It
 *  first creates a FITS file and then reads
 *  it back and allows the user to select
 *  tiles.  The values of the corner and center
 *  pixels for the selected tile are displayed.
 *  Both file and memory tiles are checked.
 */
public class Decimate {
    
    void doTile_2( ImageTiler t, 
		int x, int y, int nx, int ny) 
    throws IOException 
    {
	doTile(t, x, y, 0, nx, ny, 0, 2);
    }

    void doTile( ImageTiler t, 
		int x, int y, int z, int nx, int ny, int nz, int naxis) 
    throws IOException 
    {
	
	float[] tile = null;
	int[] int_array = null;
	if (naxis == 2)
	{
	    tile = new float[nx*ny];
	    t.getTile(tile, new int[]{y,x}, new int[]{ny,nx});
	    System.out.println("tile description: " + 
		ArrayFuncs.arrayDescription(tile));
	}
	else
	{
	    int_array = new int[nx*ny*nz];
	    t.getTile(int_array, new int[]{z,y,x}, new int[]{nz,ny,nx});
	    System.out.println("int_array description: " + 
		ArrayFuncs.arrayDescription(int_array));
	}

	
	
	for (int j=0; j<ny; j += 1) {
	    for (int i=0; i<nx; i += 1) {
		int indx = i+j*nx;
		int iraf_x = x+i+1;
		int iraf_y = y+j+1;
		if (naxis == 2)
		{
		    System.out.println("i = " + i + "  j = " + j + 
		    "  iraf_x = " + iraf_x + "  iraf_y = " + iraf_y +
		    "  nx = " + nx +
		    "  tile[" + indx +
		    "] = " + tile[i+j*nx]);
		}
		else
		{
		    System.out.println("i = " + i + "  j = " + j + 
		    "  iraf_x = " + iraf_x + "  iraf_y = " + iraf_y +
		    "  nx = " + nx +
		    "  tile[" + indx +
		    "] = " + int_array[i+j*nx]);
		}
	    }
	}
	//System.out.println(tile[0]);

    }
	
    /** test if a FITS image can be decimated
    * Fits files with multiple HDUs cannot be decimated, nor can FITS cubes
    * @param fits FITS object for the image
    * @param basic_hdu  HDU for the Fits file if it has been read - if null, this
    * method will read the HDU
    * @return boolean indicating if the image can be decimated
    */
    static public boolean isDecimateable(Fits fits, BasicHDU basic_hdu)
    {
	BasicHDU hdus[];
	ImageHDU h ;
	if (basic_hdu == null)
	{
	    try
	    {
		h = (ImageHDU) fits.readHDU();
	    }
	    catch (Exception e)
	    {
		return(false);
	    }
	}
	else
	{
	    if (!(basic_hdu instanceof ImageHDU))
	    {
		return(false);
	    }
	    h = (ImageHDU) basic_hdu;
	}
	Header header = h.getHeader();
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


    /** Decimate a FITS image 
    * @param fits FITS object for the image
    * @param basic_hdu  HDU for the Fits file if it has been read - if null, 
    * this method will read the HDU
    * @return Fits object containing the decimated image
    */
    public Fits do_decimate(Fits fits, BasicHDU basic_hdu, int decimate_factor)
    throws FitsException, IOException
    {
	int naxis, naxis1, naxis2;
	int x_in, y_in;
	int new_naxis1, new_naxis2;
	ImageHDU h ;
	if (basic_hdu == null)
	{
	    h = (ImageHDU) fits.readHDU();
	}
	else
	{
	    if (!(basic_hdu instanceof ImageHDU))
	    {
		throw new FitsException("HDU is not an ImageHDU");
	    }
	    h = (ImageHDU) basic_hdu;
	}
	Header old_header = h.getHeader();
	
	ImageTiler t = h.getTiler();

	/* first, do the header */
	Header new_header = Crop.clone_header(old_header);

	int bitpix = old_header.getIntValue("BITPIX");
	naxis = old_header.getIntValue("NAXIS");
	naxis1 = old_header.getIntValue("NAXIS1");
	naxis2 = old_header.getIntValue("NAXIS2");
	new_naxis1 = naxis1/decimate_factor;
	new_naxis2 = naxis2/decimate_factor;

	new_header.setNaxis(1, new_naxis1);
	new_header.setNaxis(2, new_naxis2);
	float crpix1 = old_header.getFloatValue("CRPIX1",Float.NaN);
	new_header.addValue("CRPIX1" , (crpix1-1)/decimate_factor + 1, null);
	float crpix2 = old_header.getFloatValue("CRPIX2",Float.NaN);
	new_header.addValue("CRPIX2" , (crpix2-1)/decimate_factor + 1, null);
	float cd1_1 = old_header.getFloatValue("CD1_1",Float.NaN);
	float cd2_1 = old_header.getFloatValue("CD2_1",Float.NaN);
	float cd1_2 = old_header.getFloatValue("CD1_2",Float.NaN);
	float cd2_2 = old_header.getFloatValue("CD2_2",Float.NaN);
	if (!Float.isNaN(cd1_1))
	{
	    new_header.addValue("CD1_1" , cd1_1 * decimate_factor, null);
	    new_header.addValue("CD2_1" , cd2_1 * decimate_factor, null);
	    new_header.addValue("CD1_2" , cd1_2 * decimate_factor, null);
	    new_header.addValue("CD2_2" , cd2_2 * decimate_factor, null);
	}
	float cdelt1 = old_header.getFloatValue("CDELT1",Float.NaN);
	float cdelt2 = old_header.getFloatValue("CDELT2",Float.NaN);
	if (!Float.isNaN(cdelt1))
	{
	    new_header.addValue("CDELT1" , cdelt1 * decimate_factor, null);
	    new_header.addValue("CDELT2" , cdelt2 * decimate_factor, null);
	}

    /* get ready for SIRTF distortion corrections and PLATE projection */
	double decimate_factors[] = new double[7];
	/* powers of decimate_factor: */
	decimate_factors[0] = 1.0 / decimate_factor;
	decimate_factors[1] = 1.0;
	decimate_factors[2] = decimate_factor;
	decimate_factors[3] = decimate_factors[2] * decimate_factor;
	decimate_factors[4] = decimate_factors[3] * decimate_factor;
	decimate_factors[5] = decimate_factors[4] * decimate_factor;
	decimate_factors[6] = decimate_factors[5] * decimate_factor;

    /* now do SIRTF distortion corrections */
    String ctype1 = new_header.getStringValue("CTYPE1") + "        ";
    String ctype1_trim = ctype1.trim();
    if ((ctype1_trim != null) && (ctype1_trim.endsWith("-SIP")))
    {

	String keyword;
	double a_order, ap_order, b_order, bp_order;
	double a[][] = new double[5][5];
	double ap[][] = new double[5][5];
	double b[][] = new double[5][5];
	double bp[][] = new double[5][5];

	a_order = new_header.getIntValue("A_ORDER");
	if (a_order>= 0)
	{
	    for (int i = 0; i <= a_order; i++)
	    {
		for (int j = 0; j <= a_order; j++)
		{
		    a[i][j] = 0.0;
		    if (i + j <= a_order)
		    {
			keyword = "A_" + i + "_" + j;
			a[i][j] = new_header.getDoubleValue(keyword, 0.0);
			System.out.println("a[" + i + "][" + j + "] = " + a[i][j]);
			new_header.addValue(keyword, 
			    a[i][j] * decimate_factors[i+j], null);
		    }
		}
	    }
	}

	b_order = new_header.getIntValue("B_ORDER");
	if (b_order>= 0)
	{
	    for (int i = 0; i <= b_order; i++)
	    {
		for (int j = 0; j <= b_order; j++)
		{
		    b[i][j] = 0.0;
		    if (i + j <= b_order)
		    {
			keyword = "B_" + i + "_" + j;
			b[i][j] = new_header.getDoubleValue(keyword, 0.0);
			System.out.println("b[" + i + "][" + j + "] = " + b[i][j]);
			new_header.addValue(keyword, 
			    b[i][j] * decimate_factors[i+j], null);
		    }
		}
	    }
	}
	ap_order = new_header.getIntValue("AP_ORDER");
	if (ap_order>= 0)
	{
	    for (int i = 0; i <= ap_order; i++)
	    {
		for (int j = 0; j <= ap_order; j++)
		{
		    ap[i][j] = 0.0;
		    if (i + j <= ap_order)
		    {
			keyword = "AP_" + i + "_" + j;
			ap[i][j] = new_header.getDoubleValue(keyword, 0.0);
			System.out.println("ap[" + i + "][" + j + "] = " + ap[i][j]);
			new_header.addValue(keyword, 
			    ap[i][j] * decimate_factors[i+j], null);
		    }
		}
	    }
	}
	bp_order = new_header.getIntValue("BP_ORDER");
	if (bp_order>= 0)
	{
	    for (int i = 0; i <= bp_order; i++)
	    {
		for (int j = 0; j <= bp_order; j++)
		{
		    bp[i][j] = 0.0;
		    if (i + j <= bp_order)
		    {
			keyword = "BP_" + i + "_" + j;
			bp[i][j] = new_header.getDoubleValue(keyword, 0.0);
			System.out.println("bp[" + i + "][" + j + "] = " + bp[i][j]);
			new_header.addValue(keyword, 
			    bp[i][j] * decimate_factors[i+j], null);
		    }
		}
	    }
	}

    }

    if (new_header.containsKey("PLTRAH"))
    {
	/* it's a PLATE projection  */
	double x_pixel_offset, y_pixel_offset;
	double x_pixel_size, y_pixel_size;
	double plt_scale;
	double ppo_coeff[], amd_x_coeff[], amd_y_coeff[];
	
	x_pixel_offset = new_header.getDoubleValue( "CNPIX1");
	new_header.addValue("CNPIX1" , (x_pixel_offset+0.5)/decimate_factor - 0.5, null);  
	y_pixel_offset = new_header.getDoubleValue( "CNPIX2");
	new_header.addValue("CNPIX2" , (y_pixel_offset+0.5)/decimate_factor - 0.5, null); 
	plt_scale = new_header.getDoubleValue( "PLTSCALE");
	new_header.addValue("PLTSCALE", plt_scale * decimate_factor, null);
	x_pixel_size = new_header.getDoubleValue( "XPIXELSZ");
	new_header.addValue("XPIXELSZ", x_pixel_size * decimate_factor, null);
	y_pixel_size = new_header.getDoubleValue( "YPIXELSZ");
	new_header.addValue("YPIXELSZ", y_pixel_size * decimate_factor, null);
    }


	/* now do the pixels */
	int[] tile_offset;
	int[] tile_size;
	if (naxis == 2)
	{
	    tile_offset = new int[]{0,0};
	    tile_size = new int[]{1, naxis1};
		//t.getTile(tile, new int[]{y_out * decimate_factor,0}, 
		 //   new int[]{1, naxis1});
	}
	else if (naxis == 3)
	{
	    int naxis3 = new_header.getIntValue("NAXIS3");
	    if (naxis3 > 1)
	    {
		throw new FitsException(
		"Cannot decimate a cube");
	    }
	    tile_offset = new int[]{0,0,0};
	    tile_size = new int[]{1, 1, naxis1};
	}
	else
	{
	    throw new FitsException(
		"Cannot decimate images with NAXIS other than 2 or 3");
	}

	ImageData new_image_data = null;
	switch (bitpix)
	{
	case -32:
	    float[][] new_datam32 = new float[new_naxis2][new_naxis1];

	    y_in = 0;
	    for (int y_out = 0; y_out < new_naxis2; y_out++)
	    {
		float[] tile = new float[naxis1];
		tile_offset[naxis - 2] = 
		    y_out * decimate_factor;  // adjust y origin
		t.getTile(tile, tile_offset, tile_size);
		//doTile_2(t, 0, y_out * decimate_factor, naxis1, 1);
		x_in = 0;
		for (int x_out = 0; x_out < new_naxis1; x_out++)
		{
		    new_datam32[y_out][x_out] = tile[x_in];
		    x_in += decimate_factor;
		}
		y_in += decimate_factor;
	    }
	    new_image_data = new ImageData(new_datam32);
	    break;
	case -64:
	    double[][] new_datam64 = new double[new_naxis2][new_naxis1];

	    y_in = 0;
	    for (int y_out = 0; y_out < new_naxis2; y_out++)
	    {
		double[] tile = new double[naxis1];
		tile_offset[naxis - 2] = 
		    y_out * decimate_factor;  // adjust y origin
		t.getTile(tile, tile_offset, tile_size);
		//doTile_2(t, 0, y_out * decimate_factor, naxis1, 1);
		x_in = 0;
		for (int x_out = 0; x_out < new_naxis1; x_out++)
		{
		    new_datam64[y_out][x_out] = tile[x_in];
		    x_in += decimate_factor;
		}
		y_in += decimate_factor;
	    }
	    new_image_data = new ImageData(new_datam64);
	    break;
	case 32:
	    int[][] new_data32 = new int[new_naxis2][new_naxis1];

	    y_in = 0;
	    for (int y_out = 0; y_out < new_naxis2; y_out++)
	    {
		int[] tile = new int[naxis1];
		tile_offset[naxis - 2] = 
		    y_out * decimate_factor;  // adjust y origin
		t.getTile(tile, tile_offset, tile_size);
		//doTile_2(t, 0, y_out * decimate_factor, naxis1, 1);
		x_in = 0;
		for (int x_out = 0; x_out < new_naxis1; x_out++)
		{
		    new_data32[y_out][x_out] = tile[x_in];
		    x_in += decimate_factor;
		}
		y_in += decimate_factor;
	    }
	    new_image_data = new ImageData(new_data32);
	    break;
	case 16:
	    short[][] new_data16 = new short[new_naxis2][new_naxis1];

	    y_in = 0;
	    for (int y_out = 0; y_out < new_naxis2; y_out++)
	    {
		short[] tile = new short[naxis1];
		tile_offset[naxis - 2] = 
		    y_out * decimate_factor;  // adjust y origin
		t.getTile(tile, tile_offset, tile_size);
		//doTile_2(t, 0, y_out * decimate_factor, naxis1, 1);
		x_in = 0;
		for (int x_out = 0; x_out < new_naxis1; x_out++)
		{
		    new_data16[y_out][x_out] = tile[x_in];
		    x_in += decimate_factor;
		}
		y_in += decimate_factor;
	    }
	    new_image_data = new ImageData(new_data16);
	    break;
	case 8:
	    byte[][] new_data8 = new byte[new_naxis2][new_naxis1];

	    y_in = 0;
	    for (int y_out = 0; y_out < new_naxis2; y_out++)
	    {
		byte[] tile = new byte[naxis1];
		tile_offset[naxis - 2] = 
		    y_out * decimate_factor;  // adjust y origin
		t.getTile(tile, tile_offset, tile_size);
		//doTile_2(t, 0, y_out * decimate_factor, naxis1, 1);
		x_in = 0;
		for (int x_out = 0; x_out < new_naxis1; x_out++)
		{
		    new_data8[y_out][x_out] = tile[x_in];
		    x_in += decimate_factor;
		}
		y_in += decimate_factor;
	    }
	    new_image_data = new ImageData(new_data8);
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
	"usage: java Decimate <input_file> <output_file> <decimate_factor>");
	System.exit(1);
    }

    public static void main(String args[]) 
    throws FitsException, IOException
    {
	if (args.length != 3)
	    usage();
	//String out_name = "decimate_out.fits";
	String in_name = args[0];
	String out_name = args[1];
	int decimate_factor = Integer.valueOf(args[2]);

	Decimate decimate = new Decimate();

	Fits f = new Fits(args[0]);
	BasicHDU basic_hdu = f.getHDU(0);
	System.out.println("decimate.isDecimateable  = " + decimate.isDecimateable(f, basic_hdu));

	//f = new Fits(args[0]);
	Fits out_fits = decimate.do_decimate(f, basic_hdu, decimate_factor);
	FileOutputStream fo = new java.io.FileOutputStream(out_name);
	BufferedDataOutputStream o = new BufferedDataOutputStream(fo);
	out_fits.write(o);
    }
}
