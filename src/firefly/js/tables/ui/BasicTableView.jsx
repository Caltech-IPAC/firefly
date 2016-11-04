/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import FixedDataTable from 'fixed-data-table';
import Resizable from 'react-component-resizable';
import {debounce, defer, get, isEmpty, padEnd} from 'lodash';

import {calcColumnWidths} from '../TableUtil.js';
import {SelectInfo} from '../SelectInfo.js';
import {FilterInfo} from '../FilterInfo.js';
import {SortInfo} from '../SortInfo.js';
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
            if (size && !this.isUnmounted) {
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
        this.onKeyDown    = this.onKeyDown.bind(this);
        this.onRowSelect  = this.onRowSelect.bind(this);
        this.onSelectAll  = this.onSelectAll.bind(this);
        this.onSort       = this.onSort.bind(this);
        this.onFilter     = this.onFilter.bind(this);
        this.onFilterSelected = this.onFilterSelected.bind(this);
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

    componentWillUnmount() {
        this.isUnmounted = true;
    }

    /*
    componentDidUpdate(){
        const {onTableChanged} = this.props;
        onTableChanged && onTableChanged();
    }
    */

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
        const {columns, data, hlRowIdx, showUnits, showFilters, filterInfo, renderers, bgColor,
            selectable, selectInfoCls, sortInfo, callbacks, textView, rowHeight, showMask, error} = this.props;
        const {widthPx, heightPx, columnWidths} = this.state;
        const {onSort, onFilter, onRowSelect, onSelectAll, onFilterSelected} = this;

        if (!error && isEmpty(columns)) return (<div style={{top: 0}} className='loading-mask'/>);

        // const filterInfoCls = FilterInfo.parse(filterInfo);
        // const sortInfoCls = SortInfo.parse(sortInfo);
        //
        const makeColumnsProps = {columns, data, selectable, selectInfoCls, renderers, bgColor,
                                  columnWidths, filterInfo, sortInfo, showUnits, showFilters,
                                  onSort, onFilter, onRowSelect, onSelectAll, onFilterSelected};

        const headerHeight = 22 + (showUnits && 8) + (showFilters && 22);

        return (
            <Resizable id='table-resizer' tabIndex='-1' onKeyDown={this.onKeyDown} className='TablePanel__frame' onResize={this.onResize}>
                {   error ? <div style={{padding: 10}}>{error}</div> :
                    widthPx === 0 ? <div /> :
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
                        { makeColumns(makeColumnsProps) }
                    </Table>
                }
                {!error && showMask && <div style={{top: 0}} className='loading-mask'/>}
                {!error && !showMask && isEmpty(data) && <div className='TablePanel_NoData'> No Data Found </div>}
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
    bgColor: PropTypes.string,
    error:  PropTypes.string,
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


// components here on down are private.  not all props are defined.
/* eslint-disable react/prop-types */
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

function makeColWidth(columns, showUnits) {
    return !columns ? {} : columns.reduce((widths, col, idx) => {
        const label = col.name;
        var nchar = col.prefWidth;
        const unitLength = showUnits ? get(col, 'units.length', 0) : 0;
        if (!nchar) {
            nchar = Math.max(label.length+2, unitLength+2, get(col,'width', 0)); // 2 is for padding and sort symbol
        }
        widths[idx] = nchar * 7;
        return widths;
    }, {});
}

function makeColumns ({columns, columnWidths, data, selectable, showUnits, showFilters, renderers, bgColor='white',
            selectInfoCls, filterInfo, sortInfo, onRowSelect, onSelectAll, onSort, onFilter, onFilterSelected}) {
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
                header={<HeadRenderer {...{col, showUnits, showFilters, filterInfo, sortInfo, onSort, onFilter}} />}
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
            header={<SelectableHeader {...{checked, onSelectAll, showUnits, showFilters, onFilterSelected}} />}
            cell={<SelectableCell style={{backgroundColor: bgColor}} selectInfoCls={selectInfoCls} onRowSelect={onRowSelect} />}
            fixed={true}
            width={25}
            allowCellsRecycling={true}
        />);
        colsEl.splice(0, 0, cbox);
    }
    return colsEl.filter((c) => c);
}


function tableToText(columns, dataAry, showUnits=false) {

    const colWidths = calcColumnWidths(columns, dataAry);

    // column's name
    var textHead = columns.reduce( (pval, col, idx) => {
        return pval + (get(columns, [idx,'visibility'], 'show') === 'show' ? `${padEnd(col.label || col.name, colWidths[idx])}|` : '');
    }, '|');

    // column's type
    textHead += '\n' + columns.reduce( (pval, col, idx) => {
            return pval + (get(columns, [idx,'visibility'], 'show') === 'show' ? `${padEnd(col.type || '', colWidths[idx])}|` : '');
        }, '|');

    if (showUnits) {
        textHead += '\n' + columns.reduce( (pval, col, idx) => {
                return pval + (get(columns, [idx,'visibility'], 'show') === 'show' ? `${padEnd(col.units || '', colWidths[idx])}|` : '');
            }, '|');
    }

    var textData = dataAry.reduce( (pval, row) => {
        return pval +
            row.reduce( (pv, cv, idx) => {
                const cname = get(columns, [idx, 'name']);
                if (!cname) return pv;      // not defined in columns.. can ignore
                return pv + (get(columns, [idx,'visibility'], 'show') === 'show' ? `${padEnd(cv || '', colWidths[idx])} ` : '');
            }, ' ') + '\n';
    }, '');
    return textHead + '\n' + textData;
}

