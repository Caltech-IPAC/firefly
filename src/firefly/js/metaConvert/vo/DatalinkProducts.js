import {cloneRequest, MAX_ROW} from '../../tables/TableRequestUtil';
import {doFetchTable, getCellValue, getMetaEntry} from '../../tables/TableUtil.js';
import {getPreferCutout} from '../../ui/tap/Cutout';
import {logger} from '../../util/Logger';
import {visRoot} from '../../visualize/ImagePlotCntlr';
import {primePlot} from '../../visualize/PlotViewUtil';
import {getObsCoreAccessURL} from '../../voAnalyzer/TableAnalysis';
import {getDataLinkData} from '../../voAnalyzer/VoDataLinkServDef.js';
import {Band} from '../../visualize/Band.js';
import {WPConst} from '../../visualize/WebPlotRequest.js';
import {createObsCoreImageTitle} from '../AnalysisUtils';
import {GROUP_BY_DATALINK_RESULT, IMAGE_ONLY, TABLE_ONLY} from '../DataProductConst';
import {
    dispatchUpdateActiveKey, dispatchUpdateDataProducts, getActiveMenuKey, getCurrentActiveKeyID
} from '../DataProductsCntlr';
import {
    dpdtFromMenu, dpdtImage, dpdtMessageWithDownload, dpdtMessageWithError, dpdtSimpleMsg, dpdtWorkingMessage, DPtypes
} from '../DataProductsType.js';
import {
    createGridImagesActivate, createRelatedGridImagesActivate, createSingleImageExtraction
} from '../ImageDataProductsUtil.js';
import {fetchAllDatalinkTables, fetchDatalinkTable} from './DatalinkFetch.js';
import {
    findMenuKeyWithName, getCutoutSizeWarning, getCutoutTotalWarning, hasBandInMenuKey, hasLabelInMenuKey,
    IMAGE, makeMenuEntry, processDatalinkTable, RELATED_IMAGE_GRID, sortRelatedGridUsingRequest, SPECTRUM, USE_ALL
} from './DataLinkProcessor.js';


/**
 *
 * @param {Object} obj
 * @param obj.dlTableUrl
 * @param obj.activateParams
 * @param obj.table
 * @param obj.row
 * @param obj.threeColorOps
 * @param obj.titleStr
 * @param obj.options
 * @return {Promise<DataProductsDisplayType>}
 */
export async function getDatalinkRelatedImageGridProduct({dlTableUrl, activateParams, table, row, threeColorOps, titleStr, options}) {
    try {

        if (options.limitViewerDisplay===TABLE_ONLY) return dpdtSimpleMsg('Configuration Error: No support for related spectrum');

        dispatchUpdateDataProducts(activateParams.dpId, dpdtWorkingMessage('Loading data products...', 'working'));
        const datalinkTable = await fetchDatalinkTable(dlTableUrl, options.datalinkTblRequestOptions);
        const preferCutout= getPreferCutout(options.dataProductsComponentKey,table?.tbl_id);

        const gridData = getDataLinkData(datalinkTable,false, table,row).filter(({dlAnalysis}) => dlAnalysis.isGrid && dlAnalysis.isImage);
        if (!gridData.length) return dpdtSimpleMsg('no support for related grid in datalink file');

        const cutoutSwitching= dataSupportsCutoutSwitching(gridData);

        const dataLinkGrid = processDatalinkTable({
            sourceTable: table, row, datalinkTable, activateParams,
            baseTitle: titleStr, dlTableUrl, doFileAnalysis: false,
            options, parsingAlgorithm: RELATED_IMAGE_GRID,
        });


        const {imageViewerId} = activateParams;
        const requestAry = dataLinkGrid.menu
            .filter((result) => result?.request && (
                result.displayType === DPtypes.IMAGE ||
                result.displayType === DPtypes.PROMISE ||
                result.displayType === DPtypes.ANALYZE))
            .map((result) => result.request);

        const dlDataAry = dataLinkGrid.menu
            .filter((result) => result?.dlData && (
                result.displayType === DPtypes.IMAGE ||
                result.displayType === DPtypes.PROMISE ||
                result.displayType === DPtypes.ANALYZE))
            .map((result) => result.dlData);

        const extractItems = dataLinkGrid.menu.filter((result) => result.displayType === DPtypes.EXTRACT);

        
        requestAry.forEach((r, idx) => r.setPlotId(r.getPlotId() + '-related_grid-' + idx));

        const threeColorReqAry= (threeColorOps && requestAry.length>1) &&
                                          make3ColorRequestAry(requestAry,threeColorOps,datalinkTable.tbl_id);
        const onActivePvChanged= getOnActivePvChanged(dlTableUrl,dataLinkGrid,activateParams,options);
        let highlightPlotId;
        if (options.relatedGridImageOrder?.length) {
            const {dpId}= activateParams;
            const activeMenuLookupKey= dlTableUrl;
            const activeMenuKey= getActiveMenuKey(dpId, activeMenuLookupKey);
            const idx= requestAry.findIndex( (r) => hasLabelInMenuKey(activeMenuKey, r.getTitle()) );
            highlightPlotId= (idx>-1) ? requestAry[idx].getPlotId() : undefined;
        }
        const activate = createRelatedGridImagesActivate({requestAry, threeColorReqAry, imageViewerId,
            tbl_id:table.tbl_id, onActivePvChanged, highlightPlotId });
        const extraction = createSingleImageExtraction(requestAry);

        const gridDlData= {...dlDataAry[0],
            cutoutToFullWarning: (preferCutout && cutoutSwitching) ? getCutoutTotalWarning(dlDataAry,requestAry.length) : undefined};

        const item= dpdtImage({name:'image grid', activate, extraction,
            dlData: gridDlData,
            enableCutout:preferCutout,
            menu: extractItems,
            gridForceRowSize: options.gridForceRowSize,
            menuKey:'image-grid-0',serDef:dataLinkGrid.serDef});
        item.menu= extractItems;
        return item;
    } catch (reason) {
        return dpdtMessageWithDownload(`No data to display: Could not retrieve datalink data, ${reason}`, 'Download File: ' + titleStr, dlTableUrl);
    }
}


