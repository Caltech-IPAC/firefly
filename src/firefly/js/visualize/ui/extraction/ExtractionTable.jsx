import {Stack, Typography} from '@mui/joy';
import React, {useEffect} from 'react';
import {MetaConst} from '../../../data/MetaConst';
import {ServerParams} from '../../../data/ServerParams';
import {makeTblRequest} from '../../../tables/TableRequestUtil';
import {dispatchTableFetch, dispatchTableSearch} from '../../../tables/TablesCntlr';
import {onTableLoaded} from '../../../tables/TableUtil';
import {showTableDownloadDialog} from '../../../tables/ui/TableSave';
import {CheckboxGroupInputField} from '../../../ui/CheckboxGroupInputField';
import { showFieldGroupPopup, showInfoPopup, showPinMessage, } from '../../../ui/PopupUtil';
import {RadioGroupInputField} from '../../../ui/RadioGroupInputField';
import {useFieldGroupValue} from '../../../ui/SimpleComponent';
import {advancedTrim} from '../../../util/WebUtil';
import {CCUtil, CysConverter} from '../../CsysConverter';
import {getExtName} from '../../FitsHeaderUtil';
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
const MAX_HDU= 21;
const MIN_FULL_TITLE_LEN= 15;
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
    const extractionSizeX= axis==='x' ? 1 : pointSize;
    const extractionSizeY= axis==='y' ? 1: pointSize;
    const params= {baseImPtAry, pv, extractionSizeX, extractionSizeY, combineOp, save,
        doOverlay, isLine, allMatchingHDUs: true};
    const {canDoMultiImage}= getExtractableImageList(pv,true);
    if (!canDoMultiImage) {
        return Promise.resolve([makeDataExtractionTable(params)]);
    }
    const {pvAry}= getExtractableImageList(pv,false);


    return new Promise((resolve) => {
        const handleAnswer= ({hdus= 'all', titleType='full', whatToExtract, pvAry:extractPvAry}) => {
            const allMatchingHDUs= hdus==='all';
            if (whatToExtract==='allDiff') {
                const tblIdAry= extractPvAry.map( (pv) => {
                    const ccBase = CysConverter.make(primePlot(extractPvAry[0]));
                    const cc= CysConverter.make(primePlot(pv));
                    const newBaseImPtAry= baseImPtAry.map((pt) => cc.getImageCoords(ccBase.getWorldCoords(pt)));
                    return makeDataExtractionTable({baseImPtAry:newBaseImPtAry, pv, extractionSizeX, extractionSizeY,
                        combineOp, save, doOverlay, isLine, exclusiveToPlot:true, allMatchingHDUs});
                } );
                resolve(tblIdAry);
            }
            else {
                const tbl_id= (whatToExtract==='allSame')
                    ? makeDataExtractionTable({...params, allMatchingHDUs, pvAry: extractPvAry, truncatedHeaders:titleType!=='full'})
                    : makeDataExtractionTable({...params, allMatchingHDUs});
                resolve([tbl_id]);
            }
        };
        showFieldGroupPopup({groupKey:'ExtractOptions', keepState:true, successText:'Pin Chart/Table',
            content:<ExtractTableOptions pvAry={pvAry} />,
            onSuccess:handleAnswer,  title:'Extract Image Type'});
    });
}


