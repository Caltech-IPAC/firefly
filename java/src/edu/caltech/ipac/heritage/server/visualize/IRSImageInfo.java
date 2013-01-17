package edu.caltech.ipac.heritage.server.visualize;

import edu.caltech.ipac.visualize.plot.MiniFitsHeader;
import edu.caltech.ipac.heritage.data.entity.IRSInfoData;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.PixelValue;
import edu.caltech.ipac.visualize.plot.PixelValueException;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
//import edu.caltech.ipac.firefly.server.visualize.WebPlotServer;

import java.awt.Point;
import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * Based on Java code provided by Russ.
 * Date: Jun 19, 2006
 * @version $id:$
 */
public class IRSImageInfo implements Serializable {

    private static final String CHANNEL = "CHNLNUM";
    private static final String FOVID   = "FOVID";
    private static final String RA_SLT  = "RA_SLT";
    private static final String DEC_SLT = "DEC_SLT";
    private static final String PA_SLT  = "PA_SLT";
    private static final double D2R = Math.PI / 180.;

    public static String wavsamp_wave_string =  "_wavsamp_wave.fits";
    public static String wavsamp_offset_string = "_wavsamp_offset.fits";
    public static String wavsamp_omask_string = "_wavsamp_omask.fits";
    private MiniFitsHeader header_wave;
    private MiniFitsHeader header_masks;
    private MiniFitsHeader header_offsets;
    private MiniFitsHeader header_IRS_file;
    private File f_wave;
    private File f_offsets;
    private File f_masks;
    private File IRS_file;



    private static float[][] _pixelValues = null;  // [y][x]

    private IRSInfoData _data;
    private Projection _projection = null;

    private int    _channel;
    private double _aperture;
    private double _raSLT;
    private double _decSLT;
    private double _paSLT;
    private int _naxis1;



    /**
    *  Create a class to access IRS 2d Spectral image values (coordiantes,
    *  wavelength, and pixel value
    *  @param _header FITS Header object with values from IRS 2d spectral img
    *  @param filenames List of calibration filenames for this image
    *  @param _IRS_file File object for the IRS 2d Spectral image file
    */
    public IRSImageInfo(Header _header, Collection<String> filenames,
	File _IRS_file) {
        _data = new IRSInfoData();
	ImageHeader image_header = null;
	updateHeaderValues(_header);
	try
	{
	    image_header = new ImageHeader(_header);
	    CoordinateSys coordinate_sys = CoordinateSys.makeCoordinateSys(
		image_header.getJsys(), image_header.file_equinox);
	    _projection = image_header.createProjection(coordinate_sys);
	}
	catch (FitsException fe)
	{
	    // do nothing - _projection will remain null
	}

	IRS_file = _IRS_file;
	header_IRS_file = image_header.makeMiniHeader();

	Iterator calfile = filenames.iterator();
	while (calfile.hasNext())
	{
	    String filename = (String) calfile.next();
	    try
	    {
		if (filename.endsWith(wavsamp_wave_string))
		{
		    loadWaveLengths(
			getCalFile_WAVSAMP_WAVE(filename));
		}
		else if (filename.endsWith(wavsamp_omask_string))
		{
		    loadMasks(
			getCalFile_WAVSAMP_OMASK(filename));
		}
		else if (filename.endsWith(wavsamp_offset_string))
		{
		    loadOffsets(
			getCalFile_WAVSAMP_OFFSET(filename));
		}
	    }
	    catch (FileNotFoundException e) 
	    {
		e.printStackTrace();
	    }
	    catch (FitsException fe)
	    {
		fe.printStackTrace();
	    }
	}
    }


    private void loadWaveLengths(File f) throws FileNotFoundException, FitsException {
        if (!f.exists()){
            throw new FileNotFoundException();
        }
	f_wave = f;
        Fits fits = new Fits(f);
        BasicHDU[] myHDUs = fits.read();

	ImageHeader ih = new ImageHeader(myHDUs[0].getHeader());
	header_wave = ih.makeMiniHeader();

                                               // usually just one primary HDU

    }

