/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {visRoot, dispatchChangeExpandedMode, ExpandType} from '../ImagePlotCntlr.js';
import {getMultiViewRoot, getExpandedViewerItemIds} from '../MultiViewCntlr.js';
import {flux} from '../../Firefly.js';
import {ExpandedTools} from './ExpandedTools.jsx';
import {MultiImageViewerView} from '../ui/MultiImageViewerView.jsx';

export class ExpandedModeDisplay extends Component {


    constructor(props) {
        super(props);
        this.state= {visRoot:visRoot(), multiViewRoot:getMultiViewRoot()};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.isUnmounted = true;
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
        if (!this.isUnmounted) {
            const {state}= this;
            const vr= visRoot();
            const mvR= getMultiViewRoot();

            if (vr!==state.visRoot || mvR!==state.multiViewRoot) {
                this.setState({visRoot:vr, multiViewRoot:mvR});
            }
        }
    }



    render() {
        const {closeFunc,insideFlex}= this.props;
        const {visRoot:vr,multiViewRoot}= this.state;
        if (vr.expandedMode===ExpandType.COLLAPSE) return false;

        const layoutType= vr.expandedMode===ExpandType.GRID ? 'grid' : 'single';

        return (
            <MultiImageViewerView viewerPlotIds={getExpandedViewerItemIds(multiViewRoot)}
                                  layoutType={layoutType}
                                  Toolbar={ExpandedTools}
                                  viewerId={'EXPANDED_VIEW'}
                                  visRoot={vr}
                                  additionalStyle={{flex:'1 1 auto'}}
                                  closeFunc={closeFunc}
                                  defaultDecoration={false} 
                                  showWhenExpanded={true}
                                  insideFlex={insideFlex}
            />
        );
    }
}

ExpandedModeDisplay.propTypes= {
    forceExpandedMode : PropTypes.bool,
    closeFunc: PropTypes.func,
    insideFlex: PropTypes.bool
};

ExpandedModeDisplay.defaultProps= {
    forceExpandedMode : false,
    closeFunc:null,
    insideFlex:false
};

