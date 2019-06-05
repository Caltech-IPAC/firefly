/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component} from 'react';
import PropTypes from 'prop-types';
import shallowequal from 'shallowequal';
import {get,isEmpty,omit} from 'lodash';
import {getPlotGroupById}  from '../PlotGroup.js';
import {ExpandType, dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {VisCtxToolbarView, canConvertHipsAndFits} from './../ui/VisCtxToolbarView.jsx';
import {VisInlineToolbarView} from './../ui/VisInlineToolbarView.jsx';
import {primePlot, isActivePlotView, getAllDrawLayersForPlot} from '../PlotViewUtil.js';
import {ImageViewerLayout}  from '../ImageViewerLayout.jsx';
import {PlotAttribute, isImage, isHiPS} from '../WebPlot.js';
import {AnnotationOps} from '../WebPlotRequest.js';
import BrowserInfo from '../../util/BrowserInfo.js';
import {AREA_SELECT,LINE_SELECT,POINT} from '../../core/ExternalAccessUtils.js';
import {PlotTitle, TitleType} from './PlotTitle.jsx';
import './ImageViewerDecorate.css';
import Catalog from '../../drawingLayers/Catalog.js';
import LSSTFootprint from '../../drawingLayers/ImageLineBasedFootprint';
import {DataTypes} from '../draw/DrawLayer.js';
import {wrapResizer} from '../../ui/SizeMeConfig.js';

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
        .some( (dl) => (dl.drawLayerTypeId === Catalog.TYPE_ID && dl.canSelect && dl.catalog) ||
                        (dl.drawLayerTypeId === LSSTFootprint.TYPE_ID && dl.canSelect) );
}

function showFilter(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .some( (dl) => (dl.drawLayerTypeId===Catalog.TYPE_ID && dl.canFilter && dl.catalog) ||
                       (dl.drawLayerTypeId === LSSTFootprint.TYPE_ID && dl.canFilter) );
}

function showClearFilter(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .some( (dl) => (dl.drawLayerTypeId===Catalog.TYPE_ID &&  get(dl,'tableRequest.filters') && dl.catalog ) ||
                        (dl.drawLayerTypeId === LSSTFootprint.TYPE_ID && get(dl, 'tableRequest.filters')));
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
                  return (Boolean(dl.drawData[DataTypes.SELECTED_IDXS] && dl.canSelect) ||
                          Boolean(!isEmpty(dl.selectRowIdxs) && dl.canSelect));
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
            <VisCtxToolbarView {...{plotView:pv, dlAry,  extensionAry,
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
            <VisCtxToolbarView plotView={pv} dlAry={dlAry} extensionAry={isEmpty(distAry)?EMPTY_ARRAY:distAry}
                               showMultiImageController={showMulti}/>
        );
    }
    else if (plot.attributes[PlotAttribute.ACTIVE_POINT]) {
        const ptAry= extensionList.filter( (ext) => ext.extType===POINT);
        if (!ptAry.length && !showMulti && !hipsFits) return false;
        return (
            <VisCtxToolbarView plotView={pv} dlAry={dlAry} extensionAry={isEmpty(ptAry)?EMPTY_ARRAY:ptAry}
                               showMultiImageController={showMulti}/>
        );
    }
    else if (showUnselect(pv, dlAry)) {
        return (
            <VisCtxToolbarView {...{plotView:pv, dlAry,  extensionAry:EMPTY_ARRAY,
                showCatUnSelect:true,
                showMultiImageController:showMulti}}
            />
        );
    }
    else if (showClearFilter(pv,dlAry)) {
        return (
            <VisCtxToolbarView {...{plotView:pv, dlAry,  extensionAry:EMPTY_ARRAY,
                showClearFilter:true,
                showMultiImageController:showMulti}}
            />
        );
    }
    else if (showMulti || hipsFits || isHiPS(plot)) {
        return (
            <VisCtxToolbarView plotView={pv} dlAry={dlAry} extensionAry={EMPTY_ARRAY}
                               showMultiImageController={showMulti}/>
        );
    }
    return false;
}


const bgSlightGray= {background: 'rgba(255,255,255,.2)'};
const bgFFGray= {background: '#e3e3e3'};

