/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {primePlot } from '../PlotViewUtil.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {LayerButton} from './VisToolbarView.jsx';
import {VisToolbar,showTools} from './VisToolbar.jsx';
import {LayoutType, PopupPanel} from '../../ui/PopupPanel.jsx'
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {dispatchShowDialog,dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {dispatchChangeExpandedMode, dispatchChangeActivePlotView, dispatchDeletePlotView} from '../ImagePlotCntlr.js';

import OUTLINE_EXPAND from 'html/images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';
import DELETE from 'html/images/blue_delete_10x10.png';
import WRENCH from 'html/images/wrench-24x24.png';




function expand(plotId) {
    dispatchChangeActivePlotView(plotId);
    dispatchSetLayoutMode( LO_MODE.expanded, LO_VIEW.images );
    dispatchChangeExpandedMode(true);
}

// function showTools() {
//     const popup= (
//         <PopupPanel title={'Tools'} layoutPosition={LayoutType.TOP_LEFT} >
//             <VisToolbar />
//         </PopupPanel>
//     );
//     DialogRootContainer.defineDialog('PopupToolbar', popup);
//     dispatchShowDialog('PopupToolbar');
// }


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



export class VisInlineToolbarView extends Component {
    constructor(props) {
        super(props);
        this.deleteClick= this.deleteClick.bind(this);
        this.expandClick= this.expandClick.bind(this);
        // this.showToolsClick= this.showToolsClick.bind(this);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    deleteClick() {dispatchDeletePlotView({plotId:this.props.pv && this.props.pv.plotId});}
    expandClick() {expand(this.props.pv && this.props.pv.plotId);}
    // showToolsClick() {showTools();}

    render() {
        const {pv, dlAry, showLayer, showExpand, showDelete, showToolbarButton, help_id}= this.props;

        return (
            <div style={rS}>
                <LayerButton pv={pv} dlCount={dlAry.length} visible={showLayer && showExpand}/>

                <ToolbarButton icon={WRENCH}
                               tip='Show tools'
                               additionalStyle={{alignSelf:'flex-start'}}
                               horizontal={true}
                               visible={showToolbarButton && showExpand}
                               onClick={showTools}/>
                {help_id && <div style={{marginRight: 20}}><HelpIcon helpId={help_id}/></div>}
                <ToolbarButton icon={OUTLINE_EXPAND}
                               tip='Expand this panel to take up a larger area'
                               horizontal={true}
                               visible={showExpand}
                               onClick={this.expandClick}/>

                <ToolbarButton icon={DELETE}
                               tip='Delete Image'
                               additionalStyle={{alignSelf:'flex-start'}}
                               horizontal={true}
                               visible={showDelete}
                               onClick={this.deleteClick}/>

            </div>
        );
    }

}

VisInlineToolbarView.propTypes= {
    pv : PropTypes.object,
    dlAry : PropTypes.arrayOf(React.PropTypes.object),
    extensionAry : PropTypes.arrayOf(React.PropTypes.object),
    showLayer : PropTypes.bool,
    showExpand : PropTypes.bool,
    showDelete : PropTypes.bool,
    showToolbarButton : PropTypes.bool
};

