import React, { useMemo } from 'react';
import PropTypes from 'prop-types';
import merge from 'lodash/merge.js';
import { ChartPanel } from './ChartPanel.jsx';

/**
 * Displays a scatter chart bound to a Firefly table.
 *
 * The chart is driven by the table identified by `tbl_id`. Columns are
 * referenced by name; Firefly keeps the plot in sync as the table is
 * filtered, sorted, or paged.
 *
 * For large row counts Firefly automatically switches from SVG `scatter`
 * to WebGL `scattergl` for performance.
 */
export function ScatterChart({ chartId, tbl_id, x_axis, y_axis, chartData, options, events, ...props }) {
    const mergedChartData = useMemo(() => {
        const defaults = {
            data: [{
                tbl_id,
                type: 'scatter',
                mode: 'markers',
                x:    `tables::${x_axis}`,
                y:    `tables::${y_axis}`,
            }],
            layout: {
                xaxis: { title: { text: x_axis } },
                yaxis: { title: { text: y_axis } },
            },
        };
        return chartData ? merge({}, defaults, chartData) : defaults;
    }, [tbl_id, x_axis, y_axis, chartData]); // eslint-disable-line react-hooks/exhaustive-deps

    return (
        <ChartPanel
            chartId={chartId}
            chartData={mergedChartData}
            options={options}
            events={events}
            {...props}
        />
    );
}

ScatterChart.propTypes = {
    /**
     * Unique ID for this chart in the Firefly store. Auto-generated if omitted.
     */
    chartId: PropTypes.string,

    /**
     * ID of the Firefly table to plot. The chart stays in sync as rows are
     * filtered, highlighted, or selected.
     */
    tbl_id: PropTypes.string.isRequired,

    /**
     * Table column name for the X axis.
     */
    x_axis: PropTypes.string.isRequired,

    /**
     * Table column name for the Y axis.
     */
    y_axis: PropTypes.string.isRequired,

    /**
     * Partial Plotly/Firefly chart object merged on top of the defaults built
     * from the required props. Use this for anything not covered by the
     * surfaced props: marker style, trace mode, axis scale, chart title, etc.
     *
     * @example — log scale + custom marker color
     * ```js
     * chartData={{
     *   data: [{ mode: 'lines+markers', marker: { color: 'steelblue', size: 5 } }],
     *   layout: {
     *     title: { text: 'Sky Positions' },
     *     xaxis: { type: 'log' },
     *   },
     * }}
     * ```
     */
    chartData: PropTypes.shape({
        data:   PropTypes.array,
        layout: PropTypes.object,
    }),

    /**
     * Display options forwarded to `ChartPanel` (`showToolbar`, `expandable`, etc.).
     */
    options: PropTypes.object,

    /**
     * Event callbacks forwarded to `ChartPanel` (`onHighlight`, `onSelect`, `onLoaded`).
     */
    events: PropTypes.object,
};
