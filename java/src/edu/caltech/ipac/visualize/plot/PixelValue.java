/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class PixelValue {

    public static void main(String[] args) 
    {

	if (args.length != 3)
	{
	    System.out.println(
	    "usage:  java edu.caltech.ipac.visualize.plot.PixelValue <FITSfilename> <x> <y>");
	    System.out.println("   where the first pixel in the file is x=0 y=0");
	    System.exit(1);
	}
	int bitpix;
	double bscale, bzero, blank_value;
	int naxis;
	int naxis1;
	int naxis2;
	int naxis3;
	double cdelt2;
	long HDU_offset;
	long header_size;
	Fits myFits = null;
	BasicHDU[] myHDUs = null;

	try 
	{
	    myFits = new Fits(args[0]);
	    myHDUs = myFits.read();
	} 
	catch (FitsException e) 
	{
	    System.out.println("Caught exception e: "+e);
	    e.printStackTrace();
	    System.exit(1);
	}
	if (myHDUs == null)
	{
	    System.out.println("no HDUs in file - apparently not a FITS file");
	    System.exit(1);
	}

       
        for (int i=0; i<myHDUs.length; i++) 
	{
	    HDU_offset =  myHDUs[i].getFileOffset();
	    System.out.println("getFileOffset = " + myHDUs[i].getFileOffset());
            myHDUs[i].info();
	    Header header = myHDUs[i].getHeader();
	    header_size = header.getOriginalSize();
	    System.out.println("header.getSize() = " + header.getSize());
	    System.out.println("header.getOriginalSize() = " + header.getOriginalSize());
	    //header.dumpHeader(System.out);

	    /* In the real code, the values are obtained from ImageHeader:
	    *
	    * ImageHeader image_header
	    * data_offset = image_header.data_offset;
	    * plane_number  = image_header.plane_number;
	    * bitpix = image_header.bitpix;
	    * naxis  = image_header.naxis;
	    * naxis1 = image_header.naxis1;
	    * naxis2 = image_header.naxis2;
	    * naxis3 = image_header.naxis3;
	    * bscale = image_header.bscale;
	    * bzero  = image_header.bzero;
	    * blank_value = image_header.blank_value;
	    * cdelt2 = image_header.cdelt2;
	    *
	    *
	    */

	    long data_offset = HDU_offset + header_size;

	    //data_offset = 23440;  // DEBUG ONLY

	    System.out.println("data_offset = " + data_offset);
	    //header.dumpHeader(System.out);
	    bitpix = header.getIntValue("BITPIX",0);
	    naxis  = header.getIntValue("NAXIS",0);
	    naxis1 = header.getIntValue("NAXIS1",0);
	    naxis2 = header.getIntValue("NAXIS2",0);
	    naxis3 = header.getIntValue("NAXIS3",1);
	    bscale = header.getDoubleValue("BSCALE",1);
	    bzero = header.getDoubleValue("BZERO",0);
	    cdelt2 = header.getDoubleValue("CDELT2",0);
	    blank_value = header.getDoubleValue("BLANK", Double.NaN);
	    System.out.println("naxis3 = " + naxis3);
	    RandomAccessFile fits_file = null;

	    //int xcenter = 49; 
	    //int ycenter = 34;
	    int xcenter = Integer.parseInt(args[1]); 
	    int ycenter = Integer.parseInt(args[2]);
	    int plane_number = 1;
	    int x = xcenter;
	    int y = ycenter;

	    /*
	    for (y = ycenter-1; y <= ycenter+1; y++)
	    {
	    for (x = xcenter-1; x <= xcenter+1; x++)
	    {
	    */


	    try
	    {
		fits_file = new RandomAccessFile(args[0], "r");
	    }
	    catch (FileNotFoundException e) 
	    {
		System.out.println("Caught exception e: "+e);
		e.printStackTrace();
		System.exit(1);
	    }

	    double pixel_data = Double.NaN;
	    System.out.println("Fetching value for x = " + x + "  y = " + y);
	    try
	    {
        MiniFitsHeader miniHeader= new MiniFitsHeader(plane_number,bitpix,naxis,naxis1,naxis2,naxis3,
                                                 cdelt2,bscale,bzero,blank_value,data_offset);
		pixel_data = PixelValue.pixelVal(fits_file, x, y, miniHeader);
		fits_file.close();
	    } 
	    catch (IOException e) 
	    {
		System.out.println("Caught exception e: "+e);
		e.printStackTrace();
		System.exit(1);
	    }
	    catch (PixelValueException e) 
	    {
		System.out.println("Caught exception e: "+e);
		System.exit(1);
	    }
	    System.out.println("x = " + x + "  y = " + y +  
		"  plane_number = " + plane_number +
		"   pixel_data = " + pixel_data);

	    /*
	    }
	    }
	    */
	}

    }

    static public double pixelVal(RandomAccessFile fits_file, int x, int y,
                                  MiniFitsHeader header)
            throws IOException, PixelValueException
    {
	int plane_offset;


    int plane_number   = header.getPlaneNumber();
    int bitpix         = header.getBixpix();
    int naxis          = header.getNaxis();
    int naxis1         = header.getNaxis1();
    int naxis2         = header.getNaxis2();
    int naxis3         = header.getNaxis3();
    double cdelt2      = header.getCDelt2();
    double bscale      = header.getBScale();
    double bzero       = header.getBZero();
    double blank_value = header.getBlankValue();
    long data_offset   = header.getDataOffset();


	if ((naxis == 2) || (naxis3 == 1))
	{
	    plane_offset = 0;
	}
	else
	{
	    plane_offset = plane_number - 1;
	}

	if (cdelt2 < 0)
	{
	    y = naxis2 -1 -y;
	}

	long pixel_offset = (naxis1 * naxis2 * plane_offset) + (y * naxis1 + x);
	double retval;
	int bytes_per_pixel;
	long file_pointer;
	double file_value= Double.NaN;

	switch(bitpix)
	{
	    case 8:
		bytes_per_pixel = 1;
		file_pointer = data_offset + pixel_offset * bytes_per_pixel;
		fits_file.seek(file_pointer);
		file_value = fits_file.readUnsignedByte();
		if (file_value == blank_value)
		    throw new PixelValueException("No flux available");
		retval = file_value * bscale + bzero;
		break;
	    case 16:
		bytes_per_pixel = 2;
		file_pointer = data_offset + pixel_offset * bytes_per_pixel;
		fits_file.seek(file_pointer);
		file_value = fits_file.readShort();
		if (file_value == blank_value)
		    throw new PixelValueException("No flux available");
		retval = file_value * bscale + bzero;
		break;
	    case 32:
		bytes_per_pixel = 4;
		file_pointer = data_offset + pixel_offset * bytes_per_pixel;
		fits_file.seek(file_pointer);
		file_value = fits_file.readInt();
		if (file_value == blank_value)
		    throw new PixelValueException("No flux available");
		retval = file_value * bscale + bzero;
		break;
	    case -32:
		bytes_per_pixel = 4;
		file_pointer = data_offset + pixel_offset * bytes_per_pixel;
		fits_file.seek(file_pointer);
        file_value = fits_file.readFloat();
		retval = file_value * bscale + bzero;
		if (Double.isNaN(retval))
		    throw new PixelValueException("No flux available");
		break;
	    case -64:
		bytes_per_pixel = 8;
		file_pointer = data_offset + pixel_offset * bytes_per_pixel;
		fits_file.seek(file_pointer);
		file_value = fits_file.readDouble();
		retval = file_value * bscale + bzero;
		/*
		System.out.println("file_value = " + file_value +
		    "  file_pointer = " + file_pointer +
		    "  bscale = " + bscale + "  bzero = " + bzero +
		    "  retval = " + retval);
		*/
		if (Double.isNaN(retval))
		    throw new PixelValueException("No flux available");
		break;
	    default:
		retval = Double.NaN;
		break;
	}


       // if we are a palomar fits file then to special things to it.
    if (header.getStringHeader(ImageHeader.ORIGIN).startsWith(ImageHeader.PALOMAR_ID) &&
                            !Double.isNaN(file_value)) {
        if (header.containsKey(ImageHeader.AIRMASS) &&
            header.containsKey(ImageHeader.EXTINCT) &&
            header.containsKey(ImageHeader.IMAGEZPT) &&
            header.containsKey(ImageHeader.EXPTIME) ) {

            double airmass= header.getDoubleHeader(ImageHeader.AIRMASS);
            double extinct= header.getDoubleHeader(ImageHeader.EXTINCT);
            double imagezpt= header.getDoubleHeader(ImageHeader.IMAGEZPT);
            double exptime= header.getDoubleHeader(ImageHeader.EXPTIME);
            retval = -2.5 * .43429 * Math.log(file_value / exptime) +
                    imagezpt + extinct * airmass;

        }


    }



	return (retval);
    }
}
