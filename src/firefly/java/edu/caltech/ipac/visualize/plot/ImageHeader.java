/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.astro.conv.CoordConv;
import edu.caltech.ipac.firefly.visualize.ClientFitsHeader;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.SUTDebug;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import edu.caltech.ipac.visualize.plot.projection.ProjectionParams;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

import java.io.Serializable;


public class ImageHeader implements Serializable
{


    // IF you modify this class please make sure you modify ProjectionParams if necessary

    public static final String ORIGIN=   "ORIGIN";
    public static final String EXPTIME=  "EXPTIME";
    public static final String IMAGEZPT= "IMAGEZPT";
    public static final String AIRMASS=  "AIRMASS";
    public static final String EXTINCT=  "EXTINCT";

    public static final String PALOMAR_ID=  "Palomar Transient Factory";

    public final int EQ = 0;
    public final int EC = 1;
    public final int GA = 2;
    public final int SGAL = 3;
    public int bitpix, naxis, naxis1, naxis2, naxis3;
    public double crpix1, crpix2, crval1, crval2, cdelt1, cdelt2, crota2;
    public double crota1;
    public double file_equinox;
    public String ctype1;
    public String ctype2;
    public String radecsys;
    public double datamax, datamin;
    public double bscale, bzero;
    public String bunit;
    public String origin;
    public double airmass;
    public double extinct;
    public double imagezpt;
    public double exptime;
    public double blank_value;
    public int maptype;
    public double cd1_1, cd1_2, cd2_1, cd2_2;
    public double dc1_1, dc1_2, dc2_1, dc2_2;
    public boolean using_cd = false;
    public long data_offset;
    public int plane_number;

    /* the following are for PLATE projection */
    public double plate_ra, plate_dec;
    public double x_pixel_offset, y_pixel_offset;
    public double x_pixel_size, y_pixel_size;
    public double plt_scale;
    public double ppo_coeff[], amd_x_coeff[], amd_y_coeff[];

    /* the following are for SIRTF distortion corrections to the */
    /* GNOMONIC projection (ctype1 ending in -SIP)*/

	public double a_order, ap_order, b_order, bp_order;
    public double a[][] = new double[ProjectionParams.MAX_SIP_LENGTH][ProjectionParams.MAX_SIP_LENGTH];
    public double ap[][] = new double[ProjectionParams.MAX_SIP_LENGTH][ProjectionParams.MAX_SIP_LENGTH];
    public double b[][] = new double[ProjectionParams.MAX_SIP_LENGTH][ProjectionParams.MAX_SIP_LENGTH];
    public double bp[][] = new double[ProjectionParams.MAX_SIP_LENGTH][ProjectionParams.MAX_SIP_LENGTH];
    public boolean map_distortion = false;
    public String keyword;


    public ImageHeader()
    {
    }

    public ImageHeader(Header header) throws FitsException
    {
	this(header, 0L, 0);
    }

