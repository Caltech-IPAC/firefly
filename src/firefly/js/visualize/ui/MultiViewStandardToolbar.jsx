/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import {dispatchChangeLayout} from '../MultiViewCntlr.js';
import {dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';

import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import ONE from 'html/images/icons-2014/Images-One.png';
import GRID from 'html/images/icons-2014/Images-Tiled.png';
import PAGE_RIGHT from 'html/images/icons-2014/20x20_PageRight.png';
import PAGE_LEFT from 'html/images/icons-2014/20x20_PageLeft.png';




const toolsStyle= {
    display:'flex',
    flexDirection:'row',
    flexWrap:'nowrap',
    alignItems: 'center',
    width:'100%',
    height: 30,
};



export function MultiViewStandardToolbar({visRoot, viewerId, viewerPlotIds, layoutType}) {
    
    var cIdx= viewerPlotIds.findIndex( (plotId) => plotId===visRoot.activePlotId);
    if (cIdx<0) cIdx= 0;


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

    return (
        <div style={toolsStyle}>
            <ToolbarButton icon={ONE} tip={'Show single image at full size'}
                           imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                           enabled={true} visible={true}
                           horizontal={true}
                           onClick={() => dispatchChangeLayout(viewerId,'single')}/>
            <ToolbarButton icon={GRID} tip={'Show all as tiles'}
                           enabled={true} visible={true} horizontal={true}
                           imageStyle={{width:24,height:24,  paddingLeft:5, flex: '0 0 auto'}}
                           onClick={() => dispatchChangeLayout(viewerId,'grid')}/>
            {layoutType==='single' && viewerPlotIds.length>1 &&
                        <img style={leftImageStyle} src={PAGE_LEFT}
                             onClick={() => dispatchChangeActivePlotView(viewerPlotIds[prevIdx])} />
            }
            {layoutType==='single' && viewerPlotIds.length>1 &&
                        <img style={{verticalAlign:'bottom', cursor:'pointer', float: 'right', paddingLeft:5, flex: '0 0 auto'}}
                             src={PAGE_RIGHT}
                             onClick={() => dispatchChangeActivePlotView(viewerPlotIds[nextIdx])} />
            }
        </div>
    );
}

MultiViewStandardToolbar.propTypes= {
    visRoot : PropTypes.object,
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string.isRequired,
    viewerPlotIds : PropTypes.arrayOf(PropTypes.string).isRequired
};






