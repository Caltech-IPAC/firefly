/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {getCellValue} from '../../../tables/TableUtil.js';
import {WebPlotRequest} from '../../../visualize/WebPlotRequest.js';
import {makeWorldPt} from '../../../visualize/Point.js';
import {CoordinateSys} from '../../../visualize/CoordSys.js';
import {ServerRequest} from '../../../data/ServerRequest.js';

import {addCommonReqParams} from '../LcConverterFactory.js';

export function makeWisePlotRequest(table, rowIdx, cutoutSize) {
    const ra = getCellValue(table, rowIdx, 'ra');
    const dec = getCellValue(table, rowIdx, 'dec');
    const frameId = getCellValue(table, rowIdx, 'frame_id');
    var   wise_sexp_ibe = /(\d+)([0-9][a-z])(\w+)/g;
    var   res = wise_sexp_ibe.exec(frameId);
    const scan_id = res[1] + res[2];
    const scangrp = res[2];
    const frame_num = res[3];

    /*the following should be from reading in the url column returned from LC search
     we are constructing the url for wise as the LC table does
     not have the url column yet
     It is only for WISE, using default cutout size 0.3 deg
    const url = `http://irsa.ipac.caltech.edu/ibe/data/wise/merge/merge_p1bm_frm/${scangrp}/${scan_id}/${frame_num}/${scan_id}${frame_num}-w1-int-1b.fits`;
    */
    const serverinfo = 'http://irsa.ipac.caltech.edu/ibe/data/wise/merge/merge_p1bm_frm/';
    const centerandsize = cutoutSize ? `?center=${ra},${dec}&size=${cutoutSize}&gzip=false` : '';
    const url = `${serverinfo}${scangrp}/${scan_id}/${frame_num}/${scan_id}${frame_num}-w1-int-1b.fits${centerandsize}`;
    const plot_desc = `WISE-${frameId}`;
    const reqParams = WebPlotRequest.makeURLPlotRequest(url, plot_desc);
    const title= 'WISE-'+ frameId + (cutoutSize ? ` size: ${cutoutSize}(deg)` : '');
    return addCommonReqParams(reqParams, title, makeWorldPt(ra,dec,CoordinateSys.EQ_J2000));
}


export function getWebPlotRequestViaWISEIbe(tableModel, hlrow, cutoutSize, params={fluxCol:'w1mpro_ep'}) {
    const ra = getCellValue(tableModel, hlrow, 'ra');
    const dec = getCellValue(tableModel, hlrow, 'dec');
    const frameId = getCellValue(tableModel, hlrow, 'frame_id');
    var   wise_sexp_ibe = /(\d+)([0-9][a-z])(\w+)/g;
    var   res = wise_sexp_ibe.exec(frameId);
    const scan_id = res[1] + res[2];
    const scangrp = res[2];
    const frame_num = res[3];
    const band=`${params.fluxCol}`.match(/\d/g);
    const title= 'WISE-W'+ band + '-'+ frameId + (cutoutSize ? ` size: ${cutoutSize}(deg)` : '');

    const sr= new ServerRequest('ibe_file_retrieve');
    sr.setParam('mission', 'wise');
    sr.setParam('PROC_ID', 'ibe_file_retrieve');
    sr.setParam('ProductLevel',  '1b');
    sr.setParam('ImageSet', 'merge');
    sr.setParam('band', `${band}`);
    sr.setParam('scangrp', `${scangrp}`);
    sr.setParam('scan_id', `${scan_id}`);
    sr.setParam('frame_num', `${frame_num}`);
    sr.setParam('center', `${ra},${dec}`);
    sr.setParam('size', `${cutoutSize}`);
    sr.setParam('subsize', `${cutoutSize}`);
    sr.setParam('in_ra',`${ra}`);
    sr.setParam('in_dec',`${dec}`);

    const reqParams = WebPlotRequest.makeProcessorRequest(sr, 'wise');
    return addCommonReqParams(reqParams, title, makeWorldPt(ra,dec,CoordinateSys.EQ_J2000));
}