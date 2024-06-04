import React, {useEffect, useRef} from 'react';
import PropTypes from 'prop-types';
import {get, set, cloneDeep} from 'lodash';
import {PlotlyWrapper} from './PlotlyWrapper.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';

import {dispatchChartHighlighted, dispatchChartUpdate, dispatchSetActiveTrace, getAnnotations, getChartData, usePlotlyReact} from '../ChartsCntlr.js';
import {clearChartConn, flattenAnnotations, handleTableSourceConnections, isSpectralOrder, isScatter2d} from '../ChartUtil.js';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';
import {Skeleton, useTheme} from '@mui/joy';

const X_TICKLBL_PX = 60;
const TITLE_PX = 30;
const MIN_MARGIN_PX = 30; // should be enough to accommodate upper limit annotation


export function PlotlyChartArea({chartId, widthPx, heightPx, thumbnail}) {

    const theme = useTheme();

    const {data=[], isLoading, highlighted, selected, layout={}, activeTrace=0, xyratio, stretch} = useStoreConnector(() => getChartState(chartId), [chartId]);

    useEffect(()=> {
        const {fireflyData} = getChartData(chartId);
        handleTableSourceConnections({chartId, data, fireflyData});
        return () => {
            if (getChartData(chartId)?.mounted === 0) {
                clearChartConn({chartId});
            }
        };
    }, [chartId]);

    const prevDim = useRef();       // save previous dimension to detect resizing
    const doingResize = widthPx !== prevDim.current?.widthPx || heightPx !== prevDim.current?.heightPx;
    prevDim.current = {widthPx, heightPx};

    // handles no data
    if (isLoading) return <Skeleton/>;
    if (data.length === 0) return null;

    // put the active trace after all inactive traces
    let pdata = data.reduce((rdata, e, idx) =>  {
        (idx !== activeTrace) && rdata.push(traceShallowCopy(e));
        return rdata;
    }, []);

    pdata.push(traceShallowCopy(data[activeTrace]));

    //let pdata = data.map((e) => Object.assign({}, e)); // create shallow copy of data elements to avoid sharing x,y,z arrays
    let annotations = getAnnotations(chartId);
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

    const showlegend = data.length > 1;
    const playout = cloneDeep(Object.assign({showlegend}, adjustLayout(layout, theme), {width: chartWidth, height: chartHeight, annotations}));

    const style = {float: 'left'};
    if (chartWidth > widthPx || chartHeight > heightPx) {
        Object.assign(style, {overflow: 'auto', width: widthPx, height: heightPx});
    }

    if (thumbnail) renderAsThumbnail(playout);

    const afterRedraw = (chart, pl) => {
        chart.on('plotly_click', onClick(chartId));
        chart.on('plotly_selected', onSelect(chartId));
    };

    return (
        <div style={style}>
            <PlotlyWrapper newPlotCB={afterRedraw} data={pdata} layout={playout}
                           chartId={chartId}
                           autoDetectResizing={false}
                           thumbnail={thumbnail}
                           doingResize={doingResize}
                           key={chartId + thumbnail}/>
        </div>
    );
}
PlotlyChartArea.propTypes = {
    chartId: PropTypes.string.isRequired,
    widthPx: PropTypes.number,
    heightPx: PropTypes.number,
    thumbnail: PropTypes.bool
};


function getChartState(chartId) {
    const {data, fireflyData=[], highlighted, layout, fireflyLayout={}, selected, activeTrace} = getChartData(chartId);
    const isLoading = fireflyData.some((e)=>get(e, 'isLoading'));
    const {xyratio, stretch} = fireflyLayout;
    return {data, isLoading, highlighted, selected, layout, activeTrace, xyratio, stretch};
}


