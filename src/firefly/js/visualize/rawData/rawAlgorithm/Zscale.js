/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

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



const MIN_NPIXELS=	5;    /* smallest permissible sample 	     */
const MAX_REJECT=	0.5;  /* max frac. of pixels to be rejected   */
const GOOD_PIXEL=	0;    /* use pixel in fit                     */
const BAD_PIXEL=	1;    /* ignore pixel in all computations     */
const REJECT_PIXEL=	2;    /* reject pixel after a bit             */
const KREJ	=	2.5;  /* k-sigma pixel rejection factor       */
const MAX_ITERATIONS=5;    /* maximum number of fitline iterations */
const INDEF=-999;    /* INDEF value flag             */


/**
 * CDL_ZSCALE -- Sample the image and compute optimal Z1 and Z2 values.
 *
 * @param float1d image data to be sampled
 * @param nx image dimensions
 * @param ny
 * @param bitpix bits per pixel
 * @param contrast adj. to slope of transfer function
 * @param opt_size desired number of pixels in sample
 * @param len_stdline optimal number of pixels per line
 * @param blank_value blank value from FITS header
 * @return {{z1:number,z2:number}}
 */
export function cdl_zscale ( float1d, nx, ny, bitpix, contrast, opt_size, len_stdline, blank_value ){

	/* Subsample the image. */
	let {npix,sample:sampleRet} = sampleImage(float1d,  nx, ny, opt_size, len_stdline, blank_value);

	/* trim array to valid length (npix) */
	const sample = new Float32Array(npix);
	for (let i = 0; i < npix; i++) sample[i] = sampleRet[i];


	/* Sort the sample, compute the minimum, maximum, and median pixel
	 * values.
	 */
	
	sample.sort( (i,j) => i-j);

  /* Yi fixes a bug:

    If the array contains multiple elements with the specified value, as NaN, the method, Arrays.binarySearch, has no guarantee which
      one will be found. So do not use this one:

    npix = Arrays.binarySearch(sample, Float.NaN);

    */

      for (let i= 0; i < sample.length; i++){
          if (isNaN(sample[i])){
              npix = i;
              break;
          }
      }

	//no NaN found, it returns a negative number thus, the npix should be the sample's length
	npix= npix>0? npix: sample.length;
	const zmin = sample[0];
	const zmax = sample[npix-1];

	/* The median value is the average of the two central values if there
	 * are an even number of pixels in the sample.
	 */
	const center_pixel = Math.max (1, Math.trunc((npix + 1) / 2));
	const left = center_pixel - 1;


	const median = ((npix % 2)===1 || center_pixel >= npix) ? sample[left]: (sample[left] + sample[left+1]) / 2;

	/* Fit a line to the sorted sample vector.  If more than half of the
	 * pixels in the sample are rejected give up and return the full range.
	 * If the user-supplied contrast factor is not 1.0 adjust the scale
	 * accordingly and compute Z1 and Z2, the y intercepts at indices 1 and
	 * npix.
	 */
	const minpix = Math.max (MIN_NPIXELS, Math.trunc(npix * MAX_REJECT));
	const ngrow =  Math.max (1, Math.round( (npix * .01)));
	let {ngoodpix, zstart, zslope} = fitLine(sample, npix,  KREJ, ngrow, MAX_ITERATIONS);

	if (ngoodpix < minpix) {
	    return {z1:zmin, z2:zmax};
	} else {
	    const zSlopeToUse= (contrast > 0) ? (zslope / contrast) : zslope;
		return {
			z1: Math.max (zmin, median - (center_pixel - 1) * zSlopeToUse),
			z2: Math.min (zmax, median + (npix - center_pixel) * zSlopeToUse)
		};
	}
}



/** sampleImage -- Extract an evenly gridded subsample of the pixels from
 * a two-dimensional image into a one-dimensional vector.
 *
 * @param float1d
 * @param nx
 * @param ny
 * @param optimal_size
 * @param len_stdline
 * @param blank_value
 * @return {{npix,sample}}
 */
function sampleImage (float1d, nx, ny,	optimal_size, len_stdline, blank_value) {


	 // Compute the number of pixels each line will contribute to the sample,
	 // and the subsampling step size for a line.  The sampling grid must
	 // span the whole line on a uniform grid.

	let npix = 0;
	const optNpixPerLine = Math.max (1, Math.min (nx, len_stdline));
	const colStep = Math.max (2, Math.trunc((nx + optNpixPerLine-1) / optNpixPerLine));
	const npixPerLine = Math.max (1, Math.trunc((nx + colStep-1) / colStep));

	/* Compute the number of lines to sample and the spacing between lines.
	 * We must ensure that the image is adequately sampled despite its
	 * size, hence there is a lower limit on the number of lines in the
	 * sample.  We also want to minimize the number of lines accessed when
	 * accessing a large image, because each disk seek and read is ex-
	 * pensive. The number of lines extracted will be roughly the sample
 	 * size divided by len_stdline, possibly more if the lines are very
 	 * short.
	 */
	const minNlinesInSample = Math.max (1, Math.trunc(optimal_size / len_stdline));
	const optNlinesInSample = Math.max(minNlinesInSample, Math.min(ny, Math.trunc((optimal_size + npixPerLine-1) / npixPerLine)));
	const lineStep = Math.max (2, Math.trunc(ny / (optNlinesInSample)));
	const maxNlinesInSample = (ny + lineStep-1) / lineStep;

	 //Allocate space for the output vector.  Buffer must be freed by our caller.
	const maxpix = npixPerLine * maxNlinesInSample;

	const sample = new Float32Array(maxpix);
	let op = 0;
	for (let line = Math.trunc((lineStep + 1)/2); (line < ny); line+=lineStep) {
	    // Load a row of float values from the image
		const ipixIndex = (line-1) * nx;
		const row= getDataSlice(ipixIndex, nx, float1d, blank_value );
		subSample (row, sample, op, npixPerLine, colStep);
		op += npixPerLine;
		npix += npixPerLine;
		if (npix > maxpix) break;
	}
	return {npix, sample};
}

