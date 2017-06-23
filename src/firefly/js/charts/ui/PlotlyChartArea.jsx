import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../../Firefly.js';
import {get} from 'lodash';
import shallowequal from 'shallowequal';
import {PlotlyWrapper} from './PlotlyWrapper.jsx';

import {dispatchChartUpdate, dispatchChartHighlighted, getChartData} from '../ChartsCntlr.js';

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
        const {data, highlighted, layout, selected, activeTrace} = getChartData(chartId);
        return  {data, highlighted, selected, layout, activeTrace};
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
        const {widthPx, heightPx} = this.props;
        const {data=[], highlighted, selected, layout={}, activeTrace=0} = this.state;
        let doingResize = false;
        if (widthPx !== this.widthPx || heightPx !== this.heightPx) {
            this.widthPx = widthPx;
            this.heightPx = heightPx;
            doingResize = true;
        }
        const showlegend = data.length > 1;
        let pdata = data;
        // TODO: change highlight or selected without forcing new plot
        if (!data[activeTrace] || get(data[activeTrace], 'type', '').includes('scatter')) {
            // highlight makes sence only for scatter at the moment
            pdata = selected ? pdata.concat([selected]) : pdata;
            pdata = highlighted ? pdata.concat([highlighted]) : pdata;
        }
        const playout = Object.assign({showlegend}, layout, {width: widthPx, height: heightPx});
        return (
            <PlotlyWrapper newPlotCB={this.afterRedraw} data={pdata} layout={playout}
                           chartId={this.props.chartId}
                           autoDetectResizing={false}
                           doingResize={doingResize}/>
        );
    }
}

PlotlyChartArea.propTypes = {
    chartId: PropTypes.string.isRequired,
    widthPx: PropTypes.number,
    heightPx: PropTypes.number
};

function onClick(chartId) {
    return (evData) => {
        // for scatter, points array has one element, for the top trace only,
        // we should have active trace, its related selected, and its highlight traces on top
        const curveNumber = get(evData.points, `${0}.curveNumber`);
        const highlighted = get(evData.points, `${0}.pointNumber`);
        dispatchChartHighlighted({chartId, activeTrace: curveNumber, highlighted, chartTrigger: true});
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
