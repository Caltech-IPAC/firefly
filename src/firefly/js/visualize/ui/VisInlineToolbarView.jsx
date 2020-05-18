/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {LayerButton} from './VisToolbarView.jsx';
import {showTools} from './VisToolbar.jsx';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {ExpandType, dispatchChangeExpandedMode, dispatchChangeActivePlotView,
        dispatchDeletePlotView} from '../ImagePlotCntlr.js';
import {pvEqualExScroll} from '../PlotViewUtil';
import shallowequal from 'shallowequal';

import OUTLINE_EXPAND from 'html/images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';
import GRID_EXPAND from 'html/images/icons-2014/24x24_ExpandArrows-grid-3.png';
import DELETE from 'html/images/blue_delete_10x10.png';
import WRENCH from 'html/images/wrench-24x24.png';


function expand(plotId, grid) {
    dispatchChangeActivePlotView(plotId);
    dispatchSetLayoutMode( LO_MODE.expanded, LO_VIEW.images );
    grid ? dispatchChangeExpandedMode(ExpandType.GRID) : dispatchChangeExpandedMode(true);
}

const rS= {
    width: '100% - 2px',
    position: 'relative',
    verticalAlign: 'top',
    whiteSpace: 'nowrap',
    display:'inline-flex',
    flexDirection:'row',
    flexWrap:'nowrap',
    alignItems: 'center',
    zIndex : 1
};

export const VisInlineToolbarView = memo( (props) => {
        const {dlCount=0, pv, showLayer, expandGrid, showExpand, showDelete, showToolbarButton, help_id}= props;
        if (!pv) return undefined;
        const deleteClick= () => dispatchDeletePlotView({plotId:pv.plotId});
        const expandClick= () => expand(pv.plotId, expandGrid);
        const shouldShowTools= Boolean((showToolbarButton && showExpand) || (showToolbarButton && !pv.plots.length));

        return (
            <div style={rS}>
                <LayerButton pv={pv} dlCount={dlCount} visible={Boolean(showLayer && showExpand)}/>
                <ToolbarButton icon={WRENCH} tip='Show tools' style={{alignSelf:'flex-start'}}
                               horizontal={true} visible={shouldShowTools} onClick={showTools}/>
                {help_id && <div style={{marginRight: 20}}><HelpIcon helpId={help_id}/></div>}
                <ToolbarButton icon={expandGrid? GRID_EXPAND : OUTLINE_EXPAND}
                               tip='Expand this panel to take up a larger area'
                               horizontal={true} visible={showExpand} onClick={expandClick}/>
                <ToolbarButton icon={DELETE} tip='Delete Image'
                               style={{alignSelf:'flex-start'}}
                               horizontal={true} visible={showDelete} onClick={deleteClick}/>
            </div>
        );
    },
    (p,nP) => shallowequal({...p, pv:undefined}, {...nP,pv:undefined}) && pvEqualExScroll(p.pv, nP.pv)
);

VisInlineToolbarView.propTypes= {
    pv : PropTypes.object,
    dlCount: PropTypes.number,
    showLayer : PropTypes.bool,
    showExpand : PropTypes.bool,
    expandGrid : PropTypes.bool,
    showDelete : PropTypes.bool,
    help_id : PropTypes.bool,
    showToolbarButton : PropTypes.bool
};