    public ImageHeader(Header header, long HDU_offset, int _plane_number) 
	throws FitsException
    {
	int i, j;
	boolean got_cd1_1, got_cd1_2, got_cd2_1, got_cd2_2;
	boolean got_pc1_1, got_pc1_2, got_pc2_1, got_pc2_2;
	double pc1_1 = Double.NaN;
	double pc1_2 = Double.NaN;
	double pc2_1 = Double.NaN;
	double pc2_2 = Double.NaN;
	boolean axes_reversed = false;
	double twist;
	double rah,ram,ras, dsign,decd,decm,decs;
	double dec_deg,ra_hours;
	String ctype1_trim = null;

	long header_size = header.getOriginalSize();
	data_offset = HDU_offset + header_size;
	plane_number = _plane_number;

	bitpix = header.getIntValue("BITPIX");
	naxis = header.getIntValue("NAXIS");
	naxis1 = header.getIntValue("NAXIS1");
	naxis2 = header.getIntValue("NAXIS2");
	if (naxis > 2)
	    naxis3 = header.getIntValue("NAXIS3");
	else
	    naxis3 = 1;
	crpix1 = header.getDoubleValue("CRPIX1", Double.NaN);
	crpix2 = header.getDoubleValue("CRPIX2", Double.NaN);
	crval1 = header.getDoubleValue("CRVAL1", Double.NaN);
	crval2 = header.getDoubleValue("CRVAL2", Double.NaN);
	cdelt1 = header.getDoubleValue("CDELT1");
	cdelt2 = header.getDoubleValue("CDELT2");
	crota1 = header.getDoubleValue("CROTA1");
	crota2 = header.getDoubleValue("CROTA2");
	if (header.containsKey("CTYPE1"))
	{
	    ctype1 = header.getStringValue("CTYPE1") + "        ";
	    ctype2 = header.getStringValue("CTYPE2") + "        ";
	    ctype1_trim = ctype1.trim();
	    String ctype1_tail = ctype1.substring(4, 8);
	    if (ctype1_trim.indexOf("-TAN") >= 0)
		maptype = Projection.GNOMONIC;
	    else if (ctype1_trim.indexOf("-SIN") >= 0)
		maptype = Projection.ORTHOGRAPHIC;
	    else if (ctype1_trim.endsWith("-NCP"))
		maptype = Projection.NCP;
	    else if (ctype1_trim.endsWith("-ARC"))
		maptype = Projection.ARC;
	    else if (ctype1_trim.endsWith("-AIT"))
		maptype = Projection.AITOFF;
	    else if (ctype1_trim.endsWith("-ATF"))
		maptype = Projection.AITOFF;
	    else if (ctype1_trim.endsWith("-CAR"))
		maptype = Projection.CAR;
	    else if (ctype1_trim.endsWith("-CEA"))
		maptype = Projection.CEA;
	    else if (ctype1_trim.endsWith("-SFL"))
		maptype = Projection.SFL;
	    else if (ctype1_trim.endsWith("-GLS"))
		maptype = Projection.SFL;
	    else if (ctype1_trim.endsWith("----"))
		maptype = Projection.LINEAR;
	    else if (ctype1_tail.equals("    "))
		maptype = Projection.LINEAR;
	    else 
		maptype = Projection.UNRECOGNIZED;
	    
	    if ((ctype1_trim.startsWith("DEC")) ||
		(ctype1_trim.startsWith("MM")) ||
		(ctype1_trim.startsWith("GLAT")) ||
		(ctype1_trim.startsWith("LAT")) ||
		(ctype1_trim.startsWith("ELAT")))
	    {
		/* flag that axes are reversed */
		axes_reversed = true;
	    }
	}
	else
	    maptype = Projection.UNSPECIFIED;

	if (header.containsKey("DSKYGRID"))
	    maptype = Projection.ORTHOGRAPHIC;

	if (maptype == Projection.CAR)
	{
	/* wcs projection routines require crpix1 in -180 to 180 hemisphere */
	    double halfway = Math.abs(180.0 / cdelt1);
	    if (crpix1 > halfway)
		crpix1 -= 2 * halfway;
	    if (crpix1 < -halfway)
		crpix1 += 2 * halfway;
	}

	got_cd1_1 = header.containsKey("CD1_1");
	if (got_cd1_1)
	{
	    cd1_1 = header.getDoubleValue("CD1_1");
	}
	else
	{
	    got_cd1_1 = header.containsKey("CD001001");
	    if (got_cd1_1)
	    {
		cd1_1 = header.getDoubleValue("CD001001");
	    }
	}
	got_pc1_1 = header.containsKey("PC1_1");
	if (got_pc1_1)
	{
	    pc1_1 = header.getDoubleValue("PC1_1");
	}

	got_cd1_2 = header.containsKey("CD1_2");
	if (got_cd1_2)
	{
	    cd1_2 = header.getDoubleValue("CD1_2");
	}
	else
	{
	    got_cd1_2 = header.containsKey("CD001002");
	    if (got_cd1_2)
	    {
		cd1_2 = header.getDoubleValue("CD001002");
	    }
	}
	got_pc1_2 = header.containsKey("PC1_2");
	if (got_pc1_2)
	{
	    pc1_2 = header.getDoubleValue("PC1_2");
	}

	got_cd2_1 = header.containsKey("CD2_1");
	if (got_cd2_1)
	{
	    cd2_1 = header.getDoubleValue("CD2_1");
	}
	else
	{
	    got_cd2_1 = header.containsKey("CD002001");
	    if (got_cd2_1)
	    {
		cd2_1 = header.getDoubleValue("CD002001");
	    }
	}
	got_pc2_1 = header.containsKey("PC2_1");
	if (got_pc2_1)
	{
	    pc2_1 = header.getDoubleValue("PC2_1");
	}

	got_cd2_2 = header.containsKey("CD2_2");
	if (got_cd2_2)
	{
	    cd2_2 = header.getDoubleValue("CD2_2");
	}
	else
	{
	    got_cd2_2 = header.containsKey("CD002002");
	    if (got_cd2_2)
	    {
		cd2_2 = header.getDoubleValue("CD002002");
	    }
	}
	got_pc2_2 = header.containsKey("PC2_2");
	if (got_pc2_2)
	{
	    pc2_2 = header.getDoubleValue("PC2_2");
	}

	if ((!got_cd1_1 ) &&
	    (!got_cd1_2 ) &&
	    (!got_cd2_1 ) &&
	    (!got_cd2_2 )) 
	{
	    /* no CD matrix values in header - look for PC matrix values */
	    if (got_pc1_1 ) 
	    {
		cd1_1 = cdelt1 * pc1_1;
		got_cd1_1 = true;
	    }
	    if (got_pc1_2 ) 
	    {
		cd1_2 = cdelt1 * pc1_2;
		got_cd1_2 = true;
	    }
	    if (got_pc2_1 ) 
	    {
		cd2_1 = cdelt2 * pc2_1;
		got_cd2_1 = true;
	    }
	    if (got_pc2_2 ) 
	    {
		cd2_2 = cdelt2 * pc2_2;
		got_cd2_2 = true;
	    }
	}


	datamax = header.getDoubleValue("DATAMAX", Double.NaN);
	datamin = header.getDoubleValue("DATAMIN", Double.NaN);
	bscale = header.getDoubleValue("BSCALE", 1.0);
	bzero = header.getDoubleValue("BZERO", 0.0);
	bunit = header.getStringValue("BUNIT");
	if (bunit == null)
	    bunit = "DN";



	origin = header.getStringValue(ORIGIN);
	if (origin == null)
	{
	    origin = "";
	}
	if (origin.startsWith(PALOMAR_ID))
	{
	    exptime = header.getDoubleValue(EXPTIME, 0.0);
	    imagezpt = header.getDoubleValue(IMAGEZPT, 0.0);
	    airmass = header.getDoubleValue(AIRMASS, 0.0);
	    extinct = header.getDoubleValue(EXTINCT, 0.0);
	}

	file_equinox = header.getDoubleValue("EQUINOX", 0.0);
	if (file_equinox == 0.0)
	    file_equinox = header.getDoubleValue("EPOCH", 2000.0);
	radecsys = header.getStringValue("RADECSYS");
	if (radecsys == null)
	{
	    radecsys = header.getStringValue("RADESYS");
	}



	blank_value = header.getDoubleValue("BLANK", Double.NaN);
	if (SUTDebug.isDebug())
	    System.out.println("blank_value = " + blank_value);
	
	String telescope = header.getStringValue("TELESCOP");
	if ((telescope != null) && (telescope.startsWith("ISO")))
	{
	    /* ISO images have bad CD matrix - try not to use it */
	    if ((cdelt1 != 0) &&
		(cdelt2 != 0))
	    {
		//System.out.println("ISO image - using CDELTn");
		got_cd1_1 = false;  // prevent use of CD matrix
	    }
	}

	if ((!Double.isNaN(crval2)) &&
	    (!Double.isNaN(crval1)) &&
	    (!Double.isNaN(crpix1)) &&
	    (!Double.isNaN(crpix2)) &&
	    (maptype != Projection.UNRECOGNIZED) &&
		((got_cd1_1 ) ||
		(got_cd1_2 ) ||
		(got_cd2_1 ) ||
		(got_cd2_2 )) )
	{
	    if (axes_reversed)
	    {
		double temp;
		temp = crval1;
		crval1 = crval2;
		crval2 = temp;

		temp = cd2_2;
		cd2_2 = cd1_2;
		cd1_2 = cd1_1;
		cd1_1 = cd2_1;
		cd2_1 = temp;
	    }
	    /* save values for Greisen's formulas */
	    using_cd = true;
	    /* invert matrix */
	    double determinant = cd1_1 * cd2_2 - cd1_2 * cd2_1;
	    dc1_1 = cd2_2 / determinant;
	    dc1_2 = - cd1_2 / determinant;
	    dc2_1 = - cd2_1 / determinant;
	    dc2_2 = cd1_1 / determinant;

	    twist = Math.atan2(-cd1_2, cd2_2);
	    crota2 = twist * Projection.rtd;

	    if (false)  /* patch out, now that CD matrix is handeld */
	    {
	    /* average the two methods  to lessen the effect of skew */
	    twist = (Math.atan2(-cd2_1, -cd1_1) + twist) / 2.0;
	    if (Math.abs(cd1_1) > Math.abs(cd1_2))
	    {
		cdelt1 = cd1_1 / Math.cos(twist);
		cdelt2 = cd2_2 / Math.cos(twist);
	    }
	    else
	    {
		cdelt1 = cd2_1 / Math.sin(twist);
		cdelt2 = - cd1_2 / Math.sin(twist);
	    }
	    }
	}
	else
	{
	    if (axes_reversed)
	    {
		double temp;
		temp = crval1;
		crval1 = crval2;
		crval2 = temp;

		temp = cdelt1;
		cdelt1 = cdelt2;
		cdelt2 = temp;
		/* dont know what to do with twist */
		/* will have to wait until I have a sample image */
	    }
	    
	}

    /* now do SIRTF distortion corrections */
    if ((ctype1_trim != null) && (ctype1_trim.endsWith("-SIP")))
    {
	map_distortion = true;

	a_order = header.getIntValue("A_ORDER");
	if (a_order>= 0)
	{
		int len= (int)Math.min(a_order+1, ProjectionParams.MAX_SIP_LENGTH);
	    for (i = 0; i < len; i++)
	    {
		for (j = 0; j < len; j++)
		{
		    a[i][j] = 0.0;
		    if (i + j <= a_order)
		    {
			keyword = "A_" + i + "_" + j;
			a[i][j] = header.getDoubleValue(keyword, 0.0);
			/*
			System.out.println("a[" + i + "][" + j + "] = " + a[i][j]);
			*/
		    }
		}
	    }
	}

	b_order = header.getIntValue("B_ORDER");
	if (b_order>= 0)
	{
		int len= (int)Math.min(b_order+1, ProjectionParams.MAX_SIP_LENGTH);
	    for (i = 0; i < len; i++)
	    {
		for (j = 0; j < len; j++)
		{
		    b[i][j] = 0.0;
		    if (i + j <= b_order)
		    {
			keyword = "B_" + i + "_" + j;
			b[i][j] = header.getDoubleValue(keyword, 0.0);
			/*
			System.out.println("b[" + i + "][" + j + "] = " + b[i][j]);
			*/
		    }
		}
	    }
	}
	ap_order = header.getIntValue("AP_ORDER");
	if (ap_order>= 0)
	{
		int len= (int)Math.min(ap_order+1, ProjectionParams.MAX_SIP_LENGTH);
	    for (i = 0; i < len; i++)
	    {
		for (j = 0; j < len; j++)
		{
		    ap[i][j] = 0.0;
		    if (i + j <= ap_order)
		    {
			keyword = "AP_" + i + "_" + j;
			ap[i][j] = header.getDoubleValue(keyword, 0.0);
			/*
			System.out.println("ap[" + i + "][" + j + "] = " + ap[i][j]);
			*/
		    }
		}
	    }
	}
	bp_order = header.getIntValue("BP_ORDER");
	if (bp_order>= 0)
	{
		int len= (int)Math.min(bp_order+1, ProjectionParams.MAX_SIP_LENGTH);
	    for (i = 0; i < len; i++)
	    {
		for (j = 0; j < len; j++)
		{
		    bp[i][j] = 0.0;
		    if (i + j <= bp_order)
		    {
			keyword = "BP_" + i + "_" + j;
			bp[i][j] = header.getDoubleValue(keyword, 0.0);
			/*
			System.out.println("bp[" + i + "][" + j + "] = " + bp[i][j]);
			*/
		    }
		}
	    }
	}

    }
    if (using_cd) 
    {
	/* need an approximation of cdelt1 and cdelt2 */
	CoordinateSys in_coordinate_sys = CoordinateSys.makeCoordinateSys(
	    this.getJsys(), this.file_equinox);
	Projection proj = createProjection(in_coordinate_sys);
	try
	{
	WorldPt proj_center = proj.getWorldCoords(crpix1 - 1, crpix2 - 1);
	WorldPt one_to_right = proj.getWorldCoords(crpix1, crpix2 - 1);
	WorldPt one_up = proj.getWorldCoords(crpix1 - 1, crpix2);

	cdelt1 = - VisUtil.computeDistance( proj_center, one_to_right);
	cdelt2 = VisUtil.computeDistance( proj_center, one_up);
	if (SUTDebug.isDebug())
	{
	System.out.println("CENTER lon = " + proj_center.getLon() +
	    "  lat = " + proj_center.getLat() +
	    "  one_right lon = " + one_to_right.getLon() +
	    "  lat = " + one_to_right.getLat() + 
	    "  cdelt1 = " + cdelt1 +
	    "  one_up lon = " + one_up.getLon() +
	    "  lat = " + one_up.getLat() + 
	    "  cdelt2 = " + cdelt2);
	}
	}
	catch (ProjectionException pe)
	{
	    cdelt1 = 0;
	    cdelt2 = 0;
	}
    }

	/* now do Digital Sky Survey plate solution coefficients */
	if  (header.containsKey("PLTRAH"))
	{
	    if (SUTDebug.isDebug())
		System.out.println("ITS a PLATE projection");
	    maptype = Projection.PLATE;
	    rah = header.getDoubleValue("PLTRAH");
	    ram = header.getDoubleValue("PLTRAM");
	    ras = header.getDoubleValue("PLTRAS");
	    ra_hours = rah + (ram / 60.0) + (ras / 3600.0);
	    plate_ra = ra_hours * 15.0 * Projection.dtr;
	    String decsign = header.getStringValue("PLTDECSN");
	    if (decsign.charAt(0) == '-')
		dsign = -1.;
	    else
		dsign = 1.;
	    decd = header.getDoubleValue("PLTDECD");
	    decm = header.getDoubleValue("PLTDECM");
	    decs = header.getDoubleValue("PLTDECS");
	    dec_deg = dsign * (decd+(decm/60.0)+(decs/3600.0));
	    plate_dec = dec_deg * Projection.dtr;
	/*
	    fprintf(debug_file,
	"WCSINIT Plate center RA=%2.0f:%2.0f:%5.3f, Dec=%3.0f:%2.0f:%5.3f\n",
	    rah,ram,ras,dsign*decd,decm,decs);
	*/
	    if (SUTDebug.isDebug())
		System.out.println(" WCSINIT Plate center RA= " + rah + " " +
		ram + " " + ras + "  DEC = " + dsign*decd + " " + decm + " " +
		decs);

	    x_pixel_offset = header.getDoubleValue( "CNPIX1");
	    y_pixel_offset = header.getDoubleValue( "CNPIX2");
	    plt_scale = header.getDoubleValue( "PLTSCALE");
	    x_pixel_size = header.getDoubleValue( "XPIXELSZ");
	    y_pixel_size = header.getDoubleValue( "YPIXELSZ");
	    ppo_coeff = new double[6];
	    ppo_coeff[0] = header.getDoubleValue( "PPO1");
	    ppo_coeff[1] = header.getDoubleValue( "PPO2");
	    ppo_coeff[2] = header.getDoubleValue( "PPO3");
	    ppo_coeff[3] = header.getDoubleValue( "PPO4");
	    ppo_coeff[4] = header.getDoubleValue( "PPO5");
	    ppo_coeff[5] = header.getDoubleValue( "PPO6");
	    amd_x_coeff = new double[20];
	    amd_x_coeff[0] = header.getDoubleValue( "AMDX1");
	    amd_x_coeff[1] = header.getDoubleValue( "AMDX2");
	    amd_x_coeff[2] = header.getDoubleValue( "AMDX3");
	    amd_x_coeff[3] = header.getDoubleValue( "AMDX4");
	    amd_x_coeff[4] = header.getDoubleValue( "AMDX5");
	    amd_x_coeff[5] = header.getDoubleValue( "AMDX6");
	    amd_x_coeff[6] = header.getDoubleValue( "AMDX7");
	    amd_x_coeff[7] = header.getDoubleValue( "AMDX8");
	    amd_x_coeff[8] = header.getDoubleValue( "AMDX9");
	    amd_x_coeff[9] = header.getDoubleValue( "AMDX10");
	    amd_x_coeff[10] = header.getDoubleValue( "AMDX11");
	    amd_x_coeff[11] = header.getDoubleValue( "AMDX12");
	    amd_x_coeff[12] = header.getDoubleValue( "AMDX13");
	    amd_x_coeff[13] = header.getDoubleValue( "AMDX14");
	    amd_x_coeff[14] = header.getDoubleValue( "AMDX15");
	    amd_x_coeff[15] = header.getDoubleValue( "AMDX16");
	    amd_x_coeff[16] = header.getDoubleValue( "AMDX17");
	    amd_x_coeff[17] = header.getDoubleValue( "AMDX18");
	    amd_x_coeff[18] = header.getDoubleValue( "AMDX19");
	    amd_x_coeff[19] = header.getDoubleValue( "AMDX20");
	    amd_y_coeff = new double[20];
	    amd_y_coeff[0] = header.getDoubleValue( "AMDY1");
	    amd_y_coeff[1] = header.getDoubleValue( "AMDY2");
	    amd_y_coeff[2] = header.getDoubleValue( "AMDY3");
	    amd_y_coeff[3] = header.getDoubleValue( "AMDY4");
	    amd_y_coeff[4] = header.getDoubleValue( "AMDY5");
	    amd_y_coeff[5] = header.getDoubleValue( "AMDY6");
	    amd_y_coeff[6] = header.getDoubleValue( "AMDY7");
	    amd_y_coeff[7] = header.getDoubleValue( "AMDY8");
	    amd_y_coeff[8] = header.getDoubleValue( "AMDY9");
	    amd_y_coeff[9] = header.getDoubleValue( "AMDY10");
	    amd_y_coeff[10] = header.getDoubleValue( "AMDY11");
	    amd_y_coeff[11] = header.getDoubleValue( "AMDY12");
	    amd_y_coeff[12] = header.getDoubleValue( "AMDY13");
	    amd_y_coeff[13] = header.getDoubleValue( "AMDY14");
	    amd_y_coeff[14] = header.getDoubleValue( "AMDY15");
	    amd_y_coeff[15] = header.getDoubleValue( "AMDY16");
	    amd_y_coeff[16] = header.getDoubleValue( "AMDY17");
	    amd_y_coeff[17] = header.getDoubleValue( "AMDY18");
	    amd_y_coeff[18] = header.getDoubleValue( "AMDY19");
	    amd_y_coeff[19] = header.getDoubleValue( "AMDY20");
	    
	    crpix1 = 0.5 - x_pixel_offset;
	    crpix2 = 0.5 - y_pixel_offset;

	    if (cdelt1 == 0.0)
	    {
		cdelt1 = - plt_scale * x_pixel_size / 1000 / 3600;
		cdelt2 = plt_scale * y_pixel_size / 1000 / 3600;
	    }
	}

    }

