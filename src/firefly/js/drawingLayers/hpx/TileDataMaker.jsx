import {uniq} from 'lodash';
import {
    DATA_NORDER, fetchPartialData, getAllSelectedIndexes, getHpxIndexData,
    getKeysForOrder,
    getTile, idxRoot, MIN_NORDER,
    MIN_NORDER_FOR_COVERAGE, MIN_NORDER_TO_ALWAYS_GROUP, MIN_ROWS_FOR_HIERARCHICAL, onOrderDataReady,
    getAllWptsIdxsForTile, getRetroGradeIpix, isSummaryTile, isIndexesLoaded, getTileTableIndexes,
} from '../../tables/HpxIndexCntlr';
import {getTblById} from '../../tables/TableUtil';
import CoordSys from '../../visualize/CoordSys';
import CysConverter from '../../visualize/CsysConverter';
import {getAllVisibleHiPSCells, getCatalogNorderlevel, getPointMaxSide} from '../../visualize/HiPSUtil';
import {visRoot} from '../../visualize/ImagePlotCntlr';
import {getFoV, primePlot} from '../../visualize/PlotViewUtil';
import {isHiPSAitoff} from '../../visualize/WebPlot';
import {BOX_GROUP_TYPE, getHeatMapNorder, HEAT_MAP_GROUP_TYPE, HPX_GRID_SIZE_LARGE} from './HpxCatalogUtil';
import {makeSingleDrawPoint, makeSmartGridTypeGroupDrawPoint} from './HpxDraw';

const MAX_SYNC_AREA = 1_000_000;

/**
 *
 * @param obj
 * @param obj.drawLayer
 * @param obj.plotId
 * @param obj.tbl_id
 * @param obj.expanded
 * @return {{abort: function, makeTileData, hasPartialTileUpdate, getSecondaryPartialTileUpdate}
 */
export function createTileDataMaker({drawLayer, plotId, tbl_id, expanded={}}) {
    let doAbort = false;
    let partialPromise= undefined;
    let partialUpdateAvailable= false;
    const isAborted= () => doAbort;

    const makeTileDataAll = async () => {
        const table = getTblById(tbl_id);
        const plot = primePlot(visRoot(), plotId);
        if (!drawLayer || !plot?.viewDim || !table) return;
        await onOrderDataReady(tbl_id);

        if (isAborted()) return;

        const tileDataParams= setupTileDataParams(drawLayer,table,plot,expanded);

        const {idxData,unloadedNorder,unloadedTiles}= tileDataParams;
        if (idxData?.partialIndexData && unloadedTiles?.length) {
            partialUpdateAvailable= true;
            partialPromise= fetchPartialData(table,unloadedNorder,unloadedTiles);
        }

        return doMakeTileData(tileDataParams);
    };

    const doMakeTileData= (tileDataParams) => {
        return tileDataParams.doAsync ?
            new Promise((resolve) => doMakeTileDataInterval(isAborted, {resolve, ...tileDataParams})) :
            doMakeTileDataSync(tileDataParams);
    };

    const getSecondaryPartialTileUpdate= async () => {
        if (!partialPromise || !partialUpdateAvailable || isAborted()) return;
        await partialPromise;
        if (isAborted()) return;
        const table = getTblById(tbl_id);
        const plot = primePlot(visRoot(), plotId);
        const tileDataParams= setupTileDataParams(drawLayer,table,plot,expanded);
        return doMakeTileData(tileDataParams);
    };

    return {
        abort: () => doAbort = true,
        makeTileData: async (drawLayer, plotId, tbl_id, expanded) => makeTileDataAll(drawLayer, plotId, tbl_id, expanded),
        hasPartialTileUpdate: () => !isAborted() && partialUpdateAvailable,
        getSecondaryPartialTileUpdate,
    };
}



const doMakeTileDataSync = ({cells, ...tileDataParams}) => {
    let plotData = [];
    if (!cells) return plotData;
    for (let index = 0; index < cells.length; index++) {
        const {drawObjs,missingCells} = getDrawDataForCell({cell: cells[index], ...tileDataParams});
        plotData= addTo(plotData,drawObjs);
    }
    return plotData;
};


function doMakeTileDataInterval(isAborted, {resolve, cells, ...tileDataParams}) {

    let plotData = [];
    let done = false;
    let index = 0;
    const length = cells.length;

    const id = window.setInterval(
        () => {
            if (isAborted()) resolve(undefined);
            if (done || isAborted()) {
                done = true;
                window.clearInterval(id);
                return;
            }
            for (; index < length;) {
                const {drawObjs, missingCells} = getDrawDataForCell({cell: cells[index], ...tileDataParams});
                plotData= addTo(plotData,drawObjs);
                index++;
                if (drawObjs.length > 300) return;
            }
            done = true;
            resolve(plotData);
        }
    );
};

