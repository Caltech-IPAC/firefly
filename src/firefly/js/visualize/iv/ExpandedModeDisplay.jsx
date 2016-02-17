/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {visRoot, dispatchChangeExpandedMode, ExpandType} from '../ImagePlotCntlr.js';
import {flux} from '../../Firefly.js';
import {ExpandedModeDisplayView} from './ExpandedModeDisplayView.jsx';



export class ExpandedModeDisplay extends Component {


    constructor(props) {
        super(props);
        this.state= {visRoot:visRoot()};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
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
        var {state}= this;
        var vr= visRoot();
        if (vr!==state.visRoot) {
            this.setState({visRoot:vr});
        }
    }



    render() {
        return (
            <ExpandedModeDisplayView visRoot={this.state.visRoot}
            />
        );
    }
}

ExpandedModeDisplay.propTypes= {
    forceExpandedMode : PropTypes.bool
};

