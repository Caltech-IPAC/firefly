package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.SUTDebug;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageHDU;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.Cursor;
import edu.caltech.ipac.visualize.plot.projection.Projection;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class Geom
{

/* geom.c (geometric image transform program) */
/* version history:
 * "2.0 (Thu Jul  7 21:09:27 PDT 1994)";
 */
    static final String VERSION = "2.1 (Thu Nov 14 17:08:51 PST 1996)";
    //private static final String im_history_line = "geom V" + VERSION;

    private ImageHeader ref_header ;
    private ImageHeader in_header = null;
    private ImageHeader out_header = null;
    private Header in_fits_header = null;
    private Header ref_fits_header = null;



/* parameters */
    boolean need_crpix_adjusted = false;
    double crpix1_base;
    double crpix2_base;
    int imageScaleFactor = 1;

    boolean n_ref_name = false;
    int tie_skip = 10;
    boolean interp_flag = false;
    double min_wgt = 0.5;
    int override_naxis1;
    int override_naxis2;
    boolean n_override_naxis1 = false;
    boolean n_override_naxis2 = false;
    String override_ctype1;
    boolean n_override_ctype1 = false;
    double override_cdelt1, override_cdelt2;
    boolean n_override_cdelt1 = false;
    boolean n_override_cdelt2 = false;
    double override_crval1, override_crval2;
    boolean n_override_crval1 = false;
    boolean n_override_crval2 = false;
    boolean n_override_CDmatrix = false;
    double override_CD1_1;
    double override_CD1_2;
    double override_CD2_1;
    double override_CD2_2;
    double override_crota2;
    boolean n_override_crota2 = false;
    boolean progress_info = false;

    /* fids */
    //int in_fid, out_fid, ref_fid;

    /* "in" info */
    private int in_naxis1;
    private int in_naxis2;
    private double in_crpix1;
    private double in_crpix2;
    private String in_ctype1;
    private String in_ctype2;
    private double in_cdelt1;
    private double in_cdelt2;
    private double in_crval1;
    private double in_crval2;
    private double in_crota1;
    private double in_crota2;
    private double in_equinox;
    private double in_x_pixel_size; 
    private double in_y_pixel_size;
    private double in_plt_scale;
    private boolean in_map_distortion;
    private boolean in_using_cd;

    /* "out" info */
    private int out_naxis1; 
    private int out_naxis2;
    private double out_crpix1; 
    private double out_crpix2;
    private String out_ctype1;
    private String out_ctype2;
    private double out_cdelt1; 
    private double out_cdelt2;
    private double out_crval1; 
    private double out_crval2;
    private double out_crota1; 
    private double out_crota2;
    private double out_equinox;
    private int out_maptype;
    private double out_x_pixel_size; 
    private double out_y_pixel_size;
    private double out_plt_scale;
    private boolean out_map_distortion;
    private boolean out_using_cd;
    private double out_cd1_1;
    private double out_cd1_2;
    private double out_cd2_1;
    private double out_cd2_2;


    /* buffers & such */
    private float blank_val = Float.NaN;
    //int stat;
    private int n2;
    private Projection in_proj;
    private Projection out_proj;
    private CoordinateSys in_coordinate_sys;
    private CoordinateSys out_coordinate_sys;
    private float in_data[];
    private double x_val[];
    private double y_val[];
    private double x_next[];
    private double y_next[];
    private double x_dd[];
    private double y_dd[];
    private float out_data[];
    float glop5;




ImageHeader open_in(FitsRead inFitsRead) throws 
    FitsException, IOException, GeomException
{
    //int stat;
    int i;
    ImageHeader temp_hdr = null;

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

	    in_header = inFitsRead.getImageHeader();

	    int pixel_count = in_header.naxis1 * in_header.naxis2;

	    if (SUTDebug.isDebug())
	    {
	    System.out.println("Geom.open_in: pixel_count = " + pixel_count);
	    }


	    if (in_header.getProjectionName() == "UNRECOGNIZED")
		throw new FitsException("Projection is not recognized");
	
	    if (in_header.getProjectionName() == "UNSPECIFIED")
		throw new FitsException("Image contains no projection info");
	
	    try
	    {
	    in_data = inFitsRead.getDataFloat();
	    //System.out.println("creating in_data  in_data.length = " + in_data.length);
	    }
	    catch (OutOfMemoryError e)
	    {
		GeomException ge= new GeomException(
		"Unable to allocate enough memory to reproject image",
		    in_header.naxis1, 
		    in_header.naxis2);
        ge.initCause(e);
        throw ge;
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

	if (SUTDebug.isDebug())
	{
	System.out.println("bitpix = " + in_header.bitpix);
	System.out.println("naxis = " + in_header.naxis);
	System.out.println("naxis1 = " + in_header.naxis1);
	System.out.println("naxis2 = " + in_header.naxis2);
	if (in_header.naxis > 2)
	{
	    System.out.println("naxis3 = " + in_header.naxis3);
	}
	

	System.out.println("getProjectionName() returns " + 
	    in_header.getProjectionName());
	System.out.println("getCoordSys() returns " + in_header.getCoordSys());
	System.out.println("file_equinox = " + in_header.file_equinox);
	System.out.println("getJsys() returns " + in_header.getJsys());
	}


   /* get header info */

   in_naxis1 = in_header.naxis1;
   in_naxis2 = in_header.naxis2;
   in_crpix1 = in_header.crpix1;
   in_crpix2 = in_header.crpix2;
   in_cdelt1 = in_header.cdelt1;
   in_cdelt2 = in_header.cdelt2;
   in_crval1 = in_header.crval1;
   in_crval2 = in_header.crval2;
   in_crota1 = in_header.crota1;
   in_crota2 = in_header.crota2;
   in_ctype1 = in_header.ctype1;
   in_ctype2 = in_header.ctype2;
   in_equinox = in_header.file_equinox;
   in_x_pixel_size = in_header.x_pixel_size;
   in_y_pixel_size = in_header.y_pixel_size;
   in_plt_scale = in_header.plt_scale;
   in_map_distortion = in_header.map_distortion;
   in_using_cd = in_header.using_cd;



   /* set mapping from input */
    try
    {
	/* should really clone in_header, but this may suffice */
	temp_hdr = new ImageHeader(in_fits_header);
	temp_hdr.cdelt2 = in_header.cdelt2;
	temp_hdr.crpix2 = in_header.crpix2;
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

   temp_hdr.crpix1 = 0;
   temp_hdr.crpix2 = 0;
   in_coordinate_sys = CoordinateSys.makeCoordinateSys(
       temp_hdr.getJsys(), temp_hdr.file_equinox);
   in_proj = temp_hdr.createProjection(in_coordinate_sys);

       
   /*
   stat = map_set(in_map_block,in_ctype1,in_ctype2,in_cdelt1,in_cdelt2,
           in_crval1,in_crval2,      0.0,      0.0,in_crota1,in_crota2);
   if (stat != 0){
      System.out.println("Illegal geometric data in input file (stat=" + stat
	  + ")");
      System.exit(0);
   }
   */

   return(in_header);

}


private void open_ref(Fits refFits) throws FitsException
{
    try
    {

	    BasicHDU HDU = refFits.getHDU(0);
	    ref_fits_header = HDU.getHeader();
	    //BufferedDataInputStream ibs = refFits.getStream();
	    //ref_fits_header = Header.readHeader(ibs);
	    if (ref_fits_header == null) 
	    {
		if (SUTDebug.isDebug())
		{
		    System.out.println("HDU null! (ref image)");
		}
		throw new FitsException("HDU null! (ref image)");
	    }

	    ref_header = new ImageHeader(ref_fits_header);
    }
    catch (IOException e)
    {
	if (SUTDebug.isDebug())
	{
	    System.out.println("got IOException: " + e.getMessage());
	}
	throw new FitsException("got IOException: " + e.getMessage());
    }
    open_ref();
}

private void open_ref() throws FitsException
{

	try
	{

	    if (ref_header.getProjectionName() == "UNRECOGNIZED")
		throw new FitsException("Projection is not recognized");

	    if (ref_header.getProjectionName() == "UNSPECIFIED")
		throw new FitsException("Image contains no projection info");

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

   out_naxis1 = ref_header.naxis1;
   out_naxis2 = ref_header.naxis2;
   out_crpix1 = ref_header.crpix1;
   out_crpix2 = ref_header.crpix2;
   out_cdelt1 = ref_header.cdelt1;
   out_cdelt2 = ref_header.cdelt2;
   out_crval1 = ref_header.crval1;
   out_crval2 = ref_header.crval2;
   out_crota1 = ref_header.crota1;
   out_crota2 = ref_header.crota2;
   out_ctype1 = ref_header.ctype1;
   out_ctype2 = ref_header.ctype2;
   out_equinox = ref_header.file_equinox;
   out_maptype = ref_header.maptype;
   out_x_pixel_size= ref_header.x_pixel_size;
   out_y_pixel_size= ref_header.y_pixel_size;
//	System.out.println(
//	    "We put into variables:  out_x_pixel_size = " +
//	    out_x_pixel_size + "  out_y_pixel_size = " + out_y_pixel_size);
   out_plt_scale = ref_header.plt_scale;
   out_map_distortion = ref_header.map_distortion;
   out_using_cd = ref_header.using_cd;
   out_cd1_1 = ref_header.cd1_1;
   out_cd1_2 = ref_header.cd1_2;
   out_cd2_1 = ref_header.cd2_1;
   out_cd2_2 = ref_header.cd2_2;


}


private void set_out_from_in() 
{
   out_naxis1 = in_naxis1; out_naxis2 = in_naxis2;
   out_crval1 = in_crval1; out_crval2 = in_crval2;
   out_cdelt1 = in_cdelt1; out_cdelt2 = in_cdelt2;
   out_crpix1 = in_crpix1; out_crpix2 = in_crpix2;
   out_crota1 = in_crota1; out_crota2 = in_crota2;
   out_ctype1 = in_ctype1;
   out_ctype2 = in_ctype2;
   out_equinox = in_equinox;
   out_maptype = in_header.maptype;
   out_x_pixel_size= in_x_pixel_size;
   out_y_pixel_size= in_y_pixel_size;
   out_plt_scale = in_plt_scale;
   out_map_distortion = in_map_distortion;
   out_using_cd = in_using_cd;

   /* make dummy ref_header (partial only) */
   ref_header = new ImageHeader();
   ref_header.naxis = in_header.naxis;
   ref_header.naxis1 = in_header.naxis1;
   ref_header.naxis2 = in_header.naxis2;
   ref_header.maptype = in_header.maptype;
   ref_header.a_order = in_header.a_order;
   ref_header.ap_order = in_header.ap_order;
   ref_header.b_order = in_header.b_order;
   ref_header.bp_order = in_header.bp_order;
   for (int i=0; i < 5; i++)
   {
      for (int j=0; j < 5; j++)
      {
	   ref_header.a[i][j] = in_header.a[i][j];
	   ref_header.ap[i][j] = in_header.ap[i][j];
	   ref_header.b[i][j] = in_header.b[i][j];
	   ref_header.bp[i][j] = in_header.bp[i][j];
      }
   }


}


private void do_user_overrides() 
{
   if (n_override_naxis1) out_naxis1 = override_naxis1;
   if (n_override_naxis2) out_naxis2 = override_naxis2;
   if (n_override_crval1) out_crval1 = override_crval1;
   if (n_override_crval2) out_crval2 = override_crval2;
   if (n_override_cdelt1)
   {
       out_cdelt1 = override_cdelt1;
       out_x_pixel_size= - override_cdelt1 / out_plt_scale * 1000 * 3600; ;
   }
   if (n_override_cdelt2)
   {
       out_cdelt2 = override_cdelt2;
       out_y_pixel_size= override_cdelt2 / out_plt_scale * 1000 * 3600; ;
   }
   if (n_override_CDmatrix)
   {
       out_cd1_1 = override_CD1_1;
       out_cd1_2 = override_CD1_2;
       out_cd2_1 = override_CD2_1;
       out_cd2_2 = override_CD2_2;
       /* need to compute out_x_pixel_size, out_y_pixel_size for DSS plate */
       out_x_pixel_size = - out_cdelt1 / out_plt_scale * 1000 * 3600;
       out_y_pixel_size = out_cdelt2 / out_plt_scale * 1000 * 3600;
   }

   if (n_override_crota2) out_crota2 = override_crota2;
   if (n_override_ctype1) {
      out_ctype1 = override_ctype1;
      if (out_ctype1.startsWith("RA"))
	  out_ctype2 = "DEC";
      else {
         if      (out_ctype1.startsWith("GLON")) 
	     out_ctype2 = "GLAT";
         else if (out_ctype1.startsWith("ELON"))
	     out_ctype2 = "ELAT";
         else            
	     out_ctype2 = "DEC-";
	 out_ctype2.concat(out_ctype1.substring(4));
      }
   }

}


private void do_auto_overrides() throws FitsException
{
   int flag; /* 2=NAX&CRPIX  1=CRPIX  0=none */

   if (n_override_crval1 | n_override_crval2 | n_override_cdelt1
     | n_override_cdelt2 | n_override_crota2 | n_override_ctype1 |
     n_override_CDmatrix) flag = 2;
   else flag=0;
   
   if (n_override_naxis1) {
      if (override_naxis1==0) flag=2;
      else {
         flag = 1;
         if (!n_override_naxis2){
	   throw new FitsException("Must alter both NAXIS's if you alter either");
         }
      }
   }
   else if (n_override_naxis2){
	   throw new FitsException("Must alter both NAXIS's if you alter either");
   }


   if (SUTDebug.isDebug())
   {
       System.out.println("RBH n_override_crval1 = " + n_override_crval1);
       System.out.println("RBH n_override_crval2 = " + n_override_crval2);
       System.out.println("RBH n_override_cdelt1 = " + n_override_cdelt1);
       if (n_override_cdelt1)
	   System.out.println("   override_cdelt1 = " + override_cdelt1);
       System.out.println("RBH n_override_cdelt2 = " + n_override_cdelt2);
       if (n_override_cdelt2)
	   System.out.println("   override_cdelt2 = " + override_cdelt2);
       if (n_override_CDmatrix)
       {
	   System.out.println("   override_CD1_1 = " + override_CD1_1);
	   System.out.println("   override_CD1_2 = " + override_CD1_2);
	   System.out.println("   override_CD2_1 = " + override_CD2_1);
	   System.out.println("   override_CD2_2 = " + override_CD2_2);
       }
       System.out.println("RBH n_override_crota2 = " + n_override_crota2);
       System.out.println("RBH n_override_ctype1 = " + n_override_ctype1);
       System.out.println("RBH n_override_naxis1 = " + n_override_naxis1);
       System.out.println("RBH override_naxis1 = " + override_naxis1);
       System.out.println("RBH flag = " + flag);
   }
   if (flag==2) override_naxis_and_crpix();
   else if (flag==1) override_crpix_only();

}


private void override_crpix_only() throws FitsException
{
    double center_x, center_y;
    double lon, lat;
    //int stat;
    ProjectionPt image_pt;
    WorldPt world_pt;

    center_x = (in_naxis1+1.0) / 2.0 - in_crpix1;
    center_y = (in_naxis2+1.0) / 2.0 - in_crpix2;

    try
    {
	world_pt = in_proj.getWorldCoords( center_x - 1, center_y - 1);

	if (!out_coordinate_sys.equals(in_coordinate_sys)) 
	{
	    world_pt = Plot.convert(world_pt, out_coordinate_sys);
	}
	lon = world_pt.getX();
	lat = world_pt.getY();
	image_pt = out_proj.getImageCoords( lon, lat);
	center_x = image_pt.getFsamp() + 1;
	center_y = image_pt.getFline() + 1;
    }
    catch (ProjectionException pe)
    {
	if (SUTDebug.isDebug())
	{
	    System.out.println("got ProjectionException: " + pe.getMessage());
	}
	throw new FitsException("Could not reproject image.\n -  Coordinates probably too far away.");
    }
    out_crpix1 += (out_naxis1+1.0) / 2.0 - center_x;
    out_crpix2 += (out_naxis2+1.0) / 2.0 - center_y;

}


/****************************************************************/

/* Take an x,y on input image, reproject to an x,y on output image */
/*  and do min-max on values in struct mm */

private int minmax_xy(double in_x, double in_y,
                 mmxy mm)  throws FitsException
{
    double lon, lat;
    double x = 0.0, y = 0.0;
    //int stat;
    ProjectionPt image_pt;
    WorldPt world_pt;

    try
    {
	world_pt = in_proj.getWorldCoords( in_x - in_crpix1 - 1, 
	    in_y - in_crpix2 - 1);
	if (!out_coordinate_sys.equals(in_coordinate_sys)) 
	{
	    world_pt = Plot.convert(world_pt, out_coordinate_sys);
	}
	lon = world_pt.getX();
	lat = world_pt.getY();

	image_pt = out_proj.getImageCoords( lon, lat);
	x = image_pt.getFsamp() + 1;
	y = image_pt.getFline() + 1;
    }
    catch (ProjectionException pe)
    {
	if (SUTDebug.isDebug())
	{
	    System.out.println("got ProjectionException: " + pe.getMessage());
	}
	throw new FitsException("Could not reproject image.\n -  Coordinates probably too far away.");
    }
    if (x > mm.max_x) mm.max_x = x;
    if (y > mm.max_y) mm.max_y = y;
    if (x < mm.min_x) mm.min_x = x;
    if (y < mm.min_y) mm.min_y = y;
    return(0);
}

/****************************************************************/

private void override_naxis_and_crpix() throws FitsException
{
   mmxy mm = new mmxy();
   int lo_x, hi_x, lo_y, hi_y;
   
   //take 9 spots on input image - find max x, max y, min x, and min y
   // on the output image
   mm.min_x = 32767;
   mm.min_y = 32767;
   mm.max_x = -32767;
   mm.max_y = -32767;
   minmax_xy(1.0,1.0,mm);
   minmax_xy(1.0,(in_naxis2+1.0)/2.0,mm);
   minmax_xy(1.0,(double)in_naxis2,mm);
   minmax_xy((in_naxis1+1.0)/2.0,1.0,mm);
   minmax_xy((in_naxis1+1.0)/2.0,(in_naxis2+1.0)/2.0,mm);
   minmax_xy((in_naxis1+1.0)/2.0,(double)in_naxis2,mm);
   minmax_xy((double)in_naxis1,1.0,mm);
   minmax_xy((double)in_naxis1,(in_naxis2+1.0)/2.0,mm);
   minmax_xy((double)in_naxis1,(double)in_naxis2,mm);

   if (mm.min_x == 32767){
      if (SUTDebug.isDebug())
      {
	  System.out.println(
		 "Unable to re-compute NAXIS's for output area");
      }
      throw new FitsException("Unable to re-compute NAXIS's for output area");
   }

   /* round */
   lo_x = (int) Math.floor(mm.min_x+0.5);
   lo_y = (int) Math.floor(mm.min_y+0.5);
   hi_x =  (int) Math.ceil(mm.max_x-0.5);
   hi_y =  (int)Math.ceil(mm.max_y-0.5);

   // now make the output image big enough to hold the reprojected input image
   out_naxis1 = hi_x - lo_x + 1;
   out_naxis2 = hi_y - lo_y + 1;
   if (out_naxis1==0 || out_naxis2==0){
      if (SUTDebug.isDebug())
      {
	  System.out.println(
		 "computed output area is null");
      }
      throw new FitsException("computed output area is null");
   }

   out_crpix1 -= mm.min_x - 1.0;
   out_crpix2 -= mm.min_y - 1.0;

   if (need_crpix_adjusted)
   {
       adjust_crpix();
   }

}

/**
 * Adjust the desired CRPIX values for Spot
 *  The goal is that getOffsetX() / imageScaleFactor is an integer
 *  and that getOffsetY() / imageScaleFactor is an integer
 *  This allows Spot to hand the 2-d package a INTEGER offsets
 *  of (getOffset() / imageScaleFactor) with full precision
 */
private void adjust_crpix()
{
    double new_crpix1_overlay;
    double new_crpix2_overlay;
    double crpix1_overlay = out_crpix1;
    double crpix2_overlay = out_crpix2;
    double value_needing_integerize;

    /* first CRPIX1 */
    value_needing_integerize = (crpix1_base - 0.5) / imageScaleFactor -
	(crpix1_overlay - 0.5);
    //System.out.println("value_needing_integerize = " + value_needing_integerize);
    value_needing_integerize = Math.round(value_needing_integerize);
    //System.out.println("value_needing_integerize = " + value_needing_integerize);
    new_crpix1_overlay =
	((crpix1_base - 0.5) / imageScaleFactor - value_needing_integerize) + 0.5;
    //System.out.println("new_crpix1_overlay = " + new_crpix1_overlay);
    out_crpix1 = new_crpix1_overlay;

    /* now CRPIX2 */
    value_needing_integerize = (crpix2_base - 0.5) / imageScaleFactor -
	(crpix2_overlay - 0.5);
    //System.out.println("value_needing_integerize = " + value_needing_integerize);
    value_needing_integerize = Math.round(value_needing_integerize);
    //System.out.println("value_needing_integerize = " + value_needing_integerize);
    new_crpix2_overlay =
	((crpix2_base - 0.5) / imageScaleFactor - value_needing_integerize) + 0.5;
    //System.out.println("new_crpix2_overlay = " + new_crpix2_overlay);
    out_crpix2 = new_crpix2_overlay;

}


private Fits write_pixels() throws FitsException
{
    int ndim;
    int dims[];
    Object data;
    Fits newFits = null;

    /* open output file */
    //PrimaryHDU myHDU = new PrimaryHDU(in_fits_header);

    ndim = in_fits_header.getIntValue("NAXIS", 0) ;
    dims = new int[ndim];

    // Note that we have to invert the order of the axes
    // for the FITS file to get the order in the array we
    // are generating.

    for (int i=0; i<ndim; i += 1) 
    {
	int cdim = in_fits_header.getIntValue("NAXIS"+(i+1), 0);
	if (cdim < 0) 
	{
	    throw new FitsException("Invalid array dimension:"+cdim);
	}
	dims[ndim-i-1] = cdim;
    }
    data = ArrayFuncs.curl(out_data, dims);
    nom.tam.fits.ImageData id = new 
	nom.tam.fits.ImageData(data);
    //myHDU.setData(id);

    ImageHDU myHDU = new ImageHDU(in_fits_header, id);

    newFits = new Fits();
    newFits.addHDU(myHDU);
    return (newFits);

}


int next_n2 = 0;

private void compute_geom_line()
{
   float n2_interp;
   int n1;

   if (n2==0) next_n2 = 0;

   if (n2 == next_n2) 
   { 
      if (n2 != 0) 
      {
	  System.arraycopy(x_next, 0, x_val, 0, out_naxis1);
	  System.arraycopy(y_next, 0, y_val, 0, out_naxis1);
      }
      else
	  compute_a_line(0,x_val,y_val);
   }

   if (n2 > next_n2) { 
      next_n2 += tie_skip;
      if (next_n2 >= out_naxis2) next_n2 = out_naxis2 - 1;
      compute_a_line(next_n2,x_next,y_next);
      n2_interp = next_n2 - n2 + 1;
      if (n2_interp > 0) for (n1=0; n1<out_naxis1; n1++) {
         if (x_val[n1]==x_val[n1] && x_next[n1]==x_next[n1]) {
            x_dd[n1] = (x_next[n1] - x_val[n1]) / n2_interp;
            y_dd[n1] = (y_next[n1] - y_val[n1]) / n2_interp;
	    glop5 = n2_interp;  // this is here to combat an optimizer bug
         }
         else x_dd[n1] = blank_val;
      }
   }

   if (n2 < next_n2) {
      for (n1=0; n1<out_naxis1; n1++) if (x_dd[n1] == x_dd[n1]) {
         x_val[n1] = x_val[n1] + x_dd[n1];
         y_val[n1] = y_val[n1] + y_dd[n1];
      }
   }

} /* end compute_geom_line */


private void compute_a_line(int local_n2, 
	double local_x_dd[], double local_y_dd[])
{
    int next_n1;
    int n1;
    int stat;
    double  x_del = Double.NaN, y_del = Double.NaN;
    double  n1_interp;
    double tmp_x, tmp_y;
    double lon, lat;
    ProjectionPt image_pt;
    WorldPt world_pt;

    next_n1 = -tie_skip;
    for (n1=0; n1<out_naxis1; n1++) 
    {

	if (n1 > next_n1) 
	{ 
	    /* compute via map functions */
	    next_n1 += tie_skip;
	    if (next_n1 >= out_naxis1) next_n1 = out_naxis1 - 1;
	    tmp_x = next_n1 + 1;
	    tmp_y = local_n2 + 1;

	    try
	    {
		//System.out.println("in = " + tmp_x + "," + tmp_y);
		world_pt = out_proj.getWorldCoords( tmp_x - 1, tmp_y - 1);
		//System.out.println("RBH PRE lon = " + world_pt.getX()
		//    + "  lat = " + world_pt.getY());
		//System.out.println(
		//    "RBH out_coordinate_sys = " + out_coordinate_sys + 
		//    "  in_coordinate_sys = " + in_coordinate_sys);
		if (!out_coordinate_sys.equals(in_coordinate_sys)) 
		{
		    world_pt = Plot.convert(world_pt, in_coordinate_sys);
		}
		lon = world_pt.getX();
		lat = world_pt.getY();
		//System.out.println("RBH POST lon = " + lon + "  lat = " + lat);
		image_pt = in_proj.getImageCoords( lon, lat);
		tmp_x = image_pt.getFsamp() + 1;
		tmp_y = image_pt.getFline() + 1;
		stat = 0;
		//System.out.println("out = " + tmp_x + "," + tmp_y);
	    }
	    catch (ProjectionException pe)
	    {
		if (SUTDebug.isDebug())
		{
		    System.out.println("got ProjectionException: " + 
			pe.getMessage());
		}
		stat = 1;
	    }
	    /* printf(" out=%f,%f stat=%d\n",tmp_x,tmp_y,stat); */
	    if (stat==0) 
	    {
		local_x_dd[next_n1] = tmp_x; 
		local_y_dd[next_n1] = tmp_y;
	    }
	    else 
	    {
		local_x_dd[next_n1] = blank_val;
		local_y_dd[next_n1] = blank_val;
	    }

	    n1_interp = next_n1 - n1 + 1; /* compute delt pix-to-pix */
	    if ((n1_interp > 0.0) && (n1 > 0)) 
	    {
		if (!Double.isNaN(local_x_dd[n1]))
		{
		    x_del = (local_x_dd[next_n1] - local_x_dd[n1-1]) / n1_interp;
		    y_del = (local_y_dd[next_n1] - local_y_dd[n1-1]) / n1_interp;
		}
		else 
		{
		    x_del = blank_val;
		    y_del = blank_val;
		}
	    }
	}

	if (n1 < next_n1) 
	{ 
	    /* interpolate within the line */
	    if (!Double.isNaN(x_del))
	    {
		local_x_dd[n1] = local_x_dd[n1-1] + x_del;
		local_y_dd[n1] = local_y_dd[n1-1] + y_del;
	    }
	    else 
	    {
		local_x_dd[n1] = blank_val;
		local_y_dd[n1] = blank_val;
	    }
	}

    } /* end for n1 */

} /* END OF compute_a_line */



private void bin_nearest_neighbor()
{
   double del_x, del_y;
   int x_coord, y_coord;
   int n1;
   int index;
   int out_index;

   del_x = in_crpix1 - 0.5;
   del_y = in_crpix2 - 0.5;

   for (n1=0; n1<out_naxis1; n1++) {

       out_index = n1 + n2 * out_naxis1;

       if (x_val[n1] != x_val[n1]) {
          out_data[out_index] = blank_val;
          continue;
       }
       x_coord = (int) (x_val[n1]  + del_x);
       if (x_coord<0 || x_coord >= in_naxis1) {
          out_data[out_index] = blank_val;
          continue;
       }
       y_coord = (int) (y_val[n1]  + del_y);
       if (y_coord<0 || y_coord >= in_naxis2) {
          out_data[out_index] = blank_val;
          continue;
       }
       index = x_coord + y_coord * in_naxis1;
       //System.out.println("x_coord = " + x_coord + "  y_coord = " + y_coord +
	//    "   index = " + index + "  n1 = " + n1
	//    + "  out_index = " + out_index);

       out_data[out_index] = in_data[index];

   } /* end for n1 */
} /* end bin_nearest_neighbor */



private void bin_bilinear()
{
   double min_x_inside, min_y_inside;
   double max_x_inside, max_y_inside;
   double del_x, del_y;
   double x_float, y_float;
   double x_frac, y_frac;
   int x_coord, y_coord;
   //float *ptr_00, *ptr_10, *ptr_01, *ptr_11;
   int ptr_00, ptr_10, ptr_01, ptr_11;
   double wgt_00, wgt_10, wgt_01, wgt_11;
   double weight;
   double sum;
   int n1;
   int index;
   int out_index;
   
   /* compute constants */
   min_x_inside = min_wgt - in_crpix1;
   min_y_inside = min_wgt - in_crpix2;
   max_x_inside = 1.0 + in_naxis1 - min_wgt - in_crpix1;
   max_y_inside = 1.0 + in_naxis2 - min_wgt - in_crpix2;

   del_x = in_crpix1 - 1.0;
   del_y = in_crpix2 - 1.0;

   for (n1=0; n1<out_naxis1; n1++) {

       out_index = n1 + n2 * out_naxis1;

       if (x_val[n1] != x_val[n1]) {out_data[out_index] = blank_val; continue;}
       if (x_val[n1] < min_x_inside || x_val[n1] > max_x_inside) {
          out_data[out_index] = blank_val;
          continue;
       }
       if (y_val[n1] < min_y_inside || y_val[n1] > max_y_inside) {
          out_data[out_index] = blank_val;
          continue;
       }

       x_float = x_val[n1]  + del_x;
       if (x_float > 0) x_coord = (int) x_float;
       else             x_coord = (int) x_float + 1;
       x_frac = x_float - x_coord;

       y_float = y_val[n1]  + del_y;
       if (y_float > 0) y_coord = (int) y_float;
       else             y_coord = (int) y_float + 1;
       y_frac = y_float - y_coord;

       index = x_coord + y_coord * in_naxis1;
       ptr_00 = index;
       ptr_10 = ptr_00 + 1;
       ptr_01 = ptr_00 + in_naxis1;
       ptr_11 = ptr_01 + 1;

       wgt_11 = x_frac * y_frac;
       wgt_01 = y_frac - wgt_11;
       wgt_10 = x_frac - wgt_11;
       wgt_00 = 1.0 - y_frac - wgt_10;

       if (x_coord < 0)
       {
	   ptr_00 = -1;
	   ptr_01 = -1;
       }
       else if (x_coord >= in_naxis1) 
       {
	   ptr_10 = -1;
	   ptr_11 = -1;
       }
       if (y_coord < 0)
       {
	   ptr_00 = -1;
	   ptr_10 = -1;
       }
       else if (y_coord >= in_naxis2) 
       {
	   ptr_01 = -1;
	   ptr_11 = -1;
       }

       try
       {
       if ((ptr_00 < 0) || (ptr_00 >= in_data.length) || 
	   (Double.isNaN(in_data[ptr_00])))
		ptr_00 = -1;
       if ((ptr_01 < 0) || (ptr_01 >= in_data.length) || 
	   (Double.isNaN(in_data[ptr_01])))
		ptr_01 = -1;
       if ((ptr_10 < 0) || (ptr_10 >= in_data.length) || 
	   (Double.isNaN(in_data[ptr_10])))
		ptr_10 = -1;
       if ((ptr_11 < 0) || (ptr_11 >= in_data.length) || 
	   (Double.isNaN(in_data[ptr_11])))
		ptr_11 = -1;
       }
       catch (ArrayIndexOutOfBoundsException ae)
       {
	    if (SUTDebug.isDebug())
	    {
		System.out.println("ArrayIndexOutOfBoundsException index = " + 
		    index + "  ptr_01 = " + ptr_01);
	    }
	    throw ae;
       }

       sum = 0.0;
       weight = 1.0;
       if (ptr_00 >= 0) sum += in_data[ptr_00] * wgt_00;
       else weight -= wgt_00;
       if (ptr_10 >= 0) sum += in_data[ptr_10] * wgt_10;
       else weight -= wgt_10;
       if (ptr_01 >= 0) sum += in_data[ptr_01] * wgt_01;
       else weight -= wgt_01;
       if (ptr_11 >= 0) sum += in_data[ptr_11] * wgt_11;
       else weight -= wgt_11;

       if (weight >= min_wgt) out_data[out_index] = (float) (sum / weight);
       else                   out_data[out_index] = blank_val;


/* +++++++++++++++++++++++++++++++++++++
if (n1==0)  {
printf("n1= %d  n2=%d\n",n1,n2);
printf("x_val[n1] = %f   y_val[n1] = %f\n",x_val[n1],y_val[n1]);
printf("x_float = %f   y_float = %f\n",x_float,y_float);
printf("x_coord = %d   y_coord = %d\n",x_coord,y_coord);
printf("x_frac = %f   y_frac = %f\n",x_frac,y_frac);
printf("ptr_00=%d  ptr_01=%d  ptr_10=%d  ptr_11=%d\n",
  ptr_00,ptr_01,ptr_10,ptr_11);
printf("*ptr_00=%g  *ptr_01=%g  *ptr_10=%g  *ptr_11=%g\n",
  *ptr_00,*ptr_01,*ptr_10,*ptr_11);
printf("wgt_00=%f  wgt_01=%f  wgt_10=%f  wgt_11=%f\n",
  wgt_00,wgt_01,wgt_10,wgt_11);
printf("sum = %g   weight = %f   out_data[n1] = %g\n",
   sum, weight, out_data[n1]);
}
++++++++++++++++++++++++++++++++ */


   } /* end for n1 */

} /* end bin_bilinear */



    /** normal entry when called from a standalone program, like GeomTest
    * @param inFits Fits object for input file to be reprojected
    * @param refFits Fits object for reference image containing the desirec projection
    * @return Fits object with the reprojected image
    */
    Fits do_geom(Fits inFits, Fits refFits) 
	throws FitsException, IOException, GeomException
    {
	FitsRead[] fitsReadArray =  FitsRead.createFitsReadArray(inFits);
	open_in(fitsReadArray[0]);
	//open_in(inFits);
	return(do_geom(refFits));
    }

    /** entry when input FITS file has already been opened via call to open_in()
    * This entry is only called locally
    * @param refFits Fits object for reference image containing the desirec projection
    * @return Fits object with the reprojected image
    */
    Fits do_geom(Fits refFits) 
    //private Fits do_geom(Fits refFits) 
	throws FitsException, IOException, GeomException
    {

	if (in_header == null)
	    throw (new FitsException(
		"Illegal to call do_geom(Fits) without an open input file"));

	if (refFits != null) 
	{
	    open_ref(refFits);
	}
	else 
	{
	    set_out_from_in();
	}
	return(do_geom());
    }

    /** entry when input FITS file has already been opened via call to open_in()
    * and reference image has been independently opened and its values are in
    * refFitsRead.  This entry is used by FitsRead.java
    * @param refFitsRead FitsRead object for reference image containing the desirec projection
    * @return Fits object with the reprojected image
    */
    Fits do_geom(FitsRead refFitsRead) 
	throws FitsException, IOException, GeomException
    {

	ref_fits_header = refFitsRead.getHeader();
	ref_header = refFitsRead.getImageHeader();
	open_ref();
	return(do_geom());
    }

    /** entry when input FITS file has already been opened via call to open_in()
    * and reference image has been independently opened and its values are in
    * ref_fits_header and ref_header
    * @return Fits object with the reprojected image
    */
    private Fits do_geom()
	throws FitsException, GeomException
    {
	do_user_overrides();


   /* fill in all the Header values */
	/* use the old header */
	try
	{
	in_fits_header.addValue("BITPIX", -32, null);
	in_fits_header.deleteKey("BLANK");
	in_fits_header.deleteKey("BSCALE");
	in_fits_header.deleteKey("BZERO");

	in_fits_header.addValue("NAXIS1", out_naxis1, null);
	in_fits_header.addValue("NAXIS2", out_naxis2, null);

	if (in_fits_header.getIntValue("NAXIS", 0) > 2)
	{
	    in_fits_header.addValue("NAXIS3", 1, null);
	}

	/* position the header pointer past NAXISn */
	String key = null;
	Cursor iter = in_fits_header.iterator();
	HeaderCard card;
	while (iter.hasNext())
	{
	    card = (HeaderCard) iter.next();
	    key = card.getKey();
	    if (key.startsWith("SIMPLE"))
		continue;
	    if (key.startsWith("BITPIX"))
		continue;
	    if (key.startsWith("NAXIS"))
		continue;
	    break;
	}
	in_fits_header.findKey(key);  // move fitsjava internal pointer
	/* done positioning header pointer */



	in_fits_header.addValue("EQUINOX", out_equinox, null);
	in_fits_header.deleteKey("EPOCH");
	in_fits_header.deleteKey("RADECSYS");
	in_fits_header.deleteKey("RADESYS");
	in_fits_header.deleteKey("SPOT_EXT");
	in_fits_header.deleteKey("SPOT_OFF");
	in_fits_header.deleteKey("SPOT_SZ");

	if (out_maptype == Projection.PLATE) 
	{
	    //in_fits_header.deleteKey("CTYPE1");
	    //in_fits_header.deleteKey("CTYPE2");
	    //in_fits_header.deleteKey("CRPIX1");
	    //in_fits_header.deleteKey("CRPIX2");
	    in_fits_header.deleteKey("CDELT1");
	    in_fits_header.deleteKey("CDELT2");
	    //in_fits_header.deleteKey("CRVAL1");
	    //in_fits_header.deleteKey("CRVAL2");
	    in_fits_header.deleteKey("CROTA1");
	    in_fits_header.deleteKey("CROTA2");
	    if (ref_fits_header != null) 
	    {
		/* fill in from ref header */
		/*   (otherwise use existing input header) */
		in_fits_header.addValue("PLTRAH", 
		    ref_fits_header.getDoubleValue( "PLTRAH"), null);
		in_fits_header.addValue("PLTRAM", 
		    ref_fits_header.getDoubleValue( "PLTRAM"), null);
		in_fits_header.addValue("PLTRAS", 
		    ref_fits_header.getDoubleValue( "PLTRAS"), null);
		in_fits_header.addValue("PLTDECSN", 
		    ref_fits_header.getStringValue( "PLTDECSN"), null);
		in_fits_header.addValue("PLTDECD", 
		    ref_fits_header.getDoubleValue( "PLTDECD"), null);
		in_fits_header.addValue("PLTDECM", 
		    ref_fits_header.getDoubleValue( "PLTDECM"), null);
		in_fits_header.addValue("PLTDECS", 
		    ref_fits_header.getDoubleValue( "PLTDECS"), null);
		in_fits_header.addValue("CNPIX1", 
		    ref_fits_header.getDoubleValue( "CNPIX1"), null);
		in_fits_header.addValue("CNPIX2", 
		    ref_fits_header.getDoubleValue( "CNPIX2"), null);
		in_fits_header.addValue("PLTSCALE", 
		    ref_fits_header.getDoubleValue( "PLTSCALE"), null);
		System.out.println(
		    "Putting into in_fits_header:  out_x_pixel_size = " +
		    out_x_pixel_size + "  out_y_pixel_size = " + out_y_pixel_size);
		in_fits_header.addValue("XPIXELSZ", 
		    out_x_pixel_size, null);
		in_fits_header.addValue("YPIXELSZ", 
		    out_y_pixel_size, null);
		in_fits_header.addValue("PPO1", 
		    ref_fits_header.getDoubleValue( "PPO1"), null);
		in_fits_header.addValue("PPO2", 
		    ref_fits_header.getDoubleValue( "PPO2"), null);
		in_fits_header.addValue("PPO3", 
		    ref_fits_header.getDoubleValue( "PPO3"), null);
		in_fits_header.addValue("PPO4", 
		    ref_fits_header.getDoubleValue( "PPO4"), null);
		in_fits_header.addValue("PPO5", 
		    ref_fits_header.getDoubleValue( "PPO5"), null);
		in_fits_header.addValue("PPO6", 
		    ref_fits_header.getDoubleValue( "PPO6"), null);
		in_fits_header.addValue("AMDX1", 
		    ref_fits_header.getDoubleValue( "AMDX1"), null);
		in_fits_header.addValue("AMDX2", 
		    ref_fits_header.getDoubleValue( "AMDX2"), null);
		in_fits_header.addValue("AMDX3", 
		    ref_fits_header.getDoubleValue( "AMDX3"), null);
		in_fits_header.addValue("AMDX4", 
		    ref_fits_header.getDoubleValue( "AMDX4"), null);
		in_fits_header.addValue("AMDX5", 
		    ref_fits_header.getDoubleValue( "AMDX5"), null);
		in_fits_header.addValue("AMDX6", 
		    ref_fits_header.getDoubleValue( "AMDX6"), null);
		in_fits_header.addValue("AMDX7", 
		    ref_fits_header.getDoubleValue( "AMDX7"), null);
		in_fits_header.addValue("AMDX8", 
		    ref_fits_header.getDoubleValue( "AMDX8"), null);
		in_fits_header.addValue("AMDX9", 
		    ref_fits_header.getDoubleValue( "AMDX9"), null);
		in_fits_header.addValue("AMDX10", 
		    ref_fits_header.getDoubleValue( "AMDX10"), null);
		in_fits_header.addValue("AMDX11", 
		    ref_fits_header.getDoubleValue( "AMDX11"), null);
		in_fits_header.addValue("AMDX12", 
		    ref_fits_header.getDoubleValue( "AMDX12"), null);
		in_fits_header.addValue("AMDX13", 
		    ref_fits_header.getDoubleValue( "AMDX13"), null);
		in_fits_header.addValue("AMDX14", 
		    ref_fits_header.getDoubleValue( "AMDX14"), null);
		in_fits_header.addValue("AMDX15", 
		    ref_fits_header.getDoubleValue( "AMDX15"), null);
		in_fits_header.addValue("AMDX16", 
		    ref_fits_header.getDoubleValue( "AMDX16"), null);
		in_fits_header.addValue("AMDX17", 
		    ref_fits_header.getDoubleValue( "AMDX17"), null);
		in_fits_header.addValue("AMDX18", 
		    ref_fits_header.getDoubleValue( "AMDX18"), null);
		in_fits_header.addValue("AMDX19", 
		    ref_fits_header.getDoubleValue( "AMDX19"), null);
		in_fits_header.addValue("AMDX20", 
		    ref_fits_header.getDoubleValue( "AMDX20"), null);
		in_fits_header.addValue("AMDY1", 
		    ref_fits_header.getDoubleValue( "AMDY1"), null);
		in_fits_header.addValue("AMDY2", 
		    ref_fits_header.getDoubleValue( "AMDY2"), null);
		in_fits_header.addValue("AMDY3", 
		    ref_fits_header.getDoubleValue( "AMDY3"), null);
		in_fits_header.addValue("AMDY4", 
		    ref_fits_header.getDoubleValue( "AMDY4"), null);
		in_fits_header.addValue("AMDY5", 
		    ref_fits_header.getDoubleValue( "AMDY5"), null);
		in_fits_header.addValue("AMDY6", 
		    ref_fits_header.getDoubleValue( "AMDY6"), null);
		in_fits_header.addValue("AMDY7", 
		    ref_fits_header.getDoubleValue( "AMDY7"), null);
		in_fits_header.addValue("AMDY8", 
		    ref_fits_header.getDoubleValue( "AMDY8"), null);
		in_fits_header.addValue("AMDY9", 
		    ref_fits_header.getDoubleValue( "AMDY9"), null);
		in_fits_header.addValue("AMDY10", 
		    ref_fits_header.getDoubleValue( "AMDY10"), null);
		in_fits_header.addValue("AMDY11", 
		    ref_fits_header.getDoubleValue( "AMDY11"), null);
		in_fits_header.addValue("AMDY12", 
		    ref_fits_header.getDoubleValue( "AMDY12"), null);
		in_fits_header.addValue("AMDY13", 
		    ref_fits_header.getDoubleValue( "AMDY13"), null);
		in_fits_header.addValue("AMDY14", 
		    ref_fits_header.getDoubleValue( "AMDY14"), null);
		in_fits_header.addValue("AMDY15", 
		    ref_fits_header.getDoubleValue( "AMDY15"), null);
		in_fits_header.addValue("AMDY16", 
		    ref_fits_header.getDoubleValue( "AMDY16"), null);
		in_fits_header.addValue("AMDY17", 
		    ref_fits_header.getDoubleValue( "AMDY17"), null);
		in_fits_header.addValue("AMDY18", 
		    ref_fits_header.getDoubleValue( "AMDY18"), null);
		in_fits_header.addValue("AMDY19", 
		    ref_fits_header.getDoubleValue( "AMDY19"), null);
		in_fits_header.addValue("AMDY20", 
		    ref_fits_header.getDoubleValue( "AMDY20"), null);
	    }
	}
	else
	{
	    in_fits_header.deleteKey("PLTRAH");
	    in_fits_header.deleteKey("PLTRAM");
	    in_fits_header.deleteKey("PLTRAS");
	    in_fits_header.deleteKey("PLTDECSN");
	    in_fits_header.deleteKey("PLTDECD");
	    in_fits_header.deleteKey("PLTDECM");
	    in_fits_header.deleteKey("PLTDECS");
	    in_fits_header.deleteKey("CNPIX1");
	    in_fits_header.deleteKey("CNPIX2");
	    in_fits_header.deleteKey("PLTSCALE");
	    in_fits_header.deleteKey("XPIXELSZ");
	    in_fits_header.deleteKey("YPIXELSZ");
	    in_fits_header.deleteKey("PPO1");
	    in_fits_header.deleteKey("PPO2");
	    in_fits_header.deleteKey("PPO3");
	    in_fits_header.deleteKey("PPO4");
	    in_fits_header.deleteKey("PPO5");
	    in_fits_header.deleteKey("PPO6");
	    in_fits_header.deleteKey("AMDX1");
	    in_fits_header.deleteKey("AMDX2");
	    in_fits_header.deleteKey("AMDX3");
	    in_fits_header.deleteKey("AMDX4");
	    in_fits_header.deleteKey("AMDX5");
	    in_fits_header.deleteKey("AMDX6");
	    in_fits_header.deleteKey("AMDX7");
	    in_fits_header.deleteKey("AMDX8");
	    in_fits_header.deleteKey("AMDX9");
	    in_fits_header.deleteKey("AMDX10");
	    in_fits_header.deleteKey("AMDX11");
	    in_fits_header.deleteKey("AMDX12");
	    in_fits_header.deleteKey("AMDX13");
	    in_fits_header.deleteKey("AMDX14");
	    in_fits_header.deleteKey("AMDX15");
	    in_fits_header.deleteKey("AMDX16");
	    in_fits_header.deleteKey("AMDX17");
	    in_fits_header.deleteKey("AMDX18");
	    in_fits_header.deleteKey("AMDX19");
	    in_fits_header.deleteKey("AMDX20");
	    in_fits_header.deleteKey("AMDY1");
	    in_fits_header.deleteKey("AMDY2");
	    in_fits_header.deleteKey("AMDY3");
	    in_fits_header.deleteKey("AMDY4");
	    in_fits_header.deleteKey("AMDY5");
	    in_fits_header.deleteKey("AMDY6");
	    in_fits_header.deleteKey("AMDY7");
	    in_fits_header.deleteKey("AMDY8");
	    in_fits_header.deleteKey("AMDY9");
	    in_fits_header.deleteKey("AMDY10");
	    in_fits_header.deleteKey("AMDY11");
	    in_fits_header.deleteKey("AMDY12");
	    in_fits_header.deleteKey("AMDY13");
	    in_fits_header.deleteKey("AMDY14");
	    in_fits_header.deleteKey("AMDY15");
	    in_fits_header.deleteKey("AMDY16");
	    in_fits_header.deleteKey("AMDY17");
	    in_fits_header.deleteKey("AMDY18");
	    in_fits_header.deleteKey("AMDY19");
	    in_fits_header.deleteKey("AMDY20");

	    in_fits_header.addValue("CTYPE1", out_ctype1, null);
	    in_fits_header.addValue("CTYPE2", out_ctype2, null);
	    in_fits_header.addValue("CRPIX1", out_crpix1, null);
	    in_fits_header.addValue("CRPIX2", out_crpix2, null);
	    in_fits_header.addValue("CRVAL1", out_crval1, null);
	    in_fits_header.addValue("CRVAL2", out_crval2, null);

	    in_fits_header.addValue("CDELT1", out_cdelt1, null);
	    in_fits_header.addValue("CDELT2", out_cdelt2, null);
	    in_fits_header.addValue("CROTA1", out_crota1, null);
	    in_fits_header.addValue("CROTA2", out_crota2, null);
	    if (in_map_distortion)
	    {
		if (out_map_distortion)
		{
		    /* output header will inherit input coefficients */
		}
		else
		{
		    /* no distortion in output image */
		    /* CTYPE1 will not end in -SIP, so the */
		    /* coefficients will be ignored */
		}
	    }
	    else
	    {
		if (out_map_distortion)
		{
		    /* need to copy over the distortion coefficients */
		    /* START COPYING DISTORTION COEFFICIENTS */
		    int i, j;
		    String keyword;

		    in_fits_header.addValue("A_ORDER", ref_header.a_order, null);
		    for (i = 0; i <= ref_header.a_order; i++)
		    {
			for (j = 0; j <= ref_header.a_order; j++)
			{
			    if ((i + j <= ref_header.a_order) && (i + j > 0))
			    {
				keyword = "A_" + i + "_" + j;
				in_fits_header.addValue(keyword, ref_header.a[i][j], null);
			    }
			}
		    }
		    in_fits_header.addValue("B_ORDER", ref_header.b_order, null);
		    for (i = 0; i <= ref_header.b_order; i++)
		    {
			for (j = 0; j <= ref_header.b_order; j++)
			{
			    if ((i + j <= ref_header.b_order) && (i + j > 0))
			    {
				keyword = "B_" + i + "_" + j;
				in_fits_header.addValue(keyword, ref_header.b[i][j], null);
			    }
			}
		    }
		    in_fits_header.addValue("AP_ORDER", ref_header.ap_order, null);
		    for (i = 0; i <= ref_header.ap_order; i++)
		    {
			for (j = 0; j <= ref_header.ap_order; j++)
			{
			    if ((i + j <= ref_header.ap_order) && (i + j > 0))
			    {
				keyword = "AP_" + i + "_" + j;
				in_fits_header.addValue(keyword, ref_header.ap[i][j], null);
			    }
			}
		    }
		    in_fits_header.addValue("BP_ORDER", ref_header.bp_order, null);
		    for (i = 0; i <= ref_header.bp_order; i++)
		    {
			for (j = 0; j <= ref_header.bp_order; j++)
			{
			    if ((i + j <= ref_header.bp_order) && (i + j > 0))
			    {
				keyword = "BP_" + i + "_" + j;
				in_fits_header.addValue(keyword, ref_header.ap[i][j], null);
			    }
			}
		    }

		    /* DONE COPYING DISTORTION COEFFICIENTS */
		}
		else
		{
		    /* neither image has distortion correction */
		}
	    }
	}
	if (in_using_cd)
	{
	    if (out_using_cd)
	    {
		/* RBH added this in (AR7897) */
		/* need to add in the CD matrix values */
		in_fits_header.addValue("CD1_1", out_cd1_1, null);
		in_fits_header.addValue("CD1_2", out_cd1_2, null);
		in_fits_header.addValue("CD2_1", out_cd2_1, null);
		in_fits_header.addValue("CD2_2", out_cd2_2, null);
		/* and delete CDELT and CROTA */
		in_fits_header.deleteKey("CDELT1");
		in_fits_header.deleteKey("CDELT2");
		in_fits_header.deleteKey("CROTA1");
		in_fits_header.deleteKey("CROTA2");
	    }
	    else
	    {
		/* delete CD keywords */
		in_fits_header.deleteKey("CD1_1");
		in_fits_header.deleteKey("CD1_2");
		in_fits_header.deleteKey("CD2_1");
		in_fits_header.deleteKey("CD2_2");
		in_fits_header.deleteKey("CD001001");
		in_fits_header.deleteKey("CD001002");
		in_fits_header.deleteKey("CD002001");
		in_fits_header.deleteKey("CD002002");
	    }
	}
	else
	{
	    if (out_using_cd)
	    {
		/* need to add in the CD matrix values */
		in_fits_header.addValue("CD1_1", out_cd1_1, null);
		in_fits_header.addValue("CD1_2", out_cd1_2, null);
		in_fits_header.addValue("CD2_1", out_cd2_1, null);
		in_fits_header.addValue("CD2_2", out_cd2_2, null);
		/* and delete CDELT and CROTA */
		in_fits_header.deleteKey("CDELT1");
		in_fits_header.deleteKey("CDELT2");
		in_fits_header.deleteKey("CROTA1");
		in_fits_header.deleteKey("CROTA2");
	    }
	    else
	    {
		/* no change */
		/* neither image uses the cd matrix */
	    }
	}

    }
	catch (HeaderCardException hce)
	{
	    if (SUTDebug.isDebug())
	    {
	    System.out.println("got HeaderCardException: " + hce.getMessage());
	    }
	    throw new FitsException("got HeaderCardException: " + hce.getMessage());
	}
	try
	{
	    out_header = new ImageHeader(in_fits_header);
	    //System.out.println("RAA out_header.crpix1 = " + out_header.crpix1);
	    //System.out.println("RAA out_header.toString()= " + out_header.toString());
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

	out_coordinate_sys = CoordinateSys.makeCoordinateSys(
	    out_header.getJsys(), out_header.file_equinox);
	//System.out.println("RBH 1 out_coordinate_sys = " + out_coordinate_sys);
	out_proj = out_header.createProjection(out_coordinate_sys);

	do_auto_overrides();

	/* update header values */
	try
	{
	in_fits_header.addValue("NAXIS1", out_naxis1, null);
	in_fits_header.addValue("NAXIS2", out_naxis2, null);
	if (out_maptype == Projection.PLATE)
	{
	    in_fits_header.addValue("CNPIX1", 0.5-out_crpix1, null);
	    in_fits_header.addValue("CNPIX2", 0.5-out_crpix2, null);
	}
	else
	{
	    in_fits_header.addValue("CRPIX1", out_crpix1, null);
	    in_fits_header.addValue("CRPIX2", out_crpix2, null);
	}
	}
	catch (HeaderCardException hce)
	{
	    if (SUTDebug.isDebug())
	    {
	    System.out.println("got HeaderCardException: " + hce.getMessage());
	    }
	    throw new FitsException("got HeaderCardException: " + hce.getMessage());

	}

	in_fits_header.resetOriginalSize();  // RBH added 3-25-2010

	try
	{
	out_header = new ImageHeader(in_fits_header);
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
	out_coordinate_sys = CoordinateSys.makeCoordinateSys(
	    out_header.getJsys(), out_header.file_equinox);
	if (SUTDebug.isDebug())
	{
	    System.out.println("RBH 2 out_coordinate_sys = " + out_coordinate_sys);
	    System.out.println("RBH 2 out_naxis1 = " + out_naxis1 +
		"   out_naxis2 = " + out_naxis2 +
		"  out_naxis1 * out_naxis2 = " + out_naxis1 * out_naxis2); 
	    System.out.println(
		"  ref_header.naxis1 = " + ref_header.naxis1 +
		"  ref_header.naxis2 = " + ref_header.naxis2);
	    System.out.println("RBH 2 out_cdelt1 = " + out_cdelt1 +
		"  out_cdelt2 = " + out_cdelt2 );
	    System.out.println("RBH 2 out_crpix1 = " + out_crpix1 +
		"  out_crpix2 = " + out_crpix2 );
	    if (out_using_cd)
	    {
		System.out.println("RBH 2 out_cd1_1 = " + out_cd1_1 
		    + "  out_cd1_2 = " + out_cd1_2 
		    + "  out_cd2_1 = " + out_cd2_1
		    + "  out_cd2_2 = " + out_cd2_2);
	    }
	}
	if ((out_naxis1 > 30 * ref_header.naxis1) ||
	    (out_naxis2 > 30 * ref_header.naxis2))
	{
	    throw new GeomException(
		"Overlay image is too different in scale to overlay",
		out_naxis1, out_naxis2);
	}
	if ((out_naxis1 <= 2) || (out_naxis2 <= 2))
	{
	    throw new GeomException(
		"Overlay image is too different in scale to overlay",
		out_naxis1, out_naxis2);
	}

	out_proj = out_header.createProjection(out_coordinate_sys);

	out_data = new float[out_naxis1 * out_naxis2];
	x_val = new double[out_naxis1];
	y_val = new double[out_naxis1];
	x_next = new double[out_naxis1];
	y_next = new double[out_naxis1];
	x_dd = new double[out_naxis1];
	y_dd = new double[out_naxis1];


    for(n2=0; n2<out_naxis2; n2++) 
    {
	compute_geom_line();

	if(interp_flag) 
	    bin_bilinear();
	else
	    bin_nearest_neighbor();

	//im_wpix_r(out_fid, out_data, out_naxis1);
    }

	Fits newFits = write_pixels();

	/* DEBUG */
	if (SUTDebug.isDebug())
	{
	try
	{
	    FileOutputStream fo = new java.io.FileOutputStream("/tmp/glop.fits");
	    BufferedDataOutputStream o = new BufferedDataOutputStream(fo);

	    if (false)  // for debug only
	    {
		try
		{
		BasicHDU HDU = newFits.getHDU(0);
		Header local_header = HDU.getHeader();
		//local_header.dumpHeader(System.out);
		}
		catch (IOException ioe)
		{
		    System.out.println("RBH got exception: " + ioe);
		}
	    }

	    newFits.write(o);
	}
	catch (FileNotFoundException e)
	{
	    System.out.println("Geom: got FileNotFoundException: " + e.getMessage());
	    e.printStackTrace();
	}
	catch (FitsException e)
	{
	    System.out.println("Geom: got FitsException: " + e.getMessage());
	    e.printStackTrace();
	}
	}
	/* END DEBUG */

	return(newFits);
    }


    private class mmxy {
	public double min_x;
	public double max_x; 
	public double min_y; 
	public double max_y;
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
