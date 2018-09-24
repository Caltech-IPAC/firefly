/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import FixedDataTable from 'fixed-data-table-2';
import {wrapResizer} from '../../ui/SizeMeConfig.js';
import {get, isEmpty} from 'lodash';

import {tableTextView, getTableUiById, tblDropDownId} from '../TableUtil.js';
import {SelectInfo} from '../SelectInfo.js';
import {FilterInfo} from '../FilterInfo.js';
import {SortInfo} from '../SortInfo.js';
import {TextCell, HeaderCell, SelectableHeader, SelectableCell} from './TableRenderer.js';

import './TablePanel.css';
import {hideDropDown} from '../../ui/DialogRootContainer';

const {Table, Column} = FixedDataTable;
const noDataMsg = 'No Data Found';
const noDataFromFilter = 'No data match these criteria';

class BasicTableViewInternal extends PureComponent {
    constructor(props) {
        super(props);
        this.state = {
            showMask: false,
            data: [],
            columnWidths: makeColWidth(props.columns, props.data)
        };

        this.onColumnResizeEndCallback = this.onColumnResizeEndCallback.bind(this);
        this.rowClassName = this.rowClassName.bind(this);
        this.onKeyDown    = this.onKeyDown.bind(this);
        this.onRowSelect  = this.onRowSelect.bind(this);
        this.onSelectAll  = this.onSelectAll.bind(this);
        this.onSort       = this.onSort.bind(this);
        this.onFilter     = this.onFilter.bind(this);
        this.onFilterSelected = this.onFilterSelected.bind(this);
    }

    onColumnResizeEndCallback(newColumnWidth, columnKey) {
        const {tbl_id} = getTableUiById(this.props.tbl_ui_id) || {};
        hideDropDown(tblDropDownId(tbl_id));

        var columnWidths = Object.assign({}, this.state.columnWidths, {[columnKey]: newColumnWidth});
        this.setState({columnWidths});
    }

    rowClassName(index) {
        const {hlRowIdx} = this.props;
        return (hlRowIdx === index) ? 'tablePanel__Row_highlighted' : '';
    }

    componentWillReceiveProps(nProps) {
        if (isEmpty(this.state.columnWidths) && !isEmpty(nProps.columns)) {
            this.setState({columnWidths: makeColWidth(nProps.columns, nProps.data)});
        }
    }

    componentWillUnmount() {
        this.isUnmounted = true;
        const {tbl_id} = getTableUiById(this.props.tbl_ui_id) || {};
        hideDropDown(tblDropDownId(tbl_id));
    }


    onKeyDown(e) {
        const {callbacks, hlRowIdx, currentPage} = this.props;
        const key = get(e, 'key');
        if (key === 'ArrowDown') {
            callbacks.onRowHighlight && callbacks.onRowHighlight(hlRowIdx + 1);
            e.preventDefault && e.preventDefault();
        } else if (key === 'ArrowUp') {
            callbacks.onRowHighlight && callbacks.onRowHighlight(hlRowIdx - 1);
            e.preventDefault && e.preventDefault();
        } else if (key === 'PageDown') {
            callbacks.onGotoPage && callbacks.onGotoPage(currentPage + 1);
            e.preventDefault && e.preventDefault();
        } else if (key === 'PageUp') {
            callbacks.onGotoPage && callbacks.onGotoPage(currentPage - 1);
            e.preventDefault && e.preventDefault();
        }
    }
    onFilterSelected() {
        const {callbacks, selectInfoCls} = this.props;
        if (callbacks.onFilterSelected) {
            const selected = [...selectInfoCls.getSelected()];
            callbacks.onFilterSelected(selected);
        }
    }

    onFilter({fieldKey, valid, value}) {
        const {callbacks, filterInfo} = this.props;
        if (callbacks.onFilter) {
            const filterInfoCls = FilterInfo.parse(filterInfo);
            if (valid && !filterInfoCls.isEqual(fieldKey, value)) {
                filterInfoCls.setFilter(fieldKey, value);
                callbacks.onFilter(filterInfoCls.serialize());
            }
        }
    };

    onSort(cname) {
        const {callbacks, sortInfo} = this.props;
        if (callbacks.onSort) {
            const sortInfoCls = SortInfo.parse(sortInfo);
            callbacks.onSort(sortInfoCls.toggle(cname));
        }
    };

    onSelectAll(checked) {
        const {callbacks} = this.props;
        callbacks.onSelectAll && callbacks.onSelectAll(checked);
    }

    onRowSelect(checked, rowIndex) {
        const {callbacks} = this.props;
        callbacks.onRowSelect && callbacks.onRowSelect(checked, rowIndex);
    }