    public String getProjectionName()
    {
        return Projection.getProjectionName(maptype);
    }


    public int getCoordSys() 
    { 
	int retval = -1;

	if (ctype1 != null)
	{
	    String s = ctype1.substring(0, 2);
	    if (s.equals("RA"))
		retval =  EQ;
	    else if (s.equals("DE"))
		retval =  EQ;
	    else if (s.equals("LL"))
		retval =  EQ;
	    else if (s.equals("GL"))
		retval =  GA;
	    else if (s.equals("LO"))
		retval =  GA;
	    else if (s.equals("EL"))
		retval =  EC;
	}
	if (maptype == Projection.PLATE)
	    retval =  EQ;
	return retval;
    }

    public double getEquinox() 
    {
	return file_equinox;
    }

    public int getJsys() 
    {
	int jsys;

	switch (getCoordSys())
	{
	    case EQ:
		if ((radecsys != null) &&
		    (radecsys.substring(0,3).equals("FK4")))
		    jsys = CoordConv.EQUATORIAL_B;
		else if ((radecsys != null) &&
		    ((radecsys.substring(0,3).equals("FK5")) ||
		    (radecsys.substring(0,4).equals("ICRS"))))
		    jsys = CoordConv.EQUATORIAL_J;
		else if (file_equinox < 2000.0)
		    jsys = CoordConv.EQUATORIAL_B;
		else
		    jsys = CoordConv.EQUATORIAL_J;
		break;
	    case EC:
		if ((radecsys != null) &&
		    (radecsys.substring(0,3).equals("FK4")))
		    jsys = CoordConv.ECLIPTIC_B;
		else if ((radecsys != null) &&
		    (radecsys.substring(0,3).equals("FK5")))
		    jsys = CoordConv.ECLIPTIC_J;
		else if (file_equinox < 2000.0)
		    jsys = CoordConv.ECLIPTIC_B;
		else
		    jsys = CoordConv.ECLIPTIC_J;
		break;
	    case GA:
		jsys = CoordConv.GALACTIC;
		break;
	    case SGAL:
		jsys = CoordConv.SUPERGALACTIC;
		break;
	    default:
		jsys = -1;
	}
	return jsys;
    }