function getDataSlice(start, length, data,blank_value ){
	let ipixIndex = start;
	const row = new Float32Array(length);
	for (let i=0; i < length; i++){
		row[i]= data[ipixIndex]===blank_value ? NaN : data[ipixIndex];
		ipixIndex++;
	}
	return row;
}

/** subSample -- Subsample an image line.  Extract the first pixel and
 * every "step"th pixel thereafter for a total of npix pixels.
 */

function subSample (row, sample, op, npix, step){
    if (step<1) step= 1;
	let ip = 0;
	for (let i=0; i < npix; i++) {
		sample[op] = row[ip];
		ip += step;
		op ++;
	}
}


/** fitLine -- Fit a straight line to a data array of type real.  This is
 * an iterative fitting algorithm, wherein points further than ksigma from the
 * current fit are excluded from the next fit.  Convergence occurs when the
 * next iteration does not decrease the number of pixels in the fit, or when
 * there are no pixels left.  The number of pixels left after pixel rejection
 * is returned as the function value.
 *
 * @param {Float32Array} data
 * @param npix
 * @param krej
 * @param ngrow
 * @param maxiter
 * @return {{zstart: number, zslope: number, ngoodpix: number}}
 */

function fitLine ( data, npix, krej, ngrow, maxiter) {

	const xscale= 2.0 / (npix - 1);

	if (npix <= 0) return {ngoodpix: 1, zstart: 0, zslope: 0};
	else if (npix===1) return {ngoodpix: 1, zstart: data[1], zslope: 0};

	/* Allocate a buffer for data minus fitted curve, another for the
	 * normalized X values, and another to flag rejected pixels.
	 */

	const badpix = new Uint8Array(npix);

	/* Compute normalized X vector.  The data X values [1:npix] are
	 * normalized to the range [-1:1].  This diagonalizes the lsq matrix
	 * and reduces its condition number.
	 */
	const normx = new Float32Array(npix);
	for (let i=0; i<npix; i++) normx[i] = i * xscale - 1.0;

	/* Fit a line with no pixel rejection.  Accumulate the elements of the
	 * matrix and data vector.  The matrix M is diagonal with
	 * M[1,1] = sum x**2 and M[2,2] = ngoodpix.  The data vector is
	 * DV[1] = sum (data[i] * x[i]) and DV[2] = sum (data[i]).
	 */
	let sumxsqr = 0;
	let sumxz = 0;
	let sumx = 0;
	let sumz = 0;
	for (let i=0; i<npix; i++) {
	    const x = normx[i];
	    const z = data[i];
	    sumxsqr = sumxsqr + (x * x);
	    sumxz   = sumxz + z * x;
	    sumz    = sumz + z;
	}


	/* Solve for the coefficients of the fitted line. */
	let z0 = sumz / npix;
	let dz =  sumxz / sumxsqr;
	const o_dz=dz;

	/* Iterate, fitting a new line in each iteration.  Compute the flattened
	 * data vector and the sigma of the flat vector.  Compute the lower and
	 * upper k-sigma pixel rejection thresholds.  Run down the flat array
	 * and detect pixels to be rejected from the fit.  Reject pixels from
	 * the fit by subtracting their contributions from the matrix sums and
	 * marking the pixel as rejected.
	 */
	let ngoodpix = npix;
	const minpix = Math.max (MIN_NPIXELS, Math.trunc(npix * MAX_REJECT));
	let  lastNgoodpix;

	for (let niter=0;  niter < maxiter;  niter++) {
	   lastNgoodpix = ngoodpix;

	    /* Subtract the fitted line from the data array. */
		const flat = flattenData (data,  normx, npix, z0, dz);

	    /* Compute the k-sigma rejection threshold.  In principle this
	     * could be more efficiently computed using the matrix sums
	     * accumulated when the line was fitted, but there are problems with
	     * numerical stability with that approach.
	     */
		const { mean, sigma} = computeSigma (flat, badpix, npix);
	    const threshold = sigma * krej;

	    /* Detect and reject pixels further than ksigma from the fitted
	     * line.
	     */
		const results= rejectPixels (data, flat, normx,
		                   badpix, npix, sumxsqr, sumxz, sumx, sumz, threshold, ngrow);
		ngoodpix = results.ngoodpix;
		sumxsqr = results.sumxsqr;
		sumxz = results.sumxz;
		sumx = results.sumx;
		sumz = results.sumz;

	    /* Solve for the coefficients of the fitted line.  Note that after
	     * pixel rejection the sum of the X values need no longer be zero.
	     */
	    if (ngoodpix > 0) {
		   const rowrat = sumx / sumxsqr;
		   z0 = (sumz - rowrat * sumxz) / (ngoodpix - rowrat * sumx);
		   dz = (sumxz - z0 * sumx) / sumxsqr;
	    }

	    if (ngoodpix >= lastNgoodpix || ngoodpix < minpix) break;
	}

	// Transform the line coefficients back to the X range [1:npix].
	const zstart = z0 - dz;
	let zslope = dz * xscale;
	if (Math.abs(zslope) < 1e-10) zslope = o_dz * xscale;

	return {ngoodpix, zstart, zslope};
}


