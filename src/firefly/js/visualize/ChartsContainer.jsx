/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import shallowequal from 'shallowequal';


import {flux} from '../Firefly.js';
import {get} from 'lodash';

import {LO_VIEW, LO_MODE, dispatchSetLayoutMode, getExpandedMode} from '../core/LayoutCntlr.js';
import {CloseButton} from '../ui/CloseButton.jsx';

import {ChartsTableViewPanel} from '../visualize/ChartsTableViewPanel.jsx';
import {CHART_SPACE_PATH} from '../visualize/ChartsCntlr.js';

export function getExpandedChartProps() {
    return get(flux.getState(),[CHART_SPACE_PATH, 'ui', 'expanded']);
}

function nextState(props) {
    const {closeable, chartId, tblId, optionsPopup} = props;
    const expandedMode = props.expandedMode && getExpandedMode() === LO_VIEW.xyPlots;
    const chartProps = expandedMode ? getExpandedChartProps() : {chartId, tblId, optionsPopup};
    return Object.assign({expandedMode,closeable}, chartProps);
}

export class ChartsContainer extends Component {
    constructor(props) {
        super(props);
        this.state = nextState(this.props);
    }

    componentWillReceiveProps(np) {
        if (!this.isUnmounted && !shallowequal(this.props, np)) {
            this.setState(nextState(np));
        }
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
    }


    storeUpdate() {
        if (!this.isUnmounted) {
            const expandedMode = this.props.expandedMode && getExpandedMode() === LO_VIEW.xyPlots;
            if (expandedMode !== this.state.expandedMode) {
                this.setState(nextState(this.props));
            }
        }
    }

    render() {
        const {expandedMode, closeable} = this.state;
        return expandedMode ? <ExpandedView key='chart-expanded' closeable={closeable} {...this.props} {...this.state}/> : <ChartsTableViewPanel {...this.props} {...this.state}/>;
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
