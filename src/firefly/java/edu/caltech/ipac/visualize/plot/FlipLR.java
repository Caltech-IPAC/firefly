/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.SUTDebug;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.ImageData;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.Cursor;
import edu.caltech.ipac.visualize.plot.projection.Projection;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FlipLR
{
    private ImageHeader in_header = null;
    private ImageHeader out_header = null;
    private Header in_fits_header = null;
    private Header out_fits_header = null;

    /* "in" info */
    private int in_naxis1;
    private int in_naxis2;



    public FitsRead do_flip(FitsRead inFitsRead)
	throws FitsException 
    {
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
	ImageData new_image_data = null;
	int naxis4 = 0;
	int naxis3 = 0;

	int i;
	ImageHeader temp_hdr = null;
	boolean good_projection = true;
	BasicHDU hdu = null;

	    try
	    {
		in_fits_header = inFitsRead.getHeader();
		if (in_fits_header == null) 
		{
		    if (SUTDebug.isDebug())
		    {
			System.out.println("HDU null! (input image)");
		    }
		    throw new FitsException("HDU null! (input image)");
		}

		if (in_fits_header.containsKey("PLTRAH"))
		{
		    throw new FitsException(
			"Cannot flip a PLATE projection image");
		}

		hdu = inFitsRead.getHDU();

		in_header = inFitsRead.getImageHeader();

		int pixel_count = in_header.naxis1 * in_header.naxis2;

		if (in_header.getProjectionName() == "UNRECOGNIZED")
		{
		    good_projection = false;
		}
	    
		if (in_header.getProjectionName() == "UNSPECIFIED")
		{
		    good_projection = false;
		}
	    

	    }
	    catch (FitsException e)
	    {
		if (SUTDebug.isDebug())
		{
		    System.out.println("got FitsException: " + e.getMessage());
		    e.printStackTrace();
		}
		throw e;
	    }

       /* get header info */

       in_naxis1 = in_header.naxis1;
       in_naxis2 = in_header.naxis2;

       if (in_header.naxis > 2)
	   naxis3 = in_fits_header.getIntValue("NAXIS3",0);
       if (in_header.naxis > 3)
	   naxis4 = in_fits_header.getIntValue("NAXIS4",0);


       /* First flip the pixels */
       out_header = new ImageHeader(in_fits_header);




	switch (in_header.bitpix)
	{
	case 32:
	    if (naxis4 == 1)
	    {
	    data32x4 = (int[][][][]) hdu.getData().getData();
	    int[][][][] new_data32x4 = new int[1][1][in_naxis2][in_naxis1];

	   for (int line = 0; line < in_naxis2; line++)
	   {
	       int in_index = in_naxis1 - 1;
	       for (int out_index = 0; out_index < in_naxis1; out_index++)
	       {
		   new_data32x4[0][0][line][out_index] = 
		       data32x4[0][0][line][in_index];
		   in_index--;
	       }
	   }

	    new_image_data =
		new ImageData(new_data32x4);
	    }
	    else if (naxis3 == 1)
	    {
	    data32x3 = (int[][][]) hdu.getData().getData();
	    int[][][] new_data32x3 = new int[1][in_naxis2][in_naxis1];

	   for (int line = 0; line < in_naxis2; line++)
	   {
	       int in_index = in_naxis1 - 1;
	       for (int out_index = 0; out_index < in_naxis1; out_index++)
	       {
		   new_data32x3[0][line][out_index] = 
		       data32x3[0][line][in_index];
		   in_index--;
	       }
	   }

	    //System.out.println("first pixel = " + new_data32x3[0][0][0]);
	    new_image_data =
		new ImageData(new_data32x3);
	    }
	    else
	    {
	    data32 = (int[][]) hdu.getData().getData();
	    int[][] new_data32 = new int[in_naxis2][in_naxis1];

	   for (int line = 0; line < in_naxis2; line++)
	   {
	       int in_index = in_naxis1 - 1;
	       for (int out_index = 0; out_index < in_naxis1; out_index++)
	       {
		   new_data32[line][out_index] = data32[line][in_index];
		   in_index--;
	       }
	   }

	    new_image_data =
		new ImageData(new_data32);
	    }
	    break;
	case 16:
	    if (naxis4 == 1)
	    {
	    data16x4 = (short[][][][]) hdu.getData().getData();
	    short[][][][] new_data16x4 = new short[1][1][in_naxis2][in_naxis1];

	   for (int line = 0; line < in_naxis2; line++)
	   {
	       int in_index = in_naxis1 - 1;
	       for (int out_index = 0; out_index < in_naxis1; out_index++)
	       {
		   new_data16x4[0][0][line][out_index] = 
		       data16x4[0][0][line][in_index];
		   in_index--;
	       }
	   }

	    new_image_data =
		new ImageData(new_data16x4);
	    }
	    else if (naxis3 == 1)
	    {
	    data16x3 = (short[][][]) hdu.getData().getData();
	    short[][][] new_data16x3 = new short[1][in_naxis2][in_naxis1];

	   for (int line = 0; line < in_naxis2; line++)
	   {
	       int in_index = in_naxis1 - 1;
	       for (int out_index = 0; out_index < in_naxis1; out_index++)
	       {
		   new_data16x3[0][line][out_index] = 
		       data16x3[0][line][in_index];
		   in_index--;
	       }
	   }

	    new_image_data =
		new ImageData(new_data16x3);
	    }
	    else
	    {
	    data16 = (short[][]) hdu.getData().getData();
	    short[][] new_data16 = new short[in_naxis2][in_naxis1];

	   for (int line = 0; line < in_naxis2; line++)
	   {
	       int in_index = in_naxis1 - 1;
	       for (int out_index = 0; out_index < in_naxis1; out_index++)
	       {
		   new_data16[line][out_index] = data16[line][in_index];
		   in_index--;
	       }
	   }

	    new_image_data =
		new ImageData(new_data16);
	    }
	    break;
	case 8:
	    if (naxis4 == 1)
	    {

	    data8x4 = (byte[][][][]) hdu.getData().getData();
	    byte[][][][] new_data8x4 = new byte[1][1][in_naxis2][in_naxis1];

	   for (int line = 0; line < in_naxis2; line++)
	   {
	       int in_index = in_naxis1 - 1;
	       for (int out_index = 0; out_index < in_naxis1; out_index++)
	       {
		   new_data8x4[0][0][line][out_index] = 
		       data8x4[0][0][line][in_index];
		   in_index--;
	       }
	   }

	    new_image_data =
		new ImageData(new_data8x4);
	    }
	    else if (naxis3 == 1)
	    {

	    data8x3 = (byte[][][]) hdu.getData().getData();
	    byte[][][] new_data8x3 = new byte[1][in_naxis2][in_naxis1];

	   for (int line = 0; line < in_naxis2; line++)
	   {
	       int in_index = in_naxis1 - 1;
	       for (int out_index = 0; out_index < in_naxis1; out_index++)
	       {
		   new_data8x3[0][line][out_index] = data8x3[0][line][in_index];
		   in_index--;
	       }
	   }

	    new_image_data =
		new ImageData(new_data8x3);
	    }
	    else
	    {
	    data8 = (byte[][]) hdu.getData().getData();
	    byte[][] new_data8 = new byte[in_naxis2][in_naxis1];

	   for (int line = 0; line < in_naxis2; line++)
	   {
	       int in_index = in_naxis1 - 1;
	       for (int out_index = 0; out_index < in_naxis1; out_index++)
	       {
		   new_data8[line][out_index] = data8[line][in_index];
		   in_index--;
	       }
	   }

	    new_image_data =
		new ImageData(new_data8);
	    }
	    break;
	case -32:
	    if (naxis4 == 1)
	    {
	    datam32x4 = (float[][][][]) hdu.getData().getData();
	    float[][][][] new_datam32x4 = new float[1][1][in_naxis2][in_naxis1];

	   for (int line = 0; line < in_naxis2; line++)
	   {
	       int in_index = in_naxis1 - 1;
	       for (int out_index = 0; out_index < in_naxis1; out_index++)
	       {
		   new_datam32x4[0][0][line][out_index] = 
		       datam32x4[0][0][line][in_index];
		   in_index--;
	       }
	   }

	    new_image_data =
		new ImageData(new_datam32x4);
	    }
	    else if (naxis3 == 1)
	    {

	    datam32x3 = (float[][][]) hdu.getData().getData();
	    float[][][] new_datam32x3 = new float[1][in_naxis2][in_naxis1];

	   for (int line = 0; line < in_naxis2; line++)
	   {
	       int in_index = in_naxis1 - 1;
	       for (int out_index = 0; out_index < in_naxis1; out_index++)
	       {
		   new_datam32x3[0][line][out_index] = 
		       datam32x3[0][line][in_index];
		   in_index--;
	       }
	   }

	    new_image_data =
		new ImageData(new_datam32x3);
	    }
	    else
	    {
	    datam32 = (float[][]) hdu.getData().getData();
	    float[][] new_datam32 = new float[in_naxis2][in_naxis1];

	   for (int line = 0; line < in_naxis2; line++)
	   {
	       int in_index = in_naxis1 - 1;
	       for (int out_index = 0; out_index < in_naxis1; out_index++)
	       {
		   new_datam32[line][out_index] = datam32[line][in_index];
		   in_index--;
	       }
	   }

	    new_image_data =
		new ImageData(new_datam32);
	    }

	    break;
	case -64:
	    if (naxis4 == 1)
	    {

	    datam64x4 = (double[][][][]) hdu.getData().getData();
	    double[][][][] new_datam64x4 = new double[1][1][in_naxis2][in_naxis1];

	   for (int line = 0; line < in_naxis2; line++)
	   {
	       int in_index = in_naxis1 - 1;
	       for (int out_index = 0; out_index < in_naxis1; out_index++)
	       {
		   new_datam64x4[0][0][line][out_index] = 
		       datam64x4[0][0][line][in_index];
		   in_index--;
	       }
	   }

	    new_image_data =
		new ImageData(new_datam64x4);
	    }
	    else if (naxis3 == 1)
	    {

	    datam64x3 = (double[][][]) hdu.getData().getData();
	    double[][][] new_datam64x3 = new double[1][in_naxis2][in_naxis1];

	   for (int line = 0; line < in_naxis2; line++)
	   {
	       int in_index = in_naxis1 - 1;
	       for (int out_index = 0; out_index < in_naxis1; out_index++)
	       {
		   new_datam64x3[0][line][out_index] = 
		       datam64x3[0][line][in_index];
		   in_index--;
	       }
	   }

	    new_image_data =
		new ImageData(new_datam64x3);
	    }
	    else
	    {
		datam64 = (double[][]) hdu.getData().getData();
		double[][] new_datam64 = new double[in_naxis2][in_naxis1];

	   for (int line = 0; line < in_naxis2; line++)
	   {
	       int in_index = in_naxis1 - 1;
	       for (int out_index = 0; out_index < in_naxis1; out_index++)
	       {
		   new_datam64[line][out_index] = datam64[line][in_index];
		   in_index--;
	       }
	    }

	    new_image_data =
		new ImageData(new_datam64);
	    }
	    break;
	default:
	    throw new FitsException("FlipLR.do_flip: Unimplemented bitpix = " +
		in_header.bitpix);
	}




       /* Now fix the header */
       out_fits_header = FitsRead.cloneHeader(in_fits_header);
       out_fits_header.addValue("CRPIX1", 
	   in_header.naxis1 - in_header.crpix1 +1 , null);
       if (in_header.using_cd)
       {
	    double cd1_1, cd2_1;
	    if (in_fits_header.containsKey("CD1_1"))
	    {
	       cd1_1 = in_fits_header.getDoubleValue("CD1_1");
	       cd2_1 = in_fits_header.getDoubleValue("CD2_1");
	       out_fits_header.addValue("CD1_1", -cd1_1, null);
	       out_fits_header.addValue("CD2_1", -cd2_1, null);
	    }

	    else if (in_fits_header.containsKey("CD001001"))
	    {
	       cd1_1 = in_fits_header.getDoubleValue("CD001001");
	       cd2_1 = in_fits_header.getDoubleValue("CD002001");
	       out_fits_header.addValue("CD001001", -cd1_1, null);
	       out_fits_header.addValue("CD002001", -cd2_1, null);
	    }

	    else if (in_fits_header.containsKey("PC1_1"))
	    {
	       cd1_1 = in_fits_header.getDoubleValue("PC1_1");
	       cd2_1 = in_fits_header.getDoubleValue("PC2_1");
	       out_fits_header.addValue("PC1_1", -cd1_1, null);
	       out_fits_header.addValue("PC2_1", -cd2_1, null);
	    }
       }
       else
       {
	   out_fits_header.addValue("CDELT1", -in_header.cdelt1, null);
       }

       if (in_header.map_distortion)
       {
	    int  j;
	    String keyword;

	/* negate all even coefficients */
	if (in_header.a_order>= 0)
	{
	    for (i = 0; i <= in_header.a_order; i+=2)  // do only even i values
	    {
		for (j = 0; j <= in_header.a_order; j++)
		{
		    if (i + j <= in_header.a_order)
		    {
			keyword = "A_" + i + "_" + j;
			out_fits_header.addValue(keyword, -in_header.a[i][j], null);
			/*
			System.out.println("a[" + i + "][" + j + "] = " + in_header.a[i][j]);
			*/
		    }
		}
	    }
	}

	if (in_header.b_order>= 0)
	{
	    for (i = 1; i <= in_header.b_order; i+=2) // do only odd i values
	    {
		for (j = 0; j <= in_header.b_order; j++)
		{
		    if (i + j <= in_header.b_order)
		    {
			keyword = "B_" + i + "_" + j;
			out_fits_header.addValue(keyword, -in_header.b[i][j], null);
			/*
			System.out.println("b[" + i + "][" + j + "] = " + in_header.b[i][j]);
			*/
		    }
		}
	    }
	}

	if (in_header.ap_order>= 0)
	{
	    for (i = 0; i <= in_header.ap_order; i+=2) // do only even i values
	    {
		for (j = 0; j <= in_header.ap_order; j++)
		{
		    if (i + j <= in_header.ap_order)
		    {
			keyword = "AP_" + i + "_" + j;
			out_fits_header.addValue(keyword, -in_header.ap[i][j], null);
			/*
			System.out.println("ap[" + i + "][" + j + "] = " + in_header.ap[i][j]);
			*/
		    }
		}
	    }
	}

	if (in_header.bp_order>= 0)
	{
	    for (i = 1; i <= in_header.bp_order; i+=2) // do only odd i values
	    {
		for (j = 0; j <= in_header.bp_order; j++)
		{
		    if (i + j <= in_header.bp_order)
		    {
			keyword = "BP_" + i + "_" + j;
			out_fits_header.addValue(keyword, -in_header.bp[i][j], null);
			/*
			System.out.println("bp[" + i + "][" + j + "] = " + in_header.bp[i][j]);
			*/
		    }
		}
	    }
	}

       }

       //out_fits_header.addValue("BITPIX", -32, null);
       //out_fits_header.deleteKey("BLANK");
       //out_fits_header.addValue("NAXIS3", 1, null);

       ImageHDU out_HDU = new ImageHDU(out_fits_header, new_image_data);
       Fits newFits = new Fits();
       newFits.addHDU(out_HDU);

       FitsRead[] outFitsRead = FitsRead.createFitsReadArray(newFits);
       FitsRead fr = outFitsRead[0];
       return fr;
    }

}
