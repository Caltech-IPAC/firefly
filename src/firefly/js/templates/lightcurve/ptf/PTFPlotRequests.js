/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {getCellValue} from '../../../tables/TableUtil.js';
import {WebPlotRequest} from '../../../visualize/WebPlotRequest.js';
import {makeWorldPt} from '../../../visualize/Point.js';
import {CoordinateSys} from '../../../visualize/CoordSys.js';
import {ServerRequest} from '../../../data/ServerRequest.js';
import {isNil} from 'lodash';
import {ERROR_MSG_KEY} from '../generic/errorMsg.js';

import {addCommonReqParams} from '../LcConverterFactory.js';
import {convertAngle} from '../../../visualize/VisUtil.js';

export function makePTFPlotRequest(table, rowIdx, cutoutSize) {
    const ra = getCellValue(table, rowIdx, 'ra');
    const dec = getCellValue(table, rowIdx, 'dec');
    const pid = getCellValue(table, rowIdx, 'pid');
    const band = 'g';

    // convert the default Cutout size in arcmin to deg for WebPlotRequest
    const cutoutSizeInDeg = convertAngle('arcmin','deg', cutoutSize);

    //http://irsa.ipac.caltech.edu/ibe/search/ptf/images/level1?where=pid=16406820

    const ibeserverinfo = 'http://irsa.ipac.caltech.edu/ibe/search/ptf/images/level1?';

    const dataserverinfo = 'http://irsatest.ipac.caltech.edu/ibe/data/ptf/images/level1/';
    const pfilename = 'proc/2013/06/16/f2/c4/p5/v1/PTF_201306163445_i_p_scie_t081605_u016406820_f02_p005137_c04.fits';
    const centerandsize = cutoutSize ? `?center=${ra},${dec}&size=${cutoutSizeInDeg}&gzip=false` : '';
    const url = `${dataserverinfo}${pfilename}/${centerandsize}`;
    const plot_desc = `PTF-${pid}`;
    const reqParams = WebPlotRequest.makeURLPlotRequest(url, plot_desc);
    const title = 'PTF-' + pid + (cutoutSize ? ` size: ${cutoutSize}(arcmin)` : '');
    return addCommonReqParams(reqParams, title, makeWorldPt(ra, dec, CoordinateSys.EQ_J2000));
}

//TODO to be implemented
/**
 *
 * @param tableModel
 * @param hlrow
 * @param cutoutSize
 * @param params object attribute match the LcManager#getImagePlotParams()
 * @returns {WebPlotRequest}
 */
export function getWebPlotRequestViaPTFIbe(tableModel, hlrow, cutoutSize, params = {
    bandName: 'g',
    fluxCol: 'mag_autocorr',
    dataSource: 'pid'
}) {
    const ra = getCellValue(tableModel, hlrow, 'ra');
    const dec = getCellValue(tableModel, hlrow, 'dec');

    const pid = getCellValue(tableModel, hlrow, 'pid');


    // convert the default Cutout size in arcmin to deg for WebPlotRequest
    const cutoutSizeInDeg = convertAngle('arcmin','deg', cutoutSize);

    // pfilename should be resolved using PtfibeResolver by ej
    // PtfIbeResolver res = new PtfIbeResolver();
    // String fs = res.getValuesFromColumn(pids, "pfilename");

    const pfilename = 'proc/2013/06/16/f2/c4/p5/v1/PTF_201306163445_i_p_scie_t081605_u016406820_f02_p005137_c04.fits';
    try {

        // flux/value column control this | unless UI has radio button band enabled, put bandName back here to match band
        const band = `${params.bandName}`;
        var tmpId = pid;

        let title = 'PTF-W' + band + '-' + tmpId;

        const sr = new ServerRequest('ibe_file_retrieve');
        sr.setParam('mission', 'ptf');
        sr.setParam('PROC_ID', 'ibe_file_retrieve');
        sr.setParam('schema', 'images');
        sr.setParam('table', 'level1');
        sr.setParam('pfilename', `${pfilename}`);
        //  sr.setParam('pfilename', `${fs}`);

        var wp = null;
        sr.setParam('doCutout', 'false');
        if (!isNil(ra) && !isNil(dec)) {
            sr.setParam('center', `${ra},${dec}`);
            sr.setParam('in_ra', `${ra}`);
            sr.setParam('in_dec', `${dec}`);
            wp = makeWorldPt(ra, dec, CoordinateSys.EQ_J2000);
            sr.setParam('doCutout', 'true');
            sr.setParam('size', `${cutoutSizeInDeg}`);
            sr.setParam('subsize', `${cutoutSizeInDeg}`);
            title = 'PTF-' + band + '-' + pid + (cutoutSize ? ` size: ${cutoutSize}(arcmin)` : '');
        }

        const reqParams = WebPlotRequest.makeProcessorRequest(sr, 'ptf');
        return addCommonReqParams(reqParams, title, wp);
    } catch (E) {
        throw new Error(E.message + ': as a consequence, images will fail', ERROR_MSG_KEY.IMAGE_FETCH);
    }
}