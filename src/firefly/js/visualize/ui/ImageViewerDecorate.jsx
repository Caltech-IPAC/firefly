/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import {getPlotGroupById}  from '../PlotGroup.js';
import {visRoot, ExpandType} from '../ImagePlotCntlr.js';
import {convertZoomToString} from '../ZoomUtil.js';
import {VisCtxToolbarView} from './VisCtxToolbarView.jsx';
import PlotViewUtil from '../PlotViewUtil.js';
import {ImageViewerView}  from './ImageViewerView.jsx';
import {PlotAttribute} from '../WebPlot.js';
import {AREA_SELECT,LINE_SELECT,POINT} from '../PlotCmdExtension.js';
import './ImageViewerDecorate.css';

export default ImageViewDecorate;

const TOOLBAR_HEIGHT= 32;



function showSelectAndFilter(pv,dlAry) {
    //todo
    return false;

}
function showUnselect(pv,dlAry) {
    //todo
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
                plotView={pv} dlAry={dlAry} extensionAry={selAry}
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

function makeInlineTitle(visRoot,pv) {
    var title= visRoot.expanded!==ExpandType.SINGLE && pv && pv.primaryPlot ? pv.primaryPlot.title : '';
    if (!title) return false;
    var zl= convertZoomToString(pv.primaryPlot.zoomFactor);
    return (
        <div className='iv-decorate-inline-title-container'>
            <div className='iv-decorate-inline-title' >{title}</div>
            <div className='iv-decorate-zoom'>{zl}</div>
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


function ImageViewDecorate({plotView:pv,drawLayersAry,extensionList,visRoot}) {
    var ctxToolbar= contextToolbar(pv,drawLayersAry,extensionList);
    var top= ctxToolbar?32:0;

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
                {makeInlineTitle(visRoot,pv)}
            </div>
        </div>
    );
}


ImageViewDecorate.propTypes= {
    plotView : PropTypes.object.isRequired,
    drawLayersAry: PropTypes.array.isRequired,
    visRoot: PropTypes.object.isRequired,
    extensionList : PropTypes.array.isRequired
};