    private void loadOffsets(File f) throws FileNotFoundException, FitsException {
        if (!f.exists()){
            throw new FileNotFoundException();
        }
	f_offsets = f;
        Fits fits = new Fits(f);
        BasicHDU[] myHDUs = fits.read();

	ImageHeader ih = new ImageHeader(myHDUs[0].getHeader());
	header_offsets = ih.makeMiniHeader();

                                               // usually just one primary HDU
    }

    private void loadMasks(File f) throws FileNotFoundException, FitsException {
        if (!f.exists()){
            throw new FileNotFoundException();
        }
	f_masks = f;
        Fits fits = new Fits(f);
        BasicHDU[] myHDUs = fits.read();

	ImageHeader ih = new ImageHeader(myHDUs[0].getHeader());
	_naxis1 = ih.naxis1;
	header_masks = ih.makeMiniHeader();
                                               // usually just one primary HDU
    }




    /**
    *  Obtain values (coordinates,wavelength, and pixel value 
    *  from the IRS 2d Spectral image
    *  @param p Point object representing the x,y of the point
    *  @return IRSInfoData object filled with the values
    */
    public IRSInfoData getInfoData(Point p) throws IOException {
	_data._ra = Double.NaN;
	_data._dec = Double.NaN;
	_data._wavelength = Double.NaN;


        if (p == null || p.x<1 || p.y<1 || p.x > _naxis1 || p.y > _naxis1) {
            return _data;
        }

        //p.translate(-1, -1);

	ImageWorkSpacePt pt = new ImageWorkSpacePt(p.x, p.y);

	_data._pixelVal = getFluxFromFitsFile(IRS_file, header_IRS_file, pt);
	_data._wavelength = getFluxFromFitsFile(f_wave, header_wave, pt);
        double offset = getFluxFromFitsFile(f_offsets, header_offsets, pt);

        // calculating ra/dec
        double omask = getFluxFromFitsFile(f_masks, header_masks, pt);

        if (omask == 0 || omask == Short.MIN_VALUE) {
            _data._ra = Double.NaN;
            _data._dec = Double.NaN;
        } else {
            double deltaOff = getDeltaOff(omask);
            double ra, dec;
            if (Double.isNaN(deltaOff)) {
                ra = _raSLT;
                dec = _decSLT;
            } else {
                ra = (_raSLT + deltaOff * Math.sin(_paSLT * D2R) / (3600. * Math.cos(_decSLT * D2R)));
                dec = (_decSLT + deltaOff * Math.cos(_paSLT * D2R) / 3600.);
            }
            double alpha = offset;
            _data._ra = (ra + alpha * Math.sin(_paSLT * D2R) / (3600. * Math.cos(dec * D2R)));
            _data._dec = (dec + alpha * Math.cos(_paSLT * D2R) / 3600.);
        }

	if (Double.isNaN(_data._ra))
	{
	    if ((_channel == 0)  && (_projection != null))
	    {
		int x = p.x;
		int y = p.y;
		if ( (x >= 85 && x <= 125 && y >= 65 && y <= 120) ||
		     (x >= 86 && x <= 126 && y >= 3 && y <= 58) ) 
		{
		    try
		    {
		    WorldPt wp = _projection.getWorldCoords(x, y);
		    _data._ra = wp.getLon();
		    _data._dec = wp.getLat();
		    }
		    catch (ProjectionException e)
		    {
			// do nothing - (returns NaN)
		    }
		}
	    }
	}

        return _data;
    }

