import {getDataLinkData, getObsCoreAccessURL, getObsCoreProdType, getServiceDescriptors} from '../util/VOAnalyzer';
import {
    dpdtAnalyze,
    dpdtDownload,
    dpdtFromMenu,
    dpdtImage,
    dpdtMessage,
    dpdtPNG,
    dpdtTable,
    DPtypes
} from './DataProductsType';
import {createTableActivate, createTableExtraction} from './converterUtils';
import {makeFileAnalysisActivate} from './MultiProductFileAnalyzer';
import {createSingleImageActivate, createSingleImageExtraction} from './ImageDataProductsUtil';
import {dispatchUpdateActiveKey, getActiveMenuKey} from './DataProductsCntlr';
import {GIG} from '../util/WebUtil.js';
import {dpdtDownloadMenuItem, dpdtMessageWithDownload} from './DataProductsType.js';




/**
 *
 * @param {TableModel} sourceTable
 * @param {number} row
 * @param {TableModel} datalinkTable
 * @param {WorldPt|undefined} positionWP
 * @param {ActivateParams} activateParams
 * @param {Function} makeReq
 * @param additionalServiceDescMenuList
 * @param {boolean} doFileAnalysis
 * @return {DataProductsDisplayType}
 */
export function processDatalinkTable(sourceTable, row, datalinkTable, positionWP, activateParams,
                                     makeReq, additionalServiceDescMenuList, doFileAnalysis=true) {
    const dataSource= getObsCoreAccessURL(sourceTable,row);
    const dataLinkData= getDataLinkData(datalinkTable);
    const prodType= (getObsCoreProdType(sourceTable,row) || '').toLocaleLowerCase();
    const descriptors= getServiceDescriptors(datalinkTable);
    const menu=  dataLinkData.length &&
         createDataLinkMenuRet(dataSource,dataLinkData,positionWP, sourceTable, row, activateParams,
             makeReq,prodType,descriptors, additionalServiceDescMenuList, doFileAnalysis);

    const hasData= menu.length>0;
    const canShow= menu.length>0 && menu.some( (m) => m.displayType!==DPtypes.DOWNLOAD && m.size<GIG);
    const activeMenuLookupKey= dataSource;


    if (canShow) {
        const {dpId}= activateParams;
        const activeMenuKey= getActiveMenuKey(dpId, dataSource);
        let index= menu.findIndex( (m) => m.menuKey===activeMenuKey);
        if (index<0) index= 0;
        dispatchUpdateActiveKey({dpId, activeMenuKeyChanges:{[activeMenuLookupKey]:menu[index].menuKey}});
        return dpdtFromMenu(menu,index,dataSource);
    }

    if (hasData) {
        const dMenu= menu.length && convertAllToDownload(menu);

        const msgMenu= [
            ...dMenu,
            dpdtTable('Show Datalink VO Table for list of products',
                createTableActivate(dataSource,'Datalink VO Table', activateParams),
                createTableExtraction(dataSource,'Datalink VO Table'),
                'nd0-showtable', {url:dataSource}),
            dpdtDownload ( 'Download Datalink VO Table for list of products', dataSource, 'nd1-downloadtable', 'vo-table' ),
        ];
        const msg= dMenu.length?
            'You may only download data for this row - nothing to display':
            'No displayable data available for this row';
        return dpdtMessage( msg, msgMenu, {activeMenuLookupKey,singleDownload:true});
    }
    else {
        return dpdtMessage('No data available for this row',undefined,{activeMenuLookupKey});
    }

}


function addDataLinkEntries(dataSource,activateParams) {
    return [
        dpdtTable('Show Datalink VO Table for list of products',
            createTableActivate(dataSource,'Datalink VO Table', activateParams),
            createTableExtraction(dataSource,'Datalink VO Table'),
            'datalink-entry-showtable', {url:dataSource}),
        dpdtDownload ( 'Download Datalink VO Table for list of products', dataSource, 'datalink-entry-downloadtable', 'vo-table' )
    ];

}



function convertAllToDownload(menu) {
    return menu.map( (d) =>  {
        if (d.displayType===DPtypes.DOWNLOAD) return {...d};
        if (d.url) return {...d, displayType:DPtypes.DOWNLOAD};
        if (d.request && d.request.getURL && d.request.getURL()) {
            return {...d,displayType:DPtypes.DOWNLOAD, url:d.request.getURL()};
        }
        else {
            return {};
        }
    }).filter( (d) => d.displayType);
}

const isThisSem= (semantics) => semantics==='#this';
const isAuxSem= (semantics) => semantics==='#auxiliary';

/**
 *
 * @param dataSource
 * @param dataLinkData
 * @param {WorldPt} positionWP
 * @param {TableModel} sourceTable
 * @param {number} row
 * @param {ActivateParams} activateParams
 * @param makeReq
 * @param prodType
 * @param {Array.<DataProductsDisplayType>} [additionalServiceDescMenuList]
 * @param {Array.<ServiceDescriptorDef>} [descriptors]
 * @param doFileAnalysis
 * @return {Array.<DataProductsDisplayType>}
 */
