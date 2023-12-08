/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import PropTypes from 'prop-types';
import {IconButton, Stack} from '@mui/joy';
import {dispatchChangeViewerLayout} from '../MultiViewCntlr.js';
import {dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {showExpandedOptionsPopup} from './ExpandedOptionsPopup.jsx';
import {VisMiniToolbar} from './VisMiniToolbar.jsx';
import {getActivePlotView} from '../PlotViewUtil.js';

import ONE from 'html/images/icons-2014/Images-One.png';
import GRID from 'html/images/icons-2014/Images-Tiled.png';
import PAGE_RIGHT from 'html/images/icons-2014/20x20_PageRight.png';
import PAGE_LEFT from 'html/images/icons-2014/20x20_PageLeft.png';
import LIST from 'html/images/icons-2014/ListOptions.png';



export function MultiViewStandardToolbar({visRoot, viewerId, viewerPlotIds,
                                          layoutType= 'grid', makeDropDown, useImageList= false,
                                             toolbarStyle={}}) {
    
    let cIdx= viewerPlotIds.findIndex( (plotId) => plotId===visRoot.activePlotId);

    if (cIdx<0) cIdx= 0;

    const moreThanOne= viewerPlotIds.length>1;
    const nextIdx= cIdx===viewerPlotIds.length-1 ? 0 : cIdx+1;
    const prevIdx= cIdx ? cIdx-1 : viewerPlotIds.length-1;


    const {showImageToolbar=true}= getActivePlotView(visRoot)?.plotViewCtx.menuItemKeys ?? {};
    if (!showImageToolbar) return <div/>;

    return (
        <Stack {...{direction:'row', flexWrap:'nowrap', alignItems: 'center', justifyContent: 'space-between',
             width:'100%', height: 30, style:toolbarStyle}}>
            <Stack {...{direction:'row', alignItems: 'center', flexWrap:'nowrap'}}>
                {moreThanOne && <ToolbarButton icon={ONE} tip={'Show single image at full size'}
                               imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                               horizontal={true}
                               onClick={() => dispatchChangeViewerLayout(viewerId,'single')}/>}
                {moreThanOne && <ToolbarButton icon={GRID} tip={'Show all as tiles'}
                               horizontal={true}
                               imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                               onClick={() => dispatchChangeViewerLayout(viewerId,'grid')}/>}
                {useImageList && moreThanOne &&
                    <ToolbarButton icon={LIST} tip={'Choose which plots to show'}
                                   imageStyle={{width:24,height:24}}
                                   horizontal={true}
                                   onClick={() =>showExpandedOptionsPopup('Pinned Images', viewerId) }/>
                }
                {layoutType==='single' && moreThanOne &&
                    <>
                        <IconButton {...{size:'sm', variant:'plain', color:'neutral',
                            onClick:() => dispatchChangeActivePlotView(viewerPlotIds[prevIdx])}}>
                            <img src={PAGE_LEFT}/>
                        </IconButton>
                        <IconButton {...{size:'sm', variant:'plain', color:'neutral',
                            onClick:() => dispatchChangeActivePlotView(viewerPlotIds[nextIdx])}}>
                            <img src={PAGE_RIGHT}/>
                        </IconButton>
                    </>
                }
                {makeDropDown?.()}
            </Stack>
            <VisMiniToolbar viewerId={viewerId}/>
        </Stack>
    );
}


MultiViewStandardToolbar.propTypes= {
    dlAry : PropTypes.arrayOf(PropTypes.object),
    visRoot : PropTypes.object,
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string,
    viewerPlotIds : PropTypes.arrayOf(PropTypes.string).isRequired,
    makeDropDownFunc: PropTypes.func,
    makeDropDown: PropTypes.bool,
    useImageList: PropTypes.bool,
    toolbarStyle: PropTypes.object,
};
