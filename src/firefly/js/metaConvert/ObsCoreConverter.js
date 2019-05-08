import {get} from 'lodash';
import {WebPlotRequest, TitleOptions} from '../visualize/WebPlotRequest.js';
import {getCellValue, doFetchTable, hasRowAccess} from '../tables/TableUtil.js';
import {getObsCoreAccessURL, getObsCoreProdType} from '../util/VOAnalyzer.js';
import {
    findTableCenterColumns,
    getDataLinkAccessUrls,
    getObsCoreProdTypeCol,
    isFormatDataLink,
    isFormatVoTable
} from '../util/VOAnalyzer';
import {makeFileRequest} from '../tables/TableRequestUtil';
import {ZoomType} from '../visualize/ZoomType.js';
import {makeWorldPt} from '../visualize/Point.js';
import {createGridImagesActivate, createSingleImageActivate} from './ImageDataProductsUtil.js';
import {dispatchTableSearch} from '../tables/TablesCntlr';

const GIG= 1048576 * 1024;
const previewTableId= 'OBCORE-TBL';

/**
 *
 * @param {TableModel} table
 * @param {DataProductsConvertType} converterTemplate
 * @return {DataProductsConvertType}
 */
export function makeObsCoreConverter(table,converterTemplate) {
    const ret= {...converterTemplate, converterId: `ObsCore-${table.tbl_id}`};
    ret.hasRelatedBands= false;

    const propTypeCol= getObsCoreProdTypeCol(table);
    if (propTypeCol.enumVals) {
        const pTypes= propTypeCol.enumVals.split(',');
        if (pTypes.every( (s) => s.toLocaleLowerCase()==='image' || s.toLocaleLowerCase()==='cube')) {
            ret.canGrid= true;
            ret.maxPlots= 8;
            return ret;
        }
    }
    const filters= get(table, 'request.filters');
    if (filters) {
        const fList= filters.split(';');
        const pTFilter= fList.find( (f) => f.includes(propTypeCol.name) && f.includes('IN'));
        if (pTFilter) {
            const inList=  pTFilter.substring( pTFilter.indexOf('(')+1, pTFilter.indexOf(')')).split(',');
            if (inList.every( (s) => s.toLocaleLowerCase()==='\'image\'' || s.toLocaleLowerCase()==='\'cube\'')) {
                ret.canGrid= true;
                ret.maxPlots= 8;
                return ret;
            }
        }
    }

    ret.canGrid= false;
    ret.maxPlots= 1;

    return ret;
}



export function getObsCoreGridDataProduct(table, plotRows, activateParams) {
    const pAry= plotRows.map( (pR) => getObsCoreSingleDataProduct(table,pR.row,activateParams,false));
    return Promise.all(pAry).then ( (resultAry) => {
        const {imageViewerId, converterId}= activateParams;
        const requestAry= resultAry
            .filter( (result) => result && result.displayType==='images')
            .map( (result) => result.request);
        const activate= createGridImagesActivate(requestAry,imageViewerId,converterId, table.tbl_id, plotRows);
        return { displayType:'images', activate, requestAry, menu:undefined};
    });
}



/**
 *  Support data the we don't know about
 * @param table
 * @param row
 * @param {ActivateParams} activateParams
 * @param {boolean} includeMenu - if true the build a menu if possible
 * @return {{}}
 */
