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
import java.util.Collections;
import java.util.List;


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
    Object im,		/* image data to be sampled		*/
    int nx,
    int ny,			/* image dimensions			*/
    int bitpix,			/* bits per pixel			*/
    double contrast,		/* adj. to slope of transfer function	*/
    int opt_size,		/* desired number of pixels in sample	*/
    int len_stdline,		/* optimal number of pixels per line	*/
    double blank_value,		/* blank value from FITS header         */
    double bscale,		
    double bzero	
)
{
	double z1 = Double.NaN;
	double z2 = Double.NaN;
	int  npix, minpix, ngoodpix, center_pixel, ngrow;
	float	zmin, zmax, median;
	float	zstart, zslope;
	//float 	*sample = NULL, *left = NULL;
	int left;
	Float sample[];


	if (SUTDebug.isDebug())
	    System.out.printf (
	    "[cdl_zscale] %dx%d-%d  contrast=%g opt_size=%d len_stdline=%d\n",
		nx, ny, bitpix, contrast, opt_size, len_stdline);

	/* Subsample the image. */
	SampleRetval sample_retval = sampleImage(im, bitpix, nx, ny,
	    opt_size, len_stdline, blank_value, bscale, bzero);
	// get sample from retval
	npix = sample_retval.npix;
	//sample = sample_retval.sample;

	/* trim array to valid length (npix) */
	sample = new Float[npix];
	for (int i = 0; i < npix; i++)
	{
	    sample[i] = sample_retval.sample[i];
	}
	List<Float> list = Arrays.asList(sample);


	/* Sort the sample, compute the minimum, maximum, and median pixel
	 * values.
	 */
	//System.out.println("npix = " + npix + "  sample.length = " + 
	//    sample.length);
	//qsort (sample, npix, sizeof (float), floatCompare);
	Collections.sort(list);

	for (int i = 0; i < npix; i++)
	{
	    //System.out.println("RBH sample[" + i + "] = " + sample[i]);
	    if (sample[i].isNaN())
	    {
		/* throw out all Nan's,  i.e. BLANK pixels */
		npix = i;
		break;
	    }
	}

	zmin = sample[0];
	zmax = sample[npix-1];

	/* The median value is the average of the two central values if there 
	 * are an even number of pixels in the sample.
	 */
	center_pixel = Math.max (1, (npix + 1) / 2);
	left = center_pixel - 1;
	if ((npix % 2) == 1 || center_pixel >= npix)
	    median = sample[left];
	else
	    median = (sample[left] + sample[left+1]) / 2;

	/* Fit a line to the sorted sample vector.  If more than half of the
	 * pixels in the sample are rejected give up and return the full range.
	 * If the user-supplied contrast factor is not 1.0 adjust the scale
	 * accordingly and compute Z1 and Z2, the y intercepts at indices 1 and
	 * npix.
	 */
	minpix = Math.max (MIN_NPIXELS, (int) (npix * MAX_REJECT));
	ngrow =  Math.max (1, (int) Math.round( (npix * .01)));
	FitLineRetval fit_line_retval = fitLine (sample, npix, 
	    KREJ, ngrow, MAX_ITERATIONS);
	ngoodpix = fit_line_retval.ngoodpix;
	zstart = fit_line_retval.zstart;
	zslope = fit_line_retval.zslope;
	if (SUTDebug.isDebug())
	{
	System.out.println("ngoodpix = " + ngoodpix +
	    "  zstart = " + zstart +
	    "   zslope = " + zslope);
	}

	ZscaleRetval retval;
	if (ngoodpix < minpix) {
	    retval = new ZscaleRetval(zmin, zmax);
	} else {
	    if (contrast > 0)
		zslope = (float) (zslope / contrast);
	    z1 = Math.max (zmin, median - (center_pixel - 1) * zslope);
	    z2 = Math.min (zmax, median + (npix - center_pixel) * zslope);
	    retval = new ZscaleRetval(z1, z2);
	}

	if (SUTDebug.isDebug()) {
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
private static SampleRetval
sampleImage (
    Object im,		        /* image to be sampled			*/
    int bitpix,			/* bits per pixel in image		*/
    int nx,
    int ny,			/* image dimensions			*/
    int optimal_size,		/* desired number of pixels in sample	*/
    int len_stdline,		/* optimal number of pixels per line	*/
    double blank_value, 		/* BLANK value from FITS header		*/
    double bscale,
    double bzero 
)
{
	int i;
	int ncols, nlines, col_step, line_step, maxpix, line;
	int opt_npix_per_line, npix_per_line, npix = 0;
	int opt_nlines_in_sample, min_nlines_in_sample, max_nlines_in_sample;
        //int     *ipix = NULL;
        //float   *fpix = NULL;
        //double  *dpix = NULL;
        //short   *spix = NULL;
        //unsigned char    *bpix = NULL;
	float sample[];		/* output vector containing the sample	*/


	ncols  = nx;
	nlines = ny;

	/* Compute the number of pixels each line will contribute to the sample,
	 * and the subsampling step size for a line.  The sampling grid must
	 * span the whole line on a uniform grid.
	 */
	opt_npix_per_line = Math.max (1, Math.min (ncols, len_stdline));
	col_step = Math.max (2, (ncols + opt_npix_per_line-1) / opt_npix_per_line);
	npix_per_line = Math.max (1, (ncols + col_step-1) / col_step);
	if (SUTDebug.isDebug())
	    System.out.printf (
	    "[sampleImage] opt_npix_per_line=%d col_step=%d npix_per_line=%d\n",
		opt_npix_per_line, col_step, npix_per_line);

	/* Compute the number of lines to sample and the spacing between lines.
	 * We must ensure that the image is adequately sampled despite its
	 * size, hence there is a lower limit on the number of lines in the
	 * sample.  We also want to minimize the number of lines accessed when
	 * accessing a large image, because each disk seek and read is ex-
	 * pensive. The number of lines extracted will be roughly the sample
 	 * size divided by len_stdline, possibly more if the lines are very
 	 * short.
	 */
	min_nlines_in_sample = Math.max (1, optimal_size / len_stdline);
	opt_nlines_in_sample = Math.max(min_nlines_in_sample, Math.min(nlines,
	    (optimal_size + npix_per_line-1) / npix_per_line));
	line_step = Math.max (2, nlines / (opt_nlines_in_sample));
	max_nlines_in_sample = (nlines + line_step-1) / line_step;
	if (SUTDebug.isDebug())
	    System.out.printf (
"[sampleImage] min_nlines_in_sample=%d opt_nlines_in_sample=%d line_step=%d max_nlines_in_sample=%d\n",
		min_nlines_in_sample, opt_nlines_in_sample, line_step,
		max_nlines_in_sample);

	/* Allocate space for the output vector.  Buffer must be freed by our
	 * caller.
	 */
	maxpix = npix_per_line * max_nlines_in_sample;
	//*sample = (float *) malloc (maxpix * sizeof (float));
	sample = new float[maxpix];
	//row = (float *) malloc (nx * sizeof (float));
	float[] row = new float[nx];

	/* Extract the vector. */
	int op = 0;
	for (line = (line_step + 1)/2; line < nlines; line+=line_step) {
	    /* Load a row of float values from the image */
		int ipix[] = (int []) im;
		int ipix_index = (line-1) * nx;
		for (i=0; i < nx; i++)
		{
			if (((float) ipix[ipix_index]) == blank_value)
			{
				row[i] = Float.NaN;
			}
			else
			{
				row[i] = (float) (ipix[ipix_index] );
				//row[i] = (float) (ipix[ipix_index] * bscale + bzero);
			}
			ipix_index++;
		}
//		switch (bitpix) {
//			case 8:
//				//byte bpix[] = new byte[npix];
//				byte bpix[] = (byte[]) im;
//				int bpix_index = (line-1) * nx;
//				for (i=0; i < nx; i++)
//				{
//					if ((float) (bpix[bpix_index] & 0xff) == blank_value)
//					{
//						row[i] = Float.NaN;
//					}
//					else
//					{
//						row[i] = (float) ((bpix[bpix_index] & 0xff) );
//						//row[i] = (float) ((bpix[bpix_index] & 0xff) * bscale +
//						//    bzero);
//					}
//					bpix_index++;
//					//System.out.println("row[" + i + "] = " + row[i]);
//				}
//				break;
//			case 16:
//				//spix = (short *) &im[(line-1) * nx * sizeof(short)];
//				short spix[] = (short[]) im;
//				int spix_index = (line-1) * nx;
//				for (i=0; i < nx; i++)
//				{
//					if (((double) spix[spix_index]) == blank_value)
//					{
//						row[i] = Float.NaN;
//					}
//					else
//					{
//						row[i] = (float) (spix[spix_index] );
//						//row[i] = (float) (spix[spix_index] * bscale + bzero);
//					}
//					spix_index++;
//					//System.out.println("row[" + i + "] = " + row[i]);
//				}
//				break;
//			case 32:
//				//ipix = (int *) &im[(line-1) * nx * sizeof(int)];
//				int ipix[] = (int []) im;
//				int ipix_index = (line-1) * nx;
//				for (i=0; i < nx; i++)
//				{
//					if (((float) ipix[ipix_index]) == blank_value)
//					{
//						row[i] = Float.NaN;
//					}
//					else
//					{
//						row[i] = (float) (ipix[ipix_index] );
//						//row[i] = (float) (ipix[ipix_index] * bscale + bzero);
//					}
//					ipix_index++;
//				}
//				break;
//			case -32:
//				//fpix = (float *) &im[(line-1) * nx * sizeof(float)];
//				float fpix[] = (float []) im;
//				int fpix_index = (line-1) * nx;
//				for (i=0; i < nx; i++)
//				{
//					row[i] = fpix[fpix_index];
//					fpix_index++;
//				}
//				break;
//			case -64:
//				//dpix = (double *) &im[(line-1) * nx * sizeof(double)];
//				double dpix[] = (double []) im;
//				int dpix_index = (line-1) * nx;
//				for (i=0; i < nx; i++)
//				{
//					row[i] = (new Double(dpix[dpix_index])).floatValue();
//					dpix_index++;
//				}
//				break;
//		}

		subSample (row, sample, op, npix_per_line, col_step);
		op += npix_per_line;
		npix += npix_per_line;
		if (npix > maxpix)
			break;
	}

	//free ((float *)row);
	return new SampleRetval(npix, sample);
}


/** subSample -- Subsample an image line.  Extract the first pixel and
 * every "step"th pixel thereafter for a total of npix pixels.
 */

private static void 
subSample (float row[], float sample[], int op, int npix, int step)
{
	int ip, i;

	if (step <= 1)
	    System.arraycopy(row, 0, sample, op, npix);
	    //memmove (b, row, npix);
	else {
	    ip = 0;
	    for (i=0; i < npix; i++) {
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

private static FitLineRetval
fitLine (
    Float data[],		/* data to be fitted	  		  */
    int npix,			/* number of pixels before rejection	  */
    float krej,			/* k-sigma pixel rejection factor	  */
    int ngrow,			/* number of pixels of growing		  */
    int maxiter			/* max iterations			  */
)
{
	int	i, ngoodpix, last_ngoodpix, minpix, niter;
	double	xscale, z0, dz, o_dz, x, z, mean, sigma, threshold;
	double	sumxsqr, sumxz, sumz, sumx, rowrat;
	float zstart;
	float zslope;
	//float 	*flat, *normx;
	//char	*badpix;

	if (npix <= 0)
	    return new FitLineRetval(1, 0.0F, 0.0F);
	else if (npix == 1) {
	    zstart = data[1];
	    zslope = 0.0F;
	    return new FitLineRetval(1, zstart, zslope);
	} else
	    xscale = 2.0 / (npix - 1);

	/* Allocate a buffer for data minus fitted curve, another for the
	 * normalized X values, and another to flag rejected pixels.
	 */
	//flat = (float *) malloc (npix * sizeof (float));
	//normx = (float *) malloc (npix * sizeof (float));
	//badpix = (char *) calloc (npix, sizeof(char));
	float[] flat = new float[npix];
	float[] normx = new float[npix];
	byte[] badpix = new byte[npix];

	/* Compute normalized X vector.  The data X values [1:npix] are
	 * normalized to the range [-1:1].  This diagonalizes the lsq matrix
	 * and reduces its condition number.
	 */
	for (i=0; i<npix; i++)
	    normx[i] = (float) (i * xscale - 1.0);

	/* Fit a line with no pixel rejection.  Accumulate the elements of the
	 * matrix and data vector.  The matrix M is diagonal with
	 * M[1,1] = sum x**2 and M[2,2] = ngoodpix.  The data vector is
	 * DV[1] = sum (data[i] * x[i]) and DV[2] = sum (data[i]).
	 */
	sumxsqr = 0;
	sumxz = 0;
	sumx = 0;
	sumz = 0;

	for (i=0; i<npix; i++) {
	    x = normx[i];
	    z = data[i];
	    sumxsqr = sumxsqr + (x * x);
	    sumxz   = sumxz + z * x;
	    sumz    = sumz + z;
	}

	/* Solve for the coefficients of the fitted line. */
	z0 = sumz / npix;
	dz = o_dz = sumxz / sumxsqr;

	/* Iterate, fitting a new line in each iteration.  Compute the flattened
	 * data vector and the sigma of the flat vector.  Compute the lower and
	 * upper k-sigma pixel rejection thresholds.  Run down the flat array
	 * and detect pixels to be rejected from the fit.  Reject pixels from
	 * the fit by subtracting their contributions from the matrix sums and
	 * marking the pixel as rejected.
	 */
	ngoodpix = npix;
	minpix = Math.max (MIN_NPIXELS, (int) (npix * MAX_REJECT));

	for (niter=0;  niter < maxiter;  niter++) {
	    last_ngoodpix = ngoodpix;

	    /* Subtract the fitted line from the data array. */
	    flattenData (data, flat, normx, npix, z0, dz);

	    /* Compute the k-sigma rejection threshold.  In principle this
	     * could be more efficiently computed using the matrix sums
	     * accumulated when the line was fitted, but there are problems with
	     * numerical stability with that approach.
	     */
	    ComputeSigmaRetval compute_sigma_retval = computeSigma (flat, badpix, npix);
	    ngoodpix = compute_sigma_retval.ngoodpix;
	    mean = compute_sigma_retval.mean;
	    sigma = compute_sigma_retval.sigma;

	    threshold = sigma * krej;

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
		rowrat = sumx / sumxsqr;
		z0 = (sumz - rowrat * sumxz) / (ngoodpix - rowrat * sumx);
		dz = (sumxz - z0 * sumx) / sumxsqr;
	    }

	    if (ngoodpix >= last_ngoodpix || ngoodpix < minpix)
		break;
	}

	/* Transform the line coefficients back to the X range [1:npix]. */
	zstart = (float) (z0 - dz);
	zslope = (float) (dz * xscale);
        if (Math.abs(zslope) < 0.001)
            zslope = (float) (o_dz * xscale);

	//free ((float *)flat);
	//free ((float *)normx);
	//free ((char *)badpix);
	return new FitLineRetval(ngoodpix, zstart, zslope);
}


/** flattenData -- Compute and subtract the fitted line from the data array,
 * returned the flattened data in FLAT.
 */


private static void 
flattenData (
    Float data[],		/* raw data array			*/
    float flat[],		/* flattened data  (output)		*/
    float x[],			/* x value of each pixel		*/
    int npix,			/* number of pixels			*/
    double z0,
    double dz			/* z-intercept, dz/dx of fitted line	*/
)
{
	int i;

	for (i=0; i < npix; i++) 
	    flat[i] = (float) (data[i] - (x[i] * dz + z0));
}


/** computeSigma -- Compute the root mean square deviation from the
 * mean of a flattened array.  Ignore rejected pixels.
 */


private static ComputeSigmaRetval
computeSigma (
    float a[],			/* flattened data array			*/
    byte badpix[],		/* bad pixel flags (!= 0 if bad pixel)	*/
    int npix)
{
	float mean, sigma;
	float	pixval;
	int	i, ngoodpix = 0;
	double	sum = 0.0, sumsq = 0.0, temp;

	/* Accumulate sum and sum of squares. */
	for (i=0; i < npix; i++)
	    if (badpix[i] == GOOD_PIXEL) {
		pixval = a[i];
		ngoodpix = ngoodpix + 1;
		sum = sum + pixval;
		sumsq = sumsq + pixval * pixval;
	    }

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
    Float data[],		/* raw data array			*/
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
	int	ngoodpix, i, j;
	float	residual, lcut, hcut;
	double	x, z;

	ngoodpix = npix;
	lcut = (float) -threshold;
	hcut = (float) threshold;

	for (i=0; i < npix; i++) {
	    if (badpix[i] == BAD_PIXEL)
		ngoodpix = ngoodpix - 1;
	    else {
		residual = flat[i];
		if (residual < lcut || residual > hcut) {
		    /* Reject the pixel and its neighbors out to the growing
		     * radius.  We must be careful how we do this to avoid
		     * directional effects.  Do not turn off thresholding on
		     * pixels in the forward direction; mark them for rejection
		     * but do not reject until they have been thresholded.
		     * If this is not done growing will not be symmetric.
		     */
		    for (j=Math.max(0,i-ngrow); j < Math.min(npix,i+ngrow); j++) {
			if (badpix[j] != BAD_PIXEL) {
			    if (j <= i) {
				x = (double) normx[j];
				z = (double) data[j];
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
    Object onedimdata;
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
	double bscale = header.getDoubleValue("BSCALE", 0.0);
	double bzero = header.getDoubleValue("BZERO", 0.0);

    //contrast = 1.0;
    //opt_size = 100;    /* desired number of pixels in sample   */
    //len_stdline = 10;  /* optimal number of pixels per line    */
    contrast = 0.25;
    opt_size = 600;    /* desired number of pixels in sample   */
    len_stdline = 120;  /* optimal number of pixels per line    */

    onedimdata =  ArrayFuncs.flatten(pixel_data); 

    /*
    for (int i = 0; i < 500; i++)
    {
    int pixval = onedimdata8[i] & 0xff;
    System.out.println("i = " + i + "  pixval = " + pixval);
    }
    */

    ZscaleRetval zscale_retval = cdl_zscale(onedimdata, naxis1, naxis2, 
	bitpix, contrast, opt_size, len_stdline, blank_value, bscale, bzero);
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
