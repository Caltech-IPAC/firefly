/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get} from 'lodash';
import {makeServerRequestBuilder} from './converterUtils.js';
import {RangeValues,STRETCH_LINEAR,SIGMA} from '../visualize/RangeValues.js';
import {getCellValue} from '../tables/TableUtil.js';
import {parseWorldPt} from '../visualize/Point.js';
import {convertAngle} from '../visualize/VisUtil.js';

const colToUse= ['field', 'ccdid', 'qid', 'filtercode', 'filefracday', 'expid', 'in_ra', 'in_dec'];
const rangeValues= RangeValues.makeRV({which:SIGMA, lowerValue:-2, upperValue:10, algorithm:STRETCH_LINEAR});

/**
 * make a list of plot request for ztf. This function works with ConverterFactory.
 * @param table
 * @param row
 * @param includeSingle
 * @param includeStandard
 * @return {{}}
 */
export function makeZtfPlotRequest(table, row, includeSingle, includeStandard) {

    const overlap= get(table, 'request.intersect', '').toUpperCase()==='OVERLAPS';
    var headerParams= overlap ? ['mission', 'ProductLevel'] :
                                ['mission', 'ProductLevel', 'subsize'];

    const svcBuilder= makeServerRequestBuilder(table,colToUse,headerParams,rangeValues,1);
    const builder = (plotId, reqKey, title, rowNum, extraParams) => {
        const req= svcBuilder(plotId, reqKey, title, rowNum, extraParams);
        req.setPreferenceColorKey('ztf-color-pref');
        const subsize = get(table, 'request.subsize');
        if (subsize && subsize>0) {
            const {UserTargetWorldPt, sizeUnit} = get(table, 'request', {});
            if (UserTargetWorldPt && sizeUnit) {
                const wp = parseWorldPt(UserTargetWorldPt);
                // cutout is requested when in_ra, in_dec, and subsize are set (see WiseIbeDataSource)
                req.setParam('center', `${wp.getLon()},${wp.getLat()}`); // degrees assumed if no unit
                req.setParam('in_ra', `${wp.getLon()}`);
                req.setParam('in_dec', `${wp.getLat()}`);
                const newSize = convertAngle(sizeUnit, 'deg', subsize);
                req.setParam('subsize', `${newSize}`);
            }
        }
        /*
        if (table.request.table_name.includes('3band')||table.request.table_name.includes('2band')) {
            const tblBand = table.request.table_name.includes('3band') ? 3 : 2;

            // add note to replace the query returned fail reason 'No Found'
            if (Number(extraParams.band) > tblBand) {
                req.setParam('userFailReason', {'not found': 'No image for band ' + extraParams.band});
            }
        }
        */
        return req;
    };

    const field= getCellValue(table,row,'field');
    const ccdid= getCellValue(table,row,'ccdid');
    const qid= getCellValue(table,row,'qid');
    const filtercode = getCellValue(table,row,'filtercode');
    const filefracday = getCellValue(table, row, 'filefracday');
    const expid = getCellValue(table, row, 'expid');
    const subsize = get(table, ['request', 'subsize']);
    const in_ra = getCellValue(table,row, 'in_ra');
    const in_dec = getCellValue(table,row, 'in_dec');

    const retval= {};

    if (includeSingle) {
        retval.single= builder(`ztf-${filefracday}`,'ibe_file_retrieve', `${filefracday} ${filtercode}`, row, {band:`${filtercode}`, field, ccdid, qid, in_ra, in_dec});
    }

    if (includeStandard) {

        retval.standard= [
            builder('ztf','ibe_file_retrieve', `${filtercode}`, row, {band:`${filtercode}`, field, ccdid, qid, in_ra, in_dec}),
        ];
        retval.highlightPlotId= 0;
    }
    
    return retval;
}


