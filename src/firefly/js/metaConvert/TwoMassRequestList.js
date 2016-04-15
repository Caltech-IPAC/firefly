/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get} from 'lodash';
import {makeServerRequestBuilder} from './converterUtils.js';
import {RangeValues,STRETCH_LINEAR,PERCENTAGE} from '../visualize/RangeValues.js';
import {getCellValue} from '../tables/TableUtil.js';

const colToUse= [ 'filter', 'scanno', 'fname', 'ordate', 'hemisphere', 'in_ra', 'in_dec', 'image_set' ];
const rangeValues= RangeValues.make(PERCENTAGE, 1, PERCENTAGE, 99);
rangeValues.algorithm= STRETCH_LINEAR;

const bandIdx= {'J':0, 'K':1, 'H':2};

/**
 * make a list of plot request for 2mass. This function works with ConverterFactory.
 * @param table
 * @param row
 * @param includeSingle
 * @param includeStandard
 * @param threeColorOps
 * @return {{}}
 */
export function make2MassPlotRequest(table, row, includeSingle, includeStandard, threeColorOps) {

    const overlap= get(table, 'request.intersect', '').toUpperCase()==='OVERLAPS';
    var headerParams= overlap ? ['mission', 'ds'] : ['mission', 'ds', 'subsize'];

    const builder= makeServerRequestBuilder(table,colToUse,headerParams,rangeValues,1);
    const band= getCellValue(table,row,'filter').toUpperCase();
    const retval= {};
    
    if (includeSingle) {
        retval.single= builder('wise-1','ibe_file_retrieve', 'Wise band 1', row, {band});
    }

    if (includeStandard) {
        retval.standard= [
            builder('2mass-J','ibe_file_retrieve', '2Mass J', row, {band:'J'}),
            builder('2mass-H','ibe_file_retrieve', '2Mass H', row, {band:'H'}),
            builder('2mass-K','ibe_file_retrieve', '2Mass K', row, {band:'K'})
        ];
        retval.highlightPlotId= retval.standard[bandIdx[band]].getPlotId();
    }

    if (threeColorOps) {
        retval.threeColor= threeColorOps
            .filter( (op) => Boolean(op.color))
            .map( (op) =>  {
                const b= bandIdx[op.band];
                return builder('2mass-three-J','ibe_file_retrieve', '2Mass 3 Color', row, {band:b});
            } );
    }
    return retval;
}
