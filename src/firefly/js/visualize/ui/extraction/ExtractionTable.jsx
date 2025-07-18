import {Stack, Typography} from '@mui/joy';
import React from 'react';
import {dispatchHideDialog} from '../../../core/ComponentCntlr';
import {MetaConst} from '../../../data/MetaConst';
import {ServerParams} from '../../../data/ServerParams';
import {makeTblRequest} from '../../../tables/TableRequestUtil';
import {dispatchTableFetch, dispatchTableSearch} from '../../../tables/TablesCntlr';
import {onTableLoaded} from '../../../tables/TableUtil';
import {showTableDownloadDialog} from '../../../tables/ui/TableSave';
import {showInfoPopup, showMultiAnswerPopup, showPinMessage} from '../../../ui/PopupUtil';
import {advancedTrim} from '../../../util/WebUtil';
import {CCUtil, CysConverter} from '../../CsysConverter';
import {getExtName, getHeader, HdrConst} from '../../FitsHeaderUtil';
import {visRoot} from '../../ImagePlotCntlr';
import {
    getAllWaveLengthsForCube, getHDU, getHduPlotStartIndexes, getImageCubeIdx, getPlotViewAry,
    getPtWavelength, getWaveLengthUnits,
    hasPixelLevelWLInfo, hasWCSProjection, hasWLInfo, isImageCube, isMultiHDUFits, primePlot,
} from '../../PlotViewUtil';
import {makeImagePt} from '../../Point';
import {getFluxUnits, isImage} from '../../WebPlot';
import {addZAxisExtractionWatcher} from '../ExtractionWatchers';

let idCnt = 0;
const MAX_HDU= 20;
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
        .map((idx) => ({hdu: getHDU(pv.plots[idx]), unit: getFluxUnits(pv.plots[idx])}))
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
    return Promise.resolve([tbl_id]);
}

export function keepDataExtraction({pv, baseImPtAry, save, doOverlay, axis='both', pointSize, combineOp, isLine=false}) {
    const plot= primePlot(pv);
    const filename= plot?.plotState?.getWorkingFitsFileStr();
    if (!pv || !plot || !filename) {
        showInfoPopup('Plot no longer exist. Cannot extract.');
        return;
    }
    const {pvAry,filteredPvAry,canDoMultiImage}= getExtractableImageList(pv);
    const extractionSizeX= axis==='x' ? 1 : pointSize;
    const extractionSizeY= axis==='y' ? 1: pointSize;
    if (!canDoMultiImage) {
        const tbl_id= makeDataExtractionTable({baseImPtAry, pv, extractionSizeX, extractionSizeY,
            combineOp, save, doOverlay, isLine});
        return Promise.resolve([tbl_id]);
    }

    if (filteredPvAry.length < pvAry.length) {
        let msg= 'Cannot extract from all images';
        if (pvAry.some( (testPv) => !hasWCSProjection(testPv))) {
            msg+= ' Some image do not have a WCS Projection.';
        }
        if (pvAry.some( (testPv) => getMatchingHDUCount(testPv)>MAX_HDU)) {
            msg+= ` Some images have more than ${MAX_HDU} HDUs.`;
        }
        msg+= ' These image can be extracted in single image mode';
        setTimeout( () => showInfoPopup(msg), 1000);
    }


    const AllImagesQuestion= () => (
        <Stack spacing={2} sx={{width:'30rem'}}>
            <Typography>
                You can either extract from a single image or all images that have a WCS and a limited amount of HDUs.
            </Typography>
            <Typography color='warning'>
                Do you want to extract from all images?
            </Typography>
        </Stack>
    );
    const answers= {allSame: 'Yes / one table', allDiff: 'Yes / multiple tables', single: 'No'};

    let tblId, tblIdAry;
    return new Promise((resolve) => {
        const handleAnswer= (id,answer) => {
            dispatchHideDialog(id);
            if (answer==='allDiff') {
                tblIdAry= getExtractableImageList(pv,false).filteredPvAry.map( (pv) => {
                    const ccBase = CysConverter.make(primePlot(filteredPvAry[0]));
                    const cc= CysConverter.make(primePlot(pv));
                    const newBaseImPtAry= baseImPtAry.map((pt) => cc.getImageCoords(ccBase.getWorldCoords(pt)));
                    return makeDataExtractionTable({baseImPtAry:newBaseImPtAry, pv, extractionSizeX, extractionSizeY,
                        combineOp, save, doOverlay, isLine, exclusiveToPlot:true});
                } );
                resolve(tblIdAry);
            }
            else {
                tblId= makeDataExtractionTable({baseImPtAry, pv, extractionSizeX, extractionSizeY, combineOp, save,
                    doOverlay, isLine, allImages: answer==='allSame'});
                resolve([tblId]);
            }
        };
        showMultiAnswerPopup(<AllImagesQuestion/>, handleAnswer, answers, 'Extract Image Type', {maxWidth:'30rem'});
    });
}




