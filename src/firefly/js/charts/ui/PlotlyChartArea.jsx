import React, {Component} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../../core/ReduxFlux.js';
import {get, set, cloneDeep} from 'lodash';
import shallowequal from 'shallowequal';
import {PlotlyWrapper} from './PlotlyWrapper.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';

import {dispatchChartHighlighted, dispatchChartUpdate, dispatchSetActiveTrace, getAnnotations, getChartData, usePlotlyReact} from '../ChartsCntlr.js';
import {clearChartConn, flattenAnnotations, handleTableSourceConnections, isSpectralOrder, isScatter2d} from '../ChartUtil.js';

const X_TICKLBL_PX = 60;
const TITLE_PX = 30;
const MIN_MARGIN_PX = 30; // should be enough to accommodate upper limit annotation

function adjustLayout(layout={}) {
    const hasTitle = get(layout, 'title');
    const yaxis = get(layout, 'yaxis', {});
    const hasOppositeY = get(yaxis, 'side') === 'right';

    const xaxis = get(layout, 'xaxis', {});
    const hasOppositeX = get(xaxis, 'side') === 'top';

    set(layout, 'yaxis.tickprefix', hasOppositeY ? '' : '  ');
    set(layout, 'yaxis.ticksuffix', hasOppositeY ? '  ' : '');
    set(layout, 'margin.b', hasOppositeX ? MIN_MARGIN_PX: X_TICKLBL_PX);
    set(layout, 'margin.t', hasOppositeX ? X_TICKLBL_PX: MIN_MARGIN_PX + (hasTitle ? TITLE_PX: 0));
    return layout;
}

function traceShallowCopy(trace) {
    if (usePlotlyReact) {
        return Object.assign({}, trace);
    } else {
        // whenever re-render happens, selectedpoints should be removed,
        // otherwise the style of unselected points is not cleared will stay dimmed
        return Object.assign({}, trace, {selectedpoints: null});
    }
}

export class PlotlyChartArea extends Component {

    constructor(props) {
        super(props);

        const {chartId} = this.props;
        const {data, fireflyData, mounted} = getChartData(chartId);
        if (mounted === 1) {
            handleTableSourceConnections({chartId, data, fireflyData});
        }
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
        const {chartId} = this.props;
        const {mounted} = getChartData(chartId);
        if (mounted === 0) {
            clearChartConn({chartId});
        }
    }

