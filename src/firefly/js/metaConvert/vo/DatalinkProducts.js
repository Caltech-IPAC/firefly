import {getCellValue} from '../../tables/TableUtil.js';
import {getPreferCutout} from '../../ui/tap/Cutout';
import {getSizeAsString} from '../../util/WebUtil';
import {visRoot} from '../../visualize/ImagePlotCntlr';
import {primePlot} from '../../visualize/PlotViewUtil';
import {getDataLinkData} from '../../voAnalyzer/VoDataLinkServDef.js';
import {Band} from '../../visualize/Band.js';
import {WPConst} from '../../visualize/WebPlotRequest.js';
import {IMAGE_ONLY, TABLE_ONLY} from '../DataProductConst';
import {
    dispatchUpdateActiveKey, dispatchUpdateDataProducts, getActiveMenuKey, getCurrentActiveKeyID
} from '../DataProductsCntlr';
import {
    dpdtFromMenu, dpdtImage, dpdtMessageWithDownload, dpdtSimpleMsg, dpdtWorkingMessage, DPtypes
} from '../DataProductsType.js';
import {
    createGridImagesActivate, createRelatedGridImagesActivate, createSingleImageExtraction
} from '../ImageDataProductsUtil.js';
import {fetchDatalinkTable} from './DatalinkFetch.js';
import {
    filterDLList, findMenuKeyWithName, hasBandInMenuKey, hasLabelInMenuKey, IMAGE, isWarnSize, processDatalinkTable,
    RELATED_IMAGE_GRID,
    SPECTRUM, USE_ALL
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
export async function getDatalinkRelatedGridProduct({dlTableUrl, activateParams, table, row, threeColorOps, titleStr, options}) {
    try {
        dispatchUpdateDataProducts(activateParams.dpId, dpdtWorkingMessage('Loading data products...', 'working'));
        const datalinkTable = await fetchDatalinkTable(dlTableUrl, options.datalinkTblRequestOptions);
        const preferCutout= getPreferCutout(options.dataProductsComponentKey,table?.tbl_id);

        const gridData = getDataLinkData(datalinkTable,table,row).filter(({dlAnalysis}) => dlAnalysis.isGrid && dlAnalysis.isImage);
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
        const onActivePvChanged= getOnActivePvChanged(dlTableUrl,dataLinkGrid,activateParams,options);;
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

        const gridDlData= {...dlDataAry[0]};
        if (preferCutout) {
            const allSize= dlDataAry.map ( (d) => d.size).reduce((tot,v) => tot+v,0) ;
            if (isWarnSize(allSize)) {
                gridDlData.cutoutToFullWarning=
                    `Warning: Loading ${requestAry.length} images with a total size of ${getSizeAsString(allSize)}, it might take awhile to load`;
            }
        }


        const item= dpdtImage({name:'image grid', activate, extraction,
            dlData: cutoutSwitching ? gridDlData : undefined,
            enableCutout:preferCutout,
            menu: extractItems,
            menuKey:'image-grid-0',serDef:dataLinkGrid.serDef});
        item.menu= extractItems;
        return item;
    } catch (reason) {
        return dpdtMessageWithDownload(`No data to display: Could not retrieve datalink data, ${reason}`, 'Download File: ' + titleStr, dlTableUrl);
    }
}

function getOnActivePvChanged(dlTableUrl, dataLinkGrid, activateParams, options) {
    if (!options.relatedGridImageOrder?.length) return;
    const menuKeyAry= dataLinkGrid.menu.map( (m) => m.menuKey);
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
                .filter( ({dlData:dl}) => dl?.dlAnalysis?.isImage && dl?.dlAnalysis?.isGrid).length>1 );

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


function makeAllRelatedGridMenu(resultAry, bandPassSet, activateParams, table, plotRows,options) {
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


export async function datalinkDescribeThreeColor(dlTableUrl, table,row, options) {
    const datalinkTable = await fetchDatalinkTable(dlTableUrl, options.datalinkTblRequestOptions);
    const dataLinkGrid = processDatalinkTable({
        sourceTable: table, row, datalinkTable, activateParams:{},
        baseTitle: '', dlTableUrl, doFileAnalysis: false,
        options, parsingAlgorithm: RELATED_IMAGE_GRID
    });

    const bandData = dataLinkGrid.menu
        .filter((result) => result?.request && (
            result.displayType === DPtypes.IMAGE ||
            result.displayType === DPtypes.PROMISE ||
            result.displayType === DPtypes.ANALYZE))
        .map((result) => result.request.getTitle())
        .reduce( (obj,title,idx) => {
            obj[idx]= {color:undefined, title};
            return obj;
        },{});


    const {r,g,b}= get3CBandIdxes(datalinkTable);

    if (bandData[r]) bandData[r].color= Band.RED;
    if (bandData[g]) bandData[g].color= Band.GREEN;
    if (bandData[b]) bandData[b].color= Band.BLUE;

    return bandData;
}

/**
 * @param {TableModel} datalinkTable
 * @return {{r: number, b: number, g: number}}
 */
function get3CBandIdxes(datalinkTable) {
    const gridData= filterDLList(RELATED_IMAGE_GRID,getDataLinkData(datalinkTable));
    const rBandIdx= gridData.findIndex( (d) => d.dlAnalysis.rBand);
    const gBandIdx= gridData.findIndex( (d) => d.dlAnalysis.gBand);
    const bBandIdx= gridData.findIndex( (d) => d.dlAnalysis.bBand);

    const bandAry= [];
    bandAry.length= gridData.length;
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
