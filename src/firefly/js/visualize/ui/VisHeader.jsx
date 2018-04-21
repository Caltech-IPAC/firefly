/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import {get} from 'lodash';
import PropTypes from 'prop-types';
import {visRoot} from '../ImagePlotCntlr.js';
import {flux} from '../../Firefly.js';
import {VisHeaderView, VisPreview} from './VisHeaderView.jsx';
import {addMouseListener, lastMouseCtx} from '../VisMouseSync.js';
import {readoutRoot} from '../../visualize/MouseReadoutCntlr.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';




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
        this.removeMouseListener= addMouseListener(() => this.storeUpdate());
    }

    storeUpdate() {
        const readout= readoutRoot();
        if (visRoot()!==this.state.visRoot || lastMouseCtx() !==this.state.currMouseState || readout!==this.state.readout) {
            this.setState({visRoot:visRoot(), currMouseState:lastMouseCtx(), readout:readout});
        }


    }

    render() {
        const {showHeader=true, showPreview=true} = this.props; 
        const {visRoot,currMouseState,readout, showHealpixPixel}= this.state;
        return (
            <div>
                {showHeader && <VisHeaderView {...{showPreview, visRoot, currMouseState, readout, showHealpixPixel}}/>}
                {showPreview && <VisPreview {...{showPreview, visRoot, currMouseState, readout}}/>}
            </div>
        );
    }
}

VisHeader.propTypes= {
    showHeader : PropTypes.bool,
    showPreview :PropTypes.bool,
};

