/**
 * Lijun Zhang
 * 05/01/2017
 * The LinearInterpolator interpolates the one dimensional data points to desired length based on Linear Interpolation
 * algorithm.  The input data array has to be monotonically ascending or descending.  The minimum length of the input array
 * is 2.  For any give coordinate (or index of the output array), the LinearInterpolator finds the value of the output array
 * at that index (or coordinate) based on the input data.
 */

/**
 * LinearInterpolator class
 */

export const  LinearInterpolator = (x, y, allowExtrapolation) => {

    const n = x.length;

    /**
     *
     * @desc Check that the class invariant is satisfied. The x-coordinates have to be monotonically ascending or
     * descending. If the input array is not strictly monotonically increasing or decreasing, exception is thrown.
     * If the input array is descending, reverse the array and return.
     */
    var checkInvariant = ()=> {
        var revX = false;
        var x0 = x[0]; // first value
        var xn = x[n - 1]; // last value I didn't hello

        if ((n < 2)) {
            throw  new Error('At least 2 points needed');
        }

        // determine "x" is ascending or descending
        var max = ((x0 > xn) ? x0 : xn);
        for (let i = 1; i < n; i++) {
            if (max > x0) {
                // check if the data is strictly monotonically increasing
                if (x[i] <= x[i - 1]) {
                    const x1 = i - 1;
                    throw new Error(
                        'Data must be strictly monotonically increasing or decreasing' + '\n    x['
                        + i + '] =' + x[i] + '\n <= x[' + x1 + '] = ' + x[x1] + ' ... ');
                }
            }
            else {
                if (x[i] >= x[i - 1]) {
                    // check if the data is strictly monotonically decreasing
                    const x1 = i - 1;
                    throw new Error(
                        'Data must be strictly monotonically increasing or decreasing' + '\n    x['
                        + i + '] = ' + x[i] + '\n >= x[' + x1 + '] = ' + x[x1] + ' ... ');

                }
                revX = true;
            }
        }

        if (revX) {
            x= x.reverse();
            y= y.reverse();

        }

    };


    /**
     * Algorithm:
     * The slope k can be expressed by:
     * k = (y-y1)/(x-x)
     * k = (y2-y1)/(x2-x1)
     * Thus:
     *  (y-y1)/(x-x) =(y2-y1)/(x2-x1)
     *  y = y1 + (y2-y1)/(x2-x1) * (x-x1)
     *
     * @desc Find the value at the xVal location.
     * @param xVal
     * @param j
     * @param k
     * @returns {*}
     */
    const interpolate = (xVal, j, k)=> {
        return y[j] + (y[k] - y[j]) * (xVal - x[j]) / (x[k] - x[j]);
    };

    /**
     * @desc to find the output value beyond the input data range.
     * @param xVal
     * @param j
     * @param k
     * @returns {*}
     */
    const extrapolate = (xVal, j, k) => {

        if (xVal < x[j]) {
            return y[j] + (y[k] - y[j]) * (xVal - x[j]) / (x[k] - x[j]);
        } else {
            return y[k] + (y[k] - y[j]) * (xVal - x[k]) / (x[k] - x[j]);
        }

    };


    const getInterpolatedValue=(xVal)=>{

        var i, j, k;

        if ((xVal < x[0]) || (xVal > x[n - 1])) {
            // Extrapolate if allowed.
            if (allowExtrapolation) {
                if (xVal <x[0]) {
                    //left knots.
                    j = 0;
                    k = 1;
                } else {
                    //right knots.
                    j = n - 2;
                    k = n - 1;
                }
                return extrapolate(xVal, j, k);
            } else {
                // No extrapolation allowed.
                throw new Error('Abscissa out of range');
            }
        } else {
            // Binary search for correct place in the table.
            j = 0;
            k = n - 1;
            while (k - j > 1) {
                i = (k + j) >> 1;
                if (x[i] > xVal) {
                    k = i;
                }
                else {
                    j = i;
                }
            }
            return interpolate(xVal, j, k);
        }
    };

    //check the input data
    checkInvariant();
    return getInterpolatedValue;

};

