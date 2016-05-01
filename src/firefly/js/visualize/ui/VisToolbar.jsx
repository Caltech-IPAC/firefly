/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import {omit,pick} from 'lodash';
import sCompare from 'react-addons-shallow-compare';
import shallowequal from 'shallowequal';
import {visRoot} from '../ImagePlotCntlr.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import {getAllDrawLayersForPlot,getActivePlotView} from '../PlotViewUtil.js';
import {flux} from '../../Firefly.js';
import {VisToolbarViewWrapper} from './VisToolbarView.jsx';

// import {deepDiff} from '../../util/WebUtil.js';

const omList= ['plotViewAry'];
const pvPickList= ['plotViewCtx'];

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
        this.iAmMounted= false;
        if (this.removeListener) this.removeListener();
    }


    componentDidMount() {
        this.iAmMounted= true;
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }


    /**
     * If the object changed then check if any of the following changed.
     *  - drawing layer count
     *  - the part of visRoot that is not the plotViewAry
     *  - active plot id
     *  - the plotViewCtx in the active plot view
     *  This is a optimization so that the toolbar does not re-render every time the the plot scrolls
     */
    storeUpdate() {
        const vr= visRoot();
        const dlCount= getAllDrawLayersForPlot(getDlAry(),vr.activePlotId).length;
        
        if (vr===this.state.visRoot && dlCount===this.state.dlCount) return;

        var needsUpdate= dlCount!==this.state.dlCount;
        if (!needsUpdate) needsUpdate= vr.activePlotId!==this.state.visRoot.activePlotId;

        if (!needsUpdate) needsUpdate= !shallowequal(omit(vr,omList),omit(this.state.visRoot,omList));

        if (!needsUpdate) {
            const newPv= getActivePlotView(vr);
            const oldPv= getActivePlotView(this.state.visRoot);
            if (oldPv===newPv) return;
            needsUpdate= !shallowequal(pick(oldPv,pvPickList),pick(newPv,pvPickList));
        }
        if (needsUpdate && this.iAmMounted) {
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