function adjustLayout(layout={}, theme) {
    const hasTitle = get(layout, 'title');
    const yaxis = get(layout, 'yaxis', {});
    const hasOppositeY = get(yaxis, 'side') === 'right';

    const xaxis = get(layout, 'xaxis', {});
    const hasOppositeX = get(xaxis, 'side') === 'top';

    set(layout, 'yaxis.tickprefix', hasOppositeY ? '' : '  ');
    set(layout, 'yaxis.ticksuffix', hasOppositeY ? '  ' : '');
    set(layout, 'margin.b', hasOppositeX ? MIN_MARGIN_PX: X_TICKLBL_PX);
    set(layout, 'margin.t', hasOppositeX ? X_TICKLBL_PX: MIN_MARGIN_PX + (hasTitle ? TITLE_PX: 0));

    const getColorStr = (cssVarStr) => cssVarStr?.split(',')[1].slice(0,-1);  // plotly will only take a color string

    // make background same as app's background color
    const bgSurface = getColorStr(theme?.palette?.background?.surface);
    set(layout, 'paper_bgcolor', bgSurface);
    set(layout, 'plot_bgcolor', bgSurface);

    set(layout, 'font.color', getColorStr(theme?.palette?.text?.tertiary));

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

function renderAsThumbnail(layout) {
    const axisOverride = {
        showgrid: false,
        zeroline: false,
        showline: false,
        autotick: true,
        ticks: '',
        showticklabels: false
    };
    layout.xaxis = {...layout.xaxis, ...axisOverride};
    layout.yaxis = {...layout.yaxis, ...axisOverride};
    layout.margin = {l:0, r:0, b:0, t:20, pad: 0};
    layout.titlefont = {...(layout.titlefont||{}), size: 9};
    layout.showlegend = false;
}

function calculateChartSize(widthPx, heightPx, xyratio, stretch) {

    let chartWidth, chartHeight;
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
            dispatchSetActiveTrace({chartId, activeTrace: traceNum});
        }

        // traceNum is related to any of trace data or SELECTED trace or HIGHLIGHTED trace
        // if traceNUm is between [0, curveNumberMap.length-1], then curveNumber is mapped to one of the trace data
        // if traceNum is greater than curveNumberMap.length-1, then curveNumber is mapped to either SELECTED trace or HIGHLIGHTED trace
        if (traceNum <= curveNumberMap.length) {
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
 *
 * For 2d scatter, when other trace points are selected and no active trace points are in the selection area,
 * the active trace is changed.
 *
 * @param chartId
 * @returns {Function}
 */
function onSelect(chartId) {
    return (evData) => {
        if (evData) {
            let points = undefined;
            const {activeTrace=0, curveNumberMap}  = getChartData(chartId);
            // this is for last range selection only, and should not be used with multi-area selections;
            // lasso selection is not implemented yet.
            const [xMin, xMax] = get(evData, 'range.x', []);
            const [yMin, yMax] = get(evData, 'range.y', []);
            if (xMin !== xMax && yMin !== yMax && curveNumberMap) {
                points = get(evData, 'points', []);
                points = points.map((o) => [o.pointNumber, curveNumberMap[o.curveNumber]]);
                let newActiveTrace = activeTrace;
                if (!isSpectralOrder(chartId)) {
                    // selected points must belong to the active trace
                    // if no active trace points are selected,
                    // find the trace with the most points in the selection area and make it active
                    let activeTracePoints = points.filter(([, c]) => c === activeTrace);
                    if (activeTracePoints.length < 1) {
                        // find the curve with max points in the selection area
                        const freqByCurveMap = {};
                        let curveWithMaxPts = activeTrace;
                        let maxPts = 0;
                        points.forEach(([, c]) => {
                            if (freqByCurveMap[c] == null) {
                                freqByCurveMap[c] = 0;
                            }
                            freqByCurveMap[c]++;
                            if (maxPts < freqByCurveMap[c]) {
                                maxPts = freqByCurveMap[c];
                                curveWithMaxPts = c;
                            }
                        });
                        if (curveWithMaxPts !== activeTrace) {
                            activeTracePoints = points.filter(([, c]) => c === curveWithMaxPts);
                            newActiveTrace = curveWithMaxPts;
                        }
                    }
                    points = activeTracePoints;
                }
                const {data} = getChartData(chartId);
                const traceData = data?.[newActiveTrace];
                const type = traceData?.type || 'scatter';
                // points are populated only for scatter2d, not for heatmap
                if (isScatter2d(type)) {
                    if (points.length < 1) {
                        showInfoPopup((<div>No points in the selection area.</div>), 'Warning');
                        return;
                    }
                } else {
                    // heatmap-like traces that support selection
                    if (traceData?.z && traceData.x && traceData.y) {
                        // check if at least one point is in the selection area
                        let ptInRange = false;
                        const {x, y, z} = traceData;
                        for (let i=0; i<x.length; i++) {
                            if (z[i] != null &&
                                xMin < x[i] && x[i] < xMax &&
                                yMin < y[i] && y[i] < yMax) {
                                ptInRange = true;
                                break;
                            }
                        }
                        if (!ptInRange) {
                            showInfoPopup((<div>Active trace does not overlap with the selection area.</div>), 'Warning');
                            return;
                        }
                    }
                }
                // trace change here removes the selection box
                // it needs to be restored in the following update
                if (newActiveTrace !== activeTrace) {
                    dispatchSetActiveTrace({chartId, activeTrace: newActiveTrace});
                }
                const selections = evData?.selections ?? [];
                dispatchChartUpdate({
                    chartId,
                    changes: {
                        selection: {
                            multiArea: selections.length > 1,
                            points,
                            range: {x: [xMin, xMax], y: [yMin, yMax]}
                        },
                        'layout.selections': selections
                    }
                });
            }
        } else {
            const {selection} = getChartData(chartId);
            if (selection) {
                // we need some change in plotly data or layout to remove the selection box - setting layout.dummy
                dispatchChartUpdate({chartId, changes: {selection: undefined}});
            }
        }
    };
}
