package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.target.TargetUtil;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.SUTDebug;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.Cursor;
import edu.caltech.ipac.visualize.plot.projection.Projection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;


public class FitsRead implements Serializable 
{
    private int plane_number;
    private int extension_number;
    private byte[] pixeldata;
    private int[] pixelhist = new int[256];
    private byte[] onedimdata8;
    private int[] onedimdata32;
    private short[] onedimdata16;
    private float[] onedimdatam32;
    private double[] onedimdatam64;
    private Fits myFits;
    private ImageHeader _image_header;
    private Header _header;
    private BasicHDU _hdu;
    private int bitpix;
    private Histogram hist = null;
    private double blank_value;
    private static RangeValues _defaultRangeValues= new RangeValues();
    private RangeValues _rangeValues= (RangeValues)_defaultRangeValues.clone();
    private int             _imageScaleFactor= 1;
    private int             _indexInFile= -1;  // -1 unknown, >=0 index in file
    private String          _srcDesc= null;
//    private int start_pixel;
//    private int last_pixel;
//    private int start_line;
//    private int last_line;
    private double slow = 0.0;
    private double shigh = 0.0;

    static
    {
	FitsFactory.setAllowTerminalJunk(true);
	FitsFactory.setUseHierarch(true);
    }



//    public FitsRead(InputStream stream, boolean compressed)
//                                                   throws FitsException{
//	this(new Fits(stream,compressed));
//    }
//
//    public FitsRead(String filename) throws FitsException {
//	this(new Fits(filename));
//    }

    /**
    *  Flip an image left to right so that pixels read backwards
    * @param fr FitsRead object for the input image
    * @return FitsRead object for the new, flipped image
    */

    public static FitsRead createFitsReadFlipLR(FitsRead fr)
	throws FitsException, GeomException
    {
	FlipLR flip_lr = new FlipLR();
	return(flip_lr.do_flip(fr));
    }


    /**
    *  Rotate an image so that Equatorial North is up in the new image
    * @param fr FitsRead object for the input image
    * @return FitsRead object for the new, rotated image
    */

    public static FitsRead createFitsReadNorthUp(FitsRead fr)
		  throws FitsException, IOException, GeomException {
	return(createFitsReadPositionAngle(fr, 0.0, CoordinateSys.EQ_J2000));
    }


    /**
    *  Rotate an image so that Galactic North is up in the new image
    * @param fr FitsRead object for the input image
    * @return FitsRead object for the new, rotated image
    */

    public static FitsRead createFitsReadNorthUpGalactic(FitsRead fr)
		  throws FitsException, IOException, GeomException {
	return(createFitsReadPositionAngle(fr, 0.0, CoordinateSys.GALACTIC));
    }

    /**
    *  Rotate an image by a specified amount
    * @param fr FitsRead object for the input image
    * @param rotation_angle number of degrees to rotate the image counter-clockwise
    * @return FitsRead object for the new, rotated image
    */
    public static FitsRead createFitsReadRotated(FitsRead fr, double rotation_angle) 
		  throws FitsException, IOException, GeomException {
    double center_x, center_y;
    double lon, lat;
    WorldPt world_pt1, world_pt2;

    ImageHeader imageHeader= fr.getImageHeader();

    CoordinateSys in_coordinate_sys = CoordinateSys.makeCoordinateSys(
	imageHeader.getJsys(), imageHeader.file_equinox);
    Projection proj = imageHeader.createProjection(in_coordinate_sys);

    //center_x = (imageHeader.naxis1+1.0) / 2.0 - imageHeader.crpix1;
    //center_y = (imageHeader.naxis2+1.0) / 2.0 - imageHeader.crpix2;
    center_x = (imageHeader.naxis1+1.0) / 2.0 ;
    center_y = (imageHeader.naxis2+1.0) / 2.0 ;

    try
    {
	world_pt1 = proj.getWorldCoords( center_x, center_y - 1);
	world_pt2 = proj.getWorldCoords( center_x, center_y);

    }
    catch (ProjectionException pe)
    {
	if (SUTDebug.isDebug())
	{
	    System.out.println("got ProjectionException: " + pe.getMessage());
	}
	throw new FitsException("Could not rotate image.\n -  got ProjectionException: " + pe.getMessage());
    }
    double position_angle = - TargetUtil.getPositionAngle(world_pt1.getX(), 
	world_pt1.getY(), world_pt2.getX(), world_pt2.getY());

    position_angle += rotation_angle;
    return(createFitsReadPositionAngle(fr, position_angle, CoordinateSys.EQ_J2000));
    }

    /**
    *  Rotate an image so that North is at the specified position angle in the new image
    * @param fr FitsRead object for the input image
    * @param position_angle  desired position angle in degrees 
    * @param coord_sys desired coordinate system for output image
    * @return FitsRead object for the new, rotated image
    */
    public static FitsRead createFitsReadPositionAngle(FitsRead fr, double position_angle, CoordinateSys coord_sys)
		  throws FitsException, IOException, GeomException {


	Geom geom=new Geom();

	ImageHeader imageHeader=geom.open_in(fr);  // throws GeomException


	/* new try - create a Fits with CDELTs and CROTA2, discarding */
	/* CD matrix, PLATE projection stuff, and SIP corrections */
	Header ref_header = new Header();
	ref_header.setSimple(true);
	ref_header.setNaxes(2);
	/* values for cropped.fits */
	ref_header.setBitpix(16);  // ignored - geom sets it to -32
	ref_header.setNaxis(1, imageHeader.naxis1);  
	ref_header.setNaxis(2, imageHeader.naxis2);  
	geom.n_override_naxis1 = true;  // make geom recalculate NAXISn
	/* 
	    pixel at center of object 
	    18398  DN at RA = 60.208423  Dec = -89.889959
	    pixel one up
	    18398  DN at RA = 59.995226  Dec = -89.889724
	    (a distance of 0.028349 arcmin or 0.00047248 degrees)
	*/


    double center_x, center_y;
    double lon, lat;
    WorldPt world_pt;

	CoordinateSys in_coordinate_sys = CoordinateSys.makeCoordinateSys(
	    imageHeader.getJsys(), imageHeader.file_equinox);
	Projection proj = imageHeader.createProjection(in_coordinate_sys);

    //center_x = (imageHeader.naxis1+1.0) / 2.0 - imageHeader.crpix1;
    //center_y = (imageHeader.naxis2+1.0) / 2.0 - imageHeader.crpix2;
    center_x = (imageHeader.naxis1+1.0) / 2.0 ;
    center_y = (imageHeader.naxis2+1.0) / 2.0 ;

    try
    {
	world_pt = proj.getWorldCoords( center_x - 1, center_y - 1);

    }
    catch (ProjectionException pe)
    {
	if (SUTDebug.isDebug())
	{
	    System.out.println("got ProjectionException: " + pe.getMessage());
	}
	throw new FitsException("Could not rotate image.\n -  got ProjectionException: " + pe.getMessage());
    }


	if (!coord_sys.equals(imageHeader.getCoordSys())) 
	{
	    world_pt = Plot.convert(world_pt, coord_sys);
	}
	lon = world_pt.getX();
	lat = world_pt.getY();

	//ref_header.addValue("CRVAL1", 60.208423, "");
	//ref_header.addValue("CRVAL2", -89.889959, "");
	ref_header.addValue("CRVAL1", lon, "");
	ref_header.addValue("CRVAL2", lat, "");
	//ref_header.addValue("CDELT1", -0.00047248, "");
	//ref_header.addValue("CDELT2", 0.00047248, "");
	ref_header.addValue("CDELT1", -Math.abs(imageHeader.cdelt1), "");
	ref_header.addValue("CDELT2", Math.abs(imageHeader.cdelt2), "");
	ref_header.addValue("CRPIX1", imageHeader.naxis1 / 2, "");
	ref_header.addValue("CRPIX2", imageHeader.naxis2 / 2, "");
	ref_header.addValue("CROTA2", position_angle, "");
	if (coord_sys.equals(CoordinateSys.EQ_J2000))
	{
	    ref_header.addValue("CTYPE1", "RA---TAN", "");
	    ref_header.addValue("CTYPE2", "DEC--TAN", "");
	    ref_header.addValue("EQUINOX", 2000.0, "");
	}
	else if (coord_sys.equals(CoordinateSys.EQ_B1950 ))
	{
	    ref_header.addValue("CTYPE1", "RA---TAN", "");
	    ref_header.addValue("CTYPE2", "DEC--TAN", "");
	    ref_header.addValue("EQUINOX", 1950.0, "");
	}
	else if (coord_sys.equals(CoordinateSys.ECL_J2000 ))
	{
	    ref_header.addValue("CTYPE1", "ELON-TAN", "");
	    ref_header.addValue("CTYPE2", "ELAT-TAN", "");
	    ref_header.addValue("EQUINOX", 2000.0, "");
	}
	else if (coord_sys.equals(CoordinateSys.ECL_B1950 ))
	{
	    ref_header.addValue("CTYPE1", "ELON-TAN", "");
	    ref_header.addValue("CTYPE2", "ELAT-TAN", "");
	    ref_header.addValue("EQUINOX", 1950.0, "");
	}
	else if (coord_sys.equals(CoordinateSys.GALACTIC ))
	{
	    ref_header.addValue("CTYPE1", "GLON-TAN", "");
	    ref_header.addValue("CTYPE2", "GLAT-TAN", "");
	}
	else 
	{
	    throw new FitsException("Could not rotate image.\n -  unrecognized coordinate system");
	}


	ImageHDU ref_HDU = new ImageHDU(ref_header, null);
	Fits refFits = new Fits();
	refFits.addHDU(ref_HDU);


	/* end new try */

	/* first try - worked fine except for PLATE projection
	//FitsRead ref_fr = fr;
	//Fits modFits=geom.do_geom(ref_fr);
	*/

	/* second try - WORKED, and saves memory (no pixels from refFits) */
	//Fits refFits = null;
	Fits modFits=geom.do_geom(refFits);  // throws GeomException

	/* third try - WORKED */
	//Fits refFits = fr.getFits();
	//FitsRead[] ref_fits_read_array = createFitsReadArray(refFits);
	//FitsRead ref_fits_read = ref_fits_read_array[0];
	//Fits modFits=geom.do_geom(ref_fits_read);


	FitsRead[] fits_read_array = createFitsReadArray(modFits);
	fr = fits_read_array[0];
        return fr;
    }

