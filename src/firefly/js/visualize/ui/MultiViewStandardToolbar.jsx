/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import PropTypes from 'prop-types';
import {dispatchChangeViewerLayout} from '../MultiViewCntlr.js';
import {dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import ONE from 'html/images/icons-2014/Images-One.png';
import GRID from 'html/images/icons-2014/Images-Tiled.png';
import PAGE_RIGHT from 'html/images/icons-2014/20x20_PageRight.png';
import PAGE_LEFT from 'html/images/icons-2014/20x20_PageLeft.png';
import {VisMiniToolbar} from './VisMiniToolbar.jsx';
import {getActivePlotView} from '../PlotViewUtil.js';




const toolsStyle= {
    display:'flex',
    flexDirection:'row',
    flexWrap:'nowrap',
    alignItems: 'center',
    width:'100%',
    height: 30,
    justifyContent: 'space-between',
    marginTop: -2,
    paddingBottom: 2
};


export function MultiViewStandardToolbar({visRoot, viewerId, viewerPlotIds,
                                          layoutType= 'grid', makeDropDown, toolbarStyle={}}) {
    
    let cIdx= viewerPlotIds.findIndex( (plotId) => plotId===visRoot.activePlotId);

    if (cIdx<0) cIdx= 0;

    const moreThanOne= viewerPlotIds.length>1;

    const leftImageStyle= {
        verticalAlign:'bottom',
        cursor:'pointer',
        flex: '0 0 auto',
        paddingLeft: 10
    };
    if (viewerPlotIds.length===2) {
        leftImageStyle.visibility='hidden';
    }


    const nextIdx= cIdx===viewerPlotIds.length-1 ? 0 : cIdx+1;
    const prevIdx= cIdx ? cIdx-1 : viewerPlotIds.length-1;


    const style= {...toolsStyle, ...toolbarStyle};
    const {showImageToolbar=true}= getActivePlotView(visRoot)?.plotViewCtx.menuItemKeys ?? {};
    if (!showImageToolbar) return <div/>;

    return (
        <div style={style}>
            <div style={{display:'flex', flexDirection:'row', alignItems: 'center', flexWrap:'nowrap'}}>
                {moreThanOne && <ToolbarButton icon={ONE} tip={'Show single image at full size'}
                               imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                               enabled={true} visible={true}
                               horizontal={true}
                               onClick={() => dispatchChangeViewerLayout(viewerId,'single')}/>}
                {moreThanOne && <ToolbarButton icon={GRID} tip={'Show all as tiles'}
                               enabled={true} visible={true} horizontal={true}
                               imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                               onClick={() => dispatchChangeViewerLayout(viewerId,'grid')}/>}
                {layoutType==='single' && moreThanOne &&
                <img style={leftImageStyle} src={PAGE_LEFT}
                     onClick={() => dispatchChangeActivePlotView(viewerPlotIds[prevIdx])} />
                }
                {layoutType==='single' && moreThanOne &&
                <img style={{verticalAlign:'bottom', cursor:'pointer', float: 'right', paddingLeft:5, flex: '0 0 auto'}}
                     src={PAGE_RIGHT}
                     onClick={() => dispatchChangeActivePlotView(viewerPlotIds[nextIdx])} />
                }
                {makeDropDown && makeDropDown()}
            </div>
            <VisMiniToolbar/>
        </div>
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
    toolbarStyle: PropTypes.object,
};

