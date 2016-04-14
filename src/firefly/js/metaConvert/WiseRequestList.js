/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get} from 'lodash';
import {makeServerRequestBuilder} from './converterUtils.js';
import {RangeValues,STRETCH_LINEAR,SIGMA} from '../visualize/RangeValues.js';
import {getCellValue} from '../tables/TableUtil.js';

const colToUse= ['scan_id', 'frame_num', 'coadd_id', 'in_ra', 'in_dec', 'image_set'];
const rangeValues= RangeValues.make(SIGMA, -2, SIGMA, 10);
rangeValues.algorithm= STRETCH_LINEAR;


/**
 * make a list of plot request for wise. This function works with ConverterFactory.
 * @param table
 * @param row
 * @param includeSingle
 * @param includeStandard
 * @param includeThree
 * @return {{}}
 */
export function makeWisePlotRequest(table, row, includeSingle, includeStandard, includeThree) {
    
    const overlap= get(table, 'request.intersect', '').toUpperCase()==='OVERLAPS';
    var headerParams= overlap ? ['mission', 'ImageSet', 'ProductLevel'] :
                                ['mission', 'ImageSet', 'ProductLevel', 'subsize'];

    const builder= makeServerRequestBuilder(table,colToUse,headerParams,rangeValues,1);
    const retval= {};
    const band= getCellValue(table,row,'band');

    if (includeSingle) {
        retval.single= builder('wise-1','ibe_file_retrieve', 'Wise band 1', row, {band});
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

    if (includeThree) {
        retval.threeColor= [
            builder('wise-three-0','ibe_file_retrieve', 'Wise 3 Color', row, {band:'1'}),
            builder('wise-three-0','ibe_file_retrieve', 'Wise 3 Color', row, {band:'2'}),
            builder('wise-three-0','ibe_file_retrieve', 'Wise 3 Color', row, {band:'4'})
        ];
    }
    return retval;
}


