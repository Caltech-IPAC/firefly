import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../../Firefly.js';
import {get} from 'lodash';
import shallowequal from 'shallowequal';
import {PlotlyWrapper} from './PlotlyWrapper.jsx';

import {dispatchChartUpdate, dispatchChartHighlighted, getChartData} from '../ChartsCntlr.js';
import {isScatter2d} from '../ChartUtil.js';

export class PlotlyChartArea extends PureComponent {

    constructor(props) {
        super(props);
        this.state = this.getNextState();
        this.afterRedraw = this.afterRedraw.bind(this);
    }

    shouldComponentUpdate(np, ns) {
        const {widthPx, heightPx, chartId} = np;
        const propsEqual = widthPx === this.props.widthPx && heightPx === this.props.heightPx && chartId === this.props.chartId;
        const stateEqual = shallowequal(ns, this.state);
        return !(propsEqual && stateEqual);
    }

    componentDidMount() {
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.isUnmounted=true;
        this.removeListener && this.removeListener();
    }

    getNextState() {
        const {chartId} = this.props;
        const {data, highlighted, layout, fireflyLayout={}, selected, activeTrace} = getChartData(chartId);
        return  {data, highlighted, selected, layout, activeTrace, xyratio: fireflyLayout.xyratio, stretch: fireflyLayout.stretch};
    }

    storeUpdate() {
        if (!this.isUnmounted) {
            const nextState = this.getNextState();
            if (nextState && !shallowequal(nextState, this.state)) {
                this.setState(nextState);
            }
        }
    }

    afterRedraw(chart, pl) {
        const {chartId} = this.props;
        chart.on('plotly_click', onClick(chartId));
        chart.on('plotly_selected', onSelect(chartId));
    }

    render() {
        var {widthPx, heightPx} = this.props;
        const {data=[], highlighted, selected, layout={}, activeTrace=0, xyratio, stretch} = this.state;
        let doingResize = false;
        if (widthPx !== this.widthPx || heightPx !== this.heightPx) {
            this.widthPx = widthPx;
            this.heightPx = heightPx;
            doingResize = true;
        }
        const showlegend = data.length > 1;
        let pdata = data.map((e) => Object.assign({}, e)); // create shallow copy of data elements to avoid sharing x,y,z arrays
        if (!data[activeTrace] || isScatter2d(get(data[activeTrace], 'type', ''))) {
            // highlight makes sense only for scatter at the moment
            // 3d scatter highlight and selected appear in front - not good: disable for the moment
            pdata = selected ? pdata.concat([selected]) : pdata;
            pdata = highlighted ? pdata.concat([highlighted]) : pdata;
        }
        const {chartWidth, chartHeight} = calculateChartSize(widthPx, heightPx, xyratio, stretch);
        const playout = Object.assign({showlegend}, layout, {width: chartWidth, height: chartHeight});

        const style = {float: 'left'};
        if (chartWidth > widthPx || chartHeight > heightPx) {
            Object.assign(style, {overflow: 'auto', width: widthPx, height: heightPx});
        }

        return (
            <div style={style}>
                <PlotlyWrapper newPlotCB={this.afterRedraw} data={pdata} layout={playout}
                               chartId={this.props.chartId}
                               autoDetectResizing={false}
                               doingResize={doingResize}/>
            </div>
        );
    }
}

PlotlyChartArea.propTypes = {
    chartId: PropTypes.string.isRequired,
    widthPx: PropTypes.number,
    heightPx: PropTypes.number
};

function calculateChartSize(widthPx, heightPx, xyratio, stretch) {

    let chartWidth = undefined, chartHeight = undefined;
    if (xyratio) {
        if (stretch === 'fit') {
            chartHeight = Number(heightPx);
            chartWidth = Number(xyratio) * Number(chartHeight);
            if (chartWidth > Number(widthPx)) {
                chartHeight -= 15; // to accommodate scroll bar
            }
        } else {
            chartWidth = Number(widthPx);
            chartHeight = Number(widthPx) / Number(xyratio);
            if (chartHeight > Number(heightPx)) {
                chartWidth -= 15; // to accommodate scroll bar
            }
        }
    } else {
        chartWidth = Number(widthPx);
        chartHeight = Number(heightPx);
    }
    return {chartWidth, chartHeight};
}

function onClick(chartId) {
    return (evData) => {
        // for scatter, points array has one element, for the top trace only,
        // we should have active trace, its related selected, and its highlight traces on top
        const curveNumber = get(evData.points, `${0}.curveNumber`);
        const highlighted = get(evData.points, `${0}.pointNumber`);
        const curveName = get(evData.points, `${0}.data.name`);
        dispatchChartHighlighted({chartId, traceNum: curveNumber, traceName: curveName, highlighted, chartTrigger: true});
    };
}

function onSelect(chartId) {
    return (evData) => {
        if (evData) {
            let points = undefined;
            // this is for range selection only... lasso selection is not implemented yet.
            const {activeTrace=0}  = getChartData(chartId);
            const [xMin, xMax] = get(evData, 'range.x', []);
            const [yMin, yMax] = get(evData, 'range.y', []);
            if (xMin !== xMax && yMin !== yMax) {

                points = get(evData, 'points', []).filter((o) => {
                    return o.curveNumber === activeTrace;
                }).map((o) => {
                    return o.pointNumber;
                });
            }

            if (points) {
                dispatchChartUpdate({
                    chartId,
                    changes: {'selection': {points, range: {x: [xMin, xMax], y: [yMin, yMax]}}}
                });
            } else {
                const {selection} = getChartData(chartId);
                if (selection) {
                    // we need some change in plotly data or layout to remove the selection box - setting layout.dummy
                    dispatchChartUpdate({chartId, changes: {'selection': undefined, 'layout.dummy': 0}});
                }
            }
        }
    };
}
