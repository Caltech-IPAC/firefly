import {isEmpty} from 'lodash';
import {dpdtAnalyze, dpdtFromMenu, dpdtMessage} from './DataProductsType.js';
import {doFetchTable, getCellValue, hasRowAccess} from '../tables/TableUtil.js';
import {getServiceDescriptors, isDataLinkServiceDesc, makeWorldPtUsingCenterColumns } from '../util/VOAnalyzer.js';
import {makeFileAnalysisActivate} from './MultiProductFileAnalyzer.js';
import {makeObsCoreRequest} from './ObsCoreConverter.js';
import {getActiveMenuKey} from './DataProductsCntlr.js';
import {makeFileRequest} from '../tables/TableRequestUtil.js';
import {processDatalinkTable} from './DataLinkProcessor.js';


/**
 * Support Service descriptor single product
 * @param table
 * @param row
 * @param {ActivateParams} activateParams
 * @return {Promise.<DataProductsDisplayType>}
 */
export async function getServiceDescSingleDataProduct(table, row, activateParams) {

    const descriptors= getServiceDescriptors(table);
    if (!descriptors) return dpdtMessage('Could not find any service descriptors');
    if (!hasRowAccess(table, row)) return dpdtMessage('You do not have access to these data.');

    const positionWP= makeWorldPtUsingCenterColumns(table,row);


    const activeMenuLookupKey= `${descriptors[0].accessURL}--${table.tbl_id}--${row}`;
    const menu= createServDescMenuRet(descriptors,positionWP,table,row,activateParams,activeMenuLookupKey);
    const activeMenuKey= getActiveMenuKey(activateParams.dpId, activeMenuLookupKey);
    let index= menu.findIndex( (m) => m.menuKey===activeMenuKey);
    if (index<0) index= 0;

    const dlUrl= makeDlUrl(findDataLinkServeDescs(descriptors)[0],table, row);
    if (dlUrl) {
        try {
            const datalinkTable= await doFetchTable(makeFileRequest('dl table', dlUrl));
            const positionWP= makeWorldPtUsingCenterColumns(table,row);
            return processDatalinkTable({sourceTable:table,row,datalinkTable,positionWP,activateParams,
                additionalServiceDescMenuList:!isEmpty(menu)?menu:undefined});
        }
        catch (reason) {
            return isEmpty(menu) ?
                dpdtMessage('Data link look up failed: ' + dlUrl) :
                dpdtFromMenu(menu,index,activeMenuLookupKey,true);
        }
    }
    else {
        return dpdtFromMenu(menu,index,activeMenuLookupKey,true);
    }
}


function makeDlUrl(dlServDesc, table, row) {
    if (!dlServDesc) return undefined;
    const {urlParams, accessUrl}= dlServDesc;
    const sendParams={};
    urlParams.filter( ({ref}) => ref).forEach( (p) => sendParams[p.name]= getCellValue(table, row, p.colName));
    const newUrl= new URL(accessUrl);
    Object.entries(sendParams).forEach( ([k,v]) => newUrl.searchParams.append(k,v));
    return newUrl.toString();
}

/**
 *
 * @param {Array.<ServiceDescriptorDef>} descriptors
 * @param {WorldPt|undefined} positionWP
 * @param {TableModel} sourceTable
 * @param {number} row
 * @param {ActivateParams} activateParams
 * @param {String} activeMenuLookupKey
 * @return {Array.<DataProductsDisplayType>}
 */
export function createServDescMenuRet(descriptors, positionWP, sourceTable, row, activateParams, activeMenuLookupKey) {
    return descriptors
        .filter( (sDesc) => !isDataLinkServiceDesc(sDesc))
        .map( (sDesc,idx) => {
            const {title,accessURL,standardID,serDefParams, ID}= sDesc;
            const menuKey= 'serdesc-dlt-'+idx;
            const allowsInput= serDefParams.some( (p) => p.allowsInput);
            const request= makeObsCoreRequest(accessURL,positionWP,title,sourceTable,row);
            const name= 'Show: '+title;
            const activate= makeFileAnalysisActivate(sourceTable,row, request, positionWP,activateParams,menuKey, undefined, serDefParams, name);
            return dpdtAnalyze(name,activate,accessURL,serDefParams,menuKey,{activeMenuLookupKey,request, allowsInput, standardID, ID});
        });
}

const findDataLinkServeDescs= (descriptors) => descriptors?.filter( (sDesc) => isDataLinkServiceDesc(sDesc));