function makeInlineRightToolbar(visRoot,pv,dlAry,mousePlotId, handleInlineTools, showDelete) {
    const useInlineToolbar = toolsAnno.includes(pv.plotViewCtx.annotationOps);
    const isExpanded= visRoot.expandedMode!==ExpandType.COLLAPSE;
    const tb= !isExpanded && visRoot.useFloatToolbar;
    const lVis= BrowserInfo.isTouchInput() || (visRoot.useFloatToolbar && pv && mousePlotId===pv.plotId);
    const style= (lVis || tb) && handleInlineTools ? bgFFGray : bgSlightGray;
    if (!pv && tb && handleInlineTools) {
        return (
            <div style={style} className='iv-decorate-inline-toolbar-container'>
                <VisInlineToolbarView pv={pv} dlAry={undefined} showLayer={false}
                                      showExpand={false} showToolbarButton={true} showDelete ={false} />
            </div>
        );

    }
    if (!pv) return false;


    if (!useInlineToolbar) return false;
    if (isExpanded) {
        if (showDelete) {
            return (

                <div className='iv-decorate-inline-toolbar-container'>
                    <VisInlineToolbarView
                        pv={pv} dlAry={dlAry}
                        showLayer={false} showExpand={false} showToolbarButton={false} showDelete ={true} />
                </div>
            );
        }
        else {
            return false;
        }
    }
    const exVis= BrowserInfo.isTouchInput() || mousePlotId===pv.plotId;
    return (
        <div style={style} className='iv-decorate-inline-toolbar-container'>
            <VisInlineToolbarView
                pv={pv} dlAry={dlAry}
                showLayer={lVis && handleInlineTools}
                showExpand={exVis && handleInlineTools}
                showToolbarButton={tb && handleInlineTools}
                showDelete ={showDelete}
                />
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

/**
 *
 * @param pv
 * @param brief
 * @param workingIcon
 * @return {*}
 */
function makeInlineTitle(pv, brief, workingIcon) {
    return (
        <PlotTitle brief={brief} titleType={TitleType.INLINE} plotView={pv} working={workingIcon}/>
    );
}

/**
 *
 * @param pv
 * @param brief
 * @return {*}
 */
function makeTitleLineHeader(pv, brief) {
    return (
        <PlotTitle brief={brief} titleType={TitleType.HEAD} plotView={pv} />
    );
}


//===========================================================================
//---------- React Components -----------------------------------------------
//===========================================================================

class ImageViewerDecorate extends Component {
    constructor(props) {
        super(props);
        this.makeActive= this.makeActive.bind(this);
    }

    shouldComponentUpdate(np) {
        const {props:p}= this;
        const omitList= ['mousePlotId'];
        const update= !shallowequal(omit(np,omitList), omit(p,omitList) );
        if (update) return true;

        const plotId= get(p.plotView, 'plotId');
        if (p.mousePlotId!==np.mousePlotId && (p.mousePlotId===plotId || np.mousePlotId===plotId)) return true;


        return false;


    } //todo: look at closely for optimization



    makeActive() {
        const plotId= get(this.props,'plotView.plotId');
        if (plotId) dispatchChangeActivePlotView(plotId);
    }

    render() {
        const {plotView:pv,drawLayersAry,extensionList,visRoot,mousePlotId,
               handleInlineTools,workingIcon, size:{width,height},
               inlineTitle=true, aboveTitle= false }= this.props;

        const showDelete= pv.plotViewCtx.userCanDeletePlots;
        const ctxToolbar= contextToolbar(pv,drawLayersAry,extensionList);
        const top= ctxToolbar?32:0;
        let titleLineHeaderUI= null;
        let inlineTitleUI= null;
        const {expandedMode}= visRoot;
        const expandedToSingle= (expandedMode===ExpandType.SINGLE);
        let iWidth= expandedToSingle ? width : width-4;
        let iHeight=expandedToSingle ? height-top :height-5-top;
        const plot= primePlot(pv);
        if (iWidth<0) iWidth= 0;
        if (iHeight<0) iHeight= 0;

        if (plot) {
            const brief= briefAnno.includes(pv.plotViewCtx.annotationOps);
            titleLineHeaderUI= aboveTitle && makeTitleLineHeader(pv, brief);
            inlineTitleUI= inlineTitle && makeInlineTitle(pv, brief, workingIcon);
        }

        const outerStyle= {
            width: '100%',
            height: '100%',
            overflow:'hidden',
            position:'relative',
        };


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


        return (
            <div style={outerStyle} className='disable-select'
                 onTouchStart={this.makeActive}
                 onClick={this.makeActive} >
                {titleLineHeaderUI}
                <div className='image-viewer-decorate' style={innerStyle}>
                    {ctxToolbar}
                    <div style={{position: 'absolute', width:'100%', top, bottom:0}}>
                        <ImageViewerLayout plotView={pv} drawLayersAry={drawLayersAry}
                                                      width={iWidth} height={iHeight}
                                                      externalWidth={width} externalHeight={height}/>
                        {inlineTitleUI}
                        {makeInlineRightToolbar(visRoot,pv,drawLayersAry,mousePlotId,handleInlineTools,showDelete)}
                    </div>
                </div>
            </div>
        );

    }

}

ImageViewerDecorate.propTypes= {
    plotView : PropTypes.object.isRequired,
    drawLayersAry: PropTypes.array.isRequired,
    visRoot: PropTypes.object.isRequired,
    extensionList : PropTypes.array.isRequired,
    mousePlotId : PropTypes.string,
    size : PropTypes.object.isRequired,
    handleInlineTools : PropTypes.bool,
    workingIcon: PropTypes.bool,
    inlineTitle: PropTypes.bool,
    aboveTitle: PropTypes.bool
};


ImageViewerDecorate.defaultProps = {
    handleInlineTools : true,
    showDelete : false
};


export const ImageViewerView= wrapResizer(ImageViewerDecorate);

