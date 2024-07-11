/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get, isNil} from 'lodash';
import {makeServerRequestBuilder} from '../ImageDataProductsUtil.js';
import {RangeValues,STRETCH_LINEAR,SIGMA} from '../../visualize/RangeValues.js';
import {getCellValue, getMetaEntry} from '../../tables/TableUtil.js';
import {makeWorldPt} from '../../visualize/Point.js';
import {convertAngle} from '../../visualize/VisUtil.js';
import {PlotAttribute} from '../../visualize/PlotAttribute.js';
import {CoordinateSys} from '../../visualize/CoordSys.js';
import {GRID_FULL, GRID_RELATED} from '../../visualize/MultiViewCntlr.js';
import {Band} from '../../visualize/Band.js';

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
        const {UserTargetWorldPt, sizeUnit, refSourceId, sourceId} = get(table, 'request', {});


        const setSubSize= () => req.setParam('subsize', `${sizeUnit? convertAngle(sizeUnit, 'deg', subsize) : subsize}`);
        let wp;
        if (subsize>0) {
            if (getMetaEntry(table,'DataType')==='MOS') {
                const ra_obj= getCellValue(table,row,'ra_obj');
                const dec_obj= getCellValue(table,row,'dec_obj');
                if (ra_obj && dec_obj) {
                    wp = makeWorldPt(ra_obj, dec_obj, CoordinateSys.EQ_J2000);
                    req.setParam('center', `${ra_obj},${dec_obj}`);
                    req.setParam('in_ra', `${ra_obj}`);
                    req.setParam('in_dec', `${dec_obj}`);
                    setSubSize();
                }
            } else if (UserTargetWorldPt || sourceId) {
                const ra_in= getCellValue(table,row,'in_ra');
                const dec_in= getCellValue(table,row,'in_dec');
                // cutout is requested when in_ra, in_dec, and subsize are set (see WiseIbeDataSource)
                if (!isNil(ra_in) && !isNil(dec_in)) {
                    req.setParam('center', `${ra_in},${dec_in}`);
                    req.setParam('in_ra', `${ra_in}`);
                    req.setParam('in_dec', `${dec_in}`);
                    setSubSize();
                    wp = makeWorldPt(ra_in, dec_in, CoordinateSys.EQ_J2000);
                }
            }
            wp && req.setOverlayPosition(wp);
        }
        if (refSourceId) {
            const ra_in= getCellValue(table,row,'crval1');
            const dec_in= getCellValue(table,row,'crval2');
            //no cutout
            if (!isNil(ra_in) && !isNil(dec_in)) {
                req.setParam('center', `${ra_in},${dec_in}`);
                req.setParam('in_ra', `${ra_in}`);
                req.setParam('in_dec', `${dec_in}`);
                wp = makeWorldPt(ra_in, dec_in, CoordinateSys.EQ_J2000);
            }
            wp && req.setOverlayPosition(wp);
        }
        return req;
    };


    const retval= {};
    const band= getCellValue(table,row,'band');

    if (includeSingle) {
        retval.single= builder(`wise-${band}`,'ibe_file_retrieve', `WISE Band ${band}`, row, {band});
    }

    if (includeStandard) {
        retval.standard= [
            builder('wise-1','ibe_file_retrieve', 'WISE Band 1', row, {band:'1'}),
            builder('wise-2','ibe_file_retrieve', 'WISE Band 2', row, {band:'2'})];

        if (!TwoBandTables.find((tbl) => get(table,'request.table_name', '').toLowerCase().includes(tbl))) {
            retval.standard.push(builder('wise-3', 'ibe_file_retrieve', 'WISE Band 3', row, {band: '3'}));
            if (!ThreeBandTables.find((tbl) => get(table,'request.table_name','').toLowerCase().includes(tbl))) {
                retval.standard.push(builder('wise-4', 'ibe_file_retrieve', 'WISE Band 4', row, {band: '4'}));
            }
        }
        const idx= Number(band)-1;
        if (retval.standard[idx]) retval.highlightPlotId= retval.standard[idx].getPlotId();
    }

    if (threeColorOps) {
        retval.threeColor= threeColorOps.map( (b) =>
            b && builder('wise-three-0','ibe_file_retrieve', 'WISE 3 Color', row, {band:bandMap[b]})
        );
    }
    return retval;
}

/**
 *
 * @param {TableModel} table
 * @param {DataProductsConvertType} converterTemplate
 * @return {DataProductsConvertType}
 */
export function makeWiseViewCreate(table,converterTemplate) {
    const defWiseView = {
        ...converterTemplate,
        canGrid:true,
        maxPlots:12,
        hasRelatedBands:true,
        threeColor: true,
        initialLayout: GRID_RELATED,
        describeThreeColor: async ()  => ( {
                        b1: {color: Band.BLUE,  title: 'Band 1'},
                        b2: {color: Band.GREEN, title: 'Band 2'},
                        b3: {color: null,       title: 'Band 3'},
                        b4: {color: Band.RED,   title: 'Band 4'}
        }),
    };
    if (!table) return defWiseView;
    const tblid = table.tbl_id;
    if (tblid === 'sso') {
        return {...defWiseView, initialLayout: GRID_FULL};
    }else if (tblid === '1b') {
        return {...defWiseView, initialLayout: GRID_FULL};
    } else if (tblid === '3a' || tblid === 'sidsearch') {
        return defWiseView;
    }
}


