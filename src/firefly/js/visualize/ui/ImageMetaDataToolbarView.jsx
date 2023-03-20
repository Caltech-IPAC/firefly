/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import PropTypes from 'prop-types';
import {
    dispatchChangeViewerLayout, getViewer, getMultiViewRoot,
    GRID_FULL, GRID_RELATED, SINGLE, GRID, getLayoutDetails
} from '../MultiViewCntlr.js';
import {showColorBandChooserPopup} from './ColorBandChooserPopup.jsx';
import {ImagePager} from './ImagePager.jsx';
import {VisMiniToolbar} from 'firefly/visualize/ui/VisMiniToolbar.jsx';

import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import ONE from 'html/images/icons-2014/Images-One.png';
import GRID_GROUP from 'html/images/icons-2014/Images-plus-related.png';
import FULL_GRID from 'html/images/icons-2014/Images-Tiled-full.png';
import THREE_COLOR from 'html/images/icons-2014/28x28_FITS_Modify3Image.png';




const toolsStyle= {
    display:'flex',
    flexDirection:'row',
    flexWrap:'nowrap',
    alignItems: 'center',
    justifyContent:'space-between',
    height: 30,
    marginTop: -2,
    paddingBottom: 2
};


export function ImageMetaDataToolbarView({viewerId, viewerPlotIds=[], layoutType,
                                          activeTable, makeDataProductsConverter, makeDropDown}) {

    const converter= makeDataProductsConverter(activeTable) || {};
    if (!converter) {
        return <div/>;
    }
    const dataId= converter.converterId;
    const viewer= getViewer(getMultiViewRoot(), viewerId);
    const layoutDetail= getLayoutDetails(getMultiViewRoot(), viewerId, activeTable?.tbl_id);

    // single mode stuff

    const showThreeColorButton= converter.threeColor && layoutDetail!==GRID_FULL && !(viewerPlotIds[0].includes(GRID_FULL.toLowerCase()));
    const showPager= activeTable && converter.canGrid && layoutType===GRID && layoutDetail===GRID_FULL;
    const showMultiImageOps= converter.canGrid || converter.hasRelatedBands;


    let metaControls= true;
    if (!makeDropDown && !showMultiImageOps && !converter.canGrid &&
        !converter.hasRelatedBands && !showThreeColorButton && !(layoutType===SINGLE && viewerPlotIds.length>1) &&
        !showPager) {
        metaControls= false;
    }


    return (
        <div style={toolsStyle}>
            {makeDropDown && makeDropDown()}
            {metaControls && <div style={{whiteSpace: 'nowrap'}}>
                {showMultiImageOps && <ToolbarButton icon={ONE} tip={'Show single image at full size'}
                               imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                               enabled={true} visible={true}
                               horizontal={true}
                               onClick={() => dispatchChangeViewerLayout(viewerId,SINGLE, undefined, activeTable?.tbl_id)}/>}

                {converter.canGrid && <ToolbarButton icon={FULL_GRID} tip={'Tile all images in the search result table'}
                               enabled={true} visible={true} horizontal={true}
                               imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                               onClick={() => dispatchChangeViewerLayout(viewerId,GRID,GRID_FULL,activeTable?.tbl_id)}/>}

                {converter.hasRelatedBands  &&
                            <ToolbarButton icon={GRID_GROUP} tip={'Tile all data products associated with the highlighted table row'}
                               enabled={true} visible={true} horizontal={true}
                               imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                               style={{marginLeft: 20}}
                               onClick={() => dispatchChangeViewerLayout(viewerId,GRID,GRID_RELATED,activeTable?.tbl_id)}/>
                }
                {showThreeColorButton &&
                             <ToolbarButton icon={THREE_COLOR} tip={'Create three color image'}
                                         enabled={true} visible={true} horizontal={true}
                                         imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                                         onClick={() => showThreeColorOps(viewer,dataId)}/>
                }
            </div> }
            {showPager && <ImagePager pageSize={converter.maxPlots} tbl_id={activeTable.tbl_id} style={{marginLeft:10}}/>}
            <VisMiniToolbar viewerId={viewerId}/>
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
    makeDropDown: PropTypes.func
};

function showThreeColorOps(viewer,dataId) {
    if (!viewer) return;
    const newCustom= Object.assign({}, viewer.customData[dataId], {threeColorVisible:true});
    showColorBandChooserPopup(viewer.viewerId,newCustom,dataId);
}


