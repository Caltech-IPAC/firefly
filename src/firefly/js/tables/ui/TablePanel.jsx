/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import ReactDOM from 'react-dom';
import FixedDataTable from 'fixed-data-table';
import Resizable from 'react-component-resizable';
import {debounce, get, uniqueId, isEmpty} from 'lodash';

import * as TblUtil from '../TableUtil.js';
import * as TblCntlr from '../TablesCntlr';
import * as TblUiCntlr from '../TablesUiCntlr.js';
import {SelectInfo} from '../SelectInfo';
import {Toolbar} from '../../ui/Toolbar.jsx';
import {TableStore} from '../TableStore.js';


import './TablePanel.css';
import LOADING from 'html/images/gxt/loading.gif';

const {Table, Column, Cell} = FixedDataTable;

const TextCell = ({rowIndex, data, col, ...rest}) => {
    return (
        <Cell {...rest}>
            {get(data, [rowIndex, col],'undef')}
        </Cell>
    );
};

function makeColumns(tableStore, columns, columnWidths, data, selectable, selectionInfo) {
    if (!columns) return false;
    var colsEl = columns.map((col, idx) => {
        return (
            <Column
                key={col.name}
                columnKey={col.name}
                header={<Cell>{col.title || col.name}</Cell>}
                cell={<TextCell data={data} col={idx} />}
                fixed={false}
                width={columnWidths[col.name]}
                isResizable={true}
                allowCellsRecycling={true}
            />
        )
    });
    if (selectable) {
        var selectInfo = SelectInfo.newInstance(selectionInfo);
        const headerCB = () => {
            return (
                <input style={{marginTop: '6px'}} className='tablePanel__checkbox' type='checkbox'
                       checked={selectInfo.isSelectAll()} onClick={(e) => tableStore.onSelectAll(e.target.checked, selectInfo)}/>
            );
        };

        const cellCB = ({rowIndex}) => {
            let absIdx = tableStore.toAbsIdx(rowIndex);
            return (
                <input className='tablePanel__checkbox' type='checkbox' value='rowIn'
                       checked={selectInfo.isSelected(absIdx)}
                       onClick={(e) => tableStore.onRowSelect(e.target.checked, rowIndex, selectInfo)}/>
            );
        };

        var cbox = <Column
            key="selectable-checkbox"
            columnKey='selectable-checkbox'
            header={headerCB}
            cell={cellCB}
            fixed={true}
            width={25}
            allowCellsRecycling={true}
        />;
        colsEl.splice(0, 0, cbox);
    }
    return colsEl;
}

export class TablePanel extends React.Component {
    constructor(props) {
        super(props);
        this.tableStore = TableStore.newInstance(this);
        this.state = {
            tableModel: undefined,
            tableUi: {
                tbl_id: undefined,
                tbl_ui_id: TblUtil.uniqueTblUiId(),
                tbl_ui_gid: undefined,
                pageSize : 1,
                currentPage : 1,
                widthPx: 300,
                heightPx: 100,
                hlRowIdx:0,           // this is the UI hlrow.  its index is relative to only current page.
                columns:[],
                columnWidths : {}
            },
            totalPages: 0,
            tableRowCount : 0,
            data: []
        };

        this.onResize = debounce((size) => {
            this.tableStore.onResize(size);
        }, 200);

        this.onColumnResizeEndCallback = this.onColumnResizeEndCallback.bind(this);
        this.rowClassName = this.rowClassName.bind(this);
        this.storeUpdate = this.storeUpdate.bind(this);
    }

    onColumnResizeEndCallback(newColumnWidth, columnKey) {
        this.tableStore.onColumnResize({[columnKey] : newColumnWidth});
    }

    rowClassName(index) {
        return (this.state.tableUi.hlRowIdx === index) ? 'tablePanel__Row_highlighted' : '';
    }

    componentDidMount() {
        this.tableStore.setChangeListener(this.storeUpdate);
    }

    componentWillUnmount() {
        this.tableStore.removeChangeListener();
    }

    componentWillReceiveProps(nProps) {
        this.tableStore.receiveProps(nProps);
    }

    shouldComponentUpdate(nProps, nState) {
        return nState !== this.state;
    }

    storeUpdate(state) {
        this.setState(state);
    }

    render() {
        const {tableUi, tableModel, tableRowCount, totalPages, data} = this.state;
        const {selectable} = this.props;
        if (isEmpty(tableUi.columns)) return false;

        const showLoading = !TblUtil.isTableLoaded(tableModel);
        const rowFrom = (tableUi.currentPage-1) * tableUi.pageSize + 1;
        const rowTo = rowFrom + tableRowCount - 1;

        return (
            <Resizable id='table-resizer' style={{width: '100%'}} onResize={this.onResize} {...this.props} >
                <div className='TablePanel__wrapper'>
                    <Toolbar>
                        <ul role='left'>
                            <li><button style={{width:70}}>Download</button></li>
                        </ul>
                        <ul role='middle' style={{width: '320px'}}>
                            <li><button onClick={() => this.tableStore.gotoPage(1)} className='paging_bar first' title='First Page'/></li>
                            <li><button onClick={() => this.tableStore.gotoPage(tableUi.currentPage - 1)} className='paging_bar previous'  title='Previous Page'/></li>
                            <li><input onClick={(e) => e.target.select()} onChange={(e) => this.tableStore.gotoPage(e.target.value)} name='pageNo' size="2" value={tableUi.currentPage}/>  of {totalPages}</li>
                            <li><button onClick={() => this.tableStore.gotoPage(tableUi.currentPage + 1)} className='paging_bar next'  title='Next Page'/></li>
                            <li><button onClick={() => this.tableStore.gotoPage(totalPages)} className='paging_bar last'  title='Last Page'/></li>
                            <li><div style={{marginTop: '3px'}} >({rowFrom.toLocaleString()} - {rowTo.toLocaleString()} of {tableModel.totalRows.toLocaleString()})</div></li>
                            {showLoading ? <img style={{width:14,height:14,marginTop: '3px'}} src={LOADING}/> : false}
                        </ul>
                        <ul role='right'>
                            <li>
                                <button>Option</button>
                            </li>
                        </ul>
                    </Toolbar>
                    <Table
                        rowHeight={20}
                        headerHeight={25}
                        rowsCount={tableRowCount}
                        isColumnResizing={false}
                        onColumnResizeEndCallback={this.onColumnResizeEndCallback}
                        onRowClick={(e, index) => this.tableStore.onRowHighlight(index)}
                        rowClassNameGetter={this.rowClassName}
                        scrollToRow={tableUi.hlRowIdx}
                        width={tableUi.widthPx}
                        height={tableUi.heightPx}
                        {...this.props}>
                        {makeColumns(this.tableStore, tableUi.columns, tableUi.columnWidths, data, selectable, tableModel.selectionInfo )}
                    </Table>
                </div>
            </Resizable>
        );
    }
}

TablePanel.propTypes = {
    tableModel: React.PropTypes.object,
    tbl_id: React.PropTypes.string,
    tbl_ui_gid: React.PropTypes.string,
    pageSize: React.PropTypes.number,
    showFilters: React.PropTypes.bool,
    selectable: React.PropTypes.bool,
    showToolbar: React.PropTypes.bool
};

TablePanel.defaultProps = {
    showFilters: true,
    selectable: true,
    showToolbar: true,
    pageSize: 50,
    tbl_ui_gid: TblUtil.uniqueTblUiGid()

};

