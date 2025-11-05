/* * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {useEffect, useRef, useState} from 'react';
import {string, number, func, bool, object, oneOfType, node} from 'prop-types';
import {debounce, difference, isEmpty, isNil, isString, xor} from 'lodash';
import Slider from 'react-slick';
import 'slick-carousel/slick/slick.css';
import 'slick-carousel/slick/slick-theme.css';
import {dispatchAddWorkingTask} from '../../core/AppDataCntlr';
import {getComponentState} from '../../core/ComponentCntlr';
import {CutoutButton, SHOWING_CUTOUT, SHOWING_FULL} from '../../ui/CutoutSizeDialog';
import {IfWorkingMaskById} from '../../ui/panel/MaskPanel';

import {useFieldGroupValue, useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {getTblById, isFullyLoaded} from '../../tables/TableUtil.js';
import {getPreferCutout} from '../../ui/tap/Cutout';
import {callWhileAwaiting} from '../../util/WebUtil';
import {ImageViewerPlaceHolder} from '../iv/ImageViewer';
import {onPlotComplete} from '../PlotCompleteMonitor';
import {UserZoomTypes} from '../ZoomUtil';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {dispatchReplaceViewerItems, getMultiViewRoot, getViewer, IMAGE, NewPlotMode,} from '../MultiViewCntlr.js';
import {
    dispatchChangeActivePlotView, dispatchDeletePlotView,
    dispatchPlotImage, dispatchRecenter, dispatchWcsMatch, dispatchZoom, visRoot, WcsMatchType
} from '../ImagePlotCntlr.js';
import {SORT_ASC, SortInfo, UNSORTED} from '../../tables/SortInfo.js';
import {CloseButton} from 'firefly/ui/CloseButton';
import {VisMiniToolbar} from './VisMiniToolbar.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {getActivePlotView, getPlotViewAry, getPlotViewById} from '../PlotViewUtil.js';
import {isImageDataRequestedEqual} from '../WebPlotRequest.js';
import {dispatchTableHighlight} from 'firefly/tables/TablesCntlr';
import {Box, Stack, Typography} from '@mui/joy';
import {SwitchInputFieldView} from 'firefly/ui/SwitchInputField';
import {BeforeButton, NextButton} from 'firefly/visualize/ui/Buttons';


const MAX_IMAGE_CNT= 7;
const IMAGE_CNT_KEY= 'imageCount';
const IMAGE_TABLE_ERROR = 'Unable to load images because the required data table couldn\'t be retrieved.';

const FORCE_RELOAD='FORCE_RELOAD';
const TABLE_ROW='TABLE_ROW';
const PAGE='PAGE';

export function ImageTableRowViewer({viewerId, makeRequestFromRow, defaultCutoutSizeAS, tbl_id, defaultWcsMatchType,
                                       defaultImageCnt= 5, imageExpandedMode, insideFlex=true, cutoutWpt,
                                        closeExpanded, maxImageCnt= MAX_IMAGE_CNT, tblErrorMsg=IMAGE_TABLE_ERROR}) {
    const table= useStoreConnector(() => getTblById(tbl_id));
    const tblLoaded = useStoreConnector(() => isFullyLoaded(tbl_id));
    const imageCnt= useFieldGroupValue(IMAGE_CNT_KEY,viewerId)[0]() ?? defaultImageCnt;
    const {wcsMatchType, activePlotId}= useStoreConnector(() =>
        ({wcsMatchType: visRoot().wcsMatchType, activePlotId:visRoot().activePlotId}) );
    const cutoutState= useStoreConnector(() =>getComponentState(viewerId));
    const hasTable= Boolean(table);


    //flags to ensure that effects are fired only when active plot or highlighted row was changed through UI interaction
    //and not when they were changed through the synchronisation code
    // const activePlotChangedByUI = useRef(true);
    const hRowChangedByUI = useRef(true);
    const layoutImageAbort = useRef();

    //keep track of slide index
    const [currentSlideIdx, setCurrentSlideIdx] = useState(0);
    const onSlideChange = (current, next) => setCurrentSlideIdx(next);


    const layoutParams= {viewerId, imageCnt:Number(imageCnt), table, makeRequestFromRow, cutoutWpt};

    useEffect( ()=>{
        if (hasTable) recenterImages(viewerId) ;
    }, [viewerId, hasTable]);

    useEffect(() => {
        if (isEmpty(cutoutState) || !table || !tblLoaded) return;
        layoutImageAbort.current= layoutImages({
            ...layoutParams, midSlideIdx:table?.highlightedRow, loadType:FORCE_RELOAD,
            previousCallAbort:layoutImageAbort.current});
    }, [cutoutState?.preferCutout?.LAST_PREF, cutoutState?.sdCutoutSize,
        cutoutState?.sdCutoutWpOverride?.x, cutoutState?.sdCutoutWpOverride?.y, table?.request?.source, tblLoaded]);

    useEffect(()=>{
        if (!activePlotId?.startsWith(plotIdRoot(viewerId))) return;
        const activePlotRowNum = getPlotIdRowNum(viewerId, activePlotId);
        changeHighlightedRow(table, activePlotRowNum, hRowChangedByUI); //synchronise highlighted row as per active plot
    }, [activePlotId]);

    useEffect(() => {
        if (!table || !tblLoaded || !makeRequestFromRow) return;

        if (hRowChangedByUI.current) {
            const midSlideIdx = table?.highlightedRow;
            layoutImageAbort.current= layoutImages({
                ...layoutParams, midSlideIdx, loadType:TABLE_ROW,
                previousCallAbort:layoutImageAbort.current}
        );
            adjustImageSlider(sliderRef, table, Number(imageCnt), midSlideIdx);
        }
        else hRowChangedByUI.current = true;
    }, [table, tblLoaded, makeRequestFromRow, viewerId, imageCnt]);

    //handles whether to slide or not when slider arrow is clicked,
    //while dispatching the required actions in different conditions
    const slideOnArrowClick = (isNext) => {
        const [beforeCnt, afterCnt] = getBeforeAfterMidCounts(Number(imageCnt));
        const midSlideIdx = currentSlideIdx + beforeCnt;
        const lastSlideIdx = midSlideIdx + afterCnt;
        const updatedTable= getTblById(tbl_id);

        const rowToHighlight = isNext ? updatedTable.highlightedRow + 1 : updatedTable.highlightedRow - 1;
        const midSlideIdxAfterSliding = isNext ? midSlideIdx + 1 : midSlideIdx - 1;

        if((isNext && lastSlideIdx>=updatedTable.totalRows-1) || (!isNext && currentSlideIdx<=0) //at the edge
            || (isNext && updatedTable.highlightedRow < midSlideIdx) || (!isNext && updatedTable.highlightedRow > midSlideIdx)) {
            // move highlighted row/plot but keep slider as it is, until it comes to the middle of slider
            changeHighlightedRow(updatedTable, rowToHighlight, hRowChangedByUI);
            changeActivePlot(viewerId, updatedTable, rowToHighlight);
            return false;
        }
        else if((isNext && updatedTable.highlightedRow > midSlideIdx) || (!isNext && updatedTable.highlightedRow < midSlideIdx)) {
            // keep highlighted row/plot as it is but move slider, until it comes to the middle of slider
            layoutImageAbort.current= layoutImages({
                ...layoutParams, midSlideIdx:midSlideIdxAfterSliding, previousCallAbort:layoutImageAbort.current});
            return true;
        }
        else { //table.highlightedRow === midSlideIdx; move highlighted row/plot as well as slider
            changeHighlightedRow(updatedTable, rowToHighlight, hRowChangedByUI);
            layoutImageAbort.current= layoutImages({
                ...layoutParams, midSlideIdx:rowToHighlight, previousCallAbort:layoutImageAbort.current});
            return true;
        }
    };

    const sliderRef = useRef(null);
    const makeCustomLayout = (viewerItemIds, makeItemViewer) => (
        <ImageSlider {...{sliderRef, viewerId, table, imageCnt: Number(imageCnt),
            viewerItemIds, makeItemViewer, slideOnArrowClick, onSlideChange}}/>
    );

    if (imageExpandedMode) {
        return (
            <MultiImageViewer
                {...{viewerId, Toolbar, insideFlex, closeFunc:closeExpanded, defaultImageCnt, maxImageCnt, tableId:tbl_id,
                    defaultWcsMatchType, cutoutWpt, wcsMatchType, activePlotId,
                    showWhenExpanded:true, defaultDecoration:false}}/>
        );
    }

    if (!table) {
        return isString(tblErrorMsg)
            ? <Typography level='title-lg' sx={{m:'auto'}}>{tblErrorMsg}</Typography>
            : tblErrorMsg;
    }

    return (
        <div style={{width:'100%', height:'100%', display:'flex', position: 'relative'}}>
            <MultiImageViewer
                {...{viewerId, Toolbar, insideFlex, defaultImageCnt, maxImageCnt, tableId:tbl_id,
                    makeRequestFromRow,defaultCutoutSizeAS, defaultWcsMatchType, cutoutWpt,
                    wcsMatchType, activePlotId, makeCustomLayout, mouseReadoutEmbedded: false,
                    forceRowSize:1, canReceiveNewPlots: NewPlotMode.create_replace.key}}
            />
            <IfWorkingMaskById {...{id:'ImageTableRowViewer', message:'Searching Images',
                           gridSize:{rows:1, cols:Number(imageCnt)}, sx:{top:34, bottom:20} }}/>
        </div>
    );
}

ImageTableRowViewer.propTypes= {
    viewerId: string.isRequired,
    tbl_id: string.isRequired,
    defaultCutoutSizeAS: number,
    defaultWcsMatchType: object,
    cutoutWpt: object,
    defaultImageCnt: number,
    maxImageCnt: number,
    imageExpandedMode: bool,
    insideFlex: bool,
    makeRequestFromRow: func.isRequired, /*pass*/
    closeExpanded: func,
    tblErrorMsg: node,
};



