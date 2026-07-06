import React, { useMemo } from 'react';
import PropTypes from 'prop-types';
import merge from 'lodash/merge.js';
import { ChartPanel } from './ChartPanel.jsx';

/**
 * Displays a histogram bound to a Firefly table column.
 *
 * Binning is computed server-side and kept in sync as the table is
 * filtered or updated. Two algorithms are supported:
 * - **Fixed-size bins** (default): uniform bins controlled by `numBins` or `binWidth`.
 * - **Bayesian Blocks**: adaptive binning that finds natural breaks in the data.
 *
 * To configure binning or axes, pass them via `chartData`:
 * ```js
 * chartData={{
 *   data: [{ firefly: { options: { numBins: 20, algorithm: 'bayesianBlocks' } } }],
 *   layout: { xaxis: { type: 'log' }, title: { text: 'My Histogram' } },
 * }}
 * ```
 */
export function HistogramChart({ chartId, tbl_id, column, chartData, options, events, ...props }) {
    const mergedChartData = useMemo(() => {
        const defaults = {
            data: [{
                type: 'fireflyHistogram',
                firefly: {
                    tbl_id,
                    options: {
                        columnOrExpr:          column,
                        algorithm:             'fixedSizeBins',
                        fixedBinSizeSelection: 'numBins',
                        numBins:               50,
                    },
                },
            }],
            layout: {
                xaxis: {},
                yaxis: {},
            },
        };
        return chartData ? merge({}, defaults, chartData) : defaults;
    }, [tbl_id, column, chartData]); // eslint-disable-line react-hooks/exhaustive-deps

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

HistogramChart.propTypes = {
    /**
     * Unique ID for this chart in the Firefly store. Auto-generated if omitted.
     */
    chartId: PropTypes.string,

    /**
     * ID of the Firefly table to plot. The histogram stays in sync as rows
     * are filtered or updated.
     */
    tbl_id: PropTypes.string.isRequired,

    /**
     * Column name or expression to histogram.
     * @example 'w1mpro'
     * @example 'log10(flux)'
     */
    column: PropTypes.string.isRequired,

    /**
     * Partial Plotly/Firefly chart object merged on top of the defaults built
     * from the required props. Use this for binning options, axis scale,
     * chart title, and anything else not covered by the surfaced props.
     *
     * The `data[0].firefly.options` object accepts:
     * - `numBins` — number of bins (default: `50`)
     * - `binWidth` — fixed bin width (alternative to `numBins`)
     * - `fixedBinSizeSelection` — `'numBins'` | `'binWidth'`
     * - `algorithm` — `'fixedSizeBins'` (default) | `'bayesianBlocks'`
     * - `falsePositiveRate` — for Bayesian Blocks (0.01–0.5)
     * - `minCutoff` / `maxCutoff` — restrict the data range
     *
     * @example — 20 bins on a log X axis
     * ```js
     * chartData={{
     *   data: [{ firefly: { options: { numBins: 20 } } }],
     *   layout: { xaxis: { type: 'log' }, title: { text: 'Flux Distribution' } },
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
