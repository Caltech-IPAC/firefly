/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {visRoot} from '../ImagePlotCntlr.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import {getAllDrawLayersForPlot} from '../PlotViewUtil.js';
import {flux} from '../../Firefly.js';
import {VisToolbarViewWrapper} from './VisToolbarView.jsx';

// import {deepDiff} from '../../util/WebUtil.js';


export class VisToolbar extends Component {
    constructor(props) {
        super(props);
        this.state= {visRoot:visRoot(), dlCount:0, tip:''};
        this.tipOn= (tip) => this.setState({tip});
        this.tipOff= () => this.setState({tip:null});
    }

    shouldComponentUpdate(np,ns) {
        return sCompare(this,np,ns);
    }

    componentDidUpdate(prevProps, prevState) {
        // deepDiff({props: prevProps, state: prevState},
        //     {props: this.props, state: this.state},
        //     '---------- vis tool bar', true);
    }

    getChildContext() {
        return {tipOnCB: this.tipOn, tipOffCB: this.tipOff};
    }


    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
    }


    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {
        const vr= visRoot();
        const dlCount= getAllDrawLayersForPlot(getDlAry(),vr.activePlotId).length;
        var needsUpdate= (vr!==this.state.visRoot && vr.activePlotId!==this.state.visRoot.activePlotId);
        if (needsUpdate || dlCount!==this.state.dlCount) {
            this.setState({visRoot:visRoot(), dlCount});
        }
    }

    render() {
        var {visRoot,tip,dlCount}= this.state;
        return <VisToolbarViewWrapper visRoot={visRoot} toolTip={tip} dlCount={dlCount}/>;
    }


}

VisToolbar.childContextTypes= {
    tipOnCB : PropTypes.func,
    tipOffCB : PropTypes.func
};