function setupTileDataParams(drawLayer, table, plot, expanded) {

    const {plotId}= plot;
    const {tbl_id}= table;
    const minGroupSize= drawLayer.groupType===HEAT_MAP_GROUP_TYPE ? 0 : drawLayer.minGroupSize;

    const idxData = getHpxIndexData(tbl_id);
    if (!idxData) return;

    let showLabels= true;
    let norder;
    let groupType= drawLayer.groupType;
    const isHeat= drawLayer.groupType===HEAT_MAP_GROUP_TYPE;
    norder= getCatalogNorderlevel(plot, MIN_NORDER, DATA_NORDER, isHeat ? HPX_GRID_SIZE_LARGE : drawLayer.gridSize);

    if (isHeat) { //modify labels and norder for heatmap
        const result= getHeatMapNorder(norder);
        showLabels= result.showLabels && drawLayer.heatMapLabels;
        norder= result.norder;
    }

    if (getFoV(plot) > 176) {
        norder= 2;
        groupType= BOX_GROUP_TYPE;
    }


    const expandedTiles = {};
    const {viewDim = {width: 1, height: 1}} = plot;
    let doAsync = viewDim.width * viewDim.height > MAX_SYNC_AREA;
    if (expanded[plotId]?.norder && expanded[plotId]?.ipixAry?.length) {
        expandedTiles[expanded[plotId].norder] = expanded[plotId].ipixAry;
        doAsync = true;
    }
    else if (visRoot().wcsMatchType && visRoot().plotViewAry.length>5) {
        doAsync= true;
    }
    else if (drawLayer.groupType===HEAT_MAP_GROUP_TYPE) {
        doAsync= true;
    }

    const {drawingDef} = drawLayer;
    const {selectAll = false} = table.selectInfo ?? {};
    const totalRows= table.totalRows;

    const {centerWp, fov} = getPointMaxSide(plot, plot.viewDim);
    const coverageTblIds = hasCoverageTables(centerWp, fov, isHiPSAitoff(plot));
    if (!centerWp || !coverageTblIds.includes(tbl_id)) return []; // this should force a clear for this layer
    const cells = getAllVisibleHiPSCells(norder, centerWp, fov*1.3, CoordSys.EQ_J2000, isHiPSAitoff(plot));
    const tblIdx = coverageTblIds.indexOf(tbl_id);
    const cc = CysConverter.make(plot);


    let unloadedNorder= undefined;
    let unloadedTiles= undefined;
    if (idxData.partialIndexData) {
        const ipixList= norder===DATA_NORDER ? uniq(cells.map((c) => getRetroGradeIpix(c.ipix))) : cells.map((c) => c.ipix);
        const tileNorder= norder===DATA_NORDER ? norder-1 : norder;
        const startingUnloadedTiles= getUnloadedTileList( {
            ipixList, minGroupSize, idxData, norder, tileNorder,
            expandedTiles, totalRows, groupType, forceShow:norder===DATA_NORDER
        });
        if (startingUnloadedTiles.length) { // retrograde the norder even further
            //todo - the retro grade decision could be influenced by how big the table is
            unloadedNorder= tileNorder===DATA_NORDER-1 ? tileNorder-2 : tileNorder>10 ? tileNorder-1 : tileNorder;
            const ipixDiff= tileNorder-unloadedNorder;
            unloadedTiles= uniq(startingUnloadedTiles.map( (t) => getRetroGradeIpix(t.pixel,ipixDiff)))
                .map( (ipix) => getTile(idxData.orderData,unloadedNorder,ipix));
        }
    }

    const tileDataParams = {
        doAsync,
        cc,
        minGroupSize,
        idxData,
        cells,
        norder,
        showLabels,
        drawingDef,
        expandedTiles,
        selectAll,
        tblIdx,
        totalRows,
        groupType,
        heatMapStretch:drawLayer.heatMapStretch,
        unloadedNorder,
        unloadedTiles,
    };
    return tileDataParams;
}



function addTo(ary,addAry) {
    if (addAry.length>50) return [...ary,...addAry];
    ary.push(...addAry);
    return ary;
}

