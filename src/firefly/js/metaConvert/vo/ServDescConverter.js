import {isEmpty} from 'lodash';
import {hasRowAccess} from '../../tables/TableUtil.js';
import {getSearchTarget, makeWorldPtUsingCenterColumns} from '../../voAnalyzer/TableAnalysis.js';
import {getServiceDescriptors, isDataLinkServiceDesc} from '../../voAnalyzer/VoDataLinkServDef.js';
import {getActiveMenuKey} from '../DataProductsCntlr.js';
import {dpdtFromMenu, dpdtSimpleMsg} from '../DataProductsType.js';
import {
    createGridResult, datalinkDescribeThreeColor, getDatalinkRelatedGridProduct, getDatalinkSingleDataProduct, makeDlUrl
} from './DatalinkProducts.js';
import {createServDescMenuRet} from './ServDescProducts.js';


const DEF_MAX_PLOTS= 8;


/**
 *
 * @param {TableModel} table
 * @param {DataProductsConvertType} converterTemplate
 * @param {DataProductsFactoryOptions} options
 * @return {DataProductsConvertType}
 */
export function makeServDescriptorConverter(table,converterTemplate,options={}) {
    if (!table) return converterTemplate;
    const descriptors = getServiceDescriptors(table);
    if (!descriptors || !findDataLinkServeDescs(descriptors)?.length) return converterTemplate;

    const canRelatedGrid= options.allowImageRelatedGrid?? false;
    const threeColor= converterTemplate.threeColor && options?.allowImageRelatedGrid;
    const allowServiceDefGrid= options.allowServiceDefGrid?? false;
    //------
    const baseRetOb = {
        ...converterTemplate,
        initialLayout: options?.dataLinkInitialLayout ?? 'single',
        describeThreeColor: (threeColor) ? describeServDefThreeColor : undefined,
        threeColor,
        canGrid: canRelatedGrid || allowServiceDefGrid,
        maxPlots: canRelatedGrid ? DEF_MAX_PLOTS : 1,
        hasRelatedBands: canRelatedGrid,
        converterId: `ServiceDef-${table.tbl_id}`
    };
    return baseRetOb;
}


function describeServDefThreeColor(table, row, options) {
    const descriptors= getServiceDescriptors(table);
    if (!descriptors || !hasRowAccess(table, row)) return;
    const dlTableUrl= makeDlUrl(findDataLinkServeDescs(descriptors)[0],table, row);
    if (!dlTableUrl) return;
    return datalinkDescribeThreeColor(dlTableUrl, table,row, options);
}


/**
 * Support Service descriptor single product
 * @param table
 * @param row
 * @param {ActivateParams} activateParams
 * @param {DataProductsFactoryOptions} options
 * @return {Promise.<DataProductsDisplayType>}
 */
export async function getServiceDescSingleDataProduct(table, row, activateParams, options) {

    const descriptors= getServiceDescriptors(table);
    if (!descriptors) return dpdtSimpleMsg('Could not find any service descriptors');
    if (!hasRowAccess(table, row)) return dpdtSimpleMsg('You do not have access to these data.');

    const positionWP= getSearchTarget(table.request,table) ?? makeWorldPtUsingCenterColumns(table,row);


    const activeMenuLookupKey= `${descriptors[0].accessURL}--${table.tbl_id}--${row}`;
    const menu= createServDescMenuRet({descriptors,positionWP,table,row,
        activateParams,activeMenuLookupKey, options});
    const activeMenuKey= getActiveMenuKey(activateParams.dpId, activeMenuLookupKey);
    let index= menu.findIndex( (m) => m.menuKey===activeMenuKey);
    if (index<0) index= 0;

    const dlTableUrl= makeDlUrl(findDataLinkServeDescs(descriptors)[0],table, row);
    if (dlTableUrl) {
        return getDatalinkSingleDataProduct({dlTableUrl, options, sourceTable:table, row,
            activateParams, titleStr:'',
            additionalServiceDescMenuList:!isEmpty(menu)?menu:undefined});
    }
    else {
        return dpdtFromMenu(menu,index,activeMenuLookupKey,true);
    }
}

export async function getServiceDescGridDataProduct(table, plotRows, activateParams, options) {
    const pAry= plotRows.map( (pR) => getServiceDescSingleDataProduct(table,pR.row,activateParams,options, false));
    return createGridResult(pAry,activateParams,table,plotRows);
}

export async function getServiceDescRelatedDataProduct(table, row, threeColorOps, highlightPlotId, activateParams, options) {
    const descriptors= getServiceDescriptors(table);
    if (!descriptors) return dpdtSimpleMsg('Could not find any service descriptors');
    if (!hasRowAccess(table, row)) return dpdtSimpleMsg('You do not have access to these data.');
    const dlTableUrl= makeDlUrl(findDataLinkServeDescs(descriptors)[0],table, row);
    if (!dlTableUrl) return dpdtSimpleMsg('a datalink service descriptors return images is required for related grid');
    return getDatalinkRelatedGridProduct({dlTableUrl, activateParams,table,row,threeColorOps, titleStr:'',options});
}







export const findDataLinkServeDescs= (sdAry) => sdAry?.filter( (serDef) => isDataLinkServiceDesc(serDef));