    private double getDeltaOff(double omask) {
        double deltaOff = Double.NaN;

        if ((_aperture >= 26) && (_aperture <= 28) &&
           (omask >= 2)) {
           deltaOff = 79;
        } else if (_aperture == 29) {
           deltaOff = omask == 1 ? -39.5 : 39.5;
        } else if ((_aperture >= 32) && (_aperture <= 34) && (omask == 1)) {
           deltaOff = -79;
        } else if ((_aperture >= 38) && (_aperture <= 40) && (omask >= 2)) {
           deltaOff = 192;
        } else if (_aperture == 41) {
            deltaOff = omask == 1 ? -96 : 96;
        } else if ((_aperture >= 44) && (_aperture <= 46) && (omask == 1)) {
           deltaOff = -192;
        }
        return deltaOff;
    }


//=========================================================================

/*
*  Everything below here is for stand-alone testing and can be removed	*
*  for the final commit.  Additionally, getFluxFromFitsFile should be	*
*  renamed WebPlotServer.getFluxFromFitsFile() and the line:		*
*  //import edu.caltech.ipac.firefly.server.visualize.WebPlotServer;	*
*  should be uncommented						*
*
*
*  The calling code will look like this:

import edu.caltech.ipac.heritage.server.persistence.UtilDao;

	// call this setup code for each new image
	Header _header = null;

	Vector<String> filenames = UtilDao.getBcdWavsamp(int bcdid);
	   (or)
	Vector<String> filenames = UtilDao.getPbcdWavsamp(int bcdid);

        IRSImageInfo image_info = new IRSImageInfo(_header, filenames) ;


	// call this code for each point
	Point p = new Point (input_x, input_y);
	IRSInfoData info_data = null;
	try
	{
	    info_data = image_info.getInfoData(p);
	}
	catch (IOException ioe)
	{
	    System.out.println("RBH caught IOException: " + ioe);
	}
	// values are info_data._ra, info_data._dec, and info_data._wavelength

*/

//=========================================================================

    private static File getCalDirectory() {
	return(new File("/Imaging/team/booth/irsdata/r3756800/ch0/cal"));
    }

    /*
    method getCalFile_WAVSAMP_WAVE
    creates calibration filename using default naming structure IF CAL_DEFAULT = true
    otherwise gets the filename from the GeneralParams
    */
    private File getCalFile_WAVSAMP_WAVE(String filename) throws FileNotFoundException {
        File WavSampWaveFile = null;

	WavSampWaveFile = new File(filename);

            // todo put in real exception here
            //  check valid file and exists and canread
            if (WavSampWaveFile == null || !WavSampWaveFile.exists() || !WavSampWaveFile.canRead()) {
                System.out.println(WavSampWaveFile.toString() + ": " + 
		    "missing or unreadable cal file");
                WavSampWaveFile = null;
            }
        return WavSampWaveFile;
    }

    /*
    method getCalFile_WAVSAMP_OMASK
    creates calibration filename using default naming structure IF CAL_DEFAULT = true
    otherwise gets the filename from the GeneralParams
    */
    private File getCalFile_WAVSAMP_OMASK(String filename) throws FileNotFoundException {
        File WavSampOmaskFile = null;
	WavSampOmaskFile = new File(filename);
            // todo put in real exception here
            //  check valid file and exists and canread
            if (WavSampOmaskFile == null || !WavSampOmaskFile.exists() || !WavSampOmaskFile.canRead()) {
                System.out.println(WavSampOmaskFile.toString() + ": " + 
		    "missing or unreadable cal file");
                WavSampOmaskFile = null;
            }
        return WavSampOmaskFile;
    }

    /*
    method getCalFile_WAVSAMP_OFFSET
    creates calibration filename using default naming structure IF CAL_DEFAULT = true
    otherwise gets the filename from the GeneralParams
    */
    private File getCalFile_WAVSAMP_OFFSET(String filename) throws FileNotFoundException {
        File WavSampOffsetFile = null;
	WavSampOffsetFile = new File(filename);
            // todo put in real exception here
            //  check valid file and exists and canread
            if (WavSampOffsetFile == null || !WavSampOffsetFile.exists() || !WavSampOffsetFile.canRead()) {
                System.out.println(WavSampOffsetFile.toString() + ": " + 
		    "missing or unreadable cal file");
                WavSampOffsetFile = null;
            }
        return WavSampOffsetFile;
    }

