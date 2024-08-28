import {
    DATA_NORDER, getAllDataIndexes, getAllSelectedIndexes, getFirstIndex, getHpxIndexData, getTile,
    idxRoot, makeHpxWpt,
    MIN_NORDER,
    MIN_NORDER_FOR_COVERAGE, MIN_NORDER_TO_ALWAYS_GROUP, MIN_ROWS_FOR_HIERARCHICAL, onOrderDataReady
} from '../../tables/HpxIndexCntlr';
import {getTblById} from '../../tables/TableUtil';
import CoordSys from '../../visualize/CoordSys';
import CysConverter from '../../visualize/CsysConverter';
import {getAllVisibleHiPSCells, getCatalogNorderlevel, getPointMaxSide} from '../../visualize/HiPSUtil';
import {visRoot} from '../../visualize/ImagePlotCntlr';
import {primePlot} from '../../visualize/PlotViewUtil';
import {isHiPSAitoff} from '../../visualize/WebPlot';
import {makeSingleDrawPoint, makeSmartGridTypeGroupDrawPoint} from './HpxDraw';

const MAX_SYNC_AREA = 1_000_000;

export function createTileDataMaker() {
    let isAborted = false;

    const makeTileDataAll = async (drawLayer, plotId, tbl_id, expanded = {}) => {
        const table = getTblById(tbl_id);
        const plot = primePlot(visRoot(), plotId);
        if (!plot?.viewDim || !table) return;
        await onOrderDataReady(tbl_id);

        if (isAborted) return;

        const idxData = getHpxIndexData(tbl_id);
        if (!idxData) return;
        const norder = getCatalogNorderlevel(plot, MIN_NORDER, DATA_NORDER);
        const showAllPoints = norder > MIN_NORDER_TO_ALWAYS_GROUP && table.totalRows < MIN_ROWS_FOR_HIERARCHICAL;

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

        const {drawingDef} = drawLayer;
        const {selectAll = false} = table.selectInfo ?? {};

        const {centerWp, fov} = getPointMaxSide(plot, plot.viewDim);
        const coverageTblIds = hasCoverageTables(centerWp, fov, isHiPSAitoff(plot));
        if (!coverageTblIds.includes(tbl_id)) return []; // this should force a clear for this layer
        const cells = getAllVisibleHiPSCells(norder, centerWp, fov, CoordSys.EQ_J2000, isHiPSAitoff(plot));
        const tblIdx = coverageTblIds.indexOf(tbl_id);

        const cc = CysConverter.make(plot);

        const tileDataParams = {
            cc,
            minGroupSize: drawLayer.minGroupSize,
            idxData,
            cells,
            norder,
            drawingDef,
            expandedTiles,
            showAllPoints,
            selectAll,
            tblIdx,
            groupType:drawLayer.groupType
        };

        return doAsync ?
            new Promise((resolve) => doMakeTileDataInterval({resolve, ...tileDataParams})) :
            doMakeTileDataSync(tileDataParams);
    };

    const doMakeTileDataSync = ({cells, ...tileDataParams}) => {
        let plotData = [];
        for (let index = 0; index < cells.length; index++) {
            const pts = getDrawDataForCell({cell: cells[index], ...tileDataParams});
            plotData= addTo(plotData,pts);
        }
        return plotData;
    };

    const doMakeTileDataInterval = ({resolve, cells, ...tileDataParams}) => {

        let plotData = [];
        let done = false;
        let index = 0;
        const length = cells.length;

        const id = window.setInterval(
            () => {
                if (isAborted) resolve(undefined);
                if (done || isAborted) {
                    done = true;
                    window.clearInterval(id);
                    return;
                }

                for (; index < length;) {
                    const pts = getDrawDataForCell({cell: cells[index], ...tileDataParams});
                    plotData= addTo(plotData,pts);
                    index++;
                    if (pts.length > 300) return;
                }
                done = true;
                resolve(plotData);
            }
        );
    };

    return {
        abort: () => isAborted = true,
        makeTileData: async (drawLayer, plotId, tbl_id, expanded) => makeTileDataAll(drawLayer, plotId, tbl_id, expanded)
    };
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
            [...idxData.orderData[MIN_NORDER_FOR_COVERAGE]?.tiles.keys()]
                .some((ipix) => ipixAry.includes(ipix)))
        .map(([tbl_id]) => tbl_id);
}


function getDrawDataForCell({ cell, minGroupSize, cc, idxData, norder, expandedTiles, showAllPoints,
                                tblIdx, drawingDef, selectAll, groupType}) {

    const isSelected = (exceptionOn) => selectAll ? !exceptionOn : exceptionOn;

    const tileData = getTile(idxData.orderData, norder, cell.ipix);
    const selectionTileData = getTile(idxData.selectionOrderData, norder, cell.ipix);
    const {count = 0} = tileData ?? {};
    if (!count) return [];
    if (tileData.summaryTile) {
        if (expandedTiles?.[norder]?.includes(cell.ipix) || showAllPoints) { // if we should force expanded groups
            const expandedIdxs = getAllDataIndexes(idxData, norder, cell.ipix);
            const selectedIndexes = getAllSelectedIndexes(idxData, norder, cell.ipix);
            return expandedIdxs.map((idx) => {
                const selected = isSelected(selectedIndexes.includes(idx));
                const wp = makeHpxWpt(idxData, idx);
                return makeSingleDrawPoint(selected, idx, wp, drawingDef);
            });
        } else if (count > 1 && count < minGroupSize && norder > MIN_NORDER_TO_ALWAYS_GROUP) { // expand if group is small
            const selectedIndexes = getAllSelectedIndexes(idxData, norder, cell.ipix);
            return getAllDataIndexes(idxData, norder, cell.ipix).map((idx) => {
                const selected = isSelected(selectedIndexes.includes(idx));
                const wp = makeHpxWpt(idxData, idx);
                return makeSingleDrawPoint(selected, idx, wp, drawingDef);
            });
        } else if (count === 1) { // only 1 point in group
            const selected = isSelected(Boolean(selectionTileData?.count));
            const idxAry = getFirstIndex(idxData.orderData, norder, cell.ipix);
            const wp = makeHpxWpt(idxData, idxAry[0]);
            return [makeSingleDrawPoint(selected, idxAry[0], wp, drawingDef)];
        } else {  // show the group - most common case
            const selected = Boolean(selectionTileData?.count) || selectAll;
            const sCnt= selectionTileData?.count ?? 0;
            const selectedCnt= selectAll ? count-sCnt : sCnt;
            return makeSmartGridTypeGroupDrawPoint(idxData, count, norder, tileData.pixel, cell, cc, drawingDef, selected, selectedCnt, tblIdx, groupType);
        }
    } else { // draw the points if at the data level
        const selectedIndexes = selectionTileData?.tableIndexes ?? [];
        return tileData.tableIndexes.map((idx) => {
            const wp = makeHpxWpt(idxData, idx);
            return makeSingleDrawPoint(isSelected(selectedIndexes.includes(idx)), idx, wp, drawingDef);
        });
    }

}