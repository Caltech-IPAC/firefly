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
 * @param size
 * @param cutoutSize
 * @param params object attribute match the ztfManage#getImagePlotParams()
 * @returns {WebPlotRequest}
 */
export function makeWebPlotRequestViaZtfIbe(tableModel, hlrow, size, cutoutSize, params = {
    dataSource: 'field'
}) {
    const ra = Number(getCellValue(tableModel, hlrow, 'in_ra'));
    const dec = Number(getCellValue(tableModel, hlrow, 'in_dec'));
    const ra_obj = Number(getCellValue(tableModel, hlrow, 'ra_obj'));
    const dec_obj = Number(getCellValue(tableModel, hlrow, 'dec_obj'));
    const field = getCellValue(tableModel, hlrow, 'field');
    const ccdid = getCellValue(tableModel, hlrow, 'ccdid');
    const qid = getCellValue(tableModel, hlrow, 'qid');
    const filtercode = getCellValue(tableModel, hlrow, 'filtercode');
    const filefracday = getCellValue(tableModel, hlrow, 'filefracday');
    const expid = getCellValue(tableModel, hlrow, 'expid');
    const rfid = getCellValue(tableModel, hlrow, 'rfid');
    const ztftable = tableModel.tbl_id;


    // convert the default Cutout size to arcsec for display on the image
    const cutoutSizeInArcsec = convertAngle('deg','arcsec', cutoutSize).toFixed(1);
    //var band = null;

    try {
        let title ='ZTF-';

        const sr = new ServerRequest('ibe_file_retrieve');
        sr.setParam('mission', 'ztf');
        sr.setParam('PROC_ID', 'ibe_file_retrieve');
        //sr.setParam('ProductLevel', ztftable);
        sr.setParam('schema', 'products');

        if (ztftable === 'ref') {
            title += ztftable + '-' + field + '-' + filtercode;
            sr.setParam('ProductLevel', 'ref');
            sr.setParam('table', 'ref');
        } else if (ztftable === 'deep') {
            title += ztftable + '-' + field + '-' + filtercode;
            sr.setParam('ProductLevel', 'deep');
            sr.setParam('table', 'deep');
        } else if (ztftable === 'sci') {
            title += ztftable + '-' + filefracday + '-' + filtercode;
            sr.setParam('ProductLevel', 'sci');
            sr.setParam('table', 'sci');
        } else if (ztftable === 'diff') {
            title += ztftable + '-' + field + '-' + filtercode;
            sr.setParam('ProductLevel', 'diff');
            sr.setParam('table', 'sci');
        } else if (ztftable === 'sso')  {
            title += ztftable + '-' + field + '-' + filtercode;
            sr.setParam('ProductLevel', 'sso');
            sr.setParam('table', 'sci');
        }

        sr.setParam('field', field);
        sr.setParam('ccdid', ccdid);
        sr.setParam('qid', qid);
        sr.setParam('filtercode', filtercode);
        sr.setParam('filefracday', filefracday);
        sr.setParam('expid', expid);

        let wp = null;
        sr.setParam('doCutout', 'false');
        if (!isNil(cutoutSize)) {
            sr.setParam('doCutout', 'true');
            sr.setParam('size', `${cutoutSize}`);
            sr.setParam('subsize', `${cutoutSize}`);
        }
        
        if (!Number.isNaN(ra_obj) && !Number.isNaN(dec_obj)) {
            //if result is MOS, use ra_obj, dec_obj
            sr.setParam('center', `${ra_obj},${dec_obj}`);
            sr.setParam('in_ra', `${ra_obj}`);
            sr.setParam('in_dec', `${dec_obj}`);
            wp = makeWorldPt(ra_obj, dec_obj, CoordinateSys.EQ_J2000);
        } if (!Number.isNaN(ra) && !Number.isNaN(dec)) {
            sr.setParam('center', `${ra},${dec}`);
            sr.setParam('in_ra', `${ra}`);
            sr.setParam('in_dec', `${dec}`);
            wp = makeWorldPt(ra, dec, CoordinateSys.EQ_J2000);
        }
        
        title += (cutoutSize ? ` size: ${cutoutSizeInArcsec}(arcsec)` : '');


        const reqParams = WebPlotRequest.makeProcessorRequest(sr, 'ztf');
        return addCommonReqParams(reqParams, title, wp);
    } catch (E) {
        throw new Error(E.message + ': as a consequence, images will fail', ERROR_MSG_KEY.IMAGE_FETCH);
    }
}
