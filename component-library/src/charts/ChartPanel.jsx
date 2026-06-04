import React, { useId, useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import { ChartPanel as FireflyChartPanel } from 'firefly/charts/ui/ChartPanel.jsx';
import { useStoreConnector } from 'firefly/ui/SimpleComponent.jsx';
import { dispatchChartAdd, dispatchChartRemove, getChartData } from 'firefly/charts/ChartsCntlr.js';

/**
 * Displays a Firefly/Plotly chart.
 *
 * Features:
 * - **Plotly rendering**: renders any Plotly-compatible `data` and `layout`.
 * - **Table binding**: trace data can reference Firefly table columns via `'tables::colName'`.
 * - **Toolbar**: zoom, pan, select, filter, and save controls.
 * - **Full-screen expand**: expand the chart to fill the viewport.
 * - **Event callbacks**: subscribe to highlight, select, and load events via the `events` prop.
 */
export function ChartPanel({ chartId: chartId_prop, chartData, options = {}, events = {}, ...props }) {
    const generatedId = useId();
    const chartId = chartId_prop ?? `chart-${generatedId}`;

    useEffect(() => {
        if (chartData) {
            dispatchChartAdd({ chartId, chartType: 'plot.ly', ...chartData });
        }
        return () => dispatchChartRemove(chartId);
    }, [chartId]); // eslint-disable-line react-hooks/exhaustive-deps

    useChartEventHandlers(chartId, events);

    return (
        <FireflyChartPanel
            chartId={chartId}
            {...options}
            {...props}
        />
    );
}

// Subscribes to store changes and fires event callbacks when chart state changes.
// Skips the subscription entirely when no callbacks are provided.
function useChartEventHandlers(chartId, events) {
    const hasEvents = Object.keys(events).length > 0;
    const chart     = useStoreConnector(() => hasEvents ? getChartData(chartId) : null, [chartId, hasEvents]);
    const prevRef   = useRef(null);

    useEffect(() => {
        const prev = prevRef.current;
        const { onHighlight, onSelect, onLoaded } = events;

        if (chart) {
            if (prev && chart.highlighted !== prev.highlighted) {
                onHighlight?.(chart.highlighted, chartId);
            }
            if (prev && chart.selected !== prev.selected) {
                onSelect?.(chart.selected, chartId);
            }
            // loading transitions to false on each trace: fireflyData[n].isLoading
            const wasLoading = prev?.fireflyData?.some((t) => t?.isLoading);
            const isLoading  = chart.fireflyData?.some((t) => t?.isLoading);
            if (wasLoading && !isLoading) {
                onLoaded?.(chart, chartId);
            }
        }

        prevRef.current = chart ? {
            highlighted: chart.highlighted,
            selected:    chart.selected,
            fireflyData: chart.fireflyData,
        } : undefined;
    }, [chart]); // eslint-disable-line react-hooks/exhaustive-deps
}

ChartPanel.propTypes = {
    /**
     * Unique ID for this chart in the Firefly store. Auto-generated if omitted.
     */
    chartId: PropTypes.string,

    /**
     * Chart data dispatched to the Firefly store on mount.
     * Shape mirrors the Plotly chart object plus optional Firefly extensions:
     * - `data` — array of Plotly trace objects; columns referenced as `'tables::colName'`
     * - `layout` — Plotly layout object (axes, title, etc.)
     * - `groupId` — chart group (default: `'main'`)
     * - `deletable` — show a delete button on the chart
     */
    chartData: PropTypes.shape({
        data:     PropTypes.array,
        layout:   PropTypes.object,
        groupId:  PropTypes.string,
        deletable: PropTypes.bool,
    }),

    /**
     * Display options forwarded to Firefly's ChartPanel.
     *
     * - `showToolbar` — show the chart toolbar. @default true
     * - `expandable` — show the full-screen expand button. @default true
     * - `deletable` — show a delete (×) button on the chart
     */
    options: PropTypes.shape({
        /** Show the chart toolbar. @default true */
        showToolbar: PropTypes.bool,
        /** Show the full-screen expand button. @default true */
        expandable:  PropTypes.bool,
        /** Show a delete button on the chart. */
        deletable:   PropTypes.bool,
    }),

    /**
     * Callbacks fired on chart interactions. All keys are optional.
     * Each callback also receives `chartId` as its last argument.
     *
     * - `onHighlight(highlighted, chartId)` — a data point was highlighted
     * - `onSelect(selected, chartId)` — data points were selected
     * - `onLoaded(chartData, chartId)` — chart data finished loading
     */
    events: PropTypes.shape({
        /** Called when a data point is highlighted. `(highlighted, chartId) => void` */
        onHighlight: PropTypes.func,
        /** Called when data points are selected. `(selected, chartId) => void` */
        onSelect:    PropTypes.func,
        /** Called when chart data finishes loading. `(chartData, chartId) => void` */
        onLoaded:    PropTypes.func,
    }),
};
