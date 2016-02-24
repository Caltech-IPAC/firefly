/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {isEmpty, get, cloneDeep, omitBy, isUndefined} from 'lodash';

import * as TblUtil from '../TableUtil.js';
import {Table} from '../Table.js';
import {TablePanelOptions} from './TablePanelOptions.jsx';
import {BasicTable} from './BasicTable.jsx';
import {RemoteTableStore, TableStore} from '../TableStore.js';
import {SelectInfo} from '../SelectInfo.js';
import {InputField} from '../../ui/InputField.jsx';
import {intValidator} from '../../util/Validate.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';

import LOADING from 'html/images/gxt/loading.gif';
import FILTER from 'html/images/icons-2014/24x24_Filter.png';

export class TablePanel extends Component {
    constructor(props) {
        super(props);
        this.storeUpdate = this.storeUpdate.bind(this);
        this.toggleOptions = this.toggleOptions.bind(this);
        this.onOptionUpdate = this.onOptionUpdate.bind(this);
        this.onPageChange = this.onPageChange.bind(this);

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
            showUnits: props.showUnits,
            showFilters: props.showFilters
        };
    }

    componentWillUnmount() {
        this.props.tableStore && this.props.tableStore.onUnmount();
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
    }

    onOptionUpdate({pageSize, columns, showUnits, showFilters}) {
        if (pageSize) {
            this.tableStore.onPageSizeChange(pageSize);
        }
        if(columns) {
            columns = ensureColumns(this.state.tableModel, columns);
            this.setState({columns});
        }
        const changes = omitBy({showUnits, showFilters}, isUndefined);
        if ( !isEmpty(changes) ) {
            this.setState(changes);
        }
    }

    storeUpdate(state) {
        this.setState(state);
    }

    toggleOptions() {
        this.setState({showOptions: !this.state.showOptions});
    }

    onPageChange (pageNum) {
        if (pageNum.valid) {
            this.tableStore.gotoPage(pageNum.value);
        }
    };

    render() {
        var {tableModel, columns, showOptions, showUnits, showFilters} = this.state;
        const {selectable} = this.props;
        if (isEmpty(columns) || isEmpty(tableModel)) return false;
        const {startIdx, hlRowIdx, currentPage, pageSize, totalPages, tableRowCount,
                    selectionInfo, filterInfo, filterCount, sortInfo, data} = prepareTableData(tableModel);
        const selectInfo = SelectInfo.newInstance(selectionInfo, startIdx);


        const showLoading = !TblUtil.isTableLoaded(tableModel);
        const rowFrom = startIdx + 1;
        const rowTo = startIdx+tableRowCount;

        return (
            <div className='TablePanel__wrapper'>
                <div role='toolbar' style={{height: '33px'}}>
                    <div className='group'>
                        <button style={{width:70}}>Download</button>
                    </div>
                    <div className='group'>
                        <button onClick={() => this.tableStore.gotoPage(1)} className='paging_bar first' title='First Page'/>
                        <button onClick={() => this.tableStore.gotoPage(currentPage - 1)} className='paging_bar previous'  title='Previous Page'/>
                        <InputField
                            validator = {intValidator(1,totalPages, 'Page Number')}
                            tooltip = {'Jump to this page'}
                            size = {2}
                            value = {currentPage+''}
                            onChange = {this.onPageChange}
                            actOn={['blur','enter']}
                            showWarning={false}
                        /> <div style={{fontSize: 'smaller', marginTop: '5px'}} >&nbsp; of {totalPages}</div>
                        <button onClick={() => this.tableStore.gotoPage(currentPage + 1)} className='paging_bar next'  title='Next Page'/>
                        <button onClick={() => this.tableStore.gotoPage(totalPages)} className='paging_bar last'  title='Last Page'/>
                        <div style={{fontSize: 'smaller', marginTop: '5px'}} > &nbsp; ({rowFrom.toLocaleString()} - {rowTo.toLocaleString()} of {tableModel.totalRows.toLocaleString()})</div>
                        {showLoading ? <img style={{width:14,height:14,marginTop: '3px'}} src={LOADING}/> : false}
                    </div>
                    <div className='group'>
                        {filterCount > 0 && <button onClick={() => this.tableStore.onFilter('')} className='tablepanel clearFilters'/>}
                                <ToolbarButton icon={FILTER} tip={'The Filter Panel can be used to remove unwanted data from the search results'}
                                       visible={true}
                                       badgeCount={filterCount}
                                       onClick={() => this.onOptionUpdate({showFilters: !showFilters})}/>
                        <button style={{marginLeft: '4px'}} onClick={this.toggleOptions} className='tablepanel options'/>
                    </div>
                </div>

                <div className='TablePanel__table'>
                    <BasicTable
                        columns={columns}
                        data={data}
                        hlRowIdx={hlRowIdx}
                        selectable={selectable}
                        showUnits={showUnits}
                        showFilters={showFilters}
                        selectInfo={selectInfo}
                        filterInfo={filterInfo}
                        sortInfo={sortInfo}
                        tableStore={this.tableStore}
                    />
                    {showOptions && <TablePanelOptions
                        columns={columns}
                        pageSize={pageSize}
                        showUnits={showUnits}
                        showFilters={showFilters}
                        onChange={this.onOptionUpdate}
                    />
                    }
                </div>
            </div>
        );
    }
}

TablePanel.propTypes = {
    tbl_id: PropTypes.string,
    tableModel: PropTypes.object,
    pageSize: PropTypes.number,
    showUnits: PropTypes.bool,
    showFilters: PropTypes.bool,
    selectable: PropTypes.bool,
    showToolbar: PropTypes.bool
};

TablePanel.defaultProps = {
    showUnits: false,
    showFilters: false,
    selectable: true,
    showToolbar: true,
    pageSize: 50
};



function prepareTableData(tableModel) {
    if (!tableModel.tableData.columns) return {};
    const {selectionInfo} = tableModel;
    const {startIdx, endIdx, hlRowIdx, currentPage, pageSize,totalPages} = TblUtil.gatherTableState(tableModel);
    var data = [];
    if ( Table.newInstance(tableModel).has(startIdx, endIdx) ) {
        data = tableModel.tableData.data.slice(startIdx, endIdx);
    } else {
        Object.assign(tableModel.request, {startIdx, pageSize});
        //TblCntlr.dispatchFetchTable(tableModel.request, highlightedRow);
    }
    var tableRowCount = data.length;
    const filterInfo = get(tableModel, 'request.filters');
    const filterCount = filterInfo ? filterInfo.split(';').length : 0;
    const sortInfo = get(tableModel, 'request.sortInfo');

    return {startIdx, hlRowIdx, currentPage, pageSize,totalPages, tableRowCount, sortInfo, selectionInfo, filterInfo, filterCount, data};
}

function ensureColumns(tableModel, columns) {
    if (isEmpty(columns)) {
        return cloneDeep(get(tableModel, 'tableData.columns', []));
    } else {
        return columns;
    }
}