    public static FitsRead createFitsReadWithGeom(FitsRead fr,
                                                  FitsRead refFitsRead,
                                                  boolean   doscale) throws
                                                                     FitsException,
                                                                     IOException,
                                                                     GeomException {
        if(refFitsRead==null) {
            //fr= new FitsRead(fits);
            // merely return the entry FitsRead
        }
        else {
            ImageHeader refHeader=refFitsRead.getImageHeader();
            Geom geom=new Geom();
            //geom.override_naxis1=0;
            geom.n_override_naxis1=doscale;

            ImageHeader imageHeader=geom.open_in(fr);
            double primCdelt1=Math.abs(imageHeader.cdelt1);
            double refCdelt1=Math.abs(refHeader.cdelt1);
            boolean shouldScale=2*refCdelt1<primCdelt1;
            int imageScaleFactor=1;

            if(doscale && shouldScale) {
                imageScaleFactor=(int) (primCdelt1/refCdelt1);
                geom.override_cdelt1=refHeader.cdelt1*imageScaleFactor;
                geom.n_override_cdelt1=true;
                geom.override_cdelt2=refHeader.cdelt2*imageScaleFactor;
		geom.n_override_cdelt2=true;
		if (refHeader.using_cd)
		{
		    geom.override_CD1_1 = refHeader.cd1_1 * imageScaleFactor;
		    geom.override_CD1_2 = refHeader.cd1_2 * imageScaleFactor;
		    geom.override_CD2_1 = refHeader.cd2_1 * imageScaleFactor;
		    geom.override_CD2_2 = refHeader.cd2_2 * imageScaleFactor;
		    geom.n_override_CDmatrix = true;
		}

		geom.crpix1_base = refHeader.crpix1;
		geom.crpix2_base = refHeader.crpix2;
		geom.imageScaleFactor = imageScaleFactor;
		geom.need_crpix_adjusted = true;
		if (SUTDebug.isDebug())
		{
		System.out.println(
		    "RBH ready for do_geom:  imageScaleFactor = " 
		    + imageScaleFactor + "  geom = " + geom);
		}
            }
            Fits modFits=geom.do_geom(refFitsRead);

            FitsRead[] fits_read_array = createFitsReadArray(modFits);
            fr = fits_read_array[0];
            fr._imageScaleFactor=imageScaleFactor;
        }
        return fr;
    }


    public static FitsRead[] createFitsReadArray(Fits      fits)
                                       throws FitsException {
        ArrayList<BasicHDU> HDUList = new ArrayList<BasicHDU>();
        Header header;
        int bitpix, naxis, naxis1, naxis2;
        //int naxis;


        BasicHDU[] HDUs = fits.read();


        if (HDUs == null)
        {
            // Error: file doesn't seem to have any HDUs!
            throw new FitsException("Bad format in FITS file");
        }
        //System.out.println("HDUs.length = " + HDUs.length);

        for (int j = 0; j < HDUs.length; j++)
        {
            if (!(HDUs[j] instanceof ImageHDU))
            {
                //System.out.println("RBH not an ImageHDU;  it is " + HDUs[j]);
                continue;   //ignor non-image extensions
            }
            header = HDUs[j].getHeader();
            if (header == null)
                throw new FitsException("Missing header in FITS file");
            /*
             if (!HDUs[j].isHeader(header))
                 throw new FitsException("Bad header in FITS file");
             */

            boolean good_image = true;
            bitpix = header.getIntValue("BITPIX",-1);
            naxis = header.getIntValue("NAXIS",-1);
            //System.out.println("RBH naxis = " + naxis);
            if (naxis == 0)
                good_image = false;
            for (int i = 1; i <= naxis; i++)
            {
                int naxis_value = header.getIntValue("NAXIS"+i,-1);
                //System.out.println("RBH naxis_value = " + naxis_value);
                if (naxis_value == 0)
                    good_image = false;
            }
            if (good_image)
            {
                if (HDUs.length > 1)
                {
                    /* position the header pointer past required keywords */
		    /* Different techniques for fitsjava version 1.97a    */
		    /* and our repaired code, which has HashedList.add()  */
		    /* declared public */


			/* This code is for fitsjava version 1.97a */
                        String key = null;
                        Cursor iter = header.iterator();
                        HeaderCard card;
			int i;
			for (i = 0; iter.hasNext(); i++)
                        {
                            card = (HeaderCard) iter.next();
                            key = card.getKey();
                            if (key.startsWith("SIMPLE"))
                                continue;
                            if (key.startsWith("XTENSION"))
                                continue;
                            if (key.startsWith("PCOUNT"))
                                continue;
                            if (key.startsWith("GCOUNT"))
                                continue;
                            if (key.startsWith("BITPIX"))
                                continue;
                            if (key.startsWith("NAXIS"))
                                continue;
                            break;
                        }

			while (key.length() == 0)
			{
			    /* move past blank cards */
			    if (iter.hasNext())
			    {
				card = (HeaderCard) iter.next();
				key = card.getKey();
			    }
			}
			iter.add("SPOT_EXT", new HeaderCard(
			    "SPOT_EXT", j, "EXTENSION NUMBER (IN SPOT)"));

			iter.add("SPOT_OFF", new HeaderCard( 
			    "SPOT_OFF", HDUs[j].getFileOffset(), 
			    "EXTENSION OFFSET (IN SPOT)"));
//			iter.add("SPOT_SZ", new HeaderCard( 
//			    "SPOT_SZ", header.getOriginalSize(), 
//			    "EXTENSION HEADER SIZE (IN SPOT)"));
                }
		else
		{
		    header.resetOriginalSize();  // RBH ADDED 12-15-2011
		}

		int naxis3 = header.getIntValue("NAXIS3", -1);
		if ((naxis > 2) && (naxis3 > 1))
                {
                    if (SUTDebug.isDebug())
                        System.out.println("GOT A FITS CUBE");
                    BasicHDU[] split_HDUs = split_FITS_cube(HDUs[j]);
                    /* for each plane of cube */
                    for (int jj = 0; jj < split_HDUs.length; jj++)
                        HDUList.add(split_HDUs[jj]);
                }
                else
                {
                    HDUList.add(HDUs[j]);
                }
            }

        }
        if (HDUList.size() == 0)
            throw new FitsException("No image headers in FITS file");

        FitsRead[] fitsReadAry = new FitsRead[HDUList.size()];
        for (int i = 0; i < HDUList.size(); i++)
        {
            fitsReadAry[i] = new FitsRead(fits, (ImageHDU) HDUList.get(i));
            fitsReadAry[i]._indexInFile= i;
        }

        return fitsReadAry;
    }