    render() {
        const {width, height}= this.props.size;
        const {columns, data, hlRowIdx, showUnits, showTypes, showFilters, filterInfo, renderers, bgColor,
            selectable, selectInfoCls, sortInfo, callbacks, textView, rowHeight, showMask, error, tbl_ui_id} = this.props;
        const {columnWidths} = this.state;
        const {onSort, onFilter, onRowSelect, onSelectAll, onFilterSelected} = this;
        const {tbl_id} = getTableUiById(tbl_ui_id) || {};

        if (!error && isEmpty(columns)) return (<div style={{top: 0}} className='loading-mask'/>);

        // const filterInfoCls = FilterInfo.parse(filterInfo);
        // const sortInfoCls = SortInfo.parse(sortInfo);
        //
        const makeColumnsProps = {columns, data, selectable, selectInfoCls, renderers, bgColor,
                                  columnWidths, filterInfo, sortInfo, showUnits, showTypes, showFilters,
                                  onSort, onFilter, onRowSelect, onSelectAll, onFilterSelected, tbl_id};

        const headerHeight = 22 + (showUnits && 8) + (showTypes && 8) + (showFilters && 22);

        return (
            <div tabIndex='-1' onKeyDown={this.onKeyDown} className='TablePanel__frame'>
                {   error ? <div style={{padding: 10}}>{error}</div> :
                    width === 0 ? <div /> :
                    textView ? <TextView { ...{columns, data, showUnits, width, height} }/> :
                    <Table
                        onHorizontalScroll={() => {hideDropDown(tblDropDownId(tbl_id)); return true;} }
                        rowHeight={rowHeight}
                        headerHeight={headerHeight}
                        rowsCount={data.length}
                        isColumnResizing={false}
                        onColumnResizeEndCallback={this.onColumnResizeEndCallback}
                        onRowClick={(e, index) => callbacks.onRowHighlight && callbacks.onRowHighlight(index)}
                        rowClassNameGetter={this.rowClassName}
                        scrollToRow={hlRowIdx}
                        width={width}
                        height={height}>
                        { makeColumns(makeColumnsProps) }
                    </Table>
                }
                {!error && showMask && <div style={{top: 0}} className='loading-mask'/>}
                {!error && !showMask && isEmpty(data) && <div className='TablePanel_NoData'> {filterInfo ? noDataFromFilter : noDataMsg} </div>}
            </div>
        );
    }
}

BasicTableViewInternal.propTypes = {
    tbl_ui_id: PropTypes.string,
    columns: PropTypes.arrayOf(PropTypes.object),
    data: PropTypes.arrayOf(PropTypes.array),
    hlRowIdx: PropTypes.number,
    selectInfoCls: PropTypes.instanceOf(SelectInfo),
    filterInfo: PropTypes.string,
    sortInfo: PropTypes.string,
    selectable: PropTypes.bool,
    showUnits: PropTypes.bool,
    showTypes: PropTypes.bool,
    showFilters: PropTypes.bool,
    textView: PropTypes.bool,
    rowHeight: PropTypes.number,
    showMask: PropTypes.bool,
    currentPage: PropTypes.number,
    bgColor: PropTypes.string,
    error:  PropTypes.string,
    size: PropTypes.object.isRequired,
    renderers: PropTypes.objectOf(
        PropTypes.shape({
            cellRenderer: PropTypes.func,
            headRenderer: PropTypes.func
        })
    ),
    callbacks: PropTypes.shape({
        onRowHighlight: PropTypes.func,
        onRowSelect: PropTypes.func,
        onSelectAll: PropTypes.func,
        onSort: PropTypes.func,
        onFilter: PropTypes.func,
        onGotoPage: PropTypes.func
    })

};

BasicTableViewInternal.defaultProps = {
    selectable: false,
    showUnits: false,
    showTypes: false,
    showFilters: false,
    showMask: false,
    rowHeight: 20,
    currentPage: -1
};

export const BasicTableView= wrapResizer(BasicTableViewInternal);

const TextView = ({columns, data, showUnits, width, height}) => {
    const text = tableTextView(columns, data, showUnits);
    return (
        <div style={{height, width,overflow: 'hidden'}}>
            <div style={{height: '100%',overflow: 'auto'}}>
                <pre>{text}</pre>
            </div>
        </div>
    );
};

function calcMaxWidth(idx, col, data) {
    let nchar = col.prefWidth || col.width;
    if (!nchar) {
        const label = col.label || col.name;
        const hWidth = Math.max(
            get(label, 'length', 0) + 2,
            get(col, 'units.length', 0) + 2,
            get(col, 'type.length', 0) + 2
        );
        nchar = hWidth;
        for (const r in data) {
            const w = get(data, [r, idx, 'length'], 0);
            if (w > nchar) nchar = w;
        }
    }
    return nchar * 7;
}

function makeColWidth(columns, data) {

    return !columns ? {} : columns.reduce((widths, col, idx) => {
        widths[idx] = calcMaxWidth(idx, col, data);
        return widths;
    }, {});
}

function makeColumns ({columns, columnWidths, data, selectable, showUnits, showTypes, showFilters, renderers, bgColor='white',
            selectInfoCls, filterInfo, sortInfo, onRowSelect, onSelectAll, onSort, onFilter, onFilterSelected, tbl_id}) {
    if (!columns) return false;

    var colsEl = columns.map((col, idx) => {
        if (col.visibility && col.visibility !== 'show') return false;
        const HeadRenderer = get(renderers, [col.name, 'headRenderer'], HeaderCell);
        const CellRenderer = get(renderers, [col.name, 'cellRenderer'], TextCell);
        const fixed = col.fixed || false;
        const style = col.fixed && {backgroundColor: bgColor};

        return (
            <Column
                key={col.name}
                columnKey={idx}
                header={<HeadRenderer {...{col, showUnits, showTypes, showFilters, filterInfo, sortInfo, onSort, onFilter, tbl_id}} />}
                cell={<CellRenderer style={style} data={data} colIdx={idx} />}
                fixed={fixed}
                width={columnWidths[idx]}
                isResizable={true}
                allowCellsRecycling={true}
            />
        );
    });
    if (selectable) {
        const checked = selectInfoCls.isSelectAll();
        var cbox = (<Column
            key='selectable-checkbox'
            columnKey='selectable-checkbox'
            header={<SelectableHeader {...{checked, onSelectAll, showUnits, showTypes, showFilters, onFilterSelected}} />}
            cell={<SelectableCell style={{backgroundColor: bgColor}} selectInfoCls={selectInfoCls} onRowSelect={onRowSelect} />}
            fixed={true}
            width={25}
            allowCellsRecycling={true}
        />);
        colsEl.splice(0, 0, cbox);
    }
    return colsEl.filter((c) => c);
}


