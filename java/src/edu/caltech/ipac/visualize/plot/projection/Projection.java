package edu.caltech.ipac.visualize.plot.projection;

import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.ProjectionPt;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.Serializable;


public class Projection implements Serializable {


    public static final double dtr = Math.PI/180.;
    public static final double rtd = 180./Math.PI;
    final static double DtoR = Math.PI/180.0;
    final static double RtoD = 180.0/Math.PI;
    static public final int GNOMONIC     = 1001;
    static public final int ORTHOGRAPHIC = 1002;
    static public final int NCP          = 1003;
    static public final int AITOFF       = 1004;
    static public final int CAR          = 1005;
    static public final int LINEAR       = 1006;
    static public final int PLATE        = 1007;
    static public final int ARC          = 1008;
    static public final int SFL          = 1009;
    static public final int CEA          = 1010;
    static public final int UNSPECIFIED  = 1998;
    static public final int UNRECOGNIZED = 1999;

    private double      _scale1;
    private double      _scale2;
    private double      _pixelScaleArcSec;
    private CoordinateSys _coordSys;
    private ProjectionParams _params;

    private Projection() {}

    public Projection(ProjectionParams params,
                      CoordinateSys coordSys)
    {
        _params= params;
        _scale1= 1/_params.cdelt1;
        _scale2= 1/_params.cdelt2;
        _pixelScaleArcSec= Math.abs(_params.cdelt1) * 3600.0;
        _coordSys= coordSys;
    }

    public CoordinateSys getCoordinateSys() { return _coordSys; }
    public ProjectionParams getProjectionParams() { return _params; }

    /**
     * Get the pixel scale in degrees
     * @return double Pixel scale in ArcSec
     */
    public double getPixelScaleArcSec() { return _pixelScaleArcSec; }

    public double getPixelWidthDegree() { return Math.abs(_params.cdelt1); }
    public double getPixelHeightDegree() { return Math.abs(_params.cdelt2); }

    public ImagePt getDistanceCoords(ImagePt pt, double x, double y) {
          ImagePt outpt= new ImagePt (
               pt.getX()+(x * _scale1), pt.getY()+(y * _scale2) );
          return outpt;
    }

    public ImageWorkSpacePt getDistanceCoords(ImageWorkSpacePt pt, double x, double y) {
          ImageWorkSpacePt outpt= new ImageWorkSpacePt (
               pt.getX()+(x * _scale1), pt.getY()+(y * _scale2) );
          return outpt;
    }

    public ProjectionPt getImageCoords( double ra, double dec) throws ProjectionException {
        return getImageCoords(ra,dec,true);
    }

    public ProjectionPt getImageCoordsSilent( double ra, double dec) {
        try {
            return getImageCoords(ra,dec,false);
        } catch (ProjectionException e) { // exception will not happen since parameter turns if off
            return null;
        }
    }



    /** Convert World coordinates to "Skyview Screen" coordinates
    *    "Skyview Screen" coordinates have 0,0 in center of lower left pixel
    * @param ra double precision 
    * @param dec double precision 
    * @return ImagePt with X,Y in "Skyview Screen" coordinates
    */

    private ProjectionPt getImageCoords( double ra, double dec, boolean useProjException) throws ProjectionException
    {
	ProjectionPt image_pt = null;

	switch (_params.maptype)
	{
	case (GNOMONIC):
	    image_pt = GnomonicProjection.RevProject( ra, dec, _params, useProjException);
	    break;
	case (PLATE):
	    image_pt = PlateProjection.RevProject( ra, dec, _params, useProjException);
	    break;
	case (ORTHOGRAPHIC):
	    image_pt = OrthographicProjection.RevProject( ra, dec, _params, useProjException);
	    break;
	case (NCP):
	    image_pt = NCPProjection.RevProject( ra, dec, _params);
	    break;
	case (ARC):
	    image_pt = ARCProjection.RevProject( ra, dec, _params);
	    break;
	case (AITOFF):
	    image_pt = AitoffProjection.RevProject( ra, dec, _params);
	    break;
	case (LINEAR):
	    image_pt = LinearProjection.RevProject( ra, dec, _params);
	    break;
	case (CAR):
	    image_pt = CartesianProjection.RevProject( ra, dec, _params, useProjException);
	    break;
	case (CEA):
	    image_pt = CylindricalProjection.RevProject( ra, dec, _params, useProjException);
	    break;
	case (SFL):
	    image_pt = SansonFlamsteedProjection.RevProject( ra, dec, _params, useProjException);
	    break;
	case (UNSPECIFIED):
        if (useProjException) throw new ProjectionException("image contains no projection information");
	default:
        if (useProjException) throw new ProjectionException("projection is not implemented");
	}
	    
	return (image_pt);
    }

