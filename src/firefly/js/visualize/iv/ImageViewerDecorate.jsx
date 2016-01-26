/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import numeral from 'numeral';
import {getPlotGroupById}  from '../PlotGroup.js';
import {RotateType}  from '../PlotState.js';
import {visRoot, ExpandType} from '../ImagePlotCntlr.js';
import {convertZoomToString} from '../ZoomUtil.js';
import {VisCtxToolbarView} from './../ui/VisCtxToolbarView.jsx';
import {VisInlineToolbarView} from './../ui/VisInlineToolbarView.jsx';
import PlotViewUtil from '../PlotViewUtil.js';
import {ImageViewerView}  from './ImageViewerView.jsx';
import {PlotAttribute} from '../WebPlot.js';
import {AnnotationOps} from '../WebPlotRequest.js';
import BrowserInfo from '../../util/BrowserInfo.js';
import {AREA_SELECT,LINE_SELECT,POINT} from '../PlotCmdExtension.js';
import {getTaskCount} from '../../core/AppDataCntlr.js';
import './ImageViewerDecorate.css';
import LOADING from 'html/images/gxt/loading.gif';

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
    var {primaryPlot:plot}= pv;
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

    if (!useInlineToolbar || visRoot.expanded!==ExpandType.COLLAPSE) return false;
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

function makeInlineTitle(annoOps, expandMode,titleStr, zoomFactor, plotState, plotId) {
    if (!plotState || !titleStr || expandMode===ExpandType.SINGLE ) return null;
    if (!annoOps || titleBarAnno.includes(annoOps)) return null;
    return (
        <Title annotationOps={annoOps} titleStr={titleStr}
               inline={true}
               zoomFactor={zoomFactor}
               plotState={plotState} plotId={plotId}
        />
    );
}

function makeTitleLineHeader(annoOps, expandMode,titleStr, zoomFactor, plotState, plotId) {
    if (!plotState || !titleStr || expandMode===ExpandType.SINGLE ) return null;
    if (!annoOps || !titleBarAnno.includes(annoOps)) return null;
    return (
        <Title annotationOps={annoOps} titleStr={titleStr}
               inline={false}
               zoomFactor={zoomFactor}
               plotState={plotState} plotId={plotId}
        />
    );
}


//===========================================================================
//---------- React Components -----------------------------------------------
//===========================================================================

export function ImageViewerDecorate({plotView:pv,drawLayersAry,extensionList,visRoot,mousePlotId}) {
    var ctxToolbar= contextToolbar(pv,drawLayersAry,extensionList);
    var top= ctxToolbar?32:0;
    var title, zoomFactor;
    var titleLineHeader= null;
    var inlineTitle= null;
    var plotId= null;
    var plotState= null;
    var {expanded}= visRoot;

    if (pv && pv.primaryPlot) {
        title= pv && pv.primaryPlot ? pv.primaryPlot.title : '';
        zoomFactor= pv.primaryPlot.zoomFactor;
        plotState= pv.primaryPlot.plotState;
        plotId= pv.plotId;
        titleLineHeader= makeTitleLineHeader(pv.options.annotationOps,expanded, title, zoomFactor,plotState,plotId);
        inlineTitle= makeInlineTitle(pv.options.annotationOps,expanded, title, zoomFactor,plotState,plotId);

    }

    var outerStyle= {
        width: '100%',
        height: '100%',
        overflow:'hidden',
        position:'relative'
    };


    var innerStyle= {
        width:'calc(100% - 4px)',
        bottom: 0,
        top: titleLineHeader ? 20 : 0,
        overflow: 'hidden',
        position: 'absolute',
        borderStyle: 'solid',
        borderWidth: '3px 2px 2px 2px',
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
        <div style={outerStyle}>
            {titleLineHeader}
            <div className='image-viewer-decorate' style={innerStyle}>
                {ctxToolbar}
                <div style={{position: 'absolute', width:'100%', top, bottom:0}}>
                    <ImageViewerView plotView={pv} drawLayersAry={drawLayersAry}/>
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
    mousePlotId : PropTypes.string
};


function Title({plotId, inline, annotationOps:annoOps,titleStr,zoomFactor,plotState}) {
    var brief= briefAnno.includes(annoOps);
    var styleName= inline ? 'iv-decorate-inline-title-container' : 'iv-decorate-header-title-container';
    var zlStr= convertZoomToString(zoomFactor);
    var rotString= null;
    if (plotState.isRotated()) {
        if (plotState.getRotateType()===RotateType.NORTH) {
            rotString= 'North';
        } else {
            var angleStr= numeral(plotState.getRotationAngle()).format('#');
            rotString= angleStr + String.fromCharCode(176);
        }
        zlStr+=',';
    }
    var showWorking= getTaskCount(plotId);

    return (
        <div className={styleName}>
            <div className='iv-decorate-title' >{titleStr}</div>
            {!brief ? <div className='iv-decorate-zoom'>{zlStr}</div> : ''}
            {!brief && rotString ? <div className='iv-decorate-rotation'>{rotString}</div> : ''}
            {showWorking ?<img style={{width:14,height:14,padding:'0 3px 0 5px'}} src={LOADING}/> : ''}
        </div>
    );
}

Title.propTypes= {
    plotId: PropTypes.string,
    inline: PropTypes.bool.isRequired,
    titleStr: PropTypes.string,
    zoomFactor:PropTypes.number,
    annotationOps : PropTypes.object,
    plotState : PropTypes.object
};


