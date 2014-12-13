package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.target.PositionJ2000;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;


   /**
    *  Create a blank FITS image 
    */
public class BlankFITS 
{
    //private WorldPt       _movingPt= null;
    public static Fits createBlankFITS(PositionJ2000 position, int width,
                                       int height,double scale) {
        return createBlankFITS(new WorldPt(position.getRa(),position.getDec()),width,height,scale);
    }


    public static Fits createBlankFITS(WorldPt wp, int width, int height,double scale)
    {
    wp= Plot.convert(wp, CoordinateSys.EQ_J2000);
	if (scale < 0)
	    scale = - scale;
	Fits blank_fits = new Fits();
	Header header = new Header();
	try
	{
	    header.setSimple(true);
	    header.setBitpix(8);
	    header.setNaxes(2);
	    header.setNaxis(1, width);
	    header.setNaxis(2, height);
	    header.addValue("BLANK", 0, null);
	    header.addValue("CRVAL1", wp.getLon(), null);
	    header.addValue("CRPIX1", width/2 , null);
	    header.addValue("CDELT1", -scale , null);
	    header.addValue("CRVAL2", wp.getLat(), null);
	    header.addValue("CRPIX2", height/2 , null);
	    header.addValue("CDELT2", scale , null);
	    if (((width * scale) > 30.0) || ((height * scale) > 30.0))
	    {
		header.addValue("CTYPE1", "LON--AIT", null);
		header.addValue("CTYPE2", "LAT--AIT", null);
	    }
	    else
	    {
		header.addValue("CTYPE1", "RA---TAN", null);
		header.addValue("CTYPE2", "DEC--TAN", null);
	    }
	}
	catch (HeaderCardException hce)
	{
	    System.out.println("RBH got HeaderCardException: " + hce);
	}

	try
	{
	byte values[][] = new byte[width][height];

	/* fill data with zeros */
	for (int x = 0; x < width; x++)
	{
	    for (int y = 0; y < height; y++)
	    {
		values[x][y] = 0;
	    }
	}
	ImageData image_data = new ImageData(values);

	ImageHDU image_hdu = new ImageHDU(header, image_data);

	//image_hdu.info();

	blank_fits.addHDU(image_hdu);
	}
	catch (FitsException fe)
	{
	    System.out.println("RBH got FitsException: " + fe);
	}
	return blank_fits;
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