    private void updateHeaderValues(Header _header) {
        _channel = _header.getIntValue(CHANNEL);
        _aperture = _header.getDoubleValue(FOVID);
        _raSLT = _header.getDoubleValue(RA_SLT);
        _decSLT = _header.getDoubleValue(DEC_SLT);
        _paSLT = _header.getDoubleValue(PA_SLT);

    }

    private static  float getFloatVal(float[][] ary, Point p) {
        return ( ary == null || p.y >= ary.length || p.x >= ary[p.y].length ) ?
                Float.NaN : ary[p.y][p.x];
    }


    private static double getFluxFromFitsFile(File f,
                                              MiniFitsHeader miniHeader,
                                              ImageWorkSpacePt ipt) throws IOException {
        RandomAccessFile fitsFile= null;
        double val= 0.0;

        try {
//            PlotServUtils.statsLog(PlotServUtils.FUNCTION +"flux");
            if (f.canRead()) {
                fitsFile= new RandomAccessFile(f, "r");
                if (miniHeader==null) {
                    throw new IOException("Can't read file, MiniFitsHeader is null");
                }
                val= PixelValue.pixelVal(fitsFile,(int)ipt.getX(),(int)ipt.getY(), miniHeader);
            }
            else {
                throw new IOException("Can't read file or it does not exist");

            }
        } catch (PixelValueException e) {
            val= Double.NaN;
        } finally {
	    try
	    {
		fitsFile.close();
	    }
	    catch (IOException ioe)
	    {
	    }
        }
        return val;
    }

    private static void usage()
    {
	System.out.println("usage: java IRSImageInfo <x> <y>");
	System.exit(1);

    }

    public static void main(String[] args) {

        if (args.length != 2)
        {
            usage();
        }

        //String filename = "/Imaging/team/booth/irsdata/r3756800/ch0/pbcd/SPITZER_S0_3756800_0005_10_E5510105_coa2d.fits";
        String filename = "/home1/booth/scr/r18945280/ch0/pbcd/SPITZER_S0_18945280_0002_9_E84836_coa2d.fits";
        int input_x = Integer.parseInt(args[0]);
        int input_y = Integer.parseInt(args[1]);

        int channel = -1;
        Header _header = null;
        try {
            Fits fits = new Fits(filename);
            BasicHDU[] myHDUs = fits.read();
            // usually just one primary HDU
            _header = myHDUs[0].getHeader();
            _pixelValues = (float[][]) myHDUs[0].getData().getData();

            channel = _header.getIntValue(CHANNEL);
        } catch (FitsException e) {
            e.printStackTrace();
        }

        Vector<String> filenames = new Vector<String>();
        //String cDir = "/Imaging/team/booth/irsdata/r3756800/ch0/cal";
        String cDir = "/home1/booth/scr/r18945280/ch0/cal";
        String WavSampWaveFileName = cDir+ "/b" + channel +
                wavsamp_wave_string;
        filenames.add(WavSampWaveFileName);

        String WavSampOmaskFileName = cDir +"/b" + channel +
                wavsamp_omask_string;
        filenames.add(WavSampOmaskFileName);

        String WavSampOffsetFileName = cDir +"/b" + channel +
                wavsamp_offset_string;
        filenames.add(WavSampOffsetFileName);

	File IRS_file = new File(filename);
        IRSImageInfo image_info = new IRSImageInfo(_header, filenames, IRS_file) ;



        Point p = new Point (input_x, input_y);
        IRSInfoData info_data = null;
        try
        {
            info_data = image_info.getInfoData(p);
        }
        catch (IOException ioe)
        {
            System.out.println("RBH caught IOException: " + ioe);
            System.exit(1);
        }

        //p.translate(-1, -1);
        double pixelVal = getFloatVal(_pixelValues, p);

        System.out.println("ra = " + info_data._ra +
                " dec = " + info_data._dec +
                " wavelength = " + info_data._wavelength +
                " pixel value = " + pixelVal);
    }
}
