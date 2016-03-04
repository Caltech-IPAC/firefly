/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import FixedDataTable from 'fixed-data-table';
import Resizable from 'react-component-resizable';
import {debounce, get, isEmpty, pick} from 'lodash';

import {SelectInfo} from '../SelectInfo.js';
import {FilterInfo, FILTER_TTIPS} from '../FilterInfo.js';
import {InputField} from '../../ui/InputField.jsx';
import {SORT_ASC, UNSORTED, SortInfo} from '../SortInfo';
import {tableToText} from '../TableUtil.js';

import './TablePanel.css';
import ASC_ICO from 'html/images/sort_asc.gif';
import DESC_ICO from 'html/images/sort_desc.gif';

const {Table, Column, Cell} = FixedDataTable;

export class BasicTable extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            showMask: false,
            widthPx: 0,
            heightPx: 0,
            columnWidths: makeColWidth(props.columns, props.data, props.showUnits)
        };

        this.onResize = this.onResize.bind(this);
        this.onColumnResizeEndCallback = this.onColumnResizeEndCallback.bind(this);
        this.rowClassName = this.rowClassName.bind(this);
    }

    onResize() {
        return debounce((size) => {
            if (size) {
                var widthPx = size.width;
                var heightPx = size.height;
                this.setState({widthPx, heightPx});
            }
        }, 200);
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
                    sortInfo, tableStore, width, height, textView} = this.props;
        const {widthPx, heightPx, columnWidths, showMask} = this.state;

        if (isEmpty(columns)) return false;

        var style = {width, height};
        const filterInfoCls = FilterInfo.parse(filterInfo);
        const sortInfoCls = SortInfo.parse(sortInfo);

        const showMaskNow = () => this.setState({showMask: true});
        const onSelect = (checked, rowIndex) => tableStore.onRowSelect && tableStore.onRowSelect(checked, rowIndex);
        const onSelectAll = (checked) => tableStore.onSelectAll && tableStore.onSelectAll(checked);
        const onSort = (cname) => {
                    showMaskNow();
                    tableStore.onSort && tableStore.onSort(sortInfoCls.toggle(cname).serialize());
                };

        const onFilter = ({fieldKey, valid, value}) => {
                    if (valid && !filterInfoCls.isEqual(fieldKey, value)) {
                        filterInfoCls.setFilter(fieldKey, value);
                        showMaskNow();
                        tableStore.onFilter && tableStore.onFilter(filterInfoCls.serialize());
                    }
                };

        const colProps = pick(this.props, ['columns', 'data', 'selectable', 'selectInfoCls', 'tableStore']);
        Object.assign(colProps, {columnWidths, filterInfoCls, sortInfoCls, showUnits, showFilters}, {onSort, onFilter, onSelect, onSelectAll});

        const headerHeight = 22 + (showUnits && 12) + (showFilters && 20);
        return (
            <Resizable id='table-resizer' style={style} onResize={this.onResize()}>
                { textView ? <TextView { ...{columns, data, showUnits} }/> :
                    <Table
                        rowHeight={20}
                        headerHeight={headerHeight}
                        rowsCount={data.length}
                        isColumnResizing={false}
                        onColumnResizeEndCallback={this.onColumnResizeEndCallback}
                        onRowClick={(e, index) => tableStore.onRowHighlight && tableStore.onRowHighlight(index)}
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

BasicTable.propTypes = {
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
    width: PropTypes.string,
    height: PropTypes.string,
    tableStore: PropTypes.shape({
        onRowHighlight: PropTypes.func,
        onRowSelect: PropTypes.func,
        onSelectAll: PropTypes.func,
        onSort: PropTypes.func,
        onFilter: PropTypes.func
    })
};

BasicTable.defaultProps = {
    selectable: false,
    showUnits: false,
    showFilters: false,
    width: '100%',
    height: '100%'
};

const TextView = ({columns, data, showUnits}) => {
    const text = tableToText(columns, data, showUnits);
    return <div style={{height:'100%',overflow: 'auto'}}><pre>{text}</pre></div>;
};

const SortSymbol = ({sortDir}) => {
    return <img style={{marginLeft: 2}} src={sortDir === SORT_ASC ? ASC_ICO : DESC_ICO}/>;
};

const TextCell = ({rowIndex, data, col}) => {
    return (
        <Cell>
            {get(data, [rowIndex, col],'undef')}
        </Cell>
    );
};

const HeaderCell = ({col, showUnits, showFilters, filterInfoCls, sortInfoCls, onSort, onFilter}) => {

    const cname = col.name;
    const sortDir = sortInfoCls.getDirection(cname);

    return (
        <div title={col.title || cname} className='TablePanel__header'>
            <div style={{width: '100%', cursor: 'pointer'}} onClick={() => onSort(cname)} >{cname}
                { sortDir!==UNSORTED && <SortSymbol sortDir={sortDir}/> }
            </div>
            {showUnits && col.units && <div style={{fontWeight: 'normal'}}>({col.units})</div>}
            {showFilters && <InputField
                validator={FilterInfo.validator}
                fieldKey={cname}
                tooltip = {FILTER_TTIPS}
                value = {filterInfoCls.getFilter(cname)}
                onChange = {(v) => onFilter(v)}
                actOn={['blur','enter']}
                showWarning={false}
                width='100%'
            />
            }
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

function makeColumns ({columns, columnWidths, data, selectable, showUnits, showFilters,
            selectInfoCls, filterInfoCls, sortInfoCls, onSelect, onSelectAll, onSort, onFilter}) {
    if (!columns) return false;

    var colsEl = columns.map((col, idx) => {
        if (col.visibility !== 'show') return false;
        return (
            <Column
                key={col.name}
                columnKey={col.name}
                header={<HeaderCell {...{col, showUnits, showFilters, filterInfoCls, sortInfoCls, onSort, onFilter}} />}
                cell={<TextCell data={data} col={idx} />}
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

