/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get} from 'lodash';
import {makeServerRequestBuilder} from './converterUtils.js';
import {RangeValues,STRETCH_LINEAR,PERCENTAGE} from '../visualize/RangeValues.js';
import {getCellValue} from '../tables/TableUtil.js';

const colToUse= [ 'facility_name', 'band_name', 'instrument_name', 'fname', 'wavelength',
    'file_type', 'ra', 'dec', 'dataproduct_type'];
const rangeValues= RangeValues.makeRV({which:PERCENTAGE, lowerValue:1, upperValue:99, algorithm:STRETCH_LINEAR});

const bandIdx= {'IRAC1':0, 'IRAC2':1, 'MIPS24':2};

/**
 * make a list of plot request for atlas. This function works with ConverterFactory.
 * @param table
 * @param row
 * @param includeSingle
 * @param includeStandard
 * @param threeColorOps
 * @return {{}}
 */
export function makeAtlasPlotRequest(table, row, includeSingle, includeStandard, threeColorOps) {

    const overlap= get(table, 'request.intersect', '').toUpperCase()==='OVERLAPS';
    var headerParams= overlap ? ['mission', 'ds'] : ['mission', 'ds', 'band'];

    const builder= makeServerRequestBuilder(table,colToUse,headerParams,rangeValues,1);
    const band= getCellValue(table,row,'band_name');
    const inst= getCellValue(table,row,'instrument_name');
    const telescope= getCellValue(table,row,'facitlity_name');
    const retval= {};
    
    if (includeSingle) {
        retval.single= builder(`spitzer-${band}`,'ibe_file_retrieve', `${telescope} ${band}`, row, {band});
    }

    if (includeStandard) {

        retval.standard= [
            builder('spitzer-irac1','ibe_file_retrieve', 'IRAC 1', row, {band:'IRAC1'}),
            builder('spitzer-irac2','ibe_file_retrieve', 'IRAC 2', row, {band:'IRAC2'}),
            builder('spitzer-irac3','ibe_file_retrieve', 'IRAC 3', row, {band:'IRAC3'}),
            builder('spitzer-irac4','ibe_file_retrieve', 'IRAC 4', row, {band:'IRAC4'}),
            builder('spitzer-mips24','ibe_file_retrieve', 'MIPS 1', row, {band:'IRAC24'})
        ];
        retval.highlightPlotId= 0;
    }

    if (threeColorOps) {
        retval.threeColor= threeColorOps
            .filter( (op) => Boolean(op.color))
            .map( (op) =>  {
                const b= bandIdx[op.band];
                return builder('spitzer-seip-three-IRAC','ibe_file_retrieve', 'SEIP 3 Color', row, {band:b});
            } );
    }
    return retval;
}
