/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {visRoot} from '../ImagePlotCntlr.js';
import {flux} from '../../Firefly.js';
import {VisHeaderView} from './VisHeaderView.jsx';
import {ImageExpandedMode} from '../iv/ImageExpandedMode.jsx';
import {readoutRoot} from '../MouseReadoutCntlr.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {addImageReadoutUpdateListener, lastMouseImageReadout, lastMouseCtx} from '../VisMouseSync.js';



export class ApiExpandedDisplay extends PureComponent {
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
        const {currMouseState,readoutData}= this.state;
        if (visRoot()!==this.state.visRoot ||
            lastMouseCtx() !==currMouseState ||
            lastMouseImageReadout() !==readoutData ||
            readoutRoot()!==this.state.readout) {
            this.setState({visRoot:visRoot(), readoutData:lastMouseImageReadout(),
                            currMouseState:lastMouseCtx(), readout:readoutRoot()});
        }
    }

    /**
     *
     * @return {XML}
     */
    render() {
        const {closeFunc, viewerId}= this.props;
        const {visRoot,currMouseState, readout, readoutData, showHealpixPixel}= this.state;
        return (
            <div style={{width:'100%', height:'100%', display:'flex', flexWrap:'nowrap',
                         alignItems:'stretch', flexDirection:'column'}}>
                <div style={{position: 'relative', marginBottom:'6px'}} className='banner-background'>
                    <VisHeaderView visRoot={visRoot} currMouseState={currMouseState}
                                   readoutData={readoutData} readout={readout}
                                   style={{
                                       height: 34,
                                       minHeight: 34,
                                       padding: '2px 0 1px 0'
                                   }}
                                   showHealpixPixel={showHealpixPixel}/>
                </div>
                <div style={{flex: '1 1 auto', display:'flex'}}>
                    <ImageExpandedMode   {...{key:'results-plots-expanded', closeFunc, viewerId}}/>
                </div>
            </div>
            );
    }
}

ApiExpandedDisplay.propTypes= {
    forceExpandedMode : PropTypes.bool,
    closeFunc: PropTypes.func,
    viewerId: PropTypes.string
};

ApiExpandedDisplay.defaultProps= {
    closeFunc:null
};
