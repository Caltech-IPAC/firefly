/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.SUTDebug;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.util.ArrayFuncs;

import java.io.File;
import java.util.Arrays;


/**
 * ZSCALE -- Compute the optimal Z1, Z2 (range of greyscale values to be
 * displayed) of an image.  For efficiency a statistical subsample of an image
 * is used.  The pixel sample evenly subsamples the image in x and y.  The
 * entire image is used if the number of pixels in the image is smaller than
 * the desired sample.
 *
 * The sample is accumulated in a buffer and sorted by greyscale value.
 * The median value is the central value of the sorted array.  The slope of a
 * straight line fitted to the sorted sample is a measure of the standard
 * deviation of the sample about the median value.  Our algorithm is to sort
 * the sample and perform an iterative fit of a straight line to the sample,
 * using pixel rejection to omit gross deviants near the endpoints.  The fitted
 * straight line is the transfer function used to map image Z into display Z.
 * If more than half the pixels are rejected the full range is used.  The slope
 * of the fitted line is divided by the user-supplied contrast factor and the
 * final Z1 and Z2 are computed, taking the origin of the fitted line at the
 * median value.
 *
 * 8/3/15
 * Refactor it in the similar way as FitsRead (convert all data to float)
 */

public class Zscale
{

   static final int MIN_NPIXELS=	5;    /* smallest permissible sample 	     */
   static final float MAX_REJECT=	0.5F;  /* max frac. of pixels to be rejected   */
   static final int GOOD_PIXEL=	0;    /* use pixel in fit                     */
   static final int BAD_PIXEL=	1;    /* ignore pixel in all computations     */
   static final int REJECT_PIXEL=	2;    /* reject pixel after a bit             */
   static final float KREJ	=	2.5F;  /* k-sigma pixel rejection factor       */
   static final int MAX_ITERATIONS=5;    /* maximum number of fitline iterations */
   static final int INDEF=-999;    /* INDEF value flag             */


  /* CDL_ZSCALE -- Sample the image and compute optimal Z1 and Z2 values.
  */
  static ZscaleRetval
    cdl_zscale (
    float[] float1d,		/* image data to be sampled		*/
    int nx,
    int ny,			/* image dimensions			*/
    int bitpix,			/* bits per pixel			*/
    double contrast,		/* adj. to slope of transfer function	*/
    int opt_size,		/* desired number of pixels in sample	*/
    int len_stdline,		/* optimal number of pixels per line	*/
    double blank_value ){	/* blank value from FITS header         */



	if (SUTDebug.isDebug())
	    System.out.printf (
	    "[cdl_zscale] %dx%d-%d  contrast=%g opt_size=%d len_stdline=%d\n",
		nx, ny, bitpix, contrast, opt_size, len_stdline);

	/* Subsample the image. */
	SampleRetval sample_retval = sampleImage(float1d,  nx, ny,
	    opt_size, len_stdline, blank_value);
	// get sample from retval
	int npix = sample_retval.npix;
	//sample = sample_retval.sample;

	/* trim array to valid length (npix) */
	float[] sample = new float[npix];
	for (int i = 0; i < npix; i++){
	    sample[i] = sample_retval.sample[i];
	}

	/* Sort the sample, compute the minimum, maximum, and median pixel
	 * values.
	 */
	Arrays.sort(sample);

  /* Yi fixes a bug:

    If the array contains multiple elements with the specified value, as NaN, the method, Arrays.binarySearch, has no guarantee which
      one will be found. So do not use this one:

    npix = Arrays.binarySearch(sample, Float.NaN);

    */

      for (int i= 0; i < sample.length; i++){
          if (Float.isNaN(sample[i])){
              npix = i;
              break;
          }
      }

	//no NaN found, it returns a negative number thus, the npix should be the sample's length
	npix= npix>0? npix: sample.length  ;
	float zmin = sample[0];
	float zmax = sample[npix-1];

	/* The median value is the average of the two central values if there 
	 * are an even number of pixels in the sample.
	 */
	int center_pixel = Math.max (1, (npix + 1) / 2);
	int left = center_pixel - 1;


	float median = ((npix % 2) == 1 || center_pixel >= npix) ? sample[left]: (sample[left] + sample[left+1]) / 2;

	/* Fit a line to the sorted sample vector.  If more than half of the
	 * pixels in the sample are rejected give up and return the full range.
	 * If the user-supplied contrast factor is not 1.0 adjust the scale
	 * accordingly and compute Z1 and Z2, the y intercepts at indices 1 and
	 * npix.
	 */
	int minpix = Math.max (MIN_NPIXELS, (int) (npix * MAX_REJECT));
	int ngrow =  Math.max (1, (int) Math.round( (npix * .01)));
	FitLineRetval  fitLineRetval = fitLine (sample, npix,  KREJ, ngrow, MAX_ITERATIONS);
	int ngoodpix = fitLineRetval.ngoodpix;
	float zstart = fitLineRetval.zstart;
	float zslope = fitLineRetval.zslope;


	double z1 = Double.NaN;
	double z2 = Double.NaN;
	ZscaleRetval retval;
	if (fitLineRetval.ngoodpix < minpix) {
	    retval = new ZscaleRetval(zmin, zmax);
	} else {
	    if (contrast > 0)
		zslope = (float) (zslope / contrast);
	    z1 = Math.max (zmin, median - (center_pixel - 1) * zslope);
	    z2 = Math.min (zmax, median + (npix - center_pixel) * zslope);
	    retval = new ZscaleRetval(z1, z2);
	}


	if (SUTDebug.isDebug()) {
		System.out.println("ngoodpix = " + ngoodpix +
				"  zstart = " + zstart +
				"   zslope = " + zslope);
	    System.out.printf(
		"[cdl_zscale] zmin=%g zmax=%g sample[left]=%g median=%g\n",
		zmin, zmax, sample[left], median);
	    System.out.printf("[cdl_zscale] minpix=%d ngrow=%d ngoodpix=%d\n",
		minpix, ngrow, ngoodpix);
	    System.out.printf(
		"[cdl_zscale] zslope=%g center_pixel=%d z1=%g z2=%g\n",
		zslope, center_pixel, z1, z2);
	}

	return(retval);
}



