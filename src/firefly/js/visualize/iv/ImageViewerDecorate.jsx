/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {Box, Stack} from '@mui/joy';
import React, {memo, useEffect, useState} from 'react';
import {object, array, bool, string, func} from 'prop-types';
import shallowequal from 'shallowequal';
import {isEmpty,omit,isFunction} from 'lodash';
import {getSearchActions} from '../../core/AppDataCntlr.js';
import HpxCatalog from '../../drawingLayers/hpx/HpxCatalog';
import {getTblById} from '../../tables/TableUtil';
import {EXPANDED_MODE_RESERVED, getMultiViewRoot, getViewer, GRID, IMAGE} from '../MultiViewCntlr.js';
import {getPlotGroupById}  from '../PlotGroup.js';
import {ExpandType, dispatchChangeActivePlotView, MOUSE_CLICK_REASON} from '../ImagePlotCntlr.js';
import {ExpandButton} from '../ui/Buttons.jsx';
import {VisCtxToolbarView, ctxToolbarBG} from '../ui/VisCtxToolbarView';
import {VisInlineToolbarView} from '../ui/VisInlineToolbarView.jsx';
import {
    primePlot, isActivePlotView, getAllDrawLayersForPlot, getPlotViewById, canConvertBetweenHipsAndFits
} from '../PlotViewUtil.js';
import {ImageViewerLayout}  from '../ImageViewerLayout.jsx';
import {isImage, isHiPS} from '../WebPlot.js';
import {PlotAttribute} from '../PlotAttribute.js';
import {AnnotationOps} from '../WebPlotRequest.js';
import {AREA_SELECT,LINE_SELECT,POINT} from '../../core/ExternalAccessUtils.js';
import {PlotTitle} from './PlotTitle.jsx';
import Catalog, {CatalogType} from '../../drawingLayers/Catalog.js';
import LSSTFootprint from '../../drawingLayers/ImageLineBasedFootprint';
import {DataTypes} from '../draw/DrawLayer.js';
import {wrapResizer} from '../../ui/SizeMeConfig.js';
import {getNumFilters} from '../../tables/FilterInfo';
import {ZoomButton, ZoomType} from 'firefly/visualize/ui/ZoomButton.jsx';
import {expand} from 'firefly/visualize/ui/VisMiniToolbar.jsx';

const EMPTY_ARRAY=[];

const briefAnno= [
    AnnotationOps.INLINE_BRIEF,
];

const isCatDl= (dl) => (dl?.drawLayerTypeId === Catalog.TYPE_ID || dl?.drawLayerTypeId === HpxCatalog.TYPE_ID);

const isCatalogPtData= (dl) => isCatDl(dl) && dl.catalogType===CatalogType.POINT;


/**
 * todo
 * show the select and filter button show?
 * @param pv
 * @param dlAry
 * @return {boolean}
 */
function showSelect(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .some( (dl) => (isCatalogPtData(dl) && (dl.canSelect && !dl.dataTooBigForSelection) ) ||
                        (dl.drawLayerTypeId === LSSTFootprint.TYPE_ID && dl.canSelect) );
}

function showFilter(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .some( (dl) => (isCatalogPtData(dl) && dl.canFilter) ||
                       (dl.drawLayerTypeId === LSSTFootprint.TYPE_ID && dl.canFilter) );
}

function showClearFilter(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .some( (dl) => {
            const request= dl.tableRequest ?? getTblById(dl.tbl_id)?.request;
            const filterCnt= getNumFilters(request);
            return (isCatalogPtData(dl) &&  filterCnt) ||
                   (dl.drawLayerTypeId === LSSTFootprint.TYPE_ID && filterCnt);
        });
}


/**
 * todo
 * show the unselect button?
 * @param pv
 * @param dlAry
 * @return {boolean}
 */
function showUnselect(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .filter( (dl) => {
            return (isCatalogPtData(dl) || dl.drawLayerTypeId===LSSTFootprint.TYPE_ID);
        })
        .some( (dl) => {

            const {exceptions= new Set(), selectAll= false}= getTblById(dl.tbl_id)?.selectInfo ?? {};
            const hasSelections= exceptions.size || (selectAll && !exceptions.size);
            // const selectIdxs=dl.drawData[DataTypes.SELECTED_IDXS];
            // const hasIndexes= isFunction(selectIdxs) || !isEmpty(selectIdxs);
            return (hasSelections && dl.canSelect);
        });
}


// TEST DATA
//var testExtAry= [
//    {
//        id: 'ext1ID',
//        plotId: pv.plotId,
//        title: 'Ext 1',
//        toolTip: 'tip for ext 1',
//        extType: LINE_SELECT,
//        callback() {console.log('hello ext 1')}
//    },
//    {
//        id: 'ext2ID',
//        plotId: pv.plotId,
//        title: 'Longer Ext 2',
//        toolTip: 'tip for ext 2, and a little longer',
//        extType: LINE_SELECT,
//        callback() {console.log('hello ext 2')}
//    }
//];



