import {get} from 'lodash';
import {WebPlotRequest, TitleOptions} from '../visualize/WebPlotRequest.js';
import {getCellValue, doFetchTable, hasRowAccess} from '../tables/TableUtil.js';
import {getObsCoreAccessURL, getObsCoreProdType} from '../util/VOAnalyzer.js';
import {
    getObsCoreProdTypeCol,
    isFormatDataLink,
    isFormatVoTable,
    makeWorldPtUsingCenterColumns
} from '../util/VOAnalyzer';
import {makeFileRequest} from '../tables/TableRequestUtil';
import {ZoomType} from '../visualize/ZoomType.js';
import {createGridImagesActivate} from './ImageDataProductsUtil.js';
import {makeAnalysisGetSingleDataProduct} from './MultiProductFileAnalyzer';
import { dpdtImage, dpdtMessage, dpdtMessageWithDownload, DPtypes, } from './DataProductsType';
import {createGuessDataType, processDatalinkTable} from './DataLinkProcessor';

const GIG= 1048576 * 1024;

/**
 *
 * @param {TableModel} table
 * @param {DataProductsConvertType} converterTemplate
 * @return {DataProductsConvertType}
 */
export function makeObsCoreConverter(table,converterTemplate) {
    const baseRetOb= {...converterTemplate,
        canGrid:false, maxPlots:1, hasRelatedBands:false, converterId: `ObsCore-${table.tbl_id}`};

    const propTypeCol= getObsCoreProdTypeCol(table);
    if (propTypeCol.enumVals) {
        const pTypes= propTypeCol.enumVals.split(',');
        if (pTypes.every( (s) => s.toLocaleLowerCase()==='image' || s.toLocaleLowerCase()==='cube')) {
            return {...baseRetOb, canGrid:true,maxPlots:8};
        }
    }

    if (get(table, 'request.filters')) {
        const fList= table.request.filters.split(';');
        const pTFilter= fList.find( (f) => f.includes(propTypeCol.name) && f.includes('IN'));
        if (pTFilter) {
            const inList=  pTFilter.substring( pTFilter.indexOf('(')+1, pTFilter.indexOf(')')).split(',');
            if (inList.every( (s) => s.toLocaleLowerCase()==='\'image\'' || s.toLocaleLowerCase()==='\'cube\'')) {
                return {...baseRetOb, canGrid:true,maxPlots:8};
            }
        }
    }

    return baseRetOb;
}


/**
 *
 * @param table
 * @param plotRows
 * @param activateParams
 * @return {Promise.<DataProductsDisplayType>}
 */
export function getObsCoreGridDataProduct(table, plotRows, activateParams) {
    const pAry= plotRows.map( (pR) => getObsCoreSingleDataProduct(table,pR.row,activateParams,false));
    return Promise.all(pAry).then ( (resultAry) => {
        const {imageViewerId, converterId}= activateParams;
        const requestAry= resultAry
            .filter( (result) => result && result.request && (
                result.displayType===DPtypes.IMAGE ||
                result.displayType===DPtypes.PROMISE ||
                result.displayType===DPtypes.ANALYZE) )
            .map( (result) => result.request);
        const activate= createGridImagesActivate(requestAry,imageViewerId,converterId, table.tbl_id, plotRows);
        return dpdtImage('image grid', activate,'image-grid-0',{requestAry});
    });
}


/**
 * Support ObsCore single product
 * @param table
 * @param row
 * @param {ActivateParams} activateParams
 * @param {boolean} doFileAnalysis - if true the build a menu if possible
 * @return {Promise.<DataProductsDisplayType>}
 */
export function getObsCoreSingleDataProduct(table, row, activateParams, doFileAnalysis= true) {

    const {size,titleStr,dataSource,prodType,isVoTable,isDataLink}= getObsCoreRowMetaInfo(table,row);

    if (!dataSource || (isVoTable && !isDataLink)) return Promise.resolve(dpdtMessage(`${prodType} is not supported`));
    if (!hasRowAccess(table, row)) return Promise.resolve(dpdtMessage('You do not have access to these data.'));


    const positionWP= makeWorldPtUsingCenterColumns(table,row);

    if (isDataLink) {
        return doFetchTable(makeFileRequest('dl table', dataSource))
            .then( (datalinkTable) =>
                processDatalinkTable(table,row,datalinkTable,positionWP,activateParams,titleStr, makeObsCoreRequest, doFileAnalysis) )
            .catch( (reason) =>
                dpdtMessageWithDownload(`No data to display: Could not retrieve datalink data, ${reason}`, 'Download File: '+titleStr, dataSource)
        );
    }
    else {
        if (size>GIG) return Promise.resolve(dpdtMessageWithDownload('Data is too large to load', 'Download File: '+titleStr, dataSource));
        return doFileAnalysis ?
            makeAnalysisGetSingleDataProduct(() => makeObsCoreRequest(dataSource, positionWP, titleStr))(table,row,activateParams,prodType) :
            createGuessDataType(titleStr,'guess-0',dataSource,prodType,makeObsCoreRequest,undefined,activateParams, positionWP,table,row,size);
    }
}



function getObsCoreRowMetaInfo(table,row) {
    if (!table) return {};
    const dataSource= getObsCoreAccessURL(table,row);
    const prodType= (getObsCoreProdType(table,row) || '').toLocaleLowerCase();
    const isVoTable= isFormatVoTable(table, row);
    const isDataLink= isFormatDataLink(table,row);
    const iName= getCellValue(table,row,'instrument_name') || '';
    const obsId= getCellValue(table,row,'obs_id') || '';
    const size= Number(getCellValue(table,row,'access_estsize')) || 0;
    let obsCollect= getCellValue(table,row,'obs_collection') || '';
    if (obsCollect===iName) obsCollect= '';

    const titleStr= `${obsCollect?obsCollect+', ':''}${iName?iName+', ':''}${obsId}`;
    return {iName,obsId,size,titleStr,dataSource,prodType,isVoTable,isDataLink};
}


/**
 *
 * @param dataSource
 * @param positionWP
 * @param titleStr
 * @return {undefined|WebPlotRequest}
 */
function makeObsCoreRequest(dataSource, positionWP, titleStr) {
    if (!dataSource) return undefined;

    const r = WebPlotRequest.makeURLPlotRequest(dataSource, 'DataProduct');
    r.setZoomType(ZoomType.FULL_SCREEN);
    if (titleStr.length>7) {
        r.setTitleOptions(TitleOptions.NONE);
        r.setTitle(titleStr);
    }
    else {
        r.setTitleOptions(TitleOptions.FILE_NAME);
    }
    r.setPlotId(dataSource);
    if (positionWP) r.setOverlayPosition(positionWP);

    return r;
}
