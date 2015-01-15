/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;
import edu.caltech.ipac.visualize.plot.projection.Projection;

import java.io.File;

/** This class computes the ra and dec of the four corners of a FITS image
*/
public class Corners

{

    /** Compute the four corners
    *
    * @param file FITS file
    *
    * @return an array of WorldPt specifying the four corners
    */
    public static WorldPt[] findCorners(File file)
                                  throws FitsException, ProjectionException
    {
	Fits fits = null;
	Header header = null;
	double naxis1 = 0;
	double naxis2 = 0;
	double crpix1 = 0;
	double crpix2 = 0;
	ImageHDU imageHDU;


	fits = new Fits(file);   //open the file
	BasicHDU[] myHDUs = fits.read();     // get all of the header-data units
					       // usually just one primary HDU
	if (myHDUs[0] instanceof ImageHDU)
	{
	    imageHDU = (ImageHDU) myHDUs[0];
	}
	else
	{
	    throw new FitsException("FITS file is not an image file");
	}


	header = imageHDU.getHeader();  // get the header
	ImageHeader _image_header = new ImageHeader(header);
	naxis1 = _image_header.naxis1;
	naxis2 = _image_header.naxis2;
	crpix1 = _image_header.crpix1;
	crpix2 = _image_header.crpix2;

	//System.out.println("naxis1 = " + naxis1 + "  naxis2 = " + naxis2);

	CoordinateSys in_coordinate_sys = CoordinateSys.makeCoordinateSys(
	    _image_header.getJsys(), _image_header.file_equinox);
//	Projection proj = new Projection(_image_header, in_coordinate_sys);
        Projection proj = _image_header.createProjection(in_coordinate_sys);

	WorldPt[] retval = new WorldPt[4];
	retval[0] = proj.getWorldCoords(0.0, 0.0);
	retval[1] = proj.getWorldCoords(naxis1 - 1.0, 0.0);
        retval[2] = proj.getWorldCoords(naxis1 - 1.0, naxis2 - 1.0);
        retval[3] = proj.getWorldCoords(0.0, naxis2 - 1.0);

//        retval[2] = proj.getWorldCoords(0.0, naxis2 - 1.0);
//	retval[3] = proj.getWorldCoords(naxis1 - 1.0, naxis2 - 1.0);

	return retval;

    }

    static public void main(String args[])
    {

	try
	{
	    WorldPt[] corner_points = Corners.findCorners(new File(args[0]));

	    System.out.println("corner_points[0]:  ra = " + 
		corner_points[0].getLon() + "  dec = " + 
		corner_points[0].getLat());
	    System.out.println("corner_points[1]:  ra = " + 
		corner_points[1].getLon() + "  dec = " + 
		corner_points[1].getLat());
	    System.out.println("corner_points[2]:  ra = " + 
		corner_points[2].getLon() + "  dec = " + 
		corner_points[2].getLat());
	    System.out.println("corner_points[3]:  ra = " + 
		corner_points[3].getLon() + "  dec = " + 
		corner_points[3].getLat());
	}
	catch (FitsException e)
	{
	    e.printStackTrace();
	}
	catch (ProjectionException e)
	{
	    e.printStackTrace();
	}
    }
}


