import './ChartPanel.css';
import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../../Firefly.js';
import {get} from 'lodash';
import {PlotlyWrapper} from './PlotlyWrapper.jsx';

import {dispatchChartUpdate, dispatchChartHighlighted, getChartData} from '../ChartsCntlr.js';

export class PlotlyChartArea extends PureComponent {

    constructor(props) {
        super(props);
        this.state = this.getNextState();
        this.afterRedraw = this.afterRedraw.bind(this);
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
        const {data, highlighted, layout, selected} = getChartData(chartId);
        return  {data, highlighted, selected, layout};
    }

    storeUpdate() {
        if (!this.isUnmounted) {
            this.setState(this.getNextState());
        }
    }

    afterRedraw(chart, pl) {
        const {chartId} = this.props;
        chart.on('plotly_click', onClick(chartId));
        chart.on('plotly_selected', onSelect(chartId));
    }

    render() {
        const {widthPx, heightPx} = this.props;
        const {data=[], highlighted, selected, layout={}} = this.state;
        const doingResize= (layout && (layout.width!==widthPx || layout.height!==heightPx));
        Object.assign(layout, {width: widthPx, height: heightPx});
        const showlegend = data.length > 1;
        let pdata = data;
        // TODO: change highlight or selected without forcing new plot
        pdata = selected ? pdata.concat([selected]) : pdata;
        pdata = highlighted ? pdata.concat([highlighted]) : pdata;
        const playout = Object.assign({showlegend}, layout);

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
