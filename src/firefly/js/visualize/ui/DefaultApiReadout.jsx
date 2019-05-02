/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {visRoot} from '../ImagePlotCntlr.js';
import {flux} from '../../Firefly.js';
import { readoutRoot} from '../MouseReadoutCntlr.js';
import {lastMouseCtx} from '../VisMouseSync.js';
import {getActivePlotView, getPlotViewById} from '../PlotViewUtil.js';
import {ThumbnailView} from './ThumbnailView.jsx';
import {MagnifiedView} from './MagnifiedView.jsx';
import {addImageReadoutUpdateListener, lastMouseImageReadout} from '../VisMouseSync';

const fullStyle= {
    display: 'inline-block',
    position: 'relative',
    verticalAlign: 'top',
    cursor:'pointer',
    whiteSpace : 'nowrap',
    overflow : 'hidden'
};

const minimalStyle= {
    display: 'inline-block',
    position: 'relative',
    verticalAlign: 'top',
    cursor:'pointer',
    whiteSpace : 'nowrap',
    overflow : 'hidden'
};
export class DefaultApiReadout extends PureComponent {
    constructor(props) {
        super(props);
        this.state= {visRoot:visRoot(), currMouseState:lastMouseCtx(), readout:readoutRoot()};
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.removeListener) this.removeListener();
        if (this.removeMouseListener) this.removeMouseListener();
    }


    componentDidMount() {
        this.iAmMounted= true;
        this.removeListener= flux.addListener(() => this.storeUpdate());
        this.removeMouseListener= addImageReadoutUpdateListener(() => this.storeUpdate());
    }

    storeUpdate() {
        const readout= readoutRoot();
        const {currMouseState,readoutData}= this.state;
        if (visRoot()!==this.state.visRoot ||
            lastMouseImageReadout()!== readoutData ||
            lastMouseCtx() !==currMouseState ||
            readout !== this.state.readout) {
            if (this.iAmMounted) {
                this.setState({visRoot:visRoot(), readoutData:lastMouseImageReadout(), currMouseState:lastMouseCtx(), readout});
            }
        }
    }

    render() {

        //<div style={{display:'inline-block', float:'right', whiteSpace:'nowrap'}}>
        const {MouseReadoutComponent, showThumb, showMag}= this.props;
        var {currMouseState, readout, readoutData={} }= this.state;
        var pv = getActivePlotView(visRoot());
        var mousePv = getPlotViewById(visRoot(), currMouseState.plotId);

        if (!showThumb && !showMag) {
            return renderMouseReadoutOnly(MouseReadoutComponent, readout, readoutData);
        }
        else {

            const style = MouseReadoutComponent &&  MouseReadoutComponent.name==='PopupMouseReadoutMinimal'?minimalStyle:fullStyle;
            return (

                <div style={{display:'flex',flexWrap:'nowrap'}}>
                    <div style={style}>
                        <div style={{position:'relative', color:'black', height:'100%'}}>
                            <MouseReadoutComponent readout={readout} readoutData={readoutData} showMag={ showMag} showThumb={showThumb}/>
                        </div>
                    </div>


                    {showThumb && <ThumbnailView  plotView={pv}/>}
                    {showMag && <MagnifiedView plotView={mousePv} size={70} mouseState={currMouseState}/>}

                </div>
            );
        }
    }
}

function renderMouseReadoutOnly(MouseReadoutComponent, readout, readoutData){
    return (
    <div style={{display:'inline-block', float:'right', whiteSpace:'nowrap'}}>
             <MouseReadoutComponent readout={readout} readoutData={readoutData}/>
    </div>
    );
}
DefaultApiReadout.propTypes= {
    forceExpandedMode : PropTypes.bool,
    closeFunc: PropTypes.func,
    showThumb: PropTypes.bool,
    showMag: PropTypes.bool,
    MouseReadoutComponent : PropTypes.any
};

DefaultApiReadout.defaultProps= {
    showThumb:true,
    showMag:true
};