  /** sampleImage -- Extract an evenly gridded subsample of the pixels from
  * a two-dimensional image into a one-dimensional vector.
  */
  private static SampleRetval sampleImage (
    float[] float1d,		        /* image to be sampled			*/
    int nx,
    int ny,			/* image dimensions			*/
    int optimal_size,		/* desired number of pixels in sample	*/
    int len_stdline,		/* optimal number of pixels per line	*/
    double blank_value){	/* BLANK value from FITS header		*/


	/* Compute the number of pixels each line will contribute to the sample,
	 * and the subsampling step size for a line.  The sampling grid must
	 * span the whole line on a uniform grid.
	 */

	int npix = 0;
	int optNpixPerLine = Math.max (1, Math.min (nx, len_stdline));
	int colStep = Math.max (2, (nx + optNpixPerLine-1) / optNpixPerLine);
	int npixPerLine = Math.max (1, (nx + colStep-1) / colStep);
	if (SUTDebug.isDebug())
	    System.out.printf (
	    "[sampleImage] opt_npix_per_line=%d col_step=%d npix_per_line=%d\n",
				optNpixPerLine, colStep, npixPerLine);

	/* Compute the number of lines to sample and the spacing between lines.
	 * We must ensure that the image is adequately sampled despite its
	 * size, hence there is a lower limit on the number of lines in the
	 * sample.  We also want to minimize the number of lines accessed when
	 * accessing a large image, because each disk seek and read is ex-
	 * pensive. The number of lines extracted will be roughly the sample
 	 * size divided by len_stdline, possibly more if the lines are very
 	 * short.
	 */
	int minNlinesInSample = Math.max (1, optimal_size / len_stdline);
	int optNlinesInSample = Math.max(minNlinesInSample, Math.min(ny,
	    (optimal_size + npixPerLine-1) / npixPerLine));
	int lineStep = Math.max (2, ny / (optNlinesInSample));
	int maxNlinesInSample = (ny + lineStep-1) / lineStep;
	if (SUTDebug.isDebug())
	    System.out.printf (
        "[sampleImage] min_nlines_in_sample=%d opt_nlines_in_sample=%d line_step=%d max_nlines_in_sample=%d\n",
		minNlinesInSample, optNlinesInSample, lineStep,
				maxNlinesInSample);

	/* Allocate space for the output vector.  Buffer must be freed by our
	 * caller.
	 */
	int maxpix = npixPerLine * maxNlinesInSample;


	float[] sample = new float[maxpix];
	int op = 0;
	for (int line = (lineStep + 1)/2; line < ny; line+=lineStep) {
	    /* Load a row of float values from the image */
		int ipixIndex = (line-1) * nx;
		float[] row= getDataSlice(ipixIndex, nx, float1d, blank_value );
		subSample (row, sample, op, npixPerLine, colStep);
		op += npixPerLine;
		npix += npixPerLine;
		if (npix > maxpix)
			break;
	}


	return new SampleRetval(npix, sample);
  }
	
