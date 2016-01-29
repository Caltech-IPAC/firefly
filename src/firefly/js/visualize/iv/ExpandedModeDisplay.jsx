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
        var allPlots= visRoot();
        this.state= {allPlots};
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
            var prevExm= this.state.allPlots.previousExpandedMode;
            dispatchChangeExpandedMode(prevExm);
        }
    }

    storeUpdate() {
        var {state}= this;
        var allPlots= visRoot();
        if (allPlots!==state.allPlots) {
            this.setState({allPlots});
        }
    }



    render() {
        var {allPlots}= this.state;
        return (
            <ExpandedModeDisplayView allPlots={allPlots}
            />
        );
    }
}

ExpandedModeDisplay.propTypes= {
    forceExpandedMode : PropTypes.bool
};

