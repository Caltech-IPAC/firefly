/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import FixedDataTable from 'fixed-data-table';
import Resizable from 'react-component-resizable';
import {debounce, defer, get, isEmpty, pick} from 'lodash';

import {SelectInfo} from '../SelectInfo.js';
import {FilterInfo} from '../FilterInfo.js';
import {SortInfo} from '../SortInfo';
import {tableToText} from '../TableUtil.js';
import {TextCell, HeaderCell, SelectableHeader, SelectableCell} from './TableRenderer.js';

import './TablePanel.css';

const {Table, Column} = FixedDataTable;

export class BasicTableView extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            showMask: false,
            widthPx: 0,
            heightPx: 200,
            data: [],
            columnWidths: makeColWidth(props.columns, props.data, props.showUnits)
        };

        const normal = (size) => {
            if (size) {
                var widthPx = size.width;
                var heightPx = size.height;
                this.setState({widthPx, heightPx});
            }
        };
        const debounced = debounce(normal, 100);
        this.onResize =  (size) => {
            if (this.state.widthPx === 0) {
                defer(normal, size);
            } else {
                debounced(size);
            }
        };

        this.onColumnResizeEndCallback = this.onColumnResizeEndCallback.bind(this);
        this.rowClassName = this.rowClassName.bind(this);
        this.onKeyDown = this.onKeyDown.bind(this);
    }

    onColumnResizeEndCallback(newColumnWidth, columnKey) {
        var columnWidths = Object.assign({}, this.state.columnWidths, {[columnKey]: newColumnWidth});
        this.setState({columnWidths});
    }

    rowClassName(index) {
        const {hlRowIdx} = this.props;
        return (hlRowIdx === index) ? 'tablePanel__Row_highlighted' : '';
    }

    componentWillReceiveProps(nProps) {
        if (isEmpty(this.state.columnWidths) && !isEmpty(nProps.columns)) {
            this.setState({columnWidths: makeColWidth(nProps.columns, nProps.data, nProps.showUnits)});
        }
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
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

    render() {
        const {columns, data, hlRowIdx, showUnits, showFilters, filterInfo,
                    sortInfo, callbacks, textView, rowHeight, showMask} = this.props;
        const {widthPx, heightPx, columnWidths} = this.state;

        if (isEmpty(columns)) return (<div style={{top: 0}} className='loading-mask'/>);

        const filterInfoCls = FilterInfo.parse(filterInfo);
        const sortInfoCls = SortInfo.parse(sortInfo);

        const onRowSelect = (checked, rowIndex) => callbacks.onRowSelect && callbacks.onRowSelect(checked, rowIndex);
        const onSelectAll = (checked) => callbacks.onSelectAll && callbacks.onSelectAll(checked);
        const onSort = (cname) => {
                if (callbacks.onSort) {
                    callbacks.onSort(sortInfoCls.toggle(cname).serialize());
                }
            };

        const onFilter = ({fieldKey, valid, value}) => {
                if (callbacks.onFilter) {
                    if (valid && !filterInfoCls.isEqual(fieldKey, value)) {
                        filterInfoCls.setFilter(fieldKey, value);
                        callbacks.onFilter(filterInfoCls.serialize());
                    }
                }
            };

        const colProps = pick(this.props, ['columns', 'data', 'selectable', 'selectInfoCls', 'callbacks', 'renderers']);
        Object.assign(colProps, {columnWidths, filterInfoCls, sortInfoCls, showUnits, showFilters}, {onSort, onFilter, onRowSelect, onSelectAll});

        const headerHeight = 22 + (showUnits && 12) + (showFilters && 20);
        return (
            <Resizable id='table-resizer' tabIndex='-1' onKeyDown={this.onKeyDown} className='TablePanel__frame' onResize={this.onResize}>
                {   widthPx === 0 ? <div /> :
                    textView ? <TextView { ...{columns, data, showUnits, heightPx, widthPx} }/> :
                    <Table
                        rowHeight={rowHeight}
                        headerHeight={headerHeight}
                        rowsCount={data.length}
                        isColumnResizing={false}
                        onColumnResizeEndCallback={this.onColumnResizeEndCallback}
                        onRowClick={(e, index) => callbacks.onRowHighlight && callbacks.onRowHighlight(index)}
                        rowClassNameGetter={this.rowClassName}
                        scrollToRow={hlRowIdx}
                        width={widthPx}
                        height={heightPx}>
                        { makeColumns(colProps) }
                    </Table>
                }
                {showMask && <div style={{top: 0}} className='loading-mask'/>}
                {!showMask && isEmpty(data) && <div className='tablePanel_NoData'> No Data Found </div>}
            </Resizable>
        );
    }
}

