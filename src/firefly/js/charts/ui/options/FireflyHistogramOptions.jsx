import React from 'react';
import {get, isUndefined, set, reverse} from 'lodash';

import {BasicOptionFields, OptionTopBar, basicFieldReducer, submitChanges} from './BasicOptions.jsx';
import {HistogramOptions} from '../HistogramOptions.jsx';
import {getChartData} from '../../ChartsCntlr.js';

import {SimpleComponent} from '../../../ui/SimpleComponent.jsx';
import {getColValStats} from '../../TableStatsCntlr.js';

export class FireflyHistogramOptions extends SimpleComponent {

    getNextState() {
        const {chartId} = this.props;
        const {activeTrace:cActiveTrace} = getChartData(chartId);
        // activeTrace is passed via property, when used from NewTracePanel
        const activeTrace = isUndefined(this.props.activeTrace) ? cActiveTrace : this.props.activeTrace;
        return {activeTrace};
    }

    render() {
        const {chartId} = this.props;
        const {tablesources, layout, data, activeTrace:cActiveTrace=0} = getChartData(chartId);
        const activeTrace = isUndefined(this.props.activeTrace) ? cActiveTrace : this.props.activeTrace;
        const groupKey = this.props.groupKey || `${chartId}-ffhist-${activeTrace}`;
        const tablesource = get(tablesources, [cActiveTrace]);
        const tbl_id = get(tablesource, 'tbl_id');
        const colValStats = getColValStats(tbl_id);

        const histogramParams = toHistogramOptions(chartId, activeTrace);

        const basicFields = BasicOptionFields({layout, data, activeTrace});
        const basicFieldsReducer = basicFieldReducer({data, layout, activeTrace, tablesources});
        return (
            <div style={{padding:'0 5px 7px'}}>
                {isUndefined(this.props.activeTrace) && <OptionTopBar {...{groupKey, activeTrace, chartId, tbl_id, submitChangesFunc: submitChangesFFHistogram}}/>}
                <HistogramOptions {...{key: activeTrace, groupKey, histogramParams, colValStats, basicFields, basicFieldsReducer}}/>
            </div>
        );
    }
}

export function submitChangesFFHistogram({chartId, activeTrace, fields, tbl_id}) {
    const changes = histogramOptionsToChanges(chartId, activeTrace, fields, tbl_id);
    submitChanges({chartId, fields: changes, tbl_id});
}

function histogramOptionsToChanges(chartId, activeTrace, fields, tbl_id) {
    const {layout} = getChartData(chartId);
    const changes = {};
    changes[`fireflyData.${activeTrace}.dataType`] = 'fireflyHistogram';
    changes[`fireflyData.${activeTrace}.tbl_id`] = tbl_id;
    const options = {};
    Object.entries(fields).forEach( ([k,v]) => {
        if (k.startsWith('data') || k.startsWith('layout')) {
            changes[k] = v;
        } else {
            options[`${k}`] = v;
        }
    });
    changes[`fireflyData.${activeTrace}.options`] = options;

    ['x', 'y'].forEach((a) => {
        const opts = get(fields, a, '');
        const range = get(layout, `${a}axis.range`);
        if (opts.includes('flip')) {
            if (range) {
                if (range[0]<range[1]) changes[`layout.${a}axis.range`] = reverse(range);
            } else {
                changes[`layout.${a}axis.autorange`] = 'reversed';
            }

        } else {
            if (range) {
                if (range[1]<range[0]) changes[`layout.${a}axis.range`] = reverse(range);
            } else {
                changes[`layout.${a}axis.autorange`] = true;
            }
        }
        if (opts.includes('opposite')) {
            changes[`layout.${a}axis.side`] = (a==='x'?'top':'right');
        } else {
            changes[`layout.${a}axis.side`] = (a==='x'?'bottom':'left');
        }
        changes[`layout.${a}axis.type`]  = opts.includes('log') ? 'log' : 'linear';
    });
    changes['activeTrace'] = activeTrace;
    return changes;
}

function toHistogramOptions(chartId, activeTrace=0) {
    const {fireflyData, layout} = getChartData(chartId);
    const options = get(fireflyData, `${activeTrace}.options`, {});
    const histogramOptions = {};
    Object.entries(options).forEach( ([k,v]) => {
        set(histogramOptions, k, v);
    });

    ['x', 'y'].forEach((a) => {
        const opts = [];
        if (get(layout, `${a}axis.autorange`) === 'reversed' ||
            (get(layout, `${a}axis.range.1`) < get(layout, `${a}axis.range.0`))) {
            opts.push('flip');
        }
        if (get(layout, `${a}axis.side`) === (a==='x'?'top':'right')) {
            opts.push('opposite');
        }
        if (get(layout, `${a}axis.type`) === 'log') {
            opts.push('log');
        }
        set(histogramOptions, a, opts.toString());
    });
    return histogramOptions;
}

