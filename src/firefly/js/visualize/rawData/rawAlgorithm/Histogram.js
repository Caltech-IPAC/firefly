/**
 * Creates a histogram of an image
 *
 * @author Booth Hartley
 *
 * Edit history
 * LZ 6/15/15
 *         - Renamed and rewrote the getTblArray method and commented out the eq_tbl and deq_dtbl
 *         - Only leave the bitpix = -32 and clean up the rest
 */

const HISTSIZ2 = 4096;  /* full size of hist array */
const HISTSIZ = 2048;     /* half size of hist array */

/**
 * @typedef HistogramData
 *
 * @prop {Array.<Number>} hist
 * @prop {Number} histMin
 * @prop {Number} histBinsize
 * @prop {Number} dnMin
 * @prop {Number} dnMax
 */


/**
 *
 * @param float1dArray
 * @param datamin
 * @param datamax
 * @return {HistogramData}
 */
export function makeHistogram(float1dArray, datamin, datamax) {

    // this.hist = new int[HISTSIZ2 + 1];
    // this.hist= new Int32Array[HISTSIZ2 + 1];
    const hist= new Int32Array(HISTSIZ2 + 1);
    let histBinsize;
    let histMin = datamin;

    // If the datamin or datamax is NaN, adjust them
    if (isNaN(datamin) || isNaN(datamax)) {
        datamax = -Number.MAX_VALUE;
        datamin = Number.MAX_VALUE;
        for (let k = 0; k < float1dArray.length; k++) {
            if (!isNaN(float1dArray[k])) {
                if (float1dArray[k] < datamin) datamin = float1dArray[k];
                if (float1dArray[k] > datamax) datamax = float1dArray[k];
            }
        }
    }

    let histDatamax = -Number.MAX_VALUE;
    let histDatamin = Number.MAX_VALUE;

    histMin= datamin;
    let histMax = datamax;
    let doing_redo = false;
    let redo_flag;

    while (true) {

        redo_flag = false;
        histBinsize =getHistBinSize(histMin, histMax);
        //reintialize the hist to 0
        hist.fill(0);
        let underflowCount = 0;
        let overflowCount = 0;
        for (let k= 0; k < float1dArray.length; k++) {
            if (!isNaN(float1dArray[k])) {
                const i = Math.trunc((float1dArray[k] - histMin) / histBinsize);
                if (i<0) {
                    underflowCount++;
                }
                else if (i>HISTSIZ2) {
                    overflowCount++;
                }
                else {
                    hist[i]++;
                }
                if (float1dArray[k] < histDatamin) histDatamin = float1dArray[k];
                if (float1dArray[k] > histDatamax) histDatamax = float1dArray[k];
            }
        }


        // printeDebugInfo(histMax, underflowCount, overflowCount);
        datamin = histDatamin;
        datamax = histDatamax;

        /* redo if more than 1% of pixels fell off histogram */
        if (underflowCount > float1dArray.length * .01) redo_flag = true;
        if (overflowCount > float1dArray.length * .01) redo_flag = true;

        /* check if we got a good spread */

        if (!redo_flag && !doing_redo) { /* don't bother checking if we already want a redo */

            /* see what happens if we lop off top and bottom 0.05% of hist */

            const lowLimit = getLowLimit(hist);
            const histMaxIndex = getHighSumIndex(lowLimit,hist) + 1;
            const histMinIndex = getLowSumIndex(lowLimit,hist);

            if (histMaxIndex===-1 || histMinIndex===-1){
                break;
            }

            if ((histMaxIndex - histMinIndex) < HISTSIZ) {
                histMax = (histMaxIndex * histBinsize) + histMin;
                histMin = (histMinIndex * histBinsize) + histMin;
                redo_flag = true;   /* we can spread it out by factor of 2 */
            }
        } else {
            if (!doing_redo) {
                histMax = datamax;
                histMin = datamin;
            }
        }

        if ( !doing_redo  &&  redo_flag ) {
            doing_redo = true;
        } else {
            break;
        }
    }
    return {hist, histMin, histBinsize, datamin, datamax};
}


// private void printeDebugInfo(double hist_max, int underFlowCount, int overFlowCount) {
//     if (SUTDebug.isDebug()) {
//         System.out.println("histMin = " + histMin);
//         System.out.println("hist_max = " + hist_max);
//         System.out.println("histBinsize = " + histBinsize);
//         System.out.println("underFlowCount = " + underFlowCount +
//                 "  overFlowCount = " + overFlowCount);
//
//
//     }
// }
//




/**
 * @param {HistogramData} histData
 * @param ra_value The percentile on the histogram (99.0 signifies 99%)
 * @param round_up Use the upper edge of the bin (versus the lower edge)
 * @return The DN value in the image corresponding to the percentile
 */
export function get_pct(histData, ra_value, round_up) {
    if (ra_value===0) return histData.dnMin;
    if (ra_value===100) return histData.dnMax;

    let goodpix = 0;
    for (let j = 0; j < HISTSIZ2; j++) goodpix += histData.hist[j];
    let sum = 0;
    const goal = Math.trunc(goodpix * (ra_value) / 100);
    let i = -1;
    do {
        i++;
        sum = sum + histData.hist[i];
    } while (sum < goal);
    if (round_up) return ((i + 1.0) * histData.histBinsize + histData.histMin);
    else return ((i) * histData.histBinsize + histData.histMin);
}