    public Projection createProjection(CoordinateSys csys) {
        ProjectionParams params= createProjectionParams(this);
        return new Projection(params,csys);
    }

    public static ProjectionParams createProjectionParams(ImageHeader hdr) {
        ProjectionParams params= new ProjectionParams();

        params.bitpix= hdr.bitpix;
        params.naxis = hdr.naxis;
        params.naxis1= hdr.naxis1;
        params.naxis2= hdr.naxis2;
        params.naxis3= hdr.naxis3;
        params.crpix1= hdr.crpix1;
        params.crpix2= hdr.crpix2;
        params.crval1= hdr.crval1;
        params.crval2= hdr.crval2;
        params.cdelt1= hdr.cdelt1;
        params.cdelt2= hdr.cdelt2;
        params.crota2= hdr.crota2;
        params.crota1= hdr.crota1;
        params.file_equinox= hdr.file_equinox;
        params.ctype1= hdr.ctype1;
        params.ctype2= hdr.ctype2;
        params.radecsys= hdr.radecsys;
        params.datamax= hdr.datamax;
        params.datamin= hdr.datamin;
        //params.bscale= hdr.bscale;
        //params.bzero= hdr.bzero;
        //params.bunit= hdr.bunit;
        //params.blank_value= hdr.blank_value;
        params.maptype= hdr.maptype;
        params.cd1_1= hdr.cd1_1;
        params.cd1_2= hdr.cd1_2;
        params.cd2_1= hdr.cd2_1;
        params.cd2_2= hdr.cd2_2;
        params.dc1_1= hdr.dc1_1;
        params.dc1_2= hdr.dc1_2;
        params.dc2_1= hdr.dc2_1;
        params.dc2_2= hdr.dc2_2;
        params.using_cd= hdr.using_cd;

        params.plate_ra= hdr.plate_ra;
        params.plate_dec= hdr.plate_dec;
        params.x_pixel_offset= hdr.x_pixel_offset;
        params.y_pixel_offset= hdr.y_pixel_offset;
        params.x_pixel_size= hdr.x_pixel_size;
        params.y_pixel_size= hdr.y_pixel_size;
        params.plt_scale= hdr.plt_scale;
        params.ppo_coeff= hdr.ppo_coeff;
        params.amd_x_coeff= hdr.amd_x_coeff;
        params.amd_y_coeff= hdr.amd_y_coeff;

        params.a_order= hdr.a_order;
        params.ap_order= hdr.ap_order;
        params.b_order= hdr.b_order;
        params.bp_order= hdr.bp_order;
        params.a= hdr.a;
        params.ap= hdr.ap;
        params.b= hdr.b;
        params.bp= hdr.bp;
        params.map_distortion= hdr.map_distortion;
        params.keyword= hdr.keyword;

        return params;

    }

