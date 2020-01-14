import {getDataLinkAccessUrls, getObsCoreAccessURL, getObsCoreProdType} from '../util/VOAnalyzer';
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
import {createTableActivate} from './converterUtils';
import {makeFileAnalysisActivate} from './MultiProductFileAnalyzer';
import {createSingleImageActivate} from './ImageDataProductsUtil';
import {dispatchUpdateActiveKey, getActiveMenuKey} from './DataProductsCntlr';

const analysisTypes= ['fits', 'cube', 'table', 'spectrum', 'auxiliary'];

const GIG= 1048576 * 1024;


/**
 *
 * @param {TableModel} sourceTable
 * @param {number} row
 * @param {TableModel} datalinkTable
 * @param {WorldPt} positionWP
 * @param {ActivateParams} activateParams
 * @param {String} titleStr
 * @param {Function} makeReq
 * @param {boolean} doFileAnalysis
 * @return {DataProductsDisplayType}
 */
export function processDatalinkTable(sourceTable, row, datalinkTable, positionWP, activateParams, titleStr, makeReq, doFileAnalysis=true) {
    const dataSource= getObsCoreAccessURL(sourceTable,row);
    const dataLinkData= getDataLinkAccessUrls(sourceTable, datalinkTable);
    const prodType= (getObsCoreProdType(sourceTable,row) || '').toLocaleLowerCase();
    const menu=  dataLinkData.length &&
         createDataLinkMenuRet(dataSource,dataLinkData,positionWP, sourceTable, row, activateParams,makeReq,prodType,doFileAnalysis);

    const hasData= menu.length>0;
    const canShow= menu.length>0 && menu.some( (m) => m.displayType!==DPtypes.DOWNLOAD && m.size<GIG);
    const activeMenuLookupKey= dataSource;


    if (canShow) {
        const {dpId}= activateParams;
        const activeMenuKey= getActiveMenuKey(dpId, dataSource);
        let index= menu.findIndex( (m) => m.menuKey===activeMenuKey);
        if (index<0) index= 0;
        dispatchUpdateActiveKey({dpId, activeMenuKeyChanges:{[dataSource]:menu[index].menuKey}});
        return dpdtFromMenu(menu,index,dataSource);
    }

    if (hasData) {
        const dMenu= menu.length && convertAllToDownload(menu);

        const msgMenu= [
            ...dMenu,
            dpdtTable('Show Datalink VO Table for list of products',
                createTableActivate(dataSource,'Datalink VO Table', activateParams),
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






function createDataLinkMenuRet(dataSource, dataLinkData, positionWP, sourceTable, row, activateParams, makeReq, prodType, doFileAnalysis) {
    const auxTot= dataLinkData.filter( (e) => e.semantics==='#auxiliary').length;
    let auxCnt=0;
    let primeCnt=0;
    const originalMenu= dataLinkData.reduce( (resultAry,e,idx) => {
        const ct= e.contentType.toLowerCase();
        const name= makeName(e.semantics, e.url, auxTot, auxCnt, primeCnt);
        const {semantics,size,url}= e;
        const activeMenuLookupKey= dataSource;

        if (ct.includes('tar')|| ct.includes('gz')) {
            let fileType;
            if (ct.includes('tar')) fileType= 'tar';
            if (ct.includes('gz')) fileType= 'gzip';
            resultAry.push(dpdtDownload('Download file: '+name,url,'dlt-'+idx,fileType,{semantics, size, activeMenuLookupKey}));
        }
        else if (ct.includes('jpeg') || ct.includes('png') || ct.includes('jpg') || ct.includes('gig')) {
            resultAry.push(dpdtPNG('Show PNG image: '+name,url,'dlt-'+idx,{semantics, size, activeMenuLookupKey}));
        }
        else if (e.size>GIG) {
            resultAry.push( dpdtDownload('Download FITS: '+name + '(too large to show)',url,'dlt-'+idx,'fits',{semantics, size, activeMenuLookupKey}));
        }
        else if (ct==='' || analysisTypes.find( (a) => ct.includes(a))) {
            if (doFileAnalysis) {
                resultAry.push( dpdtAnalyze('Show: '+name,undefined,url,'dlt-'+idx,{semantics, size, activeMenuLookupKey}));
            }
            else {
                const dpdt= createGuessDataType(name,'guess-'+idx,url,ct,makeReq,semantics, activateParams, positionWP,sourceTable,row,size);
                dpdt && resultAry.push(dpdt);
            }
        }
        if (e.semantics==='#auxiliary') auxCnt++;
        if (e.semantics==='#this') primeCnt++;
        return resultAry;
    }, []);

    let     newMenu= originalMenu.sort((s1,s2) => s1.semantics==='#this' ? -1 : 0);
    newMenu= newMenu.sort((s1,s2) => s1.name==='(#this)' ? -1 : 0);


    const menuWithDL= [...newMenu, ...addDataLinkEntries(dataSource,activateParams)];
    newMenu.forEach( (m,idx) => {
        if (m.displayType===DPtypes.ANALYZE) {
            const request= makeReq(m.url,positionWP,m.name);
            m.activate= makeFileAnalysisActivate(sourceTable,row, request,
                positionWP,activateParams,menuWithDL,m.menuKey, prodType);
            m.request= request;
        }
    });
    return newMenu;
}

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
    const {imageViewerId, converterId}= activateParams;
    if (ct.includes('image') || ct.includes('fits') || ct.includes('cube')) {
        const request= makeReq(url,positionWP,name);
        return dpdtImage(name,
            createSingleImageActivate(request,imageViewerId,converterId,table.tbl_id,row),
            menuKey, {request,url, semantics,size});
    }
    else if (ct.includes('table') || ct.includes('spectrum') || semantics.includes('auxiliary')) {
        return dpdtTable(name,
            createTableActivate(url, semantics, activateParams),
            menuKey,{url,semantics,size} );
    }
    else if (ct.includes('jpeg') || ct.includes('png') || ct.includes('jpg') || ct.includes('gig')) {
        return dpdtPNG(name,url,menuKey,{semantics});
    }
    else if (ct.includes('tar')|| ct.includes('gz')) {
        let fileType;
        if (ct.includes('tar')) fileType= 'tar';
        if (ct.includes('gz')) fileType= 'gzip';
        return dpdtDownload(name,url,menuKey,fileType,{semantics});
    }
}