/**
 * @param {HistogramData} histData
 * @return A pointer to the histogram array
 */
export function getHistogramArray(histData) {
    return [...histData.hist];
}


/**
 * @param {HistogramData} histData
 * @return The minimum DN value in the image
 */
export function getDNMin(histData) { return histData.dnMin; }

/**
 * @param {HistogramData} histData
 * @return The maximum DN value in the image
 */
export function getDNMax(histData) { return histData.dnMax; }

/**
 * @param {HistogramData} histData
 * @param bin The bin index in the histogram
 * @return The DN value in the image corresponding to the specified bin
 */
export function getDNfromBin(histData, bin) { return (bin * histData.histBinsize + histData.histMin); }

/**
 * @param {HistogramData} histData
 * @param dn The DN value
 * @return The histogram index corresponding to the DN value
 */
export function getBinfromDN(histData, dn) {
    let bin = Math.trunc((dn - histData.histMin) / histData.histBinsize);
    if (bin >= HISTSIZ2) bin = HISTSIZ2 - 1;
    if (bin < 0) bin = 0;
    return bin;
}

/**
 * @param {HistogramData} histData
 * @param pct      The percentile on the histogram (99.0 signifies 99%)
 * @param round_up Use the upper edge of the bin (versus the lower edge)
 * @return The histogram index corresponding to the percentile
 */
export function getBINfromPercentile(histData, pct, round_up) {
    const dn = get_pct(histData, pct, round_up);
    let bin = Math.trunc((dn - histData.histMin) / histData.histBinsize);
    if (bin >= HISTSIZ2) bin = HISTSIZ2 - 1;
    if (bin < 0) bin = 0;
    return bin;
}

/*
 * @param {HistogramData} histData
 * @param sigma    The sigma multiplier (-2 signifies 2 sigma below the mean)
 * @param round_up Use the upper edge of the bin (versus the lower edge)
 * @return The histogram index corresponding to the percentile
 */
export function getBINfromSigma(histData, sigma, round_up) {
    const dn = get_sigma(histData, sigma, round_up);
    let bin = Math.trunc((dn - histData.histMin) / histData.histBinsize);
    if (bin >= HISTSIZ2) bin = HISTSIZ2 - 1;
    if (bin < 0) bin = 0;
    return bin;
}

/**
 * @param {HistogramData} histData
 * @return  tbl An int array [256] to be filled with the histogram equalized values
 */
export function getTblArray(histData) {
    const tbl = [];

    let goodpix = 0;
    for (let hist_index = 0; hist_index < HISTSIZ2; hist_index++) {
        goodpix += histData.hist[hist_index];
    }
    const goodpix_255 = goodpix / 255.0;

    let tblindex = 0;
    tbl[tblindex++] = histData.histMin;
    let next_goal = goodpix_255;
    let hist_index = 0;
    let accum = 0;
    while (hist_index < HISTSIZ2 && tblindex < 255) {

        if (accum >= next_goal) {
            tbl[tblindex++] = (hist_index * histData.histBinsize + histData.histMin);
            next_goal += goodpix_255;
        } else {
            accum += histData.hist[hist_index++];
        }
    }
    while (tblindex < 255) {
        tbl[tblindex++] = hist_index * histData.histBinsize + histData.histMin;
    }
    tbl[255] = Number.MAX_VALUE;
    return tbl;
}

/**
 * get_sigma
 * set DN corresponding to a sigma on the histogram
 * Code stolen from Montage mJPEG.c from Serge Monkewitz
 *
 * @param {HistogramData} histData
 * @param sigma_value The sigma on the histogram
 * @param round_up    Use the upper edge of the bin (versus the lower edge)
 * @return The DN value in the image corresponding to the sigma
 */
export function get_sigma(histData, sigma_value, round_up) {
    const lev16 = get_pct(histData, 16., round_up);
    const lev50 = get_pct(histData, 50., round_up);
    const lev84 = get_pct(histData, 84., round_up);
    const sigma = (lev84 - lev16) / 2;
    return (lev50 + sigma_value * sigma);
}


/**
 *
 * @param histMin
 * @param histMax
 * @return {number}
 */
function getHistBinSize(histMin, histMax){
    let  hbinsiz = (histMax - histMin) / HISTSIZ2;
    if (hbinsiz===0) hbinsiz = 1.0;
    return hbinsiz;
}

function getLowSumIndex(lowLimit,hist)  {
    let lowSum = 0;
    for (let i = 0; i < HISTSIZ2; i++) {
        lowSum += hist[i];
        if (lowSum > lowLimit) return i;
    }
    return -1;
}

function getHighSumIndex(lowLimit,hist) {
    let highSum = 0;
    for (let i = HISTSIZ2; i >= 0; i--) {
        highSum += hist[i];
        if (highSum > lowLimit) return i;
    }
    return -1;
}

function getLowLimit(hist) {
    let goodpix = 0;
    for (let i = 0; i < HISTSIZ2; i++) goodpix += hist[i];
    return Math.trunc(goodpix * 0.0005);
}
