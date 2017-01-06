/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import java.lang.reflect.*;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import edu.caltech.ipac.util.SUTDebug;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.FitsException;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.util.BufferedDataOutputStream;


public class GeomTestMain
{

/* geom.c (geometric image transform program) */
/* version history:
 * "2.0 (Thu Jul  7 21:09:27 PDT 1994)";
 */

    static String out_name;
    static String ref_name; 
    private static String in_name;
    private static Geom geom;

static void usage()
{

   System.out.println("Usage: geom [flags] source  destination");
   System.out.println("");
   System.out.println("  -r file   filename of reference image"); 
   System.out.println("  -s n      compute tiepoints at every n'th line/sample"); 
   System.out.println("  -ib       use bi-linear interpolation for pixel value");
   System.out.println("  -in       use nearest neighbor pixel value [default]"); 
   System.out.println("  -d        derive output naxis and crpix values from input"); 
   System.out.println("  -f x      pixel fraction considered non-blank [0.5]");
   System.out.println("            (if -b is in effect)"); 
   System.out.println("  -t        Display progress information"); 
   System.out.println("");
   System.out.println("  -naxis1 n    Value to use for NAXIS1 [int<=32768]");
   System.out.println("  -naxis2 n    Value to use for NAXIS2 [int<=32767]");
   System.out.println("  -ctype1 str  Value to use for CTYPE1 [11 chars]");
   System.out.println("  -crval1 x    Value to use for CRVAL1 [float]");
   System.out.println("  -crval2 x    Value to use for CRVAL2 [float]");
   System.out.println("  -cdelt1 x    Value to use for CDELT1 [float]");
   System.out.println("  -cdelt2 x    Value to use for CDELT2 [float]");
   System.out.println("  -crota2 x    Value to use for CROTA2 [float]");
   System.out.println("");
   System.out.println("This is geom version " + Geom.VERSION);
   System.out.println("By Robert Narron & Rick Ebert");
   System.out.println("(C) 1993,1994 California Institute of Technology");


   System.exit(0);
}

static void get_params(String argv[])
{
   int argc = argv.length;
   int endswitches = 0;
   char switchchar;
   int index;

   geom.n_ref_name = false;
   geom.tie_skip = 10;
   geom.min_wgt =  0.5;
   geom.interp_flag= false;  /* use nearest neighbor pixel by default */
   geom.n_override_ctype1 = false;
   geom.n_override_naxis2 = false;
   geom.n_override_naxis1 = false;
   geom.n_override_cdelt1 = false;
   geom.n_override_cdelt2 = false;
   geom.n_override_crval1 = false;
   geom.n_override_crval2 = false;
   geom.n_override_crota2 = false;

   if(argc == 0) usage();

   index = 0;
   while (argv[index].charAt(0) == '-' && endswitches == 0){
      System.out.println("RBH char = " + argv[index].charAt(1) + 
	  "   argv[index] = " + argv[index]);
      switch (argv[index].charAt(1))
      {
	 case 'r':
	       ref_name = argv[index+1];
	       System.out.println("RBH ref_name = " + ref_name);
	       geom.n_ref_name = true;
	       index += 2; argc -=2;
	       break;
	 case 's':
	       geom.tie_skip = Integer.parseInt(argv[index+1]);
               if(geom.tie_skip < 1){
		 System.out.println(
		 "Error: -s " + geom.tie_skip + ": skipvalue must be >= 1\n");
                 usage();
               }
	       index += 2; argc -=2;
	       break;
	 case 'f': /* fraction of live pixels */
               geom.min_wgt = Double.valueOf(argv[index+1]).doubleValue();
	       index += 2; argc -=2;
	       break;
         case 'd': /* Cleaner user i/f = for special naxis1==0 case of old */
	       geom.override_naxis1 = 0;
	       geom.n_override_naxis1 = true;
	       index++; argc--;
	       break;
         case 't':
               geom.progress_info = true;
	       index++; argc--;
	       break;
	 case 'i': /* interpolation mode */
	       if(argv[index].charAt(2) == 'b') geom.interp_flag= true;
	       else if (argv[index].charAt(2) == 'n') geom.interp_flag= false;
	       else {
		 System.out.println("Unknown flag " +  argv[index]);
		 usage();
               }
	       index++; argc--;
	       break;
	 case 'n': /* -naxisn handling */
	       if (argv[index].equals("-naxis1"))
	       {
		   geom.override_naxis1 = Integer.parseInt(argv[index+1]);
		   geom.n_override_naxis1 = true;
		   if(geom.override_naxis1<=0 || geom.override_naxis1>32767)
		   {
		     System.out.println("Error: bad naxis1\n");
		     usage();
		   }
               }
	       else if (argv[index].equals("-naxis2"))
	       {
		   geom.override_naxis2 = Integer.parseInt(argv[index+1]);
		   geom.n_override_naxis2 = true;
		   if(geom.override_naxis2<=0 || geom.override_naxis2>32767)
		   {
		     System.out.println("Error: bad naxis2\n");
		     usage();
		   }
               }
	       else {
		 System.out.println("Unknown flag " + argv[index]);
		 usage();
               }
	       index += 2; argc -=2;
	       break;
	 case 'c': /* crval, ctype, cdelt, crota handling */
	       if (argv[index].equals("-ctype1"))
	       {
		   geom.override_ctype1 = argv[index+1];
		   geom.n_override_ctype1 = true;
               }
	       else if (argv[index].equals("-cdelt1"))
	       {
		   geom.override_cdelt1 = Double.valueOf(argv[index+1]).doubleValue();
		   geom.n_override_cdelt1 = true;
               }
	       else if (argv[index].equals("-cdelt2"))
	       {
		   geom.override_cdelt2 = Double.valueOf(argv[index+1]).doubleValue();
		   geom.n_override_cdelt2 = true;
               }
	       else if (argv[index].equals("-crval1"))
	       {
		   geom.override_crval1 = Double.valueOf(argv[index+1]).doubleValue();
		   geom.n_override_crval1 = true;
               }
	       else if (argv[index].equals("-crval2"))
	       {
		   geom.override_crval2 = Double.valueOf(argv[index+1]).doubleValue();
		   geom.n_override_crval2 = true;
               }
	       else if (argv[index].equals("-crota2"))
	       {
		   geom.override_crota2 = Double.valueOf(argv[index+1]).doubleValue();
		   geom.n_override_crota2 = true;
               }
	       else {
		 System.out.println("Unknown flag " + argv[index]);
		 usage();
               }
	       index += 2; argc -=2;
	       break;
	 case '-':
	       endswitches = 1;
	       break;
         default:
		 System.out.println("Unknown flag " + argv[index]);
	       usage();
	       break;
      }
   }
   if (argc != 2){
      System.out.println("Need source and destination file");
      usage();
   }
   in_name = argv[index];
   out_name = argv[index+1];
   System.out.println("RBH in_name = " + in_name + "   out_name = " + out_name);

}




