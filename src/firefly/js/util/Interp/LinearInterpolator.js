/**
 * Lijun Zhang
 * 05/01/2017
 * The LinearInterpolator interpolates the one dimensional data points to desired length based on Linear Interpolation
 * algorithm.  The input data array has to be monotonically ascending or descending.  The minimum length of the input array
 * is 2.  For any give coordinate (or index of the output array), the LinearInterpolator finds the value of the output array
 * at that index (or coordinate) based on the input data.
 */

/**
 * @Desc This method is displaying error message when there are exceptions.
 * @param message
 * @constructor
 */
function UserException(message) {
    this.message = message;
    this.name = 'UserException';
}

/**
 * LinearInterpolator class
 */
export class LinearInterpolator {
    constructor(x, y, allowExtrapolation) {
        this.x=x;
        this.y=y;
        this.n = x.length;
        this.allowExtrapolation=allowExtrapolation;
        this.checkInvariant();
    }
    /**
     *
     * @desc Check that the class invariant is satisfied. The x-coordinates have to be monotonically ascending or
     * descending. If the input array is not strictly monotonically increasing or decreasing, exception is thrown.
     * If the input array is descending, reverse the array and return.
     */
    checkInvariant() {
        var revX = false;
        var x0 = this.x[0]; // first value
        var xn = this.x[ this.n - 1]; // last value

        if ((this.n < 2)) {
            throw  new UserException('At least 2 points needed');
        }

        // determine "x" is ascending or descending
        var max = ((x0 > xn) ? x0 : xn);
        for (let i = 1; i < this.n; i++) {
            if (max > x0) {
                // check if the data is strictly monotonically increasing
                if (this.x[i] <= this.x[i - 1]) {
                    const  x1 = i - 1;
                    throw new UserException(
                        'Data must be strictly monotonically increasing or decreasing' + '\n    x['
                        + i + '] =' + this.x[i] + '\n <= x[' + x1 + '] = ' + this.x[x1] + ' ... ');
                }
            }
            else {
                if (this.x[i] >= this.x[i - 1]) {
                    // check if the data is strictly monotonically decreasing
                    const  x1 = i - 1;
                    throw new UserException(
                        'Data must be strictly monotonically increasing or decreasing' + '\n    x['
                        + i + '] = ' + this.x[i] + '\n >= x[' + x1 + '] = ' + this.x[x1] + ' ... ');

                }
                revX = true;
            }
        }

        if (revX) {
            this.x= this.x.reverse();
            this.y= this.y.reverse();

        }
    }

    /**
     * @desc this method calculates the output value at the coordinate=xVal.  It first finds the range where xVal falls
     * in the input array.  Then use the neighbors' points to calculate the slope and then the value.
     * @param xVal
     * @returns {*}
     */
    getInterpolatedValue(xVal){

         var i, j, k;

         if ((xVal < this.x[0]) || (xVal > this.x[this.n - 1])) {
             // Extrapolate if allowed.
             if (this.allowExtrapolation) {
                 if (xVal <this.x[0]) {
                     //left knots.
                     j = 0;
                     k = 1;
                 } else {
                     //right knots.
                     j = this.n - 2;
                     k = this.n - 1;
                 }
                 return this.extrapolate(xVal, j, k);
             } else {
                 // No extrapolation allowed.
                 throw new UserException('Abscissa out of range');
             }
         } else {
             // Binary search for correct place in the table.
             j = 0;
             k = this.n - 1;
             while (k - j > 1) {
                 i = (k + j) >> 1;
                 if (this.x[i] > xVal) {
                     k = i;
                 }
                 else {
                     j = i;
                 }
             }
             return this.interpolate(xVal, j, k);
         }
     }

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
     interpolate( xVal, j, k) {
          return  this.y[j] + (this.y[k] - this.y[j]) * (xVal - this.x[j]) / (this.x[k] - this.x[j]);
     }

    /**
     * @desc to find the output value beyong the input data range.
     * @param xVal
     * @param j
     * @param k
     * @returns {*}
     */
     extrapolate(xVal,  j,  k) {

        if (xVal < this.x[j]) {
            return this.y[j] + (this.y[k] - this.y[j]) * (xVal - this.x[j]) / (this.x[k] -this.x[j]);
        } else {
            return this.y[k] + (this.y[k] - this.y[j]) * (xVal - this.x[k]) / (this.x[k] - this.x[j]);
        }

    }
}