    getNextState() {
        const {chartId} = this.props;
        const {data, fireflyData=[], highlighted, layout, fireflyLayout={}, selected, activeTrace} = getChartData(chartId);
        return  {data, isLoading: fireflyData.some((e)=>get(e, 'isLoading')), highlighted, selected, layout, activeTrace, xyratio: fireflyLayout.xyratio, stretch: fireflyLayout.stretch};
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
        const {widthPx, heightPx, } = this.props;
        const {data=[], isLoading, highlighted, selected, layout={}, activeTrace=0, xyratio, stretch} = this.state;
        if (isLoading) {
            return (
                <div style={{position: 'relative', width: '100%', height: '100%'}}>
                    <div className='loading-mask'/>
                </div>
            );
        } else if (data.length === 0) {
            return null;
        }

        let doingResize = false;
        if (widthPx !== this.widthPx || heightPx !== this.heightPx) {
            this.widthPx = widthPx;
            this.heightPx = heightPx;
            doingResize = true;
        }
        const showlegend = data.length > 1;

        // put the active trace after all inactive traces


        let pdata = data.reduce((rdata, e, idx) =>  {
            (idx !== activeTrace) && rdata.push(traceShallowCopy(e));
            return rdata;
        }, []);

        pdata.push(traceShallowCopy(data[activeTrace]));

        //let pdata = data.map((e) => Object.assign({}, e)); // create shallow copy of data elements to avoid sharing x,y,z arrays
        let annotations = getAnnotations(this.props.chartId);
        if (!data[activeTrace] || isScatter2d(get(data[activeTrace], 'type', ''))) {
            // highlight makes sense only for scatter at the moment
            // 3d scatter highlight and selected appear in front - not good: disable for the moment
            if (selected) {
                pdata = pdata.concat([traceShallowCopy(selected)]);
                if (annotations.length>0) {
                    const selectedAnnotations = flattenAnnotations(get(selected, 'firefly.annotations'));
                    if (selectedAnnotations.length>0) { annotations = annotations.concat(selectedAnnotations); }
                }
            }
            if (highlighted) {
                pdata = pdata.concat([traceShallowCopy(highlighted)]);
                if (annotations.length>0) {
                    const highlightedAnnotations = flattenAnnotations(get(highlighted, 'firefly.annotations'));
                    annotations = annotations.concat(highlightedAnnotations);
                }
            }
        }
        const {chartWidth, chartHeight} = calculateChartSize(widthPx, heightPx, xyratio, stretch);
        //const playout = Object.assign({showlegend}, adjustLayout(layout), {width: chartWidth, height: chartHeight, annotations});
        const playout = cloneDeep(Object.assign({showlegend}, adjustLayout(layout), {width: chartWidth, height: chartHeight, annotations}))

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

/**
 * plotly chart click callback, updata chart highlight in case the click falls on active trace or selected trace
 * @param chartId
 * @returns {Function}
 */
function onClick(chartId) {
    return (evData) => {
        // for scatter, points array has one element, for the top trace only,
        // we should have active trace, its related selected, and its highlight traces on top
        const {activeTrace=0, curveNumberMap} = getChartData(chartId);
        const curveNumber = get(evData.points, `${0}.curveNumber`);
        const highlighted = get(evData.points, `${0}.pointNumber`);
        const curveName = get(evData.points, `${0}.data.name`);

        const traceNum = curveNumber >= curveNumberMap.length ? curveNumber : curveNumberMap[curveNumber];
        if (traceNum !== activeTrace && traceNum < curveNumberMap.length) {
            activeTrace = traceNum;
            dispatchSetActiveTrace({chartId, activeTrace});
        }

        // traceNum is related to any of trace data or SELECTED trace or HIGHLIGHTED trace
        // if traceNUm is between [0, curveNumberMap.length-1], then curveNumber is mapped to one of the trace data
        // if traceNum is greater than curveNumberMap.length-1, then curveNumber is mapped to either SELECTED trace or HIGHLIGHTED trace
        if (traceNum === activeTrace || traceNum === curveNumberMap.length) {
            dispatchChartHighlighted({
                chartId,
                traceNum,
                traceName: curveName,
                highlighted,
                chartTrigger: true
            });
        }
    };
}

/**
 * plotly chart, select area callback, update chart by collecting all points on active trace enclosed by selected area
 * @param chartId
 * @returns {Function}
 */
function onSelect(chartId) {
    return (evData) => {
        if (evData) {
            let points = undefined;
            // this is for range selection only... lasso selection is not implemented yet.
            const {activeTrace=0, curveNumberMap}  = getChartData(chartId);
            const [xMin, xMax] = get(evData, 'range.x', []);
            const [yMin, yMax] = get(evData, 'range.y', []);
            if (xMin !== xMax && yMin !== yMax && curveNumberMap) {
                points = get(evData, 'points', []);
                if (!isSpectralOrder(chartId)) {
                    points.filter((o) => curveNumberMap[o.curveNumber] === activeTrace);
                }
                points = points.map((o) => [o.pointNumber, curveNumberMap[o.curveNumber]]);
            }
            if (points) {
                const {data, activeTrace=0} = getChartData(chartId);
                const type = get(data, [activeTrace, 'type'], 'scatter');
                // points are populated only for scatter2d, not for heatmap
                if (isScatter2d(type) && points.length < 1) {
                    showInfoPopup((<div>No active trace points in the selection area.</div>), 'Warning');
                } else {
                    dispatchChartUpdate({
                        chartId,
                        changes: {'selection': {points, range: {x: [xMin, xMax], y: [yMin, yMax]}}}
                    });
                }
            }
        } else {
            const {selection} = getChartData(chartId);
            if (selection) {
                // we need some change in plotly data or layout to remove the selection box - setting layout.dummy
                dispatchChartUpdate({chartId, changes: {'selection': undefined, 'layout.dummy': 0}});
            }
        }
    };
}
