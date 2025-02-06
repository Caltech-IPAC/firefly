import {getPreferCutout} from '../../ui/tap/Cutout';
import {getDataLinkData} from '../../voAnalyzer/VoDataLinkServDef.js';
import {DEFAULT_DATA_PRODUCTS_COMPONENT_KEY} from '../DataProductsCntlr';
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
    const dataLinkData= getDataLinkData(table);
    const {dataProductsComponentKey=DEFAULT_DATA_PRODUCTS_COMPONENT_KEY}= options;
    const preferCutout= getPreferCutout(dataProductsComponentKey,table?.tbl_id);

    let dlData= dataLinkData?.[row];
    const {isCutout,cutoutFullPair}= dlData.dlAnalysis;

    if (cutoutFullPair) {
        if (preferCutout) {
            dlData = isCutout ? dlData : dlData.relatedDLEntries.cutout;
        }
        else {
            dlData = !isCutout ? dlData : dlData.relatedDLEntries.fullImage;
        }
    }
    return createDataLinkSingleRowItem({dlData, activateParams, options});
}