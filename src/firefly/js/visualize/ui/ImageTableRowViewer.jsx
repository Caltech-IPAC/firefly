/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {useEffect, useRef, useState} from 'react';
import {string, number, func, bool, object, oneOfType} from 'prop-types';
import {difference, isNil, xor} from 'lodash';
import Slider from 'react-slick';
import 'slick-carousel/slick/slick.css';
import 'slick-carousel/slick/slick-theme.css';

import {useFieldGroupValue, useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {getTblById} from '../../tables/TableUtil.js';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {dispatchReplaceViewerItems, getMultiViewRoot, getViewer, IMAGE, NewPlotMode,} from '../MultiViewCntlr.js';
import {
    dispatchChangeActivePlotView, dispatchDeletePlotView,
    dispatchPlotImage,
    dispatchWcsMatch,
    visRoot,
    WcsMatchType
} from '../ImagePlotCntlr.js';
import {SORT_ASC, SortInfo, UNSORTED} from '../../tables/SortInfo.js';
import {CloseButton} from 'firefly/ui/CloseButton';
import {VisMiniToolbar} from './VisMiniToolbar.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {getActivePlotView, getPlotViewAry, getPlotViewById} from '../PlotViewUtil.js';
import {isImageDataRequestedEqual} from '../WebPlotRequest.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import Validate from '../../util/Validate.js';
import {dispatchTableHighlight} from 'firefly/tables/TablesCntlr';
import {Box, Sheet, Stack, Typography} from '@mui/joy';
import {SwitchInputFieldView} from 'firefly/ui/SwitchInputField';
import {BeforeButton, NextButton} from 'firefly/visualize/ui/Buttons';


const MAX_IMAGE_CNT= 7;
const IMAGE_CNT_KEY= 'imageCount';
const CUTOUT_SIZE= 'cutoutSize';


export function ImageTableRowViewer({viewerId, makeRequestFromRow, defaultCutoutSizeAS, tbl_id, defaultWcsMatchType,
                                       defaultImageCnt= 5, imageExpandedMode, insideFlex=true,
                                        closeExpanded, maxImageCnt= MAX_IMAGE_CNT}) {
    const table= useStoreConnector(() => getTblById(tbl_id));
    const imageCnt= useFieldGroupValue(IMAGE_CNT_KEY,viewerId)[0]() ?? defaultImageCnt;
    const cutoutSize= useFieldGroupValue(CUTOUT_SIZE,viewerId)[0]() ?? defaultCutoutSizeAS;
    const {wcsMatchType, activePlotId}= useStoreConnector(() =>
        ({wcsMatchType: visRoot().wcsMatchType, activePlotId:visRoot().activePlotId}) );

    //flags to ensure that effects are fired only when active plot or highlighted row was changed through UI interaction
    //and not when they were changed through the synchronisation code
    const activePlotChangedByUI = useRef(true);
    const hRowChangedByUI = useRef(true);

    //keep track of slide index
    const [currentSlideIdx, setCurrentSlideIdx] = useState(0);
    const onSlideChange = (current, next) => setCurrentSlideIdx(next);

    useEffect(()=>{
        if (!activePlotId?.startsWith(plotIdRoot(viewerId))) return;

        if (activePlotChangedByUI.current) {
            const activePlotRowNum = getPlotIdRowNum(viewerId, activePlotId);
            changeHighlightedRow(table, activePlotRowNum, hRowChangedByUI); //synchronise highlighted row as per active plot
        }
        else activePlotChangedByUI.current = true;
    }, [activePlotId]);

    useEffect(() => {
        if (!table || table.isFetching || !makeRequestFromRow) return;

        if (hRowChangedByUI.current) {
            const midSlideIdx = table?.highlightedRow;
            layoutImages(viewerId, Number(cutoutSize), Number(imageCnt), table, makeRequestFromRow, midSlideIdx);
            changeActivePlot(viewerId, table, midSlideIdx, activePlotChangedByUI); //synchronise active plot as per highlighted row
            adjustImageSlider(sliderRef, table, Number(imageCnt), midSlideIdx);
        }
        else hRowChangedByUI.current = true;
    }, [imageCnt, table, cutoutSize]);

    //handles whether to slide or not when slider arrow is clicked,
    //while dispatching the required actions in different conditions
    const slideOnArrowClick = (isNext) => {
        const [beforeCnt, afterCnt] = getBeforeAfterMidCounts(Number(imageCnt));
        const midSlideIdx = currentSlideIdx + beforeCnt;
        const lastSlideIdx = midSlideIdx + afterCnt;

        const rowToHighlight = isNext ? table.highlightedRow + 1 : table.highlightedRow - 1;
        const midSlideIdxAfterSliding = isNext ? midSlideIdx + 1 : midSlideIdx - 1;

        if((isNext && lastSlideIdx>=table.totalRows-1) || (!isNext && currentSlideIdx<=0) //at the edge
            || (isNext && table.highlightedRow < midSlideIdx) || (!isNext && table.highlightedRow > midSlideIdx)) {
            // move highlighted row/plot but keep slider as it is, until it comes to the middle of slider
            changeHighlightedRow(table, rowToHighlight, hRowChangedByUI);
            changeActivePlot(viewerId, table, rowToHighlight, activePlotChangedByUI);
            return false;
        }
        else if((isNext && table.highlightedRow > midSlideIdx) || (!isNext && table.highlightedRow < midSlideIdx)) {
            // keep highlighted row/plot as it is but move slider, until it comes to the middle of slider
            layoutImages(viewerId, Number(cutoutSize), Number(imageCnt), table, makeRequestFromRow, midSlideIdxAfterSliding);
            return true;
        }
        else { //table.highlightedRow === midSlideIdx; move highlighted row/plot as well as slider
            changeHighlightedRow(table, rowToHighlight, hRowChangedByUI);
            layoutImages(viewerId, Number(cutoutSize), Number(imageCnt), table, makeRequestFromRow, rowToHighlight);
            changeActivePlot(viewerId, table, rowToHighlight, activePlotChangedByUI);
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
                    makeRequestFromRow,defaultCutoutSizeAS, defaultWcsMatchType,
                    wcsMatchType, activePlotId,
                    showWhenExpanded:true, defaultDecoration:false}}/>
        );
    }
    return (
        <Sheet variant='outlined' sx={{display: 'flex', flexGrow: 1, borderRadius: '5px', maxWidth: 1}}>
            <MultiImageViewer
                {...{viewerId, Toolbar, insideFlex, defaultImageCnt, maxImageCnt, tableId:tbl_id,
                    makeRequestFromRow,defaultCutoutSizeAS, defaultWcsMatchType,
                    wcsMatchType, activePlotId, makeCustomLayout,
                    forceRowSize:1, canReceiveNewPlots: NewPlotMode.create_replace.key}}
            />
        </Sheet>
    );
}

