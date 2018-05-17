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

/**
 *
 * @param tableModel
 * @param hlrow
 * @param cutoutSize
 * @param params object attribute match the LcManager#getImagePlotParams()
 * @returns {WebPlotRequest}
 */
export function getWebPlotRequestViaZTFIbe(tableModel, hlrow, cutoutSize, params = {
    fluxCol: 'mag_autocorr',
    dataSource: 'pid'
}) {
    const ra = getCellValue(tableModel, hlrow, 'ra');
    const dec = getCellValue(tableModel, hlrow, 'dec');
    const field = getCellValue(tableModel, hlrow, 'field');
    const ccdid = getCellValue(tableModel, hlrow, 'ccdid');
    const qid = getCellValue(tableModel, hlrow, 'qid');
    const filtercode = getCellValue(tableModel, hlrow, 'filtercode');
    const filefracday = getCellValue(tableModel, hlrow, 'filefracday');


    // convert the default Cutout size in arcmin to deg for WebPlotRequest
    const cutoutSizeInDeg = convertAngle('arcmin','deg', cutoutSize);
    var band = null;

    try {

        // flux/value column control this | unless UI has radio button band enabled, put bandName back here to match band
        //const band = `${params.bandName}`;

        let title = 'ZTF-' + pid + '-'+ band;

        const sr = new ServerRequest('ibe_file_retrieve');
        sr.setParam('mission', 'ztf');
        sr.setParam('PROC_ID', 'ibe_file_retrieve');
        sr.setParam('ProductLevel', 'sci');
        sr.setParam('table', 'sci');
        sr.setParam('schema', 'images');
        sr.setParam('field', field);
        sr.setParam('ccdid', ccdid);
        sr.setParam('qid', qid);
        sr.setParam('filterdode', filtercode);
        sr.setParam('filefracday', filefracday);

        let wp = null;
        sr.setParam('doCutout', 'false');
        if (!isNil(ra) && !isNil(dec)) {
            sr.setParam('center', `${ra},${dec}`);
            sr.setParam('in_ra', `${ra}`);
            sr.setParam('in_dec', `${dec}`);
            wp = makeWorldPt(ra, dec, CoordinateSys.EQ_J2000);
            sr.setParam('doCutout', 'true');
            sr.setParam('size', `${cutoutSizeInDeg}`);
            sr.setParam('subsize', `${cutoutSizeInDeg}`);
            title ='ZTF-' + field + '-'+ ccdid +'-' + qid + '-'+ filtercode + (cutoutSize ? ` size: ${cutoutSize}(arcmin)` : '');
        }

        const reqParams = WebPlotRequest.makeProcessorRequest(sr, 'ztf');
        return addCommonReqParams(reqParams, title, wp);
    } catch (E) {
        throw new Error(E.message + ': as a consequence, images will fail', ERROR_MSG_KEY.IMAGE_FETCH);
    }
}