function ImageSlider({viewerId, table, imageCnt, viewerItemIds, makeItemViewer, sliderRef, slideOnArrowClick, onSlideChange}) {

    const SliderArrow = ({ onClick, isNext }) => {
        // `onClick` prop is inserted by react-slick
        const handleArrowClick = (e) => slideOnArrowClick(isNext)
            ? onClick?.(e)  // allow sliding
            : e.preventDefault();  // prevent sliding

        const sx = {position: 'absolute', top: '50%', transform: 'translateY(-50%)'};
        return isNext
            ? <NextButton onClick={(_, e) => handleArrowClick(e)}
                          tip={'Next Image'}
                          enabled={table?.highlightedRow < table?.totalRows-1} //disable when last row/slide is selected
                          sx={{...sx, right: -25}}/>
            : <BeforeButton onClick={(_, e) => handleArrowClick(e)}
                            tip={'Previous Image'}
                            enabled={table?.highlightedRow > 0} //disable when 1st row/slide is selected
                            sx={{...sx, left: -25}}/>;
    };

    const settings = {
        dots: false,
        infinite: false,
        draggable: false,
        slidesToShow: imageCnt,
        slidesToScroll: 1,
        nextArrow: (<SliderArrow isNext={true}/>),
        prevArrow: (<SliderArrow isNext={false}/>),
        beforeChange: (current, next) => {
            onSlideChange(current, next);
        }
    };

    // override default styles of slick-carousel (used by react-slick)
    const sliderStyleOverrides = {
        '.slick-slider': {
            width: 'calc(100% - 50px)', //each slider arrow is 25px wide
            height: 1,
            margin: 'auto'
        },
        '.slick-list, .slick-track' : { height: 1 },
        '.slick-slide': { position: 'relative' },
        '.slick-disabled:before': { cursor: 'auto' }
    };

    return (
        <Stack sx={{ height: 1, width: 1, '&': sliderStyleOverrides }}>
            <Slider ref={sliderRef} {...settings}>
                {Array.from({length: table?.totalRows || viewerItemIds.length}).map((_, i)=>(
                    <div key={'slide-'+i}>
                        {viewerItemIds.includes(makePlotId(viewerId,i))
                            ? <Box sx={{display: 'inline-block', position: 'absolute', top: 0, width: 1, height: 1}}>
                                {makeItemViewer(makePlotId(viewerId,i))}
                            </Box>
                            : <ImageViewerPlaceHolder plotId={makePlotId(viewerId,i)}/>
                        }
                    </div>
                ))}
            </Slider>
        </Stack>
    );
}


