/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {visRoot, dispatchChangeExpandedMode, ExpandType} from '../ImagePlotCntlr.js';
import {flux} from '../../Firefly.js';
import {expandedPlotViewAry} from '../PlotViewUtil.js';
import {ExpandedTools} from './ExpandedTools.jsx';
import {MultiImageViewerView} from '../ui/MultiImageViewerView.jsx';

export class ExpandedModeDisplay extends Component {


    constructor(props) {
        super(props);
        this.state= {visRoot:visRoot()};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        if (this.props.forceExpandedMode) {
            dispatchChangeExpandedMode(ExpandType.COLLAPSE);
        }
    }


    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
        if (this.props.forceExpandedMode) {
            dispatchChangeExpandedMode(true);
        }
    }

    storeUpdate() {
        var {state}= this;
        var vr= visRoot();
        if (vr!==state.visRoot) {
            this.setState({visRoot:vr});
        }
    }



    render() {
        const {visRoot:vr}= this.state;
        if (vr.expandedMode===ExpandType.COLLAPSE) return false;

        const layoutType= vr.expandedMode===ExpandType.GRID ? 'grid' : 'single';

        const {plotViewAry,activePlotId}= vr;
        const pvAry= expandedPlotViewAry(plotViewAry,activePlotId).map( (pv) => pv.plotId);



        return (
            <MultiImageViewerView viewerPlotIds={pvAry}
                                  layoutType={layoutType}
                                  Toolbar={ExpandedTools}
                                  viewerId={'EXPANDED_VIEW'}
                                  visRoot={vr}
                                  additionalStyle={{flex:'1 1'}} 
                                  defaultDecoration={false} 
            />
        );
    }
}

ExpandedModeDisplay.propTypes= {
    forceExpandedMode : PropTypes.bool
};

