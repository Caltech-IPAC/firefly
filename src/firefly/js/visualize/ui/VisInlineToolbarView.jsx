/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {primePlot } from '../PlotViewUtil.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {LayerButton} from './VisToolbarView.jsx';
import {dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {dispatchChangeExpandedMode, dispatchChangeActivePlotView, dispatchDeletePlotView} from '../ImagePlotCntlr.js';
import {LO_EXPANDED} from '../../core/LayoutCntlr.js';

import OUTLINE_EXPAND from 'html/images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';
import DELETE from 'html/images/blue_delete_10x10.png';




function expand(plotId) {
    dispatchChangeActivePlotView(plotId);
    dispatchSetLayoutMode( LO_EXPANDED.images );
    dispatchChangeExpandedMode(true);
}



const rS= {
    width: '100% - 2px',
    height: 34,
    position: 'relative',
    verticalAlign: 'top',
    whiteSpace: 'nowrap',
    display:'inline-flex',
    flexDirection:'row',
    flexWrap:'nowrap',
    alignItems: 'center'
};



/**
 *
 * @param plotId
 * @param dlAry
 * @param showLayer
 * @param showExpand
 * @param showDelete
 * @return {XML}
 * @constructor
 */

export class VisInlineToolbarView extends Component {
    constructor(props) {
        super(props);
        this.deleteClick= this.deleteClick.bind(this);
        this.expandClick= this.expandClick.bind(this);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    deleteClick() {dispatchDeletePlotView(this.props.plotId);}
    expandClick() {expand(this.props.plotId);}

    render() {
        const {plotId, dlAry, showLayer, showExpand, showDelete}= this.props;

        return (
            <div style={rS}>
                <LayerButton plotId={plotId} dlCount={dlAry.length} visible={showLayer}/>

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
    plotId : PropTypes.string.isRequired,
    dlAry : PropTypes.arrayOf(React.PropTypes.object),
    extensionAry : PropTypes.arrayOf(React.PropTypes.object),
    showLayer : PropTypes.bool,
    showExpand : PropTypes.bool,
    showDelete : PropTypes.bool
};