function Toolbar({viewerId, tableId:tbl_id, closeFunc=null, maxImageCnt, defaultImageCnt,
                     defaultWcsMatchType=WcsMatchType.Standard, wcsMatchType, activePlotId, cutoutWpt,
                     defaultCutoutSizeAS}) {

    useEffect(()=>{
        if(wcsMatchType!==defaultWcsMatchType) {
            //to make sure wcsMatch checkbox is checked on initial render
            dispatchWcsMatch({matchType: defaultWcsMatchType, plotId: activePlotId});
        }
    }, []);

    const wcsMatch = (defaultWcsMatchType===WcsMatchType.Standard || defaultWcsMatchType===WcsMatchType.Target) && (
        <SwitchInputFieldView
            size='sm'
            value={wcsMatchType===defaultWcsMatchType}
            onChange={(ev) => defaultWcsMatchType===WcsMatchType.Standard
                ? doWcsMatch(ev.target.checked, activePlotId)
                : wcsMatchTarget(ev.target.checked, activePlotId)}
            endDecorator={defaultWcsMatchType===WcsMatchType.Standard ? 'WCS Match' : 'Target Match'}
        />
    );

    const options= [];
    for(let i= 1; (i<=maxImageCnt); i+=2) {
        options.push({label: i+'', value: i+''});
    }

    return (
        <FieldGroup groupKey={viewerId} keepState={true}>
            <Stack direction='row' spacing={1} alignItems='center' justifyContent='space-between' sx={{mx: 1}}>
                {closeFunc && <CloseButton onClick={closeFunc}/>}
                <RadioGroupInputField {...{
                    label: 'Image Count:',
                    options, fieldKey:IMAGE_CNT_KEY,
                    orientation: 'horizontal', tooltip:'Choose number of images to show',
                    initialState: {value: defaultImageCnt+''}
                }} />
                {defaultCutoutSizeAS &&
                    <CutoutButton {...{
                        dataProductsComponentKey:viewerId,
                        activeTable:getTblById(tbl_id),
                        serDef:undefined,
                        enableCutoutFullSwitching:true,
                        cutoutMode: getPreferCutout(viewerId, tbl_id) ? SHOWING_CUTOUT : SHOWING_FULL,
                        overrideCutoutWpt:cutoutWpt,
                    }}/>
                }
                <SortDirFeedback table={getTblById(tbl_id)}/>
                {wcsMatch}
                <VisMiniToolbar sx={{width:'auto'}}/>
            </Stack>
        </FieldGroup>
    );
}


