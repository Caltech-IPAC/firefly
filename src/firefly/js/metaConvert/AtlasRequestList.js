/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get} from 'lodash';
import {makeServerRequestBuilder} from './converterUtils.js';
import {RangeValues,STRETCH_LINEAR,PERCENTAGE} from '../visualize/RangeValues.js';
import {getCellValue} from '../tables/TableUtil.js';
import {parseWorldPt} from '../visualize/Point.js';
import {convertAngle} from '../visualize/VisUtil.js';

const colToUse= [ 'facility_name', 'band_name', 'instrument_name', 'fname', 'wavelength',
    'file_type', 'in_ra', 'in_dec', 'dataproduct_type'];
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

    var headerParams= ['mission', 'ds', 'band','dataset', 'table'];

    const svcBuilder= makeServerRequestBuilder(table,colToUse,headerParams,rangeValues,1);
    const builder = (plotId, reqKey, title, rowNum, extraParams) => {
        const req= svcBuilder(plotId, reqKey, title, rowNum, extraParams);
        const subsize = get(table, 'request.subsize');
        if (subsize && subsize>0) {
            const {UserTargetWorldPt, sizeUnit} = get(table, 'request', {});
            if (UserTargetWorldPt && sizeUnit) {
                const wp = parseWorldPt(UserTargetWorldPt);
                // cutout is requested when in_ra, in_dec, and subsize are set (see AtlasIbeDataSource)
                req.setParam('center', `${wp.getLon()},${wp.getLat()}`); // degrees assumed if no unit
                req.setParam('in_ra', `${wp.getLon()}`);
                req.setParam('in_dec', `${wp.getLat()}`);
                const newSize = convertAngle(sizeUnit, 'deg', subsize);
                req.setParam('subsize', `${newSize}`);
            }
        }
        return req;
    };


    const band= getCellValue(table,row,'band_name');
    const inst= getCellValue(table,row,'instrument_name');
    const telescope= getCellValue(table,row,'facility_name');
    const fname = getCellValue(table,row,'fname');
    const subsize = get(table, ['request', 'subsize']);
    const in_ra = getCellValue(table,row, 'in_ra');
    const in_dec = getCellValue(table,row, 'in_dec');

    const retval= {};
    
    if (includeSingle) {
        retval.single= builder(`spitzer-${band}`,'ibe_file_retrieve', `${telescope} ${band}`, row, {band});
    }

    if (includeStandard) {

        retval.standard= [
            builder('spitzer-irac1','ibe_file_retrieve', `${band}`, row, {band:`${band}`, fname, in_ra, in_dec}),
            // builder('spitzer-irac2','ibe_file_retrieve', 'IRAC 2', row, {band:'IRAC2'}),
            // builder('spitzer-irac3','ibe_file_retrieve', 'IRAC 3', row, {band:'IRAC3'}),
            // builder('spitzer-irac4','ibe_file_retrieve', 'IRAC 4', row, {band:'IRAC4'}),
            // builder('spitzer-mips24','ibe_file_retrieve', 'MIPS 1', row, {band:'MIPS24'})
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
