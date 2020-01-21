/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import PropTypes from 'prop-types';
import BrowserInfo from '../../util/BrowserInfo.js';
import {getPlotViewById, getAllDrawLayersForPlot} from '../PlotViewUtil.js';
import {dispatchChangeActivePlotView, visRoot} from '../ImagePlotCntlr.js';
import {VisInlineToolbarView} from './VisInlineToolbarView.jsx';
import {dispatchChangeViewerLayout, getViewer, getMultiViewRoot,
        GRID_FULL, GRID_RELATED, SINGLE, GRID} from '../MultiViewCntlr.js';
import {showColorBandChooserPopup} from './ColorBandChooserPopup.jsx';
import {ImagePager} from './ImagePager.jsx';

import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import ONE from 'html/images/icons-2014/Images-One.png';
import GRID_GROUP from 'html/images/icons-2014/Images-plus-related.png';
import FULL_GRID from 'html/images/icons-2014/Images-Tiled-full.png';
import PAGE_RIGHT from 'html/images/icons-2014/20x20_PageRight.png';
import PAGE_LEFT from 'html/images/icons-2014/20x20_PageLeft.png';
import THREE_COLOR from 'html/images/icons-2014/28x28_FITS_Modify3Image.png';




const toolsStyle= {
    display:'flex',
    flexDirection:'row',
    flexWrap:'nowrap',
    alignItems: 'center',
    justifyContent:'space-between',
    height: 30
};


export function ImageMetaDataToolbarView({activePlotId, viewerId, viewerPlotIds=[], layoutType, dlAry,
                                          activeTable, makeDataProductsConverter, handleInlineTools=true, makeDropDown}) {

    const converter= makeDataProductsConverter(activeTable) || {};
    if (!converter) {
        return <div/>;
    }
    const dataId= converter.converterId;
    var nextIdx, prevIdx, leftImageStyle;
    const viewer= getViewer(getMultiViewRoot(), viewerId);
    const vr= visRoot();
    const pv= getPlotViewById(vr, activePlotId);
    const pvDlAry= getAllDrawLayersForPlot(dlAry,activePlotId,true);
    let   cIdx = viewerPlotIds.findIndex( (plotId) => plotId===activePlotId);
    if (cIdx<0) cIdx= 0;

    // single mode stuff
    if (layoutType===SINGLE) {
        nextIdx= cIdx===viewerPlotIds.length-1 ? 0 : cIdx+1;
        prevIdx= cIdx ? cIdx-1 : viewerPlotIds.length-1;

        leftImageStyle= {
            cursor:'pointer',
            flex: '0 0 auto',
            paddingLeft: 10,
            visibility : viewerPlotIds.length===2 ? 'hidden' : 'visible'// hide left arrow when single mode and 2 images
        };
    }
    const showThreeColorButton= converter.threeColor && viewer.layoutDetail!==GRID_FULL && !(viewerPlotIds[0].includes(GRID_FULL.toLowerCase()));
    const showPager= activeTable && converter.canGrid && layoutType===GRID && viewer.layoutDetail===GRID_FULL;
    const showMultiImageOps= converter.canGrid || converter.hasRelatedBands;



    return (
        <div style={toolsStyle}>
            {makeDropDown && makeDropDown()}
            <div style={{whiteSpace: 'nowrap'}}>
                {showMultiImageOps && <ToolbarButton icon={ONE} tip={'Show single image at full size'}
                               imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                               enabled={true} visible={true}
                               horizontal={true}
                               onClick={() => handleViewerLayout(viewerId,SINGLE)}/>}

                {converter.canGrid && <ToolbarButton icon={FULL_GRID} tip={'Show full grid'}
                               enabled={true} visible={true} horizontal={true}
                               imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                               additionalStyle={{marginLeft: 5}}
                               onClick={() => handleViewerLayout(viewerId,'grid',GRID_FULL)}/>}

                {converter.hasRelatedBands  &&
                            <ToolbarButton icon={GRID_GROUP} tip={'Show all as tiles'}
                               enabled={true} visible={true} horizontal={true}
                               imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                               additionalStyle={{marginLeft: 20}}
                               onClick={() => dispatchChangeViewerLayout(viewerId,'grid',GRID_RELATED)}/>
                }
                {showThreeColorButton &&
                             <ToolbarButton icon={THREE_COLOR} tip={'Show three color image'}
                                         enabled={true} visible={true} horizontal={true}
                                         imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                                         additionalStyle={{marginLeft: 5}}
                                         onClick={() => showThreeColorOps(viewer,dataId)}/>
                }
                {layoutType===SINGLE && viewerPlotIds.length>1 &&
                            <img style={leftImageStyle} src={PAGE_LEFT}
                                 onClick={() => dispatchChangeActivePlotView(viewerPlotIds[prevIdx])} />
                }
                {layoutType===SINGLE && viewerPlotIds.length>1 &&
                            <img style={{cursor:'pointer', paddingLeft:5, flex: '0 0 auto'}}
                                 src={PAGE_RIGHT}
                                 onClick={() => dispatchChangeActivePlotView(viewerPlotIds[nextIdx])} />
                }
            </div>
            {showPager && <ImagePager pageSize={converter.maxPlots} tbl_id={activeTable.tbl_id} />}
            {handleInlineTools && <InlineRightToolbarWrapper visRoot={vr} pv={pv} dlAry={pvDlAry} />}
        </div>
    );
}

ImageMetaDataToolbarView.propTypes= {
    dlAry : PropTypes.arrayOf(PropTypes.object),
    activePlotId : PropTypes.string,
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string.isRequired,
    viewerPlotIds : PropTypes.arrayOf(PropTypes.string).isRequired,
    activeTable: PropTypes.object,
    makeDataProductsConverter: PropTypes.func,
    handleInlineTools : PropTypes.bool,
    makeDropDown: PropTypes.func
};

function handleViewerLayout(viewerId, grid, gridLayout) {
    dispatchChangeViewerLayout(viewerId, grid, gridLayout);
    // add the dispatchZoom in metaDataWatcher on ImagePlotCntlr.UPDATE_VIEW_SIZE per viewer size change
}

function InlineRightToolbarWrapper({visRoot,pv,dlAry}){
    if (!pv) return <div></div>;

    var lVis= BrowserInfo.isTouchInput() || visRoot.apiToolsView;
    var tb= visRoot.apiToolsView;
    return (
        <div>
            <VisInlineToolbarView
                pv={pv} dlAry={dlAry}
                showLayer={lVis}
                showExpand={true}
                showToolbarButton={tb}
                showDelete ={false}
            />
        </div>
    );
}

InlineRightToolbarWrapper.propTypes= {
    visRoot: PropTypes.object,
    pv : PropTypes.object,
    dlAry : PropTypes.array
};


function showThreeColorOps(viewer,dataId) {
    if (!viewer) return;
    const newCustom= Object.assign({}, viewer.customData[dataId], {threeColorVisible:true});
    showColorBandChooserPopup(viewer.viewerId,newCustom,dataId);
}