function SortDirFeedback({table})  {
    if (!table?.request?.sortInfo) return;
    const sInfo = SortInfo.parse(table.request.sortInfo);
    if (sInfo.direction === UNSORTED) return;
    const dirStr= sInfo.direction===SORT_ASC ? 'ascending' : 'descending';
    return (
        <Typography level='body-sm'>
            {`Sorted by column: ${sInfo.sortColumns.join(',')}  ${dirStr}`};
        </Typography>
    );
}


Toolbar.propTypes= {
    viewerId : string.isRequired,
    tableId: string.isRequired,
    closeFunc : func,
    defaultImageCnt: number,
    maxImageCnt: number,
    makeRequestFromRow: func.isRequired, // passes viewerId, tbl_id, rowNum, centerWp then returns a Promise<WebPlotRequest>
    activePlotId: string,
    wcsMatchType: oneOfType([bool, object]),
    defaultWcsMatchType: object,
    defaultCutoutSizeAS: number,
    minCutoutSize: number,
    maxCutoutSize: number,
    cutoutWpt: object,
};


const wcsMatchTarget= (doWcsStandard, plotId) =>
    dispatchWcsMatch({matchType:doWcsStandard?WcsMatchType.Target:false, plotId});

const doWcsMatch= (doWcsStandard, plotId) =>
    dispatchWcsMatch({matchType:doWcsStandard?WcsMatchType.Standard:false, plotId});

