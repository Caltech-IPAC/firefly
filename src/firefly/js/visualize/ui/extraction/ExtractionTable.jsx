import {MetaConst} from '../../../data/MetaConst';
import {ServerParams} from '../../../data/ServerParams';
import {makeTblRequest} from '../../../tables/TableRequestUtil';
import {dispatchTableFetch, dispatchTableSearch} from '../../../tables/TablesCntlr';
import {onTableLoaded} from '../../../tables/TableUtil';
import {showTableDownloadDialog} from '../../../tables/ui/TableSave';
import {showInfoPopup, showPinMessage} from '../../../ui/PopupUtil';
import {Band} from '../../Band';
import {CCUtil, CysConverter} from '../../CsysConverter';
import {getExtName} from '../../FitsHeaderUtil';
import {
    getAllWaveLengthsForCube, getHDU, getHduPlotStartIndexes, getImageCubeIdx, getPtWavelength, getWaveLengthUnits,
    hasPixelLevelWLInfo, hasWCSProjection, hasWLInfo, isImageCube, isMultiHDUFits
} from '../../PlotViewUtil';
import {getLinePointAry} from '../../VisUtil';
import {getFluxUnits} from '../../WebPlot';
import {addZAxisExtractionWatcher} from '../ExtractionWatchers';

let idCnt = 0;
const getNextTblId = () => 'extraction-table-' + (idCnt++);

async function doDispatchTableSaving(req, doOverlay) {
    const {tbl_id} = req;
    const sendReq = {...req};
    sendReq.META_INFO = !doOverlay ? {
        ...sendReq.META_INFO,
        [MetaConst.CATALOG_OVERLAY_TYPE]: 'FALSE'
    } : {...sendReq.META_INFO};
    dispatchTableFetch(sendReq, {tbl_group: 'main', backgroundable: false});
    await onTableLoaded(tbl_id);
    showTableDownloadDialog({tbl_id, tbl_ui_id: undefined})();
}

function doDispatchTable(req, doOverlay) {
    showPinMessage('Pinning Extraction to Table Area');
    const sendReq = {...req};
    sendReq.META_INFO = !doOverlay ? {
        ...sendReq.META_INFO,
        [MetaConst.CATALOG_OVERLAY_TYPE]: 'FALSE'
    } : {...sendReq.META_INFO};
    dispatchTableSearch(sendReq, {
        logHistory: false,
        removable: true,
        tbl_group: 'main',
        backgroundable: false,
        showFilters: true,
        showInfoButton: true
    });
}

let titleCnt = 1;

export function keepZAxisExtraction(pt, pv, plot, filename, refHDUNum, extractionSize, combineOp, save = false, doOverlay = true) {
    if (!pv || !plot || !filename) {
        showInfoPopup('Plot no longer exist. Cannot extract.');
        return;
    }
    const wlUnit = getWaveLengthUnits(plot);
    const wpt = CCUtil.getWorldCoords(plot, pt);
    const fluxUnit = getHduPlotStartIndexes(pv)
        .map((idx) => ({hdu: getHDU(pv.plots[idx]), unit: getFluxUnits(pv.plots[idx], Band.NO_BAND)}))
        .map(({hdu, unit}) => `${hdu}=${unit}`);

    const tbl_id = getNextTblId();
    addZAxisExtractionWatcher(tbl_id);
    const dataTableReq = makeTblRequest('ExtractFromImage', `Extraction Z-Axis - ${titleCnt}`,
        {
            startIdx: 0,
            extractionType: 'z-axis',
            pt: pt.toString(),
            wpt: wpt?.toString(),
            wlAry: hasWLInfo(plot) ? JSON.stringify(getAllWaveLengthsForCube(pv, pt)) : undefined,
            wlUnit,
            fluxUnit: JSON.stringify(fluxUnit),
            filename,
            refHDUNum,
            extractionSizeX: extractionSize,
            extractionSizeY: extractionSize,
            [ServerParams.COMBINE_OP]: combineOp,
            allMatchingHDUs: true,
        },
        {tbl_id});
    if (save) dataTableReq.pageSize = 0;
    save ? doDispatchTableSaving(dataTableReq, doOverlay) : doDispatchTable(dataTableReq, doOverlay);
    idCnt++;
    titleCnt++;
    return tbl_id;
}

