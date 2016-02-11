/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import {getPlotGroupById}  from '../PlotGroup.js';
import {ExpandType} from '../ImagePlotCntlr.js';
import {VisCtxToolbarView} from './../ui/VisCtxToolbarView.jsx';
import {VisInlineToolbarView} from './../ui/VisInlineToolbarView.jsx';
import PlotViewUtil, {primePlot} from '../PlotViewUtil.js';
import {ImageViewerLayout}  from './ImageViewerLayout.jsx';
import {PlotAttribute} from '../WebPlot.js';
import {AnnotationOps} from '../WebPlotRequest.js';
import BrowserInfo from '../../util/BrowserInfo.js';
import {AREA_SELECT,LINE_SELECT,POINT} from '../PlotCmdExtension.js';
import {PlotTitle, TitleType} from './PlotTitle.jsx';
import './ImageViewerDecorate.css';

const TOOLBAR_HEIGHT= 32;

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
function showSelectAndFilter(pv,dlAry) {
    return false;
}

/**
 * todo
 * show the unselect button?
 * @param pv
 * @param dlAry
 * @return {boolean}
 */
function showUnselect(pv,dlAry) {
    return false;
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
    var plot= primePlot(pv);
    if (!plot) return;

    if (plot.attributes[PlotAttribute.SELECTION]) {
        var selectAndFilter= showSelectAndFilter(pv,dlAry);
        const selAry= extensionList.filter( (ext) => ext.extType===AREA_SELECT);
        return (
            <VisCtxToolbarView
                plotView={pv} dlAry={dlAry} extensionAry={selAry.length?selAry:null}
                showCrop={true} showStats={true} showSelect={selectAndFilter}
                showUnSelect={showUnselect(pv,dlAry)} showFilter={selectAndFilter}
            />
        );
    }
    else if (plot.attributes[PlotAttribute.ACTIVE_DISTANCE]) {
        var distAry= extensionList.filter( (ext) => ext.extType===LINE_SELECT);
        if (!distAry.length) return false;
        return (
            <VisCtxToolbarView
                plotView={pv} dlAry={dlAry} extensionAry={distAry}
                showCrop={false} showStats={false} showSelect={false}
                showUnSelect={false} showFilter={false}
            />
        );
    }
    else if (plot.attributes[PlotAttribute.ACTIVE_POINT]) {
        const ptAry= extensionList.filter( (ext) => ext.extType===POINT);
        if (!ptAry.length) return false;
        return (
            <VisCtxToolbarView
                plotView={pv} dlAry={dlAry} extensionAry={ptAry}
                showCrop={false} showStats={false} showSelect={false}
                showUnSelect={false} showFilter={false}
            />
        );
    }
    return false;
}




function makeInlineRightToolbar(visRoot,pv,dlAry,mousePlotId) {
    if (!pv) return false;
    var useInlineToolbar = toolsAnno.includes(pv.options.annotationOps);

    if (!useInlineToolbar || visRoot.expandedMode!==ExpandType.COLLAPSE) return false;
    var lVis= BrowserInfo.isTouchInput() || (visRoot.toolBarIsPopup && mousePlotId===pv.plotId);
    var exVis= BrowserInfo.isTouchInput() || mousePlotId===pv.plotId;
    return (
        <div className='iv-decorate-inline-toolbar-container'>
            <VisInlineToolbarView
                plotView={pv} dlAry={dlAry}
                showLayer={lVis}
                showExpand={exVis}
                showDelete ={true}
                />
        </div>
    );



}

function getBorderColor(pv,visRoot) {
    if (!pv && !pv.plotId) return 'rgba(0,0,0,.4)';

    if (PlotViewUtil.isActivePlotView(visRoot,pv.plotId)) return 'orange';

    var group= getPlotGroupById(visRoot,pv.plotGroupId);

    if (group && group.lockRelated) return '#005da4';
    else return 'rgba(0,0,0,.4)';
}

function makeInlineTitle(annoOps, expandedMode,titleStr, zoomFactor, plotState, plotId) {
    if (!plotState || !titleStr || expandedMode===ExpandType.SINGLE ) return null;
    if (!annoOps || titleBarAnno.includes(annoOps)) return null;
    var brief= briefAnno.includes(annoOps);
    return (
        <PlotTitle brief={brief} titleStr={titleStr}
               titleType={TitleType.INLINE}
               zoomFactor={zoomFactor}
               plotState={plotState} plotId={plotId}
        />
    );
}

function makeTitleLineHeader(annoOps, expandedMode,titleStr, zoomFactor, plotState, plotId) {
    if (!plotState || !titleStr || expandedMode===ExpandType.SINGLE ) return null;
    if (!annoOps || !titleBarAnno.includes(annoOps)) return null;
    var brief= briefAnno.includes(annoOps);
    return (
        <PlotTitle brief={brief} titleStr={titleStr}
               titleType={TitleType.HEAD}
               zoomFactor={zoomFactor}
               plotState={plotState} plotId={plotId}
        />
    );
}


//===========================================================================
//---------- React Components -----------------------------------------------
//===========================================================================

export function ImageViewerDecorate({plotView:pv,drawLayersAry,extensionList,visRoot,mousePlotId,width,height}) {

    if (!width || !height) return <div></div>;

    const ctxToolbar= contextToolbar(pv,drawLayersAry,extensionList);
    const top= ctxToolbar?32:0;
    var title, zoomFactor;
    var titleLineHeader= null;
    var inlineTitle= null;
    var plotId= null;
    var plotState= null;
    const {expandedMode}= visRoot;
    const expandedToSingle= (expandedMode===ExpandType.SINGLE);
    const iWidth= expandedToSingle ? width : width-4;
    const iHeight=expandedToSingle ? height-top :height-5-top;
    const plot= primePlot(pv);

    if (plot) {
        title= plot ? plot.title : '';
        zoomFactor= plot.zoomFactor;
        plotState= plot.plotState;
        plotId= plot.plotId;
        titleLineHeader= makeTitleLineHeader(pv.options.annotationOps,expandedMode, title, zoomFactor,plotState,plotId);
        inlineTitle= makeInlineTitle(pv.options.annotationOps,expandedMode, title, zoomFactor,plotState,plotId);

    }

    var outerStyle= {
        width: '100%',
        height: '100%',
        overflow:'hidden',
        position:'relative',
    };


    var innerStyle= {
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
        <div style={outerStyle} className='disable-select'>
            {titleLineHeader}
            <div className='image-viewer-decorate' style={innerStyle}>
                {ctxToolbar}
                <div style={{position: 'absolute', width:'100%', top, bottom:0}}>
                    <ImageViewerLayout plotView={pv} drawLayersAry={drawLayersAry}
                                       width={iWidth} height={iHeight}
                                       externalWidth={width} externalHeight={height}/>
                    {inlineTitle}
                    {makeInlineRightToolbar(visRoot,pv,drawLayersAry,mousePlotId)}
                </div>
            </div>
        </div>
    );
}

//{makeInlineTitle(visRoot,pv)}

ImageViewerDecorate.propTypes= {
    plotView : PropTypes.object.isRequired,
    drawLayersAry: PropTypes.array.isRequired,
    visRoot: PropTypes.object.isRequired,
    extensionList : PropTypes.array.isRequired,
    mousePlotId : PropTypes.string,
    width : PropTypes.number.isRequired,
    height : PropTypes.number.isRequired
};

