/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component} from 'react';
import PropTypes from 'prop-types';
import shallowequal from 'shallowequal';
import {get,isEmpty,omit} from 'lodash';
import {getPlotGroupById}  from '../PlotGroup.js';
import {ExpandType, dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {VisCtxToolbarView} from './../ui/VisCtxToolbarView.jsx';
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
import {DataTypes} from '../draw/DrawLayer.js';

const EMPTY_ARRAY=[];

const titleBarAnno= [
    AnnotationOps.TITLE_BAR,
    AnnotationOps.TITLE_BAR_BRIEF,
    AnnotationOps.TITLE_BAR_BRIEF_TOOLS,
    AnnotationOps.TITLE_BAR_BRIEF_CHECK_BOX
];

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
        .some( (dl) => dl.drawLayerTypeId===Catalog.TYPE_ID && dl.canSelect && dl.catalog);
}

function showFilter(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .some( (dl) => dl.drawLayerTypeId===Catalog.TYPE_ID && dl.canFilter && dl.catalog);
}

function showClearFilter(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .some( (dl) => dl.drawLayerTypeId===Catalog.TYPE_ID &&  get(dl,'tableRequest.filters') && dl.catalog );
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
        .filter( (dl) => dl.drawLayerTypeId===Catalog.TYPE_ID && dl.catalog)
        .some( (dl) => Boolean(dl.drawData[DataTypes.SELECTED_IDXS] && dl.canSelect));
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
    if (!pv) return;
    const plot= primePlot(pv);
    if (!plot) return;

    const showMulti= isImage(plot) ? pv.plots.length>1 : plot.cubeDepth>1;

    // todo

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
        if (!distAry.length && !showMulti && isImage(plot)) return false;
        return (
            <VisCtxToolbarView plotView={pv} dlAry={dlAry} extensionAry={isEmpty(distAry)?EMPTY_ARRAY:distAry}
                               showMultiImageController={showMulti}/>
        );
    }
    else if (plot.attributes[PlotAttribute.ACTIVE_POINT]) {
        const ptAry= extensionList.filter( (ext) => ext.extType===POINT);
        if (!ptAry.length && !showMulti && isImage(plot)) return false;
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
    else if (showMulti || isHiPS(plot)) {
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
    if (!pv) return false;
    const useInlineToolbar = toolsAnno.includes(pv.plotViewCtx.annotationOps);
    const isExpanded= visRoot.expandedMode!==ExpandType.COLLAPSE;

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
    const lVis= BrowserInfo.isTouchInput() || (visRoot.apiToolsView && mousePlotId===pv.plotId);
    const exVis= BrowserInfo.isTouchInput() || mousePlotId===pv.plotId;
    const tb= !isExpanded && visRoot.apiToolsView;
    const style= (lVis || tb) && handleInlineTools ? bgFFGray : bgSlightGray;
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

    if (group && group.lockRelated) return '#005da4';
    else return 'rgba(0,0,0,.4)';
}

/**
 *
 * @param annoOps
 * @param expandedMode
 * @param pv
 * @return {*}
 */
function makeInlineTitle(annoOps, expandedMode,pv, working) {
    if (!pv || expandedMode===ExpandType.SINGLE ) return null;
    if (!annoOps || titleBarAnno.includes(annoOps)) return null;
    const brief= briefAnno.includes(annoOps);
    return (
        <PlotTitle brief={brief} titleType={TitleType.INLINE} plotView={pv} working={working}/>
    );
}

function makeTitleLineHeader(annoOps, expandedMode, pv) {
    if (!pv || expandedMode===ExpandType.SINGLE ) return null;
    if (!annoOps || !titleBarAnno.includes(annoOps)) return null;
    const brief= briefAnno.includes(annoOps);
    return (
        <PlotTitle brief={brief} titleType={TitleType.HEAD} plotView={pv}
        />
    );
}


//===========================================================================
//---------- React Components -----------------------------------------------
//===========================================================================

export class ImageViewerDecorate extends Component {
    constructor(props) {
        super(props);
        this.makeActive= this.makeActive.bind(this);
    }

    shouldComponentUpdate(np,ns) {
        const {props:p}= this;
        const omitList= ['mousePlotId'];
        const update= !shallowequal(omit(np,omitList), omit(p,omitList) );
        if (update) return true;

        const plotId= get(p.plotView, 'plotId');
        if (p.mousePlotId!==np.mousePlotId && (p.mousePlotId===plotId || np.mousePlotId===plotId)) return true;


        return false;


    } //todo: look at closely for optimization



    makeActive(ev) {
        const plotId= get(this.props,'plotView.plotId');
        if (plotId) dispatchChangeActivePlotView(plotId);
    }

    render() {
        const {plotView:pv,drawLayersAry,extensionList,visRoot,mousePlotId,
               handleInlineTools,taskCount,width,height}= this.props;

        if (!width || !height) return false;

        const showDelete= pv.plotViewCtx.userCanDeletePlots;
        const ctxToolbar= contextToolbar(pv,drawLayersAry,extensionList);
        const top= ctxToolbar?32:0;
        let title;
        let titleLineHeader= null;
        let inlineTitle= null;
        const {expandedMode}= visRoot;
        const expandedToSingle= (expandedMode===ExpandType.SINGLE);
        const iWidth= expandedToSingle ? width : width-4;
        const iHeight=expandedToSingle ? height-top :height-5-top;
        const plot= primePlot(pv);

        if (plot) {
            titleLineHeader= makeTitleLineHeader(pv.plotViewCtx.annotationOps,expandedMode, pv);
            inlineTitle= makeInlineTitle(pv.plotViewCtx.annotationOps,expandedMode, pv, taskCount>0);
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
            top: titleLineHeader ? 20 : 0,
            overflow: 'hidden',
            position: 'absolute',
            borderStyle: 'solid',
            borderWidth: expandedToSingle ? '0 0 0 0' : '3px 2px 2px 2px',
            borderColor: getBorderColor(pv,visRoot)
        };

        if (titleLineHeader) {
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
                {titleLineHeader}
                <div className='image-viewer-decorate' style={innerStyle}>
                    {ctxToolbar}
                    <div style={{position: 'absolute', width:'100%', top, bottom:0}}>
                        <ImageViewerLayout plotView={pv} drawLayersAry={drawLayersAry}
                                                      width={iWidth} height={iHeight}
                                                      externalWidth={width} externalHeight={height}/>
                        {inlineTitle}
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
    width : PropTypes.number.isRequired,
    height : PropTypes.number.isRequired,
    handleInlineTools : PropTypes.bool,
    taskCount: PropTypes.number
};


ImageViewerDecorate.defaultProps = {
    handleInlineTools : true,
    showDelete : false
};