//
// function changeSize(viewerId, value) {
//     value = Number(value);
//     dispatchChangeViewerLayout(viewerId, value === 1 ? SINGLE : GRID, {count: value});
// }
//
const adjustImageSlider = (sliderRef, table, imageCnt, midSlideIdx) => {
    adjustImageSliderDebounce(sliderRef, table, imageCnt, midSlideIdx) ;
};

const adjustImageSliderDebounce = debounce((sliderRef, table, imageCnt, midSlideIdx) => {
    const [visiblePlotsStartIdx,] = getVisiblePlotsRange(midSlideIdx, table.totalRows, imageCnt);
    sliderRef.current?.slickGoTo(visiblePlotsStartIdx); //slider idx is always the 1st slide shown
}, 500);

const changeHighlightedRow = (table, rowToHighlight, hRowChangedByUI) => {
    if (table && table.highlightedRow !== rowToHighlight) {
        if (!table.isFetching) {
            dispatchTableHighlight(table.tbl_id, rowToHighlight);
            // to prevent setting it false when dispatch was aborted due to out of bound
            if (rowToHighlight >= 0 && rowToHighlight < table?.totalRows) hRowChangedByUI.current = false;
        }
    }
};

const changeActivePlot = (viewerId, table, plotIdxToActivate) => {
    const newActivePlotId = makePlotId(viewerId, plotIdxToActivate);
    if (getActivePlotView(visRoot())?.plotId !== newActivePlotId) {
        dispatchChangeActivePlotView(newActivePlotId);
        // if (plotIdxToActivate >= 0 && plotIdxToActivate < table?.totalRows) activePlotChangedByUI.current = false;
    }
};


function layoutImages({previousCallAbort, ...params}) {
    let processingAborted= false;
    const isProcessAborted= () => processingAborted;
    previousCallAbort?.();
    void doLayoutImages({...params, isProcessAborted});
    return () => processingAborted= true;
}