function hasCoverageTables(centerWp, fov, plot) {
    const cells = getAllVisibleHiPSCells(MIN_NORDER_FOR_COVERAGE, centerWp, fov, CoordSys.EQ_J2000, isHiPSAitoff(plot));
    if (!cells?.length) return 0;
    const ipixAry = cells.map(({ipix}) => ipix);
    return Object.entries(idxRoot())
        .filter(([, idxData]) => idxData.ready)
        .filter(([, idxData]) =>
            getKeysForOrder(idxData.orderData,MIN_NORDER_FOR_COVERAGE).some((ipix) => ipixAry.includes(ipix)))
        .map(([tbl_id]) => tbl_id);
}


function getDrawDataForCell({ cell, minGroupSize, cc, idxData, norder, expandedTiles, totalRows,
                                showLabels, tblIdx, drawingDef, selectAll, groupType,heatMapStretch}) {

    const belowMinRow= totalRows < MIN_ROWS_FOR_HIERARCHICAL;
    const showAllPoints = norder > MIN_NORDER_TO_ALWAYS_GROUP && belowMinRow;

    const isSelected = (exceptionOn) => selectAll ? !exceptionOn : exceptionOn;

    const tileData = getTile(idxData.orderData, norder, cell.ipix);
    const selectionTileData = getTile(idxData.selectionOrderData, norder, cell.ipix);
    const {count = 0, pixel:ipix} = tileData ?? {};
    let drawObjs= [];
    const missingCells= undefined;
    if (!count) return {drawObjs,missingCells};
    if (isSummaryTile(tileData)) {
        if (alwaysShow(count,ipix,showAllPoints,groupType,minGroupSize, expandedTiles, norder)) {
            drawObjs= getDrawObsFromTableIndexes(idxData,drawingDef,norder, tileData,isSelected);
        }
        else {  // show the group - most common case
            const selected = Boolean(selectionTileData?.count) || selectAll;
            const sCnt= selectionTileData?.count ?? 0;
            const selectedCnt= selectAll ? count-sCnt : sCnt;
            const groupTypeToUse= belowMinRow ? BOX_GROUP_TYPE  : groupType;
            drawObjs= makeSmartGridTypeGroupDrawPoint(
                {idxData, count, norder, showLabels, ipix, cell, cc,
                drawingDef, selected, selectedCnt, tblIdx, groupType:groupTypeToUse,heatMapStretch});
        }
    } else { // draw the points if at the data level
        const selectedIndexes = getTileTableIndexes(selectionTileData);

        const resultAry= getAllWptsIdxsForTile(idxData,norder,tileData.pixel);
        drawObjs= resultAry.map( ({wp,idx}) => {
            return makeSingleDrawPoint(isSelected(selectedIndexes.includes(idx)), idx, wp, drawingDef);
        });
    }
    return {drawObjs,missingCells};
}
 function isMissingDrawDataTile({ ipix, minGroupSize, idxData, tileNorder, norder, expandedTiles, totalRows, groupType, forceShow}) {
     const belowMinRow= totalRows < MIN_ROWS_FOR_HIERARCHICAL;
     const showAllPoints = norder > MIN_NORDER_TO_ALWAYS_GROUP && belowMinRow;
     const tileData = getTile(idxData.orderData, tileNorder, ipix);
     if (!tileData?.count || isIndexesLoaded(idxData.orderData,norder.tileData)) return;
     if (forceShow) return tileData;
     if (!isSummaryTile(tileData)) return tileData;
     return alwaysShow(tileData.count, ipix, showAllPoints, groupType,minGroupSize, expandedTiles, norder) ? tileData : undefined;
}


function getUnloadedTileList({ipixList, ...tileDataParams}) {
    const missingTiles= ipixList
        .map( (ipix) => isMissingDrawDataTile({ipix,...tileDataParams}))
        .filter(Boolean);
    return missingTiles;
}


function alwaysShow(count, ipix, showAllPoints, groupType, minGroupSize, expandedTiles, norder) {
    if (showAllPoints) return true;
    if (groupType!==HEAT_MAP_GROUP_TYPE && norder > MIN_NORDER_TO_ALWAYS_GROUP-2 && count < 4) return true;
    if (expandedTiles?.[norder]?.includes(ipix)) return true;
    return (count > 1 && count < minGroupSize && norder >= MIN_NORDER_TO_ALWAYS_GROUP);
}

function getDrawObsFromTableIndexes(idxData, drawingDef, norder, tileData, isSelected) {
    const {pixel}= tileData;
    const selectedIndexes = getAllSelectedIndexes(idxData, norder, pixel);
    const resultAry= getAllWptsIdxsForTile(idxData,norder,tileData.pixel);

    const drawObjs= resultAry.map( ({wp,idx}) => {
        const selected = isSelected(selectedIndexes.includes(idx));
        return makeSingleDrawPoint(selected, idx, wp, drawingDef);
    });
    return drawObjs;

}