    /** Convert World coordinates to "Skyview Screen" coordinates
    *    "Skyview Screen" coordinates have 0,0 in center of lower left pixel
    * @param worldPt double precision 
    * @return ImagePt with X,Y in "Skyview Screen" coordinates
    */
    public ProjectionPt getImageCoords(WorldPt worldPt)
	throws ProjectionException
    {
	ProjectionPt image_pt = getImageCoords( worldPt.getX(), worldPt.getY());
	return (image_pt);
    }

    public WorldPt getWorldCoords( double x, double y) throws ProjectionException {
        return getWorldCoords(x,y,true);

    }


    public WorldPt getWorldCoordsSilent( double x, double y) {
        try {
            return getWorldCoords(x,y,false);
        } catch (ProjectionException e) { // exception will not happen since parameter turns if off
            return null;
        }
    }



    /** Convert "ProjectionPt" coordinates to World coordinates
    *    "ProjectionPt" coordinates have 0,0 in center of lower left pixel
    *    (same as "Skyview Screen" coordinates)
    * @param x double precision in "ProjectionPt" coordinates
    * @param y double precision in "ProjectionPt" coordinates
    */

    public WorldPt getWorldCoords( double x, double y, boolean useProjException)  throws ProjectionException {
	Pt pt = null;

	switch (_params.maptype)
	{
	case (GNOMONIC):
	    pt = GnomonicProjection.FwdProject( x, y, _params);
	    break;
	case (PLATE):
	    pt = PlateProjection.FwdProject( x, y, _params);
	    break;
	case (ORTHOGRAPHIC):
	    pt = OrthographicProjection.FwdProject( x, y, _params, useProjException);
	    break;
	case (NCP):
	    pt = NCPProjection.FwdProject( x, y, _params);
	    break;
	case (ARC):
	    pt = ARCProjection.FwdProject( x, y, _params);
	    break;
	case (AITOFF):
	    pt = AitoffProjection.FwdProject( x, y, _params, useProjException);
	    break;
	case (LINEAR):
	    pt = LinearProjection.FwdProject( x, y, _params);
	    break;
	case (CAR):
	    pt = CartesianProjection.FwdProject( x, y, _params, useProjException);
	    break;
	case (CEA):
	    pt = CylindricalProjection.FwdProject( x, y, _params, useProjException);
	    break;
	case (SFL):
	    pt = SansonFlamsteedProjection.FwdProject( x, y, _params, useProjException);
	    break;
	case (UNSPECIFIED):
	    if (useProjException) throw new ProjectionException("image contains no projection information");
	default:
	    if (useProjException) throw new ProjectionException("projection is not implemented");
	}

	WorldPt world_pt = (pt!=null) ? new WorldPt(pt.getX(), pt.getY(), _coordSys) : null;
	return (world_pt);
    }

    /** Convert "ProjectionPt" coordinates to World coordinates
    *    "ProjectionPt" coordinates have 0,0 in center of lower left pixel
    *    (same as "Skyview Screen" coordinates)
    * @param imagePt ProjectionPt with X,Y in "ProjectionPt" coordinates
    */

    public WorldPt getWorldCoords( ProjectionPt imagePt)
	throws ProjectionException
    {
	WorldPt world_pt = getWorldCoords(imagePt.getX(), imagePt.getY());
	return (world_pt);
    }

    public boolean isImplemented()
    {
	boolean retval;

	switch (_params.maptype)
	{
	    case (GNOMONIC):
	    case (PLATE):
	    case (ORTHOGRAPHIC):
	    case (NCP):
	    case (ARC):
	    case (AITOFF):
	    case (LINEAR):
	    case (CAR):
	    case (CEA):
	    case (SFL):
		retval = true;
		break;
	    default:
		retval = false;
	}
	return (retval);
    }

    public boolean isSpecified()
    {
	if (_params.maptype == UNSPECIFIED)
	    return false;
	else
	    return true;
    }