async function doLayoutImages({viewerId, imageCnt, table, makeRequestFromRow,
                                  midSlideIdx, cutoutWpt, loadType=PAGE, isProcessAborted}) {

    if (!table || isNil(midSlideIdx) || (table?.totalRows??0) < 1) return;
    const viewer= getViewer(getMultiViewRoot(),viewerId);
    if (!viewer) return;

    let vr = visRoot();
    const newPlotIdAry = makePlotIds(viewerId, midSlideIdx, table.totalRows, imageCnt);
    const exclusiveNewPlotIds = loadType===FORCE_RELOAD ? newPlotIdAry : difference(newPlotIdAry, viewer.itemIdAry);
    const promiseAry= exclusiveNewPlotIds.map( async (plotId) => {
        const rowNum = getPlotIdRowNum(viewerId,plotId);
        const wpRequestPromise= makeRequestFromRow(viewerId, table.tbl_id, rowNum, cutoutWpt); //todo - needs documentation
        const wpRequest= await callWhileAwaiting(wpRequestPromise,
            (promise) => loadType===PAGE && dispatchAddWorkingTask(plotId,promise,'Searching Image'), 400 );
        if (!wpRequest) return;

        const pv = getPlotViewById(vr, plotId);
        if (!pv || !isImageDataRequestedEqual(pv?.request, wpRequest)) {
            wpRequest.setRotateNorth(true);
            dispatchPlotImage({
                plotId, wpRequest,
                viewerId,
                setNewPlotAsActive: false,
                holdWcsMatch: true,
                pvOptions: {userCanDeletePlots: false}
            });
        }
    });

    // wait for all the request to resolve

    await callWhileAwaiting(Promise.all(promiseAry),
        (promise) => loadType!==PAGE && dispatchAddWorkingTask('ImageTableRowViewer',promise), 500);
    if (isProcessAborted()&& loadType!==FORCE_RELOAD) return;

    if (xor(viewer.itemIdAry,newPlotIdAry).length>0) { //check if any of the two arrays has a unique element
        dispatchReplaceViewerItems(viewerId, newPlotIdAry, IMAGE );
    }

    const {mpwWcsPrimId} = visRoot();
    const root= plotIdRoot(viewerId);



    const finishedPromiseAry= exclusiveNewPlotIds.map( (plotId) => onPlotComplete(plotId));
    await Promise.all(finishedPromiseAry); // wait for all the plots to complete
    if (isProcessAborted() && loadType!==FORCE_RELOAD) return;

    if (loadType===FORCE_RELOAD) {
        vr= visRoot();
        const plotId= exclusiveNewPlotIds.find( (plotId) => getPlotViewById(vr, plotId).serverCall==='success');
        dispatchChangeActivePlotView(plotId);
        dispatchZoom({ plotId, userZoomType: UserZoomTypes.FIT, });
        dispatchRecenter({plotId});
    }
    else {
        const plotId= newPlotIdAry.find( (id) => id.endsWith(midSlideIdx+''));
        if (plotId) dispatchChangeActivePlotView(plotId);
    }

    const keepPlotIdAry = makePlotIds(viewerId, midSlideIdx, table.totalRows, MAX_IMAGE_CNT);
    if (loadType!==PAGE) {
        getPlotViewAry(visRoot()) // clean up old plots
            .filter(({plotId}) => plotId.startsWith(root))
            .filter(({plotId}) => plotId !== mpwWcsPrimId)
            .filter(({plotId}) => !keepPlotIdAry.includes(plotId))
            .forEach(({plotId}) => dispatchDeletePlotView({plotId, holdWcsMatch: true}));
    }
}

function recenterImages(viewerId) {
    const root= plotIdRoot(viewerId);
    const pidAry= getPlotViewAry(visRoot()).filter(({plotId}) => plotId.startsWith(root));
    if (!pidAry.length) return;
    const {plotId}= pidAry[Math.trunc(pidAry.length/2)] ?? {};
    if (!plotId) return;
    dispatchChangeActivePlotView(plotId);
    dispatchRecenter({plotId,centerOnImage:true});
}

const getBeforeAfterMidCounts = (totalPlots) => totalPlots%2===0
    ? [totalPlots/2-1, totalPlots/2] : [(totalPlots-1)/2, (totalPlots-1)/2];


function getVisiblePlotsRange(midSlideIdx, totalRows, totalPlots) {
    const [beforeCnt, afterCnt] = getBeforeAfterMidCounts(totalPlots);
    const firstRowIdx = 0, lastRowIdx = totalRows-1;

    let rangeEndRowIdx = Math.min(lastRowIdx, midSlideIdx+afterCnt);
    let rangeStartRowIdx = Math.max(firstRowIdx, midSlideIdx-beforeCnt);

    // handle the edge case
    if (rangeStartRowIdx===firstRowIdx) rangeEndRowIdx = Math.min(lastRowIdx, totalPlots-1);
    if (rangeEndRowIdx===lastRowIdx) rangeStartRowIdx = Math.max(firstRowIdx, totalRows-totalPlots);

    return [rangeStartRowIdx, rangeEndRowIdx];
}

function makePlotIds(viewerId, midSlideIdx, totalRows, totalPlots)  {
    const [rangeStartRowIdx, rangeEndRowIdx] = getVisiblePlotsRange(midSlideIdx, totalRows, totalPlots);

    const plotIds= [];
    for(let i= rangeStartRowIdx; i<=rangeEndRowIdx; i++) plotIds.push(makePlotId(viewerId,i));
    return plotIds;
}

const plotIdRoot= (viewerId) => 'image--'+viewerId;

const makePlotId= (viewerId, rowIdx) => plotIdRoot(viewerId)+'-'+rowIdx;

const getPlotIdRowNum= (viewerId, plotId) => Number(plotId.substring(plotIdRoot(viewerId).length+1));
