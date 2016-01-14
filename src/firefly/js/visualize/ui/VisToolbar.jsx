/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {visRoot} from '../ImagePlotCntlr.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import {flux} from '../../Firefly.js';
import {VisToolbarView} from './VisToolbarView.jsx';



export class VisToolbar extends Component {
    constructor(props) {
        super(props);
        this.state= {visRoot:visRoot(), dlAry:getDlAry(), tip:''};
        this.tipOn= (tip) => this.setState({tip});
        this.tipOff= () => this.setState({tip:null});
    }

    getChildContext() {
        return {tipOnCB: this.tipOn, tipOffCB: this.tipOff};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
    }


    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {
        if (visRoot()!==this.state.visRoot || getDlAry() !==this.state.dlARy) {
            this.setState({visRoot:visRoot(), dlAry:getDlAry()});
        }
    }

    render() {
        var {visRoot,dlAry,tip}= this.state;
        return <VisToolbarView visRoot={visRoot} dlAry={dlAry} toolTip={tip}/>;
    }


}

VisToolbar.childContextTypes= {
    tipOnCB : PropTypes.func,
    tipOffCB : PropTypes.func
};
