/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Created by tatianag on 3/14/16.
 * This is a helper class to deal with decimation parameters.
 * Decimation allows to reduce the number of the points to be plotted.
 */

const DECIMATE_TAG='decimate';


/*
 * Combines decimation information (xColumnName, yColumnName, maxPoints, xyRatio, xMin, xMax, yMin, yMax)
 * into a string to be used as a value of table request's decimate parameter
 * @param {string} x column name or expression
 * @param {string} y column name or expression
 * @param {number} maximim bunber of points or bins
 * @param {number} x:y ratio
 * @param {number} optional min x limit
 * @param {number} optional max x limit
 * @param {number} optional min y limit
 * @param {number} optional max y limit
 * @param {number} optional minimum number of points to enable decimation
 * @returns {string}
 */
export const serializeDecimateInfo = function(xColumnName, yColumnName, maxPoints=10000, xyRatio=1, xMin, xMax, yMin, yMax, deciEnableLimit=Number.NaN) {
    const orderedNumericProps = [maxPoints, xyRatio, xMin, xMax, yMin, yMax, deciEnableLimit];
    const deciStrStart = `${DECIMATE_TAG}=${xColumnName},${yColumnName}`;
    return orderedNumericProps.reduce((sres, val)=>{
        return sres + ',' + (Number.isFinite(Number.parseFloat(val)) ? val : '');
    }, deciStrStart);
};

/*
 * Parses the value of table request's decimate parameter into an object
 * @param {string} decimate info object
 * @returns {object} decimate info object
 */
export const parseDecimateInfo = function(str) {
    if (!str) return;

    const kv = str.split('=', 2);
    if (kv && kv.length === 2 && kv[0]===DECIMATE_TAG) {
        const [xColumnName,yColumnName,maxPoints,xyRatio,xMin,xMax,yMin,yMax,deciEnableLimit='-1'] = kv[1].split(',');
        if (xColumnName && yColumnName) {
            return {
                xColumnName,
                yColumnName,
                maxPoints : Number.parseInt(maxPoints,10),
                xyRatio: Number.parseFloat(xyRatio),
                xMin: Number.parseFloat(xMin),
                xMax: Number.parseFloat(xMax),
                yMin: Number.parseFloat(yMin),
                yMax: Number.parseFloat(yMax),
                deciEnableLimit: Number.parseInt(deciEnableLimit)
            };
        }
    }
};

/*
 * Parses the value of decimate key string attribute into an object
 * with the properties for x and y column names (or expressions),
 * the number of bins along x and y axes, and x and y bin sizes.
 * Decimate key contains minimum information necessary to find
 * the center of the bin, if we know its x and y indexes.
 * @param {string} decimate_key(...)
 * @returns {object} decimate key object
 */
export const parseDecimateKey = function(str) {
    if (!str) return;
    let v = str.replace('decimate_key', '');
    if (v.length < 3) return null;
    v = v.substring(1,v.length-1); // remove outer braces
    const parts = v.split(',');
    if (parts.length === 8) {
        const xColNameOrExpr= parts[0];
        const yColNameOrExpr = parts[1];
        const xMin = Number(parts[2]);
        const yMin = Number(parts[3]);
        const nX = Number(parts[4]);
        const nY = Number(parts[5]);
        const xUnit = Number(parts[6]);
        const yUnit = Number(parts[7]);
        return {xColNameOrExpr, yColNameOrExpr, xMin, yMin, nX, nY, xUnit, yUnit};
    }
};
