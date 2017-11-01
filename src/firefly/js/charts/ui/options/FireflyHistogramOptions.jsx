import React from 'react';
import {get, isUndefined, set} from 'lodash';

import {BasicOptionFields, basicFieldReducer, submitChanges} from './BasicOptions.jsx';
import {HistogramOptions} from '../HistogramOptions.jsx';
import {getChartData} from '../../ChartsCntlr.js';

import {SimpleComponent} from '../../../ui/SimpleComponent.jsx';
import {getColValStats} from '../../TableStatsCntlr.js';

export class FireflyHistogramOptions extends SimpleComponent {

    getNextState() {
        const {chartId} = this.props;
        const {activeTrace:cActiveTrace=0} = getChartData(chartId);
        // activeTrace is passed via property, when used from NewTracePanel
        const activeTrace = isUndefined(this.props.activeTrace) ? cActiveTrace : this.props.activeTrace;
        return {activeTrace};
    }

    render() {
        const {chartId, tbl_id:tblIdProp} = this.props;
        const {tablesources, activeTrace:cActiveTrace=0} = getChartData(chartId);
        const activeTrace = isUndefined(this.props.activeTrace) ? cActiveTrace : this.props.activeTrace;
        const groupKey = this.props.groupKey || `${chartId}-ffhist-${activeTrace}`;
        const tablesource = get(tablesources, [cActiveTrace], tblIdProp && {tbl_id: tblIdProp});
        const tbl_id = get(tablesource, 'tbl_id');
        const colValStats = getColValStats(tbl_id);

        const histogramParams = toHistogramOptions(chartId, activeTrace);

        const basicFields = <BasicOptionFields {...{activeTrace, groupKey, xNoLog: true}}/>;
        const basicFieldsReducer = basicFieldReducer({chartId, activeTrace});
        return (
            <div style={{padding:'0 5px 7px'}}>
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
    const changes = {};
    changes[`fireflyData.${activeTrace}.dataType`] = 'fireflyHistogram';
    changes[`fireflyData.${activeTrace}.tbl_id`] = tbl_id;
    Object.entries(fields).forEach( ([k,v]) => {
        if (['data', 'layout', 'fireflyLayout', 'activeTrace', '_'].find((s) => k.startsWith(s))) {
            changes[k] = v;
        } else {
            changes[`fireflyData.${activeTrace}.options.${k}`] = v;
        }
    });

    return changes;
}

function toHistogramOptions(chartId, activeTrace=0) {
    const {fireflyData, layout={}} = getChartData(chartId);
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