    public boolean isWrappingProjection()
    {
	boolean retval;

	switch (_params.maptype)
	{
	    case (GNOMONIC):
	    case (CAR):
	    case (CEA):
	    case (SFL):
	    case (NCP):
	    case (PLATE):
	    case (ORTHOGRAPHIC):
	    case (ARC):
	    case (LINEAR):
		retval = false;
		break;
	    case (AITOFF):
	    default:
		retval = true;
	}
	return (retval);
    }

//    public static void main(String args[])
//    {
//	Fits myFits;
//	BasicHDU[] HDUs;
//	ProjectionPt image_pt;
//	WorldPt world_pt;
//	ImageHeader hdr = null;
//	Header header;
//	double input_ra, input_dec;
//
//	if (args.length != 1)
//	{
//	    System.out.println("usage:  java Projection <filename>");
//	    System.exit(1);
//	}
//
//	// RBH: these header values are taken from the image of m31
//	//      on which table file 13876_irsky_iso was overlayed.
//	/*
//	Projection proj = new Projection(
//	    60,   // NAXIS1
//	    60,   // NAXIS2
//	    -60.0,            // CRPIX1
//	    -10.0,            // CRPIX2
//	    1.300000000E+01,  // CRVAL1
//	    4.000000000E+01,  // CRVAL2
//	    -2.500000000E-02, // CDELT1
//	    2.500000000E-02,  // CDELT2
//	    0.0,              // CROTA2
//            Plot.EQ_J2000 );
//	*/
//	try
//	{
//	    myFits = new Fits(args[0]);
//	    HDUs = myFits.read();
//	    if (HDUs == null)
//	    {
//		System.out.println("Error: file doesn't have any HDUs!");
//		System.exit(1);
//	    }
//	    header = HDUs[0].getHeader();
//	    hdr = new ImageHeader(header);
//	}
//	catch (FitsException e)
//	{
//	    System.out.println("got FitsException: " + e.getMessage());
//	    System.exit(1);
//	}
//
//	Projection proj = hdr.createProjection(CoordinateSys.EQ_J2000 );
//
//	// RBH: these coordinates are taken from the first line of
//	//      table file 13876_irsky_iso
//	input_ra = 9.4092059;
//	input_dec = +41.4161779;
//
//
//	/* these values are a bright spot on m31.fits */
//	input_ra = 10.472992;
//	input_dec = +40.397477;
//
//	/* these values are a bright spot on DSSI.2.fits */
//	input_ra = 11.0003;
//	input_dec = +21.0007;
//	input_ra = 196.929994;
//	input_dec = 27.509292;
//
//	/* these values are the upper right corner on c1632b1.fits (ORTHOGR) */
//	input_ra = 83.295382;
//	input_dec = -6.2797047;
//
//	/* these values are a bright spot on file7.fits (NCP) */
//	input_ra = 234.007968;
//	input_dec = -55.907927;
//
//	System.out.println("input values:  RA = " + input_ra +
//	    "  DEC = " + input_dec);
//
//	try
//	{
//	image_pt = proj.getImageCoords( input_ra, input_dec);
//	System.out.println("RBH SC X  = " + image_pt.getFsamp() +
//	    "   SC Y  = " + image_pt.getFline());
//
//
//
//	world_pt = proj.getWorldCoords(image_pt.getFsamp(), image_pt.getFline());
//	System.out.println("RBH ra = " + world_pt.getX() +
//	    "    dec = " + world_pt.getY());
//	}
//	catch (ProjectionException pe)
//	{
//	    System.out.println("got ProjectionException: " + pe.getMessage());
//	}
//
//
//    }

    /**
     *  compute the distance between two positions (lon1, lat1)
     *  and (lon2, lat2), the lon and lat are in decimal degrees.
     *  the unit of the distance is degree
     */
    public static double computeDistance(double lon1, double lat1,
                                         double lon2, double lat2) {
        double cosine;
        double lon1Radians, lon2Radians;
        double lat1Radians, lat2Radians;
        lon1Radians  = lon1 * DtoR;
        lat1Radians  = lat1 * DtoR;
        lon2Radians  = lon2 * DtoR;
        lat2Radians  = lat2 * DtoR;
        cosine =
                                      Math.cos(lat1Radians)*Math.cos(lat2Radians)*
                                      Math.cos(lon1Radians-lon2Radians)
                                      + Math.sin(lat1Radians)*Math.sin(lat2Radians);

        if (Math.abs(cosine) > 1.0)
            cosine = cosine/Math.abs(cosine);
        return RtoD*Math.acos(cosine);
    }



    public String getProjectionName() {
        return getProjectionName(this._params.maptype);
    }


    public static String getProjectionName(int maptype)
    {
        String retval;

        switch(maptype)
        {
            case (Projection.GNOMONIC):
                retval = "GNOMONIC";
                break;
            case (Projection.ORTHOGRAPHIC):
                retval = "ORTHOGRAPHIC";
                break;
            case (Projection.NCP):
                retval = "NCP";
                break;
            case (Projection.ARC):
                retval = "ARC";
                break;
            case (Projection.AITOFF):
                retval = "AITOFF";
                break;
            case (Projection.CAR):
                retval = "CAR";
                break;
            case (Projection.CEA):
                retval = "CEA";
                break;
            case (Projection.SFL):
                retval = "SFL";
                break;
            case (Projection.LINEAR):
                retval = "LINEAR";
                break;
            case (Projection.PLATE):
                retval = "PLATE";
                break;
            case (Projection.UNSPECIFIED):
                retval = "UNSPECIFIED";
                break;
            case (Projection.UNRECOGNIZED):
            default:
                retval = "UNRECOGNIZED";
                break;
        }
        return (retval);
    }



}