export async function getObsCoreRelatedDataProductByFilter(table, row, threeColorOps, highlightPlotId, activateParams, options) {

    dispatchUpdateDataProducts(activateParams.dpId, dpdtWorkingMessage('Loading data products...', 'working'));
    const preferCutout= getPreferCutout(options.dataProductsComponentKey,table?.tbl_id);
    const {relatedGridImageOrder,gridForceRowSize= undefined}= options ?? {};

    try {
        const filteredTbl= await fetchRelatedColsFilteredTable(table,row);
        const dlTableAry= await fetchAllDatalinkTables(filteredTbl,options.datalinkTblRequestOptions);
        const {processedGrid,gridDlData, errMsg}=
            convertDLTableAryToImageGrid(dlTableAry,filteredTbl,preferCutout,activateParams,options);
        if (errMsg) return dpdtSimpleMsg(errMsg);


        const requestAry= processedGrid.map( (g) => g.request);
        requestAry.forEach((r, idx) => r.setPlotId(r.getPlotId() + '-related_grid-' + idx));
        const threeColorReqAry= (threeColorOps && requestAry.length>1) &&
            make3ColorRequestAry(requestAry,threeColorOps,table.tbl_id);
        const onActivePvChanged= getOnActivePvChanged(getObsCoreAccessURL(filteredTbl,0),processedGrid,activateParams,options);
        const {imageViewerId} = activateParams;
        const sortedReqAry= relatedGridImageOrder ? sortRelatedGridUsingRequest(requestAry,relatedGridImageOrder) : requestAry;
        const activate = createRelatedGridImagesActivate({requestAry:sortedReqAry, threeColorReqAry, imageViewerId,
            tbl_id:table.tbl_id, onActivePvChanged, highlightPlotId });
        const extraction = createSingleImageExtraction(sortedReqAry);

        const item= dpdtImage({name:'image grid', activate, extraction,
            dlData: gridDlData,
            enableCutout:preferCutout,
            menu: [],
            gridForceRowSize,
            menuKey:'image-grid-0',});

        return item;
    }
    catch (reason) {
        logger.error(`can't filter table: ${reason}`, reason);
        return dpdtSimpleMsg('can\'t filter table');
    }

}


async function fetchRelatedColsFilteredTable(table,row) {
    const s= getMetaEntry(table,'tbl.relatedCols');
    const colNameAry= s.split(',').map( (c) => c.trim());
    const values= colNameAry.map( (n) => getCellValue(table,row,n));
    const filters= colNameAry.reduce ( (str,cName,idx) =>
        `${str}${!idx?'':' AND '}"${cName}" = '${values[idx]}'`, '');
    const request = cloneRequest(table.request, { startIdx : 0, pageSize : MAX_ROW, filters});

    try {
        return await doFetchTable(request);
    }
    catch (reason) {
        logger.error('could not fetch table', reason);
    }
}