ImageTableRowViewer.propTypes= {
    viewerId: string.isRequired,
    tbl_id: string.isRequired,
    defaultCutoutSizeAS: number,
    defaultWcsMatchType: object,
    defaultImageCnt: number,
    maxImageCnt: number,
    imageExpandedMode: bool,
    insideFlex: bool,
    makeRequestFromRow: func.isRequired,
    closeExpanded: func,
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
            //console.log(current, next, viewerItemIds); //turn on for debugging slide changes
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
                            : <span/> //empty placeholder-slide
                        }
                    </div>
                ))}
            </Slider>
        </Stack>
    );
}


function Toolbar({viewerId, tableId:tbl_id, closeFunc=null, maxImageCnt, defaultImageCnt, makeRequestFromRow,
                     defaultWcsMatchType=WcsMatchType.Standard, wcsMatchType, activePlotId,
                     defaultCutoutSizeAS, minCutoutSize=50, maxCutoutSize=1000}) {

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
                {defaultCutoutSizeAS && <ValidationField fieldKey={CUTOUT_SIZE}
                                 initialState= {{value: defaultCutoutSizeAS}}
                                 validator= {(v) =>  Validate.floatRange(minCutoutSize,maxCutoutSize,1,'cutout size',v,false)}
                                 tooltip='enter cutout size for the images'
                                 orientation='horizontal'
                                 label= 'Cutout Size (arcsec):'
                                 sx={{'.MuiInput-root': {width: '5rem'}}}
                />}
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
    makeRequestFromRow: func.isRequired,
    activePlotId: string,
    wcsMatchType: oneOfType([bool, object]),
    defaultWcsMatchType: object,
    defaultCutoutSizeAS: number,
    minCutoutSize: number,
    maxCutoutSize: number,
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
    const [visiblePlotsStartIdx,] = getVisiblePlotsRange(midSlideIdx, table.totalRows, imageCnt);
    sliderRef.current?.slickGoTo(visiblePlotsStartIdx); //slider idx is always the 1st slide shown
};