export function keepLineExtraction(pt, pt2, pv, plot, filename, refHDUNum, plane, extractionSizeX, extractionSizeY, combineOp, save = false, doOverlay = true) {
    if (!pv || !plot || !filename) {
        showInfoPopup('Plot no longer exist. Cannot extract.');
        return;
    }
    const tbl_id = getNextTblId();
    const imPtAry = getLinePointAry(pt, pt2);
    const cc = CysConverter.make(plot);

    const wlAry = hasPixelLevelWLInfo(plot) ?
        imPtAry.map((pt) => getPtWavelength(plot, pt, 0))
        : undefined;

    const wptStrAry = hasWCSProjection(plot) ?
        imPtAry.map((pt) => cc.getWorldCoords(pt)).map((wpt) => wpt.toString()) : undefined;
    const dataTableReq = makeTblRequest('ExtractFromImage', makePlaneTitle('Extract Line', pv, plot, titleCnt), {
            startIdx: 0,
            extractionType: 'line',
            ptAry: JSON.stringify(imPtAry.map((pt) => pt.toString())),
            wptAry: JSON.stringify(wptStrAry),
            wlAry,
            wlUnit: getWaveLengthUnits(plot),
            filename,
            refHDUNum,
            plane,
            extractionSizeX,
            extractionSizeY,
            [ServerParams.COMBINE_OP]: combineOp,
            allMatchingHDUs: true,
        },
        {tbl_id});
    save ? doDispatchTableSaving(dataTableReq, doOverlay) : doDispatchTable(dataTableReq, doOverlay);
    idCnt++;
    titleCnt++;
    return tbl_id;
}

function makePlaneTitle(rootStr, pv, plot, cnt) {
    let hduStr = '';
    let cubeStr = '';
    if (isMultiHDUFits(pv)) {
        if (getExtName(plot)) hduStr = `- ${getExtName(plot)}`;
        else hduStr = `- HDU#${getHDU(plot)} `;
    }
    if (isImageCube(plot)) cubeStr = `- Plane: ${getImageCubeIdx(plot) + 1}`;
    return `${rootStr} ${cnt}${hduStr}${cubeStr}`;
}

export function keepPointsExtraction(ptAry, pv, plot, filename, refHDUNum, plane, extractionSize, combineOp, save = false, doOverlay = true) {
    if (!pv || !plot || !filename) {
        showInfoPopup('Plot no longer exist. Cannot extract.');
        return;
    }
    const tbl_id = getNextTblId();
    const cc = CysConverter.make(plot);
    const wlAry = hasPixelLevelWLInfo(plot) ?
        ptAry.map((pt) => getPtWavelength(plot, pt, 0))
        : undefined;
    const wptStrAry =
        hasWCSProjection(plot) ?
            ptAry.map((pt) => cc.getWorldCoords(pt)).map((pt) => pt.toString()) :
            undefined;
    const dataTableReq = makeTblRequest('ExtractFromImage', makePlaneTitle('Points', pv, plot, titleCnt),
        {
            startIdx: 0,
            extractionType: 'points',
            ptAry: JSON.stringify(ptAry.map((pt) => pt.toString())),
            wptAry: JSON.stringify(wptStrAry),
            wlAry,
            wlUnit: getWaveLengthUnits(plot),
            filename,
            refHDUNum,
            plane,
            extractionSizeX: extractionSize,
            extractionSizeY: extractionSize,
            [ServerParams.COMBINE_OP]: combineOp,
            allMatchingHDUs: true,
        },
        {tbl_id});
    save ? doDispatchTableSaving(dataTableReq, doOverlay) : doDispatchTable(dataTableReq, doOverlay);
    idCnt++;
    titleCnt++;
    return tbl_id;
}