function convertDLTableAryToImageGrid(dlTableAry, filteredTbl, preferCutout, activateParams=undefined, options=undefined) {
    const dlDataAry =
        dlTableAry.map( (t,idx) => {
            const dlList= getDataLinkData(t,false, filteredTbl,idx).filter(({dlAnalysis}) => dlAnalysis.isThis && dlAnalysis.isImage);
            return dlList[0];
        });
    if (!dlDataAry.length) return {errMsg:dpdtSimpleMsg('no support for related grid for this table')};
    const cutoutSwitching= dataSupportsCutoutSwitching(dlDataAry);

    const processedGrid= dlDataAry.map( (dlData, idx) => {
        const {dlAnalysis:{cutoutFullPair}}= dlData;
        const dlTableUrl= getObsCoreAccessURL(filteredTbl,idx);
        // const name= makeName(dlData, url, 0, 0, 0, 'todo title');
        const name= createObsCoreImageTitle(filteredTbl,idx);
        const baseTitle= '';

        const menuParams= {dlTableUrl,dlData,idx, baseTitle, filteredTbl, ropDownText:name,
            sourceRow:idx, options, name, doFileAnalysis:false, activateParams};
        if (cutoutFullPair) {
            dlData.relatedDLEntries.cutout.cutoutToFullWarning= getCutoutSizeWarning(dlData);
            if (preferCutout) {
                menuParams.dlData = dlData.relatedDLEntries.cutout;
            }
        }
        return makeMenuEntry({dlTableUrl, dlData:menuParams.dlData,idx, baseTitle, sourceTable:filteredTbl, sourceRow:idx,options,
            name, doFileAnalysis:false, activateParams});
    });

    const gridDlData= {...dlDataAry[0],
        cutoutToFullWarning: (preferCutout && cutoutSwitching) ? getCutoutTotalWarning(dlDataAry,processedGrid.length) : undefined};

    return {processedGrid,dlDataAry, gridDlData, errMsg:undefined};
}



function getOnActivePvChanged(dlTableUrl, dataLinkGrid, activateParams, options) {
    if (!options.relatedGridImageOrder?.length) return;
    const grid= dataLinkGrid.menu ?? dataLinkGrid;
    const menuKeyAry= grid.map( (m) => m.menuKey);
    if (!menuKeyAry?.length) return;
    return (plotId) => {
        const activeMenuLookupKey= dlTableUrl;
        const menuKey= findMenuKeyWithName(menuKeyAry,primePlot(visRoot(),plotId)?.title);
        if (!menuKey) return;
        dispatchUpdateActiveKey({dpId: activateParams.dpId, activeMenuKeyChanges:{[activeMenuLookupKey]:menuKey}});
    };
}

const dataSupportsCutoutSwitching= (gridData) => gridData.every( (dl) => dl.dlAnalysis.cutoutFullPair);

function make3ColorRequestAry(requestAry,threeColorOps,tbl_id) {
    const plotId= `3id_${tbl_id}`;
    return [
        threeColorOps[0] ? requestAry[threeColorOps[0]]?.makeCopy({[WPConst.PLOT_ID]:plotId}) :undefined,
        threeColorOps[1] ? requestAry[threeColorOps[1]]?.makeCopy({[WPConst.PLOT_ID]:plotId}) :undefined,
        threeColorOps[2] ? requestAry[threeColorOps[2]]?.makeCopy({[WPConst.PLOT_ID]:plotId}) :undefined,
    ];
}

/**
 *
 * @param {Object} obj
 * @param obj.dlTableUrl
 * @param obj.options
 * @param obj.sourceTable
 * @param obj.row
 * @param obj.activateParams
 * @param obj.titleStr
 * @param obj.doFileAnalysis
 * @param obj.additionalServiceDescMenuList
 * @param {boolean} [obj.useForTableGrid] - this result is part of a table grid Result
 * @return {Promise<DataProductsDisplayType>}
 */