const changeHighlightedRow = (table, rowToHighlight, hRowChangedByUI) => {
    if (table && table.highlightedRow !== rowToHighlight) {
        dispatchTableHighlight(table.tbl_id, rowToHighlight);
        // to prevent setting it false when dispatch was aborted due to out of bound
        if (rowToHighlight >= 0 && rowToHighlight < table?.totalRows) hRowChangedByUI.current = false;
    }
};

const changeActivePlot = (viewerId, table, plotIdxToActivate, activePlotChangedByUI) => {
    const newActivePlotId = makePlotId(viewerId, plotIdxToActivate);
    if (getActivePlotView(visRoot())?.plotId !== newActivePlotId) {
        dispatchChangeActivePlotView(newActivePlotId);
        if (plotIdxToActivate >= 0 && plotIdxToActivate < table?.totalRows) activePlotChangedByUI.current = false;
    }
};

function layoutImages(viewerId, cutoutSize, imageCnt, table, makeRequestFromRow, midSlideIdx) {
    if (!table || isNil(midSlideIdx) || (table?.totalRows??0) < 1) return;
    const viewer= getViewer(getMultiViewRoot(),viewerId);
    if (!viewer) return;

    const vr = visRoot();
    const newPlotIdAry = makePlotIds(viewerId, midSlideIdx, table.totalRows, imageCnt);
    const exclusiveNewPlotIds = difference(newPlotIdAry, viewer.itemIdAry);
    exclusiveNewPlotIds.forEach((plotId) => {
        const rowNum = getPlotIdRowNum(viewerId,plotId);
        const wpRequest= makeRequestFromRow(table.tbl_id, rowNum, cutoutSize); //todo - needs documentation
        if (!wpRequest) return;

        const pv = getPlotViewById(vr, plotId);
        if (!pv || !isImageDataRequestedEqual(pv?.request, wpRequest)) {
            wpRequest.setRotateNorth(true);
            dispatchPlotImage({
                plotId, wpRequest,
                setNewPlotAsActive: false,
                holdWcsMatch: true,
                pvOptions: {userCanDeletePlots: false}
            });
        }
    });

    if (xor(viewer.itemIdAry,newPlotIdAry).length>0) { //check if any of the two arrays has a unique element
        dispatchReplaceViewerItems(viewerId, newPlotIdAry, IMAGE );
    }

    const {mpwWcsPrimId} = visRoot();
    const root= plotIdRoot(viewerId);

    const keepPlotIdAry = makePlotIds(viewerId, midSlideIdx, table.totalRows, MAX_IMAGE_CNT);
    getPlotViewAry(visRoot())
        .filter(({plotId}) => plotId.startsWith(root))
        .filter(({plotId}) => plotId !== mpwWcsPrimId)
        .filter(({plotId}) => !keepPlotIdAry.includes(plotId))
        .forEach(({plotId}) => dispatchDeletePlotView({plotId, holdWcsMatch: true}));
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
