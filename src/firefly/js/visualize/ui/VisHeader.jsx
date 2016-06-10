/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import sCompare from 'react-addons-shallow-compare';
import {visRoot} from '../ImagePlotCntlr.js';
import {flux} from '../../Firefly.js';
import {VisHeaderView} from './VisHeaderView.jsx';
import {addMouseListener, lastMouseCtx} from '../VisMouseSync.js';
import {readoutRoot, dispatchReadoutData, makeValueReadoutItem, makePointReadoutItem,
    makeDescriptionItem, isLockByClick} from '../../visualize/MouseReadoutCntlr.js';


export class VisHeader_old extends React.Component {
    constructor(props) {
        super(props);
        this.state= {visRoot:visRoot(), currMouseState:lastMouseCtx()};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        if (this.removeMouseListener) this.removeMouseListener();
    }


    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
        this.removeMouseListener= addMouseListener(() => this.storeUpdate());
    }

    storeUpdate() {
        if (visRoot()!==this.state.visRoot || lastMouseCtx() !==this.state.currMouseState) {
            this.setState({visRoot:visRoot(), currMouseState:lastMouseCtx()});
        }
    }

    render() {
        var {visRoot,currMouseState}= this.state;
        return <VisHeaderView visRoot={visRoot} currMouseState={currMouseState}/>;
    }
}



export class VisHeader extends React.Component {
    constructor(props) {
        super(props);
        this.state= {visRoot:visRoot(), currMouseState:lastMouseCtx(), readout:readoutRoot()};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

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
        var {visRoot,currMouseState,readout}= this.state;
        return <VisHeaderView visRoot={visRoot} currMouseState={currMouseState} readout={readout}/>;
    }
}


