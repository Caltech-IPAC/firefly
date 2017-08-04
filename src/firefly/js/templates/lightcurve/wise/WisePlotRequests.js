/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {getCellValue} from '../../../tables/TableUtil.js';
import {WebPlotRequest} from '../../../visualize/WebPlotRequest.js';
import {makeWorldPt} from '../../../visualize/Point.js';
import {CoordinateSys} from '../../../visualize/CoordSys.js';
import {ServerRequest} from '../../../data/ServerRequest.js';
import {isNil, get} from 'lodash';
import {ERROR_MSG_KEY} from '../generic/errorMsg.js';

import {addCommonReqParams} from '../LcConverterFactory.js';
import {convertAngle} from '../../../visualize/VisUtil.js';

export function makeWisePlotRequest(table, rowIdx, cutoutSize) {
    const ra = getCellValue(table, rowIdx, 'ra');
    const dec = getCellValue(table, rowIdx, 'dec');
    const frameId = getCellValue(table, rowIdx, 'frame_id');

    var wise_sexp_ibe = /(\d+)([0-9][a-z])(\w+)/g;
    var res = wise_sexp_ibe.exec(frameId);
    const scan_id = res[1] + res[2];
    const scangrp = res[2];
    const frame_num = res[3];
    //const band=`${params.fluxCol}`.match(/\d/g);
    const band = 1;
    /*the following should be from reading in the url column returned from LC search
     we are constructing the url for wise as the LC table does
     not have the url column yet
     set default cutout size 5 arcmin
     const url = `http://irsa.ipac.caltech.edu/ibe/data/wise/merge/merge_p1bm_frm/${scangrp}/${scan_id}/${frame_num}/${scan_id}${frame_num}-w1-int-1b.fits`;
     */
    // convert the default Cutout size in arcmin to deg for WebPlotRequest
    const cutoutSizeInDeg = convertAngle('arcmin','deg', cutoutSize);

    const serverinfo = 'http://irsa.ipac.caltech.edu/ibe/data/wise/merge/merge_p1bm_frm/';
    const centerandsize = cutoutSize ? `?center=${ra},${dec}&size=${cutoutSizeInDeg}&gzip=false` : '';
    const url = `${serverinfo}${scangrp}/${scan_id}/${frame_num}/${scan_id}${frame_num}-w${band}-int-1b.fits${centerandsize}`;
    const plot_desc = `WISE-${frameId}`;
    const reqParams = WebPlotRequest.makeURLPlotRequest(url, plot_desc);
    const title = 'WISE-' + frameId + (cutoutSize ? ` size: ${cutoutSize}(arcmin)` : '');
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
export function getWebPlotRequestViaWISEIbe(tableModel, hlrow, cutoutSize, params = {
    bandName: 'w1',
    fluxCol: 'w1mpro_ep',
    dataSource: 'frame_id'
}) {

    const {CENTER_COLUMN} = get(tableModel, ['tableMeta']);
    const ra_dec = ['ra', 'dec'];

    if (CENTER_COLUMN) {
        const s = CENTER_COLUMN.split(';');

        if (s && s.length === 3) {
            ra_dec[0] = s[0];
            ra_dec[1] = s[1];
        }
    }

    const ra = getCellValue(tableModel, hlrow, ra_dec[0]);
    const dec = getCellValue(tableModel, hlrow, ra_dec[1]);

    //For images from AllWise:
    const frameId = getCellValue(tableModel, hlrow, 'frame_id');
    // For other single exposure tables (NEOWISE, etc)
    const frameNum = getCellValue(tableModel, hlrow, 'frame_num');
    const scanId = getCellValue(tableModel, hlrow, 'scan_id');
    const sourceId = getCellValue(tableModel, hlrow, 'source_id');

    // convert the default Cutout size in arcmin to deg for WebPlotRequest
    const cutoutSizeInDeg = convertAngle('arcmin','deg', cutoutSize);

    try {
        var wise_sexp_ibe = /(\d+)([0-9][a-z])(\w+)/g;
        var tmpId = '';
        var res;
        if (!isNil(frameId)) {
            res = wise_sexp_ibe.exec(frameId);
            tmpId = frameId;
        } else if (!isNil(sourceId)) {
            res = wise_sexp_ibe.exec(sourceId.substring(0, sourceId.indexOf('-')));
            tmpId = sourceId;
        } else {
            tmpId = `${scanId}${frameNum}`;
            res = wise_sexp_ibe.exec(tmpId);
        }

        const scan_id = res[1] + res[2];
        const scangrp = res[2];
        const frame_num = res[3];

        // flux/value column control this | unless UI has radio button band enabled, put bandName back here to match band
        const band = `${params.bandName}`.match(/[1-4]/i);// `${params.fluxCol}`.match(/w[1-4]/i); //check if name has wW[1-4] in name


        let title = 'WISE-W' + band + '-' + tmpId;

        const sr = new ServerRequest('ibe_file_retrieve');
        sr.setParam('mission', 'wise');
        sr.setParam('PROC_ID', 'ibe_file_retrieve');
        sr.setParam('ProductLevel', '1b');

        // TODO the value ImageSet should be collected in the server or passed in here from properties,
        // specially to handle WISE internal case
        //sr.setParam('ImageSet', 'merge'); // see property 'ibe.public_release'

        sr.setParam('band', `${band}`);
        sr.setParam('scangrp', `${scangrp}`);
        sr.setParam('scan_id', `${scan_id}`);
        sr.setParam('frame_num', `${frame_num}`);
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
            title = 'WISE-W' + band + '-' + tmpId + (cutoutSize ? ` size: ${cutoutSize}(arcmin)` : '');
        }

        const reqParams = WebPlotRequest.makeProcessorRequest(sr, 'wise');
        return addCommonReqParams(reqParams, title, wp);
    } catch (E) {
        throw new Error(E.message + ': as a consequence, images will fail', ERROR_MSG_KEY.IMAGE_FETCH);
    }
}