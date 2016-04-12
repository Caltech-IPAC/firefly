/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import FixedDataTable from 'fixed-data-table';
import Resizable from 'react-component-resizable';
import {debounce, get, isEmpty, pick} from 'lodash';

import {SelectInfo} from '../SelectInfo.js';
import {FilterInfo} from '../FilterInfo.js';
import {SortInfo} from '../SortInfo';
import {tableToText} from '../TableUtil.js';
import {TextCell, HeaderCell} from './TableRenderer.js';

import './TablePanel.css';

const {Table, Column} = FixedDataTable;

export class BasicTableView extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            showMask: false,
            widthPx: 75,
            heightPx: 75,
            columnWidths: makeColWidth(props.columns, props.data, props.showUnits)
        };

        this.onResize =  debounce((size) => {
                if (size) {
                    var widthPx = size.width;
                    var heightPx = size.height;
                    this.setState({widthPx, heightPx});
                }
            }, 100);

        this.onColumnResizeEndCallback = this.onColumnResizeEndCallback.bind(this);
        this.rowClassName = this.rowClassName.bind(this);
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
        this.setState({showMask: false});
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
    }

    render() {
        const {columns, data, hlRowIdx, showUnits, showFilters, filterInfo,
                    sortInfo, callbacks, textView, rowHeight} = this.props;
        const {widthPx, heightPx, columnWidths, showMask} = this.state;

        if (isEmpty(columns)) return (<div style={{top: 0}} className='loading-mask'/>);

        const filterInfoCls = FilterInfo.parse(filterInfo);
        const sortInfoCls = SortInfo.parse(sortInfo);

        const showMaskNow = () => this.setState({showMask: true});
        const onSelect = (checked, rowIndex) => callbacks.onRowSelect && callbacks.onRowSelect(checked, rowIndex);
        const onSelectAll = (checked) => callbacks.onSelectAll && callbacks.onSelectAll(checked);
        const onSort = (cname) => {
                if (callbacks.onSort) {
                    showMaskNow();
                    callbacks.onSort(sortInfoCls.toggle(cname).serialize());
                }
            };

        const onFilter = ({fieldKey, valid, value}) => {
                if (callbacks.onFilter) {
                    if (valid && !filterInfoCls.isEqual(fieldKey, value)) {
                        filterInfoCls.setFilter(fieldKey, value);
                        showMaskNow();
                        callbacks.onFilter(filterInfoCls.serialize());
                    }
                }
            };

        const colProps = pick(this.props, ['columns', 'data', 'selectable', 'selectInfoCls', 'callbacks', 'renderers']);
        Object.assign(colProps, {columnWidths, filterInfoCls, sortInfoCls, showUnits, showFilters}, {onSort, onFilter, onSelect, onSelectAll});

        const headerHeight = 22 + (showUnits && 12) + (showFilters && 20);
        return (
            <Resizable id='table-resizer' style={{position: 'relative', width: '100%', height: '100%', overflow: 'hidden'}} onResize={this.onResize}>
                { textView ? <TextView { ...{columns, data, showUnits, heightPx, widthPx} }/> :
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
                {isEmpty(data) && <div className='tablePanel_NoData'> No Data Found </div>}
            </Resizable>
        );
    }
}

BasicTableView.propTypes = {
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
        onFilter: PropTypes.func
    })
};

BasicTableView.defaultProps = {
    selectable: false,
    showUnits: false,
    showFilters: false,
    rowHeight: 20
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
            nchar = Math.max(label.length, unitLength, get(data, `0.${cidx}.length`, 0));
        }
        widths[col.name] = nchar * 8 + 20;    // 20 is for the padding and sort symbol
        return widths;
    }, {});
}

function makeColumns ({columns, columnWidths, data, selectable, showUnits, showFilters, renderers,
            selectInfoCls, filterInfoCls, sortInfoCls, onSelect, onSelectAll, onSort, onFilter}) {
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
        const headerCB = () => {
            return (
                <div className='tablePanel__checkbox'>
                    <input type='checkbox' checked={selectInfoCls.isSelectAll()} onChange ={(e) => onSelectAll(e.target.checked)}/>
                </div>
            );
        };

        const cellCB = ({rowIndex}) => {
            const onRowSelect = (e) => onSelect(e.target.checked, rowIndex);
            return (
                <div className='tablePanel__checkbox' style={{backgroundColor: 'whitesmoke'}}>
                    <input type='checkbox' checked={selectInfoCls.isSelected(rowIndex)} onChange={onRowSelect}/>
                </div>
            );
        };

        var cbox = <Column
            key='selectable-checkbox'
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

