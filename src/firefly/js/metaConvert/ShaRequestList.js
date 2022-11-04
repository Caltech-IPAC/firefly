/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get, isNil} from 'lodash';
import {makeServerRequestBuilder} from './converterUtils.js';
import {RangeValues,STRETCH_LINEAR,SIGMA} from '../visualize/RangeValues.js';
import {getCellValue, getMetaEntry} from '../tables/TableUtil.js';
import {makeWorldPt, parseWorldPt} from '../visualize/Point.js';
import {convertAngle} from '../visualize/VisUtil.js';
import {PlotAttribute} from '../visualize/PlotAttribute';
import {CoordinateSys} from '../visualize/CoordSys.js';
import {WebPlotRequest} from '../visualize/WebPlotRequest.js';
import {ServerRequest} from '../data/ServerRequest.js';
import {ERROR_MSG_KEY} from '../templates/lightcurve/generic/errorMsg.js';
import {addCommonReqParams} from '../templates/lightcurve/LcConverterFactory.js';

const colToUse= ['reqkey', 'heritgefilename'];
const rangeValues= RangeValues.makeRV({which:SIGMA, lowerValue:-2, upperValue:10, algorithm:STRETCH_LINEAR});

/**
 * make a list of plot request for wise. This function works with ConverterFactory.
 * @param table
 * @param row
 * @param includeSingle
 * @return {{}}
 */
export function makeShaPlotRequest(table, row, includeSingle) {
    const ra = getCellValue(table, row, 'ra');
    const dec = getCellValue(table, row, 'dec');
    const docFile = getCellValue(table, row, 'heritagefilename');
    const aorKey = getCellValue(table, row, 'reqkey');
    const bandpass = getCellValue(table, row, 'wavelength');
    const datatitle = table.title;
    const dataType = table.tbl_id === 'LEVEL_1' ? 'BCD' : table.tbl_id === 'LEVEL_2' ? 'PBCD' : 'AOR';

    /*
     const url = https://irsa.ipac.caltech.edu/data/SPITZER/SHA/archive/proc/MIPS003600/r5572864/ch2/bcd/SPITZER_M2_5572864_0007_0061_9_bcd.fits
     */
        const serverinfo = 'https://irsa.ipac.caltech.edu/data/SPITZER/';
        let filepath = docFile.replace('/sha', 'SHA');
        const url = `${serverinfo}/${filepath}`;
        const plot_desc = `SHA-${aorKey}`;
        const reqParams = WebPlotRequest.makeURLPlotRequest(url, plot_desc);
        const title = `${datatitle} ` + `${bandpass}`;
        const req = addCommonReqParams(reqParams, title, makeWorldPt(ra, dec, CoordinateSys.EQ_J2000));
        //return req;
        const retval= {};
        if (includeSingle) retval.single = req;
        return retval;
}

