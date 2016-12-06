/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

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
    public static Fits createBlankFITS(WorldPt wp, int width, int height,double scale) {
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

	blank_fits.addHDU(image_hdu);
	}
	catch (FitsException fe)
	{
	    System.out.println("RBH got FitsException: " + fe);
	}
	return blank_fits;
    }

}