function contextToolbar(plotView,dlAry,extensionList, width, makeToolbar) {
    const plot= primePlot(plotView);
    if (!plot) return;

    const showMultiImageController= isImage(plot) ? plotView.plots.length>1 : plot.cubeDepth>1;
    const hipsFits= canConvertBetweenHipsAndFits(plotView);

    if (plot.attributes[PlotAttribute.SELECTION]) {
        const select= showSelect(plotView,dlAry);
        const unselect= showUnselect(plotView,dlAry);
        const filter= showFilter(plotView,dlAry);
        const clearFilter= showClearFilter(plotView,dlAry);
        const selAry= extensionList.filter( (ext) => ext.extType===AREA_SELECT);
        const extensionAry= isEmpty(selAry) ? EMPTY_ARRAY : selAry;
        const searchActions= getSearchActions();
        return (
            <VisCtxToolbarView {...{plotView, extensionAry, width,
                showSelectionTools:true, showCatSelect:select, showCatUnSelect:unselect, searchActions,
                showFilter:filter, showClearFilter:clearFilter, showMultiImageController, makeToolbar}} />
        );
    }
    else if (plot.attributes[PlotAttribute.ACTIVE_DISTANCE]) {
        const distAry= extensionList.filter( (ext) => ext.extType===LINE_SELECT);
        if (!distAry.length && !showMultiImageController && !hipsFits) return;
        return (
                <VisCtxToolbarView {...{plotView, extensionAry:isEmpty(distAry)?EMPTY_ARRAY:distAry,
                    width, showMultiImageController, makeToolbar}}/>
        );
    }
    else if (plot.attributes[PlotAttribute.ACTIVE_POINT]) {
        const ptAry= extensionList.filter( (ext) => ext.extType===POINT);
        if (!ptAry.length && !showMultiImageController && !hipsFits) return;
        return (
                <VisCtxToolbarView {...{plotView, extensionAry:isEmpty(ptAry)?EMPTY_ARRAY:ptAry, width,
                    showMultiImageController, makeToolbar}}/>
        );
    }
    else if (showUnselect(plotView, dlAry)) {
        return (
               <VisCtxToolbarView {...{plotView, extensionAry:EMPTY_ARRAY, width,
                       showCatUnSelect:true, showClearFilter:showClearFilter(plotView,dlAry),
                       showMultiImageController, makeToolbar}} />
        );
    }
    else if (showClearFilter(plotView,dlAry)) {
        return (
                <VisCtxToolbarView {...{plotView, extensionAry:EMPTY_ARRAY,  width,
                    showClearFilter:true, showMultiImageController, makeToolbar}} />
        );
    }
    else if (showMultiImageController || hipsFits || isHiPS(plot)) {
        return ( <VisCtxToolbarView {...{plotView, extensionAry:EMPTY_ARRAY, showMultiImageController, width, makeToolbar}}/> );
    }
}

function hasManyPlots(pv,visRoot) {
    const expandedViewer= getViewer(getMultiViewRoot(), EXPANDED_MODE_RESERVED);
    const manyExpanded= visRoot.expandedMode!==ExpandType.COLLAPSE &&
        expandedViewer.itemIdAry.length>1 && expandedViewer.layout===GRID;
    const containerList= getMultiViewRoot().filter( (v) => v.containerType===IMAGE && v.mounted);
    const manyPlots= manyExpanded ||
        containerList.length>1 ||
        (containerList[0]?.layout===GRID && containerList[0]?.itemIdAry?.length>1);
    return manyPlots;
}

function getBorderColor(manyPlots, theme, pv,visRoot) {
    if (!pv?.plotId) return 'rgba(0,0,0,.4)';
    if (!pv.plotViewCtx.highlightFeedback) return 'rgba(0,0,0,.1)';
    if (isActivePlotView(visRoot,pv.plotId) || pv.subHighlight) {
        return manyPlots ? `rgba(${theme.vars.palette.warning.mainChannel} / 1)` : 'rgba(0,0,0,.02)';
    }
    const group= getPlotGroupById(visRoot,pv.plotGroupId);
    if (group?.overlayColorLock) return 'rgba(0, 0, 0, .1)';
    else return 'rgba(0,0,0,.2)';
}



//===========================================================================
//---------- React Components -----------------------------------------------
//===========================================================================

const omitList= ['mousePlotId', 'size'];

function arePropsEquals(props, np) {
    if (props.size.width!==np.size.width || props.size.height!==np.size.height) return false;
    if (!shallowequal(omit(np,omitList), omit(props,omitList))) return false;
    // if (props.mousePlotId!==np.mousePlotId && (props.mousePlotId===plotId || np.mousePlotId===plotId)) return false;
    if (props.mousePlotId!==np.mousePlotId) return false;
    if (props.plotId!==np.plotId) return false;
    return true;
} //todo: look at closely for optimization


