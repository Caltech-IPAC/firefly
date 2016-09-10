/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import shallowequal from 'shallowequal';


import {flux} from '../../Firefly.js';

import {LO_VIEW, LO_MODE, dispatchSetLayoutMode, getExpandedMode} from '../../core/LayoutCntlr.js';
import {CloseButton} from '../../ui/CloseButton.jsx';

import {ChartsTableViewPanel} from './ChartsTableViewPanel.jsx';
import {getExpandedChartProps} from '../ChartsCntlr.js';
import {getChartIdsWithPrefix} from '../ChartUtil.js';


function nextState(props) {
    const {closeable, chartId, tblId, chartType, help_id} = props;
    const expandedMode = props.expandedMode && getExpandedMode() === LO_VIEW.xyPlots;
    const chartProps = expandedMode ? getExpandedChartProps() : {chartId, tblId, chartType, help_id};
    const defaultCharts = getChartIdsWithPrefix('default');
    return Object.assign({expandedMode,closeable, defaultCharts}, chartProps);
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
            if (expandedMode !== this.state.expandedMode ||
                getChartIdsWithPrefix('default').length !== this.state.defaultCharts.length) {
                this.setState(nextState(this.props));
            }
        }
    }

    render() {
        const {defaultCharts, expandedMode, closeable} = this.state;
        return expandedMode ? <ExpandedView key='chart-expanded' closeable={closeable} {...this.props} {...this.state}/> :
            (
                <div style={{ display: 'flex',  height: '100%', flexGrow: 1, flexDirection: 'row', overflow: 'hidden'}}>
                    <ChartsTableViewPanel key='xyplot' {...this.props} {...this.state} chartType='scatter'/>
                    <ChartsTableViewPanel key='histogram' {...this.props} {...this.state} chartType='histogram'/>
                    {defaultCharts.map((c) => {
                        const type = c.includes('xyplot') ? 'scatter' : 'histogram';
                        return (
                            <ChartsTableViewPanel key={'default-'+c} {...this.props} {...this.state} deletable={true} chartType={type} chartId={c}/>
                        );
                    })}
                </div>
            );
    }
}

ChartsContainer.propTypes = {
    expandedMode: PropTypes.bool,
    closeable: PropTypes.bool
};

function ExpandedView(props) {
    const {defaultCharts, closeable} = props;
    return (
        <div style={{ display: 'flex', height: '100%', flexGrow: 1, flexDirection: 'column', overflow: 'hidden'}}>
            <div style={{marginBottom: 3}}>
                {closeable && <CloseButton style={{display: 'inline-block', paddingLeft: 10}} onClick={() => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none)}/>}
            </div>
            <ChartsTableViewPanel expandedMode={true} expandable={false} chartType={'scatter'} {...props} />
            {defaultCharts.map((c) => {
                if (props.chartId !== c) {
                    const type = c.includes('xyplot') ? 'scatter' : 'histogram';
                    return (
                        <ChartsTableViewPanel key={'default-expanded-'+c} {...props} expandedMode={true}
                                              expandable={false} chartType={type} chartId={c}/>
                    );
                }
            })}
        </div>
    );

}