  private static float[] getDataSlice(int start, int length, float[] data,double blank_value ){
	  int ipixIndex = start;
	  float[] row = new float[length];
	  for (int i=0; i < length; i++){
		  if ( data[ipixIndex] == blank_value){
			  row[i] = Float.NaN;
		  }
		  else {
			  row[i] = data[ipixIndex];

		  }
		  ipixIndex++;
	  }
	  return row;
  }

/** subSample -- Subsample an image line.  Extract the first pixel and
 * every "step"th pixel thereafter for a total of npix pixels.
 */

 private static void  subSample (float[] row,
     float[] sample, int op, int npix, int step){

	if (step <= 1)
	    System.arraycopy(row, 0, sample, op, npix);
	    //memmove (b, row, npix);
	else {
	    int ip = 0;
	    for (int i=0; i < npix; i++) {
		sample[op] = row[ip];
		ip += step;
		op ++;
	    }
	}
}


/** fitLine -- Fit a straight line to a data array of type real.  This is
 * an iterative fitting algorithm, wherein points further than ksigma from the
 * current fit are excluded from the next fit.  Convergence occurs when the
 * next iteration does not decrease the number of pixels in the fit, or when
 * there are no pixels left.  The number of pixels left after pixel rejection
 * is returned as the function value.
 */

