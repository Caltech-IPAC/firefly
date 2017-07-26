/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get} from 'lodash';
import {makeServerRequestBuilder} from './converterUtils.js';
import {RangeValues,STRETCH_LINEAR,SIGMA} from '../visualize/RangeValues.js';
import {getCellValue} from '../tables/TableUtil.js';
import {parseWorldPt} from '../visualize/Point.js';
import {convertAngle} from '../visualize/VisUtil.js';

const colToUse= ['scan_id', 'frame_num', 'coadd_id', 'in_ra', 'in_dec', 'image_set'];
const rangeValues= RangeValues.makeRV({which:SIGMA, lowerValue:-2, upperValue:10, algorithm:STRETCH_LINEAR});

const bandMap= {b1:'1', b2:'2',b3:'3',b4:'4'};

/**
 * make a list of plot request for wise. This function works with ConverterFactory.
 * @param table
 * @param row
 * @param includeSingle
 * @param includeStandard
 * @param threeColorOps
 * @return {{}}
 */
export function makeWisePlotRequest(table, row, includeSingle, includeStandard, threeColorOps) {

    const overlap= get(table, 'request.intersect', '').toUpperCase()==='OVERLAPS';
    var headerParams= overlap ? ['mission', 'ImageSet', 'ProductLevel'] :
                                ['mission', 'ImageSet', 'ProductLevel', 'subsize'];

    const svcBuilder= makeServerRequestBuilder(table,colToUse,headerParams,rangeValues,1);
    const builder = (plotId, reqKey, title, rowNum, extraParams) => {
        const req= svcBuilder(plotId, reqKey, title, rowNum, extraParams);
        req.setPreferenceColorKey('wise-color-pref');
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
        return req;
    };

    const retval= {};
    const band= getCellValue(table,row,'band');

    if (includeSingle) {
        retval.single= builder(`wise-${band}`,'ibe_file_retrieve', `Wise band ${band}`, row, {band});
    }

    if (includeStandard) {
        retval.standard= [
            builder('wise-1','ibe_file_retrieve', 'Wise band 1', row, {band:'1'}),
            builder('wise-2','ibe_file_retrieve', 'Wise band 2', row, {band:'2'}),
            builder('wise-3','ibe_file_retrieve', 'Wise band 3', row, {band:'3'}),
            builder('wise-4','ibe_file_retrieve', 'Wise band 4', row, {band:'4'})
        ];
        const idx= Number(band)-1;
        if (retval.standard[idx]) retval.highlightPlotId= retval.standard[idx].getPlotId();
    }

    if (threeColorOps) {
        retval.threeColor= threeColorOps.map( (b) =>
            b && builder('wise-three-0','ibe_file_retrieve', 'Wise 3 Color', row, {band:bandMap[b]})
        );
    }
    return retval;
}