export function getObsCoreSingleDataProduct(table, row, activateParams, includeMenu= true) {

    const dataSource= getObsCoreAccessURL(table,row);
    const prodType= (getObsCoreProdType(table,row) || '').toLocaleLowerCase();
    const imageType= prodType.includes('image') || prodType.includes('cube');
    const tableType= prodType.includes('spectrum');
    const canHandleProdType= imageType || tableType;
    const isVoTable= isFormatVoTable(table, row);
    const isDataLink= isFormatDataLink(table,row);

    if (!dataSource || (isVoTable && !isDataLink)) {
        return Promise.resolve({displayType:'message', message: prodType +' is not yet supported'});
    }

    if (!hasRowAccess(table, row)) {
        return Promise.resolve({displayType:'message', message: 'You do not have access to these data.'});
    }

    let obsCollect= getCellValue(table,row,'obs_collection') || '';
    const iName= getCellValue(table,row,'instrument_name') || '';
    const obsId= getCellValue(table,row,'obs_id') || '';
    const size= Number(getCellValue(table,row,'access_estsize')) || 0;
    if (obsCollect===iName) obsCollect= '';

    const titleStr= `${obsCollect?obsCollect+', ':''}${iName?iName+', ':''}${obsId}`;

    const cen= findTableCenterColumns(table);
    const positionWP= cen && makeWorldPt(getCellValue(table,row,cen.lonCol), getCellValue(table,row,cen.latCol), cen.csys);

    if (isDataLink) {
        const tableReq= makeFileRequest('no title', dataSource, { startIdx : 0, pageSize : 1000});

        return doFetchTable(tableReq).then(
            (datalinkTable) => {
                const dataLinkData= getDataLinkAccessUrls(table, datalinkTable);
                const menu=  includeMenu && dataLinkData.length>2 ? createMenuRet(dataLinkData,positionWP, table, row, activateParams) : undefined;
                const filterDLData= dataLinkData
                    .filter(({contentType,size}) => {
                        return (contentType.toLowerCase().includes('fits') && (size<=GIG));
                    });

                if (!filterDLData.length) {
                    if (dataLinkData.length>0) {
                        return {
                            displayType: 'message', message: 'No displayable data available for this row',
                            menu: [
                                {
                                    name: 'Show Datalink VO Table for list of products',
                                    type: 'table',
                                    activate: createTableActivate(dataSource,'Datalink VO Table', activateParams),
                                    url: dataSource
                                },
                                {
                                    name: 'Download Datalink VO Table for list of products',
                                    type: 'download',
                                    url: dataSource
                                },
                            ]
                        };
                    }
                    else {
                        return {displayType: 'message', message : 'No data available for this row'};
                    }
                }
                let idx= filterDLData.findIndex( (item) => item.semantics.includes('this'));
                if (idx<0) idx= 0;

                const canHandle= filterDLData[idx].contentType.toLowerCase().includes('fits');
                if (!canHandle) {
                    return {displayType: 'message', message: filterDLData[idx].contentType+' is not yet supported', menu};
                }
                const req= makeObsCoreRequest(filterDLData[idx].url,positionWP,titleStr);
                return makeObsCoreImageDisplayType(req,table,row,menu,activateParams);

            }
        ).catch(
            (reason) => {
                console.warn(`Failed to catalog plot data: ${reason}`, reason);
            }
        );

    }
    else {
        let retVal;
        if (imageType) {
            if (size<GIG) {
                const req= makeObsCoreRequest(dataSource,positionWP,titleStr);
                retVal= makeObsCoreImageDisplayType(req,table,row,undefined,activateParams);
            }
            else {
                retVal= {
                    displayType: 'message',
                    message : 'Image data is too large to load',
                    menu : [ { name: 'Download FITS: '+titleStr + ' (too large to show)', type : 'download', url : dataSource} ]
                };
            }
        }
        else if (tableType) {
            retVal= {displayType:'table', menu:undefined, activate: createTableActivate(dataSource,titleStr, activateParams)};
        }
        else {
            retVal= {
                displayType: 'message',
                message: prodType +' is not yet supported',
                menu : [ { name: 'Download File: '+titleStr , type : 'download', url : dataSource} ]
            };
        }
        return Promise.resolve(retVal);
    }
}


function makeName(s='', url, auxTot, autCnt) {
    let name= s==='#this' ? 'Primary product (#this)' : s;
    name= name[0]==='#' ? name.substring(1) : name;
    name= (name==='auxiliary' && auxTot>1) ? `${name}: ${autCnt}` : name;
    return name || url;
}

function createMenuRet(dataLinkData, positionWP, table, row, activateParams) {
    const {imageViewerId, converterId}= activateParams;
    const auxTot= dataLinkData.filter( (e) => e.semantics==='#auxiliary').length;
    let auxCnt=0;
    const originalMenu= dataLinkData.reduce( (resultAry,e) => {
        const ct= e.contentType.toLowerCase();
        const name= makeName(e.semantics, e.url, auxTot, auxCnt);
        if (ct.includes('fits') || ct.includes('cube')) {
            if (e.size<GIG) {
                const request= makeObsCoreRequest(e.url,positionWP,e.semantics);
                resultAry.push( {
                    name: 'Show FITS Image: '+name,
                    type : 'image',
                    request,
                    url: e.url,
                    activate : createSingleImageActivate(request,imageViewerId,converterId,table.tbl_id,row),
                    semantics: e.semantics
                });
            }
            else {
                resultAry.push( { name: 'Download FITS: '+name + '(too large to show)', type : 'download', url : e.url , semantics: e.semantics });
            }
        }
        if (ct.includes('table') || ct.includes('spectrum') || e.semantics.includes('auxiliary')) {
            resultAry.push( { name: 'Show Table: ' + name, type : 'table', url: e.url, semantics: e.semantics,
                activate: createTableActivate(e.url, e.semantics, activateParams)});
        }
        if (ct.includes('jpeg') || ct.includes('png') || ct.includes('jpg') || ct.includes('gig')) {
            resultAry.push({ name: 'Show PNG image: '+name, type : 'png', url : e.url, semantics: e.semantics  });
        }
        if (ct.includes('tar')|| ct.includes('gz')) {
            resultAry.push({ name: 'Download file: '+name, type : 'download', url : e.url , semantics: e.semantics });
        }
        if (e.semantics==='#auxiliary') auxCnt++;
        return resultAry;
    }, []);

    return originalMenu.sort((s1,s2) => s1.semantics==='#this' ? -1 : 0);


}

function makeObsCoreImageDisplayType(request, table, row, menu, activateParams) {
    const {imageViewerId, converterId}= activateParams;
    const activate= createSingleImageActivate(request,imageViewerId,converterId,table.tbl_id,row);
    return { displayType:'images', activate, request, menu};
}


function makeObsCoreRequest(dataSource, positionWP, titleStr) {
    if (!dataSource) return null;

    const r = WebPlotRequest.makeURLPlotRequest(dataSource, 'Fits Image');
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


function createTableActivate(url, titleStr, activateParams) {
    return () => {
        const {tableGroupViewerId}= activateParams;
        const dataTableReq= makeFileRequest(titleStr, url, undefined, { tbl_id:previewTableId, startIdx : 0, pageSize : 100});
        dispatchTableSearch(dataTableReq,
            {
                noHistory: true,
                removable:false,
                tbl_group: tableGroupViewerId,
                backgroundable: false,
                showFilters: true,
                showInfoButton: true
            });
    };
}
