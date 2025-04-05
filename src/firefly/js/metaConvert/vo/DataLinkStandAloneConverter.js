import {getPreferCutout} from '../../ui/tap/Cutout';
import {getDataLinkData} from '../../voAnalyzer/VoDataLinkServDef.js';
import {createDataLinkSingleRowItem} from './DataLinkProcessor.js';

/**
 *
 * @param {TableModel} table
 * @param {DataProductsConvertType} converterTemplate
 * @param {DataProductsFactoryOptions} options
 * @return {DataProductsConvertType}
 */
export function makeDatalinkStaneAloneConverter(table,converterTemplate,options={}) {
    if (!table) return converterTemplate;
    return {...converterTemplate,
        converterId: `DatalinkStandalone-${table.tbl_id}`};
}

export async function getDatalinkStandAlineDataProduct(table, row, activateParams, options) {
    const dataLinkData= getDataLinkData(table,true);
    const {dataProductsComponentKey}= options;
    const preferCutout= getPreferCutout(dataProductsComponentKey,table?.tbl_id);

    let dlData= dataLinkData?.[row];
    if (!dlData) return;
    const {isCutout,cutoutFullPair,usableEntry}= dlData.dlAnalysis;

    if (cutoutFullPair && usableEntry) {
        if (preferCutout) {
            dlData = isCutout ? dlData : dlData.relatedDLEntries.cutout;
        }
        else {
            dlData = !isCutout ? dlData : dlData.relatedDLEntries.fullImage;
        }
    }
    return createDataLinkSingleRowItem({dlData, activateParams, options});
}