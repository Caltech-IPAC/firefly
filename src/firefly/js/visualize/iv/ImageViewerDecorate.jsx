/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import {getPlotGroupById}  from '../PlotGroup.js';
import {visRoot, ExpandType} from '../ImagePlotCntlr.js';
import {convertZoomToString} from '../ZoomUtil.js';
import {VisCtxToolbarView} from './../ui/VisCtxToolbarView.jsx';
import {VisInlineToolbarView} from './../ui/VisInlineToolbarView.jsx';
import PlotViewUtil from '../PlotViewUtil.js';
import {ImageViewerView}  from './ImageViewerView.jsx';
import {PlotAttribute} from '../WebPlot.js';
import {AREA_SELECT,LINE_SELECT,POINT} from '../PlotCmdExtension.js';
import './ImageViewerDecorate.css';


const TOOLBAR_HEIGHT= 32;


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



//function makeTitleBarTitle(visRoot,pv) {
//
//}
//
//function makeInlineTitle(visRoot,pv) {
//    //todo handle showing rotation
//    //todo check pv.hideTitleDetail
//    //todo turn pv.useInlineToolbar to something that controls both title location ad toolbar location
//    var title= visRoot.expanded!==ExpandType.SINGLE && pv && pv.primaryPlot ? pv.primaryPlot.title : '';
//    if (!title) return false;
//    var zl= convertZoomToString(pv.primaryPlot.zoomFactor);
//    return (
//        <div className='iv-decorate-inline-title-container'>
//            <div className='iv-decorate-inline-title' >{title}</div>
//            <div className='iv-decorate-zoom'>{zl}</div>
//        </div>
//    );
//}

function makeInlineRightToolbar(visRoot,pv,dlAry) {
    if (!pv || !pv.options.useInlineToolbar || visRoot.expanded!==ExpandType.COLLAPSE) return false;
    return (
        <div className='iv-decorate-inline-toolbar-container'>
            <VisInlineToolbarView
                plotView={pv} dlAry={dlAry}
                showLayer={false}
                showExpand={true}
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


export function ImageViewerDecorate({plotView:pv,drawLayersAry,extensionList,visRoot}) {
    var ctxToolbar= contextToolbar(pv,drawLayersAry,extensionList);
    var top= ctxToolbar?32:0;
    var title, zoomFactor;

    if (pv && pv.primaryPlot) {
        title= pv && pv.primaryPlot ? pv.primaryPlot.title : '';
        zoomFactor= pv.primaryPlot.zoomFactor;
    }

    var style= {width:'100%',
                height:'100%',
                overflow:'hidden',
                position:'relative',
                borderStyle: 'solid',
                borderWidth: '3px 2px 2px 2px',
                borderColor: getBorderColor(pv,visRoot)
    };


    return (
        <div className='image-viewer-decorate' style={style}>
            {ctxToolbar}
            <div style={{position: 'absolute', width:'100%', top, bottom:0}}>
                <ImageViewerView plotView={pv} drawLayersAry={drawLayersAry}/>
                <InlineTitle expandMode={visRoot.expanded} titleStr={title} zoomFactor={zoomFactor}/>
                {makeInlineRightToolbar(visRoot,pv,drawLayersAry)}
            </div>
        </div>
    );
}

//{makeInlineTitle(visRoot,pv)}

ImageViewerDecorate.propTypes= {
    plotView : PropTypes.object.isRequired,
    drawLayersAry: PropTypes.array.isRequired,
    visRoot: PropTypes.object.isRequired,
    extensionList : PropTypes.array.isRequired
};



function InlineTitle({expandMode,titleStr, zoomFactor}) {
    //todo handle showing rotation
    //todo check pv.hideTitleDetail
    //todo turn pv.useInlineToolbar to something that controls both title location ad toolbar location
    //todo maybe make a title bar mode to determine how much to show
    if (!titleStr || expandMode===ExpandType.SINGLE ) return <div></div>;
    var zlStr= convertZoomToString(zoomFactor);
    return (
        <div className='iv-decorate-inline-title-container'>
            <div className='iv-decorate-inline-title' >{titleStr}</div>
            <div className='iv-decorate-zoom'>{zlStr}</div>
        </div>
    );
}


InlineTitle.propTypes= {
    expandMode:PropTypes.object,
    titleStr: PropTypes.string,
    zoomFactor:PropTypes.number
};


const EMPTY_DIV= <div></div>;

