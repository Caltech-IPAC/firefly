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
import Enum from 'enum';

var filterNames = {
    'g' : 1,
    'R' : 2,
    'I' : 4,
    'HA656' : 11,
    'HA663' : 12,
    'HA672' : 13,
    'HA681' : 14
};

/**
 *
 * @param tableModel
 * @param hlrow
 * @param cutoutSize
 * @param params object attribute match the LcManager#getImagePlotParams()
 * @returns {WebPlotRequest}
 */
export function getWebPlotRequestViaPTFIbe(tableModel, hlrow, cutoutSize, params = {
    fluxCol: 'mag_autocorr',
    dataSource: 'pid'
}) {
    const ra = getCellValue(tableModel, hlrow, 'ra');
    const dec = getCellValue(tableModel, hlrow, 'dec');
    const fid = getCellValue(tableModel, hlrow, 'fid');
    const pid = getCellValue(tableModel, hlrow, 'pid');

    // convert the default Cutout size in arcmin to deg for WebPlotRequest
    const cutoutSizeInDeg = convertAngle('arcmin','deg', cutoutSize);
    var band = null;

    if (fid == '1') {
        band = 'g';
    } else if (fid == '2') {
        band = 'R';
    } else if (fid == '4') {
        band = 'I';
    } else if (fid == '11') {
        band = 'HA656';
    } else if (fid == '12') {
        band = 'HA663';
    } else if (fid == '13') {
        band = 'HA672';
    } else if (fid == '14') {
        band = 'HA681';
    }

    try {

        // flux/value column control this | unless UI has radio button band enabled, put bandName back here to match band
        //const band = `${params.bandName}`;

        let title = 'PTF-' + pid + '-'+ band;

        const sr = new ServerRequest('ibe_file_retrieve');
        sr.setParam('mission', 'ptf');
        sr.setParam('PROC_ID', 'ibe_file_retrieve');
        sr.setParam('ProductLevel', 'l1');
        sr.setParam('table', 'level1');
        sr.setParam('schema', 'images');
        sr.setParam('pid', pid);

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
            title ='PTF-' + pid + '-'+ band + (cutoutSize ? ` size: ${cutoutSize}(arcmin)` : '');
        }

        const reqParams = WebPlotRequest.makeProcessorRequest(sr, 'ptf');
        return addCommonReqParams(reqParams, title, wp);
    } catch (E) {
        throw new Error(E.message + ': as a consequence, images will fail', ERROR_MSG_KEY.IMAGE_FETCH);
    }
}