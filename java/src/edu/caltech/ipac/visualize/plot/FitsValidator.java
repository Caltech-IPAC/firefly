package edu.caltech.ipac.visualize.plot;

import java.io.IOException;
import java.io.EOFException;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

import java.io.File;


public class FitsValidator {


    public static Header getFirstHeader(File f) throws FitsException {
        BasicHDU myHDU = null;
	Header header;

        Fits myFits = new Fits(f);
	try
	{
	    //myHDUs = myFits.read();
	    myHDU = myFits.readHDU();
	}
	catch (FitsException fe)
	{
	    throw fe;
	    //System.out.println("RBH caught FitsException");
	    //fe.printStackTrace();
	}
	catch (IOException ioe)
	{
	    throw new FitsException(ioe.getMessage());
	    //System.out.println("RBH caught IOException");
	    //ioe.printStackTrace();
	}
	if (myHDU == null)
	    throw new FitsException("cannot read header");
	header = myHDU.getHeader();
	return header;
    }


    public static void validateNaxis(File file) throws FitsException {
	Header header = getFirstHeader(file);
	validateNaxis(header);
    }


    public static void validateNaxis(Header header) throws FitsException {
	int naxis = header.getIntValue("NAXIS", 0);
	int naxis1 = header.getIntValue("NAXIS1", 0);
	if ((naxis <= 1) && (naxis1 <= 1))
	{
	    throw new FitsException(
		"degenerate image:  NAXIS = " + naxis + "  and NAXIS1 = " +
		naxis1);
	}
    }

    public static void validateProjection(Header header) throws FitsException {
	// accept DSS PLATE projection
	if (!header.containsKey("PLTRAH"))
	{
	// accept old DeepSky grid 
	if (!header.containsKey("DSKYGRID")) 
	{

	String ctype1 = header.getStringValue("CTYPE1");
	if (ctype1 == null) 
	    throw new FitsException(
		"No projection information - missing CTYPE1 header card");
	/* pad it out to 8 chars */
	ctype1 = ctype1.concat("        ");
	ctype1 = ctype1.substring(0,8);
	String projection = ctype1.substring(4);
	if (!projection.equals("-TAN") &&
	    !projection.equals("-SIN") &&
	    !projection.equals("-NCP") &&
	    !projection.equals("-ARC") &&
	    !projection.equals("-AIT") &&
	    !projection.equals("-ATF") &&
	    !projection.equals("-CAR") &&
	    !projection.equals("----") &&
	    !projection.equals("    ") )
	{
	    throw new FitsException("Unrecognized projection -  CTYPE1 = " +
		ctype1);
	}

	String ctype2 = header.getStringValue("CTYPE2");
	if (ctype2 == null) 
	    throw new FitsException(
		"No projection information - missing CTYPE2 header card");
	/* pad it out to 8 chars */
	ctype2 = ctype2.concat("        ");
	ctype2 = ctype2.substring(0,8);
	projection = ctype2.substring(4);
	if (!projection.equals("-TAN") &&
	    !projection.equals("-SIN") &&
	    !projection.equals("-NCP") &&
	    !projection.equals("-ARC") &&
	    !projection.equals("-AIT") &&
	    !projection.equals("-ATF") &&
	    !projection.equals("-CAR") &&
	    !projection.equals("----") &&
	    !projection.equals("    ") )
	{
	    throw new FitsException("Unrecognized projection -  CTYPE2 = " +
		ctype2);
	}
	}
	if (!header.containsKey("CRPIX1"))
	    throw new FitsException(
		"No projection information - missing CRPIX1 header card");

	if (!header.containsKey("CRVAL1"))
	    throw new FitsException(
		"No projection information - missing CRVAL1 header card");

	if (!header.containsKey("CRPIX2"))
	    throw new FitsException(
		"No projection information - missing CRPIX2 header card");

	if (!header.containsKey("CRVAL2"))
	    throw new FitsException(
		"No projection information - missing CRVAL2 header card");
	
	if ((!header.containsKey("CDELT1")) || (!header.containsKey("CDELT2")))
	{
	    /* no CDELT - header must have CD matrix */
	    if (
		((!header.containsKey("CD1_1")) && 
		(!header.containsKey("CD001001"))) ||
		((!header.containsKey("CD2_1")) && 
		(!header.containsKey("CD002001"))) ||
		((!header.containsKey("CD1_2")) && 
		(!header.containsKey("CD001002"))) ||
		((!header.containsKey("CD2_2")) && 
		(!header.containsKey("CD002002"))) )
	    throw new FitsException(
		"No projection information - no CDELTn or CD matrix");
	}
	}

    }

    public static void validateOther(Header header) throws FitsException {
	int bitpix = header.getIntValue("BITPIX");
	if ((bitpix < 0) && (header.containsKey("BLANK")))
	    throw new FitsException(
		"illegal to have BLANK in a floating point image");
    }


    public static void checkFits(File f) throws FitsException {
	Header header = getFirstHeader(f);
        validateNaxis(header);
	validateProjection(header);
	validateOther(header);
    }

    public static void main(String[] args) {

	for (int i = 0; i < args.length; i++)
	{
	    try {
		System.out.println("Checking file " + args[i]);
		File file = new File(args[i]);
		if (!file.canRead())
		{
		    System.out.println("   Cannot read file " + args[i]);
		}
		else
		{
		    checkFits(file);
		}
	    } catch (FitsException e) {
		System.out.println("    Fits Error: "+e.getMessage());
		//e.printStackTrace();
	    }
	}
    }
}
