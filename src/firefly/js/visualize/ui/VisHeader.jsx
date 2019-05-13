/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import {get} from 'lodash';
import PropTypes from 'prop-types';
import {visRoot} from '../ImagePlotCntlr.js';
import {flux} from '../../Firefly.js';
import {VisHeaderView, VisPreview} from './VisHeaderView.jsx';
import {lastMouseCtx} from '../VisMouseSync.js';
import {readoutRoot} from '../../visualize/MouseReadoutCntlr.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {addImageReadoutUpdateListener, lastMouseImageReadout} from '../VisMouseSync';




export class VisHeader extends PureComponent {
    constructor(props) {
        super(props);
        const showHealpixPixel= get(getAppOptions(), 'hips.readoutShowsPixel');
        this.state= {visRoot:visRoot(), currMouseState:lastMouseCtx(), readout:readoutRoot(), showHealpixPixel};
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        if (this.removeMouseListener) this.removeMouseListener();
    }


    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
        this.removeMouseListener= addImageReadoutUpdateListener(() => this.storeUpdate());
    }

    storeUpdate() {
        const readout= readoutRoot();
        const {currMouseState,readoutData}= this.state;
        if (visRoot()!==this.state.visRoot || lastMouseImageReadout()!== readoutData || lastMouseCtx() !==currMouseState || readout!==this.state.readout) {
            this.setState({visRoot:visRoot(), currMouseState:lastMouseCtx(),
                           readoutData:lastMouseImageReadout(), readout});
        }


    }

    render() {
        const {showHeader=true, showPreview=true} = this.props; 
        const {visRoot,currMouseState,readout, readoutData={}, showHealpixPixel}= this.state;
        return (
            <div>
                {showHeader && <VisHeaderView {...{readout, readoutData, showHealpixPixel}}/>}
                {showPreview && <VisPreview {...{visRoot, currMouseState}}/>}
            </div>
        );
    }
}

VisHeader.propTypes= {
    showHeader : PropTypes.bool,
    showPreview :PropTypes.bool,
};

