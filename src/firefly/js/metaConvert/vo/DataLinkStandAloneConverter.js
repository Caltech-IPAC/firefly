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
    const dataLinkData= getDataLinkData(table);
    const dlData= dataLinkData?.[row];

    const item= createDataLinkSingleRowItem({dlData, activateParams, baseTitle:'Datalink data', options});
    return item;
}