    private FitsRead(Fits fits, ImageHDU image_hdu) throws FitsException
    {
        int naxis1, naxis2;

        myFits = fits;


	_hdu = image_hdu;
        _header = image_hdu.getHeader();

        // now get SPOT plane_number from FITS cube (zero if not from a cube)
        plane_number = _header.getIntValue("SPOT_PL", 0);
        if (SUTDebug.isDebug())
            System.out.println("RBH fetched SPOT_PL: " + plane_number);

        // now get SPOT extension_number from FITS header
        // -1 if the image had no extensions
        extension_number = _header.getIntValue("SPOT_EXT", -1);
        if (SUTDebug.isDebug())
            System.out.println("RBH fetched SPOT_EXT: " + extension_number);

        if (_header == null)
            throw new FitsException("FITS file appears corrupt");

        long HDU_offset ;
	if (extension_number == -1)
	{
	    HDU_offset =  image_hdu.getFileOffset();
	}
	else
	{
	    HDU_offset =  _header.getIntValue("SPOT_OFF", 0);
	    //long header_size = _header.getIntValue("SPOT_SZ", 0);
	    //_header.setOriginalSize(header_size);
	}


        if (HDU_offset < 0) HDU_offset= 0;
        _image_header = new ImageHeader(_header, HDU_offset, plane_number);

        bitpix = _image_header.bitpix;
        //naxis = _image_header.naxis;
        blank_value = _image_header.blank_value;
        naxis1 = _image_header.naxis1;
        naxis2 = _image_header.naxis2;



        if (bitpix == 32)
        {
            onedimdata32 =
                (int[]) ArrayFuncs.flatten( image_hdu.getData().getData());

            if (_image_header.cdelt2 < 0)
            {
                /* pixels are upside down - reverse them in y */
                int x, y, index_src, index_dest;
                int temp32[] = new int[onedimdata32.length];
                index_src = 0;
                for (y = 0; y < naxis2; y++)
                {
                    index_dest = (naxis2 - y - 1) * naxis1;
                    for (x = 0; x < naxis1; x++)
                        temp32[index_dest++] = onedimdata32[index_src++];
                }
                onedimdata32 = temp32;
                _image_header.cdelt2 = - _image_header.cdelt2;
                _image_header.crpix2 =
                    _image_header.naxis2 - _image_header.crpix2 + 1;

                /* fix FITS header also */
		/*
                _header.addValue("CDELT2", _image_header.cdelt2, null);
                _header.addValue("CRPIX2", _image_header.crpix2, null);
		*/
            }

           //do_stretch();
        }
        else if (bitpix == 16)
        {

            onedimdata16 =
                (short[]) ArrayFuncs.flatten( image_hdu.getData().getData());

            if (_image_header.cdelt2 < 0)
            {
                /* pixels are upside down - reverse them in y */
                int x, y, index_src, index_dest;
                short temp16[] = new short[onedimdata16.length];
                index_src = 0;
                for (y = 0; y < naxis2; y++)
                {
                    index_dest = (naxis2 - y - 1) * naxis1;
                    for (x = 0; x < naxis1; x++)
                        temp16[index_dest++] = onedimdata16[index_src++];
                }
                onedimdata16 = temp16;
                _image_header.cdelt2 = - _image_header.cdelt2;
                _image_header.crpix2 =
                    _image_header.naxis2 - _image_header.crpix2 + 1;

                /* fix FITS header also */
		/*
                _header.addValue("CDELT2", _image_header.cdelt2, null);
                _header.addValue("CRPIX2", _image_header.crpix2, null);
		*/
            }

           //do_stretch();

        }
        else if (bitpix == 8)
        {

            onedimdata8 =
                (byte[]) ArrayFuncs.flatten( image_hdu.getData().getData());

            if (_image_header.cdelt2 < 0)
            {
                /* pixels are upside down - reverse them in y */
                int x, y, index_src, index_dest;
                byte temp8[] = new byte[onedimdata8.length];
                index_src = 0;
                for (y = 0; y < naxis2; y++)
                {
                    index_dest = (naxis2 - y - 1) * naxis1;
                    for (x = 0; x < naxis1; x++)
                        temp8[index_dest++] = onedimdata8[index_src++];
                }
                onedimdata8 = temp8;
                _image_header.cdelt2 = - _image_header.cdelt2;
                _image_header.crpix2 =
                    _image_header.naxis2 - _image_header.crpix2 + 1;

                /* fix FITS header also */
		/*
                _header.addValue("CDELT2", _image_header.cdelt2, null);
                _header.addValue("CRPIX2", _image_header.crpix2, null);
		*/
            }

           //do_stretch();



        }
        else if (bitpix == -32)
        {

            onedimdatam32 =
                (float[]) ArrayFuncs.flatten( image_hdu.getData().getData());

            if (_image_header.cdelt2 < 0)
            {
                /* pixels are upside down - reverse them in y */
                int x, y, index_src, index_dest;
                float tempm32[] = new float[onedimdatam32.length];
                index_src = 0;
                for (y = 0; y < naxis2; y++)
                {
                    index_dest = (naxis2 - y - 1) * naxis1;
                    for (x = 0; x < naxis1; x++)
                        tempm32[index_dest++] = onedimdatam32[index_src++];
                }
                onedimdatam32 = tempm32;
                _image_header.cdelt2 = - _image_header.cdelt2;
                _image_header.crpix2 =
                    _image_header.naxis2 - _image_header.crpix2 + 1;

                /* fix FITS header also */
		/*
                _header.addValue("CDELT2", _image_header.cdelt2, null);
                _header.addValue("CRPIX2", _image_header.crpix2, null);
		*/
            }

           //do_stretch();
        }
        else if (bitpix == -64)
        {

            onedimdatam64 =
                (double[]) ArrayFuncs.flatten( image_hdu.getData().getData());

            if (_image_header.cdelt2 < 0)
            {
                /* pixels are upside down - reverse them in y */
                int x, y, index_src, index_dest;
                double tempm64[] = new double[onedimdatam64.length];
                index_src = 0;
                for (y = 0; y < naxis2; y++)
                {
                    index_dest = (naxis2 - y - 1) * naxis1;
                    for (x = 0; x < naxis1; x++)
                        tempm64[index_dest++] = onedimdatam64[index_src++];
                }
                onedimdatam64 = tempm64;
                _image_header.cdelt2 = - _image_header.cdelt2;
                _image_header.crpix2 =
                    _image_header.naxis2 - _image_header.crpix2 + 1;

                /* fix FITS header also */
		/*
                _header.addValue("CDELT2", _image_header.cdelt2, null);
                _header.addValue("CRPIX2", _image_header.crpix2, null);
		*/
            }

           //do_stretch();
        }
        else
            System.out.println("Unimplemented bitpix = " + bitpix);

    }

