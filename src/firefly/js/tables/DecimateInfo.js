/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


const DECIMATE_TAG='decimate';

/**
 * Created by tatianag on 3/14/16.
 * This is a helper class to deal with decimation parameters.
 * Decimation allows to reduce the number of the points to be plotted.
 */
export class DecimateInfo {
    constructor(xColumnName, yColumnName, maxPoints, xyRatio, xMin, xMax, yMin, yMax) {
        this.xColumnName = xColumnName;
        this.yColumnName = yColumnName;
        this.maxPoints = Number.parseInt(maxPoints,10); //NaN if not defined
        this.xyRatio = Number.parseFloat(xyRatio);
        this.xMin = Number.parseFloat(xMin);
        this.xMax = Number.parseFloat(xMax);
        this.yMin = Number.parseFloat(yMin);
        this.yMax = Number.parseFloat(yMax);
    }

    static isValid(decimateInfo) {
        return decimateInfo && this.xColumnName && this.yColumnName;
    }

    static parse(str) {
        if (!str) return;

        const kv = str.split('=', 2);
        let dinfo = null;
        if (kv && kv.length === 2 && kv[0].equals(DECIMATE_TAG)) {
            dinfo = new DecimateInfo(...kv[1].split(','));
            return dinfo.isValid() ? dinfo : null;
        }
    }

    serialize() {
        const {xColumnName, yColumnName, maxPoints, xyRatio, xMin, xMax, yMin, yMax} = this;
        const orderedNumericProps = [maxPoints, xyRatio, xMin, xMax, yMin, yMax];
        const deciStrStart = DECIMATE_TAG + '=' + xColumnName + ',' + yColumnName;
        const deciStr = orderedNumericProps.reduce((sres, val)=>{
            return sres + ',' + (Number.isFinite(val) ? val : '');
        }, deciStrStart);
        return deciStr;
    }
}
