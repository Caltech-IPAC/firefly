/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {getCellValue} from '../../../tables/TableUtil.js';
import {WebPlotRequest} from '../../../visualize/WebPlotRequest.js';
import {makeWorldPt} from '../../../visualize/Point.js';
import {CoordinateSys} from '../../../visualize/CoordSys.js';
import {ServerRequest} from '../../../data/ServerRequest.js';
import {isNil, isEmpty} from 'lodash';
import {ERROR_MSG_KEY} from '../generic/errorMsg.js';

import {addCommonReqParams} from '../LcConverterFactory.js';
import {convertAngle} from '../../../visualize/VisUtil.js';

export function makePTFPlotRequest(table, rowIdx, cutoutSize) {
    const ra = getCellValue(table, rowIdx, 'ra');
    const dec = getCellValue(table, rowIdx, 'dec');
    const pid = getCellValue(table, rowIdx, 'pid');

    var ptf_sexp_ibe = /(\d+)([0-9][a-z])(\w+)/g;
    var res = ptf_sexp_ibe.exec(pid);
    const scan_id = res[1] + res[2];
    const scangrp = res[2];
    const frame_num = res[3];
    //const band=`${params.fluxCol}`.match(/\d/g);
    const band = 1;

    // convert the default Cutout size in arcmin to deg for WebPlotRequest
    const cutoutSizeInDeg = convertAngle('arcmin','deg', cutoutSize);

    const serverinfo = 'http://irsa.ipac.caltech.edu/ibe/data/ptf/merge/merge_p1bm_frm/';
    const centerandsize = cutoutSize ? `?center=${ra},${dec}&size=${cutoutSizeInDeg}&gzip=false` : '';
    const url = `${serverinfo}${scangrp}/${scan_id}/${frame_num}/${scan_id}${frame_num}-w${band}-int-1b.fits${centerandsize}`;
    const plot_desc = `PTF-${pid}`;
    const reqParams = WebPlotRequest.makeURLPlotRequest(url, plot_desc);
    const title = 'PTF-' + pid + (cutoutSize ? ` size: ${cutoutSize}(arcmin)` : '');
    return addCommonReqParams(reqParams, title, makeWorldPt(ra, dec, CoordinateSys.EQ_J2000));
}


/**
 *
 * @param tableModel
 * @param hlrow
 * @param cutoutSize
 * @param params object attribute match the LcManager#getImagePlotParams()
 * @returns {WebPlotRequest}
 */
export function getWebPlotRequestViaPTFIbe(tableModel, hlrow, cutoutSize, params = {
    bandName: 'w1',
    fluxCol: 'mag_autocorr',
    dataSource: 'frame_id'
}) {
    const ra = getCellValue(tableModel, hlrow, 'ra');
    const dec = getCellValue(tableModel, hlrow, 'dec');

    const pid = getCellValue(tableModel, hlrow, 'pid');


    // convert the default Cutout size in arcmin to deg for WebPlotRequest
    const cutoutSizeInDeg = convertAngle('arcmin','deg', cutoutSize);

    try {
        var ptf_sexp_ibe = /(\d+)([0-9][a-z])(\w+)/g;
        var tmpId = '';
        var res;
        if (!isNil(pid)) {
            res = ptf_sexp_ibe.exec(pid);
            tmpId = pid;
        }


        // flux/value column control this | unless UI has radio button band enabled, put bandName back here to match band
        const band = `${params.bandName}`.match(/[1-4]/i);// `${params.fluxCol}`.match(/w[1-4]/i); //check if name has wW[1-4] in name


        let title = 'PTFE-W' + band + '-' + tmpId;

        const sr = new ServerRequest('ibe_file_retrieve');
        sr.setParam('mission', 'PTF');
        sr.setParam('PROC_ID', 'ibe_file_retrieve');
        sr.setParam('ProductLevel', '1b');

        sr.setParam('band', `${band}`);

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
            title = 'PTF-W' + band + '-' + tmpId + (cutoutSize ? ` size: ${cutoutSize}(arcmin)` : '');
        }

        const reqParams = WebPlotRequest.makeProcessorRequest(sr, 'ptf');
        return addCommonReqParams(reqParams, title, wp);
    } catch (E) {
        throw new Error(E.message + ': as a consequence, images will fail', ERROR_MSG_KEY.IMAGE_FETCH);
    }
}