const ExtractTableOptions= ({pvAry:initPvAry}) => {
    const whatToExtract= useFieldGroupValue('whatToExtract')[0]();
    const allHdus= useFieldGroupValue('hdus')[0]()==='all';
    const [getPvAry,setPvAry]= useFieldGroupValue('pvAry');
    const someHduOverMax=  initPvAry.some( (testPv) => primePlot(testPv).totalImageHdusInFile>MAX_HDU);

    const pvAry= getPvAry() ?? initPvAry;
    const allToSameFile= whatToExtract==='allSame';

    useEffect(() => {
        const newPvAry= (allHdus && allToSameFile)
            ? initPvAry.filter( (pv) => primePlot(pv).totalImageHdusInFile<=MAX_HDU && hasWCSProjection(pv))
            : initPvAry.filter( (pv) => hasWCSProjection(pv));
        setPvAry(newPvAry);
    }, [allHdus, allToSameFile, initPvAry, someHduOverMax]);

    const wcsPvAry= initPvAry.filter( (testPv) => hasWCSProjection(testPv));
    const wcsWarning= wcsPvAry.length < initPvAry.length;
    const hasLongTitles= pvAry.some( (pv) =>  primePlot(pv)?.title?.length>MIN_FULL_TITLE_LEN);
    const someMultiHDUs=  initPvAry.some( (testPv) => primePlot(testPv).totalImageHdusInFile>1);
    const maxHduWarning= (allToSameFile && allHdus)
        ? initPvAry.some( (testPv) => primePlot(testPv).totalImageHdusInFile>MAX_HDU)
        : false ;

    return (
        <Stack spacing={3} m={2}>
            <Stack spacing={1}>
                <Stack spacing={2} sx={{width:'30rem'}}>
                    <Typography>
                        You can either extract from the currently selected single image, or all loaded images that have a WCS
                        and a limited amount of HDUs (with WCS information).
                    </Typography>
                    <Typography color='warning'>
                        What do you want to extract?
                    </Typography>
                </Stack>
                <Stack pl={6}>
                    <RadioGroupInputField
                        fieldKey='whatToExtract'
                        options={[
                            {label: 'Selected image only', value: 'single'},
                            {label: 'All images (into one table)', value: 'allSame'},
                            {label: 'All images (into multiple tables)', value: 'allDiff'}
                        ]}
                        initialState= {{value: 'allSame' }} />
                </Stack>
            </Stack>
            <Stack spacing={1} pl={6}>
                {someMultiHDUs &&
                    <CheckboxGroupInputField
                        fieldKey='hdus' options={[{
                            label:'Include all matching HDUs', value: 'all'}]}
                        initialState= {{value: '' }} />
                }
                {hasLongTitles &&
                    <CheckboxGroupInputField
                        fieldKey='titleType'
                        sx={{visibility: (allToSameFile) ? 'visible' : 'hidden'}}
                        options={[{label:'Use full image titles in column headers', value: 'full'}]}
                        initialState= {{value: '' }} />
                }
            </Stack>
            <Stack>
                {(wcsWarning || maxHduWarning) &&
                    <Typography color='warning' level='body-sm'> Cannot extract from all images.
                    </Typography>}
                <Stack sx={{pl:2}}>
                    {wcsWarning &&
                        <Typography level='body-sm'> Some images do not have a WCS Projection.
                        </Typography>}
                    {maxHduWarning &&
                        <Typography level='body-sm'>
                            <div>
                                {` Some images have more than ${MAX_HDU} HDUs.`}
                            </div>
                            <div>
                                Hint: turn off 'include all match HDUs' or don't use 'All image (into one table)'
                            </div>
                        </Typography>}
                    {(wcsWarning || maxHduWarning) &&
                        <Typography level='body-sm'>
                            These images can also be extracted in single image mode
                        </Typography>
                    }
                </Stack>
            </Stack>
        </Stack>
    );
};


export function makeDataExtractionTable({baseImPtAry, pv, pvAry, extractionSizeX, extractionSizeY,
                                       combineOp, save = false, doOverlay = true,
                                            truncatedHeaders=false,
                                            allMatchingHDUs= true,
                                            exclusiveToPlot=false, isLine}) {
    const plot= primePlot(pv);
    const tbl_id = getNextTblId();
    const cc = CysConverter.make(plot);

    const baseWpAry= hasWCSProjection(plot) ? baseImPtAry.map((pt) => cc.getWorldCoords(pt)) : undefined;
    const filteredPvAry= pvAry ?? [pv];



    const headers= truncatedHeaders ? getTitles(filteredPvAry) : filteredPvAry.map( (pv) => primePlot(pv)?.title ?? '');

    const epBase= {
        wptAry: baseWpAry ? JSON.stringify(baseWpAry.map((wpt) => wpt.toString())) :  undefined,
        startIdx: 0,
        extractionType: isLine ? 'line' : 'points',
        extractionSizeX,
        extractionSizeY,
        [ServerParams.COMBINE_OP]: combineOp,
        allMatchingHDUs,
        titleAry: JSON.stringify(headers),
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
    const lenTest= [MIN_FULL_TITLE_LEN,MIN_FULL_TITLE_LEN+3,MIN_FULL_TITLE_LEN+6,MIN_FULL_TITLE_LEN+9];
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

function getExtractableImageList(pv,limitHDUs=true) {
    const singleRet= {filteredPvAry:[pv], pvAry:[pv], canDoMultiImage:false};
    const plot= primePlot(pv);
    if (!hasWCSProjection(plot)) return singleRet;
    if (limitHDUs && plot.totalImageHdusInFile>MAX_HDU) return singleRet;
    const pvAry= [pv,
        ...getPlotViewAry(visRoot(), pv.plotGroupId).filter( (testPv) => isImage(primePlot(testPv)) && testPv!==pv) ];
    if (pvAry.length===1) return singleRet;
    const filteredPvAry= pvAry.filter( (testPv) => hasWCSProjection(testPv) && (!limitHDUs || primePlot(testPv).totalImageHdusInFile<=MAX_HDU) );
    return {filteredPvAry,pvAry,canDoMultiImage:true};
}