    public ClientFitsHeader makeMiniHeader() {
        ClientFitsHeader miniHeader= new ClientFitsHeader(plane_number,bitpix,
                                                        naxis,naxis1,naxis2,naxis3,
                                                        cdelt2,bscale,bzero,
                                                        blank_value,data_offset);
        if (origin.startsWith(PALOMAR_ID))
        {
            miniHeader.setHeader(ORIGIN,origin);
            miniHeader.setHeader(EXPTIME,exptime);
            miniHeader.setHeader(IMAGEZPT,imagezpt);
            miniHeader.setHeader(AIRMASS,airmass);
            miniHeader.setHeader(EXTINCT,extinct);
        }
        return miniHeader;
    }

	/*public static String[] parameterNames(){

        String[] names = {
		"bitpix",
		"file_equinox",
		"ctype1 ",
		"ctype2 ",
		"radecsys",
		"datamax",
		"datamin",
		"maptype",
		"cd1_1 ",
		"cd1_2 ",
		"cd2_1 ",
		"cd2_2 ",
		"dc1_1 ",
		"dc1_2 ",
		"dc2_1 ",
		"dc2_2 ",
		"using_cd" ,
		"plate_ra" ,
		"plate_dec",
		"x_pixel_offset",
		"y_pixel_offset",
		"x_pixel_size",
		"y_pixel_size" ,
		"plt_scale",
		"ppo_coeff",
		"amd_x_coeff" ,
		"amd_y_coeff" ,
		"a_order",
		"ap_order",
		"b_order" ,
		"bp_order",
		"a" ,
		"ap",
		"b",
		"bp",
		"map_distortion" ,
		"keyword"
		};
		return names;
	}*/
    public String toString()
    {
	StringBuffer sb = new StringBuffer();
	sb.append(
	    "\n  bitpix = " + bitpix + " naxis = " + naxis + 
	    " naxis1 = " + naxis1 + " naxis2 = " + naxis2 + 
	    " naxis3 = " + naxis3);
	sb.append(
	    "\n  crpix1 = " + crpix1 + " crpix2 = " + crpix2 + 
	    "\n  crval1 = " + crval1 + " crval2 = " + crval2 + 
	    "\n  cdelt1 = " + cdelt1 + " cdelt2 = " + cdelt2 + 
	    " crota2 = " + crota2);
	if (using_cd)
	{
	    sb.append(
		"\n  cd1_1 = " + cd1_1 + " cd1_2 = " + cd1_2 + " cd2_1 = " +
		cd2_1 + " cd2_2 = " + cd2_2);
	    sb.append(
		"\n  dc1_1 = " + dc1_1 + " dc1_2 = " + dc1_2 + " dc2_1 = " +
		dc2_1 + " dc2_2 = " + dc2_2);
	}
	sb.append(
	    "\n  file_equinox = " + file_equinox + "  ctype1 = " + ctype1 +
	    " ctype2 = " + ctype2);
	sb.append("\n  maptype = " + Projection.getProjectionName(maptype));

	//LZ add the rest fields

/*	sb.append("\n  plate_ra = "+ plate_ra);
	sb.append("\n  plate_dec = "+ plate_dec);
	sb.append("\n x_pixel_offset = " +x_pixel_offset);
	sb.append("\n y_pixel_offset = "+ y_pixel_offset);
	sb.append("\n x_pixel_size = "+ x_pixel_size);
	sb.append("\n y_pixel_size = "+y_pixel_size);
	sb.append("\n plt_scale = "+ plt_scale);*/

	return sb.toString();
    }