BasicTableView.propTypes = {
    tbl_ui_id: PropTypes.string,
    columns: PropTypes.arrayOf(PropTypes.object),
    data: PropTypes.arrayOf(PropTypes.array),
    hlRowIdx: PropTypes.number,
    selectInfoCls: PropTypes.instanceOf(SelectInfo),
    filterInfo: PropTypes.string,
    sortInfo: PropTypes.string,
    selectable: PropTypes.bool,
    showUnits: PropTypes.bool,
    showFilters: PropTypes.bool,
    textView: PropTypes.bool,
    rowHeight: PropTypes.number,
    showMask: PropTypes.bool,
    currentPage: PropTypes.number,
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

BasicTableView.defaultProps = {
    selectable: false,
    showUnits: false,
    showFilters: false,
    showMask: false,
    rowHeight: 20,
    currentPage: -1
};

const TextView = ({columns, data, showUnits, widthPx, heightPx}) => {
    const text = tableToText(columns, data, showUnits);
    return (
        <div style={{height: heightPx, width: widthPx,overflow: 'hidden'}}>
            <div style={{height: '100%',overflow: 'auto'}}>
                <pre>{text}</pre>
            </div>
        </div>
    );
};

function makeColWidth(columns, data, showUnits) {
    return !columns ? {} : columns.reduce((widths, col, cidx) => {
        const label = col.name;
        var nchar = col.prefWidth;
        const unitLength = showUnits ? get(col, 'units.length', 0) : 0;
        if (!nchar) {
            nchar = Math.max(label.length+2, unitLength+2, get(data, `0.${cidx}.length`, 0)); // 2 is for padding and sort symbol
        }
        widths[col.name] = nchar * 8;
        return widths;
    }, {});
}

function makeColumns ({columns, columnWidths, data, selectable, showUnits, showFilters, renderers,
            selectInfoCls, filterInfoCls, sortInfoCls, onRowSelect, onSelectAll, onSort, onFilter}) {
    if (!columns) return false;

    var colsEl = columns.map((col, idx) => {
        if (col.visibility !== 'show') return false;
        const HeadRenderer = get(renderers, [col.name, 'headRenderer'], HeaderCell);
        const CellRenderer = get(renderers, [col.name, 'cellRenderer'], TextCell);

        return (
            <Column
                key={col.name}
                columnKey={col.name}
                header={<HeadRenderer {...{col, showUnits, showFilters, filterInfoCls, sortInfoCls, onSort, onFilter}} />}
                cell={<CellRenderer data={data} col={idx} />}
                fixed={false}
                width={columnWidths[col.name]}
                isResizable={true}
                allowCellsRecycling={true}
            />
        );
    });
    if (selectable) {
        var cbox = <Column
            key='selectable-checkbox'
            columnKey='selectable-checkbox'
            header={<SelectableHeader checked={selectInfoCls.isSelectAll()} onSelectAll={onSelectAll} />}
            cell={<SelectableCell selectInfoCls={selectInfoCls} onRowSelect={onRowSelect} />}
            fixed={true}
            width={25}
            allowCellsRecycling={true}
        />;
        colsEl.splice(0, 0, cbox);
    }
    return colsEl;
}