    private static BasicHDU[] split_FITS_cube(BasicHDU hdu)
        throws FitsException
    {
        int x,y;
        byte[][][] data8;
        int[][][] data32;
        short[][][] data16;
        float[][][] datam32;
        double[][][] datam64;
        Header header = hdu.getHeader();
        nom.tam.fits.ImageData new_image_data = null;
        int bitpix = header.getIntValue("BITPIX",-1);
        int naxis = header.getIntValue("NAXIS",0);
        int naxis1 = header.getIntValue("NAXIS1",0);
        int naxis2 = header.getIntValue("NAXIS2",0);
        int naxis3 = header.getIntValue("NAXIS3",0);
        //nom.tam.fits.Data data = hdu.getData();
        BasicHDU[] retval = new BasicHDU[naxis3];
        for (int i = 0; i < naxis3; i++)
        {
            Header new_header = clone_header(header);
            //new_header.setNaxis(3, 1);  // bug fix (AR9592)

                switch (bitpix)
                {
                case 32:
                    data32 = (int[][][]) hdu.getData().getData();
                    int[][] new_data32 = new int[naxis2][naxis1];
                    for (x = 0; x < naxis1; x++)
                    {
                        for (y = 0; y < naxis2; y++)
                        {
                            new_data32[y][x] = data32[i][y][x];
                        }
                    }
                    new_image_data =
                        new nom.tam.fits.ImageData(new_data32);
                    break;
                case 16:
                    data16 = (short[][][]) hdu.getData().getData();
                    short[][] new_data16 = new short[naxis2][naxis1];
                    for (x = 0; x < naxis1; x++)
                    {
                        for (y = 0; y < naxis2; y++)
                        {
                            new_data16[y][x] = data16[i][y][x];
                        }
                    }
                    new_image_data =
                        new nom.tam.fits.ImageData(new_data16);
                    break;
                case 8:
                    data8 = (byte[][][]) hdu.getData().getData();
                    byte[][] new_data8 = new byte[naxis1][naxis1];
                    for (x = 0; x < naxis1; x++)
                    {
                        for (y = 0; y < naxis2; y++)
                        {
                            new_data8[y][x] = data8[i][y][x];
                        }
                    }
                    new_image_data =
                        new nom.tam.fits.ImageData(new_data8);
                    break;
                case -32:
                    datam32 = (float[][][]) hdu.getData().getData();
                    float[][] new_datam32 = new float[naxis2][naxis1];
                    for (x = 0; x < naxis1; x++)
                    {
                        for (y = 0; y < naxis2; y++)
                        {
                            new_datam32[y][x] = datam32[i][y][x];
                        }
                    }
                    new_image_data =
                        new nom.tam.fits.ImageData(new_datam32);
                    break;
                case -64:
                    datam64 = (double[][][]) hdu.getData().getData();
                    double[][] new_datam64 = new double[naxis2][naxis1];
                    for (x = 0; x < naxis1; x++)
                    {
                        for (y = 0; y < naxis2; y++)
                        {
                            new_datam64[y][x] = datam64[i][y][x];
                        }
                    }
                    new_image_data =
                        new nom.tam.fits.ImageData(new_datam64);
                    break;
                default:
                    Assert.tst(false,
                               "FitsRead.split_FITS_cube:  Unimplemented bitpix = " +
                               bitpix);
                }
            retval[i] = new ImageHDU(new_header, new_image_data);
            retval[i].addValue("SPOT_PL", i+1, "PLANE OF FITS CUBE (IN SPOT)");
            //System.out.println("RBH stored SPOT_PL as: " + (i+1));
	    new_header.resetOriginalSize();
        }

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

   public static void setDefaultFutureStretch(RangeValues defaultRangeValues)
   {
      _defaultRangeValues= defaultRangeValues;
   }

    public static RangeValues getDefaultFutureStretch() {
        return _defaultRangeValues;
    }


//    public synchronized void do_stretch(byte passedPixelData[],RangeValues newRangeValues,
//                                        boolean mapBlankToZero)
//    {
//	start_pixel = _start_pixel;
//	last_pixel = _last_pixel;
//	start_line = _start_line;
//	last_line = _last_line;
//        _rangeValues= newRangeValues;
//        do_stretch(passedPixelData, mapBlankToZero, 0,0,0,0);
//    }



    public synchronized void do_stretch(byte passedPixelData[], boolean mapBlankToZero,
                           int start_pixel, int last_pixel, int start_line, int last_line)
    {

        double bscale, bzero;
        double datamax, datamin;
	byte blank_pixel_value;
        Zscale.ZscaleRetval zscale_retval = null;

	if (mapBlankToZero)
	    blank_pixel_value = 0;
	else
	    blank_pixel_value = (byte) 255;

        //System.out.println("RBH SPOT_EXT = " + extension_number);
        //System.out.println("RBH SPOT_PL = " + plane_number);
        pixeldata=  passedPixelData;
//        _rangeValues= newRangeValues;

        datamin = _image_header.datamin;
        datamax = _image_header.datamax;
        bscale = _image_header.bscale;
        bzero = _image_header.bzero;

	Object onedimdata = null;
	switch(bitpix)
	{
	    case 32:
		onedimdata = onedimdata32;
		break;
	    case 16:
		onedimdata = onedimdata16;
		break;
	    case 8:
		onedimdata = onedimdata8;
		break;
	    case -32:
		onedimdata = onedimdatam32;
		break;
	    case -64:
		onedimdata = onedimdatam64;
		break;
	}

if (( _rangeValues.getLowerWhich() == RangeValues.ZSCALE) ||
	    (_rangeValues.getUpperWhich() == RangeValues.ZSCALE))
	{
	    if ((_rangeValues.getLowerWhich() == RangeValues.ZSCALE) ||
		(_rangeValues.getUpperWhich() == RangeValues.ZSCALE))
	    {
		double contrast = _rangeValues.getZscaleContrast();
		int opt_size = _rangeValues.getZscaleSamples();  
		    /* desired number of pixels in sample */
		int len_stdline = _rangeValues.getZscaleSamplesPerLine();  
		    /* optimal number of pixels per line */
		zscale_retval = Zscale.cdl_zscale(onedimdata, 
		    _image_header.naxis1, _image_header.naxis2, 
		    bitpix, contrast/100.0, opt_size, len_stdline, 
		    _image_header.blank_value,
		    _image_header.bscale,
		    _image_header.bzero);
	    }

	}

        if (hist == null)
        {
            if (((_rangeValues.getLowerWhich() != RangeValues.ABSOLUTE) &&
                (_rangeValues.getLowerWhich() != RangeValues.ZSCALE)) ||
                ((_rangeValues.getUpperWhich() != RangeValues.ABSOLUTE) &&
                (_rangeValues.getUpperWhich() != RangeValues.ZSCALE)))
            {
                /* do histogram only if needed */
		computeHistogram();
            }
        }


        switch (_rangeValues.getLowerWhich())
        {
               case RangeValues.ABSOLUTE:
                   slow = (_rangeValues.getLowerValue() - bzero) / bscale;
                   break;
               case RangeValues.PERCENTAGE:
                   slow = hist.get_pct(_rangeValues.getLowerValue(), false);
                   break;
               case RangeValues.SIGMA:
                   slow = hist.get_sigma(_rangeValues.getLowerValue(), false);
                   break;
               case RangeValues.MAXMIN:
                   slow = hist.get_pct(0.0, false);
                   break;
               case RangeValues.ZSCALE:
                   slow = zscale_retval.getZ1();
                   break;
               default:
                   Assert.tst(false, "illegal _rangeValues.getLowerWhich()");
        }
        switch (_rangeValues.getUpperWhich())
        {
               case RangeValues.ABSOLUTE:
                   shigh = (_rangeValues.getUpperValue() - bzero) / bscale;
                   break;
               case RangeValues.PERCENTAGE:
                   shigh = hist.get_pct(_rangeValues.getUpperValue(), true);
                   break;
               case RangeValues.SIGMA:
                   shigh = hist.get_sigma(_rangeValues.getUpperValue(), true);
                   break;
               case RangeValues.MAXMIN:
                   shigh = hist.get_pct(100.0, true);
                   break;
               case RangeValues.ZSCALE:
                   shigh = zscale_retval.getZ2();
                   break;
               default:
                   Assert.tst(false, "illegal _rangeValues.getUpperWhich()");
        }

        if (SUTDebug.isDebug())
        {
               System.out.println("slow = " + slow + "    shigh = " + shigh +
                                  "   bitpix = " + bitpix);
                if (_rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_LINEAR)
                    System.out.println("stretching STRETCH_LINEAR");
                else if (_rangeValues.getStretchAlgorithm() ==
                         RangeValues.STRETCH_LOG)
                    System.out.println("stretching STRETCH_LOG");
                else if (_rangeValues.getStretchAlgorithm() ==
                         RangeValues.STRETCH_LOGLOG)
                    System.out.println("stretching STRETCH_LOGLOG");
                else if (_rangeValues.getStretchAlgorithm() ==
                         RangeValues.STRETCH_EQUAL)
                    System.out.println("stretching STRETCH_EQUAL");
                else if (_rangeValues.getStretchAlgorithm() ==
                         RangeValues.STRETCH_SQUARED)
                    System.out.println("stretching STRETCH_SQUARED");
        }

       stretch_pixels(start_pixel, last_pixel, start_line, last_line,
	   bitpix, _image_header.naxis1, blank_pixel_value,
	   onedimdata, pixeldata, pixelhist);
	
	//byte[] glop = getHistColors();  // RBH DEBUG
    }


    void computeHistogram()
    {
        double datamin = _image_header.datamin;
        double datamax = _image_header.datamax;
        double bscale = _image_header.bscale;
        double bzero = _image_header.bzero;

	switch (bitpix)
	{
	case 32:
	    hist = new Histogram(onedimdata32,
				 (datamin - bzero) / bscale,
				 (datamax - bzero) / bscale,
				 blank_value);
	    break;
	case 16:
	    hist = new Histogram(onedimdata16,
				 (datamin - bzero) / bscale,
				 (datamax - bzero) / bscale,
				 blank_value);
	    break;
	case 8:
	    hist = new Histogram(onedimdata8, blank_value);
	    break;
	case -32:
	    hist = new Histogram(onedimdatam32,
				 (datamin - bzero) / bscale,
				 (datamax - bzero) / bscale,
				 blank_value);
	    break;
	case -64:
	    hist = new Histogram(onedimdatam64,
				 (datamin - bzero) / bscale,
				 (datamax - bzero) / bscale,
				 blank_value);
	    break;
	default:
	    Assert.tst(false,
		       "FitsRead.do_stretch:  Unimplemented bitpix = " +
		       bitpix);
	}
    }


    /* pixeldata and pixelhist are return values */
    private void stretch_pixels(int start_pixel, int last_pixel,
	                            int start_line, int last_line, int bitpix, int naxis1,
                                byte blank_pixel_value,
                                Object onedimdata,
                                byte[] pixeldata, int[] pixelhist)
    {
        int pixval;
        int i;
        double sdiff;
        int runval;
        int delta, deltasav;
        double floati, d_runval;
        double atbl;
        int tbl[] = new int[256];;
        int tbl1[] = new int[256];;
        int this_val, last_val;
        double dtbl[] = new double[256];

        sdiff = shigh - slow;

        for (i = 0; i < 255; i++)
            pixelhist[i] = 0;

        switch (bitpix)
        {
        case 32:
	    long start_time = (new Date()).getTime();
            if (_rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_LINEAR)
            {
		linear_tbl(tbl, slow, shigh);
            }
            else if
                ((_rangeValues.getStretchAlgorithm() ==
                  RangeValues.STRETCH_LOG) ||
                                           (_rangeValues.getStretchAlgorithm() ==
                                            RangeValues.STRETCH_LOGLOG))
            {
                sdiff = shigh - slow;
                if(sdiff == 0.)
                    sdiff = 1.;
                for (int j=0; j<255; ++j)
                {
                    atbl = Math.pow(10., j/254.0);
                    if (_rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LOGLOG)
                    {
                        atbl = Math.pow(10., (atbl - 1.0) / 9.0);
                    }
                    floati = (atbl - 1.) / 9. * sdiff + slow;
                    if  (-floati > Integer.MAX_VALUE)
                        tbl[j] = -Integer.MAX_VALUE;
                    else if (floati > Integer.MAX_VALUE)
                        tbl[j] =     Integer.MAX_VALUE;
                    else
                        tbl[j] = (int) floati;

                    //System.out.println("tbl["+ j + "] = " + tbl[j]);
                }
                tbl[255] = Integer.MAX_VALUE;
            }
            else if (_rangeValues.getStretchAlgorithm() ==
                     RangeValues.STRETCH_EQUAL)
            {
                hist.eq_tbl(tbl);
            }
            else if (_rangeValues.getStretchAlgorithm() ==
                     RangeValues.STRETCH_SQUARED)
            {
                squared_tbl(tbl, slow, shigh);
            }
            else if (_rangeValues.getStretchAlgorithm() ==
                     RangeValues.STRETCH_SQRT)
            {
                sqrt_tbl(tbl, slow, shigh);
            }


            if (sdiff >= 0)
                deltasav = 64;
            else
                deltasav = - 64;

	    int[] onedimdata32 = (int[]) onedimdata;

           //pixeldata = new byte[onedimdata32.length];
           //Assert.tst(pixeldata.length >= onedimdata32.length);

	   i = 0;
	   for (int line = start_line; line <= last_line; line++)
	   {
	   int start_index = line * naxis1 + start_pixel;
	   int last_index = line * naxis1 + last_pixel;
	   
           //for (i = 0; i < onedimdata32.length; i++)

	   for (int index= start_index; index <= last_index; index++)
           {

                // stretch each pixel
                if (onedimdata32[index] == blank_value)
                    pixeldata[i] = blank_pixel_value;
                else
                {
                    runval = onedimdata32[index];
                    pixval = 128;
                    delta = deltasav; /* 64 if ra normal, -64 if ra reversed */

                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] >= runval)
                        pixval -= 1;

                    pixeldata[i] = (byte) pixval;
                    pixeldata[i]= _rangeValues.computeBiasAndContrast(pixeldata[i]);
                    pixelhist[pixeldata[i] & 0xff]++;
                }
		i++;
           }
	   }
	   //System.out.println("RBH ELAPSED TIME = " + 
	   //    ((new Date()).getTime() - start_time) + " ms");
           break;
        case 16:
            if (_rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_LINEAR)
            {
                sdiff = shigh - slow;
                for (int j=0; j<255; j++)
                {
                    floati = (sdiff / 254) * j + slow;
                    if  (-floati > Integer.MAX_VALUE)
                        tbl[j] = -Integer.MAX_VALUE;
                    else if (floati > Integer.MAX_VALUE)
                        tbl[j] =     Integer.MAX_VALUE;
                    else
                        tbl[j] = (int) floati;
                }
                tbl[255] = Integer.MAX_VALUE;
            }
            else if
                ((_rangeValues.getStretchAlgorithm() ==
                  RangeValues.STRETCH_LOG) ||
                                           (_rangeValues.getStretchAlgorithm() ==
                                            RangeValues.STRETCH_LOGLOG))
            {
                sdiff = shigh - slow;
                if(sdiff == 0.)
                    sdiff = 1.;
                for (int j=0; j<255; ++j)
                {
                    atbl = Math.pow(10., j/254.0);
                    if (_rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LOGLOG)
                    {
                        atbl = Math.pow(10., (atbl - 1.0) / 9.0);
                    }
                    floati = (atbl - 1.) / 9. * sdiff + slow;
                    if  (-floati > Integer.MAX_VALUE)
                        tbl[j] = -Integer.MAX_VALUE;
                    else if (floati > Integer.MAX_VALUE)
                        tbl[j] =     Integer.MAX_VALUE;
                    else
                        tbl[j] = (int) floati;

                    //System.out.println("tbl["+ j + "] = " + tbl[j]);
                }
                tbl[255] = Integer.MAX_VALUE;
            }
            else if (_rangeValues.getStretchAlgorithm() ==
                     RangeValues.STRETCH_EQUAL)
            {
                hist.eq_tbl(tbl);
            }
            else if (_rangeValues.getStretchAlgorithm() ==
                     RangeValues.STRETCH_SQUARED)
            {
                squared_tbl(tbl, slow, shigh);
            }
            else if (_rangeValues.getStretchAlgorithm() ==
                     RangeValues.STRETCH_SQRT)
            {
                sqrt_tbl(tbl, slow, shigh);
            }


            if (sdiff >= 0)
                deltasav = 64;
            else
                deltasav = - 64;

	    short[] onedimdata16 = (short[]) onedimdata;


            //pixeldata = new byte[onedimdata16.length];
            //Assert.tst(pixeldata.length >= onedimdata16.length);

	    i = 0;
	   for (int line = start_line; line <= last_line; line++)
	   {
	   int start_index = line * naxis1 + start_pixel;
	   int last_index = line * naxis1 + last_pixel;

            //for (i = 0; i < onedimdata16.length; i++)
	   for (int index= start_index; index <= last_index; index++)
            {
                // stretch each pixel
                if (onedimdata16[index] == blank_value)
                    pixeldata[i] = blank_pixel_value;
                else
                {
                    runval = onedimdata16[index];
                    pixval = 128;
                    delta = deltasav; /* 64 if ra normal, -64 if ra reversed */

                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] >= runval)
                        pixval -= 1;

                    pixeldata[i] = (byte) pixval;
                    pixeldata[i]= _rangeValues.computeBiasAndContrast(pixeldata[i]);
                    pixelhist[pixeldata[i] & 0xff]++;
                }
		i++;


            }
	    }
            break;
       case 8:
            if (_rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_LINEAR)
            {
                sdiff = shigh - slow;
                for (int j=0; j<255; j++)
                {
                    tbl1[j] = (int) ((254 / sdiff) * (j - slow));
                    if (tbl1[j] < 0)
                        tbl1[j] = 0;
                    if (tbl1[j] > 254)
                        tbl1[j] = 254;
                }
                tbl1[255] = Integer.MAX_VALUE;
            }
            else if
                ((_rangeValues.getStretchAlgorithm() ==
                  RangeValues.STRETCH_LOG) ||
                                           (_rangeValues.getStretchAlgorithm() ==
                                            RangeValues.STRETCH_LOGLOG))
            {
                sdiff = shigh - slow;
                if(sdiff == 0.)
                    sdiff = 1.;
                for (int j=0; j<255; ++j)
                {
                    if (j <= slow)
                        tbl1[j] = 0;
                    else if (j >= shigh)
                        tbl1[j] = 254;
                    else
                    {
                        if (_rangeValues.getStretchAlgorithm() ==
                            RangeValues.STRETCH_LOG)
                        {
                            tbl1[j] = (int) (254 *
                                             .43429 * Math.log((9 * (j - slow) / sdiff) + 1));
                            /* .43429 changes from natural log to common log */
                        }
                        else
                        {
                            /* LOGLOG */
                            atbl = .43429 * Math.log((9 * (j - slow) / sdiff) + 1);
                            tbl1[j] = (int)
                                (254 * .43429 * Math.log((9.0 * atbl) + 1));
                        }

                    }


                    //System.out.println("tbl1["+ j + "] = " + tbl1[j]);
                }
                tbl1[255] = 254;
            }
            else if (
	  (_rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_EQUAL) ||
	  (_rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_SQUARED) ||
	  (_rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_SQRT) )
	     {
		    if (_rangeValues.getStretchAlgorithm() ==
			     RangeValues.STRETCH_EQUAL)
		    {
			hist.eq_tbl(tbl);
		    }
		    else if (_rangeValues.getStretchAlgorithm() ==
			     RangeValues.STRETCH_SQUARED)
		    {
			squared_tbl(tbl, slow, shigh);
		    }
		    else if (_rangeValues.getStretchAlgorithm() ==
			     RangeValues.STRETCH_SQRT)
		    {
			sqrt_tbl(tbl, slow, shigh);
		    }

                /* now interpolate */
                last_val = -1;
                for (int j = 0; j <= 255; j++)
                {
                    this_val = tbl[j];
                    if (this_val < 0)
                        this_val = 0;
                    else if (this_val > 254)
                        this_val = 254;
                    for (i = last_val+1; i <= this_val; i++)
                    {
                        tbl1[i] = j;
                    }
                    last_val = this_val;
                }
                for (i = last_val+1; i <= 255; i++)
                    tbl1[i] = 255;
            }

	    byte[] onedimdata8 = (byte[]) onedimdata;

           //pixeldata = new byte[onedimdata8.length];
           //Assert.tst(pixeldata.length >= onedimdata8.length);
           sdiff = shigh - slow;

	   i = 0;
	   for (int line = start_line; line <= last_line; line++)
	   {
	   int start_index = line * naxis1 + start_pixel;
	   int last_index = line * naxis1 + last_pixel;

           //for (i = 0; i < onedimdata8.length; i++)
	   for (int index= start_index; index <= last_index; index++)
           {
               // stretch each pixel
               pixval = onedimdata8[index] & 0xff;
               if (pixval == blank_value)
                   pixeldata[i] = blank_pixel_value;
               else
               {
                   if (pixval > shigh)
                       pixeldata[i] = (byte) 254;
                   else if (pixval < slow)
                       pixeldata[i] = (byte) 0;
                   else
                       pixeldata[i] = (byte) tbl1[pixval];
                   pixeldata[i]= _rangeValues.computeBiasAndContrast(pixeldata[i]);

                   pixelhist[pixeldata[i] & 0xff]++;
               }
	       i++;

           }
           }
           break;
       case -32:
            sdiff = shigh - slow;
            if(sdiff == 0.)
                sdiff = 1.;
            if
                ((_rangeValues.getStretchAlgorithm() ==
                  RangeValues.STRETCH_LOG) ||
                                           (_rangeValues.getStretchAlgorithm() ==
                                            RangeValues.STRETCH_LOGLOG))
            {
                for (int j=0; j<255; ++j)
                {
                    atbl = Math.pow(10., j/254.0);
                    if (_rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LOGLOG)
                    {
                        atbl = Math.pow(10., (atbl - 1.0) / 9.0);
                    }
                    dtbl[j] = (atbl - 1.) / 9. * sdiff + slow;

                    //System.out.println("dtbl["+ j + "] = " + dtbl[j]);
                }
                dtbl[255] = Double.MAX_VALUE;
            }
            else if (_rangeValues.getStretchAlgorithm() ==
                     RangeValues.STRETCH_EQUAL)
            {
                hist.deq_tbl(dtbl);
            }
            else if (_rangeValues.getStretchAlgorithm() ==
                     RangeValues.STRETCH_SQUARED)
            {
                squared_tbl_dbl(dtbl, slow, shigh);
            }
            else if (_rangeValues.getStretchAlgorithm() ==
                     RangeValues.STRETCH_SQRT)
            {
                sqrt_tbl_dbl(dtbl, slow, shigh);
            }

            if (sdiff > 0)
                deltasav = 64;
            else
                deltasav = - 64;


	    float[] onedimdatam32 = (float[]) onedimdata;

           //pixeldata = new byte[onedimdatam32.length];
           //System.out.println("RBH pixeldata.length = " + pixeldata.length +
           //    "   onedimdatam32.length = " + onedimdatam32.length);
           //Assert.tst(pixeldata.length >= onedimdatam32.length);
	   i = 0;
	   for (int line = start_line; line <= last_line; line++)
	   {
	   int start_index = line * naxis1 + start_pixel;
	   int last_index = line * naxis1 + last_pixel;
	   
           //for (i = 0; i < onedimdatam32.length; i++)

	   for (int index= start_index; index <= last_index; index++)

           {
                // stretch each pixel
                if (Double.isNaN(onedimdatam32[index]))
                {
                    pixeldata[i] = blank_pixel_value;
                }
                else
                {
                if (_rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_LINEAR)
                {
                    d_runval = ((onedimdatam32[index] - slow ) * 254 / sdiff);
                    if (d_runval < 0)
                        pixeldata[i] = 0;
                    else if (d_runval > 254)
                        pixeldata[i] = (byte) 254;
                    else
                        pixeldata[i] = (byte) d_runval;
                }
                else
                {
                    d_runval = onedimdatam32[index];
                    pixval = 128;
                    delta = deltasav; /* 64 if ra normal, -64 if ra reversed */

                    if (dtbl[pixval] < d_runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (dtbl[pixval] < d_runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (dtbl[pixval] < d_runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (dtbl[pixval] < d_runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (dtbl[pixval] < d_runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (dtbl[pixval] < d_runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (dtbl[pixval] < d_runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (dtbl[pixval] >= d_runval)
                        pixval -= 1;

                    pixeldata[i] = (byte) pixval;

                }
                pixeldata[i]= _rangeValues.computeBiasAndContrast(pixeldata[i]);
               pixelhist[pixeldata[i] & 0xff]++;
               }
	       i++;

           }
           }
           break;
       case -64:
            sdiff = shigh - slow;
            if(sdiff == 0.)
                sdiff = 1.;
            if
                ((_rangeValues.getStretchAlgorithm() ==
                  RangeValues.STRETCH_LOG) ||
                                           (_rangeValues.getStretchAlgorithm() ==
                                            RangeValues.STRETCH_LOGLOG))
            {
                for (int j=0; j<255; ++j)
                {
                    atbl = Math.pow(10., j/254.0);
                    if (_rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LOGLOG)
                    {
                        atbl = Math.pow(10., (atbl - 1.0) / 9.0);
                    }
                    dtbl[j] = (atbl - 1.) / 9. * sdiff + slow;

                    //System.out.println("dtbl["+ j + "] = " + dtbl[j]);
                }
                dtbl[255] = Double.MAX_VALUE;
            }
            else if (_rangeValues.getStretchAlgorithm() ==
                     RangeValues.STRETCH_EQUAL)
            {
                hist.deq_tbl(dtbl);
            }
            else if (_rangeValues.getStretchAlgorithm() ==
                     RangeValues.STRETCH_SQUARED)
            {
                squared_tbl_dbl(dtbl, slow, shigh);
            }
            else if (_rangeValues.getStretchAlgorithm() ==
                     RangeValues.STRETCH_SQRT)
            {
                sqrt_tbl_dbl(dtbl, slow, shigh);
            }

            if (sdiff > 0)
                deltasav = 64;
            else
                deltasav = - 64;


	    double[] onedimdatam64 = (double[]) onedimdata;
           //pixeldata = new byte[onedimdatam64.length];
           //Assert.tst(pixeldata.length >= onedimdatam64.length);
	   i = 0;
	   for (int line = start_line; line <= last_line; line++)
	   {
	   int start_index = line * naxis1 + start_pixel;
	   int last_index = line * naxis1 + last_pixel;
	   
           //for (i = 0; i < onedimdatam64.length; i++)

	   for (int index= start_index; index <= last_index; index++)
           {
                // stretch each pixel
                if (Double.isNaN(onedimdatam64[index]))
                {
                    pixeldata[i] = blank_pixel_value;
                }
                else
                {
                if (_rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_LINEAR)
                {
                    d_runval = ((onedimdatam64[index] - slow ) * 254 / sdiff);
                    if (d_runval < 0)
                        pixeldata[i] = 0;
                    else if (d_runval > 254)
                        pixeldata[i] = (byte) 254;
                    else
                        pixeldata[i] = (byte) d_runval;
                }
                else
                {
                    d_runval = onedimdatam64[index];
                    pixval = 128;
                    delta = deltasav; /* 64 if ra normal, -64 if ra reversed */

                    if (dtbl[pixval] < d_runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (dtbl[pixval] < d_runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (dtbl[pixval] < d_runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (dtbl[pixval] < d_runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (dtbl[pixval] < d_runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (dtbl[pixval] < d_runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (dtbl[pixval] < d_runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (dtbl[pixval] >= d_runval)
                        pixval -= 1;

                    pixeldata[i] = (byte) pixval;

                }
                pixeldata[i]= _rangeValues.computeBiasAndContrast(pixeldata[i]);

               pixelhist[pixeldata[i] & 0xff]++;
               }
	       i++;

           }
           }
           break;
       }
    }

    /** 
    * Return an array where each element corresponds to an element of 
    * the histogram, and the value in each element is the screen pixel 
    * value which would result from an image pixel which falls into that 
    * histogram bin.
    * @return array of byte (4096 elements)
    */
    public byte[] getHistColors()
    {
	int start_pixel = 0;
	int last_pixel = 4095;
	int start_line = 0;
	int last_line = 0;
	int bitpix = -64;
	int naxis1 = 1;
	byte blank_pixel_value = 0;
	byte[] pixeldata = new byte[4096];
        int[] pixelhist = new int[256];


	/*
	int[] onedimdata32;
	Object onedimdata = null;
	onedimdata = onedimdatam32;
	*/

	double[] hist_bin_values = new double[4096];
	for (int i = 0; i < 4096; i++)
	{
	    hist_bin_values[i] = hist.getDNfromBin(i);
	}


	Object onedimdata = (Object) hist_bin_values;

        stretch_pixels(start_pixel, last_pixel, start_line, last_line,
	   bitpix, naxis1, blank_pixel_value, onedimdata, pixeldata, pixelhist);

	/*
	for (int i = 0; i < 240; i++)
	{
	    int pixelDN = pixeldata[i] & 0xff;
	    System.out.println("hist_bin_values[" + i + "] = " + 
		hist_bin_values[i]  + "  -> pixval = " + pixelDN);
	}
	*/
	return pixeldata;
    }

    /**
    * fill the 256 element table with linear values
    */
    private void linear_tbl(int tbl[], double slow, double shigh)
    {
	double floati;
	double sdiff;

	sdiff = shigh - slow;
	for (int j=0; j<255; j++)
	{
	    floati = (sdiff / 254) * j + slow;
	    if  (-floati > Integer.MAX_VALUE)
		tbl[j] = -Integer.MAX_VALUE;
	    else if (floati > Integer.MAX_VALUE)
		tbl[j] =     Integer.MAX_VALUE;
	    else
		tbl[j] = (int) floati;
	}
	tbl[255] = Integer.MAX_VALUE;
    }

    /**
    * fill the 256 element table with values for a squared stretch
    */
    private void squared_tbl(int tbl[], double slow, double shigh)
    {
	double floati;
	double sdiff;

	sdiff = shigh - slow;
	if(sdiff == 0.)
	    sdiff = 1.;
	for (int j=0; j<255; ++j)
	{
	    floati = Math.sqrt(sdiff*sdiff/254*j) + slow;
	    //System.out.println("RBH j = " + j + "  floati = " + floati);
	    if  (-floati > Integer.MAX_VALUE)
		tbl[j] = -Integer.MAX_VALUE;
	    else if (floati > Integer.MAX_VALUE)
		tbl[j] =     Integer.MAX_VALUE;
	    else
		tbl[j] = (int) floati;

	    //System.out.println("tbl["+ j + "] = " + tbl[j]);
	}
	tbl[255] = Integer.MAX_VALUE;
    }

    /**
    * fill the 256 element table with values for a squared stretch
    * for floating point pixels 
    */
    private void squared_tbl_dbl(double tbl[], double slow, double shigh)
    {
	double floati;
	double sdiff;

	sdiff = shigh - slow;
	if(sdiff == 0.)
	    sdiff = 1.;
	for (int j=0; j<255; ++j)
	{
	    floati = Math.sqrt(sdiff*sdiff/254*j) + slow;
	    //System.out.println("RBH j = " + j + "  floati = " + floati);
	    tbl[j] = floati;

	    //System.out.println("tbl["+ j + "] = " + tbl[j]);
	}
	tbl[255] = Double.MAX_VALUE;
    }

    /**
    * fill the 256 element table with values for a square root stretch
    */
    private void sqrt_tbl(int tbl[], double slow, double shigh)
    {
	double floati;
	double sdiff;

	sdiff = shigh - slow;
	if(sdiff == 0.)
	    sdiff = 1.;
	for (int j=0; j<255; ++j)
	{
	    floati = (Math.sqrt(sdiff)/254*j);
	    floati = floati * floati + slow;
	    //System.out.println("RBH j = " + j + "  floati = " + floati);
	    if  (-floati > Integer.MAX_VALUE)
		tbl[j] = -Integer.MAX_VALUE;
	    else if (floati > Integer.MAX_VALUE)
		tbl[j] =     Integer.MAX_VALUE;
	    else
		tbl[j] = (int) floati;

	    //System.out.println("tbl["+ j + "] = " + tbl[j]);
	}
	tbl[255] = Integer.MAX_VALUE;
    }

    /**
    * fill the 256 element table with values for a square root stretch
    * for floating point pixels 
    */
    private void sqrt_tbl_dbl(double tbl[], double slow, double shigh)
    {
	double floati;
	double sdiff;

	sdiff = shigh - slow;
	if(sdiff == 0.)
	    sdiff = 1.;
	for (int j=0; j<255; ++j)
	{
	    floati = (Math.sqrt(sdiff)/254*j);
	    floati = floati * floati + slow;
	    tbl[j] = floati;

	    //System.out.println("tbl["+ j + "] = " + tbl[j]);
	}
	tbl[255] = Double.MAX_VALUE;
    }

    public byte[] getData8()
    {
        return (pixeldata);
    }

    // returns pixels with BSCALE and BZERO applied 
    public float[] getDataFloat()
    {
        int pixel_count = _image_header.naxis1 * _image_header.naxis2;
        float[] float_data = new float[pixel_count];
        int i;

        switch (_image_header.bitpix)
        {
            case 32:
                for (i = 0; i < pixel_count; i++)
                {
                    float_data[i] = (float)
                        (onedimdata32[i] * _image_header.bscale + _image_header.bzero);
                }
                break;
            case 16:
                for (i = 0; i < pixel_count; i++)
                {
                    float_data[i] = (float)
                        (onedimdata16[i] * _image_header.bscale + _image_header.bzero);
                }
                break;
            case 8:
                for (i = 0; i < pixel_count; i++)
                {
                    float_data[i] = (float)
                        ((onedimdata8[i] & 0xff) * _image_header.bscale + _image_header.bzero);
                }
                break;
            case -32:
                for (i = 0; i < pixel_count; i++)
                {
                    float_data[i] = (float)
                        (onedimdatam32[i] * _image_header.bscale + _image_header.bzero);
                }
                break;
            case -64:
                for (i = 0; i < pixel_count; i++)
                {
                    float_data[i] = (float) 
                        (onedimdatam64[i] * _image_header.bscale + _image_header.bzero);
                }
                break;
        }
        return float_data;
    }


    /**
     * Takes an open FITS file and returns a 2-dim float array of the pixels
     * Works for all 5 FITS data types and all FITS array dimensionality
     * Observes BLANK value for integer data
     * Applies BSCALE and BZERO
     * @param image_hdu ImageHDU for the open FITS file
     * @return 2-dim float array of pixels
     */
    public static float[][] getDataFloat(ImageHDU image_hdu) 
		throws FitsException
    {
	Header header = image_hdu.getHeader();
	int bitpix = header.getIntValue("BITPIX");
	int naxis1 = header.getIntValue("NAXIS1");
	int naxis2 = header.getIntValue("NAXIS2");
	int blank;
	double bscale = header.getDoubleValue("BSCALE", 1.0);
	double bzero = header.getDoubleValue("BZERO", 0.0);

        int pixel_count = naxis1 * naxis2;
        float[][] float_data = new float[naxis2][naxis1];
        int i;
	int line, sample;
	boolean check_blank;

        switch (bitpix)
        {
            case 32:
		check_blank = header.containsKey("BLANK");
		blank = header.getIntValue("BLANK");
		int[] onedimdata32 =
		    (int[]) ArrayFuncs.flatten( image_hdu.getData().getData());
		i = 0;
		for (line = 0; line < naxis2; line++)
		{
		    for (sample = 0; sample < naxis1; sample++)
		    {
			if (check_blank && (onedimdata32[i] == blank))
			{
			    float_data[line][sample] = Float.NaN;
			}
			else
			{
			    float_data[line][sample] = (float)
				(onedimdata32[i] * bscale + bzero);
			}
			i++;
		    }
		}
                break;
            case 16:
		check_blank = header.containsKey("BLANK");
		blank = header.getIntValue("BLANK");
		short[] onedimdata16 =
		    (short[]) ArrayFuncs.flatten( image_hdu.getData().getData());
		i = 0;
		for (line = 0; line < naxis2; line++)
		{
		    for (sample = 0; sample < naxis1; sample++)
		    {
			if (check_blank && (onedimdata16[i] == blank))
			{
			    float_data[line][sample] = Float.NaN;
			}
			else
			{
			    float_data[line][sample] = (float)
				(onedimdata16[i] * bscale + bzero);
			}
			i++;
		    }
		}
                break;
            case 8:
		check_blank = header.containsKey("BLANK");
		blank = header.getIntValue("BLANK");
		byte[] onedimdata8 =
		    (byte[]) ArrayFuncs.flatten( image_hdu.getData().getData());
		i = 0;
		for (line = 0; line < naxis2; line++)
		{
		    for (sample = 0; sample < naxis1; sample++)
		    {
			if (check_blank && (onedimdata8[i] == blank))
			{
			    float_data[line][sample] = Float.NaN;
			}
			else
			{
			    float_data[line][sample] = (float)
				((onedimdata8[i] & 0xff) * bscale + bzero);
			}
			i++;
		    }
		}
                break;
            case -32:
		float[] onedimdatam32 =
		    (float[]) ArrayFuncs.flatten( image_hdu.getData().getData());
		i = 0;
		for (line = 0; line < naxis2; line++)
		{
		    for (sample = 0; sample < naxis1; sample++)
		    {
			float_data[line][sample] = (float)
			    (onedimdatam32[i] * bscale + bzero);
			i++;
		    }
		}
                break;
            case -64:
		double[] onedimdatam64 =
		    (double[]) ArrayFuncs.flatten( image_hdu.getData().getData());
		i = 0;
		for (line = 0; line < naxis2; line++)
		{
		    for (sample = 0; sample < naxis1; sample++)
		    {
			float_data[line][sample] = (float)
			    (onedimdatam64[i] * bscale + bzero);
			i++;
		    }
		}
                break;
        }
        return float_data;
    }


    /**
     * Creates a new ImageHDU given the original HDU and the new array of pixels
     *  The new header part reflects the 2-dim float data
     *  The new data part contains the new pixels
     * Sets NAXISn according to the actual dimensions of pixels[][], which is
     *   not necessarily the dimensions of the original image
     * @param hdu ImageHDU for the open FITS file
     * @param pixels The 2-dim float array of new pixels
     * @return The new ImageHDU
     */
    public static ImageHDU makeHDU(ImageHDU hdu, float[][] pixels)
		    throws FitsException
    {
	Header header = hdu.getHeader();

	// first clone the header
	Cursor iter = header.iterator();
	String cards[] = new String[header.getNumberOfCards()];
	HeaderCard card;
	int i = 0;
	while (iter.hasNext())
	{
	    card = (HeaderCard) iter.next();
	    cards[i] = card.toString();
	    i++;
	}
	Header new_header = new Header(cards);

	new_header.deleteKey("BITPIX");
	new_header.setBitpix(-32);
	new_header.deleteKey("NAXIS");
	new_header.setNaxes(2);
	new_header.deleteKey("NAXIS1");
	new_header.setNaxis(1, pixels[0].length);
	new_header.deleteKey("NAXIS2");
	new_header.setNaxis(2, pixels.length);

	new_header.deleteKey("DATAMAX");
	new_header.deleteKey("DATAMIN");
	new_header.deleteKey("NAXIS3");
	new_header.deleteKey("NAXIS4");
	new_header.deleteKey("BLANK");
	new_header.deleteKey("BSCALE");
	new_header.deleteKey("BZERO");

	ImageData new_image_data = new ImageData(pixels);
	hdu = new ImageHDU(new_header, new_image_data);
	return hdu;
    }


    public int[] getScreenHistogram()
    {
        pixelhist[255] = 0;  // pixelhist[255] is count of blank pixels
        return (pixelhist);
    }



    /** Get flux of pixel at given "ImagePt" coordinates 
    *    "ImagePt" coordinates have 0,0 lower left corner of lower left pixel
    *    of THIS image
    * @param ipt ImagePt coordinates
    */

    public double getFlux( ImagePt ipt)
		   throws PixelValueException
    {
	double x = ipt.getX() - 0.5;
	double y = ipt.getY() - 0.5;


	int xint = (int) Math.round(x);
	int yint = (int) Math.round(y);

        int index = yint * _image_header.naxis1 + xint;
        double raw_dn;

        if ((xint < 0) || (xint >= _image_header.naxis1) ||
            (yint < 0) || (yint >= _image_header.naxis2))
            throw new PixelValueException("location not on the image");
        if (_image_header.bitpix == 32)
            raw_dn = onedimdata32[index];
        else if (_image_header.bitpix == 16)
            raw_dn = onedimdata16[index];
        else if (_image_header.bitpix == 8)
            raw_dn = onedimdata8[index];
        else if (_image_header.bitpix == -32)
            raw_dn = onedimdatam32[index];
        else if (_image_header.bitpix == -64)
            raw_dn = onedimdatam64[index];
        else
            throw new PixelValueException("illegal bitpix");

        if ((raw_dn == _image_header.blank_value) || (Double.isNaN(raw_dn)))
            throw new PixelValueException("No flux available");

	double flux;
	if (_image_header.origin.startsWith("Palomar Transient Factory"))
	{
	    flux = -2.5 * .43429 * Math.log(raw_dn / _image_header.exptime) + 
		_image_header.imagezpt + 
		_image_header.extinct * _image_header.airmass;
			/* .43429 changes from natural log to common log */
	}
	else
	{
	    flux = raw_dn * _image_header.bscale + _image_header.bzero;
	}

	/*
	if (SUTDebug.isDebug())
	{
	    System.out.println("RBH PixelValue x = " + x
	    + "  y = " + y
	    + "  xint = " + xint
	    + "  yint = " + yint
	    + "  bscale = " + _image_header.bscale
	    + "  bzero = " + _image_header.bzero
	    + "   raw_dn = " + raw_dn
	    + " flux = " + flux);
	}
	*/

        return (flux);
    }

    public String getFluxUnits() {
	String retval = _image_header.bunit;
	if (_image_header.bunit.startsWith("HITS"))
	{
	    retval = "frames";
	}
	if (_image_header.origin.startsWith(ImageHeader.PALOMAR_ID))
	{
	    retval = "mag";
	}
	return(retval);
    }

    public int getProjectionType() { return getImageHeader().maptype; }


    public boolean isSameProjection( FitsRead second_fitsread)
    {
        boolean result;

        ImageHeader H1 = getImageHeader();
        ImageHeader H2 = second_fitsread.getImageHeader();

        if (H1.maptype == H2.maptype)
        {
            if (H1.maptype == Projection.PLATE)
            {
                if (
                    (H1.plate_ra == H2.plate_ra) &&
                    (H1.plate_dec == H2.plate_dec) &&
                    (H1.x_pixel_offset == H2.x_pixel_offset) &&
                    (H1.y_pixel_offset == H2.y_pixel_offset) &&
                    (H1.plt_scale == H2.plt_scale) &&
                    (H1.x_pixel_size == H2.x_pixel_size) &&
                    (H1.y_pixel_size == H2.y_pixel_size))
                {
                    /* OK so far - now check coefficients */
                    result = true;
                    for (int i = 0; i < 6; i++)
                    {
                        if (H1.ppo_coeff[i] != H2.ppo_coeff[i])
                            result = false;
                    }
                    for (int i = 0; i < 20; i++)
                    {
                        if (H1.amd_x_coeff[i] != H2.amd_x_coeff[i])
                            result = false;
                        if (H1.amd_y_coeff[i] != H2.amd_y_coeff[i])
                            result = false;
                    }
                }
                else
                {
                    result = false;
                }
            }
            else
            {
                if (
                    (H1.naxis1 == H2.naxis1) &&
                    (H1.naxis2 == H2.naxis2) &&
                    (H1.crpix1 == H2.crpix1) &&
                    (H1.crpix2 == H2.crpix2) &&
                    (H1.cdelt1 == H2.cdelt1) &&
                    (H1.cdelt2 == H2.cdelt2) &&
                    (H1.crval1 == H2.crval1) &&
                    (H1.crval2 == H2.crval2) &&
                    (H1.crota2 == H2.crota2) &&
                    (H1.getJsys() == H2.getJsys()) &&
                    (H1.file_equinox == H2.file_equinox))
                    {
                        /* OK so far - now check distortion correction */
                        if (H1.map_distortion &&
                            H2.map_distortion)
                        {
                            /* both have distortion corrections */
                            if ((H1.ap_order == H2.ap_order) &&
                                (H1.a_order == H2.a_order) &&
                                (H1.bp_order == H2.bp_order) &&
                                (H1.b_order == H2.b_order))
                            {
                                result = true;
                                for (int i = 0; i <= H1.a_order; i++)
                                {
                                    for (int j = 0; j <= H1.a_order; j++)
                                    {
                                        if ((i+j <= H1.a_order) && (i+j > 0))
                                        {
                                            if (H1.a[i][j] != H2.a[i][j])
                                            {
                                                result = false;
                                            }
                                        }
                                    }
                                }
                                for (int i = 0; i <= H1.ap_order; i++)
                                {
                                    for (int j = 0; j <= H1.ap_order; j++)
                                    {
                                        if ((i+j <= H1.ap_order) && (i+j > 0))
                                        {
                                            if (H1.ap[i][j] != H2.ap[i][j])
                                            {
                                                result = false;
                                            }
                                        }
                                    }
                                }
                                for (int i = 0; i <= H1.b_order; i++)
                                {
                                    for (int j = 0; j <= H1.b_order; j++)
                                    {
                                        if ((i+j <= H1.b_order) && (i+j > 0))
                                        {
                                            if (H1.b[i][j] != H2.b[i][j])
                                            {
                                                result = false;
                                            }
                                        }
                                    }
                                }
                                for (int i = 0; i <= H1.bp_order; i++)
                                {
                                    for (int j = 0; j <= H1.bp_order; j++)
                                    {
                                        if ((i+j <= H1.bp_order) && (i+j > 0))
                                        {
                                            if (H1.bp[i][j] != H2.bp[i][j])
                                            {
                                                result = false;
                                            }
                                        }
                                    }
                                }
                            }
                            else
                            {
                                result = false;
                            }
                        }
                        else
                        {
                            result = true;
                        }
                    }
                    else
                    {
                        result = false;
                    }
            }
        }
        else
        {
            result = false;
        }
        return result;
    }


    public Fits getFits()
    {
        return (myFits);
    }

    public BasicHDU getHDU()
    {
        return _hdu;
    }

    public Header getHeader()
    {
        return _header;
    }

    public ImageHeader getImageHeader()
    {
        return (_image_header);
    }

    public RangeValues getRangeValues()
    {
        return (_rangeValues);
    }


    public void setRangeValues(RangeValues rangeValues) {
        _rangeValues= rangeValues;
    }

    public int getImageScaleFactor()
    {
        return (_imageScaleFactor);
    }

    /**
     * get a description of the fits file that created this fits read
     * This can be any text.
     * @return the description of the fits file
     */
    public String getSourceDec()
    {
        return (_srcDesc);
    }

    /**
     * Set a description of the fits file that created this fits read.
     * This can be any text.
     * @param s the description
     */
    public void setSourceDesc(String s)
    {
        _srcDesc= s;
    }


    Histogram getHistogram() { 
	if (hist == null)
	{
	    computeHistogram();
	}
	return hist; 
    }

    /**
     * return the index of where this fits data was i a fits file.  If a -1
     * is returned the we do not know or many this FitsRead was created with
     * geom.  Otherwise if a number >= 0 other is return then that is the
     * location in the fits file
     * @return index of where this fits data was in file
     */
    public int getIndexInFile()
    {
        return _indexInFile;
    }

    /**
     * return the plane number indicating which plane in a FITS cube
     * this image came from.
     * return value:
     *   0:  this was the only image - there was no cube
     *   1:  this was the first plane in the FITS cube
     *   2:  this was the second plane in the FITS cube
     *   etc.
     */
    public int getPlaneNumber()
    {
        return plane_number;
    }

    /**
     * return the extension number indicating which extension this image
     * was in the original FITS image
     * return value:
     *  -1:  this was the only image, the primary one - there were no extensions
     *   0:  this was the primary image (not an extension) in a FITS file with
     *              extensions
     *   1:  this was the first extension in the FITS file
     *   2:  this was the second extension in the FITS file
     *   etc.
     */
    public int getExtensionNumber()
    {
        return extension_number;
    }



    public static void main(String[] args)  throws FitsException, IOException
    {
	    //SUTDebug.setDebug(true);

            FitsRead[] fits_read_array;

            if (args.length != 3)
            {
                System.out.println("usage: FitsRead <file1> <rotation> <output_filename>");
                System.exit(1);
            }

	    double rotation = Integer.parseInt(args[1]);
	    String out_name = args[2];
	    File file = new File(args[0]);
	    if (!file.canRead())
	    {
                System.out.println("Cannot read file " + args[0]);
                System.exit(1);
	    }
            Fits fits0 = new Fits(args[0]);
            fits_read_array = createFitsReadArray(fits0);
            FitsRead fits_read_0 = fits_read_array[0];
	    System.out.println("RBHX for fits_read_0 data_offset = " + 
		fits_read_0.getImageHeader().data_offset);
	    FitsRead new_fits_read = null;
	    try
	    {
		//new_fits_read = 
		    //createFitsReadPositionAngle(fits_read_0, rotation, CoordinateSys.EQ_J2000);
		    //createFitsReadPositionAngle(fits_read_0, rotation, CoordinateSys.ECL_B1950);
		//new_fits_read = 
		    //createFitsReadRotated(fits_read_0, rotation);
		new_fits_read = 
		    createFitsReadFlipLR(fits_read_0);
	    }
	    catch (GeomException ge)
	    {
		System.out.println("GeomException: " + ge.getMessage());
		System.exit(1);
	    }


	    System.out.println("RBHX for new_fits_read data_offset = " + 
		new_fits_read.getImageHeader().data_offset);

	    Fits newFits = new_fits_read.getFits();
	    FileOutputStream fo = new java.io.FileOutputStream(out_name);
	    BufferedDataOutputStream o = new BufferedDataOutputStream(fo);
	    System.out.println("FitsRead:  writing output file = " + out_name);
	    newFits.write(o);
	    System.out.println("FitsRead:  done writing output file");

	    if (false)
	    {
	    /* NEW */
	    Fits cropFits= Crop.do_crop(newFits, 30,30,150,150);
	    FitsRead[]  cropFr=  FitsRead.createFitsReadArray(cropFits);
	    System.out.println("RBHX for cropFr[0] data_offset = " + 
		cropFr[0].getImageHeader().data_offset);
	    }


    }

    /*
    public static void main(String[] args)  throws FitsException, IOException
    {
            FitsRead[] fits_read_array;

            if (args.length != 2)
            {
                System.out.println("usage: FitsRead <file1> <file2>");
                System.exit(1);
            }

            Fits fits0 = new Fits(args[0]);
            fits_read_array = createFitsReadArray(fits0);
            FitsRead fits_read_0 = fits_read_array[0];

            Fits fits1 = new Fits(args[1]);
            fits_read_array = createFitsReadArray(fits1);
            FitsRead fits_read_1 = fits_read_array[0];

            boolean result = fits_read_1.isSameProjection(fits_read_0);
            System.out.println("result = " + result);
    }
    */

    public void freeResources() {
        pixeldata= null;
        pixelhist= null;
        onedimdata8= null;
        onedimdata32= null;
        onedimdata16= null;
        onedimdatam32= null;
        onedimdatam64= null;
        myFits= null;
        _image_header= null;
        _header= null;
        hist = null;
        _rangeValues= null;
    }

    public long getDataSize() {
        long retval= 0;
        if (pixeldata!=null) retval+= pixeldata.length;
        if (pixelhist!=null) retval+= pixelhist.length*4;
        if (onedimdata8!=null) retval+= onedimdata8.length;
        if (onedimdata32!=null) retval+= onedimdata32.length*4;
        if (onedimdata16!=null) retval+= onedimdata16.length*2;
        if (onedimdatam32!=null) retval+= onedimdatam32.length*4;
        if (onedimdatam64!=null) retval+= onedimdatam64.length*8;
        return retval;
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
