/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {memo} from 'react';
import PropTypes from 'prop-types';
import shallowequal from 'shallowequal';
import {isEmpty,omit,isFunction} from 'lodash';
import {getPlotGroupById}  from '../PlotGroup.js';
import {ExpandType, dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {VisCtxToolbarView, canConvertHipsAndFits} from '../ui/VisCtxToolbarView';
import {VisInlineToolbarView} from '../ui/VisInlineToolbarView.jsx';
import {primePlot, isActivePlotView, getAllDrawLayersForPlot} from '../PlotViewUtil.js';
import {ImageViewerLayout}  from '../ImageViewerLayout.jsx';
import {isImage, isHiPS} from '../WebPlot.js';
import {PlotAttribute} from '../PlotAttribute.js';
import {AnnotationOps} from '../WebPlotRequest.js';
import {AREA_SELECT,LINE_SELECT,POINT} from '../../core/ExternalAccessUtils.js';
import {PlotTitle, TitleType} from './PlotTitle.jsx';
import Catalog from '../../drawingLayers/Catalog.js';
import LSSTFootprint from '../../drawingLayers/ImageLineBasedFootprint';
import {DataTypes} from '../draw/DrawLayer.js';
import {wrapResizer} from '../../ui/SizeMeConfig.js';
import {getNumFilters} from '../../tables/FilterInfo';
import {ZoomButton, ZoomType} from 'firefly/visualize/ui/ZoomButton.jsx';
import './ImageViewerDecorate.css';

const EMPTY_ARRAY=[];

const briefAnno= [
    AnnotationOps.INLINE_BRIEF,
    AnnotationOps.INLINE_BRIEF_TOOLS,
    AnnotationOps.TITLE_BAR_BRIEF,
    AnnotationOps.TITLE_BAR_BRIEF_TOOLS,
    AnnotationOps.TITLE_BAR_BRIEF_CHECK_BOX
];

const toolsAnno= [
    AnnotationOps.INLINE,
    AnnotationOps.TITLE_BAR,
    AnnotationOps.TITLE_BAR_BRIEF_TOOLS,
    AnnotationOps.INLINE_BRIEF_TOOLS
];

/**
 * todo
 * show the select and filter button show?
 * @param pv
 * @param dlAry
 * @return {boolean}
 */
function showSelect(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .some( (dl) => (dl.drawLayerTypeId === Catalog.TYPE_ID && (dl.canSelect && !dl.dataTooBigForSelection) && dl.catalog) ||
                        (dl.drawLayerTypeId === LSSTFootprint.TYPE_ID && dl.canSelect) );
}

function showFilter(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .some( (dl) => (dl.drawLayerTypeId===Catalog.TYPE_ID && dl.canFilter && dl.catalog) ||
                       (dl.drawLayerTypeId === LSSTFootprint.TYPE_ID && dl.canFilter) );
}

function showClearFilter(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .some( (dl) => {
            const filterCnt= getNumFilters(dl.tableRequest);
            return (dl.drawLayerTypeId===Catalog.TYPE_ID &&  filterCnt && dl.catalog ) ||
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
            return (dl.drawLayerTypeId===Catalog.TYPE_ID && dl.catalog) ||
                   (dl.drawLayerTypeId===LSSTFootprint.TYPE_ID);
        })
        .some( (dl) => {
                  const selectIdxs=dl.drawData[DataTypes.SELECTED_IDXS];
                  const hasIndexes= isFunction(selectIdxs) || !isEmpty(selectIdxs);
                  return (hasIndexes && dl.canSelect);
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



function contextToolbar(pv,dlAry,extensionList) {
    const plot= primePlot(pv);
    if (!plot) return;

    const showMulti= isImage(plot) ? pv.plots.length>1 : plot.cubeDepth>1;
    const hipsFits= canConvertHipsAndFits(pv);


    if (plot.attributes[PlotAttribute.SELECTION]) {
        const select= showSelect(pv,dlAry);
        const unselect= showUnselect(pv,dlAry);
        const filter= showFilter(pv,dlAry);
        const clearFilter= showClearFilter(pv,dlAry);
        const selAry= extensionList.filter( (ext) => ext.extType===AREA_SELECT);
        const extensionAry= isEmpty(selAry) ? EMPTY_ARRAY : selAry;
        return (
            <VisCtxToolbarView {...{plotView:pv, extensionAry,
                                    showSelectionTools:true, showCatSelect:select,
                                    showCatUnSelect:unselect,
                                    showFilter:filter, showClearFilter:clearFilter,
                                    showMultiImageController:showMulti}}
            />
        );
    }
    else if (plot.attributes[PlotAttribute.ACTIVE_DISTANCE]) {
        const distAry= extensionList.filter( (ext) => ext.extType===LINE_SELECT);
        if (!distAry.length && !showMulti && !hipsFits) return false;
        return (
            <VisCtxToolbarView plotView={pv} extensionAry={isEmpty(distAry)?EMPTY_ARRAY:distAry}
                               showMultiImageController={showMulti}/>
        );
    }
    else if (plot.attributes[PlotAttribute.ACTIVE_POINT]) {
        const ptAry= extensionList.filter( (ext) => ext.extType===POINT);
        if (!ptAry.length && !showMulti && !hipsFits) return false;
        return (
            <VisCtxToolbarView plotView={pv} extensionAry={isEmpty(ptAry)?EMPTY_ARRAY:ptAry}
                               showMultiImageController={showMulti}/>
        );
    }
    else if (showUnselect(pv, dlAry)) {
        return (
            <VisCtxToolbarView {...{plotView:pv, extensionAry:EMPTY_ARRAY,
                showCatUnSelect:true, showClearFilter:showClearFilter(pv,dlAry),
                showMultiImageController:showMulti}}
            />
        );
    }
    else if (showClearFilter(pv,dlAry)) {
        return (
            <VisCtxToolbarView {...{plotView:pv, extensionAry:EMPTY_ARRAY,
                showClearFilter:true,
                showMultiImageController:showMulti}}
            />
        );
    }
    else if (showMulti || hipsFits || isHiPS(plot)) {
        return (
            <VisCtxToolbarView plotView={pv} extensionAry={EMPTY_ARRAY} showMultiImageController={showMulti}/>
        );
    }
    return false;
}


const bgSlightGray= {background: 'rgba(255,255,255,.2)'};

function makeInlineRightToolbar(visRoot,pv,showDelete) {
    if (!pv) return false;
    return (
        <div style={bgSlightGray} className='iv-decorate-inline-toolbar-container'>
            <VisInlineToolbarView pv={pv} showDelete={showDelete} />
        </div>
    );
}

function getBorderColor(pv,visRoot) {
    if (!pv && !pv.plotId) return 'rgba(0,0,0,.4)';
    if (isActivePlotView(visRoot,pv.plotId)) return 'orange';
    const group= getPlotGroupById(visRoot,pv.plotGroupId);
    if (group && group.overlayColorLock) return '#005da4';
    else return 'rgba(0,0,0,.4)';
}



//===========================================================================
//---------- React Components -----------------------------------------------
//===========================================================================

const omitList= ['mousePlotId', 'size'];

function arePropsEquals(props, np) {
    if (props.size.width!==np.size.width || props.size.height!==np.size.height) return false;
    if (!shallowequal(omit(np,omitList), omit(props,omitList))) return false;
    const plotId= props.plotView?.plotId;
    if (props.mousePlotId!==np.mousePlotId && (props.mousePlotId===plotId || np.mousePlotId===plotId)) return false;
    return true;
} //todo: look at closely for optimization


function ZoomPair({pv, show}) {
    return (
        <div
            style={{
                visibility: show ? 'visible' : 'hidden',
                opacity: show ? 1 : 0,
                transition: show ? 'opacity .15s linear' : 'visibility 0s .15s, opacity .15s linear',
                background:'rgba(227, 227, 227, .8)',
                display:'inline-flex',
                borderRadius:'0 0 5px ',
                position:'absolute',
                top:16,
                left:0}}>
            <ZoomButton size={20} plotView={pv} zoomType={ZoomType.UP} horizontal={true}/>
            <ZoomButton size={20} plotView={pv} zoomType={ZoomType.DOWN} horizontal={true}/>
        </div>
    );
    
}

const ImageViewerDecorate= memo((props) => {
    const {plotView:pv,drawLayersAry,extensionList,visRoot,mousePlotId, workingIcon,
        size:{width,height}, inlineTitle=true, aboveTitle= false }= props;

    const showDelete= pv.plotViewCtx.userCanDeletePlots;
    const ctxToolbar= contextToolbar(pv,drawLayersAry,extensionList);
    const top= ctxToolbar?32:0;
    const expandedToSingle= (visRoot.expandedMode===ExpandType.SINGLE);
    const plot= primePlot(pv);
    const iWidth= Math.max(expandedToSingle ? width : width-4,0);
    const iHeight=Math.max(expandedToSingle ? height-top :height-5-top,0);

    const brief= briefAnno.includes(pv.plotViewCtx.annotationOps);
    const titleLineHeaderUI= (plot && aboveTitle) ?
                <PlotTitle brief={brief} titleType={TitleType.HEAD} plotView={pv} /> : undefined;
    const inlineTitleUI= (plot && inlineTitle) ?
                <PlotTitle brief={brief} titleType={TitleType.INLINE} plotView={pv} working={workingIcon}/> : undefined;

    const outerStyle= { width: '100%', height: '100%', overflow:'hidden', position:'relative'};

    const innerStyle= {
        width:'calc(100% - 4px)',
        bottom: 0,
        top: titleLineHeaderUI ? 20 : 0,
        overflow: 'hidden',
        position: 'absolute',
        borderStyle: 'solid',
        borderWidth: expandedToSingle ? '0 0 0 0' : '3px 2px 2px 2px',
        borderColor: getBorderColor(pv,visRoot)
    };

    if (titleLineHeaderUI) {
        outerStyle.boxShadow= 'inset 0 0 3px #000';
        outerStyle.padding= '3px';
        outerStyle.width='calc(100% - 6px)';
        outerStyle.height='calc(100% - 6px)';
        innerStyle.bottom= 2;
        innerStyle.width= 'calc(100% - 10px)';
    }

    const makeActive= () => pv?.plotId && dispatchChangeActivePlotView(pv.plotId);

    return (
        <div style={outerStyle} className='disable-select' onTouchStart={makeActive} onClick={makeActive} >
            {titleLineHeaderUI}
            <div className='image-viewer-decorate' style={innerStyle}>
                {ctxToolbar}
                <div style={{position: 'absolute', width:'100%', top, bottom:0}}>
                    <ImageViewerLayout plotView={pv} drawLayersAry={drawLayersAry}
                                       width={iWidth} height={iHeight}
                                       externalWidth={width} externalHeight={height}/>
                    {inlineTitleUI}
                    <ZoomPair pv={pv} show={mousePlotId === pv?.plotId}/>
                    {makeInlineRightToolbar(visRoot,pv,showDelete)}
                </div>
            </div>
        </div>
        );

}, arePropsEquals);


ImageViewerDecorate.propTypes= {
    plotView : PropTypes.object.isRequired,
    drawLayersAry: PropTypes.array.isRequired,
    visRoot: PropTypes.object.isRequired,
    extensionList : PropTypes.array.isRequired,
    mousePlotId : PropTypes.string,
    size : PropTypes.object.isRequired,
    workingIcon: PropTypes.bool,
    inlineTitle: PropTypes.bool,
    aboveTitle: PropTypes.bool
};

export const ImageViewerView= wrapResizer(ImageViewerDecorate);