export async function getDatalinkSingleDataProduct({ dlTableUrl,
                                                       options,
                                                       sourceTable,
                                                       row,
                                                       activateParams,
                                                       titleStr = 'datalink table',
                                                       doFileAnalysis = true,
                                                       additionalServiceDescMenuList,
                                                       useForTableGrid}) {
    try {
        dispatchUpdateDataProducts(activateParams.dpId, dpdtWorkingMessage('Loading data products...', 'working'));
        const datalinkTable = await fetchDatalinkTable(dlTableUrl, options.datalinkTblRequestOptions);
        let parsingAlgorithm = USE_ALL;
        if (options.limitViewerDisplay===IMAGE_ONLY) parsingAlgorithm = IMAGE;
        if (options.limitViewerDisplay===TABLE_ONLY) parsingAlgorithm = SPECTRUM;

        return processDatalinkTable({
            sourceTable, row, datalinkTable, activateParams, baseTitle: titleStr,
            options, parsingAlgorithm, doFileAnalysis,
            dlTableUrl, additionalServiceDescMenuList, useForTableGrid
        });
    } catch (reason) {
        //todo - what about if when the data link fetch fails but there is a serviceDescMenuList - what to do? does it matter?
        if (reason.cause?.includes('DataAccessException')) {
            const eStr=  reason.cause;
            if (Number(eStr?.split('status').pop().trim().split(' ')[0]) > 0) { // if there is a number status in the cause
                return dpdtMessageWithError(eStr, [dlTableUrl]);
            }
        }
        return dpdtMessageWithDownload(`No data to display: Could not retrieve datalink data, ${reason}`, 'Download File: ' + titleStr, dlTableUrl);
    }
}


export async function createGridResult(promiseAry, activateParams, table, plotRows,options) {



    return Promise.all(promiseAry).then((resultAry) => {
        const {hasRelatedBands }= options;
        const {dpId}= activateParams;
        const activeMenuLookupKey= table.tbl_id;
        let activeMenuKey= getActiveMenuKey(dpId, activeMenuLookupKey);
        const lastSource= getCurrentActiveKeyID(dpId);
        let menuIndex= 0;
        activeMenuKey= getActiveMenuKey(dpId, lastSource);

        const isAllRelatedImageGrid= hasRelatedBands && resultAry
            ?.every( ({menu}) => menu
                ?.filter( ({dlData:dl}) => dl?.dlAnalysis?.isImage && dl?.dlAnalysis?.isGrid).length>1 );

        // const primeIdx= resultAry[0].menu.findIndex( (m) => m.menuKey=== resultAry[0].menuKey);
        const mi= makeGridMi(resultAry,activateParams,table,plotRows,activeMenuKey);
        if (isAllRelatedImageGrid) {
            const hasBandpassDefined= resultAry
                .every( (r) => r.menu
                    .every( (m) => m.dlData.bandpassNameDLExt));
            const bandPassSet= hasBandpassDefined &&
                new Set(...resultAry
                    .map( (r) => r.menu
                        .map( (m) => m.dlData.bandpassNameDLExt)));
            if (bandPassSet.size) {
                mi.menu= makeAllRelatedGridMenu(resultAry,bandPassSet,activateParams,table,plotRows,options);
                menuIndex= mi.menu.findIndex( (m) => m.menuKey===activeMenuKey);
                if (menuIndex===-1) menuIndex=0;
                dispatchUpdateActiveKey({dpId, activeMenuKeyChanges:{[activeMenuLookupKey]:mi.menu[menuIndex].menuKey}});
            }

        }

        return mi.menu ? dpdtFromMenu(mi.menu,menuIndex,table.tbl_id) : mi;
    });
}


function makeAllRelatedGridMenu(resultAry, bandPassSet, activateParams, table, plotRows) {
    const highlightedIdx= plotRows.findIndex( (r) => r.highlight);

    const flatResult= resultAry.map( (r) => r.menu).flat();
    const bandMenuAry= [...bandPassSet].map( (bandPass) =>
        flatResult.filter( (mi) => hasBandInMenuKey(mi.menuKey,bandPass) ) );


    return bandMenuAry
        .map((bandMenu, idx) => {
            const entry= makeGridMi(bandMenuAry[idx], activateParams, table, plotRows, resultAry[0].menuKey);
            entry.name= bandMenu[highlightedIdx]?.name ?? 'Image Data';
            entry.menuKey= bandMenu[highlightedIdx]?.menuKey ?? 'grid-item'+idx;
            return entry;
        } )
        .filter(Boolean);
    

}


