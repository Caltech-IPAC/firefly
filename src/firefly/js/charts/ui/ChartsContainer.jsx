/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import shallowequal from 'shallowequal';
import {isEqual} from 'lodash';


import {flux} from '../../Firefly.js';

import {LO_VIEW, LO_MODE, dispatchSetLayoutMode, getExpandedMode} from '../../core/LayoutCntlr.js';
import {CloseButton} from '../../ui/CloseButton.jsx';

import {ChartsTableViewPanel} from './ChartsTableViewPanel.jsx';
import {getExpandedChartProps, getChartIdsInGroup} from '../ChartsCntlr.js';
import {getActiveTableId} from '../../tables/TableUtil.js';


function nextState(props) {

    const {closeable, chartId} = props;
    let {tblId} = props;
    tblId = (tblId || chartId) ? tblId : getActiveTableId();

    const currentCharts = chartId ? [chartId] : getChartIdsInGroup(tblId);
    const defaultCharts = getChartIdsInGroup('default');

    const expandedMode = props.expandedMode && getExpandedMode() === LO_VIEW.xyPlots;
    const chartProps = expandedMode ? getExpandedChartProps() : {};

    return Object.assign({expandedMode, closeable, currentCharts, defaultCharts}, chartProps);
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
            if (expandedMode !== this.state.expandedMode || !(this.props.chartId||this.props.tblId)) {
                const ns = nextState(this.props);
                if (!isEqual(this.state.currentCharts, ns.currentCharts) ||
                    !isEqual(this.state.defaultCharts, ns.defaultCharts) ) {
                    this.setState(nextState(this.props));
                }
            }
        }
    }

    render() {
        const {currentCharts, defaultCharts, expandedMode, closeable} = this.state;
        return expandedMode ? <ExpandedView key='chart-expanded' closeable={closeable} {...this.props} {...this.state}/> :
            (
                <div style={{ display: 'flex',  height: '100%', flexGrow: 1, flexDirection: 'row', overflow: 'hidden'}}>
                    {currentCharts.map((c) => {
                        return (
                            <ChartsTableViewPanel key={c} expandable={true} chartId={c}/>
                        );
                    })}
                    {defaultCharts.map((c) => {
                        return (
                            <ChartsTableViewPanel key={'default-'+c} expandable={true} chartId={c}/>
                        );
                    })}
                </div>
            );
    }
}

ChartsContainer.propTypes = {
    expandedMode: PropTypes.bool,
    closeable: PropTypes.bool,
    chartId: PropTypes.string,
    tblId: PropTypes.string
};

function ExpandedView(props) {
    const {defaultCharts, closeable, chartId} = props;

    return (
        <div style={{ display: 'flex', height: '100%', flexGrow: 1, flexDirection: 'column', overflow: 'hidden'}}>
            <div style={{marginBottom: 3}}>
                {closeable && <CloseButton style={{display: 'inline-block', paddingLeft: 10}} onClick={() => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none)}/>}
            </div>
            {chartId && <ChartsTableViewPanel expandedMode={true} expandable={false} {...props}/>}
            {defaultCharts.map((c) => {
                if (chartId !== c) {
                    return (
                        <ChartsTableViewPanel key={'default-'+c} expandedMode={true} expandable={false} {...props} chartId={c}/>
                    );
                }
            })}
        </div>
    );
}
