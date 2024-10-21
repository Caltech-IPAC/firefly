/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {getCellValue} from '../../tables/TableUtil.js';
import {PlotAttribute} from '../../visualize/PlotAttribute';
import {makeWorldPt} from '../../visualize/Point.js';
import {CoordinateSys} from '../../visualize/CoordSys.js';
import {WebPlotRequest} from '../../visualize/WebPlotRequest.js';
import {addCommonReqParams} from '../../templates/lightcurve/LcConverterFactory.js';
import {GRID_FULL, SINGLE} from '../../visualize/MultiViewCntlr';
import {getRootURL} from '../../util/WebUtil.js';

/**
 * make a list of plot request for wise. This function works with ConverterFactory.
 * @param table
 * @param row
 * @param includeSingle
 * @param includeStandard
 * @return {{}}
 */
export function makeShaPlotRequest(table, row, includeSingle,includeStandard) {
    const ra = getCellValue(table, row, 'ra');
    const dec = getCellValue(table, row, 'dec');
    const aorKey = getCellValue(table, row, 'reqkey');
    const bcdId = getCellValue(table, row, 'bcdid');
    const ra_obj = getCellValue(table, row, 'ra_obj');
    const dec_obj = getCellValue(table, row, 'dec_obj');
    const bandpass = getCellValue(table, row, 'wavelength') ?? getCellValue(table, row, 'bandpass');
    const datatitle = table.title;
    const dataType = table.tbl_id === 'LEVEL_1' ? 'BCD' : table.tbl_id === 'LEVEL_2' ? 'PBCD'
            : table.tbl_id === 'SSC_IRS' ? 'IRS'
            : table.tbl_id === 'SEIP_SM' ? 'SEIP'
            : table.tbl_id === 'precovery-data' ? 'PRECOVERY'
            : 'AOR';

    const dataFile = dataType === 'AOR' ? getCellValue(table, row, 'depthofcoverage')
            : dataType === 'SEIP' ? getCellValue(table, row, 'fname')
            : getCellValue(table, row, 'heritagefilename');

    /*
     example url = https://irsa.ipac.caltech.edu/data/SPITZER/SHA/archive/proc/MIPS003600/r5572864/ch2/bcd/SPITZER_M2_5572864_0007_0061_9_bcd.fits
     */
    const serverinfo = 'https://irsa.ipac.caltech.edu/data/SPITZER/';
    const shaservlet = '/applications/Spitzer/SHA/servlet/ProductDownload?DATASET=level1';
    let filepath = '';
    let url = '';
    let wp;
    let title;
    if (dataType === 'IRS') {
        filepath = dataFile.replaceAll('/sha/archive/proc', 'Enhanced/IRS/spectra');
        url = `${serverinfo}/${filepath}`;
    } else if (dataType === 'SEIP') {
        url = `${serverinfo}` + 'Enhanced/SEIP/' + `${dataFile}`;
    } else if (dataType === 'PRECOVERY') {
        url += `${shaservlet}` + '&ID=' + `${bcdId}`;
        url = new URL(url, getRootURL());
    } else {
        filepath = dataFile.replaceAll('/sha', 'SHA');
        url = `${serverinfo}/${filepath}`;
    }
    

    if (dataType === 'PRECOVERY') {
        wp = makeWorldPt(ra_obj, dec_obj, CoordinateSys.EQ_J2000);
        title = `bcd-${bcdId}`;
    } else if (dataType === 'AOR') {
        wp = makeWorldPt(ra, dec, CoordinateSys.EQ_J2000);
        title = `${datatitle} ` + `${aorKey}: Depth of Coverage`;
    } else {
        wp = makeWorldPt(ra, dec, CoordinateSys.EQ_J2000);
        title = `${datatitle} ` + `${bandpass}`;
    }
    const plot_desc = `SHA-${aorKey}`;
    const reqParams = WebPlotRequest.makeURLPlotRequest(url, plot_desc);
    const req = addCommonReqParams(reqParams, title, wp);


    if (dataType === 'AOR') {
        req.setAttributes(
            {
                [PlotAttribute.HDU_TITLE_HEADER]:'CHANNUM',
                [PlotAttribute.HDU_TITLE_DESC]:'Channel '
            });
    }
    

    const plotId = url;
    const retval= {};
    if (includeSingle) retval.single = req;
    if (includeStandard) {
        retval.standard= [req.setPlotId(plotId)];
        retval.highlightPlotId= retval.standard[0];
    }
    return retval;
}

/**
 *
 * @param {TableModel} table
 * @param {DataProductsConvertType} converterTemplate
 * @return {DataProductsConvertType}
 */
export function makeShaViewCreate(table,converterTemplate) {
    const defShaView = {...converterTemplate,
        threeColor: false,
        hasRelatedBands: false,
        canGrid: true,
        maxPlots: 12,
        initialLayout: SINGLE};
    if (!table) return defShaView;
    const tblid = table.tbl_id;
    if (tblid === 'precovery-data') {
        return {...defShaView, initialLayout: GRID_FULL};
    } else {
        return defShaView;
    }
}


