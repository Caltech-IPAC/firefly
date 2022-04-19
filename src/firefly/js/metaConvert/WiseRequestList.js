/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get} from 'lodash';
import {makeServerRequestBuilder} from './converterUtils.js';
import {RangeValues,STRETCH_LINEAR,SIGMA} from '../visualize/RangeValues.js';
import {getCellValue, getMetaEntry} from '../tables/TableUtil.js';
import {parseWorldPt} from '../visualize/Point.js';
import {convertAngle} from '../visualize/VisUtil.js';
import {PlotAttribute} from '../visualize/PlotAttribute';

const colToUse= ['scan_id', 'frame_num', 'coadd_id', 'in_ra', 'in_dec', 'image_set'];
const rangeValues= RangeValues.makeRV({which:SIGMA, lowerValue:-2, upperValue:10, algorithm:STRETCH_LINEAR});

const bandMap= {b1:'1', b2:'2',b3:'3',b4:'4'};

const ThreeBandTables = ['allsky_3band_p1bm_frm'];
const TwoBandTables =['allsky_2band_p1bm_frm'];

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
        req.setAttributes({[PlotAttribute.PREFERENCE_COLOR_KEY]:'wise-color-pref'});
        const subsize = get(table, 'request.subsize');
        const {UserTargetWorldPt, sizeUnit} = get(table, 'request', {});


        const setSubSize= () => req.setParam('subsize', `${sizeUnit? convertAngle(sizeUnit, 'deg', subsize) : subsize}`);

        if (subsize>0) {
            if (getMetaEntry(table,'DataType')==='MOS') {
                const ra_obj= getCellValue(table,row,'ra_obj');
                const dec_obj= getCellValue(table,row,'dec_obj');
                if (ra_obj && dec_obj) {
                    req.setParam('center', `${ra_obj},${dec_obj}`);
                    req.setParam('in_ra', `${ra_obj}`);
                    req.setParam('in_dec', `${dec_obj}`);
                    setSubSize();
                }
            }
            else if (UserTargetWorldPt) {
                const wp = parseWorldPt(UserTargetWorldPt);
                // cutout is requested when in_ra, in_dec, and subsize are set (see WiseIbeDataSource)
                req.setParam('center', `${wp.getLon()},${wp.getLat()}`); // degrees assumed if no unit
                req.setParam('in_ra', `${wp.getLon()}`);
                req.setParam('in_dec', `${wp.getLat()}`);
                setSubSize();
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


    const retval= {};
    const band= getCellValue(table,row,'band');

    if (includeSingle) {
        retval.single= builder(`wise-${band}`,'ibe_file_retrieve', `Wise band ${band}`, row, {band});
    }

    if (includeStandard) {
        retval.standard= [
            builder('wise-1','ibe_file_retrieve', 'Wise band 1', row, {band:'1'}),
            builder('wise-2','ibe_file_retrieve', 'Wise band 2', row, {band:'2'})];

        if (!TwoBandTables.find((tbl) => get(table,'request.table_name', '').toLowerCase().includes(tbl))) {
            retval.standard.push(builder('wise-3', 'ibe_file_retrieve', 'Wise band 3', row, {band: '3'}));
            if (!ThreeBandTables.find((tbl) => get(table,'request.table_name','').toLowerCase().includes(tbl))) {
                retval.standard.push(builder('wise-4', 'ibe_file_retrieve', 'Wise band 4', row, {band: '4'}));
            }
        }
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