/*
 * flattenData -- Compute and subtract the fitted line from the data array,
 * returned the flattened data in FLAT.
 * @param data
 * @param x
 * @param npix
 * @param z0
 * @param dz
 * @return {Float32Array}
 */
function flattenData ( data, x, npix, z0, dz	)	{
	const flat = new Float32Array(npix);
	for (let i=0; i < npix; i++) flat[i] =  data[i] - (x[i] * dz + z0);
	return flat;
}


/** computeSigma -- Compute the root mean square deviation from the
 * mean of a flattened array.  Ignore rejected pixels.
 * @param {Array.<Number>} a - flattened data array
 * @param {Uint8Array} badpix - bad pixel flags (!= 0 if bad pixel)
 * @param npix
 * @return {{sigma: number, mean: number, ngoodpix: number}}
 */
function computeSigma ( a, badpix, npix) {

	let pixval;
	let	ngoodpix = 0;
	let	sum = 0.0, sumsq = 0.0, temp;
	/* Accumulate sum and sum of squares. */
	for (let i=0; i < npix; i++) {
	    if (badpix[i]===GOOD_PIXEL) {
			pixval = a[i];
			ngoodpix = ngoodpix + 1;
			sum = sum + pixval;
			sumsq = sumsq + pixval * pixval;
		}
	}

	let mean, sigma;

	/* Compute mean and sigma. */
	switch (ngoodpix) {
	case 0:
	    mean = INDEF;
	    sigma = INDEF;
	    break;
	case 1:
	    mean = sum;
	    sigma = INDEF;
	    break;
	default:
	    mean = (sum / ngoodpix);
	    temp = sumsq / (ngoodpix-1) - (sum*sum) / (ngoodpix*(ngoodpix - 1));
	    if (temp < 0) sigma = 0;	// possible with roundoff error
	    else sigma = Math.sqrt (temp);
	}

	return {ngoodpix, mean, sigma};
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
 *
 * @param data - raw data array
 * @param flat - flattened data array
 * @param normx - normalized x values of pixels
 * @param badpix - bad pixel flags (!= 0 if bad pixel)
 * @param npix
 * @param sumxsqr
 * @param sumxz
 * @param sumx
 * @param sumz - matrix sums
 * @param threshold - threshold for pixel rejection
 * @param ngrow - number of pixels of growing
 * @return {{sumxz: *, sumz: *, sumx: *, ngoodpix: number, sumxsqr: *}}
 */
function rejectPixels ( data, flat, normx, badpix, npix, sumxsqr, sumxz, sumx, sumz, threshold, ngrow ) {

	let ngoodpix = npix;
	const lcut = -threshold;
	const hcut = threshold;
	let x,z;

	for (let i=0; i < npix; i++) {
	    if (badpix[i]===BAD_PIXEL) {
	    	ngoodpix = ngoodpix - 1;
		}
	    else {
			const residual = flat[i];
			if (residual < lcut || residual > hcut) {
				/* Reject the pixel and its neighbors out to the growing
                 * radius.  We must be careful how we do this to avoid
                 * directional effects.  Do not turn off thresholding on
                 * pixels in the forward direction; mark them for rejection
                 * but do not reject until they have been thresholded.
                 * If this is not done growing will not be symmetric.
                 */
				for (let j=Math.max(0,i-ngrow); j < Math.min(npix,i+ngrow); j++) {
					if (badpix[j]!==BAD_PIXEL) {
						if (j <= i) {
							x = normx[j];
							z =  data[j];
							sumxsqr = sumxsqr - (x * x);
							sumxz = sumxz - z * x;
							sumx = sumx - x;
							sumz = sumz - z;
							badpix[j] = BAD_PIXEL;
							ngoodpix = ngoodpix - 1;
						} else {
							badpix[j] = REJECT_PIXEL;
						}
					}
				}
			}
	    }
	}
	return {ngoodpix, sumxsqr, sumxz, sumx, sumz};
}