  private static FitLineRetval fitLine (
    float[] data,		/* data to be fitted	  		  */
    int npix,			/* number of pixels before rejection	  */
    float krej,			/* k-sigma pixel rejection factor	  */
    int ngrow,			/* number of pixels of growing		  */
    int maxiter	) 	/* max iterations */   {


	double	xscale;
	if (npix <= 0)
	    return new FitLineRetval(1, 0.0F, 0.0F);
	else if (npix == 1) {
		    return new FitLineRetval(1, data[1], 0.0F);
	} else
	    xscale = 2.0 / (npix - 1);

	/* Allocate a buffer for data minus fitted curve, another for the
	 * normalized X values, and another to flag rejected pixels.
	 */

	byte[] badpix = new byte[npix];

	/* Compute normalized X vector.  The data X values [1:npix] are
	 * normalized to the range [-1:1].  This diagonalizes the lsq matrix
	 * and reduces its condition number.
	 */
	float[] normx = new float[npix];
	for (int i=0; i<npix; i++)
	    normx[i] = (float) (i * xscale - 1.0);

	/* Fit a line with no pixel rejection.  Accumulate the elements of the
	 * matrix and data vector.  The matrix M is diagonal with
	 * M[1,1] = sum x**2 and M[2,2] = ngoodpix.  The data vector is
	 * DV[1] = sum (data[i] * x[i]) and DV[2] = sum (data[i]).
	 */
	double sumxsqr = 0;
	double sumxz = 0;
	double sumx = 0;
	double sumz = 0;
	for (int i=0; i<npix; i++) {
	    double x = normx[i];
	    double z = data[i];
	    sumxsqr = sumxsqr + (x * x);
	    sumxz   = sumxz + z * x;
	    sumz    = sumz + z;
	}

	/* Solve for the coefficients of the fitted line. */
	double z0 = sumz / npix;
	double dz =  sumxz / sumxsqr;
	double o_dz=dz;

	/* Iterate, fitting a new line in each iteration.  Compute the flattened
	 * data vector and the sigma of the flat vector.  Compute the lower and
	 * upper k-sigma pixel rejection thresholds.  Run down the flat array
	 * and detect pixels to be rejected from the fit.  Reject pixels from
	 * the fit by subtracting their contributions from the matrix sums and
	 * marking the pixel as rejected.
	 */
	int ngoodpix = npix;
	int minpix = Math.max (MIN_NPIXELS, (int) (npix * MAX_REJECT));

	for (int niter=0;  niter < maxiter;  niter++) {
	   int  lastNgoodpix = ngoodpix;

	    /* Subtract the fitted line from the data array. */
		float[] flat = flattenData (data,  normx, npix, z0, dz);

	    /* Compute the k-sigma rejection threshold.  In principle this
	     * could be more efficiently computed using the matrix sums
	     * accumulated when the line was fitted, but there are problems with
	     * numerical stability with that approach.
	     */
	    ComputeSigmaRetval compute_sigma_retval = computeSigma (flat, badpix, npix);
	    ngoodpix = compute_sigma_retval.ngoodpix;
	    double mean = compute_sigma_retval.mean;
	    double sigma = compute_sigma_retval.sigma;

	    double threshold = sigma * krej;

	    /* Detect and reject pixels further than ksigma from the fitted
	     * line.
	     */
	    RejectPixelsRetval reject_pixels_retval = rejectPixels (data, flat, normx,
		badpix, npix, sumxsqr, sumxz, sumx, sumz, threshold,
		ngrow);
	    ngoodpix = reject_pixels_retval.ngoodpix;
	    sumxsqr = reject_pixels_retval.sumxsqr;
	    sumxz = reject_pixels_retval.sumxz;
	    sumx = reject_pixels_retval.sumx;
	    sumz = reject_pixels_retval.sumz;

	    /* Solve for the coefficients of the fitted line.  Note that after
	     * pixel rejection the sum of the X values need no longer be zero.
	     */
	    if (ngoodpix > 0) {
		   double rowrat = sumx / sumxsqr;
		   z0 = (sumz - rowrat * sumxz) / (ngoodpix - rowrat * sumx);
		   dz = (sumxz - z0 * sumx) / sumxsqr;
	    }

	    if (ngoodpix >= lastNgoodpix || ngoodpix < minpix)
		break;
	}

	/* Transform the line coefficients back to the X range [1:npix]. */
	float zstart = (float) (z0 - dz);
	float  zslope = (float) (dz * xscale);
        if (Math.abs(zslope) < 0.001)
            zslope = (float) (o_dz * xscale);


	return new FitLineRetval(ngoodpix, zstart, zslope);
}


/** flattenData -- Compute and subtract the fitted line from the data array,
 * returned the flattened data in FLAT.
 */


private static float[]
  flattenData (
    float[] data,		/* raw data array			*/
    float x[],			/* x value of each pixel		*/
    int npix,			/* number of pixels			*/
    double z0,
    double dz	)	{	/* z-intercept, dz/dx of fitted line	*/

	float[] flat = new float[npix]	;	/* flattened data  (output)		*/
	for (int i=0; i < npix; i++)
	    flat[i] = (float) (data[i] - (x[i] * dz + z0));

	return flat;
}


/** computeSigma -- Compute the root mean square deviation from the
 * mean of a flattened array.  Ignore rejected pixels.
 */


private static ComputeSigmaRetval
computeSigma (
    float[] a,			/* flattened data array			*/
    byte[] badpix,		/* bad pixel flags (!= 0 if bad pixel)	*/
    int npix) {

	int	ngoodpix = 0;
	double	sum = 0.0, sumsq = 0.0, temp;
	/* Accumulate sum and sum of squares. */
	for (int i=0; i < npix; i++)
	    if (badpix[i] == GOOD_PIXEL) {
		float pixval = a[i];
		ngoodpix = ngoodpix + 1;
		sum = sum + pixval;
		sumsq = sumsq + pixval * pixval;
	}

	float mean, sigma;

	/* Compute mean and sigma. */
	switch (ngoodpix) {
	case 0:
	    mean = INDEF;
	    sigma = INDEF;
	    break;
	case 1:
	    mean = (float) sum;
	    sigma = INDEF;
	    break;
	default:
	    mean = (float) (sum / (double) ngoodpix);
	    temp = sumsq / (double) (ngoodpix-1) -
			(sum*sum) / (double) (ngoodpix*(ngoodpix - 1));
	    if (temp < 0)		/* possible with roundoff error */
		sigma = 0.0F;
	    else
		sigma = (float) Math.sqrt (temp);
	}

	return new ComputeSigmaRetval(ngoodpix, mean, sigma);
}


/** rejectPixels -- Detect and reject pixels more than "threshold" greyscale
 * units from the fitted line.  The residuals about the fitted line are given
 * by the "flat" array, while the raw data is in "data".  Each time a pixel
 * is rejected subtract its contributions from the matrix sums and flag the
 * pixel as rejected.  When a pixel is rejected reject its neighbors out to
 * a specified radius as well.  This speeds up convergence considerably and
 * produces a more stringent rejection criteria which takes advantage of the
 * fact that bad pixels tend to be clumped.  The number of pixels left in the
 * fit is returned as the function value.
 */

private static RejectPixelsRetval
rejectPixels (
    float data[],		/* raw data array			*/
    float flat[],		/* flattened data array			*/
    float normx[],		/* normalized x values of pixels	*/
    byte  badpix[],		/* bad pixel flags (!= 0 if bad pixel)	*/
    int   npix,
    double sumxsqr,
    double sumxz,
    double sumx,
    double sumz,               /* matrix sums				*/
    double threshold,		/* threshold for pixel rejection	*/
    int ngrow			/* number of pixels of growing		*/
 )
 {

	int ngoodpix = npix;
	float lcut = (float) -threshold;
	float hcut = (float) threshold;

	for (int i=0; i < npix; i++) {
	    if (badpix[i] == BAD_PIXEL)
		ngoodpix = ngoodpix - 1;
	    else {
		float residual = flat[i];
		if (residual < lcut || residual > hcut) {
		    /* Reject the pixel and its neighbors out to the growing
		     * radius.  We must be careful how we do this to avoid
		     * directional effects.  Do not turn off thresholding on
		     * pixels in the forward direction; mark them for rejection
		     * but do not reject until they have been thresholded.
		     * If this is not done growing will not be symmetric.
		     */
		    for (int j=Math.max(0,i-ngrow); j < Math.min(npix,i+ngrow); j++) {
			if (badpix[j] != BAD_PIXEL) {
			    if (j <= i) {
				double x = normx[j];
				double z =  data[j];
				sumxsqr = sumxsqr - (x * x);
				sumxz = sumxz - z * x;
				sumx = sumx - x;
				sumz = sumz - z;
				badpix[j] = BAD_PIXEL;
				ngoodpix = ngoodpix - 1;
			    } else
				badpix[j] = REJECT_PIXEL;
			}
		    }
		}
	    }
	}

	return new RejectPixelsRetval(ngoodpix, sumxsqr, sumxz, sumx, sumz);
}



/*
static int 
floatCompare (float *i, float *j)
{
        return ((*i <= *j) ? -1 : 1);
}
*/


public static void main(String args[])
{
    if (args.length != 1)
    {
	System.out.println("usage: java Zscale <fits_filename>");
	System.exit(1);
    }

    File file = new File(args[0]);
    Fits fits = null;
    Header header = null;
    Object pixel_data = null;
    double contrast;
    int opt_size;
    int len_stdline;
    BasicHDU[] myHDUs = null;
    float[]  onedimdata;
    try
    {
	fits = new Fits(file);   //open the file
	myHDUs = fits.read();   // get all of the header-data units
					   // usually just one primary HDU
	pixel_data = myHDUs[0].getData().getData();
    }
    catch (FitsException e)
    {
	e.printStackTrace();
    }
	header = myHDUs[0].getHeader();    // get the header
	int bitpix = header.getIntValue("BITPIX");
	int naxis = header.getIntValue("NAXIS");
	int naxis1 = header.getIntValue("NAXIS1");
	int naxis2 = header.getIntValue("NAXIS2");
	double blank_value = header.getDoubleValue("BLANK", Double.NaN);

    contrast = 0.25;
    opt_size = 600;    /* desired number of pixels in sample   */
    len_stdline = 120;  /* optimal number of pixels per line    */

    onedimdata = (float[]) ArrayFuncs.flatten( ArrayFuncs.convertArray( pixel_data, Float.TYPE));

    ZscaleRetval zscale_retval = cdl_zscale(onedimdata, naxis1, naxis2,
	bitpix, contrast, opt_size, len_stdline, blank_value);
    double z1 = zscale_retval.getZ1();
    double z2 = zscale_retval.getZ2();
    System.out.println("z1 = " + z1 + "  z2 = " + z2);
}

private static class SampleRetval
{
    int npix;
    float sample[];
    SampleRetval(int _npix, float _sample[])
    {
	npix = _npix;
	sample = _sample;
    }

}

private static class RejectPixelsRetval
{
    int ngoodpix;
    double sumxsqr;
    double sumxz;
    double sumx;
    double sumz;               /* matrix sums				*/
    RejectPixelsRetval(int _ngoodpix, double _sumxsqr, double _sumxz,
    double _sumx, double _sumz)
    {
	ngoodpix = _ngoodpix;
	sumxsqr = _sumxsqr;
	sumxz = _sumxz;
	sumx = _sumx;
	sumz = _sumz;
    }
}

private static class ComputeSigmaRetval
{
    int ngoodpix;
    float mean;
    float sigma;
    ComputeSigmaRetval(int _ngoodpix, float _mean, float _sigma)
    {
	ngoodpix = _ngoodpix;
	mean = _mean;
	sigma = _sigma;
    }
}

private static class FitLineRetval
{
    int ngoodpix;
    float zstart;
    float zslope;
    FitLineRetval(int _ngoodpix, float _zstart, float _zslope)
    {
	ngoodpix = _ngoodpix;
	zstart = _zstart;
	zslope = _zslope;
    }
}

public static class ZscaleRetval
{
    private double z1;
    private double  z2;
    public ZscaleRetval(double _z1, double _z2)
    {
	z1 = _z1;
	z2 = _z2;
    }
    public double getZ1()
    {
	return z1;
    }
    public double getZ2()
    {
	return z2;
    }
}
}
