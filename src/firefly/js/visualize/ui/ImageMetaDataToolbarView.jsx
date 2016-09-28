/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import BrowserInfo from '../../util/BrowserInfo.js';
import {getPlotViewById, getAllDrawLayersForPlot} from '../PlotViewUtil.js';
import {dispatchChangeActivePlotView, visRoot} from '../ImagePlotCntlr.js';
import {VisInlineToolbarView} from './VisInlineToolbarView.jsx';
import {dispatchChangeLayout, getViewer, getMultiViewRoot,
        GRID, GRID_FULL, GRID_RELATED, SINGLE} from '../MultiViewCntlr.js';
import {showColorBandChooserPopup} from './ColorBandChooserPopup.jsx';
import {ImagePager} from './ImagePager.jsx';

import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import ONE from 'html/images/icons-2014/Images-One.png';
import GRID_GROUP from 'html/images/icons-2014/Images-Tiled.png';
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



export function ImageMetaDataToolbarView({activePlotId, viewerId, viewerPlotIds, layoutType, dlAry,
                                          activeTable, converterFactory, handleInlineTools=true }) {

    const {dataId,converter}= converterFactory(activeTable) || {};
    if (!converter) return <div/>;
    var nextIdx, prevIdx, leftImageStyle;
    const viewer= getViewer(getMultiViewRoot(), viewerId);
    const vr= visRoot();
    const pv= getPlotViewById(vr, activePlotId);
    const pvDlAry= getAllDrawLayersForPlot(dlAry,activePlotId,true);

    // single mode stuff
    if (layoutType===SINGLE) {
        var cIdx= viewerPlotIds.findIndex( (plotId) => plotId===activePlotId);
        if (cIdx<0) cIdx= 0;
        nextIdx= cIdx===viewerPlotIds.length-1 ? 0 : cIdx+1;
        prevIdx= cIdx ? cIdx-1 : viewerPlotIds.length-1;

        leftImageStyle= {
            cursor:'pointer',
            flex: '0 0 auto',
            paddingLeft: 10,
            visibility : viewerPlotIds.length===2 ? 'hidden' : 'visible'// hide left arrow when single mode and 2 images
        };
    }
    const showThreeColorButton= converter.threeColor && viewer.layout===GRID && viewer.layoutDetail!==GRID_FULL;
    const showPager= activeTable && viewer.layoutDetail===GRID_FULL;


    return (
        <div style={toolsStyle}>
            <div style={{whiteSpace: 'nowrap'}}>
                <ToolbarButton icon={ONE} tip={'Show single image at full size'}
                               imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                               enabled={true} visible={true}
                               horizontal={true}
                               onClick={() => dispatchChangeLayout(viewerId,SINGLE)}/>
                {converter.hasRelatedBands  &&
                            <ToolbarButton icon={GRID_GROUP} tip={'Show all as tiles'}
                                           enabled={true} visible={true} horizontal={true}
                                           imageStyle={{width:24,height:24,  paddingLeft:5, flex: '0 0 auto'}}
                                           onClick={() => dispatchChangeLayout(viewerId,'grid',GRID_RELATED)}/>
                }
                <ToolbarButton icon={FULL_GRID} tip={'Show full grid'}
                               enabled={true} visible={true} horizontal={true}
                               imageStyle={{width:24,height:24,  paddingLeft:5, flex: '0 0 auto'}}
                               onClick={() => dispatchChangeLayout(viewerId,'grid',GRID_FULL)}/>
                {showThreeColorButton &&
                             <ToolbarButton icon={THREE_COLOR} tip={'Show three color image'}
                                         enabled={true} visible={true} horizontal={true}
                                         imageStyle={{width:24,height:24,  paddingLeft:5, flex: '0 0 auto'}}
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
            {showPager && <ImagePager pageSize={10} tbl_id={activeTable.tbl_id} />}
            {handleInlineTools && <InlineRightToolbarWrapper visRoot={vr} pv={pv} dlAry={pvDlAry} />}
        </div>
    );
}

ImageMetaDataToolbarView.propTypes= {
    dlAry : PropTypes.arrayOf(React.PropTypes.object),
    activePlotId : PropTypes.string,
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string.isRequired,
    viewerPlotIds : PropTypes.arrayOf(PropTypes.string).isRequired,
    activeTable: PropTypes.object,
    converterFactory: PropTypes.func,
    handleInlineTools : PropTypes.bool
};


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