export function makeDataExtractionTable({baseImPtAry, pv, extractionSizeX, extractionSizeY,
                                       combineOp, save = false, doOverlay = true,
                                            allImages=false, exclusiveToPlot=false, isLine}) {
    const plot= primePlot(pv);
    const tbl_id = getNextTblId();
    const cc = CysConverter.make(plot);

    const baseWpAry= hasWCSProjection(plot) ? baseImPtAry.map((pt) => cc.getWorldCoords(pt)) : undefined;
    const {filteredPvAry}= allImages ? getExtractableImageList(pv) : {filteredPvAry: [pv]};

    const epBase= {
        wptAry: baseWpAry ? JSON.stringify(baseWpAry.map((wpt) => wpt.toString())) :  undefined,
        startIdx: 0,
        extractionType: isLine ? 'line' : 'points',
        extractionSizeX,
        extractionSizeY,
        [ServerParams.COMBINE_OP]: combineOp,
        allMatchingHDUs: true,
        titleAry: JSON.stringify(getTitles(filteredPvAry)),
    };

    const extractParams= filteredPvAry.reduce( (obj,workingPv,idx) => {
            const workingPlot= primePlot(workingPv);
            const cc = CysConverter.make(workingPlot);
            const ptAry= idx===0 ? baseImPtAry :
                baseWpAry?.map( (wpt) => {
                    const ipt= cc.getImageCoords(wpt);
                    return ipt ? makeImagePt(Math.round(ipt.x), Math.round(ipt.y)) : undefined;
                });
            const wlAry = hasPixelLevelWLInfo(workingPlot) ?
                ptAry.map((pt) => getPtWavelength(workingPlot, pt, 0))
                : undefined;
            obj['ptAry'+idx]= JSON.stringify(ptAry.map((pt) => pt.toString()));
            obj['wlAry'+idx]= wlAry;
            obj['wlUnit'+idx]= getWaveLengthUnits(workingPlot);
            obj['filename'+idx]= workingPlot.plotState.getWorkingFitsFileStr();
            obj['refHDUNum'+idx]= getHDU(workingPlot);
            obj['plane'+idx]= getImageCubeIdx(workingPlot)>-1 ? getImageCubeIdx(workingPlot) : 0;
            return obj;
        },epBase);
    if (exclusiveToPlot) {
        extractParams.META_INFO= {[MetaConst.EXCLUSIVE_TO_PLOT]: 'true'};
    }
    const dataTableReq = makeTblRequest('ExtractFromImage',
        makePlaneTitle(`Extract ${isLine?'Line':'Points'}`, pv, plot, titleCnt), extractParams, {tbl_id});
    save ? doDispatchTableSaving(dataTableReq, doOverlay) : doDispatchTable(dataTableReq, doOverlay);
    idCnt++;
    titleCnt++;
    return tbl_id;
}


function getTitles(pvAry) {
    const titleAry= pvAry.map( (pv) => primePlot(pv)?.title ?? '');
    const lenTest= [15,18,21,24];
    for(let i=0;i<lenTest.length;i++) {
        const tryArray= getTestTitles(titleAry,lenTest[i]);
        if (new Set(tryArray).size === tryArray.length) return tryArray;
    }
    return titleAry;
}

function getTestTitles(titleAry,size) {
    const sizeHalf= Math.floor(size/2);
    const sizeThird= Math.floor(size/3);
    const firstTitle= titleAry[0];
    const firstTitleStart= firstTitle.substring(0,sizeThird);
    const firstTitleEnd= titleAry[0].substring(firstTitle.length-sizeThird,firstTitle.length);
    const firstMidPoint = Math.ceil(firstTitle.length / 2);
    const firstTitleMiddle= titleAry[0].substring(firstMidPoint-sizeHalf,firstMidPoint+sizeHalf);
    const discardStart= titleAry.every( (t) => firstTitleStart===t.substring(0,sizeThird));
    const discardEnd= titleAry.every( (t) => firstTitleEnd===t.substring(t.length-sizeThird,t.length));
    const discardMiddle= titleAry.every( (t) => {
        const midPoint = Math.ceil(t.length / 2);
        return firstTitleMiddle===t.substring(midPoint-sizeHalf,midPoint+sizeHalf);
    });

    let trimType;
    if (discardStart && discardEnd && !discardMiddle) trimType= 'bothEnds';
    else if (!discardStart && !discardEnd && !discardMiddle) trimType= 'complex';
    else if (!discardStart && !discardMiddle) trimType= 'startMiddle';
    else if (!discardStart) trimType= 'start';
    else if (!discardEnd) trimType= 'end';
    else trimType= 'middle';


    return titleAry.map( (t) => advancedTrim(t,size,trimType) );
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

function getMatchingHDUCount(pv) {
    const plot= primePlot(pv);
    if (!plot) return 0;
    return pv.plots.filter( (p) => hduMatches(plot,p)).length;
}


function hduMatches(refPlot,plot) {

    const dims= getHeader(refPlot,HdrConst.NAXIS,'0');
    const xLen= getHeader(refPlot,HdrConst.NAXIS1,'1');
    const yLen= getHeader(refPlot,HdrConst.NAXIS2,'1');
    const zLen= getHeader(refPlot,HdrConst.NAXIS3,'1');

    return (
        dims=== getHeader(plot,HdrConst.NAXIS,'0') &&
        xLen===getHeader(plot,HdrConst.NAXIS1,'1') &&
        yLen===getHeader(plot,HdrConst.NAXIS2,'1') &&
        zLen===getHeader(plot,HdrConst.NAXIS3,'1')
    );


}

function getExtractableImageList(pv,limitHDUs=true) {
    const singleRet= {filteredPvAry:[pv], pvAry:[pv], canDoMultiImage:false};
    if (!hasWCSProjection(primePlot(pv))) return singleRet;
    if (limitHDUs && getMatchingHDUCount(pv)>MAX_HDU) return singleRet;
    const pvAry= [pv,
        ...getPlotViewAry(visRoot(), pv.plotGroupId).filter( (testPv) => isImage(primePlot(testPv)) && testPv!==pv) ];
    if (pvAry.length===1) return singleRet;
    const filteredPvAry= pvAry.filter( (testPv) => hasWCSProjection(testPv) && (!limitHDUs || getMatchingHDUCount(testPv)<=MAX_HDU) );
    return {filteredPvAry,pvAry,canDoMultiImage:true};
}