function createDataLinkMenuRet(dataSource, dataLinkData, positionWP, sourceTable, row, activateParams, makeReq,
                               prodType, descriptors, additionalServiceDescMenuList, doFileAnalysis=true) {
    const auxTot= dataLinkData.filter( (e) => e.semantics==='#auxiliary').length;
    let auxCnt=0;
    let primeCnt=0;
    const menu=[];
    dataLinkData.forEach( (e,idx) => {
        const contentType= e.contentType.toLowerCase();
        const name= makeName(e.semantics, e.url, auxTot, auxCnt, primeCnt);
        const {semantics,size,url, serviceDefRef}= e;
        const activeMenuLookupKey= dataSource;
        const menuKey= 'dlt-'+idx;
        let menuEntry;

        if (serviceDefRef) {
            const servDesc= descriptors.find( ({ID}) => ID===serviceDefRef);
            if (servDesc) {
                const {title,accessURL,standardID,urlParams, ID}= servDesc;
                const request= makeReq(accessURL,positionWP,title);
                const allowsInput=urlParams.some( (p) => p.allowsInput);
                const activate= makeFileAnalysisActivate(sourceTable,row, request, positionWP,activateParams,menuKey,
                                       undefined, urlParams, name);
                menuEntry= dpdtAnalyze(`Show: ${name} ${allowsInput?' (Input Required)':''}` ,
                    activate,accessURL,urlParams,menuKey,
                    {activeMenuLookupKey,request, allowsInput,
                        standardID, ID, semantics,size});
            }
        }
        else if (url) {
            if (isDownloadType(contentType)) {
                let fileType;
                if (contentType.includes('tar')) fileType= 'tar';
                if (contentType.includes('gz')) fileType= 'gzip';
                menuEntry= isThisSem(semantics) ?
                    dpdtDownloadMenuItem('Download file: '+name,url,menuKey,fileType,{semantics, size, activeMenuLookupKey}) :
                    dpdtDownload('Download file: '+name,url,menuKey,fileType,{semantics, size, activeMenuLookupKey});
            }
            else if (isImageType(contentType)) {
                menuEntry= dpdtPNG('Show PNG image: '+name,url,menuKey,{semantics, size, activeMenuLookupKey});
            }
            else if (isTooBig(size)) {
                menuEntry= dpdtDownload('Download: '+name + '(too large to show)',url,menuKey,'fits',{semantics, size, activeMenuLookupKey});
            }
            else if (isAnalysisType(contentType)) {
                if (doFileAnalysis) {
                    const request= makeReq(url,positionWP,name);
                    const activate= makeFileAnalysisActivate(sourceTable,row, request, positionWP,activateParams,menuKey, prodType);
                    menuEntry= dpdtAnalyze('Show: '+name,activate,url,undefined, menuKey,{semantics, size, activeMenuLookupKey,request});
                }
                else {
                    menuEntry= createGuessDataType(name,menuKey,url,contentType,makeReq,semantics, activateParams, positionWP,sourceTable,row,size);
                }
            }
        }
        menuEntry && menu.push(menuEntry);
        if (isAuxSem(e.semantics)) auxCnt++;
        if (isThisSem(e.semantics)) primeCnt++;
    });
    if (additionalServiceDescMenuList) {
        menu.push(...additionalServiceDescMenuList);
    }

    menu.push(...addDataLinkEntries(dataSource,activateParams));
    return menu
        .sort(({semantics:sem1,name:n1},{semantics:sem2,name:n2}) => {
            if (isThisSem(sem1)) {
                if (isThisSem(sem2)) {
                    if (n1?.includes('(#this)')) return -1;
                    else if (n2?.includes('(#this)')) return 1;
                    else if (n1<n2) return -1;
                    else if (n1>n2) return 1;
                    else return 0;
                }
                else return -1;
            }
            else {
                return 0;
            }
        })
        .sort((s1) => s1.name==='(#this)' ? -1 : 0);
}



const analysisTypes= ['fits', 'cube', 'table', 'spectrum', 'auxiliary'];

const isImageType= (ct) => (ct.includes('jpeg') || ct.includes('png') || ct.includes('jpg') || ct.includes('gig'));
const isDownloadType= (ct) => ct.includes('tar') || ct.includes('gz') || ct.includes('octet-stream');
const isAnalysisType= (ct) => (ct==='' || analysisTypes.some( (a) => ct.includes(a)));
const isTooBig= (size) => size>GIG;

function makeName(s='', url, auxTot, autCnt, primeCnt=0) {
    let name= (s==='#this' && primeCnt>0) ? '#this '+primeCnt  : s;
    name= s.startsWith('#this') ? `Primary product (${name})` : s;
    name= name[0]==='#' ? name.substring(1) : name;
    name= (name==='auxiliary' && auxTot>1) ? `${name}: ${autCnt}` : name;
    return name || url;
}


/**
 *
 * @param name
 * @param menuKey
 * @param url
 * @param ct
 * @param makeReq
 * @param semantics
 * @param activateParams
 * @param positionWP
 * @param table
 * @param row
 * @param size
 * @return {DataProductsDisplayType}
 */
export function createGuessDataType(name, menuKey, url,ct,makeReq, semantics, activateParams, positionWP, table,row,size) {
    const {imageViewerId}= activateParams;
    if (ct.includes('image') || ct.includes('fits') || ct.includes('cube')) {
        const request= makeReq(url,positionWP,name);
        return dpdtImage(name,
            createSingleImageActivate(request,imageViewerId,table.tbl_id,row),
            createSingleImageExtraction(request),
            menuKey, {request,url, semantics,size});
    }
    else if (ct.includes('table') || ct.includes('spectrum') || semantics.includes('auxiliary')) {
        return dpdtTable(name,
            createTableActivate(url, semantics, activateParams, ct),
            menuKey,{url,semantics,size} );
    }
    else if (isImageType(ct)) {
        return dpdtPNG(name,url,menuKey,{semantics});
    }
    else if (isDownloadType(ct)) {
        let fileType;
        if (ct.includes('tar')) fileType= 'tar';
        if (ct.includes('gz')) fileType= 'gzip';
        return dpdtDownload(name,url,menuKey,fileType,{semantics});
    }
}