function ZoomGroup({visRoot, pv, show}) {

    const {showImageToolbar=true}= pv?.plotViewCtx.menuItemKeys ?? {};
    const manageExpand= !showImageToolbar && visRoot.expandedMode===ExpandType.COLLAPSE;
    const p= primePlot(pv);
    if (!p) return <div/>;

    const sxFunc= (theme) => ({
        visibility: show ? 'visible' : 'hidden',
        opacity: show ? 1 : 0,
        transition: show ? 'opacity .15s linear' : 'visibility 0s .15s, opacity .15s linear',
        background:ctxToolbarBG(theme, 85),
        borderRadius:'0 0 5px ',
        position: 'relative',
        maxWidth: '100%',
        alignSelf: 'flex-start',
    });

    return (
        <Stack direction='row' alignItems='flex-start' sx={sxFunc}>
            {manageExpand && <ExpandButton tip='Expand this panel to take up a larger area'
                                            onClick={() =>expand(pv?.plotId, false)}/>}

            <Stack direction='row' alignItems='flex-start'>
                <ZoomButton size={20} plotView={pv} zoomType={ZoomType.UP} />
                <ZoomButton size={20} plotView={pv} zoomType={ZoomType.DOWN} />
            </Stack>
            <Stack direction='row' alignItems='flex-start'>
                <ZoomButton size={20} plotView={pv} zoomType={ZoomType.FIT} />
                <ZoomButton size={20} plotView={pv} zoomType={ZoomType.FILL} />
                {isImage(p) && <ZoomButton size={20} plotView={pv} zoomType={ZoomType.ONE} />}
            </Stack>
        </Stack>
    );
    
}

const ImageViewerDecorate= memo((props) => {
    const {plotView:pv,drawLayersAry,extensionList,visRoot,mousePlotId, workingIcon,makeToolbar,
        size:{width,height}}= props;

    const [showDelAnyway, setShowSelAnyway]= useState(false);

    useEffect(() => {
        const mousePlotIdExist= Boolean(getPlotViewById(visRoot,mousePlotId));
        if (mousePlotIdExist) {
            setShowSelAnyway(false);
            return;
        }
        setShowSelAnyway(true);
        const id= setTimeout(() => setShowSelAnyway(false), 5000);
        return () => clearTimeout(id);
    },[mousePlotId]);

    const showDelete= pv.plotViewCtx.userCanDeletePlots;
    const ctxToolbar= contextToolbar(pv,drawLayersAry,extensionList,width, makeToolbar);
    const expandedToSingle= (visRoot.expandedMode===ExpandType.SINGLE);
    const plot= primePlot(pv);
    const iWidth= Math.trunc(width);
    const iHeight=Math.trunc(height);

    const brief= briefAnno.includes(pv.plotViewCtx.annotationOps);

    const outerStyle= { width: '100%', height: '100%', overflow:'hidden', position:'relative'};

    const innerStyle= (theme) => {
        const manyPlots= hasManyPlots(pv,visRoot);
        const active= isActivePlotView(visRoot,pv.plotId);
        return {
            width: !manyPlots  ? 1 : 'calc(100% - 4px)',
            bottom: 0,
            top: 0,
            overflow: 'hidden',
            position: 'absolute',
            borderStyle: !manyPlots ? undefined : (pv.subHighlight && !active) ? 'dashed' : 'solid' ,
            borderWidth: (expandedToSingle || !manyPlots) ? '0 0 0 0' : '1px',
            borderRadius: manyPlots ? '5px' : undefined,
            borderColor: getBorderColor(manyPlots, theme, pv,visRoot),
        };
    };

    const makeActive= () => pv?.plotId && dispatchChangeActivePlotView(pv.plotId,MOUSE_CLICK_REASON);
    const showZoom= mousePlotId===pv?.plotId;
    const showDel= showDelAnyway || mousePlotId===pv?.plotId || !plot || pv.nonRecoverableFail;

    return (
        <Box style={outerStyle} className='disable-select' onTouchStart={makeActive} onClick={makeActive} >
            <Stack className='image-viewer-decorate' direction='row' sx={innerStyle}>
                <Stack {...{position: 'absolute', width:1, top:0, bottom:0, mr:2 }}>
                    <ImageViewerLayout plotView={pv} drawLayersAry={drawLayersAry}
                                       width={iWidth} height={iHeight}
                                       externalWidth={width} externalHeight={height}/>
                    {ctxToolbar}
                    {(plot) ? <PlotTitle brief={brief}  plotView={pv} working={workingIcon} /> : undefined}
                    <ZoomGroup visRoot={visRoot} pv={pv} show={showZoom} />
                </Stack>
                <VisInlineToolbarView pv={pv} showDelete={showDelete} deleteVisible={showDel}/>
            </Stack>
        </Box>
        );

}, arePropsEquals);


ImageViewerDecorate.propTypes= {
    plotView : object.isRequired,
    drawLayersAry: array.isRequired,
    visRoot: object.isRequired,
    extensionList : array.isRequired,
    mousePlotId : string,
    size : object.isRequired,
    workingIcon: bool,
    makeToolbar: func,
};

export const ImageViewerView= wrapResizer(ImageViewerDecorate);
