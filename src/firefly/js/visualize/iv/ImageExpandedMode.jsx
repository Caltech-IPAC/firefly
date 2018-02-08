/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {visRoot, dispatchChangeExpandedMode, ExpandType} from '../ImagePlotCntlr.js';
import {getMultiViewRoot, getViewer, getExpandedViewerItemIds, EXPANDED_MODE_RESERVED} from '../MultiViewCntlr.js';
import {flux} from '../../Firefly.js';
import {ExpandedTools} from './ExpandedTools.jsx';
import {MultiImageViewerView} from '../ui/MultiImageViewerView.jsx';
import {getPlotViewById} from '../PlotViewUtil.js';

export class ImageExpandedMode extends PureComponent {


    constructor(props) {
        super(props);
        this.state= {visRoot:visRoot(), multiViewRoot:getMultiViewRoot()};
    }

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

        var viewer= getViewer(getMultiViewRoot(),EXPANDED_MODE_RESERVED);

        return (
            <MultiImageViewerView viewerPlotIds={getExpandedViewerItemIds(multiViewRoot)}
                                  layoutType={layoutType}
                                  Toolbar={ExpandedTools}
                                  viewerId={viewer.viewerId}
                                  visRoot={vr}
                                  style={{flex:'1 1 auto'}}
                                  closeFunc={closeFunc}
                                  defaultDecoration={false} 
                                  showWhenExpanded={true}
                                  inlineTitle={true}
                                  aboveTitle={false}
                                  insideFlex={insideFlex}
            />
        );
    }
}

ImageExpandedMode.propTypes= {
    forceExpandedMode : PropTypes.bool,
    closeFunc: PropTypes.func,
    insideFlex: PropTypes.bool
};

ImageExpandedMode.defaultProps= {
    forceExpandedMode : true,
    closeFunc:null,
    insideFlex:true
};

