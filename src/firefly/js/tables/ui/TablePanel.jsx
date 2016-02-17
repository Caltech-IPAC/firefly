/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import sCompare from 'react-addons-shallow-compare';
import {isEmpty, get, cloneDeep} from 'lodash';

import * as TblUtil from '../TableUtil.js';
import {Table} from '../Table.js';
import {TablePanelOptions} from './TablePanelOptions.jsx';
import {BasicTable} from './BasicTable.jsx';
import {RemoteTableStore, TableStore} from '../TableStore.js';
import {SelectInfo} from '../SelectInfo.js';

import LOADING from 'html/images/gxt/loading.gif';


function prepareTableData(tableModel) {
    if (!tableModel.tableData.columns) return {};
    const {sortInfo, selectionInfo, filterInfo} = tableModel;
    const {startIdx, endIdx, hlRowIdx, currentPage, pageSize,totalPages} = TblUtil.gatherTableState(tableModel);
    var data = [];
    if ( Table.newInstance(tableModel).has(startIdx, endIdx) ) {
        data = tableModel.tableData.data.slice(startIdx, endIdx);
    } else {
        Object.assign(tableModel.request, {startIdx, pageSize});
        //TblCntlr.dispatchFetchTable(tableModel.request, highlightedRow);
    }
    var tableRowCount = data.length;

    return {startIdx, hlRowIdx, currentPage, pageSize,totalPages, tableRowCount, sortInfo, selectionInfo, filterInfo, data};
}

function ensureColumns(tableModel, columns) {
    if (isEmpty(columns)) {
        return cloneDeep(get(tableModel, 'tableData.columns', []));
    } else {
        return columns;
    }
}

export class TablePanel extends React.Component {
    constructor(props) {
        super(props);
        this.storeUpdate = this.storeUpdate.bind(this);
        this.toggleOptions = this.toggleOptions.bind(this);
        this.onOptionUpdate = this.onOptionUpdate.bind(this);

        if (props.tbl_id) {
            this.tableStore = RemoteTableStore.newInstance(props.tbl_id, this.storeUpdate);
        } else if (props.tableModel) {
            this.tableStore = TableStore.newInstance(props.tableModel, this.storeUpdate);
        }
        const columns = ensureColumns(this.tableStore.tableModel);
        this.state = {
            tableModel:this.tableStore.tableModel,
            columns,
            showOptions: false,
            showUnits: true
        };
    }

    componentWillUnmount() {
        this.props.tableStore && this.props.tableStore.onUnmount();
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
    }

    onOptionUpdate({pageSize, columns, showUnits}) {
        if (pageSize) {
            this.tableStore.onPageSizeChange(pageSize);
        }
        if(columns) {
            columns = ensureColumns(this.state.tableModel, columns);
            this.setState({columns});
        }
        if (showUnits !== undefined) {
            this.setState({showUnits});
        }
    }

    storeUpdate(state) {
        this.setState(state);
    }

    toggleOptions() {
        this.setState({showOptions: !this.state.showOptions});
    }

    render() {
        var {tableModel, columns, showOptions, showUnits} = this.state;
        const {selectable} = this.props;
        if (isEmpty(columns) || isEmpty(tableModel)) return false;
        const {startIdx, hlRowIdx, currentPage, pageSize, totalPages, tableRowCount, selectionInfo, data} = prepareTableData(tableModel);
        const selectInfo = SelectInfo.newInstance(selectionInfo, startIdx);

        const showLoading = !TblUtil.isTableLoaded(tableModel);
        const rowFrom = startIdx + 1;
        const rowTo = startIdx+tableRowCount;

        return (
            <div className='TablePanel__wrapper'>
                <div role='toolbar'>
                    <div className='group'>
                        <button style={{width:70}}>Download</button>
                    </div>
                    <div className='group'>
                        <button onClick={() => this.tableStore.gotoPage(1)} className='paging_bar first' title='First Page'/>
                        <button onClick={() => this.tableStore.gotoPage(currentPage - 1)} className='paging_bar previous'  title='Previous Page'/>
                        <input onClick={(e) => e.target.select()} onChange={(e) => this.tableStore.gotoPage(e.target.value)} name='pageNo' size="2" value={currentPage}/> <div style={{fontSize: 'smaller', marginTop: '5px'}} >&nbsp; of {totalPages}</div>
                        <button onClick={() => this.tableStore.gotoPage(currentPage + 1)} className='paging_bar next'  title='Next Page'/>
                        <button onClick={() => this.tableStore.gotoPage(totalPages)} className='paging_bar last'  title='Last Page'/>
                        <div style={{fontSize: 'smaller', marginTop: '5px'}} > &nbsp; ({rowFrom.toLocaleString()} - {rowTo.toLocaleString()} of {tableModel.totalRows.toLocaleString()})</div>
                        {showLoading ? <img style={{width:14,height:14,marginTop: '3px'}} src={LOADING}/> : false}
                    </div>
                    <div className='group'>
                        <button onClick={this.toggleOptions} className='tablepanel options'/>
                    </div>
                </div>

                <div className='TablePanel__table'>
                    <BasicTable
                        columns={columns}
                        data={data}
                        hlRowIdx={hlRowIdx}
                        selectable={selectable}
                        showUnits={showUnits}
                        selectInfo={selectInfo}
                        tableStore={this.tableStore}
                    />
                    {showOptions && <TablePanelOptions
                        columns={columns}
                        pageSize={pageSize}
                        showUnits={showUnits}
                        onChange={this.onOptionUpdate}
                    />
                    }
                </div>
            </div>
        );
    }
}

TablePanel.propTypes = {
    tbl_id: React.PropTypes.string,
    tableModel: React.PropTypes.object,
    pageSize: React.PropTypes.number,
    showFilters: React.PropTypes.bool,
    selectable: React.PropTypes.bool,
    showToolbar: React.PropTypes.bool
};

TablePanel.defaultProps = {
    showFilters: true,
    selectable: true,
    showToolbar: true,
    pageSize: 50
};