	//This method is added to for unit testing in javascript

	/**
	 * This is no imageHeader in js.  This method produces string, a string translates to json
	 * string. Thus, js can take it as json header.
	 * TODO
	 * Add method to produce an ImageHeader object form array or string.
	 * Add method to handle the array data
	 * But the array data does not work.
	 *
     */
	public String imageHeaderToString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(
				"\n  bitpix = " + bitpix + " naxis = " + naxis +
						" naxis1 = " + naxis1 + " naxis2 = " + naxis2 +
						" naxis3 = " + naxis3);
		sb.append(
				"\n  crpix1 = " + crpix1 + " crpix2 = " + crpix2 +
						"\n  crval1 = " + crval1 + " crval2 = " + crval2 +
						"\n  cdelt1 = " + cdelt1 + " cdelt2 = " + cdelt2 +
						" crota2 = " + crota2);
		//if (using_cd)
		//{
			sb.append(
					"\n  cd1_1 = " + cd1_1 + " cd1_2 = " + cd1_2 + " cd2_1 = " +
							cd2_1 + " cd2_2 = " + cd2_2);
			sb.append(
					"\n  dc1_1 = " + dc1_1 + " dc1_2 = " + dc1_2 + " dc2_1 = " +
							dc2_1 + " dc2_2 = " + dc2_2);
		//}
		sb.append(
				"\n  file_equinox = " + file_equinox + "  ctype1 = " + ctype1 +
						" ctype2 = " + ctype2);
		sb.append("\n  maptype = " + maptype);
		sb.append(
				"\n  using_cd = " + using_cd);