    // main is for testing only
    public static void main(String[] args)
    {
	int i;
	Fits inFits = null;
	Fits refFits = null;
	Fits newFits;

/*	System.err.println("Enter a CR:");
        try
	{
	    int c = System.in.read();
	}
	catch (IOException e)
	{
	}*/


	geom = new Geom();
	get_params(args);

	System.out.println("GeomTestMain:  opening file in_name = " + in_name);
	try
	{
	    inFits = new Fits(in_name);
	}
	catch (FitsException e)
	{
	    System.out.println("got FitsException: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(1);
	}
	System.out.println("GeomTestMain:  done opening file in_name = " + in_name);


	if (geom.n_ref_name) 
	{
	    System.out.println("GeomTestMain:  opening ref_name = " + ref_name);
	    try
	    {
	    refFits = new Fits(ref_name);
	    }
	    catch (FitsException e)
	    {
		System.out.println("got FitsException: " + e.getMessage());
		e.printStackTrace();
		System.exit(1);
	    }
	    System.out.println("GeomTestMain:  done opening ref_name = " + ref_name);

	}





	//DEBUG

		geom.crpix1_base = 162.5;
		geom.crpix2_base = 485.5;
		geom.imageScaleFactor = 89;
		geom.need_crpix_adjusted = true;

	// END DEBUG 

	try
	{
	    System.out.println("GeomTestMain:  calling geom.do_geom");
	    newFits = geom.do_geom(inFits, refFits);
	    System.out.println("GeomTestMain:  back from geom.do_geom");
	    FileOutputStream fo = new java.io.FileOutputStream(out_name);
	    BufferedDataOutputStream o = new BufferedDataOutputStream(fo);
	    System.out.println("GeomTestMain:  writing output file  o = " + o);
	    newFits.write(o);
	    System.out.println("GeomTestMain:  done writing output file");
	}
	catch (FileNotFoundException e)
	{
	    System.out.println("got FileNotFoundException: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(1);
	}
	catch (FitsException e)
	{
	    System.out.println("got FitsException: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(1);
	}
	catch (GeomException e)
	{
	    System.out.println("got GeomException: " + e.getMessage() +
	    "  getRequestedNaxis1() = " + e.getRequestedNaxis1() +
	    "  getRequestedNaxis2() = " + e.getRequestedNaxis2());
	    e.printStackTrace();
	    System.exit(1);
	}
	catch (IOException e)
	{
	    System.out.println("got IOException: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(1);
	}
	catch (OutOfMemoryError e)
	{
	    System.out.println("got OutOfMemoryError");
	    e.printStackTrace();
	    System.exit(1);
	}
	
    }

}