function makeGridMi(bandMenu, activateParams, table, plotRows, menuKey) {
    const {imageViewerId} = activateParams;
    const requestAry = bandMenu
        .filter((result) => result?.request && (
            result?.displayType === DPtypes.IMAGE ||
            result?.displayType === DPtypes.PROMISE ||
            result?.displayType === DPtypes.ANALYZE))
        .map((result) => result.request);
    const activate = createGridImagesActivate(requestAry, imageViewerId, table.tbl_id, plotRows);
    const extraction = createSingleImageExtraction(requestAry);
    return dpdtImage({name:'image grid', activate, extraction, menuKey});
}


export async function datalinkDescribeThreeColor(dlTableUrl, table, row, options={}) {
    const {relatedBandMethod=GROUP_BY_DATALINK_RESULT }= options;
    let dataLinkGrid;
    let gridDLData;
    if (relatedBandMethod===GROUP_BY_DATALINK_RESULT) {
        const datalinkTable = await fetchDatalinkTable(dlTableUrl, options.datalinkTblRequestOptions);
        const results = processDatalinkTable({
            sourceTable: table, row, datalinkTable, activateParams:{},
            baseTitle: '', dlTableUrl, doFileAnalysis: false,
            options, parsingAlgorithm: RELATED_IMAGE_GRID
        });
        dataLinkGrid = results.menu;
        gridDLData= dataLinkGrid.map( (d) => d.dlData);
    }
    else {
        const filteredTbl= await fetchRelatedColsFilteredTable(table,row);
        const dlTableAry= await fetchAllDatalinkTables(filteredTbl,options.datalinkTblRequestOptions);
        const preferCutout= getPreferCutout(options.dataProductsComponentKey,table?.tbl_id);
        const {processedGrid}= convertDLTableAryToImageGrid(dlTableAry,filteredTbl,preferCutout,{});
        dataLinkGrid= processedGrid;
        gridDLData= processedGrid.map( (d) => d.dlData);
    }

    const bandData = dataLinkGrid
        .filter((result) => result?.request && (
            result.displayType === DPtypes.IMAGE ||
            result.displayType === DPtypes.PROMISE ||
            result.displayType === DPtypes.ANALYZE))
        .map((result) => result.request.getTitle())
        .reduce( (obj,title,idx) => {
            obj[idx]= {color:undefined, title};
            return obj;
        },{});


    const {r,g,b}= get3CBandIdxes(gridDLData);

    if (bandData[r]) bandData[r].color= Band.RED;
    if (bandData[g]) bandData[g].color= Band.GREEN;
    if (bandData[b]) bandData[b].color= Band.BLUE;

    return bandData;
}

/**
 * @param {Array.<DatalinkData>} gridDLData
 * @return {{r: number, b: number, g: number}}
 */
function get3CBandIdxes(gridDLData) {
    const rBandIdx= gridDLData.findIndex( (d) => d.dlAnalysis.rBand);
    const gBandIdx= gridDLData.findIndex( (d) => d.dlAnalysis.gBand);
    const bBandIdx= gridDLData.findIndex( (d) => d.dlAnalysis.bBand);

    const bandAry= [];
    bandAry.length= gridDLData.length;
    if (rBandIdx!==-1) bandAry[rBandIdx]= Band.RED;
    if (gBandIdx!==-1) bandAry[gBandIdx]= Band.GREEN;
    if (bBandIdx!==-1) bandAry[bBandIdx]= Band.BLUE;

    if (!bandAry.includes(Band.RED)) bandAry[bandAry.findIndex((v)=> !v)]= Band.RED;
    if (!bandAry.includes(Band.GREEN)) bandAry[bandAry.findIndex((v)=> !v)]= Band.GREEN;
    if (!bandAry.includes(Band.BLUE)) bandAry[bandAry.findIndex((v)=> !v)]= Band.BLUE;
    return {
        r: bandAry.findIndex( (v) => v===Band.RED),
        g: bandAry.findIndex( (v) => v===Band.GREEN),
        b: bandAry.findIndex( (v) => v===Band.BLUE),
    };
}

export function makeDlUrl(dlServDesc, table, row) {
    if (!dlServDesc) return undefined;
    const {serDefParams, accessURL}= dlServDesc;
    const sendParams={};
    serDefParams?.filter( ({ref}) => ref).forEach( (p) => sendParams[p.name]= getCellValue(table, row, p.colName));
    const newUrl= new URL(accessURL);
    Object.entries(sendParams).forEach( ([k,v]) => newUrl.searchParams.append(k,v));
    return newUrl.toString();
}