		sb.append(
				"\n x_pixel_size= " + x_pixel_size + " x_pixel_size = " + y_pixel_size +
				 "\n plate_ra = " + plate_ra + " plate_dec = " + plate_ra);

		return sb.toString();
	}
   //LZ add this to pass the array data so it can be included the json string
	public double[] getCoeffData(String key){
		if (key.equalsIgnoreCase("amd_x_coeff")){
			return  amd_x_coeff;
		}
		if (key.equalsIgnoreCase("amd_y_coeff")){
			return  amd_y_coeff;
		}

		if (key.equalsIgnoreCase("ppo_coeff")){
			return  ppo_coeff;
		}
		return null;
	}

    // main is for testing only
//    public static void main(String[] args)
//    {
//	Fits myFits = null;
//	BasicHDU[] HDUs = null;
//	ImageHeader rbhtest = null;
//
//	if (args.length != 1)
//	{
//	    System.out.println("usage:  java ImageHeader <filename>");
//	    System.exit(1);
//	}
//
//	try
//	{
//	    myFits = new Fits(args[0]);
//	    HDUs = myFits.read();
//
//	    if (HDUs == null)
//	    {
//		// Error: file doesn't seem to have any HDUs!
//		throw new FitsException("Bad format in FITS file");
//	    }
//	    Header header = HDUs[0].getHeader();
//	    rbhtest = new ImageHeader(header);
//	}
//	catch (FitsException e)
//	{
//	    //System.out.println("got FitsException e= " + e);
//	    System.out.println("got FitsException: " + e.getMessage());
//	    // e.printStackTrace();
//	    System.exit(1);
//	}
//
//	System.out.println("bitpix = " + rbhtest.bitpix);
//	System.out.println("naxis = " + rbhtest.naxis);
//	System.out.println("naxis1 = " + rbhtest.naxis1);
//	System.out.println("naxis2 = " + rbhtest.naxis2);
//	if (rbhtest.naxis > 2)
//	{
//	    System.out.println("naxis3 = " + rbhtest.naxis3);
//	}
//
//
//	System.out.println("getProjectionName() returns " +
//	    rbhtest.getProjectionName());
//	System.out.println("getCoordSys() returns " + rbhtest.getCoordSys());
//	System.out.println("file_equinox = " + rbhtest.file_equinox);
//	System.out.println("getJsys() returns " + rbhtest.getJsys());
//
//    }
}



