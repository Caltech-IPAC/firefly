/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import shallowequal from 'shallowequal';


import {flux} from '../Firefly.js';

import {LO_VIEW, LO_MODE, dispatchSetLayoutMode, getExpandedMode} from '../core/LayoutCntlr.js';
import {CloseButton} from '../ui/CloseButton.jsx';

import {ChartsTableViewPanel} from '../visualize/ChartsTableViewPanel.jsx';
import {getExpandedChartProps} from '../visualize/ChartsCntlr.js';


function nextState(props) {
    const {closeable, chartId, tblId, chartType, optionsPopup} = props;
    const expandedMode = props.expandedMode && getExpandedMode() === LO_VIEW.xyPlots;
    const chartProps = expandedMode ? getExpandedChartProps() : {chartId, tblId, chartType, optionsPopup};
    return Object.assign({expandedMode,closeable}, chartProps);
}

export class ChartsContainer extends Component {
    constructor(props) {
        super(props);
        this.state = nextState(this.props);
    }

    componentWillReceiveProps(np) {
        if (this.iAmMounted && !shallowequal(this.props, np)) {
            this.setState(nextState(np));
        }
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
        this.iAmMounted = true;
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        this.removeListener && this.removeListener();
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
    }


    storeUpdate() {
        if (this.iAmMounted) {
            const expandedMode = this.props.expandedMode && getExpandedMode() === LO_VIEW.xyPlots;
            if (expandedMode !== this.state.expandedMode) {
                this.setState(nextState(this.props));
            }
        }
    }

    render() {
        const {expandedMode, closeable} = this.state;
        return expandedMode ? <ExpandedView key='chart-expanded' closeable={closeable} {...this.props} {...this.state}/> :
            (
                <div style={{ display: 'flex',  height: '100%', flexGrow: 1, flexDirection: 'row', overflow: 'hidden'}}>
                    <ChartsTableViewPanel key='xyplot' {...this.props} {...this.state} chartType='scatter'/>
                    <ChartsTableViewPanel key='histogram' {...this.props} {...this.state} chartType='histogram'/>
                </div>
            );
    }
}

ChartsContainer.propTypes = {
    expandedMode: PropTypes.bool,
    closeable: PropTypes.bool
};

function ExpandedView(props) {
    return (
        <div style={{ display: 'flex', height: '100%', flexGrow: 1, flexDirection: 'column', overflow: 'hidden'}}>
            <div style={{marginBottom: 3}}>
                {props.closeable && <CloseButton style={{display: 'inline-block', paddingLeft: 10}} onClick={() => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none)}/>}
            </div>
            <ChartsTableViewPanel expandedMode={true} expandable={false} {...props} />
        </div>
    );

}
