/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';

import {get, isBoolean, isUndefined} from 'lodash';
import {flux} from '../../Firefly.js';

import {FilterEditor} from '../../tables/ui/FilterEditor.jsx';
import * as TblUtil from '../../tables/TableUtil.js';
import * as TablesCntlr from '../../tables/TablesCntlr.js';
import * as ChartsCntlr from '../ChartsCntlr.js';
import * as TableStatsCntlr from '../TableStatsCntlr.js';


/**
 * Get properties defining chart from the store.
 * @param chartId
 * @returns {{chartId: string, tblId: string, tableModel: TableModel, tblStatsData: *, chartData: *, deletable: *}}
 */
export function getChartProperties(chartId) {
    const chartData =  ChartsCntlr.getChartData(chartId);
    // one data element - one related tbl id at this time
    const tblId = ChartsCntlr.getRelatedTblIds(chartId)[0];
    const tableModel = TblUtil.getTblById(tblId);
    const tblStatsData = flux.getState()[TableStatsCntlr.TBLSTATS_DATA_KEY][tblId];
    const deletable = isBoolean(deletable) ? deletable : ChartsCntlr.getNumCharts(tblId) > 1;
    return {chartId, tblId, tableModel, tblStatsData, chartData, deletable};
}

export function updateOnStoreChange(oldChartProperties) {
    const tblId = oldChartProperties.tblId;
    return TblUtil.isFullyLoaded(tblId) &&
        (oldChartProperties.tableModel !== TblUtil.getTblById(tblId) ||
         oldChartProperties.chartData !== ChartsCntlr.getChartData(oldChartProperties.chartId));
}

export class FilterEditorWrapper extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            sortInfo: ''
        };
    }

    shouldComponentUpdate(np, ns) {
        const tblId = get(np.tableModel, 'tbl_id');
        return ns.sortInfo !== this.state.sortInfo || tblId !== get(this.props.tableModel, 'tbl_id') ||
            (TblUtil.isFullyLoaded(tblId) && np.tableModel !== this.props.tableModel); // to avoid flickering when changing the filter
    }

    render() {
        const {tableModel} = this.props;
        const {sortInfo} = this.state;
        return (
            <div style={{width: 350, height: 'calc(100% - 20px)'}}>
                <FilterEditor
                    columns={get(tableModel, 'tableData.columns', [])}
                    selectable={false}
                    filterInfo={get(tableModel, 'request.filters')}
                    sortInfo={sortInfo}
                    onChange={(obj) => {
                            if (!isUndefined(obj.filterInfo)) {
                                const newRequest = Object.assign({}, tableModel.request, {filters: obj.filterInfo});
                                TablesCntlr.dispatchTableFilter(newRequest, 0);
                            } else if (!isUndefined(obj.sortInfo)) {
                                this.setState({sortInfo: obj.sortInfo});
                            }
                          } }/>
            </div>
        );
    }
}

FilterEditorWrapper.propTypes = {
    toggleFilters : PropTypes.func,
    tableModel : PropTypes.object
};


